import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import * as authApi from '../api/auth'
import { refreshSession, setSessionExpiredHandler, type UserDto } from '../api/client'
import { AuthContext, type AuthContextValue, type AuthStatus } from './authContext'

export default function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<UserDto | null>(null)
  const [status, setStatus] = useState<AuthStatus>('loading')

  const clearSession = useCallback(() => {
    setUser(null)
    setStatus('anonymous')
    queryClient.clear() // drop cached data that belonged to the previous user
  }, [queryClient])

  // On page load: the access token is gone (memory only), but the HttpOnly refresh cookie may still
  // be valid. Ask for a new access token; if that works, the user stays logged in across reloads.
  useEffect(() => {
    let cancelled = false
    refreshSession().then((session) => {
      if (cancelled) return
      setUser(session?.user ?? null)
      setStatus(session ? 'authenticated' : 'anonymous')
    })
    setSessionExpiredHandler(clearSession)
    return () => {
      cancelled = true
      setSessionExpiredHandler(null)
    }
  }, [clearSession])

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login(email, password)
    setUser(res.user)
    setStatus('authenticated')
  }, [])

  const register = useCallback(async (input: authApi.RegisterInput) => {
    const res = await authApi.register(input)
    setUser(res.user)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      clearSession()
    }
  }, [clearSession])

  const value = useMemo<AuthContextValue>(
    () => ({ user, status, login, register, logout }),
    [user, status, login, register, logout],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
