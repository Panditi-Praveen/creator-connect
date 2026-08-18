/**
 * In-app notification types — mirrors the backend NotificationType enum
 * and NotificationResponse DTO in hiring-service.
 */

export type NotificationType =
  | 'APPLICATION_RECEIVED'
  | 'APPLICATION_ACCEPTED'
  | 'APPLICATION_REJECTED'
  | 'APPLICATION_WITHDRAWN'
  | 'REVIEW_RECEIVED'

export interface NotificationResponse {
  id: string
  recipientId: string
  type: NotificationType
  title: string
  message: string | null
  read: boolean
  relatedResourceId: string | null
  relatedResourceType: string | null
  createdAt: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
