package com.example.model

import androidx.compose.ui.graphics.Color

enum class WatchStatus(val dbValue: String, val label: String) {
    WATCHING("watching", "Watching"),
    COMPLETED("completed", "Completed"),
    PLAN_TO_WATCH("plan_to_watch", "Plan to Watch"),
    ON_HOLD("on_hold", "On Hold"),
    DROPPED("dropped", "Dropped");

    companion object {
        fun fromDbValue(value: String?): WatchStatus {
            return when (value?.lowercase()?.trim()) {
                "watching", "current" -> WATCHING
                "completed" -> COMPLETED
                "plan_to_watch", "planning", "wishlist" -> PLAN_TO_WATCH
                "on_hold", "paused" -> ON_HOLD
                "dropped" -> DROPPED
                else -> PLAN_TO_WATCH
            }
        }
    }
}

enum class VisibilityLevel(val dbValue: String, val label: String) {
    PRIVATE("private", "Private"),
    COMMUNITY("community", "Community");

    companion object {
        fun fromDbValue(value: String?): VisibilityLevel {
            return if (value.equals("community", ignoreCase = true)) COMMUNITY else PRIVATE
        }
    }
}

enum class MemberStatus(val dbValue: String, val label: String) {
    NONE("none", "Not Joined"),
    ACTIVE("active", "Active"),
    LEFT("left", "Left"),
    REMOVED("removed", "Removed");

    companion object {
        fun fromDbValue(value: String?): MemberStatus {
            return when (value?.lowercase()?.trim()) {
                "active" -> ACTIVE
                "left" -> LEFT
                "removed" -> REMOVED
                else -> NONE
            }
        }
    }
}

enum class BuiltInAvatar(
    val key: String,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    AVATAR_1("avatar_1", "Crimson Wolf", Color(0xFF9F1239), Color(0xFF334155)),
    AVATAR_2("avatar_2", "Azure Sovereign", Color(0xFF0284C7), Color(0xFF6366F1)),
    AVATAR_3("avatar_3", "Amethyst Dragon", Color(0xFFA855F7), Color(0xFFE11D48)),
    AVATAR_4("avatar_4", "Frost Empress", Color(0xFF38BDF8), Color(0xFF818CF8)),
    AVATAR_5("avatar_5", "Emerald Flora", Color(0xFF10B981), Color(0xFF38BDF8)),
    AVATAR_6("avatar_6", "Amber Scholar", Color(0xFFF59E0B), Color(0xFF92400E)),
    AVATAR_7("avatar_7", "Autumn Vanguard", Color(0xFFEA580C), Color(0xFF06B6D4)),
    AVATAR_8("avatar_8", "Indigo Flame", Color(0xFF4F46E5), Color(0xFF0EA5E9)),
    AVATAR_9("avatar_9", "Solar Halo", Color(0xFFFBBF24), Color(0xFF64748B));

    companion object {
        fun fromKey(key: String?): BuiltInAvatar {
            if (key.isNullOrBlank()) return AVATAR_1
            val cleaned = key.substringBefore('|').trim().lowercase()
            entries.firstOrNull { it.key == cleaned || it.displayName.lowercase() == cleaned }?.let { return it }
            val match = Regex("""avatar[_-]?([1-9])""").find(cleaned)
                ?: Regex("""^([1-9])$""").find(cleaned)
            val idx = match?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (idx != null && idx in 1..9) {
                return entries[idx - 1]
            }
            return AVATAR_1
        }
    }
}

const val AVATAR_FRAME_UNLOCK_XP = 50

enum class BuiltInAvatarFrame(
    val key: String,
    val displayName: String,
    val accentColor: Color
) {
    FRAME_1("frame_1", "Moonlit Deer", Color(0xFF7DD3FC)),
    FRAME_2("frame_2", "Sakura Kitsune", Color(0xFFF472B6)),
    FRAME_3("frame_3", "Frost Dew", Color(0xFF93C5FD)),
    FRAME_4("frame_4", "Sakura Crescent", Color(0xFFFBCFE8)),
    FRAME_5("frame_5", "Obsidian Dragon", Color(0xFFC084FC)),
    FRAME_6("frame_6", "Crimson Lantern", Color(0xFFF87171)),
    FRAME_7("frame_7", "Spectral Lantern", Color(0xFF60A5FA)),
    FRAME_8("frame_8", "Crimson Eclipse", Color(0xFFEF4444)),
    FRAME_9("frame_9", "Astral Cube", Color(0xFF818CF8)),
    FRAME_10("frame_10", "Lunar Seraph", Color(0xFFBAE6FD));

    companion object {
        private val frameRegex = Regex("""(?:^|\||:)frame[_-]?(10|[1-9])(?:\b|$)""", RegexOption.IGNORE_CASE)

        fun extractKey(rawAvatarKey: String?, rawAvatarUrl: String? = null): String? {
            val candidates = listOfNotNull(rawAvatarKey, rawAvatarUrl)
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            for (c in candidates) {
                val match = frameRegex.find(c)
                val idx = match?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (idx != null && idx in 1..10) {
                    return "frame_$idx"
                }
            }
            return null
        }

        fun fromRawKey(rawAvatarKey: String?, rawAvatarUrl: String? = null): BuiltInAvatarFrame? {
            val key = extractKey(rawAvatarKey, rawAvatarUrl) ?: return null
            return entries.firstOrNull { it.key == key }
        }

        fun composeAvatarKey(baseAvatarKey: String?, frameKey: String?): String {
            val base = BuiltInAvatar.fromKey(baseAvatarKey).key
            val cleanFrame = extractKey(frameKey)
            return if (cleanFrame != null) "$base|$cleanFrame" else base
        }

        fun resolveCompositeAvatarKey(
            rawAvatarKey: String?,
            rawAvatarFrame: String? = null,
            rawAvatarUrl: String? = null
        ): String {
            val baseSource = rawAvatarKey?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: rawAvatarUrl?.takeIf { it.startsWith("avatar_", ignoreCase = true) }
                ?: BuiltInAvatar.AVATAR_1.key
            val base = BuiltInAvatar.fromKey(baseSource).key
            val frame = extractKey(rawAvatarFrame) ?: extractKey(rawAvatarKey, rawAvatarUrl)
            return if (frame != null) "$base|$frame" else base
        }
    }
}

