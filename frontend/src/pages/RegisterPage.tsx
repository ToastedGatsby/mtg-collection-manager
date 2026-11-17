import { Link } from 'react-router'

export default function RegisterPage() {
  return (
    <div className="mx-auto max-w-sm space-y-4 rounded-lg border border-surface-3 bg-surface-2 p-6">
      <h1 className="text-xl font-semibold text-white">Creează cont</h1>
      <form className="space-y-3">
        <input placeholder="Nume afișat" autoComplete="nickname" className="w-full rounded-md bg-surface px-3 py-2 text-sm" />
        <input type="email" placeholder="Email" autoComplete="email" className="w-full rounded-md bg-surface px-3 py-2 text-sm" />
        <input type="password" placeholder="Parolă (min. 12 caractere)" autoComplete="new-password" className="w-full rounded-md bg-surface px-3 py-2 text-sm" />
        {/* GDPR: unchecked by default; backend stores document version + timestamp in consent_log */}
        <label className="flex items-start gap-2 text-xs text-zinc-400">
          <input type="checkbox" required className="mt-0.5" />
          <span>
            Am citit și accept <Link to="/legal/terms" className="text-accent underline">Termenii</Link> și{' '}
            <Link to="/legal/privacy" className="text-accent underline">Politica de confidențialitate</Link>.
          </span>
        </label>
        <button type="submit" className="w-full rounded-md bg-accent py-2 text-sm font-semibold text-surface">
          Creează cont
        </button>
      </form>
    </div>
  )
}
