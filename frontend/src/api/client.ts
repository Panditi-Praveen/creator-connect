import axios, {
  type AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from 'axios'
import type { ApiResponse, ErrorResponse } from '../types/api'
import { clearSession, getAccessToken } from '../utils/storage'

/**
 * Dispatched on the window when a protected request is answered 401 (expired
 * or invalid JWT). AuthContext listens and redirects to /login.
 */
export const UNAUTHORIZED_EVENT = 'auth:unauthorized'

/**
 * Normalized error carrying the backend ErrorResponse contract. `message` is
 * the backend-provided, user-facing text — display it as-is whenever present.
 */
export class ApiError extends Error {
  readonly status: number
  readonly error: string
  readonly path: string
  readonly timestamp: string
  readonly retryAfter?: number

  constructor(body: ErrorResponse) {
    super(body.message)
    this.name = 'ApiError'
    this.status = body.status
    this.error = body.error
    this.path = body.path
    this.timestamp = body.timestamp
    this.retryAfter = body.retryAfter
  }
}

/**
 * Frontend API base URL. Empty by default: the Vite dev server proxies the
 * gateway base paths (see vite.config.ts). Set VITE_API_BASE_URL to the full
 * gateway origin when serving a build without the dev proxy.
 */
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? ''

const http: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
})

// Attach the stored JWT to every request as `Authorization: Bearer <token>`.
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Success: unwrap the backend envelope ({ timestamp, status, message, data,
// path }) so callers receive the `data` payload directly.
// Failure: normalize every non-2xx response into an ApiError that carries the
// backend ErrorResponse `message` — the backend message always wins.
http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown> | undefined
    if (body && typeof body === 'object' && 'data' in body) {
      response.data = body.data
    }
    return response
  },
  (error: AxiosError<ErrorResponse>) => {
    if (error.response) {
      const body = error.response.data
      const url = error.config?.url ?? ''
      const isAuthCall =
        url.includes('/auth/login') || url.includes('/auth/register')

      // 401 outside login/register means the session expired or the token is
      // invalid: drop the stored session and ask the user to sign in again.
      // (Login/register themselves return 401 for bad credentials — that is a
      // displayable error, not a session expiry, so it is left to the caller.)
      if (error.response.status === 401 && !isAuthCall) {
        clearSession()
        window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
      }

      if (body && typeof body.message === 'string') {
        return Promise.reject(new ApiError(body))
      }

      // HTTP response received but without a usable ErrorResponse body (e.g.
      // a gateway/proxy HTML error page, or JSON without a string `message`):
      // classify by status code — never by the generic connection message.
      return Promise.reject(new ApiError(buildFallbackError(error)))
    }

    // No HTTP response at all (network down, CORS block, timeout, aborted):
    // only this case is a genuine connection failure.
    return Promise.reject(new ApiError(buildFallbackError(error)))
  },
)

const FALLBACK_MESSAGES: Record<number, string> = {
  400: 'Please check the submitted information.',
  401: 'Your session has expired. Please log in again.',
  403: 'You do not have permission to perform this action.',
  404: 'The requested resource was not found.',
  409: 'This action conflicts with an existing record.',
  429: 'AI is receiving too many requests. Please wait a moment and try again.',
  500: 'Something went wrong on the server. Please try again.',
  // 502 covers the AI Service's documented LLM-failure contract (OpenAI
  // quota/availability) and any other upstream failure. Pages may override
  // with more specific copy (see AiDiscoveryPage).
  502: 'The service is temporarily unavailable. Please try again later.',
  503: 'Service temporarily unavailable. Please try again later.',
}

function buildFallbackError(error: AxiosError): ErrorResponse {
  const status = error.response?.status ?? 0
  return {
    timestamp: new Date().toISOString(),
    status,
    error: error.response?.statusText ?? 'Network Error',
    message:
      FALLBACK_MESSAGES[status] ??
      'Unable to reach the server. Please check your connection and try again.',
    path: error.config?.url ?? '',
  }
}

// Typed request helpers. The response interceptor has already unwrapped the
// envelope, so the resolved value is the payload T itself.
export async function get<T>(url: string): Promise<T> {
  const response = await http.get<T>(url)
  return response.data
}

export async function post<T>(url: string, body?: unknown): Promise<T> {
  const response = await http.post<T>(url, body)
  return response.data
}

export async function put<T>(url: string, body?: unknown): Promise<T> {
  const response = await http.put<T>(url, body)
  return response.data
}

export async function del<T>(url: string): Promise<T> {
  const response = await http.delete<T>(url)
  return response.data
}

export async function patch<T>(url: string, body?: unknown): Promise<T> {
  const response = await http.patch<T>(url, body)
  return response.data
}

/**
 * Uploads a file via multipart/form-data.
 * The JWT is attached by the existing request interceptor.
 */
export async function uploadFile<T>(url: string, file: File): Promise<T> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await http.post<T>(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

export default http
