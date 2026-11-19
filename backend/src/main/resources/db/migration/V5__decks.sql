-- Decks and their cards. A deck entry points at a specific printing (art, set, price);
-- rules are checked per card name, so 2 Lightning Bolt from M10 + 2 from 2XM count as 4.

CREATE TABLE decks (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE, -- GDPR: account deletion removes decks
    name        VARCHAR(100)  NOT NULL,
    format      VARCHAR(20)   NOT NULL
        CHECK (format IN ('STANDARD', 'PIONEER', 'MODERN', 'LEGACY', 'VINTAGE', 'PAUPER', 'COMMANDER')),
    description VARCHAR(2000),
    visibility  VARCHAR(10)   NOT NULL DEFAULT 'PRIVATE' CHECK (visibility IN ('PRIVATE', 'UNLISTED', 'PUBLIC')),
    share_slug  VARCHAR(16) UNIQUE, -- for public share links later (/d/{slug})
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_decks_user ON decks (user_id, updated_at DESC);

CREATE TABLE deck_cards (
    id          BIGSERIAL PRIMARY KEY,
    deck_id     BIGINT      NOT NULL REFERENCES decks (id) ON DELETE CASCADE,
    scryfall_id UUID        NOT NULL REFERENCES card_printings (scryfall_id),
    board       VARCHAR(12) NOT NULL CHECK (board IN ('MAIN', 'SIDEBOARD', 'COMMANDER', 'MAYBEBOARD')),
    quantity    INTEGER     NOT NULL CHECK (quantity BETWEEN 1 AND 250),
    CONSTRAINT ux_deck_cards UNIQUE (deck_id, scryfall_id, board)
);
CREATE INDEX ix_deck_cards_deck ON deck_cards (deck_id);
