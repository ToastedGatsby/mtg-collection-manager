const DEMO_CONTAINERS = ['Toată colecția', 'Rares', 'Potential Commander', 'Trade binder']

// Layout placeholder: containers sidebar + inventory table.
export default function CollectionPage() {
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-[240px_1fr]">
      <aside className="space-y-1 rounded-lg border border-surface-3 bg-surface-2 p-3">
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-white">Containere</h2>
          <button className="rounded bg-accent px-2 py-0.5 text-xs font-bold text-surface">+ Nou</button>
        </div>
        {DEMO_CONTAINERS.map((name) => (
          <button key={name} className="block w-full rounded px-2 py-1.5 text-left text-sm hover:bg-surface-3">
            {name}
          </button>
        ))}
      </aside>
      <section className="rounded-lg border border-surface-3 bg-surface-2 p-4 text-sm text-stone-500">
        Tabelul cu cărțile din container (nume, set, stare, foil, cantitate, preț) apare aici.
      </section>
    </div>
  )
}
