package com.ascend.feature.dungeon.prototype

import com.ascend.core.model.MemberActivityState
import com.ascend.core.model.WorkoutActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DungeonPrototypeControllerTest {
    private fun controller() = DungeonPrototypeController(FakeNearbyAscendedProvider(seed = 5))

    private fun presence(handle: String) = FakeNearbyPresence(handle, NearbySignal.NEARBY, classId = null)

    // ---- explicit join is required ----

    @Test
    fun `a party starts empty and requires an explicit join`() {
        val c = controller()
        assertTrue(c.party.isEmpty())
        c.join(presence("Ascended-AAAA"))
        assertEquals(1, c.party.size)
    }

    @Test
    fun `joining is capped at four members and de-duplicated`() {
        val c = controller()
        repeat(6) { c.join(presence("Ascended-$it")) }
        assertEquals(DungeonPrototypeController.MAX_PARTY, c.party.size)
        c.join(presence("Ascended-0")) // duplicate handle
        assertEquals(DungeonPrototypeController.MAX_PARTY, c.party.size)
    }

    @Test
    fun `leaving removes a member`() {
        val c = controller()
        c.join(presence("Ascended-AAAA"))
        val id = c.party.first().id
        c.leave(id)
        assertTrue(c.party.isEmpty())
    }

    @Test
    fun `ready check requires every member ready`() {
        val c = controller()
        c.join(presence("Ascended-AAAA"))
        c.join(presence("Ascended-BBBB"))
        assertFalse(c.allReady)
        c.party.toList().forEach { c.toggleReady(it.id) }
        assertTrue(c.allReady)
    }

    // ---- encounter + contributions ----

    @Test
    fun `an active member's trigger deals damage and logs a contribution`() {
        val c = start1()
        val before = c.combat!!.enemyHealth
        c.trigger(c.party.first().id, WorkoutActionType.PERSONAL_RECORD)
        assertTrue("damage was dealt", c.combat!!.enemyHealth < before)
        assertTrue(c.timeline.any { it.isContribution })
    }

    @Test
    fun `a safety-paused member cannot contribute and is noted neutrally`() {
        val c = start1()
        val id = c.party.first().id
        c.safetyStop(id)
        val before = c.combat!!.enemyHealth
        c.trigger(id, WorkoutActionType.PERSONAL_RECORD)
        assertEquals("no damage while safety-paused", before, c.combat!!.enemyHealth)
        assertTrue(c.timeline.any { !it.isContribution && it.text.contains("paused") })
    }

    @Test
    fun `a disconnected member cannot contribute until reconnected`() {
        val c = start1()
        val id = c.party.first().id
        c.setActivityState(id, MemberActivityState.DISCONNECTED)
        val before = c.combat!!.enemyHealth
        c.trigger(id, WorkoutActionType.PERSONAL_RECORD)
        assertEquals(before, c.combat!!.enemyHealth)
        // Reconnect.
        c.setActivityState(id, MemberActivityState.ACTIVE)
        c.trigger(id, WorkoutActionType.PERSONAL_RECORD)
        assertTrue(c.combat!!.enemyHealth < before)
    }

    @Test
    fun `an idle member past the timeout cannot contribute`() {
        val c = start1()
        val id = c.party.first().id
        c.advanceTimer(600) // 10 minutes — beyond the inactivity timeout
        val before = c.combat!!.enemyHealth
        c.trigger(id, WorkoutActionType.PERSONAL_RECORD)
        // The encounter may already have failed on the budget; either way no contribution landed.
        assertEquals(before, c.combat!!.enemyHealth)
    }

    @Test
    fun `enough contributions complete the encounter with a reward`() {
        val c = start1()
        val id = c.party.first().id
        repeat(20) { if (c.phase == DungeonPrototypePhase.IN_ENCOUNTER) c.trigger(id, WorkoutActionType.PERSONAL_RECORD) }
        assertEquals(DungeonPrototypePhase.VICTORY, c.phase)
        assertTrue(c.reward!!.playerXp > 0)
    }

    @Test
    fun `running past the time budget fails the encounter`() {
        val c = start1()
        c.advanceTimer(10_000) // far beyond the budget
        assertEquals(DungeonPrototypePhase.DEFEAT, c.phase)
    }

    @Test
    fun `more members scale total enemy health up`() {
        val small = start(1)
        val large = start(4)
        assertTrue(large.scaling!!.scaledEnemyHealth > small.scaling!!.scaledEnemyHealth)
    }

    private fun start1(): DungeonPrototypeController = start(1)

    private fun start(members: Int): DungeonPrototypeController {
        val c = controller()
        c.selectedDungeonId = "dungeon-serpent-tunnels"
        repeat(members) { c.join(presence("Ascended-M$it")) }
        c.startEncounter()
        return c
    }
}
