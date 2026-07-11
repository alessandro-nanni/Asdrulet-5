import { useNavigate } from 'react-router-dom'
import { useUsername } from '../identity/useUsername'
import { createParty } from '../party/api'
import { addFakeMembers } from './api'

const FAKE_MEMBER_COUNT = 3

export function QuickGameButton() {
  const navigate = useNavigate()
  const [username] = useUsername()

  if (!import.meta.env.DEV) {
    return null
  }

  async function handleQuickGame() {
    const party = await createParty(username.trim() || 'Dev')
    await addFakeMembers(party.code, FAKE_MEMBER_COUNT)
    navigate(`/party/${party.code}`)
  }

  return (
    <button type="button" className="btn btn-dev btn-block" onClick={handleQuickGame}>
      ⚡ Quick game (dev)
    </button>
  )
}
