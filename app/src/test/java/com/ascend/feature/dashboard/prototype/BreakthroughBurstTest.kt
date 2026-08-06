package com.ascend.feature.dashboard.prototype

import androidx.compose.ui.graphics.vector.VectorPath
import org.junit.Assert.assertEquals
import org.junit.Test

/** CP-D: the pre-authored breakthrough burst is an ImageVector authored once (12 spokes + a ring). */
class BreakthroughBurstTest {
    @Test
    fun `the pre-authored burst vector has twelve spokes plus one shockwave ring`() {
        val vector = breakthroughBurstVector()
        val pathCount = countPaths(vector.root)
        assertEquals("12 radial spokes + 1 ring", 13, pathCount)
    }

    @Test
    fun `both burst modes are selectable`() {
        assertEquals(2, BurstMode.entries.size)
        assertEquals(BurstMode.PROCEDURAL, BurstMode.entries.first())
    }

    private fun countPaths(group: androidx.compose.ui.graphics.vector.VectorGroup): Int =
        group.sumOf { node ->
            when (node) {
                is VectorPath -> 1
                is androidx.compose.ui.graphics.vector.VectorGroup -> countPaths(node)
                else -> 0
            }
        }
}
