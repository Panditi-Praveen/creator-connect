/**
 * Types mirroring the existing backend contract 1:1 (see docs/DAY-15-DOCUMENTATION.md).
 *
 * Success envelope: { timestamp, status, message, data, path }  (ApiResponse)
 * Error envelope:   { timestamp, status, error, message, path } (ErrorResponse)
 */

export interface ApiResponse<T> {
  timestamp: string
  status: number
  message: string
  data: T
  path: string
}

export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}

export type Role = 'ADMIN' | 'CREATOR' | 'FREELANCER'

export interface LoginRequest {
  email: string
  password: string
}

/** Payload of POST /auth/login -> ApiResponse<LoginResponse> */
export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  userId: string
  email: string
  role: Role
}

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
  phone?: string
  role: Role
}

/** Payload of POST /auth/register -> ApiResponse<RegisterResponse> */
export interface RegisterResponse {
  id: string
  firstName: string
  lastName: string
  email: string
  phone: string | null
  role: Role
  provider: string
  createdAt: string
}

/** Safe user projection persisted client-side next to the JWT. */
export interface AuthUser {
  userId: string
  email: string
  role: Role
}
