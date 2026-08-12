package com.ascend.feature.ascended

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.domain.repository.ClassRepository
import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.feature.ascended.model.AscendedClass
import com.ascend.feature.ascended.model.BodyBase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Read-only state for the production Your Ascended page. */
data class YourAscendedUiState(
    val loading: Boolean = true,
    val figureClass: AscendedClass? = null,
    val bodyBase: BodyBase? = null,
    val needsBodyBaseChoice: Boolean = false,
    val reducedMotion: Boolean = false,
)

/**
 * Presentation-only ViewModel for Your Ascended. Observes the real class selection + the cosmetic
 * avatar body-base preference and maps them to a figure. It may PERSIST the body-base choice, but it
 * must never mutate progression (XP, class XP, attributes, skills, quests, workouts, readiness,
 * proficiency, or the event queue) — it holds no reference to any of those paths.
 */
@HiltViewModel
class YourAscendedViewModel
    @Inject
    constructor(
        private val playerRepository: PlayerRepository,
        classRepository: ClassRepository,
    ) : ViewModel() {
        private val userId = LOCAL_USER_ID

        val uiState: StateFlow<YourAscendedUiState> =
            combine(
                classRepository.observeSelection(userId),
                playerRepository.observeAvatarBodyBase(userId),
            ) { selection, bodyBaseToken ->
                val bodyBase = bodyBaseToken?.let { token -> runCatching { BodyBase.valueOf(token) }.getOrNull() }
                YourAscendedUiState(
                    loading = false,
                    figureClass = productionAvatarClassOf(selection.primaryClassId),
                    bodyBase = bodyBase,
                    needsBodyBaseChoice = bodyBase == null,
                    reducedMotion = false,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), YourAscendedUiState())

        /** Persist the cosmetic body-base choice. Presentation-only — no progression is touched. */
        fun chooseBodyBase(base: BodyBase) {
            viewModelScope.launch { playerRepository.setAvatarBodyBase(userId, base.name) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/**
 * Map a persisted class id to a production avatar class. Only berserker/monk/mage are
 * production-selectable; unbound or any not-yet-shipped class resolves to null → the neutral base
 * figure. Future classes (assassin/fighter/ranger/guardian) are never exposed as production art.
 */
internal fun productionAvatarClassOf(classId: String?): AscendedClass? =
    when (classId) {
        "berserker" -> AscendedClass.BERSERKER
        "monk" -> AscendedClass.MONK
        "mage" -> AscendedClass.MAGE
        else -> null
    }
