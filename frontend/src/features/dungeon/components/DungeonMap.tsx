import { useEffect, useRef } from 'react'
import { MemberAvatar } from '../../party/components/MemberAvatar'
import type { PartyMember } from '../../party/types'
import roomStart from '../../../assets/dungeon/room-start.png'
import roomFight from '../../../assets/dungeon/room-fight.png'
import roomLoot from '../../../assets/dungeon/room-loot.png'
import roomMerchant from '../../../assets/dungeon/room-merchant.png'
import roomBoss from '../../../assets/dungeon/room-boss.png'
import type { DungeonNode, DungeonState, RoomType } from '../types'

interface Props {
  dungeon: DungeonState
  members: PartyMember[]
  isLeader: boolean
  onSelectNode?: (nodeId: string) => void
}

// Fixed, non-normalized spacing: every node is exactly LAYER_GAP apart from
// its neighboring layer (vertically, since the graph flows top-to-bottom)
// and ROW_GAP apart from its neighbors within a layer (horizontally),
// regardless of how many nodes any given layer has. That fixed minimum
// (well above NODE_RADIUS*2) is what guarantees nodes never overlap.
const NODE_RADIUS = 42
const MARGIN = 80
const LAYER_GAP = 200
const ROW_GAP = 170
const ICON_SIZE = NODE_RADIUS * 1.4
const DRAG_CLICK_THRESHOLD = 6

const ROOM_ICONS: Record<RoomType, string> = {
  START: roomStart,
  FIGHT: roomFight,
  LOOT: roomLoot,
  MERCHANT: roomMerchant,
  BOSS: roomBoss,
}

// Formation offsets (as a fraction of NODE_RADIUS) for arranging 1-4 party
// members inside a node: single/pair/triangle/diamond.
const FORMATIONS: Record<number, Array<[number, number]>> = {
  1: [[0, 0]],
  2: [
    [-0.44, 0],
    [0.44, 0],
  ],
  3: [
    [0, -0.48],
    [-0.44, 0.34],
    [0.44, 0.34],
  ],
  4: [
    [0, -0.55],
    [-0.55, 0],
    [0.55, 0],
    [0, 0.55],
  ],
}

interface Point {
  x: number
  y: number
}

function layout(nodes: DungeonNode[]): { points: Map<string, Point>; width: number; height: number } {
  const layerCount = Math.max(...nodes.map((node) => node.layer)) + 1
  const countByLayer = new Map<number, number>()
  for (const node of nodes) {
    countByLayer.set(node.layer, (countByLayer.get(node.layer) ?? 0) + 1)
  }
  const maxCountInAnyLayer = Math.max(...countByLayer.values())

  // Graph flows top-to-bottom: layer -> y (grows with depth), indexInLayer
  // -> x (each layer's row is centered horizontally within the widest row).
  const width = MARGIN * 2 + (maxCountInAnyLayer - 1) * ROW_GAP
  const height = MARGIN * 2 + (layerCount - 1) * LAYER_GAP

  const points = new Map<string, Point>()
  for (const node of nodes) {
    const countInLayer = countByLayer.get(node.layer) ?? 1
    const rowWidth = (countInLayer - 1) * ROW_GAP
    const startX = (width - rowWidth) / 2
    points.set(node.id, {
      x: startX + node.indexInLayer * ROW_GAP,
      y: MARGIN + node.layer * LAYER_GAP,
    })
  }
  return { points, width, height }
}

