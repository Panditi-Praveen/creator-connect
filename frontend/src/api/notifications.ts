import { get, patch } from './client'
import type { NotificationResponse, Page } from '../types/notifications'

/**
 * Notification API surface — mirrors NotificationController in
 * backend/hiring-service.  Gateway rewrites /hiring/** so browser calls
 * /hiring/notifications/… paths.
 */

export function getNotifications(
  unreadOnly = false,
): Promise<Page<NotificationResponse>> {
  const qs = unreadOnly ? '?unreadOnly=true' : ''
  return get<Page<NotificationResponse>>(`/hiring/notifications${qs}`)
}

export function getUnreadCount(): Promise<number> {
  return get<number>('/hiring/notifications/unread-count')
}

export function markAsRead(notificationId: string): Promise<NotificationResponse> {
  return patch<NotificationResponse>(`/hiring/notifications/${notificationId}/read`)
}

export function markAllAsRead(): Promise<number> {
  return patch<number>('/hiring/notifications/read-all')
}
