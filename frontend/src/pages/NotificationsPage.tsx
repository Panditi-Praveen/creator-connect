import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getNotifications,
  markAsRead,
  markAllAsRead,
  getUnreadCount,
} from '../api/notifications'
import type { NotificationResponse, Page } from '../types/notifications'

type Filter = 'ALL' | 'UNREAD' | 'READ'

function timeAgo(dateStr: string): string {
  const seconds = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000)
  if (seconds < 60) return 'just now'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  return `${days}d ago`
}

function iconFor(type: string): string {
  switch (type) {
    case 'APPLICATION_RECEIVED':
      return '📩'
    case 'APPLICATION_ACCEPTED':
      return '✅'
    case 'APPLICATION_REJECTED':
      return '❌'
    case 'APPLICATION_WITHDRAWN':
      return '↩️'
    case 'REVIEW_RECEIVED':
      return '⭐'
    default:
      return '🔔'
  }
}

export default function NotificationsPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState<Page<NotificationResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<Filter>('ALL')
  const [unreadCount, setUnreadCount] = useState(0)

  const fetchNotifications = useCallback(async () => {
    setLoading(true)
    try {
      const unreadOnly = filter === 'UNREAD'
      const data = await getNotifications(unreadOnly)
      const filtered =
        filter === 'READ'
          ? { ...data, content: data.content.filter((n) => n.read) }
          : data
      setPage(filtered)
    } catch {
      setPage(null)
    } finally {
      setLoading(false)
    }
  }, [filter])

  useEffect(() => {
    fetchNotifications()
  }, [fetchNotifications])

  useEffect(() => {
    getUnreadCount()
      .then((c) => setUnreadCount(c))
      .catch(() => {})
  }, [fetchNotifications])

  const handleMarkRead = async (n: NotificationResponse) => {
    if (n.read) return
    try {
      await markAsRead(n.id)
      setUnreadCount((c) => Math.max(0, c - 1))
      setPage((prev) =>
        prev
          ? {
              ...prev,
              content: prev.content.map((item) =>
                item.id === n.id ? { ...item, read: true } : item,
              ),
            }
          : prev,
      )
    } catch {
      /* non-critical */
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await markAllAsRead()
      setUnreadCount(0)
      setPage((prev) =>
        prev
          ? {
              ...prev,
              content: prev.content.map((n) => ({ ...n, read: true })),
            }
          : prev,
      )
    } catch {
      /* non-critical */
    }
  }

  const handleNavigate = (n: NotificationResponse) => {
    handleMarkRead(n)
    if (n.relatedResourceType === 'APPLICATION') {
      navigate('/applications')
    } else if (n.relatedResourceType === 'PROJECT') {
      navigate('/projects')
    }
  }

  const notifications = page?.content ?? []

  return (
    <main className="page page-anim">
      <header className="page-header">
        <div className="notif-page-head">
          <h1>Notifications</h1>
          {unreadCount > 0 && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={handleMarkAllRead}
            >
              Mark all as read
            </button>
          )}
        </div>
        <p className="muted">Stay updated on your applications and projects.</p>
      </header>

      <div className="notif-filters">
        {(['ALL', 'UNREAD', 'READ'] as Filter[]).map((f) => (
          <button
            key={f}
            type="button"
            className={`notif-filter ${filter === f ? 'active' : ''}`}
            onClick={() => setFilter(f)}
          >
            {f.charAt(0) + f.slice(1).toLowerCase()}
            {f === 'UNREAD' && unreadCount > 0 && (
              <span className="notif-filter-count">{unreadCount}</span>
            )}
          </button>
        ))}
      </div>

      {loading && (
        <div className="skeleton-grid">
          {[1, 2, 3].map((i) => (
            <div key={i} className="skeleton skeleton-card" />
          ))}
        </div>
      )}

      {!loading && notifications.length === 0 && (
        <div className="empty-state">
          <span className="empty-icon">🔔</span>
          <h3>No notifications</h3>
          <p>
            {filter === 'UNREAD'
              ? "You're all caught up!"
              : 'Notifications about your applications and projects will appear here.'}
          </p>
        </div>
      )}

      {!loading && notifications.length > 0 && (
        <div className="notif-list">
          {notifications.map((n) => (
            <button
              key={n.id}
              type="button"
              className={`notif-card ${n.read ? '' : 'unread'}`}
              onClick={() => handleNavigate(n)}
            >
              <span className="notif-card-icon">{iconFor(n.type)}</span>
              <div className="notif-card-body">
                <div className="notif-card-title">{n.title}</div>
                {n.message && <div className="notif-card-msg">{n.message}</div>}
                <div className="notif-card-time">{timeAgo(n.createdAt)}</div>
              </div>
              {!n.read && <span className="notif-dot" />}
            </button>
          ))}
        </div>
      )}
    </main>
  )
}
