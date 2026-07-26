package com.ascend.feature.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.core.domain.repository.WorkoutRepository
import com.ascend.core.domain.usecase.SeedExerciseCatalogUseCase
import com.ascend.core.model.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutsUiState(
    val isLoading: Boolean = true,
    val workouts: List<Workout> = emptyList(),
)

@HiltViewModel
class WorkoutsViewModel
    @Inject
    constructor(
        private val playerRepository: PlayerRepository,
        private val workoutRepository: WorkoutRepository,
        private val seedExerciseCatalog: SeedExerciseCatalogUseCase,
    ) : ViewModel() {
        private val userId = MutableStateFlow<String?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<WorkoutsUiState> =
            userId
                .filterNotNull()
                .flatMapLatest { uid ->
                    workoutRepository.observeWorkoutsForUser(uid).map { WorkoutsUiState(isLoading = false, workouts = it) }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutsUiState())

        init {
            viewModelScope.launch {
                val uid = playerRepository.ensureLocalPlayer()
                seedExerciseCatalog()
                userId.value = uid
            }
        }
    }