data class PrivacySettings(
    val userId: String = "",
    val watchlistVisibility: VisibilityLevel = VisibilityLevel.PRIVATE,
    val reviewsVisibility: VisibilityLevel = VisibilityLevel.PRIVATE,
    val collectionsVisibility: VisibilityLevel = VisibilityLevel.PRIVATE,
    val activityVisibility: VisibilityLevel = VisibilityLevel.PRIVATE
)

data class UserProfile(
    val id: String,
    val email: String = "",
    val username: String,
    val avatarKey: String = BuiltInAvatar.AVATAR_1.key,
    val avatarUrl: String? = null,
    val bio: String = "",
    val role: String = "member",
    val canGenerateInvites: Boolean = false,
    val memberStatus: MemberStatus = MemberStatus.ACTIVE,
    val joinedAt: String? = null,
    val xp: Int = 0
) {
    val isAdmin: Boolean
        get() = role.equals("admin", ignoreCase = true)

    val isInviteCreator: Boolean
        get() = isAdmin || canGenerateInvites
}

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresAtEpochSec: Long,
    val pendingUsername: String = "",
    val pendingInviteCode: String = "",
    val pendingAvatarKey: String = "",
    val onboardingCompleted: Boolean = true
)

data class CommunityCapacity(
    val activeCount: Int = 0,
    val maxCapacity: Int = 50
) {
    val isFull: Boolean get() = activeCount >= maxCapacity
}

data class InvitationCode(
    val id: String,
    val code: String,
    val maxUses: Int,
    val usesCount: Int,
    val expiresAt: String,
    val isActive: Boolean,
    val createdAt: String? = null
)

data class CommunityMember(
    val userId: String,
    val username: String,
    val avatarKey: String,
    val avatarUrl: String?,
    val bio: String,
    val role: String,
    val status: MemberStatus,
    val joinedAt: String?,
    val privacy: PrivacySettings = PrivacySettings(userId = userId)
)

fun isHentaiOrAdultGenres(genres: Collection<String>, isAdultFlag: Boolean = false): Boolean {
    if (isAdultFlag) return true
    return genres.any { raw ->
        val g = raw.trim().lowercase()
        g == "hentai" || g == "adult" || g.contains("18+")
    }
}

data class CommunityReview(
    val id: String,
    val userId: String,
    val username: String,
    val avatarKey: String,
    val avatarUrl: String?,
    val animeId: Int,
    val animeTitle: String,
    val animeCoverImage: String,
    val rating: Double?,
    val reviewNotes: String,
    val status: WatchStatus,
    val episodeProgress: Int,
    val totalEpisodes: Int?,
    val updatedAt: String,
    val likesCount: Int = 0,
    val likedByMe: Boolean = false,
    val genres: List<String> = emptyList(),
    val isAdult: Boolean = isHentaiOrAdultGenres(genres)
)

/**
 * Single unified community showcase card per anime.
 * Consolidates all joined members' notes and star ratings for the same anime
 * into one template instead of creating duplicate cards per member.
 */
data class CommunityAnimeGroup(
    val animeId: Int,
    val animeTitle: String,
    val animeCoverImage: String,
    val averageRating10: Double?,
    val ratedCount: Int,
    val notedCount: Int,
    val latestUpdatedAt: String,
    val totalLikesCount: Int,
    val leadReview: CommunityReview,
    val reviews: List<CommunityReview>,
    val genres: List<String> = emptyList(),
    val isAdult: Boolean = isHentaiOrAdultGenres(genres)
) {
    val notesAndRatingsCount: Int get() = reviews.size
    val distinctContributors: List<CommunityReview>
        get() = reviews.distinctBy { it.userId.ifBlank { it.username } }
}

