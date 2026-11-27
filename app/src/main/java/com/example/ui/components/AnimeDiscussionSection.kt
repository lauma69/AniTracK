package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.model.AnimeCommentItem
import com.example.model.CommunityEmoji
import com.example.model.FlatReplyTo
import com.example.model.MentionableUser
import com.example.model.NotificationJumpTarget
import com.example.service.CommunityEmojiCacheService
import com.example.ui.screens.formatCommunityTimestamp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.MemberAvatar
import com.example.util.DiscussionRules
import com.example.viewmodel.AnimeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LivTrAkBg = Color(0xFF030304)
private val LivTrAkSurface = Color(0xFF0E0E16)
private val LivTrAkSurface2 = Color(0xFF14141E)
private val FrostedMenuBg = Color(0xEB151524)
private val FrostedMenuBorder = Color(0x3DE8C88F)
private val LivTrAkGold = Color(0xFFE8C88F)
private val LivTrAkGoldSoft = Color(0xFFF5E4BE)
private val LivTrAkIvory = Color(0xFFF8F4EB)
private val LivTrAkAsh = Color(0xFF9A9AA8)
private val LivTrAkAsh2 = Color(0xFF66667A)
private val LivTrAkHairline = Color(0x14FFFFFF)

private val SOLO_EMOJI_BRACKET_REGEX = Regex("""^\[emoji:([a-zA-Z0-9_-]{1,64})]$""", RegexOption.IGNORE_CASE)
private val SOLO_EMOJI_COLON_REGEX = Regex("""^:([a-zA-Z0-9][a-zA-Z0-9_-]{0,63}):$""", RegexOption.IGNORE_CASE)
private val ANY_EMOJI_BRACKET_REGEX = Regex("""\[emoji:([a-zA-Z0-9_-]{1,64})]""", RegexOption.IGNORE_CASE)
private val ANY_EMOJI_COLON_REGEX = Regex(""":([a-zA-Z0-9][a-zA-Z0-9_-]{0,63}):""", RegexOption.IGNORE_CASE)

private fun extractSoloEmojiId(content: String): String? {
    val cleaned = content.replace(Regex("""[\s\u200B-\u200D\uFEFF]+"""), "")
    SOLO_EMOJI_BRACKET_REGEX.matchEntire(cleaned)?.groupValues?.getOrNull(1)?.trim()?.lowercase()?.let { return it }
    SOLO_EMOJI_COLON_REGEX.matchEntire(cleaned)?.groupValues?.getOrNull(1)?.trim()?.lowercase()?.let { return it }
    return null
}

private fun extractFirstStickerCode(content: String?): String? {
    if (content.isNullOrBlank()) return null
    ANY_EMOJI_BRACKET_REGEX.find(content)?.groupValues?.getOrNull(1)?.trim()?.lowercase()?.let { return it }
    ANY_EMOJI_COLON_REGEX.find(content)?.groupValues?.getOrNull(1)?.trim()?.lowercase()?.let { return it }
    return null
}

private fun stripStickerTokens(text: String): String {
    val stripped = ANY_EMOJI_BRACKET_REGEX.replace(text, " ")
        .let { ANY_EMOJI_COLON_REGEX.replace(it, " ") }
        .replace(Regex("""\s+"""), " ")
        .trim()
    return stripped
}

private fun isSoloUnicodeEmoji(content: String): Boolean {
    val cleaned = content.replace(Regex("""[\s\u200B-\u200D\uFEFF]+"""), "")
    if (cleaned.isEmpty() || cleaned.length > 16) return false
    var i = 0
    var hasEmoji = false
    while (i < cleaned.length) {
        val cp = cleaned.codePointAt(i)
        val isEmojiCp = (cp in 0x1F000..0x1FAFF) ||
            (cp in 0x2600..0x27BF) ||
            (cp in 0x2300..0x23FF) ||
            cp == 0xFE0F ||
            cp == 0x200D ||
            (cp in 0x1F3FB..0x1F3FF)
        if (!isEmojiCp) return false
        if (cp != 0xFE0F && cp != 0x200D) hasEmoji = true
        i += Character.charCount(cp)
    }
    return hasEmoji
}

private fun formatPlainPreviewWithEmojis(content: String): String =
    ANY_EMOJI_BRACKET_REGEX.replace(content) { ":${it.groupValues[1]}:" }.trim()

