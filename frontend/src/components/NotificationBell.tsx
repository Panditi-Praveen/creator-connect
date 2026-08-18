import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  getNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
} from '../api/notifications'
import type { NotificationResponse, Page } from '../types/notifications'

const POLL_MS = 30_000 // re-check unread count every 30 s

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

export default function NotificationBell() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [count, setCount] = useState(0)
  const [page, setPage] = useState<Page<NotificationResponse> | null>(null)
  const [loading, setLoading] = useState(false)
  const dropRef = useRef<HTMLDivElement>(null)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  /* ---- fetch helpers ---- */

  const refreshCount = useCallback(() => {
    getUnreadCount()
      .then((c) => setCount(c))
      .catch(() => {})
  }, [])

  const refreshPage = useCallback(() => {
    setLoading(true)
    getNotifications(false)
      .then((p) => setPage(p))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  /* ---- poll unread count ---- */
  useEffect(() => {
    refreshCount()
    timerRef.current = setInterval(refreshCount, POLL_MS)
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [refreshCount])

  /* ---- fetch page when dropdown opens ---- */
  useEffect(() => {
    if (open) {
      refreshPage()
    }
  }, [open, refreshPage])

  /* ---- close on outside click ---- */
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (dropRef.current && !dropRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    if (open) document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [open])

  /* ---- actions ---- */

  const handleMarkRead = async (n: NotificationResponse) => {
    if (n.read) return
    try {
      await markAsRead(n.id)
      setCount((c) => Math.max(0, c - 1))
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
      /* notification failure is non-critical */
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await markAllAsRead()
      setCount(0)
      setPage((prev) =>
        prev
          ? { ...prev, content: prev.content.map((n) => ({ ...n, read: true })) }
          : prev,
      )
    } catch {
      /* non-critical */
    }
  }

  const handleBellClick = () => {
    setOpen((o) => !o)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') setOpen(false)
  }

  const items = page?.content?.slice(0, 8) ?? []

  return (
    <div className="notif-bell-wrap" ref={dropRef} onKeyDown={handleKeyDown}>
      <button
        type="button"
        className="notif-bell-btn"
        aria-label={`Notifications${count > 0 ? ` (${count} unread)` : ''}`}
        onClick={handleBellClick}
      >
        🔔
        {count > 0 && (
          <span className="notif-badge">{count > 99 ? '99+' : count}</span>
        )}
      </button>

      {open && (
        <div className="notif-dropdown" role="menu">
          <div className="notif-drop-header">
            <h4>Notifications</h4>
            {count > 0 && (
              <button
                type="button"
                className="notif-mark-all"
                onClick={handleMarkAllRead}
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="notif-drop-list">
            {loading && <p className="notif-empty">Loading…</p>}

            {!loading && items.length === 0 && (
              <p className="notif-empty">No notifications yet</p>
            )}

            {!loading &&
              items.map((n) => (
                <button
                  key={n.id}
                  type="button"
                  className={`notif-item ${n.read ? '' : 'unread'}`}
                  role="menuitem"
                  onClick={() => {
                    handleMarkRead(n)
                    setOpen(false)
                    if (n.relatedResourceType === 'APPLICATION') {
                      navigate('/applications')
                    } else if (n.relatedResourceType === 'PROJECT') {
                      navigate('/projects')
                    } else {
                      navigate('/notifications')
                    }
                  }}
                >
                  <span className="notif-icon">{iconFor(n.type)}</span>
                  <span className="notif-body">
                    <span className="notif-title">{n.title}</span>
                    {n.message && (
                      <span className="notif-msg">{n.message}</span>
                    )}
                    <span className="notif-time">{timeAgo(n.createdAt)}</span>
                  </span>
                </button>
              ))}
          </div>

          <div className="notif-drop-footer">
            <Link
              to="/notifications"
              className="notif-view-all"
              onClick={() => setOpen(false)}
            >
              View all notifications
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
