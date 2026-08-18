import { del, get, post, put } from './client'
import type { ProfileRequest, ProfileResponse } from '../types/api'

/**
 * Profile API surface — mirrors ProfileController in backend/profile-service,
 * reached through the API Gateway at /profile/**.
 */

export function getMyProfile(): Promise<ProfileResponse> {
  return get<ProfileResponse>('/profile/me')
}

export function getProfileByUserId(userId: string): Promise<ProfileResponse> {
  return get<ProfileResponse>(`/profile/${userId}`)
}

export function listFreelancerProfiles(): Promise<ProfileResponse[]> {
  return get<ProfileResponse[]>('/profile/freelancers')
}

export function createProfile(payload: ProfileRequest): Promise<ProfileResponse> {
  return post<ProfileResponse>('/profile', payload)
}

export function updateProfile(
  userId: string,
  payload: Partial<ProfileRequest>,
): Promise<ProfileResponse> {
  return put<ProfileResponse>(`/profile/${userId}`, payload)
}

export function deleteProfile(userId: string): Promise<void> {
  return del<void>(`/profile/${userId}`)
}
