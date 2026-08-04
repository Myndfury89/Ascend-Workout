package com.ascend.core.domain.onboarding

import com.ascend.core.model.QuestTemplate
import com.ascend.core.model.onboarding.AgeSafetyPolicy
import com.ascend.core.model.onboarding.DifficultyBand
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.InitialQuestDefinition
import com.ascend.core.model.onboarding.InitialQuestPlan
import com.ascend.core.model.onboarding.SessionDuration
import com.ascend.core.model.onboarding.TrainingFrequency
import javax.inject.Inject
import kotlin.math.roundToInt

/** One template + its planning profile, supplied by the caller (real templates in a later checkpoint). */
data class QuestPlanCandidate(
    val template: QuestTemplate,
    val profile: QuestActivityProfile,
)

/**
 * Generates a **provisional, conservative** starting Daily Quest plan from the onboarding assessment.
 * It never runs adaptive-training reward math or a second quest engine — it only chooses safe targets
 * within each existing [QuestTemplate]'s ranges and explains the choice. New/returning users and
 * minors start near the low end; the plan respects equipment, environment, availability, and
 * limitations. Every result is [InitialQuestDefinition.provisional] and awards nothing.
 */
class InitialQuestPlanGenerator
    @Inject
    constructor() {
        fun generate(
            assessment: InitialAssessment,
            candidates: List<QuestPlanCandidate>,
            now: Long = 0L,
        ): InitialQuestPlan {
            val conservative = AgeSafetyPolicy.usesConservativeDefaults(assessment.ageSafetyCategory)
            val band = difficultyBand(assessment.trainingFrequency, conservative)
            val safetyAdjustments = mutableListOf<String>()
            if (assessment.trainingFrequency == TrainingFrequency.NEW_OR_RETURNING ||
                assessment.trainingFrequency == TrainingFrequency.LESS_THAN_ONCE_WEEKLY
            ) {
                safetyAdjustments += "Conservative starting targets to build a consistent habit first."
            }
            if (conservative && AgeSafetyPolicy.isMinor(assessment.ageSafetyCategory)) {
                safetyAdjustments += "Reduced starting volume for under-18 safety."
            }

            val environment = assessment.environment
            val equipment = assessment.equipment
            val limitations = assessment.effectiveLimitations

            val eligible =
                candidates.filter { candidate ->
                    val p = candidate.profile
                    val equipmentOk = p.equipmentSatisfied(equipment)
                    val environmentOk = p.environmentSatisfied(environment)
                    val blockedByLimitation = p.conflictsWith(limitations) && !p.substitutable
                    equipmentOk && environmentOk && !blockedByLimitation
                }

            val ranked = eligible.sortedByDescending { relevance(it, assessment) }
            val planCount = ranked.size.coerceAtMost(maxQuests(assessment))
            val chosen = ranked.take(planCount.coerceAtLeast(if (ranked.isEmpty()) 0 else 1))

            val durationFactor = durationFactor(assessment.availability?.sessionDuration)
            val definitions =
                chosen.map { candidate ->
                    toDefinition(candidate, band, conservative, durationFactor, limitations, equipment, safetyAdjustments)
                }

            return InitialQuestPlan(
                userId = assessment.userId,
                questDefinitions = definitions,
                assessmentSuggestions = emptyList(),
                rationale = planRationale(assessment, band, definitions.size),
                difficultyBand = band,
                safetyAdjustments = safetyAdjustments.distinct(),
                provisional = true,
                createdAt = now,
            )
        }

        private fun toDefinition(
            candidate: QuestPlanCandidate,
            band: DifficultyBand,
            conservative: Boolean,
            durationFactor: Double,
            limitations: Set<com.ascend.core.model.onboarding.Limitation>,
            equipment: Set<com.ascend.core.model.onboarding.Equipment>,
            safetyAdjustments: MutableList<String>,
        ): InitialQuestDefinition {
            val template = candidate.template
            val profile = candidate.profile
            val substituted = profile.requiresSubstitution(equipment)
            val limitationTouch = profile.conflictsWith(limitations)
            val safetyAdjusted = substituted || limitationTouch || conservative

            var factor = bandFactor(band) * durationFactor * (if (conservative) CONSERVATIVE_MULTIPLIER else 1.0)
            if (substituted || limitationTouch) factor *= SUBSTITUTION_MULTIPLIER
            if (substituted) safetyAdjustments += "Substituted an equipment-free option for ${template.name}."
            if (limitationTouch) safetyAdjustments += "Lowered ${template.name} intensity for a reported limitation."

            val target = conservativeTarget(template, factor)
            val setSize =
                if (band == DifficultyBand.FOUNDATION || conservative) {
                    template.minimumAllowedSetSize
                } else {
                    template.defaultPreferredSetSize
                }
            return InitialQuestDefinition(
                templateId = template.id,
                name = template.name,
                unit = template.unit,
                target = target,
                preferredSetSize = setSize,
                rationale = "Start ${template.name.lowercase()} at $target ${template.unit} — a comfortable, repeatable baseline.",
                equipmentCompatible = true,
                scheduleCompatible = true,
                safetyAdjusted = safetyAdjusted,
                provisional = true,
            )
        }

        private fun conservativeTarget(
            template: QuestTemplate,
            factor: Double,
        ): Int {
            val min = template.minimumTarget
            val max = template.maximumTarget
            val step = template.targetStep.coerceAtLeast(1)
            val span = (max - min).coerceAtLeast(0)
            val raw = min + span * factor.coerceIn(0.0, 1.0)
            val stepped = min + (((raw - min) / step).roundToInt()) * step
            // Stay strictly conservative: never at/above the high-target confirmation threshold.
            val ceiling = (template.safetyWarningThreshold - step).coerceAtLeast(min)
            return stepped.coerceIn(min, minOf(max, ceiling))
        }

        private fun difficultyBand(
            frequency: TrainingFrequency?,
            conservative: Boolean,
        ): DifficultyBand {
            val base =
                when (frequency) {
                    null, TrainingFrequency.NEW_OR_RETURNING, TrainingFrequency.LESS_THAN_ONCE_WEEKLY -> DifficultyBand.FOUNDATION
                    TrainingFrequency.ONE_TO_TWO_WEEKLY, TrainingFrequency.THREE_TO_FOUR_WEEKLY -> DifficultyBand.DEVELOPING
                    TrainingFrequency.FIVE_PLUS_WEEKLY -> DifficultyBand.STEADY
                }
            // Minors / not-provided are capped one band lower (never above DEVELOPING).
            return if (conservative && base == DifficultyBand.STEADY) DifficultyBand.DEVELOPING else base
        }

        private fun bandFactor(band: DifficultyBand): Double =
            when (band) {
                DifficultyBand.FOUNDATION -> FOUNDATION_FACTOR
                DifficultyBand.DEVELOPING -> DEVELOPING_FACTOR
                DifficultyBand.STEADY -> STEADY_FACTOR
            }

        private fun durationFactor(duration: SessionDuration?): Double =
            when (duration) {
                SessionDuration.UNDER_20 -> SHORT_SESSION_FACTOR
                SessionDuration.MIN_20_30 -> MID_SESSION_FACTOR
                null -> MID_SESSION_FACTOR
                else -> FULL_SESSION_FACTOR
            }

        private fun maxQuests(assessment: InitialAssessment): Int {
            val days = assessment.availability?.effectiveTrainingDays ?: DEFAULT_DAYS
            return days.coerceIn(1, MAX_QUESTS)
        }

        private fun relevance(
            candidate: QuestPlanCandidate,
            assessment: InitialAssessment,
        ): Int {
            val wanted = mutableSetOf<String>()
            assessment.primaryGoal?.let { wanted += GoalAffinityMapping.goalTags(it) }
            assessment.secondaryGoals.forEach { wanted += GoalAffinityMapping.goalTags(it) }
            assessment.activityPreferences.forEach { wanted += GoalAffinityMapping.preferenceTags(it) }
            return candidate.profile.tags.count { it in wanted }
        }

        private fun planRationale(
            assessment: InitialAssessment,
            band: DifficultyBand,
            count: Int,
        ): String {
            val goal = assessment.primaryGoal?.displayName ?: "general health"
            return "A $count-quest starting plan at ${band.name.lowercase()} intensity, matched to your goal of $goal " +
                "and your available equipment, environment, and schedule. It is provisional — it adapts as you log real workouts."
        }

        private companion object {
            const val FOUNDATION_FACTOR = 0.0
            const val DEVELOPING_FACTOR = 0.20
            const val STEADY_FACTOR = 0.40
            const val CONSERVATIVE_MULTIPLIER = 0.5
            const val SUBSTITUTION_MULTIPLIER = 0.5
            const val SHORT_SESSION_FACTOR = 0.6
            const val MID_SESSION_FACTOR = 0.8
            const val FULL_SESSION_FACTOR = 1.0
            const val MAX_QUESTS = 3
            const val DEFAULT_DAYS = 3
        }
    }
