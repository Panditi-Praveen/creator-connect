import { post } from './client'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from '../types/api'

/**
 * Auth API surface — matches the existing auth-service contract exactly
 * (POST /auth/register, POST /auth/login). No endpoint names were invented;
 * these mirror AuthController in backend/auth-service.
 */

export function login(payload: LoginRequest): Promise<LoginResponse> {
  return post<LoginResponse>('/auth/login', payload)
}

export function register(payload: RegisterRequest): Promise<RegisterResponse> {
  return post<RegisterResponse>('/auth/register', payload)
}
