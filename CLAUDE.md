# Lagos Rent Platform — CLAUDE.md

Trust-first rental marketplace (Lagos MVP) built as a real distributed system: 9 Spring
Boot deployables, Spring Cloud Config + Eureka, Keycloak, RabbitMQ, database-per-service,
saga orchestration, full observability. This is a personal learning project — the goal is
understanding every piece well enough to speak to it confidently, not just shipping working
code. Owner has ~6 months of Spring Boot experience from tutorials and small projects only.

## Required reading

At the start of any session where these aren't already in context, read in full:

- `docs/architecture/architecture.md` — the architecture blueprint, all 27 sections
- `docs/implementation-guide.md` — phase-by-phase build order, exit criteria, checklists

Every implementation decision must trace back to one of these two documents or an ADR in
`docs/adr/`. If something isn't covered by either, stop and ask rather than improvising —
don't quietly invent an architectural choice.

## Stack

- Java 21, Spring Boot 4.1.x, Spring Cloud 2025.1.x — import the matching Spring Cloud BOM,
  don't pick these independently (`docs/architecture/architecture.md` §27, "Version baseline")
- Maven, PostgreSQL (database-per-service), RabbitMQ, Keycloak, MinIO/S3
- Docker Compose first, Kubernetes later (implementation-guide.md Phase 24)
- OpenTelemetry + Prometheus/Grafana + Tempo/Jaeger + Loki

## How we work

This is a teaching engagement, not a code-generation engagement. For each phase in
`docs/implementation-guide.md` Part II:

1. Explain the plan before writing code: what we're building, which architecture rule(s)
   it serves, what could go wrong.
2. Wait for a go-ahead before implementing, unless told to proceed through several phases
   at once.
3. Write the explanation and the code together into
   `notes/<phase-number>-<phase-slug>/` as one or more markdown files — split into multiple
   files rather than letting one get overwhelming. Don't create or edit files directly in
   the codebase (e.g. under `services/`) unless explicitly asked to; the code lives in the
   notes, and I'll copy it into the actual project myself.
4. Update "Current status" below before ending the session.

Follow the phase order exactly (Phase 0 → Phase 25, see "Phase dependency map"). Don't
scaffold every domain service up front — complete each as a thin vertical slice. Don't
skip a phase's exit criteria to move faster.

