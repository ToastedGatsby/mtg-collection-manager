-- Scryfall import: prices, purchase links, default printing per card, search indexes, import log.

-- Some double-faced names / type lines ("Front // Back") are long; give them room.
ALTER TABLE oracle_cards ALTER COLUMN name TYPE VARCHAR(400);
ALTER TABLE oracle_cards ALTER COLUMN type_line TYPE VARCHAR(400);
ALTER TABLE oracle_cards ALTER COLUMN mana_cost TYPE VARCHAR(200);
-- Un-cards exist with absurd mana values (Gleemax: 1,000,000); NUMERIC(5,1) would overflow.
ALTER TABLE oracle_cards ALTER COLUMN cmc TYPE NUMERIC(10, 1);

-- The printing shown in search results (image + price); chosen after each import.
ALTER TABLE oracle_cards ADD COLUMN default_scryfall_id UUID;

ALTER TABLE card_printings
    ADD COLUMN promo             BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN border_color      VARCHAR(20),
    ADD COLUMN purchase_uris     JSONB,
    -- Latest prices from Scryfall: USD = TCGplayer market, EUR = Cardmarket trend, TIX = Cardhoarder (MTGO)
    ADD COLUMN price_usd         NUMERIC(10, 2),
    ADD COLUMN price_usd_foil    NUMERIC(10, 2),
    ADD COLUMN price_usd_etched  NUMERIC(10, 2),
    ADD COLUMN price_eur         NUMERIC(10, 2),
    ADD COLUMN price_eur_foil    NUMERIC(10, 2),
    ADD COLUMN price_tix         NUMERIC(10, 2),
    ADD COLUMN prices_updated_at TIMESTAMPTZ;

-- Trigram indexes make "name contains ..." (ILIKE '%bolt%') fast instead of a full table scan.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX ix_oracle_cards_name_trgm ON oracle_cards USING gin (name gin_trgm_ops);
CREATE INDEX ix_oracle_cards_type_trgm ON oracle_cards USING gin (type_line gin_trgm_ops);
-- GIN on arrays supports the colour filters (@> contains, <@ contained by).
CREATE INDEX ix_oracle_cards_colors ON oracle_cards USING gin (colors);
CREATE INDEX ix_oracle_cards_color_identity ON oracle_cards USING gin (color_identity);
CREATE INDEX ix_oracle_cards_cmc ON oracle_cards (cmc);
CREATE INDEX ix_card_printings_oracle_rarity ON card_printings (oracle_id, rarity);

CREATE TABLE scryfall_imports (
    id               BIGSERIAL PRIMARY KEY,
    bulk_type        VARCHAR(40) NOT NULL,
    bulk_updated_at  TIMESTAMPTZ,
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at      TIMESTAMPTZ,
    status           VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED')),
    cards_processed  INTEGER     NOT NULL DEFAULT 0,
    error_message    TEXT
);
