import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core'
import type { CardSummary } from '../api/cards'
import { ApiError } from '../api/client'
import { deleteDeck, getDeck, updateDeck, type Board, type DeckCard, type DeckFormat } from '../api/decks'
import CardSearchPanel from '../components/cards/CardSearchPanel'
import PrintingsPanel from '../components/cards/PrintingsPanel'
import DeckStats from '../components/decks/DeckStats'
import { MainZone, SideColumn, type DeckView } from '../components/decks/DeckZone'
import ExportDialog from '../components/decks/ExportDialog'
import ValidationBadge from '../components/decks/ValidationBadge'
import {
  FORMAT_LABEL,
  FORMATS,
  fromPrinting,
  fromSummary,
  toSummary,
  GROUP_LABEL,
  SORT_LABEL,
  zonesFor,
  type GroupBy,
  type SortBy,
} from '../components/decks/deckUtils'
import { useDeckCards } from '../components/decks/useDeckCards'
import Spinner from '../components/Spinner'

type DragData = { type: 'search'; card: CardSummary } | { type: 'deck'; card: DeckCard }

interface ViewPrefs {
  view: DeckView
  group: GroupBy
  sort: SortBy
}
const DEFAULT_PREFS: ViewPrefs = { view: 'columns', group: 'cmc', sort: 'name' }

function readPrefs(): ViewPrefs {
  try {
    return { ...DEFAULT_PREFS, ...JSON.parse(localStorage.getItem('deckViewPrefs') ?? '{}') }
  } catch {
    return DEFAULT_PREFS // storage unavailable (private mode) or corrupt: just use defaults
  }
}

const SIDE_HINT: Partial<Record<Board, string>> = {
  COMMANDER: 'Trage aici comandantul.',
  SIDEBOARD: 'Trage aici cărțile de sideboard.',
  MAYBEBOARD: 'Cărți de încercat.',
}

/**
 * The deck editor: card search on the left, the deck on a parchment sheet in the middle,
 * sideboard / commander as a column on the right. Cards are dragged between them.
 */
