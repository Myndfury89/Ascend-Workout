package com.ascend.core.data.repository

import com.ascend.core.data.mapper.toDomain
import com.ascend.core.data.mapper.toEntity
import com.ascend.core.database.dao.QuestTemplateDao
import com.ascend.core.domain.repository.QuestTemplateRepository
import com.ascend.core.domain.usecase.SeedQuestTemplatesUseCase
import com.ascend.core.model.QuestTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestTemplateRepositoryImpl
    @Inject
    constructor(
        private val questTemplateDao: QuestTemplateDao,
    ) : QuestTemplateRepository {
        override suspend fun seed() {
            if (questTemplateDao.count() > 0) return
            val ts = System.currentTimeMillis()
            questTemplateDao.upsertAll(SeedQuestTemplatesUseCase.CATALOG.map { it.toEntity(ts) })
        }

        override fun observeTemplates(): Flow<List<QuestTemplate>> =
            questTemplateDao.observeAll().map {
                    list ->
                list.takeIf { it.isNotEmpty() }?.map { it.toDomain() } ?: SeedQuestTemplatesUseCase.CATALOG
            }

        override suspend fun getTemplates(): List<QuestTemplate> =
            questTemplateDao.getAll().takeIf { it.isNotEmpty() }?.map { it.toDomain() } ?: SeedQuestTemplatesUseCase.CATALOG

        override suspend fun getTemplate(id: String): QuestTemplate? =
            questTemplateDao.getById(id)?.toDomain() ?: SeedQuestTemplatesUseCase.CATALOG.firstOrNull { it.id == id }

        override suspend fun saveCustomTemplate(template: QuestTemplate) {
            questTemplateDao.upsert(template.copy(isBuiltIn = false).toEntity(System.currentTimeMillis()))
        }
    }
