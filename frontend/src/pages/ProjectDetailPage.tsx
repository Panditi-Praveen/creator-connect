import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  deleteProject,
  getProject,
  updateProject,
  updateProjectStatus,
} from '../api/projects'
import {
  applyToProject,
  getMyApplications,
  getProjectApplications,
  updateApplicationStatus,
} from '../api/hiring'
import { ApiError } from '../api/client'
import type {
  ApplicationResponse,
  ProjectResponse,
  ProjectStatus,
} from '../types/api'
import { useAuth } from '../hooks/useAuth'
import { swalConfirm, swalError, swalSuccess } from '../utils/notify'

const NEXT_TRANSITIONS: Record<ProjectStatus, ProjectStatus[]> = {
  OPEN: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
}

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [project, setProject] = useState<ProjectResponse | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const [editing, setEditing] = useState(false)
  const [editTitle, setEditTitle] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [editBudget, setEditBudget] = useState('')

  // Freelancer apply flow
  const [applied, setApplied] = useState(false)
  const [applyOpen, setApplyOpen] = useState(false)
  const [proposal, setProposal] = useState('')
  const [applyBudget, setApplyBudget] = useState('')
  const [applyDuration, setApplyDuration] = useState('')
  const [applying, setApplying] = useState(false)
  const [applyError, setApplyError] = useState<string | null>(null)

  // Creator applicant management
  const [applicants, setApplicants] = useState<ApplicationResponse[] | null>(null)
  const [applicantsError, setApplicantsError] = useState<string | null>(null)
  const [deciding, setDeciding] = useState<string | null>(null)
  const [decideError, setDecideError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!id) return
    setLoading(true)
    setError(null)
    try {
      const result = await getProject(id)
      setProject(result)
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setNotFound(true)
      } else {
        setError(
          err instanceof Error ? err.message : 'Failed to load project.',
        )
      }
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    void load()
  }, [load])

  const isOwner = project?.userId === user?.userId

  // Determine whether the current freelancer already applied (single request,
  // never N+1). Unknown state just leaves the Apply action available.
  useEffect(() => {
    if (!project || user?.role !== 'FREELANCER') return
    let cancelled = false
    getMyApplications()
      .then((page) => {
        if (!cancelled) {
          setApplied(page.content.some((a) => a.projectId === project.id))
        }
      })
      .catch(() => {
        /* keep unknown */
      })
    return () => {
      cancelled = true
    }
  }, [project, user?.role])

  // Creator owner: load this project's incoming applications.
  useEffect(() => {
    if (!project || !isOwner || user?.role !== 'CREATOR') return
    let cancelled = false
    getProjectApplications(project.id)
      .then((page) => {
        if (!cancelled) setApplicants(page.content)
      })
      .catch((err) => {
        if (!cancelled) {
          setApplicants([])
          setApplicantsError(
            err instanceof Error ? err.message : 'Failed to load applicants.',
          )
        }
      })
    return () => {
      cancelled = true
    }
  }, [project, isOwner, user?.role])

  const handleStatus = async (status: ProjectStatus) => {
    if (!project) return
    setBusy(true)
    setError(null)
    try {
      const updated = await updateProjectStatus(project.id, status)
      setProject(updated)
    } catch (err) {
      setError(
        err instanceof Error ? err.message : 'Failed to update status.',
      )
    } finally {
      setBusy(false)
    }
  }

  const handleDelete = async () => {
    if (!project) return
    const confirmed = await swalConfirm(
      'Delete project?',
      'This action cannot be undone. Are you sure you want to permanently delete this project?',
      'Delete',
    )
    if (!confirmed) return
    setBusy(true)
    setError(null)
    try {
      await deleteProject(project.id)
      navigate('/projects', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete project.')
      setBusy(false)
    }
  }

  const startEdit = () => {
    if (!project) return
    setEditTitle(project.title)
    setEditDescription(project.description)
    setEditBudget(project.budget?.toString() ?? '')
    setEditing(true)
  }

  const handleEdit = async (event: FormEvent) => {
    event.preventDefault()
    if (!project) return
    setBusy(true)
    setError(null)
    try {
      const updated = await updateProject(project.id, {
        title: editTitle.trim(),
        description: editDescription.trim(),
        budget: Number(editBudget),
      })
      setProject(updated)
      setEditing(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update project.')
    } finally {
      setBusy(false)
    }
  }

  const handleApply = async (event: FormEvent) => {
    event.preventDefault()
    if (!project) return
    setApplying(true)
    setApplyError(null)
    try {
      await applyToProject({
        projectId: project.id,
        proposal: proposal.trim(),
        expectedBudget: Number(applyBudget),
        estimatedDuration: applyDuration.trim(),
      })
      setApplied(true)
      setApplyOpen(false)
      setProposal('')
      setApplyBudget('')
      setApplyDuration('')
      const result = await swalSuccess(
        'Application Submitted!',
        'Your application has been submitted successfully.',
        'View My Applications',
      )
      if (result.isConfirmed) navigate('/applications')
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          // Already applied — reflect the true state and inform the user.
          setApplied(true)
          setApplyOpen(false)
          void swalError('Already applied', 'You have already applied to this project.')
        } else if (err.status === 401) {
          setApplyError('Please sign in to apply.')
        } else if (err.status === 404) {
          setApplyError('Project not found.')
        } else if (err.status >= 500) {
          setApplyError('Something went wrong. Please try again.')
        } else {
          // 400 and any other status — show the backend's own message.
          setApplyError(err.message)
        }
      } else {
        setApplyError('Something went wrong. Please try again.')
      }
    } finally {
      setApplying(false)
    }
  }

  const handleDecision = async (
    applicationId: string,
    status: 'ACCEPTED' | 'REJECTED',
  ) => {
    if (status === 'REJECTED') {
      const confirmed = await swalConfirm(
        'Reject application?',
        'Are you sure you want to reject this application?',
        'Reject',
      )
      if (!confirmed) return
    }
    setDeciding(applicationId)
    setDecideError(null)
    try {
      const updated = await updateApplicationStatus(applicationId, status)
      setApplicants((prev) =>
        prev ? prev.map((a) => (a.id === applicationId ? updated : a)) : prev,
      )
      if (status === 'ACCEPTED') {
        void swalSuccess(
          'Application Accepted',
          'The freelancer has been selected for this project.',
        )
        // Accepting moves the project to IN_PROGRESS via Feign — refresh it.
        void load()
      } else {
        void swalSuccess('Application Rejected', 'The application has been rejected.')
      }
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 403) {
          setDecideError("You don't have permission to manage this application.")
        } else if (err.status === 404) {
          setDecideError('Application no longer exists.')
        } else if (err.status === 409) {
          setDecideError('This application has already been processed.')
          // Re-sync statuses from the backend.
          try {
            if (project) {
              const page = await getProjectApplications(project.id)
              setApplicants(page.content)
            }
          } catch {
            /* keep current list */
          }
        } else if (err.status === 400) {
          setDecideError(err.message)
        } else if (err.status === 401) {
          setDecideError('Your session has expired. Please sign in again.')
        } else {
          setDecideError('Something went wrong. Please try again.')
        }
      } else {
        setDecideError('Something went wrong. Please try again.')
      }
    } finally {
      setDeciding(null)
    }
  }

  if (loading) {
    return (
      <main className="page">
        <h1>Project</h1>
        <p className="loading">Loading project…</p>
      </main>
    )
  }

  if (notFound) {
    return (
      <main className="page">
        <h1>Project not found</h1>
        <p>
          <Link to="/projects">← Back to projects</Link>
        </p>
      </main>
    )
  }

  if (!project) {
    return (
      <main className="page">
        <h1>Project</h1>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <p>
          <Link to="/projects">← Back to projects</Link>
        </p>
      </main>
    )
  }

  const transitions = NEXT_TRANSITIONS[project.status] ?? []

  return (
    <main className="page">
      <p style={{ marginTop: 0 }}>
        <Link to="/projects">← Back to projects</Link>
      </p>

      <header className="page-header">
        <div>
          <h1 style={{ margin: 0 }}>{project.title}</h1>
          <p className="muted">
            {project.category} · posted {project.createdAt.slice(0, 10)}
          </p>
        </div>
        <span className={`badge badge-${project.status.toLowerCase()}`}>
          {project.status.replace('_', ' ')}
        </span>
      </header>

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      <div className="meta-card">
        <div className="meta-cell">
          <div className="mc-label">Budget</div>
          <div className="mc-value">${Number(project.budget).toLocaleString()}</div>
        </div>
        <div className="meta-cell">
          <div className="mc-label">Duration</div>
          <div className="mc-value">{project.duration}</div>
        </div>
        <div className="meta-cell">
          <div className="mc-label">Experience</div>
          <div className="mc-value">{project.experienceLevel}</div>
        </div>
        <div className="meta-cell">
          <div className="mc-label">Location</div>
          <div className="mc-value">{project.location ?? 'Remote'}</div>
        </div>
        <div className="meta-cell">
          <div className="mc-label">Deadline</div>
          <div className="mc-value">{project.applicationDeadline}</div>
        </div>
        {project.ownerProfile && (
          <div className="meta-cell">
            <div className="mc-label">Owner</div>
            <div className="mc-value">
              {project.ownerProfile.firstName} {project.ownerProfile.lastName}
              {project.ownerProfile.headline ? ` — ${project.ownerProfile.headline}` : ''}
            </div>
          </div>
        )}
      </div>

      {project.skillsRequired && project.skillsRequired.length > 0 && (
        <div className="tags" style={{ marginBottom: '1.25rem' }}>
          {project.skillsRequired.map((skill) => (
            <span className="tag" key={skill}>
              {skill}
            </span>
          ))}
        </div>
      )}

      <div className="form-card">
        <h2 style={{ marginTop: 0 }}>Description</h2>
        <p style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>{project.description}</p>
      </div>

      {!isOwner && user?.role === 'FREELANCER' && (
        <section style={{ marginTop: '1.5rem' }}>
          <div className="form-card">
            <div className="section-head">
              <div>
                <h2 style={{ margin: 0 }}>Apply to this project</h2>
                <p className="muted">
                  {applied
                    ? 'You have already applied to this project.'
                    : 'Submit your proposal — it only takes a minute.'}
                </p>
              </div>
              {applied && (
                <Link to="/applications" className="btn btn-sm">
                  View my applications
                </Link>
              )}
            </div>

            {applied ? (
              <p className="form-success" role="status" style={{ margin: 0 }}>
                ✓ Applied
              </p>
            ) : project.status !== 'OPEN' && project.status !== 'IN_PROGRESS' ? (
              <p className="muted" style={{ margin: 0 }}>
                This project is no longer accepting applications.
              </p>
            ) : applyOpen ? (
              <form className="form" onSubmit={handleApply} style={{ marginTop: '0.5rem' }}>
                <div className="field">
                  <label htmlFor="applyProposal">Proposal *</label>
                  <textarea
                    id="applyProposal"
                    required
                    rows={4}
                    maxLength={5000}
                    placeholder="Why are you a great fit for this project?"
                    value={proposal}
                    onChange={(event) => setProposal(event.target.value)}
                  />
                </div>
                <div className="form-grid">
                  <div className="field">
                    <label htmlFor="applyBudget">Expected budget (USD) *</label>
                    <input
                      id="applyBudget"
                      type="number"
                      required
                      min={0}
                      step="0.01"
                      value={applyBudget}
                      onChange={(event) => setApplyBudget(event.target.value)}
                    />
                  </div>
                  <div className="field">
                    <label htmlFor="applyDuration">Estimated duration *</label>
                    <input
                      id="applyDuration"
                      required
                      placeholder="e.g. 3 weeks"
                      value={applyDuration}
                      onChange={(event) => setApplyDuration(event.target.value)}
                    />
                  </div>
                </div>
                {applyError && (
                  <p className="form-error" role="alert">
                    {applyError}
                  </p>
                )}
                <div className="card-actions">
                  <button type="submit" className="btn btn-primary" disabled={applying}>
                    {applying ? 'Applying…' : 'Submit application'}
                  </button>
                  <button
                    type="button"
                    className="btn"
                    disabled={applying}
                    onClick={() => {
                      setApplyOpen(false)
                      setApplyError(null)
                    }}
                  >
                    Cancel
                  </button>
                </div>
              </form>
            ) : (
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => setApplyOpen(true)}
              >
                Apply Now
              </button>
            )}
          </div>
        </section>
      )}

      {isOwner && (
        <section>
          <div className="card-actions">
            {transitions.length > 0 && (
              <>
                <span className="muted" style={{ alignSelf: 'center' }}>
                  Update status:
                </span>
                {transitions.map((status) => (
                  <button
                    key={status}
                    type="button"
                    className="btn"
                    disabled={busy}
                    onClick={() => void handleStatus(status)}
                  >
                    Mark {status.replace('_', ' ').toLowerCase()}
                  </button>
                ))}
              </>
            )}
            {!editing && (
              <button type="button" className="btn" onClick={startEdit}>
                Edit
              </button>
            )}
            <button
              type="button"
              className="btn btn-danger"
              disabled={busy}
              onClick={() => void handleDelete()}
            >
              Delete
            </button>
          </div>

          {editing && (
            <form className="form" onSubmit={handleEdit} style={{ marginTop: '1rem' }}>
              <div className="field">
                <label htmlFor="editTitle">Title</label>
                <input
                  id="editTitle"
                  value={editTitle}
                  onChange={(event) => setEditTitle(event.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="editDescription">Description</label>
                <textarea
                  id="editDescription"
                  rows={4}
                  value={editDescription}
                  onChange={(event) => setEditDescription(event.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="editBudget">Budget (USD)</label>
                <input
                  id="editBudget"
                  type="number"
                  min={0}
                  step="0.01"
                  value={editBudget}
                  onChange={(event) => setEditBudget(event.target.value)}
                />
              </div>
              <div className="card-actions">
                <button type="submit" className="btn btn-primary" disabled={busy}>
                  Save changes
                </button>
                <button type="button" className="btn" onClick={() => setEditing(false)}>
                  Cancel
                </button>
              </div>
            </form>
          )}

          <div className="section-head" style={{ marginTop: '2rem' }}>
            <div>
              <h2 style={{ margin: 0 }}>Applicants</h2>
              <p className="muted">Freelancers who applied to this project.</p>
            </div>
            <Link to={`/hiring?project=${project.id}`} className="btn btn-sm">
              Manage in Hiring
            </Link>
          </div>

          {decideError && (
            <p className="form-error" role="alert">
              {decideError}
            </p>
          )}
          {applicantsError && (
            <p className="form-error" role="alert">
              {applicantsError}
            </p>
          )}

          {applicants === null ? (
            <div className="skeleton-grid" aria-label="Loading applicants">
              {[0, 1].map((item) => (
                <div className="skeleton-card" key={item}>
                  <div className="skeleton w40" />
                  <div className="skeleton w90" />
                  <div className="skeleton w60" />
                </div>
              ))}
            </div>
          ) : applicants.length === 0 ? (
            <div className="empty">
              <span className="empty-icon" aria-hidden="true">💬</span>
              <span className="empty-title">No applicants yet</span>
              <p className="empty-desc">
                Freelancer applications for this project will appear here.
              </p>
            </div>
          ) : (
            <div className="card-grid stagger">
              {applicants.map((application) => (
                <article className="card" key={application.id}>
                  <div className="project-card-top">
                    <span className={`badge badge-${application.status.toLowerCase()}`}>
                      {application.status}
                    </span>
                    <span className="muted" style={{ fontSize: '0.8rem' }}>
                      Freelancer {application.freelancerId.slice(0, 8)}
                    </span>
                  </div>
                  <p style={{ fontSize: '0.92rem', lineHeight: 1.55 }}>{application.proposal}</p>
                  <div className="meta-row">
                    <span className="meta-item">
                      💰 ${Number(application.expectedBudget).toLocaleString()}
                    </span>
                    <span className="meta-item">⏱️ {application.estimatedDuration}</span>
                    <span className="meta-item">📅 {application.createdAt.slice(0, 10)}</span>
                  </div>
                  {application.status === 'PENDING' && (
                    <div className="card-actions">
                      <button
                        type="button"
                        className="btn btn-sm"
                        disabled={deciding === application.id}
                        onClick={() => void handleDecision(application.id, 'ACCEPTED')}
                      >
                        {deciding === application.id ? 'Accepting…' : 'Accept'}
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm"
                        disabled={deciding === application.id}
                        onClick={() => void handleDecision(application.id, 'REJECTED')}
                      >
                        {deciding === application.id ? 'Rejecting…' : 'Reject'}
                      </button>
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>
      )}
    </main>
  )
}
