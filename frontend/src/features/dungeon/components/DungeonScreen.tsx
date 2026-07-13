import { moveToNode } from '../api'
import { moveToNodeAsMember } from '../../dev/api'
import { useDungeonState } from '../useDungeonState'
import { DungeonMap } from './DungeonMap'
import type { PartyMember } from '../../party/types'

interface Props {
  code: string
  members: PartyMember[]
  isLeader: boolean
  selfId: string
  isGuestSession: boolean
}

export function DungeonScreen({ code, members, isLeader, selfId, isGuestSession }: Props) {
  const { dungeon, error, applyUpdate } = useDungeonState(code)

  if (error) {
    return (
      <p className="alert" role="alert">
        {error}
      </p>
    )
  }
  if (!dungeon) {
    return <p className="muted">Loading dungeon...</p>
  }

  async function handleSelectNode(nodeId: string) {
    // Apply the response immediately rather than waiting for the broadcast
    // round-trip back over the WebSocket — it still arrives moments later
    // but is a no-op then, since it's identical to what we just applied.
    if (isGuestSession) {
      applyUpdate(await moveToNodeAsMember(code, selfId, nodeId))
    } else {
      applyUpdate(await moveToNode(code, nodeId))
    }
  }

  return (
    <div className="dungeon-screen">
      <h2 className="section-title dungeon-title">Choose your path</h2>
      <DungeonMap
        dungeon={dungeon}
        members={members}
        isLeader={isLeader}
        onSelectNode={isLeader ? handleSelectNode : undefined}
      />
      {!isLeader && <p className="muted dungeon-waiting">Waiting for the leader to choose a path...</p>}
    </div>
  )
}
