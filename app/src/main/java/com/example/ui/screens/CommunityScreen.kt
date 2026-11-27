package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.DoneAll
import kotlinx.coroutines.delay
import com.example.model.CommunityAnimeGroup
import com.example.model.CommunityReview
import com.example.model.MemberStatus
import com.example.model.WatchStatus
import com.example.model.MentionableUser
import com.example.model.NotificationJumpTarget
import com.example.model.SentimentCategory
import com.example.model.StarRatingUtils
import com.example.model.UnseenDiscussionAnime
import com.example.model.UserNotificationItem
import com.example.model.groupCommunityReviewsByAnime
import com.example.service.CommunityEmojiCacheService
import com.example.ui.components.AnimeDiscussionSection
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AppCard
import com.example.ui.widgets.AppEmptyState
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.MemberAvatar
import com.example.ui.widgets.ObsidianAmbientBackground
import com.example.ui.widgets.ObsidianTopBar
import com.example.ui.widgets.ResilientPosterImage
import com.example.ui.widgets.SentimentPill
import com.example.ui.widgets.StarRatingRow
import com.example.ui.widgets.badgeColor
import com.example.viewmodel.AnimeViewModel
import com.example.viewmodel.ReviewSortOption
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val REVIEW_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

internal fun formatCommunityTimestamp(raw: String): String {
    if (raw.isBlank()) return ""
    return runCatching {
        val instant = runCatching { Instant.parse(raw) }.getOrElse { OffsetDateTime.parse(raw).toInstant() }
        val secs = java.time.Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0L)
        when {
            secs < 60L -> "Just now"
            secs < 3600L -> "${secs / 60L}m ago"
            secs < 86400L -> "${secs / 3600L}h ago"
            secs < 86400L * 7L -> "${secs / 86400L}d ago"
            else -> REVIEW_DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()))
        }
    }.getOrElse {
        raw.take(10)
    }
}

private data class CommunityPosterOption(
    val key: String,
    val userId: String,
    val username: String,
    val avatarKey: String,
    val avatarUrl: String?,
    val postCount: Int
)