private data class ReplyComposerTarget(
    val messageId: String,
    val senderName: String,
    val previewText: String,
    val stickerCode: String? = null
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AnimeDiscussionSection(
    animeId: Int,
    animeTitle: String,
    animeCover: String,
    viewModel: AnimeViewModel,
    highlightTarget: NotificationJumpTarget? = null,
    initialReplyTargetUser: MentionableUser? = null,
    onInitialReplyTargetConsumed: () -> Unit = {},
    inModal: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val haptics = LocalHapticFeedback.current
    val profile by viewModel.userProfile.collectAsState()
    val authSession by viewModel.authSession.collectAsState()
    val isSignedIn = profile != null || authSession != null
    val currentUserId = profile?.id?.takeIf { it.isNotBlank() } ?: authSession?.userId
    val comments by viewModel.discussionComments.collectAsState()
    val totalComments by viewModel.discussionTotalCount.collectAsState()
    val hasMore by viewModel.discussionHasMore.collectAsState()
    val isLoading by viewModel.isDiscussionLoading.collectAsState()
    val isSubmitting by viewModel.isDiscussionSubmitting.collectAsState()
    val feedbackError by viewModel.discussionErrorMessage.collectAsState()
    val mentionSuggestions by viewModel.mentionSuggestions.collectAsState()
    val communityEmojis by viewModel.communityEmojis.collectAsState()
    val communityMembers by viewModel.communityMembers.collectAsState()

    val emojiById = remember(communityEmojis) { communityEmojis.associateBy { it.id.lowercase() } }
    val memberById = remember(communityMembers) { communityMembers.associateBy { it.userId } }
    val chronologicalComments = remember(comments) { comments.asReversed() }
    val listState = rememberLazyListState()

    var draft by remember(animeId) { mutableStateOf(TextFieldValue("")) }
    var selectedTags by remember(animeId) { mutableStateOf<List<MentionableUser>>(emptyList()) }
    var activeReplyTarget by remember(animeId) { mutableStateOf<ReplyComposerTarget?>(null) }
    var isEmojiTrayOpen by remember(animeId) { mutableStateOf(false) }
    var showMentionMenu by remember(animeId) { mutableStateOf(false) }
    var editingCommentId by remember(animeId) { mutableStateOf<String?>(null) }
    var editingCommentDraft by remember(animeId) { mutableStateOf("") }
    var openCommentMenuId by remember(animeId) { mutableStateOf<String?>(null) }
    var scrolledToHighlightKey by remember(animeId) { mutableStateOf<String?>(null) }
    var lastLatestMessageId by remember(animeId) { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var activeHighlightTarget by remember(animeId) { mutableStateOf(highlightTarget) }

    LaunchedEffect(animeId) {
        viewModel.loadCommunityEmojis()
    }

    // Persistent Supabase Realtime WebSocket listener with clean unsubscribe on dispose
    DisposableEffect(animeId, authSession?.userId) {
        if (authSession != null && animeId > 0) {
            viewModel.startAnimeChatRealtime(animeId)
        }
        onDispose {
            viewModel.stopAnimeChatRealtime()
        }
    }

    // Smooth auto-scroll when a new message arrives at the bottom of the chronological stream
    LaunchedEffect(chronologicalComments.lastOrNull()?.id) {
        val newestId = chronologicalComments.lastOrNull()?.id
        if (newestId != null && newestId != lastLatestMessageId) {
            val wasEmpty = lastLatestMessageId == null
            lastLatestMessageId = newestId
            if (!isLoading && chronologicalComments.isNotEmpty() && activeHighlightTarget == null) {
                val targetIndex = chronologicalComments.lastIndex
                if (wasEmpty) {
                    listState.scrollToItem(targetIndex)
                } else {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val nearBottom = lastVisible >= (targetIndex - 3)
                    val isOwnNewest = chronologicalComments.lastOrNull()?.userId == currentUserId
                    if (nearBottom || isOwnNewest) {
                        listState.animateScrollToItem(targetIndex)
                    }
                }
            }
        }
    }

    LaunchedEffect(highlightTarget) {
        if (highlightTarget != null) {
            activeHighlightTarget = highlightTarget
            scrolledToHighlightKey = null
        }
    }

    // Scroll to highlighted message when jumping from notification or clicking a quoted preview
    LaunchedEffect(activeHighlightTarget, chronologicalComments) {
        val target = activeHighlightTarget ?: return@LaunchedEffect
        val targetId = target.replyId?.takeIf { it.isNotBlank() } ?: target.commentId
        val key = "${target.commentId}_${target.replyId.orEmpty()}"
        if (scrolledToHighlightKey == key) return@LaunchedEffect
        val idx = chronologicalComments.indexOfFirst { it.id == targetId || it.id == target.commentId }
        if (idx >= 0) {
            scrolledToHighlightKey = key
            listState.animateScrollToItem(idx)
        }
    }

    // When triggered via "Reply" on a Community review, switch composer into reply mode or pre-tag
    LaunchedEffect(initialReplyTargetUser, isLoading, comments) {
        val targetUser = initialReplyTargetUser ?: return@LaunchedEffect
        if (isLoading) return@LaunchedEffect
        val existingComment = comments.firstOrNull { c ->
            (targetUser.id.isNotBlank() && c.userId == targetUser.id) ||
                c.username.equals(targetUser.username, ignoreCase = true)
        }
        if (existingComment != null) {
            activeReplyTarget = ReplyComposerTarget(
                messageId = existingComment.id,
                senderName = existingComment.username,
                previewText = formatPlainPreviewWithEmojis(existingComment.content).take(180),
                stickerCode = extractFirstStickerCode(existingComment.content)
            )
        } else if (targetUser.id.isNotBlank()) {
            val currentTotal = DiscussionRules.validateContentMentions(draft.text, selectedTags.size).totalEntries
            if (currentTotal < DiscussionRules.MAX_MENTIONS_PER_CONTENT &&
                selectedTags.none { it.id == targetUser.id }
            ) {
                selectedTags = selectedTags + targetUser
            }
        }
        onInitialReplyTargetConsumed()
    }

    val onMentionTapped: (String) -> Unit = { mentionedUsername ->
        if (isSignedIn) {
            val clean = mentionedUsername.removePrefix("@").trim()
            val cur = draft.text
            val addition = if (cur.isNotEmpty() && !cur.endsWith(" ")) " @$clean " else "@$clean "
            if (DiscussionRules.validateContentMentions(cur + addition, selectedTags.size).totalEntries <= DiscussionRules.MAX_MENTIONS_PER_CONTENT) {
                draft = TextFieldValue(cur + addition, TextRange((cur + addition).length))
            }
        }
    }

    val draftValidation = remember(draft.text, selectedTags) {
        DiscussionRules.validateContentMentions(draft.text, explicitTagCount = selectedTags.size)
    }

    val draftEmojiIds = remember(draft.text) {
        CommunityEmojiCacheService.extractEmojiIds(draft.text)
    }

    val insertEmojiIntoDraft: (CommunityEmoji) -> Unit = { emoji ->
        val token = "[emoji:${emoji.id}] "
        val cur = draft.text
        val pos = draft.selection.end.coerceIn(0, cur.length)
        val prefix = cur.substring(0, pos)
        val needsSpace = prefix.isNotEmpty() && !prefix.endsWith(" ")
        val inserted = (if (needsSpace) " " else "") + token
        val nextText = prefix + inserted + cur.substring(pos)
        val nextCursor = pos + inserted.length
        draft = TextFieldValue(nextText, TextRange(nextCursor))
    }

    val sendAction: () -> Unit = {
        if (!isSubmitting && draftValidation.isValid) {
            val replyTarget = activeReplyTarget?.let {
                FlatReplyTo(
                    messageId = it.messageId,
                    senderName = it.senderName,
                    previewText = it.previewText.take(180),
                    stickerCode = it.stickerCode
                )
            }
            viewModel.postAnimeComment(
                animeId = animeId,
                animeTitle = animeTitle,
                animeCover = animeCover,
                content = draft.text,
                taggedUserIds = selectedTags.map { it.id },
                replyTo = replyTarget
            ) {
                draft = TextFieldValue("")
                selectedTags = emptyList()
                activeReplyTarget = null
                showMentionMenu = false
                isEmojiTrayOpen = false
                viewModel.clearMentionSuggestions()
            }
        }
    }

    val containerModifier = if (inModal) {
        modifier
            .fillMaxSize()
            .background(LivTrAkBg)
            .testTag("anime_discussion_section")
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(LivTrAkSurface)
            .border(1.dp, LivTrAkHairline, RoundedCornerShape(24.dp))
            .testTag("anime_discussion_section")
    }

    Column(modifier = containerModifier) {
        if (!inModal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(LivTrAkGold)
                    )
                    Text(
                        text = "LIVE CHAT",
                        style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 2.sp),
                        color = LivTrAkGold
                    )
                }
                Text(
                    text = "$totalComments ${if (totalComments == 1) "message" else "messages"}",
                    style = TabularNumSmall.copy(fontSize = 11.sp),
                    color = LivTrAkAsh
                )
            }
            HorizontalDivider(color = LivTrAkHairline, thickness = 1.dp)
        }

        AnimatedVisibility(visible = !feedbackError.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.error.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("discussion_error_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = feedbackError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearDiscussionFeedback() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = colors.error, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Chat Stream
        val chatListModifier = if (inModal) {
            Modifier
                .weight(1f)
                .fillMaxWidth()
        } else {
            Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 420.dp)
        }

        Box(modifier = chatListModifier) {
            when {
                isLoading && chronologicalComments.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LivTrAkGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
                chronologicalComments.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet. Say hello or drop a custom emoji below!",
                            style = MaterialTheme.typography.bodySmall,
                            color = LivTrAkAsh
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("livtrak_chat_messages_list")
                    ) {
                        if (hasMore) {
                            item(key = "load_older_messages") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = LivTrAkSurface2,
                                        border = BorderStroke(1.dp, LivTrAkHairline),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable(enabled = !isLoading) { viewModel.loadMoreAnimeDiscussion(animeId) }
                                    ) {
                                        Text(
                                            text = if (isLoading) "Loading older..." else "Load older messages",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LivTrAkGoldSoft,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(chronologicalComments, key = { it.id }) { comment ->
                            val isMine = currentUserId != null && currentUserId == comment.userId
                            val isHighlighted = activeHighlightTarget?.let { t ->
                                t.commentId == comment.id || t.replyId == comment.id
                            } == true
                            val isEditingThis = editingCommentId == comment.id
                            val memberFallback = memberById[comment.userId]
                            val resolvedAvatarKey = when {
                                isMine && !profile?.avatarKey.isNullOrBlank() -> profile!!.avatarKey
                                com.example.model.BuiltInAvatarFrame.extractKey(comment.avatarKey) != null -> comment.avatarKey
                                !memberFallback?.avatarKey.isNullOrBlank() -> memberFallback!!.avatarKey
                                else -> comment.avatarKey
                            }
                            val resolvedUsername = when {
                                isMine && !profile?.username.isNullOrBlank() -> profile!!.username
                                comment.username.isNotBlank() -> comment.username
                                !memberFallback?.username.isNullOrBlank() -> memberFallback!!.username
                                else -> "Member"
                            }
                            val displayComment = if (resolvedAvatarKey != comment.avatarKey || resolvedUsername != comment.username) {
                                comment.copy(avatarKey = resolvedAvatarKey, username = resolvedUsername)
                            } else comment

                            LivTrAkChatMessageItem(
                                comment = displayComment,
                                isMine = isMine,
                                isSignedIn = isSignedIn,
                                isHighlighted = isHighlighted,
                                isEditing = isEditingThis,
                                editingDraft = editingCommentDraft,
                                isSubmitting = isSubmitting,
                                emojiById = emojiById,
                                menuExpanded = openCommentMenuId == comment.id,
                                onOpenMenu = {
                                    if (isSignedIn) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        openCommentMenuId = comment.id
                                    }
                                },
                                onDismissMenu = { openCommentMenuId = null },
                                onReplyToMessage = {
                                    activeReplyTarget = ReplyComposerTarget(
                                        messageId = comment.id,
                                        senderName = comment.username,
                                        previewText = formatPlainPreviewWithEmojis(comment.content).take(180),
                                        stickerCode = extractFirstStickerCode(comment.content)
                                    )
                                },
                                onStartEditComment = {
                                    editingCommentId = comment.id
                                    editingCommentDraft = comment.content
                                },
                                onChangeEditCommentDraft = { editingCommentDraft = it },
                                onCancelEditComment = { editingCommentId = null },
                                onSaveEditComment = {
                                    viewModel.editAnimeComment(comment.id, editingCommentDraft) {
                                        editingCommentId = null
                                    }
                                },
                                onDeleteComment = { viewModel.deleteAnimeComment(comment.id) },
                                onJumpToQuoted = { targetMsgId ->
                                    if (targetMsgId.isNotBlank()) {
                                        activeHighlightTarget = NotificationJumpTarget(
                                            animeId = animeId,
                                            commentId = targetMsgId,
                                            threadId = null,
                                            replyId = null
                                        )
                                        scrolledToHighlightKey = null
                                        val jumpIdx = chronologicalComments.indexOfFirst { it.id == targetMsgId }
                                        if (jumpIdx >= 0) {
                                            coroutineScope.launch { listState.animateScrollToItem(jumpIdx) }
                                        }
                                    }
                                },
                                onMentionClick = onMentionTapped
                            )
                        }
                    }
                }
            }
        }

        // Bottom Docked Area: Mention Autocomplete + Reply Banner + Emoji Tray + Pill Input Bar
        if (isSignedIn) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LivTrAkSurface)
            ) {
                HorizontalDivider(color = LivTrAkHairline, thickness = 1.dp)

                AnimatedVisibility(visible = showMentionMenu && mentionSuggestions.isNotEmpty()) {
                    MentionAutocompletePanel(
                        suggestions = mentionSuggestions,
                        onSelectUser = { user ->
                            val active = DiscussionRules.detectActiveMentionQuery(draft.text, draft.selection.end)
                            if (active != null) {
                                val (stripped, cursor) = DiscussionRules.stripMentionTrigger(
                                    text = draft.text,
                                    atSignIndex = active.first,
                                    cursorPosition = draft.selection.end
                                )
                                draft = TextFieldValue(stripped, TextRange(cursor))
                            }
                            if (draftValidation.totalEntries < DiscussionRules.MAX_MENTIONS_PER_CONTENT &&
                                selectedTags.none { it.id == user.id }
                            ) {
                                selectedTags = selectedTags + user
                            }
                            showMentionMenu = false
                            viewModel.clearMentionSuggestions()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Active Reply Context Strip with Inline 20dp Circular Sticker Preview
                val replyTarget = activeReplyTarget
                AnimatedVisibility(visible = replyTarget != null) {
                    if (replyTarget != null) {
                        val stickerId = replyTarget.stickerCode ?: extractFirstStickerCode(replyTarget.previewText)
                        val stickerObj = stickerId?.let { emojiById[it.lowercase()] }
                        val remainingText = remember(replyTarget.previewText, stickerId) {
                            if (stickerId != null) stripStickerTokens(replyTarget.previewText) else replyTarget.previewText.trim()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LivTrAkGold.copy(alpha = 0.09f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .clip(CircleShape)
                                    .background(LivTrAkGold)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = null,
                                        tint = LivTrAkGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Replying to @${replyTarget.senderName}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = LivTrAkGoldSoft,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (stickerObj != null) {
                                        CachedCustomEmojiImage(emoji = stickerObj, size = 20.dp)
                                    } else if (stickerId != null) {
                                        EmojiShimmerPlaceholder(size = 20.dp)
                                    }
                                    if (remainingText.isNotBlank()) {
                                        Text(
                                            text = remainingText,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = LivTrAkAsh,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { activeReplyTarget = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel reply", tint = LivTrAkAsh, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }

                // Selected @tag chips + Custom Emoji draft preview chips
                if (selectedTags.isNotEmpty() || draftEmojiIds.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        selectedTags.forEachIndexed { idx, user ->
                            Surface(
                                shape = CircleShape,
                                color = LivTrAkGold.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, LivTrAkGold.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = LivTrAkGoldSoft
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove @${user.username}",
                                        tint = LivTrAkGold,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedTags = selectedTags.filterIndexed { i, _ -> i != idx }
                                            }
                                    )
                                }
                            }
                        }
                        draftEmojiIds.distinct().take(6).forEach { eid ->
                            val emoji = emojiById[eid]
                            if (emoji != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = LivTrAkSurface2,
                                    border = BorderStroke(1.dp, LivTrAkHairline)
                                ) {
                                    Box(modifier = Modifier.padding(3.dp)) {
                                        CachedCustomEmojiImage(
                                            emoji = emoji,
                                            size = 20.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Toggleable Custom Emoji Tray (Dynamic Top Emojis + Scrollable 6-col Grid)
                AnimatedVisibility(visible = isEmojiTrayOpen) {
                    LivTrAkEmojiTray(
                        emojis = communityEmojis,
                        onSelectEmoji = insertEmojiIntoDraft
                    )
                }

                // Pill Input Bar ("Message...") with Smiley Toggle + Circular Gold Send Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isEmojiTrayOpen) LivTrAkGold.copy(alpha = 0.18f) else LivTrAkSurface2,
                        border = BorderStroke(1.dp, if (isEmojiTrayOpen) LivTrAkGold.copy(alpha = 0.5f) else LivTrAkHairline),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                isEmojiTrayOpen = !isEmojiTrayOpen
                                if (isEmojiTrayOpen && communityEmojis.isEmpty()) {
                                    viewModel.loadCommunityEmojis(forceRefresh = true)
                                }
                            }
                            .testTag("chat_emoji_toggle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.SentimentSatisfiedAlt,
                                contentDescription = "Toggle custom emojis",
                                tint = if (isEmojiTrayOpen) LivTrAkGold else LivTrAkAsh,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = LivTrAkSurface2,
                        border = BorderStroke(1.dp, LivTrAkHairline),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (draft.text.isEmpty()) {
                                Text(
                                    text = if (replyTarget != null) "Reply to @${replyTarget.senderName}..." else "Message...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LivTrAkAsh2
                                )
                            }
                            BasicTextField(
                                value = draft,
                                onValueChange = { next ->
                                    draft = next
                                    val active = DiscussionRules.detectActiveMentionQuery(next.text, next.selection.end)
                                    if (active != null) {
                                        showMentionMenu = true
                                        viewModel.searchMentionUsers(active.second)
                                    } else if (showMentionMenu) {
                                        showMentionMenu = false
                                        viewModel.clearMentionSuggestions()
                                    }
                                },
                                enabled = !isSubmitting,
                                textStyle = TextStyle(
                                    color = LivTrAkIvory,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                cursorBrush = SolidColor(LivTrAkGold),
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("discussion_comment_input")
                            )
                        }
                    }

                    val canSend = !isSubmitting && draftValidation.isValid
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) LivTrAkGold else LivTrAkSurface2,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(enabled = canSend, onClick = sendAction)
                            .testTag("discussion_post_comment_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = LivTrAkBg,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    tint = if (canSend) LivTrAkBg else LivTrAkAsh2,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (draftValidation.totalEntries > DiscussionRules.MAX_MENTIONS_PER_CONTENT) {
                    Text(
                        text = draftValidation.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.error,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                }
            }
        } else {
            Surface(
                color = LivTrAkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sign in to chat, tag friends with @, and send custom emojis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LivTrAkAsh,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun LivTrAkChatMessageItem(
    comment: AnimeCommentItem,
    isMine: Boolean,
    isSignedIn: Boolean,
    isHighlighted: Boolean,
    isEditing: Boolean,
    editingDraft: String,
    isSubmitting: Boolean,
    emojiById: Map<String, CommunityEmoji>,
    menuExpanded: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onReplyToMessage: () -> Unit,
    onStartEditComment: () -> Unit,
    onChangeEditCommentDraft: (String) -> Unit,
    onCancelEditComment: () -> Unit,
    onSaveEditComment: () -> Unit,
    onDeleteComment: () -> Unit,
    onJumpToQuoted: (messageId: String) -> Unit,
    onMentionClick: (String) -> Unit
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "chat_msg_press_scale"
    )

    val bubbleShape = if (isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    }
    val bubbleBg = when {
        isHighlighted -> LivTrAkGold.copy(alpha = 0.24f)
        isMine -> Color(0x26E8C88F)
        else -> Color(0x0DFFFFFF)
    }
    val bubbleBorder = when {
        isHighlighted -> LivTrAkGold
        isMine -> Color(0x4DE8C88F)
        else -> LivTrAkHairline
    }

    val soloEmojiId = remember(comment.content) { extractSoloEmojiId(comment.content) }
    val soloEmojiObj = soloEmojiId?.let { emojiById[it] }
    val isSoloUnicode = remember(comment.content) { soloEmojiId == null && isSoloUnicodeEmoji(comment.content) }

    // Long-press (with scroll-slop cancel + subtle scale feedback) & desktop secondary-click handler
    val pressGestureModifier = if (isSignedIn && !isEditing) {
        Modifier.pointerInput(comment.id, isSignedIn) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (currentEvent.buttons.isSecondaryPressed) {
                    down.consume()
                    onOpenMenu()
                    return@awaitEachGesture
                }
                isPressed = true
                var resolved = false
                val touchSlop = viewConfiguration.touchSlop
                val holdJob = scope.launch {
                    delay(430L)
                    if (!resolved) {
                        resolved = true
                        isPressed = false
                        onOpenMenu()
                    }
                }
                try {
                    while (!resolved) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            resolved = true
                            break
                        }
                        if ((change.position - down.position).getDistance() > touchSlop) {
                            resolved = true
                            break
                        }
                    }
                } finally {
                    holdJob.cancel()
                    isPressed = false
                }
            }
        }
    } else Modifier

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Left avatar + frame for other users
            if (!isMine) {
                MemberAvatar(
                    avatarKey = comment.avatarKey,
                    avatarUrl = comment.avatarUrl,
                    username = comment.username,
                    size = 30.dp,
                    modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                )
            }

            Column(
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Header: Show username + timestamp cleanly for both other users and self
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
                ) {
                    val editedTag = if (comment.updatedAt.isNotBlank() && comment.updatedAt != comment.createdAt) " • edited" else ""
                    if (!isMine) {
                        Text(
                            text = comment.username,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                            color = LivTrAkGoldSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatCommunityTimestamp(comment.createdAt)}$editedTag",
                            style = TabularNumSmall.copy(fontSize = 10.sp),
                            color = LivTrAkAsh2
                        )
                    } else {
                        Text(
                            text = "${formatCommunityTimestamp(comment.createdAt)}$editedTag",
                            style = TabularNumSmall.copy(fontSize = 10.sp),
                            color = LivTrAkAsh2
                        )
                        Text(
                            text = comment.username,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                            color = LivTrAkGoldSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Solo circular sticker / unicode emoji WITHOUT comment cloud
                if (!isEditing && (soloEmojiId != null || isSoloUnicode)) {
                    Column(
                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .then(pressGestureModifier)
                            .testTag("discussion_comment_card_${comment.id}")
                    ) {
                        comment.replyTo?.let { quote ->
                            FlatQuotedReplyPreviewCard(
                                replyTo = quote,
                                emojiById = emojiById,
                                onClick = { onJumpToQuoted(quote.messageId) },
                                modifier = Modifier.widthIn(max = 260.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .then(
                                    if (isHighlighted) {
                                        Modifier.border(2.dp, LivTrAkGold, CircleShape).padding(3.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            when {
                                soloEmojiObj != null -> CachedCustomEmojiImage(emoji = soloEmojiObj, size = 90.dp)
                                soloEmojiId != null -> EmojiShimmerPlaceholder(size = 90.dp)
                                else -> Text(text = comment.content.trim(), fontSize = 48.sp, lineHeight = 54.sp)
                            }
                        }
                        if (comment.mentions.isNotEmpty()) {
                            ClickableMentionChipsRow(
                                mentions = comment.mentions,
                                onMentionClick = onMentionClick
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = bubbleShape,
                        color = bubbleBg,
                        border = BorderStroke(1.dp, bubbleBorder),
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .clip(bubbleShape)
                            .then(pressGestureModifier)
                            .testTag("discussion_comment_card_${comment.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editingDraft,
                                    onValueChange = onChangeEditCommentDraft,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                val editCheck = DiscussionRules.validateContentMentions(editingDraft)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AppPrimaryButton(
                                        text = if (isSubmitting) "Saving..." else "Save",
                                        enabled = !isSubmitting && editCheck.isValid,
                                        onClick = onSaveEditComment
                                    )
                                    TextButton(onClick = onCancelEditComment) {
                                        Text("Cancel", color = LivTrAkAsh)
                                    }
                                }
                            } else {
                                comment.replyTo?.let { quote ->
                                    FlatQuotedReplyPreviewCard(
                                        replyTo = quote,
                                        emojiById = emojiById,
                                        onClick = { onJumpToQuoted(quote.messageId) }
                                    )
                                }
                                RichChatMessageText(
                                    content = comment.content,
                                    emojiById = emojiById,
                                    mentionColor = LivTrAkGold,
                                    textColor = LivTrAkIvory,
                                    onMentionClick = onMentionClick
                                )
                                if (comment.mentions.isNotEmpty()) {
                                    ClickableMentionChipsRow(
                                        mentions = comment.mentions,
                                        onMentionClick = onMentionClick
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right avatar + frame for own messages so users clearly see their profile & frame in chat
            if (isMine) {
                MemberAvatar(
                    avatarKey = comment.avatarKey,
                    avatarUrl = comment.avatarUrl,
                    username = comment.username,
                    size = 30.dp,
                    modifier = Modifier.padding(top = 2.dp, start = 8.dp)
                )
            }
        }

        // Compact frosted-glass floating action menu on long-press or right-click
        Box(
            modifier = Modifier
                .align(if (isMine) Alignment.BottomEnd else Alignment.BottomStart)
        ) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onDismissMenu,
                shape = RoundedCornerShape(14.dp),
                containerColor = FrostedMenuBg,
                border = BorderStroke(1.dp, FrostedMenuBorder)
            ) {
                DropdownMenuItem(
                    text = { Text("Reply", color = LivTrAkIvory, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = LivTrAkGold, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        onDismissMenu()
                        onReplyToMessage()
                    }
                )
                if (isMine && !isEditing) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = LivTrAkIvory, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = LivTrAkAsh, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onDismissMenu()
                            onStartEditComment()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = colors.error, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onDismissMenu()
                            onDeleteComment()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FlatQuotedReplyPreviewCard(
    replyTo: FlatReplyTo,
    emojiById: Map<String, CommunityEmoji>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stickerId = remember(replyTo.stickerCode, replyTo.previewText) {
        replyTo.stickerCode?.takeIf { it.isNotBlank() }?.lowercase()
            ?: extractFirstStickerCode(replyTo.previewText)
    }
    val stickerObj = stickerId?.let { emojiById[it] }
    val cleanSnippet = remember(replyTo.previewText, stickerId) {
        if (stickerId != null) {
            stripStickerTokens(replyTo.previewText)
        } else {
            replyTo.previewText.trim()
        }
    }

    Surface(
        shape = RoundedCornerShape(9.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(1.dp, LivTrAkHairline),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .clickable(enabled = replyTo.messageId.isNotBlank(), onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(22.dp)
                    .clip(CircleShape)
                    .background(LivTrAkGold)
            )
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "@${replyTo.senderName}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = LivTrAkGoldSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (stickerObj != null) {
                        CachedCustomEmojiImage(emoji = stickerObj, size = 20.dp)
                    } else if (stickerId != null) {
                        EmojiShimmerPlaceholder(size = 20.dp)
                    }
                    if (cleanSnippet.isNotBlank()) {
                        Text(
                            text = cleanSnippet,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                            color = LivTrAkAsh,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (stickerId == null) {
                        Text(
                            text = "Original message",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                            color = LivTrAkAsh2,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClickableMentionChipsRow(
    mentions: List<com.example.model.ContentMention>,
    onMentionClick: (String) -> Unit
) {
    val uniqueTags = remember(mentions) { mentions.map { it.mentionedUsername }.distinct() }
    if (uniqueTags.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        uniqueTags.forEach { uname ->
            Surface(
                shape = CircleShape,
                color = LivTrAkGold.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, LivTrAkGold.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onMentionClick(uname) }
            ) {
                Text(
                    text = "@$uname",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                    color = LivTrAkGold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Renders chat text with inline clickable gold @mentions AND inline circular 28dp custom emojis
 * fetched & cached via Coil memory+disk cache using a single canonical Storage URL.
 */
@Composable
private fun RichChatMessageText(
    content: String,
    emojiById: Map<String, CommunityEmoji>,
    mentionColor: Color,
    textColor: Color,
    fontSizeSp: Float = 14f,
    onMentionClick: ((String) -> Unit)? = null
) {
    val combinedRegex = remember { Regex("""\[emoji:([a-zA-Z0-9_-]{1,64})]|(@[A-Za-z0-9_.-]{2,32})""") }
    val inlineMap = remember(content, emojiById) {
        val map = LinkedHashMap<String, InlineTextContent>()
        CommunityEmojiCacheService.extractEmojiIds(content).distinct().forEach { eid ->
            val emoji = emojiById[eid]
            if (emoji != null) {
                map["emoji_$eid"] = InlineTextContent(
                    placeholder = Placeholder(
                        width = 28.sp,
                        height = 28.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    CachedCustomEmojiImage(emoji = emoji, size = 26.dp)
                }
            }
        }
        map
    }

    val annotated = remember(content, emojiById, mentionColor, textColor) {
        buildAnnotatedString {
            var cursor = 0
            combinedRegex.findAll(content).forEach { match ->
                if (match.range.first > cursor) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(content.substring(cursor, match.range.first))
                    }
                }
                val emojiId = match.groups[1]?.value
                val mention = match.groups[2]?.value
                when {
                    emojiId != null && emojiById.containsKey(emojiId) -> {
                        appendInlineContent("emoji_$emojiId", "✨")
                    }
                    emojiId != null -> {
                        withStyle(SpanStyle(color = LivTrAkAsh, fontSize = 11.sp)) {
                            append(":$emojiId:")
                        }
                    }
                    mention != null -> {
                        val uname = mention.removePrefix("@")
                        pushStringAnnotation(tag = "MENTION", annotation = uname)
                        withStyle(SpanStyle(color = mentionColor, fontWeight = FontWeight.SemiBold)) {
                            append(mention)
                        }
                        pop()
                    }
                }
                cursor = match.range.last + 1
            }
            if (cursor < content.length) {
                withStyle(SpanStyle(color = textColor)) {
                    append(content.substring(cursor))
                }
            }
        }
    }

    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = annotated,
        inlineContent = inlineMap,
        onTextLayout = { textLayout = it },
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp + 6f).sp
        ),
        modifier = if (onMentionClick != null) {
            Modifier.pointerInput(annotated) {
                detectTapGestures { pos ->
                    val offset = textLayout?.getOffsetForPosition(pos) ?: return@detectTapGestures
                    annotated.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                        .firstOrNull()?.item?.let { onMentionClick(it) }
                }
            }
        } else {
            Modifier
        }
    )
}

/**
 * Toggleable LivTrAk Custom Emoji Tray:
 * - Dynamic "TOP EMOJIS" horizontal strip (sorted by real usage frequency, never hardcoded)
 * - Lazy 6-column circular emoji grid (only loads visible cells, cached in memory + disk)
 */
@Composable
private fun LivTrAkEmojiTray(
    emojis: List<CommunityEmoji>,
    onSelectEmoji: (CommunityEmoji) -> Unit
) {
    val topEmojis = remember(emojis) { emojis.take(8) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LivTrAkSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("livtrak_emoji_tray"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (emojis.isEmpty()) {
            Text(
                text = "Upload emojis to your Supabase \"community_emojis\" or \"community-emojis\" bucket to see them here.",
                style = MaterialTheme.typography.bodySmall,
                color = LivTrAkAsh,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            // Dynamic Top Emojis Strip (ordered by real usage frequency)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TOP EMOJIS",
                    style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.6.sp),
                    color = LivTrAkGold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(topEmojis, key = { "top_${it.id}" }) { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = LivTrAkSurface2,
                            border = BorderStroke(1.dp, LivTrAkGold.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { onSelectEmoji(emoji) }
                                .testTag("top_emoji_${emoji.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                CachedCustomEmojiImage(emoji = emoji, size = 34.dp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = LivTrAkHairline, thickness = 1.dp)

            // Scrollable Lazy 6-Column All Emojis Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                items(emojis, key = { "grid_${it.id}" }) { emoji ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LivTrAkSurface2.copy(alpha = 0.7f))
                            .clickable { onSelectEmoji(emoji) }
                            .testTag("grid_emoji_${emoji.id}")
                    ) {
                        CachedCustomEmojiImage(emoji = emoji, size = 36.dp)
                    }
                }
            }
        }
    }
}

/**
 * Lightweight cached custom emoji renderer:
 * - Uses stable memoryCacheKey & diskCacheKey ("livtrak_emoji_<id>") so the same image is never redownloaded
 * - Sends immutable Cache-Control header
 * - Shows a subtle animated circular shimmer while loading and a graceful fallback on error
 */
@Composable
internal fun CachedCustomEmojiImage(
    emoji: CommunityEmoji,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cacheKey = remember(emoji.id) { CommunityEmojiCacheService.cacheKeyFor(emoji.id) }
    val request = remember(emoji.id, emoji.url) {
        ImageRequest.Builder(context)
            .data(emoji.url)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .addHeader("Cache-Control", "public, max-age=31536000, immutable")
            .crossfade(120)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = emoji.id,
        contentScale = ContentScale.Crop,
        loading = { EmojiShimmerPlaceholder(size = size) },
        error = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(LivTrAkSurface2)
            ) {
                Text(
                    text = emoji.id.take(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = LivTrAkAsh2
                )
            }
        },
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}

@Composable
private fun EmojiShimmerPlaceholder(size: Dp) {
    val transition = rememberInfiniteTransition(label = "emoji_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_shimmer_alpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(LivTrAkGold.copy(alpha = alpha * 0.28f))
    )
}

@Composable
private fun MentionAutocompletePanel(
    suggestions: List<MentionableUser>,
    onSelectUser: (MentionableUser) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LivTrAkSurface2,
        border = BorderStroke(1.dp, LivTrAkGold.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("mention_autocomplete_panel")
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            suggestions.take(6).forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectUser(user) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("mention_suggestion_${user.username}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MemberAvatar(
                        avatarKey = user.avatarKey,
                        avatarUrl = user.avatarUrl,
                        username = user.username,
                        size = 26.dp
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = LivTrAkGoldSoft,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.AlternateEmail,
                        contentDescription = null,
                        tint = LivTrAkGold,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
