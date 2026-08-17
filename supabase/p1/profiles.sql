-- Ascend Community P1 — canonical `public.profiles` schema (source of truth).
--
-- This file previously lived only in the P1 dashboard runbook, not in the repo — which let the P2A
-- RPCs reference a `display_name` column the live table never actually got. Committing it here makes the
-- approved profile model version-controlled and lets a drifted table be repaired by re-running this.
--
-- Idempotent: safe to run any number of times. It creates the table if absent and adds any approved
-- column that is missing (drift repair). P2A adds `social_eligible` separately (see p2a/01_tables.sql).
-- Apply this BEFORE the p2a/ scripts.

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade
);

-- Approved columns, each add-if-missing so an older/minimal profiles table is brought into line.
alter table public.profiles add column if not exists handle text;
alter table public.profiles add column if not exists display_name text;
alter table public.profiles add column if not exists created_at timestamptz not null default now();
alter table public.profiles add column if not exists updated_at timestamptz not null default now();

alter table public.profiles enable row level security;

-- Own-row read/write. Guarded creates so re-running does not error on existing policies.
do $$
begin
  if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'profiles' and policyname = 'profiles_select_own') then
    create policy profiles_select_own on public.profiles for select using (auth.uid() = id);
  end if;
  if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'profiles' and policyname = 'profiles_insert_self') then
    create policy profiles_insert_self on public.profiles for insert with check (auth.uid() = id);
  end if;
  if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'profiles' and policyname = 'profiles_update_own') then
    create policy profiles_update_own on public.profiles for update using (auth.uid() = id) with check (auth.uid() = id);
  end if;
end $$;
