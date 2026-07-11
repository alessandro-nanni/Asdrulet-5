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
      <ol>
        {order.map((userId, index) => (
          <li key={userId}>
            {nameFor(userId)}
            <button type="button" onClick={() => move(index, -1)} disabled={index === 0}>
              Up
            </button>
            <button
              type="button"
              onClick={() => move(index, 1)}
              disabled={index === order.length - 1}
            >
              Down
            </button>
          </li>
        ))}
      </ol>
      <button type="button" onClick={() => onSubmit(order)}>
        Confirm turn order
      </button>
    </div>
  )
}
