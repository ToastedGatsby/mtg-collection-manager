import { Link, NavLink, Outlet, useMatch, useNavigate } from 'react-router'
import { useAuth } from '../auth/authContext'

// active link: brass text with a thin brass underline, like a bookmark ribbon
const navClass = ({ isActive }: { isActive: boolean }) =>
  `px-1 py-1 font-serif text-[17px] border-b-2 transition ${
    isActive ? 'border-accent text-accent' : 'border-transparent text-stone-300 hover:text-parchment'
  }`

export default function AppLayout() {
  // the deck editor needs the whole screen width
  const wide = useMatch('/decks/:deckId') !== null
  return (
    <div className="flex min-h-full flex-col">
      <header className="border-b border-accent/25 bg-surface-2/80">
        <nav className={`mx-auto flex items-center gap-6 px-4 py-2.5 ${wide ? 'max-w-none' : 'max-w-7xl'}`}>
          <Link to="/" className="mr-4 font-display text-2xl leading-none text-parchment drop-shadow-[0_1px_0_rgb(0_0_0/0.6)]">
            MTG Vault
          </Link>
          <NavLink to="/decks" className={navClass}>Deckuri</NavLink>
          <NavLink to="/collection" className={navClass}>Colecție</NavLink>
          <div className="ml-auto flex items-center gap-2">
            <AccountArea />
          </div>
        </nav>
      </header>

      <main className={`mx-auto w-full flex-1 px-4 ${wide ? 'max-w-none py-4' : 'max-w-7xl py-8'}`}>
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
        <span className="text-sm text-stone-300">
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
          className="rounded-md px-3 py-2 text-sm text-stone-300 hover:bg-surface-3 hover:text-white"
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
    <footer className="border-t border-surface-3 bg-surface-2 text-xs text-stone-400">
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
