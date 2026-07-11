import { useEffect, useState } from 'react'
import type { PartyMember } from '../types'

interface Props {
  members: PartyMember[]
  onSubmit: (order: string[]) => void
}

export function TurnOrderEditor({ members, onSubmit }: Props) {
  const memberIds = members.map((member) => member.userId).join(',')
  const [order, setOrder] = useState<string[]>(members.map((member) => member.userId))

  useEffect(() => {
    setOrder(memberIds ? memberIds.split(',') : [])
  }, [memberIds])

  function move(index: number, direction: -1 | 1) {
    const target = index + direction
    if (target < 0 || target >= order.length) return
    const next = [...order]
    ;[next[index], next[target]] = [next[target], next[index]]
    setOrder(next)
  }

  function nameFor(userId: string) {
    return members.find((member) => member.userId === userId)?.displayName ?? userId
  }

  return (
    <div>
      <ol className="turn-order-editor">
        {order.map((userId, index) => (
          <li key={userId} className="turn-order-editor-item">
            <span className="turn-order-index">{index + 1}</span>
            <span className="turn-order-editor-name">{nameFor(userId)}</span>
            <button
              type="button"
              className="icon-btn"
              onClick={() => move(index, -1)}
              disabled={index === 0}
              aria-label="Move up"
            >
              ▲
            </button>
            <button
              type="button"
              className="icon-btn"
              onClick={() => move(index, 1)}
              disabled={index === order.length - 1}
              aria-label="Move down"
            >
              ▼
            </button>
          </li>
        ))}
      </ol>
      <button type="button" className="btn btn-primary btn-block" onClick={() => onSubmit(order)}>
        Confirm turn order
      </button>
    </div>
  )
}
