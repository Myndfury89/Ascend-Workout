# Community P2A — Supabase schema & RLS (Friends / Profiles / Safety)

Backend for the app's **first cross-user visibility**. Apply these in the Supabase SQL editor (or CLI)
against the Ascend project. This is P2A.1 — schema + policies only; the Android seams (P2A.2) and UI +
block/report (P2A.3) come next, on their own branches.

## Apply order
1. `01_tables.sql` — tables + indexes (adds `profiles.social_eligible`; creates share_settings,
   friendships, blocks, reports, shared_profiles).
2. `02_functions.sql` — RLS helper functions (are_friends, is_blocked_either, is_social_eligible,
   can_view_shared_profile).
3. `03_rls.sql` — enable RLS + policies on every new table.
4. `04_lookup.sql` — exact-handle discovery RPC (minimal card, safety-filtered).

## Design invariants (why it's shaped this way)
- **Default-deny.** `profiles.social_eligible` defaults FALSE — a user is invisible to social features
  until the client marks them adult-eligible. **Age is never stored or exposed here; only this boolean.**
- **Derived-over-raw is structural.** Cross-user reads hit only `shared_profiles`, which contains ONLY
  presentation fields (class name, level, rank, build-identity label, dominantEligible top affinities,
  avatar base). Raw workout evidence, Build Characteristic scores/coverage/confidence, and every health
  metric are *absent from any readable table*, so they cannot leak via RLS.
- **profiles stays own-row.** Identity is exposed cross-user only via `shared_profiles` (consented) and
  the `lookup_profile_by_handle` RPC (minimal card). The `profiles` SELECT policy is NOT broadened.
- **Consent + friendship + no-block + eligibility** are all required to read a shared profile
  (`can_view_shared_profile`).
- **Blocks are bidirectional in effect** and short-circuit friendship inserts, accepts, reads, and lookup.
- **Minors:** never `social_eligible` → cannot be looked up, cannot send/accept requests, cannot be read.
  (Client sets `social_eligible=true` only for adults; posture pending legal review.)

## Acceptance checklist (verify before P2A.2)
- [ ] All five tables exist; RLS **enabled** on share_settings, friendships, blocks, reports, shared_profiles.
- [ ] `profiles.social_eligible` exists, default false.
- [ ] Two adult test users, both `social_eligible=true`, both `share_settings.visibility='FRIENDS'`,
      `share_build_identity=true`: after an ACCEPTED friendship, each can read the other's `shared_profiles`
      row; with no friendship, neither can.
- [ ] Blocking: after A blocks B, B can no longer read A's shared profile, cannot send A a request, and
      `lookup_profile_by_handle` returns nothing for A↔B.
- [ ] A non-eligible (minor) user: not returned by `lookup_profile_by_handle`; cannot insert a friendship;
      their `shared_profiles` is unreadable by others.
- [ ] `lookup_profile_by_handle` returns only (id, handle, display_name, avatar_body_base) — never
      class/level/rank/build fields for a non-friend.
- [ ] `friendships` cannot hold both A→B and B→A (pair-key unique).
- [ ] Accept works only for the addressee; either party can delete (decline/cancel/remove).

## Rollback (if needed)
Drop in reverse dependency order: policies (implicit on table drop), then
`drop table if exists public.shared_profiles, public.reports, public.blocks, public.friendships, public.share_settings cascade;`
`drop function if exists public.lookup_profile_by_handle(text), public.can_view_shared_profile(uuid,uuid), public.is_social_eligible(uuid), public.is_blocked_either(uuid,uuid), public.are_friends(uuid,uuid);`
`alter table public.profiles drop column if exists social_eligible;`

## Not in P2A.1
No Android code, no Room changes, no migrations to the app DB. No parties (P2B). No PUBLIC visibility,
no push, no presence. CAPTCHA/Custom SMTP remain separate pre-launch items.
