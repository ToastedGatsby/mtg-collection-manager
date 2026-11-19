import { useState } from 'react'
import type { CardSummary } from '../api/cards'
import CardSearchPanel from '../components/cards/CardSearchPanel'
import PrintingsPanel from '../components/cards/PrintingsPanel'

// Left: live card search. Right: the deck (drag & drop comes in the next step).
export default function DeckbuilderPage() {
  const [selected, setSelected] = useState<CardSummary | null>(null)

  return (
    <div className="grid h-[calc(100vh-11rem)] min-h-[520px] grid-cols-1 gap-4 lg:grid-cols-[3fr_2fr]">
      <CardSearchPanel onSelect={setSelected} />

      <section className="flex flex-col rounded-lg border border-surface-3 bg-surface-2">
        <div className="flex flex-wrap items-center gap-2 border-b border-surface-3 p-3 text-sm">
          <span className="font-semibold text-white">Deck nou</span>
          <select className="ml-auto rounded-md bg-surface px-2 py-1 text-xs" aria-label="Formatul deckului">
            <option>Commander</option>
            <option>Standard</option>
            <option>Pioneer</option>
            <option>Modern</option>
            <option>Legacy</option>
            <option>Vintage</option>
            <option>Pauper</option>
          </select>
        </div>
        <div className="m-3 flex flex-1 items-center justify-center rounded-md border-2 border-dashed border-surface-3 p-6 text-center text-sm text-zinc-500">
          Trage cărțile aici (Main / Sideboard / Commander) — urmează în pasul următor
        </div>
      </section>

      {selected && <PrintingsPanel card={selected} onClose={() => setSelected(null)} />}
    </div>
  )
}
