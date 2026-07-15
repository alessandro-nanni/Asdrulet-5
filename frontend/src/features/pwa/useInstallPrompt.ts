import {useEffect, useState} from 'react'

interface BeforeInstallPromptEvent extends Event {
    prompt: () => Promise<void>
    userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

export function useInstallPrompt() {
    const [deferredEvent, setDeferredEvent] = useState<BeforeInstallPromptEvent | null>(null)
    const [installed, setInstalled] = useState(false)

    useEffect(() => {
        function handleBeforeInstallPrompt(event: Event) {
            event.preventDefault()
            setDeferredEvent(event as BeforeInstallPromptEvent)
        }

        function handleAppInstalled() {
            setInstalled(true)
            setDeferredEvent(null)
        }

        window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
        window.addEventListener('appinstalled', handleAppInstalled)
        return () => {
            window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
            window.removeEventListener('appinstalled', handleAppInstalled)
        }
    }, [])

    async function promptInstall() {
        if (!deferredEvent) return
        await deferredEvent.prompt()
        await deferredEvent.userChoice
        setDeferredEvent(null)
    }

    return {canInstall: !installed && deferredEvent !== null, promptInstall}
}
