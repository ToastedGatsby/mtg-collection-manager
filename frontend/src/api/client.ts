// Thin fetch wrapper. In dev, VITE_API_URL is empty and Vite proxies /api to :8080.
// In production, set VITE_API_URL to the deployed backend (e.g. https://mtg-api.onrender.com).
const BASE_URL = import.meta.env.VITE_API_URL ?? ''

export class ApiError extends Error {
  readonly status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    credentials: 'include', // refresh-token cookie (HttpOnly) will travel with requests
    headers: { 'Content-Type': 'application/json', ...init.headers },
  })
  if (!res.ok) {
    // Backend returns RFC 9457 ProblemDetail: { title, detail, status }
    const body = await res.json().catch(() => null)
    throw new ApiError(res.status, body?.detail ?? res.statusText)
  }
  return res.status === 204 ? (undefined as T) : res.json()
}

export interface HealthResponse {
  status: string
  database: string
  time: string
}

export const getHealth = () => api<HealthResponse>('/api/health')
