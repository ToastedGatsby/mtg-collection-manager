import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { exportDeck } from '../../api/decks'

/** Arena/MTGO text list with a copy button. */
export default function ExportDialog({ deckId, onClose }: { deckId: number; onClose: () => void }) {
  const text = useQuery({ queryKey: ['deck-export', deckId], queryFn: () => exportDeck(deckId), staleTime: 0 })
  const [copied, setCopied] = useState(false)

  const copy = async () => {
    if (!text.data) return
    await navigator.clipboard.writeText(text.data)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Export deck"
        onClick={(e) => e.stopPropagation()}
        className="parchment w-full max-w-lg space-y-3 p-6"
      >
        <h2 className="font-serif text-2xl">Exportă lista</h2><p className="font-serif text-sm italic text-ink-faded">Format MTG Arena, se poate lipi și în Moxfield sau MTGO.</p>
        <textarea
          readOnly
          value={text.isPending ? 'Se generează…' : text.isError ? 'Exportul a eșuat.' : text.data}
          className="h-72 w-full resize-none rounded-sm border ink-rule bg-parchment/60 p-3 font-serif text-sm text-ink"
          aria-label="Lista deckului"
        />
        <div className="flex justify-end gap-2">
          <button type="button" onClick={onClose} className="rounded-sm px-3 py-1.5 font-serif text-ink-faded hover:bg-ink/10">
            Închide
          </button>
          <button
            type="button"
            onClick={copy}
            disabled={!text.data}
            className="rounded-sm bg-ink px-3 py-1.5 font-serif text-parchment disabled:opacity-50"
          >
            {copied ? 'Copiat' : 'Copiază lista'}
          </button>
        </div>
      </div>
    </div>
  )
}
