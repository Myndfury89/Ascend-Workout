package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ascend.core.database.entity.ClassAffinityResultEntity
import com.ascend.core.database.entity.InitialAssessmentEntity
import com.ascend.core.database.entity.InitialQuestPlanEntity
import com.ascend.core.database.entity.InitialQuestPlanItemEntity
import com.ascend.core.database.entity.OnboardingStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OnboardingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(entity: OnboardingStateEntity)

    @Query("SELECT * FROM onboarding_state WHERE userId = :userId")
    suspend fun getState(userId: String): OnboardingStateEntity?

    @Query("SELECT * FROM onboarding_state WHERE userId = :userId")
    fun observeState(userId: String): Flow<OnboardingStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssessment(entity: InitialAssessmentEntity)

    @Query("SELECT * FROM initial_assessment WHERE userId = :userId")
    suspend fun getAssessment(userId: String): InitialAssessmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAffinity(entity: ClassAffinityResultEntity)

    @Query("SELECT * FROM class_affinity_result WHERE userId = :userId")
    suspend fun getAffinity(userId: String): ClassAffinityResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(entity: InitialQuestPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlanItems(items: List<InitialQuestPlanItemEntity>)

    @Query("DELETE FROM initial_quest_plan_item WHERE planUserId = :userId")
    suspend fun clearPlanItems(userId: String)

    @Query("SELECT * FROM initial_quest_plan WHERE userId = :userId")
    suspend fun getPlan(userId: String): InitialQuestPlanEntity?

    @Query("SELECT * FROM initial_quest_plan_item WHERE planUserId = :userId ORDER BY sortOrder ASC")
    suspend fun getPlanItems(userId: String): List<InitialQuestPlanItemEntity>

    /** Replace the whole plan (header + items) atomically so a re-generated plan never half-writes. */
    @Transaction
    suspend fun replacePlan(
        plan: InitialQuestPlanEntity,
        items: List<InitialQuestPlanItemEntity>,
    ) {
        upsertPlan(plan)
        clearPlanItems(plan.userId)
        upsertPlanItems(items)
    }
}