Tests are not deferred to Phase 22. Write unit and Testcontainers tests as each service is
built (see the guide's "Service completion checklist"), using the test-writer subagent
below to introduce concepts along the way. Phase 22 is where testing becomes a first-class
topic in its own right — contract tests, chaos/failure tests, full distributed end-to-end —
flag it explicitly when we get there. (Change this if you'd rather defer all testing to
Phase 22 as originally planned.)

## Non-negotiable architecture rules

Full list: `docs/implementation-guide.md` §3. The ones that bite most often:

- No shared database, JPA entity, or repository across services; no cross-service table
  queries.
- The API Gateway has no domain logic.
- Every published event uses the transactional outbox; every consumer is idempotent
  (at-least-once delivery, never exactly-once).
- Search is a projection, never the source of truth. Payment redirects are never proof of
  payment.
- Every synchronous call has an explicit timeout. Every admin mutation is audited.
- Production Git Workflow: Never push directly to main. Work in short-lived feature branches
  (`feat/<scope>-<desc>`, `fix/...`, `test/...`). Use atomic Conventional Commits
  (`feat(payment): ...`, `test(rental): ...`). Provide clear commit and PR instructions
  in every phase note (see `notes/git-industry-guide/`).
- **Interactive Shell Safety & Test Endpoint Standards (Strict Non-Negotiable)**:
  - **No Pre-existing State Assumptions**: Always design test flows to provision fresh upstream entities from scratch (Property -> Unit -> Listing -> Application). Never assume the database has an existing listing or application.
  - **Step-by-Step Interactive Execution**: Provide tests as modular, step-by-step commands with clear explanations of what happens behind the scenes. Never push the user to run batch scripts all at once when they want interactive step-by-step observation.
  - **No `exit 1` in Interactive Commands**: Never include `exit 1` in snippets meant for interactive shell execution, as it terminates the user's terminal window and loses exported session variables.
  - **Edge Gateway Token Propagation**: Always include `-H "Authorization: Bearer $TOKEN"` on API Gateway routes (`:8080`) since Gateway enforces authentication on all non-public routes.
  - **Zero `!` in Double Quotes**: NEVER output shell commands containing exclamation marks `!` inside double quotes (`"..."`). In macOS Zsh/Bash, `!` triggers history expansion, swallowing quotes and hanging the shell. Use single quotes `'...'` or omit `!` entirely.
  - Format all JSON API responses with `jq .` so the user can inspect every field and status code clearly.
  - **Always Quote `jq` Filters in Single Quotes**: In macOS Zsh, square brackets `[...]` (like `[0]`) trigger pattern/glob expansion unless quoted. Writing `jq .measurements[0].value` crashes with `zsh: no matches found`. ALWAYS wrap jq filters in single quotes: `jq '.measurements[0].value'`.
- **Database Migrations & UUID Standard (Strict Non-Negotiable)**:
  - Always use `gen_random_uuid()` for generating UUID primary keys in PostgreSQL Flyway migration files (`DEFAULT gen_random_uuid()`).
  - NEVER use `uuid_generate_v4()` or require `CREATE EXTENSION "uuid-ossp"`. In PostgreSQL 13+, `gen_random_uuid()` is built into the core runtime and avoids needing superuser extension privileges in managed production databases (AWS RDS, GCP Cloud SQL).
- **Production Standards & Fail-Fast Data Invariants (Strict Non-Negotiable)**:
  - **Zero Placeholder / Dummy Fallback Names**: NEVER use placeholder names or fallback defaults (e.g., `?: "Landlord Partner"`, `?: "Tenant"`, `?: "Valued Customer"`, dummy titles, mock defaults) just to make code succeed in local development.
  - **Fail Fast**: If an entity or user profile lacks required business fields (e.g., first name, last name, email), the system MUST fail fast by throwing an explicit domain/validation exception or rejecting the event. Never paper over missing or corrupt data with artificial strings. Code must strictly adhere to production invariants.

## Common AI Mistakes to Avoid & Strict Production Alternatives

Every AI assistant working on this codebase must strictly internalize and avoid these three systemic mistakes:

### 1. Relying on Generic Training Boilerplate / Defaults (e.g., `guest:guest`, default ports)
* **The Anti-Pattern:** Hallucinating or copy-pasting textbook defaults (e.g. `guest:guest` for RabbitMQ, `admin:admin`, generic URLs) without inspecting the repository. This causes API calls to fail silently or with 401 Unauthorized, returning `null` in `jq` and invalidating user verification.
* **Strict Alternative:** Every credential, port, and configuration value MUST be verified against `docker-compose.yml`, `lagos_rent_config/`, or existing service configurations BEFORE presenting it. If a service/container is running, test commands in the shell to ensure they succeed with HTTP 200 before writing them in notes.

### 2. Papering Over Errors with Placeholder Strings (e.g., `?: "unknown"`, `?: "Tenant"`)
* **The Anti-Pattern:** Using defensive ternary defaults like `context.getLastThrowable() != null ? context.getLastThrowable().getMessage() : "unknown"` to prevent NPEs. This produces useless production logs like `Retrying event processing due to: unknown`, destroying observability for the on-call engineer.
* **Strict Alternative:** Adhere to enterprise error observability. In Spring Retry / Resilience4j, register dedicated listener hooks (e.g., `RetryListener.onError`) where the `throwable` is non-null. Always capture and log the full exception class, message, and stack trace (`throwable.getClass().getName()`, `throwable.getMessage()`, `throwable`). Never swallow root causes.

### 3. Tunnel-Vision Code Suggestions that Violate Established Invariants
* **The Anti-Pattern:** Focusing exclusively on a single new pattern (e.g. bounded retries) and arbitrarily altering an earlier, established platform contract (e.g. switching `acknowledge-mode: manual` to `auto` or dropping `Channel channel`) just to make the snippet easier to implement.
* **Strict Alternative:** Always cross-reference earlier architectural agreements (`docs/architecture/`, `notes/11-rabbitmq-and-event-contracts/`, etc.). New resilience patterns MUST compose cleanly ON TOP of existing platform invariants (e.g., building bounded in-consumer retries that preserve manual acks and dead-letter routing), never by silently dismantling prior architectural guarantees.

## Verified Local Platform State & Seeded Test Identities (Strict Non-Negotiable)

To prevent AI amnesia and fake data generation after session restarts or context compactions, the assistant must use these verified constants:

- **PostgreSQL Container (`lagos-rent-postgres`):** Port `5432`, Superuser: `postgres`, Superpass: `postgres`
  - Core databases: `account_db`, `listing_db`, `rental_db`, `payment_db`, `search_db`, `notification_db`
- **RabbitMQ Container (`lagos-rent-rabbitmq`):** AMQP `:5672`, Management UI `:15672`
  - Credentials: User: `lagos_user`, Password: `lagos_rabbit_password`
- **Verified Seeded Tenant Identity:**
  - `keycloak_user_id`: `cc15287b-2af6-49fb-9ad7-9b30e1c57484`
  - Email: `tenant@example.com`, Name: `Tenant User`
- **Verified Seeded Landlord Identity:**
  - `keycloak_user_id`: `818746e8-5729-4aa5-91fc-a99199dbd2aa`
  - Email: `landlord@example.com`, Name: `Landlord User`
- **Pre-Flight Test Identity Rule:** NEVER invent synthetic placeholder strings (e.g., `"test-tenant-id"`). Any event published in chaos testing or runbooks MUST use the verified seeded IDs above so that downstream queries (e.g., `AccountClient` -> `account-service`) resolve successfully with HTTP 200 instead of HTTP 404.

## Context Window & Session Management (80% Threshold Alert)

- **80% Context Warning Mandate:** The assistant must actively monitor conversation token consumption. When conversation history approaches **80% of context window capacity** (before automated lossy compaction occurs), the assistant MUST explicitly notify the user:
  > *"Context window is approaching 80% capacity. To prevent lossy compaction and agent amnesia, commit current work to git and start a fresh session for the next phase."*
- **Phase Scoping:** Threads should be scoped per Phase. When a phase is completed, the user should be guided to commit changes, update `Current status` in `CLAUDE.md`, and start a fresh chat thread.

## Subagents

- **debug** — when a prompt starts with "I have a bug", hand off to the `debug-helper`
  subagent so exploration and stack traces don't consume the main thread's context. It
  reports back a summary and a fix, not the raw investigation.
- **test-writer** — writes and explains tests. No prior testing experience — introduce
  concepts as they come up (what a Testcontainers integration test is and why, what a
  contract test proves that a unit test doesn't, etc.), don't assume familiarity.

## Repos

Two repositories: this application monorepo, and a separate `lagos-rent-config` repo for
Spring Cloud Config data (`docs/architecture/architecture.md` §22). Decide where the config repo lives locally
(sibling directory recommended) in Phase 2 and record the decision here.

`notes/` is local-only — add it to `.gitignore` in Phase 1.

## Commands

- `mvn -B verify` — full build from repo root (Phase 1 exit criterion)
- `docker compose up` — local platform (Phase 5)
- _(add more here as they're established)_

- Phase: 19 complete — Decentralized admin operations implemented across domain services (account, listing, rental, payment, notification) preserving service autonomy, with segmented gateway routing, audit trails, and optimistic concurrency
- Last exit criterion met: Phase 19 — Privileged admin operations functional via Gateway without central admin service
- Open: Phase 20 — Add resilience deliberately (Resilience4j circuit breakers, timeouts, bulkheads, rate limiters, bounded retries, DLQ recovery, and chaos testing)
- Branch: `feature/resilience-deliberate`
- Next: Author notes/20-resilience-deliberately/00-plan-and-core-concepts.md and explain resilience engineering from first principles


