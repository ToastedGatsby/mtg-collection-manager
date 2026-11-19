import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../auth/authContext'

const navClass = ({ isActive }: { isActive: boolean }) =>
  `px-3 py-2 rounded-md text-sm font-medium transition ${
    isActive ? 'bg-surface-3 text-accent' : 'text-zinc-300 hover:bg-surface-2 hover:text-white'
  }`

export default function AppLayout() {
  return (
    <div className="flex min-h-full flex-col">
      <header className="border-b border-surface-3 bg-surface-2">
        <nav className="mx-auto flex max-w-7xl items-center gap-2 px-4 py-3">
          <Link to="/" className="mr-6 font-display text-lg font-bold tracking-wide text-accent">
            MTG Vault
          </Link>
          <NavLink to="/deckbuilder" className={navClass}>Deckbuilder</NavLink>
          <NavLink to="/collection" className={navClass}>Colecție</NavLink>
          <div className="ml-auto flex items-center gap-2">
            <AccountArea />
          </div>
        </nav>
      </header>

      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6">
        <Outlet />
      </main>

      <Footer />
    </div>
  )
}

function AccountArea() {
  const { status, user, logout } = useAuth()
  const navigate = useNavigate()

  if (status === 'loading') {
    return <span className="h-8 w-24 animate-pulse rounded-md bg-surface-3" aria-hidden />
  }

  if (status === 'authenticated' && user) {
    return (
      <>
        <span className="text-sm text-zinc-300">
          Salut, <span className="font-semibold text-white">{user.displayName}</span>
          {user.role === 'DEMO' && (
            <span className="ml-2 rounded bg-accent/20 px-1.5 py-0.5 text-xs text-accent">DEMO</span>
          )}
        </span>
        <button
          type="button"
          onClick={async () => {
            await logout()
            navigate('/')
          }}
          className="rounded-md px-3 py-2 text-sm text-zinc-300 hover:bg-surface-3 hover:text-white"
        >
          Logout
        </button>
      </>
    )
  }

  return (
    <>
      <NavLink to="/login" className={navClass}>Login</NavLink>
      <Link
        to="/register"
        className="rounded-md bg-accent px-3 py-2 text-sm font-semibold text-surface hover:brightness-110"
      >
        Creează cont
      </Link>
    </>
  )
}

function Footer() {
  return (
    <footer className="border-t border-surface-3 bg-surface-2 text-xs text-zinc-400">
      <div className="mx-auto max-w-7xl space-y-2 px-4 py-5">
        <div className="flex flex-wrap gap-4">
          <Link to="/legal/terms" className="hover:text-white">Termeni și condiții</Link>
          <Link to="/legal/privacy" className="hover:text-white">Politica de confidențialitate</Link>
          <Link to="/legal/cookies" className="hover:text-white">Politica de cookie-uri</Link>
        </div>
        {/* Required by the Wizards of the Coast Fan Content Policy */}
        <p>
          MTG Vault is unofficial Fan Content permitted under the Fan Content Policy. Not
          approved/endorsed by Wizards. Portions of the materials used are property of Wizards of
          the Coast. ©Wizards of the Coast LLC.
        </p>
        <p>
          Card data and images provided by{' '}
          <a href="https://scryfall.com" className="underline hover:text-white" target="_blank" rel="noreferrer">
            Scryfall
          </a>
          . Prices are indicative and come from third-party marketplaces.
        </p>
      </div>
    </footer>
  )
}
