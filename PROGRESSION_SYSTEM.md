# Progression System

All progression math lives in `core/domain/progression` as **pure, configurable**
classes (no Android, no persistence). Persistence and idempotency live in the
Room‑backed `ProgressionRepository`.

## Level curve — `LevelCalculator`

XP to advance **from** a level to the next:

```
xpToReachNextLevel(level) = round(base * level^exponent)   // default base=200, exponent=1.35
```

- Level 1→2 = 200, 2→3 = 510, …  A player starts at level 1 with 0 lifetime XP.
- `resolve(lifetimeXp)` returns the current level, XP into the level, and XP to the
  next level. Level is always derived from the immutable lifetime XP total, so it
  can never silently drift.

## XP sources — `XpCalculator`

Workout XP (initial formula, all weights configurable via `XpConfig`):

```
100 base + up to 90 (1/min) + 10–50 intensity + 0–100 volume
+ 25 consistency (if eligible) + 50 personal record, × expedition multiplier
```

Quest XP: full `baseReward` on completion; proportional when partial rewards are
enabled; a **small, capped** over‑completion bonus (default: at most 50% of the
target counts, at 0.2×) so excessive volume is never strongly incentivized.

## Ranks — `RankCalculator`

Original ladder: **Initiate → Iron → Bronze → Silver → Gold → Vanguard →
Ascendant → Apex → Mythic**. Rank is a configurable blend of an *ascension score*:

```
score = level + lifetimeXp / xpPerScorePoint + consistencyDays * consistencyWeight
```

Rank **never depends on weight lifted** — different goals and abilities are treated
fairly. Rank never decreases as level increases.

## Attributes — `AttributeProgressCalculator`

Five core attributes: **Strength, Endurance, Agility, Discipline, Recovery**.
Completing a planned quest grants Discipline (adherence); the exercise's primary
attribute grows with accumulated volume (capped). Difficulty scales both.

## Streak — `StreakCalculator`

Consecutive‑day activity increments the streak; a missed day resets the *active*
streak to 1 but **never removes historical XP or the longest‑streak record**.
Missing training is not treated as failure.

## Idempotency (award XP exactly once)

The immutable `XpTransaction` ledger is the single source of truth. Two guarantees:

1. **Database:** a unique index on `(transactionType, sourceType, sourceId)` makes a
   second `AWARD` for the same source impossible, while still allowing a matching
   `REVERSAL`.
2. **Repository:** `awardXp` inserts with `IGNORE`; a blocked insert returns a
   `Duplicate` result instead of double‑counting. Level and rank are recomputed
   from the ledger inside the same transaction.

Editing or deleting a rewarded record uses `reverseXp` (also idempotent) rather
than issuing new duplicate XP. Attribute awards mirror the same guard, keyed
additionally by attribute so one source grants each attribute exactly once.
