package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Everything the diagnostics panel prints — assembled by the screen from pure prototype state. */
data class DiagnosticsInfo(
    val state: String,
    val variant: String,
    val speed: String,
    val reducedMotion: Boolean,
    val effectsQuality: String,
    val particleCount: Int,
    val particlesActive: Boolean,
    val scanActive: Boolean,
    val sigilIdleActive: Boolean,
    val infinitePaused: Boolean,
    val entranceMode: String,
    val majorEventActive: Boolean,
    val approxEntranceMs: Int,
    val deviceWidth: String,
)

/**
 * A rough frame-time meter using Compose's [withFrameNanos] — no new dependency. Only runs while
 * [active]; returns a smoothed millisecond estimate (≈16.7 ms ≈ 60 fps). Approximate by design;
 * exact profiling belongs to Studio's tools, which this deliberately does not replace.
 */
@Composable
fun rememberApproxFrameMs(active: Boolean): Float {
    var frameMs by remember { mutableFloatStateOf(0f) }
    if (active) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last != 0L) {
                        val delta = (now - last) / 1_000_000f
                        frameMs = if (frameMs == 0f) delta else frameMs * 0.9f + delta * 0.1f
                    }
                    last = now
                }
            }
        }
    }
    return frameMs
}

/** The debug-only diagnostics panel. Restrained monospace read-out; never shown in production. */
@Composable
fun StatusDiagnosticsPanel(
    info: DiagnosticsInfo,
    frameMs: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xFF0B0F16).copy(alpha = 0.95f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp)) {
            Line("state", info.state)
            Line("class", info.variant)
            Line("entrance", "${info.entranceMode}${if (info.majorEventActive) " (major)" else ""}")
            Line("speed", info.speed)
            Line("reduced motion", info.reducedMotion.toString())
            Line("effects", info.effectsQuality)
            Line("particles", "${info.particleCount} ${if (info.particlesActive) "active" else "paused"}")
            Line("scan", if (info.scanActive) "on" else "off")
            Line("sigil idle", if (info.sigilIdleActive) "on" else "off")
            Line("infinite paused", info.infinitePaused.toString())
            Line("approx entrance", "${info.approxEntranceMs} ms")
            Line("frame time", if (frameMs > 0f) "~${"%.1f".format(frameMs)} ms" else "—")
            Line("device width", info.deviceWidth)
        }
    }
}

@Composable
private fun Line(
    key: String,
    value: String,
) {
    Text(
        "$key: $value",
        color = Color(0xFF9AE6DA),
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
    )
}
