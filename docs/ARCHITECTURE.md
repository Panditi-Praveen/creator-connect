# CreatorConnect — Architecture Documentation

## Overview

CreatorConnect follows a **microservices architecture** using **Spring Cloud**, enabling modular development, independent scaling, and clear separation of concerns. Each service is independently deployable, communicates via REST APIs, and registers with the Eureka service discovery server.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      FRONTEND (React + TypeScript)           │
│                  Single Page Application                     │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP/HTTPS
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                  API GATEWAY (Spring Cloud Gateway)           │
│              Port 8080 — Single Entry Point                   │
│              Routing & Load Balancing (no token checks)       │
└────┬────────┬────────┬────────┬────────┬────────┬───────────┘
     │        │        │        │        │        │
     ▼        ▼        ▼        ▼        ▼        ▼
┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐
│ Auth    ││ Profile ││ Project ││ Hiring  ││   AI    ││ Eureka  │
│ Service ││ Service ││ Service ││ Service ││ Service ││ Registry│
│ :8081   ││ :8082   ││ :8083   ││ :8084   ││ :8085   ││ :8761   │
└────┬────┘└────┬────┘└────┬────┘└────┬────┘└────┬────┘└─────────┘
     │          │          │          │          │
     └──────────┴──────────┴──────────┴──────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │    MySQL DB      │
                    │  (Shared or per- │
                    │   service)       │
                    └──────────────────┘
