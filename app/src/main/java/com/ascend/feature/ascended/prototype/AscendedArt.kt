package com.ascend.feature.ascended.prototype

import android.content.Context
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.ascended.prototype.model.EvolutionStage

/*
 * The figure-art naming contract. Class figures are imported grayscale images dropped into
 * `app/src/debug/res/drawable/` following a fixed naming convention, so new art appears with no code
 * change. Resolution is by name at runtime (missing art → 0 → the on-screen placeholder), which lets
 * the system exist and be reviewed before every image is supplied, and lets a stage-specific variant
 * fall back to the class's base figure.
 *
 *   ascended_<classId>_<base>            e.g. ascended_guardian_male
 *   ascended_<classId>_<base>_<stage>    e.g. ascended_guardian_male_mastered   (optional per-stage)
 */
object AscendedArt {
    /** The base (stage-agnostic) drawable name for a class + body base, e.g. `ascended_monk_female`. */
    fun figureResourceName(
        cls: AscendedClass,
        base: BodyBase,
    ): String = "ascended_${cls.id}_${base.name.lowercase()}"

    /**
     * The neutral base-body drawable name for a body base, e.g. `ascended_base_male`. Every class
     * falls back to this shared body until its own class art is supplied, so the two base figures
     * drive the whole roster on their own.
     */
    fun baseBodyResourceName(base: BodyBase): String = "ascended_base_${base.name.lowercase()}"

    /** The preferred drawable name for a class + base at a stage; BASE has no suffix. */
    fun figureResourceName(
        cls: AscendedClass,
        base: BodyBase,
        stage: EvolutionStage,
    ): String =
        if (stage == EvolutionStage.BASE) {
            figureResourceName(cls, base)
        } else {
            "${figureResourceName(cls, base)}_${stage.slug}"
        }

    /** Resolve a drawable id by name, or 0 when the image has not been supplied. */
    @Suppress("DEPRECATION")
    fun resolveDrawable(
        context: Context,
        name: String,
    ): Int = context.resources.getIdentifier(name, "drawable", context.packageName)
}
