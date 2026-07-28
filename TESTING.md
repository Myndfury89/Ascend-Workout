# Testing

Tests run on the **JVM** — fast, no emulator required. Pure logic uses plain
JUnit; database, repository, and Compose UI tests use **Robolectric**.

Run everything:

```bash
./gradlew testDebugUnitTest
```

## What's covered today (120 tests, all passing)

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
- `AscendMigrationTest` — v1→v2, v2→v3, and v3→v4 migrations validated against the exported schema; data survives, tags are backfilled, and the new tables (incl. the exactly‑once guards) are usable

**Repositories (Robolectric integration):**
- `ProgressionRepositoryTest` — award once → level up, reverse → recompute, attributes once, multi‑level award
- `QuestFlowIntegrationTest` — **the acceptance‑criteria proof**: create 200‑push‑up quest → log uneven sets → delete a set → finish → complete → XP once → level up → Strength + Discipline rise; re‑completion and imported duplicates don't double‑count
- `WorkoutFlowIntegrationTest` — seed catalog (idempotent) → log sets across two exercises → finish → XP once → level up → Strength + Discipline rise; re‑finish, empty, and unknown workouts award nothing

**Class system (pure + Robolectric):**
- `ClassProgressionCalculatorTest` — tag affinity, class‑XP interpolation, attribute reshaping (no invented attributes, zero stays zero), unique proficiency (favored vs zero)
- `ClassRecommendationEngineTest` — plain‑language goals → explainable, non‑locking recommendation (Berserker / Monk / Magician)
- `ClassRepositoryTest` — DB seed loads + round‑trips, class switch records history and **preserves earned Class XP**, class progress derivation
- `ClassRewardFlowTest` — Monk vs Berserker vs Magician on real workouts: **class‑neutral Player XP**, class‑shaped attributes/Class XP/unique proficiency, secondary 50% allocation, non‑favored nonzero Class XP, idempotency, and event enqueue

**Daily Quests — customizable targets + interval scheduling (pure + Robolectric):**
- `QuestTargetValidatorTest` / `CreateQuestFromTemplateFlowTest` — range validation, high‑target confirmation, create‑from‑template, recurring, save‑custom, reduce/increase target after progress, seed loads
- `IntervalGenerationTest` — distribution strategies (equal/preferred/front/back), suggested tiling, fixed custom times, overlap/window/unassigned validation
- `IntervalProgressTest` — interval status, daily‑met‑before‑expiry, redistribution (even/heavier‑final/lighter‑next/preserve/max‑cap), reminder suppression
- `CheckpointAndStepTest` — cumulative checkpoints (met/overdue), step→interval resolution, duplicate‑import guard, epoch/timezone/DST‑safe boundaries
- `QuestIntervalDaoTest` / `AscendMigrationTest` — multi‑set interval sums, import dedup, v6→v7 and v7→v8 migrations

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
