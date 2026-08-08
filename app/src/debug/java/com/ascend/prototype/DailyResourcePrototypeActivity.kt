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
import com.ascend.feature.dashboard.prototype.resources.DailyResourcePrototypeScreen

/**
 * Debug-only launcher for the HP/MP/XP daily-resource HUD prototype. Deterministic fake inputs run
 * through the real resolvers; read-only, no Hilt, no repositories, no schema. Debug variant only.
 */
class DailyResourcePrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AscendTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF04050B)) {
                    DailyResourcePrototypeScreen()
                }
            }
        }
    }
}
