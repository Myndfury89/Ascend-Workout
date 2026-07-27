# Class System

Classes layer a **build fantasy** on top of the existing progression engine without
changing how the account levels. This is the data + engine layer; the animated
Status‑menu presentation is deferred until the Status composition is signed off.

## The core rule: Player XP is class‑neutral

A class **never** changes the Player XP an activity awards, so no class is the
objectively fastest way to level the account. Class specialization affects only:

- **Class XP** — a separate ledger, per class, scaled by activity affinity.
- **Universal attribute proficiency** — the same five attributes, but the amount
  gained is reshaped by the primary class's multipliers.
- **Unique class proficiency** — Force / Body Mastery / Energy Control, earned
  heavily from favored activities.

With **no class selected**, everything is a pass‑through — identical to pre‑class
behavior (the 200‑push‑up acceptance test still passes unchanged).

## Data‑driven, seed‑configured

- **Classes** are seeded from `ClassCatalog` (Berserker, Monk, Magician — the single
  tunable source of balancing defaults) into a DB `class_definition` table, so they
  can be updated independently of player progression. The engine reads a
  `ClassDefinition`; it never branches on a class id. Further classes (Assassin,
  Fighter, Tidewalker, Ranger, Guardian) can be added as seed rows without touching
  calculation logic.
- **Affinity** is computed from **activity tags** (`BODYWEIGHT`, `HEAVY_STRENGTH`,
  `STEADY_STATE_CARDIO`, …) carried on each exercise — tag overlap, not
  class‑specific conditionals. New classes/activities need zero engine changes.
- Each class also carries **presentation metadata** (`statusThemeKey`,
  `frameVariantKey`, `accentTokenKey`, …) — variations of Ascend's own design
  system, consumed later by the Status menu.

## How a reward is computed (`ClassRewardApplier`)

For a completion worth some class‑neutral Player XP with a base attribute
distribution (e.g. push‑ups → Strength + Discipline):

1. **Affinity** = fraction of the activity's tags the class favors (0.0–1.0).
2. **Class XP** = `basePlayerXp × classXpBaseFraction × lerp(neutral 0.75 → favored 1.30 by affinity) × allocation`.
3. **Attribute proficiency** = each base attribute × the class's multiplier — it only
   reshapes the *existing* distribution and never invents an unrelated attribute
   (any cross‑attribute conversion would have to be explicit in the class seed).
4. **Unique proficiency** = base magnitude × affinity × favored multiplier × rate —
   zero for a non‑favored activity.

The **primary** class shapes attributes and earns Class XP + unique proficiency; a
**secondary** class earns Class XP + unique proficiency at a **50%** allocation but
does not re‑modify attributes (no double award). All awards are idempotent on
`(classId | proficiencyKey, transactionType, sourceType, sourceId)`, mirroring the
player XP/attribute ledgers.

### Worked example (Monk vs Berserker, same push‑up workout)

```
                     Monk (push-ups favored)   Berserker (push-ups unfavored)
Player XP            (identical)               (identical)      ← class-neutral
Strength proficiency  base 29 × 1.25 = 36       base 29 × 1.50 = 44
Discipline            base 10 × 1.35 = 14       base 10 × 1.00 = 10
Class XP              favored (×1.30)           neutral  (×0.75)
Unique proficiency    Body Mastery > 0          Force = 0
```

## Reward transparency

Every completion returns an inspectable `RewardBreakdown`: base Player XP + level,
the base vs. awarded attribute distribution, and a `ClassRewardLine` per class
(affinity, Class XP, class level, unique proficiency). Nothing about the reward math
is hidden — the reward screen (later) renders this directly.

## Components (single‑responsibility calculators)

Pure, injectable, data‑driven — none branch on a class id:

- `ActivityAffinityCalculator` → `ActivityAffinityResult` (tag overlap).
- `ClassXpCalculator` — class‑specific XP, deliberately separate from the
  class‑neutral player `XpCalculator` (never touched).
- `UniqueProficiencyCalculator` — the class‑unique meter.
- `ClassAttributeScaler` — scales the existing base distribution (zero stays zero).
- `MulticlassRewardCalculator` — composes the above into a primary + optional
  secondary reward (no persistence).
- `ClassRewardApplier` — persists the calculated reward idempotently and returns the
  `RewardBreakdown`.
- `ClassRecommendationEngine` — see below.

## Multiclass, history & switching

`player_class` holds the primary + optional secondary selection with start dates,
selection reason, change source, and **schema support** for a switch cooldown and a
respecialization‑quest gate (the full flow is a later phase). Every change appends to
`class_history` (previous selection closed, new one opened). **Switching never deletes
earned Class XP or unique proficiency** — the ledgers are immutable and keyed by class.

## Class recommendation (`ClassRecommendationEngine`)

Maps plain‑language goals + preferred training style (from the Awakening onboarding,
passed in as signals) onto a ranked, **explainable** suggestion — scored by tag +
keyword overlap against the class definitions, never per‑class conditionals. It only
recommends: it returns the top class, its score and reasons, plus ranked alternatives,
and the actual selection stays an explicit user action.

## Persistence (schema v6)

- `exercise.tags` — comma‑separated activity tags.
- `class_definition` — seeded from `ClassCatalog`, updateable without touching progression.
- `player_class` + `class_history` — selection + append‑only history.
- `class_xp_transaction`, `class_proficiency_transaction` — idempotent ledgers, keyed
  by `(subject, transactionType, sourceType, sourceId, rewardType)`.

Class level is derived from cumulative Class XP via the existing level curve.

## Not yet wired

Class **selection UX** waits on onboarding; until then a class is set via
`ClassRepository.setClasses`. The class **Status‑menu presentation** (class block,
reward breakdown screen, per‑class themes) is the next step, after the Status
composition is approved.

## Testing

`ClassProgressionCalculatorTest` (pure: affinity, XP scaling, attribute reshaping,
unique proficiency), `ClassRewardFlowTest` (Monk vs Berserker on a real workout —
class‑neutral Player XP, class‑shaped everything else, secondary allocation,
idempotency), and `AscendMigrationTest` (v3→v4 tags backfill + class tables +
exactly‑once guard). See `TESTING.md`.