fun groupCommunityReviewsByAnime(
    reviews: List<CommunityReview>
): List<CommunityAnimeGroup> {
    return reviews
        .filter { it.reviewNotes.isNotBlank() || (it.rating ?: 0.0) > 0.0 }
        .groupBy { it.animeId }
        .values
        .mapNotNull { groupItems ->
            val sortedEntries = groupItems.sortedByDescending { it.updatedAt }
            if (sortedEntries.isEmpty()) return@mapNotNull null

            val lead = sortedEntries.firstOrNull { it.reviewNotes.isNotBlank() } ?: sortedEntries.first()
            val positiveRatings = sortedEntries.mapNotNull { it.rating?.takeIf { r -> r > 0.0 } }
            val avgRating = if (positiveRatings.isNotEmpty()) positiveRatings.average() else null
            val noted = sortedEntries.count { it.reviewNotes.isNotBlank() }
            val bestTitle = sortedEntries.firstOrNull { it.animeTitle.isNotBlank() }?.animeTitle ?: "Anime #${lead.animeId}"
            val bestCover = sortedEntries.firstOrNull { it.animeCoverImage.isNotBlank() }?.animeCoverImage.orEmpty()
            val mergedGenres = sortedEntries.flatMap { it.genres }.distinct()
            val isAdult = sortedEntries.any { it.isAdult } || isHentaiOrAdultGenres(mergedGenres)

            CommunityAnimeGroup(
                animeId = lead.animeId,
                animeTitle = bestTitle,
                animeCoverImage = bestCover,
                averageRating10 = avgRating,
                ratedCount = positiveRatings.size,
                notedCount = noted,
                latestUpdatedAt = sortedEntries.maxOfOrNull { it.updatedAt }.orEmpty(),
                totalLikesCount = sortedEntries.sumOf { it.likesCount },
                leadReview = lead,
                reviews = sortedEntries,
                genres = mergedGenres,
                isAdult = isAdult
            )
        }
}

data class TableStorageStat(
    val tableName: String,
    val rowCount: Long,
    val tableBytes: Long,
    val indexBytes: Long,
    val totalBytes: Long
)

data class DatabaseStorageStats(
    val totalBytes: Long = 0L,
    val limitBytes: Long = 524_288_000L, // 500 MB
    val usagePercent: Double = 0.0,
    val warningLevel: String = "normal", // normal, warning_60, warning_75, critical_85, critical_95
    val userCount: Int = 1,
    val entryCount: Int = 0,
    val reviewCount: Int = 0,
    val bytesPerUser: Long = 2048L,
    val bytesPerReview: Long = 380L,
    val estimatedRemainingUsers: Long = 200_000L,
    val tables: List<TableStorageStat> = emptyList()
) {
    val warningBannerText: String?
        get() = when {
            usagePercent >= 95.0 || warningLevel == "critical_95" ->
                "CRITICAL (95%+ Quota): Database is at ${"%.1f".format(usagePercent)}% of the 500 MB limit. Archive obsolete records immediately."
            usagePercent >= 85.0 || warningLevel == "critical_85" ->
                "HIGH ALERT (85%+ Quota): Database has reached ${"%.1f".format(usagePercent)}% of the 500 MB limit."
            usagePercent >= 75.0 || warningLevel == "warning_75" ->
                "WARNING (75%+ Quota): Database is at ${"%.1f".format(usagePercent)}% of the 500 MB limit."
            usagePercent >= 60.0 || warningLevel == "warning_60" ->
                "NOTICE (60%+ Quota): Database has crossed 60% (${"%.1f".format(usagePercent)}%) of the 500 MB limit."
            else -> null
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024L) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024.0) return "${"%.1f".format(kb)} KB"
            val mb = kb / 1024.0
            return "${"%.2f".format(mb)} MB"
        }
    }
}

data class CommunityActivityItem(
    val id: String,
    val userId: String,
    val username: String,
    val avatarKey: String,
    val avatarUrl: String?,
    val animeId: Int,
    val animeTitle: String,
    val animeCoverImage: String,
    val status: WatchStatus,
    val episodeProgress: Int,
    val totalEpisodes: Int?,
    val personalRating: Double?,
    val updatedAt: String
)

data class AnimeCollection(
    val id: String,
    val creatorId: String,
    val creatorUsername: String = "",
    val name: String,
    val description: String = "",
    val visibility: VisibilityLevel = VisibilityLevel.PRIVATE,
    val animeIds: List<Int> = emptyList(),
    val updatedAt: String? = null
)

enum class AnimePartCategory(val filterLabel: String) {
    ALL("All"),
    MOVIE("Movies"),
    SPIN_OFF("Spin-Offs"),
    SIDE_STORY("Side Stories"),
    OVA_SPECIAL("OVA & Specials"),
    OTHER("More Parts");

