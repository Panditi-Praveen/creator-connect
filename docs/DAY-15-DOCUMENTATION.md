# CreatorConnect — Day 15: Frontend-Ready API Error Contract

## 1. Day 15 Objective

Make the backend **frontend-ready from an error-contract perspective** and verify the
existing error handling end-to-end.

The `frontend/` directory is empty (the Day 7 React frontend was never built), so no
frontend code was created or modified. Instead this day:

1. Audited the existing backend error handling (auth, profile, project, hiring, gateway).
2. Verified the HTTP error contract (`400 / 401 / 403 / 404 / 409 / 500 / 503`).
3. Verified the project and hiring/application error scenarios.
4. Documented exactly how a future React frontend must interpret backend errors.
5. Ran the full backend test suite and performed live API verification.
6. Ran a security review of the error/identity surfaces.

**No backend business logic, JWT, Eureka, gateway, Docker, or environment/security
configuration was modified.**

---

## 2. Existing Error Architecture

Every business service (auth, profile, project, hiring) follows the same pattern:

| Piece | Where | Role |
|---|---|---|
| `ErrorResponse` | `dto/response/ErrorResponse.java` (one per service, identical shape) | Standard error envelope for **every** failed request |
| `GlobalExceptionHandler` | `exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`) | Translates domain exceptions → HTTP status + meaningful message |
| `JwtAuthenticationEntryPoint` | `security/JwtAuthenticationEntryPoint.java` | Turns missing/invalid JWT on a protected route into a clean `401` JSON body |
| `JwtAuthenticationFilter` + `SecurityBeansConfig` | `security/` + `config/` | Validates the bearer token; every route is authenticated except `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui*` |
| `ApiResponse<T>` | `dto/response/ApiResponse.java` | Success envelope: `{ timestamp, status, message, data, path }` |

The API Gateway (`api-gateway`, Spring Cloud Gateway MVC) is a **transparent proxy**: it
has **no error handling of its own**, so downstream `ErrorResponse` bodies are expected to
pass through unchanged. (See §9/§11 for the current environment caveat.)

Domain exceptions per service and their status mapping:

- **project-service**: `ProjectNotFoundException` → 404, `ProjectAccessDeniedException` → 403,
  `ProjectStatusConflictException` → 409, `ProjectValidationException` → 400,
  `DataIntegrityViolationException` → 409, malformed body / type mismatch → 400,
  unknown path → 404, catch-all → 500.
- **hiring-service**: `ApplicationNotFoundException`/`ProjectNotFoundException` → 404,
  `ApplicationAccessDeniedException`/`ReviewAccessDeniedException` → 403,
  `DuplicateApplicationException`/`DuplicateReviewException`/`ApplicationStatusConflictException` → 409,
  `ApplicationValidationException`/`ReviewValidationException` → 400,
  `FeignException` (Project Service unreachable / 5xx) → **503**, unknown path → 404, catch-all → 500.
- **auth-service**: `EmailAlreadyExistsException` → 409, `UserNotFoundException`/`InvalidCredentialsException` → 401 (login),
  Bean Validation → 400, malformed body → 400, unknown path → 404, catch-all → 500.
- **profile-service**: `ProfileNotFoundException` → 404, `ProfileAccessDeniedException` → 403,
  `ProfileAlreadyExistsException` → 409, validation → 400, unknown path → 404, catch-all → 500.

---

## 3. ErrorResponse Structure

All four services produce the **exact same JSON shape** (verified live — see §9):

```json
{
  "timestamp": "2026-08-14T15:34:01.4337695",
  "status": 409,
  "error": "Conflict",
  "message": "Project status cannot change from IN_PROGRESS to OPEN",
  "path": "/projects/ffa5d95b-7fe4-4c81-ac1b-eb7ee7ebdbb6/status"
}
```

| Field | Type | Meaning |
|---|---|---|
| `timestamp` | string (ISO local date-time) | When the error occurred |
| `status` | int | HTTP status code — use for programmatic handling |
| `error` | string | HTTP reason phrase (`Bad Request`, `Unauthorized`, …) |
| `message` | string | **Human-readable message — display this to the user** |
| `path` | string | Request URI that failed |

`message` is meaningful in every handled case (business-rule text, field-level validation
like `title Title is required`, or a descriptive generic for 500/503). Clients should rely
on `status` for logic and `message` for display.

---

## 4. HTTP Status Contract

