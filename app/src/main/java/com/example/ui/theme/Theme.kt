package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = PureWhite,
    onPrimary = TrueBlack,
    secondary = GrayLight,
    onSecondary = TrueBlack,
    background = TrueBlack,
    surface = TrueBlack,
    surfaceVariant = GrayDark,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onSurfaceVariant = GrayMedium,
    outline = GlassBorderLight,
    error = ErrorColor
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
      colorScheme = DarkColorScheme, 
      typography = Typography, 
      content = content
  )
}
