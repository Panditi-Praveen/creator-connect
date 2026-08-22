import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import {
  createProfile,
  getMyProfile,
  updateProfile,
  uploadProfilePicture,
  updateLocation,
} from '../api/profile'
import { ApiError } from '../api/client'
import type { ProfileResponse } from '../types/api'
import { useAuth } from '../hooks/useAuth'
import { swalSuccess, swalError } from '../utils/notify'

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
  const [uploadingPhoto, setUploadingPhoto] = useState(false)
  const [photoPreview, setPhotoPreview] = useState<string | null>(null)
  const [fetchingLocation, setFetchingLocation] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

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

  const handlePhotoUpload = useCallback(async (file: File) => {
    setUploadingPhoto(true)
    try {
      const updated = await uploadProfilePicture(file)
      setProfile(updated)
      setForm(fromProfile(updated))
      setPhotoPreview(null)
      await swalSuccess('Profile Picture Updated', 'Your profile picture has been uploaded successfully.')
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to upload profile picture.'
      await swalError('Upload Failed', message)
    } finally {
      setUploadingPhoto(false)
    }
  }, [])

  const handlePhotoFileChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    // Validate file type on client side
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
    if (!allowedTypes.includes(file.type)) {
      swalError('Invalid File', 'Please select a JPG, JPEG, PNG, or WEBP image file.')
      return
    }

    // Validate file size (10 MB)
    if (file.size > 10 * 1024 * 1024) {
      swalError('File Too Large', 'The maximum file size is 10 MB.')
      return
    }

    // Show preview
    const reader = new FileReader()
    reader.onload = (e) => setPhotoPreview(e.target?.result as string)
    reader.readAsDataURL(file)

    void handlePhotoUpload(file)
    // Reset the input so the same file can be re-selected
    event.target.value = ''
  }, [handlePhotoUpload])

  const handleUseCurrentLocation = useCallback(async () => {
    if (!navigator.geolocation) {
      await swalError('Not Supported', 'Geolocation is not supported by your browser.')
      return
    }

    setFetchingLocation(true)
    try {
      const position = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 300000,
        })
      })

      const { latitude, longitude } = position.coords

      // Try reverse geocoding via a free API
      let city = ''
      let state = ''
      let country = ''
      let formattedAddress = ''

      try {
        const geoResponse = await fetch(
          `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}`
        )
        const geoData = await geoResponse.json()
        if (geoData.address) {
          city = geoData.address.city || geoData.address.town || geoData.address.village || ''
          state = geoData.address.state || ''
          country = geoData.address.country || ''
          formattedAddress = geoData.display_name || ''
        }
      } catch {
        // Geocoding failed — still save coordinates
      }

      const updated = await updateLocation({
        latitude,
        longitude,
        city,
        state,
        country,
        formattedAddress,
      })
      setProfile(updated)
      setForm(fromProfile(updated))
      await swalSuccess('Location Updated', 'Your current location has been saved successfully.')
    } catch (err) {
      if (err instanceof GeolocationPositionError) {
        let message = 'Unable to retrieve your location.'
        if (err.code === 1) message = 'Location permission denied. Please allow location access in your browser settings.'
        else if (err.code === 2) message = 'Location unavailable. Please try again.'
        else if (err.code === 3) message = 'Location request timed out. Please try again.'
        await swalError('Location Error', message)
      } else {
        const message = err instanceof Error ? err.message : 'Failed to save location.'
        await swalError('Location Error', message)
      }
    } finally {
      setFetchingLocation(false)
    }
  }, [])

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
        <h1 style={{ marginBottom: '1rem' }}>Profile</h1>
        <div className="skeleton-card">
          <div className="skeleton w60" />
          <div className="skeleton w40" />
          <div className="skeleton w90" />
          <div className="skeleton w30" />
        </div>
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
  const completionFields: Array<keyof ProfileResponse> = [
    'headline', 'bio', 'location', 'website', 'linkedin', 'github', 'skills',
    'experience',
  ]
  const completion = profile
    ? Math.round(
        (completionFields.filter((field) => {
          const value = profile[field]
          return value !== undefined && value !== null && value !== ''
        }).length /
          completionFields.length) *
          100,
      )
    : 0
  const skillList = (profile?.skills ?? '')
    .split(',')
    .map((skill) => skill.trim())
    .filter(Boolean)
  const initials = [form.firstName, form.lastName]
    .filter(Boolean)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')

  const displayImage = photoPreview || profile?.profileImagePath
    ? (photoPreview || `/profile/me/photo?t=${profile?.updatedAt ?? ''}`)
    : null

  return (
    <main className="page">
      {editing && profile && (
        <section className="profile-card">
          <div className="profile-cover" />
          <div className="profile-head">
            <div style={{ position: 'relative', marginTop: '-38px' }}>
              {displayImage ? (
                <img
                  src={displayImage}
                  alt="Profile"
                  className="avatar"
                  style={{ width: 74, height: 74, fontSize: '1.5rem', borderRadius: 22, boxShadow: '0 0 0 4px var(--surface), var(--shadow-sm)', objectFit: 'cover' }}
                  onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }}
                />
              ) : (
                <span className="avatar" aria-hidden="true" style={{ width: 74, height: 74, fontSize: '1.5rem', borderRadius: 22, marginTop: 0 }}>
                  {initials || '?'}
                </span>
              )}
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingPhoto}
                className="btn btn-sm btn-ghost"
                style={{
                  position: 'absolute',
                  bottom: -4,
                  right: -4,
                  padding: '0.25rem 0.5rem',
                  fontSize: '0.7rem',
                  borderRadius: 8,
                  background: 'var(--primary-600)',
                  color: '#fff',
                  border: '2px solid var(--surface)',
                  boxShadow: 'var(--shadow-sm)',
                  cursor: 'pointer',
                }}
                title="Upload photo"
              >
                {uploadingPhoto ? '...' : '📷'}
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/jpg,image/png,image/webp"
                onChange={handlePhotoFileChange}
                style={{ display: 'none' }}
              />
            </div>
            <div className="profile-head-info">
              <h1>
                {profile.firstName} {profile.lastName}
              </h1>
              {profile.headline && <p className="headline">{profile.headline}</p>}
              <p className="muted" style={{ margin: '0.35rem 0 0', fontSize: '0.88rem' }}>
                {profile.location ?? profile.city ?? 'Remote'}
                {profile.experience ? ` · ${profile.experience} yr${profile.experience === 1 ? '' : 's'} experience` : ''}
                {profile.availableForHire === false ? ' · Not available' : ' · Available for hire'}
              </p>
            </div>
            <span className={`badge ${profile.availableForHire === false ? 'badge-cancelled' : 'badge-completed'}`}>
              {profile.availableForHire === false ? 'Not available' : 'Available'}
            </span>
          </div>
          <div className="profile-stats">
            <div className="profile-stat">
              <div className="ps-value">{completion}%</div>
              <div className="ps-label">Complete</div>
            </div>
            <div className="profile-stat">
              <div className="ps-value">{skillList.length}</div>
              <div className="ps-label">Skills</div>
            </div>
            {profile.website && (
              <div className="profile-stat">
                <div className="ps-value">✓</div>
                <div className="ps-label">Website</div>
              </div>
            )}
          </div>
          {skillList.length > 0 && (
            <div style={{ padding: '0 1.75rem 1.5rem' }}>
              <div className="tags">
                {skillList.map((skill) => (
                  <span className="tag" key={skill}>
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          )}
        </section>
      )}

      <header className="page-header" style={{ marginBottom: '1rem' }}>
        <div>
          <h1>{editing ? 'Edit your profile' : 'Create your profile'}</h1>
          <p className="sub">
            {editing
              ? 'Keep your portfolio fresh so clients can find you.'
              : 'Tell clients who you are — it only takes a minute.'}
          </p>
        </div>
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
        <div className="form-card">
          <div className="form-grid">
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
          </div>

          <div className="field" style={{ marginTop: '1rem' }}>
            <label htmlFor="headline">Headline</label>
            <input
              id="headline"
              placeholder="e.g. Senior Video Editor & Motion Designer"
              value={form.headline}
              onChange={(event) => setField('headline', event.target.value)}
            />
          </div>

          <div className="field" style={{ marginTop: '1rem' }}>
            <label htmlFor="bio">Bio</label>
            <textarea
              id="bio"
              rows={3}
              value={form.bio}
              onChange={(event) => setField('bio', event.target.value)}
            />
          </div>

          <div className="form-grid" style={{ marginTop: '1rem' }}>
            <div className="field">
              <label htmlFor="location">Location</label>
              <input
                id="location"
                value={form.location}
                onChange={(event) => setField('location', event.target.value)}
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
          </div>

          {editing && (
            <div className="form-card" style={{ marginTop: '1rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.75rem', flexWrap: 'wrap' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1rem' }}>Current Location</h3>
                  <p className="muted" style={{ margin: '0.25rem 0 0', fontSize: '0.85rem' }}>
                    {profile.latitude && profile.longitude
                      ? `${profile.latitude.toFixed(4)}, ${profile.longitude.toFixed(4)}`
                      : 'No location set'}
                    {profile.city ? ` · ${profile.city}` : ''}
                    {profile.country ? `, ${profile.country}` : ''}
                  </p>
                </div>
                <button
                  type="button"
                  className="btn btn-sm"
                  onClick={() => void handleUseCurrentLocation()}
                  disabled={fetchingLocation}
                >
                  {fetchingLocation ? (
                    <>⏳ Fetching...</>
                  ) : (
                    <>📍 Use My Current Location</>
                  )}
                </button>
              </div>
            </div>
          )}

          <div className="field" style={{ marginTop: '1rem' }}>
            <label htmlFor="skills">Skills (comma separated)</label>
            <input
              id="skills"
              placeholder="e.g. Premiere Pro, After Effects"
              value={form.skills}
              onChange={(event) => setField('skills', event.target.value)}
            />
          </div>

          <div className="form-grid" style={{ marginTop: '1rem' }}>
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
          </div>

          <div className="form-grid" style={{ marginTop: '1rem' }}>
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
          </div>
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
