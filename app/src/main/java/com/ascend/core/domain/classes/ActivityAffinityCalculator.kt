package com.ascend.core.domain.classes

import com.ascend.core.model.ActivityAffinityResult
import com.ascend.core.model.ClassDefinition
import javax.inject.Inject

/**
 * Computes how well an activity matches a class, purely from tag overlap — never
 * from class‑specific conditionals. Affinity is the fraction of the activity's tags
 * the class favors, in 0.0..1.0 (untagged activity -> 0, neutral).
 */
class ActivityAffinityCalculator
    @Inject
    constructor() {
        fun calculate(
            activityTags: Set<String>,
            def: ClassDefinition,
        ): ActivityAffinityResult {
            val matched = activityTags.filterTo(LinkedHashSet()) { it in def.favoredTags }
            val affinity = if (activityTags.isEmpty()) 0.0 else matched.size.toDouble() / activityTags.size
            return ActivityAffinityResult(
                classId = def.id,
                affinity = affinity,
                matchedTags = matched,
                favored = affinity > 0.0,
            )
        }

        fun affinity(
            activityTags: Set<String>,
            def: ClassDefinition,
        ): Double = calculate(activityTags, def).affinity
    }
