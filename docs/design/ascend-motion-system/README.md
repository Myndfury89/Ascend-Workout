# Handoff: Ascend Motion System (v1)

## Overview
Ascend is a portrait-first fitness RPG. This package documents the v1 motion language for its "ceremonial sigil" system — the animated seal that represents player identity, progress, and class, plus the four core motion verbs (Assemble / Charge / Lock / Ascend) that drive every progression event in the app.

## About the design file
`Ascend Motion System.dc.html` is an **interactive HTML/React prototype built as a motion-review tool**, not production code to copy directly. It renders a portrait Android-frame mock of the "Status" screen and lets a reviewer trigger each event (attribute increase, level-up, skill unlock, sigil assembly, a chained multi-event sequence) at different speeds, with reduced-motion and per-class variants, plus timing/audio-hook overlays and inline Jetpack Compose notes per effect.

**Task: recreate this motion system natively in Jetpack Compose** (the target stack per the original brief), using the timing tokens, layer structure, and event choreography documented below — not by embedding or porting the HTML/JS. Where the codebase already has animation utilities or a motion/design-token system, use those; this doc defines the values and behavior they should encode.

**Note on preview status:** the live HTML prototype could not be screenshotted during this session due to an environment-side preview hang (confirmed independent of this file's code — even a trivial one-line test page hung identically). The specification below is complete and implemented in the file's source; a developer can read the source directly (it's plain templated markup + one JS class, `renderVals()`) if visual confirmation via screenshot isn't available yet either.

## Fidelity
**Medium-high fidelity for motion/timing/geometry, low fidelity for final art.** Colors, typography, and the sigil's exact linework are original placeholders built to convey direction (dark, cool-toned, thin glowing geometric lines) — timing values, easing curves, layer/rotation rules, and event choreography are the load-bearing spec and should be implemented precisely. Final sigil artwork (per class) should ideally be authored by a graphic/motion artist as vector assets per the Performance section below, using this prototype's structure as the layout reference.

## Core motion language
Four verbs, each with a validated default token. Every animation in the app should clearly read as one of these:

| Verb | Duration | Easing | Meaning |
|---|---|---|---|
| **Assemble** | 850ms | `cubic-bezier(0.16,1,0.3,1)` (expo decelerate) | Geometry/panels/sigils form from fragments or traced lines |
| **Charge** | 1100ms | `cubic-bezier(0.65,0,0.35,1)` (ease in-out) | Energy gathers before an important event. **Least validated token** — re-check feel at real particle counts / on-device frame rate |
| **Lock** | 380ms | `cubic-bezier(0.34,1.56,0.64,1)` (snap overshoot) | Values/results snap into a final precise state |
| **Ascend** | 2200ms | `cubic-bezier(0.22,1,0.36,1)` (slow decelerate) | Rank/identity elements permanently gain complexity |
| **Reduced motion (all verbs)** | 140ms | linear cross-fade only | No scale, rotation, or procedural assembly |

**Speed multipliers** (used for review, not necessarily exposed to end users): Fast ×0.55, Standard ×1, Cinematic ×1.6.

### Tiers
- **Everyday** (200–700ms): status open, nav, forms, quest progress. Lock is the reference implementation.
- **Reward** (700–1500ms): quest complete, PR, attribute up, skill XP. Charge is the reference. Reduced motion must still emit a brief non-kinetic cue (flat color pulse) — a silent PR reads as a bug.
- **Ascension** (1500–3000ms, rare): class pick, skill unlock, level milestone, rank promotion. Assemble + Ascend are the reference implementations.

### Event priority
1. Ascension interrupts Reward and Everyday.
2. Reward interrupts Everyday, never Ascension.
3. Concurrent Reward events queue and settle sequentially into the sigil.
4. Everyday motion never blocks input; Ascension may hold input briefly for readability.

## The sigil: layer structure (critical rule)
The sigil is a persistent background element behind the Status screen's readable content. **Only the middle ceremonial layer may rotate — everything that shows tracked data must stay stationary.**

**Stationary outer layer** (never rotates):
- Player XP progress ring, Class XP progress ring
- Decorative tick-dot rings and the outer border/bezel motif
- The 8-mark glyph ring, and the 4 functional attribute medallions (STR/END/CTL/LVL) + the class-proficiency medallion
- All text/labels

**Rotating middle ceremonial layer** (slow, restrained, ambient — never fast, never like a loading spinner):
- The inner support-glyph ring (12 marks, between the class ring and the central star)
- The central interlocking star/polygon structure, at ~150–260s per revolution (varies by class, see below), reversed direction from the layer just outside it for a subtle parallax feel

**Static core anchor** (center, never rotates — only scales in via Assemble/Lock on events):
- A small non-square emblem per class (see Class differentiation) that anchors the eye while the ring around it drifts

Reduced motion: remove all continuous rotation; sigil becomes fully static except for short fades/highlights.

### Opacity rule
The sigil is atmospheric, not a foreground emblem:
- **Idle/at-rest:** ~0.24 opacity — quiet background presence, text stays dominant
- **During an active progression event:** rises to ~0.62 opacity
- **Hidden (opacity 0)** before any event has ever fired (true idle/idle phase)
- Always settles back down to the low resting opacity afterward

### Line/glow styling
Thin white/cool-toned linework (this prototype uses an oklch blue-violet accent, ~74% lightness / 0.15 chroma, hue 250 — treat as "cool white-blue" glow bias), soft restrained glow only on primary geometry (the center star, the active medallion), no heavy bloom that obscures linework. Reserve real bloom/glow effects for Ascension-tier moments only; Everyday/Reward tiers use flat color shifts + scale, not glow.

## Class differentiation (shape language, not just color)
**Avoid square/boxy geometry everywhere in the ceremonial system** — no square popups, panels, or central motifs for celebratory moments. Use radial/polygonal/star/arc forms instead.

Three classes are production-ready in this prototype; the rest are future-direction notes.

| Class | Central rotating structure | Core anchor (static) | Stroke weight | Rotation feel |
|---|---|---|---|---|
| **Berserker** | Single bold 5-point star polygon (outer r≈88, inner r≈30) | Sharp 4-point spike star (not a diamond/square) | Heaviest (~2.4–2.6px) | Slowest, heaviest drift (~260s/rev) |
| **Monk** | Balanced two-triangle hexagram + one thin third triangle | Regular hexagon | Even (~1.3px core lines) | Moderate, measured (~210s/rev) |
| **Magician** | Four overlapping flowing circles (a soft rosette) | Circle | Thinnest (~1.1–1.3px) | Fastest, lightest, reversed direction (~170s/rev) |

Future-direction only (not built): **Assassin** — lean asymmetric blade-facets, quick sharp micro-pulses. **Fighter** — compact mixed polygon/ring core, rhythmic alternating pulses. **Ranger** — open long arc sweeps, sustained outward-pathing drift. **Guardian** — layered concentric shield-rings, slow reinforcing pulses.

## Event choreography

### System entrance (Everyday tier, short)
Background near-black → faint radial glow wakes → brief horizontal light sweep → panel edge/corner brackets assemble → energy rails (top/bottom hairlines) extend and brighten → sigil rings trace in → text resolves sharply → sigil settles to its low idle opacity. A cinematic (longer, Ascension-tier) version is reserved for first launch or major events only — the everyday version must stay short.

### Sigil assembly (Ascension tier)
Player XP ring traces → Class XP ring traces → central rank geometry assembles (with class personality: Berserker staggers fast/hard, Monk is symmetric/even, Magician trails in slowly and fluidly) → 4 attribute medallions + 4 decorative perimeter medallions appear staggered → class-proficiency medallion locks (Lock, 380ms snap) → whole seal settles to ambient opacity.

### Attribute increase (Reward tier) — center-out wave
1. Old value exits subtly (~150ms fade/slide, not a core verb — just a quick prep beat)
2. New value **locks** into place (Lock, 380ms snap overshoot) in the stat chip
3. The matching medallion brightens (flat color, no glow — Reward tier)
4. **A wave of energy radiates from the sigil's center outward toward the ring structure** — a thin circle scaling from the center, fading as it expands (not a bright flash/explosion — restrained and elegant)
5. Sigil settles back to idle opacity

Wave personality per attribute (this is the "attribute variant" mentioned below):
- **Strength** = firmer/faster wave (~420ms, snap-overshoot easing)
- **Endurance** = longer sustained wave (~950ms, ease-in-out)
- **Control/Discipline** = precise clean wave (~520ms, expo-decelerate easing)

Progress rings stay stationary throughout.

### Level-up (Ascension tier)
1. **Charge**: XP ring approaches completion, ambient particles pull inward toward center, motion tightens (Charge, 1100ms)
2. **Breakthrough**: XP ring completes; central geometry brightens; a **12-line radial burst** (not a square/box) extends outward from the center (~22px to ~165px) as a ceremonial "geometric bloom"; large numeral + "Threshold Surpassed" label appear centered (Ascend, 2200ms, slow decelerate); a thin shockwave ring also expands once across the panel; top/bottom energy rails brighten
3. **Settle/Lock**: new level value locks into its stat chip (Lock, 380ms); burst and glow recede; sigil returns to low idle opacity; progress rings remain fixed and readable throughout

### Skill unlock (Reward tier, 4 variants)
A skill card overlay reveals over the sigil zone, with a per-skill reveal personality, then the name/icon locks (Lock, 380ms):
- **Perception** — scan/reveal (clip/opacity reveal), Assemble timing
- **Strength Boost** — compress then snap (scale down then Lock overshoot)
- **Breath Control** — two expand/contract half-cycles, Charge timing (1100ms)
- **Body Awareness** — 4 connection points align (fade/scale in)

### Chained test sequence (the key v1 validation case)
Attribute increase (exit → lock) → **Charge** (energy gathers, particles converge, screen dims slightly) → **Breakthrough** (level expands, "Threshold Surpassed") → **Lock** (final value snaps) → **Settle**. Built to be replayed back-to-back repeatedly to calibrate Charge/Ascend feel once real device frame rates are available — Charge (1100ms) is explicitly the least-validated token in the system.

## Reduced motion (accessibility — required, not optional)
- All four verbs collapse to a single **140ms linear cross-fade**
- No ambient rotation, no continuous fog/particle movement, no large scale changes, no procedural fragment assembly (replace with short fades)
- Final progression values render immediately
- **Reward-tier events must still emit a brief non-kinetic cue** (this prototype uses a ~320ms flat color flash across the panel) — a silent personal record must never read as a bug
- All information hierarchy is preserved; decorative elements are not separate screen-reader nodes

## Performance posture (for Compose implementation)
- **Everyday / Reward tiers**: real-time procedural motion is fine — alpha, translation, scale, path-trim/sweep-angle are cheap per frame
- **Ascension tier**: treat glow/bloom and the breakthrough "geometric bloom" burst as **pre-authored assets** (AnimatedVectorDrawable or a composited sprite/vector sequence), not recomputed every frame
- One glow source per screen (the sigil center); secondary elements (medallions, chips) react via color + scale only, never their own independent glow
- Cache SVG/Path geometry for rings and the center star rather than rebuilding paths every frame
- The particle-convergence effect (level-up "particles pull inward") is flagged in the prototype as **requires real-device profiling** — it's the highest perf-risk effect in the system

### Suggested Compose primitive mapping
| Effect | Primitive | Classification |
|---|---|---|
| XP ring trace/completion | `Canvas` `drawArc` with animated sweep angle, cached `Path` | Cached Canvas path |
| Center star / core anchor assembly | `AnimatedVectorDrawable` (per-class authored) or Compose `Path` morph | AnimatedVectorDrawable candidate |
| Medallion stagger-in | `AnimatedVisibility` per item, staggered `delay` | Safe procedural Compose |
| Value lock (snap overshoot) | `tween(easing = CubicBezier(0.34,1.56,0.64,1))` or `spring(dampingRatio = MediumBouncy)` | Safe procedural Compose |
| Attribute center-out wave | `Canvas`, single circle scaled along a cached path | Cached Canvas path |
| Level-up radial burst + breakthrough glow | Pre-authored vector sequence (AVD) | Pre-authored vector sequence candidate |
| Particle convergence (level-up) | `Canvas`, N cached dot positions lerped toward center | **Requires real-device profiling** |
| Ambient ring/star rotation (idle) | Compose `rememberInfiniteTransition` on a `Path`/vector layer group, very long duration (150–260s) | Safe procedural Compose, but keep duration long — never a fast spinner |

## Design tokens used in the prototype
- **Fonts:** Space Grotesk (UI/headers), JetBrains Mono (numeric readouts/data/telemetry labels) — both Google Fonts
- **Background:** oklch(0.13 0.014 258) near-black blue-black
- **Ink (text):** oklch(0.96 0.01 258), dim oklch(0.66 0.02 258), faint oklch(0.46 0.02 258)
- **Primary/system accent:** oklch(0.74 0.15 250) — cool blue-violet
- **Class accent hues** (same lightness/chroma, hue varies): Berserker 25 (warm red-orange), Monk 165 (green-cyan), Magician 285 (violet)
- **Hairlines:** oklch(0.34 0.02 258) / oklch(0.26 0.02 258)

These are placeholder brand values — swap for the app's real palette, keeping the "same L/C, hue varies per class" relationship if that system is adopted.

## Asset export checklist (to keep this prototype's ornate sigil geometry, driven by real Compose animation)

Export the sigil as layered vector assets that preserve the stationary/rotating split above, then animate them with the tokens in this doc instead of re-authoring the motion by hand.

1. **Outer instrumentation layer (export as one static SVG per class, or shared + per-class overlay):**
   - Outer bezel circle + tick-dot ring (28 dots) — static
   - Player XP ring (r=130) and Class XP ring (r=100) as separate stroke paths — these need `strokeDashoffset` driven live, so export as bare `Path` data (not flattened), one path each
   - 8-mark glyph ring + 4 attribute medallions + 4 perimeter decorative medallions — static, but each medallion needs an isolated layer/group so its fill color can be swapped on Reward-tier events
   - Class-proficiency medallion — isolated layer, animates in on Lock

2. **Middle ceremonial layer (export as one rotating group per class):**
   - The 12-mark inner support-glyph ring
   - The class-specific central structure: Berserker's 5-point star, Monk's hexagram (two triangles + thin third), Magician's four overlapping circles
   - Export this whole group as a single `Path`/vector-group with its center pinned to the same origin as the outer layer, so `rememberInfiniteTransition` can rotate it as one unit at 150–260s/rev without re-parenting

3. **Static core anchor (export as one small vector per class):**
   - Berserker: 4-point spike star
   - Monk: hexagon
   - Magician: circle
   - Keep this as its own layer since it scales in independently on Assemble/Lock and gets a `drop-shadow` glow on Ascension events — never bake it into the middle layer

4. **Event-only vector assets (pre-authored, not procedural):**
   - Level-up radial burst (12 spokes, center to ~165px) — export as a burst sprite/AVD since it's Ascension-tier
   - Attribute center-out wave — this one is simple enough to stay procedural (single circle, cached path, animated scale/opacity) per the perf table

5. **Naming/anchoring convention:** keep every exported path centered on the same (150,150) origin used in the prototype so layer registration lines up exactly when composed in Compose — the artist doesn't need to hit final render size, just consistent centering across all layers and all three classes.

## Assets
No image/icon assets — everything in the prototype is vector (SVG circles/polygons/lines) or CSS. No copyrighted or franchise symbols are used anywhere (no runes, occult alphabets, or borrowed marks) — all glyphs are original simple geometric shapes (circles, diamonds, stars, hexagons).

## Files in this bundle
- `Ascend Motion System.dc.html` — the full interactive prototype (single self-contained file, open directly in a browser). Contains: the control console (demo/tier/speed/reduced-motion/class/skill/attribute selectors, replay + pause, timing-label and audio/haptic-hook overlays), the portrait Android-frame + frameless canvas views, and an expandable Jetpack Compose implementation-notes panel per demo (more granular than the table above — read the `composeNotesFor()` method in the file's script for the full per-effect breakdown).
