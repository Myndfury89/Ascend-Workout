# Database

Room, offline‑first. Schema version **3**, exported to `app/schemas/` so migrations
have a versioned baseline from the start.

## Entities

**v1**

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

**v2 (Milestone 2 — Workouts)**

| Entity | Purpose |
|---|---|
| `exercise` | Exercise catalog entry (name, category, primary attribute, measurement type, default unit) |
| `workout` | A logged training session (title, difficulty, status, performedAt, duration) |
| `workout_set` | One set within a workout (reps / weight / duration / distance, denormalised `volume`) |

**v3 (Status motion system — ProgressionEventQueue)**

| Entity | Purpose |
|---|---|
| `progression_event` | Persisted presentation event (XP/attribute/level/rank change) drained by the Status animation, exactly‑once per `(batchId, sequence)` |

Foreign keys cascade from `user_profile` → progress/stats/xp/attributes/quests/workouts/events,
`quest` → objectives → entries, and `workout` → sets. A `workout_set` also references
`exercise` with `ON DELETE RESTRICT` (catalog rows can't be deleted while referenced).
Timestamps are epoch millis; enums are stored as their names and mapped to domain enums
in the data layer.

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

`PlayerDao`, `XpDao`, `AttributeDao`, `QuestDao`, `ExerciseDao`, `WorkoutDao`.
Inserts that must dedupe use `@Insert(onConflict = IGNORE)` and return the row id
(`-1` when blocked). Relation queries (`@Transaction` + `@Relation`) load a quest
with its objectives and entries — and a workout with its sets (each joined to its
exercise) — in one shot for the UI.

## Migrations

Schema export is enabled (`room.schemaLocation`). Each schema change bumps the
version and adds a `Migration` (registered via `AscendMigrations.ALL`) plus a
`MigrationTestHelper` test against the exported JSON. No destructive fallback is
configured.

- **v1 → v2** (`AscendMigrations.MIGRATION_1_2`): adds `exercise`, `workout`, and
  `workout_set` with their indices and foreign keys.
- **v2 → v3** (`AscendMigrations.MIGRATION_2_3`): adds `progression_event` (the
  ProgressionEventQueue) with its FK and the unique `(batchId, sequence)` guard.

Both are validated on the JVM by `AscendMigrationTest` (Robolectric, no emulator).
The exported schemas are wired into the debug source set's assets so
`MigrationTestHelper` can load them under Robolectric; they never ship in the
release APK.
