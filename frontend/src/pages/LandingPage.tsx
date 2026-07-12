import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { LoginButton } from '../features/auth/LoginButton'
import { useUsername } from '../features/identity/useUsername'
import { UsernameField } from '../features/identity/UsernameField'
import { InstallAppButton } from '../features/pwa/InstallAppButton'
import { QuickGameButton } from '../features/dev/QuickGameButton'
import { createParty, joinParty } from '../features/party/api'

export function LandingPage() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useUsername()
  const [joinCode, setJoinCode] = useState('')
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
          <h1 className="title">Asdrulet 5</h1>
          <p className="subtitle">A turn-based dungeon crawler for you and your friends.</p>
          <LoginButton />
          <InstallAppButton />
          <QuickGameButton />
        </div>
      </div>
    )
  }

  const canAct = username.trim().length > 0

  async function handleCreate() {
    setError(null)
    const party = await createParty(username.trim())
    navigate(`/party/${party.code}`)
  }

  async function handleJoin() {
    setError(null)
    try {
      const party = await joinParty(joinCode.trim().toUpperCase(), username.trim())
      navigate(`/party/${party.code}`)
    } catch {
      setError('Could not join that party. Check the code and try again.')
    }
  }

  return (
    <div className="page page-center">
      <div className="card card-hero">
        <h1 className="title">Asdrulet 5</h1>
        <p className="subtitle">Signed in as {user.displayName}</p>

        <UsernameField value={username} onChange={setUsername} />

        <button type="button" className="btn btn-primary btn-block" onClick={handleCreate} disabled={!canAct}>
          Create party
        </button>

        <div className="divider">or</div>

        <div className="field">
          <label className="field-label" htmlFor="join-code">
            Party code
          </label>
          <input
            id="join-code"
            className="input input-code"
            value={joinCode}
            onChange={(event) => setJoinCode(event.target.value)}
            placeholder="ABC123"
            maxLength={6}
            autoComplete="off"
          />
        </div>
        <button
          type="button"
          className="btn btn-secondary btn-block"
          onClick={handleJoin}
          disabled={!canAct || joinCode.trim().length === 0}
        >
          Join party
        </button>

        {error && (
          <p className="alert" role="alert">
            {error}
          </p>
        )}

        <InstallAppButton />
        <QuickGameButton />
      </div>
    </div>
  )
}
