import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { searchCards, type CardSearchParams, type CardSummary, type ColorMode } from '../../api/cards'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import ColorFilter from './ColorFilter'
import { shortPrice } from './format'

const TYPES = ['', 'Creature', 'Instant', 'Sorcery', 'Artifact', 'Enchantment', 'Planeswalker', 'Land', 'Battle', 'Legendary']
const FORMATS = ['', 'standard', 'pioneer', 'modern', 'legacy', 'vintage', 'pauper', 'commander']
const PAGE_SIZE = 40

interface Props {
  onSelect: (card: CardSummary) => void
}

/** Left side of the deckbuilder: search box + MTGO-style filters + image grid. */
export default function CardSearchPanel({ onSelect }: Props) {
  const [name, setName] = useState('')
  const [colors, setColors] = useState<Set<string>>(new Set())
  const [colorMode, setColorMode] = useState<ColorMode>('including')
  const [type, setType] = useState('')
  const [format, setFormat] = useState('')
  const [sort, setSort] = useState<NonNullable<CardSearchParams['sort']>>('name')
  const [page, setPage] = useState(0)

  const debouncedName = useDebouncedValue(name.trim())
  const params: CardSearchParams = {
    q: debouncedName,
    colors: [...colors].join(''),
    colorMode,
    type,
    format,
    sort,
    page,
    size: PAGE_SIZE,
  }
  const hasFilter = debouncedName.length >= 2 || colors.size > 0 || type !== '' || format !== ''

  const results = useQuery({
    queryKey: ['cards', params],
    queryFn: () => searchCards(params),
    enabled: hasFilter, // don't list 30,000 cards before the user asked for anything
    placeholderData: keepPreviousData, // keep showing old results while the next page loads
  })

  // any filter change starts again from the first page
  const resetPage = <T,>(setter: (v: T) => void) => (v: T) => {
    setter(v)
    setPage(0)
  }
  const toggleColor = (c: string) => {
    setColors((prev) => {
      const next = new Set(prev)
      if (next.has(c)) next.delete(c)
      else next.add(c)
      return next
    })
    setPage(0)
  }

  const data = results.data

  return (
    <section className="flex min-h-0 flex-col rounded-lg border border-surface-3 bg-surface-2">
      <div className="space-y-2 border-b border-surface-3 p-3">
        <input
          value={name}
          onChange={(e) => resetPage(setName)(e.target.value)}
          placeholder="Caută o carte… (ex: Lightning Bolt)"
          aria-label="Numele cărții"
          className="w-full rounded-md bg-surface px-3 py-2 text-sm outline-none ring-1 ring-surface-3 focus:ring-accent"
        />
        <ColorFilter selected={colors} onToggle={toggleColor} mode={colorMode} onModeChange={resetPage(setColorMode)} />
        <div className="flex flex-wrap gap-2 text-xs">
          <select value={type} onChange={(e) => resetPage(setType)(e.target.value)} aria-label="Tip" className="rounded-md bg-surface px-2 py-1">
            {TYPES.map((t) => <option key={t} value={t}>{t || 'Orice tip'}</option>)}
          </select>
          <select value={format} onChange={(e) => resetPage(setFormat)(e.target.value)} aria-label="Format" className="rounded-md bg-surface px-2 py-1">
            {FORMATS.map((f) => <option key={f} value={f}>{f ? `Legal în ${f}` : 'Orice format'}</option>)}
          </select>
          <select
            value={sort}
            onChange={(e) => resetPage(setSort)(e.target.value as typeof sort)}
            aria-label="Sortare"
            className="ml-auto rounded-md bg-surface px-2 py-1"
          >
            <option value="name">Sortare: nume</option>
            <option value="cmc">Sortare: cost (CMC)</option>
            <option value="price">Sortare: preț</option>
          </select>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-3">
        {!hasFilter && <p className="py-10 text-center text-sm text-zinc-500">Scrie cel puțin 2 litere sau alege un filtru.</p>}
        {hasFilter && results.isPending && <p className="py-10 text-center text-sm text-zinc-500">Se caută…</p>}
        {results.isError && <p className="py-10 text-center text-sm text-red-400">Căutarea a eșuat. Backend-ul rulează?</p>}
        {data && data.totalElements === 0 && <p className="py-10 text-center text-sm text-zinc-500">Nicio carte găsită.</p>}

        {data && data.totalElements > 0 && (
          <>
            <p className="mb-2 text-xs text-zinc-500">
              {data.totalElements.toLocaleString('ro-RO')} cărți
              {results.isFetching && ' · se actualizează…'}
            </p>
            <ul className="grid grid-cols-[repeat(auto-fill,minmax(130px,1fr))] gap-3">
              {data.content.map((card) => (
                <li key={card.oracleId}>
                  <button
                    type="button"
                    onClick={() => onSelect(card)}
                    className="group relative block w-full overflow-hidden rounded-[4.75%/3.5%] text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-accent"
                    title={card.name}
                  >
                    {card.images.normal ? (
                      <img
                        src={card.images.normal}
                        alt={card.name}
                        loading="lazy"
                        width={488}
                        height={680}
                        className="aspect-[488/680] w-full bg-surface-3 transition group-hover:scale-[1.03]"
                      />
                    ) : (
                      <div className="flex aspect-[488/680] items-center justify-center bg-surface-3 p-2 text-center text-xs">
                        {card.name}
                      </div>
                    )}
                    {shortPrice(card.prices) && (
                      <span className="absolute bottom-1.5 right-1.5 rounded bg-black/75 px-1.5 py-0.5 text-[11px] font-semibold text-white">
                        {shortPrice(card.prices)}
                      </span>
                    )}
                  </button>
                </li>
              ))}
            </ul>
            {data.totalPages > 1 && (
              <nav className="mt-4 flex items-center justify-center gap-3 text-sm" aria-label="Paginare">
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  className="rounded-md bg-surface-3 px-3 py-1 disabled:opacity-40"
                >
                  ← Înapoi
                </button>
                <span className="text-zinc-400">
                  {page + 1} / {data.totalPages}
                </span>
                <button
                  type="button"
                  disabled={page + 1 >= data.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                  className="rounded-md bg-surface-3 px-3 py-1 disabled:opacity-40"
                >
                  Înainte →
                </button>
              </nav>
            )}
          </>
        )}
      </div>
    </section>
  )
}
