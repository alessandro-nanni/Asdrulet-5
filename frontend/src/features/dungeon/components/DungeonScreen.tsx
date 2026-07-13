import { moveToNode } from '../api'
import { moveToNodeAsMember } from '../../dev/api'
import { useDungeonState } from '../useDungeonState'
import { DungeonMap } from './DungeonMap'
import type { PartyMember } from '../../party/types'
import type { DungeonState, RoomType } from '../types'

interface Props {
  code: string
  members: PartyMember[]
  isLeader: boolean
  selfId: string
  isGuestSession: boolean
  onEnterCombat: () => void
}

const COMBAT_ROOM_TYPES: RoomType[] = ['FIGHT', 'BOSS']

const ROOM_TYPE_LABELS: Record<RoomType, string> = {
  START: 'Starting Room',
  FIGHT: 'Fight Room',
  LOOT: 'Loot Room',
  MERCHANT: 'Merchant',
  BOSS: 'Boss Room',
}

// The full graph (including each node's nextNodeIds) is already in `dungeon`,
// so the post-move state is fully predictable client-side. Applying it right
// away — instead of waiting on the request round-trip — is what makes the
// marker start moving the instant you click; the eventual server response
// still arrives and reconciles via applyUpdate, but is a no-op by then since
// it matches what we already show.
function optimisticMove(dungeon: DungeonState, nodeId: string): DungeonState {
  const targetNode = dungeon.nodes.find((node) => node.id === nodeId)
  return {
    ...dungeon,
    currentNodeId: nodeId,
    availableNodeIds: targetNode?.nextNodeIds ?? [],
    visitedNodeIds: dungeon.visitedNodeIds.includes(nodeId) ? dungeon.visitedNodeIds : [...dungeon.visitedNodeIds, nodeId],
  }
}

export function DungeonScreen({ code, members, isLeader, selfId, isGuestSession, onEnterCombat }: Props) {
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
    applyUpdate(optimisticMove(dungeon!, nodeId))
    if (isGuestSession) {
      applyUpdate(await moveToNodeAsMember(code, selfId, nodeId))
    } else {
      applyUpdate(await moveToNode(code, nodeId))
    }
  }

  const currentRoom = dungeon.nodes.find((node) => node.id === dungeon.currentNodeId)
  const isCombatRoom = currentRoom != null && COMBAT_ROOM_TYPES.includes(currentRoom.roomType)

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

      <div className="dungeon-controls">
        <p className="dungeon-room-label">{currentRoom ? ROOM_TYPE_LABELS[currentRoom.roomType] : ''}</p>
        {/* Loot/merchant rooms have no content yet — Enter only does
            something for combat rooms, so it's hidden everywhere else. */}
        {isLeader && isCombatRoom && (
          <button type="button" className="btn btn-primary btn-block" onClick={onEnterCombat}>
            Enter
          </button>
        )}
      </div>
    </div>
  )
}
