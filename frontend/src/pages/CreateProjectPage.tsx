import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { createProject } from '../api/projects'

const MIN_DATE = new Date(Date.now() + 86400000).toISOString().slice(0, 10)

export default function CreateProjectPage() {
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [category, setCategory] = useState('')
  const [skills, setSkills] = useState('')
  const [budget, setBudget] = useState('')
  const [duration, setDuration] = useState('')
  const [experienceLevel, setExperienceLevel] = useState('Intermediate')
  const [location, setLocation] = useState('')
  const [deadline, setDeadline] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await createProject({
        title: title.trim(),
        description: description.trim(),
        category: category.trim(),
        skillsRequired: skills
          .split(',')
          .map((skill) => skill.trim())
          .filter(Boolean),
        budget: Number(budget),
        duration: duration.trim(),
        experienceLevel,
        location: location.trim() || undefined,
        applicationDeadline: deadline,
      })
      navigate('/projects', { replace: true })
    } catch (err) {
      setError(
        err instanceof Error ? err.message : 'Failed to create project.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>Create project</h1>
      </header>

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      <form className="form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="title">Title *</label>
          <input
            id="title"
            required
            maxLength={200}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="description">Description *</label>
          <textarea
            id="description"
            required
            rows={4}
            maxLength={5000}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="category">Category *</label>
          <input
            id="category"
            required
            placeholder="e.g. Video Editing, Graphic Design"
            value={category}
            onChange={(event) => setCategory(event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="skills">Required skills (comma separated)</label>
          <input
            id="skills"
            placeholder="e.g. Premiere Pro, After Effects"
            value={skills}
            onChange={(event) => setSkills(event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="budget">Budget (USD) *</label>
          <input
            id="budget"
            type="number"
            required
            min={0}
            step="0.01"
            value={budget}
            onChange={(event) => setBudget(event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="duration">Duration *</label>
          <input
            id="duration"
            required
            placeholder="e.g. 4 weeks"
            value={duration}
            onChange={(event) => setDuration(event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="experienceLevel">Experience level *</label>
          <select
            id="experienceLevel"
            value={experienceLevel}
            onChange={(event) => setExperienceLevel(event.target.value)}
          >
            <option value="Entry">Entry</option>
            <option value="Intermediate">Intermediate</option>
            <option value="Senior">Senior</option>
          </select>
        </div>

        <div className="field">
          <label htmlFor="location">Location</label>
          <input
            id="location"
            placeholder="e.g. Remote, Mumbai"
            value={location}
            onChange={(event) => setLocation(event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="deadline">Application deadline *</label>
          <input
            id="deadline"
            type="date"
            required
            min={MIN_DATE}
            value={deadline}
            onChange={(event) => setDeadline(event.target.value)}
          />
        </div>

        <div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create project'}
          </button>
        </div>
      </form>
    </main>
  )
}
