import type { ColorMode } from '../../api/cards'

const SYMBOL_URL = 'https://svgs.scryfall.io/card-symbols/'
const COLOR_KEYS = ['W', 'U', 'B', 'R', 'G', 'C'] as const
const COLOR_NAMES: Record<string, string> = {
  W: 'Alb', U: 'Albastru', B: 'Negru', R: 'Roșu', G: 'Verde', C: 'Incolor',
}

interface Props {
  selected: Set<string>
  onToggle: (color: string) => void
  mode: ColorMode
  onModeChange: (mode: ColorMode) => void
}

/** WUBRG toggles like MTGO, plus how to combine them. */
export default function ColorFilter({ selected, onToggle, mode, onModeChange }: Props) {
  return (
    <div className="flex flex-wrap items-center gap-1.5">
      {COLOR_KEYS.map((c) => {
        const active = selected.has(c)
        return (
          <button
            key={c}
            type="button"
            onClick={() => onToggle(c)}
            aria-pressed={active}
            title={COLOR_NAMES[c]}
            className={`rounded-full p-0.5 transition ${
              active ? 'ring-2 ring-accent' : 'opacity-40 grayscale hover:opacity-80 hover:grayscale-0'
            }`}
          >
            <img src={`${SYMBOL_URL}${c}.svg`} alt={COLOR_NAMES[c]} width={22} height={22} />
          </button>
        )
      })}
      <select
        value={mode}
        onChange={(e) => onModeChange(e.target.value as ColorMode)}
        className="ml-1 rounded-sm border border-surface-3 bg-surface px-2 py-1 text-sm"
        aria-label="Mod de filtrare a culorilor"
      >
        <option value="including">Include culorile</option>
        <option value="exact">Exact culorile</option>
        <option value="identity">Color identity (Commander)</option>
      </select>
    </div>
  )
}
