import { createContext, useContext } from 'react'
import type { UserDto } from '../api/client'
import type { RegisterInput } from '../api/auth'

/** loading = still asking the server whether the refresh cookie is valid (first render after page load). */
export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

export interface AuthContextValue {
  user: UserDto | null
  status: AuthStatus
  login: (email: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>')
  }
  return ctx
}
