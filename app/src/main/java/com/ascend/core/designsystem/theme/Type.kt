package com.ascend.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System font family keeps the app original (no bundled third-party typefaces).
// Weights and tracking are tuned for a cinematic, high-contrast feel.
internal val AscendTypography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.5.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 26.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.15.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.4.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.5.sp,
            ),
    )