    companion object {
        fun classify(relationType: String, format: String?): AnimePartCategory {
            val fmt = format?.uppercase()?.trim().orEmpty()
            val rel = relationType.uppercase().replace('_', ' ').trim()
            return when {
                fmt == "MOVIE" -> MOVIE
                rel.contains("SPIN") || rel == "CHARACTER" -> SPIN_OFF
                rel.contains("SIDE") -> SIDE_STORY
                fmt in listOf("OVA", "SPECIAL", "MUSIC") -> OVA_SPECIAL
                else -> OTHER
            }
        }

        fun badgeLabel(relationType: String, format: String?): String {
            val fmt = format?.uppercase()?.trim().orEmpty()
            val rel = relationType.uppercase().replace('_', ' ').trim()
            return when {
                fmt == "MOVIE" -> "MOVIE"
                rel.contains("SPIN") -> "SPIN-OFF"
                rel.contains("SIDE") -> "SIDE STORY"
                fmt == "OVA" -> "OVA"
                fmt == "SPECIAL" -> "SPECIAL"
                fmt == "MUSIC" -> "MUSIC"
                rel == "PARENT" -> "MAIN SERIES"
                rel == "SUMMARY" -> "SUMMARY"
                rel == "ALTERNATIVE" -> "ALTERNATIVE"
                rel.isNotBlank() -> rel
                fmt.isNotBlank() -> fmt
                else -> "RELATED"
            }
        }
    }
}

data class AnimeRelationItem(
    val id: Int,
    val relationType: String,
    val title: String,
    val coverImage: String,
    val format: String?,
    val status: String?,
    val averageScore: Int?,
    val mediaType: String? = "ANIME",
    val episodes: Int? = null,
    val seasonYear: Int? = null
)

enum class FranchiseSeasonOverrideMode(val storageValue: String) {
    AUTO("auto"),
    MERGE_ONE("merge_one"),
    SPLIT_ALL("split_all");

    companion object {
        fun fromStorage(raw: String?): FranchiseSeasonOverrideMode {
            return when (raw?.trim()?.lowercase()) {
                "merge_one", "merge" -> MERGE_ONE
                "split_all", "split" -> SPLIT_ALL
                else -> AUTO
            }
        }
    }
}

data class AnimeSeasonSegment(
    val id: Int,
    val title: String,
    val episodes: Int?,
    val startEpisodeOffset: Int,
    val status: String?,
    val format: String?,
    val seasonYear: Int?
)

data class AnimeMovieOvaEntry(
    val id: Int,
    val title: String,
    val coverImage: String,
    val formatBadge: String,
    val status: String?,
    val episodes: Int,
    val releaseYear: Int?,
    val isCurrent: Boolean = false
)

data class AnimeSeasonEntry(
    val id: Int,
    val seasonNumber: Int,
    val title: String,
    val coverImage: String,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val seasonYear: Int?,
    val isCurrent: Boolean,
    val partNumber: Int? = null,
    val shortLabel: String = seasonNumber.toString(),
    val displayLabel: String = if (partNumber != null && partNumber > 1) {
        "Season $seasonNumber • Part $partNumber"
    } else {
        "Season $seasonNumber"
    },
    val subEntries: List<AnimeSeasonSegment> = emptyList(),
    val isMergedContinuousRun: Boolean = subEntries.size > 1
) {
    fun containsAnimeId(targetId: Int): Boolean {
        return id == targetId || subEntries.any { it.id == targetId }
    }
}

data class AnimeRecommendationItem(
    val id: Int,
    val title: String,
    val coverImage: String,
    val format: String?,
    val averageScore: Int?
)

data class AnimeCharacterItem(
    val id: Int,
    val name: String,
    val imageUrl: String
)

data class AnimeRichDetail(
    val animeId: Int,
    val studios: List<String> = emptyList(),
    val characters: List<AnimeCharacterItem> = emptyList(),
    val relations: List<AnimeRelationItem> = emptyList(),
    val recommendations: List<AnimeRecommendationItem> = emptyList(),
    val seasons: List<AnimeSeasonEntry> = emptyList(),
    val moviesAndOvas: List<AnimeMovieOvaEntry> = emptyList(),
    val franchiseId: Int = animeId,
    val rawMainSeasonNodeCount: Int = seasons.size
)

enum class ThemeModePreference(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light")
}

enum class WatchlistLayoutMode {
    GRID,
    LIST
}

sealed class EpisodeInputValidation {
    data class Valid(val episodes: Int) : EpisodeInputValidation()
    data class Invalid(val message: String) : EpisodeInputValidation()
}

object ProgressRules {
    const val MAX_REASONABLE_UNKNOWN_EPISODES = 20_000

    fun isOfficiallyFinished(officialStatus: String?): Boolean {
        val norm = officialStatus?.trim()?.uppercase().orEmpty()
        return norm == "FINISHED" || norm == "FINISHED_AIRING"
    }

