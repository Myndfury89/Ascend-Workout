# Testing

Tests run on the **JVM** — fast, no emulator required. Pure logic uses plain
JUnit; database, repository, and Compose UI tests use **Robolectric**.

Run everything:

```bash
./gradlew testDebugUnitTest
```

## What's covered today (64 tests, all passing)

**Progression (pure):**
- `LevelCalculatorTest` — formula, monotonicity, cumulative XP, `resolve`, boundaries
- `XpCalculatorTest` — workout stacking, duration cap, quest partial/over‑completion caps
- `RankCalculatorTest` — thresholds, lifetime‑XP nudge, rank never decreases
- `AttributeProgressCalculatorTest` — volume + discipline, caps, difficulty, merge
- `StreakCalculatorTest` — first/consecutive/same‑day/missed‑day, history preserved
- `SetSuggestionEngineTest` — even sets, remainder folding, max respected, totals

**Database (Robolectric, in‑memory Room):**
- `AscendDatabaseDaoTest` — XP dedup, import dedup, uneven‑set accumulation
- `WorkoutDaoTest` — workout↔set relation join to the exercise, set ordering, per‑set delete
- `AscendMigrationTest` — v1→v2 and v2→v3 migrations validated against the exported schema; data survives and the new tables (incl. the unique `(batchId, sequence)` guard) are usable

**Repositories (Robolectric integration):**
- `ProgressionRepositoryTest` — award once → level up, reverse → recompute, attributes once, multi‑level award
- `QuestFlowIntegrationTest` — **the acceptance‑criteria proof**: create 200‑push‑up quest → log uneven sets → delete a set → finish → complete → XP once → level up → Strength + Discipline rise; re‑completion and imported duplicates don't double‑count
- `WorkoutFlowIntegrationTest` — seed catalog (idempotent) → log sets across two exercises → finish → XP once → level up → Strength + Discipline rise; re‑finish, empty, and unknown workouts award nothing

**Status motion system (pure + Robolectric):**
- `MotionSpecTest` — full motion preserves durations/stagger; reduced motion collapses them to zero
- `ProgressionEventFactoryTest` — ordered chain (XP → attributes → level → rank), change‑only emission, dense sequence
- `ProgressionEventQueueTest` — persistence, exactly‑once enqueue on `(batchId, sequence)`, drain in play order, consume removes
- `FakeStatusDataTest` — a simulated completion advances the domain **and** enqueues the full event chain; light vs. heavy vs. reset

**UI (Robolectric + Compose test):**
- `ActiveQuestScreenTest` — renders progress, quick‑add fires with the tapped amount, complete button invokes completion, deleting a set invokes delete

## Strategy

- **Pure domain logic** is tested directly (no mocks needed).
- **Idempotency** is verified at both the DAO (unique index) and repository levels.
- **External integrations** (Health Connect, cloud) will be tested against **fakes**
  implementing the same domain interfaces.
- **Instrumented tests** (androidTest) exist for on‑device verification but require
  an emulator/device; the Robolectric suite covers the same logic on the JVM in CI.

## Not yet

Room `MigrationTestHelper` tests (added when the first migration lands), and
broader UI/interaction coverage as more screens are built.
