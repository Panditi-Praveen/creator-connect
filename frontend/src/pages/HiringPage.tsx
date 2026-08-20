import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  applyToProject,
  getMyApplications,
  getProjectApplications,
  updateApplicationStatus,
  withdrawApplication,
} from '../api/hiring'
import { listMyProjects, listProjects } from '../api/projects'
import type { ApplicationResponse, ProjectResponse } from '../types/api'
import { useAuth } from '../hooks/useAuth'
import { swalConfirm, swalSuccess, swalError } from '../utils/notify'

export default function HiringPage() {
  const { user } = useAuth()
  const isFreelancer = user?.role === 'FREELANCER'

  return (
    <main className="page">
      <header className="page-header" style={{ marginBottom: '1.25rem' }}>
        <div>
          <h1>Hiring</h1>
          <p className="sub">
            {isFreelancer
              ? 'Apply to open projects and track your proposals.'
              : 'Review applicants for your projects and pick the right talent.'}
          </p>
        </div>
      </header>
      {isFreelancer ? <FreelancerHiring /> : <CreatorHiring />}
    </main>
  )
}

function statusBadge(status: string) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{status}</span>
}

/* ------------------------------ Freelancer ------------------------------ */

function FreelancerHiring() {
  const [projects, setProjects] = useState<ProjectResponse[]>([])
  const [applications, setApplications] = useState<ApplicationResponse[]>([])
  const [selectedProject, setSelectedProject] = useState('')
  const [proposal, setProposal] = useState('')
  const [budget, setBudget] = useState('')
  const [duration, setDuration] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [openProjects, myApps] = await Promise.all([
        listProjects(),
        getMyApplications(),
      ])
      // Backend accepts applications on OPEN and IN_PROGRESS projects.
      setProjects(
        openProjects.filter(
          (p) => p.status === 'OPEN' || p.status === 'IN_PROGRESS',
        ),
      )
      setApplications(myApps.content)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load hiring data.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const handleApply = async (event: FormEvent) => {
    event.preventDefault()
    if (!selectedProject) return
    setSubmitting(true)
    setError(null)
    setSuccess(null)
    try {
      await applyToProject({
        projectId: selectedProject,
        proposal: proposal.trim(),
        expectedBudget: Number(budget),
        estimatedDuration: duration.trim(),
      })
      setProposal('')
      setBudget('')
      setDuration('')
      setSelectedProject('')
      void swalSuccess(
        'Application Submitted!',
        'Your application has been submitted successfully.',
        'OK',
      )
      setSuccess('Application submitted successfully.')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit application.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleWithdraw = async (applicationId: string) => {
    const confirmed = await swalConfirm(
      'Withdraw application?',
      'This action cannot be undone. Are you sure?',
      'Withdraw',
    )
    if (!confirmed) return
    setError(null)
    try {
      await withdrawApplication(applicationId)
      void swalSuccess('Withdrawn', 'Your application has been withdrawn.')
      await load()
    } catch (err) {
      void swalError('Error', err instanceof Error ? err.message : 'Failed to withdraw application.')
      setError(err instanceof Error ? err.message : 'Failed to withdraw application.')
    }
  }

  if (loading) {
    return (
      <div className="skeleton-grid" aria-label="Loading hiring data">
        {[0, 1, 2].map((item) => (
          <div className="skeleton-card" key={item}>
            <div className="skeleton w40" />
            <div className="skeleton w90" />
            <div className="skeleton w60" />
          </div>
        ))}
      </div>
    )
  }

  return (
    <>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      {success && (
        <p className="form-success" role="status">
          {success}
        </p>
      )}

      <section>
        <div className="section-head">
          <h2>Apply to a project</h2>
        </div>
        {projects.length === 0 ? (
          <div className="empty">
            <span className="empty-icon" aria-hidden="true">🗂️</span>
            <span className="empty-title">No open projects right now</span>
            <p className="empty-desc">
              No projects are accepting applications at the moment.{' '}
              <Link to="/projects">Browse projects</Link> to see what is available.
            </p>
          </div>
        ) : (
          <form className="form" onSubmit={handleApply}>
            <div className="form-card">
            <div className="field">
              <label htmlFor="project">Project *</label>
              <select
                id="project"
                required
                value={selectedProject}
                onChange={(event) => setSelectedProject(event.target.value)}
              >
                <option value="">Select a project…</option>
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.title} (${Number(project.budget).toLocaleString()})
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <label htmlFor="proposal">Proposal *</label>
              <textarea
                id="proposal"
                required
                rows={4}
                maxLength={5000}
                value={proposal}
                onChange={(event) => setProposal(event.target.value)}
              />
            </div>

            <div className="field">
              <label htmlFor="expectedBudget">Expected budget (USD) *</label>
              <input
                id="expectedBudget"
                type="number"
                required
                min={0}
                step="0.01"
                value={budget}
                onChange={(event) => setBudget(event.target.value)}
              />
            </div>

            <div className="field">
              <label htmlFor="estimatedDuration">Estimated duration *</label>
              <input
                id="estimatedDuration"
                required
                placeholder="e.g. 3 weeks"
                value={duration}
                onChange={(event) => setDuration(event.target.value)}
              />
            </div>

            <div>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Submitting…' : 'Submit application'}
              </button>
            </div>
            </div>
          </form>
        )}
      </section>

      <section style={{ marginTop: '2rem' }}>
        <div className="section-head">
          <h2>My applications</h2>
          <span className="badge badge-ai">{applications.length}</span>
        </div>
        {applications.length === 0 ? (
          <div className="empty">
            <span className="empty-icon" aria-hidden="true">📨</span>
            <span className="empty-title">No applications yet</span>
            <p className="empty-desc">
              You have not applied to any project yet. Find an opportunity above
              and submit your first proposal.
            </p>
          </div>
        ) : (
          <div className="card-grid stagger">
            {applications.map((application) => (
              <article className="card" key={application.id}>
                <p>
                  <strong>Project:</strong>{' '}
                  <Link to={`/projects/${application.projectId}`}>
                    {application.projectId.slice(0, 8)}
                  </Link>
                </p>
                <p className="muted">
                  ${Number(application.expectedBudget).toLocaleString()} ·{' '}
                  {application.estimatedDuration}
                </p>
                <p className="muted" style={{ fontSize: '0.85rem' }}>
                  {application.proposal.slice(0, 140)}
                  {application.proposal.length > 140 ? '…' : ''}
                </p>
                {statusBadge(application.status)}
                {application.status === 'PENDING' && (
                  <div className="card-actions">
                    <button
                      type="button"
                      className="btn btn-sm"
                      onClick={() => void handleWithdraw(application.id)}
                    >
                      Withdraw
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </>
  )
}

/* ------------------------------- Creator -------------------------------- */

function CreatorHiring() {
  const [searchParams] = useSearchParams()
  const requestedProject = searchParams.get('project') ?? ''
  const [myProjects, setMyProjects] = useState<ProjectResponse[]>([])
  const [selectedProject, setSelectedProject] = useState('')
  const [applications, setApplications] = useState<ApplicationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadProjects = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const projects = await listMyProjects()
      setMyProjects(projects)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load your projects.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadProjects()
  }, [loadProjects])

  // Preselect a project from the ?project= deep link (e.g. the project detail
  // page's "Manage in Hiring" / "View Applicants" action).
  useEffect(() => {
    if (myProjects.length === 0 || !requestedProject || selectedProject) return
    if (!myProjects.some((p) => p.id === requestedProject)) return
    setSelectedProject(requestedProject)
    setError(null)
    getProjectApplications(requestedProject)
      .then((page) => setApplications(page.content))
      .catch((err) => {
        setError(
          err instanceof Error ? err.message : 'Failed to load applications.',
        )
        setApplications([])
      })
  }, [myProjects, requestedProject, selectedProject])

  const handleProjectChange = async (projectId: string) => {
    setSelectedProject(projectId)
    setError(null)
    if (!projectId) {
      setApplications([])
      return
    }
    try {
      const page = await getProjectApplications(projectId)
      setApplications(page.content)
    } catch (err) {
      setError(
        err instanceof Error ? err.message : 'Failed to load applications.',
      )
      setApplications([])
    }
  }

  const handleDecision = async (applicationId: string, status: 'ACCEPTED' | 'REJECTED') => {
    if (status === 'REJECTED') {
      const confirmed = await swalConfirm(
        'Reject application?',
        'Are you sure you want to reject this application?',
        'Reject',
      )
      if (!confirmed) return
    }
    setBusy(true)
    setError(null)
    try {
      await updateApplicationStatus(applicationId, status)
      if (status === 'ACCEPTED') {
        void swalSuccess(
          'Application Accepted',
          'The freelancer has been selected for this project.',
        )
      } else {
        void swalSuccess('Application Rejected', 'The application has been rejected.')
      }
      await handleProjectChange(selectedProject)
    } catch (err) {
      void swalError('Error', err instanceof Error ? err.message : 'Failed to update application.')
      setError(err instanceof Error ? err.message : 'Failed to update application.')
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return (
      <div className="skeleton-grid" aria-label="Loading your projects">
        {[0, 1, 2].map((item) => (
          <div className="skeleton-card" key={item}>
            <div className="skeleton w40" />
            <div className="skeleton w90" />
            <div className="skeleton w60" />
          </div>
        ))}
      </div>
    )
  }

  return (
    <>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      <section>
        <div className="section-head">
          <h2>Incoming applications</h2>
        </div>
        {myProjects.length === 0 ? (
          <div className="empty">
            <span className="empty-icon" aria-hidden="true">📭</span>
            <span className="empty-title">No projects posted yet</span>
            <p className="empty-desc">
              You have not posted any projects yet. <Link to="/projects/create">Create one</Link>{' '}
              to start receiving applications.
            </p>
          </div>
        ) : (
          <>
            <div className="field" style={{ maxWidth: 480 }}>
              <label htmlFor="myProject">Your project</label>
              <select
                id="myProject"
                value={selectedProject}
                onChange={(event) => void handleProjectChange(event.target.value)}
              >
                <option value="">Select a project…</option>
                {myProjects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.title} ({project.status.replace('_', ' ')})
                  </option>
                ))}
              </select>
            </div>

            {selectedProject && applications.length === 0 && (
              <div className="empty">
                <span className="empty-icon" aria-hidden="true">💬</span>
                <span className="empty-title">No applications yet</span>
                <p className="empty-desc">
                  Nobody has applied to this project yet. Share the listing to
                  attract applicants.
                </p>
              </div>
            )}

            {applications.length > 0 && (
              <div className="card-grid stagger">
                {applications.map((application) => (
                  <article className="card" key={application.id}>
                    <p className="muted">
                      Freelancer {application.freelancerId.slice(0, 8)} · $
                      {Number(application.expectedBudget).toLocaleString()} ·{' '}
                      {application.estimatedDuration}
                    </p>
                    <p style={{ fontSize: '0.9rem' }}>{application.proposal}</p>
                    {statusBadge(application.status)}
                    {application.status === 'PENDING' && (
                      <div className="card-actions">
                        <button
                          type="button"
                          className="btn btn-sm"
                          disabled={busy}
                          onClick={() =>
                            void handleDecision(application.id, 'ACCEPTED')
                          }
                        >
                          Accept
                        </button>
                        <button
                          type="button"
                          className="btn btn-sm"
                          disabled={busy}
                          onClick={() =>
                            void handleDecision(application.id, 'REJECTED')
                          }
                        >
                          Reject
                        </button>
                      </div>
                    )}
                  </article>
                ))}
              </div>
            )}
          </>
        )}
      </section>
    </>
  )
}
