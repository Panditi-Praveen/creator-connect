# CreatorConnect Frontend

React + TypeScript frontend for CreatorConnect. Consumes the existing Spring
Boot microservices **only through the API Gateway** (port 8080) — never by
calling individual services directly.

## Stack

- React 19 + TypeScript (Vite)
- React Router 7
- Axios

## Getting started

```bash
npm install
npm run dev
```

The Vite dev server proxies `/auth`, `/profile`, `/projects`, `/hiring` and
`/ai` to the API Gateway at `http://localhost:8080` (see `vite.config.ts`), so
the browser never hits CORS and no backend CORS configuration is required.

## Environment

| Variable | Purpose | Default |
|---|---|---|
| `VITE_API_BASE_URL` | Full gateway origin for builds served without the dev proxy | empty (dev proxy) |

Frontend environment variables are **not secret** — never store JWT secrets,
passwords, or credentials in them.

## Scripts

- `npm run dev` — start the Vite dev server
- `npm run build` — type-check (`tsc -b`) + production build
- `npm run lint` — oxlint
- `npm run preview` — preview the production build

## Structure

```
src/
├── api/          Centralized Axios client + per-domain API modules
├── components/   Shared components (ProtectedRoute, PagePlaceholder)
├── context/      AuthProvider (JWT session state)
├── hooks/        useAuth
├── pages/        Route pages (Login, Register, Dashboard, …)
├── types/        Backend contract types (ApiResponse, ErrorResponse, DTOs)
└── utils/        Storage helpers (JWT + user session persistence)
```
