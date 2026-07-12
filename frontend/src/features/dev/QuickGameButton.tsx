import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useUsername } from '../identity/useUsername'
import { addFakeMembers, createDevParty } from './api'
import { setGuestSelfId } from './guestIdentity'

const MAX_PARTY_SIZE = 4
const PARTY_SIZES = Array.from({ length: MAX_PARTY_SIZE }, (_, index) => index + 1)

export function QuickGameButton() {
  const navigate = useNavigate()
  const [username] = useUsername()
  const [partySize, setPartySize] = useState(MAX_PARTY_SIZE)
  const [error, setError] = useState<string | null>(null)

  if (!import.meta.env.DEV) {
    return null
  }

  async function handleQuickGame() {
    setError(null)
    try {
      const party = await createDevParty(username.trim() || 'Dev')
      setGuestSelfId(party.code, party.leaderId)
      const extraMembers = partySize - 1
      if (extraMembers > 0) {
        await addFakeMembers(party.code, extraMembers)
      }
      navigate(`/party/${party.code}`)
    } catch {
      setError('Quick game is unavailable (dev tools disabled on the server).')
    }
  }

  return (
    <div className="quick-game">
      <div className="field">
        <label className="field-label" htmlFor="quick-game-size">
          Quick game party size
        </label>
        <select
          id="quick-game-size"
          className="input"
          value={partySize}
          onChange={(event) => setPartySize(Number(event.target.value))}
        >
          {PARTY_SIZES.map((size) => (
            <option key={size} value={size}>
              {size === 1 ? 'Solo' : `${size} players`}
            </option>
          ))}
        </select>
      </div>
      <button type="button" className="btn btn-dev btn-block" onClick={handleQuickGame}>
        ⚡ Quick game (dev, no sign-in)
      </button>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
