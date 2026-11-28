package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.model.DailyAnimePick
import com.example.model.TopAnimeRankedItem
import com.example.model.TopAnimeSource
import com.example.model.TopAnimeTimeframe
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AppCard
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.ResilientPosterImage

// ============================================================================
// RANDOM ANIME OF THE DAY SECTION
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomAnimeOfTheDaySection(
    pick: DailyAnimePick?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onSelectAnime: (Int) -> Unit,
    selectedCategory: RandomSectionCategory = RandomSectionCategory.DAILY_PICK,
    onSelectCategory: (RandomSectionCategory) -> Unit = {},
    myShuffleItem: com.example.service.MyShuffleAnimeItem? = null,
    isMyShuffleLoading: Boolean = false,
    myShuffleError: String? = null,
    myShuffleSequence: Int = 0,
    onShuffleAgain: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    var detailsExpanded by rememberSaveable(pick?.selectionDate, pick?.anilistId) { mutableStateOf(false) }
    var trailerPlaying by rememberSaveable(pick?.selectionDate, pick?.anilistId) { mutableStateOf(false) }
    var trailerEmbedFailed by rememberSaveable(pick?.selectionDate, pick?.anilistId) { mutableStateOf(false) }

    // Automatically load or silently auto-retry when pick is not yet available (no manual Refresh button)
    androidx.compose.runtime.LaunchedEffect(pick, isLoading, error) {
        if (pick == null && !isLoading) {
            if (!error.isNullOrBlank()) {
                kotlinx.coroutines.delay(2500L)
            }
            onRetry()
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
            .padding(bottom = 112.dp)
            .testTag("community_random_section"),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        // Two categories inside Random section: 1. Daily Pick | 2. My Shuffle
        RandomSectionCategorySwitcher(
            selectedCategory = selectedCategory,
            onSelectCategory = onSelectCategory
        )

        if (selectedCategory == RandomSectionCategory.MY_SHUFFLE) {
            MyShuffleSectionContent(
                item = myShuffleItem,
                isLoading = isMyShuffleLoading,
                error = myShuffleError,
                shuffleSequence = myShuffleSequence,
                onShuffleAgain = onShuffleAgain,
                onSelectAnime = onSelectAnime
            )
        } else when {
            pick == null -> {
                RandomAnimeSkeletonCard()
            }
            else -> {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = colors.elevatedSurface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.34f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Subtle top banner backdrop if available
                        if (!pick.bannerImage.isNullOrBlank()) {
                            AsyncImage(
                                model = pick.bannerImage,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alpha = 0.2f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(176.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(176.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                colors.elevatedSurface.copy(alpha = 0.96f)
                                            )
                                        )
                                    )
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Header Row: Badge + Heading + Open Page
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = colors.accentPrimary.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.55f))
                                        ) {
                                            Text(
                                                text = "★ TODAY'S PICK",
                                                style = MicroUpperLabel.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = colors.highlight,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (pick.selectionDate.isNotBlank()) {
                                            Text(
                                                text = pick.selectionDateLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Random Anime of the Day",
                                        fontFamily = CormorantSerifFamily,
                                        fontSize = 27.sp,
                                        lineHeight = 31.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                }

                                if (pick.anilistId > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.accentPrimary.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.48f)),
                                        modifier = Modifier
                                            .heightIn(min = 38.dp)
                                            .clip(CircleShape)
                                            .clickable { onSelectAnime(pick.anilistId) }
                                            .testTag("random_anime_open_detail_button")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "Open Page",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = colors.highlight
                                            )
                                        }
                                    }
                                }
                            }

                            // Main Content: Poster + Core Metadata
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                ResilientPosterImage(
                                    imageUrl = pick.coverImage,
                                    contentDescription = pick.title,
                                    modifier = Modifier
                                        .width(116.dp)
                                        .height(168.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                        .clickable(enabled = pick.anilistId > 0) { onSelectAnime(pick.anilistId) }
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = pick.title,
                                        fontFamily = CormorantSerifFamily,
                                        fontSize = 22.sp,
                                        lineHeight = 25.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (pick.secondaryTitle.isNotBlank()) {
                                        Text(
                                            text = pick.secondaryTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = colors.accentPrimary.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.45f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = colors.highlight,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = pick.rating10Text,
                                                    style = TabularNumSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = colors.highlight
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = colors.backgroundSecondary,
                                            border = BorderStroke(1.dp, colors.borderSubtle)
                                        ) {
                                            Text(
                                                text = pick.formatLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = colors.textSecondary,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }

                                        val statusTint = when (pick.rawStatus?.uppercase()) {
                                            "RELEASING" -> colors.success
                                            "NOT_YET_RELEASED" -> colors.accentPrimary
                                            "CANCELLED" -> colors.error
                                            else -> colors.textSecondary
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = statusTint.copy(alpha = if (colors.isDark) 0.14f else 0.11f),
                                            border = BorderStroke(1.dp, statusTint.copy(alpha = if (colors.isDark) 0.35f else 0.38f))
                                        ) {
                                            Text(
                                                text = pick.statusLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = statusTint,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (pick.genres.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            pick.genres.take(4).forEach { genre ->
                                                Surface(
                                                    shape = CircleShape,
                                                    color = colors.accentPrimary.copy(alpha = if (colors.isDark) 0.13f else 0.10f),
                                                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = if (colors.isDark) 0.28f else 0.30f))
                                                ) {
                                                    Text(
                                                        text = genre,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.highlight,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Description
                            val descText = pick.description.ifBlank { "No description available." }
                            Text(
                                text = descText,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = colors.textSecondary,
                                maxLines = if (detailsExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Expandable View Details Button
                            Surface(
                                shape = CircleShape,
                                color = colors.backgroundSecondary,
                                border = BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .clip(CircleShape)
                                    .clickable { detailsExpanded = !detailsExpanded }
                                    .testTag("random_anime_toggle_details_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = if (detailsExpanded) "Hide Details" else "View Details",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary
                                    )
                                    Icon(
                                        imageVector = if (detailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        tint = colors.highlight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Expandable Metadata Grid
                            AnimatedVisibility(visible = detailsExpanded) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = colors.backgroundSecondary,
                                    border = BorderStroke(1.dp, colors.borderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val detailPairs = listOf(
                                        "Total Episodes" to pick.episodesLabel,
                                        "Airing Status" to pick.statusLabel,
                                        "Release Date" to pick.startDateLabel,
                                        "Ending Date" to pick.endDateLabel,
                                        "Season" to pick.seasonLabel,
                                        "Release Year" to pick.releaseYearLabel,
                                        "Episode Duration" to pick.durationLabel,
                                        "Source Material" to pick.sourceLabel,
                                        "Animation Studio" to pick.studiosLabel,
                                        "Country" to pick.countryLabel
                                    )
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        detailPairs.chunked(2).forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                rowItems.forEach { (label, value) ->
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = label.uppercase(),
                                                            style = MicroUpperLabel.copy(fontSize = 10.sp),
                                                            color = colors.textMuted
                                                        )
                                                        Text(
                                                            text = value,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = colors.textPrimary
                                                        )
                                                    }
                                                }
                                                if (rowItems.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = colors.borderSubtle)

                            // Trailer Section (Lazy — Never auto-loads YouTube iframe!)
                            val trailerId = pick.validTrailerId
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "OFFICIAL TRAILER",
                                        style = MicroUpperLabel.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textMuted
                                    )
                                    if (trailerId != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    val watchUri = Uri.parse("https://www.youtube.com/watch?v=$trailerId")
                                                    runCatching {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, watchUri))
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("random_anime_watch_youtube_link")
                                        ) {
                                            Text(
                                                text = "Watch on YouTube",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = colors.highlight
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Watch on YouTube",
                                                tint = colors.highlight,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                when {
                                    trailerId == null -> {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = colors.backgroundSecondary,
                                            border = BorderStroke(1.dp, colors.borderSubtle),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Trailer preview is currently unavailable for this title.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                    !trailerPlaying -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(18.dp))
                                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
                                                .background(Color(0xFF09090F))
                                                .clickable { trailerPlaying = true }
                                                .testTag("random_anime_play_trailer_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = "https://i.ytimg.com/vi/$trailerId/hqdefault.jpg",
                                                contentDescription = "${pick.title} official trailer thumbnail",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.42f))
                                            )
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = colors.accentPrimary,
                                                    modifier = Modifier.size(58.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Filled.PlayArrow,
                                                            contentDescription = "Play Trailer",
                                                            tint = colors.onAccentPrimary,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xCC0D0E16)
                                                ) {
                                                    Text(
                                                        text = "Play Trailer",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    trailerEmbedFailed -> {
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = colors.backgroundSecondary,
                                            border = BorderStroke(1.dp, colors.borderSubtle),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(18.dp)
                                            ) {
                                                Text(
                                                    text = "In-app trailer playback is unavailable on this device.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.textSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                                AppPrimaryButton(
                                                    text = "Watch on YouTube",
                                                    onClick = {
                                                        val watchUri = Uri.parse("https://www.youtube.com/watch?v=$trailerId")
                                                        runCatching {
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, watchUri))
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("random_anime_fallback_youtube_button")
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        YouTubeLazyTrailerPlayer(
                                            videoId = trailerId,
                                            onRenderError = { trailerEmbedFailed = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(18.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                                                .testTag("random_anime_youtube_player")
                                        )
                                        Text(
                                            text = "If embedding is restricted by the video owner, tap Watch on YouTube above.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeLazyTrailerPlayer(
    videoId: String,
    onRenderError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeVideoId = videoId.trim()
    val context = LocalContext.current
    val embedOrigin = "https://${context.packageName.lowercase().replace('_', '-')}.app"
    val mainHandler = Handler(Looper.getMainLooper())

    // Official YouTube IFrame Player API hosted under a valid HTTPS app origin so YouTube
    // always receives a valid HTTP Referer (prevents YouTube Error 153 in Android WebView).
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
          <meta name="referrer" content="strict-origin-when-cross-origin">
          <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
            #player, iframe { width: 100%; height: 100%; border: 0; display: block; }
          </style>
        </head>
        <body>
          <div id="player"></div>
          <script>
            var videoId = "$safeVideoId";
            var originUrl = "$embedOrigin";
            var fallbackLoaded = false;

            function loadDirectIframe() {
              if (fallbackLoaded) return;
              fallbackLoaded = true;
              var wrap = document.getElementById('player');
              if (!wrap) return;
              wrap.innerHTML = '<iframe ' +
                'src="https://www.youtube.com/embed/' + videoId + '?autoplay=1&playsinline=1&rel=0&modestbranding=1&enablejsapi=1&origin=' + encodeURIComponent(originUrl) + '" ' +
                'referrerpolicy="strict-origin-when-cross-origin" ' +
                'allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen" ' +
                'allowfullscreen></iframe>';
            }

            window.onYouTubeIframeAPIReady = function() {
              try {
                new YT.Player('player', {
                  width: '100%',
                  height: '100%',
                  videoId: videoId,
                  host: 'https://www.youtube.com',
                  playerVars: {
                    autoplay: 1,
                    playsinline: 1,
                    rel: 0,
                    modestbranding: 1,
                    enablejsapi: 1,
                    origin: originUrl,
                    widget_referrer: originUrl
                  },
                  events: {
                    onReady: function(ev) {
                      try { ev.target.playVideo(); } catch (e) {}
                    },
                    onError: function(ev) {
                      if (window.AndroidYT && typeof window.AndroidYT.onPlayerError === 'function') {
                        window.AndroidYT.onPlayerError(Number(ev && ev.data ? ev.data : -1));
                      }
                    }
                  }
                });
              } catch (e) {
                loadDirectIframe();
              }
            };

            var tag = document.createElement('script');
            tag.src = 'https://www.youtube.com/iframe_api';
            tag.onerror = loadDirectIframe;
            document.head.appendChild(tag);

            setTimeout(function() {
              if (!window.YT || !window.YT.Player) {
                loadDirectIframe();
              }
            }, 3500);
          </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.allowContentAccess = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                // Strip "; wv" token so YouTube mobile player treats WebView like standard Chrome Mobile
                settings.userAgentString = settings.userAgentString.replace("; wv", "")

                runCatching {
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                }

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onPlayerError(errorCode: Int) {
                            // 100 = not found, 101/150/152/153 = owner/embed restricted
                            if (errorCode in setOf(2, 5, 100, 101, 150, 152, 153)) {
                                mainHandler.post { onRenderError() }
                            }
                        }
                    },
                    "AndroidYT"
                )

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url ?: return false
                        val host = uri.host?.lowercase().orEmpty()
                        val isAllowedInFrame =
                            host.endsWith("youtube.com") ||
                            host.endsWith("youtube-nocookie.com") ||
                            host.endsWith("ytimg.com") ||
                            host.endsWith("googlevideo.com") ||
                            host.endsWith("gstatic.com") ||
                            host.endsWith("google.com") ||
                            host == context.packageName.lowercase().replace('_', '-') + ".app"

                        // If user taps "Watch on YouTube" logo inside player, open YouTube app/browser
                        if (request.isForMainFrame && (host.endsWith("youtube.com") || host == "youtu.be") && !uri.path.orEmpty().startsWith("/embed")) {
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                            return true
                        }
                        if (!isAllowedInFrame && request.isForMainFrame) {
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                            return true
                        }
                        return false
                    }

                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        runCatching {
                            view?.stopLoading()
                            view?.destroy()
                        }
                        mainHandler.post { onRenderError() }
                        return true
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    runCatching {
                        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                    }
                }
                loadDataWithBaseURL("$embedOrigin/", html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { webView ->
            runCatching {
                webView.removeJavascriptInterface("AndroidYT")
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.onPause()
                webView.removeAllViews()
                webView.destroy()
            }
        },
        modifier = modifier
    )
}

@Composable
private fun RandomAnimeSkeletonCard() {
    val colors = AppTheme.colors
    val skelColor = colors.textMuted.copy(alpha = if (colors.isDark) 0.16f else 0.14f)
    val skelSoft = colors.textMuted.copy(alpha = if (colors.isDark) 0.11f else 0.09f)
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(22.dp)
                    .clip(CircleShape)
                    .background(skelColor)
            )
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(skelColor)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .width(116.dp)
                        .height(168.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(skelColor)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(22.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(skelColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(skelSoft)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(skelSoft)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(skelSoft)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(skelSoft)
            )
        }
    }
}

// ============================================================================
// TOP ANIME SECTION (Screenshot-Matched Layout — Community & All)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopAnimeCommunitySection(
    source: TopAnimeSource,
    limit: Int,
    timeframe: TopAnimeTimeframe,
    items: List<TopAnimeRankedItem>,
    isLoading: Boolean,
    error: String?,
    onSelectSource: (TopAnimeSource) -> Unit,
    onSelectLimit: (Int) -> Unit,
    onSelectTimeframe: (TopAnimeTimeframe) -> Unit,
    onRetry: () -> Unit,
    onSelectAnime: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val pillBgInactive = colors.backgroundSecondary
    val pillBgActive = colors.accentPrimary.copy(alpha = if (colors.isDark) 0.22f else 0.14f)
    val pillTextInactive = colors.textSecondary
    val pillTextActive = colors.highlight
    val crownTint = colors.accentPrimary
    val skelStrong = colors.textMuted.copy(alpha = if (colors.isDark) 0.16f else 0.14f)
    val skelSoft = colors.textMuted.copy(alpha = if (colors.isDark) 0.11f else 0.09f)

    LazyColumn(
        modifier = modifier.testTag("community_top_section"),
        contentPadding = PaddingValues(
            start = AppSpacing.md,
            end = AppSpacing.md,
            top = AppSpacing.sm,
            bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "top_anime_header_card") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.cardBackground,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header: Crown + "Top Anime" on left, Community | All toggle on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            CrownVectorIcon(
                                tint = crownTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Top Anime",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 21.sp
                                ),
                                color = colors.textPrimary
                            )
                        }

                        SlidableTopSourceToggle(
                            source = source,
                            onSelectSource = onSelectSource
                        )
                    }

                    // Row 1: Top 10 | Top 25 | Top 50 | Top 100
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 25, 50, 100).forEach { lim ->
                            val active = limit == lim
                            Surface(
                                shape = CircleShape,
                                color = if (active) pillBgActive else pillBgInactive,
                                border = BorderStroke(
                                    1.dp,
                                    if (active) crownTint.copy(alpha = 0.55f) else colors.borderSubtle
                                ),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .clip(CircleShape)
                                    .clickable { onSelectLimit(lim) }
                                    .testTag("top_limit_$lim")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Top $lim",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (active) pillTextActive else pillTextInactive
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.borderSubtle)

                    // Row 2: Weekly | Monthly | Seasonal | Yearly | All Time
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TopAnimeTimeframe.entries.forEach { tf ->
                            val active = timeframe == tf
                            Surface(
                                shape = CircleShape,
                                color = if (active) pillBgActive else pillBgInactive,
                                border = BorderStroke(
                                    1.dp,
                                    if (active) crownTint.copy(alpha = 0.55f) else colors.borderSubtle
                                ),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .clip(CircleShape)
                                    .clickable { onSelectTimeframe(tf) }
                                    .testTag("top_timeframe_${tf.key}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = tf.label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (active) pillTextActive else pillTextInactive
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        when {
            isLoading -> {
                items(6, key = { "top_skel_$it" }) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardBackground,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(skelSoft)
                            )
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(skelStrong)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.75f)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(skelStrong)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.45f)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(skelSoft)
                                )
                            }
                        }
                    }
                }
            }
            !error.isNullOrBlank() -> {
                item(key = "top_error") {
                    AppCard(shape = RoundedCornerShape(22.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            AppPrimaryButton(
                                text = "Try Again",
                                onClick = onRetry,
                                modifier = Modifier.testTag("top_anime_retry_button")
                            )
                        }
                    }
                }
            }
            items.isEmpty() -> {
                item(key = "top_empty") {
                    AppCard(shape = RoundedCornerShape(22.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No rankings yet",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 23.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (source == TopAnimeSource.COMMUNITY) {
                                    "No community ratings match this timeframe yet. Try All Time or rate an anime!"
                                } else {
                                    "No titles matched this ranking filter right now."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            else -> {
                itemsIndexed(items, key = { idx, item -> "top_${source.key}_${item.animeId}_$idx" }) { index, item ->
                    val rank = index + 1
                    TopAnimeRankRowCard(
                        rank = rank,
                        item = item,
                        onClick = { onSelectAnime(item.animeId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopAnimeRankRowCard(
    rank: Int,
    item: TopAnimeRankedItem,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val (rankBg, rankText, rankBorder) = when (rank) {
        1 -> if (colors.isDark) {
            Triple(Color(0x3DFBBF24), Color(0xFFFDE68A), Color(0x8CFBBF24))
        } else {
            Triple(Color(0xFFFEF3C7), Color(0xFF92400E), Color(0xFFD97706))
        }
        2 -> if (colors.isDark) {
            Triple(Color(0x33E2E8F0), Color(0xFFF1F5F9), Color(0x80CBD5E1))
        } else {
            Triple(Color(0xFFE2E8F0), Color(0xFF334155), Color(0xFF94A3B8))
        }
        3 -> if (colors.isDark) {
            Triple(Color(0x33FB923C), Color(0xFFFDBA74), Color(0x7AFB923C))
        } else {
            Triple(Color(0xFFFFEDD5), Color(0xFF9A3412), Color(0xFFEA580C))
        }
        else -> Triple(colors.backgroundSecondary, colors.textSecondary, colors.borderSubtle)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("top_anime_row_${item.animeId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = rankBg,
                border = BorderStroke(1.dp, rankBorder),
                modifier = Modifier.size(width = 38.dp, height = 36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$rank",
                        style = TabularNumSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 12.sp),
                        color = rankText
                    )
                }
            }

            ResilientPosterImage(
                imageUrl = item.coverImage,
                contentDescription = item.title,
                modifier = Modifier
                    .width(52.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val metaParts = listOfNotNull(
                    item.formatLabel.takeIf { it.isNotBlank() },
                    item.releaseYear?.toString(),
                    item.episodes?.takeIf { it > 0 }?.let { "$it eps" }
                )
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.genres.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        item.genres.take(2).forEach { g ->
                            Surface(
                                shape = CircleShape,
                                color = colors.backgroundSecondary
                            ) {
                                Text(
                                    text = g,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.accentPrimary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.36f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = colors.highlight,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = item.scoreDisplay,
                            style = TabularNumSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.highlight
                        )
                    }
                }
                item.voteCount?.let { votes ->
                    Text(
                        text = "$votes ${if (votes == 1) "rating" else "ratings"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun CrownVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val crownPath = Path().apply {
            moveTo(w * 0.16f, h * 0.66f)
            lineTo(w * 0.10f, h * 0.28f)
            lineTo(w * 0.33f, h * 0.45f)
            lineTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.67f, h * 0.45f)
            lineTo(w * 0.90f, h * 0.28f)
            lineTo(w * 0.84f, h * 0.66f)
            close()
        }
        drawPath(crownPath, color = tint, style = stroke)
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.82f),
            end = androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.82f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun SlidableTopSourceToggle(
    source: TopAnimeSource,
    onSelectSource: (TopAnimeSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val selectedIndex = if (source == TopAnimeSource.COMMUNITY) 0 else 1

    Surface(
        shape = CircleShape,
        color = colors.backgroundSecondary,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
            .width(172.dp)
            .height(38.dp)
            .pointerInput(source) {
                var dragAccum = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccum += dragAmount
                    },
                    onDragEnd = {
                        if (dragAccum > 18f && source != TopAnimeSource.ALL) {
                            onSelectSource(TopAnimeSource.ALL)
                        } else if (dragAccum < -18f && source != TopAnimeSource.COMMUNITY) {
                            onSelectSource(TopAnimeSource.COMMUNITY)
                        }
                    }
                )
            }
            .testTag("top_source_slidable_toggle")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            val slotWidth = maxWidth / 2f
            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "top_source_slider_offset"
            )

            // Smoothly sliding active capsule pill
            Surface(
                shape = CircleShape,
                color = colors.accentPrimary.copy(alpha = if (colors.isDark) 0.24f else 0.16f),
                border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.55f)),
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(slotWidth)
                    .fillMaxHeight()
            ) {}

            Row(modifier = Modifier.fillMaxSize()) {
                TopAnimeSource.entries.forEach { src ->
                    val isSelected = source == src
                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) colors.highlight else colors.textSecondary,
                        animationSpec = tween(durationMillis = 180),
                        label = "top_source_label_${src.key}"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable { onSelectSource(src) }
                            .testTag("top_source_${src.key}")
                    ) {
                        Text(
                            text = src.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = labelColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

