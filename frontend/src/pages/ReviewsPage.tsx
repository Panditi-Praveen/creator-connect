import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import {
  getFreelancerReviews,
  getProjectApplications,
  submitReview,
} from '../api/hiring'
import { listMyProjects } from '../api/projects'
import type {
  FreelancerReviewsResponse,
  ProjectResponse,
  ReviewResponse,
} from '../types/api'
import { useAuth } from '../hooks/useAuth'

export default function ReviewsPage() {
  const { user } = useAuth()
  const isFreelancer = user?.role === 'FREELANCER'

  return (
    <main className="page">
      <header className="page-header" style={{ marginBottom: '1.25rem' }}>
        <div>
          <h1>Reviews</h1>
          <p className="sub">
            {isFreelancer
              ? 'Your reputation — ratings and feedback from creators you worked with.'
              : 'Leave feedback for freelancers after a completed project.'}
          </p>
        </div>
      </header>
      {isFreelancer ? <FreelancerReviews /> : <CreatorReviews />}
    </main>
  )
}

function StarRating({ rating, size = '1rem' }: { rating: number; size?: string }) {
  return (
    <span
      className="stars"
      style={{ fontSize: size, color: '#f59e0b', letterSpacing: '0.1em' }}
      aria-label={`${rating} out of 5 stars`}
    >
      {'★'.repeat(Math.max(0, Math.min(5, Math.round(rating))))}
      <span style={{ color: 'var(--ink-300)' }}>
        {'★'.repeat(Math.max(0, 5 - Math.min(5, Math.round(rating))))}
      </span>
    </span>
  )
}

function ReviewCard({ review }: { review: ReviewResponse }) {
  return (
    <article className="card">
      <div className="project-card-top">
        <StarRating rating={review.rating} size="1.1rem" />
        <span className="budget">{review.rating}.0</span>
      </div>
      {review.reviewText && <p style={{ fontSize: '0.95rem', lineHeight: 1.6 }}>{review.reviewText}</p>}
      <p className="muted" style={{ fontSize: '0.82rem', marginTop: 'auto' }}>
        Reviewed {review.createdAt.slice(0, 10)} · project {review.projectId.slice(0, 8)}
      </p>
    </article>
  )
}

/* ------------------------------ Freelancer ------------------------------ */

