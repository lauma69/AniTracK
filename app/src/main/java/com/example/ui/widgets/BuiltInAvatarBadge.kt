package com.example.ui.widgets

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BuiltInAvatar
import com.example.model.BuiltInAvatarFrame
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldBright
import com.example.ui.theme.ChampagneGoldSoft
import com.example.ui.theme.ChampagneOnGold
import com.example.ui.theme.IvoryText

@DrawableRes
fun BuiltInAvatar.drawableRes(): Int = when (this) {
    BuiltInAvatar.AVATAR_1 -> R.drawable.avatar_1
    BuiltInAvatar.AVATAR_2 -> R.drawable.avatar_2
    BuiltInAvatar.AVATAR_3 -> R.drawable.avatar_3
    BuiltInAvatar.AVATAR_4 -> R.drawable.avatar_4
    BuiltInAvatar.AVATAR_5 -> R.drawable.avatar_5
    BuiltInAvatar.AVATAR_6 -> R.drawable.avatar_6
    BuiltInAvatar.AVATAR_7 -> R.drawable.avatar_7
    BuiltInAvatar.AVATAR_8 -> R.drawable.avatar_8
    BuiltInAvatar.AVATAR_9 -> R.drawable.avatar_9
}

@DrawableRes
fun BuiltInAvatarFrame.drawableRes(): Int = when (this) {
    BuiltInAvatarFrame.FRAME_1 -> R.drawable.avatar_frame_1
    BuiltInAvatarFrame.FRAME_2 -> R.drawable.avatar_frame_2
    BuiltInAvatarFrame.FRAME_3 -> R.drawable.avatar_frame_3
    BuiltInAvatarFrame.FRAME_4 -> R.drawable.avatar_frame_4
    BuiltInAvatarFrame.FRAME_5 -> R.drawable.avatar_frame_5
    BuiltInAvatarFrame.FRAME_6 -> R.drawable.avatar_frame_6
    BuiltInAvatarFrame.FRAME_7 -> R.drawable.avatar_frame_7
    BuiltInAvatarFrame.FRAME_8 -> R.drawable.avatar_frame_8
    BuiltInAvatarFrame.FRAME_9 -> R.drawable.avatar_frame_9
    BuiltInAvatarFrame.FRAME_10 -> R.drawable.avatar_frame_10
}

@Composable
fun MemberAvatar(
    avatarKey: String?,
    avatarUrl: String?,
    username: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    frameOverride: BuiltInAvatarFrame? = null,
    suppressFrame: Boolean = false
) {
    val effectiveKey = avatarKey?.takeIf { it.isNotBlank() && it != "null" }
        ?: avatarUrl?.takeIf { !it.startsWith("http", ignoreCase = true) }
    val preset = BuiltInAvatar.fromKey(effectiveKey)
    val frame = if (suppressFrame) null else (frameOverride ?: BuiltInAvatarFrame.fromRawKey(avatarKey, avatarUrl))
    val validRemoteUrl = avatarUrl?.trim()?.takeIf {
        it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
    }

    val containerSize = if (frame != null) size * 1.38f else size
    val innerAvatarSize = if (frame != null) size else size

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(innerAvatarSize)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(preset.primaryColor.copy(alpha = 0.25f), preset.secondaryColor.copy(alpha = 0.25f))
                    )
                )
                .border(
                    width = if (frame != null) 1.dp else 1.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = if (frame != null) 0.28f else 0.55f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = preset.drawableRes()),
                contentDescription = "$username avatar (${preset.displayName})",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
            if (validRemoteUrl != null) {
                AsyncImage(
                    model = validRemoteUrl,
                    contentDescription = "$username avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
        if (frame != null) {
            Image(
                painter = painterResource(id = frame.drawableRes()),
                contentDescription = "${frame.displayName} avatar frame",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Premium circular horizontal-scrolling avatar carousel showing exclusively the 9 built-in portraits.
 */
@Composable
fun RoundAvatarCarousel(
    selectedKey: String,
    onSelectAvatar: (BuiltInAvatar) -> Unit,
    modifier: Modifier = Modifier,
    avatarDiameter: Dp = 74.dp,
    showLabels: Boolean = true,
    darkSurface: Boolean = false
) {
    val colors = AppTheme.colors
    val normalizedSelectedKey = BuiltInAvatar.fromKey(selectedKey).key
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("round_avatar_carousel"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(BuiltInAvatar.entries, key = { it.key }) { avatar ->
            val isSelected = normalizedSelectedKey == avatar.key
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.12f else 0.96f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "round_avatar_scale"
            )
            val ringColor by animateColorAsState(
                targetValue = if (isSelected) ChampagneGoldBright else Color.White.copy(alpha = 0.16f),
                animationSpec = tween(220),
                label = "round_avatar_ring"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .width(avatarDiameter + 14.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clip(CircleShape)
                    .clickable { onSelectAvatar(avatar) }
                    .padding(vertical = 4.dp)
                    .testTag("avatar_preset_${avatar.key}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(avatarDiameter)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .shadow(
                                    elevation = 14.dp,
                                    shape = CircleShape,
                                    ambientColor = ChampagneGold.copy(alpha = 0.45f),
                                    spotColor = ChampagneGold.copy(alpha = 0.45f)
                                )
                                .background(
                                    Brush.radialGradient(
                                        listOf(ChampagneGold.copy(alpha = 0.34f), Color.Transparent)
                                    ),
                                    CircleShape
                                )
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = if (isSelected) 2.5.dp else 1.2.dp,
                            brush = if (isSelected) {
                                Brush.linearGradient(listOf(ChampagneGoldBright, ChampagneGoldSoft, ChampagneGold))
                            } else {
                                Brush.linearGradient(listOf(ringColor, ringColor))
                            }
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isSelected) 1.dp else 3.dp)
                    ) {
                        Image(
                            painter = painterResource(id = avatar.drawableRes()),
                            contentDescription = avatar.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                                .clip(CircleShape)
                        )
                    }
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = ChampagneGold,
                            border = BorderStroke(1.5.dp, Color(0xFF0E0E14)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = ChampagneOnGold,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
                if (showLabels) {
                    val labelColor = when {
                        isSelected -> ChampagneGoldBright
                        darkSurface -> IvoryText.copy(alpha = 0.78f)
                        else -> colors.textSecondary
                    }
                    Text(
                        text = avatar.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
