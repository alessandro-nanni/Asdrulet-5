import { useEffect, useState } from 'react'
import { createStompClient } from '../../shared/ws/stompClient'
import { getCombat } from './api'
import type { CombatState } from './types'

interface CombatStateResult {
  combat: CombatState | null
  error: string | null
}

export function useCombatState(code: string): CombatStateResult {
  const [combat, setCombat] = useState<CombatState | null>(null)
  const [error, setError] = useState<string | null>(null)

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
        setCombat(JSON.parse(message.body) as CombatState)
      })
    }
    client.activate()

    return () => {
      cancelled = true
      client.deactivate()
    }
  }, [code])

  return { combat, error }
}
