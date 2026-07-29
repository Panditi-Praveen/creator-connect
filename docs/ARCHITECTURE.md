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
│         Routing, Authentication Filtering, Load Balancing     │
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

Routes incoming requests to the appropriate microservice based on the request path. Performs authentication token validation at the gateway level before forwarding requests to downstream services.

**Configuration:**
- Port: 8080
- Dependency: `spring-cloud-starter-gateway`, `spring-cloud-starter-netflix-eureka-client`
- Routes defined in `application.yml`:
  - `/api/auth/**` → Auth Service (:8081)
  - `/api/profiles/**` → Profile Service (:8082)
  - `/api/projects/**` → Project Service (:8083)
  - `/api/hiring/**` → Hiring Service (:8084)
  - `/api/ai/**` → AI Service (:8085)

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
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and receive JWT |
| GET | `/api/auth/validate` | Validate JWT token |
| GET | `/api/auth/profile` | Get current user info |

**Data:** User entity (id, email, password hash, role)

### Profile Service (Port 8082)

**Purpose:** Manage creator and freelancer profiles.

**Responsibilities:**
- Creator profile CRUD
- Freelancer profile CRUD
- Skill management
- Portfolio management
- Experience tracking

**API Endpoints:**
| Method | Path | Description |
|---|---|---|
| GET | `/api/profiles/freelancers` | List freelancers (with filters) |
| GET | `/api/profiles/freelancers/{id}` | Get freelancer profile |
| PUT | `/api/profiles/freelancers/{id}` | Update freelancer profile |
| GET | `/api/profiles/creators/{id}` | Get creator profile |
| PUT | `/api/profiles/creators/{id}` | Update creator profile |
| POST | `/api/profiles/skills` | Add skill to freelancer |
| DELETE | `/api/profiles/skills/{id}` | Remove skill |
| POST | `/api/profiles/portfolio` | Add portfolio item |
| DELETE | `/api/profiles/portfolio/{id}` | Remove portfolio item |

**Data:** CreatorProfile, FreelancerProfile, Skill, FreelancerSkill, Portfolio

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
| POST | `/api/projects` | Create project |
| GET | `/api/projects` | List projects (with filters) |
| GET | `/api/projects/{id}` | Get project details |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete/close project |

**Data:** Project entity

### Hiring Service (Port 8084)

**Purpose:** Manage applications, shortlisting, hiring, and reviews.

**Responsibilities:**
- Application submission and management
- Shortlisting workflow
- Hiring workflow
- Reviews and ratings

**API Endpoints:**
| Method | Path | Description |
|---|---|---|
| POST | `/api/hiring/applications` | Apply to project |
| GET | `/api/hiring/applications/project/{id}` | Get applications for project |
| PUT | `/api/hiring/applications/{id}/shortlist` | Shortlist applicant |
| PUT | `/api/hiring/applications/{id}/hire` | Hire applicant |
| POST | `/api/hiring/reviews` | Submit review |
| GET | `/api/hiring/reviews/freelancer/{id}` | Get freelancer reviews |

**Data:** Application, Review entities

### AI Service (Port 8085)

**Purpose:** AI-assisted talent discovery.

**Responsibilities:**
- Accept natural language queries
- Interpret queries via LLM API
- Extract structured search criteria
- Match against real freelancer profiles
- Rank results by relevance

**API Endpoints:**
| Method | Path | Description |
|---|---|---|
| POST | `/api/ai/discover` | Natural language talent search |
| GET | `/api/ai/status` | AI service health check |

**Data:** Interfaces with Profile Service for freelancer data; no own entities.

---

## Communication Patterns

### Frontend → API Gateway → Microservice
All frontend requests go through the API Gateway. The gateway:
1. Receives the request from the React frontend
2. Validates the JWT token (if applicable)
3. Routes to the appropriate microservice based on path
4. Returns the response to the frontend

### Inter-Service Communication (OpenFeign)
Microservices communicate with each other using OpenFeign declarative REST clients:
- **Project Service → Profile Service:** Fetch freelancer details during hiring
- **AI Service → Profile Service:** Fetch freelancer profiles for matching
- **Hiring Service → Project Service:** Update project status on hire

All inter-service calls are authenticated and routed through Eureka for service discovery.

---

## Security Architecture

```
User Request
    │
    ▼
API Gateway
    │
    ├── Public routes (register, login) → pass through
    │
    └── Protected routes → JWT validation
            │
            ▼
        If valid → extract user context, forward to service
        If invalid → return 401 Unauthorized
```

- **JWT tokens** are issued by the Auth Service on login
- **Tokens contain:** userId, email, role (CREATOR/FREELANCER)
- **Gateway** validates tokens before routing to downstream services
- **Services** can also validate tokens for inter-service communication
- **Passwords** hashed with BCrypt (never stored in plain text)

---

## Database Strategy

### Option 1: Shared Database (MVP)
Single MySQL database with schema-per-module naming:
- `auth_` tables (users)
- `profile_` tables (profiles, skills, portfolio)
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
