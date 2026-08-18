import { useState, type FormEvent } from 'react'
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

export default function AiDiscoveryPage() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<TalentResult[] | null>(null)
  const [searchedQuery, setSearchedQuery] = useState('')
  const [lastQuery, setLastQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [unavailable, setUnavailable] = useState(false)

  const runDiscovery = async (trimmed: string) => {
    setLoading(true)
    setError(null)
    setUnavailable(false)
    setResults(null)
    try {
      const response = await discoverTalent(trimmed)
      setResults(response.results)
      setSearchedQuery(response.query)
    } catch (err) {
      // A 502 from the AI Service is the documented LLM-failure contract
      // (OpenAI quota/availability) — NOT a connection failure. Show the
      // friendly unavailable state and offer a retry.
      if (err instanceof ApiError && err.status === 502) {
        setUnavailable(true)
      } else {
        // Any other failure keeps its real message: network problems surface
        // as "Unable to reach the server…", 401s redirect via the global
        // handler, 400s show the backend validation message, etc.
        setError(
          err instanceof Error ? err.message : 'Discovery failed. Please try again.',
        )
      }
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const trimmed = query.trim()
    if (!trimmed) return
    setLastQuery(trimmed)
    void runDiscovery(trimmed)
  }

  // Retry re-sends the exact same query through the existing API call.
  const handleRetry = () => {
    if (!lastQuery) return
    void runDiscovery(lastQuery)
  }

  const handleExample = (example: string) => {
    setQuery(example)
  }

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
              disabled={loading || query.trim().length === 0}
            >
              {loading ? 'Searching…' : '✨ Discover talent'}
            </button>
          </div>
        </div>
      </form>

      {loading && (
        <div className="ai-loading" role="status">
          <span className="ai-spinner" aria-hidden="true" />
          <span className="ai-loading-text">
            Finding the best matches
            <span className="ai-dots" aria-hidden="true">
              <i />
              <i />
              <i />
            </span>
          </span>
        </div>
      )}

      {unavailable && (
        <div className="empty" role="alert" style={{ marginTop: '1.25rem' }}>
          <span className="empty-icon" aria-hidden="true">✨</span>
          <span className="empty-title">AI recommendations are temporarily unavailable</span>
          <p className="empty-desc">
            The AI service is currently unavailable. Please try again later.
          </p>
          <button
            type="button"
            className="btn btn-primary"
            disabled={loading}
            onClick={handleRetry}
          >
            {loading ? 'Retrying…' : 'Try again'}
          </button>
        </div>
      )}

      {error && (
        <p className="form-error" role="alert" style={{ marginTop: '1.25rem' }}>
          {error}
        </p>
      )}

      {results !== null && !loading && (
        <section style={{ marginTop: '1.75rem' }}>
          <div className="section-head">
            <h2>
              Results for “{searchedQuery}”
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
