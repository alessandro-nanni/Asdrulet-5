import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useLocalIdentity } from '../features/identity/useLocalIdentity'
import { UsernameField } from '../features/identity/UsernameField'
import { AvatarPicker } from '../features/identity/AvatarPicker'
import { joinParty } from '../features/party/api'

export function JoinPage() {
  const { code = '' } = useParams()
  const normalizedCode = code.toUpperCase()
  const navigate = useNavigate()
  const { identity, setDisplayName, setAvatarUrl } = useLocalIdentity()
  const [error, setError] = useState<string | null>(null)

  async function handleJoin() {
    setError(null)
    try {
      await joinParty(normalizedCode, identity.id, identity.displayName.trim(), identity.avatarUrl)
      navigate(`/party/${normalizedCode}`, { replace: true })
    } catch {
      setError('Could not join that party. Check the code and try again.')
    }
  }

  return (
    <div className="page page-center">
      <div className="card card-hero">
        <h1 className="title">Party {normalizedCode}</h1>

        <AvatarPicker value={identity.avatarUrl} onChange={setAvatarUrl} />
        <UsernameField value={identity.displayName} onChange={setDisplayName} />

        <button
          type="button"
          className="btn btn-primary btn-block"
          onClick={handleJoin}
          disabled={identity.displayName.trim().length === 0}
        >
          Join party
        </button>

        {error && (
          <p className="alert" role="alert">
            {error}
          </p>
        )}
      </div>
    </div>
  )
}
