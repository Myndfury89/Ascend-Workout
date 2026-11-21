package com.ascend.core.data.community

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Static guard for supabase/p2a/07_age_check.sql (A2): the age-check infrastructure ships inactive
 * ('off'), keeps its tables and RPCs service-role only, never stores a DOB/age, and has a rollback.
 * Repo-internal only; it does not run the SQL against Supabase (see supabase/tests for a local run).
 */
class SupabaseAgeCheckSchemaTest {
    private fun normalized(rel: String): String {
        val file = File("../supabase/$rel")
        assertTrue("supabase/$rel must exist", file.exists())
        return file.readText().lowercase().replace(Regex("\\s+"), " ")
    }

    private val sql: String by lazy { normalized("p2a/07_age_check.sql") }
    private val rollback: String by lazy { normalized("p2a/07_age_check_rollback.sql") }

    private fun assertContains(
        label: String,
        needle: String,
    ) = assertTrue("07_age_check.sql is missing $label", sql.contains(needle))

    @Test
    fun `enforcement defaults to off and config is constrained`() {
        assertContains("default off", "enforcement_mode text not null default 'off'")
        assertContains("mode whitelist", "check (enforcement_mode in ('off','log_only','enforce'))")
        assertContains("threshold bounds", "check (min_age between 13 and 25)")
        assertContains("singleton row", "id boolean primary key default true check (id)")
        assertContains("seed does not overwrite", "on conflict (id) do nothing")
    }

    @Test
    fun `tables are rls-enabled and revoked from clients`() {
        assertContains("config rls", "alter table public.age_check_config enable row level security")
        assertContains("result rls", "alter table public.age_checks enable row level security")
        assertContains(
            "config revoke",
            "revoke all on table public.age_check_config from public, anon, authenticated",
        )
        assertContains(
            "result revoke",
            "revoke all on table public.age_checks from public, anon, authenticated",
        )
        assertFalse("07 must not add client policies", sql.contains("create policy"))
    }

    @Test
    fun `rpcs are service-role only and fail closed`() {
        assertContains(
            "record revoke",
            "revoke all on function public.age_check_record(uuid, boolean, integer, text) " +
                "from public, anon, authenticated",
        )
        assertContains(
            "record grant",
            "grant execute on function public.age_check_record(uuid, boolean, integer, text) to service_role",
        )
        assertContains("service_role check", "coalesce(auth.role(), '') <> 'service_role'")
        assertContains("missing config", "config missing (fail closed)")
        assertContains("stale threshold", "stale threshold (fail closed)")
        assertContains("only enforce writes eligibility", "update public.profiles p set social_eligible = eligible")
        assertFalse("07 must not be security definer", sql.contains("security definer"))
    }

    @Test
    fun `no dob or exact age is stored`() {
        // Strip SQL comments (they mention "DOB" to say it is NOT stored), then look for any column
        // declaration that would hold a birth date, an exact age, or an age band. ('dob'/'age_band'
        // appear only as values of the `method` column, which records how the user attested.)
        val code =
            File("../supabase/p2a/07_age_check.sql").readLines()
                .joinToString(" ") { it.substringBefore("--") }
                .lowercase()
        val forbidden =
            Regex(
                "\\b(dob|date_of_birth|birth_date|birthdate|birthday|age_band|age_years|age)\\s+" +
                    "(date|text|int|integer|smallint|numeric|timestamptz)\\b",
            )
        assertFalse("07 must not reference a DOB/age column", forbidden.containsMatchIn(code))
        assertContains("boolean outcome only", "meets_threshold boolean not null")
    }

    @Test
    fun `rollback drops only the 07 objects`() {
        listOf(
            "drop function if exists public.age_check_record(uuid, boolean, integer, text)",
            "drop function if exists public.age_check_get_config()",
            "drop table if exists public.age_checks",
            "drop table if exists public.age_check_config",
        ).forEach { assertTrue("rollback is missing: $it", rollback.contains(it)) }
        assertFalse("rollback must not drop profiles", rollback.contains("drop table if exists public.profiles"))
    }
}
