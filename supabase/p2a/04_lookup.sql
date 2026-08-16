-- Ascend Community P2A — handle lookup RPC.
-- Exact-match, minimal-card discovery WITHOUT broadening the profiles SELECT policy. Returns only the
-- safe card (id, handle, display name, avatar body base) and NOT for minors/ineligible or blocked users.
-- No fuzzy search, no "user exists" oracle beyond an exact handle hit. Rate-limit at the app layer.

create or replace function public.lookup_profile_by_handle(p_handle text)
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
  select p.id, p.handle, p.display_name, sp.avatar_body_base
  from public.profiles p
  left join public.shared_profiles sp on sp.user_id = p.id
  where lower(p.handle) = lower(p_handle)
    and p.social_eligible = true                              -- never surface minors/ineligible
    and public.is_social_eligible(auth.uid())                 -- caller must be eligible too
    and not public.is_blocked_either(auth.uid(), p.id)        -- respect blocks either direction
    and auth.uid() <> p.id;                                   -- don't "discover" yourself
$$;

-- Authenticated users may call the RPC; the function body enforces the safety filters above.
revoke all on function public.lookup_profile_by_handle(text) from public;
grant execute on function public.lookup_profile_by_handle(text) to authenticated;
