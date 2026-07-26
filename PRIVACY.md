# Privacy

Health and fitness data is sensitive. Ascend is **local‑first** and treats health
data with care. Ascend is **not** a medical service and does not diagnose
conditions.

## Principles

- **Local‑first.** The app works fully offline with a single local profile; no
  account or cloud connection is required. Cloud sync is strictly opt‑in (Phase 11).
- **Minimal permissions.** Health Connect permissions (later phase) will be
  requested only as needed, each with a plain‑language rationale, and are revocable.
- **No advertising / no sale.** Health data is never used for advertising or sold.
- **Backups exclude sensitive data by default.** `data_extraction_rules.xml`
  excludes the database, shared prefs, and datastore from cloud backup and device
  transfer; `allowBackup=false`.
- **Redaction in logs.** Private health values must never be written to production
  logs; a logging abstraction that redacts sensitive fields is planned before any
  Health Connect values flow through the app.

## Data the app stores (current)

Local Room database only: profile display name, RPG progression (level, XP, rank,
attributes, streaks), quests and logged sets. No health‑platform data is read yet
(Health Connect is a later phase).

## User controls (planned, per spec)

Export (CSV/JSON), delete local data, delete cloud data, permission management, and
a privacy screen. These are part of the Settings/Privacy work in later phases.

## Secrets

No API keys or secrets are stored in the repository. Backend adapters
(Supabase/Firebase) will read configuration from environment‑specific config, not
source control.

## Minors

For users under 18, aggressive calorie‑deficit coaching and extreme exercise
targets are avoided (safety design, later phases).