| Status | Meaning | When the backend returns it | Example `message` (real) |
|---|---|---|---|
| **400** | Validation / business-rule error | Bean Validation failure, malformed body, bad path variable, business rule Bean Validation cannot express | `A new project must start in OPEN status (received: COMPLETED)`; `title Title is required` |
| **401** | Authentication required / token invalid | Missing, malformed, expired, or wrongly-signed JWT on a protected route; wrong credentials at login | `Authentication required` |
| **403** | Authenticated but not authorized | Non-owner modifying a project/application/review; wrong role (e.g. non-freelancer applying) | `You do not have permission to modify this project`; `Only freelancers can apply to projects` |
| **404** | Resource not found | Project/application/profile not found; unknown or trailing-slash path | `Project not found: 00000000-0000-0000-0000-000000000000`; `Resource not found` |
| **405** | Method not allowed | HTTP method not supported by the route (project/hiring/profile services) | `Request method 'GET' is not supported` |
| **409** | Duplicate / conflict / state conflict | Duplicate email/profile/application/review; illegal project status transition; application no longer pending | `Project status cannot change from IN_PROGRESS to OPEN`; `You have already applied to this project` |
| **500** | Unexpected backend error | Catch-all — the real cause is logged server-side, never leaked | `An unexpected error occurred` |
| **503** | Dependent service unavailable | Hiring/Project Service Feign call fails (connection, timeout, downstream 5xx) | `Project Service is unavailable, please try again later` |

---

## 5. Project API Error Scenarios

Base path `/projects` (all endpoints require a JWT; identity always from the token).

| Scenario | Endpoint | Status | Backend `message` |
|---|---|---|---|
| Create project — valid payload | `POST /projects` | 201 | `Project created successfully` (success envelope) |
| Create with terminal/illegal status (`IN_PROGRESS`, `COMPLETED`, `CANCELLED`) | `POST /projects` | 400 | `A new project must start in OPEN status (received: COMPLETED)` |
| Create with missing/invalid required fields | `POST /projects` | 400 | e.g. `title Title is required`, `Budget is required`, `Application deadline must be in the future` |
| No / invalid JWT | any | 401 | `Authentication required` |
| Update/delete/status-change on someone else's project | `PUT/DELETE /projects/{id}`, `PUT /projects/{id}/status` | 403 | `You do not have permission to modify this project` |
| Project does not exist | `GET/PUT/DELETE /projects/{id}` | 404 | `Project not found: <uuid>` |
| Illegal lifecycle transition (e.g. `IN_PROGRESS → OPEN`, or any change on `COMPLETED`/`CANCELLED`) | `PUT /projects/{id}/status` | 409 | `Project status cannot change from IN_PROGRESS to OPEN` |
| Re-applying the current status | `PUT /projects/{id}/status` | 200 (idempotent no-op) | `Project status updated successfully` |
| Unknown / trailing-slash path | e.g. `GET /projects/` | 404 | `Resource not found` |

State-machine rules (unchanged this day): `OPEN → IN_PROGRESS/COMPLETED/CANCELLED`,
`IN_PROGRESS → COMPLETED/CANCELLED`, terminal states locked.

---

## 6. Hiring / Application API Error Scenarios

Base path `/applications` (through the gateway: `/hiring/applications`), `/reviews`.

| Scenario | Endpoint | Status | Backend `message` |
|---|---|---|---|
| Apply with a valid payload | `POST /applications` | 201 | `Application submitted successfully` (success envelope) |
| Apply twice to the same project | `POST /applications` | 409 | `You have already applied to this project` |
| Apply to a terminal project (`COMPLETED`/`CANCELLED`) | `POST /applications` | 400 | `Cannot apply to a COMPLETED project` |
| Apply with missing/invalid fields | `POST /applications` | 400 | e.g. `Proposal is required`, `Project id is required` |
| Non-freelancer tries to apply | `POST /applications` | 403 | `Only freelancers can apply to projects` |
| Project does not exist in Project Service | `POST /applications` | 404 | `Project not found: <uuid>` |
| Project Service unreachable (Feign failure) | `POST /applications` and owner-dependent reads | 503 | `Project Service is unavailable, please try again later` |
| No / invalid JWT | any | 401 | `Authentication required` |
| Non-creator decides on an application | `PUT /applications/{id}/status` | 403 | `Only creators can update application status` |
| Decide on a non-pending application | `PUT /applications/{id}/status` | 409 | `Only pending applications can be decided on (current status: …)` |
| Invalid decision value | `PUT /applications/{id}/status` | 400 | `Status must be ACCEPTED or REJECTED` |
| Withdraw someone else's application | `DELETE /applications/{id}` | 403 | `You can only withdraw your own applications` |
| Withdraw a non-pending application | `DELETE /applications/{id}` | 409 | `Only pending applications can be withdrawn (current status: …)` |
| Review a project that is not `COMPLETED` | `POST /reviews` | 400 | `Only completed projects can be reviewed (current status: …)` |
| Duplicate review for the same (project, freelancer) | `POST /reviews` | 409 | `This freelancer has already been reviewed on this project` |

