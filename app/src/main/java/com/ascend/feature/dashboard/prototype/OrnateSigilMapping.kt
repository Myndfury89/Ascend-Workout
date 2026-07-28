package com.ascend.feature.dashboard.prototype

import kotlin.math.roundToInt

/*
 * Maps fake prototype data → the ornate-sigil state and its accessibility description. Every value
 * the seal represents also appears as readable text in the panel; this description is the single
 * concise summary exposed to accessibility services (not each decorative line).
 */

private fun rankDisplayName(tier: RankTier): String = tier.name.lowercase().replaceFirstChar { it.uppercase() }

/** e.g. "Gold-rank Monk seal. Player level progress 72 percent. Class level progress 44 percent. Discipline recently increased." */
fun sigilDescription(
    data: StatusPrototypeData,
    playerRing: Float,
    classRing: Float?,
    activeMedallion: Int,
): String {
    val sb = StringBuilder()
    sb.append("${rankDisplayName(data.rankTier)}-rank ${data.variant.displayName} seal. ")
    sb.append("Player level progress ${(playerRing * 100).roundToInt()} percent. ")
    if (classRing != null) sb.append("Class level progress ${(classRing * 100).roundToInt()} percent. ")
    if (activeMedallion in data.attributes.indices) {
        sb.append("${data.attributes[activeMedallion].name} recently increased.")
    }
    return sb.toString().trim()
}
