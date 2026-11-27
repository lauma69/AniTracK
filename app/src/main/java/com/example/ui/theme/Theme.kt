package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.example.model.ThemeModePreference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val LocalAppColors = compositionLocalOf { DarkAppColors }
val LocalKinetoscopeIsDark = compositionLocalOf { true }
val LocalDynamicAccentColor = compositionLocalOf { ChampagneGold }

@Immutable
data class AccentPresetSpec(
    val key: String,
    val label: String,
    val swatchColor: Color
)

@Immutable
data class ResolvedAccentPalette(
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color
)

val BuiltInAccentPresets: List<AccentPresetSpec> = listOf(
    AccentPresetSpec("CHAMPAGNE_GOLD", "Champagne Gold", Color(0xFFE6C586)),
    AccentPresetSpec("SAKURA_PINK", "Sakura Rose", Color(0xFFEB9AC0)),
    AccentPresetSpec("EMERALD_MINT", "Emerald Mint", Color(0xFF5CE098)),
    AccentPresetSpec("INDIGO_VIOLET", "Midnight Indigo", Color(0xFF9C8FFF))
)

fun normalizeAccentThemeKey(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return "CHAMPAGNE_GOLD"
    if (trimmed.uppercase().startsWith("CUSTOM:")) {
        val parsedHex = sanitizeHexColor(trimmed.substringAfter(":"))
        return if (parsedHex != null) "CUSTOM:$parsedHex" else "CHAMPAGNE_GOLD"
    }
    return when (trimmed.uppercase()) {
        "SAKURA_PINK", "ROSE" -> "SAKURA_PINK"
        "EMERALD_MINT", "EMERALD" -> "EMERALD_MINT"
        "INDIGO_VIOLET", "INDIGO" -> "INDIGO_VIOLET"
        "CHAMPAGNE_GOLD", "GOLD", "COSMIC_CYAN" -> "CHAMPAGNE_GOLD"
        else -> "CHAMPAGNE_GOLD"
    }
}

fun sanitizeHexColor(input: String?): String? {
    val clean = input?.trim()?.removePrefix("#")?.uppercase().orEmpty()
    val expanded = when (clean.length) {
        3 -> clean.map { "$it$it" }.joinToString("")
        6 -> clean
        else -> return null
    }
    if (!expanded.all { it in '0'..'9' || it in 'A'..'F' }) return null
    return "#$expanded"
}

fun parseHexColor(hex: String?): Color? {
    val normalized = sanitizeHexColor(hex) ?: return null
    val rgb = normalized.removePrefix("#").toLongOrNull(16) ?: return null
    return Color((0xFF000000L or rgb).toInt())
}

fun extractCustomAccentHex(accentTheme: String?): String? {
    val normalized = normalizeAccentThemeKey(accentTheme)
    if (!normalized.startsWith("CUSTOM:")) return null
    return sanitizeHexColor(normalized.substringAfter(":"))
}

