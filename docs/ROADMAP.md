# CreatorConnect — 10-Day Development Roadmap

## Overview

This document outlines the day-by-day development plan for the CreatorConnect MVP. Each day builds on the previous day's work, following a logical progression from infrastructure through frontend and deployment.

---

## Day 1 — Documentation & Planning

**Goal:** Establish project foundation, documentation, and infrastructure modules.

**Deliverables:**
- [x] Project documentation (overview, abstract, problem statement, requirements)
- [x] Architecture design documentation
- [x] Module documentation
- [x] Database planning (entities and relationships)
- [x] User workflow documentation
- [x] Business rules documentation
- [x] Technology stack definition
- [x] Repository structure created
- [x] Git initialized
- [x] Service Registry (Eureka — Port 8761) created and configured
- [x] API Gateway (Spring Cloud Gateway — Port 8080) created and configured
- [x] Gateway registered with Eureka
- [x] Both applications running and verified

**Services:** `service-registry`, `api-gateway`

---

## Day 2 — Authentication Service

**Goal:** Implement user registration, login, JWT, and role-based access.

**Deliverables:**
- [x] Auth service module created (Port 8081)
- [x] User entity and database schema
- [x] Registration endpoint (`POST /auth/register`)
- [x] Login endpoint (`POST /auth/login`)
- [x] JWT token generation and validation
- [x] BCrypt password hashing
- [x] Role-based access (CREATOR, FREELANCER)
- [x] Gateway routing to auth service
- [x] API testing with Postman

**Services:** `auth-service`

---

## Day 3 — Profiles & Portfolio Service

**Goal:** Implement creator and freelancer profiles, skills management, and portfolio.

**Deliverables:**
- [x] Profile service module created (Port 8082)
- [x] Profile entity and schema (single unified model for creators and freelancers — the separate CreatorProfile/FreelancerProfile split was consolidated)
- [x] Skills support (stored as a field on the Profile model rather than a separate Skill entity)
- [ ] Portfolio entity and CRUD endpoints (deferred — profiles carry portfolio/link fields instead)
- [x] Profile CRUD endpoints (`POST /profile`, `GET /profile/me`, `GET /profile/{userId}`, `PUT /profile/{userId}`, `DELETE /profile/{userId}`)
- [x] Gateway routing to profile service
- [x] API testing with Postman

**Services:** `profile-service`

---

## Day 4 — Project Service

**Goal:** Implement project posting, browsing, and management.

**Deliverables:**
- [x] Project service module created (Port 8083)
- [x] Project entity and schema
- [x] Project CRUD endpoints
- [x] Project listing with filters
- [x] Project status management
- [x] Gateway routing to project service
- [x] API testing with Postman

**Services:** `project-service`

---

## Day 5 — Applications & Hiring Service

**Goal:** Implement project applications, shortlisting, hiring workflow, and reviews.

**Deliverables:**
- [x] Hiring service module created (Port 8084)
- [x] Application entity and schema
- [x] Application endpoints (apply, view, withdraw)
- [x] Shortlisting / hiring workflow (implemented as a generic creator decision — `PUT /applications/{id}/status` with ACCEPTED or REJECTED; accepting a freelancer automatically moves the project to `IN_PROGRESS` via the Project Service, and reviews require the project to be `COMPLETED`)
- [x] Review entity and schema
- [x] Review endpoints
- [x] Business rules enforced (duplicate prevention, authorization, owner-only reviews)
- [x] Gateway routing to hiring service (via the `/hiring/**` `rewritePath` strip — see ARCHITECTURE.md)
- [x] API testing with Postman

**Services:** `hiring-service`

---

## Day 6 — Service Integration

**Goal:** Integrate services with OpenFeign communication and end-to-end workflow testing.

**Status:** ✅ Complete — the Hiring Service integrates with the Project Service
via OpenFeign (project existence + creator ownership verification, caller's
JWT forwarded, gateway `/hiring/**` prefix rewrite in place), and the Project
Service now enriches project reads with the owner's public profile from the
Profile Service (best-effort — a missing profile or Profile Service outage
never fails a project read).

