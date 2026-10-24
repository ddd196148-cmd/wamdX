package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppThemeMode
import com.example.ui.i18n.ArabicAppStrings
import com.example.ui.i18n.EnglishAppStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings

private val WamdXDarkColorScheme = darkColorScheme(
  primary = ElectricBluePrimary,
  onPrimary = OnElectricBlue,
  primaryContainer = ElectricBlueContainer,
  onPrimaryContainer = OnElectricBlueContainer,
  secondary = DeepPurpleSecondary,
  onSecondary = OnDeepPurple,
  secondaryContainer = DeepPurpleContainer,
  onSecondaryContainer = OnDeepPurpleContainer,
  tertiary = CyanSparkTertiary,
  tertiaryContainer = CyanSparkContainer,
  background = ObsidianBlack,
  onBackground = DarkOnBackground,
  surface = MidnightSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = MidnightSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  surfaceContainerHigh = MidnightSurfaceElevated,
  outline = DarkOutline,
  error = ErrorRoseDark,
  errorContainer = ErrorContainerDark,
)

private val WamdXLightColorScheme = lightColorScheme(
  primary = CobaltBluePrimary,
  onPrimary = OnCobaltBlue,
  primaryContainer = CobaltBlueContainer,
  onPrimaryContainer = OnCobaltBlueContainer,
  secondary = RoyalPurpleSecondary,
  onSecondary = OnRoyalPurple,
  secondaryContainer = RoyalPurpleContainer,
  onSecondaryContainer = OnRoyalPurpleContainer,
  background = AlabasterBackground,
  onBackground = LightOnBackground,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  surfaceContainerHigh = LightSurfaceElevated,
  outline = LightOutline,
  error = ErrorRoseLight,
  errorContainer = ErrorContainerLight,
)

val WamdXShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun WamdXTheme(
  language: AppLanguage = AppLanguage.AR,
  themeMode: AppThemeMode = AppThemeMode.DARK,
  content: @Composable () -> Unit,
) {
  val useDark = when (themeMode) {
    AppThemeMode.DARK -> true
    AppThemeMode.LIGHT -> false
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val colorScheme = if (useDark) WamdXDarkColorScheme else WamdXLightColorScheme
  val strings = if (language == AppLanguage.AR) ArabicAppStrings else EnglishAppStrings
  val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

  CompositionLocalProvider(
    LocalLayoutDirection provides layoutDirection,
    LocalAppStrings provides strings,
    LocalAppLanguage provides language,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = WamdXShapes,
      content = content,
    )
  }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  WamdXTheme(
    language = AppLanguage.AR,
    themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
    content = content,
  )
}
