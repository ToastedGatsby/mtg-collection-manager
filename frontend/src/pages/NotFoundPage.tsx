import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <div className="py-20 text-center">
      <h1 className="font-display text-5xl text-accent">404</h1>
      <p className="mt-2 text-stone-400">Pagina nu există.</p>
      <Link to="/" className="mt-4 inline-block text-accent underline">Înapoi acasă</Link>
    </div>
  )
}
