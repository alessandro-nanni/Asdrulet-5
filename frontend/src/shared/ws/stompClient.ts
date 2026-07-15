import {Client} from '@stomp/stompjs'

// Relative to the current origin so this works both in dev (proxied by Vite
// to the backend, see vite.config.ts) and in production (same-origin, bundled).
export function createStompClient(): Client {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    return new Client({
        brokerURL: `${protocol}://${window.location.host}/ws`,
        reconnectDelay: 3000,
    })
}
