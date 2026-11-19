import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getPrintings, type CardSummary, type Printing } from '../../api/cards'
import ManaCost from './ManaCost'
import { formatEur, formatUsd, RARITY_COLOR } from './format'

interface Props {
  card: CardSummary
  onClose: () => void
}

/** "Which version?" panel: every printing of a card with its own art and prices. */
export default function PrintingsPanel({ card, onClose }: Props) {
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
        className="grid max-h-[90vh] w-full max-w-4xl grid-cols-1 gap-5 overflow-hidden rounded-xl border border-surface-3 bg-surface-2 p-5 md:grid-cols-[300px_1fr]"
      >
        <div className="space-y-3">
          {image && (
            <img src={image} alt={card.name} width={488} height={680} className="w-full rounded-[4.75%/3.5%]" />
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
              <h2 className="text-lg font-semibold text-white">{card.name}</h2>
              <p className="flex items-center gap-2 text-sm text-zinc-400">
                <ManaCost cost={card.manaCost} /> {card.typeLine}
              </p>
            </div>
            <button type="button" onClick={onClose} aria-label="Închide" className="rounded-md px-2 text-xl text-zinc-400 hover:text-white">
              ×
            </button>
          </div>

          <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-zinc-500">
            Printări {printings.data && `(${printings.data.length})`}
          </h3>
          {printings.isPending && <p className="text-sm text-zinc-500">Se încarcă…</p>}
          {printings.isError && <p className="text-sm text-red-400">Nu s-au putut încărca printările.</p>}

          {printings.data && (
            <div className="min-h-0 flex-1 overflow-y-auto rounded-md border border-surface-3">
              <table className="w-full text-sm">
                <thead className="sticky top-0 bg-surface-3 text-left text-xs text-zinc-400">
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
                        <span className="ml-1.5 text-xs uppercase text-zinc-500">
                          {p.setCode} #{p.collectorNumber}
                          {p.promo && ' · promo'}
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
          <p className="mt-2 text-[11px] text-zinc-500">
            Prețuri orientative: EUR = Cardmarket (trend), USD = TCGplayer (market), via Scryfall, actualizate zilnic.
          </p>
        </div>
      </div>
    </div>
  )
}
