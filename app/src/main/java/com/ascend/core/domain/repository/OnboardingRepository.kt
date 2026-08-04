package com.ascend.core.domain.repository

import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.ClassAffinityResult
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.InitialQuestPlan
import com.ascend.core.model.onboarding.OnboardingState
import com.ascend.core.model.onboarding.SocialPrivacyDefaults
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for onboarding + the provisional initial assessment. Reuses the existing user_profile
 * (via the player DAO) for stable profile attributes (height, age-safety category, social/privacy
 * defaults, onboarding completion) and stores the resumable state, provisional assessment,
 * class-affinity result, and initial plan in their own tables. Nothing here awards progression.
 */
interface OnboardingRepository {
    fun observeState(userId: String): Flow<OnboardingState?>

    suspend fun getState(userId: String): OnboardingState?

    suspend fun saveState(
        userId: String,
        state: OnboardingState,
    )

    /** Persists the provisional assessment; canonical height is written to the profile, not here. */
    suspend fun saveAssessment(assessment: InitialAssessment)

    suspend fun getAssessment(userId: String): InitialAssessment?

    suspend fun saveAffinity(
        userId: String,
        result: ClassAffinityResult,
        createdAt: Long,
    )

    suspend fun getAffinity(userId: String): ClassAffinityResult?

    suspend fun savePlan(plan: InitialQuestPlan)

    suspend fun getPlan(userId: String): InitialQuestPlan?

    /** Writes the age-safety category + social/privacy defaults onto the profile. */
    suspend fun saveSafetyProfile(
        userId: String,
        category: AgeSafetyCategory,
        defaults: SocialPrivacyDefaults,
    )

    /** Marks onboarding complete on the profile with the given version (never awards anything). */
    suspend fun markCompleted(
        userId: String,
        version: Int,
    )
}
