# MTG Vault

Full-stack web app for managing a **Magic: The Gathering** collection: MTGO-style deckbuilder with format validation, collection containers, and multi-marketplace prices.

**Stack:** Java 25 · Spring Boot 4.1 · Spring Security · PostgreSQL · Flyway · React 19 + TypeScript · Vite · Tailwind CSS · TanStack Query · dnd-kit

> Live: _coming soon_ · Demo account: _coming soon_

## Project structure

```
mtg-collection-manager/
├── backend/            Spring Boot API (modular monolith)
│   └── src/main/java/com/jacob/mtg/
│       ├── auth/       register, login, JWT, refresh tokens
│       ├── user/       profile, GDPR export & account deletion
│       ├── card/       Scryfall import + card search
│       ├── price/      price sources (Adapter pattern)
│       ├── collection/ inventory + containers
│       ├── deck/       decks + format rules (Strategy pattern)
│       ├── legal/      versioned ToS / Privacy + consent log
│       └── common/     security config, errors, health
├── frontend/           React + Vite SPA
├── docker-compose.yml  local PostgreSQL
└── .github/workflows/  CI (backend tests + frontend lint/build)
```

## Run locally

Requirements: JDK 25, Node 22+, Docker Desktop.

```bash
# 1. Database
docker compose up -d

# 2. Backend (or run MtgApplication from IntelliJ)
cd backend
mvn spring-boot:run
# -> http://localhost:8080/api/health
# -> http://localhost:8080/swagger-ui.html

# 3. Frontend
cd frontend
npm install
npm run dev
# -> http://localhost:5173
```

## Configuration (environment variables)

| Variable       | Default (dev)                            | Purpose                          |
|----------------|------------------------------------------|----------------------------------|
| `DB_URL`       | `jdbc:postgresql://localhost:5432/mtg`   | PostgreSQL JDBC URL              |
| `DB_USER`      | `mtg`                                    | DB user                          |
| `DB_PASSWORD`  | `mtg_dev_password`                       | DB password                      |
| `CORS_ORIGINS` | `http://localhost:5173`                  | Allowed frontend origin(s)       |
| `PORT`         | `8080`                                   | HTTP port (set by host in prod)  |
| `VITE_API_URL` | _(empty: uses Vite proxy)_               | Backend URL for the built SPA    |

## Legal

MTG Vault is unofficial Fan Content permitted under the Fan Content Policy. Not approved/endorsed by Wizards. Portions of the materials used are property of Wizards of the Coast. ©Wizards of the Coast LLC.
Card data and images provided by [Scryfall](https://scryfall.com).
