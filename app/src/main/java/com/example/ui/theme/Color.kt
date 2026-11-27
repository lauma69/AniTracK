package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ============================================================================
// OBSIDIAN & CHAMPAGNE GOLD — DARK PALETTE (Default)
// Balanced slate-obsidian canvas (#0C0E14) with distinct tonal card layers and AA+ contrast
// ============================================================================
val ObsidianBlack = Color(0xFF0C0E14)
val ObsidianSurface = Color(0xFF131620)
val ObsidianSurface2 = Color(0xFF1A1E2B)
val ObsidianSurface3 = Color(0xFF24293A)

val ChampagneGold = Color(0xFFE6C586)
val ChampagneGoldSoft = Color(0xFFF6E6C4)
val ChampagneGoldBright = Color(0xFFD6B067)
val ChampagneGoldBronze = Color(0xFFB58C45)
val ChampagneGoldDeep = Color(0xFF3A2D15)
val ChampagneOnGold = Color(0xFF141008)

val IvoryText = Color(0xFFF4F6FA)
val AshText = Color(0xFFB9C0CF)
val AshDim = Color(0xFF8E96AA)
val AshDark = Color(0xFF3E4558)

val HairlineSubtle = Color(0x1EFFFFFF)      // ~12% crisp subtle edge
val HairlineStrong = Color(0x33FFFFFF)      // ~20% strong edge

val ObsidianEmerald = Color(0xFF5CE098)
val ObsidianAmber = Color(0xFFF5BA3B)
val ObsidianCrimson = Color(0xFFF0646F)

// ============================================================================
// GALLERY PORCELAIN & BRONZE — LIGHT PALETTE (Optional via Settings)
// Crisp daylight alabaster surfaces with pure white cards and rich deep ink
// ============================================================================
val LightIvoryCanvas = Color(0xFFF5F6FA)
val LightWarmParchment = Color(0xFFECEEF5)
val LightPorcelainCard = Color(0xFFFFFFFF)
val LightElevatedSand = Color(0xFFE3E6F0)

val LightBronzeGold = Color(0xFF9A670E)
val LightBronzeSoft = Color(0xFF7B4F05)
val LightChampagneTint = Color(0xFFF5E9D0)

val LightEspressoInk = Color(0xFF141720)
val LightMutedUmber = Color(0xFF4C5366)
val LightHairlineBorder = Color(0x1C141720)

val LightEmerald = Color(0xFF127A4E)
val LightAmber = Color(0xFFA66208)
val LightCrimson = Color(0xFFC43040)

// Semantic Status & Sentiment Colours
val StatusWatchingColor = ChampagneGold
val StatusCompletedColor = ObsidianEmerald
val StatusPlanToWatchColor = ChampagneGoldSoft
val StatusOnHoldColor = ObsidianAmber
val StatusDroppedColor = ObsidianCrimson
val LiveAiringEmerald = ObsidianEmerald

// Legacy aliases preserved for backward compatibility
val MidnightBlack = ObsidianBlack
val DeepViolet = ObsidianSurface
val DarkIndigo = ObsidianSurface2
val SoftIndigo = ObsidianSurface3
val ElectricIndigo = ChampagneGold
val SoftLavender = ChampagneGoldSoft
val PaleViolet = ChampagneGoldSoft
val SoftWhite = IvoryText
val MutedLavenderGrey = AshText
val SubtleVioletBorder = HairlineSubtle
val SoftMint = ObsidianEmerald
val MutedAmber = ObsidianAmber
val SoftCoral = ObsidianCrimson

val UniverseDarkBackground = ObsidianBlack
val UniverseDarkSurface = ObsidianSurface2
val UniverseDarkSurfaceVariant = ObsidianSurface3
val UniverseDarkOutline = HairlineSubtle
val UniverseDarkOnBackground = IvoryText
val UniverseDarkOnSurfaceVariant = AshText

val UniverseLightBackground = LightIvoryCanvas
val UniverseLightSurface = LightPorcelainCard
val UniverseLightSurfaceVariant = LightWarmParchment
val UniverseLightOutline = LightHairlineBorder
val UniverseLightOnBackground = LightEspressoInk
val UniverseLightOnSurfaceVariant = LightMutedUmber

@Immutable
data class AppColors(
    val isDark: Boolean,
    val backgroundMain: Color,
    val backgroundSecondary: Color,
    val cardBackground: Color,
    val elevatedSurface: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val highlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val onAccentPrimary: Color
)

val DarkAppColors = AppColors(
    isDark = true,
    backgroundMain = ObsidianBlack,
    backgroundSecondary = ObsidianSurface,
    cardBackground = ObsidianSurface2,
    elevatedSurface = ObsidianSurface3,
    accentPrimary = ChampagneGold,
    accentSecondary = ChampagneGoldBright,
    highlight = ChampagneGoldSoft,
    textPrimary = IvoryText,
    textSecondary = AshText,
    textMuted = AshDim,
    borderSubtle = HairlineSubtle,
    borderStrong = HairlineStrong,
    success = ObsidianEmerald,
    warning = ObsidianAmber,
    error = ObsidianCrimson,
    onAccentPrimary = ChampagneOnGold
)

val LightAppColors = AppColors(
    isDark = false,
    backgroundMain = LightIvoryCanvas,
    backgroundSecondary = LightWarmParchment,
    cardBackground = LightPorcelainCard,
    elevatedSurface = LightElevatedSand,
    accentPrimary = LightBronzeGold,
    accentSecondary = LightBronzeSoft,
    highlight = LightBronzeSoft,
    textPrimary = LightEspressoInk,
    textSecondary = LightMutedUmber,
    textMuted = Color(0xFF5D657A),
    borderSubtle = LightHairlineBorder,
    borderStrong = LightEspressoInk.copy(alpha = 0.24f),
    success = LightEmerald,
    warning = LightAmber,
    error = LightCrimson,
    onAccentPrimary = Color(0xFFFFFFFF)
)