function FreelancerReviews() {
  const { user } = useAuth()
  const [summary, setSummary] = useState<FreelancerReviewsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!user) return
    setLoading(true)
    setError(null)
    try {
      setSummary(await getFreelancerReviews(user.userId))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load reviews.')
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    void load()
  }, [load])

  if (loading) {
    return (
      <div className="skeleton-grid" aria-label="Loading reviews">
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

  if (error) {
    return (
      <p className="form-error" role="alert">
        {error}
      </p>
    )
  }

  if (!summary) return null

  return (
    <section>
      <div className="stat-grid" style={{ marginBottom: '1.25rem' }}>
        <div className="stat-card">
          <span className="stat-icon" aria-hidden="true">⭐</span>
          <div>
            <div className="stat-value">{summary.averageRating.toFixed(1)} / 5</div>
            <div className="stat-label">Average rating</div>
            <StarRating rating={summary.averageRating} size="0.95rem" />
          </div>
        </div>
        <div className="stat-card">
          <span className="stat-icon" aria-hidden="true">💬</span>
          <div>
            <div className="stat-value">{summary.totalReviews}</div>
            <div className="stat-label">
              {summary.totalReviews === 1 ? 'Review' : 'Reviews'}
            </div>
            <div className="stat-hint">from completed projects</div>
          </div>
        </div>
      </div>
      {summary.reviews.length === 0 ? (
        <div className="empty">
          <span className="empty-icon" aria-hidden="true">🌟</span>
          <span className="empty-title">No reviews yet</span>
          <p className="empty-desc">
            You do not have any reviews yet. Reviews appear after creators
            complete projects with you.
          </p>
        </div>
      ) : (
        <div className="card-grid stagger">
          {summary.reviews.map((review) => (
            <ReviewCard key={review.id} review={review} />
          ))}
        </div>
      )}
    </section>
  )
}

/* -------------------------------- Creator ------------------------------- */

function CreatorReviews() {
  const [projects, setProjects] = useState<ProjectResponse[]>([])
  const [selectedProject, setSelectedProject] = useState('')
  const [freelancerId, setFreelancerId] = useState('')
  const [rating, setRating] = useState('5')
  const [reviewText, setReviewText] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const mine = await listMyProjects()
      setProjects(mine.filter((p) => p.status === 'COMPLETED'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load projects.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  // Resolve the hired (ACCEPTED) freelancer for the selected completed project.
  const handleProjectChange = async (projectId: string) => {
    setSelectedProject(projectId)
    setFreelancerId('')
    setError(null)
    setSuccess(null)
    if (!projectId) return
    try {
      const page = await getProjectApplications(projectId)
      const accepted = page.content.find((app) => app.status === 'ACCEPTED')
      if (accepted) {
        setFreelancerId(accepted.freelancerId)
      }
    } catch (err) {
      setError(
        err instanceof Error ? err.message : 'Failed to load applications.',
      )
    }
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!selectedProject || !freelancerId) return
    setSubmitting(true)
    setError(null)
    setSuccess(null)
    try {
      await submitReview({
        projectId: selectedProject,
        freelancerId,
        rating: Number(rating),
        reviewText: reviewText.trim() || undefined,
      })
      setReviewText('')
      setSuccess('Review submitted successfully.')
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Failed to submit review.',
      )
    } finally {
      setSubmitting(false)
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
    <section>
      <div className="form-card" style={{ marginBottom: '1.25rem' }}>
        <p className="muted" style={{ margin: 0 }}>
          Review a freelancer you hired. A project must be{' '}
          <strong>completed</strong> and the freelancer's application{' '}
          <strong>accepted</strong> before a review can be submitted.
        </p>
      </div>

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

      {projects.length === 0 ? (
        <div className="empty">
          <span className="empty-icon" aria-hidden="true">✅</span>
          <span className="empty-title">No completed projects yet</span>
          <p className="empty-desc">
            You have no completed projects to review yet.{' '}
            <Link to="/projects">Go to projects</Link> and mark a project as
            completed to unlock reviews.
          </p>
        </div>
      ) : (
        <form className="form" onSubmit={handleSubmit}>
          <div className="form-card">
          <div className="field">
            <label htmlFor="reviewProject">Completed project</label>
            <select
              id="reviewProject"
              value={selectedProject}
              onChange={(event) => void handleProjectChange(event.target.value)}
            >
              <option value="">Select a completed project…</option>
              {projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.title}
                </option>
              ))}
            </select>
          </div>

          {selectedProject && !freelancerId && (
            <p className="muted">
              No accepted application found for this project — a review can only
              be submitted for a freelancer whose application was accepted.
            </p>
          )}

          {freelancerId && (
            <>
              <div className="field">
                <label htmlFor="rating">Rating *</label>
                <select
                  id="rating"
                  value={rating}
                  onChange={(event) => setRating(event.target.value)}
                >
                  <option value="5">5 - Excellent</option>
                  <option value="4">4 - Good</option>
                  <option value="3">3 - Average</option>
                  <option value="2">2 - Below average</option>
                  <option value="1">1 - Poor</option>
                </select>
              </div>

              <div className="field">
                <label htmlFor="reviewText">Review text</label>
                <textarea
                  id="reviewText"
                  rows={4}
                  maxLength={2000}
                  value={reviewText}
                  onChange={(event) => setReviewText(event.target.value)}
                />
              </div>

              <div>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submitting}
                >
                  {submitting ? 'Submitting…' : 'Submit review'}
                </button>
              </div>
            </>
          )}
          </div>
        </form>
      )}
    </section>
  )
}
