import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { discoverTalent } from '../api/ai'
import { ApiError } from '../api/client'
import type { TalentResult } from '../types/api'

const EXAMPLE_QUERY =
  'Find Java developers with Spring Boot, microservices and REST API experience.'

const EXAMPLE_CHIPS = [
  'Java + Spring Boot developers',
  'Video editors for YouTube',
  'Brand designers with Figma skills',
]

/** Classify an error into a user-friendly category and message. */
function classifyError(err: unknown): { message: string; retryAfter?: number } {
  if (err instanceof ApiError) {
    // 429 — rate limit
    if (err.status === 429) {
      return {
        message:
          err.message ||
          'AI is receiving too many requests. Please wait a moment and try again.',
        retryAfter: err.retryAfter,
      }
    }
    // 502 — LLM provider failure (quota exhausted / unavailable)
    if (err.status === 502) {
      return {
        message:
          'AI service quota is currently unavailable. Please try again later.',
      }
    }
    // 503 — AI service not configured or profile service down
    if (err.status === 503) {
      return {
        message:
          'AI service is temporarily unavailable. Please try again later.',
      }
    }
    // Any other ApiError — use the backend message
    return { message: err.message }
  }

  // Network / unknown
  if (err instanceof TypeError && err.message.includes('fetch')) {
    return {
      message:
        'Unable to connect to the AI service. Please check your connection.',
    }
  }

  return {
    message:
      err instanceof Error
        ? err.message
        : 'Something went wrong while discovering talent. Please try again.',
  }
}

