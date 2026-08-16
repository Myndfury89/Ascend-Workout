package com.ascend.core.domain.community

import com.ascend.core.common.LOCAL_USER_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Architecture + convention guards for the Community domain seam. */
class CommunityDomainSeamTest {
    @Test
    fun `the community domain package contains zero Supabase or SDK types`() {
        val dir = File("src/main/java/com/ascend/core/domain/community")
        val files = dir.walk().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("expected community domain files to scan", files.isNotEmpty())
        // Scan IMPORT lines only — a KDoc that mentions "the Supabase SDK" as prose is fine; an actual
        // SDK import is not.
        val forbidden = listOf("io.github.jan", "supabase", "postgrest", "io.ktor", "gotrue")
        files.forEach { file ->
            val imports = file.readLines().filter { it.trimStart().startsWith("import ") }.map { it.lowercase() }
            imports.forEach { line ->
                forbidden.forEach { needle ->
                    assertFalse("${file.name} imports '$needle' — domain must stay SDK-free", line.contains(needle))
                }
            }
        }
    }

    @Test
    fun `offline is distinguishable from a remote error`() {
        val offline: RemoteResult<Unit> = RemoteResult.Offline
        val error: RemoteResult<Unit> = RemoteResult.Failure(RemoteErrorKind.UNKNOWN, "x")
        assertTrue(offline is RemoteResult.Offline)
        assertTrue(error is RemoteResult.Failure)
        assertFalse(offline is RemoteResult.Failure)
    }

    @Test
    fun `local game identity is a plain String constant, never a RemoteUserId`() {
        // RemoteUserId is a distinct type; local progression is keyed by LOCAL_USER_ID and never re-keyed.
        assertEquals("local-player", LOCAL_USER_ID)
        val remote = RemoteUserId("some-uuid")
        assertFalse("a RemoteUserId value must not equal the local id", remote.value == LOCAL_USER_ID)
    }

    @Test
    fun `report reasons are coarse categories with no sensitive data surface`() {
        assertEquals(
            setOf(ReportReason.HARASSMENT, ReportReason.SPAM, ReportReason.INAPPROPRIATE_NAME, ReportReason.OTHER),
            ReportReason.entries.toSet(),
        )
    }
}
