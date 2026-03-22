# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack personal finance app: **Spring Boot 3.3.6 (Kotlin)** backend on GCP Cloud Run + **Flutter** frontend on Firebase Hosting. Authentication is done exclusively via **Firebase Auth SDK** (no JWT/password auth).

## Build & Run Commands

### Backend

```bash
# Local dev stack (Spring Boot + PostgreSQL)
docker compose up --build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "br.com.useinet.finance.service.TransacaoServiceTest"

# Build JAR (skip tests)
./gradlew build -x test

# Code coverage report (HTML in build/reports/jacoco/)
./gradlew jacocoTestReport
```

### Frontend (run from `frontend/`)

```bash
flutter pub get
flutter run

# Build for web (production)
flutter build web --release --dart-define=API_URL=<backend-url>
```

### Database

Flyway migrations run automatically on startup from `src/main/resources/db/migration/V*__*.sql`. No manual steps needed.

## Architecture

### Backend (`src/main/kotlin/br/com/useinet/finance/`)

- **`config/`** — Spring Security, Firebase Admin SDK setup, OpenAPI (Swagger at `/swagger-ui/index.html`)
- **`security/`** — `FirebaseAuthenticationFilter` validates every request's Firebase token against Firebase Admin SDK (Application Default Credentials)
- **`controller/`** — 8 REST controllers: `Transacao`, `Dashboard`, `Conta`, `Categoria`, `User`, `Orcamento`, `Meta`, `BillingEvent`
- **`service/`** — Business logic; `DashboardService` aggregates monthly summaries; `TransacaoService` handles CSV/XLSX exports and recurring transaction scheduler; `NotificationService` sends FCM push notifications
- **`model/`** + **`repository/`** — JPA entities and Spring Data repos (PostgreSQL); enums: `TipoTransacao`, `FrequenciaRecorrencia`

**Active Spring profile:** `prod` in Cloud Run (uses GCP Secret Manager for DB credentials), default for local dev (uses `application.properties`).

### Database Schema

Core tables (created via Flyway V1–V20): `usuarios`, `categorias`, `contas`, `transacoes`, `billing_events`, `orcamentos`, `metas`.
Adding a new table = new `V{N}__description.sql` migration file (next: `V24__...sql`). Note: `transacoes.data` is type `DATE` (mapped to `LocalDate` in Kotlin).

### Frontend (`frontend/lib/`)

- **`core/`** — HTTP client, base models, providers, utility services
- **`features/`** — Modular screens (transactions, dashboard, categories, accounts, profile, budgets/orcamentos, goals/metas)
- **State management**: Provider
- **Auth**: Firebase Auth SDK (token auto-refreshed, see commit `a47a73b`)
- **Dark mode**: follows system preference

### Infrastructure

```
Flutter (iOS/Android/Web)
    ↓ HTTPS REST
Spring Boot (Cloud Run, us-central1)
    ↓ JDBC
PostgreSQL (Cloud SQL)
```

Secrets live in GCP Secret Manager (`finance-db-*`). The Cloud Run service account uses Workload Identity — no service account keys in code.

## CI/CD

| Workflow | Trigger | Purpose |
|---|---|---|
| `ci.yml` | Push to `main` (backend changes) | Test → build Docker image → deploy to Cloud Run |
| `frontend.yml` | Push to `main` (`frontend/` changes) | Build Flutter web → deploy to Firebase Hosting |
| `project-automation.yml` | PR/issue events | Move GitHub Projects cards to In Progress / Done |
| `roadmap-update.yml` | Issue status changes | Update issue #78 (roadmap) automatically |

Docker image is tagged with git SHA + `latest`, pushed to Artifact Registry.

## Key Conventions

- **Authentication**: All endpoints are protected by `FirebaseAuthenticationFilter`. Never add password-based or JWT auth.
- **New API endpoint**: Add controller → service → repository. Follow existing patterns in `TransacaoController`.
- **Database changes**: Always via Flyway migration. Increment version number (next: `V24__...sql`).
- **Secrets**: Use GCP Secret Manager in prod profile. Never hardcode credentials.
- **GitHub Projects board**: When starting an issue, move card to "In Progress"; when done, move to "Done" (automated via `project-automation.yml`, but verify manually if needed).
- **Tests**: Use Testcontainers (PostgreSQL) for integration tests. No mocking the database.
