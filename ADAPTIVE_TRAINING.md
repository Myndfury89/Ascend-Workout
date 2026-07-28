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

## Bodyweight & calisthenics (variation graph)

Bodyweight progression is **data-driven** — never inferred from an exercise's name. Each
exercise owns a directed `ExerciseVariationGraph` of `ExerciseVariation` nodes and
`ExerciseVariationProgressionEdge` transitions (seeded push-up and pull-up graphs, both
branching and both regressable). An edge carries its **real-performance gate**: minimum
successful exposures, reps, sets, optional max RPE / min RIR, assistance/ROM/tempo
requirements, and an optional `classUnlockRequirement`. `ExerciseVariationProgressionEngine`
only advances when *every* gate is met; a satisfied class unlock is one extra AND-gate,
**never** a substitute for performance (a class unlock alone can never create readiness).
Missing optional signals only ever block, never fabricate readiness. Regression is
first-class — repeated real failure surfaces a safe way back down the graph.

The single-variable calculators each move exactly one dimension:

- `RepProgressionCalculator` — raise the target range once its top is owned, hold while
  climbing, reduce (floored) after real decline; configurable rep increment.
- `SetProgressionCalculator` — adding a set needs **more** evidence than a rep (several
  consecutive strong sessions, headroom, acceptable fatigue); one easy session never adds a
  set; removes a set on decline.
- `RestProgressionCalculator` — rest is not a punishment metric: a reduction counts as
  progression only while output stays stable, heavy strength work keeps its rest, and
  declining output earns *more* rest (never framed as failure).
- `AssistanceProgressionCalculator` — lowers band/machine assistance after enough strong
  exposures; restores it (a safe regression) on decline.
- `ExternalLoadProgressionCalculator` — adds light load only once a load-capable variation
  is mastered unassisted.
- `TempoProgressionCalculator` — slower eccentric / pause / greater ROM, offered only with
  consistent controlled completion and no safety flags; never to make a movement
  arbitrarily harder.

`BodyweightProgressionCalculator` composes these into **safe candidates, one per
dimension** — it never fuses several aggressive changes into one recommendation, and tempo
work yields to a variation advance by default.

## Cardio

`CardioProgressionCalculator` and `IntervalProgressionCalculator` progress cardio from the
**minimum viable evidence** — manual duration, distance, pace, and perceived effort — with
Health-Connect / heart-rate fields optional (a recommendation is always possible without
them). One primary variable moves per step: duration **or** distance **or** pace **or** a
single intensity bump; an interval session adds a round **or** trims rest — never both
aggressively together. Repeated struggle earns a deload or a reduction, never a push.
Cardio prescriptions persist in `cardio_prescription`.

## Ranking safe options by class

When several options are safe, `ProgressionOptionRanker` orders them into one primary plus
alternatives. Ranking **never touches readiness or safety** — only already-safe changes are
ranked, so a class can never promote an unsafe option. `ClassProgressionPreferenceResolver`
supplies each class's data-driven preference (`ClassProgressionPreferenceCatalog`):
Berserker favours load → strength reps → compound sets → longer rest (and won't favour
cutting rest on heavy work); Monk favours reps → variation → reduced assistance → tempo →
ROM; Magician favours cardio duration → pace → distance → intervals. The **secondary** class
biases the order at a lower, configurable influence than the primary; **off-class options
are only pushed down, never removed** (cross-training stays available); and with **no class
the ranking is neutral** (fatigue + goal only, no class influence attributed). Each ranked
candidate carries its rank, score, reasons, and the class influence that moved it.

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

## Persistence (schema v11)

`exercise_prescription`, `training_readiness_snapshot`, `progression_recommendation`,
`progression_milestone` (unique `milestoneKey`), the bodyweight graph
`exercise_variation` / `exercise_variation_edge` (unique
`(source, destination, progressionType)`), and `cardio_prescription`. Migrations
`MIGRATION_8_9`, `MIGRATION_9_10`, and `MIGRATION_10_11`, non‑destructive, validated on the
JVM.

## Adaptive Daily-Quest interval redistribution

When a day runs off-plan, `QuestIntervalRedistributionEngine.adaptiveRecommend` decides what
to do with the leftover work, and `AdaptiveIntervalRedistributionUseCase` bridges it to the
persisted plan. **The daily total is authoritative** — if the day's target is met, a missed
interval is never treated as failure. Options include redistribute evenly / lighter-next /
heavier-final, preserve future intervals and leave the rest flexible, add a new interval,
reduce interval sizes, reduce today's total, convert to flexible completion, maintain, or ask
the user. Automatic adaptation is **opt-in** and never: exceeds the max interval target or set
size, schedules into quiet hours, adds to future intervals under high fatigue, overrides a
safety flag, or increases the daily total. Anything it can't safely place is left flexible.

## Stable UI-facing surface (no UI yet)

`AdaptiveTrainingQueries` is a single delegating entry point for future screens — pending
recommendations, the active prescription, a variation path (current + next/previous
movements), option ranking, pending interval redistribution, and the accept / reject / apply /
redistribute actions — all forwarding to the already-tested repositories and engines. No
Compose UI is built in this slice.

## Implemented vs. deferred

**Implemented:** Slice A (bench double progression → smallest load increase → accept →
apply‑once → reward‑once), Slice C (Daily Quest baseline → capped increase / maintain /
floored reduce), the readiness + safety model, recommendation lifecycle, class‑neutral
rewards, **Slice B** (the bodyweight variation graph + `ExerciseVariationProgressionEngine`
and the rep / set / rest / assistance / external‑load / tempo calculators with
single‑variable composition), **cardio** (`CardioProgressionCalculator` +
`IntervalProgressionCalculator`, working without wearables), **class‑priority ranking** of
safe options (`ProgressionOptionRanker` + `ClassProgressionPreferenceResolver`), and
**adaptive Daily‑Quest interval redistribution** (`QuestIntervalRedistributionEngine` +
`AdaptiveIntervalRedistributionUseCase`) with a stable `AdaptiveTrainingQueries` surface for
future UI. **Deferred beyond this slice:** training blocks and Health‑Connect / HR signals.
UI is deferred throughout (stable use cases exposed for future screens).

## Balancing assumptions & safety limitations

Numbers (required exposures, increments, reward XP, consistency thresholds) are tunable
config, not constants. The engine is decision‑support, not medical advice; it degrades
to conservative behavior when data is missing and never diagnoses.
