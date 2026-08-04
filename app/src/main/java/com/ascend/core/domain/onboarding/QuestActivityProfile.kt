package com.ascend.core.domain.onboarding

import com.ascend.core.model.ActivityTags
import com.ascend.core.model.onboarding.ActivityDomain
import com.ascend.core.model.onboarding.Equipment
import com.ascend.core.model.onboarding.Limitation
import com.ascend.core.model.onboarding.TrainingEnvironment

/**
 * Planning metadata for a Daily Quest template — which equipment/environment it needs, what it
 * trains, and which limitations it conflicts with. This is *annotation only*: it references existing
 * seeded [com.ascend.core.model.QuestTemplate] ids and never redefines targets/ranges (those stay in
 * the template). [substitutable] means an equipment-free or limitation-safe substitution exists, so
 * the activity can still be offered (with a safety adjustment) rather than dropped.
 */
data class QuestActivityProfile(
    val templateId: String,
    val domain: ActivityDomain,
    val tags: Set<String>,
    val requiredEquipment: Set<Equipment>,
    val allowedEnvironments: Set<TrainingEnvironment>,
    val limitationConflicts: Set<Limitation>,
    val substitutable: Boolean,
) {
    private val environmentAgnostic get() = allowedEnvironments.isEmpty()

    /** Equipment is satisfied when nothing is required, the user has it, or a substitution exists. */
    fun equipmentSatisfied(available: Set<Equipment>): Boolean =
        requiredEquipment.isEmpty() || available.containsAll(requiredEquipment) || substitutable

    fun environmentSatisfied(environment: TrainingEnvironment?): Boolean =
        environmentAgnostic || environment == null || environment == TrainingEnvironment.MIXED ||
            environment in allowedEnvironments

    /** True when the user's required equipment is genuinely missing (only a substitution keeps it). */
    fun requiresSubstitution(available: Set<Equipment>): Boolean =
        requiredEquipment.isNotEmpty() && !available.containsAll(requiredEquipment) && substitutable

    fun conflictsWith(limitations: Set<Limitation>): Boolean = limitationConflicts.any { it in limitations }
}

/**
 * Profiles for the currently-seeded Daily Quest templates. Kept as data (not per-template code) so it
 * extends as new templates are seeded. Only walking + bodyweight staples exist today; the pull-up
 * template is the one that genuinely needs equipment (a bar) and has no valid onboarding substitute.
 */
object QuestActivityProfileCatalog {
    val profiles: List<QuestActivityProfile> =
        listOf(
            QuestActivityProfile(
                templateId = "tmpl-pushups",
                domain = ActivityDomain.CALISTHENICS,
                tags = setOf(ActivityTags.BODYWEIGHT, ActivityTags.MUSCULAR_ENDURANCE),
                requiredEquipment = emptySet(),
                allowedEnvironments = emptySet(),
                limitationConflicts = setOf(Limitation.UPPER_BODY),
                substitutable = true,
            ),
            QuestActivityProfile(
                templateId = "tmpl-pullups",
                domain = ActivityDomain.CALISTHENICS,
                tags = setOf(ActivityTags.BODYWEIGHT, ActivityTags.HEAVY_STRENGTH),
                requiredEquipment = setOf(Equipment.PULL_UP_BAR),
                allowedEnvironments = emptySet(),
                limitationConflicts = setOf(Limitation.UPPER_BODY),
                substitutable = false,
            ),
            QuestActivityProfile(
                templateId = "tmpl-crunches",
                domain = ActivityDomain.CALISTHENICS,
                tags = setOf(ActivityTags.BODYWEIGHT),
                requiredEquipment = emptySet(),
                allowedEnvironments = emptySet(),
                limitationConflicts = setOf(Limitation.BACK_SENSITIVITY, Limitation.PREGNANCY_POSTPARTUM),
                substitutable = false,
            ),
            QuestActivityProfile(
                templateId = "tmpl-squats",
                domain = ActivityDomain.CALISTHENICS,
                tags = setOf(ActivityTags.BODYWEIGHT, ActivityTags.MUSCULAR_ENDURANCE),
                requiredEquipment = emptySet(),
                allowedEnvironments = emptySet(),
                limitationConflicts = setOf(Limitation.LOWER_BODY),
                substitutable = true,
            ),
            QuestActivityProfile(
                templateId = "tmpl-steps",
                domain = ActivityDomain.CARDIO,
                tags = setOf(ActivityTags.STEADY_STATE_CARDIO),
                requiredEquipment = emptySet(),
                allowedEnvironments = emptySet(),
                limitationConflicts = emptySet(),
                substitutable = true,
            ),
        )

    fun forTemplate(templateId: String): QuestActivityProfile? = profiles.firstOrNull { it.templateId == templateId }
}
