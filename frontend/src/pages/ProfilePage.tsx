import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import {
  createProfile,
  deleteProfilePicture,
  getMyProfile,
  updateProfile,
  uploadProfilePicture,
  updateLocation,
} from '../api/profile'
import { ApiError } from '../api/client'
import type { ProfileResponse } from '../types/api'
import { useAuth } from '../hooks/useAuth'
import { swalSuccess, swalError, swalConfirm } from '../utils/notify'

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

/* --------------------------------------------------------------------------
   Profile completion calculation
   -------------------------------------------------------------------------- */

interface CompletionFieldDef {
  key: string
  label: string
  /** Weight determines contribution to 100%. */
  weight: number
  /** Return true when the field is filled. */
  filled: (profile: ProfileResponse) => boolean
}

const COMPLETION_FIELDS: CompletionFieldDef[] = [
  {
    key: 'profilePicture',
    label: 'Add a profile picture',
    weight: 12,
    filled: (p) => !!(p.profileImagePath || p.profileImageUrl),
  },
  {
    key: 'firstName',
    label: 'Add your first name',
    weight: 5,
    filled: (p) => !!p.firstName?.trim(),
  },
  {
    key: 'lastName',
    label: 'Add your last name',
    weight: 5,
    filled: (p) => !!p.lastName?.trim(),
  },
  {
    key: 'headline',
    label: 'Add a professional headline',
    weight: 10,
    filled: (p) => !!p.headline?.trim(),
  },
  {
    key: 'bio',
    label: 'Write a bio',
    weight: 12,
    filled: (p) => !!p.bio?.trim(),
  },
  {
    key: 'location',
    label: 'Add your location',
    weight: 8,
    filled: (p) => !!p.location?.trim(),
  },
  {
    key: 'skills',
    label: 'Add your skills',
    weight: 12,
    filled: (p) => !!p.skills?.trim(),
  },
  {
    key: 'experience',
    label: 'Add years of experience',
    weight: 6,
    filled: (p) => p.experience != null,
  },
  {
    key: 'website',
    label: 'Add your website',
    weight: 10,
    filled: (p) => !!p.website?.trim(),
  },
  {
    key: 'linkedin',
    label: 'Add your LinkedIn URL',
    weight: 10,
    filled: (p) => !!p.linkedin?.trim(),
  },
  {
    key: 'github',
    label: 'Add your GitHub URL',
    weight: 5,
    filled: (p) => !!p.github?.trim(),
  },
  {
    key: 'availableForHire',
    label: 'Set your availability',
    weight: 5,
    filled: () => true, // always defaults to true
  },
]

interface CompletionInfo {
  percentage: number
  missingFields: string[]
}

