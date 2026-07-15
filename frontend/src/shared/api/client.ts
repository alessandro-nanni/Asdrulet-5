function readCookie(name: string): string | null {
    const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
    return match ? decodeURIComponent(match[1]) : null
}

export class ApiError extends Error {
    status: number

    constructor(status: number, message: string) {
        super(message)
        this.status = status
    }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const method = options.method ?? 'GET'
    const headers = new Headers(options.headers)

    if (method !== 'GET') {
        const csrfToken = readCookie('XSRF-TOKEN')
        if (csrfToken) {
            headers.set('X-XSRF-TOKEN', csrfToken)
        }
    }
    if (options.body) {
        headers.set('Content-Type', 'application/json')
    }

    const response = await fetch(path, {
        ...options,
        method,
        headers,
        credentials: 'include',
    })

    if (!response.ok) {
        throw new ApiError(response.status, `${method} ${path} failed with ${response.status}`)
    }
    if (response.status === 204) {
        return undefined as T
    }
    return await response.json() as Promise<T>
}

export const apiClient = {
    get: <T>(path: string) => request<T>(path),
    post: <T>(path: string, body?: unknown) =>
        request<T>(path, {method: 'POST', body: body ? JSON.stringify(body) : undefined}),
}
