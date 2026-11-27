package com.example.service

import com.example.config.AppConfig
import com.example.model.AnimeCommentItem
import com.example.model.AnimeDiscussionPage
import com.example.model.AuthSession
import com.example.model.CommentReplyItem
import com.example.model.CommentReplyThread
import com.example.model.ContentMention
import com.example.model.FlatReplyTo
import com.example.model.MentionableUser
import com.example.model.NotificationsPage
import com.example.model.ThreadReplyCreationResult
import com.example.model.UnseenDiscussionAnime
import com.example.model.UnseenDiscussionSummary
import com.example.model.UserNotificationItem
import com.example.util.ErrorSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

sealed interface DiscussionRealtimeEvent {
    data class InsertComment(val row: AnimeCommentItem) : DiscussionRealtimeEvent
    data class UpdateMessage(
        val id: String,
        val content: String,
        val updatedAt: String,
        val replyTo: FlatReplyTo?
    ) : DiscussionRealtimeEvent
    data class DeleteMessage(val id: String) : DiscussionRealtimeEvent
}

data class UnreadNotificationSnapshot(
    val unreadIds: Set<String> = emptySet(),
    val unreadCountByAnime: Map<Int, Int> = emptyMap()
) {
    val unreadCount: Int get() = unreadIds.size
}

sealed interface NotificationRealtimeEvent {
    data class Insert(
        val id: String,
        val recipientUserId: String,
        val senderUserId: String,
        val animeId: Int,
        val isRead: Boolean,
        val readAt: String?,
        val previewText: String
    ) : NotificationRealtimeEvent

    data class Update(
        val id: String,
        val recipientUserId: String,
        val animeId: Int,
        val oldIsRead: Boolean?,
        val newIsRead: Boolean,
        val readAt: String?
    ) : NotificationRealtimeEvent
}

