import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { LoginButton } from '../features/auth/LoginButton'
import { createParty, joinParty } from '../features/party/api'

export function LandingPage() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const [joinCode, setJoinCode] = useState('')
  const [error, setError] = useState<string | null>(null)

  if (loading) {
    return <p>Loading...</p>
  }

  if (!user) {
    return (
      <div>
        <h1>Asdrulet 5</h1>
        <LoginButton />
      </div>
    )
  }

  async function handleCreate() {
    const party = await createParty()
    navigate(`/party/${party.code}`)
  }

  async function handleJoin() {
    setError(null)
    try {
      const party = await joinParty(joinCode.trim().toUpperCase())
      navigate(`/party/${party.code}`)
    } catch {
      setError('Could not join that party. Check the code and try again.')
    }
  }

  return (
    <div>
      <h1>Asdrulet 5</h1>
      <p>Welcome, {user.displayName}</p>

      <button type="button" onClick={handleCreate}>
        Create party
      </button>

      <div>
        <input
          value={joinCode}
          onChange={(event) => setJoinCode(event.target.value)}
          placeholder="Party code"
        />
        <button type="button" onClick={handleJoin}>
          Join party
        </button>
      </div>

      {error && <p role="alert">{error}</p>}
    </div>
  )
}