```

---

## Service Details

### Service Registry (Eureka — Port 8761)

**Purpose:** Service discovery and registration.

All microservices register themselves with Eureka on startup. The API Gateway uses Eureka to discover service instances and route requests dynamically.

**Configuration:**
- Port: 8761
- Dependency: `spring-cloud-starter-netflix-eureka-server`
- No database required (in-memory registry)

### API Gateway (Spring Cloud Gateway — Port 8080)

**Purpose:** Single entry point for all client requests.

Routes incoming requests to the appropriate microservice based on the request path, load-balancing through Eureka. JWT validation happens **inside each microservice** — the gateway forwards the `Authorization` header unchanged and does not inspect tokens.

**Configuration:**
- Port: 8080
- Dependency: `spring-cloud-starter-gateway-server-webmvc`, `spring-cloud-starter-netflix-eureka-client`
- Routes defined in `application.yml`:
  - `/auth/**` → Auth Service (:8081)
  - `/profile/**` → Profile Service (:8082)
  - `/projects/**` → Project Service (:8083)
  - `/hiring/**` → Hiring Service (:8084)
  - `/ai/**` → AI Service (:8085)
- The Hiring Service's controllers are mapped at `/applications` and `/reviews`
  (no `/hiring` prefix), so the hiring route applies a `rewritePath` filter
  (`/hiring/(.*)` → `/$1`) that strips the prefix before forwarding:
  `/hiring/applications` reaches the service as `/applications`.

### Auth Service (Port 8081)

**Purpose:** User authentication and authorization.

**Responsibilities:**
- User registration (CREATOR or FREELANCER role)
- User login with JWT token generation
- Token validation for protected endpoints
- BCrypt password hashing
- Role-based access control

**API Endpoints:**
| Method | Path | Description |
|---|---|---|
| POST | `/auth/register` | Register new user |
| POST | `/auth/login` | Login and receive JWT |

(JWT validation for protected endpoints is performed by every service, which
shares the same HMAC signing secret and validates the issuer claim.)

**Data:** User entity (id, email, password hash, role)

### Profile Service (Port 8082)

**Purpose:** Manage creator and freelancer profiles (one unified profile per user).

**Responsibilities:**
- Profile CRUD (a single `Profile` model serves both creators and freelancers)
- Skills, experience, availability, and portfolio/link fields

**API Endpoints:**
| Method | Path | Description |
|---|---|---|
| POST | `/profile` | Create my profile |
| GET | `/profile/me` | Get my profile |
| GET | `/profile/{userId}` | Get any user's profile |
| PUT | `/profile/{userId}` | Update profile (owner only) |
| DELETE | `/profile/{userId}` | Delete profile (owner only) |

**Data:** Profile entity (unified creator/freelancer model — skills and experience
are fields on the profile; there are no separate Skill/Portfolio entities)

### Project Service (Port 8083)

**Purpose:** Manage project listings and lifecycle.

**Responsibilities:**
- Project creation by creators
- Project browsing by freelancers
- Project updates and status management
- Project visibility control

**API Endpoints:**
| Method | Path | Description |
|---|---|---|
| POST | `/projects` | Create project |
| GET | `/projects` | List projects (with filters) |
| GET | `/projects/my` | Get my projects |
| GET | `/projects/{id}` | Get project details |
| PUT | `/projects/{id}` | Update project |
| DELETE | `/projects/{id}` | Delete/close project |

**Data:** Project entity

**Integration (Day 6):** project reads are enriched with the owner's public
profile via the `ProfileClient` OpenFeign client (resolved through Eureka,
forwarding the caller's JWT): `GET /projects`, `GET /projects/{id}` and
`GET /projects/my` attach an `ownerProfile` block (name, headline, profile
image, skills) fetched from `GET /profile/{userId}` on the Profile Service.
The enrichment is best-effort: a user may post a project before creating a
profile (`404`), and a Profile Service outage must not take down the project
feed — in both cases the project is returned with `ownerProfile` omitted.

### Hiring Service (Port 8084)

**Purpose:** Manage applications, hiring decisions, and reviews.

**Responsibilities:**
- Application submission, viewing, and withdrawal
- Creator decisions (ACCEPT / REJECT) on pending applications
- Reviews and ratings (creator-owned projects only)
- Project existence and creator project-ownership verification against the
  Project Service (OpenFeign)

**API Endpoints** (gateway paths — the `/hiring` prefix is stripped before forwarding):
| Method | Path | Description |
|---|---|---|
| POST | `/hiring/applications` | Apply to project |
| GET | `/hiring/applications/my` | Get my applications |
| GET | `/hiring/applications/project/{projectId}` | Get applications for a project (owner only) |
| PUT | `/hiring/applications/{id}/status` | Accept/reject an application (owner only) |
| DELETE | `/hiring/applications/{id}` | Withdraw application |
| POST | `/hiring/reviews` | Submit review (project owner only) |
| GET | `/hiring/reviews/freelancer/{freelancerId}` | Get freelancer reviews + average rating |

**Data:** Application, Review entities

**Integration (Day 6):** via the `ProjectClient` OpenFeign client (resolved
through Eureka, forwarding the caller's JWT), the service calls
`GET /projects/{id}` on the Project Service to verify a project exists before
accepting an application, and to verify the caller owns the project before
exposing its applications, deciding on an application, or submitting a review.
A missing project yields `404`, a non-owner `403`, and an unreachable Project
Service `503`.

### AI Service (Port 8085)

**Purpose:** AI-assisted talent discovery.

**Responsibilities:**
- Accept natural language queries
- Interpret queries via LLM API
- Extract structured search criteria
- Match against real freelancer profiles
- Rank results by relevance

**API Endpoints (planned — the AI service is not implemented yet):**
| Method | Path | Description |
|---|---|---|
| POST | `/ai/discover` | Natural language talent search |
| GET | `/ai/status` | AI service health check |

**Data:** Interfaces with Profile Service for freelancer data; no own entities.

---

## Communication Patterns

### Frontend → API Gateway → Microservice
All frontend requests go through the API Gateway. The gateway:
1. Receives the request from the React frontend
2. Routes to the appropriate microservice based on path (no token inspection at the gateway)
3. The target microservice validates the JWT and serves the request (or rejects with 401/403)
4. Returns the response to the frontend

### Inter-Service Communication (OpenFeign)
Microservices communicate with each other using OpenFeign declarative REST clients,
resolved through Eureka for service discovery. Implemented today:
- **Hiring Service → Project Service:** `GET /projects/{id}` to verify a project
  exists and read its owner for creator ownership checks. The caller's JWT is
  forwarded on the Feign call (the Project Service authenticates every request).

Implemented:
- **Project Service → Profile Service:** Fetch the project owner's public
  profile to enrich project reads (`GET /projects`, `GET /projects/{id}`,
  `GET /projects/my`). The caller's JWT is forwarded and the enrichment is
  best-effort — projects are returned unchanged (without `ownerProfile`) when
  the owner has no profile or the Profile Service is unavailable.

Planned (not yet implemented):
- **AI Service → Profile Service:** Fetch freelancer profiles for matching
- **Hiring Service → Project Service:** Update project status on hire

---

## Error Handling

Every microservice translates failures into a consistent `ErrorResponse` body
(`{ timestamp, status, error, message, path }`) through its own
`GlobalExceptionHandler`:

- **400** — validation failures, malformed JSON bodies, bad path variables
- **401** — missing or invalid JWT (and failed login credentials in Auth)
- **403** — authenticated but not authorized (non-owner, wrong role)
- **404** — resource not found
- **409** — duplicate resource or conflicting state
- **503** — downstream service unavailable (Hiring Service → Project Service)
- **500** — unexpected errors (logged server-side; generic message returned)

**Unknown paths.** Spring 6 no longer matches trailing slashes against
controller mappings, so a path like `/projects/` (or any unmatched path) falls
through to the static-resource handler. Every service's `GlobalExceptionHandler`
handles the resulting `NoResourceFoundException` and returns **404** with the
standard body (`"message": "Resource not found"`) instead of a 500.

---

## Security Architecture

```
User Request
    │
    ▼
API Gateway — routes by path only (no token inspection)
    │
    ▼
Microservice — validates the bearer JWT
    │
    ├── Public endpoints (register, login) → served without a token
    │
    └── Protected endpoints → valid JWT required
            │
            ▼
        If valid → principal from token claims (userId, email, role)
        If invalid / missing → 401 Unauthorized
```

- **JWT tokens** are issued by the Auth Service on login
- **Tokens contain:** userId, email, role (CREATOR/FREELANCER)
- **Every microservice** validates the JWT (shared HMAC secret, issuer
  check) before serving protected endpoints; the gateway does not inspect
  tokens and forwards the `Authorization` header unchanged
- **Services** validate the caller's JWT for inter-service communication
  (the Feign client forwards it on outbound calls)
- **Passwords** hashed with BCrypt (never stored in plain text)

---

## Database Strategy

### Option 1: Shared Database (MVP)
Single MySQL database with schema-per-module naming:
- `auth_` tables (users)
- `profile_` tables (profiles)
- `project_` tables (projects)
- `hiring_` tables (applications, reviews)

### Option 2: Database per Service (Post-MVP)
Each service owns its own database schema, promoting true independence.

For the MVP, **Option 1** (shared database) is preferred for simplicity.

---

## Deployment Architecture

```
Docker Compose
    │
    ├── mysql:8.0          (Database)
    ├── service-registry   (Eureka — Port 8761)
    ├── api-gateway        (Gateway — Port 8080)
    ├── auth-service       (Port 8081)
    ├── profile-service    (Port 8082)
    ├── project-service    (Port 8083)
    ├── hiring-service     (Port 8084)
    ├── ai-service         (Port 8085)
    └── frontend           (React — Port 3000)
```

---

## Key Architectural Decisions

| Decision | Rationale |
|---|---|
| **Spring Cloud over monolithic** | Modular development, independent scaling, clear boundaries |
| **Eureka over Consul/Zookeeper** | Native Spring Cloud integration, minimal configuration |
| **Spring Cloud Gateway over Zuul** | Reactive, non-blocking, modern replacement for Zuul |
| **OpenFeign for inter-service calls** | Declarative, Spring-native REST client |
| **JWT over session-based auth** | Stateless, scalable, suitable for microservices |
| **MySQL over NoSQL** | Relational integrity, structured data, ACID compliance |
| **React over Angular** | Lighter weight, faster development, broader ecosystem for UI components |
| **Tailwind CSS over Material UI** | Utility-first, customizable, no heavy component library dependencies |
