package com.ascend.core.model

/**
 * How a class *prefers* to progress, expressed as data. This shapes only the **ranking**
 * of options that are already safe — a class can never make an unsafe option safe, change
 * readiness, or remove an off-class option (discouraged options are only pushed down, never
 * dropped). [favoredDimensions] is an ordered preference list (most preferred first).
 */
data class ClassProgressionPreference(
    val classId: String,
    val favoredDimensions: List<ProgressionDimension>,
    val discouragedDimensions: Set<ProgressionDimension> = emptySet(),
    val discouragedRecommendationTypes: Set<ProgressionRecommendationType> = emptySet(),
    val favoredTags: Set<String> = emptySet(),
    val rewardModifier: Double = 1.0,
)
