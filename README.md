# CreatorConnect

**AI-Assisted Creative Talent Discovery & Collaboration Platform**

CreatorConnect is a Java Full Stack, microservices-based platform that connects content creators with creative professionals — video editors, photographers, videographers, graphic designers, writers, and social-media managers.

## 🎯 The Problem

Content creators struggle to find, evaluate, hire, and manage creative talent. The current process involves scrolling Instagram, sending DMs, checking scattered portfolio links, and juggling multiple conversations — with no centralized tool.

## 🚀 The Solution

CreatorConnect provides a single, centralized platform that streamlines the entire talent lifecycle:

- **Talent Discovery** — Search and filter freelancers by profession, skills, experience, location, and more
- **Portfolio-First Browsing** — View rich portfolios within the platform
- **Project Management** — Post projects, receive applications, shortlist, and hire
- **AI-Assisted Discovery** — Describe requirements in natural language and get ranked recommendations from real freelancer profiles
- **Reviews & Reputation** — Build community trust through ratings and reviews

## 🏗️ Architecture

```
Frontend (React + TypeScript — Vite dev server on :5173)
        │  (Vite proxy /auth,/profile,/projects,/hiring,/ai)
        ▼
API Gateway (Spring Cloud Gateway — Port 8080)  ← single entry point
        │
┌───────┼───────┬───────┬───────┬───────┐
Auth   Profile Project Hiring  AI     Eureka
:8081  :8082   :8083   :8084  :8085   :8761
        │
   MySQL Database
```

Every client request goes **only** through the API Gateway at `http://localhost:8080`.
The gateway discovers services via Eureka and forwards the `Authorization` header
unchanged; each microservice validates the JWT itself. See
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full design.

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Backend | Spring Boot, Spring Cloud |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA, Hibernate, MySQL |
| Frontend | React, TypeScript, Vite, React Router, Axios |
| Styling | Plain CSS (custom design system in `frontend/src/index.css`) |
| Service Comm | OpenFeign (service-to-service), Spring Cloud Gateway |
| AI | OpenAI Chat Completions API (optional — see Known Limitations) |
| DevOps | Git, Docker, Docker Compose |
| Testing | JUnit (backend), Postman + Newman (E2E), oxlint + `tsc` (frontend) |

## 📁 Repository Structure

```
creator-connect/
├── backend/
│   ├── service-registry/   (Eureka — Port 8761)
│   ├── api-gateway/        (Spring Cloud Gateway — Port 8080)
│   ├── auth-service/       (Authentication — Port 8081)
│   ├── profile-service/    (Profiles & Portfolio — Port 8082)
│   ├── project-service/    (Projects — Port 8083)
│   ├── hiring-service/     (Hiring & Reviews — Port 8084)
│   └── ai-service/         (AI Discovery — Port 8085)
├── frontend/               (React + TypeScript)
├── docs/                   (Architecture, planning, interview prep)
├── postman/                (API collections — incl. E2E suite)
├── docker-compose.yml
├── .env.example            (environment template — never commit .env)
└── README.md
```

## ⚡ Quick Start (Docker Compose — recommended)

**Prerequisites:** Docker with Compose v2, and a shell where you can export env vars.

```bash
cp .env.example .env        # then fill in real values — never commit .env
docker compose up -d --build
```

Wait for all containers to become healthy (first build takes a few minutes):

```bash
docker compose ps           # expect 8 containers: all "healthy"
```

Open the app: **http://localhost:8080** (API Gateway) — or run the frontend dev
server for the full UI (see Frontend Setup below).

## 🔐 Environment Variables

Copy `.env.example` to `.env` and fill in real values. **No real secrets are
committed** — the repository only tracks the placeholder template. Secrets are
never hardcoded in Dockerfiles, `docker-compose.yml`, or `application.yml`.

| Variable | Required | Used by | Description |
|---|---|---|---|
| `APP_JWT_SECRET` | ✅ | auth, profile, project, hiring, ai | HMAC key used to sign (auth) and validate (all) JWTs. Must be **identical across all five services**. |
| `MYSQL_PASSWORD` | ✅ | auth, profile, project, hiring | Password for the MySQL user the services connect with. |
| `MYSQL_ROOT_PASSWORD` | ✅ (Docker) | `mysql` container | Root password for the MySQL container. |
| `MYSQL_USER` | — | auth, profile, project, hiring | MySQL username (defaults to `root`). |
| `MYSQL_URL` | — | auth, profile, project, hiring | JDBC URL (defaults to `jdbc:mysql://localhost:3306/creatorconnect?...`). |
| `OPENAI_API_KEY` | — | ai-service | OpenAI key for AI discovery. **Optional** — when missing, `/ai/discover` returns a clean 503 configuration error (documented contract). |
| `OPENAI_MODEL` | — | ai-service | LLM model (default `gpt-4o-mini`). |
| `OPENAI_BASE_URL` | — | ai-service | LLM endpoint (default OpenAI). |

### Generating a safe JWT secret

Generate a random value of at least 256 bits (32+ bytes) and base64-encode it:

```bash
openssl rand -base64 64
# or, without openssl:
python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(64)).decode())"
```

## 🖥️ Backend Setup (local development, no Docker)

**Prerequisites:** JDK 25, Maven 3.9+, a running MySQL on port 3306 (or adjust `MYSQL_URL`).

