import { useQuery } from '@tanstack/react-query'
import { getHealth } from '../api/client'

export default function HomePage() {
  const health = useQuery({ queryKey: ['health'], queryFn: getHealth })

  return (
    <section className="space-y-6">
      <h1 className="font-serif text-5xl font-medium text-parchment">Colecția ta de Magic, organizată.</h1>
      <p className="max-w-2xl text-stone-400">
        Deckbuilder cu validare pe formate, containere pentru colecție și prețuri din TCGplayer și
        Cardmarket.
      </p>

      {/* Smoke test: proves frontend -> backend -> PostgreSQL works end to end */}
      <div className="inline-flex items-center gap-3 tome px-4 py-3 text-sm">
        <span
          className={`h-2.5 w-2.5 rounded-full ${
            health.isSuccess ? 'bg-emerald-400' : health.isError ? 'bg-red-500' : 'bg-stone-500'
          }`}
        />
        {health.isPending && 'Verific conexiunea cu backend-ul…'}
        {health.isError && 'Backend indisponibil — pornește aplicația Spring Boot pe portul 8080.'}
        {health.isSuccess && `Backend: ${health.data.status}, bază de date: ${health.data.database}`}
      </div>
    </section>
  )
}
