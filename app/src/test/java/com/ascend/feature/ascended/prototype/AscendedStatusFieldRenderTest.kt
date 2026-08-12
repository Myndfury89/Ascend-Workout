package com.ascend.feature.ascended.prototype

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.feature.ascended.model.AscendedClass
import com.ascend.feature.ascended.model.BodyBase
import com.ascend.feature.ascended.presentation.AscendedStatusField
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM render check for the dark-field composite. Ambient off keeps it static (no drifting
 * fog/particles), so the figure composites onto the dark field headlessly. The animated field is
 * validated on-device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class AscendedStatusFieldRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the figure composites onto the static dark field`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                AscendedStatusField(
                    ascendedClass = AscendedClass.GUARDIAN,
                    bodyBase = BodyBase.MALE,
                    modifier = Modifier.size(300.dp, 480.dp),
                    reducedMotion = true,
                )
            }
        }
        compose.onNodeWithContentDescription("Guardian Male figure").assertExists()
    }

    @Test
    fun `a different class resolves its art on the dark field`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                AscendedStatusField(
                    ascendedClass = AscendedClass.MONK,
                    bodyBase = BodyBase.FEMALE,
                    modifier = Modifier.size(300.dp, 480.dp),
                    reducedMotion = true,
                )
            }
        }
        compose.onNodeWithContentDescription("Monk Female figure").assertExists()
    }
}
