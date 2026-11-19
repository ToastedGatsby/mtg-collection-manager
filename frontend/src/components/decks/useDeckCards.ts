import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query'
import { setDeckCard, type Board, type DeckCard, type DeckDetail } from '../../api/decks'
import { applyQuantity, quantityOf } from './deckUtils'

const MAX_QUANTITY = 250

/**
 * Changing card quantities with optimistic updates:
 *  1. the cached deck is changed immediately, so +/- and drag & drop feel instant;
 *  2. requests are sent one after another (mutation scope), never in parallel, so they reach the
 *     server in the order the user clicked;
 *  3. only the response of the LAST pending request replaces the cache (it contains everything, plus
 *     the server's validation) - earlier responses would briefly undo newer clicks;
 *  4. on error, the deck is reloaded from the server, which undoes the optimistic change.
 */
export function useDeckCards(deckId: number) {
  const queryClient = useQueryClient()
  const queryKey = ['deck', deckId]
  const mutationKey = ['deck-card', deckId]

  const mutation = useMutation({
    mutationKey,
    scope: { id: `deck-${deckId}` },
    mutationFn: (change: { scryfallId: string; board: Board; quantity: number }) => setDeckCard(deckId, change),
    onSuccess: (deck) => {
      if (queryClient.isMutating({ mutationKey }) <= 1) {
        queryClient.setQueryData(queryKey, deck)
      }
    },
    onError: () => queryClient.invalidateQueries({ queryKey }),
  })
  const saving = useIsMutating({ mutationKey }) > 0

  const current = (scryfallId: string, board: Board) =>
    quantityOf(queryClient.getQueryData<DeckDetail>(queryKey)?.cards ?? [], scryfallId, board)

  const set = (card: DeckCard, board: Board, quantity: number) => {
    const q = Math.max(0, Math.min(MAX_QUANTITY, quantity))
    queryClient.setQueryData<DeckDetail>(queryKey, (deck) => deck && applyQuantity(deck, card, board, q))
    mutation.mutate({ scryfallId: card.scryfallId, board, quantity: q })
  }

  return {
    saving,
    error: mutation.error,
    set,
    add: (card: DeckCard, board: Board, delta = 1) => set(card, board, current(card.scryfallId, board) + delta),
    /** Swaps every copy of a printing for another printing of the same card, in the same zone. */
    replace: (card: DeckCard, replacement: DeckCard) => {
      if (card.scryfallId === replacement.scryfallId) return
      const copies = current(card.scryfallId, card.board)
      set(card, card.board, 0)
      set(replacement, card.board, current(replacement.scryfallId, card.board) + copies)
    },
    /** Moves one copy, like dragging a card between zones in MTGO. */
    move: (card: DeckCard, from: Board, to: Board) => {
      if (from === to || current(card.scryfallId, from) < 1) return
      set(card, from, current(card.scryfallId, from) - 1)
      set(card, to, current(card.scryfallId, to) + 1)
    },
  }
}
