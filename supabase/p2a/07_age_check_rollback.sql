-- Rollback for 07_age_check.sql (A2 age-check infrastructure). Idempotent.
-- Removes only what 07 added. It does NOT reset profiles.social_eligible; if enforce mode was ever
-- on and you want everyone back to default-deny, run (as service role / SQL editor, separately approved):
--   alter table public.profiles disable trigger profiles_guard_social_eligible;
--   update public.profiles set social_eligible = false where social_eligible;
--   alter table public.profiles enable trigger profiles_guard_social_eligible;
drop function if exists public.age_check_record(uuid, boolean, integer, text);
drop function if exists public.age_check_get_config();
drop table if exists public.age_checks;
drop table if exists public.age_check_config;
