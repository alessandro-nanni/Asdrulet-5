import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { usePartyState } from '../features/party/usePartyState'
import { PartyMemberList } from '../features/party/components/PartyMemberList'
import { ClassSelector } from '../features/party/components/ClassSelector'
import { TurnOrderEditor } from '../features/party/components/TurnOrderEditor'
import { QrCodeCard } from '../features/party/components/QrCodeCard'
import { selectClass, setTurnOrder } from '../features/party/api'
import type { CharacterClass } from '../features/party/types'

export function PartyLobbyPage() {
  const { code = '' } = useParams()
  const { user } = useAuth()
  const { party, error } = usePartyState(code.toUpperCase())
  const [classError, setClassError] = useState<string | null>(null)

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

  async function handleSelectClass(characterClass: CharacterClass) {
    setClassError(null)
    try {
      await selectClass(party!.code, characterClass)
    } catch {
      setClassError('That class was just taken. Pick another one.')
    }
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

      <section className="card">
        <h2 className="section-title">Choose your class</h2>
        <ClassSelector members={party.members} selfUserId={user.id} onSelect={handleSelectClass} />
        {classError && (
          <p className="alert" role="alert">
            {classError}
          </p>
        )}
      </section>

      {isLeader && (
        <section className="card">
          <h2 className="section-title">Set turn order</h2>
          <TurnOrderEditor
            members={party.members}
            onSubmit={(order) => {
              void setTurnOrder(party.code, order)
            }}
          />
        </section>
      )}

      {party.turnOrder.length > 0 && (
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
      )}
    </div>
  )
}