function computeCompletion(profile: ProfileResponse | null): CompletionInfo {
  if (!profile) return { percentage: 0, missingFields: COMPLETION_FIELDS.map((f) => f.label) }

  let earned = 0
  const missing: string[] = []
  for (const field of COMPLETION_FIELDS) {
    if (field.filled(profile)) {
      earned += field.weight
    } else {
      missing.push(field.label)
    }
  }
  return { percentage: Math.round(earned), missingFields: missing }
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
  const [removingPhoto, setRemovingPhoto] = useState(false)
  const [photoPreview, setPhotoPreview] = useState<string | null>(null)
  const [pendingFile, setPendingFile] = useState<File | null>(null)
  const [showPhotoModal, setShowPhotoModal] = useState(false)
  const [fetchingLocation, setFetchingLocation] = useState(false)
  const [skillInput, setSkillInput] = useState('')
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
      setPendingFile(null)
      setShowPhotoModal(false)
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

    // Show preview and open the confirm modal (do NOT auto-upload)
    const reader = new FileReader()
    reader.onload = (e) => {
      setPhotoPreview(e.target?.result as string)
      setPendingFile(file)
      setShowPhotoModal(true)
    }
    reader.readAsDataURL(file)

    // Reset the input so the same file can be re-selected
    event.target.value = ''
  }, [])

  const handleConfirmUpload = useCallback(() => {
    if (!pendingFile || uploadingPhoto) return
    void handlePhotoUpload(pendingFile)
  }, [pendingFile, uploadingPhoto, handlePhotoUpload])

  const handleCancelPreview = useCallback(() => {
    setPhotoPreview(null)
    setPendingFile(null)
    setShowPhotoModal(false)
  }, [])

  const handleRemovePhoto = useCallback(async () => {
    if (!profile?.profileImagePath || removingPhoto) return
    const confirmed = await swalConfirm(
      'Remove Profile Picture?',
      'This will permanently remove your profile picture. You can upload a new one later.',
      'Remove',
    )
    if (!confirmed) return

    setRemovingPhoto(true)
    try {
      const updated = await deleteProfilePicture()
      setProfile(updated)
      setForm(fromProfile(updated))
      setPhotoPreview(null)
      setPendingFile(null)
      setShowPhotoModal(false)
      await swalSuccess('Photo Removed', 'Your profile picture has been removed.')
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to remove profile picture.'
      await swalError('Remove Failed', message)
    } finally {
      setRemovingPhoto(false)
    }
  }, [profile?.profileImagePath, removingPhoto])

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

      // Reverse geocoding via OpenStreetMap Nominatim
      let city = ''
      let state = ''
      let country = ''
      let formattedAddress = ''
      let readableLocation = ''

      try {
        const geoResponse = await fetch(
          `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${latitude}&lon=${longitude}`,
          { headers: { 'Accept-Language': 'en' } },
        )
        if (!geoResponse.ok) throw new Error(`Geocoding API responded with ${geoResponse.status}`)
        const geoData = await geoResponse.json()
        if (geoData.address) {
          const addr = geoData.address
          city = addr.city || addr.town || addr.village || addr.municipality || ''
          state = addr.state || ''
          country = addr.country || ''
          formattedAddress = geoData.display_name || ''

          // Build a clean, readable address from the best available fields
          const parts: string[] = []
          const suburbOrNeighbourhood = addr.suburb || addr.neighbourhood || addr.city_district || addr.hamlet || ''
          if (suburbOrNeighbourhood) parts.push(suburbOrNeighbourhood)
          if (city && suburbOrNeighbourhood !== city) parts.push(city)
          if (state && state !== city) parts.push(state)
          if (country) parts.push(country)
          readableLocation = parts.join(', ')
        }

        if (!readableLocation) {
          throw new Error('Could not determine a readable address from the coordinates.')
        }
      } catch (geoErr) {
        // Geocoding failed — show a meaningful error instead of silently displaying coordinates
        const msg = geoErr instanceof Error ? geoErr.message : 'Reverse geocoding failed.'
        await swalError('Address Lookup Failed', `${msg} Location coordinates were captured but could not be converted to a readable address.`)
        setFetchingLocation(false)
        return
      }

      // Update the form's location field so the user sees the readable name
      setField('location', readableLocation)

      const updated = await updateLocation({
        latitude,
        longitude,
        city,
        state,
        country,
        formattedAddress,
      })
      setProfile(updated)
      setForm(() => ({ ...fromProfile(updated), location: readableLocation }))
      await swalSuccess('Location Updated', `Your current location has been saved as: ${readableLocation}`)
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

  /* ---- Skills helpers ---- */
  const skillTags: string[] = useMemo(
    () =>
      form.skills
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean),
    [form.skills],
  )

  const syncSkills = (tags: string[]) => {
    // Deduplicate while preserving order
    const unique = [...new Set(tags.map((t) => t.trim()).filter(Boolean))]
    setField('skills', unique.join(', '))
  }

  const addSkillsFromText = useCallback(
    (raw: string) => {
      const newTags = raw
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
      if (newTags.length === 0) return
      syncSkills([...skillTags, ...newTags])
      setSkillInput('')
    },
    [skillTags],
  )

  const removeSkill = useCallback(
    (skill: string) => {
      syncSkills(skillTags.filter((t) => t !== skill))
    },
    [skillTags],
  )

  const handleSkillKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter') {
        e.preventDefault()
        addSkillsFromText(skillInput)
      }
    },
    [addSkillsFromText, skillInput],
  )

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

  // All hooks must be called before any early returns (Rules of Hooks).
  const editing = hasProfile === true
  const { percentage: completionPct, missingFields } = useMemo(
    () => computeCompletion(profile),
    [profile],
  )

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
                disabled={uploadingPhoto || removingPhoto}
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
                title={profile?.profileImagePath ? 'Replace photo' : 'Upload photo'}
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
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem', marginTop: '-32px', marginLeft: '82px' }}>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingPhoto || removingPhoto}
                className="btn btn-sm btn-ghost"
                style={{ fontSize: '0.78rem', padding: '0.2rem 0.6rem', width: 'fit-content' }}
              >
                {uploadingPhoto ? 'Uploading…' : profile?.profileImagePath ? '📷 Replace Photo' : '📷 Change Photo'}
              </button>
              {profile?.profileImagePath && (
                <button
                  type="button"
                  onClick={() => void handleRemovePhoto()}
                  disabled={uploadingPhoto || removingPhoto}
                  className="btn btn-sm btn-ghost"
                  style={{ fontSize: '0.78rem', padding: '0.2rem 0.6rem', width: 'fit-content', color: 'var(--danger-ink, #dc2626)' }}
                >
                  {removingPhoto ? 'Removing…' : '🗑️ Remove Photo'}
                </button>
              )}
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
              <div className="ps-value">{completionPct}%</div>
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

      {/* ---------- Profile Strength card ---------- */}
      {editing && (
        <section
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.35rem 1.5rem',
            boxShadow: 'var(--shadow-sm)',
            marginBottom: '1.25rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.75rem', flexWrap: 'wrap', marginBottom: '0.65rem' }}>
            <h2 style={{ margin: 0, fontSize: '1.05rem' }}>Profile Strength</h2>
            <span
              style={{
                fontWeight: 800,
                fontSize: '1.15rem',
                color: completionPct === 100
                  ? 'var(--success-strong)'
                  : completionPct >= 70
                    ? 'var(--primary-600)'
                    : 'var(--ink-600)',
              }}
            >
              {completionPct}%
            </span>
          </div>

          {/* Progress bar */}
          <div className="progress-track" style={{ marginBottom: '0.75rem' }}>
            <div
              className="progress-fill"
              style={{
                width: `${completionPct}%`,
                background: completionPct === 100
                  ? 'linear-gradient(135deg, #059669 0%, #10b981 100%)'
                  : undefined,
              }}
            />
          </div>

          {completionPct === 100 ? (
            <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--success-strong)', fontWeight: 600 }}>
              ✅ Your profile is complete!
            </p>
          ) : (
            <>
              <p className="muted" style={{ margin: '0 0 0.5rem', fontSize: '0.88rem' }}>
                Complete your profile to increase your visibility.
              </p>
              <ul style={{ margin: 0, padding: '0 0 0 1.15rem', listStyle: 'none' }}>
                {missingFields.map((label) => (
                  <li
                    key={label}
                    style={{
                      fontSize: '0.84rem',
                      color: 'var(--ink-600)',
                      padding: '0.15rem 0',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.4rem',
                    }}
                  >
                    <span style={{ color: 'var(--ink-400)', fontSize: '0.7rem' }}>○</span>
                    {label}
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      )}

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
                    {fetchingLocation
                      ? '⏳ Detecting your location...'
                      : form.location || (profile?.latitude && profile?.longitude
                        ? [profile?.city, profile?.state, profile?.country].filter(Boolean).join(', ') || 'Location saved'
                        : 'No location set')}
                  </p>
                </div>
                <button
                  type="button"
                  className="btn btn-sm"
                  onClick={() => void handleUseCurrentLocation()}
                  disabled={fetchingLocation}
                >
                  {fetchingLocation ? (
                    <>⏳ Detecting your location...</>
                  ) : (
                    <>📍 Use My Current Location</>
                  )}
                </button>
              </div>
            </div>
          )}

          <div className="field" style={{ marginTop: '1rem' }}>
            <label htmlFor="skills">Skills</label>
            {/* Rendered skill tags */}
            {skillTags.length > 0 && (
              <div className="tags" style={{ marginBottom: '0.5rem' }}>
                {skillTags.map((skill) => (
                  <span
                    key={skill}
                    className="tag"
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.3rem',
                      paddingRight: '0.35rem',
                    }}
                  >
                    {skill}
                    <button
                      type="button"
                      onClick={() => removeSkill(skill)}
                      aria-label={`Remove ${skill}`}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: 'var(--ink-400)',
                        cursor: 'pointer',
                        padding: 0,
                        fontSize: '0.9rem',
                        lineHeight: 1,
                        display: 'inline-flex',
                        alignItems: 'center',
                        transition: 'color var(--t) var(--ease)',
                      }}
                      onMouseEnter={(e) => { (e.target as HTMLElement).style.color = 'var(--danger-ink)' }}
                      onMouseLeave={(e) => { (e.target as HTMLElement).style.color = 'var(--ink-400)' }}
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            )}
            {/* Input for adding new skills */}
            <input
              id="skills"
              name="profile-skills-input"
              autoComplete="off"
              placeholder="Type a skill and press Enter"
              value={skillInput}
              onChange={(event) => setSkillInput(event.target.value)}
              onKeyDown={handleSkillKeyDown}
              onBlur={() => {
                // Also add on blur (e.g. user tabs away with unsubmitted text)
                if (skillInput.trim()) addSkillsFromText(skillInput)
              }}
            />
            <p className="muted" style={{ margin: '0.25rem 0 0', fontSize: '0.78rem' }}>
              Press Enter or comma to add skills. {skillTags.length > 0 && `${skillTags.length} skill${skillTags.length === 1 ? '' : 's'} added.`}
            </p>
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

      {/* -------- Photo preview / confirm modal -------- */}
      {showPhotoModal && photoPreview && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Profile picture preview"
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 1000,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'rgba(0,0,0,0.5)',
            backdropFilter: 'blur(4px)',
          }}
          onClick={handleCancelPreview}
        >
          <div
            style={{
              background: 'var(--surface, #fff)',
              borderRadius: 'var(--radius-lg, 12px)',
              boxShadow: '0 8px 30px rgba(0,0,0,0.25)',
              padding: '1.5rem',
              maxWidth: 380,
              width: '90%',
              textAlign: 'center',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ margin: '0 0 1rem', fontSize: '1.1rem' }}>Preview Profile Picture</h3>
            <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1.25rem' }}>
              <img
                src={photoPreview}
                alt="Preview"
                style={{
                  width: 140,
                  height: 140,
                  borderRadius: '50%',
                  objectFit: 'cover',
                  border: '3px solid var(--primary-400, #818cf8)',
                  boxShadow: '0 2px 12px rgba(0,0,0,0.15)',
                }}
              />
            </div>
            <p style={{ margin: '0 0 1rem', fontSize: '0.88rem', color: 'var(--ink-600, #475569)' }}>
              {pendingFile?.name}
              {pendingFile && ` (${(pendingFile.size / 1024 / 1024).toFixed(1)} MB)`}
            </p>
            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={handleCancelPreview}
                disabled={uploadingPhoto}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleConfirmUpload}
                disabled={uploadingPhoto}
              >
                {uploadingPhoto ? 'Uploading…' : '✓ Confirm Upload'}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}
