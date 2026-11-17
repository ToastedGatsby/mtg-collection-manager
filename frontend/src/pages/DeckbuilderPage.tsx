const COLORS = [
  { key: 'W', cls: 'bg-mana-w text-black' },
  { key: 'U', cls: 'bg-mana-u' },
  { key: 'B', cls: 'bg-mana-b' },
  { key: 'R', cls: 'bg-mana-r' },
  { key: 'G', cls: 'bg-mana-g' },
  { key: 'C', cls: 'bg-mana-c text-black' },
]

// Layout placeholder for the MTGO-style deckbuilder.
// Next steps: card search (left) -> printings panel -> drag & drop (dnd-kit) into the deck zone (right).
export default function DeckbuilderPage() {
  return (
    <div className="grid h-[70vh] grid-cols-1 gap-4 lg:grid-cols-[2fr_3fr]">
      <section className="flex flex-col rounded-lg border border-surface-3 bg-surface-2">
        <div className="border-b border-surface-3 p-3">
          <input
            placeholder="Caută o carte… (ex: Lightning Bolt)"
            className="w-full rounded-md bg-surface px-3 py-2 text-sm outline-none ring-accent focus:ring-1"
          />
        </div>
        <div className="flex-1 p-4 text-sm text-zinc-500">Rezultatele căutării apar aici.</div>
      </section>

      <section className="flex flex-col rounded-lg border border-surface-3 bg-surface-2">
        <div className="flex flex-wrap items-center gap-2 border-b border-surface-3 p-3">
          {COLORS.map((c) => (
            <button key={c.key} className={`h-7 w-7 rounded-full text-xs font-bold ${c.cls}`}>
              {c.key}
            </button>
          ))}
          <select className="ml-auto rounded-md bg-surface px-2 py-1 text-sm">
            <option>Sortare: CMC</option>
            <option>Culoare</option>
            <option>Tip</option>
            <option>Nume</option>
            <option>Preț</option>
          </select>
          <select className="rounded-md bg-surface px-2 py-1 text-sm">
            <option>Commander</option>
            <option>Standard</option>
            <option>Pioneer</option>
            <option>Modern</option>
            <option>Legacy</option>
            <option>Vintage</option>
            <option>Pauper</option>
          </select>
        </div>
        <div className="m-3 flex flex-1 items-center justify-center rounded-md border-2 border-dashed border-surface-3 text-sm text-zinc-500">
          Trage cărțile aici (Main / Sideboard / Commander)
        </div>
      </section>
    </div>
  )
}
