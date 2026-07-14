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
  isEntering?: boolean
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

// Added evenly to both sides of the widest row's natural width, so there's
// always a bit of empty space to pan into horizontally — without this, a
// dungeon whose widest layer happens to be narrower than the viewport has
// nothing to scroll, so a left/right drag does nothing at all.
const HORIZONTAL_PAN_SLACK = 120

// Vertical zig-zag applied within a layer's row (alternating up/down by
// index) so same-layer nodes aren't all pinned to one dead-straight
// horizontal line. Straight rows are what made many edges run at nearly
// identical angles between rows, overlapping each other visually even with
// the bow on edgePath — breaking the row itself gives every node's edges a
// distinct enough angle to stay readable. Kept well under LAYER_GAP/2 so
// staggered nodes never drift close to the neighboring layer's row.
const ROW_STAGGER = 50

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

// A deterministic (not random — must stay stable across re-renders) offset
// derived from the edge's own id string, used to fan out edges that would
// otherwise run at nearly the same angle through the same stretch of the
// map. Range is roughly [-1, 1].
function edgeHash(key: string): number {
  let h = 0
  for (let i = 0; i < key.length; i++) {
    h = (h * 31 + key.charCodeAt(i)) | 0
  }
  return ((h % 1000) + 1000) % 1000 / 500 - 1
}

// Edges are drawn as curves rather than straight lines: a straight line
// between every source/target pair means any two edges heading in roughly
// the same direction overlap for most of their length, which is what made
// the map look tangled once nodes had 2-3 outgoing edges each plus the
// occasional two-layer skip edge. Bowing each edge out by a per-edge,
// layer-distance-scaled amount (skip edges — which otherwise cut straight
// through an entire row of unrelated nodes — bow out much further than
// adjacent-layer ones) spreads overlapping edges apart into distinct,
// readable paths. The endpoints are still pulled back by NODE_RADIUS first
// so the curve starts/ends right at the node's circle, same as before.
function edgePath(from: Point, to: Point, radius: number, layerDiff: number, key: string): string {
  const dx = to.x - from.x
  const dy = to.y - from.y
  const dist = Math.hypot(dx, dy) || 1
  const ux = dx / dist
  const uy = dy / dist
  const x1 = from.x + ux * radius
  const y1 = from.y + uy * radius
  const x2 = to.x - ux * radius
  const y2 = to.y - uy * radius

  const bowRange = layerDiff > 1 ? 70 : 24
  const bow = edgeHash(key) * bowRange
  const px = -uy
  const py = ux
  const midX = (x1 + x2) / 2 + px * bow
  const midY = (y1 + y2) / 2 + py * bow

  return `M ${x1} ${y1} Q ${midX} ${midY} ${x2} ${y2}`
}

// Sugiyama-style barycenter sweep: a node's indexInLayer only reflects the
// order the generator happened to create it in, which has no relation to
// which node in the previous layer actually connects to it — that mismatch,
// not just crossing edges themselves, is what made the map hard to read (a
// node's edges would zig across the whole row to reach parents/children
// positioned far from it). Repeatedly reordering each layer by the average
// x-position of its already-placed neighbors in the adjacent layer (first a
// downward pass using parents, then upward using children, alternating)
// pulls connected nodes toward each other column-wise, which is the standard
// heuristic for minimizing edge crossings in a layered graph.
const BARYCENTER_SWEEPS = 4

function layout(nodes: DungeonNode[]): { points: Map<string, Point>; width: number; height: number } {
  const nodesById = new Map(nodes.map((node) => [node.id, node]))
  const layerCount = Math.max(...nodes.map((node) => node.layer)) + 1

  const parentsById = new Map<string, string[]>()
  for (const node of nodes) {
    for (const targetId of node.nextNodeIds) {
      const parents = parentsById.get(targetId) ?? []
      parents.push(node.id)
      parentsById.set(targetId, parents)
    }
  }

  const order: string[][] = Array.from({ length: layerCount }, () => [])
  for (const node of nodes) {
    order[node.layer].push(node.id)
  }
  for (const layerIds of order) {
    layerIds.sort((a, b) => nodesById.get(a)!.indexInLayer - nodesById.get(b)!.indexInLayer)
  }

  for (let sweep = 0; sweep < BARYCENTER_SWEEPS; sweep++) {
    const downward = sweep % 2 === 0
    const layerRange = downward
      ? Array.from({ length: layerCount - 1 }, (_, i) => i + 1)
      : Array.from({ length: layerCount - 1 }, (_, i) => layerCount - 2 - i)

    for (const layer of layerRange) {
      const neighborLayer = downward ? layer - 1 : layer + 1
      const neighborPos = new Map(order[neighborLayer].map((id, idx) => [id, idx]))
      const scored = order[layer].map((id, idx) => {
        const neighborIds = downward ? (parentsById.get(id) ?? []) : nodesById.get(id)!.nextNodeIds
        const relevant = neighborIds.filter((nid) => nodesById.get(nid)?.layer === neighborLayer)
        const score =
          relevant.length > 0
            ? relevant.reduce((sum, nid) => sum + (neighborPos.get(nid) ?? 0), 0) / relevant.length
            : idx
        return { id, score, idx }
      })
      scored.sort((a, b) => a.score - b.score || a.idx - b.idx)
      order[layer] = scored.map((item) => item.id)
    }
  }

  const maxCountInAnyLayer = Math.max(...order.map((layerIds) => layerIds.length))

  // Graph flows top-to-bottom: layer -> y (grows with depth), position within
  // the barycenter-refined order -> x (each layer's row is centered
  // horizontally within the widest row).
  const width = MARGIN * 2 + (maxCountInAnyLayer - 1) * ROW_GAP + HORIZONTAL_PAN_SLACK
  const height = MARGIN * 2 + (layerCount - 1) * LAYER_GAP

  const points = new Map<string, Point>()
  order.forEach((layerIds, layer) => {
    const rowWidth = (layerIds.length - 1) * ROW_GAP
    const startX = (width - rowWidth) / 2
    layerIds.forEach((id, idx) => {
      const stagger = layerIds.length > 1 ? (idx % 2 === 0 ? -ROW_STAGGER : ROW_STAGGER) : 0
      points.set(id, {
        x: startX + idx * ROW_GAP,
        y: MARGIN + layer * LAYER_GAP + stagger,
      })
    })
  })
  return { points, width, height }
}

