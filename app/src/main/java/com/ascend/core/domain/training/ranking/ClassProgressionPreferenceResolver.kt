package com.ascend.core.domain.training.ranking

import com.ascend.core.model.ClassProgressionPreference
import javax.inject.Inject

/**
 * Resolves a class's progression preference. A thin, injectable seam over the catalog so
 * the source can later become the DB-backed `class_definition` without touching the ranker.
 */
class ClassProgressionPreferenceResolver
    @Inject
    constructor() {
        fun preferenceFor(classId: String?): ClassProgressionPreference? = ClassProgressionPreferenceCatalog.byId(classId)
    }
