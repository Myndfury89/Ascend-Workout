package com.ascend.prototype

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.feature.dungeon.prototype.DungeonPrototypeScreen

/**
 * Debug-only launcher for the fake party Dungeon prototype. Appears as its own icon in debug
 * builds ("Ascend Dungeon Prototype") and opens the fully-simulated prototype — no Bluetooth, no
 * real users, no networking, no Hilt. Ships only in the debug variant.
 */
class DungeonPrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AscendTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DungeonPrototypeScreen()
                }
            }
        }
    }
}
