package com.ascend.feature.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.core.common.WeightUnit
import com.ascend.core.common.WeightUnits
import com.ascend.core.domain.repository.CompleteWorkoutResult
import com.ascend.core.domain.repository.NewSetSpec
import com.ascend.core.domain.repository.NewWorkoutSpec
import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.core.domain.repository.WorkoutRepository
import com.ascend.core.domain.usecase.SeedExerciseCatalogUseCase
import com.ascend.core.model.Difficulty
import com.ascend.core.model.Exercise
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.WorkoutSet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutCompletionInfo(val xp: Long, val leveledUp: Boolean, val newLevel: Int)

data class LogWorkoutUiState(
    val exercises: List<Exercise> = emptyList(),
    val title: String = DEFAULT_TITLE,
    val difficulty: Difficulty = Difficulty.MODERATE,
    val sets: List<WorkoutSet> = emptyList(),
    val completion: WorkoutCompletionInfo? = null,
) {
    val canFinish: Boolean get() = sets.isNotEmpty()

    companion object {
        const val DEFAULT_TITLE = "Training Session"
    }
}

@HiltViewModel
class LogWorkoutViewModel
    @Inject
    constructor(
        private val playerRepository: PlayerRepository,
        private val workoutRepository: WorkoutRepository,
        private val seedExerciseCatalog: SeedExerciseCatalogUseCase,
    ) : ViewModel() {
        private val userId = MutableStateFlow<String?>(null)
        private val workoutId = MutableStateFlow<String?>(null)
        private val title = MutableStateFlow(LogWorkoutUiState.DEFAULT_TITLE)
        private val difficulty = MutableStateFlow(Difficulty.MODERATE)
        private val completion = MutableStateFlow<WorkoutCompletionInfo?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<LogWorkoutUiState> =
            combine(
                workoutRepository.observeExercises(),
                workoutId.flatMapLatest { id ->
                    if (id == null) flowOf(null) else workoutRepository.observeWorkout(id)
                },
                title,
                difficulty,
                completion,
            ) { exercises, workout, title, difficulty, completion ->
                LogWorkoutUiState(
                    exercises = exercises,
                    title = title,
                    difficulty = difficulty,
                    sets = workout?.sets.orEmpty(),
                    completion = completion,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogWorkoutUiState())

        /** The display weight unit; canonical storage is always kilograms. */
        @OptIn(ExperimentalCoroutinesApi::class)
        val weightUnit: StateFlow<WeightUnit> =
            userId
                .filterNotNull()
                .flatMapLatest { playerRepository.observeWeightUnit(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.KILOGRAMS)

        init {
            viewModelScope.launch {
                val uid = playerRepository.ensureLocalPlayer()
                seedExerciseCatalog()
                userId.value = uid
            }
        }

        fun setWeightUnit(unit: WeightUnit) =
            viewModelScope.launch {
                playerRepository.setWeightUnit(userId.filterNotNull().first(), unit)
            }

        fun setTitle(value: String) {
            title.value = value
        }

        fun setDifficulty(value: Difficulty) {
            difficulty.value = value
        }

        fun addSet(
            exercise: Exercise,
            value: Int,
            weight: Double?,
        ) = viewModelScope.launch {
            if (value <= 0) return@launch
            val uid = userId.filterNotNull().first()
            val id = ensureWorkout(uid)
            // The screen provides the weight in the display unit; store it canonically in kg.
            val canonicalWeight = weight?.let { WeightUnits.toCanonicalKg(it, weightUnit.value) }
            workoutRepository.addSet(
                id,
                NewSetSpec(
                    exerciseId = exercise.id,
                    volume = value.toDouble(),
                    unit = exercise.defaultUnit,
                    reps = if (exercise.measurementType.isRepBased()) value else null,
                    weight = canonicalWeight,
                    durationSeconds = if (exercise.measurementType == ObjectiveType.DURATION) value.toLong() else null,
                    distance = if (exercise.measurementType == ObjectiveType.DISTANCE) value.toDouble() else null,
                ),
            )
        }

        fun deleteSet(setId: String) =
            viewModelScope.launch {
                workoutRepository.deleteSet(setId)
            }

        fun finish() =
            viewModelScope.launch {
                val id = workoutId.value ?: return@launch
                when (val result = workoutRepository.completeWorkout(id)) {
                    is CompleteWorkoutResult.Completed ->
                        completion.value = WorkoutCompletionInfo(result.xpAwarded, result.leveledUp, result.newLevel)
                    else -> Unit
                }
            }

        fun dismissCompletion() {
            completion.value = null
        }

        /** Create the backing workout on first set so empty drafts never persist. */
        private suspend fun ensureWorkout(uid: String): String {
            workoutId.value?.let { return it }
            val id =
                workoutRepository.createWorkout(
                    NewWorkoutSpec(
                        userId = uid,
                        title = title.value.ifBlank { LogWorkoutUiState.DEFAULT_TITLE },
                        difficulty = difficulty.value,
                    ),
                )
            workoutId.value = id
            return id
        }

        private fun ObjectiveType.isRepBased(): Boolean = this == ObjectiveType.REPETITIONS || this == ObjectiveType.WEIGHT_AND_REPS
    }
