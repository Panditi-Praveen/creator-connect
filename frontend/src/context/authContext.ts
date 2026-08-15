import { createContext } from 'react'
import type {
  AuthUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from '../types/api'

export interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  login: (payload: LoginRequest) => Promise<LoginResponse>
  register: (payload: RegisterRequest) => Promise<RegisterResponse>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