export default function DeckEditorPage() {
  const deckId = Number(useParams().deckId)
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()

  const deck = useQuery({ queryKey: ['deck', deckId], queryFn: () => getDeck(deckId), enabled: Number.isFinite(deckId) })
  const cards = useDeckCards(deckId)

  const [prefs, setPrefs] = useState<ViewPrefs>(readPrefs)
  const [selected, setSelected] = useState<CardSummary | null>(null)
  // a card already in the deck whose printing is being changed
  const [swapping, setSwapping] = useState<DeckCard | null>(null)
  const [hovered, setHovered] = useState<DeckCard | null>(null)
  const [dragging, setDragging] = useState<DragData | null>(null)
  const [exportOpen, setExportOpen] = useState(false)
  // import report handed over by the "My decks" page
  const [importReport, setImportReport] = useState(
    location.state as { unresolvedCards?: string[]; parseErrors?: string[] } | null,
  )

  useEffect(() => {
    try {
      localStorage.setItem('deckViewPrefs', JSON.stringify(prefs))
    } catch {
      // not remembered, not a problem
    }
  }, [prefs])

  const rename = useMutation({
    mutationFn: (changes: { name?: string; format?: DeckFormat }) => updateDeck(deckId, changes),
    onSuccess: (d) => queryClient.setQueryData(['deck', deckId], d),
  })
  const remove = useMutation({
    mutationFn: () => deleteDeck(deckId),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['deck', deckId] })
      navigate('/decks', { replace: true })
    },
  })

  // A small move threshold keeps plain clicks working (click = printings, drag = add).
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor),
  )

  if (deck.isPending) return <Spinner label="Se încarcă deckul…" />
  if (deck.isError) {
    const notFound = deck.error instanceof ApiError && deck.error.status === 404
    return (
      <div className="py-20 text-center text-stone-400">
        {notFound ? 'Deckul nu există.' : 'Deckul nu s-a putut încărca.'}{' '}
        <Link to="/decks" className="text-accent underline">Înapoi la deckuri</Link>
      </div>
    )
  }

  const d = deck.data
  const zones = zonesFor(d.format, d.cards)
  const sideZones = zones.filter((z) => z !== 'MAIN')
  const cardsIn = (board: Board) => d.cards.filter((c) => c.board === board)

  const showPreview = (card: DeckCard | null) => card && setHovered(card)

  const addFromSearch = (card: CardSummary, board: Board) => {
    const entry = fromSummary(card, board)
    if (entry) cards.add(entry, board)
  }

  const onDragStart = (e: DragStartEvent) => setDragging((e.active.data.current as DragData) ?? null)
  const onDragEnd = (e: DragEndEvent) => {
    setDragging(null)
    const data = e.active.data.current as DragData | undefined
    const board = e.over?.data.current?.board as Board | undefined
    if (!data || !board) return
    if (data.type === 'search') addFromSearch(data.card, board)
    else if (data.card.board !== board) cards.move(data.card, data.card.board, board)
  }

  const dragImage = dragging?.card.images.normal ?? dragging?.card.images.small
  // the preview keeps showing the last card pointed at, like MTGO
  const preview = hovered
  const selectClass = 'rounded-sm border ink-rule bg-transparent px-1.5 py-0.5 font-serif text-sm text-ink focus:outline-none focus:ring-1 focus:ring-accent'

  return (
    <DndContext sensors={sensors} onDragStart={onDragStart} onDragEnd={onDragEnd} onDragCancel={() => setDragging(null)}>
      <div className="grid gap-4 lg:h-[calc(100vh-6.5rem)] lg:min-h-[600px] lg:grid-cols-[340px_minmax(0,1fr)_150px] xl:grid-cols-[380px_minmax(0,1fr)_160px]">
        <CardSearchPanel onSelect={setSelected} onQuickAdd={(c) => addFromSearch(c, 'MAIN')} />

        {/* the parchment sheet */}
        <section className="parchment flex min-h-[520px] flex-col lg:min-h-0">
          <header className="flex flex-wrap items-center gap-3 px-5 pt-4 pb-2">
            <Link to="/decks" className="font-serif text-lg text-ink-faded hover:text-ink" aria-label="Înapoi la deckuri">←</Link>
            <input
              key={d.name}
              defaultValue={d.name}
              maxLength={100}
              aria-label="Numele deckului"
              onBlur={(e) => {
                const name = e.target.value.trim()
                if (name && name !== d.name) rename.mutate({ name })
              }}
              onKeyDown={(e) => e.key === 'Enter' && e.currentTarget.blur()}
              className="min-w-0 flex-1 rounded-sm bg-transparent px-1 font-serif text-2xl font-medium text-ink outline-none hover:bg-ink/5 focus:bg-ink/5"
            />
            <select
              value={d.format}
              onChange={(e) => rename.mutate({ format: e.target.value as DeckFormat })}
              aria-label="Formatul deckului"
              className={selectClass}
            >
              {FORMATS.map((f) => <option key={f} value={f}>{FORMAT_LABEL[f]}</option>)}
            </select>
            <ValidationBadge validation={d.validation} pending={cards.saving} />
          </header>

          <div className="mx-5 flex flex-wrap items-center gap-x-4 gap-y-2 border-y ink-rule py-2 font-serif text-sm">
            <div className="flex overflow-hidden rounded-sm border ink-rule" role="group" aria-label="Mod de afișare">
              {(['columns', 'list'] as const).map((v) => (
                <button
                  key={v}
                  type="button"
                  aria-pressed={prefs.view === v}
                  onClick={() => setPrefs((p) => ({ ...p, view: v }))}
                  className={`px-2.5 py-0.5 ${prefs.view === v ? 'bg-ink text-parchment' : 'text-ink-faded hover:text-ink'}`}
                >
                  {v === 'columns' ? 'Coloane' : 'Listă'}
                </button>
              ))}
            </div>
            <label className="flex items-center gap-1.5 text-ink-faded">
              Grupează după
              <select value={prefs.group} onChange={(e) => setPrefs((p) => ({ ...p, group: e.target.value as GroupBy }))} className={selectClass}>
                {(Object.keys(GROUP_LABEL) as GroupBy[]).map((g) => <option key={g} value={g}>{GROUP_LABEL[g]}</option>)}
              </select>
            </label>
            <label className="flex items-center gap-1.5 text-ink-faded">
              Sortează după
              <select value={prefs.sort} onChange={(e) => setPrefs((p) => ({ ...p, sort: e.target.value as SortBy }))} className={selectClass}>
                {(Object.keys(SORT_LABEL) as SortBy[]).map((s) => <option key={s} value={s}>{SORT_LABEL[s]}</option>)}
              </select>
            </label>
            <span className="text-xs italic text-ink-faded" aria-live="polite">
              {cards.saving ? 'Se salvează…' : cards.error ? 'Ultima modificare nu s-a salvat.' : 'Salvat'}
            </span>
            <span className="ml-auto flex gap-1">
              <button type="button" onClick={() => setExportOpen(true)} className="rounded-sm px-2 py-0.5 hover:bg-ink/10">
                Exportă
              </button>
              <button
                type="button"
                onClick={() => window.confirm(`Ștergi deckul „${d.name}”? Acțiunea nu poate fi anulată.`) && remove.mutate()}
                className="rounded-sm px-2 py-0.5 text-wax hover:bg-wax/10"
              >
                Șterge
              </button>
            </span>
          </div>

          {importReport && ((importReport.unresolvedCards?.length ?? 0) > 0 || (importReport.parseErrors?.length ?? 0) > 0) && (
            <div role="status" className="mx-5 mt-3 rounded-sm border border-wax/40 bg-wax/10 p-3 font-serif text-sm">
              <div className="flex justify-between">
                <strong>Importul nu a găsit toate cărțile</strong>
                <button type="button" onClick={() => setImportReport(null)} aria-label="Ascunde">×</button>
              </div>
              {(importReport.unresolvedCards?.length ?? 0) > 0 && (
                <p>Lipsesc: {importReport.unresolvedCards!.join(', ')}</p>
              )}
              {importReport.parseErrors?.map((err) => <p key={err}>{err}</p>)}
            </div>
          )}

          <div className="min-h-0 flex-1 overflow-y-auto px-2 pt-2">
            <MainZone
              board="MAIN"
              format={d.format}
              cards={cardsIn('MAIN')}
              view={prefs.view}
              group={prefs.group}
              sort={prefs.sort}
              add={cards.add}
              move={cards.move}
              onHover={showPreview}
              onOpen={setSwapping}
            />
          </div>

          <footer className="mx-5 border-t ink-rule py-3">
            <DeckStats cards={d.cards} />
          </footer>
        </section>

        {/* right column: card preview + sideboard / commander */}
        <aside className="flex min-h-0 flex-col gap-3 overflow-y-auto" aria-label="Zone secundare">
          <div className="hidden aspect-[488/680] w-full lg:block" aria-hidden>
            {preview?.images.normal && (
              <img src={preview.images.normal} alt="" className="w-full rounded-[4.75%/3.5%] shadow-lg shadow-black/70" />
            )}
          </div>
          {sideZones.map((board) => (
            <SideColumn
              key={board}
              board={board}
              format={d.format}
              cards={cardsIn(board)}
              sort={prefs.sort}
              limit={board === 'SIDEBOARD' ? 15 : board === 'COMMANDER' ? 2 : undefined}
              hint={SIDE_HINT[board] ?? ''}
              add={cards.add}
              move={cards.move}
              onHover={showPreview}
              onOpen={setSwapping}
            />
          ))}
        </aside>
      </div>

      <DragOverlay dropAnimation={null}>
        {dragImage ? <img src={dragImage} alt="" className="w-[124px] rotate-3 rounded-[4.75%/3.5%] shadow-2xl shadow-black" /> : null}
      </DragOverlay>

      {selected && (
        <PrintingsPanel
          card={selected}
          onClose={() => setSelected(null)}
          zones={zones}
          onAdd={(printing, board) => {
            cards.add(fromPrinting(selected, printing, board), board)
            setSelected(null)
          }}
        />
      )}
      {swapping && (
        <PrintingsPanel
          card={toSummary(swapping)}
          onClose={() => setSwapping(null)}
          onReplace={(printing) => {
            cards.replace(swapping, fromPrinting(toSummary(swapping), printing, swapping.board))
            setSwapping(null)
          }}
        />
      )}
      {exportOpen && <ExportDialog deckId={deckId} onClose={() => setExportOpen(false)} />}
    </DndContext>
  )
}
