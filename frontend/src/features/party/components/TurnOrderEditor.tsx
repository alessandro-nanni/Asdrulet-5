import { useEffect, useState } from 'react'
import type { PartyMember } from '../types'

interface Props {
  members: PartyMember[]
  onSubmit: (order: string[]) => void
}

export function TurnOrderEditor({ members, onSubmit }: Props) {
  const memberIds = members.map((member) => member.userId).join(',')
  const [order, setOrder] = useState<string[]>(members.map((member) => member.userId))
  const [draggedId, setDraggedId] = useState<string | null>(null)

  useEffect(() => {
    setOrder(memberIds ? memberIds.split(',') : [])
  }, [memberIds])

  useEffect(() => {
    if (!draggedId) return

    function handleMove(event: PointerEvent) {
      const target = document.elementFromPoint(event.clientX, event.clientY)
      const item = target?.closest<HTMLElement>('[data-user-id]')
      const overId = item?.dataset.userId
      if (!overId || overId === draggedId) return

      setOrder((current) => {
        const from = current.indexOf(draggedId!)
        const to = current.indexOf(overId)
        if (from === -1 || to === -1 || from === to) return current
        const next = [...current]
        next.splice(from, 1)
        next.splice(to, 0, draggedId!)
        return next
      })
    }

    function handleUp() {
      setDraggedId(null)
    }

    document.addEventListener('pointermove', handleMove)
    document.addEventListener('pointerup', handleUp)
    document.addEventListener('pointercancel', handleUp)
    return () => {
      document.removeEventListener('pointermove', handleMove)
      document.removeEventListener('pointerup', handleUp)
      document.removeEventListener('pointercancel', handleUp)
    }
  }, [draggedId])

  function nameFor(userId: string) {
    return members.find((member) => member.userId === userId)?.displayName ?? userId
  }

  return (
    <div>
      <ol className="turn-order-editor">
        {order.map((userId, index) => (
          <li
            key={userId}
            data-user-id={userId}
            className={`turn-order-editor-item ${draggedId === userId ? 'is-dragging' : ''}`}
          >
            <span className="turn-order-index">{index + 1}</span>
            <span className="turn-order-editor-name">{nameFor(userId)}</span>
            <span
              className="drag-handle"
              aria-label={`Drag to reorder ${nameFor(userId)}`}
              onPointerDown={() => setDraggedId(userId)}
            >
              ⠿
            </span>
          </li>
        ))}
      </ol>
      <button type="button" className="btn btn-primary btn-block" onClick={() => onSubmit(order)}>
        Start
      </button>
    </div>
  )
}
