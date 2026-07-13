import { useEffect, useRef, useState } from 'react'
import { createStompClient } from '../../shared/ws/stompClient'
import { jsonEqual } from '../../shared/jsonEqual'
import { getParty } from './api'
import type { PartyState } from './types'

interface PartyStateResult {
  party: PartyState | null
  error: string | null
  applyUpdate: (state: PartyState) => void
}

export function usePartyState(code: string): PartyStateResult {
  const [party, setParty] = useState<PartyState | null>(null)
  const [error, setError] = useState<string | null>(null)
  const partyRef = useRef<PartyState | null>(null)
  partyRef.current = party

  // Skips the update if it's identical to what's already shown — this is
  // what lets applyUpdate (a mutation's own REST response, applied
  // immediately instead of waiting on the broadcast round-trip) and the
  // subsequent WebSocket broadcast of that same change coexist without the
  // second arrival causing a redundant re-render.
  function applyUpdate(next: PartyState) {
    if (!partyRef.current || !jsonEqual(partyRef.current, next)) {
      setParty(next)
    }
  }

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
        applyUpdate(JSON.parse(message.body) as PartyState)
      })
    }
    client.activate()

    return () => {
      cancelled = true
      client.deactivate()
    }
  }, [code])

  return { party, error, applyUpdate }
}
