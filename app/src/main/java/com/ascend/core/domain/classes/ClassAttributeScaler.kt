package com.ascend.core.domain.classes

import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassAttributeModifier
import com.ascend.core.model.ClassDefinition
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Applies a class's per‑attribute multipliers to an activity's **existing** base
 * distribution. Keys are preserved 1:1 — a multiplier applied to a zero (or absent)
 * base stays zero, so no proficiency is ever created in an attribute the activity
 * didn't already train (constraint #4).
 */
class ClassAttributeScaler
    @Inject
    constructor() {
        fun scale(
            baseDistribution: Map<AttributeType, Long>,
            def: ClassDefinition,
        ): Map<AttributeType, Long> {
            val out = LinkedHashMap<AttributeType, Long>()
            baseDistribution.forEach { (attribute, base) ->
                val scaled = (base * def.attributeMultiplier(attribute)).roundToLong().coerceAtLeast(0)
                if (scaled > 0) out[attribute] = scaled
            }
            return out
        }

        /** The multipliers actually applied to the trained attributes (for the breakdown). */
        fun modifiers(
            baseDistribution: Map<AttributeType, Long>,
            def: ClassDefinition,
        ): List<ClassAttributeModifier> = baseDistribution.keys.map { ClassAttributeModifier(it, def.attributeMultiplier(it)) }
    }
