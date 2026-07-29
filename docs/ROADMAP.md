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
- [ ] Auth service module created (Port 8081)
- [ ] User entity and database schema
- [ ] Registration endpoint (`POST /api/auth/register`)
- [ ] Login endpoint (`POST /api/auth/login`)
- [ ] JWT token generation and validation
- [ ] BCrypt password hashing
- [ ] Role-based access (CREATOR, FREELANCER)
- [ ] Gateway routing to auth service
- [ ] API testing with Postman

**Services:** `auth-service`

---

## Day 3 — Profiles & Portfolio Service

**Goal:** Implement creator and freelancer profiles, skills management, and portfolio.

**Deliverables:**
- [ ] Profile service module created (Port 8082)
- [ ] CreatorProfile and FreelancerProfile entities
- [ ] Skill entity and FreelancerSkill mapping
- [ ] Portfolio entity
- [ ] Profile CRUD endpoints
- [ ] Skill management endpoints
- [ ] Portfolio CRUD endpoints
- [ ] Gateway routing to profile service
- [ ] API testing with Postman

**Services:** `profile-service`

---

## Day 4 — Project Service

**Goal:** Implement project posting, browsing, and management.

**Deliverables:**
- [ ] Project service module created (Port 8083)
- [ ] Project entity and schema
- [ ] Project CRUD endpoints
- [ ] Project listing with filters
- [ ] Project status management
- [ ] Gateway routing to project service
- [ ] API testing with Postman

**Services:** `project-service`

---

## Day 5 — Applications & Hiring Service

**Goal:** Implement project applications, shortlisting, hiring workflow, and reviews.

**Deliverables:**
- [ ] Hiring service module created (Port 8084)
- [ ] Application entity and schema
- [ ] Application endpoints (apply, view, withdraw)
- [ ] Shortlisting endpoints
- [ ] Hiring endpoints
- [ ] Review entity and schema
- [ ] Review endpoints
- [ ] Business rules enforced (duplicate prevention, authorization)
- [ ] Gateway routing to hiring service
- [ ] API testing with Postman

**Services:** `hiring-service`

---

## Day 6 — Service Integration

**Goal:** Integrate all services with OpenFeign communication and end-to-end workflow testing.

**Deliverables:**
- [ ] OpenFeign clients configured for inter-service communication
- [ ] Auth service integration (token validation across services)
- [ ] Profile-service ↔ Project-service integration
- [ ] Hiring-service ↔ Project-service integration
- [ ] End-to-end workflow tested (register → create profile → post project → apply → hire → complete → review)
- [ ] Error handling and edge cases
- [ ] Postman collection updated

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
