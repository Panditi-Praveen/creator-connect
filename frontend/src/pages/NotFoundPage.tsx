import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <main className="page" style={{ maxWidth: 640 }}>
      <div className="empty" style={{ marginTop: '3rem', padding: '3.5rem 2rem' }}>
        <span className="empty-icon" aria-hidden="true">🧭</span>
        <span className="empty-title">Page not found</span>
        <p className="empty-desc">
          The page you are looking for does not exist or may have been moved.
        </p>
        <div className="card-actions" style={{ justifyContent: 'center', marginTop: '0.5rem' }}>
          <Link to="/dashboard" className="btn btn-primary">
            Back to dashboard
          </Link>
          <Link to="/projects" className="btn">
            Browse projects
          </Link>
        </div>
      </div>
    </main>
  )
}
