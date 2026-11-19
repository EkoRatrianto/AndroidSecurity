package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val JagaLightColorScheme = lightColorScheme(
    primary = DeepNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = DeepNavyContainer,
    onPrimaryContainer = OnDeepNavyContainer,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = StatusReview,
    onTertiary = Color.White,
    tertiaryContainer = StatusReviewContainer,
    background = WhiteBackground,
    onBackground = TextPrimary,
    surface = WhiteBackground,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = WhiteBackground,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateSurfaceVariant,
    outline = SlateOutlineStrong,
    outlineVariant = SlateOutline,
    error = StatusHighRisk,
    onError = Color.White,
    errorContainer = StatusHighRiskContainer,
    onErrorContainer = StatusHighRisk
)

val JagaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JagaPerangkatTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JagaLightColorScheme,
        typography = JagaTypography,
        shapes = JagaShapes,
        content = content
    )
}
