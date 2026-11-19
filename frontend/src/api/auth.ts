import { api, setAccessToken, type AuthResponse, type UserDto } from './client'

export interface RegisterInput {
  displayName: string
  email: string
  password: string
  acceptTerms: boolean
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await api<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
  setAccessToken(res.accessToken)
  return res
}

export async function register(input: RegisterInput): Promise<AuthResponse> {
  const res = await api<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(input),
  })
  setAccessToken(res.accessToken)
  return res
}

export async function logout(): Promise<void> {
  try {
    await api<void>('/api/auth/logout', { method: 'POST' })
  } finally {
    setAccessToken(null) // log out locally even if the server call fails
  }
}

export const getMe = () => api<UserDto>('/api/users/me')
