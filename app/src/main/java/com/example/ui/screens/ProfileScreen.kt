package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.config.AppConfig
import com.example.model.AVATAR_FRAME_UNLOCK_XP
import com.example.model.BuiltInAvatar
import com.example.model.BuiltInAvatarFrame
import com.example.model.MemberStatus
import com.example.model.StarRatingUtils
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ChampagneOnGold
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumLarge
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AppCard
import com.example.ui.widgets.AppIconButton
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.MemberAvatar
import com.example.ui.widgets.ObsidianAmbientBackground
import com.example.ui.widgets.ResilientPosterImage
import com.example.ui.widgets.RoundAvatarCarousel
import com.example.viewmodel.AnimeViewModel

@Composable
fun ProfileScreen(
    viewModel: AnimeViewModel,
    onBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onSelectAnime: (Int) -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val colors = AppTheme.colors
    val profile by viewModel.userProfile.collectAsState()
    val profileError by viewModel.profileError.collectAsState()
    val isProfileSaving by viewModel.isProfileSaving.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val communityReviews by viewModel.communityReviews.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    var showAvatarDialog by remember { mutableStateOf(false) }
    var showAvatarFramesDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showLeaveCommunityConfirm by remember { mutableStateOf(false) }

    var draftAvatarKey by remember(profile?.avatarKey) {
        mutableStateOf(BuiltInAvatar.fromKey(profile?.avatarKey).key)
    }
    var draftUsername by remember(profile?.username) {
        mutableStateOf(profile?.username ?: "")
    }

    val activeFrame = remember(profile?.avatarKey) { BuiltInAvatarFrame.fromRawKey(profile?.avatarKey) }
    val userXp = remember(watchlist, communityReviews, profile?.id, profile?.xp) {
        val ids = HashSet<Int>()
        watchlist.forEach { if ((it.userScore ?: 0) > 0) ids.add(it.id) }
        val uid = profile?.id
        if (!uid.isNullOrBlank()) {
            communityReviews.forEach {
                if (it.userId == uid && (it.rating ?: 0.0) > 0.0) ids.add(it.animeId)
            }
        }
        maxOf(profile?.xp ?: 0, ids.size)
    }
    val framesUnlocked = userXp >= AVATAR_FRAME_UNLOCK_XP
    val watchedAnimeCount = remember(watchlist) { watchlist.count { it.watchedEpisodeCount >= 1 } }
    val meanScore = remember(watchlist) {
        val scored = watchlist.mapNotNull { it.userScore?.takeIf { s -> s > 0 } }
        if (scored.isEmpty()) "—" else {
            val avgStars = StarRatingUtils.score100ToStars(scored.average().toInt())
            "★ ${StarRatingUtils.formatStars(avgStars)}"
        }
    }

    ObsidianAmbientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .testTag("profile_screen"),
                contentPadding = PaddingValues(start = AppSpacing.md, end = AppSpacing.md, top = AppSpacing.xs, bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with circular Back button & Settings icon
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                            modifier = Modifier.testTag("profile_back_button")
                        )
                        Text(
                            text = "Profile",
                            fontFamily = CormorantSerifFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        AppIconButton(
                            icon = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("profile_settings_button")
                        )
                    }
                }

                // Centered Account Hero (Clickable PFP & Username; Email never displayed)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val heroStageSize = if (activeFrame != null) 144.dp else 108.dp
                        Box(
                            modifier = Modifier
                                .size(heroStageSize)
                                .then(
                                    if (profile != null) {
                                        Modifier
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable {
                                                draftAvatarKey = BuiltInAvatar.fromKey(profile?.avatarKey).key
                                                viewModel.clearProfileError()
                                                showAvatarDialog = true
                                            }
                                            .testTag("profile_avatar_button")
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeFrame != null) {
                                MemberAvatar(
                                    avatarKey = profile?.avatarKey,
                                    avatarUrl = null,
                                    username = profile?.username ?: "A",
                                    size = 102.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(104.dp)
                                        .shadow(
                                            elevation = 16.dp,
                                            shape = CircleShape,
                                            ambientColor = colors.accentPrimary.copy(alpha = 0.42f),
                                            spotColor = colors.accentPrimary.copy(alpha = 0.42f)
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(colors.accentPrimary, colors.accentSecondary)
                                            )
                                        )
                                        .border(2.5.dp, colors.backgroundMain, CircleShape)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MemberAvatar(
                                        avatarKey = profile?.avatarKey,
                                        avatarUrl = null,
                                        username = profile?.username ?: "A",
                                        size = 94.dp,
                                        suppressFrame = true
                                    )
                                }
                            }
                            if (profile != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.accentPrimary,
                                    border = BorderStroke(2.dp, colors.backgroundMain),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(if (activeFrame != null) 8.dp else 0.dp)
                                        .size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Change avatar",
                                            tint = ChampagneOnGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .then(
                                    if (profile != null) {
                                        Modifier
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                draftUsername = profile?.username ?: ""
                                                viewModel.clearProfileError()
                                                showUsernameDialog = true
                                            }
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .testTag("profile_username_button")
                                    } else {
                                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    }
                                )
                        ) {
                            Text(
                                text = profile?.username ?: "Guest Tracker",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            if (profile != null) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit username",
                                    tint = colors.accentPrimary.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = when {
                                profile == null -> "LOCAL VAULT"
                                profile?.memberStatus == MemberStatus.ACTIVE -> {
                                    if (profile?.isAdmin == true) "ADMIN • COMMUNITY" else "COMMUNITY MEMBER"
                                }
                                else -> profile?.memberStatus?.label?.uppercase() ?: "ACCOUNT ACTIVE"
                            },
                            style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.6.sp),
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 4-Tile Editorial Stats Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileStatTile("XP", userXp.toString(), Modifier.weight(1f))
                        ProfileStatTile("WATCHED", watchedAnimeCount.toString(), Modifier.weight(1f))
                        ProfileStatTile("SAVED", watchlist.size.toString(), Modifier.weight(1f))
                        ProfileStatTile("MEAN", meanScore, Modifier.weight(1f))
                    }
                }

                // Avatar Frames Trigger Button (Opens dedicated Avatar Frames section; unlocks at 50 XP)
                item {
                    AvatarFramesTriggerButton(
                        currentAvatarKey = profile?.avatarKey ?: BuiltInAvatar.AVATAR_1.key,
                        username = profile?.username ?: "A",
                        activeFrame = activeFrame,
                        userXp = userXp,
                        framesUnlocked = framesUnlocked,
                        onClick = { showAvatarFramesDialog = true }
                    )
                }

                // Premium Community Status Card
                if (profile != null) {
                    item {
                        val isActiveMember = profile!!.memberStatus == MemberStatus.ACTIVE
                        val statusAccent = if (isActiveMember) colors.success else colors.accentPrimary
                        val cardBrush = Brush.linearGradient(
                            colors = if (colors.isDark) {
                                listOf(
                                    colors.accentPrimary.copy(alpha = 0.14f),
                                    colors.cardBackground,
                                    statusAccent.copy(alpha = 0.08f)
                                )
                            } else {
                                listOf(
                                    colors.accentPrimary.copy(alpha = 0.10f),
                                    colors.cardBackground,
                                    statusAccent.copy(alpha = 0.06f)
                                )
                            }
                        )
                        val borderBrush = Brush.linearGradient(
                            colors = listOf(
                                colors.accentPrimary.copy(alpha = 0.45f),
                                colors.borderSubtle,
                                statusAccent.copy(alpha = 0.35f)
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, borderBrush),
                            shadowElevation = if (colors.isDark) 4.dp else 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_community_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(cardBrush)
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Top bar: micro-label + luminous status pill
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Group,
                                            contentDescription = null,
                                            tint = colors.accentPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "COMMUNITY STATUS",
                                            style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 2.2.sp),
                                            color = colors.accentPrimary
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = statusAccent.copy(alpha = 0.14f),
                                        border = BorderStroke(0.75.dp, statusAccent.copy(alpha = 0.38f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(statusAccent)
                                            )
                                            Text(
                                                text = profile!!.memberStatus.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = statusAccent
                                            )
                                        }
                                    }
                                }

                                // Centerpiece membership emblem + editorial tier title
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        colors.accentPrimary.copy(alpha = 0.28f),
                                                        colors.accentPrimary.copy(alpha = 0.08f)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 1.dp,
                                                brush = Brush.linearGradient(
                                                    listOf(
                                                        colors.accentPrimary.copy(alpha = 0.65f),
                                                        statusAccent.copy(alpha = 0.45f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isActiveMember) Icons.Filled.Verified else Icons.Filled.Group,
                                            contentDescription = null,
                                            tint = if (isActiveMember) colors.accentPrimary else colors.textSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                !isActiveMember -> "Lounge Inactive"
                                                profile!!.isAdmin -> "Circle Steward"
                                                else -> "Verified Circle Member"
                                            },
                                            fontFamily = CormorantSerifFamily,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = when {
                                                isActiveMember -> "Shared ratings & circle lounge active"
                                                else -> "Rejoin via invite to unlock community"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isActiveMember) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        colors.borderSubtle,
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )

                                    Surface(
                                        onClick = { showLeaveCommunityConfirm = true },
                                        shape = CircleShape,
                                        color = colors.error.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, colors.error.copy(alpha = 0.32f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .testTag("profile_leave_community_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                                contentDescription = null,
                                                tint = colors.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Leave Community",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Watchlist Highlights
                if (watchlist.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            Text(
                                text = "Watchlist Highlights",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(watchlist.take(10), key = { it.id }) { entry ->
                                    AppCard(
                                        shape = RoundedCornerShape(20.dp),
                                        onClick = { onSelectAnime(entry.id) },
                                        modifier = Modifier.width(120.dp)
                                    ) {
                                        Column {
                                            ResilientPosterImage(
                                                imageUrl = entry.coverImage,
                                                contentDescription = entry.getResolvedTitle(preferences.titleLanguage),
                                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                                modifier = Modifier.fillMaxWidth().height(160.dp)
                                            )
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = entry.getResolvedTitle(preferences.titleLanguage),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = entry.watchStatusEnum.label,
                                                    style = TabularNumSmall.copy(fontSize = 10.sp),
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Guest Sign-In Card
                if (profile == null) {
                    item {
                        AppCard(shape = RoundedCornerShape(24.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Sign in to sync your watchlist and collections (${collections.size} local).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                                AppPrimaryButton(
                                    text = "Sign In / Create Account",
                                    onClick = { viewModel.exitLocalPreviewMode() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "${AppConfig.APP_NAME} v${AppConfig.APP_VERSION}",
                        style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 2.8.sp),
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Round Horizontal Avatar Picker Dialog (Tap PFP)
    if (showAvatarDialog && profile != null) {
        val currentPreset = remember(draftAvatarKey) { BuiltInAvatar.fromKey(draftAvatarKey) }
        AlertDialog(
            onDismissRequest = { if (!isProfileSaving) showAvatarDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.backgroundSecondary,
            modifier = Modifier.testTag("profile_avatar_picker_dialog"),
            title = {
                Text(
                    text = "Choose Avatar",
                    fontFamily = CormorantSerifFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val previewCompositeKey = remember(draftAvatarKey, activeFrame) {
                        BuiltInAvatarFrame.composeAvatarKey(draftAvatarKey, activeFrame?.key)
                    }
                    Box(
                        modifier = if (activeFrame != null) {
                            Modifier.size(120.dp)
                        } else {
                            Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.dp, colors.accentPrimary, CircleShape)
                                .padding(3.dp)
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        MemberAvatar(
                            avatarKey = previewCompositeKey,
                            avatarUrl = null,
                            username = profile!!.username,
                            size = 86.dp
                        )
                    }
                    Text(
                        text = currentPreset.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accentPrimary
                    )
                    RoundAvatarCarousel(
                        selectedKey = draftAvatarKey,
                        onSelectAvatar = { draftAvatarKey = it.key },
                        avatarDiameter = 68.dp,
                        showLabels = true,
                        darkSurface = colors.isDark
                    )
                    if (!profileError.isNullOrBlank()) {
                        Text(
                            text = profileError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isProfileSaving,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = ChampagneOnGold
                    ),
                    onClick = {
                        val nextComposite = BuiltInAvatarFrame.composeAvatarKey(draftAvatarKey, activeFrame?.key)
                        viewModel.updateProfile(
                            username = profile!!.username,
                            avatarKey = nextComposite,
                            clearCustomAvatar = true,
                            bio = ""
                        ) {
                            showAvatarDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_avatar_button")
                ) {
                    if (isProfileSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ChampagneOnGold)
                    } else {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isProfileSaving,
                    onClick = { showAvatarDialog = false }
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Username Change Dialog (Tap Username)
    if (showUsernameDialog && profile != null) {
        AlertDialog(
            onDismissRequest = { if (!isProfileSaving) showUsernameDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.backgroundSecondary,
            modifier = Modifier.testTag("profile_edit_username_dialog"),
            title = {
                Text(
                    text = "Edit Username",
                    fontFamily = CormorantSerifFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draftUsername,
                        onValueChange = {
                            draftUsername = it
                            viewModel.clearProfileError()
                        },
                        label = { Text("Username") },
                        singleLine = true,
                        isError = !profileError.isNullOrBlank(),
                        shape = RoundedCornerShape(AppRadius.md),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_username_input")
                    )
                    if (!profileError.isNullOrBlank()) {
                        Text(
                            text = profileError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                            modifier = Modifier.testTag("profile_username_error")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isProfileSaving,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentPrimary,
                        contentColor = ChampagneOnGold
                    ),
                    onClick = {
                        viewModel.updateProfile(
                            username = draftUsername,
                            avatarKey = profile!!.avatarKey,
                            clearCustomAvatar = true,
                            bio = ""
                        ) {
                            showUsernameDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_username_button")
                ) {
                    if (isProfileSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ChampagneOnGold)
                    } else {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isProfileSaving,
                    onClick = { showUsernameDialog = false }
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (showAvatarFramesDialog) {
        AvatarFramesDialog(
            currentAvatarKey = profile?.avatarKey ?: BuiltInAvatar.AVATAR_1.key,
            username = profile?.username ?: "A",
            activeFrame = activeFrame,
            userXp = userXp,
            framesUnlocked = framesUnlocked,
            isSaving = isProfileSaving,
            onSelectFrame = { selectedFrame ->
                viewModel.equipAvatarFrame(selectedFrame?.key, userXp)
            },
            onDismiss = { showAvatarFramesDialog = false }
        )
    }

    if (showLeaveCommunityConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveCommunityConfirm = false },
            shape = RoundedCornerShape(AppRadius.xl),
            containerColor = colors.backgroundSecondary,
            title = { Text("Leave Community?", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Text(
                    "Your shared reviews will be hidden until you rejoin.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveCommunity()
                        showLeaveCommunityConfirm = false
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveCommunityConfirm = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProfileStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        border = BorderStroke(0.75.dp, colors.borderSubtle),
        shadowElevation = if (colors.isDark) 1.dp else 3.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = TabularNumLarge.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 23.sp
                ),
                color = colors.highlight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MicroUpperLabel.copy(fontSize = 7.5.sp, letterSpacing = 1.4.sp),
                color = colors.textMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AvatarFramesTriggerButton(
    currentAvatarKey: String,
    username: String,
    activeFrame: BuiltInAvatarFrame?,
    userXp: Int,
    framesUnlocked: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val badgeAccent = if (framesUnlocked) colors.success else colors.accentPrimary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.36f)),
        shadowElevation = if (colors.isDark) 4.dp else 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .testTag("profile_avatar_frames_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.accentPrimary.copy(alpha = if (colors.isDark) 0.14f else 0.10f),
                            colors.cardBackground
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                MemberAvatar(
                    avatarKey = currentAvatarKey,
                    avatarUrl = null,
                    username = username,
                    size = 42.dp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = colors.accentPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "AVATAR FRAMES",
                        style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.9.sp),
                        color = colors.accentPrimary
                    )
                }
                Text(
                    text = activeFrame?.displayName ?: "Choose Avatar Frame",
                    fontFamily = CormorantSerifFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Usable on all avatars • Visible to all users",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = badgeAccent.copy(alpha = 0.14f),
                    border = BorderStroke(0.75.dp, badgeAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        if (!framesUnlocked) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = badgeAccent,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = if (framesUnlocked) "UNLOCKED" else "$userXp / $AVATAR_FRAME_UNLOCK_XP XP",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = badgeAccent
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open Avatar Frames",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AvatarFramesDialog(
    currentAvatarKey: String,
    username: String,
    activeFrame: BuiltInAvatarFrame?,
    userXp: Int,
    framesUnlocked: Boolean,
    isSaving: Boolean,
    onSelectFrame: (BuiltInAvatarFrame?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val progress = (userXp.toFloat() / AVATAR_FRAME_UNLOCK_XP.toFloat()).coerceIn(0f, 1f)
    val remaining = (AVATAR_FRAME_UNLOCK_XP - userXp).coerceAtLeast(0)
    val badgeAccent = if (framesUnlocked) colors.success else colors.accentPrimary
    val allOptions: List<BuiltInAvatarFrame?> = remember {
        listOf<BuiltInAvatarFrame?>(null) + BuiltInAvatarFrame.entries
    }
    val rows = remember(allOptions) { allOptions.chunked(3) }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.backgroundSecondary,
            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.35f)),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .testTag("profile_avatar_frames_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = colors.accentPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "PROFILE COSMETICS",
                                style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 2.0.sp),
                                color = colors.accentPrimary
                            )
                        }
                        Text(
                            text = "Avatar Frames",
                            fontFamily = CormorantSerifFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }

                    AppIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "Close Avatar Frames",
                        onClick = { if (!isSaving) onDismiss() },
                        modifier = Modifier.testTag("close_avatar_frames_button")
                    )
                }

                // Live Preview Hero
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.cardBackground,
                    border = BorderStroke(0.75.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(124.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MemberAvatar(
                                avatarKey = currentAvatarKey,
                                avatarUrl = null,
                                username = username,
                                size = 88.dp
                            )
                        }
                        Text(
                            text = activeFrame?.displayName ?: "No Frame Equipped",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Works on all avatars • Visible to all users",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // XP Unlock Status & Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (framesUnlocked) {
                                "Unlocked at $AVATAR_FRAME_UNLOCK_XP XP"
                            } else {
                                "$remaining XP needed to unlock"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                        Surface(
                            shape = CircleShape,
                            color = badgeAccent.copy(alpha = 0.14f),
                            border = BorderStroke(0.75.dp, badgeAccent.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                if (!framesUnlocked) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = badgeAccent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    text = if (framesUnlocked) "$userXp XP • UNLOCKED" else "$userXp / $AVATAR_FRAME_UNLOCK_XP XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeAccent
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(colors.textPrimary.copy(alpha = 0.09f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(colors.accentPrimary, colors.highlight)
                                    )
                                )
                        )
                    }
                }

                // 3-Column Frame Grid (None + 10 Collector Frames)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("avatar_frames_grid")
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { option ->
                                val isSelected = activeFrame?.key == option?.key
                                val isLockedOption = option != null && !framesUnlocked
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isSelected) colors.accentPrimary.copy(alpha = 0.16f) else colors.cardBackground,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.75.dp,
                                        color = if (isSelected) colors.accentPrimary else colors.borderSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable(enabled = !isSaving) { onSelectFrame(option) }
                                        .testTag("avatar_frame_option_${option?.key ?: "none"}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(74.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            MemberAvatar(
                                                avatarKey = currentAvatarKey,
                                                avatarUrl = null,
                                                username = username,
                                                size = 52.dp,
                                                frameOverride = option,
                                                suppressFrame = option == null
                                            )
                                            if (isSelected) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = colors.accentPrimary,
                                                    border = BorderStroke(1.5.dp, colors.backgroundMain),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(20.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "Equipped",
                                                            tint = ChampagneOnGold,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            } else if (isLockedOption) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = colors.backgroundMain.copy(alpha = 0.88f),
                                                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.45f)),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .size(20.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Lock,
                                                            contentDescription = "Requires 50 XP",
                                                            tint = colors.accentPrimary,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Text(
                                            text = option?.displayName ?: "None",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.highlight else colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
