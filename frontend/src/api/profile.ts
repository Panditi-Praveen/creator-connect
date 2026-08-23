import { del, get, post, put, uploadFile } from './client'
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

/**
 * Uploads or replaces the authenticated user's profile picture.
 */
export function uploadProfilePicture(file: File): Promise<ProfileResponse> {
  return uploadFile<ProfileResponse>('/profile/me/photo', file)
}

/**
 * Removes the authenticated user's profile picture.
 */
export function deleteProfilePicture(): Promise<ProfileResponse> {
  return del<ProfileResponse>('/profile/me/photo')
}

/**
 * Saves or updates the authenticated user's location details.
 */
export function updateLocation(params: {
  latitude: number
  longitude: number
  city?: string
  state?: string
  country?: string
  formattedAddress?: string
}): Promise<ProfileResponse> {
  const query = new URLSearchParams()
  query.set('latitude', params.latitude.toString())
  query.set('longitude', params.longitude.toString())
  if (params.city) query.set('city', params.city)
  if (params.state) query.set('state', params.state)
  if (params.country) query.set('country', params.country)
  if (params.formattedAddress) query.set('formattedAddress', params.formattedAddress)
  return put<ProfileResponse>(`/profile/me/location?${query.toString()}`)
}
