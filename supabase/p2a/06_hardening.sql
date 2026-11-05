-- Ascend Community P2A - access-rule hardening. Apply after 05_cards.sql.
-- Closes two gaps in 03_rls.sql / p1/profiles.sql (main @ 1fb09fd):
--  1) friendships_update_addressee lets the addressee rewrite requester_id (to any eligible user)
--     while accepting, forcing a friendship on someone who never asked. It also allows re-updating
--     rows that are not PENDING.
--  2) profiles_update_own lets any user set profiles.social_eligible = true on their own row,
--     bypassing the minor-protection default-deny.

-- ---- 1) Friendships: only PENDING -> ACCEPTED, participants immutable ----
create or replace function public.friendships_guard_update()
returns trigger language plpgsql as $$
begin
  if new.requester_id is distinct from old.requester_id
     or new.addressee_id is distinct from old.addressee_id then
    raise exception 'friendship participants are immutable';
  end if;
  if not (old.status = 'PENDING' and new.status = 'ACCEPTED') then
    raise exception 'only PENDING -> ACCEPTED is allowed';
  end if;
  return new;
end $$;

drop trigger if exists friendships_guard_update on public.friendships;
create trigger friendships_guard_update
  before update on public.friendships
  for each row execute function public.friendships_guard_update();

-- Tighten the policy too: the row must be PENDING before the addressee can touch it.
drop policy if exists friendships_update_addressee on public.friendships;
create policy friendships_update_addressee on public.friendships
  for update using (auth.uid() = addressee_id and status = 'PENDING')
  with check (
    auth.uid() = addressee_id
    and status = 'ACCEPTED'
    and not public.is_blocked_either(requester_id, addressee_id)
    and public.is_social_eligible(requester_id)
    and public.is_social_eligible(addressee_id)
  );

-- ---- 2) Profiles: social_eligible is server-controlled only ----
-- Clients may still edit their own profile, but only the service role (e.g. an Edge Function
-- that performs the age check) can change social_eligible.
create or replace function public.profiles_guard_social_eligible()
returns trigger language plpgsql as $$
begin
  if new.social_eligible is distinct from old.social_eligible
     and coalesce(auth.role(), '') <> 'service_role' then
    raise exception 'social_eligible can only be set server-side';
  end if;
  return new;
end $$;

drop trigger if exists profiles_guard_social_eligible on public.profiles;
create trigger profiles_guard_social_eligible
  before update on public.profiles
  for each row execute function public.profiles_guard_social_eligible();

-- Also block a client from inserting its own row pre-marked eligible.
create or replace function public.profiles_guard_social_eligible_insert()
returns trigger language plpgsql as $$
begin
  if new.social_eligible and coalesce(auth.role(), '') <> 'service_role' then
    new.social_eligible := false;
  end if;
  return new;
end $$;

drop trigger if exists profiles_guard_social_eligible_insert on public.profiles;
create trigger profiles_guard_social_eligible_insert
  before insert on public.profiles
  for each row execute function public.profiles_guard_social_eligible_insert();
