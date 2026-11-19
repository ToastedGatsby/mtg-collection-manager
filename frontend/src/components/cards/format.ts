import type { Prices } from '../../api/cards'

const eur = new Intl.NumberFormat('ro-RO', { style: 'currency', currency: 'EUR' })
const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

export const formatEur = (value: number | null) => (value == null ? '—' : eur.format(value))
export const formatUsd = (value: number | null) => (value == null ? '—' : usd.format(value))

/** Short label for the grid: Cardmarket EUR first (EU users), TCGplayer USD as fallback. */
export function shortPrice(prices: Prices): string | null {
  if (prices.eur != null) return eur.format(prices.eur)
  if (prices.usd != null) return usd.format(prices.usd)
  if (prices.eurFoil != null) return `${eur.format(prices.eurFoil)} ✦`
  if (prices.usdFoil != null) return `${usd.format(prices.usdFoil)} ✦`
  return null
}

export const RARITY_COLOR: Record<string, string> = {
  common: 'text-zinc-300',
  uncommon: 'text-slate-300',
  rare: 'text-amber-300',
  mythic: 'text-orange-400',
  special: 'text-fuchsia-300',
  bonus: 'text-fuchsia-300',
}
