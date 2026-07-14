import { useEffect, useState } from 'react'
import { Portal } from '../../../shared/ui/Portal'
import { acknowledgeWheelResult, spinWheel } from '../../party/api'
import type { PartyMember, PartyState, WheelEffect } from '../../party/types'

interface Props {
  code: string
  selfId: string
  members: PartyMember[]
  wheelResults: Record<string, WheelEffect>
  turnOrder: string[]
  onApplyUpdate: (state: PartyState) => void
}

// Order here is only a tiebreaker for laying out whatever's left — as slices
// get claimed, the remaining ones are recomputed to fill the whole 360°
// rather than leaving a gap where a claimed slice used to be.
const SEGMENTS: WheelEffect[] = ['FULL_HEAL', 'HALVE_HEALTH', 'GIVE_ITEM', 'CLEAR_EFFECTS', 'POISON']
const EXTRA_SPINS = 5
const SPIN_DURATION_MS = 2600
// How long the result stays on screen, announced, before this client tells
// the server it's done looking — see acknowledgeWheelResult. Not a backend
// timer: the room only actually clears once every member's own client has
// paused like this and confirmed, so there's no fixed delay to tune against
// how long the spin animation itself takes.
const ANNOUNCE_PAUSE_MS = 3000

const EFFECT_INFO: Record<WheelEffect, { title: string; description: string; color: string }> = {
  FULL_HEAL: {
    title: 'Full Heal',
    description: 'Your wounds vanish. You start your next fight at full health.',
    color: '#3ddc84',
  },
  HALVE_HEALTH: {
    title: 'Cursed',
    description: 'A dark pulse cuts your health in half for your next fight.',
    color: '#ff4d4d',
  },
  GIVE_ITEM: {
    title: 'Treasure',
    description: 'A random item is equipped onto you, and only you.',
    color: '#ffb703',
  },
  CLEAR_EFFECTS: {
    title: 'Cleansed',
    description: 'Any lingering poison on you is purged.',
    color: '#38bdf8',
  },
  POISON: {
    title: 'Poisoned',
    description: 'A venomous curse takes hold for the first 4 turns of your next fight.',
    color: '#a855f7',
  },
}

const WHEEL_SIZE = 220
const WHEEL_CENTER = WHEEL_SIZE / 2
const WHEEL_RADIUS = 100
const LABEL_RADIUS = 62

function polarToCartesian(angleDeg: number, radius: number): { x: number; y: number } {
  // -90 so 0deg points straight up (12 o'clock), matching the pointer.
  const angleRad = ((angleDeg - 90) * Math.PI) / 180
  return {
    x: WHEEL_CENTER + radius * Math.cos(angleRad),
    y: WHEEL_CENTER + radius * Math.sin(angleRad),
  }
}

function sliceCornerPath(startAngle: number, endAngle: number): string {
  const start = polarToCartesian(endAngle, WHEEL_RADIUS)
  const end = polarToCartesian(startAngle, WHEEL_RADIUS)
  const largeArcFlag = endAngle - startAngle <= 180 ? 0 : 1
  return `M ${WHEEL_CENTER} ${WHEEL_CENTER} L ${start.x} ${start.y} A ${WHEEL_RADIUS} ${WHEEL_RADIUS} 0 ${largeArcFlag} 0 ${end.x} ${end.y} Z`
}

// Angle math is always relative to whatever `pool` currently is — a wheel
// with 5 unclaimed effects has 72° slices, one with 2 left has 180° slices,
// etc, so landing on a given effect only makes sense against the exact pool
// it was drawn from.
function rotationFor(effect: WheelEffect, pool: WheelEffect[]): number {
  const angle = 360 / pool.length
  const index = pool.indexOf(effect)
  const segmentCenter = index * angle + angle / 2
  return 360 * EXTRA_SPINS - segmentCenter
}

