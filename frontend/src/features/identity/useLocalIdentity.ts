import {useEffect, useState} from 'react'

const STORAGE_KEY = 'asdrulet5:identity'

export interface LocalIdentity {
    id: string
    displayName: string
    avatarUrl: string | null
}

function loadIdentity(): LocalIdentity {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
        try {
            const parsed = JSON.parse(raw) as Partial<LocalIdentity>
            if (typeof parsed.id === 'string' && parsed.id) {
                return {
                    id: parsed.id,
                    displayName: typeof parsed.displayName === 'string' ? parsed.displayName : '',
                    avatarUrl: typeof parsed.avatarUrl === 'string' ? parsed.avatarUrl : null,
                }
            }
        } catch {
            // Fall through and mint a fresh identity below.
        }
    }
    return {id: crypto.randomUUID(), displayName: '', avatarUrl: null}
}

/**
 * The whole identity system for this app: no login, just a persistent id
 * this browser generated once (crypto.randomUUID(), never sent anywhere
 * except as the acting-user id on requests this same browser makes) plus a
 * display name and avatar the player picks. The backend trusts whatever id
 * is presented — same model the old dev-only "quick game" tooling used, now
 * used everywhere.
 */
export function useLocalIdentity() {
    const [identity, setIdentity] = useState<LocalIdentity>(loadIdentity)

    useEffect(() => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(identity))
    }, [identity])

    function setDisplayName(displayName: string) {
        setIdentity((current) => ({...current, displayName}))
    }

    function setAvatarUrl(avatarUrl: string | null) {
        setIdentity((current) => ({...current, avatarUrl}))
    }

    return {identity, setDisplayName, setAvatarUrl}
}
