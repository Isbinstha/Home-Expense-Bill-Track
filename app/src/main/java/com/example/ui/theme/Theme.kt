package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    secondary = GroceriesPurple,
    onSecondary = OnPrimaryBlue,
    secondaryContainer = GroceriesContainer,
    onSecondaryContainer = AppTextDark,
    tertiary = AnalyticsGray,
    onTertiary = OnPrimaryBlue,
    tertiaryContainer = AnalyticsContainer,
    onTertiaryContainer = AppTextDark,
    error = HealthRed,
    onError = OnHealthRed,
    errorContainer = HealthContainer,
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF22242B),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF44474F)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    secondary = GroceriesPurple,
    onSecondary = OnPrimaryBlue,
    secondaryContainer = GroceriesContainer,
    onSecondaryContainer = AppTextDark,
    tertiary = AnalyticsGray,
    onTertiary = OnPrimaryBlue,
    tertiaryContainer = AnalyticsContainer,
    onTertiaryContainer = AppTextDark,
    error = HealthRed,
    onError = OnHealthRed,
    errorContainer = HealthContainer,
    background = AppBackground,
    onBackground = AppTextDark,
    surface = AppBackground,
    onSurface = AppTextDark,
    surfaceVariant = AnalyticsContainer,
    onSurfaceVariant = AppTextGrey,
    outline = AppOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For Professional Polish, we prioritize our premium custom color schemes
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
