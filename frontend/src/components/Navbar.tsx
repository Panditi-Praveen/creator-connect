import { useState } from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import NotificationBell from './NotificationBell'

const baseLinks = [
  { to: '/', label: 'Home', end: true },
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/projects', label: 'Projects' },
  { to: '/hiring', label: 'Hiring' },
  { to: '/reviews', label: 'Reviews' },
  { to: '/ai', label: 'AI Discovery' },
  { to: '/profile', label: 'Profile' },
]

type NavLinkItem = { to: string; label: string; end?: boolean }

function linksForRole(isFreelancer: boolean): NavLinkItem[] {
  const links: NavLinkItem[] = []
  for (const link of baseLinks) {
    links.push(link)
    if (link.to === '/projects' && isFreelancer) {
      links.push({ to: '/applications', label: 'My Applications' })
    }
  }
  return links
}

function initials(name?: string): string {
  if (!name) return '?'
  return name
    .split('@')[0]
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}

export default function Navbar() {
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const links = linksForRole(user?.role === 'FREELANCER')

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <NavLink to="/" className="navbar-brand" onClick={() => setMenuOpen(false)}>
          <span className="brand-mark" aria-hidden="true">
            CC
          </span>
          CreatorConnect
        </NavLink>

        <button
          type="button"
          className="navbar-toggle"
          aria-label="Toggle navigation menu"
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((open) => !open)}
        >
          {menuOpen ? '✕' : '☰'}
        </button>

        <div className={menuOpen ? 'navbar-links open' : 'navbar-links'}>
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) =>
                isActive ? 'navbar-link active' : 'navbar-link'
              }
              onClick={() => setMenuOpen(false)}
            >
              {link.label}
            </NavLink>
          ))}
        </div>

        <div className="navbar-right">
          {user && (
            <span className="navbar-user" title={`${user.email} · ${user.role}`}>
              <NotificationBell />
              <span className="avatar" aria-hidden="true">
                {initials(user.email)}
              </span>
              <span className="user-text">{user.email}</span>
              <span className="role-chip">{user.role}</span>
            </span>
          )}
          <button type="button" className="navbar-logout" onClick={logout}>
            Sign out
          </button>
        </div>
      </div>
    </nav>
  )
}
