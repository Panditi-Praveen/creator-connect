import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { listProjects } from '../api/projects'
import { getMyApplications } from '../api/hiring'
import type { ProjectResponse } from '../types/api'
import { useAuth } from '../hooks/useAuth'

const CATEGORY_ICONS: Record<string, string> = {
  'video editing': '🎬',
  'graphic design': '🎨',
  'web development': '💻',
  'writing': '✍️',
  'social media': '📱',
  'photography': '📷',
  'music': '🎵',
  'animation': '🎞️',
}

function categoryIcon(category: string): string {
  return CATEGORY_ICONS[category.toLowerCase()] ?? '📌'
}

export default function ProjectsPage() {
  const [searchParams] = useSearchParams()
  const urlKeyword = searchParams.get('keyword') ?? ''
  const { user } = useAuth()
  const isFreelancer = user?.role === 'FREELANCER'
  const [projects, setProjects] = useState<ProjectResponse[]>([])
  // Pre-fill from the home page search/category deep links (?keyword=...).
  const [keyword, setKeyword] = useState(urlKeyword)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // Project ids the current freelancer already applied to — fetched once
  // (never N+1); the project detail page remains the source of truth.
  const [appliedIds, setAppliedIds] = useState<Set<string>>(new Set())

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
    void load(urlKeyword)
  }, [load, urlKeyword])

  useEffect(() => {
    if (!isFreelancer) return
    let cancelled = false
    getMyApplications()
      .then((page) => {
        if (!cancelled) {
          setAppliedIds(new Set(page.content.map((a) => a.projectId)))
        }
      })
      .catch(() => {
        /* cards keep showing the default View action */
      })
    return () => {
      cancelled = true
    }
  }, [isFreelancer])

  const handleSearch = (event: FormEvent) => {
    event.preventDefault()
    void load(keyword.trim())
  }

  const handleClear = () => {
    setKeyword('')
    void load('')
  }

  return (
    <main className="page">
      <section className="hero">
        <h1>Find your next opportunity</h1>
        <p className="sub">
          Explore projects from clients looking for talented creators. Filter by
          keyword and apply in minutes.
        </p>
        <div className="hero-actions">
          <Link to="/projects/create" className="btn btn-primary">
            + Create project
          </Link>
        </div>
      </section>

      <form className="search-bar" role="search" onSubmit={handleSearch}>
        <span className="search-icon" aria-hidden="true">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <circle cx="11" cy="11" r="7" />
            <path d="m21 21-4.3-4.3" />
          </svg>
        </span>
        <input
          id="keyword"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="Search projects — e.g. youtube, video editing, design…"
          aria-label="Search projects by title or description"
        />
        {keyword.length > 0 && (
          <button
            type="button"
            className="search-clear"
            aria-label="Clear search"
            onClick={handleClear}
          >
            ✕
          </button>
        )}
        <button type="submit" className="btn btn-primary search-submit">
          Search
        </button>
      </form>

      {loading && (
        <div className="skeleton-grid" aria-label="Loading projects">
          {[0, 1, 2, 3, 4, 5].map((item) => (
            <div className="skeleton-card" key={item}>
              <div className="skeleton w40" />
              <div className="skeleton w90" />
              <div className="skeleton w60" />
              <div className="skeleton w30" />
            </div>
          ))}
        </div>
      )}

      {error && (
        <p className="form-error" role="alert" style={{ marginTop: '1rem' }}>
          {error}
        </p>
      )}

      {!loading && !error && projects.length === 0 && (
        <div className="empty">
          <span className="empty-icon" aria-hidden="true">🔍</span>
          <span className="empty-title">No projects found</span>
          <p className="empty-desc">
            {keyword
              ? `Nothing matched “${keyword}”. Try a different search term.`
              : 'There are no projects yet. Post the first one — it only takes a minute.'}
          </p>
          {keyword ? (
            <button type="button" className="btn" onClick={handleClear}>
              Clear search
            </button>
          ) : (
            <Link to="/projects/create" className="btn btn-primary">
              Create project
            </Link>
          )}
        </div>
      )}

      {!loading && !error && projects.length > 0 && (
        <div className="card-grid stagger">
          {projects.map((project) => (
            <article className="card" key={project.id}>
              <div className="project-card-top">
                <span className="category-icon" aria-hidden="true">
                  {categoryIcon(project.category)}
                </span>
                <span className="badge-group">
                  {appliedIds.has(project.id) && (
                    <span className="badge badge-completed">✓ Applied</span>
                  )}
                  <span className={`badge badge-${project.status.toLowerCase()}`}>
                    {project.status.replace('_', ' ')}
                  </span>
                </span>
              </div>
              <h3>
                <Link to={`/projects/${project.id}`}>{project.title}</Link>
              </h3>
              <p className="muted">{project.description.slice(0, 120)}{project.description.length > 120 ? '…' : ''}</p>
              <div className="meta-row">
                <span className="meta-item">🗂️ {project.category}</span>
                <span className="meta-item">⏱️ {project.duration}</span>
                <span className="meta-item">🎯 {project.experienceLevel}</span>
                <span className="meta-item">📅 Deadline {project.applicationDeadline}</span>
              </div>
              <div className="card-footer">
                <span className="budget">
                  ${Number(project.budget).toLocaleString()} <span>budget</span>
                </span>
                <span className="meta-item">
                  📍 {project.location ?? 'Remote'}
                </span>
              </div>
            </article>
          ))}
        </div>
      )}
    </main>
  )
}
