import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Footer from '../components/Footer'
import { listProjects } from '../api/projects'
import { ApiError } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import type { ProjectResponse } from '../types/api'

const CATEGORIES = [
  { icon: '💻', name: 'Software Development', desc: 'Web, mobile and backend engineering roles' },
  { icon: '🤖', name: 'AI & Machine Learning', desc: 'AI, ML and data science projects' },
  { icon: '🎨', name: 'UI/UX Design', desc: 'Product, web and app design work' },
  { icon: '🖌️', name: 'Graphic Design', desc: 'Branding, illustration and visual assets' },
  { icon: '🎬', name: 'Video Editing', desc: 'Editing, motion graphics and post-production' },
  { icon: '📈', name: 'Digital Marketing', desc: 'Growth, SEO and social campaigns' },
  { icon: '✍️', name: 'Content Writing', desc: 'Articles, copywriting and documentation' },
  { icon: '📊', name: 'Data Analytics', desc: 'Dashboards, BI and data insights' },
]

const SUGGESTED_SEARCHES = [
  'Java Developer',
  'Spring Boot',
  'React Developer',
  'UI/UX Designer',
  'Video Editor',
  'AI Engineer',
]

const TRUST_FEATURES = [
  {
    icon: '🔎',
    title: 'Discover',
    desc: 'Find projects and talent that match your needs.',
  },
  {
    icon: '🤝',
    title: 'Connect',
    desc: 'Apply, hire and communicate through a structured workflow.',
  },
  {
    icon: '📈',
    title: 'Grow',
    desc: 'Build your profile, complete projects and earn reviews.',
  },
  {
    icon: '✨',
    title: 'AI-Powered Matching',
    desc: 'Use CreatorConnect AI to discover relevant freelancer talent.',
  },
]

function projectsErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status >= 500) return 'Something went wrong while loading projects.'
    if (err.status === 0) return 'Unable to load projects. Please try again.'
    return err.message
  }
  return 'Unable to load projects. Please try again.'
}

