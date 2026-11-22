# `age-check` Edge Function (A2) — inactive by default

Server-side age check that is the only path allowed to set `profiles.social_eligible` (06_hardening
makes that column service-role-only). Ships with `enforcement_mode = 'off'` in
`public.age_check_config`, so deploying it changes nothing until that row is updated in a separately
approved step. Not called by the Android app (Community is hidden for the beta).

- **Auth:** `Authorization: Bearer <user access token>`. The token is verified with
  `auth.getUser(jwt)`; only that user id is acted on. A `user_id` in the body is ignored.
- **Input:** exactly one of `{"dob":"YYYY-MM-DD"}` or
  `{"age_band":"under_13"|"13_15"|"16_17"|"18_plus"}`.
- **Computation (logic.ts):** completed years as of the UTC-12 calendar date (never counts a
  birthday early); Feb 29 birthdays reach the next year on Mar 1. A band meets the threshold only if
  its youngest age does (a band straddling the threshold fails).
- **Data minimization:** the DOB/band is reduced to one boolean in memory. Only
  `(user_id, meets_threshold, threshold, method, mode, checked_at)` reaches the DB. Logs carry no
  user id, DOB or band.
- **Fail-safe:** bad JWT → 401; bad input → 400; config missing/invalid, DB error, stale threshold →
  503 with `social_eligible: false`. Nothing is ever granted outside `enforce`.
- **Env:** only the auto-provided `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY`. No new secrets.

Tests: `cd supabase/functions/age-check && deno test` (pure logic + handler with fakes).
