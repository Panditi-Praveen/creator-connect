import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  deleteProject,
  getProject,
  updateProject,
  updateProjectStatus,
} from '../api/projects'
import { ApiError } from '../api/client'
import type { ProjectResponse, ProjectStatus } from '../types/api'
import { useAuth } from '../hooks/useAuth'

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
    if (!window.confirm('Delete this project permanently?')) return
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
      <p>
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

      <ul className="detail-list">
        <li>
          <strong>Budget:</strong> ${Number(project.budget).toLocaleString()}
        </li>
        <li>
          <strong>Duration:</strong> {project.duration}
        </li>
        <li>
          <strong>Experience level:</strong> {project.experienceLevel}
        </li>
        <li>
          <strong>Location:</strong> {project.location ?? 'Remote'}
        </li>
        <li>
          <strong>Application deadline:</strong> {project.applicationDeadline}
        </li>
        {project.skillsRequired && project.skillsRequired.length > 0 && (
          <li>
            <strong>Skills:</strong> {project.skillsRequired.join(', ')}
          </li>
        )}
        {project.ownerProfile && (
          <li>
            <strong>Owner:</strong> {project.ownerProfile.firstName}{' '}
            {project.ownerProfile.lastName}
            {project.ownerProfile.headline ? ` — ${project.ownerProfile.headline}` : ''}
          </li>
        )}
      </ul>

      <h2>Description</h2>
      <p>{project.description}</p>

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
        </section>
      )}
    </main>
  )
}
