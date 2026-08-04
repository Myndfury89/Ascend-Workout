package com.ascend.core.domain.onboarding

import com.ascend.core.model.onboarding.AgeRange
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.SocialVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Age-safety policy: minimum age 13, four bands, conservative + private defaults for minors. */
class AgeSafetyClassifierTest {
    private val classifier = AgeSafetyClassifier()

    @Test
    fun `age ranges map to the four bands`() {
        assertEquals(AgeSafetyCategory.BELOW_MINIMUM, classifier.classify(AgeRange.UNDER_13))
        assertEquals(AgeSafetyCategory.MINOR_YOUNGER, classifier.classify(AgeRange.AGE_13_15))
        assertEquals(AgeSafetyCategory.MINOR_OLDER, classifier.classify(AgeRange.AGE_16_17))
        assertEquals(AgeSafetyCategory.ADULT, classifier.classify(AgeRange.AGE_18_24))
        assertEquals(AgeSafetyCategory.ADULT, classifier.classify(AgeRange.AGE_50_PLUS))
        assertEquals(AgeSafetyCategory.NOT_PROVIDED, classifier.classify(AgeRange.PREFER_NOT_TO_SAY))
    }

    @Test
    fun `below minimum age cannot onboard`() {
        assertFalse(classifier.canOnboard(AgeSafetyCategory.BELOW_MINIMUM))
        assertTrue(classifier.canOnboard(AgeSafetyCategory.MINOR_YOUNGER))
        assertTrue(classifier.canOnboard(AgeSafetyCategory.ADULT))
        assertTrue(classifier.canOnboard(AgeSafetyCategory.NOT_PROVIDED))
    }

    @Test
    fun `minors and not-provided use conservative defaults and are private with presence off`() {
        listOf(AgeSafetyCategory.MINOR_YOUNGER, AgeSafetyCategory.MINOR_OLDER, AgeSafetyCategory.NOT_PROVIDED).forEach { cat ->
            assertTrue("$cat should be conservative", classifier.usesConservativeDefaults(cat))
            val d = classifier.socialDefaults(cat)
            assertEquals(SocialVisibility.PRIVATE, d.socialVisibility)
            assertFalse("party presence off for $cat", d.partyPresenceEnabled)
            assertFalse("stranger discovery off for $cat", d.strangerDiscoveryEnabled)
            assertFalse("$cat may not enable social features", d.mayEnableSocialFeatures)
        }
    }

    @Test
    fun `adults default private with presence off but may opt in later`() {
        val d = classifier.socialDefaults(AgeSafetyCategory.ADULT)
        assertEquals(SocialVisibility.PRIVATE, d.socialVisibility)
        assertFalse(d.partyPresenceEnabled)
        assertFalse(d.strangerDiscoveryEnabled)
        assertTrue("adults are eligible to opt in", d.mayEnableSocialFeatures)
        assertFalse(classifier.usesConservativeDefaults(AgeSafetyCategory.ADULT))
    }

    @Test
    fun `becoming an adult does not auto-enable any presence or discovery`() {
        // Classifying as adult only changes eligibility; the defaults themselves stay OFF.
        val d = classifier.socialDefaults(AgeSafetyCategory.ADULT)
        assertFalse("turning 18 must not auto-enable party presence", d.partyPresenceEnabled)
        assertFalse("turning 18 must not auto-enable stranger discovery", d.strangerDiscoveryEnabled)
    }
}