export function DungeonMap({ dungeon, members, isLeader, onSelectNode }: Props) {
  const { points, width, height } = layout(dungeon.nodes)
  const containerRef = useRef<HTMLDivElement>(null)
  const panState = useRef<{ startX: number; startY: number; scrollLeft: number; scrollTop: number; moved: number } | null>(
    null,
  )

  const availableSet = new Set(dungeon.availableNodeIds)
  const visitedSet = new Set(dungeon.visitedNodeIds)
  const currentPoint = points.get(dungeon.currentNodeId)
  const formation = FORMATIONS[Math.min(members.length, 4)] ?? FORMATIONS[1]

  // Keep the party centered in view whenever it moves to a new node — this is
  // what makes panning viable once the map is wider than the screen (portrait
  // phones especially): the player is never left hunting for their position.
  // Deliberately keyed only on currentNodeId, not currentPoint/points: node
  // layout is fixed for the lifetime of a given dungeon (only the party's
  // position within it changes), so re-centering on every layout recompute
  // would be redundant.
  useEffect(() => {
    const container = containerRef.current
    if (!container || !currentPoint) return
    container.scrollTo({
      left: currentPoint.x - container.clientWidth / 2,
      top: currentPoint.y - container.clientHeight / 2,
    })
  }, [dungeon.currentNodeId])

  function handlePointerDown(event: React.PointerEvent<HTMLDivElement>) {
    const container = containerRef.current
    if (!container) return
    panState.current = {
      startX: event.clientX,
      startY: event.clientY,
      scrollLeft: container.scrollLeft,
      scrollTop: container.scrollTop,
      moved: 0,
    }
    container.setPointerCapture(event.pointerId)
  }

  function handlePointerMove(event: React.PointerEvent<HTMLDivElement>) {
    const container = containerRef.current
    const state = panState.current
    if (!container || !state) return
    const dx = event.clientX - state.startX
    const dy = event.clientY - state.startY
    state.moved = Math.max(state.moved, Math.abs(dx), Math.abs(dy))
    container.scrollLeft = state.scrollLeft - dx
    container.scrollTop = state.scrollTop - dy
  }

  function handlePointerUp(event: React.PointerEvent<HTMLDivElement>) {
    containerRef.current?.releasePointerCapture(event.pointerId)
    panState.current = null
  }

  function handleNodeClick(nodeId: string) {
    if ((panState.current?.moved ?? 0) > DRAG_CLICK_THRESHOLD) return
    onSelectNode?.(nodeId)
  }

  return (
    <div
      ref={containerRef}
      className="dungeon-map"
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerLeave={handlePointerUp}
    >
      <div className="dungeon-map-inner" style={{ width, height }}>
        <svg className="dungeon-map-svg" width={width} height={height} role="img" aria-label="Dungeon map">
          {dungeon.nodes.map((node) => {
            const from = points.get(node.id)
            if (!from) return null
            return node.nextNodeIds.map((targetId) => {
              const to = points.get(targetId)
              if (!to) return null
              const active = node.id === dungeon.currentNodeId
              return (
                <line
                  key={`${node.id}-${targetId}`}
                  className={`dungeon-edge${active ? ' dungeon-edge-active' : ''}`}
                  x1={from.x}
                  y1={from.y}
                  x2={to.x}
                  y2={to.y}
                />
              )
            })
          })}

          {dungeon.nodes.map((node) => {
            const point = points.get(node.id)
            if (!point) return null
            const isCurrent = node.id === dungeon.currentNodeId
            const isVisited = visitedSet.has(node.id)
            // Belt-and-suspenders alongside the backend's own guard: never
            // treat an already-visited node as a valid destination, even if
            // it somehow ended up in availableNodeIds.
            const isAvailable = isLeader && availableSet.has(node.id) && !isVisited
            const classNames = [
              'dungeon-node',
              `dungeon-node-${node.roomType.toLowerCase()}`,
              isCurrent ? 'is-current' : '',
              isVisited && !isCurrent ? 'is-visited' : '',
              isAvailable ? 'is-selectable' : '',
            ]
              .filter(Boolean)
              .join(' ')

            return (
              <g
                key={node.id}
                className={classNames}
                transform={`translate(${point.x} ${point.y})`}
                onClick={isAvailable ? () => handleNodeClick(node.id) : undefined}
                role={isAvailable ? 'button' : undefined}
                aria-label={isAvailable ? `Move to ${node.roomType.toLowerCase()} room` : undefined}
              >
                <circle className="dungeon-node-ring" cx="0" cy="0" r={NODE_RADIUS} />
                {/* Hidden on the current node — the party marker sits on top of it there, so the icon would just be clutter underneath. */}
                {!isCurrent && (
                  <image
                    className="dungeon-node-icon"
                    href={ROOM_ICONS[node.roomType]}
                    x={-ICON_SIZE / 2}
                    y={-ICON_SIZE / 2}
                    width={ICON_SIZE}
                    height={ICON_SIZE}
                  />
                )}
              </g>
            )
          })}
        </svg>

        {currentPoint && (
          <div
            className="dungeon-party-marker"
            style={{ left: currentPoint.x, top: currentPoint.y }}
            aria-hidden="true"
          >
            {members.map((member, index) => {
              const [ox, oy] = formation[index] ?? [0, 0]
              const spread = NODE_RADIUS * 0.8
              return (
                <div
                  key={member.userId}
                  className="dungeon-party-marker-avatar"
                  style={{
                    transform: `translate(-50%, -50%) translate(${ox * spread}px, ${oy * spread}px) scale(0.5)`,
                    zIndex: members.length - index,
                  }}
                >
                  <MemberAvatar member={member} />
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
