import { useEffect, useRef, useState } from 'react'
import { createStompClient } from '../../shared/ws/stompClient'
import { jsonEqual } from '../../shared/jsonEqual'
import { getCombat } from './api'
import type { CombatState } from './types'

interface CombatStateResult {
  combat: CombatState | null
  error: string | null
  applyUpdate: (state: CombatState) => void
}

export function useCombatState(code: string): CombatStateResult {
  const [combat, setCombat] = useState<CombatState | null>(null)
  const [error, setError] = useState<string | null>(null)
  const combatRef = useRef<CombatState | null>(null)
  combatRef.current = combat

  // Skips the update if it's identical to what's already shown — this is
  // what lets applyUpdate (a mutation's own REST response, applied
  // immediately instead of waiting on the broadcast round-trip) and the
  // subsequent WebSocket broadcast of that same change coexist without the
  // second arrival causing a redundant re-render or re-triggering combat
  // animations.
  function applyUpdate(next: CombatState) {
    if (!combatRef.current || !jsonEqual(combatRef.current, next)) {
      setCombat(next)
    }
  }

  useEffect(() => {
    let cancelled = false
    setCombat(null)
    setError(null)

    getCombat(code)
      .then((state) => {
        if (!cancelled) setCombat(state)
      })
      .catch(() => {
        if (!cancelled) setError('Combat not found')
      })

    const client = createStompClient()
    client.onConnect = () => {
      client.subscribe(`/topic/party/${code}/combat`, (message) => {
        applyUpdate(JSON.parse(message.body) as CombatState)
      })
    }
    client.activate()

    return () => {
      cancelled = true
      client.deactivate()
    }
  }, [code])

  return { combat, error, applyUpdate }
}
