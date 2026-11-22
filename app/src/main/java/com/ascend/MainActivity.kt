package com.ascend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ascend.core.designsystem.motion.ProvideMotionSpec
import com.ascend.core.designsystem.motion.ReducedMotionController
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.navigation.AscendRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** System animator scale + persisted in-app toggle, resolved once for the whole app (PR 5b). */
    @Inject
    lateinit var reducedMotionController: ReducedMotionController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val reducedMotionFlags = reducedMotionController.observe()
        setContent {
            // App-root LocalMotionSpec: every screen below (incl. the sigils) reads this one spec.
            ProvideMotionSpec(reducedMotionFlags) {
                AscendTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AscendRoot()
                    }
                }
            }
        }
    }
}
