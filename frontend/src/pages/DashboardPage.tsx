import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function DashboardPage() {
  const { user, logout } = useAuth()

  return (
    <main className="page">
      <header className="page-header">
        <h1>Dashboard</h1>
        <button type="button" onClick={logout}>
          Sign out
        </button>
      </header>

      {user && (
        <p>
          Signed in as <strong>{user.email}</strong> ({user.role})
        </p>
      )}

      <nav className="nav-list">
        <Link to="/projects">Projects</Link>
        <Link to="/projects/create">Create project</Link>
        <Link to="/hiring">Hiring</Link>
        <Link to="/profile">Profile</Link>
      </nav>
    </main>
  )
}
