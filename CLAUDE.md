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

- Phase: 16 complete — Payment Service vertical slice implemented (Non-custodial payable, mock/live provider adapter, HMAC signed webhooks, idempotent transactions, receipts, outbox emission)
- Last exit criterion met: Phase 16 — Provider webhook signature cryptographically verified; duplicate deliveries safely ignored
- Open: Phase 17 — Implement the Rental-Payment Saga (ADR-009 orchestrator, atomic listing reservation, payable auto-provisioning, tenancy activation)
- Branch: `rental-payment-saga`
- Next: Branch off main, copy notes from `notes/17-rental-payment-saga/`, execute atomic commits, and run `test-saga-workflow.sh`
