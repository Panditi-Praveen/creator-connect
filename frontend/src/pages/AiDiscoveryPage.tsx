import { useState, type FormEvent } from 'react'
import { discoverTalent } from '../api/ai'
import { ApiError } from '../api/client'
import type { TalentResult } from '../types/api'

const EXAMPLE_QUERY =
  'Find Java developers with Spring Boot, microservices and REST API experience.'

export default function AiDiscoveryPage() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<TalentResult[] | null>(null)
  const [searchedQuery, setSearchedQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [unavailable, setUnavailable] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const trimmed = query.trim()
    if (!trimmed) return
    setLoading(true)
    setError(null)
    setUnavailable(false)
    setResults(null)
    try {
      const response = await discoverTalent(trimmed)
      setResults(response.results)
      setSearchedQuery(response.query)
    } catch (err) {
      if (err instanceof ApiError && err.status === 502) {
        // OpenAI quota exhausted (or LLM failure) — documented backend contract.
        setUnavailable(true)
      } else {
        setError(
          err instanceof Error ? err.message : 'Discovery failed. Please try again.',
        )
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>AI Discovery</h1>
      </header>
      <p className="muted">
        Describe the talent you need in plain language. The platform matches
        your request against real freelancer profiles and ranks the best fits.
      </p>

      <form className="form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="query">What are you looking for?</label>
          <textarea
            id="query"
            rows={3}
            maxLength={500}
            placeholder={EXAMPLE_QUERY}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>
        <div>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || query.trim().length === 0}
          >
            {loading ? 'Searching…' : 'Discover talent'}
          </button>
        </div>
      </form>

      {loading && <p className="loading">Ranking freelancer profiles…</p>}

      {unavailable && (
        <div className="empty" role="alert">
          AI recommendations are temporarily unavailable. Please try again
          later.
        </div>
      )}

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      {results !== null && !loading && (
        <section style={{ marginTop: '1.5rem' }}>
          <h2>
            Results for “{searchedQuery}” ({results.length})
          </h2>
          {results.length === 0 ? (
            <div className="empty">
              No freelancer profiles matched your request.
            </div>
          ) : (
            <div className="card-grid">
              {results.map((result) => (
                <article className="card" key={result.profileId}>
                  <h3>{result.name ?? 'Unnamed profile'}</h3>
                  {result.headline && <p className="muted">{result.headline}</p>}
                  {result.skills && (
                    <p style={{ fontSize: '0.88rem' }}>{result.skills}</p>
                  )}
                  <p className="muted" style={{ fontSize: '0.88rem' }}>
                    {result.location ?? 'Remote'}
                    {result.availableForHire === false ? ' · Not available' : ''}
                  </p>
                  <p>
                    <strong>Match: {Math.round(result.score * 100)}%</strong>
                  </p>
                  {result.reason && (
                    <p style={{ fontSize: '0.88rem' }}>{result.reason}</p>
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
