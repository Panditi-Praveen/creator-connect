import { del, get, post, put } from './client'
import type {
  ApplicationRequest,
  ApplicationResponse,
  ApplicationStatus,
  FreelancerReviewsResponse,
  Page,
  ReviewRequest,
  ReviewResponse,
} from '../types/api'

/**
 * Hiring API surface — mirrors ApplicationController and ReviewController in
 * backend/hiring-service. The gateway rewrites /hiring/** -> /** before
 * forwarding, so the browser always talks to /hiring/... paths.
 */

export function applyToProject(payload: ApplicationRequest): Promise<ApplicationResponse> {
  return post<ApplicationResponse>('/hiring/applications', payload)
}

export function getMyApplications(): Promise<Page<ApplicationResponse>> {
  return get<Page<ApplicationResponse>>('/hiring/applications/my')
}

export function getProjectApplications(
  projectId: string,
): Promise<Page<ApplicationResponse>> {
  return get<Page<ApplicationResponse>>(`/hiring/applications/project/${projectId}`)
}

export function updateApplicationStatus(
  applicationId: string,
  status: ApplicationStatus,
): Promise<ApplicationResponse> {
  return put<ApplicationResponse>(`/hiring/applications/${applicationId}/status`, {
    status,
  })
}

export function withdrawApplication(applicationId: string): Promise<void> {
  return del<void>(`/hiring/applications/${applicationId}`)
}

export function submitReview(payload: ReviewRequest): Promise<ReviewResponse> {
  return post<ReviewResponse>('/hiring/reviews', payload)
}

export function getFreelancerReviews(
  freelancerId: string,
): Promise<FreelancerReviewsResponse> {
  return get<FreelancerReviewsResponse>(`/hiring/reviews/freelancer/${freelancerId}`)
}
