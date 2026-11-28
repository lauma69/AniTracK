package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.service.MyShuffleAnimeItem
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.ResilientPosterImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ShuffleDustMote(
    val normX: Float,
    val normY: Float,
    val driftDx: Float,
    val driftDy: Float,
    val radiusDp: Float,
    val delayWave: Float,
    val swirlFreq: Float,
    val swirlAmp: Float,
    val colorType: Int,
    val peakAlpha: Float
)

enum class RandomSectionCategory(val key: String, val label: String, val subtitle: String) {
    DAILY_PICK("daily_pick", "Daily Pick", "Global recommendation • Resets 12:00 AM IST"),
    MY_SHUFFLE("my_shuffle", "My Shuffle", "Personal anime discovery • Roll the dice anytime")
}

@Composable
fun RandomSectionCategorySwitcher(
    selectedCategory: RandomSectionCategory,
    onSelectCategory: (RandomSectionCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val selectedIndex = if (selectedCategory == RandomSectionCategory.DAILY_PICK) 0 else 1

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("random_category_switcher_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (selectedCategory == RandomSectionCategory.DAILY_PICK) {
                            Icons.Filled.Star
                        } else {
                            Icons.Filled.Casino
                        },
                        contentDescription = null,
                        tint = colors.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = if (selectedCategory == RandomSectionCategory.DAILY_PICK) {
                                "DAILY GLOBAL FEATURE"
                            } else {
                                "PERSONAL DISCOVERY DECK"
                            },
                            style = MicroUpperLabel.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = colors.highlight
                        )
                        Text(
                            text = selectedCategory.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = colors.backgroundSecondary,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
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
                        label = "random_category_slider_offset"
                    )

                    Surface(
                        shape = CircleShape,
                        color = colors.accentPrimary.copy(alpha = if (colors.isDark) 0.24f else 0.16f),
                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.58f)),
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(slotWidth)
                            .fillMaxHeight()
                    ) {}

                    Row(modifier = Modifier.fillMaxSize()) {
                        RandomSectionCategory.entries.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            val labelColor by animateColorAsState(
                                targetValue = if (isSelected) colors.highlight else colors.textSecondary,
                                animationSpec = tween(durationMillis = 180),
                                label = "random_cat_color_${cat.key}"
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .clickable { onSelectCategory(cat) }
                                    .testTag("random_category_tab_${cat.key}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (cat == RandomSectionCategory.DAILY_PICK) "★" else "⚄",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = labelColor
                                    )
                                    Text(
                                        text = cat.label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = labelColor,
                                        maxLines = 1
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyShuffleSectionContent(
    item: MyShuffleAnimeItem?,
    isLoading: Boolean,
    error: String?,
    shuffleSequence: Int,
    onShuffleAgain: () -> Unit,
    onSelectAnime: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val isReducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    var detailsExpanded by rememberSaveable(item?.anilistId) { mutableStateOf(false) }
    var isRollingDice by remember { mutableStateOf(false) }
    var activeDiceFace by remember { mutableIntStateOf(5) }

    // Full-Card Dust Disintegration & Re-Materialization Animatables
    // dustMode: 0 = idle, 1 = disintegrating old card into dust, 2 = forming new card from dust
    var dustMode by remember { mutableIntStateOf(0) }
    val dustProgress = remember { Animatable(0f) }
    val cardDriftX = remember { Animatable(0f) }
    val cardDriftY = remember { Animatable(0f) }
    val cardScale = remember { Animatable(1f) }
    val cardAlpha = remember { Animatable(1f) }
    val buttonDieRotation = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(1f) }

    // Pre-allocated rich 125-particle dust field across the card template
    val dustMotes = remember {
        val rng = Random(7741)
        List(125) { idx ->
            val nx = rng.nextFloat()
            val ny = rng.nextFloat()
            // Wave sweeps diagonally from bottom-left to top-right
            val wave = ((nx * 0.65f) + ((1f - ny) * 0.35f)).coerceIn(0f, 1f) * 0.42f
            ShuffleDustMote(
                normX = nx,
                normY = ny,
                driftDx = 34f + rng.nextFloat() * 78f,
                driftDy = -(24f + rng.nextFloat() * 72f),
                radiusDp = 1.6f + rng.nextFloat() * 3.8f,
                delayWave = wave,
                swirlFreq = 2.2f + rng.nextFloat() * 4.0f,
                swirlAmp = 6f + rng.nextFloat() * 16f,
                colorType = idx % 4,
                peakAlpha = 0.58f + rng.nextFloat() * 0.42f
            )
        }
    }

    // Auto-generate initial random anime when My Shuffle is opened for the first time
    LaunchedEffect(item, isLoading, error) {
        if (item == null && !isLoading && error.isNullOrBlank()) {
            onShuffleAgain()
        }
    }

    // Phase 2: New card template materializes directly out of the swirling dust cloud
    LaunchedEffect(item?.anilistId, shuffleSequence) {
        if (item == null) return@LaunchedEffect
        if (isReducedMotion) {
            dustMode = 0
            dustProgress.snapTo(0f)
            cardDriftX.snapTo(0f)
            cardDriftY.snapTo(0f)
            cardScale.snapTo(1f)
            cardAlpha.snapTo(1f)
            contentOffsetY.snapTo(0f)
            contentAlpha.snapTo(1f)
            isRollingDice = false
        } else {
            dustMode = 2
            dustProgress.snapTo(0f)
            cardDriftX.snapTo(-18f)
            cardDriftY.snapTo(14f)
            cardScale.snapTo(0.962f)
            cardAlpha.snapTo(0.05f)
            contentOffsetY.snapTo(10f)
            contentAlpha.snapTo(0f)

            launch {
                dustProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 390, easing = FastOutSlowInEasing)
                )
                dustMode = 0
            }
            launch {
                cardAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                )
            }
            launch {
                cardDriftX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
                )
            }
            launch {
                cardDriftY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
                )
            }
            launch {
                cardScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                delay(55L)
                contentAlpha.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
            }
            delay(55L)
            contentOffsetY.animateTo(0f, tween(durationMillis = 270, easing = FastOutSlowInEasing))
            isRollingDice = false
        }
    }

    fun triggerDiceRollShuffle() {
        if (isLoading || isRollingDice) return
        isRollingDice = true
        scope.launch {
            if (!isReducedMotion && item != null) {
                dustMode = 1
                dustProgress.snapTo(0f)
                buttonDieRotation.snapTo(0f)

                // Cycle button die pips while the card template dissolves into dust
                launch {
                    val sequence = listOf(3, 6, 1, 4, 2, 5, Random.nextInt(1, 7))
                    for (face in sequence) {
                        activeDiceFace = face
                        delay(45L)
                    }
                }
                launch {
                    buttonDieRotation.animateTo(360f, tween(durationMillis = 310, easing = FastOutSlowInEasing))
                }
                launch {
                    cardScale.animateTo(1.028f, tween(durationMillis = 310, easing = FastOutSlowInEasing))
                }
                launch {
                    cardDriftX.animateTo(26f, tween(durationMillis = 310, easing = FastOutSlowInEasing))
                }
                launch {
                    cardDriftY.animateTo(-18f, tween(durationMillis = 310, easing = FastOutSlowInEasing))
                }
                launch {
                    cardAlpha.animateTo(0.03f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
                }
                dustProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                )
            } else {
                activeDiceFace = Random.nextInt(1, 7)
            }

            onShuffleAgain()
            delay(400L)
            isRollingDice = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("my_shuffle_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when {
            isLoading && item == null -> {
                MyShuffleDiceLoadingCard(
                    isReducedMotion = isReducedMotion,
                    diceFace = activeDiceFace
                )
            }
            !error.isNullOrBlank() && item == null -> {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = colors.elevatedSurface.copy(alpha = 0.97f),
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TactileDiceBadge(
                            face = activeDiceFace,
                            size = 46.dp,
                            backgroundColor = colors.accentPrimary.copy(alpha = 0.18f),
                            borderColor = colors.accentPrimary,
                            pipColor = colors.highlight
                        )
                        Text(
                            text = "Shuffle Deck Unavailable",
                            fontFamily = CormorantSerifFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        AppPrimaryButton(
                            text = "Shuffle Anime",
                            onClick = { triggerDiceRollShuffle() },
                            modifier = Modifier.testTag("my_shuffle_retry_button")
                        )
                    }
                }
            }
            item != null -> {
                val controlsBusy = isLoading || isRollingDice

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Ambient card stack base shadow
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = colors.cardBackground.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.18f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .offset(y = 7.dp)
                            .height(44.dp)
                            .align(Alignment.BottomCenter)
                    ) {}

                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = colors.elevatedSurface.copy(alpha = 0.98f),
                        border = BorderStroke(
                            width = if (controlsBusy) 1.5.dp else 1.dp,
                            color = if (controlsBusy) {
                                colors.accentPrimary.copy(alpha = 0.82f)
                            } else {
                                colors.accentPrimary.copy(alpha = 0.34f)
                            }
                        ),
                        shadowElevation = if (controlsBusy) 14.dp else 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationX = cardDriftX.value
                                translationY = cardDriftY.value
                                scaleX = cardScale.value
                                scaleY = cardScale.value
                                alpha = cardAlpha.value
                            }
                            .testTag("my_shuffle_card")
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Top Hero Banner Backdrop
                            val bannerUrl = item.bannerImage?.takeIf { it.isNotBlank() } ?: item.coverImage
                            if (bannerUrl.isNotBlank()) {
                                AsyncImage(
                                    model = bannerUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alpha = if (colors.isDark) 0.24f else 0.16f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(186.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(186.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.elevatedSurface.copy(alpha = 0.22f),
                                                    colors.elevatedSurface.copy(alpha = 0.85f),
                                                    colors.elevatedSurface.copy(alpha = 0.98f)
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
                                // Top Header Bar: MY SHUFFLE Pill + Airing Status Pill + Open Page Action
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = colors.highlight,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = "MY SHUFFLE",
                                                    style = MicroUpperLabel.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    ),
                                                    color = colors.highlight
                                                )
                                            }
                                        }

                                        val statusTint = when (item.rawStatus?.uppercase()) {
                                            "RELEASING" -> colors.success
                                            "NOT_YET_RELEASED" -> colors.accentPrimary
                                            "CANCELLED" -> colors.error
                                            else -> colors.textSecondary
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = statusTint.copy(alpha = if (colors.isDark) 0.15f else 0.11f),
                                            border = BorderStroke(
                                                1.dp,
                                                statusTint.copy(alpha = if (colors.isDark) 0.38f else 0.36f)
                                            )
                                        ) {
                                            Text(
                                                text = item.statusLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = statusTint,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (item.anilistId > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = colors.accentPrimary.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.48f)),
                                            modifier = Modifier
                                                .heightIn(min = 36.dp)
                                                .clip(CircleShape)
                                                .clickable { onSelectAnime(item.anilistId) }
                                                .testTag("my_shuffle_open_detail_button")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "Open Page",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = colors.highlight
                                                )
                                                Icon(
                                                    imageVector = Icons.Filled.OpenInNew,
                                                    contentDescription = null,
                                                    tint = colors.highlight,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Hero Showcase Row: Framed Poster + Title + Genre Chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            translationY = contentOffsetY.value
                                            alpha = contentAlpha.value
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Poster with overlaid Format badge
                                    Box(
                                        modifier = Modifier
                                            .width(122.dp)
                                            .height(176.dp)
                                    ) {
                                        ResilientPosterImage(
                                            imageUrl = item.coverImage,
                                            contentDescription = item.title,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(18.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = colors.accentPrimary.copy(alpha = 0.38f),
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                                .clickable(enabled = item.anilistId > 0) {
                                                    onSelectAnime(item.anilistId)
                                                }
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xCC0D1017),
                                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.55f)),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(7.dp)
                                        ) {
                                            Text(
                                                text = item.formatLabel.uppercase(),
                                                style = MicroUpperLabel.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontFamily = CormorantSerifFamily,
                                            fontSize = 24.sp,
                                            lineHeight = 27.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (item.secondaryTitle.isNotBlank()) {
                                            Text(
                                                text = item.secondaryTitle,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = colors.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Prominent Rating + Studio / Source Sub-pill
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = colors.accentPrimary.copy(alpha = 0.18f),
                                                border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.52f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Star,
                                                        contentDescription = null,
                                                        tint = colors.highlight,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = item.rating10Text,
                                                        style = TabularNumSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = colors.highlight
                                                    )
                                                }
                                            }

                                            if (item.studios.isNotEmpty()) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = colors.backgroundSecondary,
                                                    border = BorderStroke(1.dp, colors.borderSubtle)
                                                ) {
                                                    Text(
                                                        text = item.studios.first(),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = colors.textSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (item.genres.isNotEmpty()) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                item.genres.take(4).forEach { genre ->
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = colors.accentPrimary.copy(alpha = if (colors.isDark) 0.13f else 0.09f),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            colors.accentPrimary.copy(alpha = if (colors.isDark) 0.30f else 0.28f)
                                                        )
                                                    ) {
                                                        Text(
                                                            text = genre,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = colors.highlight,
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 4-Cell Quick Specs Strip: Rating • Format • Episodes • Release
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = colors.backgroundSecondary.copy(alpha = if (colors.isDark) 0.78f else 0.92f),
                                    border = BorderStroke(1.dp, colors.borderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            translationY = contentOffsetY.value
                                            alpha = contentAlpha.value
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        QuickSpecColumn(
                                            label = "SCORE",
                                            value = "★ ${item.rating10Text.removeSuffix(" / 10")}",
                                            highlight = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        QuickSpecDivider()
                                        QuickSpecColumn(
                                            label = "FORMAT",
                                            value = item.formatLabel,
                                            highlight = false,
                                            modifier = Modifier.weight(1f)
                                        )
                                        QuickSpecDivider()
                                        QuickSpecColumn(
                                            label = "EPISODES",
                                            value = item.episodes?.toString() ?: "TBA",
                                            highlight = false,
                                            modifier = Modifier.weight(1f)
                                        )
                                        QuickSpecDivider()
                                        QuickSpecColumn(
                                            label = "YEAR",
                                            value = item.releaseYearLabel,
                                            highlight = false,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Synopsis Block
                                val descText = item.description.ifBlank { "No description available." }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = colors.backgroundSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.6f),
                                    border = BorderStroke(1.dp, colors.borderSubtle.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            translationY = contentOffsetY.value
                                            alpha = contentAlpha.value
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "SYNOPSIS",
                                            style = MicroUpperLabel.copy(
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = colors.textMuted
                                        )
                                        Text(
                                            text = descText,
                                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                            color = colors.textSecondary,
                                            maxLines = if (detailsExpanded) Int.MAX_VALUE else 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // View Details toggle + Release Date label
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.backgroundSecondary,
                                        border = BorderStroke(1.dp, colors.borderSubtle),
                                        modifier = Modifier
                                            .heightIn(min = 38.dp)
                                            .clip(CircleShape)
                                            .clickable { detailsExpanded = !detailsExpanded }
                                            .testTag("my_shuffle_toggle_details_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
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

                                    Text(
                                        text = "Released: ${item.releaseDateOrYearLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }

                                AnimatedVisibility(visible = detailsExpanded) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = colors.backgroundSecondary,
                                        border = BorderStroke(1.dp, colors.borderSubtle),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val detailPairs = listOf(
                                            "Total Episodes" to item.episodesCountLabel,
                                            "Format" to item.formatLabel,
                                            "Airing Status" to item.statusLabel,
                                            "Release Date" to item.releaseDateOrYearLabel,
                                            "Season" to item.seasonLabel,
                                            "Release Year" to item.releaseYearLabel,
                                            "Episode Duration" to item.durationLabel,
                                            "Source Material" to item.sourceLabel,
                                            "Animation Studio" to item.studiosLabel,
                                            "Average Rating" to item.rating10Text
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

                                // Single Prominent Shuffle Button with Live Rolling Die Icon
                                Surface(
                                    shape = CircleShape,
                                    color = if (controlsBusy) {
                                        colors.accentPrimary.copy(alpha = 0.82f)
                                    } else {
                                        colors.accentPrimary
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        colors.highlight.copy(alpha = if (controlsBusy) 0.42f else 0.55f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = !controlsBusy) { triggerDiceRollShuffle() }
                                        .testTag("my_shuffle_again_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    rotationZ = if (isRollingDice && !isReducedMotion) buttonDieRotation.value else 0f
                                                }
                                        ) {
                                            TactileDiceBadge(
                                                face = activeDiceFace,
                                                size = 24.dp,
                                                backgroundColor = colors.onAccentPrimary.copy(alpha = 0.16f),
                                                borderColor = colors.onAccentPrimary,
                                                pipColor = colors.onAccentPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (controlsBusy) "SHUFFLING…" else "SHUFFLE ANIME",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.9.sp
                                            ),
                                            color = colors.onAccentPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Full-Card Dust Disintegration & Re-Materialization Particle Cloud Overlay
                    if (dustMode != 0 && !isReducedMotion) {
                        val prog = dustProgress.value.coerceIn(0f, 1f)
                        val accentCol = colors.accentPrimary
                        val highlightCol = colors.highlight
                        val pearlCol = colors.textPrimary
                        val emberCol = colors.textSecondary

                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .testTag("my_shuffle_dust_canvas")
                        ) {
                            val w = size.width
                            val h = size.height
                            val d = density.density

                            for (mote in dustMotes) {
                                // Normalize per-mote progress along the diagonal disintegration/reformation wave
                                val localT = ((prog - mote.delayWave) / (1f - mote.delayWave).coerceAtLeast(0.25f))
                                    .coerceIn(0f, 1f)
                                if (localT <= 0f || localT >= 1f) continue

                                // Phase 1 (dustMode == 1): particles break off card and drift outward/upward into dust
                                // Phase 2 (dustMode == 2): particles swirl in from dust cloud and condense into new card
                                val displacementT = if (dustMode == 1) localT else (1f - localT)
                                val swirlOffset = sin((displacementT * mote.swirlFreq * PI).toFloat()) * mote.swirlAmp * d
                                val px = (mote.normX * w) + (mote.driftDx * d * displacementT) + swirlOffset
                                val py = (mote.normY * h) + (mote.driftDy * d * displacementT) +
                                    (cos((displacementT * mote.swirlFreq * PI).toFloat()) * (mote.swirlAmp * 0.45f) * d)

                                val alphaEnvelope = sin((localT * PI).toFloat()) * mote.peakAlpha
                                val baseColor = when (mote.colorType) {
                                    0 -> highlightCol
                                    1 -> accentCol
                                    2 -> pearlCol
                                    else -> emberCol
                                }
                                val rPx = mote.radiusDp * d * (0.65f + 0.55f * sin((localT * PI).toFloat()))

                                // Soft outer dust halo
                                drawCircle(
                                    color = baseColor.copy(alpha = (alphaEnvelope * 0.28f).coerceIn(0f, 1f)),
                                    radius = rPx * 2.2f,
                                    center = Offset(px, py)
                                )
                                // Crisp dust grain core
                                drawCircle(
                                    color = baseColor.copy(alpha = alphaEnvelope.coerceIn(0f, 1f)),
                                    radius = rPx,
                                    center = Offset(px, py)
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
private fun QuickSpecColumn(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MicroUpperLabel.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = colors.textMuted,
            maxLines = 1
        )
        Text(
            text = value,
            style = TabularNumSmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = if (highlight) colors.highlight else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickSpecDivider() {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(26.dp)
            .background(colors.borderSubtle)
    )
}

/**
 * Custom vector-drawn die face (1..6 pips) with rounded tactile casing.
 */
@Composable
fun TactileDiceBadge(
    face: Int,
    size: Dp,
    backgroundColor: Color,
    borderColor: Color,
    pipColor: Color,
    modifier: Modifier = Modifier
) {
    val safeFace = face.coerceIn(1, 6)
    Canvas(modifier = modifier.size(size)) {
        val corner = CornerRadius(this.size.minDimension * 0.24f, this.size.minDimension * 0.24f)
        drawRoundRect(
            color = backgroundColor,
            topLeft = Offset.Zero,
            size = Size(this.size.width, this.size.height),
            cornerRadius = corner
        )
        drawRoundRect(
            color = borderColor,
            topLeft = Offset.Zero,
            size = Size(this.size.width, this.size.height),
            cornerRadius = corner,
            style = Stroke(width = (this.size.minDimension * 0.08f).coerceAtLeast(1.5f))
        )

        val w = this.size.width
        val h = this.size.height
        val pipRadius = (this.size.minDimension * 0.095f).coerceAtLeast(2f)

        val left = w * 0.27f
        val centerX = w * 0.50f
        val right = w * 0.73f

        val top = h * 0.27f
        val centerY = h * 0.50f
        val bottom = h * 0.73f

        val pips = when (safeFace) {
            1 -> listOf(Offset(centerX, centerY))
            2 -> listOf(Offset(left, top), Offset(right, bottom))
            3 -> listOf(Offset(left, top), Offset(centerX, centerY), Offset(right, bottom))
            4 -> listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(left, bottom),
                Offset(right, bottom)
            )
            5 -> listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(centerX, centerY),
                Offset(left, bottom),
                Offset(right, bottom)
            )
            else -> listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(left, centerY),
                Offset(right, centerY),
                Offset(left, bottom),
                Offset(right, bottom)
            )
        }

        pips.forEach { pt ->
            drawCircle(
                color = pipColor,
                radius = pipRadius,
                center = pt
            )
        }
    }
}

@Composable
private fun MyShuffleDiceLoadingCard(
    isReducedMotion: Boolean,
    diceFace: Int
) {
    val colors = AppTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "shuffle_dice_loading")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 640, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shuffle_dice_spin"
    )
    val skelColor = colors.textMuted.copy(alpha = if (colors.isDark) 0.16f else 0.14f)
    val skelSoft = colors.textMuted.copy(alpha = if (colors.isDark) 0.11f else 0.09f)

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = colors.elevatedSurface.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = if (isReducedMotion) 0f else spinAngle
                    }
                ) {
                    TactileDiceBadge(
                        face = diceFace,
                        size = 22.dp,
                        backgroundColor = colors.accentPrimary.copy(alpha = 0.18f),
                        borderColor = colors.highlight,
                        pipColor = colors.highlight
                    )
                }
                Text(
                    text = "ROLLING FOR YOUR NEXT ANIME…",
                    style = MicroUpperLabel.copy(fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.highlight
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .width(122.dp)
                        .height(176.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(skelColor)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(24.dp)
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
        }
    }
}
