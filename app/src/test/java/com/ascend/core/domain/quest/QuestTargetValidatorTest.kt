package com.ascend.core.domain.quest

import com.ascend.core.domain.usecase.SeedQuestTemplatesUseCase
import com.ascend.core.model.QuestTargetValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestTargetValidatorTest {
    private val validator = QuestTargetValidator()
    private val pushups = SeedQuestTemplatesUseCase.CATALOG.first { it.id == "tmpl-pushups" } // 25..500, safety 300

    @Test
    fun `minimum and maximum targets are accepted`() {
        assertTrue(validator.validate(pushups, 25) is QuestTargetValidation.Accepted)
        assertTrue(validator.validate(pushups, 500) is QuestTargetValidation.Accepted)
    }

    @Test
    fun `a custom in-range target below the safety threshold needs no confirmation`() {
        val result = validator.validate(pushups, 125) as QuestTargetValidation.Accepted
        assertFalse(result.requiresConfirmation)
        assertEquals(null, result.warning)
    }

    @Test
    fun `below minimum is rejected with the allowed range`() {
        val result = validator.validate(pushups, 24) as QuestTargetValidation.Rejected
        assertEquals(QuestTargetValidation.Rejected.Reason.BELOW_MINIMUM, result.reason)
        assertEquals(25..500, result.allowedRange)
    }

    @Test
    fun `above maximum is rejected`() {
        val result = validator.validate(pushups, 501) as QuestTargetValidation.Rejected
        assertEquals(QuestTargetValidation.Rejected.Reason.ABOVE_MAXIMUM, result.reason)
    }

    @Test
    fun `a high target above the safety threshold is accepted but needs confirmation`() {
        val result = validator.validate(pushups, 350) as QuestTargetValidation.Accepted
        assertTrue(result.requiresConfirmation)
        assertTrue(result.warning!!.isNotBlank())
    }

    @Test
    fun `a large jump over the recent baseline needs confirmation`() {
        // 100 is under the 300 safety threshold, but far above a recent baseline of 50.
        val result = validator.validate(pushups, 100, recentBaseline = 50) as QuestTargetValidation.Accepted
        assertTrue(result.requiresConfirmation)
    }
}
