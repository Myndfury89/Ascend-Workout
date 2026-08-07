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
import com.ascend.feature.ascended.prototype.YourAscendedPrototypeScreen

/**
 * Debug-only launcher for the image-driven "Your Ascended" character prototype. Its own icon in
 * debug builds; deterministic fake data only, no Hilt, no navigation, no production wiring, no
 * schema. Figures are imported drawables (add them under app/src/debug/res/drawable/); until then
 * each figure shows a placeholder naming the file to drop in. Debug variant only.
 */
class YourAscendedPrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AscendTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFEDEBE6)) {
                    YourAscendedPrototypeScreen()
                }
            }
        }
    }
}
