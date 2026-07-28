package com.ascend.core.domain.usecase

import com.ascend.core.domain.repository.ExerciseVariationGraphRepository
import javax.inject.Inject

/**
 * Seeds the built-in bodyweight variation graphs (push-up, pull-up) idempotently. Runs
 * only when no variations exist, so user edits are never clobbered.
 */
class SeedExerciseVariationsUseCase
    @Inject
    constructor(
        private val repository: ExerciseVariationGraphRepository,
    ) {
        suspend operator fun invoke() = repository.seedBuiltInGraphs()
    }
