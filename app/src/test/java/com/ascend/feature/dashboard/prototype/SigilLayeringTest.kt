package com.ascend.feature.dashboard.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CP1 sigil-layering + opacity-hierarchy rules as pure data invariants. These lock the handoff rule
 * (only the ceremonial middle rotates; rings read highest) and guarantee the LEGACY presets stay
 * byte-identical to the current production sigil so the untouched Status screen never shifts.
 */
class SigilLayeringTest {
    private val rot = 123f

    @Test
    fun `progress instrumentation never rotates in either profile`() {
        SigilRotationProfile.entries.forEach { profile ->
            assertEquals("${profile.name} progress must be stationary", 0f, profile.progressRotation(rot))
        }
    }

    @Test
    fun `the ceremonial rule keeps the outer frame stationary and rotates only the middle`() {
        val ceremonial = SigilRotationProfile.CEREMONIAL_MIDDLE
        assertEquals("outer frame is stationary", 0f, ceremonial.outerMotifRotation(rot))
        assertEquals("middle drifts at the base rate", rot, ceremonial.middleRotation(rot))
        assertTrue("inner detail is a reversed parallax", ceremonial.innerParallaxRotation(rot) < 0f)
    }

    @Test
    fun `legacy rotation reproduces the current production behaviour`() {
        val legacy = SigilRotationProfile.LEGACY
        assertEquals(rot, legacy.outerMotifRotation(rot))
        assertEquals(rot * 0.5f, legacy.middleRotation(rot))
        assertEquals(rot * 0.5f, legacy.innerParallaxRotation(rot))
    }

    @Test
    fun `ring-forward opacity reads rings highest, central medium, ornament lowest`() {
        val w = SigilOpacityWeights.RingForward
        val ringsLow = minOf(w.ringPrimary, w.ringSecondary, w.playerFinalCap, w.classFinalCap)
        val ornamentHigh = maxOf(w.aura, w.outerMotif, w.connectors, w.arcs)
        assertTrue("rings outrank central geometry", ringsLow > w.centralCap)
        assertTrue("central geometry outranks ornament", w.centralCap > ornamentHigh)
    }

    @Test
    fun `legacy opacity weights match the documented production values`() {
        val w = SigilOpacityWeights.Legacy
        assertEquals(0.10f, w.aura)
        assertEquals(0.5f, w.outerMotif)
        assertEquals(0.35f, w.ringPrimary)
        assertEquals(0.70f, w.centralCap)
        assertEquals(0.85f, w.playerFinalCap)
        assertEquals(0.80f, w.classFinalCap)
        assertEquals(0.85f, w.medallionFinalCap)
    }

    @Test
    fun `the refined profile maps to the proposed rotation and opacity profiles`() {
        val c = PrototypeReviewController()
        // Default is the current, approved look.
        assertEquals(SigilRotationProfile.LEGACY, c.rotationProfile)
        assertEquals(SigilOpacityProfile.LEGACY, c.opacityProfile)
        c.sigilRefined = true
        assertEquals(SigilRotationProfile.CEREMONIAL_MIDDLE, c.rotationProfile)
        assertEquals(SigilOpacityProfile.RING_FORWARD, c.opacityProfile)
    }
}
