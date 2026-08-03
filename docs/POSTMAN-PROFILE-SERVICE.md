# Profile Service — API Testing Guide

This document shows how to exercise every Profile Service endpoint with
**Postman** (importable collection included) or plain **curl**.

---

## 1. Prerequisites

1. **MySQL** running (see `docker-compose.yml`, or your local MySQL). The service
   auto-creates the `profiles` table via `spring.jpa.hibernate.ddl-auto: update`.
2. **Service Registry** on port `8761` (optional but recommended):
   ```bash
   cd backend && mvn -pl service-registry spring-boot:run
   ```
3. **Auth Service** on port `8081` — you need it to register + login and get a JWT:
   ```bash
   cd backend && mvn -pl auth-service spring-boot:run
   ```
4. **Profile Service** on port `8082`:
   ```bash
   cd backend && mvn -pl profile-service spring-boot:run
   ```
   (Or run through the **API Gateway** on port `8080` — the routes are already
   configured: `/profile/**` → `lb://profile-service`.)

---

## 2. Get a JWT (via Auth Service)

```bash
# Register a user (once)
curl -s -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Praveen",
    "lastName": "Kumar",
    "email": "praveen@gmail.com",
    "password": "Password@123",
    "role": "CREATOR"
  }'

# Login to obtain a token
curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "praveen@gmail.com", "password": "Password@123"}'
```

Copy the `accessToken` and the `userId` from the login response into the
variables below.

```bash
export TOKEN="<paste accessToken here>"
export USER_ID="<paste userId here>"     # UUID from the login response
```

---

## 3. Endpoints

### 3.1 Create Profile — `POST /profile`

The owning `userId` is taken from the **JWT** — never from the body.

```bash
curl -s -X POST http://localhost:8082/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Praveen",
    "lastName": "Kumar",
    "headline": "Senior Video Editor & Motion Designer",
    "bio": "10+ years crafting brand stories, product demos and social content.",
    "profileImageUrl": "https://example.com/avatars/praveen.jpg",
    "location": "Bengaluru, India",
    "website": "https://praveenportfolio.example",
    "linkedin": "https://www.linkedin.com/in/praveen-kumar",
    "github": "https://github.com/praveenkumar",
    "skills": "Adobe Premiere Pro, After Effects, DaVinci Resolve",
    "experience": 10,
    "availableForHire": true
  }'
```

**Expected:** `201 Created` with an `ApiResponse` envelope carrying the profile.

| Case | Result |
|---|---|
| Valid payload | `201` with profile |
| Missing/invalid JWT | `401` |
| Blank firstName/lastName, bad URL, experience > 100 | `400` |
| Same user creates again | `409` |

### 3.2 Get My Profile — `GET /profile/me`

```bash
curl -s http://localhost:8082/profile/me -H "Authorization: Bearer $TOKEN"
```

**Expected:** `200` with the caller's profile, or `404` if no profile exists.

### 3.3 Get Profile by userId — `GET /profile/{userId}`

```bash
curl -s http://localhost:8082/profile/$USER_ID -H "Authorization: Bearer $TOKEN"
```

**Expected:** `200` with the requested profile; `400` for a malformed UUID;
`404` when the user has no profile. Any authenticated user may view any profile.

### 3.4 Update Profile — `PUT /profile/{userId}`

Partial-update semantics: omitted fields stay unchanged; send `""` for an
optional field to **clear** it. Owner only (`403` otherwise).

```bash
curl -s -X PUT http://localhost:8082/profile/$USER_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "headline": "Lead Video Editor & Creative Director",
    "experience": 12,
    "availableForHire": false,
    "github": ""
  }'
```

**Expected:** `200` with the updated profile; `403` when updating someone
else's profile; `404` when the target has no profile.

### 3.5 Delete Profile — `DELETE /profile/{userId}`

Owner only (`403` otherwise).

```bash
curl -s -X DELETE http://localhost:8082/profile/$USER_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** `200` with `"message": "Profile deleted successfully"` and a
`null` `data` payload; `404` when the profile does not exist.

---

## 4. Postman

Import `postman/ProfileService.postman_collection.json` into Postman, then set
three collection variables:

| Variable | Value |
|---|---|
| `baseUrl` | `http://localhost:8082` (or `http://localhost:8080` via gateway) |
| `jwtToken` | your JWT from `/auth/login` |
| `userId` | your UUID from the login response |

---

## 5. Swagger UI

With the service running, browse to:

- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

These two paths (plus `/actuator/**`) are the only ones that do **not**
require a JWT. Use the **Authorize** button to paste your token and try the
endpoints from the browser.
