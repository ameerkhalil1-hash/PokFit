package com.pokopia.macros.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Pokopia Cozy Island & Cyber-Lab Palette
 * - Warm Driftwood cards, Lush Tropical Foliage greens, Ocean Teals
 * - Ditto Lavender (#D88CE8 to #B865C9)
 * - Porygon Cyber Cyan (#4CC9F0) and Porygon Coral Pink (#E86A73)
 * - Adherence-Neutral feedback colors (no punishing angry red bars)
 */
object PokopiaColors {
    val DeepOceanBg = Color(0xFF0B2229)
    val LagoonSurface = Color(0xFF13333D)
    val DriftwoodCard = Color(0xFF233D3B)
    val DriftwoodBorder = Color(0xFF3E635D)
    val WarmSand = Color(0xFFF6E7C8)
    val TropicalFoliage = Color(0xFF56D38A)
    val LushPalmGreen = Color(0xFF2FA86E)
    val OceanTeal = Color(0xFF25B8C8)

    // Ditto Dual-Protagonist Palette
    val DittoLavenderLight = Color(0xFFD88CE8)
    val DittoLavenderDeep = Color(0xFFB865C9)
    val DittoCheekBlush = Color(0xFFF3A9D8)

    // Porygon Dual-Protagonist Palette
    val PorygonCoral = Color(0xFFE86A73)
    val PorygonCoralDark = Color(0xFFC24953)
    val PorygonCyberCyan = Color(0xFF4CC9F0)
    val PorygonCyberBlue = Color(0xFF2589BD)
    val TriAttackYellow = Color(0xFFFFD166)

    // Berry / Apricorn Macro Orbs (Adherence-Neutral)
    val PechaProteinPink = Color(0xFFF284B6)
    val SitrusCarbGold = Color(0xFFFFCA5C)
    val LumFatMint = Color(0xFF63E2B7)
    val OranCalorieCyan = Color(0xFF4CC9F0)

    val TextCream = Color(0xFFF9F6EE)
    val TextMutedSand = Color(0xFFB8C9C6)
}

private val PokopiaDarkColorScheme = darkColorScheme(
    primary = PokopiaColors.DittoLavenderLight,
    onPrimary = Color(0xFF1D0D24),
    secondary = PokopiaColors.PorygonCyberCyan,
    onSecondary = Color(0xFF06232D),
    tertiary = PokopiaColors.TropicalFoliage,
    background = PokopiaColors.DeepOceanBg,
    onBackground = PokopiaColors.TextCream,
    surface = PokopiaColors.LagoonSurface,
    onSurface = PokopiaColors.TextCream,
    surfaceVariant = PokopiaColors.DriftwoodCard,
    onSurfaceVariant = PokopiaColors.TextMutedSand
)

val PokopiaTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        color = PokopiaColors.TextCream
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = PokopiaColors.TextCream
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = PokopiaColors.TextCream
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = PokopiaColors.TextCream
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = PokopiaColors.TextMutedSand
    )
)

@Composable
fun PokopiaMacrosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PokopiaDarkColorScheme,
        typography = PokopiaTypography,
        content = content
    )
}
