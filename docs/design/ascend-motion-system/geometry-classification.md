# Sigil geometry classification (asset-export pre-work)

Analysis of the current procedural sigil against the handoff **Asset export checklist**
(`README.md` §"Asset export checklist"), to plan the hybrid layered-vector + procedural
architecture **before** converting any geometry. No rendering changes accompany this doc.

Source under analysis: `feature/dashboard/prototype/OrnateSigil.kt`,
`OrnateSigilGeometry.kt`, `OrnateSigilModels.kt` (CP1 layering, CP2 class differentiation,
HUD internal glow).

## 1. Stable ornamental geometry (candidates for exported vector layers)
- Outer border/bezel **motif** (`geo.motif`) — *shape* is stable; only tick **density** is rank-driven (see §3).
- Structural **ring tracks** (the faint circles at r·0.72 / r·0.60 drawn by `ringTrack`).
- **Medallion outlines + glyphs** (`geo.medallionCenters`, `MedallionGlyph` draw) — positions from `medallionAngles` are stable; fill/pulse is live (see §5).
- **Class central base structure** (`classCentralPaths`): Berserker 5-point star + pentagon, Monk hexagon + hexagram, Magician rosette.
- **Per-class core anchor** (`coreAnchorPath`): spike-star / hexagon / circle.

## 2. Data-driven geometry (must stay generated, not baked)
- The **rank-detail overlay**: `RankGeometryCatalog.configFor(tier)` drives nested polygons, rotated polygons, radial connectors, segmented arcs, and motif tick count — 9 tiers.
- **Medallion count** (5 attributes, +1 proficiency when a class is chosen).
- **Radius**: all geometry is parametric on `radiusPx` (bucketed by `radiusBucket`), so it is not a fixed-viewport asset today.

## 3. Varies by rank
- `nestedPolygons`, `rotatedPolygons`, `radialConnectors`, `segmentedArcs`, `motifTicks`
  (all from `RankGeometryConfig`). Complexity rises monotonically (`complexityScore`).

## 4. Varies by class
- Central structure (`CentralStructure`), core anchor (`CoreAnchorShape`), primary stroke
  scale, signed rotation factor (speed + direction), proficiency glyph, accent
  (`ClassSigilGeometryCatalog`, `SigilClassStyleCatalog`).

## 5. Reacts to live events
- **Player XP / Class XP ring trims** (`anim.playerRingTrim` / `classRingTrim`) — the
  strokeDashoffset-equivalent; bound to real fractions.
- **Medallion activation** (`anim.medallionPulse`, `proficiencyPulse`) — scale + color.
- **Internal glow** (`anim.glow` → `glowE`, HUD pass) — event-reactive, capped.
- **Assembly** (`anim.assembly`), **rotation** (`anim.rotation`), **opacity**
  (`settledOpacity` + assembly bump), **attribute center-out wave** (CP3, procedural).

## 6. Proposed asset / path grouping (maps to the checklist)
| Checklist layer | Contents | Realization |
|---|---|---|
| **Outer instrumentation** | bezel + tick-dot ring + 8-mark glyph ring + 4 attribute + 4 perimeter + proficiency medallion **outlines** | cached Compose `Path` layer (static), medallions isolated for color swap |
| **Player/Class XP rings** | r≈130 / r≈100 stroke paths | **live** cached Compose `Path` + animated sweep (never flattened) |
| **Middle ceremonial group (per class)** | 12-mark support ring + class central structure | cached Compose `Path` group, single shared origin, rotated as one unit |
| **Static core anchor (per class)** | spike-star / hexagon / circle | cached Compose `Path`, separate layer, scales in independently |
| **Rank-detail overlay** | nested/rotated polygons, connectors, arcs | **procedural**, composed on top (data-driven) |
| **Level-up breakthrough burst** | 12 spokes → ~165px | **pre-authored** AVD / ImageVector sequence (Ascension) |
| **Attribute center-out wave** | single scaling circle | **procedural** (per perf table) |

## 7. Can Android VectorDrawable preserve grouping + pivots?
Partly. `<group android:pivotX/pivotY>` + AVD can animate group rotation, and AVD
`trimPathStart/End/Offset` can drive ring trims. **But** our geometry is parametric on
radius and the rank overlay is data-driven, so a fixed-viewport XML asset can't represent
the full sigil. **Recommendation:** realize the checklist's "vector layers" as **cached
Compose `Path` layer objects** (grouped by the taxonomy above, all pinned to a shared
`(150,150)`-normalized origin) rather than drawable XML — this preserves the geometry, the
stationary/rotating split, and live behavior while keeping rank/class parametric. Reserve
**true VectorDrawable/AVD only for the pre-authored level-up burst** (Ascension, no live
parametric variation).

## 8. Geometry that should remain cached Compose Path (not XML)
XP/Class rings (live trim), rank-detail overlay (data-driven), medallion state (live
color/pulse), the attribute wave (procedural). These are explicitly *not* export targets.

## 9. Expected test changes
- New: middle group rotates as **one unit**; core anchor is a **separate** layer (not in the
  middle group); XP/Class ring paths remain **independent live** paths; all layers share the
  `(150,150)` origin. Extend `SigilLayeringTest` / `ClassSigilGeometryTest`.
- Preserve: rank complexity still varies **independently of class** (existing
  `ClassSigilGeometryTest` + `OrnateSigilTest` monotonic-complexity checks).

## 10. Expected performance benefit
- Geometry is already cached by `SigilGeometryKey` + radius bucket; formalizing layer groups
  lets the rotating middle animate **one transform** instead of re-rotating sublayers, and the
  pre-authored burst avoids per-frame recompute of the Ascension bloom. Net: fewer `Path`
  rebuilds and one rotation node — a modest win on top of existing caching, with the main gain
  being **clarity/addressability** (isolated medallions, live rings, per-class groups).

## Recommendation
Proceed with the hybrid per the architecture decision: **cached Compose `Path` layer sets**
(outer instrumentation, per-class middle group, per-class anchor) + **live** rings/medallions +
**procedural** rank overlay & attribute wave + **pre-authored AVD** level-up burst. Do **not**
bake per-rank/class/state combinations. Checkpoints B (instrumentation + medallions), C (middle
groups + anchors), D (pre-authored burst + profiling) follow.
