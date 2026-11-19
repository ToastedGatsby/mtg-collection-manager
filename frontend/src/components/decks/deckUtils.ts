import type { CardSummary, Printing } from '../../api/cards'
import type { Board, DeckCard, DeckDetail, DeckFormat, ValidationIssue } from '../../api/decks'

export const FORMATS: DeckFormat[] = ['COMMANDER', 'STANDARD', 'PIONEER', 'MODERN', 'LEGACY', 'VINTAGE', 'PAUPER']

export const FORMAT_LABEL: Record<DeckFormat, string> = {
  STANDARD: 'Standard',
  PIONEER: 'Pioneer',
  MODERN: 'Modern',
  LEGACY: 'Legacy',
  VINTAGE: 'Vintage',
  PAUPER: 'Pauper',
  COMMANDER: 'Commander',
}

export const ZONE_LABEL: Record<Board, string> = {
  COMMANDER: 'Commander',
  MAIN: 'Main deck',
  SIDEBOARD: 'Sideboard',
  MAYBEBOARD: 'Maybeboard',
}

/**
 * Commander decks have a command zone and no sideboard; 60-card formats the opposite.
 * A zone that still holds cards (e.g. after switching a Commander deck to Modern) stays visible,
 * so those cards can be moved instead of silently counting against validation.
 */
export function zonesFor(format: DeckFormat, cards: DeckCard[] = []): Board[] {
  const base: Board[] = format === 'COMMANDER' ? ['COMMANDER', 'MAIN'] : ['MAIN', 'SIDEBOARD']
  const used = new Set(cards.map((c) => c.board))
  return (['COMMANDER', 'MAIN', 'SIDEBOARD', 'MAYBEBOARD'] as Board[]).filter((b) => base.includes(b) || used.has(b))
}

/** Where the "⇄" button sends a card. */
export const otherZone = (format: DeckFormat, board: Board): Board =>
  format === 'COMMANDER' ? (board === 'COMMANDER' ? 'MAIN' : 'COMMANDER') : board === 'MAIN' ? 'SIDEBOARD' : 'MAIN'

export const quantityOf = (cards: DeckCard[], scryfallId: string, board: Board) =>
  cards.find((c) => c.scryfallId === scryfallId && c.board === board)?.quantity ?? 0

/** Optimistic update: the UI changes instantly; the server response (with validation) replaces it later. */
export function applyQuantity(deck: DeckDetail, template: DeckCard, board: Board, quantity: number): DeckDetail {
  const others = deck.cards.filter((c) => !(c.scryfallId === template.scryfallId && c.board === board))
  const existing = deck.cards.find((c) => c.scryfallId === template.scryfallId && c.board === board)
  if (quantity === 0) return { ...deck, cards: others }
  const updated: DeckCard = { ...(existing ?? template), board, quantity }
  return { ...deck, cards: existing ? deck.cards.map((c) => (c === existing ? updated : c)) : [...deck.cards, updated] }
}

/** A search result, as a deck entry using its default printing. */
export function fromSummary(card: CardSummary, board: Board): DeckCard | null {
  if (!card.defaultPrintingId) return null
  return {
    scryfallId: card.defaultPrintingId,
    oracleId: card.oracleId,
    board,
    quantity: 0,
    name: card.name,
    manaCost: card.manaCost,
    cmc: card.cmc,
    typeLine: card.typeLine,
    colors: card.colors,
    colorIdentity: card.colorIdentity,
    setCode: card.setCode,
    collectorNumber: null,
    rarity: card.rarity,
    images: card.images,
    prices: card.prices,
  }
}

/** A card already in the deck, shaped like a search result so the printings panel can open it. */
export function toSummary(card: DeckCard): CardSummary {
  return {
    oracleId: card.oracleId,
    name: card.name,
    manaCost: card.manaCost,
    cmc: card.cmc,
    typeLine: card.typeLine,
    colors: card.colors,
    colorIdentity: card.colorIdentity,
    defaultPrintingId: card.scryfallId, // the printing used now is pre-selected
    setCode: card.setCode,
    rarity: card.rarity,
    images: card.images,
    prices: card.prices,
  }
}

