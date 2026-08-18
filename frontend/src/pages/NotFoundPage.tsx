import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <main className="page">
      <h1>Page not found</h1>
      <p>The page you are looking for does not exist.</p>
      <p>
        <Link to="/dashboard">← Back to dashboard</Link>
      </p>
    </main>
  )
}
