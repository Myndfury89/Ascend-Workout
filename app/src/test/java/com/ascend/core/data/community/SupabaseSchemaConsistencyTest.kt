package com.ascend.core.data.community

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards against the P2A.1 mistake where an RPC referenced `profiles.display_name` — a column the
 * committed schema did not declare. It parses the canonical profiles columns from
 * supabase/p1/profiles.sql (+ social_eligible added by p2a/01) and asserts the P2A RPCs reference only
 * columns that actually exist in the approved schema. A stale/undeclared profile column reference now
 * fails the build instead of blowing up in live Supabase.
 *
 * (This checks repo-internal consistency; true live-DB drift detection would need a CI step running the
 * SQL against Supabase, which is out of scope here.)
 */
class SupabaseSchemaConsistencyTest {
    private val supabase = File("../supabase")

    private fun approvedProfileColumns(): Set<String> {
        val profiles = File(supabase, "p1/profiles.sql")
        assertTrue("canonical supabase/p1/profiles.sql must exist", profiles.exists())
        val text = profiles.readText().lowercase()
        val columns = mutableSetOf("id") // primary key from the create table
        Regex("add column if not exists (\\w+)").findAll(text).forEach { columns += it.groupValues[1] }
        columns += "social_eligible" // added by p2a/01_tables.sql
        return columns
    }

    @Test
    fun `p2a RPCs only reference profile columns that the approved schema declares`() {
        val columns = approvedProfileColumns()
        // In these RPCs, aliases `p` and `other` are the profiles table; `sp`/`f` are other tables.
        val profileAlias = Regex("\\b(?:p|other)\\.(\\w+)")
        listOf("p2a/04_lookup.sql", "p2a/05_cards.sql").forEach { rel ->
            val file = File(supabase, rel)
            assertTrue("$rel must exist", file.exists())
            profileAlias.findAll(file.readText().lowercase()).forEach { match ->
                val column = match.groupValues[1]
                assertTrue(
                    "$rel references profiles.$column, which is not in the approved schema $columns",
                    column in columns,
                )
            }
        }
    }

    @Test
    fun `display_name is part of the approved profile schema`() {
        // Regression anchor: display_name must stay declared, since the P1 code (ProfileDto/RemoteProfile)
        // and the P2A RPCs all depend on it.
        assertTrue("display_name" in approvedProfileColumns())
    }
}
