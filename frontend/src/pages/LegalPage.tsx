import { useParams } from 'react-router'

const TITLES: Record<string, string> = {
  terms: 'Termeni și condiții',
  privacy: 'Politica de confidențialitate',
  cookies: 'Politica de cookie-uri',
}

// The real texts will live in the backend (legal module, versioned) so consent can reference a version.
export default function LegalPage() {
  const { doc = '' } = useParams()
  return (
    <article className="max-w-3xl">
      <h1 className="font-serif text-3xl text-parchment">{TITLES[doc] ?? 'Document'}</h1>
      <p className="mt-2 text-sm text-stone-400">Versiunea 1.0, în lucru</p>
    </article>
  )
}
