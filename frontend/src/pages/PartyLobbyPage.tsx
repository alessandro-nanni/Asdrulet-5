import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { usePartyState } from '../features/party/usePartyState'
import { PartyMemberList } from '../features/party/components/PartyMemberList'
import { ClassSelector } from '../features/party/components/ClassSelector'
import { TurnOrderEditor } from '../features/party/components/TurnOrderEditor'
import { QrCodeCard } from '../features/party/components/QrCodeCard'
import { selectClass, startGame } from '../features/party/api'
import { selectClassAsFakeMember } from '../features/dev/api'
import type { CharacterClass } from '../features/party/types'

export function PartyLobbyPage() {
  const { code = '' } = useParams()
  const { user } = useAuth()
  const { party, error } = usePartyState(code.toUpperCase())
  const [classError, setClassError] = useState<string | null>(null)
  const [actingAsId, setActingAsId] = useState<string | null>(null)

  if (error) {
    return (
      <div className="page page-center">
        <p className="alert" role="alert">
          {error}
        </p>
      </div>
    )
  }
  if (!party || !user) {
    return (
      <div className="page page-center">
        <p className="muted">Loading party...</p>
      </div>
    )
  }

  const self = party.members.find((member) => member.userId === user.id)
  const isLeader = self?.leader ?? false
  const effectiveActingAsId = actingAsId ?? user.id
  const everyoneHasAClass = party.members.every((member) => member.characterClass !== null)

  async function handleSelectClass(characterClass: CharacterClass) {
    setClassError(null)
    try {
      if (effectiveActingAsId === user!.id) {
        await selectClass(party!.code, characterClass)
      } else {
        await selectClassAsFakeMember(party!.code, effectiveActingAsId, characterClass)
      }
    } catch {
      setClassError('That class was just taken. Pick another one.')
    }
  }

  if (party.status === 'IN_PROGRESS') {
    return (
      <div className="page">
        <header className="lobby-header">
          <p className="eyebrow">Party code</p>
          <h1 className="party-code">{party.code}</h1>
        </header>

        <section className="card">
          <h2 className="section-title">The adventure begins...</h2>
          <p className="muted">Combat isn't built yet — this is just confirming the party has started.</p>
        </section>

        <section className="card">
          <h2 className="section-title">Turn order</h2>
          <ol className="turn-order-list">
            {party.turnOrder.map((userId, index) => (
              <li key={userId} className="turn-order-item">
                <span className="turn-order-index">{index + 1}</span>
                {party.members.find((member) => member.userId === userId)?.displayName ?? userId}
              </li>
            ))}
          </ol>
        </section>
      </div>
    )
  }

  return (
    <div className="page">
      <header className="lobby-header">
        <p className="eyebrow">Party code</p>
        <h1 className="party-code">{party.code}</h1>
      </header>

      <QrCodeCard code={party.code} />

      <section className="card">
        <h2 className="section-title">Party ({party.members.length})</h2>
        <PartyMemberList members={party.members} />
      </section>

      {import.meta.env.DEV && party.members.some((member) => member.bot) && (
        <section className="card dev-panel">
          <h2 className="section-title">Playing as (dev)</h2>
          <select
            className="input"
            value={effectiveActingAsId}
            onChange={(event) => setActingAsId(event.target.value)}
          >
            {party.members.map((member) => (
              <option key={member.userId} value={member.userId}>
                {member.displayName}
                {member.bot ? ' (bot)' : member.userId === user.id ? ' (you)' : ''}
              </option>
            ))}
          </select>
        </section>
      )}

      <section className="card">
        <h2 className="section-title">Choose your class</h2>
        <ClassSelector members={party.members} selfUserId={effectiveActingAsId} onSelect={handleSelectClass} />
        {classError && (
          <p className="alert" role="alert">
            {classError}
          </p>
        )}
      </section>

      {isLeader && (
        <section className="card">
          <h2 className="section-title">Set turn order</h2>
          {everyoneHasAClass ? (
            <TurnOrderEditor
              members={party.members}
              onSubmit={(order) => {
                void startGame(party.code, order)
              }}
            />
          ) : (
            <p className="muted">Waiting for everyone to choose a class...</p>
          )}
        </section>
      )}
    </div>
  )
}
