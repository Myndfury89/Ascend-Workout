package com.ascend.prototype

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.feature.ascended.prototype.AscendedStatusFieldScreen

/**
 * Debug-only launcher for the "Your Ascended" dark-field integration prototype — the imported figure
 * composited onto the holographic Status field. Deterministic; read-only; no production Status
 * wiring; art stays debug-only. Debug variant only.
 */
class AscendedStatusFieldPrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AscendTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF04050B)) {
                    AscendedStatusFieldScreen()
                }
            }
        }
    }
}