export function DungeonMap({ dungeon, members, isLeader, isEntering, onSelectNode }: Props) {
  const { points, width, height } = layout(dungeon.nodes)
  const containerRef = useRef<HTMLDivElement>(null)
  const panState = useRef<{ startX: number; startY: number; scrollLeft: number; scrollTop: number; moved: number } | null>(
    null,
  )

  const availableSet = new Set(dungeon.availableNodeIds)
  const clearedSet = new Set(dungeon.clearedNodeIds)
  const nodesById = new Map(dungeon.nodes.map((node) => [node.id, node]))
  const currentPoint = points.get(dungeon.currentNodeId)
  const isBrowsingLocked = dungeon.enteredNodeId != null

  // clearedNodeIds is recorded in the order the party actually cleared each
  // room (the graph is forward-only and a cleared room's home never moves
  // back), so consecutive pairs are exactly the edges that were taken —
  // everything else is either the home node's still-open options or a path
  // never chosen.
  const takenEdgeKeys = new Set<string>()
  for (let i = 0; i < dungeon.clearedNodeIds.length - 1; i++) {
    takenEdgeKeys.add(`${dungeon.clearedNodeIds[i]}-${dungeon.clearedNodeIds[i + 1]}`)
  }
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
              // Highlighted from the home node — always the anchor whose
              // next-room options are open right now, regardless of which
              // sibling (if any) is currently being previewed.
              const active = node.id === dungeon.homeNodeId
              const edgeKey = `${node.id}-${targetId}`
              const taken = takenEdgeKeys.has(edgeKey)
              const layerDiff = (nodesById.get(targetId)?.layer ?? node.layer + 1) - node.layer
              return (
                <path
                  key={edgeKey}
                  className={`dungeon-edge${taken ? ' dungeon-edge-taken' : ''}${active ? ' dungeon-edge-active' : ''}`}
                  d={edgePath(from, to, NODE_RADIUS, layerDiff, edgeKey)}
                />
              )
            })
          })}

          {dungeon.nodes.map((node) => {
            const point = points.get(node.id)
            if (!point) return null
            const isCurrent = node.id === dungeon.currentNodeId
            const isHome = node.id === dungeon.homeNodeId
            const isClearedOther = clearedSet.has(node.id) && !isHome
            // A genuine next-room option (not home itself, never a room
            // that's already been cleared).
            const isChoice = availableSet.has(node.id) && !isHome
            // Browsing (moving the preview between home and any of its
            // options) is only meaningful before anything's been entered,
            // and only the leader drives it. Home is always a valid target
            // to browse back to; a sibling choice is valid too — clicking
            // either just re-selects, it never re-triggers a request if
            // it's already where we are.
            const isClickable =
              isLeader && !isBrowsingLocked && !isCurrent && (isHome || isChoice)
            const classNames = [
              'dungeon-node',
              `dungeon-node-${node.roomType.toLowerCase()}`,
              isCurrent ? 'is-current' : '',
              isHome && !isCurrent ? 'is-home' : '',
              isClearedOther && !isCurrent ? 'is-visited' : '',
              isChoice && isClickable ? 'is-selectable' : '',
            ]
              .filter(Boolean)
              .join(' ')

            return (
              <g
                key={node.id}
                className={classNames}
                transform={`translate(${point.x} ${point.y})`}
                onClick={isClickable ? () => handleNodeClick(node.id) : undefined}
                role={isClickable ? 'button' : undefined}
                aria-label={
                  isClickable ? (isHome ? 'Return to current room' : `Move to ${node.roomType.toLowerCase()} room`) : undefined
                }
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
            className={`dungeon-party-marker${isEntering ? ' is-entering' : ''}`}
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
