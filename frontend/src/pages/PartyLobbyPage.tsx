import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useLocalIdentity } from '../features/identity/useLocalIdentity'
import { usePartyState } from '../features/party/usePartyState'
import { PartyMemberList } from '../features/party/components/PartyMemberList'
import { ClassSelector } from '../features/party/components/ClassSelector'
import { TurnOrderEditor } from '../features/party/components/TurnOrderEditor'
import { QrCodeCard } from '../features/party/components/QrCodeCard'
import { enterRoom, selectClass, startGame } from '../features/party/api'
import { selectClassAsFakeMember } from '../features/dev/api'
import { useClassDefinitions } from '../features/classes/useClassDefinitions'
import { BattleScreen } from '../features/combat/components/BattleScreen'
import { DungeonScreen } from '../features/dungeon/components/DungeonScreen'
import { CoinChangeToast } from '../features/party/components/CoinChangeToast'
import type { CharacterClass } from '../features/party/types'

export function PartyLobbyPage() {
  const { code = '' } = useParams()
  const normalizedCode = code.toUpperCase()
  const { identity } = useLocalIdentity()
  const selfId = identity.id
  const { party, error, applyUpdate } = usePartyState(normalizedCode)
  const { definitions } = useClassDefinitions()
  const [classError, setClassError] = useState<string | null>(null)
  const [actingAsId, setActingAsId] = useState<string | null>(null)

  if (error) {
    return (
      <div className="page page-center">
        <div className="card card-hero">
          <p className="alert" role="alert">
            {error}
          </p>
          <Link to="/" className="btn btn-secondary btn-block">
            Back to home
          </Link>
        </div>
      </div>
    )
  }
  if (!party) {
    return (
      <div className="page page-center">
        <p className="muted">Loading party...</p>
      </div>
    )
  }

  const self = party.members.find((member) => member.userId === selfId)
  const isLeader = self?.leader ?? false
  const effectiveActingAsId = actingAsId ?? selfId
  const everyoneHasAClass = party.members.every((member) => member.characterClass !== null)

  async function handleSelectClass(characterClass: CharacterClass) {
    setClassError(null)
    try {
      // Apply the response immediately rather than waiting for the
      // broadcast round-trip back over the WebSocket — the broadcast still
      // arrives moments later (carrying updates from other players too) but
      // is a no-op here since it's identical to what we just applied.
      if (effectiveActingAsId === selfId) {
        applyUpdate(await selectClass(party!.code, selfId, characterClass))
      } else {
        applyUpdate(await selectClassAsFakeMember(party!.code, effectiveActingAsId, characterClass))
      }
    } catch {
      setClassError('That class was just taken. Pick another one.')
    }
  }

  async function handleStartGame(order: string[]) {
    applyUpdate(await startGame(party!.code, selfId, order))
  }

  async function handleEnterRoom() {
    // The server itself holds this response (and the broadcast it triggers)
    // back briefly so the entry swirl animation has time to play — see
    // RoomEntryDelay on the backend for why that has to happen server-side
    // rather than as a delay here.
    applyUpdate(await enterRoom(party!.code, selfId))
  }

  if (party.status === 'DUNGEON') {
    return (
      <div className="battle-page">
        <CoinChangeToast coins={party.coins} />
        <DungeonScreen
          code={party.code}
          members={party.members}
          isLeader={isLeader}
          selfId={selfId}
          storage={party.storage}
          wheelResults={party.wheelResults}
          lootResults={party.lootResults}
          turnOrder={party.turnOrder}
          definitions={definitions}
          coins={party.coins}
          shopStock={party.shopStock}
          onEnterRoom={handleEnterRoom}
          onApplyUpdate={applyUpdate}
        />
      </div>
    )
  }

  if (party.status === 'IN_PROGRESS') {
    return (
      <div className="battle-page">
        <CoinChangeToast coins={party.coins} />
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
                  {member.bot ? ' (bot)' : member.userId === selfId ? ' (you)' : ''}
                </option>
              ))}
            </select>
          </section>
        )}

        <BattleScreen code={party.code} members={party.members} actingAsId={effectiveActingAsId} />
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
                {member.bot ? ' (bot)' : member.userId === selfId ? ' (you)' : ''}
              </option>
            ))}
          </select>
        </section>
      )}

      <section className="card">
        <h2 className="section-title">Choose your class</h2>
        <ClassSelector
          members={party.members}
          selfUserId={effectiveActingAsId}
          onSelect={handleSelectClass}
          definitions={definitions}
        />
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
            <TurnOrderEditor members={party.members} onSubmit={handleStartGame} />
          ) : (
            <p className="muted">Waiting for everyone to choose a class...</p>
          )}
        </section>
      )}
    </div>
  )
}
