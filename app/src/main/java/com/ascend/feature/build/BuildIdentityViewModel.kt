package com.ascend.feature.build

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.domain.build.BuildCharacteristic
import com.ascend.core.domain.build.BuildClass
import com.ascend.core.domain.build.BuildProfileRepository
import com.ascend.core.domain.build.CharacteristicScore
import com.ascend.core.domain.build.ClassAffinity
import com.ascend.core.domain.build.RefreshBuildProfileUseCase
import com.ascend.core.domain.repository.ClassRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Read-only presentation state for the Build Analysis surface. */
data class BuildIdentityUiState(
    val loading: Boolean = true,
    val characteristics: List<CharacteristicScore> = emptyList(),
    val affinities: List<ClassAffinity> = emptyList(),
    val dominant: ClassAffinity? = null,
    /** The player's currently-bound class and its affinity — shown beside the ranking, never inside it. */
    val currentClass: ClassAffinity? = null,
    val currentClassName: String? = null,
)

/**
 * Presents the cached Build snapshot and refreshes it when the surface opens (recompute-on-view). It
 * only reads — the snapshot, the class selection — and triggers a read-only recompute; it awards
 * nothing. The current class is surfaced separately from the affinity ranking so the ranking stays
 * truthful (you can be a Monk who has lately been training like a Fighter).
 */
@HiltViewModel
class BuildIdentityViewModel
    @Inject
    constructor(
        repository: BuildProfileRepository,
        classRepository: ClassRepository,
        private val refreshBuildProfile: RefreshBuildProfileUseCase,
    ) : ViewModel() {
        private val userId = LOCAL_USER_ID

        val uiState: StateFlow<BuildIdentityUiState> =
            combine(
                repository.observe(userId),
                classRepository.observeSelection(userId),
            ) { snapshot, selection ->
                if (snapshot == null) {
                    BuildIdentityUiState(loading = true)
                } else {
                    val ranked = snapshot.affinities.ranked
                    val currentClass = selection.primaryClassId?.let { id -> ranked.firstOrNull { it.buildClass.id == id } }
                    BuildIdentityUiState(
                        loading = false,
                        characteristics = BuildCharacteristic.entries.mapNotNull { snapshot.profile[it] },
                        affinities = ranked,
                        dominant = snapshot.affinities.dominant,
                        currentClass = currentClass,
                        currentClassName = currentClass?.buildClass?.displayName ?: selection.primaryClassId?.let(::displayNameFor),
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BuildIdentityUiState())

        init {
            viewModelScope.launch {
                runCatching { refreshBuildProfile.refresh(userId, System.currentTimeMillis()) }
            }
        }

        private fun displayNameFor(classId: String): String? = BuildClass.fromId(classId)?.displayName

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
