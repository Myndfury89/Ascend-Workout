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
import com.ascend.feature.dashboard.prototype.RevisedStatusPrototypeScreen

/**
 * Debug-only launcher for the revised Status motion prototype. It appears as its own icon in
 * debug builds ("Ascend Status Prototype") and opens the fake-data prototype directly — no
 * Hilt, no navigation, no production wiring. Ships only in the debug variant.
 */
class StatusPrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AscendTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF05070B)) {
                    RevisedStatusPrototypeScreen()
                }
            }
        }
    }
}
