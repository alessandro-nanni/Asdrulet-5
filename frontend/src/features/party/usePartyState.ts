import { useEffect, useState } from 'react'
import { createStompClient } from '../../shared/ws/stompClient'
import { getParty } from './api'
import type { PartyState } from './types'

interface PartyStateResult {
  party: PartyState | null
  error: string | null
}

export function usePartyState(code: string): PartyStateResult {
  const [party, setParty] = useState<PartyState | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setParty(null)
    setError(null)

    getParty(code)
      .then((state) => {
        if (!cancelled) setParty(state)
      })
      .catch(() => {
        if (!cancelled) setError('Party not found')
      })

    const client = createStompClient()
    client.onConnect = () => {
      client.subscribe(`/topic/party/${code}`, (message) => {
        setParty(JSON.parse(message.body) as PartyState)
      })
    }
    client.activate()

    return () => {
      cancelled = true
      client.deactivate()
    }
  }, [code])

  return { party, error }
}
