import {useEffect, useState} from 'react'
import {getItemDefinitions} from './api'
import type {ItemDefinition} from './types'

export function useItemDefinitions() {
    const [definitions, setDefinitions] = useState<ItemDefinition[]>([])
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        let cancelled = false

        getItemDefinitions()
            .then((result) => {
                if (!cancelled) {
                    setDefinitions(result)
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setError('Could not load item data.')
                }
            })

        return () => {
            cancelled = true
        }
    }, [])

    return {definitions, error}
}
