import { Link } from 'react-router'

export default function LoginPage() {
  return (
    <div className="mx-auto max-w-sm space-y-4 rounded-lg border border-surface-3 bg-surface-2 p-6">
      <h1 className="text-xl font-semibold text-white">Autentificare</h1>
      <form className="space-y-3">
        <input type="email" placeholder="Email" autoComplete="email" className="w-full rounded-md bg-surface px-3 py-2 text-sm" />
        <input type="password" placeholder="Parolă" autoComplete="current-password" className="w-full rounded-md bg-surface px-3 py-2 text-sm" />
        <button type="submit" className="w-full rounded-md bg-accent py-2 text-sm font-semibold text-surface">
          Intră în cont
        </button>
      </form>
      <button className="w-full rounded-md border border-accent py-2 text-sm text-accent">
        Intră cu contul demo
      </button>
      <p className="text-center text-xs text-zinc-400">
        Nu ai cont? <Link to="/register" className="text-accent underline">Creează unul</Link>
      </p>
    </div>
  )
}