class SupabaseDiscussionService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun rpcRequest(rpcName: String, token: String, payload: JSONObject): Request {
        return Request.Builder()
            .url("${AppConfig.supabaseUrl}/rest/v1/rpc/$rpcName")
            .addHeader("apikey", AppConfig.supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    suspend fun fetchAnimeDiscussionPage(
        session: AuthSession,
        animeId: Int,
        limit: Int = 15,
        offset: Int = 0
    ): Result<AnimeDiscussionPage> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_anime_id", animeId)
                .put("p_limit", limit.coerceIn(1, 50))
                .put("p_offset", offset.coerceAtLeast(0))
            client.newCall(rpcRequest("get_anime_discussion_page", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to load comments right now.")))
                }
                val obj = JSONObject(body)
                val commentsArr = obj.optJSONArray("comments") ?: JSONArray()
                val rawComments = buildList {
                    for (i in 0 until commentsArr.length()) {
                        add(parseComment(commentsArr.getJSONObject(i)))
                    }
                }
                val flatComments = flattenMessagesList(rawComments)
                Result.success(
                    AnimeDiscussionPage(
                        animeId = obj.optInt("anime_id", animeId),
                        totalComments = obj.optInt("total_comments", flatComments.size),
                        hasMore = obj.optBoolean("has_more", false),
                        comments = flatComments
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to load comments right now.")))
        }
    }

    suspend fun searchMentionableUsers(
        session: AuthSession,
        query: String,
        limit: Int = 8
    ): Result<List<MentionableUser>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.success(emptyList())
        try {
            val payload = JSONObject()
                .put("p_query", query.trim())
                .put("p_limit", limit.coerceIn(1, 20))
            client.newCall(rpcRequest("search_mentionable_users", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful || !body.startsWith("[")) {
                    return@withContext Result.success(emptyList())
                }
                val arr = JSONArray(body)
                val users = buildList {
                    for (i in 0 until arr.length()) {
                        val u = arr.getJSONObject(i)
                        add(
                            MentionableUser(
                                id = u.optString("id"),
                                username = u.optString("username"),
                                avatarKey = u.optString("avatar_key", "avatar_1").ifBlank { "avatar_1" },
                                avatarUrl = u.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }
                            )
                        )
                    }
                }
                Result.success(users)
            }
        } catch (_: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun createAnimeComment(
        session: AuthSession,
        animeId: Int,
        content: String,
        animeTitle: String,
        animeCover: String,
        taggedUserIds: List<String> = emptyList(),
        replyTo: FlatReplyTo? = null
    ): Result<Pair<AnimeCommentItem, Int>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_anime_id", animeId)
                .put("p_content", content.trim())
                .put("p_anime_title", animeTitle.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("p_anime_cover", animeCover.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("p_tagged_user_ids", JSONArray(taggedUserIds))
            if (replyTo != null) {
                payload.put(
                    "p_reply_to",
                    JSONObject()
                        .put("messageId", replyTo.messageId)
                        .put("senderName", replyTo.senderName)
                        .put("previewText", replyTo.previewText.take(180))
                        .put("stickerCode", replyTo.stickerCode ?: JSONObject.NULL)
                )
            }
            client.newCall(rpcRequest("create_anime_comment", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to post message right now.")))
                }
                val obj = JSONObject(body)
                val comment = parseComment(obj.getJSONObject("comment"))
                val total = obj.optInt("total_comments", 1)
                Result.success(comment to total)
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to post message right now.")))
        }
    }

    suspend fun updateAnimeComment(
        session: AuthSession,
        commentId: String,
        content: String,
        taggedUserIds: List<String> = emptyList()
    ): Result<Triple<String, String, List<ContentMention>>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_comment_id", commentId)
                .put("p_content", content.trim())
                .put("p_tagged_user_ids", JSONArray(taggedUserIds))
            client.newCall(rpcRequest("update_anime_comment", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to update comment right now.")))
                }
                val obj = JSONObject(body)
                Result.success(
                    Triple(
                        obj.optString("content", content.trim()),
                        obj.optString("updated_at", ""),
                        parseMentions(obj.optJSONArray("mentions"))
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to update comment right now.")))
        }
    }

    suspend fun deleteAnimeComment(
        session: AuthSession,
        commentId: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject().put("p_comment_id", commentId)
            client.newCall(rpcRequest("delete_anime_comment", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to delete comment right now.")))
                }
                val obj = JSONObject(body)
                Result.success(obj.optInt("total_comments", 0))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to delete comment right now.")))
        }
    }

    suspend fun createThreadReply(
        session: AuthSession,
        commentId: String,
        targetUserId: String,
        content: String,
        taggedUserIds: List<String> = emptyList(),
        quotedReplyId: String? = null,
        quotedUsername: String? = null,
        quotedPreview: String? = null
    ): Result<ThreadReplyCreationResult> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_comment_id", commentId)
                .put("p_target_user_id", targetUserId)
                .put("p_content", content.trim())
                .put("p_tagged_user_ids", JSONArray(taggedUserIds))
                .put("p_quoted_reply_id", quotedReplyId?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("p_quoted_username", quotedUsername?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("p_quoted_preview", quotedPreview?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            client.newCall(rpcRequest("create_thread_reply", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to post reply right now.")))
                }
                val obj = JSONObject(body)
                val tObj = obj.getJSONObject("thread")
                val rObj = obj.getJSONObject("reply")
                val reply = parseReply(rObj)
                val thread = CommentReplyThread(
                    id = tObj.optString("id"),
                    commentId = tObj.optString("comment_id", commentId),
                    targetUserId = tObj.optString("target_user_id", targetUserId),
                    targetUsername = tObj.optString("target_username", "Member"),
                    targetAvatarKey = tObj.optString("target_avatar_key", "avatar_1").ifBlank { "avatar_1" },
                    targetAvatarUrl = tObj.optString("target_avatar_url").takeIf { it.isNotBlank() && it != "null" },
                    createdAt = tObj.optString("created_at", ""),
                    replies = listOf(reply)
                )
                Result.success(ThreadReplyCreationResult(thread = thread, reply = reply))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to post reply right now.")))
        }
    }

    suspend fun updateThreadReply(
        session: AuthSession,
        replyId: String,
        content: String,
        taggedUserIds: List<String> = emptyList()
    ): Result<Triple<String, String, List<ContentMention>>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_reply_id", replyId)
                .put("p_content", content.trim())
                .put("p_tagged_user_ids", JSONArray(taggedUserIds))
            client.newCall(rpcRequest("update_thread_reply", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to update reply right now.")))
                }
                val obj = JSONObject(body)
                Result.success(
                    Triple(
                        obj.optString("content", content.trim()),
                        obj.optString("updated_at", ""),
                        parseMentions(obj.optJSONArray("mentions"))
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to update reply right now.")))
        }
    }

    suspend fun deleteThreadReply(
        session: AuthSession,
        replyId: String
    ): Result<Pair<String, Boolean>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject().put("p_reply_id", replyId)
            client.newCall(rpcRequest("delete_thread_reply", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to delete reply right now.")))
                }
                val obj = JSONObject(body)
                Result.success(obj.optString("thread_id", "") to obj.optBoolean("thread_removed", false))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to delete reply right now.")))
        }
    }

    private fun isMissingReadAtColumnError(code: Int, body: String): Boolean {
        if (code != 400 && code != 404) return false
        val lower = body.lowercase()
        return lower.contains("read_at") && (
            lower.contains("pgrst204") ||
                lower.contains("42703") ||
                lower.contains("column") ||
                lower.contains("schema cache")
            )
    }

    private fun executeNotificationPatch(
        session: AuthSession,
        queryFilter: String,
        readAtIso: String
    ): Pair<Boolean, String> {
        val url = "${AppConfig.supabaseUrl}/rest/v1/user_notifications?$queryFilter"
        val withReadAt = JSONObject()
            .put("is_read", true)
            .put("read_at", readAtIso)
        val req1 = Request.Builder()
            .url(url)
            .addHeader("apikey", AppConfig.supabaseAnonKey)
            .addHeader("Authorization", "Bearer ${session.accessToken}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Prefer", "return=representation")
            .patch(withReadAt.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req1).execute().use { resp ->
            val body = resp.body?.string().orEmpty().trim()
            if (resp.isSuccessful) return true to body
            if (!isMissingReadAtColumnError(resp.code, body)) {
                return false to body
            }
        }
        // Live DB fallback when read_at migration has not been applied yet: still persist is_read = true
        val fallbackBody = JSONObject().put("is_read", true)
        val req2 = Request.Builder()
            .url(url)
            .addHeader("apikey", AppConfig.supabaseAnonKey)
            .addHeader("Authorization", "Bearer ${session.accessToken}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Prefer", "return=representation")
            .patch(fallbackBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req2).execute().use { resp ->
            val body = resp.body?.string().orEmpty().trim()
            return resp.isSuccessful to body
        }
    }

    /**
     * Authoritative unread calculation strictly scoped to
     * `recipient_user_id = authenticated user ID AND is_read = false` under existing RLS.
     */
    suspend fun fetchAuthoritativeUnreadSnapshot(
        session: AuthSession
    ): Result<UnreadNotificationSnapshot> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured || session.userId.isBlank() || session.accessToken.isBlank()) {
            return@withContext Result.success(UnreadNotificationSnapshot())
        }
        try {
            val encodedUid = URLEncoder.encode(session.userId, "UTF-8")
            val req = Request.Builder()
                .url("${AppConfig.supabaseUrl}/rest/v1/user_notifications?select=id,anime_id&recipient_user_id=eq.$encodedUid&is_read=eq.false&limit=1000")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Accept", "application/json")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful || !body.startsWith("[")) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to fetch unread notification count.")))
                }
                val arr = JSONArray(body)
                val ids = LinkedHashSet<String>(arr.length())
                val byAnime = HashMap<Int, Int>()
                for (i in 0 until arr.length()) {
                    val row = arr.optJSONObject(i) ?: continue
                    val id = row.optString("id").trim()
                    if (id.isNotBlank()) {
                        ids.add(id)
                        val aid = row.optInt("anime_id", 0)
                        if (aid > 0) {
                            byAnime[aid] = (byAnime[aid] ?: 0) + 1
                        }
                    }
                }
                Result.success(UnreadNotificationSnapshot(unreadIds = ids, unreadCountByAnime = byAnime))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to fetch unread notification count.")))
        }
    }

    suspend fun fetchUnreadNotificationCount(session: AuthSession): Result<Int> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured || session.userId.isBlank()) return@withContext Result.success(0)
        fetchAuthoritativeUnreadSnapshot(session).onSuccess { snap ->
            return@withContext Result.success(snap.unreadCount)
        }
        try {
            client.newCall(rpcRequest("get_unread_notification_count", session.accessToken, JSONObject())).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parseError(body, "Unable to load unread count.")))
                Result.success((body.toIntOrNull() ?: 0).coerceAtLeast(0))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to load unread count.")))
        }
    }

    suspend fun fetchNotifications(
        session: AuthSession,
        limit: Int = 30,
        offset: Int = 0
    ): Result<NotificationsPage> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud notifications are not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_limit", limit.coerceIn(1, 60))
                .put("p_offset", offset.coerceAtLeast(0))
            client.newCall(rpcRequest("get_user_notifications", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to load notifications.")))
                }
                val obj = JSONObject(body)
                val arr = obj.optJSONArray("notifications") ?: JSONArray()
                val items = buildList {
                    for (i in 0 until arr.length()) {
                        val n = arr.getJSONObject(i)
                        add(
                            UserNotificationItem(
                                id = n.optString("id"),
                                recipientUserId = n.optString("recipient_user_id"),
                                senderUserId = n.optString("sender_user_id"),
                                senderUsername = n.optString("sender_username", "Member"),
                                senderAvatarKey = n.optString("sender_avatar_key", "avatar_1").ifBlank { "avatar_1" },
                                senderAvatarUrl = n.optString("sender_avatar_url").takeIf { it.isNotBlank() && it != "null" },
                                notificationType = n.optString("notification_type", "mention_comment"),
                                communityId = n.optString("community_id", "anime_community").ifBlank { "anime_community" },
                                communityName = n.optString("community_name", "Community").let { if (it.isBlank() || it.equals("LivTrAk Community", ignoreCase = true)) "Community" else it },
                                animeId = n.optInt("anime_id", 0),
                                animeTitle = n.optString("anime_title", "Anime"),
                                animeCoverImage = n.optString("anime_cover_image", ""),
                                commentId = n.optString("comment_id", ""),
                                threadId = n.optString("thread_id").takeIf { it.isNotBlank() && it != "null" },
                                replyId = n.optString("reply_id").takeIf { it.isNotBlank() && it != "null" },
                                previewText = n.optString("preview_text", ""),
                                isRead = n.optBoolean("is_read", false),
                                readAt = n.optString("read_at").takeIf { it.isNotBlank() && it != "null" },
                                createdAt = n.optString("created_at", "")
                            )
                        )
                    }
                }
                val exactSnap = fetchAuthoritativeUnreadSnapshot(session).getOrNull()
                Result.success(
                    NotificationsPage(
                        unreadCount = exactSnap?.unreadCount ?: obj.optInt("unread_count", items.count { !it.isRead }).coerceAtLeast(0),
                        totalCount = obj.optInt("total_count", items.size),
                        hasMore = obj.optBoolean("has_more", false),
                        notifications = items
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to load notifications.")))
        }
    }

    suspend fun markNotificationRead(
        session: AuthSession,
        notificationId: String,
        readAtIso: String = Instant.now().toString()
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured || session.userId.isBlank() || notificationId.isBlank()) {
            return@withContext Result.success(0)
        }
        try {
            val encodedId = URLEncoder.encode(notificationId, "UTF-8")
            val encodedUid = URLEncoder.encode(session.userId, "UTF-8")
            val (patchOk, patchBody) = executeNotificationPatch(
                session = session,
                queryFilter = "id=eq.$encodedId&recipient_user_id=eq.$encodedUid&is_read=eq.false",
                readAtIso = readAtIso
            )
            if (patchOk) {
                val exactCount = fetchAuthoritativeUnreadSnapshot(session).getOrNull()?.unreadCount
                if (exactCount != null) {
                    return@withContext Result.success(exactCount)
                }
            }
            // Fallback to RPC if direct PATCH failed or exact count query failed
            val payload = JSONObject().put("p_notification_id", notificationId)
            client.newCall(rpcRequest("mark_notification_read", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(parseError(if (patchBody.isNotBlank()) patchBody else body, "Unable to mark notification as read."))
                    )
                }
                val obj = JSONObject(body)
                Result.success(obj.optInt("unread_count", 0).coerceAtLeast(0))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to mark notification as read.")))
        }
    }

    suspend fun markAllNotificationsRead(
        session: AuthSession,
        readAtIso: String = Instant.now().toString()
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured || session.userId.isBlank()) return@withContext Result.success(0)
        try {
            val encodedUid = URLEncoder.encode(session.userId, "UTF-8")
            val (patchOk, patchBody) = executeNotificationPatch(
                session = session,
                queryFilter = "recipient_user_id=eq.$encodedUid&is_read=eq.false",
                readAtIso = readAtIso
            )
            if (patchOk) {
                val exactCount = fetchAuthoritativeUnreadSnapshot(session).getOrNull()?.unreadCount ?: 0
                return@withContext Result.success(exactCount)
            }
            client.newCall(rpcRequest("mark_all_notifications_read", session.accessToken, JSONObject())).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(parseError(if (patchBody.isNotBlank()) patchBody else body, "Unable to mark notifications as read."))
                    )
                }
                Result.success(0)
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to mark notifications as read.")))
        }
    }

    suspend fun fetchUnseenDiscussionAnime(session: AuthSession): Result<UnseenDiscussionSummary> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured || session.userId.isBlank()) return@withContext Result.success(UnseenDiscussionSummary())
        try {
            val exactSnap = fetchAuthoritativeUnreadSnapshot(session).getOrNull()
            if (exactSnap != null && exactSnap.unreadCount == 0) {
                return@withContext Result.success(UnseenDiscussionSummary(totalUnseen = 0, anime = emptyList()))
            }
            client.newCall(rpcRequest("get_unseen_discussion_anime", session.accessToken, JSONObject())).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful || !body.startsWith("{")) {
                    return@withContext Result.success(
                        UnseenDiscussionSummary(
                            totalUnseen = exactSnap?.unreadCount ?: 0,
                            anime = emptyList()
                        )
                    )
                }
                val obj = JSONObject(body)
                val arr = obj.optJSONArray("anime") ?: JSONArray()
                val items = buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val aid = item.optInt("anime_id", 0)
                        val exactForAnime = exactSnap?.unreadCountByAnime?.get(aid)
                        if (exactSnap != null && (exactForAnime == null || exactForAnime <= 0)) {
                            continue
                        }
                        add(
                            UnseenDiscussionAnime(
                                animeId = aid,
                                animeTitle = item.optString("anime_title", "Anime"),
                                animeCoverImage = item.optString("anime_cover_image", ""),
                                communityId = item.optString("community_id", "anime_community").ifBlank { "anime_community" },
                                communityName = item.optString("community_name", "Community").let { if (it.isBlank() || it.equals("LivTrAk Community", ignoreCase = true)) "Community" else it },
                                unseenCount = exactForAnime ?: item.optInt("unseen_count", 1),
                                latestCommentId = item.optString("latest_comment_id", ""),
                                latestThreadId = item.optString("latest_thread_id").takeIf { it.isNotBlank() && it != "null" },
                                latestReplyId = item.optString("latest_reply_id").takeIf { it.isNotBlank() && it != "null" },
                                latestSenderUsername = item.optString("latest_sender_username", "Member"),
                                latestNotificationType = item.optString("latest_notification_type", "mention_comment"),
                                latestPreview = item.optString("latest_preview", ""),
                                latestCreatedAt = item.optString("latest_created_at", "")
                            )
                        )
                    }
                }
                val authoritativeTotal = exactSnap?.unreadCount ?: obj.optInt("total_unseen", items.sumOf { it.unseenCount }).coerceAtLeast(0)
                Result.success(
                    UnseenDiscussionSummary(
                        totalUnseen = authoritativeTotal,
                        anime = if (authoritativeTotal == 0) emptyList() else items
                    )
                )
            }
        } catch (_: Exception) {
            Result.success(UnseenDiscussionSummary())
        }
    }

    suspend fun markAnimeNotificationsRead(
        session: AuthSession,
        animeId: Int,
        readAtIso: String = Instant.now().toString()
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured || session.userId.isBlank() || animeId <= 0) return@withContext Result.success(0)
        try {
            val encodedUid = URLEncoder.encode(session.userId, "UTF-8")
            val (patchOk, patchBody) = executeNotificationPatch(
                session = session,
                queryFilter = "recipient_user_id=eq.$encodedUid&anime_id=eq.$animeId&is_read=eq.false",
                readAtIso = readAtIso
            )
            if (patchOk) {
                val exactCount = fetchAuthoritativeUnreadSnapshot(session).getOrNull()?.unreadCount
                if (exactCount != null) {
                    return@withContext Result.success(exactCount)
                }
            }
            val payload = JSONObject().put("p_anime_id", animeId)
            client.newCall(rpcRequest("mark_anime_notifications_read", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(parseError(if (patchBody.isNotBlank()) patchBody else body, "Unable to update read status."))
                    )
                }
                val obj = JSONObject(body)
                Result.success(obj.optInt("unread_count", 0).coerceAtLeast(0))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to update read status.")))
        }
    }

    suspend fun fetchUserProfileSnippet(session: AuthSession, userId: String): MentionableUser? = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured || userId.isBlank()) return@withContext null
        try {
            val encodedId = URLEncoder.encode(userId, "UTF-8")
            val req = Request.Builder()
                .url("${AppConfig.supabaseUrl}/rest/v1/profiles?id=eq.$encodedId&select=*&limit=1")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful || !body.startsWith("[")) return@withContext null
                val arr = JSONArray(body)
                if (arr.length() == 0) return@withContext null
                val u = arr.getJSONObject(0)
                val rawAvatarKey = u.optString("avatar_key").takeIf { it.isNotBlank() && it != "null" }
                val rawAvatarFrame = u.optString("avatar_frame").takeIf { it.isNotBlank() && it != "null" }
                val rawAvatarUrl = u.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }
                MentionableUser(
                    id = u.optString("id", userId),
                    username = u.optString("username", "Member").ifBlank { "Member" },
                    avatarKey = com.example.model.BuiltInAvatarFrame.resolveCompositeAvatarKey(
                        rawAvatarKey = rawAvatarKey,
                        rawAvatarFrame = rawAvatarFrame,
                        rawAvatarUrl = rawAvatarUrl
                    ),
                    avatarUrl = rawAvatarUrl
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun subscribeToAnimeChatRealtime(
        session: AuthSession,
        animeId: Int,
        onEvent: (DiscussionRealtimeEvent) -> Unit
    ): WebSocket? {
        if (!AppConfig.isSupabaseConfigured || animeId <= 0 || session.accessToken.isBlank()) return null
        val wsBase = AppConfig.supabaseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        val encodedKey = URLEncoder.encode(AppConfig.supabaseAnonKey, "UTF-8")
        val wsUrl = "$wsBase/realtime/v1/websocket?apikey=$encodedKey&vsn=1.0.0"
        val req = Request.Builder().url(wsUrl).build()
        val refCounter = AtomicInteger(1)
        val topic = "realtime:public:anime_comments:$animeId"
        var heartbeatTimer: Timer? = null

        return client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val joinMsg = JSONObject()
                    .put("topic", topic)
                    .put("event", "phx_join")
                    .put(
                        "payload",
                        JSONObject()
                            .put(
                                "config",
                                JSONObject()
                                    .put("broadcast", JSONObject().put("self", false))
                                    .put("presence", JSONObject().put("key", ""))
                                    .put(
                                        "postgres_changes",
                                        JSONArray()
                                            .put(
                                                JSONObject()
                                                    .put("event", "*")
                                                    .put("schema", "public")
                                                    .put("table", "anime_comments")
                                                    .put("filter", "anime_id=eq.$animeId")
                                            )
                                            .put(
                                                JSONObject()
                                                    .put("event", "UPDATE")
                                                    .put("schema", "public")
                                                    .put("table", "comment_replies")
                                            )
                                            .put(
                                                JSONObject()
                                                    .put("event", "DELETE")
                                                    .put("schema", "public")
                                                    .put("table", "comment_replies")
                                            )
                                    )
                            )
                            .put("access_token", session.accessToken)
                    )
                    .put("ref", refCounter.getAndIncrement().toString())
                webSocket.send(joinMsg.toString())

                val tokenMsg = JSONObject()
                    .put("topic", topic)
                    .put("event", "access_token")
                    .put("payload", JSONObject().put("access_token", session.accessToken))
                    .put("ref", refCounter.getAndIncrement().toString())
                webSocket.send(tokenMsg.toString())

                heartbeatTimer = Timer("supabase-chat-hb-$animeId", true).apply {
                    scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            val hb = JSONObject()
                                .put("topic", "phoenix")
                                .put("event", "heartbeat")
                                .put("payload", JSONObject())
                                .put("ref", refCounter.getAndIncrement().toString())
                            runCatching { webSocket.send(hb.toString()) }
                        }
                    }, 25_000L, 25_000L)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val msg = JSONObject(text)
                    if (msg.optString("event") != "postgres_changes") return
                    val data = msg.optJSONObject("payload")?.optJSONObject("data") ?: return
                    val evType = data.optString("type").uppercase()
                    val table = data.optString("table")
                    val rec = data.optJSONObject("record")
                    val oldRec = data.optJSONObject("old_record")

                    when {
                        evType == "INSERT" && table == "anime_comments" && rec != null -> {
                            val recId = rec.optString("id")
                            val recAnimeId = rec.optInt("anime_id", 0)
                            if (recId.isNotBlank() && recAnimeId == animeId) {
                                val item = AnimeCommentItem(
                                    id = recId,
                                    animeId = recAnimeId,
                                    userId = rec.optString("user_id"),
                                    username = "Member",
                                    avatarKey = "avatar_1",
                                    avatarUrl = null,
                                    content = rec.optString("content", ""),
                                    createdAt = rec.optString("created_at", ""),
                                    updatedAt = rec.optString("updated_at", ""),
                                    replyTo = parseReplyTo(rec.opt("reply_to")),
                                    sourceType = "comment"
                                )
                                onEvent(DiscussionRealtimeEvent.InsertComment(item))
                            }
                        }
                        evType == "UPDATE" && rec != null -> {
                            val recId = rec.optString("id")
                            if (recId.isNotBlank()) {
                                onEvent(
                                    DiscussionRealtimeEvent.UpdateMessage(
                                        id = recId,
                                        content = rec.optString("content", ""),
                                        updatedAt = rec.optString("updated_at", ""),
                                        replyTo = parseReplyTo(rec.opt("reply_to"))
                                    )
                                )
                            }
                        }
                        evType == "DELETE" -> {
                            val delId = oldRec?.optString("id")?.takeIf { it.isNotBlank() }
                                ?: rec?.optString("id")?.takeIf { it.isNotBlank() }
                            if (!delId.isNullOrBlank()) {
                                onEvent(DiscussionRealtimeEvent.DeleteMessage(delId))
                            }
                        }
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                heartbeatTimer?.cancel()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                heartbeatTimer?.cancel()
            }
        })
    }

    /**
     * Subscribes to Supabase Realtime INSERT and UPDATE events on `public.user_notifications`
     * strictly filtered to `recipient_user_id=eq.${session.userId}` with the user's JWT.
     */
    fun subscribeToUserNotificationsRealtime(
        session: AuthSession,
        onEvent: (NotificationRealtimeEvent) -> Unit,
        onConnected: () -> Unit = {},
        onDisconnected: () -> Unit = {}
    ): WebSocket? {
        if (!AppConfig.isSupabaseConfigured || session.userId.isBlank() || session.accessToken.isBlank()) return null
        val wsBase = AppConfig.supabaseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        val encodedKey = URLEncoder.encode(AppConfig.supabaseAnonKey, "UTF-8")
        val wsUrl = "$wsBase/realtime/v1/websocket?apikey=$encodedKey&vsn=1.0.0"
        val req = Request.Builder().url(wsUrl).build()
        val refCounter = AtomicInteger(1)
        val topic = "realtime:public:user_notifications:${session.userId}"
        val filter = "recipient_user_id=eq.${session.userId}"
        var heartbeatTimer: Timer? = null

        return client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val joinMsg = JSONObject()
                    .put("topic", topic)
                    .put("event", "phx_join")
                    .put(
                        "payload",
                        JSONObject()
                            .put(
                                "config",
                                JSONObject()
                                    .put("broadcast", JSONObject().put("self", false))
                                    .put("presence", JSONObject().put("key", ""))
                                    .put(
                                        "postgres_changes",
                                        JSONArray()
                                            .put(
                                                JSONObject()
                                                    .put("event", "INSERT")
                                                    .put("schema", "public")
                                                    .put("table", "user_notifications")
                                                    .put("filter", filter)
                                            )
                                            .put(
                                                JSONObject()
                                                    .put("event", "UPDATE")
                                                    .put("schema", "public")
                                                    .put("table", "user_notifications")
                                                    .put("filter", filter)
                                            )
                                    )
                            )
                            .put("access_token", session.accessToken)
                    )
                    .put("ref", refCounter.getAndIncrement().toString())
                webSocket.send(joinMsg.toString())

                val tokenMsg = JSONObject()
                    .put("topic", topic)
                    .put("event", "access_token")
                    .put("payload", JSONObject().put("access_token", session.accessToken))
                    .put("ref", refCounter.getAndIncrement().toString())
                webSocket.send(tokenMsg.toString())

                heartbeatTimer = Timer("supabase-notif-hb", true).apply {
                    scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            val hb = JSONObject()
                                .put("topic", "phoenix")
                                .put("event", "heartbeat")
                                .put("payload", JSONObject())
                                .put("ref", refCounter.getAndIncrement().toString())
                            runCatching { webSocket.send(hb.toString()) }
                        }
                    }, 25_000L, 25_000L)
                }
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val msg = JSONObject(text)
                    if (msg.optString("event") != "postgres_changes") return
                    val data = msg.optJSONObject("payload")?.optJSONObject("data") ?: return
                    val table = data.optString("table")
                    if (table != "user_notifications") return
                    val evType = data.optString("type").uppercase()
                    val rec = data.optJSONObject("record") ?: return
                    val oldRec = data.optJSONObject("old_record")
                    val id = rec.optString("id").trim()
                    if (id.isBlank()) return
                    val recipientId = rec.optString("recipient_user_id").trim()
                    if (recipientId != session.userId) return
                    val readAt = rec.optString("read_at").takeIf { it.isNotBlank() && it != "null" }

                    when (evType) {
                        "INSERT" -> {
                            onEvent(
                                NotificationRealtimeEvent.Insert(
                                    id = id,
                                    recipientUserId = recipientId,
                                    senderUserId = rec.optString("sender_user_id"),
                                    animeId = rec.optInt("anime_id", 0),
                                    isRead = rec.optBoolean("is_read", false),
                                    readAt = readAt,
                                    previewText = rec.optString("preview_text", "")
                                )
                            )
                        }
                        "UPDATE" -> {
                            val oldIsRead: Boolean? = if (oldRec != null && oldRec.has("is_read") && !oldRec.isNull("is_read")) {
                                oldRec.optBoolean("is_read")
                            } else {
                                null
                            }
                            onEvent(
                                NotificationRealtimeEvent.Update(
                                    id = id,
                                    recipientUserId = recipientId,
                                    animeId = rec.optInt("anime_id", 0),
                                    oldIsRead = oldIsRead,
                                    newIsRead = rec.optBoolean("is_read", false),
                                    readAt = readAt
                                )
                            )
                        }
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                heartbeatTimer?.cancel()
                onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                heartbeatTimer?.cancel()
                onDisconnected()
            }
        })
    }

    private fun extractFirstStickerCode(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val bracket = Regex("""\[emoji:([a-zA-Z0-9_-]{1,64})]""", RegexOption.IGNORE_CASE).find(text)
        if (bracket != null) return bracket.groupValues[1].lowercase()
        val colon = Regex(""":([a-zA-Z0-9][a-zA-Z0-9_-]{0,63}):""").find(text)
        return colon?.groupValues?.getOrNull(1)?.lowercase()
    }

    private fun parseReplyTo(raw: Any?): FlatReplyTo? {
        val obj = when (raw) {
            is JSONObject -> raw
            is String -> if (raw.trim().startsWith("{")) runCatching { JSONObject(raw) }.getOrNull() else null
            else -> null
        } ?: return null
        val msgId = obj.optString("messageId").ifBlank { obj.optString("message_id") }
        val senderName = obj.optString("senderName").ifBlank { obj.optString("sender_name") }
        val previewText = obj.optString("previewText").ifBlank { obj.optString("preview_text") }
        val explicitSticker = obj.optString("stickerCode")
            .ifBlank { obj.optString("sticker_code") }
            .takeIf { it.isNotBlank() && it != "null" }
            ?.lowercase()
        val sticker = explicitSticker ?: extractFirstStickerCode(previewText)
        if (msgId.isBlank() && senderName.isBlank() && previewText.isBlank() && sticker.isNullOrBlank()) {
            return null
        }
        return FlatReplyTo(
            messageId = msgId,
            senderName = senderName.ifBlank { "Member" },
            previewText = previewText,
            stickerCode = sticker
        )
    }

    private fun flattenMessagesList(raw: List<AnimeCommentItem>): List<AnimeCommentItem> {
        if (raw.isEmpty()) return emptyList()
        val byId = LinkedHashMap<String, AnimeCommentItem>()
        val repliesById = HashMap<String, CommentReplyItem>()
        raw.forEach { c ->
            c.threads.forEach { th ->
                th.replies.forEach { r -> repliesById[r.id] = r }
            }
        }
        raw.forEach { c ->
            val topLevel = if (c.threads.isEmpty()) c else c.copy(threads = emptyList())
            byId[topLevel.id] = topLevel
            c.threads.forEach { th ->
                th.replies.forEach { r ->
                    val refReplyId = r.quotedReplyId?.takeIf { it.isNotBlank() }
                        ?: r.parentCommentId?.takeIf { it.isNotBlank() && it != c.id }
                    val matchedReply = refReplyId?.let { repliesById[it] }
                    val targetId = refReplyId ?: (r.quotedCommentId?.takeIf { it.isNotBlank() } ?: c.id)
                    val targetSender = if (refReplyId != null) {
                        matchedReply?.username?.takeIf { it.isNotBlank() }
                            ?: r.quotedUsername?.takeIf { it.isNotBlank() }
                            ?: "Member"
                    } else {
                        r.quotedUsername?.takeIf { it.isNotBlank() } ?: c.username
                    }
                    val rawPreview = if (refReplyId != null) {
                        r.quotedPreview?.takeIf { it.isNotBlank() }
                            ?: matchedReply?.content?.takeIf { it.isNotBlank() }
                            ?: ""
                    } else {
                        r.quotedPreview?.takeIf { it.isNotBlank() } ?: c.content
                    }
                    byId[r.id] = AnimeCommentItem(
                        id = r.id,
                        animeId = c.animeId,
                        userId = r.userId,
                        username = r.username,
                        avatarKey = r.avatarKey,
                        avatarUrl = r.avatarUrl,
                        content = r.content,
                        createdAt = r.createdAt,
                        updatedAt = r.updatedAt,
                        mentions = r.mentions,
                        threads = emptyList(),
                        replyTo = FlatReplyTo(
                            messageId = targetId,
                            senderName = targetSender,
                            previewText = rawPreview.take(180),
                            stickerCode = extractFirstStickerCode(rawPreview)
                        ),
                        sourceType = "reply",
                        threadId = r.threadId.ifBlank { th.id }
                    )
                }
            }
        }
        return byId.values.sortedByDescending { it.createdAt }
    }

    private fun parseComment(obj: JSONObject): AnimeCommentItem {
        val threadsArr = obj.optJSONArray("threads") ?: JSONArray()
        val threads = buildList {
            for (i in 0 until threadsArr.length()) {
                val t = threadsArr.getJSONObject(i)
                val repliesArr = t.optJSONArray("replies") ?: JSONArray()
                val replies = buildList {
                    for (j in 0 until repliesArr.length()) {
                        add(parseReply(repliesArr.getJSONObject(j)))
                    }
                }
                add(
                    CommentReplyThread(
                        id = t.optString("id"),
                        commentId = t.optString("comment_id", obj.optString("id")),
                        targetUserId = t.optString("target_user_id"),
                        targetUsername = t.optString("target_username", "Member"),
                        targetAvatarKey = t.optString("target_avatar_key", "avatar_1").ifBlank { "avatar_1" },
                        targetAvatarUrl = t.optString("target_avatar_url").takeIf { it.isNotBlank() && it != "null" },
                        createdAt = t.optString("created_at", ""),
                        replies = replies
                    )
                )
            }
        }
        return AnimeCommentItem(
            id = obj.optString("id"),
            animeId = obj.optInt("anime_id", 0),
            userId = obj.optString("user_id"),
            username = obj.optString("username", "Member"),
            avatarKey = obj.optString("avatar_key", "avatar_1").ifBlank { "avatar_1" },
            avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" },
            content = obj.optString("content", ""),
            createdAt = obj.optString("created_at", ""),
            updatedAt = obj.optString("updated_at", ""),
            mentions = parseMentions(obj.optJSONArray("mentions")),
            threads = threads,
            replyTo = parseReplyTo(obj.opt("reply_to")),
            sourceType = obj.optString("source_type", "comment").ifBlank { "comment" },
            threadId = obj.optString("thread_id").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    private fun parseReply(obj: JSONObject): CommentReplyItem {
        return CommentReplyItem(
            id = obj.optString("id"),
            threadId = obj.optString("thread_id"),
            userId = obj.optString("user_id"),
            username = obj.optString("username", "Member"),
            avatarKey = obj.optString("avatar_key", "avatar_1").ifBlank { "avatar_1" },
            avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" },
            content = obj.optString("content", ""),
            parentCommentId = obj.optString("parent_comment_id").takeIf { it.isNotBlank() && it != "null" },
            quotedCommentId = obj.optString("quoted_comment_id").takeIf { it.isNotBlank() && it != "null" },
            quotedReplyId = obj.optString("quoted_reply_id").takeIf { it.isNotBlank() && it != "null" },
            quotedUsername = obj.optString("quoted_username").takeIf { it.isNotBlank() && it != "null" },
            quotedPreview = obj.optString("quoted_preview").takeIf { it.isNotBlank() && it != "null" },
            createdAt = obj.optString("created_at", ""),
            updatedAt = obj.optString("updated_at", ""),
            mentions = parseMentions(obj.optJSONArray("mentions"))
        )
    }

    private fun parseMentions(arr: JSONArray?): List<ContentMention> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                add(
                    ContentMention(
                        id = m.optString("id", ""),
                        mentionedUserId = m.optString("mentioned_user_id", ""),
                        mentionedUsername = m.optString("mentioned_username", ""),
                        occurrencePosition = m.optInt("occurrence_position", 0)
                    )
                )
            }
        }
    }

    private fun parseError(body: String, fallback: String): String {
        val raw = runCatching {
            val obj = JSONObject(body)
            obj.optString("message").takeIf { it.isNotBlank() }
                ?: obj.optString("hint").takeIf { it.isNotBlank() }
                ?: fallback
        }.getOrDefault(fallback)
        if (raw.contains("PGRST202", ignoreCase = true) || raw.contains("Could not find the function", ignoreCase = true)) {
            return "Live chat SQL update is pending in Supabase. Please run the 20261124 migration in SQL Editor."
        }
        return ErrorSanitizer.sanitize(raw, fallback)
    }
}
