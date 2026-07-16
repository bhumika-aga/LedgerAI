# ADR-017 — Integration and Local Persistence Test Database

**Status:** Proposed
**Date:** 2026-07-16
**Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [TESTING_STRATEGY §5](../../03-engineering/TESTING_STRATEGY.md#5-integration-testing-strategy) · [TESTING_STRATEGY §13](../../03-engineering/TESTING_STRATEGY.md#13-test-environments) · [ADR-004 (Primary Database)](./ADR-004-Primary-Database.md) · [ADR-016 (Database Migration Tooling)](./ADR-016-Database-Migration-Tooling.md)

---

## Context

[TESTING_STRATEGY §5](../../03-engineering/TESTING_STRATEGY.md#5-integration-testing-strategy) requires integration
tests to run against **a real database, not mocks**,
and [§13](../../03-engineering/TESTING_STRATEGY.md#13-test-environments)
requires local and CI tests to run with **no dependence on shared or production services**. The database is fixed as
**PostgreSQL** and relies on Postgres-specific features — `citext`, `jsonb`, and GIN full-text search
([ADR-004](./ADR-004-Primary-Database.md), [DATABASE.md](../DATABASE.md)) — so an in-memory substitute such as H2 would
misrepresent real behavior and is not acceptable. The strategy names _what_ to test but, like the migration tooling,
does
not name the _mechanism_ that provides the real Postgres. That mechanism is required to verify the persistence
foundation (the application must start and initialize persistence against a real database) and is the same class of
implementation-level decision as [ADR-016](./ADR-016-Database-Migration-Tooling.md).

---

## Decision (proposed)

Use **Testcontainers** to provision an ephemeral PostgreSQL container for integration tests, in both local and CI runs.

- Each test run starts a disposable PostgreSQL instance matching the production major version; **Flyway**
  ([ADR-016](./ADR-016-Database-Migration-Tooling.md)) builds the schema inside it, so tests exercise the real schema
  and
  real Postgres features.
- No shared or production database is involved, satisfying the isolation requirement of
  [§13](../../03-engineering/TESTING_STRATEGY.md#13-test-environments); the only prerequisite is a container runtime,
  which CI already provides.
- Spring Boot's Docker Compose support MAY additionally back local `bootRun` for developer convenience, but the
  **test gate** is Testcontainers, so tests never depend on a developer's hand-started services.

This ADR is **Proposed** and awaits approval before the corresponding persistence test infrastructure is implemented.

---

## Alternatives Considered

- **Spring Boot Docker Compose support as the test mechanism.** Good for local `bootRun`, but as the integration-test
  gate it depends on a shared, hand-managed compose stack and gives weaker per-run isolation than disposable containers.
  Recommended as an optional local-run convenience, not the test gate.
- **H2 / embedded in-memory database.** Rejected: cannot faithfully emulate the Postgres-specific features the schema
  depends on ([ADR-004](./ADR-004-Primary-Database.md)); would give false confidence.
- **A shared or staging database for tests.** Rejected: violates the "no dependence on shared services" rule
  ([§13](../../03-engineering/TESTING_STRATEGY.md#13-test-environments)), introduces cross-run interference and
  flakiness, and risks contaminating non-test data.

---

## Consequences

### Advantages

- Tests run against real PostgreSQL with the real, Flyway-built schema — the highest-fidelity option.
- Fully isolated and repeatable; no shared state, matching the testing rules on determinism and isolation.
- The same mechanism works identically on a developer machine and in CI.

### Disadvantages

- Requires a container runtime available to the test environment (present in CI; a local prerequisite for developers).
- Container startup adds some time to integration-test runs, though it stays off the fast unit-test path.

### Trade-offs

- We accept a container-runtime prerequisite and modest integration-test startup cost in exchange for real-database
  fidelity and complete isolation from shared services.

---

## References

[TESTING_STRATEGY.md](../../03-engineering/TESTING_STRATEGY.md) · [ADR-004](./ADR-004-Primary-Database.md) · [ADR-016](./ADR-016-Database-Migration-Tooling.md) · [DATABASE.md](../DATABASE.md)