/** A specific printing chosen in the printings panel. */
export function fromPrinting(card: CardSummary, printing: Printing, board: Board): DeckCard {
  return {
    ...(fromSummary(card, board) as DeckCard),
    scryfallId: printing.scryfallId,
    setCode: printing.setCode,
    collectorNumber: printing.collectorNumber,
    rarity: printing.rarity,
    images: printing.images,
    prices: printing.prices,
  }
}

// ---------- grouping ----------

const frontType = (typeLine: string | null) => (typeLine ?? '').split(' // ')[0]
export const isLand = (card: DeckCard) => /\bLand\b/.test(frontType(card.typeLine))

const TYPE_ORDER = ['Creature', 'Planeswalker', 'Battle', 'Instant', 'Sorcery', 'Artifact', 'Enchantment', 'Land'] as const
export const TYPE_LABEL: Record<string, string> = {
  Creature: 'Creaturi',
  Planeswalker: 'Planeswalkeri',
  Battle: 'Battles',
  Instant: 'Instants',
  Sorcery: 'Sorceries',
  Artifact: 'Artefacte',
  Enchantment: 'Enchantments',
  Land: 'Terenuri',
  Other: 'Altele',
}

/** First matching type in MTGO order: an "Artifact Creature" counts as a creature. */
export function typeGroup(card: DeckCard): string {
  const front = frontType(card.typeLine)
  if (/\bLand\b/.test(front)) return 'Land'
  return TYPE_ORDER.find((t) => new RegExp(`\\b${t}\\b`).test(front)) ?? 'Other'
}

export type GroupBy = 'cmc' | 'type' | 'color' | 'rarity'
export type SortBy = 'name' | 'cmc' | 'price' | 'rarity'

export const GROUP_LABEL: Record<GroupBy, string> = { cmc: 'Cost de mana', type: 'Tip', color: 'Culoare', rarity: 'Raritate' }
export const SORT_LABEL: Record<SortBy, string> = { name: 'Nume', cmc: 'Cost', price: 'Preț', rarity: 'Raritate' }

export interface CardGroup {
  key: string
  label: string
  cards: DeckCard[]
}

const RARITY_ORDER = ['mythic', 'rare', 'uncommon', 'common', 'special', 'bonus']
const RARITY_NAME: Record<string, string> = {
  mythic: 'Mythic', rare: 'Rare', uncommon: 'Uncommon', common: 'Common', special: 'Special', bonus: 'Bonus',
}
const COLOR_ORDER = ['W', 'U', 'B', 'R', 'G', 'M', 'C', 'Land']
const COLOR_NAME: Record<string, string> = {
  W: 'Alb', U: 'Albastru', B: 'Negru', R: 'Roșu', G: 'Verde', M: 'Multicolor', C: 'Incolor', Land: 'Terenuri',
}

function groupKey(card: DeckCard, group: GroupBy): string {
  switch (group) {
    case 'cmc':
      return isLand(card) ? 'Land' : String(Math.min(Math.floor(card.cmc), 7))
    case 'type':
      return typeGroup(card)
    case 'color':
      if (isLand(card)) return 'Land'
      return card.colors.length === 0 ? 'C' : card.colors.length > 1 ? 'M' : card.colors[0]
    case 'rarity':
      return card.rarity ?? 'common'
  }
}

function groupLabel(key: string, group: GroupBy): string {
  switch (group) {
    case 'cmc':
      return key === 'Land' ? 'Terenuri' : key === '7' ? '7+' : key
    case 'type':
      return TYPE_LABEL[key] ?? key
    case 'color':
      return COLOR_NAME[key] ?? key
    case 'rarity':
      return RARITY_NAME[key] ?? key
  }
}

const GROUP_ORDER: Record<GroupBy, string[]> = {
  cmc: ['0', '1', '2', '3', '4', '5', '6', '7', 'Land'],
  type: [...TYPE_ORDER, 'Other'],
  color: COLOR_ORDER,
  rarity: RARITY_ORDER,
}

/** Splits cards into ordered groups (MTGO-style columns or list sections), each sorted. */
export function groupCards(cards: DeckCard[], group: GroupBy, sort: SortBy): CardGroup[] {
  const map = new Map<string, DeckCard[]>()
  for (const card of cards) {
    const key = groupKey(card, group)
    map.set(key, [...(map.get(key) ?? []), card])
  }
  const order = GROUP_ORDER[group]
  const keys = [...map.keys()].sort((a, b) => (order.indexOf(a) + 1 || 99) - (order.indexOf(b) + 1 || 99))
  return keys.map((key) => ({ key, label: groupLabel(key, group), cards: sortCards(map.get(key)!, sort) }))
}

