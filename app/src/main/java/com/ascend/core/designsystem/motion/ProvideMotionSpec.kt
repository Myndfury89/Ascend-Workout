package com.ascend.core.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow

/** The flags assumed before the first emission: nothing reduced, so the app starts on [MotionSpec.Full]. */
private val InitialReducedMotionFlags = ReducedMotionFlags(system = false, inApp = false)

/** The app-wide [MotionSpec] for a set of reduced-motion flags (system animator scale 0 OR in-app toggle). */
fun appMotionSpec(flags: ReducedMotionFlags): MotionSpec = if (flags.effective) MotionSpec.Reduced else MotionSpec.Full

/**
 * App-root motion provider (sigil spec §4.2, PR 5b). Collects [reducedMotionFlags] — the device
 * "Animator duration scale" (0 == animations off) combined with the persisted in-app reduced-motion
 * toggle — and provides the resolved spec through [LocalMotionSpec], so the sigils and anything
 * else below read one source of truth instead of threading their own motion plumbing.
 *
 * Until the first emission the spec is [MotionSpec.Full], the same as [LocalMotionSpec]'s default,
 * so default settings render exactly as before. Pass a flow created once (not per recomposition).
 */
@Suppress("FunctionNaming") // Compose convention.
@Composable
fun ProvideMotionSpec(
    reducedMotionFlags: Flow<ReducedMotionFlags>,
    content: @Composable () -> Unit,
) {
    val flags by reducedMotionFlags.collectAsStateWithLifecycle(initialValue = InitialReducedMotionFlags)
    CompositionLocalProvider(LocalMotionSpec provides appMotionSpec(flags), content = content)
}
