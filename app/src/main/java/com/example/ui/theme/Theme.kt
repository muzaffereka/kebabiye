package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SizzlingRed,
    secondary = SaffronOrange,
    tertiary = NeonYellow,
    background = CharcoalDark,
    surface = SlateGrey,
    onPrimary = SoftWhite,
    onSecondary = DeepBlack,
    onTertiary = DeepBlack,
    onBackground = SoftWhite,
    onSurface = SoftWhite,
    surfaceVariant = CharcoalDark,
    onSurfaceVariant = SoftWhite
  )

private val LightColorScheme =
  darkColorScheme( // Enforce dark/dim look even if light mode is selected for kebab shop vibe
    primary = SizzlingRed,
    secondary = SaffronOrange,
    tertiary = NeonYellow,
    background = DeepBlack,
    surface = SlateGrey,
    onPrimary = SoftWhite,
    onSecondary = DeepBlack,
    onTertiary = DeepBlack,
    onBackground = SoftWhite,
    onSurface = SoftWhite,
    surfaceVariant = CharcoalDark,
    onSurfaceVariant = SoftWhite
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+ (disable for custom branded branding consistency)
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
