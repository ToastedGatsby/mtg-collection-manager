import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import { createDeck, importDeck, listDecks, type DeckFormat } from '../api/decks'
import { formatEur } from '../components/cards/format'
import { FORMAT_LABEL, FORMATS } from '../components/decks/deckUtils'
import Spinner from '../components/Spinner'

/** "My decks": cards with cover art, plus creating an empty deck or importing a text list. */
export default function DecksPage() {
  const navigate = useNavigate()
  const decks = useQuery({ queryKey: ['decks'], queryFn: listDecks, staleTime: 0 })
  const [dialog, setDialog] = useState<'new' | 'import' | null>(null)

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="font-serif text-3xl font-medium text-parchment">Deckurile mele</h1>
        <div className="ml-auto flex gap-2">
          <button type="button" onClick={() => setDialog('import')} className="rounded-sm border border-accent/70 px-3 py-2 font-serif text-accent hover:bg-accent/10">
            Importă listă
          </button>
          <button type="button" onClick={() => setDialog('new')} className="rounded-sm bg-accent px-3 py-2 font-serif font-medium text-ink hover:brightness-110">
            Deck nou
          </button>
        </div>
      </div>

      {decks.isPending && <Spinner label="Se încarcă deckurile…" />}
      {decks.isError && <p className="text-sm text-red-400">Deckurile nu s-au putut încărca.</p>}
      {decks.data?.length === 0 && (
        <div className="parchment py-16 text-center font-serif text-lg italic text-ink-faded">
          Nu ai niciun deck încă. Creează unul sau importă o listă din Arena / Moxfield.
        </div>
      )}

      {decks.data && decks.data.length > 0 && (
        <ul className="grid grid-cols-[repeat(auto-fill,minmax(220px,1fr))] gap-4">
          {decks.data.map((deck) => (
            <li key={deck.id}>
              <Link
                to={`/decks/${deck.id}`}
                className="tome group block overflow-hidden transition hover:border-accent/70"
              >
                <div
                  className="h-28 bg-surface-3 bg-cover bg-center transition group-hover:scale-[1.02]"
                  style={deck.coverImage ? { backgroundImage: `url(${deck.coverImage})` } : undefined}
                  role="img"
                  aria-label={`Imagine pentru ${deck.name}`}
                />
                <div className="space-y-1 p-3">
                  <p className="truncate font-serif text-lg text-parchment">{deck.name}</p>
                  <p className="flex justify-between gap-2 text-sm text-stone-400">
                    <span>{FORMAT_LABEL[deck.format]}, {deck.cardCount} {deck.cardCount === 1 ? 'carte' : 'cărți'}</span>
                    <span>{formatEur(deck.totalEur)}</span>
                  </p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {dialog === 'new' && (
        <NewDeckDialog onClose={() => setDialog(null)} onCreated={(id) => navigate(`/decks/${id}`)} />
      )}
      {dialog === 'import' && (
        <ImportDialog
          onClose={() => setDialog(null)}
          onImported={(id, report) => navigate(`/decks/${id}`, { state: report })}
        />
      )}
    </div>
  )
}

function Dialog({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
        className="parchment w-full max-w-lg space-y-4 p-6"
      >
        <h2 className="font-serif text-2xl">{title}</h2>
        {children}
      </div>
    </div>
  )
}

function NameAndFormat({ name, setName, format, setFormat }: {
  name: string; setName: (v: string) => void; format: DeckFormat; setFormat: (f: DeckFormat) => void
}) {
  return (
    <div className="grid grid-cols-[1fr_auto] gap-2">
      <input
        autoFocus
        value={name}
        onChange={(e) => setName(e.target.value)}
        maxLength={100}
        placeholder="Numele deckului"
        aria-label="Numele deckului"
        className="rounded-sm border ink-rule bg-parchment/60 px-3 py-2 font-serif text-ink outline-none focus:ring-1 focus:ring-accent"
      />
      <select value={format} onChange={(e) => setFormat(e.target.value as DeckFormat)} aria-label="Format" className="rounded-sm border ink-rule bg-transparent px-2 font-serif text-ink">
        {FORMATS.map((f) => <option key={f} value={f}>{FORMAT_LABEL[f]}</option>)}
      </select>
    </div>
  )
}

const errorText = (err: unknown) => (err instanceof ApiError ? err.message : 'Serverul nu răspunde.')

function NewDeckDialog({ onClose, onCreated }: { onClose: () => void; onCreated: (id: number) => void }) {
  const [name, setName] = useState('')
  const [format, setFormat] = useState<DeckFormat>('COMMANDER')
  const create = useMutation({ mutationFn: () => createDeck(name.trim(), format), onSuccess: (d) => onCreated(d.id) })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (name.trim()) create.mutate()
  }
  return (
    <Dialog title="Deck nou" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <NameAndFormat name={name} setName={setName} format={format} setFormat={setFormat} />
        {create.isError && <p className="font-serif text-sm text-wax">{errorText(create.error)}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" onClick={onClose} className="rounded-sm px-3 py-1.5 font-serif text-ink-faded hover:bg-ink/10">Anulează</button>
          <button type="submit" disabled={!name.trim() || create.isPending} className="rounded-sm bg-ink px-3 py-1.5 font-serif text-parchment hover:bg-ink/85 disabled:opacity-50">
            {create.isPending ? 'Se creează…' : 'Creează'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}

function ImportDialog({ onClose, onImported }: {
  onClose: () => void
  onImported: (id: number, report: { unresolvedCards: string[]; parseErrors: string[] }) => void
}) {
  const [name, setName] = useState('')
  const [format, setFormat] = useState<DeckFormat>('COMMANDER')
  const [list, setList] = useState('')
  const run = useMutation({
    mutationFn: () => importDeck(name.trim(), format, list),
    onSuccess: (r) => onImported(r.deck.id, { unresolvedCards: r.unresolvedCards, parseErrors: r.parseErrors }),
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (name.trim() && list.trim()) run.mutate()
  }
  return (
    <Dialog title="Importă o listă" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <NameAndFormat name={name} setName={setName} format={format} setFormat={setFormat} />
        <textarea
          value={list}
          onChange={(e) => setList(e.target.value)}
          aria-label="Lista de cărți"
          placeholder={'Commander\n1 Krenko, Mob Boss\n\nDeck\n1 Lightning Bolt (M10) 146\n30 Mountain'}
          className="h-56 w-full resize-none rounded-sm border ink-rule bg-parchment/60 p-3 font-serif text-sm text-ink outline-none focus:ring-1 focus:ring-accent"
        />
        <p className="font-serif text-sm italic text-ink-faded">Merge cu listele exportate din MTG Arena, MTGO, Moxfield sau Archidekt.</p>
        {run.isError && <p className="font-serif text-sm text-wax">{errorText(run.error)}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" onClick={onClose} className="rounded-sm px-3 py-1.5 font-serif text-ink-faded hover:bg-ink/10">Anulează</button>
          <button type="submit" disabled={!name.trim() || !list.trim() || run.isPending} className="rounded-sm bg-ink px-3 py-1.5 font-serif text-parchment hover:bg-ink/85 disabled:opacity-50">
            {run.isPending ? 'Se importă…' : 'Importă'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}
