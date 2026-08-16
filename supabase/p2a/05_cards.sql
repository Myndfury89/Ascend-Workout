-- Ascend Community P2A — friend/request card resolution.
-- Returns a minimal card (id, handle, display name, avatar) for every user the caller has a friendship
-- row with (PENDING or ACCEPTED, either direction), excluding blocks. This lets the client render the
-- friends list AND incoming/outgoing requests with real names — without broadening the profiles SELECT
-- policy (which stays own-row) and without exposing class/build data to non-friends.
--
-- The caller joins these cards with their friendship edges (which carry status/direction) by id.

create or replace function public.friend_cards()
returns table (
  id               uuid,
  handle           text,
  display_name     text,
  avatar_body_base text
)
language sql
security definer
set search_path = public
stable
as $$
  select
    other.id,
    other.handle,
    other.display_name,
    sp.avatar_body_base
  from public.friendships f
  join public.profiles other
    on other.id = case when f.requester_id = auth.uid() then f.addressee_id else f.requester_id end
  left join public.shared_profiles sp on sp.user_id = other.id
  where auth.uid() in (f.requester_id, f.addressee_id)
    and not public.is_blocked_either(auth.uid(), other.id);
$$;

revoke all on function public.friend_cards() from public;
grant execute on function public.friend_cards() to authenticated;
