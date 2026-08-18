import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { listProjects } from '../api/projects'
import type { ProjectResponse } from '../types/api'

export default function ProjectsPage() {
  const [projects, setProjects] = useState<ProjectResponse[]>([])
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async (search: string) => {
    setLoading(true)
    setError(null)
    try {
      const result = await listProjects(search ? { keyword: search } : undefined)
      setProjects(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load projects.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load('')
  }, [load])

  const handleSearch = (event: FormEvent) => {
    event.preventDefault()
    void load(keyword.trim())
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>Projects</h1>
        <Link to="/projects/create" className="btn btn-primary">
          Create project
        </Link>
      </header>

      <form className="field" onSubmit={handleSearch} style={{ marginTop: '1rem' }}>
        <label htmlFor="keyword">Search (title or description)</label>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <input
            id="keyword"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="e.g. youtube, video editing"
            style={{ flex: 1 }}
          />
          <button type="submit" className="btn">
            Search
          </button>
        </div>
      </form>

      {loading && <p className="loading">Loading projects…</p>}

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      {!loading && !error && projects.length === 0 && (
        <div className="empty">
          No projects found. Post the first one — it only takes a minute.
        </div>
      )}

      {!loading && !error && projects.length > 0 && (
        <div className="card-grid">
          {projects.map((project) => (
            <article className="card" key={project.id}>
              <h3>
                <Link to={`/projects/${project.id}`}>{project.title}</Link>
              </h3>
              <p className="muted">
                {project.category} · {project.duration}
              </p>
              <p>
                Budget: <strong>${Number(project.budget).toLocaleString()}</strong>{' '}
                · {project.experienceLevel}
              </p>
              <p className="muted">
                {project.location ?? 'Remote'} · Deadline{' '}
                {project.applicationDeadline}
              </p>
              <span className={`badge badge-${project.status.toLowerCase()}`}>
                {project.status.replace('_', ' ')}
              </span>
            </article>
          ))}
        </div>
      )}
    </main>
  )
}
