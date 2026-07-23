# ADR-016 — Database Migration Tooling

**Status:** Accepted **Date:** 2026-07-16 **Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [DATABASE — Migration Strategy](../DATABASE.md#database-migration-strategy) · [ADR-004 (Primary Database)](./ADR-004-Primary-Database.md) · [BACKEND_CODING_STANDARDS §11](../../03-engineering/BACKEND_CODING_STANDARDS.md#11-configuration-standards) · [IMPLEMENTATION_PLAN Phase 0](../../03-engineering/IMPLEMENTATION_PLAN.md#3-build-order)

---

## Context

[DATABASE.md](../DATABASE.md#database-migration-strategy) defines the schema-evolution _principles_ — additive,
reversible, incremental, reviewed, expand-then-contract — but explicitly leaves the **tool** unspecified: "the choice of
migration tool is an implementation concern outside this document's scope." The Phase 0 persistence foundation cannot be
built without one: the schema must have a single, versioned authority, and Hibernate's `ddl-auto` must **not** own the
schema (uncontrolled generation would violate the additive, reviewed strategy). A concrete tool is therefore required
before any persistence code lands, and the choice is precedent-setting — every future migration is authored in the
selected tool's format — which makes it an ADR-level decision ([CLAUDE.md §8](../../../CLAUDE.md)). The database is
fixed as PostgreSQL on Neon ([ADR-004](./ADR-004-Primary-Database.md)).

---

## Decision

Adopt **Flyway** (Community Edition) as the database migration tool.

- **Migrations are the sole schema authority.** Versioned SQL scripts define every schema change; Hibernate runs with
  `ddl-auto: none` and never creates, alters, or drops schema.
- **SQL-first, versioned scripts** live under the backend's migration resource location and follow Flyway's
  `V<version>__<description>.sql` convention, one focused, reviewable change per script — matching the "one logical
  change per migration" principle in [DATABASE.md](../DATABASE.md#database-migration-strategy).
- **Forward-oriented, expand-then-contract** evolution, consistent with the documented additive strategy; scripts are
  reversible in the practical sense the database strategy requires (deprecate-then-remove, backfill-before-enforce),
  rather than relying on automated down-migrations.
- Flyway runs on application startup and in tests, establishing the schema before the application uses it.

This ADR selects the tool only. The schema itself is introduced incrementally by the feature slices that own each table;
this decision creates no tables.

---

## Alternatives Considered

- **Liquibase.** Capable and database-agnostic, with changelog abstraction and built-in rollback. Rejected as heavier
  than needed: LedgerAI targets a single, fixed database (PostgreSQL, [ADR-004](./ADR-004-Primary-Database.md)), so the
  cross-database abstraction earns little, while transparent, Postgres-native SQL scripts are easier to review and
  reason about — a priority the migration strategy emphasizes.
- **Hibernate `ddl-auto` (create/update/validate as the schema source).** Rejected: uncontrolled, unreviewable schema
  generation is directly contrary to the additive, reviewed, versioned strategy in
  [DATABASE.md](../DATABASE.md#database-migration-strategy). `validate` is retained only as a possible safety check
  against the Flyway-built schema, never as the schema's source.
- **Hand-run SQL / manual scripts.** Rejected: no versioning, no applied-state history, no repeatability across
  environments — the problems a migration tool exists to solve.

---

## Consequences

### Advantages

- Transparent, Postgres-native SQL migrations that are easy to review — fitting the reviewed, incremental strategy.
- A versioned applied-migration history table gives a clear, ordered schema lineage across every environment.
- Minimal configuration and first-class Spring Boot integration; migrations apply consistently on startup and in tests.
- Keeps Hibernate strictly out of schema ownership, preserving the documented boundary between application and schema.

### Disadvantages

- SQL-first scripts are database-specific — acceptable because the database is fixed by
  [ADR-004](./ADR-004-Primary-Database.md) and portability is not a goal.
- No automated down-migrations in the Community Edition; reversibility is achieved by the strategy's
  expand-then-contract discipline rather than by generated rollbacks.

### Trade-offs

- We accept Postgres-specific migration scripts and manual reversibility discipline in exchange for transparency,
  simplicity, and a native fit with the fixed database and the documented migration principles.

---

## References

[DATABASE.md](../DATABASE.md) · [ADR-004](./ADR-004-Primary-Database.md) · [BACKEND_CODING_STANDARDS](../../03-engineering/BACKEND_CODING_STANDARDS.md) · [IMPLEMENTATION_PLAN](../../03-engineering/IMPLEMENTATION_PLAN.md)
