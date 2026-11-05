package com.ascend.core.data.community

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Static guard for supabase/p2a/06_hardening.sql: asserts the access-rule hardening (immutable
 * friendship participants, PENDING -> ACCEPTED only, server-only social_eligible) stays in the repo.
 * Repo-internal only; it does not run the SQL against Supabase.
 */
class SupabaseHardeningSchemaTest {
    private val sql: String by lazy {
        val file = File("../supabase/p2a/06_hardening.sql")
        assertTrue("supabase/p2a/06_hardening.sql must exist", file.exists())
        file.readText().lowercase().replace(Regex("\\s+"), " ")
    }

    private fun assertContains(label: String, needle: String) =
        assertTrue("06_hardening.sql is missing $label", sql.contains(needle))

    @Test
    fun `friendship updates are guarded by a trigger`() {
        assertContains("friendships_guard_update function", "create or replace function public.friendships_guard_update()")
        assertContains("friendships_guard_update trigger", "create trigger friendships_guard_update before update on public.friendships")
        assertContains("participant immutability", "new.requester_id is distinct from old.requester_id")
        assertContains("addressee immutability", "new.addressee_id is distinct from old.addressee_id")
        assertContains("pending to accepted only", "old.status = 'pending' and new.status = 'accepted'")
    }

    @Test
    fun `addressee update policy requires a pending row and an accepted result`() {
        assertContains("policy recreate", "create policy friendships_update_addressee on public.friendships for update")
        assertContains("pending using clause", "using (auth.uid() = addressee_id and status = 'pending')")
        assertContains("accepted check", "status = 'accepted'")
        assertContains("block check", "not public.is_blocked_either(requester_id, addressee_id)")
    }

    @Test
    fun `social_eligible is server-only on update and insert`() {
        assertContains("update trigger", "create trigger profiles_guard_social_eligible before update on public.profiles")
        assertContains("insert trigger", "create trigger profiles_guard_social_eligible_insert before insert on public.profiles")
        assertContains("service_role gate", "coalesce(auth.role(), '') <> 'service_role'")
        assertContains("insert forced false", "new.social_eligible := false")
    }
}
