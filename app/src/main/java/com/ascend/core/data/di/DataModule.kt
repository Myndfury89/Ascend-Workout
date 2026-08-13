package com.ascend.core.data.di

import com.ascend.core.data.repository.AscendBuildEvidenceProvider
import com.ascend.core.data.repository.BuildProfileRepositoryImpl
import com.ascend.core.data.repository.ClassRepositoryImpl
import com.ascend.core.data.repository.ExerciseVariationGraphRepositoryImpl
import com.ascend.core.data.repository.OnboardingRepositoryImpl
import com.ascend.core.data.repository.PlayerRepositoryImpl
import com.ascend.core.data.repository.ProgressionEventRepositoryImpl
import com.ascend.core.data.repository.ProgressionRecommendationRepositoryImpl
import com.ascend.core.data.repository.ProgressionRepositoryImpl
import com.ascend.core.data.repository.QuestRepositoryImpl
import com.ascend.core.data.repository.QuestTemplateRepositoryImpl
import com.ascend.core.data.repository.SkillRepositoryImpl
import com.ascend.core.data.repository.WorkoutRepositoryImpl
import com.ascend.core.domain.build.BuildEvidenceProvider
import com.ascend.core.domain.build.BuildProfileRepository
import com.ascend.core.domain.repository.ClassRepository
import com.ascend.core.domain.repository.ExerciseVariationGraphRepository
import com.ascend.core.domain.repository.OnboardingRepository
import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.core.domain.repository.ProgressionEventRepository
import com.ascend.core.domain.repository.ProgressionRecommendationRepository
import com.ascend.core.domain.repository.ProgressionRepository
import com.ascend.core.domain.repository.QuestRepository
import com.ascend.core.domain.repository.QuestTemplateRepository
import com.ascend.core.domain.repository.SkillRepository
import com.ascend.core.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindProgressionRepository(impl: ProgressionRepositoryImpl): ProgressionRepository

    @Binds
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    abstract fun bindQuestRepository(impl: QuestRepositoryImpl): QuestRepository

    @Binds
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    abstract fun bindProgressionEventRepository(impl: ProgressionEventRepositoryImpl): ProgressionEventRepository

    @Binds
    abstract fun bindClassRepository(impl: ClassRepositoryImpl): ClassRepository

    @Binds
    abstract fun bindQuestTemplateRepository(impl: QuestTemplateRepositoryImpl): QuestTemplateRepository

    @Binds
    abstract fun bindProgressionRecommendationRepository(
        impl: ProgressionRecommendationRepositoryImpl,
    ): ProgressionRecommendationRepository

    @Binds
    abstract fun bindExerciseVariationGraphRepository(impl: ExerciseVariationGraphRepositoryImpl): ExerciseVariationGraphRepository

    @Binds
    abstract fun bindSkillRepository(impl: SkillRepositoryImpl): SkillRepository

    @Binds
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    abstract fun bindBuildProfileRepository(impl: BuildProfileRepositoryImpl): BuildProfileRepository

    @Binds
    abstract fun bindBuildEvidenceProvider(impl: AscendBuildEvidenceProvider): BuildEvidenceProvider
}
