# Adaptive Training & Progressive Overload

Tracks real performance and recommends progression, maintenance, or regression that
the user accepts or rejects — then rewards it only after it's proven.

## The critical rule

**Real performance determines readiness. Game progression only unlocks options and
grants rewards.** Player Level, Class Level, and EXP may unlock advanced templates,
higher tiers, and new variations, but they **never** independently force a change in
weight, reps, sets, rest, variation, cardio duration, pace, distance, intensity, or
volume. A class only *ranks* multiple already‑safe options; it can't make an unsafe
one safe.

## The loop

```
train → record actual performance → evaluate readiness → recommend
     → user accepts / rejects → apply accepted change → verify success
     → award progression reward (once) → record for future adaptation
```

## Performance source (no duplicated data)

`PerformanceHistoryRepository` derives everything from the **existing** records:
exercise sessions from completed `workout` / `workout_set` rows; Daily Quest outcomes
from finished quests linked to a template's exercise. No parallel raw‑set store.

## Readiness (`TrainingReadinessCalculator`)

Structured and explainable — a `TrainingReadiness(state, score, confidence, evidence,
positive/limiting/missing signals, safety)`, never a lone opaque number. It works
with missing signals (confidence simply drops; RIR/RPE/wearables are never required).
Safety symptoms (pain, injury, serious symptoms) always block and win — never a
diagnosis, and it suggests stopping/seeking guidance for serious warnings.

## Progression

- **Double progression** (default resistance): keep the weight while working up the
  rep range; increase load — by the **smallest** configured increment — only once all
  working sets reach the top of the range with acceptable performance, then reset reps
  toward the bottom. One strong set, a failed rep, or high effort blocks a load
  increase; low data requires an extra exposure first.
- **Daily Quest**: `RecentBaselineCalculator` builds a rolling baseline from recent
  outcomes (median target so an outlier can't dominate, plus consistency + trend).
  `DailyQuestTargetProgressionCalculator` steps the target up/down by the template's
  preferred set size, always clamped to the template's configurable min/max. This
  powers the `recentBaseline` hook in `QuestTargetValidator`.

`ExerciseProgressionEngine` and `QuestProgressionEngine` turn readiness into a single
`ProgressionRecommendation` (with the proposed prescription or target); a change
always requires confirmation unless the user enables automatic safe adaptation.

## Lifecycle & rewards

`ProgressionRecommendationRepository` persists recommendations and prescriptions
(survives restart) and drives accept / reject / apply. An accepted recommendation is
applied **exactly once** (the proposed prescription becomes active, the old one is
superseded); **rejecting changes nothing** — no XP, level, attributes, streak, or
records are lost.

`ProgressionRewardService` grants a reward **only after** the harder prescription is
proven, idempotent on the milestone's identity (unique `milestoneKey`) so the same
milestone never awards twice. Player XP stays class‑neutral; class shaping flows
through the existing `ClassRewardApplier`, reusing the class‑XP / attribute /
unique‑proficiency ledgers, and returns an inspectable `ProgressionRewardBreakdown`.

## Persistence (schema v9)

`exercise_prescription`, `training_readiness_snapshot`, `progression_recommendation`,
`progression_milestone` (unique `milestoneKey`). Migration `MIGRATION_8_9`,
non‑destructive, validated on the JVM.

## This slice vs. deferred

**Implemented:** Slice A (bench double progression → smallest load increase → accept →
apply‑once → reward‑once) and Slice C (Daily Quest baseline → capped increase /
maintain / floored reduce), the readiness + safety model, recommendation lifecycle,
and class‑neutral rewards. **Deferred to the next slice:** the bodyweight variation
graph + `ExerciseVariationProgressionEngine` (Slice B), cardio calculators, set/rest
progression detail, class‑priority ranking of safe options, adaptive interval
redistribution wiring, training blocks, and Health‑Connect / HR signals. UI is
deferred throughout (stable use cases exposed for future screens).

## Balancing assumptions & safety limitations

Numbers (required exposures, increments, reward XP, consistency thresholds) are tunable
config, not constants. The engine is decision‑support, not medical advice; it degrades
to conservative behavior when data is missing and never diagnoses.