The rule that **terminal projects cannot receive new applications** is preserved and
unchanged.

---

## 7. Frontend Integration Guidance (for the future React app)

The backend contract is stable and identical across services. A future frontend should:

1. **Use one centralized HTTP client/interceptor** (e.g. Axios) that:
   - attaches `Authorization: Bearer <token>` to every request;
   - on a non-2xx response, reads the body and extracts the **`message` field**;
   - surfaces that message to the user via the existing toast/alert mechanism.
2. **Prefer the backend message over any hardcoded fallback.** The backend already sends
   meaningful, user-friendly text for every handled case. A fallback is only used when the
   body has no usable `message` (e.g. a network failure with no response body).
3. **Interpret by `status`, display by `message`:**

| Status | Frontend action |
|---|---|
| 400 | Show the backend `message` (validation / business rule) — e.g. keep the form open, highlight the field if the message names one |
| 401 | Session expired / not authenticated → drop the stored token, redirect to login |
| 403 | Authenticated but not permitted → show the backend `message`, disable the action |
| 404 | Resource gone → show the backend `message` |
| 409 | Duplicate / state conflict → show the backend `message` (e.g. "You have already applied to this project") |
| 500 | Show `An unexpected error occurred` (as sent by the backend) |
| 503 | Downstream service unavailable → show the backend `message` and offer retry with backoff |

Suggested frontend-only fallbacks (only when the backend provides **no** message):
`400` → "Please check the submitted information." · `401` → "Your session has expired. Please log in again." ·
`403` → "You do not have permission to perform this action." · `404` → "The requested resource was not found." ·
`409` → "This action conflicts with an existing record." · `500` → "Something went wrong on the server. Please try again."

4. **Call through the API Gateway (port 8080)** — the single entry point. Base paths:
   `/auth/**`, `/profile/**`, `/projects/**`, `/hiring/applications/**` (the gateway strips
   the `/hiring` prefix), `/hiring/reviews/**`, `/ai/**`.
5. Do not hardcode `userId`/`freelancerId`/`creatorId` in request bodies — the backend
   derives identity exclusively from the JWT.

---

## 8. Testing Performed

Full backend build via `mvn -f backend/pom.xml test` — **223 tests, 0 failures, 0 errors, 0 skipped**:

| Module | Tests |
|---|---|
| api-gateway | 8 |
| auth-service | 14 |
| profile-service | 17 |
| project-service | 88 |
| hiring-service | 96 |
| **Total** | **223** |

Coverage highlights (asserted statuses in project + hiring suites): 400 (×22), 200 (×15),
404 (×8), 403 (×7), 409 (×5), 401 (×3), 201 (×3), 503 (×1). Tests assert both the HTTP
status and the `ErrorResponse` shape (`$.status`, `$.message`, `$.path`), e.g.
`Project Service is unavailable, please try again later` for the Feign 503 path and
`Authentication required` for the JWT entry point.

No tests were deleted or weakened. No business rules were changed to make tests pass.

---

## 9. Live API Verification

**Round 1** was performed directly against the running services on `localhost` while the
stale-Eureka-IP issue (§11) was active — it proved the per-service error contract but the
gateway and Feign paths failed.

**Round 2 (post-restart)** was performed **end-to-end through the API Gateway** (`localhost:8080`)
after the six services were restarted and re-registered with the current LAN IP. Every check
below passed **through the gateway**, confirming the gateway passes downstream
`ErrorResponse` bodies through unchanged:

