import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { LoginButton } from '../features/auth/LoginButton'
import { useUsername } from '../features/identity/useUsername'
import { UsernameField } from '../features/identity/UsernameField'
import { joinParty } from '../features/party/api'

export function JoinPage() {
  const { code = '' } = useParams()
  const normalizedCode = code.toUpperCase()
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useUsername()
  const [error, setError] = useState<string | null>(null)

  if (loading) {
    return (
      <div className="page page-center">
        <p className="muted">Loading...</p>
      </div>
    )
  }

  if (!user) {
    return (
      <div className="page page-center">
        <div className="card card-hero">
          <h1 className="title">Party {normalizedCode}</h1>
          <p className="subtitle">Sign in to join this party.</p>
          <LoginButton />
        </div>
      </div>
    )
  }

  async function handleJoin() {
    setError(null)
    try {
      await joinParty(normalizedCode, username.trim())
      navigate(`/party/${normalizedCode}`, { replace: true })
    } catch {
      setError('Could not join that party. Check the code and try again.')
    }
  }

  return (
    <div className="page page-center">
      <div className="card card-hero">
        <h1 className="title">Party {normalizedCode}</h1>
        <p className="subtitle">Signed in as {user.displayName}</p>

        <UsernameField value={username} onChange={setUsername} />

        <button
          type="button"
          className="btn btn-primary btn-block"
          onClick={handleJoin}
          disabled={username.trim().length === 0}
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
