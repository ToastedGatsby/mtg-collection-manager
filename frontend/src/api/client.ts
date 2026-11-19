// Fetch wrapper with JWT handling.
// In dev, VITE_API_URL is empty and Vite proxies /api to :8080 (same origin -> cookies just work).
// In production, set VITE_API_URL to the deployed backend (e.g. https://mtg-api.onrender.com).
const BASE_URL = import.meta.env.VITE_API_URL ?? ''

export class ApiError extends Error {
  readonly status: number
  /** Field-level validation messages from the backend ({ "password": "size must be ..." }). */
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export interface UserDto {
  id: number
  email: string
  displayName: string
  role: 'USER' | 'ADMIN' | 'DEMO'
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserDto
}

// ---------------------------------------------------------------------------
// Access token lives ONLY in memory (a module variable), never in localStorage:
// anything in localStorage can be read by injected scripts (XSS). After a page
// reload the token is gone, and we get a new one from the HttpOnly refresh cookie.
// ---------------------------------------------------------------------------
let accessToken: string | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

let onSessionExpired: (() => void) | null = null

/** AuthProvider registers this so a dead session (refresh failed) logs the UI out. */
export function setSessionExpiredHandler(handler: (() => void) | null) {
  onSessionExpired = handler
}

function send(path: string, init: RequestInit, withAuth: boolean): Promise<Response> {
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (withAuth && accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return fetch(`${BASE_URL}${path}`, { ...init, headers, credentials: 'include' })
}

async function parse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    // Backend errors are RFC 9457 ProblemDetail: { status, detail, errors? }
    const body = await res.json().catch(() => null)
    throw new ApiError(res.status, body?.detail ?? res.statusText ?? 'Request failed', body?.errors ?? {})
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T)
}

// ---------------------------------------------------------------------------
// Refresh. The server rotates refresh tokens and treats a reused one as theft
// (it revokes every session). So we must never send the same cookie twice:
//  - inside one tab: concurrent callers share one in-flight request;
//  - across tabs: a Web Lock makes tabs take turns, so the second tab sends
//    the new cookie the first tab just received.
// ---------------------------------------------------------------------------
let refreshInFlight: Promise<AuthResponse | null> | null = null

export function refreshSession(): Promise<AuthResponse | null> {
  refreshInFlight ??= refreshWithLock().finally(() => {
    refreshInFlight = null
  })
  return refreshInFlight
}

async function refreshWithLock(): Promise<AuthResponse | null> {
  const run = async (): Promise<AuthResponse | null> => {
    try {
      const res = await send('/api/auth/refresh', { method: 'POST' }, false)
      if (!res.ok) {
        setAccessToken(null)
        return null
      }
      const data = (await res.json()) as AuthResponse
      setAccessToken(data.accessToken)
      return data
    } catch {
      return null // backend unreachable: behave as logged out
    }
  }
  return 'locks' in navigator ? navigator.locks.request('mtg-vault-refresh', run) : run()
}

/**
 * Calls the API with the access token. If it has expired (401), refreshes once and retries.
 * Auth endpoints are excluded, otherwise a wrong password would trigger a refresh.
 */
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  let res = await send(path, init, true)

  if (res.status === 401 && !path.startsWith('/api/auth/')) {
    const refreshed = await refreshSession()
    if (refreshed) {
      res = await send(path, init, true)
    } else {
      onSessionExpired?.()
    }
  }
  return parse<T>(res)
}

/** Like api(), for endpoints that return plain text (e.g. deck export). */
export async function apiText(path: string): Promise<string> {
  let res = await send(path, { headers: { Accept: 'text/plain' } }, true)
  if (res.status === 401) {
    const refreshed = await refreshSession()
    if (refreshed) res = await send(path, { headers: { Accept: 'text/plain' } }, true)
    else onSessionExpired?.()
  }
  if (!res.ok) throw new ApiError(res.status, res.statusText)
  return res.text()
}

export interface HealthResponse {
  status: string
  database: string
  time: string
}

export const getHealth = () => api<HealthResponse>('/api/health')
