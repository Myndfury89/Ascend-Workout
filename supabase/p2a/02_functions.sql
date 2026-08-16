-- Ascend Community P2A — helper functions for RLS.
-- SECURITY DEFINER + locked search_path so policies can check friendship/block/consent without
-- recursively triggering RLS on those tables. These are the single source of truth for "who may see whom".

-- Are these two users accepted friends?
create or replace function public.are_friends(a uuid, b uuid)
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1 from public.friendships f
    where f.status = 'ACCEPTED'
      and ( (f.requester_id = a and f.addressee_id = b)
         or (f.requester_id = b and f.addressee_id = a) )
  );
$$;

-- Is there a block in EITHER direction between the two users?
create or replace function public.is_blocked_either(a uuid, b uuid)
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1 from public.blocks bl
    where (bl.blocker_id = a and bl.blocked_id = b)
       or (bl.blocker_id = b and bl.blocked_id = a)
  );
$$;

-- Is a user socially eligible (adult-eligible + has not been globally restricted)?
create or replace function public.is_social_eligible(u uuid)
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select coalesce((select p.social_eligible from public.profiles p where p.id = u), false);
$$;

-- May `viewer` see `target`'s consented shared profile?
-- ALL must hold: accepted friends, no block either way, both socially eligible,
-- and target's visibility is FRIENDS.
create or replace function public.can_view_shared_profile(viewer uuid, target uuid)
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select
    public.are_friends(viewer, target)
    and not public.is_blocked_either(viewer, target)
    and public.is_social_eligible(viewer)
    and public.is_social_eligible(target)
    and coalesce(
          (select s.visibility from public.share_settings s where s.user_id = target),
          'PRIVATE'
        ) = 'FRIENDS';
$$;
