import { useParams } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { usePartyState } from '../features/party/usePartyState'
import { PartyMemberList } from '../features/party/components/PartyMemberList'
import { ClassSelector } from '../features/party/components/ClassSelector'
import { TurnOrderEditor } from '../features/party/components/TurnOrderEditor'
import { QrCodeCard } from '../features/party/components/QrCodeCard'
import { selectClass, setTurnOrder } from '../features/party/api'

export function PartyLobbyPage() {
  const { code = '' } = useParams()
  const { user } = useAuth()
  const { party, error } = usePartyState(code.toUpperCase())

  if (error) {
    return <p role="alert">{error}</p>
  }
  if (!party || !user) {
    return <p>Loading party...</p>
  }

  const self = party.members.find((member) => member.userId === user.id)
  const isLeader = self?.leader ?? false

  return (
    <div>
      <h1>Party {party.code}</h1>
      <QrCodeCard code={party.code} />

      <PartyMemberList members={party.members} />

      <h2>Choose your class</h2>
      <ClassSelector
        selected={self?.characterClass ?? null}
        onSelect={(characterClass) => {
          void selectClass(party.code, characterClass)
        }}
      />

      {isLeader && (
        <>
          <h2>Set turn order</h2>
          <TurnOrderEditor
            members={party.members}
            onSubmit={(order) => {
              void setTurnOrder(party.code, order)
            }}
          />
        </>
      )}

      {party.turnOrder.length > 0 && (
        <div>
          <h2>Turn order</h2>
          <ol>
            {party.turnOrder.map((userId) => (
              <li key={userId}>
                {party.members.find((member) => member.userId === userId)?.displayName ?? userId}
              </li>
            ))}
          </ol>
        </div>
      )}
    </div>
  )
}
