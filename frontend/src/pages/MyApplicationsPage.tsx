import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { getMyApplications, withdrawApplication } from '../api/hiring'
import { listProjects } from '../api/projects'
import { useAuth } from '../hooks/useAuth'
import type { ApplicationResponse, ApplicationStatus, ProjectResponse } from '../types/api'

const FILTERS: Array<{ key: 'ALL' | ApplicationStatus; label: string }> = [
  { key: 'ALL', label: 'All' },
  { key: 'PENDING', label: 'Pending' },
  { key: 'ACCEPTED', label: 'Accepted' },
  { key: 'REJECTED', label: 'Rejected' },
  { key: 'WITHDRAWN', label: 'Withdrawn' },
]

function dateLabel(value: string): string {
  return value.slice(0, 10)
}

export default function MyApplicationsPage() {
  const { user } = useAuth()

  // Freelancers only — creators manage applicants from Hiring / project pages.
  if (user?.role !== 'FREELANCER') {
    return <Navigate to="/hiring" replace />
  }

  return <ApplicationsList />
}

function ApplicationsList() {
  const [applications, setApplications] = useState<ApplicationResponse[]>([])
  const [projects, setProjects] = useState<ProjectResponse[]>([])
  const [filter, setFilter] = useState<'ALL' | ApplicationStatus>('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [withdrawingId, setWithdrawingId] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [appsPage, projectList] = await Promise.all([
        getMyApplications(),
        listProjects(),
      ])
      setApplications(appsPage.content)
      setProjects(projectList)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load applications.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const projectsById = useMemo(() => {
    const map = new Map<string, ProjectResponse>()
    for (const project of projects) map.set(project.id, project)
    return map
  }, [projects])

  const filtered = useMemo(
    () =>
      filter === 'ALL'
        ? applications
        : applications.filter((application) => application.status === filter),
    [applications, filter],
  )

  const handleWithdraw = async (applicationId: string) => {
    setWithdrawingId(applicationId)
    setError(null)
    try {
      await withdrawApplication(applicationId)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to withdraw application.')
    } finally {
      setWithdrawingId(null)
    }
  }

  if (loading) {
    return (
      <div className="skeleton-grid" aria-label="Loading applications">
        {[0, 1, 2].map((item) => (
          <div className="skeleton-card" key={item}>
            <div className="skeleton w40" />
            <div className="skeleton w90" />
            <div className="skeleton w60" />
            <div className="skeleton w30" />
          </div>
        ))}
      </div>
    )
  }

  return (
    <>
      <div className="section-head" style={{ marginTop: '1.5rem' }}>
        <div>
          <h2>My Applications</h2>
          <p className="muted">
            {applications.length === 0
              ? 'Track the status of your proposals here.'
              : `${applications.length} application${applications.length === 1 ? '' : 's'} submitted.`}
          </p>
        </div>
      </div>

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      {applications.length === 0 ? (
        <div className="empty">
          <span className="empty-icon" aria-hidden="true">📨</span>
          <span className="empty-title">No applications yet</span>
          <p className="empty-desc">
            Explore projects and apply to opportunities that match your skills.
          </p>
          <Link to="/projects" className="btn btn-primary">
            Find Projects
          </Link>
        </div>
      ) : (
        <>
          <div className="filter-row" role="group" aria-label="Filter applications">
            {FILTERS.map((option) => (
              <button
                key={option.key}
                type="button"
                className={filter === option.key ? 'filter-pill active' : 'filter-pill'}
                onClick={() => setFilter(option.key)}
                aria-pressed={filter === option.key}
              >
                {option.label}
              </button>
            ))}
          </div>

          {filtered.length === 0 ? (
            <div className="empty">
              <span className="empty-icon" aria-hidden="true">🔎</span>
              <span className="empty-title">
                No {filter === 'ALL' ? '' : `${filter.toLowerCase()} `}applications
              </span>
              <p className="empty-desc">
                Applications with this status will appear here.
              </p>
              <button type="button" className="btn" onClick={() => setFilter('ALL')}>
                Show all applications
              </button>
            </div>
          ) : (
            <div className="card-grid stagger">
              {filtered.map((application) => {
                const project = projectsById.get(application.projectId)
                return (
                  <article className="card" key={application.id}>
                    <div className="project-card-top">
                      <span className={`badge badge-${application.status.toLowerCase()}`}>
                        {application.status}
                      </span>
                      <span className={`badge badge-${project?.status.toLowerCase() ?? 'open'}`}>
                        {project?.status.replace('_', ' ') ?? 'Project'}
                      </span>
                    </div>
                    <h3>
                      {project ? (
                        <Link to={`/projects/${project.id}`}>{project.title}</Link>
                      ) : (
                        `Project ${application.projectId.slice(0, 8)}`
                      )}
                    </h3>
                    {project && (
                      <p className="muted">
                        {project.category} · ${Number(project.budget).toLocaleString()}{' '}
                        budget · {project.duration}
                      </p>
                    )}
                    <div className="meta-row">
                      <span className="meta-item">📅 Applied {dateLabel(application.createdAt)}</span>
                      {project?.applicationDeadline && (
                        <span className="meta-item">⏳ Deadline {project.applicationDeadline}</span>
                      )}
                      <span className="meta-item">💰 ${Number(application.expectedBudget).toLocaleString()}</span>
                    </div>
                    {application.estimatedDuration && (
                      <p className="muted" style={{ fontSize: '0.85rem' }}>
                        Estimated {application.estimatedDuration}
                      </p>
                    )}
                    <div className="card-actions">
                      <Link
                        to={`/projects/${application.projectId}`}
                        className="btn btn-sm btn-primary"
                      >
                        View project
                      </Link>
                      {application.status === 'PENDING' && (
                        <button
                          type="button"
                          className="btn btn-sm"
                          disabled={withdrawingId === application.id}
                          onClick={() => void handleWithdraw(application.id)}
                        >
                          {withdrawingId === application.id ? 'Withdrawing…' : 'Withdraw'}
                        </button>
                      )}
                    </div>
                  </article>
                )
              })}
            </div>
          )}
        </>
      )}
    </>
  )
}
