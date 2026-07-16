import {useEffect, useState} from 'react'
import {getSkillTrees} from './api'
import type {SkillTree} from './types'

export function useSkillTrees() {
    const [trees, setTrees] = useState<SkillTree[]>([])
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        let cancelled = false

        getSkillTrees()
            .then((result) => {
                if (!cancelled) {
                    setTrees(result)
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setError('Could not load skill tree data.')
                }
            })

        return () => {
            cancelled = true
        }
    }, [])

    return {trees, error}
}
