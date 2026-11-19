import type { DeckCard } from '../../api/decks'
import { formatEur, formatUsd } from '../cards/format'
import { colorPips, manaCurve, value } from './deckUtils'

const SYMBOL_URL = 'https://svgs.scryfall.io/card-symbols/'

/** Mana curve drawn in sepia ink, coloured symbols and value; recomputed instantly from the cards on screen. */
export default function DeckStats({ cards }: { cards: DeckCard[] }) {
  const curve = manaCurve(cards)
  const max = Math.max(1, ...curve)
  const pips = colorPips(cards)
  const totalPips = Object.values(pips).reduce((a, b) => a + b, 0)
  const { eur, usd } = value(cards)

  return (
    <div className="flex flex-wrap items-end gap-x-8 gap-y-3">
      <figure aria-label="Curba de mana" className="min-w-[240px] flex-1">
        <div className="flex h-14 items-end gap-1.5">
          {curve.map((n, cmc) => (
            <div key={cmc} className="flex flex-1 flex-col items-center gap-0.5">
              <span className="font-serif text-xs tabular-nums text-ink-faded">{n || ''}</span>
              <div
                className="w-full rounded-t-sm bg-ink/70"
                style={{ height: `${(n / max) * 36}px`, minHeight: n ? 2 : 0 }}
              />
            </div>
          ))}
        </div>
        <figcaption className="mt-1 flex gap-1.5 border-t ink-rule pt-0.5 text-center font-serif text-xs text-ink-faded">
          {curve.map((_, cmc) => (
            <span key={cmc} className="flex-1">{cmc === 7 ? '7+' : cmc}</span>
          ))}
        </figcaption>
      </figure>

      {totalPips > 0 && (
        <div aria-label="Simboluri de mana pe culori" className="flex items-center gap-3 pb-1">
          {Object.entries(pips)
            .filter(([, n]) => n > 0)
            .map(([color, n]) => (
              <span key={color} className="flex items-center gap-1 font-serif text-sm">
                <img src={`${SYMBOL_URL}${color}.svg`} alt={color} width={16} height={16} />
                {Math.round((n / totalPips) * 100)}%
              </span>
            ))}
        </div>
      )}

      <div className="pb-0.5 text-right font-serif">
        <p className="text-lg font-bold leading-tight">{formatEur(eur)}</p>
        <p className="text-sm text-ink-faded">{formatUsd(usd)}</p>
      </div>
    </div>
  )
}
