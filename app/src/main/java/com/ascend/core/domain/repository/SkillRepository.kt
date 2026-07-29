package com.ascend.core.domain.repository

import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.PlayerSkill
import com.ascend.core.model.SkillDefinition
import com.ascend.core.model.SkillEligibility
import com.ascend.core.model.SkillEvidence
import kotlinx.coroutines.flow.Flow

/** The outcome of an idempotent Skill-XP award. */
data class SkillAwardResult(
    val awarded: Boolean,
    val amount: Long,
    val newLevel: Int,
    val leveledUp: Boolean,
    val alreadyAwarded: Boolean,
)

/**
 * Persists player Skill state and drives unlock / progress / eligibility. Skill definitions are
 * served from the seed catalog. Unlocks happen from **real activity only** and are recorded once;
 * Skill‑XP grants are idempotent per source. Class affinity may scale XP but never gates access.
 */
interface SkillRepository {
    fun definitions(): List<SkillDefinition>

    suspend fun playerSkills(userId: String): List<PlayerSkill>

    fun observePlayerSkills(userId: String): Flow<List<PlayerSkill>>

    suspend fun playerSkill(
        userId: String,
        skillId: String,
    ): PlayerSkill?

    /** Unlock a Skill from proven evidence; idempotent (a second call returns false). */
    suspend fun unlock(
        userId: String,
        skillId: String,
        evidence: List<String>,
        sourceType: String,
        sourceId: String,
    ): Boolean

    /** Award Skill XP for a source; idempotent per `(skill, sourceType, sourceId)`. */
    suspend fun awardXp(
        userId: String,
        skillId: String,
        evidenceUnits: Int,
        sourceType: String,
        sourceId: String,
        classAffinity: Double,
    ): SkillAwardResult

    /** Evaluate every Skill's eligibility against real-activity [evidence] for the user's class. */
    suspend fun evaluate(
        userId: String,
        evidence: SkillEvidence,
        classDef: ClassDefinition?,
    ): List<SkillEligibility>

    /** Apply a class switch: transferable Skills stay, class-exclusive ones may be removed. Returns removed count. */
    suspend fun applyClassSwitch(
        userId: String,
        newClassId: String?,
    ): Int
}
