package com.ascend.core.domain.skill

import com.ascend.core.domain.classes.ClassCatalog
import com.ascend.core.model.PlayerSkill
import com.ascend.core.model.SkillEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure tests for the Skills & Techniques catalog, eligibility, affinity, and progression. */
class SkillCatalogTest {
    private val engine = SkillEligibilityEngine()
    private val affinity = SkillClassAffinityResolver()
    private val progress = SkillProgressCalculator()
    private val levels = SkillLevelCalculator()
    private val effects = SkillEffectResolver()
    private val transferable = TransferableSkillResolver()

    private fun def(id: String) = SkillCatalog.byId(id)!!

    // ---- class gating: none of the four initial Skills has a hard class requirement ----

    @Test
    fun `no initial skill has a hard class requirement`() {
        SkillCatalog.ALL.forEach { skill ->
            assertFalse("${skill.id} must not hard-require a class", skill.hasHardClassRequirement)
            assertTrue("${skill.id} definition classIds must be null/empty", skill.classIds.isNullOrEmpty())
            skill.prerequisites.forEach { p ->
                assertTrue("${skill.id} prerequisite must not hard-require a class", p.classIds.isNullOrEmpty())
            }
        }
    }

    // ---- unlock eligibility from real activity ----

    @Test
    fun `perception unlocks at a valid 15-minute cardio session but not at 14 minutes`() {
        assertTrue(eligible(SkillCatalog.PERCEPTION, SkillEvidence(cardioContinuousSeconds = 15 * 60)))
        assertFalse(eligible(SkillCatalog.PERCEPTION, SkillEvidence(cardioContinuousSeconds = 14 * 60)))
    }

    @Test
    fun `a disqualifying safety event blocks a completion-gated unlock`() {
        val evidence = SkillEvidence(cardioContinuousSeconds = 15 * 60, disqualifyingSafetyEvent = true)
        assertFalse(eligible(SkillCatalog.PERCEPTION, evidence))
    }

    @Test
    fun `strength boost unlocks from a verified PR or a proven load progression`() {
        assertTrue(eligible(SkillCatalog.STRENGTH_BOOST, SkillEvidence(strengthPersonalRecords = 1)))
        assertTrue(eligible(SkillCatalog.STRENGTH_BOOST, SkillEvidence(loadProgressionsProven = 1)))
        assertFalse(eligible(SkillCatalog.STRENGTH_BOOST, SkillEvidence()))
    }

    @Test
    fun `a magician can unlock strength boost — class never blocks access`() {
        val magicianAffinity = affinity.affinity(def(SkillCatalog.STRENGTH_BOOST), ClassCatalog.MAGICIAN)
        val result =
            engine.evaluate(
                def(SkillCatalog.STRENGTH_BOOST),
                SkillEvidence(strengthPersonalRecords = 1),
                magicianAffinity,
                unlocked = false,
            )
        assertTrue("eligibility must not depend on class", result.eligible)
    }

    @Test
    fun `breath control unlocks from sustained training`() {
        assertTrue(eligible(SkillCatalog.BREATH_CONTROL, SkillEvidence(cardioContinuousSeconds = 20 * 60)))
        assertFalse(eligible(SkillCatalog.BREATH_CONTROL, SkillEvidence(cardioContinuousSeconds = 17 * 60)))
    }

    @Test
    fun `body awareness unlocks from bodyweight progression, tempo, assistance, or mobility`() {
        assertTrue(eligible(SkillCatalog.BODY_AWARENESS, SkillEvidence(bodyweightVariationAdvances = 1)))
        assertTrue(eligible(SkillCatalog.BODY_AWARENESS, SkillEvidence(tempoProgressions = 1)))
        assertTrue(eligible(SkillCatalog.BODY_AWARENESS, SkillEvidence(assistanceReductions = 1)))
        assertTrue(eligible(SkillCatalog.BODY_AWARENESS, SkillEvidence(mobilityOrBalanceMilestones = 1)))
        assertFalse(eligible(SkillCatalog.BODY_AWARENESS, SkillEvidence()))
    }

    // ---- class affinity changes XP, never access ----

    @Test
    fun `class affinity changes skill XP but not eligibility`() {
        val evidence = SkillEvidence(cardioContinuousSeconds = 15 * 60)
        // Eligibility identical regardless of affinity.
        assertEquals(
            engine.evaluate(def(SkillCatalog.PERCEPTION), evidence, 0.0, false).eligible,
            engine.evaluate(def(SkillCatalog.PERCEPTION), evidence, 1.0, false).eligible,
        )
        // XP scales with affinity.
        val neutralXp = progress.xpFor(def(SkillCatalog.PERCEPTION), 1, classAffinity = 0.0)
        val favoredXp = progress.xpFor(def(SkillCatalog.PERCEPTION), 1, classAffinity = 1.0)
        assertTrue("favored class earns more Skill XP", favoredXp > neutralXp)
    }

    @Test
    fun `affinity is tag-driven and neutral for a non-matching or absent class`() {
        assertTrue(affinity.affinity(def(SkillCatalog.STRENGTH_BOOST), ClassCatalog.BERSERKER) > 0.0)
        assertEquals(0.0, affinity.affinity(def(SkillCatalog.STRENGTH_BOOST), ClassCatalog.MAGICIAN), 1e-9)
        assertEquals(0.0, affinity.affinity(def(SkillCatalog.STRENGTH_BOOST), null), 1e-9)
    }

    @Test
    fun `off-class skill effects remain usable`() {
        val resolved = effects.resolve(def(SkillCatalog.STRENGTH_BOOST), level = 1, classAffinity = 0.0)
        assertTrue(resolved.isNotEmpty())
        assertTrue("every effect stays usable off-class", resolved.all { it.magnitude > 0.0 })
    }

    // ---- levels + transfer ----

    @Test
    fun `skill level increases with skill XP`() {
        assertEquals(1, levels.resolve(0, maxLevel = 10).level)
        assertEquals(2, levels.resolve(100, maxLevel = 10).level)
        assertEquals(3, levels.resolve(250, maxLevel = 10).level)
        assertEquals(10, levels.resolve(999_999, maxLevel = 10).level)
    }

    @Test
    fun `switching classes preserves transferable skills`() {
        val playerSkills =
            SkillCatalog.ALL.map {
                PlayerSkill("u1", it.id, unlocked = true, level = 1, skillXp = 0, currentLevelXp = 0, xpToNextLevel = 100, unlockedAt = 0)
            }
        val defsById = SkillCatalog.ALL.associateBy { it.id }
        val retained = transferable.retainedAfterSwitch(playerSkills, defsById, newClassId = "magician")
        assertEquals("all initial skills are transferable", playerSkills.size, retained.size)
    }

    private fun eligible(
        skillId: String,
        evidence: SkillEvidence,
    ): Boolean = engine.evaluate(def(skillId), evidence, classAffinity = 0.0, unlocked = false).eligible
}