export function MysteryWheelScreen({ code, selfId, members, wheelResults, turnOrder, onApplyUpdate }: Props) {
  const [rotation, setRotation] = useState(0)
  const [isSpinning, setIsSpinning] = useState(false)
  const [hasSpun, setHasSpun] = useState(selfId in wheelResults)
  const [error, setError] = useState<string | null>(null)
  // Frozen the instant this client's own spin is submitted, so the wheel this
  // player is watching keeps the layout (and thus the landing angle) it had
  // when they clicked — even if someone else's spin shrinks the live pool a
  // moment later. Anyone not mid-spin just renders the live pool directly.
  const [spinPoolSnapshot, setSpinPoolSnapshot] = useState<WheelEffect[] | null>(null)

  const selfResult = wheelResults[selfId] ?? null
  const showResult = hasSpun && !isSpinning && selfResult != null
  const claimedEffects = new Set(Object.values(wheelResults))
  const availableEffects = SEGMENTS.filter((effect) => !claimedEffects.has(effect))
  const displayedPool = spinPoolSnapshot ?? availableEffects
  const segmentAngle = 360 / displayedPool.length
  const nextToSpinId = turnOrder.find((id) => !(id in wheelResults)) ?? null
  const isMyTurn = nextToSpinId === selfId
  const nextToSpinMember = members.find((member) => member.userId === nextToSpinId) ?? null
  const everyoneHasSpun = members.every((member) => member.userId in wheelResults)

  // Fires once the result is actually visible — whether that's right away
  // (rejoining a room where this member already spun) or after the spin
  // animation finishes — and tells the server this client is done looking,
  // after a brief pause. The room clears only once every member has done
  // this, so nobody's screen gets pulled out from under them mid-read.
  useEffect(() => {
    if (!showResult) {
      return
    }
    const timer = setTimeout(() => {
      acknowledgeWheelResult(code, selfId).catch(() => {})
    }, ANNOUNCE_PAUSE_MS)
    return () => clearTimeout(timer)
  }, [showResult, code, selfId])

  async function handleSpin() {
    if (isSpinning || hasSpun || !isMyTurn) {
      return
    }
    setError(null)
    const poolAtSpinTime = availableEffects
    setSpinPoolSnapshot(poolAtSpinTime)
    setIsSpinning(true)
    try {
      const updated = await spinWheel(code, selfId)
      const effect = updated.wheelResults[selfId]
      setRotation(rotationFor(effect, poolAtSpinTime))
      onApplyUpdate(updated)
      setHasSpun(true)
      setTimeout(() => setIsSpinning(false), SPIN_DURATION_MS)
    } catch {
      setError('Could not spin the wheel. Try again.')
      setIsSpinning(false)
      setSpinPoolSnapshot(null)
    }
  }

  function subtitle(): string {
    if (showResult) {
      return everyoneHasSpun ? 'Everyone has spun — resolving...' : 'Waiting for the rest of the party to spin...'
    }
    if (isSpinning) {
      return 'Spinning…'
    }
    if (isMyTurn) {
      return "It's your turn — give it a spin!"
    }
    if (nextToSpinMember) {
      return `Waiting for ${nextToSpinMember.displayName} to spin...`
    }
    return 'Everyone has spun.'
  }

  return (
    <Portal>
      <div className="wheel-screen">
        <h2 className="section-title">Mystery Wheel</h2>
        <p className="muted">{subtitle()}</p>

        <div className="wheel-shell">
          <div className="wheel-pointer" aria-hidden="true" />
          <svg
            className="wheel-dial"
            viewBox={`0 0 ${WHEEL_SIZE} ${WHEEL_SIZE}`}
            style={{ transform: `rotate(${rotation}deg)`, transitionDuration: isSpinning ? `${SPIN_DURATION_MS}ms` : '0ms' }}
          >
            {displayedPool.map((effect, index) => {
              const startAngle = index * segmentAngle
              const endAngle = startAngle + segmentAngle
              return (
                <path
                  key={effect}
                  d={sliceCornerPath(startAngle, endAngle)}
                  fill={EFFECT_INFO[effect].color}
                  stroke="rgba(10,8,20,0.5)"
                  strokeWidth={1.5}
                />
              )
            })}
            {displayedPool.map((effect, index) => {
              const midAngle = index * segmentAngle + segmentAngle / 2
              const pos = polarToCartesian(midAngle, LABEL_RADIUS)
              // No counter-rotation here on purpose: the text is meant to
              // spin along with the wheel (it's a child of the rotating
              // <svg>), just oriented radially within its own slice — rim to
              // center — rather than fighting the spin to stay screen-upright.
              const textRotation = midAngle + 90
              return (
                <text
                  key={effect}
                  x={pos.x}
                  y={pos.y}
                  textAnchor="middle"
                  dominantBaseline="middle"
                  className="wheel-segment-text"
                  style={{
                    transform: `rotate(${textRotation}deg)`,
                    transformOrigin: `${pos.x}px ${pos.y}px`,
                  }}
                >
                  {EFFECT_INFO[effect].title}
                </text>
              )
            })}
          </svg>
        </div>

        {!hasSpun && isMyTurn && (
          <button type="button" className="btn btn-primary btn-block" onClick={handleSpin} disabled={isSpinning}>
            {isSpinning ? 'Spinning…' : 'Spin the wheel'}
          </button>
        )}

        {showResult && selfResult && (
          <div className="wheel-result">
            <p className="wheel-result-title">{EFFECT_INFO[selfResult].title}!</p>
            <p className="muted">{EFFECT_INFO[selfResult].description}</p>
          </div>
        )}

        {error && (
          <p className="alert" role="alert">
            {error}
          </p>
        )}

        <ul className="wheel-member-list">
          {members.map((member) => {
            const effect = wheelResults[member.userId]
            const isNext = member.userId === nextToSpinId
            return (
              <li key={member.userId} className={`wheel-member-row${isNext ? ' is-current-turn' : ''}`}>
                <span>{member.displayName}</span>
                <span className="muted">
                  {effect ? EFFECT_INFO[effect].title : isNext ? "Spinning now…" : 'Waiting to spin…'}
                </span>
              </li>
            )
          })}
        </ul>
      </div>
    </Portal>
  )
}
