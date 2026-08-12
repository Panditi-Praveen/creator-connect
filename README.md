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
Frontend (React + TypeScript)
        │
API Gateway (Spring Cloud Gateway — Port 8080)
        │
┌───────┼───────┬───────┬───────┬───────┐
Auth   Profile Project Hiring  AI     Eureka
:8081  :8082   :8083   :8084  :8085   :8761
        │
   MySQL Database
```

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Backend | Spring Boot, Spring Cloud |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA, Hibernate, MySQL |
| Frontend | React, TypeScript, Tailwind CSS |
| Service Comm | OpenFeign |
| DevOps | Git, Docker, Docker Compose |
| AI | LLM API Integration |

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
├── docs/                   (Documentation)
├── postman/                (API Collections)
├── docker-compose.yml
├── .gitignore
└── README.md
```

## 🔐 Environment Variables

The backend services require two environment variables (no secret defaults are committed):

| Variable | Required | Used by | Description |
|---|---|---|---|
| `APP_JWT_SECRET` | ✅ | auth, profile, project, hiring | HMAC key used to sign (auth) and validate (all) JWTs. Must be **identical** across all four services. |
| `MYSQL_PASSWORD` | ✅ | auth, profile, project, hiring | Password for the MySQL user the services connect with. |
| `MYSQL_USER` | — | auth, profile, project, hiring | MySQL username (defaults to `root`). |
| `MYSQL_URL` | — | auth, profile, project, hiring | JDBC URL (defaults to `jdbc:mysql://localhost:3306/creatorconnect?...`). |
| `MYSQL_ROOT_PASSWORD` | Docker only | `mysql` container | Root password for the MySQL container (docker-compose). |

### Generating a safe JWT secret

Generate a random value of at least 256 bits (32+ bytes) and base64-encode it:

```bash
openssl rand -base64 64
# or, without openssl:
python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(64)).decode())"
```

### Running locally

```bash
export APP_JWT_SECRET="$(openssl rand -base64 64)"
export MYSQL_PASSWORD="your-mysql-password"
mvn -f backend/pom.xml spring-boot:run -pl auth-service
```

### Running with Docker Compose

```bash
cp .env.example .env    # then fill in real values — never commit .env
docker compose up -d
```

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
