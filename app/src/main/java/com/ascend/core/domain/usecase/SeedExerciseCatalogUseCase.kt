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
                        preferredSetSize = 20,
                    ),
                    Seed(
                        "ex-pullup",
                        "Pull-ups",
                        "Bodyweight",
                        AttributeType.STRENGTH,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        preferredSetSize = 8,
                    ),
                    Seed(
                        "ex-squat",
                        "Bodyweight Squats",
                        "Bodyweight",
                        AttributeType.STRENGTH,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        preferredSetSize = 25,
                    ),
                    Seed(
                        "ex-lunge",
                        "Walking Lunges",
                        "Bodyweight",
                        AttributeType.STRENGTH,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        preferredSetSize = 20,
                    ),
                    Seed("ex-situp", "Sit-ups", "Core", AttributeType.STRENGTH, ObjectiveType.REPETITIONS, "reps", preferredSetSize = 25),
                    Seed("ex-plank", "Plank", "Core", AttributeType.DISCIPLINE, ObjectiveType.DURATION, "seconds", preferredSetSize = 60),
                    Seed(
                        "ex-burpee",
                        "Burpees",
                        "Conditioning",
                        AttributeType.ENDURANCE,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        preferredSetSize = 15,
                    ),
                    Seed(
                        "ex-jumpingjack",
                        "Jumping Jacks",
                        "Conditioning",
                        AttributeType.ENDURANCE,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        preferredSetSize = 40,
                    ),
                    Seed("ex-run", "Running", "Cardio", AttributeType.ENDURANCE, ObjectiveType.DISTANCE, "metres"),
                    Seed("ex-row", "Rowing", "Cardio", AttributeType.ENDURANCE, ObjectiveType.DISTANCE, "metres"),
                    Seed(
                        "ex-benchpress",
                        "Bench Press",
                        "Weights",
                        AttributeType.STRENGTH,
                        ObjectiveType.WEIGHT_AND_REPS,
                        "reps",
                        isWeighted = true,
                        preferredSetSize = 8,
                    ),
                    Seed(
                        "ex-deadlift",
                        "Deadlift",
                        "Weights",
                        AttributeType.STRENGTH,
                        ObjectiveType.WEIGHT_AND_REPS,
                        "reps",
                        isWeighted = true,
                        preferredSetSize = 5,
                    ),
                    Seed(
                        "ex-backsquat",
                        "Back Squat",
                        "Weights",
                        AttributeType.STRENGTH,
                        ObjectiveType.WEIGHT_AND_REPS,
                        "reps",
                        isWeighted = true,
                        preferredSetSize = 5,
                    ),
                    Seed(
                        "ex-jumprope",
                        "Jump Rope",
                        "Agility",
                        AttributeType.AGILITY,
                        ObjectiveType.REPETITIONS,
                        "reps",
                        preferredSetSize = 100,
                    ),
                    Seed(
                        "ex-mobility",
                        "Mobility Flow",
                        "Recovery",
                        AttributeType.RECOVERY,
                        ObjectiveType.DURATION,
                        "seconds",
                        preferredSetSize = 120,
                    ),
                    Seed(
                        "ex-stretch",
                        "Stretching",
                        "Recovery",
                        AttributeType.RECOVERY,
                        ObjectiveType.DURATION,
                        "seconds",
                        preferredSetSize = 90,
                    ),
                )
        }
    }
