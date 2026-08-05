package com.ascend.core.domain.classes

import com.ascend.core.model.ActivityTags
import com.ascend.core.model.ClassDefinition

/**
 * Turns the data-driven activity tags into human phrases so a class's affinity can be described
 * without any class-specific text. Because it reads only [ClassDefinition.favoredTags], the same
 * routine describes real classes and preview ("yet to awaken") ones — there are no id conditionals.
 */
object ClassAffinityVocabulary {
    /** A stable ordering so a Set of tags renders deterministically. */
    private val TAG_ORDER: List<String> =
        listOf(
            ActivityTags.HEAVY_STRENGTH, ActivityTags.HYPERTROPHY, ActivityTags.EXPLOSIVE,
            ActivityTags.BODYWEIGHT, ActivityTags.MUSCULAR_ENDURANCE, ActivityTags.MOBILITY,
            ActivityTags.BALANCE, ActivityTags.COMBAT, ActivityTags.HIGH_INTENSITY_CARDIO,
            ActivityTags.STEADY_STATE_CARDIO, ActivityTags.BREATH_CONTROL, ActivityTags.AQUATIC,
            ActivityTags.RECOVERY,
        )

    private val PHRASE: Map<String, String> =
        mapOf(
            ActivityTags.HEAVY_STRENGTH to "heavy strength",
            ActivityTags.HYPERTROPHY to "muscle growth",
            ActivityTags.EXPLOSIVE to "explosive power",
            ActivityTags.BODYWEIGHT to "bodyweight control",
            ActivityTags.MUSCULAR_ENDURANCE to "muscular endurance",
            ActivityTags.MOBILITY to "mobility",
            ActivityTags.BALANCE to "balance",
            ActivityTags.COMBAT to "combat technique",
            ActivityTags.HIGH_INTENSITY_CARDIO to "high-intensity conditioning",
            ActivityTags.STEADY_STATE_CARDIO to "sustained cardio",
            ActivityTags.BREATH_CONTROL to "breath control",
            ActivityTags.AQUATIC to "swimming",
            ActivityTags.RECOVERY to "recovery",
        )

    /** A readable phrase for a single tag (falls back to a de-slugged form for unknown tags). */
    fun phrase(tag: String): String = PHRASE[tag] ?: tag.lowercase().replace('_', ' ')

    /** Favored tags as ordered phrases, e.g. ["heavy strength", "muscle growth", "explosive power"]. */
    fun phrases(tags: Set<String>): List<String> =
        (TAG_ORDER.filter { it in tags } + tags.filter { it !in TAG_ORDER }.sorted()).map { phrase(it) }

    /** A one-line affinity summary, e.g. "Trains heavy strength, muscle growth, and explosive power." */
    fun summary(favoredTags: Set<String>): String {
        val list = phrases(favoredTags)
        return if (list.isEmpty()) "A balanced training path." else "Trains ${joinReadable(list)}."
    }

    fun summary(definition: ClassDefinition): String = summary(definition.favoredTags)

    /** Join a list as "a, b, and c" (Oxford), "a and b", or "a". */
    fun joinReadable(items: List<String>): String =
        when (items.size) {
            0 -> ""
            1 -> items[0]
            2 -> "${items[0]} and ${items[1]}"
            else -> items.dropLast(1).joinToString(", ") + ", and ${items.last()}"
        }
}
