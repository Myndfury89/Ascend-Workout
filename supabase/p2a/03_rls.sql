-- Ascend Community P2A — Row Level Security.
-- Every P2A table is RLS-on with default-deny; access is granted only by the policies below.
-- Cross-user reads are gated by can_view_shared_profile() (friends AND consent AND no-block AND both eligible).

-- ============================ share_settings : own-row only ============================
alter table public.share_settings enable row level security;

create policy share_settings_select_own on public.share_settings
  for select using (auth.uid() = user_id);
create policy share_settings_insert_own on public.share_settings
  for insert with check (auth.uid() = user_id);
create policy share_settings_update_own on public.share_settings
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ============================ friendships : participants only ============================
alter table public.friendships enable row level security;

-- Read your own relationships (either side).
create policy friendships_select_participant on public.friendships
  for select using (auth.uid() in (requester_id, addressee_id));

-- Send a request: you are the requester, it starts PENDING, both eligible, and no block exists.
create policy friendships_insert_requester on public.friendships
  for insert with check (
    auth.uid() = requester_id
    and status = 'PENDING'
    and public.is_social_eligible(requester_id)
    and public.is_social_eligible(addressee_id)
    and not public.is_blocked_either(requester_id, addressee_id)
  );

-- Accept: only the addressee may flip PENDING -> ACCEPTED (and only if still unblocked/eligible).
create policy friendships_update_addressee on public.friendships
  for update using (auth.uid() = addressee_id)
  with check (
    auth.uid() = addressee_id
    and status = 'ACCEPTED'
    and not public.is_blocked_either(requester_id, addressee_id)
    and public.is_social_eligible(requester_id)
    and public.is_social_eligible(addressee_id)
  );

-- Decline / cancel / remove: either participant may delete the row.
create policy friendships_delete_participant on public.friendships
  for delete using (auth.uid() in (requester_id, addressee_id));

-- ============================ blocks : blocker only ============================
alter table public.blocks enable row level security;

create policy blocks_select_own on public.blocks
  for select using (auth.uid() = blocker_id);
create policy blocks_insert_own on public.blocks
  for insert with check (auth.uid() = blocker_id);
create policy blocks_delete_own on public.blocks
  for delete using (auth.uid() = blocker_id);

-- ============================ reports : reporter insert/read; service role manages ============================
alter table public.reports enable row level security;

create policy reports_insert_own on public.reports
  for insert with check (auth.uid() = reporter_id);
create policy reports_select_own on public.reports
  for select using (auth.uid() = reporter_id);
-- (No update/delete for users; the review queue is managed with the service role, off-device.)

-- ============================ shared_profiles : own write, consented friend read ============================
alter table public.shared_profiles enable row level security;

-- Owner can always read/write their own snapshot.
create policy shared_profiles_select_own on public.shared_profiles
  for select using (auth.uid() = user_id);
create policy shared_profiles_insert_own on public.shared_profiles
  for insert with check (auth.uid() = user_id);
create policy shared_profiles_update_own on public.shared_profiles
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- A friend may read the snapshot only when consent + friendship + no-block + eligibility all hold.
create policy shared_profiles_select_friend on public.shared_profiles
  for select using (public.can_view_shared_profile(auth.uid(), user_id));

-- ============================ profiles : keep P1 own-row read; DO NOT broaden here ============================
-- Cross-user identity is exposed ONLY via shared_profiles (derived) and the handle-lookup RPC (04),
-- never by broadening the profiles SELECT policy. profiles stays own-row (as created in P1).
