import {useEffect, useRef, useState} from 'react'
import {createStompClient} from '../../shared/ws/stompClient'
import {jsonEqual} from '../../shared/jsonEqual'
import {getDungeon} from './api'
import type {DungeonState} from './types'

interface DungeonStateResult {
    dungeon: DungeonState | null
    error: string | null
    applyUpdate: (state: DungeonState) => void
}

export function useDungeonState(code: string): DungeonStateResult {
    const [dungeon, setDungeon] = useState<DungeonState | null>(null)
    const [error, setError] = useState<string | null>(null)
    const dungeonRef = useRef<DungeonState | null>(null)
    dungeonRef.current = dungeon

    // Skips the update if it's identical to what's already shown — this is
    // what lets applyUpdate (a mutation's own REST response, applied
    // immediately instead of waiting on the broadcast round-trip) and the
    // subsequent WebSocket broadcast of that same change coexist without the
    // second arrival causing a redundant re-render.
    function applyUpdate(next: DungeonState) {
        if (!dungeonRef.current || !jsonEqual(dungeonRef.current, next)) {
            setDungeon(next)
        }
    }

    useEffect(() => {
        let cancelled = false
        setDungeon(null)
        setError(null)

        getDungeon(code)
            .then((state) => {
                if (!cancelled) setDungeon(state)
            })
            .catch(() => {
                if (!cancelled) setError('Dungeon not found')
            })

        const client = createStompClient()
        client.onConnect = () => {
            client.subscribe(`/topic/party/${code}/dungeon`, (message) => {
                applyUpdate(JSON.parse(message.body) as DungeonState)
            })
        }
        client.activate()

        return () => {
            cancelled = true
            client.deactivate()
        }
    }, [code])

    return {dungeon, error, applyUpdate}
}
