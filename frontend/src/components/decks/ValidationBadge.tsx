import { useState, type ReactNode } from 'react'
import type { ValidationResult } from '../../api/decks'
import { FORMAT_LABEL, issueText } from './deckUtils'

/**
 * A wax seal pressed on the parchment: green-bronze when the deck is legal in its format,
 * red wax with the number of problems otherwise (click to read them).
 */
export default function ValidationBadge({ validation, pending }: { validation: ValidationResult; pending: boolean }) {
  const [open, setOpen] = useState(false)
  const n = validation.issues.length
  const format = FORMAT_LABEL[validation.format]
  const sealBase =
    'flex items-center gap-2 rounded-full py-1 pl-1 pr-3 font-serif text-sm text-parchment shadow-[inset_0_-2px_4px_rgb(0_0_0/0.35),0_2px_4px_rgb(60_30_10/0.4)]'

  if (validation.legal) {
    return (
      <span className={`${sealBase} bg-verdigris ${pending ? 'opacity-60' : ''}`}>
        <Seal>✓</Seal>
        Legal în {format}
      </span>
    )
  }
  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        className={`${sealBase} bg-wax hover:brightness-110 ${pending ? 'opacity-60' : ''}`}
      >
        <Seal>{n}</Seal>
        {n === 1 ? 'problemă' : 'probleme'} în {format}
      </button>
      {open && (
        <ul className="parchment absolute right-0 z-30 mt-2 w-80 space-y-2 p-4 font-serif text-sm">
          {validation.issues.map((issue, i) => (
            <li key={i} className="flex gap-2">
              <span className="text-wax">✦</span>
              {issueText(issue)}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function Seal({ children }: { children: ReactNode }) {
  return (
    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-black/20 text-xs font-bold ring-1 ring-black/25">
      {children}
    </span>
  )
}
