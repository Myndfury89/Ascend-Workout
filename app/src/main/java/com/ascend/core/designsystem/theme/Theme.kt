package com.ascend.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val AscendDarkColors = darkColorScheme(
    primary = Aqua,
    onPrimary = AquaOn,
    primaryContainer = AquaDeep,
    onPrimaryContainer = Aqua,
    secondary = Ember,
    onSecondary = EmberDeep,
    secondaryContainer = EmberDeep,
    onSecondaryContainer = Ember,
    tertiary = Slate,
    onTertiary = Charcoal,
    tertiaryContainer = SlateDeep,
    onTertiaryContainer = Slate,
    background = Charcoal,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalSurfaceHigh,
    onSurfaceVariant = TextMuted,
    outline = CharcoalOutline,
    error = Danger,
    onError = TextPrimary,
    errorContainer = DangerDeep,
    onErrorContainer = Danger,
)

private val AscendLightColors = lightColorScheme(
    primary = LightAqua,
    secondary = LightEmber,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightOnSurface,
    error = Danger,
)

@Composable
fun AscendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // App identity is dark-first; dynamic color is opt-in via settings later.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AscendDarkColors
        else -> AscendLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AscendTypography,
        shapes = AscendShapes,
        content = content,
    )
}
