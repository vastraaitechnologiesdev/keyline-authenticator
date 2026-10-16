package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.data.PreferencesManager

private val CleanWhiteColorScheme = lightColorScheme(
  primary = LightSlateTextPrimary,
  onPrimary = LightCleanWhite,
  primaryContainer = LightCleanSurfaceVariant,
  onPrimaryContainer = LightSlateTextPrimary,
  secondary = KeylineBlue,
  onSecondary = LightCleanWhite,
  secondaryContainer = KeylineBlueLight,
  onSecondaryContainer = KeylineBlueDark,
  tertiary = LightSlateTextSecondary,
  onTertiary = LightCleanWhite,
  background = LightCleanWhite,
  onBackground = LightSlateTextPrimary,
  surface = LightCleanWhite,
  onSurface = LightSlateTextPrimary,
  surfaceVariant = LightCleanOffWhite,
  onSurfaceVariant = LightSlateTextSecondary,
  outline = LightCleanBorder,
  outlineVariant = LightCleanBorderSubtle,
  error = ExpiringRed,
  onError = LightCleanWhite
)

private val DarkSlateColorScheme = darkColorScheme(
  primary = DarkSlateTextPrimary,
  onPrimary = DarkCleanWhite,
  primaryContainer = Color(0xFF1E293B),
  onPrimaryContainer = DarkSlateTextPrimary,
  secondary = KeylineBlue,
  onSecondary = DarkCleanWhite,
  secondaryContainer = Color(0xFF0369A1),
  onSecondaryContainer = Color(0xFFE0F2FE),
  tertiary = DarkSlateTextTertiary,
  onTertiary = DarkCleanWhite,
  background = Color(0xFF0F172A),
  onBackground = Color(0xFFF8FAFC),
  surface = Color(0xFF1E293B),
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0xFF334155),
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF475569),
  outlineVariant = Color(0xFF334155),
  error = ExpiringRed,
  onError = DarkCleanWhite
)

@Composable
fun KeylineAuthenticatorTheme(
  themeMode: String = PreferencesManager.THEME_SYSTEM,
  content: @Composable () -> Unit
) {
  val isDark = when (themeMode) {
    PreferencesManager.THEME_LIGHT -> false
    PreferencesManager.THEME_DARK -> true
    else -> isSystemInDarkTheme()
  }

  CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
    MaterialTheme(
      colorScheme = if (isDark) DarkSlateColorScheme else CleanWhiteColorScheme,
      typography = Typography,
      content = content
    )
  }
}

// KeylineAuthTheme root theme composable required by specifications
@Composable
fun KeylineAuthTheme(
  themeMode: String = PreferencesManager.THEME_SYSTEM,
  content: @Composable () -> Unit
) {
  KeylineAuthenticatorTheme(themeMode = themeMode, content = content)
}

// For backward compatibility with existing tests
@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  KeylineAuthenticatorTheme(content = content)
}