private fun rgbToHsl(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val maxVal = max(r, max(g, b))
    val minVal = min(r, min(g, b))
    val delta = maxVal - minVal
    val l = (maxVal + minVal) / 2f
    val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
    val h = when {
        delta == 0f -> 0f
        maxVal == r -> 60f * (((g - b) / delta) % 6f)
        maxVal == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    val sat = s.coerceIn(0f, 1f)
    val lig = l.coerceIn(0f, 1f)
    val c = (1f - abs(2f * lig - 1f)) * sat
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = lig - c / 2f
    val (r1, g1, b1) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

private fun accessibleOnColor(background: Color): Color {
    return if (background.luminance() > 0.38f) Color(0xFF101218) else Color(0xFFFFFFFF)
}

fun resolveAccentPalette(accentTheme: String, isDark: Boolean): ResolvedAccentPalette {
    val key = normalizeAccentThemeKey(accentTheme)
    if (key.startsWith("CUSTOM:")) {
        val rawColor = parseHexColor(key.substringAfter(":")) ?: ChampagneGold
        val hsl = rgbToHsl(rawColor)
        val h = hsl[0]
        val s = hsl[1].coerceAtLeast(0.22f)
        return if (isDark) {
            val primary = hslToColor(h, min(0.92f, s), hsl[2].coerceIn(0.60f, 0.80f))
            val secondary = hslToColor(h, min(0.92f, s * 0.95f), (hsl[2].coerceIn(0.60f, 0.80f) - 0.08f).coerceAtLeast(0.48f))
            val highlight = hslToColor(h, min(0.95f, s), min(0.92f, hsl[2].coerceIn(0.60f, 0.80f) + 0.14f))
            val container = lerp(ObsidianSurface2, primary, 0.24f)
            ResolvedAccentPalette(
                primary = primary,
                secondary = secondary,
                highlight = highlight,
                onPrimary = accessibleOnColor(primary),
                primaryContainer = container,
                onPrimaryContainer = highlight
            )
        } else {
            // Ensure high WCAG contrast on white/alabaster light surfaces
            val lightL = hsl[2].coerceIn(0.30f, 0.44f)
            val primary = hslToColor(h, min(0.92f, max(0.38f, s)), lightL)
            val secondary = hslToColor(h, min(0.95f, max(0.40f, s)), (lightL - 0.06f).coerceAtLeast(0.24f))
            val highlight = hslToColor(h, min(0.95f, max(0.42f, s)), (lightL - 0.08f).coerceAtLeast(0.22f))
            val container = lerp(LightPorcelainCard, primary, 0.14f)
            ResolvedAccentPalette(
                primary = primary,
                secondary = secondary,
                highlight = highlight,
                onPrimary = accessibleOnColor(primary),
                primaryContainer = container,
                onPrimaryContainer = highlight
            )
        }
    }

    return when (key) {
        "SAKURA_PINK" -> if (isDark) {
            ResolvedAccentPalette(
                primary = Color(0xFFEB9AC0),
                secondary = Color(0xFFF5BFE0),
                highlight = Color(0xFFFCE0F0),
                onPrimary = Color(0xFF2B0B1B),
                primaryContainer = Color(0xFF471830),
                onPrimaryContainer = Color(0xFFFCE0F0)
            )
        } else {
            ResolvedAccentPalette(
                primary = Color(0xFFAD2E75),
                secondary = Color(0xFF8C215C),
                highlight = Color(0xFF851D57),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFCE4F0),
                onPrimaryContainer = Color(0xFF4A0D2E)
            )
        }

        "EMERALD_MINT" -> if (isDark) {
            ResolvedAccentPalette(
                primary = ObsidianEmerald,
                secondary = Color(0xFF9BEFC3),
                highlight = Color(0xFFC2F7DC),
                onPrimary = Color(0xFF042614),
                primaryContainer = Color(0xFF103B24),
                onPrimaryContainer = Color(0xFFC2F7DC)
            )
        } else {
            ResolvedAccentPalette(
                primary = LightEmerald,
                secondary = Color(0xFF0E613E),
                highlight = Color(0xFF0D5939),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFDDF5E9),
                onPrimaryContainer = Color(0xFF063822)
            )
        }

        "INDIGO_VIOLET" -> if (isDark) {
            ResolvedAccentPalette(
                primary = Color(0xFF9C8FFF),
                secondary = Color(0xFFC0B6FF),
                highlight = Color(0xFFE0DAFF),
                onPrimary = Color(0xFF140D3A),
                primaryContainer = Color(0xFF28205E),
                onPrimaryContainer = Color(0xFFE0DAFF)
            )
        } else {
            ResolvedAccentPalette(
                primary = Color(0xFF4F3ED4),
                secondary = Color(0xFF3E2EB8),
                highlight = Color(0xFF3B2AB0),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE8E5FF),
                onPrimaryContainer = Color(0xFF1E146B)
            )
        }

        else -> if (isDark) {
            ResolvedAccentPalette(
                primary = ChampagneGold,
                secondary = ChampagneGoldBright,
                highlight = ChampagneGoldSoft,
                onPrimary = ChampagneOnGold,
                primaryContainer = ChampagneGoldDeep,
                onPrimaryContainer = ChampagneGoldSoft
            )
        } else {
            ResolvedAccentPalette(
                primary = LightBronzeGold,
                secondary = LightBronzeSoft,
                highlight = LightBronzeSoft,
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = LightChampagneTint,
                onPrimaryContainer = Color(0xFF3D2700)
            )
        }
    }
}

