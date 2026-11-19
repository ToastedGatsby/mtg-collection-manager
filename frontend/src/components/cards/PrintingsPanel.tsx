import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getPrintings, type CardSummary, type Printing } from '../../api/cards'
import type { Board } from '../../api/decks'
import { ZONE_LABEL } from '../decks/deckUtils'
import ManaCost from './ManaCost'
import { formatEur, formatUsd, RARITY_COLOR } from './format'

interface Props {
  card: CardSummary
  onClose: () => void
  /** when opened from a deck: add the selected printing to one of these zones */
  zones?: Board[]
  onAdd?: (printing: Printing, board: Board) => void
  /** when opened from a card already in the deck: swap its printing (art, set, price) */
  onReplace?: (printing: Printing) => void
}

/** "Which version?" panel: every printing of a card with its own art and prices. */
export default function PrintingsPanel({ card, onClose, zones, onAdd, onReplace }: Props) {
  const printings = useQuery({
    queryKey: ['printings', card.oracleId],
    queryFn: () => getPrintings(card.oracleId),
  })
  const [selectedId, setSelectedId] = useState<string | null>(card.defaultPrintingId)
  const selected: Printing | undefined =
    printings.data?.find((p) => p.scryfallId === selectedId) ?? printings.data?.[0]

  // Esc closes the panel
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const image = selected?.images.normal ?? card.images.normal

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label={card.name}
        onClick={(e) => e.stopPropagation()}
        className="grid max-h-[90vh] w-full max-w-4xl grid-cols-1 gap-5 overflow-hidden tome p-5 md:grid-cols-[300px_1fr]"
      >
        <div className="space-y-3">
          {image && (
            <img src={image} alt={card.name} width={488} height={680} className="w-full rounded-[4.75%/3.5%]" />
          )}
          {selected && onReplace && (
            <button
              type="button"
              disabled={selected.scryfallId === card.defaultPrintingId}
              onClick={() => onReplace(selected)}
              className="w-full rounded-sm bg-accent py-2 font-serif text-base font-medium text-ink hover:brightness-110 disabled:cursor-default disabled:bg-surface-3 disabled:text-stone-400"
            >
              {selected.scryfallId === card.defaultPrintingId ? 'Printarea folosită acum' : 'Folosește această printare'}
            </button>
          )}
          {selected && onAdd && zones && (
            <div className="flex gap-2 text-xs">
              {zones.map((zone) => (
                <button
                  key={zone}
                  type="button"
                  onClick={() => onAdd(selected, zone)}
                  className="flex-1 rounded-md bg-accent py-1.5 font-semibold text-surface hover:brightness-110"
                >
                  + {ZONE_LABEL[zone]}
                </button>
              ))}
            </div>
          )}
          {selected && (
            <div className="flex gap-2 text-xs">
              {selected.cardmarketUrl && (
                <a href={selected.cardmarketUrl} target="_blank" rel="noreferrer" className="flex-1 rounded-md bg-surface-3 py-1.5 text-center hover:text-white">
                  Cardmarket ↗
                </a>
              )}
              {selected.tcgplayerUrl && (
                <a href={selected.tcgplayerUrl} target="_blank" rel="noreferrer" className="flex-1 rounded-md bg-surface-3 py-1.5 text-center hover:text-white">
                  TCGplayer ↗
                </a>
              )}
            </div>
          )}
        </div>

        <div className="flex min-h-0 flex-col">
          <div className="mb-3 flex items-start justify-between gap-3">
            <div>
              <h2 className="font-serif text-2xl text-parchment">{card.name}</h2>
              <p className="flex items-center gap-2 text-sm text-stone-400">
                <ManaCost cost={card.manaCost} /> {card.typeLine}
              </p>
            </div>
            <button type="button" onClick={onClose} aria-label="Închide" className="rounded-md px-2 text-xl text-stone-400 hover:text-white">
              ×
            </button>
          </div>

          <h3 className="mb-2 font-serif text-base text-stone-300">
            {printings.data ? `${printings.data.length} ${printings.data.length === 1 ? 'printare' : 'printări'}` : 'Printări'}
          </h3>
          {printings.isPending && <p className="text-sm text-stone-500">Se încarcă…</p>}
          {printings.isError && <p className="text-sm text-red-400">Nu s-au putut încărca printările.</p>}

          {printings.data && (
            <div className="min-h-0 flex-1 overflow-y-auto rounded-md border border-surface-3">
              <table className="w-full text-sm">
                <thead className="sticky top-0 bg-surface-3 text-left text-xs text-stone-400">
                  <tr>
                    <th className="px-2 py-1.5 font-medium">Set</th>
                    <th className="px-2 py-1.5 font-medium">Raritate</th>
                    <th className="px-2 py-1.5 text-right font-medium">EUR</th>
                    <th className="px-2 py-1.5 text-right font-medium">EUR foil</th>
                    <th className="px-2 py-1.5 text-right font-medium">USD</th>
                  </tr>
                </thead>
                <tbody>
                  {printings.data.map((p) => (
                    <tr
                      key={p.scryfallId}
                      onClick={() => setSelectedId(p.scryfallId)}
                      className={`cursor-pointer border-t border-surface-3 hover:bg-surface-3/60 ${
                        p.scryfallId === selected?.scryfallId ? 'bg-accent/10' : ''
                      }`}
                    >
                      <td className="px-2 py-1.5">
                        <span className="font-medium text-white">{p.setName}</span>
                        <span className="ml-1.5 text-xs uppercase text-stone-500">
                          {p.setCode} #{p.collectorNumber}
                          {p.promo && ', promo'}
                        </span>
                      </td>
                      <td className={`px-2 py-1.5 capitalize ${RARITY_COLOR[p.rarity] ?? ''}`}>{p.rarity}</td>
                      <td className="px-2 py-1.5 text-right tabular-nums">{formatEur(p.prices.eur)}</td>
                      <td className="px-2 py-1.5 text-right tabular-nums">{formatEur(p.prices.eurFoil)}</td>
                      <td className="px-2 py-1.5 text-right tabular-nums">{formatUsd(p.prices.usd)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="mt-2 text-[11px] text-stone-500">
            Prețuri orientative: EUR = Cardmarket (trend), USD = TCGplayer (market), via Scryfall, actualizate zilnic.
          </p>
        </div>
      </div>
    </div>
  )
}