export default function HomePage() {
  const { user, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [projects, setProjects] = useState<ProjectResponse[] | null>(null)
  const [projectsError, setProjectsError] = useState<string | null>(null)

  const isFreelancer = user?.role === 'FREELANCER'

  const loadProjects = useCallback(async () => {
    setProjectsError(null)
    try {
      setProjects(await listProjects())
    } catch (err) {
      setProjectsError(projectsErrorMessage(err))
    }
  }, [])

  // Real data only for authenticated users — a public fetch would 401 and the
  // global handler would redirect guests to /login.
  useEffect(() => {
    if (isAuthenticated) void loadProjects()
  }, [isAuthenticated, loadProjects])

  const handleSearch = (event: FormEvent) => {
    event.preventDefault()
    const trimmed = search.trim()
    navigate(trimmed ? `/projects?keyword=${encodeURIComponent(trimmed)}` : '/projects')
  }

  const primaryAction =
    user === null
      ? { to: '/register', label: 'Join CreatorConnect' }
      : isFreelancer
        ? { to: '/projects', label: 'Find Projects' }
        : { to: '/ai', label: 'Find Talent' }

  return (
    <main>
      {/* ---------------- 1. Hero ---------------- */}
      <section className="home-hero">
        <div className="home-hero-inner">
          <div className="home-hero-copy">
            <span className="home-hero-eyebrow">
              ✨ AI-Assisted Creative Talent Marketplace
            </span>
            <h1>
              Find work. Find talent.
              <br />
              <span className="gradient-text">Build together.</span>
            </h1>
            <p className="home-hero-sub">
              CreatorConnect connects skilled freelancers with opportunities and
              helps creators find the right talent for their projects.
            </p>
            <div className="home-hero-actions">
              <Link to={primaryAction.to} className="btn btn-primary btn-lg">
                {primaryAction.label}
              </Link>
              <Link to="/projects" className="btn btn-ghost-light btn-lg">
                Explore Projects
              </Link>
            </div>
          </div>
          <div className="home-hero-visual" aria-hidden="true">
            <div className="float-chip chip-1">🎬 Video Editing</div>
            <div className="float-chip chip-2">💻 Spring Boot</div>
            <div className="float-chip chip-3">🎨 UI/UX Design</div>
            <div className="float-chip chip-4">🤖 AI Engineer</div>
            <div className="hero-card-preview">
              <div className="hcp-head">
                <span className="hcp-icon">🎬</span>
                <span className="badge badge-open">Open</span>
              </div>
              <div className="hcp-title">YouTube channel rebrand</div>
              <div className="hcp-meta">Video Editing · 2 weeks · Remote</div>
              <div className="hcp-foot">
                <span className="budget">$1,200</span>
                <span className="hcp-apply">Apply →</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className="page home-body">
        {/* ---------------- 2. Search ---------------- */}
        <section className="home-section">
          <form className="search-bar home-search" role="search" onSubmit={handleSearch}>
            <span className="search-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <circle cx="11" cy="11" r="7" />
                <path d="m21 21-4.3-4.3" />
              </svg>
            </span>
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search jobs, skills, projects or freelancers..."
              aria-label="Search jobs, skills, projects or freelancers"
            />
            {search.length > 0 && (
              <button
                type="button"
                className="search-clear"
                aria-label="Clear search"
                onClick={() => setSearch('')}
              >
                ✕
              </button>
            )}
            <button type="submit" className="btn btn-primary search-submit">
              Search
            </button>
          </form>
          <div className="suggested-searches">
            <span className="suggested-label">Popular:</span>
            {SUGGESTED_SEARCHES.map((term) => (
              <button
                key={term}
                type="button"
                onClick={() => navigate(`/projects?keyword=${encodeURIComponent(term)}`)}
              >
                {term}
              </button>
            ))}
          </div>
        </section>

        {/* ---------------- 3. Categories ---------------- */}
        <section className="home-section">
          <div className="section-head">
            <div>
              <h2>Explore by category</h2>
              <p className="muted">Browse opportunities across creative and technical fields.</p>
            </div>
          </div>
          <div className="category-grid stagger">
            {CATEGORIES.map((category) => (
              <Link
                key={category.name}
                to={`/projects?keyword=${encodeURIComponent(category.name)}`}
                className="category-card"
              >
                <span className="category-card-icon" aria-hidden="true">
                  {category.icon}
                </span>
                <span className="category-card-name">{category.name}</span>
                <span className="category-card-desc">{category.desc}</span>
              </Link>
            ))}
          </div>
        </section>

        {/* ---------------- 4. Recommended projects ---------------- */}
        <section className="home-section">
          <div className="section-head">
            <div>
              <h2>Recommended Projects</h2>
              <p className="muted">Opportunities that match your interests and skills.</p>
            </div>
            <Link to="/projects" className="btn btn-sm btn-ghost">
              View all
            </Link>
          </div>

          {!isAuthenticated && (
            <div className="empty">
              <span className="empty-icon" aria-hidden="true">🔐</span>
              <span className="empty-title">Sign in to see recommended projects</span>
              <p className="empty-desc">
                Create a free account to browse live opportunities and apply in
                minutes.
              </p>
              <div className="card-actions" style={{ justifyContent: 'center' }}>
                <Link to="/register" className="btn btn-primary">
                  Create account
                </Link>
                <Link to="/login" className="btn">
                  Sign in
                </Link>
              </div>
            </div>
          )}

          {isAuthenticated && projects === null && !projectsError && (
            <div className="card-grid skeleton-grid" aria-label="Loading recommended projects">
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

          {isAuthenticated && projectsError && (
            <p className="form-error" role="alert">
              {projectsError}
            </p>
          )}

          {isAuthenticated && !projectsError && projects !== null && projects.length === 0 && (
            <div className="empty">
              <span className="empty-icon" aria-hidden="true">🗂️</span>
              <span className="empty-title">No projects available yet</span>
              <p className="empty-desc">
                New opportunities will appear here as creators publish projects.
              </p>
              <Link to="/projects" className="btn btn-primary">
                Explore Projects
              </Link>
            </div>
          )}

          {isAuthenticated && !projectsError && projects !== null && projects.length > 0 && (
            <div className="card-grid stagger">
              {projects.slice(0, 6).map((project) => (
                <article className="card" key={project.id}>
                  <div className="project-card-top">
                    <span className={`badge badge-${project.status.toLowerCase()}`}>
                      {project.status.replace('_', ' ')}
                    </span>
                    <span className="category-icon" aria-hidden="true">
                      {project.category.slice(0, 2).toUpperCase()}
                    </span>
                  </div>
                  <h3>
                    <Link to={`/projects/${project.id}`}>{project.title}</Link>
                  </h3>
                  <div className="meta-row">
                    <span className="meta-item">🗂️ {project.category}</span>
                    <span className="meta-item">⏱️ {project.duration}</span>
                    <span className="meta-item">🎯 {project.experienceLevel}</span>
                    <span className="meta-item">📍 {project.location ?? 'Remote'}</span>
                    <span className="meta-item">📅 {project.applicationDeadline}</span>
                  </div>
                  <div className="card-footer">
                    <span className="budget">
                      ${Number(project.budget).toLocaleString()}{' '}
                      <span>budget</span>
                    </span>
                    <Link to={`/projects/${project.id}`} className="btn btn-sm btn-primary">
                      View
                    </Link>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        {/* ---------------- 5. Role-aware experience ---------------- */}
        <section className="home-section">
          <div className="section-head">
            <div>
              <h2>
                {user === null
                  ? 'Built for both sides of the market'
                  : isFreelancer
                    ? 'Recommended for you'
                    : 'Build your next project'}
              </h2>
              <p className="muted">
                {user === null
                  ? 'Whether you create or collaborate, CreatorConnect brings the marketplace together.'
                  : isFreelancer
                    ? 'Find projects, discover talent tools and grow your reputation.'
                    : 'Post work, review applicants and find the right talent.'}
              </p>
            </div>
          </div>
          <div className="role-grid stagger">
            {(user === null
              ? [
                  { icon: '🔎', title: 'Find Projects', desc: 'Browse open opportunities', to: '/projects' },
                  { icon: '✨', title: 'AI Discovery', desc: 'Match talent with natural language', to: '/ai' },
                  { icon: '📨', title: 'Applications', desc: 'Apply and track proposals', to: '/projects' },
                  { icon: '👤', title: 'Your Profile', desc: 'Showcase your skills', to: '/profile' },
                ]
              : isFreelancer
                ? [
                    { icon: '🔎', title: 'Find Projects', desc: 'Browse open opportunities', to: '/projects' },
                    { icon: '🤖', title: 'AI Discovery', desc: 'Discover relevant work', to: '/ai' },
                    { icon: '📋', title: 'My Applications', desc: 'Track your proposals', to: '/hiring' },
                    { icon: '👤', title: 'My Profile', desc: 'Keep your portfolio fresh', to: '/profile' },
                  ]
                : [
                    { icon: '➕', title: 'Create Project', desc: 'Post a project in minutes', to: '/projects/create' },
                    { icon: '📁', title: 'Manage Projects', desc: 'Track status and updates', to: '/projects' },
                    { icon: '👥', title: 'View Applicants', desc: 'Review who applied', to: '/hiring' },
                    { icon: '✨', title: 'Find Talent', desc: 'AI-assisted talent discovery', to: '/ai' },
                  ]
            ).map((action) => (
              <Link key={action.title} to={action.to} className="role-card">
                <span className="role-card-icon" aria-hidden="true">
                  {action.icon}
                </span>
                <span className="role-card-title">{action.title}</span>
                <span className="role-card-desc">{action.desc}</span>
                <span className="qa-arrow" aria-hidden="true">→</span>
              </Link>
            ))}
          </div>
        </section>

        {/* ---------------- 6. Quick actions ---------------- */}
        <section className="home-section">
          <div className="section-head">
            <h2>Quick Actions</h2>
          </div>
          <div className="quick-row">
            {(user === null
              ? [
                  { icon: '🔍', label: 'Find Projects', to: '/projects' },
                  { icon: '✨', label: 'AI Discovery', to: '/ai' },
                  { icon: '👤', label: 'Create Profile', to: '/register' },
                ]
              : isFreelancer
                ? [
                    { icon: '🔍', label: 'Find Projects', to: '/projects' },
                    { icon: '🤖', label: 'AI Discovery', to: '/ai' },
                    { icon: '📋', label: 'My Applications', to: '/hiring' },
                    { icon: '👤', label: 'Complete Profile', to: '/profile' },
                  ]
                : [
                    { icon: '➕', label: 'Create Project', to: '/projects/create' },
                    { icon: '👥', label: 'View Applicants', to: '/hiring' },
                    { icon: '📁', label: 'Manage Projects', to: '/projects' },
                    { icon: '⭐', label: 'Reviews', to: '/reviews' },
                  ]
            ).map((action) => (
              <Link key={action.label} to={action.to} className="quick-pill">
                <span aria-hidden="true">{action.icon}</span>
                {action.label}
              </Link>
            ))}
          </div>
        </section>

        {/* ---------------- 7. Trust ---------------- */}
        <section className="home-section">
          <div className="section-head">
            <div>
              <h2>Everything you need to work smarter</h2>
              <p className="muted">A complete workflow from discovery to delivery.</p>
            </div>
          </div>
          <div className="trust-grid stagger">
            {TRUST_FEATURES.map((feature) => (
              <div className="trust-card" key={feature.title}>
                <span className="trust-icon" aria-hidden="true">
                  {feature.icon}
                </span>
                <h3>{feature.title}</h3>
                <p className="muted">{feature.desc}</p>
              </div>
            ))}
          </div>
        </section>

        {/* ---------------- 8. AI promotion ---------------- */}
        <section className="ai-promo">
          <div className="ai-promo-inner">
            <span className="ai-promo-badge">🤖 AI Feature</span>
            <h2>Meet your AI-powered talent assistant</h2>
            <p>
              Describe the skills you need and CreatorConnect can discover
              relevant freelancer profiles.
            </p>
            <Link to="/ai" className="btn btn-light btn-lg">
              Try AI Discovery →
            </Link>
          </div>
        </section>

        {/* ---------------- 9. CTA ---------------- */}
        <section className="cta-section">
          <h2>Ready to build something great?</h2>
          <p className="muted">
            {isFreelancer
              ? 'Your next opportunity is waiting.'
              : user === null
                ? 'Join the marketplace in under a minute.'
                : 'Find the talent your next project deserves.'}
          </p>
          <div className="home-hero-actions" style={{ justifyContent: 'center' }}>
            <Link
              to={isFreelancer ? '/projects' : user === null ? '/register' : '/projects/create'}
              className="btn btn-primary btn-lg"
            >
              {isFreelancer
                ? 'Start finding opportunities'
                : user === null
                  ? 'Create your account'
                  : 'Start your next project'}
            </Link>
          </div>
        </section>
      </div>

      <Footer />
    </main>
  )
}
