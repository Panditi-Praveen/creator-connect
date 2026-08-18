import { del, get, post, put } from './client'
import type {
  ProjectFilters,
  ProjectRequest,
  ProjectResponse,
  ProjectStatus,
  UpdateProjectRequest,
} from '../types/api'

/**
 * Project API surface — mirrors ProjectController in backend/project-service,
 * reached through the API Gateway at /projects/**.
 */

function toQuery(filters?: ProjectFilters): string {
  if (!filters) return ''
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== null && value !== '') {
      params.set(key, String(value))
    }
  }
  const qs = params.toString()
  return qs ? `?${qs}` : ''
}

export function listProjects(filters?: ProjectFilters): Promise<ProjectResponse[]> {
  return get<ProjectResponse[]>(`/projects${toQuery(filters)}`)
}

export function listMyProjects(filters?: ProjectFilters): Promise<ProjectResponse[]> {
  return get<ProjectResponse[]>(`/projects/my${toQuery(filters)}`)
}

export function getProject(projectId: string): Promise<ProjectResponse> {
  return get<ProjectResponse>(`/projects/${projectId}`)
}

export function createProject(payload: ProjectRequest): Promise<ProjectResponse> {
  return post<ProjectResponse>('/projects', payload)
}

export function updateProject(
  projectId: string,
  payload: UpdateProjectRequest,
): Promise<ProjectResponse> {
  return put<ProjectResponse>(`/projects/${projectId}`, payload)
}

export function updateProjectStatus(
  projectId: string,
  status: ProjectStatus,
): Promise<ProjectResponse> {
  return put<ProjectResponse>(`/projects/${projectId}/status`, { status })
}

export function deleteProject(projectId: string): Promise<void> {
  return del<void>(`/projects/${projectId}`)
}
