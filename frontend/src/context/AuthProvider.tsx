import { useEffect, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { login as loginRequest, register as registerRequest } from '../api/auth'
import { UNAUTHORIZED_EVENT } from '../api/client'
import type { AuthUser, LoginRequest, RegisterRequest } from '../types/api'
import { clearSession, getStoredUser, setSession } from '../utils/storage'
import { AuthContext } from './authContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [user, setUser] = useState<AuthUser | null>(() => getStoredUser())

  // Any protected request answered 401 -> the JWT is invalid/expired: drop
  // the stored session and return to the login page.
  useEffect(() => {
    const onUnauthorized = () => {
      setUser(null)
      if (window.location.pathname !== '/login') {
        navigate('/login', { replace: true })
      }
    }
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [navigate])

  const login = async (payload: LoginRequest) => {
    const result = await loginRequest(payload)
    const authUser: AuthUser = {
      userId: result.userId,
      email: result.email,
      role: result.role,
    }
    setSession(result.accessToken, authUser)
    setUser(authUser)
    return result
  }

  const register = (payload: RegisterRequest) => registerRequest(payload)

  const logout = () => {
    clearSession()
    setUser(null)
    navigate('/login', { replace: true })
  }

  return (
    <AuthContext.Provider
      value={{ user, isAuthenticated: user !== null, login, register, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}
