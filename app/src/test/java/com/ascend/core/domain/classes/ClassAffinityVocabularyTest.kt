package com.ascend.core.domain.classes

import com.ascend.core.model.ActivityTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Affinity summaries are derived from tags only, so they work for real and preview classes alike. */
class ClassAffinityVocabularyTest {
    @Test
    fun `berserker tags summarize to a readable strength phrase`() {
        val summary = ClassAffinityVocabulary.summary(ClassCatalog.BERSERKER)
        assertEquals("Trains heavy strength, muscle growth, and explosive power.", summary)
    }

    @Test
    fun `monk tags summarize to a bodyweight and mobility phrase`() {
        val summary = ClassAffinityVocabulary.summary(ClassCatalog.MONK.favoredTags)
        assertTrue(summary.contains("bodyweight control"))
        assertTrue(summary.contains("mobility"))
    }

    @Test
    fun `summaries render deterministically regardless of set ordering`() {
        val a = ClassAffinityVocabulary.summary(setOf(ActivityTags.EXPLOSIVE, ActivityTags.HEAVY_STRENGTH))
        val b = ClassAffinityVocabulary.summary(setOf(ActivityTags.HEAVY_STRENGTH, ActivityTags.EXPLOSIVE))
        assertEquals(a, b)
        assertEquals("Trains heavy strength and explosive power.", a)
    }

    @Test
    fun `an unknown tag falls back to a de-slugged phrase`() {
        assertEquals("free running", ClassAffinityVocabulary.phrase("FREE_RUNNING"))
    }

    @Test
    fun `readable joins use an oxford comma for three or more items`() {
        assertEquals("a, b, and c", ClassAffinityVocabulary.joinReadable(listOf("a", "b", "c")))
        assertEquals("a and b", ClassAffinityVocabulary.joinReadable(listOf("a", "b")))
        assertEquals("a", ClassAffinityVocabulary.joinReadable(listOf("a")))
    }
}