const priceOf = (c: DeckCard) => c.prices.eur ?? c.prices.usd ?? -1

export function sortCards(cards: DeckCard[], sort: SortBy): DeckCard[] {
  const byName = (a: DeckCard, b: DeckCard) => a.name.localeCompare(b.name)
  const compare: Record<SortBy, (a: DeckCard, b: DeckCard) => number> = {
    name: byName,
    cmc: (a, b) => a.cmc - b.cmc || byName(a, b),
    price: (a, b) => priceOf(b) - priceOf(a) || byName(a, b),
    rarity: (a, b) => RARITY_ORDER.indexOf(a.rarity ?? '') - RARITY_ORDER.indexOf(b.rarity ?? '') || byName(a, b),
  }
  return [...cards].sort(compare[sort])
}

export const count = (cards: DeckCard[]) => cards.reduce((n, c) => n + c.quantity, 0)

// ---------- stats ----------

/** Non-land cards per mana value 0..7+ (main deck + commander). */
export function manaCurve(cards: DeckCard[]): number[] {
  const curve = new Array(8).fill(0)
  for (const c of cards) {
    if ((c.board === 'MAIN' || c.board === 'COMMANDER') && !isLand(c)) {
      curve[Math.min(Math.floor(c.cmc), 7)] += c.quantity
    }
  }
  return curve
}

/** Coloured mana symbols in the mana costs ({W/U} hybrid counts for both). */
export function colorPips(cards: DeckCard[]): Record<string, number> {
  const pips: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0, C: 0 }
  for (const c of cards) {
    if (c.board !== 'MAIN' && c.board !== 'COMMANDER') continue
    for (const m of (c.manaCost ?? '').matchAll(/\{([^}]+)\}/g)) {
      for (const color of ['W', 'U', 'B', 'R', 'G', 'C']) {
        if (m[1].split('/').includes(color)) pips[color] += c.quantity
      }
    }
  }
  return pips
}

export function value(cards: DeckCard[]) {
  let eur = 0
  let usd = 0
  for (const c of cards) {
    if (c.board === 'MAYBEBOARD') continue
    eur += (c.prices.eur ?? 0) * c.quantity
    usd += (c.prices.usd ?? 0) * c.quantity
  }
  return { eur, usd }
}

// ---------- validation messages (the backend sends codes, the UI speaks Romanian) ----------

export function issueText(issue: ValidationIssue): string {
  const card = issue.cardName ?? ''
  switch (issue.code) {
    case 'DECK_TOO_SMALL':
      return `Main deck-ul are ${issue.actual} cărți; minimum ${issue.limit}.`
    case 'SIDEBOARD_TOO_LARGE':
      return `Sideboard-ul are ${issue.actual} cărți; maximum ${issue.limit}.`
    case 'TOO_MANY_COPIES':
      return `${card}: ${issue.actual} copii, maximum ${issue.limit}.`
    case 'RESTRICTED_TOO_MANY':
      return `${card} este restricted: maximum o copie.`
    case 'BANNED':
      return `${card} este banned în acest format.`
    case 'NOT_LEGAL':
      return `${card} nu este legală în acest format.`
    case 'COMMANDER_ZONE_NOT_ALLOWED':
      return 'Acest format nu folosește comandant: mută cartea în main deck.'
    case 'NO_COMMANDER':
      return 'Alege un comandant: trage o creatură legendară în zona Commander.'
    case 'TOO_MANY_COMMANDERS':
      return `Ai ${issue.actual} comandanți; maximum 2.`
    case 'INVALID_COMMANDER':
      return `${card} nu poate fi comandant (nu este creatură legendară).`
    case 'INVALID_PARTNERS':
      return `${card} și celălalt comandant nu pot fi folosiți împreună (fără Partner/Background).`
    case 'WRONG_DECK_SIZE':
      return `Deckul are ${issue.actual} cărți; Commander cere exact ${issue.limit}, cu tot cu comandantul.`
    case 'COLOR_IDENTITY':
      return `${card} iese din color identity-ul comandantului.`
    default:
      return issue.message
  }
}
