package com.ascend.core.domain.repository

import com.ascend.core.model.QuestTemplate
import kotlinx.coroutines.flow.Flow

/**
 * Reads the Daily Quest templates (built‑in seed + user‑saved) and stores custom
 * templates. Template ranges are configurable data — never hardcoded in UI.
 */
interface QuestTemplateRepository {
    suspend fun seed()

    fun observeTemplates(): Flow<List<QuestTemplate>>

    suspend fun getTemplates(): List<QuestTemplate>

    suspend fun getTemplate(id: String): QuestTemplate?

    /** Save a customized quest as a reusable (non‑built‑in) template. */
    suspend fun saveCustomTemplate(template: QuestTemplate)
}
