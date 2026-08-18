import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { getMyApplications, getFreelancerReviews } from '../api/hiring'
import { listMyProjects } from '../api/projects'
import { getMyProfile } from '../api/profile'
import { getNotifications } from '../api/notifications'
import { ApiError } from '../api/client'
import type { ProfileResponse, ProjectResponse } from '../types/api'
import type { NotificationResponse } from '../types/notifications'

function displayName(userName: string | undefined, email: string | undefined): string {
  if (userName && userName.trim()) return userName.trim().split(/\s+/)[0]
  return email?.split('@')[0] ?? 'there'
}

/** Completion = filled fields / (required + optional) fields on the profile. */
function completionPercent(profile: ProfileResponse | null): number {
  if (!profile) return 0
  const fields: Array<keyof ProfileResponse> = [
    'firstName', 'lastName', 'headline', 'bio', 'location', 'website',
    'linkedin', 'github', 'skills', 'experience',
  ]
  const filled = fields.filter(
    (field) => {
      const value = profile[field]
      return value !== undefined && value !== null && value !== ''
    },
  ).length
  return Math.round((filled / fields.length) * 100)
}

export default function DashboardPage() {
  const { user } = useAuth()
  const [myProjects, setMyProjects] = useState<ProjectResponse[]>([])
  const [applicationCount, setApplicationCount] = useState(0)
  const [profile, setProfile] = useState<ProfileResponse | null>(null)
  const [reviews, setReviews] = useState<{ average: number; count: number } | null>(null)
  const [recentActivity, setRecentActivity] = useState<NotificationResponse[]>([])
  const [loaded, setLoaded] = useState(false)

  const load = useCallback(async () => {
    const isFreelancer = user?.role === 'FREELANCER'
    const results = await Promise.allSettled([
      listMyProjects(),
      getMyApplications(),
      getMyProfile(),
      isFreelancer && user?.userId
        ? getFreelancerReviews(user.userId)
        : Promise.resolve(null),
      getNotifications(false),
    ])
    if (results[0].status === 'fulfilled') setMyProjects(results[0].value)
    if (results[1].status === 'fulfilled') {
      setApplicationCount(results[1].value.totalElements ?? results[1].value.content.length)
    }
    if (results[2].status === 'fulfilled') setProfile(results[2].value)
    else if (
      results[2].status === 'rejected' &&
      !(results[2].reason instanceof ApiError && results[2].reason.status === 404)
    ) {
      // Non-404 profile failures fall through silently; the dashboard stays usable.
    }
    if (results[3]?.status === 'fulfilled' && results[3].value) {
      setReviews({
        average: results[3].value.averageRating,
        count: results[3].value.totalReviews,
      })
    }
    if (results[4]?.status === 'fulfilled' && results[4].value) {
      setRecentActivity(results[4].value.content?.slice(0, 5) ?? [])
    }
    setLoaded(true)
  }, [user])

  useEffect(() => {
    void load()
  }, [load])

  const isFreelancer = user?.role === 'FREELANCER'
  const completion = completionPercent(profile)

  return (
    <main className="page">
      <section className="welcome">
        <div>
          <h1>Welcome back, {displayName(user?.email, user?.email)} 👋</h1>
          <p className="sub">
            {isFreelancer
              ? 'Find your next opportunity and manage your applications.'
              : 'Post projects, review applicants, and discover top talent.'}
          </p>
        </div>
        <Link to={isFreelancer ? '/projects' : '/projects/create'} className="btn btn-primary">
          {isFreelancer ? 'Browse projects' : 'Create project'}
        </Link>
      </section>

      <section className="stat-grid">
        <Link to="/projects" className="stat-card stat-card-link">
          <span className="stat-icon" aria-hidden="true">🗂️</span>
          <div>
            <div className="stat-value">{loaded ? myProjects.length : '—'}</div>
            <div className="stat-label">My projects</div>
            <div className="stat-hint">{isFreelancer ? 'Marketplace listings' : 'Projects you posted'}</div>
          </div>
        </Link>

        <Link to={isFreelancer ? '/applications' : '/hiring'} className="stat-card stat-card-link">
          <span className="stat-icon" aria-hidden="true">📨</span>
          <div>
            <div className="stat-value">{loaded ? applicationCount : '—'}</div>
            <div className="stat-label">Applications</div>
            <div className="stat-hint">
              {isFreelancer ? 'Your proposals' : 'Incoming for your projects'}
            </div>
          </div>
        </Link>

        <Link to="/profile" className="stat-card stat-card-link">
          <span className="stat-icon" aria-hidden="true">👤</span>
          <div>
            <div className="stat-value">{completion}%</div>
            <div className="stat-label">Profile completion</div>
            <div className="progress-track" style={{ marginTop: 6 }}>
              <div className="progress-fill" style={{ width: `${completion}%` }} />
            </div>
          </div>
        </Link>

        {isFreelancer ? (
          <Link to="/reviews" className="stat-card stat-card-link">
            <span className="stat-icon" aria-hidden="true">⭐</span>
            <div>
              <div className="stat-value">
                {reviews ? reviews.average.toFixed(1) : '—'}
              </div>
              <div className="stat-label">Average rating</div>
              <div className="stat-hint">
                {reviews ? `${reviews.count} review${reviews.count === 1 ? '' : 's'}` : 'No reviews yet'}
              </div>
            </div>
          </Link>
        ) : (
          <Link to="/ai" className="stat-card stat-card-link">
            <span className="stat-icon" aria-hidden="true">✨</span>
            <div>
              <div className="stat-value">AI</div>
              <div className="stat-label">Talent discovery</div>
              <div className="stat-hint">Describe talent in plain language</div>
            </div>
          </Link>
        )}
      </section>

      <section className="section">
        <div className="section-head">
          <h2>Quick actions</h2>
        </div>
        <div className="quick-actions">
          <Link to="/projects" className="quick-action">
            <span className="qa-icon" aria-hidden="true">🔎</span>
            <span className="qa-text">
              Browse projects
              <small>Explore opportunities across categories</small>
            </span>
            <span className="qa-arrow" aria-hidden="true">→</span>
          </Link>
          <Link to="/projects/create" className="quick-action">
            <span className="qa-icon" aria-hidden="true">➕</span>
            <span className="qa-text">
              Create project
              <small>Post a project and invite applications</small>
            </span>
            <span className="qa-arrow" aria-hidden="true">→</span>
          </Link>
          <Link
            to={isFreelancer ? '/applications' : '/hiring'}
            className="quick-action"
          >
            <span className="qa-icon" aria-hidden="true">🤝</span>
            <span className="qa-text">
              {isFreelancer ? 'My Applications' : 'Hiring'}
              <small>{isFreelancer ? 'Track your proposals' : 'Review incoming applications'}</small>
            </span>
            <span className="qa-arrow" aria-hidden="true">→</span>
          </Link>
          <Link to="/ai" className="quick-action">
            <span className="qa-icon" aria-hidden="true">✨</span>
            <span className="qa-text">
              AI discovery
              <small>Find matching talent with natural language</small>
            </span>
            <span className="qa-arrow" aria-hidden="true">→</span>
          </Link>
        </div>
      </section>

      {myProjects.length > 0 && (
        <section className="section">
          <div className="section-head">
            <h2>Recent projects</h2>
            <Link to="/projects" className="btn btn-sm btn-ghost">
              View all
            </Link>
          </div>
          <div className="card-grid stagger">
            {myProjects.slice(0, 3).map((project) => (
              <article className="card" key={project.id}>
                <div className="project-card-top">
                  <h3>
                    <Link to={`/projects/${project.id}`}>{project.title}</Link>
                  </h3>
                  <span className={`badge badge-${project.status.toLowerCase()}`}>
                    {project.status.replace('_', ' ')}
                  </span>
                </div>
                <p className="muted">
                  {project.category} · {project.duration} · {project.experienceLevel}
                </p>
                <p className="budget">
                  ${Number(project.budget).toLocaleString()}{' '}
                  <span>budget</span>
                </p>
              </article>
            ))}
          </div>
        </section>
      )}

      {recentActivity.length > 0 && (
        <section className="section">
          <div className="section-head">
            <h2>Recent activity</h2>
            <Link to="/notifications" className="btn btn-sm btn-ghost">
              View all
            </Link>
          </div>
          <div className="activity-feed">
            {recentActivity.map((n) => (
              <Link
                key={n.id}
                to={n.relatedResourceType === 'APPLICATION' ? '/applications' : '/projects'}
                className="activity-item"
              >
                <span className="activity-icon" aria-hidden="true">
                  {n.type === 'APPLICATION_RECEIVED' && '📩'}
                  {n.type === 'APPLICATION_ACCEPTED' && '✅'}
                  {n.type === 'APPLICATION_REJECTED' && '❌'}
                  {n.type === 'APPLICATION_WITHDRAWN' && '↩️'}
                  {n.type === 'REVIEW_RECEIVED' && '⭐'}
                </span>
                <span className="activity-body">
                  <span className="activity-title">{n.title}</span>
                  <span className="activity-time">
                    {new Date(n.createdAt).toLocaleDateString()}
                  </span>
                </span>
              </Link>
            ))}
          </div>
        </section>
      )}
    </main>
  )
}
