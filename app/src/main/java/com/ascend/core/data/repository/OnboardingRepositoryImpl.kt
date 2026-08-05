package com.ascend.core.data.repository

import com.ascend.core.data.mapper.toDomain
import com.ascend.core.data.mapper.toEntity
import com.ascend.core.data.mapper.toItemEntities
import com.ascend.core.database.dao.OnboardingDao
import com.ascend.core.database.dao.PlayerDao
import com.ascend.core.domain.repository.OnboardingRepository
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.ClassAffinityResult
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.InitialQuestPlan
import com.ascend.core.model.onboarding.OnboardingState
import com.ascend.core.model.onboarding.SocialPrivacyDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed onboarding persistence. Stable profile attributes (height, age-safety category,
 * social/privacy defaults, completion) are written to the existing user_profile via [PlayerDao]; the
 * resumable state, provisional assessment, affinity, and plan live in their own tables via
 * [OnboardingDao]. All writes are provisional/self-reported and award nothing.
 */
class OnboardingRepositoryImpl(
    private val onboardingDao: OnboardingDao,
    private val playerDao: PlayerDao,
    private val clock: () -> Long,
) : OnboardingRepository {
    // Hilt entry point; the primary constructor's injectable clock is test-only.
    @Inject
    constructor(
        onboardingDao: OnboardingDao,
        playerDao: PlayerDao,
    ) : this(onboardingDao, playerDao, { System.currentTimeMillis() })

    override fun observeCompleted(userId: String): Flow<Boolean> = playerDao.observeOnboardingCompleted(userId).map { it == true }

    override suspend fun saveDisplayName(
        userId: String,
        displayName: String,
    ) = playerDao.updateDisplayName(userId, displayName, clock())

    override fun observeState(userId: String): Flow<OnboardingState?> = onboardingDao.observeState(userId).map { it?.toDomain() }

    override suspend fun getState(userId: String): OnboardingState? = onboardingDao.getState(userId)?.toDomain()

    override suspend fun saveState(
        userId: String,
        state: OnboardingState,
    ) = onboardingDao.upsertState(state.toEntity(userId))

    override suspend fun saveAssessment(assessment: InitialAssessment) {
        onboardingDao.upsertAssessment(assessment.toEntity())
        // Canonical height is a stable profile attribute, not stored in the assessment table.
        assessment.bodyMetrics.heightCm?.let { playerDao.updateHeight(assessment.userId, it, clock()) }
    }

    override suspend fun getAssessment(userId: String): InitialAssessment? {
        val entity = onboardingDao.getAssessment(userId) ?: return null
        val heightCm = playerDao.getProfile(userId)?.heightCm
        return entity.toDomain(heightCm)
    }

    override suspend fun saveAffinity(
        userId: String,
        result: ClassAffinityResult,
        createdAt: Long,
    ) = onboardingDao.upsertAffinity(result.toEntity(userId, createdAt))

    override suspend fun getAffinity(userId: String): ClassAffinityResult? = onboardingDao.getAffinity(userId)?.toDomain()

    override suspend fun savePlan(plan: InitialQuestPlan) = onboardingDao.replacePlan(plan.toEntity(), plan.toItemEntities())

    override suspend fun getPlan(userId: String): InitialQuestPlan? {
        val header = onboardingDao.getPlan(userId) ?: return null
        return header.toDomain(onboardingDao.getPlanItems(userId))
    }

    override suspend fun saveSafetyProfile(
        userId: String,
        category: AgeSafetyCategory,
        defaults: SocialPrivacyDefaults,
    ) = playerDao.updateSafetyProfile(
        userId = userId,
        ageSafetyCategory = category.name,
        socialVisibility = defaults.socialVisibility.name,
        partyPresenceEnabled = defaults.partyPresenceEnabled,
        strangerDiscoveryEnabled = defaults.strangerDiscoveryEnabled,
        updatedAt = clock(),
    )

    override suspend fun markCompleted(
        userId: String,
        version: Int,
    ) = playerDao.markOnboardingComplete(userId, version, clock())
}