    /**
     * Automatic Anime Completion (Part 2) — strictly requires ALL 3 conditions:
     * 1. The anime's official status is Completed (FINISHED / FINISHED_AIRING).
     * 2. The official total episode count is known (> 0).
     * 3. The user's watched episode count is >= total episode count.
     */
    fun shouldAutoComplete(
        officialStatus: String?,
        totalEpisodes: Int?,
        watchedEpisodes: Int
    ): Boolean {
        if (!isOfficiallyFinished(officialStatus)) return false
        if (totalEpisodes == null || totalEpisodes <= 0) return false
        return watchedEpisodes >= totalEpisodes
    }

    /**
     * Determines whether an anime should be automatically removed from Home
     * (while preserving Library entry, progress, notes, ratings, and Community).
     */
    fun shouldRemoveFromHome(
        watchStatus: WatchStatus,
        officialStatus: String?,
        totalEpisodes: Int?,
        watchedEpisodes: Int
    ): Boolean {
        if (watchStatus == WatchStatus.DROPPED || watchStatus == WatchStatus.COMPLETED) return true
        return shouldAutoComplete(officialStatus, totalEpisodes, watchedEpisodes)
    }

    /**
     * Validates direct custom episode number input (Part 5).
     */
    fun validateEpisodeInput(rawInput: String, totalEpisodes: Int?): EpisodeInputValidation {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return EpisodeInputValidation.Invalid("Enter a whole episode number")
        }
        if (trimmed.startsWith("-")) {
            return EpisodeInputValidation.Invalid("Episode count cannot be negative")
        }
        if (trimmed.contains('.') || trimmed.contains(',')) {
            return EpisodeInputValidation.Invalid("Whole numbers only (no decimals)")
        }
        if (!trimmed.all { it.isDigit() }) {
            return EpisodeInputValidation.Invalid("Numbers only (0–9)")
        }
        val parsed = trimmed.toIntOrNull()
            ?: return EpisodeInputValidation.Invalid("Episode number is too large")
        if (parsed < 0) {
            return EpisodeInputValidation.Invalid("Episode count cannot be negative")
        }
        if (totalEpisodes != null && totalEpisodes > 0 && parsed > totalEpisodes) {
            return EpisodeInputValidation.Invalid("Cannot exceed $totalEpisodes episodes")
        }
        if ((totalEpisodes == null || totalEpisodes <= 0) && parsed > MAX_REASONABLE_UNKNOWN_EPISODES) {
            return EpisodeInputValidation.Invalid("Please enter a reasonable episode count")
        }
        return EpisodeInputValidation.Valid(parsed)
    }

    /**
     * Enforces episode progress bounds: never negative, and never exceeds totalEpisodes when known (> 0).
     */
    fun clampEpisodeProgress(requested: Int, totalEpisodes: Int?): Int {
        val nonNegative = requested.coerceAtLeast(0)
        return if (totalEpisodes != null && totalEpisodes > 0) {
            nonNegative.coerceAtMost(totalEpisodes)
        } else {
            nonNegative
        }
    }

    /**
     * Calculates next progress for "+1" quick increment and indicates if auto-completion triggers.
     */
    fun incrementEpisode(
        current: Int,
        totalEpisodes: Int?,
        officialStatus: String? = null
    ): Pair<Int, Boolean> {
        val next = clampEpisodeProgress(current + 1, totalEpisodes)
        val isNowCompleted = if (officialStatus != null) {
            shouldAutoComplete(officialStatus, totalEpisodes, next)
        } else {
            totalEpisodes != null && totalEpisodes > 0 && next >= totalEpisodes
        }
        return next to isNowCompleted
    }
}

object SeasonRules {
    private val NON_SERIES_FORMATS = setOf("MOVIE", "OVA", "SPECIAL", "MUSIC")
    private val EXPLICIT_SEASON_REGEX = Regex(
        pattern = """(?i)(?:\bseason\s*(\d{1,2})\b|\b(\d{1,2})(?:st|nd|rd|th)\s+season\b|\bs(\d{1,2})\b|\bdi\s*(\d{1,2})\s*ji\b|\bdai\s*(\d{1,2})\s*ki\b)"""
    )
    private val PINYIN_SEASON_REGEX = Regex(
        pattern = """(?i)\bdi\s*(yi|er|san|si|wu|liu|qi|ba|jiu|shi)\s*ji\b"""
    )
    private val NON_SEASON_NUM_CONTEXT_REGEX = Regex(
        pattern = """(?i)\b(part|cour|movie|film|special|ova|oad|episode|ep|chapter|vol|volume|no\.|number|stage|act|movie\s*#?)\s*([0-9ivx]+)?$"""
    )
    private val TRAILING_ROMAN_REGEX = Regex("""\s+(II|III|IV|V|VI)(?:$|[:\-])""")
    private val TRAILING_SINGLE_DIGIT_SEASON_REGEX = Regex("""(?<!No\.\s)(?<!No\s)(?<!#)\s+([2-9])$""")
    private val NON_SEASON_TITLE_WORDS = Regex(
        pattern = """(?i)\b(movie|film|ova|oad|special|recap|prologue|teaser|pv|mini\s*theater|xiao\s*juchang|chibi|q[- ]?ban|fanwai|tebie\s*pian)\b"""
    )

