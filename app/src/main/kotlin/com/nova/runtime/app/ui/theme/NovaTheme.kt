package com.nova.runtime.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Quiet light palette. Legacy aliases keep older screens on the same design tokens.
val NovaAccentBlue = Color(0xFF245BC4)
val NovaAkzidenzRed = NovaAccentBlue
val NovaPrimaryRedContainer = Color(0xFF245BC4)
val NovaPureBlack = Color(0xFF202124)
val NovaOffWhite = Color(0xFFF4F5F7)
val NovaSurfaceLowest = Color(0xFFFFFFFF)
val NovaSurfaceLow = Color(0xFFF0F2F5)
val NovaSurfaceContainer = Color(0xFFEBEEF3)
val NovaSurfaceHigh = Color(0xFFE6EAF0)
val NovaSurfaceHighest = Color(0xFFE1E5EC)
val NovaSurfaceContainerHighest = Color(0xFFE1E5EC)
val NovaSurfaceDim = Color(0xFFDCE1E9)
val NovaSecondaryGrey = Color(0xFF626873)
val NovaOnSecondaryVariant = Color(0xFF626873)
val NovaOutlineGrey = Color(0xFF737B88)
val NovaErrorRed = Color(0xFFBA1A1A)

// Legacy & UI Color Aliases for backward compatibility
val NovaNeonOrange = NovaAkzidenzRed
val NovaSystemBlue = NovaAccentBlue
val NovaSystemGreen = Color(0xFF28734D)
val NovaSystemRed = NovaErrorRed
val NovaSystemOrange = NovaAkzidenzRed
val NovaSystemGray = NovaSecondaryGrey
val NovaCyanAccent = NovaAkzidenzRed
val NovaIndigoAccent = NovaPureBlack
val NovaEmeraldGreen = NovaSystemGreen
val NovaDarkBackground = NovaOffWhite
val NovaSurfaceDark = NovaSurfaceHighest
val NovaSurfaceVariant = NovaSurfaceHighest
val NovaTextPrimary = NovaPureBlack
val NovaTextSecondary = NovaSecondaryGrey

object NovaColors {
    val background @Composable get() = MaterialTheme.colorScheme.background
    val surface @Composable get() = MaterialTheme.colorScheme.surface
    val surfaceElevated @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val surfaceContainerLowest @Composable get() = NovaSurfaceLowest
    val surfaceContainerLow @Composable get() = NovaSurfaceLow
    val surfaceContainer @Composable get() = NovaSurfaceContainer
    val surfaceContainerHigh @Composable get() = NovaSurfaceHigh
    val surfaceContainerHighest @Composable get() = NovaSurfaceHighest
    val surfaceDim @Composable get() = NovaSurfaceDim
    val textPrimary @Composable get() = MaterialTheme.colorScheme.onBackground
    val textSecondary @Composable get() = NovaSecondaryGrey
    val primary @Composable get() = MaterialTheme.colorScheme.primary
    val accent @Composable get() = MaterialTheme.colorScheme.primary
    val neonOrange @Composable get() = NovaAkzidenzRed
    val success @Composable get() = NovaSystemGreen
    val error @Composable get() = NovaErrorRed
    val cardBorder @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val glassSurface @Composable get() = NovaSurfaceLowest
    val glassBorder @Composable get() = MaterialTheme.colorScheme.outlineVariant
}

private val NovaLightColorScheme = lightColorScheme(
    primary = NovaAkzidenzRed,
    onPrimary = NovaSurfaceLowest,
    primaryContainer = Color(0xFFE8EFFF),
    onPrimaryContainer = NovaAkzidenzRed,
    secondary = NovaSecondaryGrey,
    onSecondary = NovaSurfaceLowest,
    secondaryContainer = NovaSurfaceHighest,
    onSecondaryContainer = NovaOnSecondaryVariant,
    tertiary = Color(0xFF575959),
    onTertiary = NovaSurfaceLowest,
    background = NovaOffWhite,
    onBackground = NovaPureBlack,
    surface = NovaSurfaceLowest,
    onSurface = NovaPureBlack,
    surfaceVariant = NovaSurfaceHighest,
    onSurfaceVariant = NovaSecondaryGrey,
    surfaceTint = Color(0xFF245BC4),
    outline = NovaOutlineGrey,
    outlineVariant = NovaSurfaceDim,
    error = NovaErrorRed,
    onError = NovaSurfaceLowest,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
)

// Mobile typography: predictable line height and system font scaling.
private val NovaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // 800
        fontSize = 40.sp,
        letterSpacing = (-0.03).sp,
        lineHeight = 48.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // 700
        fontSize = 28.sp,
        letterSpacing = (-0.02).sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // 700
        fontSize = 24.sp,
        letterSpacing = (-0.02).sp,
        lineHeight = 32.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 16.sp,
        letterSpacing = (-0.01).sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, // 700
        fontSize = 14.sp,
        letterSpacing = 0.05.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 14.sp,
    ),
)

// Rounded surfaces and native controls, without decorative elevation.
private val NovaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun NovaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NovaLightColorScheme,
        typography = NovaTypography,
        shapes = NovaShapes,
        content = content,
    )
}
