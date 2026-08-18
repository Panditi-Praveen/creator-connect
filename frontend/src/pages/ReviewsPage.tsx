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
      <h1>Reviews</h1>
      {isFreelancer ? <FreelancerReviews /> : <CreatorReviews />}
    </main>
  )
}

function StarRating({ rating }: { rating: number }) {
  return <span>{'★'.repeat(rating)}</span>
}

function ReviewCard({ review }: { review: ReviewResponse }) {
  return (
    <article className="card">
      <p>
        <StarRating rating={review.rating} />{' '}
        <span className="muted">
          {review.createdAt.slice(0, 10)} · project {review.projectId.slice(0, 8)}
        </span>
      </p>
      {review.reviewText && <p style={{ fontSize: '0.92rem' }}>{review.reviewText}</p>}
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
    return <p className="loading">Loading reviews…</p>
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
      <p>
        <strong>Average rating:</strong> {summary.averageRating.toFixed(1)} / 5 ·{' '}
        <strong>{summary.totalReviews}</strong>{' '}
        {summary.totalReviews === 1 ? 'review' : 'reviews'}
      </p>
      {summary.reviews.length === 0 ? (
        <div className="empty">
          You do not have any reviews yet. Reviews appear after creators complete
          projects with you.
        </div>
      ) : (
        <div className="card-grid">
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
    return <p className="loading">Loading your projects…</p>
  }

  return (
    <section>
      <p className="muted">
        Review a freelancer you hired. A project must be{' '}
        <strong>completed</strong> and the freelancer's application{' '}
        <strong>accepted</strong> before a review can be submitted.
      </p>

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
          You have no completed projects to review yet.{' '}
          <Link to="/projects">Go to projects</Link>
        </div>
      ) : (
        <form className="form" onSubmit={handleSubmit}>
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
        </form>
      )}
    </section>
  )
}
