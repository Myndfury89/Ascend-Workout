# Ascend

Ascend turns real exercise and health activity into an original dark‑fantasy RPG
leveling experience. Workouts and daily fitness quests earn XP, raise character
attributes, and advance you through original ranks — awakening, growth,
discipline, progression.

> **Original IP.** Ascend does not copy names, terminology, art, fonts, icons,
> layouts, sound, rank symbols, or color combinations associated with any
> franchise. Its identity (palette, ranks, marks, wording) is its own.

> **Not medical advice.** Ascend is a fitness‑tracking app, not a medical
> service. It encourages ambitious goals while discouraging unsafe behavior.

---

## Status: Milestone 2 — Workouts (in progress)

This repository currently implements **complete, tested vertical slices** of the
core loop, built in verifiable phases. It is not the full product yet.

**Done and verified (build green, unit + integration tests passing):**

- **Foundation** — Kotlin, Jetpack Compose, Material 3, Hilt, type‑safe Navigation,
  an original dark design system, a 5‑destination shell (Status, Quests, Workout,
  Calendar, Progress).
- **Database** — Room (entities, DAOs, schema exported for migrations, now at
  **v2** with a real `MIGRATION_1_2`) with unique‑index idempotency guards for XP
  and imports.
- **Progression engine** — pure, configurable calculators (Level, XP, Rank,
  Attribute, Streak, Set‑Suggestion) plus a Room‑backed, transactional,
  **idempotent** XP/attribute ledger with reversal.
- **200‑push‑up quest flow** — create an accumulation quest, log uneven custom
  sets, edit/delete sets, see remaining reps and safe set suggestions, complete
  the quest, earn XP **exactly once**, level up, and grow Strength + Discipline.
- **Workout logging (Milestone 2)** — a seeded exercise catalog, log a session of
  sets across exercises (reps / duration / distance / weighted), then finish to
  earn XP **exactly once** and grow the exercise's primary attribute (plus a flat
  Discipline adherence bonus). Backed by the same transactional, idempotent
  ledger, with a real v1→v2 migration validated on the JVM.

**Not yet implemented:** onboarding ("The Awakening"), the real Status dashboard,
scheduling/calendar, Health Connect, notifications, expeditions, progress
dashboards, achievements/titles, and cloud sync. Status, Calendar, and Progress
screens are still placeholders.

See `ARCHITECTURE.md`, `PROGRESSION_SYSTEM.md`, `QUEST_SYSTEM.md`, `WORKOUT_SYSTEM.md`,
`DATABASE.md`, `TESTING.md`, and `PRIVACY.md` for details.

---

## Screens (current)

| Destination | State |
|---|---|
| Quests | Live — seeded 200‑push‑up quest, Active Quest screen with quick‑add, set history, suggestions, completion |
| Workout | Live — logged‑session list, log‑workout flow (exercise catalog, sets, difficulty), finish for XP + attributes |
| Status / Calendar / Progress | Placeholder |

## Architecture (summary)

Clean Architecture + MVVM, offline‑first, unidirectional data flow:

```
Compose UI (immutable UiState, ViewModel)
   → UseCase / Repository interface (domain — no Android)
      → Room / DataStore / (future) Health Connect, cloud (data)
```

Currently a single `:app` module with strict package boundaries mirroring the
intended feature/core modules (see `ARCHITECTURE.md` → migration plan).

## Requirements

- Android Studio (bundled JDK 17+ works; project targets Java 17)
- Android SDK Platform 36, Build‑Tools 36
- `minSdk 26` (Health Connect), `compileSdk 36`, `targetSdk 36`

## Setup

1. Open the project in Android Studio and let Gradle sync, **or** build from the
   command line with the Gradle wrapper.
2. `local.properties` must point at your SDK (`sdk.dir=...`). It is git‑ignored.

## Build & test commands

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew testDebugUnitTest      # run unit + Robolectric integration/UI tests
./gradlew installDebug           # install on a running emulator/device
```

On Windows use `gradlew.bat`. The CLI needs a JDK; Android Studio's bundled JBR
works: set `JAVA_HOME` to `<Android Studio>/jbr`.

## Try the flow

Run the app → **Quests** tab → open **Upper‑Body Trial** → tap `+25 / +40 / +50`
or **Custom** to log sets → delete a set to see progress recompute → **Complete
Quest** to earn XP once and (if the threshold is crossed) level up.

## Environment configuration

No cloud backend is required — Ascend runs fully offline with a single local
profile. Authentication, sync, analytics, and crash reporting are defined behind
replaceable interfaces (Supabase/Firebase adapters planned for Phase 11); none
are wired yet. No secrets are stored in the repository.

## Known limitations

- The UI has been verified by JVM/Robolectric Compose tests, not yet on a
  physical device across form factors.
- Lint/Detekt/Ktlint and Room migration tests are being wired.
- Only the Quests flow is functional; other tabs are placeholders.

## Roadmap

Workouts ✅ → Scheduling/Calendar → Health Connect → Notifications → Expeditions →
Progress dashboards → Achievements/Titles → Cloud sync → Release hardening.
