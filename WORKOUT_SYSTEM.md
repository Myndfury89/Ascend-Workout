# Workout System

A **workout** is a logged training session made of **sets**, each performed against
an **exercise** from the catalog. Workouts are distinct from quests (an accumulation
goal) but feed the **same** XP / attribute ledger when finished.

## Exercise catalog (`exercise`)

A seeded, offline library of movements. Each entry declares:

- `primaryAttribute` — the attribute the movement trains (e.g. push‑ups → `STRENGTH`,
  plank → `DISCIPLINE`, running → `ENDURANCE`).
- `measurementType` — `REPETITIONS`, `DURATION`, `DISTANCE`, `WEIGHT_AND_REPS`, …
- `defaultUnit` — `reps`, `seconds`, `metres`.
- `isWeighted` — whether the movement is normally loaded with external weight.

`SeedExerciseCatalogUseCase` seeds the built‑in list with **stable ids**
(`ex‑pushup`, `ex‑plank`, …) and is idempotent — it only seeds when the catalog is
empty, so it never duplicates rows or clobbers later edits.

## Logging a session

`WorkoutRepository.createWorkout` opens a session; `addSet` appends a set. Each set
stores its raw measurement (`reps` / `weight` / `durationSeconds` / `distance`) plus
a denormalised **`volume`** (the training value in the set's unit) used to grow the
exercise's primary attribute. The UI creates the backing workout lazily on the first
set, so an abandoned draft never persists an empty session.

```
Session "Upper Body" (MODERATE)
  Push-ups   100 reps
  Push-ups    90 reps      →  Strength volume 190
  Plank       60 seconds   →  Discipline volume 60
```

## Finishing a workout (`WorkoutRepository.completeWorkout`)

In a single transaction:

1. Compute workout XP via `XpCalculator.workoutXp` — a base plus duration,
   difficulty‑derived **intensity**, and a normalised **volume score**.
2. `awardXp(... sourceType=WORKOUT_COMPLETION, sourceId=workoutId)` — **exactly once**.
3. Award primary‑attribute **volume points** per attribute (summed across the sets
   that train it) plus one flat **Discipline** adherence bonus.
4. Mark the workout `COMPLETED`.

Re‑finishing returns `AlreadyCompleted` and awards nothing (the XP dedup guard on
`(transactionType, sourceType, sourceId)`). Finishing a session with no sets returns
`Empty` and awards nothing.

### Worked example (defaults, MODERATE)

```
190 reps push-ups + 60s plank  →  total volume 250
XP   = base 100 + duration 0 + intensity 30 + volume 100          = 230  (level up)
STR  = round(190 * 0.15)                                          = 29
DISC = round(60 * 0.15) + flat adherence 10                       = 19
```

## Statuses

`IN_PROGRESS`, `COMPLETED`, `ABANDONED`.

## Testing

`WorkoutFlowIntegrationTest` proves the full flow end‑to‑end (seed → log across two
exercises → finish → XP once → level up → correct attributes → idempotent re‑finish),
`WorkoutDaoTest` covers the relation join and set ordering, and `AscendMigrationTest`
validates the v1→v2 migration — all on the JVM via Robolectric, no emulator. See
`TESTING.md`.
