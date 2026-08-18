import { useCallback, useEffect, useState, type FormEvent } from 'react'
import {
  createProfile,
  getMyProfile,
  updateProfile,
} from '../api/profile'
import { ApiError } from '../api/client'
import type { ProfileResponse } from '../types/api'
import { useAuth } from '../hooks/useAuth'

type FormState = {
  firstName: string
  lastName: string
  headline: string
  bio: string
  location: string
  website: string
  linkedin: string
  github: string
  skills: string
  experience: string
  availableForHire: boolean
}

const EMPTY_FORM: FormState = {
  firstName: '',
  lastName: '',
  headline: '',
  bio: '',
  location: '',
  website: '',
  linkedin: '',
  github: '',
  skills: '',
  experience: '',
  availableForHire: true,
}

function fromProfile(profile: ProfileResponse): FormState {
  return {
    firstName: profile.firstName,
    lastName: profile.lastName,
    headline: profile.headline ?? '',
    bio: profile.bio ?? '',
    location: profile.location ?? '',
    website: profile.website ?? '',
    linkedin: profile.linkedin ?? '',
    github: profile.github ?? '',
    skills: profile.skills ?? '',
    experience: profile.experience?.toString() ?? '',
    availableForHire: profile.availableForHire ?? true,
  }
}

export default function ProfilePage() {
  const { user } = useAuth()
  const [profile, setProfile] = useState<ProfileResponse | null>(null)
  const [hasProfile, setHasProfile] = useState<boolean | null>(null)
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const mine = await getMyProfile()
      setProfile(mine)
      setHasProfile(true)
      setForm(fromProfile(mine))
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setHasProfile(false)
        setForm({ ...EMPTY_FORM, firstName: user?.email.split('@')[0] ?? '' })
      } else {
        setError(
          err instanceof Error ? err.message : 'Failed to load profile.',
        )
      }
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    void load()
  }, [load])

  const setField = <K extends keyof FormState>(key: K, value: FormState[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    setSuccess(null)
    const payload = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      headline: form.headline.trim() || undefined,
      bio: form.bio.trim() || undefined,
      location: form.location.trim() || undefined,
      website: form.website.trim() || undefined,
      linkedin: form.linkedin.trim() || undefined,
      github: form.github.trim() || undefined,
      skills: form.skills.trim() || undefined,
      experience: form.experience === '' ? undefined : Number(form.experience),
      availableForHire: form.availableForHire,
    }
    try {
      if (hasProfile && profile) {
        const updated = await updateProfile(profile.userId, payload)
        setProfile(updated)
        setForm(fromProfile(updated))
        setSuccess('Profile updated successfully.')
      } else {
        const created = await createProfile(payload)
        setProfile(created)
        setHasProfile(true)
        setForm(fromProfile(created))
        setSuccess('Profile created successfully.')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save profile.')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <main className="page">
        <h1>Profile</h1>
        <p className="loading">Loading your profile…</p>
      </main>
    )
  }

  if (error) {
    return (
      <main className="page">
        <h1>Profile</h1>
        <p className="form-error" role="alert">
          {error}
        </p>
        <button type="button" className="btn" onClick={() => void load()}>
          Try again
        </button>
      </main>
    )
  }

  const editing = hasProfile === true

  return (
    <main className="page">
      <header className="page-header">
        <h1>{editing ? 'Your profile' : 'Create your profile'}</h1>
      </header>

      {success && (
        <p className="form-success" role="status">
          {success}
        </p>
      )}
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      <form className="form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="firstName">First name *</label>
          <input
            id="firstName"
            required
            value={form.firstName}
            onChange={(event) => setField('firstName', event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="lastName">Last name *</label>
          <input
            id="lastName"
            required
            value={form.lastName}
            onChange={(event) => setField('lastName', event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="headline">Headline</label>
          <input
            id="headline"
            placeholder="e.g. Senior Video Editor & Motion Designer"
            value={form.headline}
            onChange={(event) => setField('headline', event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="bio">Bio</label>
          <textarea
            id="bio"
            rows={3}
            value={form.bio}
            onChange={(event) => setField('bio', event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="location">Location</label>
          <input
            id="location"
            value={form.location}
            onChange={(event) => setField('location', event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="skills">Skills (comma separated)</label>
          <input
            id="skills"
            placeholder="e.g. Premiere Pro, After Effects"
            value={form.skills}
            onChange={(event) => setField('skills', event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="experience">Years of experience</label>
          <input
            id="experience"
            type="number"
            min={0}
            max={100}
            value={form.experience}
            onChange={(event) => setField('experience', event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="website">Website</label>
          <input
            id="website"
            type="url"
            value={form.website}
            onChange={(event) => setField('website', event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="linkedin">LinkedIn URL</label>
          <input
            id="linkedin"
            type="url"
            value={form.linkedin}
            onChange={(event) => setField('linkedin', event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="github">GitHub URL</label>
          <input
            id="github"
            type="url"
            value={form.github}
            onChange={(event) => setField('github', event.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="availableForHire">Available for hire</label>
          <select
            id="availableForHire"
            value={form.availableForHire ? 'true' : 'false'}
            onChange={(event) => setField('availableForHire', event.target.value === 'true')}
          >
            <option value="true">Yes</option>
            <option value="false">No</option>
          </select>
        </div>

        <div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting
              ? 'Saving…'
              : editing
                ? 'Update profile'
                : 'Create profile'}
          </button>
        </div>
      </form>
    </main>
  )
}
