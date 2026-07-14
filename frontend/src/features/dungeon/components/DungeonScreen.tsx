import { useState } from 'react'
import { selectNode } from '../api'
import { useDungeonState } from '../useDungeonState'
import { DungeonMap } from './DungeonMap'
import type { PartyMember } from '../../party/types'
import type { DungeonState, RoomType } from '../types'

interface Props {
  code: string
  members: PartyMember[]
  isLeader: boolean
  selfId: string
  onEnterRoom: () => Promise<void>
}

const ROOM_TYPE_LABELS: Record<RoomType, string> = {
  START: 'Starting Room',
  FIGHT: 'Fight Room',
  LOOT: 'Loot Room',
  MERCHANT: 'Merchant',
  BOSS: 'Boss Room',
}

const ROOM_TYPE_DESCRIPTIONS: Record<RoomType, string> = {
  START: 'Your journey begins here. Choose a path to move deeper into the dungeon.',
  FIGHT: 'You sense danger nearby. Enemies are waiting inside.',
  LOOT: 'Something valuable glints in the dark, worth a closer look.',
  MERCHANT: 'A traveling merchant has set up shop here.',
  BOSS: 'A powerful presence looms ahead. This is the final battle.',
}

// The full graph (including each node's nextNodeIds) is already in `dungeon`,
// so browsing to a next-room option only ever changes currentNodeId — home,
// availableNodeIds and clearedNodeIds are untouched until something is
// actually entered. Applying that right away — instead of waiting on the
// request round-trip — is what makes the marker start moving the instant you
// click; the eventual server response still arrives and reconciles via
// applyUpdate, but is a no-op by then since it matches what we already show.
function optimisticSelect(dungeon: DungeonState, nodeId: string): DungeonState {
  return { ...dungeon, currentNodeId: nodeId }
}

export function DungeonScreen({ code, members, isLeader, selfId, onEnterRoom }: Props) {
  const { dungeon, error, applyUpdate } = useDungeonState(code)
  const [isEntering, setIsEntering] = useState(false)

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
    if (nodeId === dungeon!.currentNodeId) return
    applyUpdate(optimisticSelect(dungeon!, nodeId))
    applyUpdate(await selectNode(code, selfId, nodeId))
  }

  async function handleEnter() {
    setIsEntering(true)
    try {
      // onEnterRoom (see PartyLobbyPage) already holds its own response back
      // until the swirl's minimum duration has elapsed, so a plain await
      // here is enough — no need for this component to also race a timer.
      await onEnterRoom()
    } finally {
      setIsEntering(false)
    }
  }

  const currentRoom = dungeon.nodes.find((node) => node.id === dungeon.currentNodeId)
  const hasSelectedNextRoom = dungeon.currentNodeId !== dungeon.homeNodeId

  return (
    <div className="dungeon-screen">
      <DungeonMap
        dungeon={dungeon}
        members={members}
        isLeader={isLeader}
        isEntering={isEntering}
        onSelectNode={isLeader ? handleSelectNode : undefined}
      />
      {!isLeader && <p className="muted dungeon-waiting">Waiting for the leader to choose a path...</p>}

      <div className="dungeon-controls">
        <div className="dungeon-room-info">
          <p className="dungeon-room-label">{currentRoom ? ROOM_TYPE_LABELS[currentRoom.roomType] : ''}</p>
          <p className="dungeon-room-description">
            {currentRoom ? ROOM_TYPE_DESCRIPTIONS[currentRoom.roomType] : ''}
          </p>
        </div>
        {/* Nothing to enter until a next room has been picked — the home
            room (including the very first, starting room) never shows an
            Enter button of its own. */}
        {isLeader && hasSelectedNextRoom && (
          <button
            type="button"
            className="btn btn-primary btn-block"
            onClick={handleEnter}
            disabled={isEntering}
          >
            Enter
          </button>
        )}
      </div>
    </div>
  )
}
