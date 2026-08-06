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
import com.ascend.feature.dashboard.prototype.AscendMotionPrototypeScreen

/**
 * Debug-only launcher for the HTML-faithful **motion-review** prototype — the sigil-dominant review
 * viewport (not the production Status information panel). Its own icon in debug builds ("Ascend
 * Motion Prototype"); fake data only, no Hilt, no navigation, no production wiring. Debug variant only.
 */
class AscendMotionPrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AscendTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF04050B)) {
                    AscendMotionPrototypeScreen()
                }
            }
        }
    }
}
