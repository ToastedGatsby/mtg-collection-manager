-- Hibernate's schema validation maps String to VARCHAR; CHAR(64) (bpchar) fails `ddl-auto: validate`.
-- Never edit an applied migration (Flyway checksums) - always add a new one.
ALTER TABLE refresh_tokens ALTER COLUMN token_hash TYPE VARCHAR(64);
