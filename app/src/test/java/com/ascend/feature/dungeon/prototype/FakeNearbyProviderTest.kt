package com.ascend.feature.dungeon.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the fake, debug-only nearby-presence provider — no BLE, no distance, rotating ids. */
class FakeNearbyProviderTest {
    @Test
    fun `fake discovery yields anonymous presences`() {
        val provider = FakeNearbyAscendedProvider(seed = 1, initialCount = 4)
        val nearby = provider.nearby.value
        assertEquals(4, nearby.size)
        assertTrue("handles are anonymous, not names", nearby.all { it.handle.startsWith("Ascended-") })
    }

    @Test
    fun `presence exposes only a coarse signal category, never an exact distance`() {
        val presence = FakeNearbyAscendedProvider(seed = 2).nearby.value.first()
        // The signal is one of the four coarse categories; there is no distance field at all.
        assertTrue(presence.signal in NearbySignal.entries)
    }

    @Test
    fun `rotating changes the anonymous handles`() {
        val provider = FakeNearbyAscendedProvider(seed = 3, initialCount = 3)
        val before = provider.nearby.value.map { it.handle }
        provider.rotate()
        val after = provider.nearby.value.map { it.handle }
        assertNotEquals("rotation should churn the anonymous ids", before, after)
    }

    @Test
    fun `the same seed is deterministic`() {
        val a = FakeNearbyAscendedProvider(seed = 7, initialCount = 4).nearby.value.map { it.handle }
        val b = FakeNearbyAscendedProvider(seed = 7, initialCount = 4).nearby.value.map { it.handle }
        assertEquals(a, b)
    }
}
