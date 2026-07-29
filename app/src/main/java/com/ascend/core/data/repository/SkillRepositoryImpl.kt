package com.ascend.core.data.repository

import androidx.room.withTransaction
import com.ascend.core.common.newId
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.SkillDao
import com.ascend.core.database.entity.PlayerSkillEntity
import com.ascend.core.database.entity.SkillProgressTransactionEntity
import com.ascend.core.database.entity.SkillUnlockEventEntity
import com.ascend.core.domain.repository.SkillAwardResult
import com.ascend.core.domain.repository.SkillRepository
import com.ascend.core.domain.skill.SkillCatalog
import com.ascend.core.domain.skill.SkillClassAffinityResolver
import com.ascend.core.domain.skill.SkillEligibilityEngine
import com.ascend.core.domain.skill.SkillLevelCalculator
import com.ascend.core.domain.skill.SkillProgressCalculator
import com.ascend.core.domain.skill.TransferableSkillResolver
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.PlayerSkill
import com.ascend.core.model.SkillDefinition
import com.ascend.core.model.SkillEligibility
import com.ascend.core.model.SkillEvidence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillRepositoryImpl
    @Inject
    constructor(
        private val db: AscendDatabase,
        private val dao: SkillDao,
        private val levelCalculator: SkillLevelCalculator,
        private val progressCalculator: SkillProgressCalculator,
        private val eligibilityEngine: SkillEligibilityEngine,
        private val affinityResolver: SkillClassAffinityResolver,
        private val transferableResolver: TransferableSkillResolver,
    ) : SkillRepository {
        private val definitionsById: Map<String, SkillDefinition> = SkillCatalog.ALL.associateBy { it.id }

        private fun now() = System.currentTimeMillis()

        override fun definitions(): List<SkillDefinition> = SkillCatalog.ALL

        override suspend fun playerSkills(userId: String): List<PlayerSkill> = dao.getPlayerSkills(userId).mapNotNull { toDomain(it) }

        override fun observePlayerSkills(userId: String): Flow<List<PlayerSkill>> =
            dao.observePlayerSkills(userId).map { list -> list.mapNotNull { toDomain(it) } }

        override suspend fun playerSkill(
            userId: String,
            skillId: String,
        ): PlayerSkill? = dao.getPlayerSkill(userId, skillId)?.let { toDomain(it) }

        override suspend fun unlock(
            userId: String,
            skillId: String,
            evidence: List<String>,
            sourceType: String,
            sourceId: String,
        ): Boolean =
            db.withTransaction {
                definitionsById[skillId] ?: return@withTransaction false
                val ts = now()
                val row =
                    dao.insertUnlockEvent(
                        SkillUnlockEventEntity(
                            id = newId(),
                            userId = userId,
                            skillId = skillId,
                            unlockedAt = ts,
                            triggeringSourceType = sourceType,
                            triggeringSourceId = sourceId,
                            evidence = evidence.joinToString("|"),
                            createdAt = ts,
                        ),
                    )
                if (row == -1L) return@withTransaction false // already unlocked — idempotent
                val existing = dao.getPlayerSkill(userId, skillId)
                dao.upsertPlayerSkill(
                    PlayerSkillEntity(
                        userId = userId,
                        skillId = skillId,
                        unlocked = true,
                        level = (existing?.level ?: 1).coerceAtLeast(1),
                        skillXp = existing?.skillXp ?: 0,
                        unlockedAt = existing?.unlockedAt ?: ts,
                        updatedAt = ts,
                    ),
                )
                true
            }

        override suspend fun awardXp(
            userId: String,
            skillId: String,
            evidenceUnits: Int,
            sourceType: String,
            sourceId: String,
            classAffinity: Double,
        ): SkillAwardResult =
            db.withTransaction {
                val def = definitionsById[skillId] ?: return@withTransaction SkillAwardResult(false, 0, 0, false, false)
                val amount = progressCalculator.xpFor(def, evidenceUnits, classAffinity)
                val inserted =
                    dao.insertProgress(
                        SkillProgressTransactionEntity(
                            id = newId(),
                            userId = userId,
                            skillId = skillId,
                            amount = amount,
                            sourceType = sourceType,
                            sourceId = sourceId,
                            createdAt = now(),
                        ),
                    )
                if (inserted == -1L) {
                    // Idempotent: this exact source already granted XP — nothing added.
                    val current = dao.getPlayerSkill(userId, skillId)
                    val level = current?.let { levelCalculator.resolve(it.skillXp, def.maxLevel).level } ?: 1
                    return@withTransaction SkillAwardResult(false, 0, level, false, alreadyAwarded = true)
                }
                val existing = dao.getPlayerSkill(userId, skillId)
                val oldLevel = existing?.let { levelCalculator.resolve(it.skillXp, def.maxLevel).level } ?: 1
                val newXp = (existing?.skillXp ?: 0) + amount
                val newLevel = levelCalculator.resolve(newXp, def.maxLevel).level
                dao.upsertPlayerSkill(
                    PlayerSkillEntity(
                        userId = userId,
                        skillId = skillId,
                        unlocked = existing?.unlocked ?: true,
                        level = newLevel,
                        skillXp = newXp,
                        unlockedAt = existing?.unlockedAt ?: now(),
                        updatedAt = now(),
                    ),
                )
                SkillAwardResult(true, amount, newLevel, leveledUp = newLevel > oldLevel, alreadyAwarded = false)
            }

        override suspend fun evaluate(
            userId: String,
            evidence: SkillEvidence,
            classDef: ClassDefinition?,
        ): List<SkillEligibility> {
            val unlockedIds = dao.getPlayerSkills(userId).filter { it.unlocked }.map { it.skillId }.toSet()
            return SkillCatalog.ALL.map { def ->
                eligibilityEngine.evaluate(def, evidence, affinityResolver.affinity(def, classDef), unlocked = def.id in unlockedIds)
            }
        }

        override suspend fun applyClassSwitch(
            userId: String,
            newClassId: String?,
        ): Int =
            db.withTransaction {
                val current = playerSkills(userId)
                val retained = transferableResolver.retainedAfterSwitch(current, definitionsById, newClassId).map { it.skillId }.toSet()
                val removed = current.filter { it.skillId !in retained }
                removed.forEach { dao.deletePlayerSkill(userId, it.skillId) }
                removed.size
            }

        private fun toDomain(entity: PlayerSkillEntity): PlayerSkill? {
            val def = definitionsById[entity.skillId] ?: return null
            val state = levelCalculator.resolve(entity.skillXp, def.maxLevel)
            return PlayerSkill(
                userId = entity.userId,
                skillId = entity.skillId,
                unlocked = entity.unlocked,
                level = state.level,
                skillXp = entity.skillXp,
                currentLevelXp = state.currentLevelXp,
                xpToNextLevel = state.xpToNextLevel,
                unlockedAt = entity.unlockedAt,
            )
        }
    }