private fun normalizeCommunityReviewUserKey(review: CommunityReview): String {
    val uid = review.userId.trim()
    return uid.ifBlank { review.username.trim().lowercase() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: AnimeViewModel,
    onSelectAnime: (Int) -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val profile by viewModel.userProfile.collectAsState()
    val reviews by viewModel.communityReviews.collectAsState()
    val isLoading by viewModel.isCommunityLoading.collectAsState()
    val error by viewModel.communityError.collectAsState()
    val reviewSort by viewModel.reviewSortOption.collectAsState()
    val isSubmittingNote by viewModel.isSubmittingCommunityNote.collectAsState()
    val noteSuccessFeedback by viewModel.communityNoteFeedback.collectAsState()
    val noteErrorFeedback by viewModel.communityNoteError.collectAsState()
    val unseenDiscussionAnime by viewModel.unseenDiscussionAnime.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val isNotificationsLoading by viewModel.isNotificationsLoading.collectAsState()
    val hasMoreNotifications by viewModel.hasMoreNotifications.collectAsState()

    var activeSection by rememberSaveable { mutableStateOf("feed") } // "feed" | "tagged"
    var selectedSentimentKey by rememberSaveable { mutableStateOf<String?>(null) }
    var userFilterOpen by rememberSaveable { mutableStateOf(false) }
    var selectedUserKeys by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var selectedCommentsAnimeId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedFallbackGroup by remember { mutableStateOf<CommunityAnimeGroup?>(null) }
    var activeModalJumpTarget by remember { mutableStateOf<NotificationJumpTarget?>(null) }
    var forceLiveChatTab by remember { mutableStateOf(false) }

    // Non-blocking preload of top frequently used custom emojis & latest Tagged You list when Community opens
    LaunchedEffect(profile?.id) {
        if (profile != null) {
            viewModel.loadCommunityEmojis()
            viewModel.refreshNotifications(reset = true)
        }
    }

    val selectedSentiment = remember(selectedSentimentKey) {
        SentimentCategory.entries.firstOrNull { it.key == selectedSentimentKey }
    }

    val unseenByAnimeId = remember(unseenDiscussionAnime) {
        unseenDiscussionAnime.associateBy { it.animeId }
    }

    val allUnfilteredGroups = remember(reviews) {
        groupCommunityReviewsByAnime(reviews)
    }

    val allPosters = remember(allUnfilteredGroups) {
        val byUser = LinkedHashMap<String, Triple<CommunityReview, MutableSet<Int>, String>>()
        allUnfilteredGroups.forEach { grp ->
            grp.reviews.forEach { rev ->
                val k = normalizeCommunityReviewUserKey(rev)
                if (k.isNotBlank()) {
                    val existing = byUser[k]
                    if (existing != null) {
                        existing.second.add(grp.animeId)
                    } else {
                        byUser[k] = Triple(rev, mutableSetOf(grp.animeId), k)
                    }
                }
            }
        }
        byUser.values.map { (rev, animeIds, k) ->
            CommunityPosterOption(
                key = k,
                userId = rev.userId,
                username = rev.username.ifBlank { "Member" },
                avatarKey = rev.avatarKey,
                avatarUrl = rev.avatarUrl,
                postCount = animeIds.size
            )
        }.sortedWith(compareByDescending<CommunityPosterOption> { it.postCount }.thenBy { it.username.lowercase() })
    }

    val activeSelectedPosters = remember(selectedUserKeys, allPosters) {
        val posterByKey = allPosters.associateBy { it.key }
        selectedUserKeys.mapNotNull { posterByKey[it] }.take(3)
    }

    val animeGroups = remember(reviews, activeSelectedPosters) {
        if (activeSelectedPosters.isEmpty()) {
            allUnfilteredGroups
        } else {
            val allowedKeys = activeSelectedPosters.map { it.key }.toSet()
            val allowedNames = activeSelectedPosters.map { it.username.trim().lowercase() }.toSet()
            val scopedReviews = reviews.filter { r ->
                val rk = normalizeCommunityReviewUserKey(r)
                val rn = r.username.trim().lowercase()
                rk in allowedKeys || rn in allowedNames
            }
            groupCommunityReviewsByAnime(scopedReviews)
        }
    }

    val filteredGroups = remember(animeGroups, selectedSentiment, reviewSort) {
        val bySentiment = if (selectedSentiment == null) {
            animeGroups
        } else {
            animeGroups.filter { grp ->
                SentimentCategory.fromRating10(grp.averageRating10) == selectedSentiment ||
                    grp.reviews.any { SentimentCategory.fromRating10(it.rating) == selectedSentiment }
            }
        }
        when (reviewSort) {
            ReviewSortOption.NEWEST -> bySentiment.sortedByDescending { it.latestUpdatedAt }
            ReviewSortOption.HIGHEST_RATED -> bySentiment.sortedWith(
                compareByDescending<CommunityAnimeGroup> { it.averageRating10 ?: -1.0 }
                    .thenByDescending { it.latestUpdatedAt }
            )
            ReviewSortOption.MOST_LIKED -> bySentiment.sortedWith(
                compareByDescending<CommunityAnimeGroup> { it.totalLikesCount }
                    .thenByDescending { it.averageRating10 ?: -1.0 }
                    .thenByDescending { it.latestUpdatedAt }
            )
        }
    }

    val unseenOnlyAnime = remember(unseenDiscussionAnime, filteredGroups, activeSelectedPosters) {
        if (activeSelectedPosters.isNotEmpty()) {
            emptyList()
        } else {
            val shownIds = filteredGroups.map { it.animeId }.toSet()
            unseenDiscussionAnime.filter { it.animeId !in shownIds }
        }
    }

    val selectedCommentsGroup = remember(animeGroups, selectedCommentsAnimeId, selectedFallbackGroup) {
        val id = selectedCommentsAnimeId ?: return@remember null
        animeGroups.firstOrNull { it.animeId == id }
            ?: selectedFallbackGroup?.takeIf { it.animeId == id }
    }

    fun openTaggedAnimeTemplate(
        animeId: Int,
        animeTitle: String,
        animeCoverImage: String,
        jumpTarget: NotificationJumpTarget,
        notificationId: String? = null
    ) {
        if (!notificationId.isNullOrBlank()) {
            viewModel.markNotificationRead(notificationId)
        }
        viewModel.markAnimeDiscussionSeen(animeId)
        val existing = animeGroups.firstOrNull { it.animeId == animeId }
        if (existing == null) {
            val placeholderLead = CommunityReview(
                id = "tagged_fallback_$animeId",
                userId = "",
                username = "Community",
                avatarKey = "avatar_1",
                avatarUrl = null,
                animeId = animeId,
                animeTitle = animeTitle.ifBlank { "Anime #$animeId" },
                animeCoverImage = animeCoverImage,
                rating = null,
                reviewNotes = "",
                status = WatchStatus.PLAN_TO_WATCH,
                episodeProgress = 0,
                totalEpisodes = null,
                updatedAt = ""
            )
            selectedFallbackGroup = CommunityAnimeGroup(
                animeId = animeId,
                animeTitle = animeTitle.ifBlank { "Anime #$animeId" },
                animeCoverImage = animeCoverImage,
                averageRating10 = null,
                ratedCount = 0,
                notedCount = 0,
                latestUpdatedAt = "",
                totalLikesCount = 0,
                leadReview = placeholderLead,
                reviews = emptyList()
            )
        } else {
            selectedFallbackGroup = null
        }
        activeModalJumpTarget = jumpTarget
        forceLiveChatTab = true
        selectedCommentsAnimeId = animeId
    }

    if (profile == null) {
        ObsidianAmbientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 104.dp)
                    .testTag("community_locked_view")
            ) {
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
                    ObsidianTopBar(
                        title = "Community",
                        subtitle = "Rated & Noted Anime",
                        userProfile = null,
                        onOpenProfile = onNavigateToProfile
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    AppCard(
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier.widthIn(max = 460.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.accentPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = colors.accentPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Sign In for Community Reviews",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Sign in or create an account to browse rated anime showcases and written notes from your circle.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AppPrimaryButton(
                                text = "Sign In / Create Account",
                                onClick = { viewModel.exitLocalPreviewMode() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        return
    }

    if (profile?.memberStatus != MemberStatus.ACTIVE) {
        ObsidianAmbientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 104.dp)
                    .testTag("community_join_gate_view")
            ) {
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
                    ObsidianTopBar(
                        title = "Community",
                        subtitle = "Rated & Noted Anime",
                        userProfile = profile,
                        onOpenProfile = onNavigateToProfile
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    AppCard(
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier.widthIn(max = 460.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.accentPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.GroupAdd,
                                        contentDescription = null,
                                        tint = colors.accentPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Join the Community",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Unlock community anime templates. Once joined, every note and rating you write is automatically added to the shared anime cards.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (!error.isNullOrBlank()) {
                                Surface(
                                    color = colors.error.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = error!!,
                                        color = colors.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(AppSpacing.sm)
                                    )
                                }
                            }
                            AppPrimaryButton(
                                text = if (isLoading) "Joining..." else "Join Community",
                                enabled = !isLoading,
                                onClick = { viewModel.joinCommunity() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("join_community_button")
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val totalEntriesCount = remember(animeGroups) { animeGroups.sumOf { it.notesAndRatingsCount } }

    ObsidianAmbientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1120.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .testTag("community_screen")
            ) {
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
                    ObsidianTopBar(
                        title = "Community",
                        subtitle = "${animeGroups.size} Anime • $totalEntriesCount Notes & Ratings",
                        userProfile = profile,
                        onOpenProfile = onNavigateToProfile
                    )
                }

                if (!error.isNullOrBlank()) {
                    Surface(
                        color = colors.error.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = error!!,
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(AppSpacing.sm)
                        )
                    }
                }

                // Dedicated Community Feed vs "Tagged You" Switcher
                val totalUnreadTags = maxOf(
                    unreadNotificationCount,
                    unseenDiscussionAnime.sumOf { it.unseenCount }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val feedActive = activeSection == "feed"
                    val taggedActive = activeSection == "tagged"
                    Surface(
                        shape = CircleShape,
                        color = if (feedActive) colors.accentPrimary.copy(alpha = 0.2f) else colors.elevatedSurface.copy(alpha = 0.65f),
                        border = BorderStroke(
                            1.dp,
                            if (feedActive) colors.accentPrimary.copy(alpha = 0.7f) else colors.borderSubtle.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(CircleShape)
                            .clickable { activeSection = "feed" }
                            .testTag("community_tab_feed")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = "Community Feed",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (feedActive) colors.highlight else colors.textSecondary
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (taggedActive) colors.accentPrimary.copy(alpha = 0.2f) else colors.elevatedSurface.copy(alpha = 0.65f),
                        border = BorderStroke(
                            1.dp,
                            if (taggedActive) colors.accentPrimary.copy(alpha = 0.7f)
                            else if (totalUnreadTags > 0) Color(0xFFEF4444).copy(alpha = 0.55f)
                            else colors.borderSubtle.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(CircleShape)
                            .clickable {
                                activeSection = "tagged"
                                viewModel.refreshNotifications(reset = true)
                            }
                            .testTag("community_tab_tagged_you")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AlternateEmail,
                                contentDescription = null,
                                tint = if (taggedActive) colors.highlight else colors.accentPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Tagged You",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (taggedActive) colors.highlight else colors.textSecondary
                            )
                            if (totalUnreadTags > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.testTag("tagged_you_unread_badge")
                                ) {
                                    Text(
                                        text = if (totalUnreadTags > 99) "99+" else "$totalUnreadTags",
                                        style = TabularNumSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (activeSection == "tagged") {
                    TaggedYouCommunitySection(
                        notifications = notifications,
                        unreadCount = totalUnreadTags,
                        isLoading = isNotificationsLoading,
                        hasMore = hasMoreNotifications,
                        onRefresh = { viewModel.refreshNotifications(reset = true) },
                        onMarkAllRead = { viewModel.markAllNotificationsRead() },
                        onLoadMore = { viewModel.loadMoreNotifications() },
                        onOpenNotification = { n ->
                            openTaggedAnimeTemplate(
                                animeId = n.animeId,
                                animeTitle = n.animeTitle,
                                animeCoverImage = n.animeCoverImage,
                                jumpTarget = NotificationJumpTarget(
                                    animeId = n.animeId,
                                    commentId = n.commentId,
                                    threadId = n.threadId,
                                    replyId = n.replyId
                                ),
                                notificationId = n.id
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    // Sentiment Filter Pills
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "rev_all") {
                            val active = selectedSentiment == null
                            Surface(
                                shape = CircleShape,
                                color = if (active) colors.accentPrimary.copy(alpha = 0.18f) else colors.elevatedSurface.copy(alpha = 0.65f),
                                border = if (active) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.65f)) else null,
                                modifier = Modifier
                                    .height(34.dp)
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clip(CircleShape)
                                    .clickable { selectedSentimentKey = null }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                    Text(
                                        text = "ALL (${animeGroups.size})",
                                        style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 0.8.sp),
                                        color = if (active) colors.highlight else colors.textSecondary
                                    )
                                }
                            }
                        }
                        items(SentimentCategory.entries, key = { it.key }) { cat ->
                            val active = selectedSentiment == cat
                            val catColor = cat.badgeColor()
                            Surface(
                                shape = CircleShape,
                                color = if (active) catColor.copy(alpha = 0.22f) else colors.elevatedSurface.copy(alpha = 0.65f),
                                border = if (active) BorderStroke(1.dp, catColor.copy(alpha = 0.72f)) else null,
                                modifier = Modifier
                                    .height(34.dp)
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clip(CircleShape)
                                    .clickable { selectedSentimentKey = if (active) null else cat.key }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 13.dp)) {
                                    Text(
                                        text = cat.shortLabel.uppercase(),
                                        style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 0.8.sp),
                                        color = if (active) catColor else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Sort Capsules + Posted By User Filter Toggle
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(ReviewSortOption.entries, key = { it.name }) { opt ->
                            val active = reviewSort == opt
                            Surface(
                                shape = CircleShape,
                                color = if (active) colors.accentPrimary.copy(alpha = 0.18f) else colors.elevatedSurface.copy(alpha = 0.58f),
                                border = if (active) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.6f)) else null,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clip(CircleShape)
                                    .clickable { viewModel.setReviewSortOption(opt) }
                            ) {
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) colors.highlight else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                        item(key = "posted_by_user_filter_toggle") {
                            val active = userFilterOpen || activeSelectedPosters.isNotEmpty()
                            Surface(
                                shape = CircleShape,
                                color = if (active) colors.accentPrimary.copy(alpha = 0.22f) else colors.elevatedSurface.copy(alpha = 0.58f),
                                border = if (active) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.72f)) else null,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clip(CircleShape)
                                    .clickable { userFilterOpen = !userFilterOpen }
                                    .testTag("community_user_filter_toggle")
                            ) {
                                Text(
                                    text = if (activeSelectedPosters.isNotEmpty()) {
                                        "Posted By (${activeSelectedPosters.size}/3) ${if (userFilterOpen) "▴" else "▾"}"
                                    } else {
                                        "Posted By ${if (userFilterOpen) "▴" else "▾"}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) colors.highlight else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Active "by -> 1, 2, 3" Filter Indicator Bar (Up to 3 users max)
                    if (activeSelectedPosters.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.accentPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.md, vertical = 4.dp)
                                .testTag("community_active_user_filter_bar")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "by →",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = colors.accentPrimary
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(activeSelectedPosters, key = { it.key }) { poster ->
                                        Surface(
                                            shape = CircleShape,
                                            color = colors.accentPrimary.copy(alpha = 0.22f),
                                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.65f)),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    selectedUserKeys = selectedUserKeys.filterNot { it == poster.key }
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                modifier = Modifier.padding(start = 6.dp, end = 9.dp, top = 4.dp, bottom = 4.dp)
                                            ) {
                                                MemberAvatar(
                                                    avatarKey = poster.avatarKey,
                                                    avatarUrl = poster.avatarUrl,
                                                    username = poster.username,
                                                    size = 18.dp
                                                )
                                                Text(
                                                    text = poster.username,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = colors.highlight
                                                )
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove ${poster.username}",
                                                    tint = colors.highlight,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "${activeSelectedPosters.size}/3",
                                    style = TabularNumSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = colors.textMuted
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = colors.elevatedSurface,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { selectedUserKeys = emptyList() }
                                ) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Expandable Posted By Username Selector Panel (Up to 3 users max)
                    if (userFilterOpen) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.elevatedSurface.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.32f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.md, vertical = 4.dp)
                                .testTag("community_user_filter_panel")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "SELECT UP TO 3 USERS (by → 1, 2, 3)",
                                    style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 0.8.sp),
                                    color = colors.accentPrimary
                                )
                                if (allPosters.isEmpty()) {
                                    Text(
                                        text = "No members have posted templates yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(allPosters, key = { it.key }) { poster ->
                                            val selIdx = selectedUserKeys.indexOf(poster.key)
                                            val isSelected = selIdx >= 0
                                            val atMaxLimit = !isSelected && selectedUserKeys.size >= 3
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) {
                                                    colors.accentPrimary.copy(alpha = 0.24f)
                                                } else {
                                                    colors.cardBackground.copy(alpha = if (atMaxLimit) 0.45f else 0.85f)
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) colors.accentPrimary else colors.borderSubtle.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        if (isSelected) {
                                                            selectedUserKeys = selectedUserKeys.filterNot { it == poster.key }
                                                        } else if (selectedUserKeys.size < 3) {
                                                            selectedUserKeys = selectedUserKeys + poster.key
                                                        }
                                                    }
                                                    .testTag("community_user_chip_${poster.username}")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    MemberAvatar(
                                                        avatarKey = poster.avatarKey,
                                                        avatarUrl = poster.avatarUrl,
                                                        username = poster.username,
                                                        size = 20.dp
                                                    )
                                                    Text(
                                                        text = poster.username,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isSelected) colors.highlight else colors.textPrimary
                                                    )
                                                    Text(
                                                        text = "(${poster.postCount})",
                                                        style = TabularNumSmall.copy(fontSize = 10.sp),
                                                        color = colors.textMuted
                                                    )
                                                    if (isSelected) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = colors.accentPrimary
                                                        ) {
                                                            Text(
                                                                text = "#${selIdx + 1}",
                                                                style = TabularNumSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                                color = Color(0xFF12151F),
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val pullRefreshState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { viewModel.refreshCommunityData() },
                        state = pullRefreshState,
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullRefreshState,
                                isRefreshing = isLoading,
                                containerColor = colors.elevatedSurface,
                                color = colors.accentPrimary,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("community_pull_to_refresh")
                    ) {
                        if (filteredGroups.isEmpty() && unseenOnlyAnime.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(AppSpacing.xl),
                                contentAlignment = Alignment.Center
                            ) {
                                AppEmptyState(
                                    icon = Icons.Filled.RateReview,
                                    title = if (animeGroups.isEmpty()) "No community notes or ratings yet" else "No matching showcases",
                                    description = if (animeGroups.isEmpty()) {
                                        "Rate an anime or add a note in your list — as a joined member, it automatically appears in Community."
                                    } else {
                                        "Try clearing your sentiment filter to view all community anime templates."
                                    }
                                )
                            }
                        } else {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val columns = if (maxWidth >= 720.dp) 2 else 1
                                val chunked = remember(filteredGroups, columns) { filteredGroups.chunked(columns) }
                                LazyColumn(
                                    contentPadding = PaddingValues(
                                        start = AppSpacing.md,
                                        end = AppSpacing.md,
                                        top = 8.dp,
                                        bottom = 112.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (unseenOnlyAnime.isNotEmpty()) {
                                        items(
                                            items = unseenOnlyAnime,
                                            key = { "unseen_${it.animeId}" }
                                        ) { unseen ->
                                            Box(modifier = Modifier.fillMaxWidth().animateItem()) {
                                                UnseenTaggedAnimeCard(
                                                    unseen = unseen,
                                                    onClick = {
                                                        openTaggedAnimeTemplate(
                                                            animeId = unseen.animeId,
                                                            animeTitle = unseen.animeTitle,
                                                            animeCoverImage = unseen.animeCoverImage,
                                                            jumpTarget = NotificationJumpTarget(
                                                                animeId = unseen.animeId,
                                                                commentId = unseen.latestCommentId,
                                                                threadId = unseen.latestThreadId,
                                                                replyId = unseen.latestReplyId
                                                            )
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    items(
                                        items = chunked,
                                        key = { row -> row.joinToString("_") { it.animeId.toString() } }
                                    ) { rowItems ->
                                        if (columns == 1) {
                                            val firstGroup = rowItems.first()
                                            val unseenInfo = unseenByAnimeId[firstGroup.animeId]
                                            Box(modifier = Modifier.fillMaxWidth().animateItem()) {
                                                CommunityAnimeTemplateCard(
                                                    group = firstGroup,
                                                    unseenInfo = unseenInfo,
                                                    onClick = {
                                                        if (unseenInfo != null) {
                                                            openTaggedAnimeTemplate(
                                                                animeId = unseenInfo.animeId,
                                                                animeTitle = firstGroup.animeTitle,
                                                                animeCoverImage = firstGroup.animeCoverImage,
                                                                jumpTarget = NotificationJumpTarget(
                                                                    animeId = unseenInfo.animeId,
                                                                    commentId = unseenInfo.latestCommentId,
                                                                    threadId = unseenInfo.latestThreadId,
                                                                    replyId = unseenInfo.latestReplyId
                                                                )
                                                            )
                                                        } else {
                                                            onSelectAnime(firstGroup.animeId)
                                                        }
                                                    },
                                                    onOpenComments = {
                                                        selectedFallbackGroup = null
                                                        activeModalJumpTarget = unseenInfo?.let {
                                                            NotificationJumpTarget(it.animeId, it.latestCommentId, it.latestThreadId, it.latestReplyId)
                                                        }
                                                        forceLiveChatTab = true
                                                        selectedCommentsAnimeId = firstGroup.animeId
                                                    },
                                                    onToggleLike = { viewModel.toggleReviewLike(firstGroup.leadReview) },
                                                    initialMyRatingStars = StarRatingUtils.rating10ToStars(
                                                        firstGroup.reviews.firstOrNull { it.userId == profile?.id.orEmpty() }?.rating
                                                    ),
                                                    onQuickRate = { newStars ->
                                                        val existingNote = firstGroup.reviews.firstOrNull { it.userId == profile?.id.orEmpty() }?.reviewNotes.orEmpty()
                                                        viewModel.submitCommunityCommentNote(
                                                            animeId = firstGroup.animeId,
                                                            animeTitle = firstGroup.animeTitle,
                                                            coverImage = firstGroup.animeCoverImage,
                                                            ratingStars = newStars,
                                                            noteText = existingNote,
                                                            onSuccessCallback = {}
                                                        )
                                                    }
                                                )
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().animateItem(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                rowItems.forEach { grp ->
                                                    val unseenInfo = unseenByAnimeId[grp.animeId]
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        CommunityAnimeTemplateCard(
                                                            group = grp,
                                                            unseenInfo = unseenInfo,
                                                            onClick = {
                                                                if (unseenInfo != null) {
                                                                    openTaggedAnimeTemplate(
                                                                        animeId = unseenInfo.animeId,
                                                                        animeTitle = grp.animeTitle,
                                                                        animeCoverImage = grp.animeCoverImage,
                                                                        jumpTarget = NotificationJumpTarget(
                                                                            animeId = unseenInfo.animeId,
                                                                            commentId = unseenInfo.latestCommentId,
                                                                            threadId = unseenInfo.latestThreadId,
                                                                            replyId = unseenInfo.latestReplyId
                                                                        )
                                                                    )
                                                                } else {
                                                                    onSelectAnime(grp.animeId)
                                                                }
                                                            },
                                                            onOpenComments = {
                                                                selectedFallbackGroup = null
                                                                activeModalJumpTarget = unseenInfo?.let {
                                                                    NotificationJumpTarget(it.animeId, it.latestCommentId, it.latestThreadId, it.latestReplyId)
                                                                }
                                                                forceLiveChatTab = true
                                                                selectedCommentsAnimeId = grp.animeId
                                                            },
                                                            onToggleLike = { viewModel.toggleReviewLike(grp.leadReview) },
                                                            initialMyRatingStars = StarRatingUtils.rating10ToStars(
                                                                grp.reviews.firstOrNull { it.userId == profile?.id.orEmpty() }?.rating
                                                            ),
                                                            onQuickRate = { newStars ->
                                                                val existingNote = grp.reviews.firstOrNull { it.userId == profile?.id.orEmpty() }?.reviewNotes.orEmpty()
                                                                viewModel.submitCommunityCommentNote(
                                                                    animeId = grp.animeId,
                                                                    animeTitle = grp.animeTitle,
                                                                    coverImage = grp.animeCoverImage,
                                                                    ratingStars = newStars,
                                                                    noteText = existingNote,
                                                                    onSuccessCallback = {}
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                                repeat(columns - rowItems.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedCommentsGroup != null) {
            val currentUserId = profile?.id.orEmpty()
            val myExistingReview = selectedCommentsGroup.reviews.firstOrNull { it.userId == currentUserId }
            val unseenForGroup = unseenByAnimeId[selectedCommentsGroup.animeId]
            val effectiveJumpTarget = activeModalJumpTarget ?: unseenForGroup?.let {
                NotificationJumpTarget(
                    animeId = it.animeId,
                    commentId = it.latestCommentId,
                    threadId = it.latestThreadId,
                    replyId = it.latestReplyId
                )
            }
            CommunityCommentsDialog(
                group = selectedCommentsGroup,
                viewModel = viewModel,
                hasUnseenDiscussion = unseenForGroup != null,
                openLiveChatInitially = forceLiveChatTab || unseenForGroup != null || effectiveJumpTarget != null,
                highlightTarget = effectiveJumpTarget,
                initialMyRatingStars = StarRatingUtils.rating10ToStars(myExistingReview?.rating),
                initialMyNote = myExistingReview?.reviewNotes.orEmpty(),
                isSubmitting = isSubmittingNote,
                successMessage = noteSuccessFeedback,
                errorMessage = noteErrorFeedback,
                onDismiss = {
                    viewModel.clearCommunityNoteFeedback()
                    selectedCommentsAnimeId = null
                    selectedFallbackGroup = null
                    activeModalJumpTarget = null
                    forceLiveChatTab = false
                },
                onOpenAnimeDetail = {
                    val targetId = selectedCommentsGroup.animeId
                    viewModel.clearCommunityNoteFeedback()
                    selectedCommentsAnimeId = null
                    selectedFallbackGroup = null
                    activeModalJumpTarget = null
                    forceLiveChatTab = false
                    onSelectAnime(targetId)
                },
                onToggleLike = { rev -> viewModel.toggleReviewLike(rev) },
                onSubmitComment = { stars, note, onDone ->
                    viewModel.submitCommunityCommentNote(
                        animeId = selectedCommentsGroup.animeId,
                        animeTitle = selectedCommentsGroup.animeTitle,
                        coverImage = selectedCommentsGroup.animeCoverImage,
                        ratingStars = stars,
                        noteText = note,
                        onSuccessCallback = onDone
                    )
                }
            )
        }
    }
}

/**
 * Single Unified Anime Template Card for Community:
 * Consolidates all joined members' notes and star ratings for the same anime into one card,
 * featuring a themed Comment Icon pill that opens the full multi-user notes & ratings sheet.
 */
@Composable
internal fun CommunityAnimeTemplateCard(
    group: CommunityAnimeGroup,
    onClick: () -> Unit,
    onOpenComments: () -> Unit,
    onToggleLike: (() -> Unit)? = null,
    unseenInfo: UnseenDiscussionAnime? = null,
    initialMyRatingStars: Float = 0f,
    onQuickRate: ((Float) -> Unit)? = null
) {
    val colors = AppTheme.colors
    val lead = group.leadReview
    val formattedDate = remember(group.latestUpdatedAt) { formatCommunityTimestamp(group.latestUpdatedAt) }
    val sentiment = remember(group.averageRating10) { SentimentCategory.fromRating10(group.averageRating10) }
    val avgStars = remember(group.averageRating10) { StarRatingUtils.rating10ToStars(group.averageRating10) }
    val contributors = remember(group.reviews) { group.distinctContributors }
    val extraContributors = (contributors.size - 1).coerceAtLeast(0)
    var showQuickRate by remember(group.animeId) { mutableStateOf(false) }
    var draftQuickStars by remember(group.animeId, initialMyRatingStars) {
        mutableFloatStateOf(if (initialMyRatingStars > 0f) initialMyRatingStars else 4.0f)
    }
    var quickRateTouched by remember(group.animeId) { mutableStateOf(false) }
    var isQuickSliderDragging by remember(group.animeId) { mutableStateOf(false) }

    LaunchedEffect(showQuickRate, draftQuickStars, quickRateTouched, isQuickSliderDragging) {
        if (showQuickRate && quickRateTouched && !isQuickSliderDragging && onQuickRate != null) {
            delay(3000L)
            onQuickRate(StarRatingUtils.roundTenth(draftQuickStars))
            quickRateTouched = false
            showQuickRate = false
        }
    }

    AppCard(
        shape = RoundedCornerShape(24.dp),
        onClick = onClick,
        modifier = Modifier.testTag("community_anime_card_${group.animeId}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ResilientPosterImage(
                        imageUrl = group.animeCoverImage,
                        contentDescription = group.animeTitle,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(width = 66.dp, height = 96.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = group.animeTitle,
                                fontFamily = CormorantSerifFamily,
                                fontSize = 21.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (group.isAdult) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    modifier = Modifier.testTag("community_adult_badge_${group.animeId}")
                                ) {
                                    Text(
                                        text = "18+",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.4.sp
                                        ),
                                        color = Color(0xFFFCA5A5),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (unseenInfo != null) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .border(1.5.dp, colors.cardBackground, CircleShape)
                                        .testTag("community_unseen_red_dot_${group.animeId}")
                                )
                            }
                        }
                        if (unseenInfo != null) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444).copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                    val actionWord = if (unseenInfo.latestNotificationType == "reply") "replied" else "tagged you"
                                    val extra = if (unseenInfo.unseenCount > 1) " (+${unseenInfo.unseenCount - 1})" else ""
                                    Text(
                                        text = "${unseenInfo.latestSenderUsername} $actionWord$extra • Tap to open",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFFFCA5A5),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    if (avgStars > 0f) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StarRatingRow(
                                stars = avgStars,
                                starSize = 16.dp,
                                showNumeric = true
                            )
                            if (group.ratedCount > 1) {
                                Text(
                                    text = "(${group.ratedCount})",
                                    style = TabularNumSmall.copy(fontSize = 11.sp),
                                    color = colors.textMuted
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = colors.accentPrimary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "COMMUNITY NOTES",
                                style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 0.8.sp),
                                color = colors.accentPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (sentiment != null) {
                        SentimentPill(sentiment = sentiment, solid = false, fullText = true)
                    }
                }
            }

            // Editorial Quote Box: displays lead/latest written note (or summary if rating-only)
            val leadStars = StarRatingUtils.rating10ToStars(lead.rating)
            val previewQuote = if (lead.reviewNotes.isNotBlank()) {
                "“${lead.reviewNotes}”"
            } else {
                "Rated ${StarRatingUtils.formatStars(leadStars)}★ by ${lead.username}"
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.elevatedSurface.copy(alpha = if (colors.isDark) 0.55f else 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onOpenComments() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(colors.accentPrimary.copy(alpha = 0.65f))
                        )
                        Text(
                            text = previewQuote,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = if (lead.reviewNotes.isNotBlank()) FontStyle.Italic else FontStyle.Normal,
                                lineHeight = 20.sp
                            ),
                            color = colors.textPrimary.copy(alpha = 0.90f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (group.notesAndRatingsCount > 1) {
                        Text(
                            text = "Tap comment icon to view all ${group.notesAndRatingsCount} notes & ratings",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.accentPrimary,
                            modifier = Modifier.padding(start = 13.dp)
                        )
                    }
                }
            }

            // Attribution Footer + Themed Comment Icon Pill + Like Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val visibleAvatars = contributors.take(3)
                    Box(
                        modifier = Modifier.width((40 + (visibleAvatars.size - 1) * 22).dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        visibleAvatars.forEachIndexed { idx, contributor ->
                            Box(
                                modifier = Modifier.offset(x = (idx * 22).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                MemberAvatar(
                                    avatarKey = contributor.avatarKey,
                                    avatarUrl = contributor.avatarUrl,
                                    username = contributor.username,
                                    size = 26.dp
                                )
                            }
                        }
                    }
                    Text(
                        text = if (extraContributors > 0) "${contributors.first().username} +$extraContributors" else contributors.first().username,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (formattedDate.isNotBlank()) {
                        Text(
                            text = "• $formattedDate",
                            style = TabularNumSmall.copy(fontSize = 10.sp),
                            color = colors.textMuted,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onQuickRate != null) {
                        Surface(
                            shape = CircleShape,
                            color = colors.accentPrimary.copy(alpha = if (showQuickRate) 0.24f else 0.14f),
                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .height(34.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable { showQuickRate = !showQuickRate }
                                .testTag("quick_rate_anime_${group.animeId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Rate directly in Community",
                                    tint = colors.accentPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (initialMyRatingStars > 0f) StarRatingUtils.formatStars(initialMyRatingStars) else "Rate",
                                    style = TabularNumSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = colors.highlight
                                )
                            }
                        }
                    }

                    // Themed Comment Icon Pill (Obsidian + Champagne Gold)
                    Surface(
                        shape = CircleShape,
                        color = colors.accentPrimary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .height(34.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clip(CircleShape)
                            .clickable { onOpenComments() }
                            .testTag("comment_anime_${group.animeId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "View all ${group.notesAndRatingsCount} community notes and ratings",
                                tint = colors.accentPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${group.notesAndRatingsCount}",
                                style = TabularNumSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = colors.highlight
                            )
                        }
                    }

                    // Lead Review Like Pill
                    Surface(
                        shape = CircleShape,
                        color = if (lead.likedByMe) colors.accentPrimary.copy(alpha = 0.2f) else colors.elevatedSurface.copy(alpha = 0.65f),
                        border = if (lead.likedByMe) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.6f)) else null,
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .clickable(enabled = onToggleLike != null) { onToggleLike?.invoke() }
                            .testTag("like_review_${lead.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (lead.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (lead.likedByMe) "Unlike review" else "Like review",
                                tint = if (lead.likedByMe) colors.accentPrimary else colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${lead.likesCount}",
                                style = TabularNumSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = if (lead.likedByMe) colors.highlight else colors.textSecondary
                            )
                        }
                    }
                }
            }

            if (showQuickRate && onQuickRate != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.elevatedSurface.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("community_quick_rate_drawer_${group.animeId}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "DIRECT RATING",
                                    style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                                    color = colors.accentPrimary
                                )
                                Text(
                                    text = "${StarRatingUtils.formatStars(draftQuickStars)} ★",
                                    style = TabularNumSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    color = colors.highlight
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.cardBackground,
                                    border = BorderStroke(1.dp, colors.borderSubtle.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            draftQuickStars = StarRatingUtils.roundTenth((draftQuickStars - 0.1f).coerceAtLeast(0.1f))
                                            quickRateTouched = true
                                        }
                                        .testTag("quick_rate_minus_${group.animeId}")
                                ) {
                                    Text(
                                        text = "−0.1 ★",
                                        style = TabularNumSmall.copy(fontSize = 10.sp),
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = colors.cardBackground,
                                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            draftQuickStars = StarRatingUtils.roundTenth((draftQuickStars + 0.1f).coerceAtMost(5.0f))
                                            quickRateTouched = true
                                        }
                                        .testTag("quick_rate_plus_${group.animeId}")
                                ) {
                                    Text(
                                        text = "+0.1 ★",
                                        style = TabularNumSmall.copy(fontSize = 10.sp),
                                        color = colors.accentPrimary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                        Slider(
                            value = draftQuickStars.coerceIn(0.1f, 5.0f),
                            onValueChange = { raw ->
                                isQuickSliderDragging = true
                                quickRateTouched = true
                                draftQuickStars = raw.coerceIn(0.1f, 5.0f)
                            },
                            onValueChangeFinished = {
                                draftQuickStars = StarRatingUtils.roundTenth(draftQuickStars)
                                isQuickSliderDragging = false
                                quickRateTouched = true
                            },
                            valueRange = 0.1f..5.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accentPrimary,
                                activeTrackColor = colors.accentPrimary,
                                inactiveTrackColor = colors.borderSubtle.copy(alpha = 0.65f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("quick_rate_slider_${group.animeId}")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "0.1 ★  —  5.0 ★",
                                style = TabularNumSmall.copy(fontSize = 10.sp),
                                color = colors.textMuted
                            )
                            Surface(
                                shape = CircleShape,
                                color = colors.accentPrimary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onQuickRate(StarRatingUtils.roundTenth(draftQuickStars))
                                        showQuickRate = false
                                    }
                                    .testTag("quick_rate_save_${group.animeId}")
                            ) {
                                Text(
                                    text = "Save",
                                    style = TabularNumSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = colors.onAccentPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnseenTaggedAnimeCard(
    unseen: UnseenDiscussionAnime,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    AppCard(
        shape = RoundedCornerShape(24.dp),
        onClick = onClick,
        modifier = Modifier.testTag("community_unseen_anime_card_${unseen.animeId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ResilientPosterImage(
                imageUrl = unseen.animeCoverImage,
                contentDescription = unseen.animeTitle,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(width = 60.dp, height = 86.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .testTag("community_unseen_red_dot_${unseen.animeId}")
                    )
                    Text(
                        text = unseen.animeTitle.ifBlank { "Anime Discussion #${unseen.animeId}" },
                        fontFamily = CormorantSerifFamily,
                        fontSize = 20.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                val actionLabel = if (unseen.latestNotificationType == "reply") "replied to you" else "tagged you in discussion"
                Text(
                    text = "${unseen.latestSenderUsername} $actionLabel${if (unseen.unseenCount > 1) " • ${unseen.unseenCount} unseen" else ""}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFFCA5A5)
                )
                if (unseen.latestPreview.isNotBlank()) {
                    Text(
                        text = "“${unseen.latestPreview}”",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Open anime discussion to read & clear indicator",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accentPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaggedYouCommunitySection(
    notifications: List<UserNotificationItem>,
    unreadCount: Int,
    isLoading: Boolean,
    hasMore: Boolean,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenNotification: (UserNotificationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val pullRefreshState = rememberPullToRefreshState()

    Column(modifier = modifier.testTag("tagged_you_section")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TAGGED YOU",
                    style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 1.8.sp),
                    color = colors.accentPrimary
                )
                Text(
                    text = if (unreadCount > 0) "$unreadCount unread mention${if (unreadCount == 1) "" else "s"} — tap to jump directly" else "All caught up on community tags",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            if (unreadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = colors.accentPrimary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onMarkAllRead)
                        .testTag("tagged_you_mark_all_read")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = "Mark all read",
                            tint = colors.accentPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Mark all read",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.highlight
                        )
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = isLoading,
                    containerColor = colors.elevatedSurface,
                    color = colors.accentPrimary,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (notifications.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AppSpacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    AppEmptyState(
                        icon = Icons.Filled.AlternateEmail,
                        title = "No tags yet",
                        description = "When someone mentions you with @username or replies to you in any anime template (new or old), it appears here for one-tap jump."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = AppSpacing.md,
                        end = AppSpacing.md,
                        top = 6.dp,
                        bottom = 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notifications, key = { it.id }) { item ->
                        val isUnread = !item.isRead
                        val cleanPreview = remember(item.previewText) {
                            CommunityEmojiCacheService.formatPreviewSnippet(item.previewText)
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isUnread) Color(0xFF14141E) else colors.cardBackground,
                            border = BorderStroke(
                                1.dp,
                                if (isUnread) Color(0xFFEF4444).copy(alpha = 0.55f) else colors.borderSubtle.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onOpenNotification(item) }
                                .testTag("tagged_you_card_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                ResilientPosterImage(
                                    imageUrl = item.animeCoverImage,
                                    contentDescription = item.animeTitle,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(width = 56.dp, height = 80.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isUnread) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFEF4444))
                                                        .testTag("tagged_you_unread_dot_${item.id}")
                                                )
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = colors.accentPrimary.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = item.communityName.uppercase(),
                                                    style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 0.9.sp),
                                                    color = colors.accentPrimary,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = formatCommunityTimestamp(item.createdAt),
                                            style = TabularNumSmall.copy(fontSize = 10.sp),
                                            color = colors.textMuted
                                        )
                                    }

                                    Text(
                                        text = item.animeTitle,
                                        fontFamily = CormorantSerifFamily,
                                        fontSize = 19.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        MemberAvatar(
                                            avatarKey = item.senderAvatarKey,
                                            avatarUrl = item.senderAvatarUrl,
                                            username = item.senderUsername,
                                            size = 20.dp
                                        )
                                        val actionWord = if (item.notificationType == "thread_reply") "replied to you" else "tagged you"
                                        Text(
                                            text = "@${item.senderUsername} $actionWord",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isUnread) Color(0xFFF5E4BE) else colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (cleanPreview.isNotBlank()) {
                                        Text(
                                            text = "“$cleanPreview”",
                                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                            color = colors.textSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (hasMore) {
                        item(key = "load_more_tags") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.elevatedSurface,
                                    border = BorderStroke(1.dp, colors.borderSubtle),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(enabled = !isLoading, onClick = onLoadMore)
                                ) {
                                    Text(
                                        text = if (isLoading) "Loading..." else "Load older tags",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.highlight,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * LivTrAk Community Hub Modal:
 * Matches the LivTrAk Live Chat reference design with 60x86 poster, COMMUNITY HUB eyebrow,
 * Cormorant Garamond title + red 18+ badge, Live Chat & Reviews tabs, and docked Live Chat stream.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommunityCommentsDialog(
    group: CommunityAnimeGroup,
    viewModel: AnimeViewModel,
    hasUnseenDiscussion: Boolean = false,
    openLiveChatInitially: Boolean = true,
    highlightTarget: NotificationJumpTarget? = null,
    initialMyRatingStars: Float,
    initialMyNote: String,
    isSubmitting: Boolean = false,
    successMessage: String? = null,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onOpenAnimeDetail: () -> Unit,
    onToggleLike: (CommunityReview) -> Unit,
    onSubmitComment: (ratingStars: Float, noteText: String, onDone: () -> Unit) -> Unit
) {
    val colors = AppTheme.colors
    val haptics = LocalHapticFeedback.current
    val currentProfile by viewModel.userProfile.collectAsState()
    val currentUserId = currentProfile?.id.orEmpty()
    val avgStars = StarRatingUtils.rating10ToStars(group.averageRating10)
    var selectedStars by remember(group.animeId) { mutableFloatStateOf(initialMyRatingStars) }
    var noteDraft by remember(group.animeId) { mutableStateOf(initialMyNote) }
    var isEditingReview by remember(group.animeId) { mutableStateOf(false) }
    var activeTab by remember(group.animeId, openLiveChatInitially) {
        mutableStateOf(if (openLiveChatInitially || hasUnseenDiscussion) "discussion" else "notes")
    }
    var pendingReplyUser by remember(group.animeId) { mutableStateOf<MentionableUser?>(null) }
    var openNoteMenuId by remember(group.animeId) { mutableStateOf<String?>(null) }

    LaunchedEffect(group.animeId) {
        viewModel.loadAnimeDiscussion(group.animeId, reset = true)
        viewModel.loadCommunityEmojis()
    }

    LaunchedEffect(group.animeId, activeTab) {
        if (activeTab == "discussion") {
            viewModel.markAnimeDiscussionSeen(group.animeId)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF030304),
            border = BorderStroke(1.dp, Color(0x33E8C88F)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .heightIn(min = 520.dp, max = 820.dp)
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .testTag("community_comments_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // LivTrAk Hub Header: 60x86 Poster + COMMUNITY HUB Eyebrow + Cormorant Title + 18+ Badge + Close Circle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ResilientPosterImage(
                        imageUrl = group.animeCoverImage,
                        contentDescription = group.animeTitle,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .size(width = 60.dp, height = 86.dp)
                            .clickable { onOpenAnimeDetail() }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp)
                            .clickable { onOpenAnimeDetail() },
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "COMMUNITY HUB",
                            style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 2.sp),
                            color = Color(0xFFE8C88F)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = group.animeTitle,
                                fontFamily = CormorantSerifFamily,
                                fontSize = 21.sp,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF8F4EB),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (group.isAdult) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                                ) {
                                    Text(
                                        text = "18+",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.4.sp
                                        ),
                                        color = Color(0xFFFCA5A5),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        val avgLabel = if (avgStars > 0f) "★ ${StarRatingUtils.formatStars(avgStars)} avg • " else ""
                        val memberCount = group.distinctContributors.size
                        Text(
                            text = "${avgLabel}${memberCount} ${if (memberCount == 1) "member" else "members"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFF9A9AA8)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0x14FFFFFF),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                            .testTag("close_community_comments_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close chat hub",
                                tint = Color(0xFFF8F4EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // LivTrAk Underlined Tabs: Live Chat | Reviews (N)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val discussionActive = activeTab == "discussion"
                    val notesActive = activeTab == "notes"

                    Column(
                        modifier = Modifier
                            .clickable { activeTab = "discussion" }
                            .padding(vertical = 8.dp)
                            .testTag("community_modal_tab_discussion"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Live Chat",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = if (discussionActive) Color(0xFFE8C88F) else Color(0xFF9A9AA8)
                            )
                            if (hasUnseenDiscussion && !discussionActive) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(68.dp)
                                .height(2.dp)
                                .background(if (discussionActive) Color(0xFFE8C88F) else Color.Transparent)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .clickable { activeTab = "notes" }
                            .padding(vertical = 8.dp)
                            .testTag("community_modal_tab_notes"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Reviews (${group.reviews.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (notesActive) Color(0xFFE8C88F) else Color(0xFF9A9AA8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(78.dp)
                                .height(2.dp)
                                .background(if (notesActive) Color(0xFFE8C88F) else Color.Transparent)
                        )
                    }
                }

                HorizontalDivider(color = Color(0x14FFFFFF))

                if (activeTab == "discussion") {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("community_modal_discussion_container")
                    ) {
                        AnimeDiscussionSection(
                            animeId = group.animeId,
                            animeTitle = group.animeTitle,
                            animeCover = group.animeCoverImage,
                            viewModel = viewModel,
                            highlightTarget = highlightTarget,
                            initialReplyTargetUser = pendingReplyUser,
                            onInitialReplyTargetConsumed = { pendingReplyUser = null },
                            inModal = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    // All Contributors' Notes & Ratings List (with Reply button + Long-press / Right-click Reply menu)
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 330.dp)
                            .fillMaxWidth()
                            .testTag("community_comments_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(group.reviews, key = { it.id }) { rev ->
                            val revStars = StarRatingUtils.rating10ToStars(rev.rating)
                            val revSentiment = SentimentCategory.fromRating10(rev.rating)
                            val revDate = formatCommunityTimestamp(rev.updatedAt)
                            val triggerReplyToReviewer = {
                                pendingReplyUser = MentionableUser(
                                    id = rev.userId,
                                    username = rev.username,
                                    avatarKey = rev.avatarKey,
                                    avatarUrl = rev.avatarUrl
                                )
                                activeTab = "discussion"
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = colors.cardBackground,
                                    border = BorderStroke(1.dp, colors.borderSubtle.copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .pointerInput(rev.id) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                                        openNoteMenuId = rev.id
                                                    }
                                                }
                                            }
                                        }
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                openNoteMenuId = rev.id
                                            }
                                        )
                                        .testTag("community_comment_row_${rev.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                MemberAvatar(
                                                    avatarKey = rev.avatarKey,
                                                    avatarUrl = rev.avatarUrl,
                                                    username = rev.username,
                                                    size = 34.dp
                                                )
                                                Column {
                                                    Text(
                                                        text = rev.username,
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = colors.textPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (revDate.isNotBlank()) {
                                                        Text(
                                                            text = revDate,
                                                            style = TabularNumSmall.copy(fontSize = 10.sp),
                                                            color = colors.textMuted
                                                        )
                                                    }
                                                }
                                            }

                                            val isMyReview = currentUserId.isNotBlank() && rev.userId == currentUserId
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isMyReview) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = colors.elevatedSurface.copy(alpha = 0.85f),
                                                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.55f)),
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .clickable {
                                                                selectedStars = revStars
                                                                noteDraft = rev.reviewNotes
                                                                isEditingReview = true
                                                            }
                                                            .testTag("community_note_edit_${rev.id}")
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Edit,
                                                                contentDescription = "Edit my review",
                                                                tint = colors.accentPrimary,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Text(
                                                                text = "Edit",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                                color = colors.accentPrimary
                                                            )
                                                        }
                                                    }
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = colors.accentPrimary.copy(alpha = 0.14f),
                                                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.45f)),
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .clickable { triggerReplyToReviewer() }
                                                        .testTag("community_note_reply_${rev.id}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.Reply,
                                                            contentDescription = "Reply to ${rev.username}",
                                                            tint = colors.accentPrimary,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = "Reply",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = colors.highlight
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (rev.likedByMe) colors.accentPrimary.copy(alpha = 0.2f) else colors.elevatedSurface.copy(alpha = 0.65f),
                                                    border = if (rev.likedByMe) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.6f)) else null,
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .clickable { onToggleLike(rev) }
                                                        .testTag("comment_like_${rev.id}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (rev.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                            contentDescription = if (rev.likedByMe) "Unlike" else "Like",
                                                            tint = if (rev.likedByMe) colors.accentPrimary else colors.textSecondary,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = "${rev.likesCount}",
                                                            style = TabularNumSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                                            color = if (rev.likedByMe) colors.highlight else colors.textSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (revStars > 0f || revSentiment != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (revStars > 0f) {
                                                    StarRatingRow(stars = revStars, starSize = 14.dp, showNumeric = true)
                                                }
                                                if (revSentiment != null) {
                                                    SentimentPill(sentiment = revSentiment, solid = false, fullText = false)
                                                }
                                            }
                                        }

                                        if (rev.reviewNotes.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = colors.elevatedSurface.copy(alpha = if (colors.isDark) 0.55f else 0.72f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(3.dp)
                                                            .height(26.dp)
                                                            .clip(CircleShape)
                                                            .background(colors.accentPrimary.copy(alpha = 0.7f))
                                                    )
                                                    Text(
                                                        text = "“${rev.reviewNotes}”",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontStyle = FontStyle.Italic,
                                                            lineHeight = 19.sp
                                                        ),
                                                        color = colors.textPrimary,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        } else if (revStars > 0f) {
                                            Text(
                                                text = "Rated ${StarRatingUtils.formatStars(revStars)}★ (no written note yet)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textMuted
                                            )
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = openNoteMenuId == rev.id,
                                    onDismissRequest = { openNoteMenuId = null },
                                    containerColor = colors.backgroundSecondary
                                ) {
                                    if (currentUserId.isNotBlank() && rev.userId == currentUserId) {
                                        DropdownMenuItem(
                                            text = { Text("Edit my review", color = colors.textPrimary) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = null,
                                                    tint = colors.accentPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                openNoteMenuId = null
                                                selectedStars = revStars
                                                noteDraft = rev.reviewNotes
                                                isEditingReview = true
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Reply to @${rev.username}", color = colors.textPrimary) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                                contentDescription = null,
                                                tint = colors.accentPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            openNoteMenuId = null
                                            triggerReplyToReviewer()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.65f))

                    // Inline Composer: Add or update your note & rating in the same anime template
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isEditingReview -> "EDITING YOUR REVIEW"
                                            initialMyNote.isNotBlank() || initialMyRatingStars > 0f -> "UPDATE RATING"
                                            else -> "YOUR RATING"
                                        },
                                        style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                                        color = colors.accentPrimary
                                    )
                                    if (selectedStars > 0f) {
                                        Text(
                                            text = "${StarRatingUtils.formatStars(selectedStars)} ★",
                                            style = TabularNumSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                            color = colors.highlight
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.elevatedSurface,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                val base = if (selectedStars > 0f) selectedStars else 0.2f
                                                selectedStars = StarRatingUtils.roundTenth((base - 0.1f).coerceAtLeast(0.1f))
                                            }
                                    ) {
                                        Text(
                                            text = "−0.1 ★",
                                            style = TabularNumSmall.copy(fontSize = 10.sp),
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.elevatedSurface,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedStars = StarRatingUtils.roundTenth((selectedStars + 0.1f).coerceAtMost(5f))
                                            }
                                    ) {
                                        Text(
                                            text = "+0.1 ★",
                                            style = TabularNumSmall.copy(fontSize = 10.sp),
                                            color = colors.accentPrimary,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Slider(
                                value = if (selectedStars > 0f) selectedStars.coerceIn(0.1f, 5.0f) else 4.0f,
                                onValueChange = { raw -> selectedStars = raw.coerceIn(0.1f, 5.0f) },
                                onValueChangeFinished = { selectedStars = StarRatingUtils.roundTenth(selectedStars) },
                                valueRange = 0.1f..5.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.accentPrimary,
                                    activeTrackColor = colors.accentPrimary,
                                    inactiveTrackColor = colors.borderSubtle.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("community_modal_rating_slider")
                            )
                        }

                        if (!successMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.success.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, colors.success.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("community_comment_success_banner")
                            ) {
                                Text(
                                    text = "✓ $successMessage",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.success,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        if (!errorMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.error.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("community_comment_error_banner")
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it },
                            enabled = !isSubmitting,
                            placeholder = { Text("Share your thoughts on ${group.animeTitle}...", fontSize = 13.sp) },
                            maxLines = 3,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accentPrimary,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("community_comment_input")
                        )

                        AppPrimaryButton(
                            text = when {
                                isSubmitting -> "Saving..."
                                isEditingReview -> "Save Edited Review"
                                initialMyNote.isNotBlank() || initialMyRatingStars > 0f -> "Update Review in Community"
                                else -> "Post Review to Community"
                            },
                            enabled = !isSubmitting && (noteDraft.trim().isNotEmpty() || selectedStars > 0f),
                            onClick = {
                                onSubmitComment(selectedStars, noteDraft) {
                                    isEditingReview = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("community_comment_submit_button")
                        )
                    }
                    }
                }
            }
        }
    }
}

/**
 * Editorial Anime Template Card kept for AnimeDetailScreen single-review cards.
 */
@Composable
internal fun EditorialCommunityNoteCard(
    review: CommunityReview,
    onClick: () -> Unit,
    showAnimeHeader: Boolean = true,
    onToggleLike: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val formattedDate = remember(review.updatedAt) { formatCommunityTimestamp(review.updatedAt) }
    val sentiment = remember(review.rating) { SentimentCategory.fromRating10(review.rating) }
    val starRating = remember(review.rating) { StarRatingUtils.rating10ToStars(review.rating) }

    AppCard(
        shape = RoundedCornerShape(24.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showAnimeHeader) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ResilientPosterImage(
                        imageUrl = review.animeCoverImage,
                        contentDescription = review.animeTitle,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(width = 66.dp, height = 96.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = review.animeTitle,
                            fontFamily = CormorantSerifFamily,
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (starRating > 0f) {
                            StarRatingRow(
                                stars = starRating,
                                starSize = 16.dp,
                                showNumeric = true
                            )
                        }
                        if (sentiment != null) {
                            SentimentPill(sentiment = sentiment, solid = false, fullText = true)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (starRating > 0f) {
                        StarRatingRow(stars = starRating, starSize = 16.dp, showNumeric = true)
                    }
                    if (sentiment != null) {
                        SentimentPill(sentiment = sentiment, solid = false, fullText = true)
                    }
                }
            }

            if (review.reviewNotes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.elevatedSurface.copy(alpha = if (colors.isDark) 0.55f else 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(colors.accentPrimary.copy(alpha = 0.65f))
                        )
                        Text(
                            text = "“${review.reviewNotes}”",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 20.sp
                            ),
                            color = colors.textPrimary.copy(alpha = 0.90f),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Minimal reviewer attribution footer + Like button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemberAvatar(
                        avatarKey = review.avatarKey,
                        avatarUrl = review.avatarUrl,
                        username = review.username,
                        size = 26.dp
                    )
                    Text(
                        text = review.username,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    if (formattedDate.isNotBlank()) {
                        Text(
                            text = "• $formattedDate",
                            style = TabularNumSmall.copy(fontSize = 10.sp),
                            color = colors.textMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onEdit != null) {
                        Surface(
                            shape = CircleShape,
                            color = colors.elevatedSurface.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onEdit() }
                                .testTag("edit_review_${review.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit review",
                                    tint = colors.accentPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.accentPrimary
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (review.likedByMe) colors.accentPrimary.copy(alpha = 0.2f) else colors.elevatedSurface.copy(alpha = 0.65f),
                        border = if (review.likedByMe) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.6f)) else null,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = onToggleLike != null) { onToggleLike?.invoke() }
                            .testTag("like_review_${review.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (review.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (review.likedByMe) "Unlike review" else "Like review",
                                tint = if (review.likedByMe) colors.accentPrimary else colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${review.likesCount}",
                                style = TabularNumSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = if (review.likedByMe) colors.highlight else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

