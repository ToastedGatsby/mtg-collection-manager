// Renders "{2}{W/U}{R}" with the official mana symbol SVGs hosted by Scryfall.
const SYMBOL_URL = 'https://svgs.scryfall.io/card-symbols/'

export default function ManaCost({ cost, size = 16 }: { cost: string | null; size?: number }) {
  if (!cost) return null
  // split cards: "{1}{R} // {1}{U}"
  const halves = cost.split(' // ')
  return (
    <span className="inline-flex flex-wrap items-center gap-0.5" aria-label={cost}>
      {halves.map((half, h) => (
        <span key={h} className="inline-flex items-center gap-0.5">
          {h > 0 && <span className="px-0.5 text-stone-500">//</span>}
          {Array.from(half.matchAll(/\{([^}]+)\}/g)).map((m, i) => (
            <img
              key={i}
              src={`${SYMBOL_URL}${m[1].replace('/', '')}.svg`}
              alt={`{${m[1]}}`}
              width={size}
              height={size}
              loading="lazy"
            />
          ))}
        </span>
      ))}
    </span>
  )
}
