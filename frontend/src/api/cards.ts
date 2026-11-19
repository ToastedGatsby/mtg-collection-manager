import { api } from './client'

export interface Prices {
  usd: number | null
  usdFoil: number | null
  usdEtched: number | null
  eur: number | null
  eurFoil: number | null
  tix: number | null
}

export interface Images {
  small: string | null
  normal: string | null
  large: string | null
  artCrop: string | null
}

export interface CardSummary {
  oracleId: string
  name: string
  manaCost: string | null
  cmc: number
  typeLine: string | null
  colors: string[]
  colorIdentity: string[]
  defaultPrintingId: string | null
  setCode: string | null
  rarity: string | null
  images: Images
  prices: Prices
}

export interface Printing {
  scryfallId: string
  setCode: string
  setName: string
  collectorNumber: string
  rarity: string
  lang: string
  releasedAt: string | null
  finishes: string[]
  promo: boolean
  images: Images
  prices: Prices
  tcgplayerUrl: string | null
  cardmarketUrl: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ColorMode = 'including' | 'exact' | 'identity'

export interface CardSearchParams {
  q?: string
  text?: string
  colors?: string
  colorMode?: ColorMode
  type?: string
  rarity?: string
  format?: string
  sort?: 'name' | 'cmc' | 'price'
  page?: number
  size?: number
}

export function searchCards(params: CardSearchParams): Promise<PageResponse<CardSummary>> {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value))
    }
  }
  return api<PageResponse<CardSummary>>(`/api/cards?${query}`)
}

export const getPrintings = (oracleId: string) => api<Printing[]>(`/api/cards/${oracleId}/printings`)
