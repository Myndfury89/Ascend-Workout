package com.ascend.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ascend.core.database.dao.AdaptiveTrainingDao
import com.ascend.core.database.dao.AttributeDao
import com.ascend.core.database.dao.BuildProfileDao
import com.ascend.core.database.dao.ClassDao
import com.ascend.core.database.dao.ExerciseDao
import com.ascend.core.database.dao.ExerciseVariationDao
import com.ascend.core.database.dao.OnboardingDao
import com.ascend.core.database.dao.PlayerDao
import com.ascend.core.database.dao.ProgressionEventDao
import com.ascend.core.database.dao.QuestDao
import com.ascend.core.database.dao.QuestIntervalDao
import com.ascend.core.database.dao.QuestTemplateDao
import com.ascend.core.database.dao.SkillDao
import com.ascend.core.database.dao.WorkoutDao
import com.ascend.core.database.dao.XpDao
import com.ascend.core.database.entity.AttributeTransactionEntity
import com.ascend.core.database.entity.BuildProfileSnapshotEntity
import com.ascend.core.database.entity.CardioPrescriptionEntity
import com.ascend.core.database.entity.ClassAffinityResultEntity
import com.ascend.core.database.entity.ClassDefinitionEntity
import com.ascend.core.database.entity.ClassHistoryEntity
import com.ascend.core.database.entity.ClassProficiencyTransactionEntity
import com.ascend.core.database.entity.ClassXpTransactionEntity
import com.ascend.core.database.entity.ExerciseEntity
import com.ascend.core.database.entity.ExercisePrescriptionEntity
import com.ascend.core.database.entity.ExerciseVariationEdgeEntity
import com.ascend.core.database.entity.ExerciseVariationEntity
import com.ascend.core.database.entity.InitialAssessmentEntity
import com.ascend.core.database.entity.InitialQuestPlanEntity
import com.ascend.core.database.entity.InitialQuestPlanItemEntity
import com.ascend.core.database.entity.OnboardingStateEntity
import com.ascend.core.database.entity.PlayerClassEntity
import com.ascend.core.database.entity.PlayerProgressEntity
import com.ascend.core.database.entity.PlayerSkillEntity
import com.ascend.core.database.entity.PlayerStatsEntity
import com.ascend.core.database.entity.ProgressionEventEntity
import com.ascend.core.database.entity.ProgressionMilestoneEntity
import com.ascend.core.database.entity.ProgressionRecommendationEntity
import com.ascend.core.database.entity.QuestCheckpointEntity
import com.ascend.core.database.entity.QuestEntity
import com.ascend.core.database.entity.QuestIntervalEntity
import com.ascend.core.database.entity.QuestIntervalProgressEntryEntity
import com.ascend.core.database.entity.QuestIntervalScheduleEntity
import com.ascend.core.database.entity.QuestObjectiveEntity
import com.ascend.core.database.entity.QuestProgressEntryEntity
import com.ascend.core.database.entity.QuestTemplateEntity
import com.ascend.core.database.entity.SkillProgressTransactionEntity
import com.ascend.core.database.entity.SkillUnlockEventEntity
import com.ascend.core.database.entity.TrainingReadinessSnapshotEntity
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.database.entity.WorkoutEntity
import com.ascend.core.database.entity.WorkoutSetEntity
import com.ascend.core.database.entity.XpTransactionEntity

@Database(
    entities = [
        UserProfileEntity::class,
        PlayerProgressEntity::class,
        PlayerStatsEntity::class,
        XpTransactionEntity::class,
        AttributeTransactionEntity::class,
        QuestEntity::class,
        QuestObjectiveEntity::class,
        QuestProgressEntryEntity::class,
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        ProgressionEventEntity::class,
        ClassDefinitionEntity::class,
        PlayerClassEntity::class,
        ClassHistoryEntity::class,
        ClassXpTransactionEntity::class,
        ClassProficiencyTransactionEntity::class,
        QuestTemplateEntity::class,
        QuestIntervalScheduleEntity::class,
        QuestIntervalEntity::class,
        QuestCheckpointEntity::class,
        QuestIntervalProgressEntryEntity::class,
        ExercisePrescriptionEntity::class,
        TrainingReadinessSnapshotEntity::class,
        ProgressionRecommendationEntity::class,
        ProgressionMilestoneEntity::class,
        ExerciseVariationEntity::class,
        ExerciseVariationEdgeEntity::class,
        CardioPrescriptionEntity::class,
        PlayerSkillEntity::class,
        SkillProgressTransactionEntity::class,
        SkillUnlockEventEntity::class,
        OnboardingStateEntity::class,
        InitialAssessmentEntity::class,
        ClassAffinityResultEntity::class,
        InitialQuestPlanEntity::class,
        InitialQuestPlanItemEntity::class,
        BuildProfileSnapshotEntity::class,
    ],
    version = 17,
    exportSchema = true,
)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao

    abstract fun xpDao(): XpDao

    abstract fun attributeDao(): AttributeDao

    abstract fun questDao(): QuestDao

    abstract fun exerciseDao(): ExerciseDao

    abstract fun workoutDao(): WorkoutDao

    abstract fun progressionEventDao(): ProgressionEventDao

    abstract fun classDao(): ClassDao

    abstract fun questTemplateDao(): QuestTemplateDao

    abstract fun questIntervalDao(): QuestIntervalDao

    abstract fun adaptiveTrainingDao(): AdaptiveTrainingDao

    abstract fun exerciseVariationDao(): ExerciseVariationDao

    abstract fun skillDao(): SkillDao

    abstract fun onboardingDao(): OnboardingDao

    abstract fun buildProfileDao(): BuildProfileDao

    companion object {
        const val NAME = "ascend.db"
    }
}
