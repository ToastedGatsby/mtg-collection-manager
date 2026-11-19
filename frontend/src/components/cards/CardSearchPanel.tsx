import { useRef, useState } from 'react'
import { useDraggable } from '@dnd-kit/core'
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
  /** double-click: add straight to the main deck */
  onQuickAdd?: (card: CardSummary) => void
}

/**
 * Left side of the deckbuilder: search box + MTGO-style filters + image grid.
 * Results can be dragged into the deck (it must be rendered inside a dnd-kit DndContext).
 */
export default function CardSearchPanel({ onSelect, onQuickAdd }: Props) {
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
    <section className="tome flex min-h-[420px] flex-col lg:min-h-0" aria-label="Căutare cărți">
      <div className="space-y-2 border-b border-surface-3 p-3">
        <input
          value={name}
          onChange={(e) => resetPage(setName)(e.target.value)}
          placeholder="Caută o carte, ex: Lightning Bolt"
          aria-label="Numele cărții"
          className="w-full rounded-sm bg-surface px-3 py-2 font-serif text-base text-parchment placeholder:text-stone-500 outline-none ring-1 ring-surface-3 focus:ring-accent"
        />
        <ColorFilter selected={colors} onToggle={toggleColor} mode={colorMode} onModeChange={resetPage(setColorMode)} />
        <div className="flex flex-wrap gap-2 text-xs">
          <select value={type} onChange={(e) => resetPage(setType)(e.target.value)} aria-label="Tip" className="rounded-sm border border-surface-3 bg-surface px-2 py-1 text-sm">
            {TYPES.map((t) => <option key={t} value={t}>{t || 'Orice tip'}</option>)}
          </select>
          <select value={format} onChange={(e) => resetPage(setFormat)(e.target.value)} aria-label="Format" className="rounded-sm border border-surface-3 bg-surface px-2 py-1 text-sm">
            {FORMATS.map((f) => <option key={f} value={f}>{f ? `Legal în ${f}` : 'Orice format'}</option>)}
          </select>
          <select
            value={sort}
            onChange={(e) => resetPage(setSort)(e.target.value as typeof sort)}
            aria-label="Sortare"
            className="ml-auto rounded-sm border border-surface-3 bg-surface px-2 py-1 text-sm"
          >
            <option value="name">Sortare: nume</option>
            <option value="cmc">Sortare: cost (CMC)</option>
            <option value="price">Sortare: preț</option>
          </select>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-3">
        {!hasFilter && <p className="py-10 text-center text-sm text-stone-500">Scrie cel puțin 2 litere sau alege un filtru.</p>}
        {hasFilter && results.isPending && <p className="py-10 text-center text-sm text-stone-500">Se caută…</p>}
        {results.isError && <p className="py-10 text-center text-sm text-red-400">Căutarea a eșuat. Backend-ul rulează?</p>}
        {data && data.totalElements === 0 && <p className="py-10 text-center text-sm text-stone-500">Nicio carte găsită.</p>}

        {data && data.totalElements > 0 && (
          <>
            <p className="mb-2 font-serif text-sm italic text-stone-400">
              {data.totalElements.toLocaleString('ro-RO')} {data.totalElements === 1 ? 'carte găsită' : 'cărți găsite'}
              {results.isFetching && ', se actualizează…'}
            </p>
            <ul className="grid grid-cols-[repeat(auto-fill,minmax(140px,1fr))] gap-3">
              {data.content.map((card) => (
                <ResultCard key={card.oracleId} card={card} onSelect={onSelect} onQuickAdd={onQuickAdd} />
              ))}
            </ul>
            {data.totalPages > 1 && (
              <nav className="mt-4 flex items-center justify-center gap-3 text-sm" aria-label="Paginare">
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  className="rounded-sm border border-surface-3 px-3 py-1 font-serif text-stone-300 hover:border-accent disabled:opacity-40"
                >
                  Pagina anterioară
                </button>
                <span className="text-stone-400">
                  {page + 1} / {data.totalPages}
                </span>
                <button
                  type="button"
                  disabled={page + 1 >= data.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                  className="rounded-sm border border-surface-3 px-3 py-1 font-serif text-stone-300 hover:border-accent disabled:opacity-40"
                >
                  Pagina următoare
                </button>
              </nav>
            )}
          </>
        )}
      </div>
    </section>
  )
}

function ResultCard({ card, onSelect, onQuickAdd }: { card: CardSummary } & Props) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `search-${card.oracleId}`,
    data: { type: 'search', card },
    disabled: !card.defaultPrintingId,
  })
  const price = shortPrice(card.prices)
  // single click opens the printings panel, but only if no second click follows (double-click = add)
  const clickTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const handleClick = () => {
    if (clickTimer.current) clearTimeout(clickTimer.current)
    clickTimer.current = setTimeout(() => onSelect(card), onQuickAdd ? 220 : 0)
  }
  const handleDoubleClick = () => {
    if (clickTimer.current) clearTimeout(clickTimer.current)
    onQuickAdd?.(card)
  }
  return (
    <li>
      <button
        ref={setNodeRef}
        type="button"
        {...attributes}
        {...listeners}
        onClick={handleClick}
        onDoubleClick={handleDoubleClick}
        className={`group relative block w-full touch-none overflow-hidden rounded-[4.75%/3.5%] text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-accent ${
          isDragging ? 'opacity-40' : ''
        }`}
        title={`${card.name} — click: printări, dublu-click sau trage: adaugă în deck`}
      >
        {card.images.normal ? (
          <img
            src={card.images.normal}
            alt={card.name}
            loading="lazy"
            draggable={false}
            width={488}
            height={680}
            className="aspect-[488/680] w-full bg-surface-3 transition group-hover:scale-[1.03]"
          />
        ) : (
          <div className="flex aspect-[488/680] items-center justify-center bg-surface-3 p-2 text-center text-xs">{card.name}</div>
        )}
        {price && (
          <span className="absolute bottom-1.5 right-1.5 rounded bg-black/75 px-1.5 py-0.5 text-[11px] font-semibold text-white">
            {price}
          </span>
        )}
      </button>
    </li>
  )
}
