import { api, apiText } from './client'
import type { Images, Prices } from './cards'

export type DeckFormat = 'STANDARD' | 'PIONEER' | 'MODERN' | 'LEGACY' | 'VINTAGE' | 'PAUPER' | 'COMMANDER'
export type Board = 'MAIN' | 'SIDEBOARD' | 'COMMANDER' | 'MAYBEBOARD'

export interface DeckSummary {
  id: number
  name: string
  format: DeckFormat
  cardCount: number
  coverImage: string | null
  totalEur: number | null
  updatedAt: string
}

export interface DeckCard {
  scryfallId: string
  oracleId: string
  board: Board
  quantity: number
  name: string
  manaCost: string | null
  cmc: number
  typeLine: string | null
  colors: string[]
  colorIdentity: string[]
  setCode: string | null
  collectorNumber: string | null
  rarity: string | null
  images: Images
  prices: Prices
}

export interface ValidationIssue {
  code: string
  message: string
  cardName: string | null
  actual: number | null
  limit: number | null
}

export interface ValidationResult {
  format: DeckFormat
  legal: boolean
  mainCount: number
  sideboardCount: number
  commanderCount: number
  issues: ValidationIssue[]
}

export interface DeckDetail {
  id: number
  name: string
  format: DeckFormat
  description: string | null
  createdAt: string
  updatedAt: string
  cards: DeckCard[]
  validation: ValidationResult
  totals: { eur: number; usd: number; cardsWithoutEurPrice: number }
}

export interface ImportDeckResponse {
  deck: DeckDetail
  unresolvedCards: string[]
  parseErrors: string[]
}

const json = (body: unknown) => ({ body: JSON.stringify(body) })

export const listDecks = () => api<DeckSummary[]>('/api/decks')
export const getDeck = (id: number) => api<DeckDetail>(`/api/decks/${id}`)

export const createDeck = (name: string, format: DeckFormat) =>
  api<DeckDetail>('/api/decks', { method: 'POST', ...json({ name, format }) })

export const updateDeck = (id: number, changes: { name?: string; format?: DeckFormat; description?: string }) =>
  api<DeckDetail>(`/api/decks/${id}`, { method: 'PATCH', ...json(changes) })

export const deleteDeck = (id: number) => api<void>(`/api/decks/${id}`, { method: 'DELETE' })

export const setDeckCard = (id: number, change: { scryfallId: string; board: Board; quantity: number }) =>
  api<DeckDetail>(`/api/decks/${id}/cards`, { method: 'PUT', ...json(change) })

export const importDeck = (name: string, format: DeckFormat, list: string) =>
  api<ImportDeckResponse>('/api/decks/import', { method: 'POST', ...json({ name, format, list }) })

export const exportDeck = (id: number) => apiText(`/api/decks/${id}/export`)
