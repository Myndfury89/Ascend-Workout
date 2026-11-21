-- Minimal Supabase stand-in for LOCAL, THROWAWAY Postgres runs only (never apply to a real project).
-- Mimics: anon/authenticated/service_role roles (service_role bypasses RLS), Supabase's default
-- grants on public, auth.users, auth.uid(), auth.role() (from request.jwt.claim.*).
create role anon nologin;
create role authenticated nologin;
create role service_role nologin bypassrls;
grant usage on schema public to anon, authenticated, service_role;
alter default privileges in schema public grant all on tables to anon, authenticated, service_role;
alter default privileges in schema public grant all on functions to anon, authenticated, service_role;
create schema auth;
grant usage on schema auth to anon, authenticated, service_role;
create table auth.users (id uuid primary key);
create function auth.uid() returns uuid language sql stable as
  $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
create function auth.role() returns text language sql stable as
  $$ select nullif(current_setting('request.jwt.claim.role', true), '') $$;
grant execute on all functions in schema auth to anon, authenticated, service_role;
