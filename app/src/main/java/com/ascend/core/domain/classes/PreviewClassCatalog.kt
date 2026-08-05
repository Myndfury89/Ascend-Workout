package com.ascend.core.domain.classes

import com.ascend.core.model.ActivityTags

/**
 * Presentation-only descriptors for classes that are *directionally planned* but not yet implemented
 * (Assassin, Fighter, Ranger, Guardian). These are shown in onboarding's "Paths Yet to Awaken"
 * preview section — never selectable, never seeded into [ClassCatalog] / the `class_definition` table,
 * and carrying **no** attribute multipliers, Class-XP rules, proficiency ledgers, or progression data.
 *
 * A preview intentionally holds only what can be honestly described from the design direction: an
 * affinity summary (via [ClassAffinityVocabulary] over directional tags), typical training styles, and
 * a *suggested* unique-proficiency label. Names and proficiency labels remain configurable data. When
 * a preview class becomes fully specified it graduates to a real [com.ascend.core.model.ClassDefinition]
 * in [ClassCatalog]; nothing here fabricates the numbers that would require.
 */
data class PreviewClass(
    val id: String,
    val name: String,
    val favoredTags: Set<String>,
    val primaryStyles: List<String>,
    val suggestedProficiencyName: String,
) {
    /** Data-driven affinity summary, identical styling to real class cards. */
    val affinitySummary: String get() = ClassAffinityVocabulary.summary(favoredTags)
}

object PreviewClassCatalog {
    val ASSASSIN =
        PreviewClass(
            id = "assassin",
            name = "Assassin",
            favoredTags = setOf(ActivityTags.EXPLOSIVE, ActivityTags.HIGH_INTENSITY_CARDIO, ActivityTags.COMBAT),
            primaryStyles = listOf("Sprint intervals", "Boxing", "Agility drills", "Footwork", "Reaction work", "Explosive calisthenics"),
            suggestedProficiencyName = "Precision",
        )
    val FIGHTER =
        PreviewClass(
            id = "fighter",
            name = "Fighter",
            favoredTags =
                setOf(
                    ActivityTags.HEAVY_STRENGTH,
                    ActivityTags.COMBAT,
                    ActivityTags.HIGH_INTENSITY_CARDIO,
                    ActivityTags.MUSCULAR_ENDURANCE,
                ),
            primaryStyles =
                listOf(
                    "Mixed resistance training",
                    "Boxing or martial arts",
                    "Circuits",
                    "Conditioning",
                    "Functional strength",
                    "Balanced athletic development",
                ),
            suggestedProficiencyName = "Combat Rhythm",
        )
    val RANGER =
        PreviewClass(
            id = "ranger",
            name = "Ranger",
            favoredTags = setOf(ActivityTags.STEADY_STATE_CARDIO, ActivityTags.MUSCULAR_ENDURANCE, ActivityTags.BALANCE),
            primaryStyles =
                listOf(
                    "Running",
                    "Hiking",
                    "Walking distance",
                    "Outdoor conditioning",
                    "Coordination",
                    "Steady-state endurance",
                ),
            suggestedProficiencyName = "Pursuit",
        )
    val GUARDIAN =
        PreviewClass(
            id = "guardian",
            name = "Guardian",
            favoredTags = setOf(ActivityTags.HEAVY_STRENGTH, ActivityTags.MUSCULAR_ENDURANCE, ActivityTags.RECOVERY, ActivityTags.BALANCE),
            primaryStyles =
                listOf(
                    "Loaded carries",
                    "Core stability",
                    "Controlled strength",
                    "Work capacity",
                    "Recovery consistency",
                    "Injury-conscious substitutions",
                ),
            suggestedProficiencyName = "Fortitude",
        )

    /** All preview classes, in display order. None are selectable. */
    val ALL: List<PreviewClass> = listOf(ASSASSIN, FIGHTER, RANGER, GUARDIAN)

    /** Ids that must never be accepted as a real selection (guards against a preview being saved). */
    val PREVIEW_IDS: Set<String> = ALL.map { it.id }.toSet()
}
