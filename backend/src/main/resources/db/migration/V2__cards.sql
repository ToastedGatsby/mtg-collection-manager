-- Card catalog imported from Scryfall bulk data.
-- oracle_cards = the abstract card (rules text, legality); card_printings = each physical printing (set, art, price ids).

CREATE TABLE oracle_cards (
    oracle_id      UUID PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    mana_cost      VARCHAR(100),
    cmc            NUMERIC(5, 1) NOT NULL DEFAULT 0,
    type_line      VARCHAR(200),
    oracle_text    TEXT,
    colors         VARCHAR(1)[] NOT NULL DEFAULT '{}',
    color_identity VARCHAR(1)[] NOT NULL DEFAULT '{}',
    keywords       TEXT[]       NOT NULL DEFAULT '{}',
    legalities     JSONB        NOT NULL DEFAULT '{}'::jsonb,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_oracle_cards_name ON oracle_cards (lower(name));

CREATE TABLE card_printings (
    scryfall_id      UUID PRIMARY KEY,
    oracle_id        UUID         NOT NULL REFERENCES oracle_cards (oracle_id),
    set_code         VARCHAR(10)  NOT NULL,
    set_name         VARCHAR(200) NOT NULL,
    collector_number VARCHAR(20)  NOT NULL,
    rarity           VARCHAR(20)  NOT NULL,
    lang             VARCHAR(5)   NOT NULL DEFAULT 'en',
    finishes         VARCHAR(10)[] NOT NULL DEFAULT '{}',
    image_uris       JSONB,
    tcgplayer_id     INTEGER,
    cardmarket_id    INTEGER,
    released_at      DATE
);
CREATE INDEX ix_card_printings_oracle ON card_printings (oracle_id);
CREATE INDEX ix_card_printings_set ON card_printings (set_code, collector_number);
