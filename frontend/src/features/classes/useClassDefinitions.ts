import {useEffect, useState} from 'react'
import {getClassDefinitions} from './api'
import type {ClassDefinition} from './types'

export function useClassDefinitions() {
    const [definitions, setDefinitions] = useState<ClassDefinition[]>([])
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        let cancelled = false

        getClassDefinitions()
            .then((result) => {
                if (!cancelled) {
                    setDefinitions(result)
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setError('Could not load class data.')
                }
            })

        return () => {
            cancelled = true
        }
    }, [])

    return {definitions, error}
}
