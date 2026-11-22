-- Ascend Community P2A - A2 server-side age check (INFRASTRUCTURE ONLY, INACTIVE BY DEFAULT).
-- Apply after 06_hardening.sql. Rollback: 07_age_check_rollback.sql.
--
-- What this adds:
--   * public.age_check_config  - singleton config row (threshold + enforcement mode). Service role only.
--   * public.age_checks        - one minimal result row per user. Service role only. NO DOB, NO age.
--   * public.age_check_get_config()  - service-role-only RPC used by the age-check Edge Function.
--   * public.age_check_record(...)   - service-role-only RPC; the ONLY code path that sets
--                                      profiles.social_eligible from an age check.
--
-- Safety properties:
--   * Default enforcement_mode = 'off'  -> the RPC writes nothing. Nobody is restricted or granted.
--     'log_only' records the minimal outcome but never touches profiles.social_eligible.
--     Only 'enforce' changes social_eligible. Switching modes is a separate, approved production step.
--   * Fail-safe: missing/invalid config, a stale threshold, or any error -> no grant (exception or no-op).
--     social_eligible stays false (the 01/06 default-deny), and is only ever set true in 'enforce'.
--   * Data minimization: no DOB, no exact age, no age band is stored. Only: met threshold (bool),
--     the threshold applied, the input method, the mode, and a timestamp.
--   * Clients (anon/authenticated) cannot read or write either table or call either RPC
--     (RLS on with no client policies + explicit REVOKEs). 06_hardening.sql still blocks any
--     non-service-role change to profiles.social_eligible.
--
-- Idempotent: safe to re-run. Re-running never overwrites an existing config row.

-- ---- Config (singleton) ----
create table if not exists public.age_check_config (
  id               boolean primary key default true check (id),
  enforcement_mode text    not null default 'off' check (enforcement_mode in ('off','log_only','enforce')),
  min_age          integer not null default 18 check (min_age between 13 and 25),
  allow_reattest   boolean not null default false,
  updated_at       timestamptz not null default now()
);

insert into public.age_check_config (id) values (true) on conflict (id) do nothing;

alter table public.age_check_config enable row level security;
revoke all on table public.age_check_config from public, anon, authenticated;

-- ---- Minimal per-user result ----
create table if not exists public.age_checks (
  user_id         uuid primary key references auth.users(id) on delete cascade,
  meets_threshold boolean not null,
  threshold       integer not null check (threshold between 13 and 25),
  method          text    not null check (method in ('dob','age_band')),
  mode            text    not null check (mode in ('log_only','enforce')),
  checked_at      timestamptz not null default now()
);

alter table public.age_checks enable row level security;
revoke all on table public.age_checks from public, anon, authenticated;
-- Deliberately NO policies on either table: with RLS enabled and no policy, clients get nothing.

-- ---- RPC: read config (service role only) ----
create or replace function public.age_check_get_config()
returns table (enforcement_mode text, min_age integer, allow_reattest boolean)
language sql
stable
set search_path = public
as $$
  select c.enforcement_mode, c.min_age, c.allow_reattest
  from public.age_check_config c
  where c.id = true;
$$;

-- ---- RPC: record a computed outcome (service role only) ----
-- Input is the already-computed boolean; the raw DOB / band never reaches the database.
-- Returns: status in ('disabled','recorded','applied','locked','profile_missing') and the
-- resulting social_eligible value (false unless enforce + meets + not locked).
create or replace function public.age_check_record(
  p_user_id         uuid,
  p_meets_threshold boolean,
  p_threshold       integer,
  p_method          text
)
returns table (status text, social_eligible boolean)
language plpgsql
volatile
set search_path = public
as $$
declare
  cfg      public.age_check_config%rowtype;
  prev     public.age_checks%rowtype;
  eligible boolean;
  locked   boolean := false;
  updated  integer;
begin
  -- Defense in depth: EXECUTE is already service-role only (grants below).
  if coalesce(auth.role(), '') <> 'service_role' then
    raise exception 'age_check_record is service-role only';
  end if;
  if p_user_id is null or p_meets_threshold is null or p_threshold is null
     or p_method is null or p_method not in ('dob','age_band') then
    raise exception 'age_check_record: invalid arguments';
  end if;

  select * into cfg from public.age_check_config where id = true;
  if not found then
    raise exception 'age_check_record: config missing (fail closed)';
  end if;

  if cfg.enforcement_mode = 'off' then
    return query select 'disabled'::text, public.is_social_eligible(p_user_id);
    return;
  end if;

  -- The Edge Function must have evaluated against the CURRENT threshold.
  if p_threshold <> cfg.min_age then
    raise exception 'age_check_record: stale threshold (fail closed)';
  end if;

  -- A prior below-threshold outcome at the same-or-lower threshold cannot be flipped by simply
  -- re-attesting, unless allow_reattest is on. (Lowering the threshold re-opens the check.)
  select * into prev from public.age_checks where user_id = p_user_id;
  if found and not prev.meets_threshold and not cfg.allow_reattest
     and prev.threshold <= p_threshold and p_meets_threshold then
    locked := true;
  end if;

  eligible := p_meets_threshold and not locked;

  if not locked then
    insert into public.age_checks as a (user_id, meets_threshold, threshold, method, mode, checked_at)
    values (p_user_id, p_meets_threshold, p_threshold, p_method, cfg.enforcement_mode, now())
    on conflict (user_id) do update
      set meets_threshold = excluded.meets_threshold,
          threshold       = excluded.threshold,
          method          = excluded.method,
          mode            = excluded.mode,
          checked_at      = excluded.checked_at;
  end if;

  if cfg.enforcement_mode = 'log_only' then
    return query select (case when locked then 'locked' else 'recorded' end)::text,
                        public.is_social_eligible(p_user_id);
    return;
  end if;

  -- enforce: the only place an age check changes social_eligible.
  update public.profiles p set social_eligible = eligible where p.id = p_user_id;
  get diagnostics updated = row_count;
  if updated = 0 then
    return query select 'profile_missing'::text, false;
    return;
  end if;
  return query select (case when locked then 'locked' else 'applied' end)::text, eligible;
end $$;

revoke all on function public.age_check_get_config() from public, anon, authenticated;
revoke all on function public.age_check_record(uuid, boolean, integer, text) from public, anon, authenticated;
grant execute on function public.age_check_get_config() to service_role;
grant execute on function public.age_check_record(uuid, boolean, integer, text) to service_role;
