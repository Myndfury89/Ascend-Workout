# Status Motion System

> **Authoritative spec:** the detailed ceremonial-sigil + motion design — timing
> tokens, easing curves, sigil layer structure, event choreography, reduced-motion
> rules, and the **asset-export checklist** — lives in the handoff at
> [`docs/design/ascend-motion-system/README.md`](docs/design/ascend-motion-system/README.md)
> (review prototype: `docs/design/ascend-motion-system/Ascend Motion System.dc.html`,
> reference only — it needs its bundled `support.js` to render). **That handoff is the
> source of truth for the sigil/motion spec; this file documents how the app implements
> it and must not restate or contradict those values.** Sigil geometry classification for
> the asset-export work: [`docs/design/ascend-motion-system/geometry-classification.md`](docs/design/ascend-motion-system/geometry-classification.md).

The Status screen's game‑feel layer. It sits **on top of** the existing (already
tested) progression engine and quest flow — it adds motion, not new progression
rules. The current build ships an **interactive prototype on fake data**; the
pipeline underneath (the persisted ProgressionEventQueue) is real, so wiring it to
production earnings later is a small, contained change.

Original by design: the timing, easing, colour and sequence language here is
Ascend's own — no franchise's visual/sound/timing signature.

## Three separated state layers

The architecture deliberately keeps three concerns apart so none can corrupt the
others:

| Layer | What it is | Where it lives |
|---|---|---|
| **Domain state** | The authoritative facts — level, rank, lifetime XP, attributes | `StatusDataSource` → `StatusDomain` (prototype: `FakeStatusData`) |
| **Presentation‑event state** | The persisted, ordered script of *what changed*, decoupled from the transaction that earned it | `ProgressionEventQueue` (`progression_event` table + `ProgressionEventRepository`) |
| **Animation state** | The transient interpolation — Animatables for the bar, rows, pulses, flashes | `StatusMotion` (composable‑side; the only place that touches durations/easing) |

The `StatusMotionViewModel` wires them together: it collects domain state, drains
the event queue one batch at a time, and never itself touches a millisecond.

## Motion tokens (`core/designsystem/motion`)

- **Durations** — `INSTANT, QUICK (130), STANDARD (260), DELIBERATE (440), DRAMATIC (760)`,
  plus `STAGGER (70)` and `HOLD (180)`. Named by intent, not number.
- **Easing** — original curves: `settle` (hard decelerate), `emphasize` (gentle
  overshoot), `surge` (accelerate out), `pulse` (symmetric swell).
- **Reduced motion** — `MotionSpec(reducedMotion = true)` collapses every derived
  duration and stagger to `0` in one place, so the exact same code path runs as an
  instant cut. No `if (reducedMotion)` scattered at call sites.

## The ProgressionEventQueue (real, persisted)

`progression_event` records each atomic change (XP_GAINED, ATTRIBUTE_CHANGED,
LEVEL_UP, RANK_UP) grouped into a **batch** (all events from one earning) and ordered
by **sequence**. Key properties:

- **Exactly‑once** — a unique index on `(batchId, sequence)` means re‑running the
  same earning transaction can't duplicate the animation script.
- **Decoupled** — events are written the instant a reward is earned and consumed
  later by whatever is on screen; a reward can be closed mid‑animation and **replays
  exactly once** on next open.
- **Deterministic** — `ProgressionEventFactory` builds the batch purely from a
  before/after `ProgressionSnapshot`, in a stable order (XP → attributes → level →
  rank), emitting only actual changes.

## The Status screen

1. **Entrance** — staged: header fades in, the XP bar fills `0 → current`, then the
   five attribute rows reveal and count up `0 → current`, staggered.
2. **XP bar** — driven off a single animated *lifetime‑XP* value swept through the
   level curve, so a level‑up (fill → snap → continue) falls out of the math; the
   `LEVEL_UP` event only fires the celebratory flash.
3. **Event chain** — a batch plays as: XP gain → each changed attribute (value tween
   + pulse) → level‑up beat → rank‑up beat, then the batch is marked consumed.

### Prototype controls

The prototype hosts on‑screen controls: **Trial of Focus** (light gain — XP +
attributes, no level‑up), **Ascension Trial** (heavy gain — crosses a level *and* a
rank), **Replay entrance**, **Reset**, and a **Reduced motion** switch. The baseline
hunter starts ~500 XP below the level‑6 threshold so the two simulations demonstrate
the in‑level and cross‑level cases distinctly.

## Wiring to production (next step, after review)

The prototype's `simulate()` already exercises the exact production path: build
events from before/after snapshots and enqueue them. To go live:

1. In `completeQuest` / `completeWorkout`, capture before/after `ProgressionSnapshot`s
   and, **inside the same transaction**, enqueue `ProgressionEventFactory.build(...)`.
2. Bind `StatusDataSource` to a real `ProgressionRepository`‑backed source and drop
   `FakeStatusData` / the simulator.

The ViewModel and the whole animation layer stay unchanged.

## Testing

All on the JVM (Robolectric, no emulator):

- `ProgressionEventFactoryTest` — ordering, change‑only emission, level/rank beats.
- `ProgressionEventQueueTest` — persistence, exactly‑once enqueue, drain + consume order.
- `FakeStatusDataTest` — a simulate advances the domain **and** enqueues the full chain.
- `MotionSpecTest` — reduced motion collapses all durations/stagger to zero.
- `AscendMigrationTest` — the v2→v3 migration and the unique `(batchId, sequence)` guard.
