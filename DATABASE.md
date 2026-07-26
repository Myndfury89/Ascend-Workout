# Database

Room, offline‑first. Schema version **1**, exported to `app/schemas/` so migrations
have a versioned baseline from the start.

## Entities (v1)

| Entity | Purpose |
|---|---|
| `user_profile` | Local player profile (single local user by default) |
| `player_progress` | Derived level, lifetime XP, rank, streaks, totals |
| `player_stats` | The five core attributes |
| `xp_transaction` | Immutable XP ledger |
| `attribute_transaction` | Immutable attribute ledger |
| `quest` | Quest header |
| `quest_objective` | An objective within a quest (target, current, set sizes, primary attribute) |
| `quest_progress_entry` | One logged contribution toward an objective |

Foreign keys cascade from `user_profile` → progress/stats/xp/attributes/quests, and
`quest` → objectives → entries. Timestamps are epoch millis; enums are stored as
their names and mapped to domain enums in the data layer.

## Idempotency constraints (the important ones)

| Unique index | Guarantees |
|---|---|
| `xp_transaction (transactionType, sourceType, sourceId)` | XP awarded exactly once per source; reversal still allowed |
| `attribute_transaction (attributeType, transactionType, sourceType, sourceId)` | Each attribute granted once per source |
| `quest_progress_entry (sourceApplication, externalRecordId)` | No duplicate imports; manual entries (nulls) never blocked |

Indexed columns: FKs, `quest.scheduledDate`, `quest.status`,
`quest_progress_entry.externalRecordId`, and the XP source pair.

## Transactions

Multi‑step mutations run inside `db.withTransaction { }` so they are atomic:
completing a quest, awarding XP, updating attributes, and recomputing progress all
commit together or not at all. Nested `withTransaction` calls (e.g. quest
completion invoking the progression repository) coalesce into one transaction.

## DAOs

`PlayerDao`, `XpDao`, `AttributeDao`, `QuestDao`. Inserts that must dedupe use
`@Insert(onConflict = IGNORE)` and return the row id (`-1` when blocked). Relation
queries (`@Transaction` + `@Relation`) load a quest with its objectives and entries
in one shot for the UI.

## Migrations

Schema export is enabled (`room.schemaLocation`). While the app is pre‑release we
keep version 1; once released, each schema change bumps the version and adds a
`Migration` plus a `MigrationTestHelper` test against the exported JSON. No
destructive fallback is configured.
