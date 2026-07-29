package com.ascend.core.domain.dungeon

import com.ascend.core.model.DungeonDefinition
import com.ascend.core.model.SoloReadiness
import com.ascend.core.model.SoloReadinessInput
import javax.inject.Inject

/** Per-dungeon solo requirements (real fitness evidence only; Class Level is intentionally absent). */
data class SoloReadinessConfig(
    val minActiveWorkoutSeconds: Long = 10 * 60,
    val minTrainingTier: Int = 2,
    val requiredSkillIds: Set<String> = emptySet(),
    val requireRecentActivity: Boolean = true,
)

/**
 * Decides whether a member may attempt a solo encounter — from **real fitness evidence** only.
 * Class Level is not an input and therefore can never prove readiness. A safety stop always blocks
 * a solo attempt. Explainable: it returns the satisfied reasons and the missing requirements.
 */
class SoloReadinessEvaluator
    @Inject
    constructor() {
        fun evaluate(
            input: SoloReadinessInput,
            definition: DungeonDefinition,
            config: SoloReadinessConfig = SoloReadinessConfig(),
        ): SoloReadiness {
            val reasons = mutableListOf<String>()
            val missing = mutableListOf<String>()

            if (!definition.soloAllowed) missing += "This dungeon cannot be attempted solo"
            if (input.safetyStopActive) missing += "A safety stop is active"

            check(reasons, missing, input.activeWorkoutSeconds >= config.minActiveWorkoutSeconds) {
                "Active workout ${input.activeWorkoutSeconds}s / ${config.minActiveWorkoutSeconds}s"
            }
            check(reasons, missing, input.trainingTier >= config.minTrainingTier) {
                "Training tier ${input.trainingTier} / ${config.minTrainingTier}"
            }
            if (config.requireRecentActivity) {
                check(reasons, missing, input.recentQualifyingActivity) { "Recent qualifying activity" }
            }
            val missingSkills = config.requiredSkillIds - input.unlockedSkillIds
            check(reasons, missing, missingSkills.isEmpty()) { "Required skills unlocked" }

            return SoloReadiness(eligible = missing.isEmpty(), reasons = reasons, missing = missing)
        }

        private inline fun check(
            reasons: MutableList<String>,
            missing: MutableList<String>,
            condition: Boolean,
            label: () -> String,
        ) {
            if (condition) reasons += label() else missing += label()
        }
    }
