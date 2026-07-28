package com.ascend.core.domain.usecase

import com.ascend.core.data.mapper.toEntity
import com.ascend.core.database.dao.QuestTemplateDao
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.QuestTemplate
import javax.inject.Inject

/**
 * Seeds the built‑in Daily Quest templates (idempotent). All target/set limits are
 * **configurable seed data** here — raising squats to 100+ later is a data edit, not
 * a code change.
 */
class SeedQuestTemplatesUseCase
    @Inject
    constructor(
        private val questTemplateDao: QuestTemplateDao,
    ) {
        suspend operator fun invoke() {
            if (questTemplateDao.count() > 0) return
            val ts = System.currentTimeMillis()
            questTemplateDao.upsertAll(CATALOG.map { it.toEntity(ts) })
        }

        companion object {
            val CATALOG: List<QuestTemplate> =
                listOf(
                    QuestTemplate(
                        id = "tmpl-pushups", name = "Push-ups", objectiveType = ObjectiveType.REPETITIONS, unit = "reps",
                        primaryAttribute = AttributeType.STRENGTH, exerciseId = "ex-pushup",
                        minimumTarget = 25, maximumTarget = 500, defaultTarget = 100, targetStep = 5,
                        defaultQuickAddValues = listOf(10, 25, 50),
                        defaultPreferredSetSize = 25, minimumAllowedSetSize = 10, maximumAllowedSetSize = 50,
                        supportsAutomaticProgress = false, supportsManualProgress = true,
                        supportedVariations = emptyList(), safetyWarningThreshold = 300, baseRewardXp = 350,
                    ),
                    QuestTemplate(
                        id = "tmpl-pullups", name = "Pull-ups", objectiveType = ObjectiveType.REPETITIONS, unit = "reps",
                        primaryAttribute = AttributeType.STRENGTH, exerciseId = "ex-pullup",
                        minimumTarget = 10, maximumTarget = 50, defaultTarget = 20, targetStep = 1,
                        defaultQuickAddValues = listOf(1, 5, 10),
                        defaultPreferredSetSize = 8, minimumAllowedSetSize = 1, maximumAllowedSetSize = 15,
                        supportsAutomaticProgress = false, supportsManualProgress = true,
                        supportedVariations = listOf("Assisted", "Band-assisted"), safetyWarningThreshold = 40, baseRewardXp = 300,
                    ),
                    QuestTemplate(
                        id = "tmpl-crunches", name = "Crunches", objectiveType = ObjectiveType.REPETITIONS, unit = "reps",
                        primaryAttribute = AttributeType.STRENGTH, exerciseId = "ex-situp",
                        minimumTarget = 25, maximumTarget = 200, defaultTarget = 75, targetStep = 5,
                        defaultQuickAddValues = listOf(10, 25, 50),
                        defaultPreferredSetSize = 25, minimumAllowedSetSize = 10, maximumAllowedSetSize = 50,
                        supportsAutomaticProgress = false, supportsManualProgress = true,
                        supportedVariations = emptyList(), safetyWarningThreshold = 150, baseRewardXp = 250,
                    ),
                    QuestTemplate(
                        id = "tmpl-squats", name = "Squats", objectiveType = ObjectiveType.REPETITIONS, unit = "reps",
                        primaryAttribute = AttributeType.STRENGTH, exerciseId = "ex-squat",
                        minimumTarget = 10, maximumTarget = 50, defaultTarget = 30, targetStep = 5,
                        defaultQuickAddValues = listOf(5, 10, 25),
                        defaultPreferredSetSize = 10, minimumAllowedSetSize = 5, maximumAllowedSetSize = 25,
                        supportsAutomaticProgress = false, supportsManualProgress = true,
                        supportedVariations = listOf("Bodyweight", "Weighted"), safetyWarningThreshold = 40, baseRewardXp = 250,
                    ),
                    QuestTemplate(
                        id = "tmpl-steps", name = "Walking Steps", objectiveType = ObjectiveType.STEPS, unit = "steps",
                        primaryAttribute = AttributeType.ENDURANCE, exerciseId = null,
                        minimumTarget = 6_000, maximumTarget = 10_000, defaultTarget = 8_000, targetStep = 500,
                        defaultQuickAddValues = listOf(500, 1_000),
                        defaultPreferredSetSize = 2_000, minimumAllowedSetSize = 500, maximumAllowedSetSize = 5_000,
                        supportsAutomaticProgress = true, supportsManualProgress = true,
                        supportedVariations = emptyList(), safetyWarningThreshold = 15_000, baseRewardXp = 200,
                    ),
                )
        }
    }
