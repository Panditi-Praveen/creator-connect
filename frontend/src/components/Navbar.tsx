import { NavLink } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

const links = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/projects', label: 'Projects' },
  { to: '/hiring', label: 'Hiring' },
  { to: '/reviews', label: 'Reviews' },
  { to: '/ai', label: 'AI Discovery' },
  { to: '/profile', label: 'Profile' },
]

export default function Navbar() {
  const { user, logout } = useAuth()

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <NavLink to="/dashboard" className="navbar-brand">
          CreatorConnect
        </NavLink>
        <div className="navbar-links">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => (isActive ? 'navbar-link active' : 'navbar-link')}
            >
              {link.label}
            </NavLink>
          ))}
        </div>
        <div className="navbar-right">
          {user && (
            <span className="navbar-user">
              {user.email} ({user.role})
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