    fun clampSeason(requested: Int): Int = requested.coerceIn(1, 99)

    fun isNonSeriesFormat(format: String?): Boolean {
        val norm = format?.trim()?.uppercase().orEmpty()
        return norm in NON_SERIES_FORMATS
    }

    fun inferSeasonFromTitle(title: String?, format: String? = null): Int {
        if (title.isNullOrBlank()) return 1
        if (isNonSeriesFormat(format)) return 1
        val cleaned = title.trim()

        // 1. Explicit "Season N", "2nd Season", "S2", "Di 2 Ji", "Dai 2 Ki"
        EXPLICIT_SEASON_REGEX.find(cleaned)?.let { match ->
            val raw = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toIntOrNull()
            if (raw != null && raw in 1..20) return raw
        }

        // 2. Explicit Chinese Pinyin season markers ("Di Er Ji" = Season 2, etc.)
        PINYIN_SEASON_REGEX.find(cleaned)?.let { match ->
            return when (match.groupValues[1].lowercase()) {
                "yi" -> 1
                "er" -> 2
                "san" -> 3
                "si" -> 4
                "wu" -> 5
                "liu" -> 6
                "qi" -> 7
                "ba" -> 8
                "jiu" -> 9
                "shi" -> 10
                else -> 1
            }
        }

        // Do not guess season numbers from Movies/Specials/OVAs/Parts/numbered titles (e.g. "Kaiju No. 8", "Mob Psycho 100", "Part 2")
        if (NON_SEASON_TITLE_WORDS.containsMatchIn(cleaned)) return 1
        if (NON_SEASON_NUM_CONTEXT_REGEX.containsMatchIn(cleaned)) return 1

        // 3. Case-sensitive uppercase Roman numerals ("Soul Land II", "Title III") when not preceded by Part/Movie/etc.
        TRAILING_ROMAN_REGEX.find(cleaned)?.let { match ->
            val before = cleaned.substring(0, match.range.first).trim()
            if (!NON_SEASON_NUM_CONTEXT_REGEX.containsMatchIn(before)) {
                return when (match.groupValues[1]) {
                    "II" -> 2
                    "III" -> 3
                    "IV" -> 4
                    "V" -> 5
                    "VI" -> 6
                    else -> 1
                }
            }
        }

        // 4. Safe trailing single digit 2..9 ("A Will Eternal 3", "Kingdom 3") after non-season prefixes are excluded
        TRAILING_SINGLE_DIGIT_SEASON_REGEX.find(cleaned)?.let { match ->
            val before = cleaned.substring(0, match.range.first).trim()
            if (!NON_SEASON_NUM_CONTEXT_REGEX.containsMatchIn(before) &&
                !Regex("""(?i)\b(?:no\.?|#|grade|class|unit|type|level|room|floor|zero|one)$""").containsMatchIn(before)
            ) {
                match.groupValues[1].toIntOrNull()?.let { if (it in 2..9) return it }
            }
        }
        return 1
    }

    fun resolveInitialSeason(storedSeason: Int, title: String?, format: String? = null): Int {
        if (isNonSeriesFormat(format)) return 1
        return if (storedSeason > 1) clampSeason(storedSeason) else inferSeasonFromTitle(title, format)
    }
}

enum class SentimentCategory(
    val key: String,
    val shortLabel: String,
    val fullLabel: String,
    val suggestedScoreTenths: Int
) {
    CINEMA("cinema", "Cinema", "Absolute Cinema", 100),
    WORTH("worth", "Worth", "Worth Watching", 80),
    AVG("avg", "Avg", "Average", 60),
    BORING("boring", "Boring", "Boring", 40),
    WASTE("waste", "Waste", "Waste of Time", 20);

    companion object {
        fun fromScore100(score100: Int?): SentimentCategory? {
            if (score100 == null || score100 <= 0) return null
            return when {
                score100 >= 90 -> CINEMA
                score100 >= 75 -> WORTH
                score100 >= 55 -> AVG
                score100 >= 35 -> BORING
                else -> WASTE
            }
        }

        fun fromRating10(rating10: Double?): SentimentCategory? {
            if (rating10 == null || rating10 <= 0.0) return null
            return fromScore100((rating10 * 10.0).toInt())
        }

        fun fromStars(stars: Float): SentimentCategory? {
            if (stars <= 0f) return null
            return fromScore100(StarRatingUtils.starsToScore100(stars))
        }
    }
}

object StarRatingUtils {
    fun roundTenth(stars: Float): Float {
        if (stars <= 0f) return 0f
        val tenths = kotlin.math.round(stars * 10.0).toInt().coerceIn(1, 50)
        return tenths / 10f
    }

