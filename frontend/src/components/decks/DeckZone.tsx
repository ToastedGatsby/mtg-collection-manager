import { useDraggable, useDroppable } from '@dnd-kit/core'
import type { Board, DeckCard, DeckFormat } from '../../api/decks'
import ManaCost from '../cards/ManaCost'
import { formatEur } from '../cards/format'
import { count, groupCards, otherZone, sortCards, ZONE_LABEL, type GroupBy, type SortBy } from './deckUtils'

export type DeckView = 'columns' | 'list'

export interface CardActions {
  add: (card: DeckCard, board: Board, delta?: number) => void
  move: (card: DeckCard, from: Board, to: Board) => void
  onHover: (card: DeckCard | null) => void
  /** click on a card in the deck: choose another printing */
  onOpen: (card: DeckCard) => void
}

interface MainProps extends CardActions {
  board: Board
  format: DeckFormat
  cards: DeckCard[]
  view: DeckView
  group: GroupBy
  sort: SortBy
}

const CARD_W = 'w-[124px]'
// a 124px-wide card is ~173px tall; stacked cards show only their 26px title strip
const STACK_OVERLAP = '-mt-[147px]'

/** The main deck, written on the parchment: MTGO-style columns or a list, grouped and sorted. */
export function MainZone({ board, format, cards, view, group, sort, ...actions }: MainProps) {
  const { setNodeRef, isOver } = useDroppable({ id: `zone-${board}`, data: { board } })
  const groups = groupCards(cards, group, sort)

  return (
    <section
      ref={setNodeRef}
      aria-label={ZONE_LABEL[board]}
      className={`min-h-[240px] rounded-sm p-3 transition ${isOver ? 'bg-accent/15 ring-2 ring-accent/60' : ''}`}
    >
      <ZoneTitle label={ZONE_LABEL[board]} total={count(cards)} />

      {cards.length === 0 ? (
        <p className="py-16 text-center font-serif italic text-ink-faded">
          Trage cărți aici din căutare, sau dă dublu-click pe una.
        </p>
      ) : view === 'columns' ? (
        <div className="flex gap-3 overflow-x-auto pb-3">
          {groups.map((g) => (
            <div key={g.key} className={`${CARD_W} shrink-0`}>
              <p className="mb-1.5 flex items-baseline justify-between border-b ink-rule px-0.5 font-serif text-sm">
                <span className="font-medium">{g.label}</span>
                <span className="tabular-nums text-ink-faded">{count(g.cards)}</span>
              </p>
              <div className="flex flex-col">
                {g.cards.map((card, i) => (
                  <CardTile key={card.scryfallId} card={card} first={i === 0} format={format} {...actions} />
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="columns-1 gap-8 md:columns-2 min-[1800px]:columns-3">
          {groups.map((g) => (
            <div key={g.key} className="mb-4 break-inside-avoid">
              <p className="mb-1 flex items-baseline justify-between border-b ink-rule font-serif text-sm">
                <span className="font-medium">{g.label}</span>
                <span className="tabular-nums text-ink-faded">{count(g.cards)}</span>
              </p>
              <ul>
                {g.cards.map((card) => (
                  <CardRow key={card.scryfallId} card={card} format={format} {...actions} />
                ))}
              </ul>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

interface SideProps extends CardActions {
  board: Board
  format: DeckFormat
  cards: DeckCard[]
  sort: SortBy
  /** shown as "12 / 15" when the zone has a size limit */
  limit?: number
  hint: string
}

/** A narrow vertical column on the right (sideboard, commander), cards stacked like in MTGO. */
export function SideColumn({ board, format, cards, sort, limit, hint, ...actions }: SideProps) {
  const { setNodeRef, isOver } = useDroppable({ id: `zone-${board}`, data: { board } })
  const n = count(cards)
  return (
    <section
      ref={setNodeRef}
      aria-label={ZONE_LABEL[board]}
      className={`tome flex min-h-[140px] flex-col p-2.5 transition ${isOver ? 'border-accent ring-2 ring-accent/50' : ''}`}
    >
      <h3 className="mb-2 flex items-baseline justify-between font-serif text-base text-parchment">
        {ZONE_LABEL[board]}
        <span className={`text-sm tabular-nums ${limit && n > limit ? 'text-red-300' : 'text-stone-400'}`}>
          {limit ? `${n} / ${limit}` : n}
        </span>
      </h3>
      {cards.length === 0 ? (
        <p className="flex flex-1 items-center justify-center px-2 text-center text-sm italic text-stone-500">{hint}</p>
      ) : (
        <div className="flex flex-col items-center">
          {sortCards(cards, sort).map((card, i) => (
            <CardTile key={card.scryfallId} card={card} first={i === 0} format={format} dark {...actions} />
          ))}
        </div>
      )}
    </section>
  )
}

function ZoneTitle({ label, total }: { label: string; total: number }) {
  return (
    <h3 className="mb-3 flex items-baseline gap-3 font-serif">
      <span className="text-xl font-medium">{label}</span>
      <span className="text-sm tabular-nums text-ink-faded">{total} {total === 1 ? 'carte' : 'cărți'}</span>
    </h3>
  )
}

function useDeckCardDrag(card: DeckCard) {
  return useDraggable({ id: `deck-${card.scryfallId}-${card.board}`, data: { type: 'deck', card } })
}

/** Stacked card image: only the title strip shows, except for the last card of the column. */
function CardTile({ card, first, format, dark = false, add, move, onHover, onOpen }:
  { card: DeckCard; first: boolean; format: DeckFormat; dark?: boolean } & CardActions) {
  const { attributes, listeners, setNodeRef, isDragging } = useDeckCardDrag(card)
  const target = otherZone(format, card.board)
  return (
    <div
      ref={setNodeRef}
      {...attributes}
      {...listeners}
      onMouseEnter={() => onHover(card)}
      onMouseLeave={() => onHover(null)}
      onClick={() => onOpen(card)}
      title={`${card.name}: click pentru altă printare, trage pentru a muta`}
      className={`group relative ${CARD_W} cursor-pointer touch-none ${first ? '' : STACK_OVERLAP} hover:z-10 ${isDragging ? 'opacity-30' : ''}`}
    >
      <img
        src={card.images.normal ?? card.images.small ?? ''}
        alt={card.name}
        draggable={false}
        width={124}
        height={173}
        className={`aspect-[488/680] w-full rounded-[4.75%/3.5%] bg-surface-3 ${
          dark ? 'shadow-md shadow-black/70' : 'shadow-[0_3px_8px_rgb(60_38_12/0.45)]'
        }`}
      />
      {card.quantity > 1 && (
        <span className="absolute right-1 top-1 rounded-sm bg-ink/85 px-1 font-serif text-xs font-bold text-parchment group-hover:hidden group-focus-within:hidden">
          {card.quantity}
        </span>
      )}
      <div className="absolute right-1 top-1 hidden gap-0.5 group-hover:flex group-focus-within:flex">
        <TinyButton label={`Scoate un ${card.name}`} onClick={() => add(card, card.board, -1)}>−</TinyButton>
        <TinyButton label={`Adaugă un ${card.name}`} onClick={() => add(card, card.board, 1)}>+</TinyButton>
        <TinyButton label={`Mută în ${ZONE_LABEL[target]}`} onClick={() => move(card, card.board, target)}>⇄</TinyButton>
      </div>
    </div>
  )
}

/** One line of the list view, written in ink. */
function CardRow({ card, format, add, move, onHover, onOpen }: { card: DeckCard; format: DeckFormat } & CardActions) {
  const { attributes, listeners, setNodeRef, isDragging } = useDeckCardDrag(card)
  const target = otherZone(format, card.board)
  return (
    <li
      ref={setNodeRef}
      {...attributes}
      {...listeners}
      onMouseEnter={() => onHover(card)}
      onMouseLeave={() => onHover(null)}
      onClick={() => onOpen(card)}
      className={`group flex cursor-pointer touch-none items-center gap-2 rounded-sm px-1 py-0.5 font-serif hover:bg-ink/5 ${
        isDragging ? 'opacity-30' : ''
      }`}
    >
      <span className="w-5 text-right tabular-nums text-ink-faded">{card.quantity}</span>
      <span className="flex-1 truncate">{card.name}</span>
      <ManaCost cost={card.manaCost} size={14} />
      <span className="w-16 text-right font-sans text-xs tabular-nums text-ink-faded group-hover:hidden group-focus-within:hidden">
        {formatEur(card.prices.eur)}
      </span>
      <span className="hidden w-16 justify-end gap-0.5 group-hover:flex group-focus-within:flex">
        <TinyButton label={`Scoate un ${card.name}`} onClick={() => add(card, card.board, -1)}>−</TinyButton>
        <TinyButton label={`Adaugă un ${card.name}`} onClick={() => add(card, card.board, 1)}>+</TinyButton>
        <TinyButton label={`Mută în ${ZONE_LABEL[target]}`} onClick={() => move(card, card.board, target)}>⇄</TinyButton>
      </span>
    </li>
  )
}

function TinyButton({ label, onClick, children }: { label: string; onClick: () => void; children: string }) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      // keep the click from starting a drag
      onPointerDown={(e) => e.stopPropagation()}
      onClick={(e) => {
        e.stopPropagation()
        onClick()
      }}
      className="h-5 w-5 rounded-sm bg-ink/85 font-sans text-xs font-bold leading-none text-parchment hover:bg-accent hover:text-ink"
    >
      {children}
    </button>
  )
}
