import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/authContext'
import FormField from '../components/FormField'

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 401) return 'Email sau parolă greșită.'
    if (err.status === 429) return 'Prea multe încercări eșuate. Încearcă din nou peste 15 minute.'
    if (err.status === 400) return 'Completează corect emailul și parola.'
  }
  return 'Serverul nu răspunde. Încearcă din nou.'
}

export default function LoginPage() {
  const { status, login } = useAuth()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/collection'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // Already logged in (or just logged in): go where the user originally wanted to go.
  if (status === 'authenticated') {
    return <Navigate to={from} replace />
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email, password)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="tome mx-auto max-w-sm space-y-4 p-6">
      <h1 className="font-serif text-3xl text-parchment">Autentificare</h1>

      {error && (
        <p role="alert" className="rounded-md border border-red-500/40 bg-red-500/10 px-3 py-2 text-sm text-red-300">
          {error}
        </p>
      )}

      <form onSubmit={handleSubmit} className="space-y-3" noValidate>
        <FormField
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <FormField
          label="Parolă"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button
          type="submit"
          disabled={submitting || !email || !password}
          className="w-full rounded-md bg-accent py-2 text-sm font-semibold text-surface transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitting ? 'Se conectează…' : 'Intră în cont'}
        </button>
      </form>

      <button
        type="button"
        disabled
        title="Contul demo va fi disponibil după lansare"
        className="w-full cursor-not-allowed rounded-md border border-accent/40 py-2 text-sm text-accent/60"
      >
        Intră cu contul demo (în curând)
      </button>

      <p className="text-center text-xs text-stone-400">
        Nu ai cont? <Link to="/register" className="text-accent underline">Creează unul</Link>
      </p>
    </div>
  )
}
