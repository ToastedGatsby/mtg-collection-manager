import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/authContext'
import FormField from '../components/FormField'

const MIN_PASSWORD = 12
const MAX_PASSWORD_BYTES = 72 // BCrypt limit, same check as the backend

interface Form {
  displayName: string
  email: string
  password: string
  confirmPassword: string
  acceptTerms: boolean
}

type FieldErrors = Partial<Record<keyof Form, string>>

/** Client-side checks give instant feedback; the backend validates everything again regardless. */
function validate(form: Form): FieldErrors {
  const errors: FieldErrors = {}
  const name = form.displayName.trim()
  if (name.length < 2 || name.length > 50) errors.displayName = 'Între 2 și 50 de caractere.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) errors.email = 'Adresă de email invalidă.'
  if (form.password.length < MIN_PASSWORD) errors.password = `Minimum ${MIN_PASSWORD} caractere.`
  else if (new TextEncoder().encode(form.password).length > MAX_PASSWORD_BYTES) errors.password = 'Parola este prea lungă.'
  if (form.confirmPassword !== form.password) errors.confirmPassword = 'Parolele nu coincid.'
  if (!form.acceptTerms) errors.acceptTerms = 'Trebuie să accepți termenii pentru a crea un cont.'
  return errors
}

export default function RegisterPage() {
  const { status, register } = useAuth()
  const [form, setForm] = useState<Form>({
    displayName: '',
    email: '',
    password: '',
    confirmPassword: '',
    acceptTerms: false, // GDPR: consent is never pre-checked
  })
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (status === 'authenticated') {
    return <Navigate to="/collection" replace />
  }

  const update = <K extends keyof Form>(key: K, value: Form[K]) => {
    setForm((f) => ({ ...f, [key]: value }))
    setFieldErrors((errs) => ({ ...errs, [key]: undefined }))
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSubmitting(true)
    try {
      await register({
        displayName: form.displayName.trim(),
        email: form.email.trim(),
        password: form.password,
        acceptTerms: form.acceptTerms,
      })
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setFieldErrors({ email: 'Există deja un cont cu acest email.' })
      } else if (err instanceof ApiError && err.status === 400) {
        setFieldErrors(err.fieldErrors as FieldErrors)
        setError('Verifică datele introduse.')
      } else {
        setError('Serverul nu răspunde. Încearcă din nou.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="tome mx-auto max-w-sm space-y-4 p-6">
      <h1 className="font-serif text-3xl text-parchment">Creează cont</h1>

      {error && (
        <p role="alert" className="rounded-md border border-red-500/40 bg-red-500/10 px-3 py-2 text-sm text-red-300">
          {error}
        </p>
      )}

      <form onSubmit={handleSubmit} className="space-y-3" noValidate>
        <FormField
          label="Nume afișat"
          name="displayName"
          autoComplete="nickname"
          value={form.displayName}
          error={fieldErrors.displayName}
          onChange={(e) => update('displayName', e.target.value)}
        />
        <FormField
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          value={form.email}
          error={fieldErrors.email}
          onChange={(e) => update('email', e.target.value)}
        />
        <FormField
          label="Parolă"
          name="password"
          type="password"
          autoComplete="new-password"
          hint={`Minimum ${MIN_PASSWORD} caractere. O frază lungă e mai sigură decât una scurtă și complicată.`}
          value={form.password}
          error={fieldErrors.password}
          onChange={(e) => update('password', e.target.value)}
        />
        <FormField
          label="Confirmă parola"
          name="confirmPassword"
          type="password"
          autoComplete="new-password"
          value={form.confirmPassword}
          error={fieldErrors.confirmPassword}
          onChange={(e) => update('confirmPassword', e.target.value)}
        />

        <div className="space-y-1">
          <label className="flex items-start gap-2 text-xs text-stone-400">
            <input
              type="checkbox"
              className="mt-0.5 accent-accent"
              checked={form.acceptTerms}
              aria-invalid={fieldErrors.acceptTerms ? true : undefined}
              onChange={(e) => update('acceptTerms', e.target.checked)}
            />
            <span>
              Am citit și accept{' '}
              <Link to="/legal/terms" target="_blank" className="text-accent underline">Termenii și condițiile</Link> și{' '}
              <Link to="/legal/privacy" target="_blank" className="text-accent underline">Politica de confidențialitate</Link>.
            </span>
          </label>
          {fieldErrors.acceptTerms && <p className="text-xs text-red-400">{fieldErrors.acceptTerms}</p>}
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-accent py-2 text-sm font-semibold text-surface transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitting ? 'Se creează contul…' : 'Creează cont'}
        </button>
      </form>

      <p className="text-center text-xs text-stone-400">
        Ai deja cont? <Link to="/login" className="text-accent underline">Autentifică-te</Link>
      </p>
    </div>
  )
}
