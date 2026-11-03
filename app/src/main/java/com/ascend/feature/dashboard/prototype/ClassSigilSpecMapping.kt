package com.ascend.feature.dashboard.prototype

import com.ascend.core.designsystem.sigil.ClassKey
import com.ascend.core.designsystem.sigil.ClassSigilCatalog
import com.ascend.core.designsystem.sigil.ClassSigilSpec

/** The class key for a prototype variant (same names; Neutral = no class). */
fun StatusClassVariant.classKey(): ClassKey =
    when (this) {
        StatusClassVariant.NEUTRAL -> ClassKey.NEUTRAL
        StatusClassVariant.BERSERKER -> ClassKey.BERSERKER
        StatusClassVariant.MONK -> ClassKey.MONK
        StatusClassVariant.MAGE -> ClassKey.MAGE
    }

/** The data-driven sigil spec (geometry + palette) for a prototype variant. */
fun StatusClassVariant.sigilSpec(): ClassSigilSpec = ClassSigilCatalog.of(classKey())

/** The prototype variant that renders a [ClassKey]. */
fun ClassKey.statusVariant(): StatusClassVariant =
    when (this) {
        ClassKey.NEUTRAL -> StatusClassVariant.NEUTRAL
        ClassKey.BERSERKER -> StatusClassVariant.BERSERKER
        ClassKey.MONK -> StatusClassVariant.MONK
        ClassKey.MAGE -> StatusClassVariant.MAGE
    }