    fun score100ToStars(score100: Int?): Float {
        if (score100 == null || score100 <= 0) return 0f
        val tenths = kotlin.math.round(score100 / 2.0).toInt().coerceIn(1, 50)
        return tenths / 10f
    }

    fun rating10ToStars(rating10: Double?): Float {
        if (rating10 == null || rating10 <= 0.0) return 0f
        val tenths = kotlin.math.round(rating10 * 5.0).toInt().coerceIn(1, 50)
        return tenths / 10f
    }

    fun starsToScore100(stars: Float): Int {
        if (stars <= 0f) return 0
        val tenths = kotlin.math.round(stars * 10.0).toInt().coerceIn(1, 50)
        return tenths * 2
    }

    fun formatStars(stars: Float): String {
        val clamped = stars.coerceIn(0f, 5f)
        return "%.1f".format(java.util.Locale.US, clamped)
    }
}

enum class TopAnimeSource(val key: String, val label: String) {
    COMMUNITY("community", "Community"),
    ALL("all", "All")
}

enum class TopAnimeTimeframe(val key: String, val label: String) {
    WEEKLY("weekly", "Weekly"),
    MONTHLY("monthly", "Monthly"),
    SEASONAL("seasonal", "Seasonal"),
    YEARLY("yearly", "Yearly"),
    ALL_TIME("all_time", "All Time")
}

data class TopAnimeRankedItem(
    val animeId: Int,
    val title: String,
    val coverImage: String,
    val scoreDisplay: String,
    val voteCount: Int? = null,
    val formatLabel: String = "",
    val releaseYear: Int? = null,
    val episodes: Int? = null,
    val genres: List<String> = emptyList()
)

data class DailyAnimePick(
    val id: String = "",
    val selectionDate: String = "",
    val anilistId: Int = 0,
    val title: String = "",
    val secondaryTitle: String = "",
    val rating100: Int? = null,
    val coverImage: String = "",
    val bannerImage: String? = null,
    val trailerId: String? = null,
    val episodes: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val rawStatus: String? = null,
    val rawFormat: String? = null,
    val rawSeason: String? = null,
    val releaseYear: Int? = null,
    val durationMinutes: Int? = null,
    val rawSource: String? = null,
    val studios: List<String> = emptyList(),
    val countryOfOrigin: String? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val description: String = ""
) {
    val rating10Text: String
        get() = if (rating100 != null && rating100 > 0) {
            "%.1f / 10".format(java.util.Locale.US, rating100 / 10.0)
        } else {
            "Unknown"
        }

    val statusLabel: String
        get() = DailyAnimeFormatters.formatAiringStatus(rawStatus)

    val formatLabel: String
        get() = DailyAnimeFormatters.formatFormat(rawFormat)

    val seasonLabel: String
        get() = DailyAnimeFormatters.formatSeason(rawSeason)

    val sourceLabel: String
        get() = DailyAnimeFormatters.formatSource(rawSource)

    val startDateLabel: String
        get() = DailyAnimeFormatters.formatIsoDate(startDate)

    val endDateLabel: String
        get() = DailyAnimeFormatters.formatIsoDate(endDate)

    val selectionDateLabel: String
        get() = DailyAnimeFormatters.formatIsoDate(selectionDate)

    val episodesLabel: String
        get() = if (episodes != null && episodes > 0) episodes.toString() else "Unknown"

    val releaseYearLabel: String
        get() = if (releaseYear != null && releaseYear > 0) releaseYear.toString() else "Unknown"

    val durationLabel: String
        get() = if (durationMinutes != null && durationMinutes > 0) "$durationMinutes min" else "Unknown"

    val studiosLabel: String
        get() = if (studios.isNotEmpty()) studios.joinToString(", ") else "Unknown"

    val countryLabel: String
        get() = countryOfOrigin?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown"

    val validTrailerId: String?
        get() = trailerId?.trim()?.takeIf { DailyAnimeFormatters.isValidYouTubeId(it) }
}

object DailyAnimeFormatters {
    private val YT_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")
    private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val IST_ZONE: java.time.ZoneId = java.time.ZoneId.of("Asia/Kolkata")

    val EXCLUDED_ANILIST_GENRES: List<String> = listOf(
        "Hentai",
        "Boys' Love",
        "Girls' Love",
        "Yaoi",
        "Yuri",
        "Shounen Ai",
        "Shoujo Ai"
    )

    val EXCLUDED_ANILIST_TAGS: List<String> = listOf(
        "Boys' Love",
        "Girls' Love",
        "Yuri",
        "Yaoi",
        "Shounen Ai",
        "Shoujo Ai"
    )

    private val EXCLUDED_BL_GL_EXACT_TOKENS: Set<String> = setOf(
        "bl",
        "gl",
        "yaoi",
        "yuri",
        "boys' love",
        "boys love",
        "boy's love",
        "girls' love",
        "girls love",
        "girl's love",
        "shounen ai",
        "shonen ai",
        "shoujo ai",
        "shojo ai"
    )