| Scenario | Call (via gateway) | Result (exact) |
|---|---|---|
| **Success** — register | `POST /auth/register` (new user) | **201** `User registered successfully` |
| **Success** — create project | `POST /projects` (creator JWT) | **201** `Project created successfully` |
| **Success** — browse feed (Feign profile enrichment) | `GET /projects` (creator JWT) | **200** `Projects retrieved successfully` (no hang) |
| **400** — terminal status on create | `POST /projects` with `status: COMPLETED` | **400** `A new project must start in OPEN status (received: COMPLETED)` |
| **400** — missing required field | `POST /projects` without `title` | **400** `title Title is required` |
| **400** — apply to terminal project | `POST /hiring/applications` on a `COMPLETED` project | **400** `Cannot apply to a COMPLETED project` |
| **401** — no token | `GET /projects` | **401** `Authentication required` (proper `ErrorResponse` body) |
| **403** — non-owner update | `PUT /projects/{id}` (second creator) | **403** `You do not have permission to modify this project` |
| **404** — nonexistent project | `GET /projects/00000000-…` | **404** `Project not found: 00000000-0000-0000-0000-000000000000` |
| **409** — duplicate application | `POST /hiring/applications` twice with same freelancer+project | **409** `You have already applied to this project` |
| **409** — illegal state transition | `PUT /projects/{id}/status` `IN_PROGRESS → OPEN` | **409** `Project status cannot change from IN_PROGRESS to OPEN` |
| **503** — genuine Project Service outage | `POST /hiring/applications` while project-service was stopped | **503** `Project Service is unavailable, please try again later` (≈2 s, fast-fail) |

**Still not live-verified** (documented, not claimed):
- **500 catch-all** — deliberately not triggered live (no safe, non-destructive trigger);
  handler is trivial and code-reviewed.

Verification created test users `day15.verify.creator@example.com`,
`day15.verify.creator2@example.com`, `day15.verify.freelancer@example.com`,
`day15.gw.creator@example.com` and `day15.gw.freelancer@example.com` plus temporary
projects/applications, which were deleted afterwards (no user-deletion endpoint exists, so
the test users remain in the local database).

---

## 10. Security Verification

- **JWT still required** on all protected endpoints (only `/actuator/**`, `/v3/api-docs/**`,
  `/swagger-ui*` are public) — verified live via 401 responses.
- **No identity spoofing vector**: `ProjectRequest` has no `userId`; `ApplicationRequest` has
  no `freelancerId`/`status`; ownership is always derived from the authenticated JWT principal
  (verified in controllers/services and live via the 403 non-owner test).
- **No secrets hardcoded**: `application.yml` files reference env vars only
  (`${APP_JWT_SECRET}`, `${MYSQL_PASSWORD}`); the only base64 constants in the repo are
  explicit **test fixtures** in `JwtServiceTest` (placeholder "change-me" keys).
- **`.env` remains ignored/untracked** — not in `git ls-files`, not in `git status`.
- No JWT secret, password, token, or private credential was added to Git, logs, or docs.
- **JWT, Eureka, gateway, Docker, and environment/security configuration were NOT modified.**

---

## 11. Remaining Issues

1. **Stale Eureka IP registration — RESOLVED.** The original stack had registered the LAN IP
   `192.168.0.8` while the machine's current IP was `192.168.0.6` (DHCP change after startup),
   which made every gateway request return 500 and broke Feign. The six Java services were
   restarted (no code change) and now register `192.168.0.6` in Eureka; the gateway and Feign
   paths are verified working in §9. **Watch for recurrence**: if the machine's IP changes
   again while the services run, restart them to re-register.
2. **500 path** — only code-reviewed; no live or automated test exercises the catch-all.
3. **Test users** from §9 remain in the local database (no user-deletion endpoint).
4. `auth-service` lacks the `405`/type-mismatch handlers the other services have — cosmetic
   (its only endpoints are `POST` JSON), not a functional gap.
5. The gateway has no error handler of its own — intentional (transparent proxy) and verified
   to pass downstream `ErrorResponse` bodies through unchanged; if gateway-level failures
   (e.g. no matching route) ever need a custom body, that would be a future enhancement.

---

## 12. Next Roadmap Step

**Day 7 — React + TypeScript Frontend** (unbuilt). The backend now exposes a documented,
stable error contract (§3–§7) and the full request path (gateway + Feign) is verified
healthy (§9). The frontend should be built with a **single centralized HTTP interceptor**
that attaches the JWT, extracts `ErrorResponse.message` on any non-2xx response, and displays
the backend's message — with per-status fallbacks only when no message is provided.
