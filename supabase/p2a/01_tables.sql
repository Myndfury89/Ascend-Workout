-- Ascend Community P2A — Friends / Profiles / Safety : TABLES
-- Apply order: 01_tables.sql -> 02_functions.sql -> 03_rls.sql
-- Idempotent-ish (IF NOT EXISTS) so re-running is safe. Every table gets RLS in 03.
--
-- Privacy invariants encoded here:
--  * profiles.social_eligible defaults FALSE (default-deny): a user is socially invisible until the
--    client marks them adult-eligible. Age itself is NEVER stored/exposed here — only this boolean.
--  * shared_profiles holds ONLY derived, presentation-safe fields. Raw training evidence, Build
--    Characteristic scores/coverage, and any health metrics are structurally absent — friends can only
--    read this snapshot, so "share identity, not evidence" is enforced by schema, not by client code.

-- --- profiles (created in P1): add the social-eligibility gate + ensure handle is unique/lookup-ready.
alter table public.profiles
  add column if not exists social_eligible boolean not null default false;

create unique index if not exists profiles_handle_key on public.profiles (lower(handle))
  where handle is not null;

-- --- share_settings: per-user consent. Default PRIVATE; v1 caps audience at FRIENDS (no PUBLIC).
create table if not exists public.share_settings (
  user_id              uuid primary key references auth.users(id) on delete cascade,
  visibility           text not null default 'PRIVATE' check (visibility in ('PRIVATE','FRIENDS')),
  share_build_identity boolean not null default false,
  share_class_progress boolean not null default false,
  updated_at           timestamptz not null default now()
);

-- --- friendships: one row per unordered pair; mutual consent (PENDING -> ACCEPTED).
create table if not exists public.friendships (
  id           uuid primary key default gen_random_uuid(),
  requester_id uuid not null references auth.users(id) on delete cascade,
  addressee_id uuid not null references auth.users(id) on delete cascade,
  status       text not null default 'PENDING' check (status in ('PENDING','ACCEPTED')),
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  check (requester_id <> addressee_id)
);

-- Unordered-pair uniqueness: A->B and B->A cannot coexist.
create unique index if not exists friendships_pair_key
  on public.friendships (least(requester_id, addressee_id), greatest(requester_id, addressee_id));

-- --- blocks: one directional record; bidirectional effect enforced in RLS/functions.
create table if not exists public.blocks (
  blocker_id uuid not null references auth.users(id) on delete cascade,
  blocked_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (blocker_id, blocked_id),
  check (blocker_id <> blocked_id)
);

-- --- reports: minimum moderation path into a manual review queue.
create table if not exists public.reports (
  id          uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references auth.users(id) on delete cascade,
  subject_id  uuid references auth.users(id) on delete set null,
  reason      text not null,
  note        text,
  status      text not null default 'OPEN' check (status in ('OPEN','REVIEWING','RESOLVED')),
  created_at  timestamptz not null default now()
);

-- --- shared_profiles: the ONLY cross-user-readable derived snapshot. Owner-published, presentation-only.
create table if not exists public.shared_profiles (
  user_id         uuid primary key references auth.users(id) on delete cascade,
  selected_class  text,           -- display name, e.g. "Mage"
  class_level     integer,
  rank            text,           -- derived (RankCalculator output), e.g. "Gold"
  overall_level   integer,
  build_identity  text,           -- dominant affinity label, e.g. "Berserker-leaning"
  top_affinities  jsonb,          -- [{class,affinity}] — ONLY dominantEligible entries; no coverage/evidence
  avatar_body_base text,          -- Your Ascended cosmetic base (MALE/FEMALE/null)
  updated_at      timestamptz not null default now()
);