    private val EXCLUDED_BL_GL_PHRASE_REGEX = Regex(
        "(^|[^a-z0-9])(boys'?\\s*love|boy's\\s*love|girls'?\\s*love|girl's\\s*love|yaoi|yuri|shou?nen\\s*ai|shou?jo\\s*ai)([^a-z0-9]|$)"
    )

    fun getCurrentIstDateString(): String {
        return java.time.LocalDate.now(IST_ZONE).toString()
    }

    fun millisUntilNextIstMidnight(): Long {
        val nowIst = java.time.ZonedDateTime.now(IST_ZONE)
        val nextMidnightIst = nowIst.toLocalDate().plusDays(1).atStartOfDay(IST_ZONE)
        return java.time.Duration.between(nowIst, nextMidnightIst).toMillis().coerceAtLeast(1000L)
    }

    fun isExcludedBlGlToken(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val normalized = raw
            .trim()
            .lowercase(java.util.Locale.US)
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('`', '\'')
            .replace(Regex("[_\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
        if (normalized in EXCLUDED_BL_GL_EXACT_TOKENS) return true
        return EXCLUDED_BL_GL_PHRASE_REGEX.containsMatchIn(normalized)
    }

    fun hasExcludedBlGlGenreOrTag(genres: List<String>, tags: List<String>): Boolean {
        if (genres.isEmpty()) return true
        for (genre in genres) {
            if (isExcludedBlGlToken(genre)) return true
        }
        for (tag in tags) {
            if (isExcludedBlGlToken(tag)) return true
        }
        return false
    }

    fun isPickEligibleForDate(pick: DailyAnimePick?, expectedIstDate: String? = null): Boolean {
        if (pick == null) return false
        if (pick.anilistId <= 0) return false
        if (pick.title.isBlank() || pick.title == "Unknown") return false
        if (!pick.coverImage.startsWith("http://", ignoreCase = true) &&
            !pick.coverImage.startsWith("https://", ignoreCase = true)
        ) {
            return false
        }
        val score = pick.rating100 ?: return false
        if (score !in 60..100) return false
        if (!isValidYouTubeId(pick.trailerId)) return false
        if (pick.genres.isEmpty() || pick.tags.isEmpty()) return false
        if (hasExcludedBlGlGenreOrTag(pick.genres, pick.tags)) return false
        if (expectedIstDate != null && pick.selectionDate != expectedIstDate) return false
        return true
    }

    fun isValidYouTubeId(id: String?): Boolean {
        return !id.isNullOrBlank() && YT_ID_REGEX.matches(id.trim())
    }

    fun formatAiringStatus(status: String?): String = when (status?.trim()?.uppercase()) {
        "RELEASING" -> "Currently airing"
        "FINISHED" -> "Finished airing"
        "NOT_YET_RELEASED" -> "Upcoming"
        "CANCELLED" -> "Cancelled"
        else -> "Unknown"
    }

    fun formatFormat(format: String?): String = when (format?.trim()?.uppercase()) {
        "TV" -> "TV"
        "TV_SHORT" -> "TV Short"
        "MOVIE" -> "Movie"
        "OVA" -> "OVA"
        "ONA" -> "ONA"
        "SPECIAL" -> "Special"
        "MUSIC" -> "Music"
        else -> "Unknown"
    }

    fun formatSeason(season: String?): String = when (season?.trim()?.uppercase()) {
        "WINTER" -> "Winter"
        "SPRING" -> "Spring"
        "SUMMER" -> "Summer"
        "FALL" -> "Fall"
        else -> "Unknown"
    }

    fun formatSource(source: String?): String = when (source?.trim()?.uppercase()) {
        "ORIGINAL" -> "Original"
        "MANGA" -> "Manga"
        "LIGHT_NOVEL" -> "Light Novel"
        "VISUAL_NOVEL" -> "Visual Novel"
        "VIDEO_GAME" -> "Video Game"
        "WEB_NOVEL" -> "Web Novel"
        "NOVEL" -> "Novel"
        "DOUJINSHI" -> "Doujinshi"
        "ANIME" -> "Anime"
        "COMIC" -> "Comic"
        "GAME" -> "Game"
        "OTHER" -> "Other"
        else -> "Unknown"
    }

    fun formatIsoDate(raw: String?): String {
        val cleaned = raw?.trim().orEmpty()
        if (cleaned.isBlank()) return "Unknown"
        val parts = cleaned.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty() || parts[0] <= 0) return "Unknown"
        return when {
            parts.size >= 3 && parts[1] in 1..12 && parts[2] in 1..31 ->
                "${MONTHS[parts[1] - 1]} ${parts[2]}, ${parts[0]}"
            parts.size >= 2 && parts[1] in 1..12 ->
                "${MONTHS[parts[1] - 1]} ${parts[0]}"
            else -> parts[0].toString()
        }
    }

    fun stripHtml(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</?[^>]+(>|$)"), "")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}