**Deliverables:**
- [x] OpenFeign clients configured for inter-service communication (`ProjectClient` in the Hiring Service, `ProfileClient` in the Project Service)
- [x] Auth service integration (token validation across services — every service validates the shared JWT)
- [x] Profile-service ↔ Project-service integration (owner profile enrichment on `GET /projects`, `GET /projects/{id}`, `GET /projects/my` — best-effort degradation)
- [x] Hiring-service ↔ Project-service integration (project existence + creator ownership checks)
- [x] End-to-end workflow tested (register → create profile → post project → apply → hire → complete → review)
- [x] Project lifecycle integrated with hiring — `PUT /projects/{id}/status` on the Project Service enforces a forward-only state machine (OPEN → IN_PROGRESS/COMPLETED/CANCELLED, IN_PROGRESS → COMPLETED/CANCELLED; terminal states locked, illegal transitions → 409). Accepting an application moves the project to IN_PROGRESS automatically; reviews require the project to be COMPLETED.
- [x] Error handling and edge cases (404 project not found, 403 not the owner, 409 illegal project transition, 503 Project Service unavailable)
- [x] Postman collection updated (no new endpoints were added in Day 6 — the existing Hiring Service collection covers all APIs)

**Services:** All backend services

---

## Day 7 — React + TypeScript Frontend

**Goal:** Build the frontend application with role-based views.

**Deliverables:**
- [ ] React + TypeScript project scaffolded
- [ ] Tailwind CSS configured
- [ ] React Router for client-side routing
- [ ] Axios HTTP client configured
- [ ] Context API for state management
- [ ] Authentication pages (Login, Register)
- [ ] Creator dashboard
- [ ] Freelancer dashboard
- [ ] Profile pages (view, edit)
- [ ] Portfolio display and management
- [ ] Project listing and creation
- [ ] Application workflow UI
- [ ] Search and filtering UI

**Modules:** `frontend/`

---

## Day 8 — AI-Assisted Talent Discovery

**Goal:** Implement the AI module for natural-language talent discovery.

**Deliverables:**
- [ ] AI service module created (Port 8085)
- [ ] LLM API integration
- [ ] Natural language query endpoint
- [ ] Query interpretation and structured criteria extraction
- [ ] Profile matching against database
- [ ] Relevance ranking logic
- [ ] Graceful degradation (fallback to manual search on AI failure)
- [ ] Frontend AI search UI component
- [ ] API testing with Postman

**Services:** `ai-service`

---

## Day 9 — Testing, Postman, Swagger & Bug Fixing

**Goal:** Comprehensive testing, API documentation, and bug fixing.

**Deliverables:**
- [ ] JUnit 5 unit tests for all services
- [ ] Mockito tests for service layers
- [ ] Integration tests for key workflows
- [ ] Swagger/OpenAPI configuration for all services
- [ ] Postman collection with all endpoints
- [ ] Bug fixes from testing
- [ ] Edge case handling
- [ ] Error response standardization

**Services:** All services

---

## Day 10 — Docker, Deployment Preparation, README & Demo

**Goal:** Containerization, deployment preparation, and final documentation.

**Deliverables:**
- [ ] Dockerfile for each service
- [ ] Docker Compose configuration (all services)
- [ ] `.dockerignore` files
- [ ] Final README with setup instructions
- [ ] Environment variable documentation
- [ ] Demo script preparation
- [ ] Final end-to-end verification
- [ ] Git tag for MVP release

**Services:** All services + Docker

---

## Key Milestones

| Milestone | Day | Description |
|---|---|---|
| Infrastructure Ready | Day 1 | Eureka + Gateway running |
| Users Can Register | Day 2 | Auth service operational |
| Profiles Live | Day 3 | Profiles, skills, portfolio |
| Projects Live | Day 4 | Project CRUD operational |
| Hiring Works | Day 5 | Full hiring workflow |
| System Integrated | Day 6 | All services connected |
| Frontend Ready | Day 7 | UI complete |
| AI Feature Added | Day 8 | AI discovery operational |
| Tested & Documented | Day 9 | Tests + Swagger + Postman |
| Deployed & Demo | Day 10 | Docker + demo |

---

## Scope Management

If behind schedule, prioritize in this order:
1. Authentication (Day 2) — Non-negotiable
2. Profiles + Portfolio (Day 3) — Core value
3. Projects (Day 4) — Core value
4. Applications + Hiring (Day 5) — Core workflow
5. Frontend (Day 7) — User-facing
6. AI (Day 8) — Differentiator
7. Testing (Day 9) — Quality
8. Docker (Day 10) — Deployment