private val ObsidianGoldDarkScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = ChampagneOnGold,
    primaryContainer = ChampagneGoldDeep,
    onPrimaryContainer = ChampagneGoldSoft,
    secondary = ChampagneGoldBright,
    onSecondary = ChampagneOnGold,
    secondaryContainer = ObsidianSurface2,
    onSecondaryContainer = IvoryText,
    tertiary = ChampagneGoldSoft,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianSurface2,
    onSurfaceVariant = AshText,
    surfaceContainer = ObsidianSurface2,
    surfaceContainerHigh = ObsidianSurface3,
    surfaceContainerLow = ObsidianSurface,
    outline = HairlineSubtle,
    outlineVariant = HairlineStrong,
    error = ObsidianCrimson,
    onError = Color.White
)

private val WarmIvoryLightScheme = lightColorScheme(
    primary = LightBronzeGold,
    onPrimary = Color.White,
    primaryContainer = LightChampagneTint,
    onPrimaryContainer = LightEspressoInk,
    secondary = LightBronzeSoft,
    onSecondary = Color.White,
    secondaryContainer = LightWarmParchment,
    onSecondaryContainer = LightEspressoInk,
    tertiary = LightBronzeGold,
    onTertiary = Color.White,
    background = LightIvoryCanvas,
    onBackground = LightEspressoInk,
    surface = LightPorcelainCard,
    onSurface = LightEspressoInk,
    surfaceVariant = LightElevatedSand,
    onSurfaceVariant = LightMutedUmber,
    surfaceContainer = LightPorcelainCard,
    surfaceContainerHigh = LightElevatedSand,
    surfaceContainerLow = LightWarmParchment,
    outline = LightHairlineBorder,
    outlineVariant = LightHairlineBorder,
    error = LightCrimson,
    onError = Color.White
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppRadius.xs),
    small = RoundedCornerShape(AppRadius.sm),
    medium = RoundedCornerShape(AppRadius.md),
    large = RoundedCornerShape(AppRadius.lg),
    extraLarge = RoundedCornerShape(AppRadius.xl)
)

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
fun AnimeUniverseTheme(
    themeMode: ThemeModePreference = ThemeModePreference.DARK,
    accentTheme: String = "CHAMPAGNE_GOLD",
    dynamicPosterAccent: Color? = null,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeModePreference.SYSTEM -> isSystemInDarkTheme()
        ThemeModePreference.DARK -> true
        ThemeModePreference.LIGHT -> false
    }

    val baseColors = if (isDark) DarkAppColors else LightAppColors
    val baseScheme = if (isDark) ObsidianGoldDarkScheme else WarmIvoryLightScheme
    val accent = resolveAccentPalette(accentTheme = accentTheme, isDark = isDark)

    val appColors = baseColors.copy(
        accentPrimary = accent.primary,
        accentSecondary = accent.secondary,
        highlight = accent.highlight,
        onAccentPrimary = accent.onPrimary,
        borderStrong = accent.primary.copy(alpha = if (isDark) 0.42f else 0.48f)
    )
    val finalScheme = baseScheme.copy(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        secondary = accent.secondary,
        onSecondary = accent.onPrimary,
        tertiary = accent.highlight,
        onTertiary = accent.onPrimary
    )

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalKinetoscopeIsDark provides isDark,
        LocalDynamicAccentColor provides accent.primary
    ) {
        MaterialTheme(
            colorScheme = finalScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
