package dog.srh.locateme.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val darkTheme = Colors(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    error = errorDark,
    onError = onErrorDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    onSurfaceVariant = onSurfaceVariantDark,
)


private val lightTheme = Colors(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    error = errorLight,
    onError = onErrorLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    onSurfaceVariant = onSurfaceVariantLight,
)

@Composable
fun LightTheme(
    content: @Composable() () -> Unit
) {
    MaterialTheme(
        colors = lightTheme,
        typography = AppTypography,
        content = content
    )
}

@Composable
fun DarkTheme(
    content: @Composable() () -> Unit
) {
    MaterialTheme(
        colors = darkTheme,
        typography = AppTypography,
        content = content
    )
}
