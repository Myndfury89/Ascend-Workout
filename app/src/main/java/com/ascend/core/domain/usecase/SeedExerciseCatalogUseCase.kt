package com.ascend.core.domain.usecase

import com.ascend.core.database.dao.ExerciseDao
import com.ascend.core.database.entity.ExerciseEntity
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ObjectiveType
import javax.inject.Inject

/**
 * Seeds the built-in exercise library (idempotent). Uses stable ids so an upsert
 * refreshes definitions without duplicating rows; runs only when the catalog is
 * empty to avoid clobbering user edits later.
 */
class SeedExerciseCatalogUseCase
    @Inject
    constructor(
        private val exerciseDao: ExerciseDao,
    ) {
        suspend operator fun invoke() {
            if (exerciseDao.count() > 0) return
            val ts = System.currentTimeMillis()
            exerciseDao.upsertAll(CATALOG.map { it.toEntity(ts) })
        }

        private data class Seed(
            val id: String,
            val name: String,
            val category: String,
            val primaryAttribute: AttributeType,
            val measurementType: ObjectiveType,
            val defaultUnit: String,
            val tags: String,
            val isWeighted: Boolean = false,
            val preferredSetSize: Int? = null,
        ) {
            fun toEntity(ts: Long) =
                ExerciseEntity(
                    id = id,
                    name = name,
                    category = category,
                    primaryAttribute = primaryAttribute.name,
                    measurementType = measurementType.name,
                    defaultUnit = defaultUnit,
                    isWeighted = isWeighted,
                    preferredSetSize = preferredSetSize,
                    tags = tags,
                    isBuiltIn = true,
                    createdAt = ts,
                    updatedAt = ts,
                )
        }

        private companion object {
            val CATALOG =
                listOf(
                    Seed(
                        "ex-pushup",
                        "Push-ups",
                        "Bodyweight",
                        AttributeType.STRENGTH,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        "BODYWEIGHT,MUSCULAR_ENDURANCE",
                        preferredSetSize = 20,
                    ),
                    Seed(
                        "ex-pullup",
                        "Pull-ups",
                        "Bodyweight",
                        AttributeType.STRENGTH,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        "BODYWEIGHT,MUSCULAR_ENDURANCE",
                        preferredSetSize = 8,
                    ),
                    Seed(
                        "ex-squat",
                        "Bodyweight Squats",
                        "Bodyweight",
                        AttributeType.STRENGTH,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        "BODYWEIGHT,MUSCULAR_ENDURANCE",
                        preferredSetSize = 25,
                    ),
                    Seed(
                        "ex-lunge",
                        "Walking Lunges",
                        "Bodyweight",
                        AttributeType.STRENGTH,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        "BODYWEIGHT,BALANCE,MUSCULAR_ENDURANCE",
                        preferredSetSize = 20,
                    ),
                    Seed(
                        "ex-situp",
                        "Sit-ups",
                        "Core",
                        AttributeType.STRENGTH,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        "BODYWEIGHT,MUSCULAR_ENDURANCE",
                        preferredSetSize = 25,
                    ),
                    Seed(
                        "ex-plank",
                        "Plank",
                        "Core",
                        AttributeType.DISCIPLINE,
                        ObjectiveType.DURATION,
                        "seconds",
                        "BODYWEIGHT,BALANCE",
                        preferredSetSize = 60,
                    ),
                    Seed(
                        "ex-burpee",
                        "Burpees",
                        "Conditioning",
                        AttributeType.ENDURANCE,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        "HIGH_INTENSITY_CARDIO,EXPLOSIVE,BODYWEIGHT",
                        preferredSetSize = 15,
                    ),
                    Seed(
                        "ex-jumpingjack",
                        "Jumping Jacks",
                        "Conditioning",
                        AttributeType.ENDURANCE,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        "STEADY_STATE_CARDIO,BODYWEIGHT",
                        preferredSetSize = 40,
                    ),
                    Seed("ex-run", "Running", "Cardio", AttributeType.ENDURANCE, ObjectiveType.DISTANCE, "metres", "STEADY_STATE_CARDIO"),
                    Seed(
                        "ex-row",
                        "Rowing",
                        "Cardio",
                        AttributeType.ENDURANCE,
                        ObjectiveType.DISTANCE,
                        "metres",
                        "STEADY_STATE_CARDIO,HIGH_INTENSITY_CARDIO",
                    ),
                    Seed(
                        "ex-benchpress", "Bench Press", "Weights", AttributeType.STRENGTH, ObjectiveType.WEIGHT_AND_REPS,
                        "reps", "HEAVY_STRENGTH,HYPERTROPHY", isWeighted = true, preferredSetSize = 8,
                    ),
                    Seed(
                        "ex-deadlift", "Deadlift", "Weights", AttributeType.STRENGTH, ObjectiveType.WEIGHT_AND_REPS,
                        "reps", "HEAVY_STRENGTH,EXPLOSIVE", isWeighted = true, preferredSetSize = 5,
                    ),
                    Seed(
                        "ex-backsquat", "Back Squat", "Weights", AttributeType.STRENGTH, ObjectiveType.WEIGHT_AND_REPS,
                        "reps", "HEAVY_STRENGTH,HYPERTROPHY", isWeighted = true, preferredSetSize = 5,
                    ),
                    Seed(
                        "ex-jumprope",
                        "Jump Rope",
                        "Agility",
                        AttributeType.AGILITY,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        "HIGH_INTENSITY_CARDIO,EXPLOSIVE",
                        preferredSetSize = 100,
                    ),
                    Seed(
                        "ex-mobility",
                        "Mobility Flow",
                        "Recovery",
                        AttributeType.RECOVERY,
                        ObjectiveType.DURATION,
                        "seconds",
                        "MOBILITY,RECOVERY",
                        preferredSetSize = 120,
                    ),
                    Seed(
                        "ex-stretch",
                        "Stretching",
                        "Recovery",
                        AttributeType.RECOVERY,
                        ObjectiveType.DURATION,
                        "seconds",
                        "MOBILITY,RECOVERY",
                        preferredSetSize = 90,
                    ),
                )
        }
    }
