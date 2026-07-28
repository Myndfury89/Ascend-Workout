# Database

Room, offline‑first. Schema version **10**, exported to `app/schemas/` so migrations
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

**v4 (Class system)**

| Entity | Purpose |
|---|---|
| `player_class` | The player's primary/secondary class selection (+ start dates, reason, change source, cooldown/respec schema in v6) |
| `class_xp_transaction` | Immutable Class‑XP ledger (per class), separate from the class‑neutral player XP |
| `class_proficiency_transaction` | Immutable ledger for class‑unique proficiencies (Force / Body Mastery / Energy Control) |

`exercise` also gains a `tags` column (comma‑separated activity tags) that the class
affinity engine reads.

**v5** — `progression_event.subjectKey` (class id / proficiency key for class events).

**v6 (Class system — DB definitions + history)**

| Entity | Purpose |
|---|---|
| `class_definition` | Seed‑backed, updateable class definitions (multipliers, favored tags/categories, unique proficiency, presentation metadata, association ids) |
| `class_history` | Append‑only record of class selections, so a switch preserves history and never deletes earned ledgers |

The class ledgers also gain a `rewardType` column, widening the exactly‑once guard to
`(subject, transactionType, sourceType, sourceId, rewardType)`.

**v7 (Customizable Daily Quest targets)**

| Entity | Purpose |
|---|---|
| `quest_template` | Seed‑backed Daily Quest blueprints with configurable safe ranges (min/max/default/step, quick‑add, set‑size limits, variations, safety threshold) |

**v8 (Daily Quest interval scheduling)**

| Entity | Purpose |
|---|---|
| `quest_interval_schedule` | The execution plan attached to a quest (mode, active window, count, distribution + redistribution settings) |
| `quest_interval` | A scheduled portion of the daily target within a time slot |
| `quest_checkpoint` | A cumulative "by this time" milestone |
| `quest_interval_progress_entry` | One logged contribution toward an interval (unique `(sourceApplication, externalRecordId)` blocks double‑counted imports) |

**v9 (Adaptive Training & Progressive Overload)**

| Entity | Purpose |
|---|---|
| `exercise_prescription` | A current/proposed exercise prescription (sets, rep range, weight, rest, variation, …) |
| `training_readiness_snapshot` | A structured, explainable readiness evaluation |
| `progression_recommendation` | A pending/accepted/applied progression recommendation (survives restart) |
| `progression_milestone` | A proven progression milestone; unique `milestoneKey` makes rewards exactly‑once |

**v10 (Bodyweight variation graph)**

| Entity | Purpose |
|---|---|
| `exercise_variation` | A movement in an exercise's variation graph (push-up, pull-up, …): tier, tags, assistance type/value, external-load support, range-of-motion level, tempo profile |
| `exercise_variation_edge` | A directed advance/regress transition between two variations, carrying its real-performance gate (exposures, reps, sets, RPE/RIR, assistance/ROM/tempo, optional class unlock); unique `(sourceVariationId, destinationVariationId, progressionType)` |

Foreign keys cascade from `user_profile` → progress/stats/xp/attributes/quests/workouts/events/class‑selection/class‑ledgers,
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
- **v3 → v4** (`AscendMigrations.MIGRATION_3_4`): adds `exercise.tags` (backfilling
  the built‑in catalog), `player_class`, and the two class ledgers with their
  exactly‑once guards.
- **v4 → v5** (`AscendMigrations.MIGRATION_4_5`): adds `progression_event.subjectKey`.
- **v5 → v6** (`AscendMigrations.MIGRATION_5_6`): adds `class_definition` and
  `class_history`, the richer `player_class` columns, and the `rewardType` column +
  widened unique guard on the class ledgers.
- **v6 → v7** (`AscendMigrations.MIGRATION_6_7`): adds `quest_template`.
- **v7 → v8** (`AscendMigrations.MIGRATION_7_8`): adds the four interval‑scheduling
  tables with their FKs, indices, and the import dedup guard.
- **v8 → v9** (`AscendMigrations.MIGRATION_8_9`): adds the four adaptive‑training
  tables, including the unique `milestoneKey` reward guard.
- **v9 → v10** (`AscendMigrations.MIGRATION_9_10`): adds `exercise_variation` and
  `exercise_variation_edge`, including the unique `(source, destination, progressionType)`
  edge guard.

All are validated on the JVM by `AscendMigrationTest` (Robolectric, no emulator).
The exported schemas are wired into the debug source set's assets so
`MigrationTestHelper` can load them under Robolectric; they never ship in the
release APK.
