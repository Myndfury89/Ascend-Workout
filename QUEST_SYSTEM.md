# Quest System

A **quest** is a goal completed through one or more **objectives**. Each objective
tracks a target and accumulates **progress entries** (individual logged
contributions, e.g. one set of push‑ups).

## Quest types

`SINGLE_ACTION`, `ACCUMULATION`, `SCHEDULED_WORKOUT`, `QUEST_CHAIN`, `RECURRING`,
`PROGRESSIVE`, `RECOVERY`, `HEALTH_DATA`, `PERSONAL_RECORD`, `EXPEDITION`.

The implemented flow today is **accumulation** (the flagship 200‑push‑up quest).

## Accumulation quests

An accumulation objective has a total target and can be completed with any valid
combination of sets — **uneven sets are fully supported**:

```
Objective: 200 push-ups   preferred 25   min 10   max 50
Log: 25, 25, 40, 30  →  progress 120 / 200, remaining 80
```

Cumulative progress is always recomputed as the **sum of the objective's entries**
(the entries are the source of truth), so editing or deleting a set immediately and
correctly recomputes progress.

## Set‑splitting engine — `SetSuggestionEngine`

Given remaining reps and the preferred/min/max set sizes, it suggests a primary
plan plus alternatives. It **never recommends a set larger than the configured
maximum** unless the user explicitly opts into unrestricted suggestions. Every
suggested plan totals exactly the remaining amount.

```
remaining 125, preferred 25  → 25, 25, 25, 25, 25
remaining 125, preferred 40  → 40, 40, 45   (small remainder folded, no max set)
```

## Completing a quest (`QuestRepository.completeQuest`)

In a single transaction:

1. Compute quest XP from completion fraction (partial/over‑completion aware).
2. `awardXp(... sourceType=QUEST_COMPLETION, sourceId=questId)` — **exactly once**.
3. Award primary‑attribute volume points per objective + one Discipline award.
4. Mark the quest/objectives `COMPLETED` / `OVER_COMPLETED` / `PARTIALLY_COMPLETED`.

Re‑completing returns `AlreadyCompleted` and awards nothing (XP dedup guard).

## Import de‑duplication

A progress entry carries optional `sourceApplication` + `externalRecordId`. A
unique index on that pair blocks re‑importing the same external record, while
manual entries (both null) are never blocked. This is how Health Connect / file
imports (later phases) avoid double‑counting.

## Statuses

`DRAFT, SCHEDULED, ACTIVE, IN_PROGRESS, COMPLETED, OVER_COMPLETED,
PARTIALLY_COMPLETED, SKIPPED, EXPIRED, RESCHEDULED, CANCELLED`. Skipping/missing a
quest never removes historical XP (see `PROGRESSION_SYSTEM.md`).

## Safety

Suggestions respect configured maximums; over‑completion rewards are small and
capped; partial completion is honored.

## Customizable targets (`quest_template`)

Daily Quests are created from **templates** carrying configurable safe ranges — the
single source of a quest's limits, stored as seed data (never hardcoded in Compose or
calculators). Five built‑ins ship (push‑ups 25–500, pull‑ups 10–50, crunches 25–200,
squats 10–50, walking steps 6 000–10 000), with quick‑add values, set‑size limits,
supported variations, and a safety threshold. Raising a limit later is a data edit.

`QuestTargetValidator` rejects out‑of‑range targets (returning the allowed range so
the UI preserves the entry) and flags in‑range **high targets** (above the safety
threshold or a large jump over recent baseline) for explicit confirmation — difficulty
alone never blocks. `CreateQuestFromTemplateUseCase` runs that validation, then builds
a normal accumulation quest; the target can be reduced or increased after progress
(logged sets preserved, remaining recomputed) and a customized quest can be saved as a
reusable template.

## Scheduling & interval completion

A quest has two separate concepts: the **daily objective** ("200 push‑ups today") and
the **execution plan**. The objective can be completed even if the plan changes. Five
`QuestScheduleMode`s: `FLEXIBLE_DAILY_TOTAL`, `FIXED_INTERVALS`, `SUGGESTED_INTERVALS`,
`TIME_WINDOW_BLOCKS`, `MANUAL_CHECKPOINTS`.

Pure, tested engines drive it:

- `QuestIntervalDistributionCalculator` — Equal / PreferredSetSize / FrontLoaded /
  BackLoaded / Custom, always summing exactly to the target (uneven splits welcome).
- `QuestIntervalGenerator` — tiles the active window into non‑overlapping intervals.
- `QuestIntervalProgressCalculator` — interval + daily progress; daily can be met
  before every window passes.
- `QuestIntervalRedistributionEngine` — rebalances leftover from a missed/partial
  interval (even / lighter‑next / heavier‑final / preserve), respecting max set size
  (overflow returned, never dropped).
- `QuestCheckpointEvaluator` — cumulative "by this time" checkpoints (met / overdue).
- `StepIntervalProgressResolver` — assigns Health Connect step records to intervals by
  **absolute epoch timestamp** (timezone/DST‑safe) and de‑duplicates imports.
- `IntervalScheduleValidator` / `QuestReminderPolicy` — overlap/window validation and
  quiet‑hour + done/skipped/disabled reminder suppression.

The UI for these flows is a later phase; the models, persistence, and engines are in place.