```bash
# From the repo root — export the same secrets your shell uses
export APP_JWT_SECRET="$(openssl rand -base64 64)"
export MYSQL_PASSWORD="your-mysql-password"

# Build everything
mvn -f backend/pom.xml -DskipTests package

# Run one service (Eureka first, then the rest):
mvn -f backend/pom.xml spring-boot:run -pl service-registry
mvn -f backend/pom.xml spring-boot:run -pl api-gateway
mvn -f backend/pom.xml spring-boot:run -pl auth-service
# ... profile-service, project-service, hiring-service, ai-service
```

Each service registers with Eureka on startup; the gateway picks them up
automatically. Verify discovery at **http://localhost:8761**.

## ⚛️ Frontend Setup

**Prerequisites:** Node 20+ (npm).

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```

The Vite dev server proxies `/auth`, `/profile`, `/projects`, `/hiring`, and
`/ai` to the API Gateway at `http://localhost:8080` (see `vite.config.ts`), so
the browser never hits CORS. For builds served without the dev proxy, set
`VITE_API_BASE_URL` to the full gateway origin (e.g. `http://localhost:8080`).

See [`frontend/README.md`](frontend/README.md) for details.

## 🚪 API Gateway & Service Ports

| Service | Port | Notes |
|---|---|---|
| API Gateway | **8080** | **Single entry point for all client requests** |
| Auth Service | 8081 | `POST /auth/register`, `POST /auth/login` |
| Profile Service | 8082 | `/profile/**` |
| Project Service | 8083 | `/projects/**` |
| Hiring Service | 8084 | `/hiring/**` (gateway rewrites to `/applications`, `/reviews`) |
| AI Service | 8085 | `/ai/status`, `/ai/discover` |
| Eureka Registry | 8761 | Service discovery dashboard |
| MySQL | 3307 (host) → 3306 (container) | Docker mapping; local dev uses 3306 |

**Gateway routes (all load-balanced via Eureka):** `/auth/**`, `/profile/**`,
`/projects/**`, `/hiring/**` (prefix rewritten), `/ai/**`.

## 🔑 Authentication Flow

1. **Register** — `POST /auth/register` with `{firstName, lastName, email, password, role}` (`CREATOR` or `FREELANCER`) → `201`.
2. **Login** — `POST /auth/login` → `200` with `data.accessToken` (JWT, valid 24h) plus `data.userId` / `data.role` / `data.email`. The JWT is the only credential the client holds.
3. **Authorize** — every protected request sends `Authorization: Bearer <JWT>`. Each microservice validates the token (shared `APP_JWT_SECRET`, issuer `creatorconnect-auth-service`) before processing.
4. **Logout** — client-side: the frontend clears the stored session; JWTs are stateless and expire after 24h.

## 📮 Postman Collection

- **E2E suite:** `postman/CreatorConnect-E2E.postman_collection.json` — 42 requests across Auth / Profile / Project / Hiring / AI folders, covering the full workflow (register → login → profile → projects → applications → reviews → AI discovery) plus negative cases (401/400/409/502). JWT is captured from login and reused automatically via collection variables.
- **Environment:** `postman/CreatorConnect-E2E.postman_environment.json` (carries only `baseUrl` = `http://localhost:8080`).
- Legacy collections for individual services live in `postman/`.

Run the E2E suite from the CLI:

```bash
npx newman run postman/CreatorConnect-E2E.postman_collection.json \
  -e postman/CreatorConnect-E2E.postman_environment.json
```

## 🧪 Testing & Validation

| Check | Command | Status |
|---|---|---|
| Backend unit/integration tests | `mvn -f backend/pom.xml test` | 249 tests, 0 failures |
| Backend build | `mvn -f backend/pom.xml package` | PASS |
| Frontend type-check + build | `cd frontend && npm run build` | PASS |
| Frontend lint | `cd frontend && npm run lint` | 0 warnings |
| Compose validation | `docker compose config -q` | VALID |
| API E2E | Postman suite via Newman (above) | 103 assertions, 0 failures |
| Browser E2E | Full journey through the Vite proxy | PASS |

## ⚠️ Known Limitations

- **OpenAI API quota** — the AI Service calls the OpenAI Chat Completions API
  for natural-language talent ranking. When the configured key is missing or its
  quota is exhausted, `POST /ai/discover` returns **502** (`AI service is
  temporarily unavailable`) per the documented error contract — the frontend
  shows a friendly message. This is an **external credential limitation, not an
  application bug**; `GET /ai/status` keeps working. Restore full AI ranking by
  setting a valid `OPENAI_API_KEY` in `.env` and running
  `docker compose up -d ai-service`.

## 📅 10-Day Roadmap

| Day | Focus |
|---|---|
| Day 1 | Documentation & Planning |
| Day 2 | Authentication Service |
| Day 3 | Profiles & Portfolio Service |
| Day 4 | Project Service |
| Day 5 | Applications & Hiring Service |
| Day 6 | Service Integration |
| Day 7 | React + TypeScript Frontend |
| Day 8 | AI-Assisted Talent Discovery |
| Day 9 | Testing, Postman, Swagger & Bug Fixing |
| Day 10 | Docker, Deployment, README & Demo |

## 👥 Target Users

- **Creators** — Content producers (YouTubers, Instagrammers, podcasters, etc.)
- **Freelancers** — Creative professionals (video editors, photographers, designers, writers, etc.)

---
*Built with Java 25, Spring Boot, Spring Cloud, React & TypeScript*
