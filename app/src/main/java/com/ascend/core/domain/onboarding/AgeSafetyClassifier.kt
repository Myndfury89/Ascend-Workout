package com.ascend.core.domain.onboarding

import com.ascend.core.model.onboarding.AgeRange
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.AgeSafetyPolicy
import com.ascend.core.model.onboarding.SocialPrivacyDefaults
import javax.inject.Inject

/**
 * Injectable façade over the pure [AgeSafetyPolicy] so ViewModels can classify an age range and read
 * the implied safety/social defaults without depending on the policy object directly. Adds no
 * behaviour of its own — the policy is the single source of truth (minimum age 13; conservative
 * defaults for minors and not-provided).
 */
class AgeSafetyClassifier
    @Inject
    constructor() {
        fun classify(range: AgeRange): AgeSafetyCategory = AgeSafetyPolicy.categoryFor(range)

        fun canOnboard(category: AgeSafetyCategory): Boolean = AgeSafetyPolicy.canOnboard(category)

        fun isMinor(category: AgeSafetyCategory): Boolean = AgeSafetyPolicy.isMinor(category)

        fun usesConservativeDefaults(category: AgeSafetyCategory): Boolean = AgeSafetyPolicy.usesConservativeDefaults(category)

        fun socialDefaults(category: AgeSafetyCategory): SocialPrivacyDefaults = AgeSafetyPolicy.defaultsFor(category)
    }
