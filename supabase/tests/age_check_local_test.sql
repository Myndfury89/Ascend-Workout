-- Executed checks for 07_age_check.sql against the shim. Run via run_age_check_local.sh.
\set ON_ERROR_STOP on
\set A '''aaaaaaaa-0000-0000-0000-000000000001'''
\set B '''bbbbbbbb-0000-0000-0000-000000000002'''
\set C '''cccccccc-0000-0000-0000-000000000003'''

insert into auth.users values (:A), (:B), (:C);
insert into public.profiles (id, handle) values (:A, 'a'), (:B, 'b'); -- C has no profile

create or replace function pg_temp.expect_error(stmt text, label text) returns void language plpgsql as $$
begin
  begin
    execute stmt;
  exception when others then
    raise notice 'PASS (denied as expected): % [%]', label, sqlerrm;
    return;
  end;
  raise exception 'FAIL: % was allowed', label;
end $$;
create or replace function pg_temp.check(ok boolean, label text) returns void language plpgsql as $$
begin
  if ok is not true then raise exception 'FAIL: %', label; end if;
  raise notice 'PASS: %', label;
end $$;

-- Defaults
select pg_temp.check((select enforcement_mode = 'off' and min_age = 18 and not allow_reattest
  from public.age_check_config), 'default config is off / 18 / no re-attest');
select pg_temp.check(not exists (select 1 from information_schema.columns
  where table_schema = 'public' and table_name in ('age_checks','age_check_config','profiles')
    and column_name ~ '(dob|birth|age_band|^age$)'), 'no DOB / age / band column anywhere');

-- ---- Client (authenticated) cannot touch any of it ----
grant execute on function pg_temp.expect_error(text, text), pg_temp.check(boolean, text) to public;
set role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', false);
select set_config('request.jwt.claim.sub', :A, false);
select pg_temp.expect_error('select * from public.age_check_config', 'client read config');
select pg_temp.expect_error('update public.age_check_config set enforcement_mode = ''enforce''', 'client write config');
select pg_temp.expect_error('select * from public.age_checks', 'client read age_checks');
select pg_temp.expect_error(format('insert into public.age_checks values (%L, true, 18, ''dob'', ''enforce'', now())', :A), 'client insert age_checks');
select pg_temp.expect_error('select public.age_check_get_config()', 'client call get_config');
select pg_temp.expect_error(format('select public.age_check_record(%L, true, 18, ''dob'')', :A), 'client call record');
select pg_temp.expect_error('update public.profiles set social_eligible = true where id = auth.uid()', 'client self-set social_eligible (06 trigger)');
reset role;
set role anon;
select set_config('request.jwt.claim.role', 'anon', false);
select pg_temp.expect_error('select public.age_check_get_config()', 'anon call get_config');
select pg_temp.expect_error(format('select public.age_check_record(%L, true, 18, ''dob'')', :A), 'anon call record');
reset role;

-- No JWT role at all (e.g. SQL editor as postgres): record refuses.
select set_config('request.jwt.claim.role', '', false);
select pg_temp.expect_error(format('select public.age_check_record(%L, true, 18, ''dob'')', :A), 'record without service_role claim');

-- ---- Service role ----
set role service_role;
select set_config('request.jwt.claim.role', 'service_role', false);
select set_config('request.jwt.claim.sub', '', false);
select pg_temp.check((select enforcement_mode = 'off' from public.age_check_get_config()), 'service role reads config');

-- off: no write
select pg_temp.check((select status = 'disabled' and not social_eligible
  from public.age_check_record(:A, true, 18, 'dob')), 'off -> disabled, not eligible');
select pg_temp.check((select count(*) = 0 from public.age_checks), 'off -> nothing stored');

-- log_only: stores minimal row, never changes profiles
update public.age_check_config set enforcement_mode = 'log_only';
select pg_temp.check((select status = 'recorded' and not social_eligible
  from public.age_check_record(:A, true, 18, 'dob')), 'log_only -> recorded, not eligible');
select pg_temp.check((select meets_threshold and threshold = 18 and mode = 'log_only'
  from public.age_checks where user_id = :A), 'log_only stores only the boolean + threshold');
select pg_temp.check((select not social_eligible from public.profiles where id = :A), 'log_only leaves social_eligible false');
select pg_temp.expect_error(format('select public.age_check_record(%L, true, 17, ''dob'')', :A), 'stale threshold');
select pg_temp.expect_error(format('select public.age_check_record(%L, true, 18, ''ssn'')', :A), 'bad method');
select pg_temp.expect_error(format('select public.age_check_record(%L, null, 18, ''dob'')', :A), 'null outcome');

-- enforce
update public.age_check_config set enforcement_mode = 'enforce';
select pg_temp.check((select status = 'applied' and social_eligible
  from public.age_check_record(:A, true, 18, 'dob')), 'enforce + meets -> eligible');
select pg_temp.check((select social_eligible from public.profiles where id = :A), 'profiles.social_eligible set true by service role');
select pg_temp.check((select status = 'applied' and not social_eligible
  from public.age_check_record(:B, false, 18, 'age_band')), 'enforce + below -> not eligible');
select pg_temp.check((select status = 'locked' and not social_eligible
  from public.age_check_record(:B, true, 18, 'dob')), 're-attest after below is locked');
select pg_temp.check((select not meets_threshold from public.age_checks where user_id = :B), 'locked re-attest does not overwrite the row');
select pg_temp.check((select status = 'applied' and not social_eligible
  from public.age_check_record(:A, false, 18, 'dob')), 'enforce + below revokes prior eligibility');
select pg_temp.check((select status = 'profile_missing' and not social_eligible
  from public.age_check_record(:C, true, 18, 'dob')), 'no profile row -> profile_missing, not eligible');

-- lowering the threshold re-opens the check; allow_reattest also unlocks
update public.age_check_config set min_age = 16;
select pg_temp.check((select status = 'applied' and social_eligible
  from public.age_check_record(:B, true, 16, 'dob')), 'lower threshold re-opens check');
update public.age_check_config set min_age = 18, allow_reattest = false;
select public.age_check_record(:B, false, 18, 'dob');
update public.age_check_config set allow_reattest = true;
select pg_temp.check((select status = 'applied' and social_eligible
  from public.age_check_record(:B, true, 18, 'dob')), 'allow_reattest unlocks');

-- fail-safe: config row missing
delete from public.age_check_config;
select pg_temp.expect_error(format('select public.age_check_record(%L, true, 18, ''dob'')', :A), 'missing config fails closed');
select pg_temp.expect_error('insert into public.age_check_config (id, enforcement_mode) values (true, ''on'')', 'invalid mode rejected by CHECK');
select pg_temp.expect_error('insert into public.age_check_config (id, min_age) values (true, 12)', 'min_age below 13 rejected');
select pg_temp.expect_error('insert into public.age_check_config (id) values (false)', 'second config row rejected');
reset role;
select set_config('request.jwt.claim.role', '', false);
\echo 'ALL AGE-CHECK LOCAL CHECKS PASSED'
