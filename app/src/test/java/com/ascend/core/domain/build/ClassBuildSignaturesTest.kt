package com.ascend.core.domain.build

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassBuildSignaturesTest {
    @Test
    fun `all seven classes have a signature`() {
        assertEquals(BuildClass.entries.toSet(), ClassBuildSignatures.ALL.map { it.buildClass }.toSet())
    }

    @Test
    fun `activity never appears in a class signature`() {
        ClassBuildSignatures.ALL.forEach { sig ->
            assertTrue(
                "${sig.buildClass} must not weight Activity",
                BuildCharacteristic.ACTIVITY !in sig.weights.keys,
            )
        }
    }

    @Test
    fun `every weight is in the open unit range and every class has a defining characteristic`() {
        ClassBuildSignatures.ALL.forEach { sig ->
            sig.weights.forEach { (_, w) -> assertTrue(w.weight > 0.0 && w.weight <= 1.0) }
            assertNotNull("${sig.buildClass} needs a top characteristic", sig.topCharacteristic)
        }
    }

    @Test
    fun `only berserker, monk and mage are active`() {
        assertEquals(
            setOf(BuildClass.BERSERKER, BuildClass.MONK, BuildClass.MAGE),
            BuildClass.entries.filter { it.active }.toSet(),
        )
    }

    @Test
    fun `speed and distance always carry a metric or product provenance, never tag-only`() {
        ClassBuildSignatures.ALL.forEach { sig ->
            listOf(BuildCharacteristic.SPEED, BuildCharacteristic.DISTANCE).forEach { c ->
                sig.weights[c]?.let { w ->
                    assertTrue(
                        "${sig.buildClass} $c cannot be tag-derived (tags have no speed/distance axis)",
                        WeightSource.METRIC in w.sources || WeightSource.PRODUCT in w.sources,
                    )
                }
            }
        }
    }
}
