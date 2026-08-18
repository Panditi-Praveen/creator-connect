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

/* ------------------------------------------------------------------------- */
/* Profile (profile-service)                                                  */
/* ------------------------------------------------------------------------- */

export interface ProfileRequest {
  firstName: string
  lastName: string
  headline?: string
  bio?: string
  profileImageUrl?: string
  location?: string
  website?: string
  linkedin?: string
  github?: string
  skills?: string
  experience?: number
  availableForHire?: boolean
}

export interface ProfileResponse {
  id: string
  userId: string
  firstName: string
  lastName: string
  headline?: string
  bio?: string
  profileImageUrl?: string
  location?: string
  website?: string
  linkedin?: string
  github?: string
  skills?: string
  experience?: number
  availableForHire?: boolean
  createdAt: string
  updatedAt: string
}

/* ------------------------------------------------------------------------- */
/* Project (project-service)                                                  */
/* ------------------------------------------------------------------------- */

export type ProjectStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface ProjectRequest {
  title: string
  description: string
  category: string
  skillsRequired?: string[]
  budget: number
  duration: string
  experienceLevel: string
  location?: string
  applicationDeadline: string // yyyy-MM-dd
}

/** Partial-update payload for PUT /projects/{id} (PUT-as-PATCH semantics). */
export interface UpdateProjectRequest {
  title?: string
  description?: string
  category?: string
  skillsRequired?: string[]
  budget?: number
  duration?: string
  experienceLevel?: string
  location?: string
  applicationDeadline?: string
}

export interface ProjectResponse {
  id: string
  userId: string
  ownerProfile?: ProfileResponse | null
  title: string
  description: string
  category: string
  skillsRequired?: string[]
  budget: number
  duration: string
  experienceLevel: string
  location?: string
  status: ProjectStatus
  applicationDeadline: string
  createdAt: string
  updatedAt: string
}

export interface ProjectFilters {
  category?: string
  skill?: string
  budgetMin?: number
  budgetMax?: number
  experienceLevel?: string
  location?: string
  keyword?: string
}

/* ------------------------------------------------------------------------- */
/* Hiring - applications (hiring-service, reached via /hiring/**)             */
/* ------------------------------------------------------------------------- */

export type ApplicationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN'

export interface ApplicationRequest {
  projectId: string
  proposal: string
  expectedBudget: number
  estimatedDuration: string
}

export interface ApplicationResponse {
  id: string
  projectId: string
  freelancerId: string
  proposal: string
  expectedBudget: number
  estimatedDuration: string
  status: ApplicationStatus
  createdAt: string
  updatedAt: string
}

/** Spring Data page envelope carried in ApiResponse.data. */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
}

/* ------------------------------------------------------------------------- */
/* Hiring - reviews                                                           */
/* ------------------------------------------------------------------------- */

export interface ReviewRequest {
  projectId: string
  freelancerId: string
  rating: number // 1-5
  reviewText?: string
}

export interface ReviewResponse {
  id: string
  projectId: string
  creatorId: string
  freelancerId: string
  rating: number
  reviewText?: string
  createdAt: string
  updatedAt: string
}

export interface FreelancerReviewsResponse {
  freelancerId: string
  averageRating: number
  totalReviews: number
  reviews: ReviewResponse[]
}

/* ------------------------------------------------------------------------- */
/* AI discovery (ai-service)                                                  */
/* ------------------------------------------------------------------------- */

export interface DiscoverRequest {
  query: string
}

export interface TalentResult {
  profileId: string
  userId: string
  name?: string
  headline?: string
  skills?: string
  location?: string
  availableForHire?: boolean
  score: number
  reason?: string
}

export interface DiscoverResponse {
  query: string
  count: number
  results: TalentResult[]
}

export interface AiStatusResponse {
  service: string
  status: string
}