export default function AiDiscoveryPage() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<TalentResult[] | null>(null)
  const [searchedQuery, setSearchedQuery] = useState('')
  const [lastQuery, setLastQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [retryAfter, setRetryAfter] = useState(0)

  // Ref to guard against concurrent requests
  const inFlightRef = useRef(false)
  // Ref to track the latest request id so stale responses are discarded
  const requestIdRef = useRef(0)

  // Countdown timer for retry cooldown
  useEffect(() => {
    if (retryAfter <= 0) return
    const id = setInterval(() => {
      setRetryAfter((prev) => {
        if (prev <= 1) {
          clearInterval(id)
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(id)
  }, [retryAfter > 0]) // re-effect only when timer starts

  const runDiscovery = useCallback(
    async (trimmed: string) => {
      // Prevent duplicate requests
      if (inFlightRef.current) return
      inFlightRef.current = true
      const thisRequest = ++requestIdRef.current

      setLoading(true)
      setError(null)
      setRetryAfter(0)
      setResults(null)

      try {
        const response = await discoverTalent(trimmed)
        // Discard stale responses
        if (thisRequest !== requestIdRef.current) return
        setResults(response.results)
        setSearchedQuery(response.query)
      } catch (err) {
        if (thisRequest !== requestIdRef.current) return
        const classified = classifyError(err)
        setError(classified.message)
        if (classified.retryAfter && classified.retryAfter > 0) {
          setRetryAfter(classified.retryAfter)
        }
      } finally {
        if (thisRequest === requestIdRef.current) {
          setLoading(false)
        }
        inFlightRef.current = false
      }
    },
    [],
  )

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const trimmed = query.trim()
    if (!trimmed || loading) return
    setLastQuery(trimmed)
    void runDiscovery(trimmed)
  }

  // Retry re-sends the exact same query through the existing API call.
  const handleRetry = () => {
    if (!lastQuery || loading || retryAfter > 0) return
    void runDiscovery(lastQuery)
  }

  const handleExample = (example: string) => {
    setQuery(example)
  }

  const cooldownMessage =
    retryAfter > 0
      ? `Please try again in ${retryAfter} second${retryAfter === 1 ? '' : 's'}.`
      : null

  return (
    <main className="page">
      <section className="hero">
        <h1>Find the perfect talent</h1>
        <p className="sub">
          Describe what you need and let CreatorConnect discover the best
          matching freelancers from real profiles.
        </p>
      </section>

      <form onSubmit={handleSubmit} aria-label="AI talent discovery">
        <div className="ai-query">
          <textarea
            id="query"
            rows={3}
            maxLength={500}
            placeholder={EXAMPLE_QUERY}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            aria-label="Describe the talent you need"
          />
          <div className="ai-query-footer">
            <span className="ai-examples">
              Try:{' '}
              {EXAMPLE_CHIPS.map((chip) => (
                <button
                  key={chip}
                  type="button"
                  onClick={() => handleExample(chip)}
                >
                  {chip}
                </button>
              ))}
            </span>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading || query.trim().length === 0 || retryAfter > 0}
            >
              {loading
                ? 'Discovering talent...'
                : retryAfter > 0
                  ? `Wait ${retryAfter}s`
                  : '✨ Discover talent'}
            </button>
          </div>
        </div>
      </form>

      {loading && (
        <div className="ai-loading" role="status">
          <span className="ai-spinner" aria-hidden="true" />
          <span className="ai-loading-text">
            Discovering talent
            <span className="ai-dots" aria-hidden="true">
              <i />
              <i />
              <i />
            </span>
          </span>
        </div>
      )}

      {error && !loading && (
        <div
          style={{
            marginTop: '1.25rem',
            background: 'var(--danger-bg)',
            border: '1px solid #fecaca',
            borderRadius: 'var(--radius-lg)',
            padding: '1.25rem 1.4rem',
          }}
          role="alert"
        >
          <p
            style={{
              margin: 0,
              fontWeight: 600,
              color: 'var(--danger-strong)',
              fontSize: '0.95rem',
            }}
          >
            {error}
          </p>
          {cooldownMessage && (
            <p
              style={{
                margin: '0.35rem 0 0',
                fontSize: '0.85rem',
                color: 'var(--danger-ink)',
              }}
            >
              {cooldownMessage}
            </p>
          )}
          <button
            type="button"
            className="btn btn-sm"
            disabled={loading || retryAfter > 0}
            onClick={handleRetry}
            style={{ marginTop: '0.75rem' }}
          >
            {loading ? 'Retrying…' : retryAfter > 0 ? `Retry in ${retryAfter}s` : 'Retry'}
          </button>
        </div>
      )}

      {results !== null && !loading && (
        <section style={{ marginTop: '1.75rem' }}>
          <div className="section-head">
            <h2>
              Results for &ldquo;{searchedQuery}&rdquo;
              <span className="badge badge-ai" style={{ marginLeft: '0.6rem' }}>
                {results.length}
              </span>
            </h2>
          </div>
          {results.length === 0 ? (
            <div className="empty">
              <span className="empty-icon" aria-hidden="true">🔎</span>
              <span className="empty-title">No matches found</span>
              <p className="empty-desc">
                No freelancer profiles matched your request. Try a different
                description or broaden your keywords.
              </p>
            </div>
          ) : (
            <div className="card-grid stagger">
              {results.map((result) => (
                <article className="card" key={result.profileId}>
                  <div className="project-card-top">
                    <span className="category-icon" aria-hidden="true">🧑‍💻</span>
                    <span className="badge match-badge">
                      {Math.round(result.score * 100)}% match
                    </span>
                  </div>
                  <h3>{result.name ?? 'Unnamed profile'}</h3>
                  {result.headline && <p className="muted">{result.headline}</p>}
                  <div className="score-row">
                    <span className="score-bar" aria-hidden="true">
                      <i style={{ width: `${Math.round(result.score * 100)}%` }} />
                    </span>
                    <span className="score-pct">{Math.round(result.score * 100)}%</span>
                  </div>
                  {result.skills && (
                    <div className="tags">
                      {result.skills
                        .split(',')
                        .map((skill) => skill.trim())
                        .filter(Boolean)
                        .slice(0, 5)
                        .map((skill) => (
                          <span className="tag" key={skill}>
                            {skill}
                          </span>
                        ))}
                    </div>
                  )}
                  <p className="muted" style={{ fontSize: '0.85rem' }}>
                    📍 {result.location ?? 'Remote'}
                    {result.availableForHire === false ? ' · Not available' : ' · Available'}
                  </p>
                  {result.reason && (
                    <p
                      className="muted"
                      style={{
                        fontSize: '0.88rem',
                        borderTop: '1px solid var(--border)',
                        paddingTop: '0.6rem',
                      }}
                    >
                      {result.reason}
                    </p>
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
