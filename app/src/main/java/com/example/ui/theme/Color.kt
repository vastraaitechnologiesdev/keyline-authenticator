package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Static light colors
val LightCleanWhite = Color(0xFFFFFFFF)
val LightCleanOffWhite = Color(0xFFF8FAFC)
val LightCleanSurfaceVariant = Color(0xFFF1F5F9)
val LightCleanBorder = Color(0xFFE2E8F0)
val LightCleanBorderSubtle = Color(0xFFF1F5F9)

val LightSlateTextPrimary = Color(0xFF0F172A)
val LightSlateTextSecondary = Color(0xFF64748B)
val LightSlateTextTertiary = Color(0xFF94A3B8)

// Static dark colors
val DarkCleanWhite = Color(0xFF1E293B)
val DarkCleanOffWhite = Color(0xFF0F172A)
val DarkCleanSurfaceVariant = Color(0xFF334155)
val DarkCleanBorder = Color(0xFF334155)
val DarkCleanBorderSubtle = Color(0xFF1E293B)

val DarkSlateTextPrimary = Color(0xFFF8FAFC)
val DarkSlateTextSecondary = Color(0xFF94A3B8)
val DarkSlateTextTertiary = Color(0xFF64748B)

val LocalIsDarkTheme = staticCompositionLocalOf { false }

// Dynamic adaptive colors based on active theme
val CleanWhite: Color
  @Composable
  @ReadOnlyComposable
  get() = if (LocalIsDarkTheme.current) DarkCleanWhite else LightCleanWhite

val CleanOffWhite: Color
  @Composable
  @ReadOnlyComposable
  get() = if (LocalIsDarkTheme.current) DarkCleanOffWhite else LightCleanOffWhite

val CleanSurfaceVariant: Color
  @Composable
  @ReadOnlyComposable
  get() = if (LocalIsDarkTheme.current) DarkCleanSurfaceVariant else LightCleanSurfaceVariant

val CleanBorder: Color
  @Composable
  @ReadOnlyComposable
  get() = if (LocalIsDarkTheme.current) DarkCleanBorder else LightCleanBorder

val CleanBorderSubtle: Color
  @Composable
  @ReadOnlyComposable
  get() = if (LocalIsDarkTheme.current) DarkCleanBorderSubtle else LightCleanBorderSubtle

val SlateTextPrimary: Color
  @Composable
  @ReadOnlyComposable
  get() = if (LocalIsDarkTheme.current) DarkSlateTextPrimary else LightSlateTextPrimary

val SlateTextSecondary: Color
  @Composable
  @ReadOnlyComposable
  get() = if (LocalIsDarkTheme.current) DarkSlateTextSecondary else LightSlateTextSecondary

val SlateTextTertiary: Color
  @Composable
  @ReadOnlyComposable
  get() = if (LocalIsDarkTheme.current) DarkSlateTextTertiary else LightSlateTextTertiary

val KeylineBlue = Color(0xFF0284C7)
val KeylineBlueDark = Color(0xFF0369A1)
val KeylineBlueLight = Color(0xFFE0F2FE)

val ExpiringRed = Color(0xFFEF4444)
val ExpiringRedLight = Color(0xFFFEE2E2)
val SuccessGreen = Color(0xFF10B981)
val SuccessGreenLight = Color(0xFFD1FAE5)
