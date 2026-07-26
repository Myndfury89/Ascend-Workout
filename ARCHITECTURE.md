# Architecture

Ascend follows **Clean Architecture + MVVM**, offline‑first, with unidirectional
data flow. Domain logic is independent of Android framework code, external
services sit behind interfaces, and UI state is immutable.

## Layers

```
┌───────────────────────────────────────────────┐
│ Presentation   Compose screens + ViewModels    │  UiState (immutable) ↑  events ↓
│                (feature/*)                      │
├───────────────────────────────────────────────┤
│ Domain         models, repository INTERFACES,   │  no Android imports
│                pure calculators, use cases      │
│                (core/model, core/domain)        │
├───────────────────────────────────────────────┤
│ Data           Room impls of the interfaces,    │  implements domain interfaces
│                mappers, DI                       │
│                (core/data, core/database)        │
└───────────────────────────────────────────────┘
```

**Flow:** Composable emits an event → `ViewModel` calls a repository/use case →
repository talks to Room → results flow back as a single immutable `UiState` via
`StateFlow`, collected with `collectAsStateWithLifecycle`. No business logic lives
in composables.

## Module strategy (single module, package boundaries)

The spec's target is a fully modular Gradle build (`core:*`, `feature:*`). To keep
early‑phase iteration fast, we start as a **single `:app` module with strict
package boundaries that mirror the intended modules 1:1**:

```
com.ascend
  core/{common, model, domain, data, database, designsystem}
  feature/{dashboard, quests, workouts, calendar, progress, ...}
  navigation
```

**Migration plan:** when a boundary stabilizes and cross‑package deps are clean,
extract it to a Gradle module (`:core:model` first — zero Android deps — then
`:core:domain`, `:core:database`, `:core:designsystem`, then `:feature:*`). The
package layout already enforces the dependency direction, so extraction is
mechanical (move sources + add `build.gradle.kts` + `api`/`implementation` edges).

## Dependency injection (Hilt)

- `@HiltAndroidApp` on `AscendApplication`, `@AndroidEntryPoint` on `MainActivity`,
  `@HiltViewModel` for ViewModels.
- `DatabaseModule` provides the Room database + DAOs.
- `ProgressionModule` provides the pure calculators as singletons.
- `DataModule` `@Binds` repository interfaces to their Room implementations.

## Key design rules (enforced)

1. Domain is Android‑free; data implements domain interfaces.
2. UI state is immutable; unidirectional data flow.
3. Progress is persisted locally; offline‑first.
4. Synchronization/reward logic is **idempotent** (see `PROGRESSION_SYSTEM.md`).
5. Calculations are configurable and centralized (no scattered formulas).
6. User‑facing strings live in resources where practical.
7. Dark mode + accessibility are first‑class.

## Cross‑platform intent

Domain models and repository interfaces contain no Android types, so an iOS client
could reuse the same domain concepts and a shared backend behind the same
interfaces (auth/sync/analytics are already abstracted for Phase 11).

## Toolchain

Kotlin 2.0.21 · AGP 8.9.1 · Gradle 8.11.1 · Compose BOM 2024.12.01 · Hilt 2.52 ·
Room 2.6.1 · Java 17 · minSdk 26 / compileSdk 36 / targetSdk 36.
