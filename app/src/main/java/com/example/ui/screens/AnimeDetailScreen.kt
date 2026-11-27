package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.AnimePartCategory
import com.example.model.ProgressRules
import com.example.model.SentimentCategory
import com.example.model.StarRatingUtils
import com.example.model.VisibilityLevel
import com.example.model.WatchStatus
import com.example.ui.components.AnimeDiscussionSection
import com.example.ui.components.formatCompactCountdown
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldSoft
import com.example.ui.theme.ChampagneOnGold
import com.example.ui.theme.CormorantDisplayNumeral
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumMedium
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AppCard
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.AppProgressBar
import com.example.ui.widgets.AppSecondaryButton
import com.example.ui.widgets.InlineEpisodeNumberStepper
import com.example.ui.widgets.ObsidianAmbientBackground
import com.example.ui.widgets.RatingBadge
import com.example.ui.widgets.RemoveFromHomeConfirmDialog
import com.example.ui.widgets.ResilientPosterImage
import com.example.ui.widgets.StarRatingRow
import com.example.ui.widgets.TrackingEditorDialog
import com.example.ui.widgets.WatchStatusPill
import com.example.ui.widgets.badgeColor
import com.example.ui.widgets.formatLabel
import com.example.viewmodel.AnimeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    animeId: Int,
    viewModel: AnimeViewModel,
    onBack: () -> Unit,
    onSelectAnime: (Int) -> Unit
) {
    val colors = AppTheme.colors
    val anime by viewModel.selectedAnimeDetail.collectAsState()
    val richDetail by viewModel.selectedRichDetail.collectAsState()
    val isLoading by viewModel.isDetailLoading.collectAsState()
    val reviews by viewModel.animeReviews.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val privacy by viewModel.privacySettings.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val ratingFeedback by viewModel.ratingFeedbackMessage.collectAsState()
    val pendingJump by viewModel.pendingNotificationJump.collectAsState()
    val discussionComments by viewModel.discussionComments.collectAsState()
    val listState = rememberLazyListState()

    val ratedAndNotedReviews = remember(reviews) {
        reviews.filter { (it.rating ?: 0.0) > 0.0 || it.reviewNotes.isNotBlank() }
    }

    var synopsisExpanded by remember { mutableStateOf(false) }
    var showTrackingEditor by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showRemoveFromHomeConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(animeId) {
        viewModel.selectAnimeDetail(animeId)
    }

    val activeJumpForThisAnime = pendingJump?.takeIf { it.animeId == animeId }
    LaunchedEffect(activeJumpForThisAnime, discussionComments.size) {
        if (activeJumpForThisAnime != null && discussionComments.isNotEmpty()) {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    val current = anime
    if (current == null) {
        ObsidianAmbientBackground {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(color = colors.accentPrimary)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        Text("Anime details unavailable.", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        AppPrimaryButton(text = "Go Back", onClick = onBack)
                    }
                }
            }
        }
        return
    }

    val resolvedTitle = current.getResolvedTitle(preferences.titleLanguage)
    val studiosList = richDetail?.studios?.takeIf { it.isNotEmpty() }
        ?: current.studios.split(",").map { it.trim() }.filter { it.isNotBlank() }

    var nowUnix by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    val nextTime = current.getEffectiveAiringTime(nowUnix)
    val nextEpNum = current.getEffectiveNextEpisodeNumber(nowUnix)
    val isFinished = current.status.equals("FINISHED", ignoreCase = true)

    LaunchedEffect(nextTime, isFinished) {
        if (nextTime == null || nextTime <= 0L || isFinished) return@LaunchedEffect
        while (true) {
            nowUnix = System.currentTimeMillis() / 1000
            val diff = nextTime - nowUnix
            delay(if (diff > 86400) 30_000L else 1000L)
        }
    }

    val watchlist by viewModel.watchlist.collectAsState()
    val trackedById = remember(watchlist, current) {
        val map = watchlist.associateBy { it.id }.toMutableMap()
        map[current.id] = current
        map
    }

    val seasonEntries = remember(richDetail) { richDetail?.seasons.orEmpty() }
    val moviesAndOvas = remember(richDetail) { richDetail?.moviesAndOvas.orEmpty() }
    val franchiseId = richDetail?.franchiseId ?: current.id
    val rawMainSeasonNodeCount = richDetail?.rawMainSeasonNodeCount ?: seasonEntries.size

    val activeSeasonEntry = remember(seasonEntries, current.id) {
        seasonEntries.firstOrNull { it.isCurrent || it.containsAnimeId(current.id) }
    }
    val seasonIdSet = remember(seasonEntries, moviesAndOvas) {
        val sIds = seasonEntries.flatMap { s -> listOf(s.id) + s.subEntries.map { it.id } }
        val mIds = moviesAndOvas.map { it.id }
        (sIds + mIds).toSet()
    }
    val otherParts = remember(richDetail, seasonIdSet, current.id) {
        richDetail?.relations.orEmpty()
            .filter { rel ->
                rel.id != current.id &&
                    (rel.mediaType.isNullOrBlank() || rel.mediaType.equals("ANIME", ignoreCase = true)) &&
                    !seasonIdSet.contains(rel.id)
            }
            .distinctBy { it.id }
    }

    ObsidianAmbientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 980.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .testTag("anime_detail_screen"),
                contentPadding = PaddingValues(bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cinematic Hero Backdrop fading cleanly into #030304
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(312.dp)
                    ) {
                        AsyncImage(
                            model = current.bannerImage?.takeIf { it.isNotBlank() } ?: current.coverImage,
                            contentDescription = resolvedTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = 0.45f),
                                        0.30f to Color.Black.copy(alpha = 0.15f),
                                        0.68f to colors.backgroundMain.copy(alpha = 0.80f),
                                        1f to colors.backgroundMain
                                    )
                                )
                        )

                        // Circular Glass Top Buttons (Back & Favorite)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0x9908080C),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onBack)
                                    .testTag("detail_back_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0x9908080C),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.toggleFavorite(current.id, !current.isFavorite) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (current.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (current.isFavorite) colors.error else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Cover Inset + Cormorant Serif Title Overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ResilientPosterImage(
                                imageUrl = current.coverImage,
                                contentDescription = resolvedTitle,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .width(104.dp)
                                    .height(152.dp)
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(
                                    text = resolvedTitle,
                                    fontFamily = CormorantSerifFamily,
                                    fontSize = 30.sp,
                                    lineHeight = 32.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subJp = current.titleNative?.takeIf { it.isNotBlank() }
                                    ?: current.titleRomaji.takeIf { it.isNotBlank() && it != resolvedTitle }
                                if (!subJp.isNullOrBlank()) {
                                    Text(
                                        text = subJp.uppercase(),
                                        style = MicroUpperLabel.copy(fontSize = 9.5.sp, letterSpacing = 1.4.sp),
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val userStars = StarRatingUtils.score100ToStars(current.userScore)
                                    val catalogStars = StarRatingUtils.score100ToStars(current.score)
                                    val badgeStars = if (userStars > 0f) userStars else catalogStars
                                    if (badgeStars > 0f) {
                                        RatingBadge(scoreText = StarRatingUtils.formatStars(badgeStars))
                                    }
                                    if (current.inWatchlist) {
                                        WatchStatusPill(status = current.watchStatusEnum)
                                    }
                                }
                            }
                        }
                    }
                }

                // Editorial Big-Card 1: EPISODE PROGRESS & RATING
                item {
                    val isMergedContinuous = activeSeasonEntry != null && activeSeasonEntry.subEntries.size > 1
                    val total = if (isMergedContinuous) {
                        activeSeasonEntry?.episodes ?: current.totalEpisodes
                    } else {
                        current.totalEpisodes
                    }
                    val effectiveWatched = if (isMergedContinuous) {
                        viewModel.computeContinuousWatchedProgress(activeSeasonEntry, current, trackedById)
                    } else {
                        current.watchedEpisodeCount
                    }
                    val isTrackedInVault = current.inWatchlist || (
                        isMergedContinuous && activeSeasonEntry?.subEntries.orEmpty().any { seg ->
                            trackedById[seg.id]?.inWatchlist == true
                        }
                        )
                    val canInc = !isTrackedInVault || total == null || total <= 0 || effectiveWatched < total
                    val canDec = isTrackedInVault && effectiveWatched > 0
                    val isCompleted = isTrackedInVault && (
                        current.watchStatusEnum == WatchStatus.COMPLETED ||
                            ProgressRules.shouldAutoComplete(
                                activeSeasonEntry?.status ?: current.status,
                                total,
                                effectiveWatched
                            )
                        )

                    val isFranchiseMergedAsOne = seasonEntries.size == 1 && (
                        seasonEntries.first().subEntries.size > 1 || rawMainSeasonNodeCount > 1
                        )
                    val canToggleSeasonMerge = rawMainSeasonNodeCount > 1 ||
                        seasonEntries.size > 1 ||
                        seasonEntries.any { it.subEntries.size > 1 }
                    val showSeasonsSelector = seasonEntries.size > 1 ||
                        isFranchiseMergedAsOne ||
                        (seasonEntries.isNotEmpty() && moviesAndOvas.isNotEmpty())

                    AppCard(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.md)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (showSeasonsSelector) {
                                val distinctSeasonCount = seasonEntries.map { it.seasonNumber }.distinct().size
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.testTag("detail_seasons_section")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            DetailCardLabel("SEASONS")
                                            if (activeSeasonEntry != null) {
                                                val yearText = activeSeasonEntry.seasonYear?.let { " • $it" }.orEmpty()
                                                val totalText = if (distinctSeasonCount == 1) {
                                                    if (activeSeasonEntry.subEntries.size > 1) {
                                                        "1 Continuous Season (${activeSeasonEntry.subEntries.size} Parts)"
                                                    } else {
                                                        "1 Season"
                                                    }
                                                } else {
                                                    "$distinctSeasonCount Seasons"
                                                }
                                                Text(
                                                    text = "${activeSeasonEntry.displayLabel} • $totalText$yearText",
                                                    style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.sp),
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }
                                        if (canToggleSeasonMerge) {
                                            val toggleLabel = if (isFranchiseMergedAsOne) {
                                                "Split Seasons"
                                            } else {
                                                "Merge as 1 Season"
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = colors.elevatedSurface.copy(alpha = 0.9f),
                                                border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.45f)),
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .clip(CircleShape)
                                                    .pointerHoverIcon(PointerIcon.Hand)
                                                    .clickable {
                                                        viewModel.toggleFranchiseSeasonOverride(
                                                            franchiseId = franchiseId,
                                                            currentlyMergedAsOne = isFranchiseMergedAsOne
                                                        )
                                                    }
                                                    .testTag("detail_season_override_toggle")
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = toggleLabel,
                                                        style = MicroUpperLabel.copy(
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.8.sp
                                                        ),
                                                        color = colors.accentPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(seasonEntries, key = { it.id }) { s ->
                                            val isSelected = s.isCurrent || s.containsAnimeId(current.id)
                                            val isUpcoming = s.status.equals("NOT_YET_RELEASED", ignoreCase = true)
                                            val pillBrush = if (isSelected) {
                                                if (colors.isDark) Brush.linearGradient(listOf(ChampagneGoldSoft, ChampagneGold))
                                                else Brush.linearGradient(listOf(colors.accentPrimary, colors.accentSecondary))
                                            } else {
                                                Brush.linearGradient(
                                                    listOf(
                                                        colors.elevatedSurface.copy(alpha = 0.78f),
                                                        colors.cardBackground.copy(alpha = 0.92f)
                                                    )
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .widthIn(min = 52.dp)
                                                    .height(48.dp)
                                                    .pointerHoverIcon(PointerIcon.Hand)
                                                    .clip(CircleShape)
                                                    .background(pillBrush)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) Color.Transparent else colors.borderSubtle,
                                                        shape = CircleShape
                                                    )
                                                    .clickable(enabled = !isSelected) { onSelectAnime(s.id) }
                                                    .padding(horizontal = 16.dp)
                                                    .testTag("detail_season_pill_${s.seasonNumber}_${s.partNumber ?: 0}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = s.shortLabel,
                                                        style = TabularNumMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                                        color = if (isSelected) {
                                                            if (colors.isDark) ChampagneOnGold else colors.onAccentPrimary
                                                        } else {
                                                            colors.textPrimary
                                                        }
                                                    )
                                                    if (s.isMergedContinuousRun && s.episodes != null && s.episodes > 0) {
                                                        Text(
                                                            text = "• ${s.episodes} EPS",
                                                            style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 0.8.sp),
                                                            color = if (isSelected) {
                                                                if (colors.isDark) ChampagneOnGold.copy(alpha = 0.85f) else colors.onAccentPrimary.copy(alpha = 0.85f)
                                                            } else {
                                                                colors.textSecondary
                                                            }
                                                        )
                                                    }
                                                    if (isUpcoming && !isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(colors.warning)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (moviesAndOvas.isNotEmpty()) {
                                if (showSeasonsSelector) {
                                    HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.6f), thickness = 1.dp)
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.testTag("detail_movies_ovas_section")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DetailCardLabel("MOVIES & OVAs")
                                        Text(
                                            text = "${moviesAndOvas.size} ${if (moviesAndOvas.size == 1) "ITEM" else "ITEMS"}",
                                            style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.sp),
                                            color = colors.textSecondary
                                        )
                                    }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(moviesAndOvas, key = { it.id }) { m ->
                                            val isSelected = m.isCurrent || m.id == current.id
                                            val mEntity = if (m.id == current.id) current else trackedById[m.id]
                                            val mWatched = mEntity?.watchedEpisodeCount ?: 0
                                            val mTotal = (mEntity?.totalEpisodes?.takeIf { it > 0 } ?: m.episodes).coerceAtLeast(1)
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) {
                                                    colors.accentPrimary.copy(alpha = 0.16f)
                                                } else {
                                                    colors.elevatedSurface.copy(alpha = 0.85f)
                                                },
                                                border = BorderStroke(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) colors.accentPrimary else colors.borderSubtle
                                                ),
                                                modifier = Modifier
                                                    .widthIn(min = 160.dp, max = 230.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .pointerHoverIcon(PointerIcon.Hand)
                                                    .clickable(enabled = !isSelected) { onSelectAnime(m.id) }
                                                    .testTag("detail_movie_ova_pill_${m.id}")
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (isSelected) colors.accentPrimary else colors.cardBackground
                                                        ) {
                                                            Text(
                                                                text = m.formatBadge,
                                                                style = MicroUpperLabel.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp),
                                                                color = if (isSelected) {
                                                                    if (colors.isDark) ChampagneOnGold else colors.onAccentPrimary
                                                                } else {
                                                                    colors.accentPrimary
                                                                },
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = "${m.releaseYear ?: "TBA"} • EP $mWatched / $mTotal",
                                                            style = TabularNumSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                                            color = if (isSelected) colors.accentPrimary else colors.textSecondary
                                                        )
                                                    }
                                                    Text(
                                                        text = m.title,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = colors.textPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (showSeasonsSelector || moviesAndOvas.isNotEmpty()) {
                                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                            }

                            val isMovieOrOvaSpecial = current.isMovie ||
                                current.format.equals("OVA", ignoreCase = true) ||
                                current.format.equals("SPECIAL", ignoreCase = true)

                            DetailCardLabel(if (current.isMovie) "MOVIE PROGRESS" else "EPISODE PROGRESS")

                            if (!isTrackedInVault) {
                                AppPrimaryButton(
                                    text = "Add to My List",
                                    leadingIcon = Icons.Filled.Add,
                                    onClick = { viewModel.addToWatchlist(current, WatchStatus.WATCHING) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("detail_add_to_list")
                                )
                            } else {
                                DetailEpisodeStepperControl(
                                    animeId = current.id,
                                    watchedEpisodeCount = effectiveWatched,
                                    totalEpisodes = total ?: if (isMovieOrOvaSpecial) 1 else null,
                                    subtitle = when {
                                        current.isMovie -> "MOVIE • TAP TO MARK WATCHED"
                                        isMergedContinuous && activeSeasonEntry != null ->
                                            "${activeSeasonEntry.displayLabel.uppercase()} • CONTINUOUS EPISODE COUNTER"
                                        activeSeasonEntry != null && seasonEntries.size > 1 ->
                                            "${activeSeasonEntry.displayLabel.uppercase()} • TAP NUMBER TO EDIT"
                                        else -> "EPISODES WATCHED • TAP NUMBER TO EDIT"
                                    },
                                    canDecrement = canDec,
                                    canIncrement = canInc,
                                    onDecrement = {
                                        if (isMergedContinuous && activeSeasonEntry != null) {
                                            viewModel.stepMergedContinuousEpisode(activeSeasonEntry, effectiveWatched, -1)
                                        } else {
                                            viewModel.decrementEpisode(current.id)
                                        }
                                    },
                                    onIncrement = {
                                        if (isMergedContinuous && activeSeasonEntry != null) {
                                            viewModel.stepMergedContinuousEpisode(activeSeasonEntry, effectiveWatched, 1)
                                        } else {
                                            viewModel.incrementEpisode(current.id)
                                        }
                                    },
                                    onCommitCustomEpisode = { targetEp ->
                                        if (isMergedContinuous && activeSeasonEntry != null) {
                                            viewModel.commitMergedContinuousEpisodeProgress(activeSeasonEntry, targetEp).map { }
                                        } else {
                                            viewModel.commitEpisodeProgress(current.id, targetEp).map { }
                                        }
                                    }
                                )

                                // Soft Capsule Edit / Star Rating & Review Studio button
                                Surface(
                                    shape = CircleShape,
                                    color = colors.accentPrimary.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(CircleShape)
                                        .clickable { showTrackingEditor = true }
                                        .testTag("detail_edit_tracking")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = null,
                                            tint = colors.accentPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "RATE IN STARS, REVIEW & TRACK",
                                            style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 1.4.sp),
                                            color = colors.accentPrimary
                                        )
                                    }
                                }

                                val pct = if (total != null && total > 0) {
                                    (effectiveWatched.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                } else if (effectiveWatched > 0) 0.5f else 0.05f

                                AppProgressBar(
                                    progress = pct,
                                    isCompleted = isCompleted,
                                    heightDp = 5
                                )
                            }

                            // 3-column stats (AIRED | NEXT | AIRS IN)
                            HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val airedText = when {
                                    isFinished && total != null && total > 0 -> total.toString()
                                    nextEpNum != null && nextEpNum > 1 -> (nextEpNum - 1).toString()
                                    else -> maxOf(current.currentEpisode, current.watchedEpisodeCount).toString()
                                }
                                val nextText = when {
                                    isCompleted || isFinished -> "✓"
                                    nextEpNum != null -> nextEpNum.toString()
                                    else -> "—"
                                }
                                val countdownText = when {
                                    isCompleted || isFinished -> "Done"
                                    nextTime != null && nextTime > nowUnix -> formatCompactCountdown(nextTime - nowUnix)
                                    else -> "TBD"
                                }

                                DetailStatColumn("AIRED", airedText, colors.textPrimary, Modifier.weight(1f))
                                DetailStatColumn("NEXT", nextText, colors.highlight, Modifier.weight(1f))
                                DetailStatColumn("AIRS IN", countdownText, colors.highlight, Modifier.weight(1f))
                            }

                            if (current.inWatchlist) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (profile != null) "✓ SYNCED TO CLOUD" else "✓ SAVED IN LOCAL VAULT",
                                        style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.sp),
                                        color = colors.success
                                    )
                                    Text(
                                        text = current.formatLabel(),
                                        style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.sp),
                                        color = colors.textMuted
                                    )
                                }
                            }

                            if (profile != null) {
                                AppSecondaryButton(
                                    text = "Add to Collection",
                                    leadingIcon = Icons.Filled.CollectionsBookmark,
                                    onClick = { showCollectionPicker = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (current.inHome) {
                                OutlinedButton(
                                    onClick = { showRemoveFromHomeConfirm = true },
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.55f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.accentPrimary.copy(alpha = 0.08f),
                                        contentColor = colors.accentPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_remove_from_home")
                                ) {
                                    Icon(Icons.Filled.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Remove from Home (Keep in Library)", fontWeight = FontWeight.SemiBold)
                                }
                            } else if (current.inWatchlist) {
                                OutlinedButton(
                                    onClick = { viewModel.addToHome(current) },
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.55f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.accentPrimary.copy(alpha = 0.08f),
                                        contentColor = colors.accentPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_add_to_home")
                                ) {
                                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add to Home Tracking", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (current.inWatchlist || current.hasActiveUserState) {
                                OutlinedButton(
                                    onClick = { viewModel.removeFromWatchlist(current.id) },
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.55f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.error.copy(alpha = 0.08f),
                                        contentColor = colors.error
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_remove_from_list")
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Remove from Library", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            val myStars = StarRatingUtils.score100ToStars(current.userScore)
                            if (myStars > 0f || !current.userNotes.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = colors.elevatedSurface.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { showTrackingEditor = true }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("YOUR RATING & REVIEW", style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 1.4.sp), color = colors.accentPrimary)
                                            if (myStars > 0f) {
                                                StarRatingRow(stars = myStars, starSize = 14.dp, showNumeric = true)
                                            }
                                        }
                                        if (!current.userNotes.isNullOrBlank()) {
                                            Text(
                                                text = "“${current.userNotes}”",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            if (!ratingFeedback.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.success.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, colors.success.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("detail_rating_feedback_banner")
                                ) {
                                    Text(
                                        text = "✓ $ratingFeedback",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.success,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Genres & Metadata
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        val metaLine = buildList {
                            add(current.formatLabel())
                            current.totalEpisodes?.let { add("$it Episodes") }
                            listOfNotNull(current.season, current.seasonYear?.toString())
                                .takeIf { it.isNotEmpty() }
                                ?.let { add(it.joinToString(" ")) }
                            addAll(studiosList)
                        }.joinToString(" • ")

                        if (metaLine.isNotBlank()) {
                            Text(
                                text = metaLine,
                                style = TabularNumSmall,
                                color = colors.textSecondary
                            )
                        }

                        val genres = current.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        if (genres.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                genres.forEach { genre ->
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.elevatedSurface.copy(alpha = 0.55f)
                                    ) {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.highlight,
                                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Synopsis
                if (!current.synopsis.isNullOrBlank()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.md)
                                .animateContentSize(),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                        ) {
                            Text(
                                text = "Synopsis",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = current.synopsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                maxLines = if (synopsisExpanded) Int.MAX_VALUE else 5,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(onClick = { synopsisExpanded = !synopsisExpanded }) {
                                Text(if (synopsisExpanded) "Show Less" else "Read More", color = colors.accentPrimary)
                            }
                            HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                        }
                    }
                }

                // Characters
                val characters = richDetail?.characters.orEmpty()
                if (characters.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                            Text(
                                text = "Characters",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = AppSpacing.md)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = AppSpacing.md),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                items(characters, key = { it.id }) { ch ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(80.dp)
                                    ) {
                                        ResilientPosterImage(
                                            imageUrl = ch.imageUrl,
                                            contentDescription = ch.name,
                                            shape = CircleShape,
                                            modifier = Modifier.size(68.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = ch.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Other Parts of Same Anime (Movies, Spin-Offs, Side Stories, OVAs, Specials, Summaries)
                if (otherParts.isNotEmpty()) {
                    item {
                        val availableCategories = remember(otherParts) {
                            val present = otherParts.map { AnimePartCategory.classify(it.relationType, it.format) }.toSet()
                            buildList {
                                add(AnimePartCategory.ALL)
                                AnimePartCategory.entries
                                    .filter { it != AnimePartCategory.ALL && present.contains(it) }
                                    .forEach { add(it) }
                            }
                        }
                        var selectedCategory by rememberSaveable(current.id) {
                            mutableStateOf(AnimePartCategory.ALL)
                        }
                        val displayedParts = remember(otherParts, selectedCategory) {
                            if (selectedCategory == AnimePartCategory.ALL) otherParts
                            else otherParts.filter { AnimePartCategory.classify(it.relationType, it.format) == selectedCategory }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                            modifier = Modifier.testTag("detail_other_parts_section")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Other Parts & Spin-Offs",
                                    fontFamily = CormorantSerifFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${otherParts.size} PARTS",
                                    style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                                    color = colors.accentPrimary
                                )
                            }

                            if (availableCategories.size > 2) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = AppSpacing.md),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(availableCategories, key = { it.name }) { cat ->
                                        val isSelected = selectedCategory == cat
                                        val count = if (cat == AnimePartCategory.ALL) otherParts.size
                                        else otherParts.count { AnimePartCategory.classify(it.relationType, it.format) == cat }
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) colors.accentPrimary.copy(alpha = 0.20f) else colors.elevatedSurface.copy(alpha = 0.58f),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) colors.accentPrimary.copy(alpha = 0.72f) else colors.borderSubtle
                                            ),
                                            modifier = Modifier
                                                .heightIn(min = 40.dp)
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clip(CircleShape)
                                                .clickable { selectedCategory = cat }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "${cat.filterLabel} ($count)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (isSelected) colors.highlight else colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = AppSpacing.md),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                items(displayedParts, key = { "${it.id}_${it.relationType}" }) { part ->
                                    val partBadge = remember(part) {
                                        AnimePartCategory.badgeLabel(part.relationType, part.format)
                                    }
                                    val partStars = remember(part.averageScore) {
                                        StarRatingUtils.score100ToStars(part.averageScore)
                                    }
                                    val subParts = remember(part) {
                                        buildList {
                                            part.format?.takeIf { it.isNotBlank() }?.let { add(it.replace('_', ' ')) }
                                            part.episodes?.takeIf { it > 0 }?.let { add("$it ep") }
                                            part.seasonYear?.let { add(it.toString()) }
                                        }.joinToString(" • ")
                                    }
                                    AppCard(
                                        shape = RoundedCornerShape(20.dp),
                                        onClick = { onSelectAnime(part.id) },
                                        modifier = Modifier.width(142.dp)
                                    ) {
                                        Column {
                                            Box {
                                                ResilientPosterImage(
                                                    imageUrl = part.coverImage,
                                                    contentDescription = part.title,
                                                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(172.dp)
                                                )
                                                if (partStars > 0f) {
                                                    RatingBadge(
                                                        scoreText = StarRatingUtils.formatStars(partStars),
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                    )
                                                }
                                            }
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = partBadge,
                                                    style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 0.8.sp),
                                                    color = colors.accentPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = part.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (subParts.isNotBlank()) {
                                                    Text(
                                                        text = subParts,
                                                        style = TabularNumSmall.copy(fontSize = 10.sp),
                                                        color = colors.textMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
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

                // Recommendations
                val recommendations = richDetail?.recommendations.orEmpty()
                if (recommendations.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                            Text(
                                text = "Recommended Next",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = AppSpacing.md)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = AppSpacing.md),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                items(recommendations, key = { it.id }) { rec ->
                                    AppCard(
                                        shape = RoundedCornerShape(20.dp),
                                        onClick = { onSelectAnime(rec.id) },
                                        modifier = Modifier.width(126.dp)
                                    ) {
                                        Column {
                                            ResilientPosterImage(
                                                imageUrl = rec.coverImage,
                                                contentDescription = rec.title,
                                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                                modifier = Modifier.fillMaxWidth().height(158.dp)
                                            )
                                            Column(modifier = Modifier.padding(AppSpacing.xs)) {
                                                Text(
                                                    text = rec.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colors.textPrimary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Editorial Big-Card 2: COMMUNITY REVIEWS (Rated & Noted Only)
                item {
                    AppCard(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.md)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DetailCardLabel("COMMUNITY REVIEWS")

                            // 5-Column Sentiment Tally Strip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SentimentCategory.entries.forEach { cat ->
                                    val count = ratedAndNotedReviews.count { SentimentCategory.fromRating10(it.rating) == cat }
                                    val catColor = cat.badgeColor()
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colors.elevatedSurface.copy(alpha = 0.50f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 3.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                fontFamily = CormorantSerifFamily,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = catColor,
                                                lineHeight = 20.sp
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = cat.shortLabel.uppercase(),
                                                style = MicroUpperLabel.copy(fontSize = 7.sp, letterSpacing = 0.6.sp),
                                                color = catColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.5f), thickness = 1.dp)

                            if (ratedAndNotedReviews.isEmpty()) {
                                Text(
                                    text = if (profile == null) "Sign in to view rated & noted community reviews for this anime."
                                    else "No rated & noted community reviews for this anime yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            } else {
                                ratedAndNotedReviews.forEach { rev ->
                                    EditorialCommunityNoteCard(
                                        review = rev,
                                        onClick = { onSelectAnime(rev.animeId) },
                                        showAnimeHeader = false,
                                        onToggleLike = { viewModel.toggleReviewLike(rev) },
                                        onEdit = if (profile != null && rev.userId == profile?.id) {
                                            { showTrackingEditor = true }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }

                // Editorial Big-Card 3: ANIME DISCUSSION (Comments, @Mentions & Target-User Reply Threads)
                item(key = "anime_discussion_section_item") {
                    AnimeDiscussionSection(
                        animeId = current.id,
                        animeTitle = resolvedTitle,
                        animeCover = current.coverImage,
                        viewModel = viewModel,
                        highlightTarget = activeJumpForThisAnime,
                        modifier = Modifier.padding(horizontal = AppSpacing.md)
                    )
                }
            }
        }
    }

    if (showTrackingEditor) {
        TrackingEditorDialog(
            anime = current,
            onDismiss = { showTrackingEditor = false },
            onSave = { status, progress, seasonNumber, score, notes, fav ->
                viewModel.updateTracking(
                    animeId = current.id,
                    status = status,
                    episodeProgress = progress,
                    scoreTenths = score,
                    notes = notes,
                    isFavorite = fav,
                    seasonNumber = seasonNumber
                )
                showTrackingEditor = false
            },
            onRemove = {
                viewModel.removeFromWatchlist(current.id)
                showTrackingEditor = false
            },
            shareReviewsWithCommunity = if (profile != null) {
                privacy.reviewsVisibility == VisibilityLevel.COMMUNITY
            } else null,
            onToggleShareReviews = if (profile != null) {
                { share ->
                    viewModel.updatePrivacySettings(
                        privacy.copy(reviewsVisibility = if (share) VisibilityLevel.COMMUNITY else VisibilityLevel.PRIVATE)
                    )
                }
            } else null
        )
    }

    if (showCollectionPicker) {
        AlertDialog(
            onDismissRequest = { showCollectionPicker = false },
            shape = RoundedCornerShape(AppRadius.xl),
            containerColor = colors.backgroundSecondary,
            title = { Text("Add to Collection", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    if (collections.isEmpty()) {
                        Text("Create a collection from the Library tab first.", color = colors.textSecondary)
                    } else {
                        collections.forEach { col ->
                            val alreadyIn = col.animeIds.contains(current.id)
                            AppSecondaryButton(
                                text = if (alreadyIn) "${col.name} (Added)" else col.name,
                                onClick = {
                                    if (!alreadyIn) {
                                        viewModel.addAnimeToCollection(col.id, current)
                                    }
                                    showCollectionPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCollectionPicker = false }) { Text("Close", color = colors.accentPrimary) }
            }
        )
    }

    if (showRemoveFromHomeConfirm) {
        RemoveFromHomeConfirmDialog(
            animeTitle = resolvedTitle,
            onDismiss = { showRemoveFromHomeConfirm = false },
            onConfirm = {
                viewModel.removeFromHome(current.id)
                showRemoveFromHomeConfirm = false
            }
        )
    }
}

@Composable
internal fun DetailEpisodeStepperControl(
    animeId: Int,
    watchedEpisodeCount: Int,
    totalEpisodes: Int?,
    subtitle: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onCommitCustomEpisode: suspend (Int) -> Result<Unit>
) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        InlineEpisodeNumberStepper(
            animeId = animeId,
            watchedEpisodeCount = watchedEpisodeCount,
            totalEpisodes = totalEpisodes,
            canDecrement = canDecrement,
            canIncrement = canIncrement,
            compact = false,
            onDecrement = onDecrement,
            onIncrement = onIncrement,
            onCommitCustomEpisode = onCommitCustomEpisode,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("detail_episode_center")
        )
        Text(
            text = subtitle,
            style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 1.6.sp),
            color = colors.textSecondary
        )
    }
}

@Composable
private fun DetailCardLabel(text: String) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, colors.accentPrimary)))
        )
        Text(
            text = text,
            style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 2.6.sp),
            color = colors.accentPrimary
        )
    }
}

@Composable
private fun DetailStatColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MicroUpperLabel.copy(fontSize = 7.5.sp, letterSpacing = 1.8.sp),
            color = colors.textMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = CormorantSerifFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
