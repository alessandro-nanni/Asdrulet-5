import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { LoginButton } from '../features/auth/LoginButton'
import { joinParty } from '../features/party/api'

export function JoinPage() {
  const { code = '' } = useParams()
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return

    const normalizedCode = code.toUpperCase()
    joinParty(normalizedCode)
      .then(() => navigate(`/party/${normalizedCode}`, { replace: true }))
      .catch(() => setError('Could not join that party. Check the code and try again.'))
  }, [user, code, navigate])

  if (loading) {
    return <p>Loading...</p>
  }

  if (!user) {
    return (
      <div>
        <p>Sign in to join party {code}</p>
        <LoginButton />
      </div>
    )
  }

  if (error) {
    return <p role="alert">{error}</p>
  }

  return <p>Joining party {code}...</p>
}
