package com.example.service

import com.example.config.AppConfig
import com.example.data.local.AnimeEntity
import com.example.model.AnimeCollection
import com.example.model.AuthSession
import com.example.model.BuiltInAvatarFrame
import com.example.model.CommunityActivityItem
import com.example.model.CommunityCapacity
import com.example.model.CommunityMember
import com.example.model.CommunityReview
import com.example.model.DatabaseStorageStats
import com.example.model.InvitationCode
import com.example.model.MemberStatus
import com.example.model.PrivacySettings
import com.example.model.TableStorageStat
import com.example.model.UserProfile
import com.example.model.VisibilityLevel
import com.example.model.WatchStatus
import com.example.util.ErrorSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class UsernameCheckResult(
    val available: Boolean,
    val reason: String? = null
)

data class EmailCooldownResult(
    val allowed: Boolean,
    val retryAfterSeconds: Long = 0L
)

data class AccountDeletionResult(
    val deletedData: Boolean,
    val deletedAuthUser: Boolean
)

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isMandatory: Boolean = false,
    val platform: String = "android"
) {
    companion object {
        fun isRemoteNewer(
            currentVersionCode: Int,
            currentVersionName: String,
            remoteVersionCode: Int,
            remoteVersionName: String
        ): Boolean {
            if (remoteVersionCode > currentVersionCode) return true
            if (remoteVersionCode < currentVersionCode) return false
            return compareSemVer(remoteVersionName, currentVersionName) > 0
        }

        fun compareSemVer(v1: String, v2: String): Int {
            val p1 = v1.trim().removePrefix("v").removePrefix("V")
                .substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
            val p2 = v2.trim().removePrefix("v").removePrefix("V")
                .substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(p1.size, p2.size)
            for (i in 0 until maxLen) {
                val a = p1.getOrElse(i) { 0 }
                val b = p2.getOrElse(i) { 0 }
                if (a != b) return a.compareTo(b)
            }
            return 0
        }
    }
}

class SupabaseDatabaseService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Session-level catalog hash cache prevents re-uploading identical metadata on rapid +1 episode clicks
    private val syncedCatalogHashes = ConcurrentHashMap<Int, Int>()
    @Volatile
    private var supportsUserSeasonColumn = true

    private fun requestBuilder(path: String, token: String): Request.Builder {
        return Request.Builder()
            .url("${AppConfig.supabaseUrl}$path")
            .addHeader("apikey", AppConfig.supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
    }

    /**
     * Checks Supabase `get_latest_app_release` RPC (with fallback to `app_releases` table)
     * to determine if a newer app version is available.
     */
    suspend fun checkAppUpdate(
        currentVersionCode: Int,
        currentVersionName: String,
        platform: String = "android"
    ): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.success(null)
        }
        try {
            val payload = JSONObject().put("p_platform", platform.lowercase())
            val rpcReq = requestBuilder("/rest/v1/rpc/get_latest_app_release", AppConfig.supabaseAnonKey)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(rpcReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (resp.isSuccessful && body.startsWith("{")) {
                    val obj = JSONObject(body)
                    if (!obj.optBoolean("available", false)) {
                        return@withContext Result.success(null)
                    }
                    val remoteCode = obj.optInt("version_code", currentVersionCode)
                    val remoteName = obj.optString("version_name", currentVersionName).ifBlank { currentVersionName }
                    if (AppUpdateInfo.isRemoteNewer(currentVersionCode, currentVersionName, remoteCode, remoteName)) {
                        return@withContext Result.success(
                            AppUpdateInfo(
                                versionCode = remoteCode,
                                versionName = remoteName,
                                releaseNotes = obj.optString("release_notes", "").trim(),
                                downloadUrl = obj.optString("download_url", "").trim(),
                                isMandatory = obj.optBoolean("is_mandatory", false),
                                platform = obj.optString("platform", platform)
                            )
                        )
                    }
                    return@withContext Result.success(null)
                }
            }

            // Fallback direct query on public.app_releases if RPC is not deployed yet
            val tableReq = requestBuilder(
                "/rest/v1/app_releases?platform=in.(${platform.lowercase()},all)&order=version_code.desc,published_at.desc&limit=1",
                AppConfig.supabaseAnonKey
            ).get().build()
            client.newCall(tableReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (resp.isSuccessful && body.startsWith("[")) {
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        val obj = arr.getJSONObject(0)
                        val remoteCode = obj.optInt("version_code", currentVersionCode)
                        val remoteName = obj.optString("version_name", currentVersionName).ifBlank { currentVersionName }
                        if (AppUpdateInfo.isRemoteNewer(currentVersionCode, currentVersionName, remoteCode, remoteName)) {
                            return@withContext Result.success(
                                AppUpdateInfo(
                                    versionCode = remoteCode,
                                    versionName = remoteName,
                                    releaseNotes = obj.optString("release_notes", "").trim(),
                                    downloadUrl = obj.optString("download_url", "").trim(),
                                    isMandatory = obj.optBoolean("is_mandatory", false),
                                    platform = obj.optString("platform", platform)
                                )
                            )
                        }
                    }
                }
            }
            Result.success(null)
        } catch (_: Exception) {
            Result.success(null)
        }
    }

    /**
     * Registers or updates the user's push notification token in Supabase `user_push_tokens`.
     */
    suspend fun syncPushToken(
        session: AuthSession,
        token: String,
        enabled: Boolean,
        platform: String = "android"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured || token.isBlank()) {
            return@withContext Result.success(Unit)
        }
        try {
            val payload = JSONObject()
                .put("p_token", token.trim())
                .put("p_platform", platform.lowercase())
                .put("p_enabled", enabled)
            val req = requestBuilder("/rest/v1/rpc/upsert_push_token", session.accessToken)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().close()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.success(Unit)
        }
    }

    /**
     * Resolves either an email address or a unique username to an account email before sign-in or password reset.
     */
    suspend fun resolveLoginEmail(identifier: String): Result<String> = withContext(Dispatchers.IO) {
        val clean = identifier.trim().removePrefix("@").trim()
        if (clean.isBlank()) {
            return@withContext Result.failure(Exception("Enter your email address or username."))
        }
        if (clean.contains("@") && clean.contains(".")) {
            return@withContext Result.success(clean.lowercase())
        }
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud account service is currently unavailable."))
        }
        try {
            val payload = JSONObject().put("p_identifier", clean)
            val req = Request.Builder()
                .url("${AppConfig.supabaseUrl}/rest/v1/rpc/resolve_login_email")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${AppConfig.supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    if (body.contains("PGRST202", ignoreCase = true) ||
                        body.contains("Could not find the function", ignoreCase = true)
                    ) {
                        return@withContext Result.failure(
                            Exception("Username sign-in needs the latest database SQL function. Sign in with your email for now.")
                        )
                    }
                    return@withContext Result.failure(
                        Exception(parsePostgrestError(body, "Unable to sign in with username right now. Try using your email address."))
                    )
                }
                val resolved = body.removeSurrounding("\"").trim()
                if (resolved.isBlank() || resolved.equals("null", ignoreCase = true) || !resolved.contains("@")) {
                    Result.failure(Exception("No account found matching \"$clean\". Try your email address or create a new account."))
                } else {
                    Result.success(resolved)
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to reach account service right now.")))
        }
    }

    /**
     * Case-insensitive username uniqueness & 24-hour post-deletion cooldown check.
     */
    suspend fun checkUsernameAvailability(
        username: String,
        accessToken: String? = null,
        currentUserId: String? = null
    ): Result<Boolean> = checkUsernameStatus(username, accessToken, currentUserId).map { it.available }

    suspend fun checkUsernameStatus(
        username: String,
        accessToken: String? = null,
        currentUserId: String? = null
    ): Result<UsernameCheckResult> = withContext(Dispatchers.IO) {
        val clean = username.trim()
        if (clean.length !in 2..32) {
            return@withContext Result.failure(Exception("Username must be between 2 and 32 characters."))
        }
        if (clean.contains("@")) {
            return@withContext Result.failure(Exception("Usernames cannot contain the @ symbol."))
        }
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.success(UsernameCheckResult(available = true))
        }
        val token = accessToken?.takeIf { it.isNotBlank() } ?: AppConfig.supabaseAnonKey
        try {
            val payload = JSONObject().put("p_username", clean)
            val statusReq = requestBuilder("/rest/v1/rpc/check_username_status", token)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(statusReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (resp.isSuccessful && body.startsWith("{")) {
                    val obj = JSONObject(body)
                    val available = obj.optBoolean("available", false)
                    val reason = obj.optString("reason", "").takeIf { it.isNotBlank() }
                    return@withContext Result.success(UsernameCheckResult(available = available, reason = reason))
                }
            }

            val req = requestBuilder("/rest/v1/rpc/is_username_available", token)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (resp.isSuccessful) {
                    val available = body.equals("true", ignoreCase = true)
                    return@withContext Result.success(
                        UsernameCheckResult(
                            available = available,
                            reason = if (available) null else "That username is already taken or in a 24-hour cooldown."
                        )
                    )
                }
            }

            // Fallback direct query when RPC hasn't been deployed yet
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val fallbackReq = requestBuilder("/rest/v1/profiles?username=ilike.$encoded&select=id", token)
                .get()
                .build()
            client.newCall(fallbackReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(parsePostgrestError(body, "Unable to verify username availability right now."))
                    )
                }
                val arr = JSONArray(body)
                var takenByOther = false
                for (i in 0 until arr.length()) {
                    val rowId = arr.getJSONObject(i).optString("id")
                    if (currentUserId.isNullOrBlank() || rowId != currentUserId) {
                        takenByOther = true
                        break
                    }
                }
                Result.success(
                    UsernameCheckResult(
                        available = !takenByOther,
                        reason = if (takenByOther) "That username is already taken." else null
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to verify username right now. Check your connection.")))
        }
    }

    /**
     * Enforces the 5-minute cooldown before re-registering with a recently deleted email.
     */
    suspend fun checkEmailRegistrationAllowed(email: String): Result<EmailCooldownResult> = withContext(Dispatchers.IO) {
        val clean = email.trim().lowercase()
        if (!clean.contains("@") || !clean.contains(".")) {
            return@withContext Result.failure(Exception("Enter a valid email address."))
        }
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.success(EmailCooldownResult(allowed = true))
        }
        try {
            val payload = JSONObject().put("p_email", clean)
            val req = Request.Builder()
                .url("${AppConfig.supabaseUrl}/rest/v1/rpc/can_register_email")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${AppConfig.supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (resp.isSuccessful && body.startsWith("{")) {
                    val obj = JSONObject(body)
                    return@withContext Result.success(
                        EmailCooldownResult(
                            allowed = obj.optBoolean("allowed", true),
                            retryAfterSeconds = obj.optLong("retry_after_seconds", 0L)
                        )
                    )
                }
                Result.success(EmailCooldownResult(allowed = true))
            }
        } catch (_: Exception) {
            Result.success(EmailCooldownResult(allowed = true))
        }
    }

    /**
     * Permanently deletes the signed-in user's account, storage files,all user database rows,
     * and the Supabase `auth.users` authentication record, while recording the 5-minute email
     * and 24-hour username cooldowns.
     */
    suspend fun deleteAccountCompletely(session: AuthSession): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud account service is currently unavailable."))
        }
        try {
            var authDeleted = false
            var dataDeleted = false
            var lastErrorMsg: String? = null

            // Step 1: Invoke the privileged Edge Function `/functions/v1/delete-account` first
            // (uses SUPABASE_SERVICE_ROLE_KEY server-side to purge storage, DB rows, cooldowns, and auth.users)
            runCatching {
                val fnReq = requestBuilder("/functions/v1/delete-account", session.accessToken)
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(fnReq).execute().use { resp ->
                    val body = resp.body?.string().orEmpty().trim()
                    if (resp.isSuccessful) {
                        dataDeleted = true
                        authDeleted = if (body.startsWith("{")) {
                            val obj = JSONObject(body)
                            obj.optBoolean("deleted_auth_user", obj.optBoolean("deleted", true))
                        } else {
                            true
                        }
                    }
                }
            }

            // Step 2: Execute the `SECURITY DEFINER` SQL RPC `/rest/v1/rpc/delete_my_account`
            // (owned by `postgres`; deletes storage.objects, user tables, cooldowns, and auth.users)
            if (!authDeleted || !dataDeleted) {
                val rpcReq = requestBuilder("/rest/v1/rpc/delete_my_account", session.accessToken)
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(rpcReq).execute().use { resp ->
                    val body = resp.body?.string().orEmpty().trim()
                    if (resp.isSuccessful) {
                        dataDeleted = true
                        if (body.startsWith("{")) {
                            val obj = JSONObject(body)
                            if (obj.optBoolean("auth_deleted", false)) {
                                authDeleted = true
                            }
                        }
                    } else if (resp.code == 401 || resp.code == 403 || body.contains("JWT", ignoreCase = true)) {
                        // Token already invalidated because Step 1 deleted auth.users
                        if (dataDeleted) {
                            authDeleted = true
                        }
                    } else if (!body.contains("PGRST202", ignoreCase = true) &&
                        !body.contains("Could not find the function", ignoreCase = true)
                    ) {
                        lastErrorMsg = parsePostgrestError(body, "Unable to delete account right now.")
                    }
                }
            }

            // Step 3: Fallback direct REST cleanup if neither Edge Function nor RPC was deployed yet
            if (!dataDeleted) {
                var allDirectSucceeded = true
                listOf(
                    "/rest/v1/user_push_tokens?user_id=eq.${session.userId}",
                    "/rest/v1/collections?creator_id=eq.${session.userId}",
                    "/rest/v1/user_anime?user_id=eq.${session.userId}",
                    "/rest/v1/privacy_settings?user_id=eq.${session.userId}",
                    "/rest/v1/community_members?user_id=eq.${session.userId}",
                    "/rest/v1/profiles?id=eq.${session.userId}"
                ).forEach { path ->
                    runCatching {
                        client.newCall(requestBuilder(path, session.accessToken).delete().build()).execute().use { r ->
                            if (!r.isSuccessful && r.code !in listOf(401, 404)) {
                                allDirectSucceeded = false
                            }
                        }
                    }.onFailure { allDirectSucceeded = false }
                }
                if (allDirectSucceeded) {
                    dataDeleted = true
                }
            }

            // Step 4: If data was deleted by Step 2/3 before Edge Function ran, call Edge Function once more to ensure auth.users is purged
            if (dataDeleted && !authDeleted) {
                runCatching {
                    val fnReq = requestBuilder("/functions/v1/delete-account", session.accessToken)
                        .post("{}".toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(fnReq).execute().use { resp ->
                        if (resp.isSuccessful || resp.code == 401 || resp.code == 404) {
                            if (resp.isSuccessful) authDeleted = true
                        }
                    }
                }
            }

            // Step 5: Guarantee the account can NEVER be logged back into even if the SQL function or Edge Function
            // hasn't been deployed with auth.users DELETE privileges yet: mark account_deleted=true and rotate
            // the password to an unguessable cryptographic value before revoking the session.
            if (!authDeleted) {
                runCatching {
                    val randomLockPassword = "Del#" + java.util.UUID.randomUUID().toString().replace("-", "") + "9!aZ"
                    val lockMeta = JSONObject()
                        .put("account_deleted", true)
                        .put("onboarding_completed", false)
                        .put("username", "")
                    val lockPayload = JSONObject()
                        .put("password", randomLockPassword)
                        .put("data", lockMeta)
                    val lockReq = Request.Builder()
                        .url("${AppConfig.supabaseUrl}/auth/v1/user")
                        .addHeader("apikey", AppConfig.supabaseAnonKey)
                        .addHeader("Authorization", "Bearer ${session.accessToken}")
                        .addHeader("Content-Type", "application/json")
                        .put(lockPayload.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(lockReq).execute().close()
                }
            }

            if (!dataDeleted) {
                return@withContext Result.failure(
                    Exception(lastErrorMsg ?: "Unable to delete account right now. Please check your connection and try again.")
                )
            }

            syncedCatalogHashes.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to delete account right now.")))
        }
    }

    /**
     * Completes post-verification onboarding by claiming a unique username + Royal built-in PFP.
     * Prevents duplicate usernames across all users case-insensitively.
     */
    suspend fun completeOnboardingProfile(
        session: AuthSession,
        username: String,
        avatarKey: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanName = username.trim()
        val availableRes = checkUsernameStatus(
            username = cleanName,
            accessToken = session.accessToken,
            currentUserId = session.userId
        )
        if (availableRes.isFailure) {
            return@withContext Result.failure(
                availableRes.exceptionOrNull() ?: Exception("Unable to verify username availability.")
            )
        }
        val status = availableRes.getOrThrow()
        if (!status.available) {
            return@withContext Result.failure(
                Exception(status.reason ?: "That username is already taken. Please choose a different unique username.")
            )
        }
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud account service is currently unavailable."))
        }

        try {
            val existing = fetchCurrentProfile(session).getOrNull()
            val payload = JSONObject()
                .put("username", cleanName)
                .put("avatar_key", avatarKey)
                .put("avatar_url", JSONObject.NULL)

            val saveResp = if (existing != null) {
                val patchReq = requestBuilder("/rest/v1/profiles?id=eq.${session.userId}", session.accessToken)
                    .addHeader("Prefer", "return=minimal")
                    .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(patchReq).execute()
            } else {
                val insertPayload = JSONObject()
                    .put("id", session.userId)
                    .put("username", cleanName)
                    .put("avatar_key", avatarKey)
                    .put("role", "member")
                    .put("can_generate_invites", false)
                val insertReq = requestBuilder("/rest/v1/profiles", session.accessToken)
                    .addHeader("Prefer", "return=minimal")
                    .post(insertPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(insertReq).execute()
            }

            saveResp.use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(parsePostgrestError(body, "Unable to save your profile right now."))
                    )
                }
            }

            val privacyPayload = JSONObject()
                .put("user_id", session.userId)
                .put("watchlist_visibility", "private")
                .put("reviews_visibility", "community")
                .put("collections_visibility", "private")
                .put("activity_visibility", "community")
            val privacyReq = requestBuilder("/rest/v1/privacy_settings?on_conflict=user_id", session.accessToken)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(privacyPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            runCatching { client.newCall(privacyReq).execute().close() }

            val loaded = fetchCurrentProfile(session).getOrNull() ?: UserProfile(
                id = session.userId,
                email = session.email,
                username = cleanName,
                avatarKey = avatarKey,
                memberStatus = MemberStatus.NONE
            )
            Result.success(loaded)
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to complete profile setup right now.")))
        }
    }

    /**
     * Ensures the signed-in user has a personal profile & default privacy row for app login/sync.
     */
    suspend fun ensurePersonalProfile(
        session: AuthSession,
        preferredUsername: String = "",
        preferredAvatarKey: String = "avatar_1"
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Cloud account service is currently unavailable."))
        fetchCurrentProfile(session).getOrNull()?.let { existing ->
            return@withContext Result.success(existing)
        }
        val baseName = preferredUsername.trim()
            .takeIf { it.isNotBlank() && !it.contains("@") }
            ?: session.pendingUsername.trim().takeIf { it.isNotBlank() && !it.contains("@") }
            ?: "traveler_${session.userId.replace("-", "").take(8).ifBlank { "member" }}"
        val safeName = if (baseName.length < 2) "traveler_${session.userId.replace("-", "").take(8)}" else baseName.take(32)
        val avatarKey = preferredAvatarKey.ifBlank { session.pendingAvatarKey }.ifBlank { "avatar_1" }

        try {
            val payload = JSONObject()
                .put("id", session.userId)
                .put("username", safeName)
                .put("avatar_key", avatarKey)
                .put("role", "member")
                .put("can_generate_invites", false)

            val req = requestBuilder("/rest/v1/profiles?on_conflict=id", session.accessToken)
                .addHeader("Prefer", "resolution=ignore-duplicates,return=minimal")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    if (body.contains("profiles_username_key", ignoreCase = true) ||
                        body.contains("idx_profiles_username_lower_unique", ignoreCase = true) ||
                        body.contains("duplicate", ignoreCase = true)
                    ) {
                        val uniqueName = "${safeName.take(26)}_${session.userId.take(4)}"
                        val retryPayload = JSONObject()
                            .put("id", session.userId)
                            .put("username", uniqueName)
                            .put("avatar_key", avatarKey)
                            .put("role", "member")
                            .put("can_generate_invites", false)
                        val retryReq = requestBuilder("/rest/v1/profiles?on_conflict=id", session.accessToken)
                            .addHeader("Prefer", "resolution=ignore-duplicates,return=minimal")
                            .post(retryPayload.toString().toRequestBody("application/json".toMediaType()))
                            .build()
                        client.newCall(retryReq).execute().close()
                    }
                }
            }

            val privacyPayload = JSONObject()
                .put("user_id", session.userId)
                .put("watchlist_visibility", "private")
                .put("reviews_visibility", "community")
                .put("collections_visibility", "private")
                .put("activity_visibility", "community")
            val privacyReq = requestBuilder("/rest/v1/privacy_settings?on_conflict=user_id", session.accessToken)
                .addHeader("Prefer", "resolution=ignore-duplicates,return=minimal")
                .post(privacyPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(privacyReq).execute().close()

            fetchCurrentProfile(session).getOrNull()?.let { loaded ->
                return@withContext Result.success(loaded)
            }

            Result.success(
                UserProfile(
                    id = session.userId,
                    email = session.email,
                    username = safeName,
                    avatarKey = avatarKey,
                    memberStatus = MemberStatus.NONE
                )
            )
        } catch (_: Exception) {
            Result.success(
                UserProfile(
                    id = session.userId,
                    email = session.email,
                    username = safeName,
                    avatarKey = avatarKey,
                    memberStatus = MemberStatus.NONE
                )
            )
        }
    }

    suspend fun joinCommunity(
        session: AuthSession,
        username: String,
        avatarKey: String,
        inviteCode: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Cloud account service is currently unavailable."))
        try {
            val cleanCode = inviteCode.trim().uppercase()
            val payload = JSONObject()
                .put("p_username", username.trim())
                .put("p_avatar_key", avatarKey)
                .put("p_code", if (cleanCode.isBlank()) JSONObject.NULL else cleanCode)
            val req = requestBuilder("/rest/v1/rpc/join_community", session.accessToken)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            var joinedOk = false
            var errMsg: String? = null
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    joinedOk = true
                } else {
                    errMsg = parsePostgrestError(body, "Unable to join community right now.")
                }
            }
            if (!joinedOk) {
                // Direct fallback to community_members table in case RPC fails or requires direct upsert
                val directMemberPayload = JSONObject()
                    .put("user_id", session.userId)
                    .put("role", "member")
                    .put("status", "active")
                val directMemberReq = requestBuilder("/rest/v1/community_members?on_conflict=user_id", session.accessToken)
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(directMemberPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                runCatching {
                    client.newCall(directMemberReq).execute().use { directResp ->
                        if (directResp.isSuccessful) {
                            joinedOk = true
                        }
                    }
                }
            }
            if (!joinedOk) {
                return@withContext Result.failure(Exception(errMsg ?: "Unable to join community right now."))
            }

            // Any user joined in Community automatically shares their notes & ratings to Community templates
            val autoSharePayload = JSONObject()
                .put("user_id", session.userId)
                .put("reviews_visibility", "community")
                .put("activity_visibility", "community")
            val autoShareReq = requestBuilder("/rest/v1/privacy_settings?on_conflict=user_id", session.accessToken)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(autoSharePayload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            runCatching { client.newCall(autoShareReq).execute().close() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to join community right now.")))
        }
    }

    suspend fun redeemInvitationAndJoin(
        session: AuthSession,
        code: String,
        username: String,
        avatarKey: String
    ): Result<Unit> = joinCommunity(
        session = session,
        username = username,
        avatarKey = avatarKey,
        inviteCode = code
    )

    suspend fun fetchCurrentProfile(session: AuthSession): Result<UserProfile?> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val profileReq = requestBuilder(
                "/rest/v1/profiles?id=eq.${session.userId}&select=*",
                session.accessToken
            ).get().build()
            val memberReq = requestBuilder(
                "/rest/v1/community_members?user_id=eq.${session.userId}&select=*",
                session.accessToken
            ).get().build()

            val profileArr = client.newCall(profileReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to load profile")))
                JSONArray(body)
            }
            if (profileArr.length() == 0) {
                return@withContext Result.success(null)
            }
            val p = profileArr.getJSONObject(0)

            val memberArr = client.newCall(memberReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) JSONArray(body) else JSONArray()
            }
            val m = if (memberArr.length() > 0) memberArr.getJSONObject(0) else null

            val rawAvatarKey = p.optString("avatar_key").takeIf { it.isNotBlank() && it != "null" }
            val rawAvatarFrame = p.optString("avatar_frame").takeIf { it.isNotBlank() && it != "null" }
            val rawAvatarUrl = p.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }
            Result.success(
                UserProfile(
                    id = p.getString("id"),
                    email = session.email,
                    username = p.optString("username", "Member"),
                    avatarKey = BuiltInAvatarFrame.resolveCompositeAvatarKey(
                        rawAvatarKey = rawAvatarKey,
                        rawAvatarFrame = rawAvatarFrame,
                        rawAvatarUrl = rawAvatarUrl
                    ),
                    avatarUrl = rawAvatarUrl?.takeIf {
                        it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
                    },
                    bio = p.optString("bio", ""),
                    role = p.optString("role", "member"),
                    canGenerateInvites = p.optBoolean("can_generate_invites", false),
                    memberStatus = MemberStatus.fromDbValue(m?.optString("status")),
                    joinedAt = m?.optString("joined_at"),
                    xp = p.optInt("xp", 0).coerceAtLeast(0)
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Failed to load profile.")))
        }
    }

    suspend fun updateProfile(
        session: AuthSession,
        username: String,
        avatarKey: String,
        avatarUrl: String?,
        bio: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Cloud account service is currently unavailable."))
        val cleanName = username.trim()
        val avail = checkUsernameStatus(cleanName, session.accessToken, session.userId)
        if (avail.isFailure) {
            return@withContext Result.failure(avail.exceptionOrNull() ?: Exception("Invalid username."))
        }
        val status = avail.getOrThrow()
        if (!status.available) {
            return@withContext Result.failure(
                Exception(status.reason ?: "That username is already taken. Please choose a different unique username.")
            )
        }
        try {
            val extractedFrame = BuiltInAvatarFrame.extractKey(avatarKey)
            val payload = JSONObject()
                .put("username", cleanName)
                .put("avatar_key", avatarKey)
                .put("avatar_frame", extractedFrame ?: JSONObject.NULL)
                .put("bio", bio.trim())
            val validHttpAvatarUrl = avatarUrl?.trim()?.takeIf {
                it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
            }
            if (validHttpAvatarUrl == null) {
                payload.put("avatar_url", JSONObject.NULL)
            } else {
                payload.put("avatar_url", validHttpAvatarUrl)
            }

            val req = requestBuilder("/rest/v1/profiles?id=eq.${session.userId}", session.accessToken)
                .addHeader("Prefer", "return=minimal")
                .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    return@withContext Result.success(Unit)
                }
                if (body.contains("avatar_frame", ignoreCase = true)) {
                    payload.remove("avatar_frame")
                    val retryReq = requestBuilder("/rest/v1/profiles?id=eq.${session.userId}", session.accessToken)
                        .addHeader("Prefer", "return=minimal")
                        .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(retryReq).execute().use { retryResp ->
                        val retryBody = retryResp.body?.string().orEmpty()
                        if (!retryResp.isSuccessful) {
                            return@withContext Result.failure(Exception(parsePostgrestError(retryBody, "Failed to update profile.")))
                        }
                        return@withContext Result.success(Unit)
                    }
                }
                Result.failure(Exception(parsePostgrestError(body, "Failed to update profile.")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Failed to update profile.")))
        }
    }

    suspend fun equipAvatarFrame(
        session: AuthSession,
        baseAvatarKey: String,
        frameKey: String?
    ): Result<Pair<String, Int?>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Cloud account service is currently unavailable."))
        val cleanFrame = BuiltInAvatarFrame.extractKey(frameKey)
        val fallbackComposite = BuiltInAvatarFrame.composeAvatarKey(baseAvatarKey, cleanFrame)
        try {
            val rpcPayload = JSONObject().put("p_frame_key", cleanFrame ?: JSONObject.NULL)
            val rpcReq = requestBuilder("/rest/v1/rpc/equip_avatar_frame", session.accessToken)
                .post(rpcPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(rpcReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (resp.isSuccessful) {
                    if (body.startsWith("{")) {
                        val obj = JSONObject(body)
                        val resolvedKey = BuiltInAvatarFrame.resolveCompositeAvatarKey(
                            rawAvatarKey = obj.optString("avatar_key").takeIf { it.isNotBlank() && it != "null" } ?: fallbackComposite,
                            rawAvatarFrame = obj.optString("avatar_frame").takeIf { it.isNotBlank() && it != "null" } ?: cleanFrame
                        )
                        val serverXp = if (obj.has("xp") && !obj.isNull("xp")) obj.optInt("xp", 0).coerceAtLeast(0) else null
                        return@withContext Result.success(resolvedKey to serverXp)
                    }
                    return@withContext Result.success(fallbackComposite to null)
                }
                val isMissingRpc = resp.code == 404 || body.contains("equip_avatar_frame", ignoreCase = true)
                if (!isMissingRpc) {
                    return@withContext Result.failure(Exception(parsePostgrestError(body, "Unable to update avatar frame.")))
                }
            }

            // Graceful fallback when RPC migration has not yet been applied
            val patchPayload = JSONObject()
                .put("avatar_key", fallbackComposite)
                .put("avatar_frame", cleanFrame ?: JSONObject.NULL)
            val patchReq = requestBuilder("/rest/v1/profiles?id=eq.${session.userId}", session.accessToken)
                .addHeader("Prefer", "return=minimal")
                .patch(patchPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(patchReq).execute().use { patchResp ->
                val patchBody = patchResp.body?.string().orEmpty()
                if (patchResp.isSuccessful) {
                    return@withContext Result.success(fallbackComposite to null)
                }
                if (patchBody.contains("avatar_frame", ignoreCase = true)) {
                    patchPayload.remove("avatar_frame")
                    val retryReq = requestBuilder("/rest/v1/profiles?id=eq.${session.userId}", session.accessToken)
                        .addHeader("Prefer", "return=minimal")
                        .patch(patchPayload.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(retryReq).execute().use { retryResp ->
                        val retryBody = retryResp.body?.string().orEmpty()
                        if (!retryResp.isSuccessful) {
                            return@withContext Result.failure(Exception(parsePostgrestError(retryBody, "Unable to update avatar frame.")))
                        }
                        return@withContext Result.success(fallbackComposite to null)
                    }
                }
                Result.failure(Exception(parsePostgrestError(patchBody, "Unable to update avatar frame.")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to update avatar frame.")))
        }
    }

    suspend fun uploadCustomAvatar(
        session: AuthSession,
        bytes: ByteArray,
        mimeType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        if (bytes.size > 2 * 1024 * 1024) {
            return@withContext Result.failure(Exception("Avatar image must be 2 MB or smaller."))
        }
        val ext = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/jpeg", "image/jpg" -> "jpg"
            else -> return@withContext Result.failure(Exception("Unsupported image type ($mimeType). Use PNG, JPEG, or WebP."))
        }
        try {
            val objectPath = "${session.userId}/avatar_${System.currentTimeMillis()}.$ext"
            val req = Request.Builder()
                .url("${AppConfig.supabaseUrl}/storage/v1/object/avatars/$objectPath")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Content-Type", mimeType)
                .addHeader("x-upsert", "true")
                .post(bytes.toRequestBody(mimeType.toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parsePostgrestError(body, "Avatar upload failed.")))
                }
                val publicUrl = "${AppConfig.supabaseUrl}/storage/v1/object/public/avatars/$objectPath"
                Result.success(publicUrl)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPrivacySettings(session: AuthSession): Result<PrivacySettings> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val req = requestBuilder(
                "/rest/v1/privacy_settings?user_id=eq.${session.userId}&select=*",
                session.accessToken
            ).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to load privacy settings")))
                val arr = JSONArray(body)
                if (arr.length() == 0) {
                    return@withContext Result.success(PrivacySettings(userId = session.userId))
                }
                val obj = arr.getJSONObject(0)
                Result.success(
                    PrivacySettings(
                        userId = session.userId,
                        watchlistVisibility = VisibilityLevel.fromDbValue(obj.optString("watchlist_visibility")),
                        reviewsVisibility = VisibilityLevel.fromDbValue(obj.optString("reviews_visibility")),
                        collectionsVisibility = VisibilityLevel.fromDbValue(obj.optString("collections_visibility")),
                        activityVisibility = VisibilityLevel.fromDbValue(obj.optString("activity_visibility"))
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePrivacySettings(session: AuthSession, settings: PrivacySettings): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val payload = JSONObject()
                .put("user_id", session.userId)
                .put("watchlist_visibility", settings.watchlistVisibility.dbValue)
                .put("reviews_visibility", settings.reviewsVisibility.dbValue)
                .put("collections_visibility", settings.collectionsVisibility.dbValue)
                .put("activity_visibility", settings.activityVisibility.dbValue)

            val req = requestBuilder("/rest/v1/privacy_settings?on_conflict=user_id", session.accessToken)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) Result.failure(Exception(parsePostgrestError(body, "Failed to save privacy settings")))
                else Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertAnimeCatalog(session: AuthSession, anime: AnimeEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Cloud account service is currently unavailable."))
        val compactDesc = anime.synopsis?.trim()?.take(360)?.takeIf { it.isNotBlank() }
        val genresList = anime.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(5)
        val studiosList = anime.studios.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
        val catalogSignature = listOf(
            anime.displayTitle,
            anime.coverImage,
            anime.totalEpisodes,
            anime.status,
            anime.score,
            anime.airingTimeUnix,
            anime.nextEpisodeNumber,
            genresList.size
        ).hashCode()
        if (syncedCatalogHashes[anime.id] == catalogSignature) {
            return@withContext Result.success(Unit)
        }
        try {
            val genresArr = JSONArray(genresList)
            val studiosArr = JSONArray(studiosList)
            val payload = JSONObject()
                .put("anilist_id", anime.id)
                .put("title_english", anime.titleEnglish ?: JSONObject.NULL)
                .put("title_romaji", anime.titleRomaji)
                .put("title_native", anime.titleNative ?: JSONObject.NULL)
                .put("cover_image", anime.coverImage)
                .put("banner_image", anime.bannerImage ?: JSONObject.NULL)
                .put("description", compactDesc ?: JSONObject.NULL)
                .put("genres", genresArr)
                .put("release_year", anime.seasonYear ?: JSONObject.NULL)
                .put("season", anime.season ?: JSONObject.NULL)
                .put("episodes", anime.totalEpisodes ?: JSONObject.NULL)
                .put("average_score", anime.score ?: JSONObject.NULL)
                .put("popularity", anime.popularity ?: JSONObject.NULL)
                .put("format", anime.format ?: JSONObject.NULL)
                .put("status", anime.status ?: JSONObject.NULL)
                .put("studios", studiosArr)
                .put("next_airing_at", anime.airingTimeUnix ?: JSONObject.NULL)
                .put("next_episode_number", anime.nextEpisodeNumber ?: JSONObject.NULL)

            val req = requestBuilder("/rest/v1/anime?on_conflict=anilist_id", session.accessToken)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Result.failure(Exception(parsePostgrestError(body, "Failed to sync anime catalog.")))
                } else {
                    syncedCatalogHashes[anime.id] = catalogSignature
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Failed to sync anime catalog.")))
        }
    }

    suspend fun upsertUserAnimeEntry(session: AuthSession, anime: AnimeEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        upsertAnimeCatalog(session, anime).onFailure { return@withContext Result.failure(it) }
        try {
            val status = WatchStatus.fromDbValue(anime.userStatus ?: if (anime.isWishlist) "plan_to_watch" else if (anime.isWatched) "completed" else "watching")
            val clampedProgress = com.example.model.ProgressRules.clampEpisodeProgress(anime.watchedEpisodeCount, anime.totalEpisodes)
            val clampedSeason = com.example.model.SeasonRules.clampSeason(anime.effectiveSeasonNumber)

            fun buildPayload(includeSeason: Boolean): JSONObject {
                val compactNotes = (anime.userNotes ?: "").trim().take(500)
                return JSONObject()
                    .put("user_id", session.userId)
                    .put("anime_id", anime.id)
                    .put("status", status.dbValue)
                    .put("episode_progress", clampedProgress)
                    .put("total_episodes", anime.totalEpisodes ?: JSONObject.NULL)
                    .put("personal_rating", anime.userScore?.let { it / 10.0 } ?: JSONObject.NULL)
                    .put("review_notes", compactNotes)
                    .put("is_favorite", anime.isFavorite)
                    .apply {
                        if (includeSeason) put("user_season", clampedSeason)
                    }
            }

            fun executeUpsert(includeSeason: Boolean): Pair<Boolean, String> {
                val req = requestBuilder("/rest/v1/user_anime?on_conflict=user_id,anime_id", session.accessToken)
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(buildPayload(includeSeason).toString().toRequestBody("application/json".toMediaType()))
                    .build()
                return client.newCall(req).execute().use { resp ->
                    resp.isSuccessful to resp.body?.string().orEmpty()
                }
            }

            var (ok, body) = executeUpsert(supportsUserSeasonColumn)
            if (!ok && supportsUserSeasonColumn && (body.contains("user_season", ignoreCase = true) || body.contains("PGRST204"))) {
                supportsUserSeasonColumn = false
                val retry = executeUpsert(false)
                ok = retry.first
                body = retry.second
            }
            if (!ok) Result.failure(Exception(parsePostgrestError(body, "Failed to sync watchlist entry")))
            else Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserAnimeEntry(session: AuthSession, animeId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val req = requestBuilder(
                "/rest/v1/user_anime?user_id=eq.${session.userId}&anime_id=eq.$animeId",
                session.accessToken
            ).delete().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) Result.failure(Exception(parsePostgrestError(body, "Failed to remove entry")))
                else Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchHomeTrackingIds(session: AuthSession): Result<Set<Int>?> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.success(null)
        try {
            val req = requestBuilder(
                "/rest/v1/home_tracking?user_id=eq.${session.userId}&select=anime_id",
                session.accessToken
            ).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    if (resp.code == 404 || body.contains("PGRST205", ignoreCase = true) || body.contains("home_tracking", ignoreCase = true)) {
                        return@withContext Result.success(null)
                    }
                    return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to load Home list")))
                }
                val arr = JSONArray(body)
                val ids = mutableSetOf<Int>()
                for (i in 0 until arr.length()) {
                    val id = arr.getJSONObject(i).optInt("anime_id", 0)
                    if (id > 0) ids.add(id)
                }
                Result.success(ids)
            }
        } catch (_: Exception) {
            Result.success(null)
        }
    }

    suspend fun addToHome(session: AuthSession, anime: AnimeEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        upsertAnimeCatalog(session, anime).onFailure { return@withContext Result.failure(it) }
        try {
            val payload = JSONObject()
                .put("p_anime_id", anime.id)
                .put("p_airing_day", anime.airingDayOfWeek.coerceIn(1, 7))
            val rpcReq = requestBuilder("/rest/v1/rpc/add_to_home", session.accessToken)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(rpcReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) return@withContext Result.success(Unit)
                if (resp.code != 404 && !body.contains("PGRST202", ignoreCase = true) && !body.contains("Could not find", ignoreCase = true)) {
                    return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to add to Home")))
                }
            }
            // Fallback before SQL migration is deployed
            val htPayload = JSONObject()
                .put("user_id", session.userId)
                .put("anime_id", anime.id)
                .put("airing_day_of_week", anime.airingDayOfWeek.coerceIn(1, 7))
            runCatching {
                val htReq = requestBuilder("/rest/v1/home_tracking?on_conflict=user_id,anime_id", session.accessToken)
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(htPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(htReq).execute().close()
            }
            upsertUserAnimeEntry(session, anime)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromHome(session: AuthSession, animeId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val payload = JSONObject().put("p_anime_id", animeId)
            val rpcReq = requestBuilder("/rest/v1/rpc/remove_from_home", session.accessToken)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(rpcReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) return@withContext Result.success(Unit)
                if (resp.code != 404 && !body.contains("PGRST202", ignoreCase = true) && !body.contains("Could not find", ignoreCase = true)) {
                    return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to remove from Home")))
                }
            }
            // Fallback direct delete on home_tracking (never deletes user_anime Library entry)
            val delReq = requestBuilder(
                "/rest/v1/home_tracking?user_id=eq.${session.userId}&anime_id=eq.$animeId",
                session.accessToken
            ).delete().build()
            runCatching { client.newCall(delReq).execute().close() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertCommunityNote(
        session: AuthSession,
        animeId: Int,
        animeTitle: String,
        coverImage: String,
        rating10: Double?,
        reviewNotes: String,
        genres: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        val catalogSeed = AnimeEntity(
            id = animeId,
            titleRomaji = animeTitle.ifBlank { "Anime #$animeId" },
            titleEnglish = animeTitle.ifBlank { null },
            coverImage = coverImage,
            genres = genres
        )
        runCatching { upsertAnimeCatalog(session, catalogSeed) }
        try {
            val cleanNote = reviewNotes.trim().take(500)
            val cleanRating = rating10?.takeIf { it > 0.0 }?.coerceIn(0.5, 10.0)
            val payload = JSONObject()
                .put("p_anime_id", animeId)
                .put("p_rating", cleanRating ?: JSONObject.NULL)
                .put("p_note_text", cleanNote)
            val rpcReq = requestBuilder("/rest/v1/rpc/upsert_community_note", session.accessToken)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(rpcReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) return@withContext Result.success(Unit)
                if (resp.code != 404 && !body.contains("PGRST202", ignoreCase = true) && !body.contains("Could not find", ignoreCase = true)) {
                    return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to save community note")))
                }
            }
            // Fallback direct upsert into community_notes (never touches home_tracking)
            val tablePayload = JSONObject()
                .put("user_id", session.userId)
                .put("anime_id", animeId)
                .put("rating", cleanRating ?: JSONObject.NULL)
                .put("note_text", cleanNote)
            val tableReq = requestBuilder("/rest/v1/community_notes?on_conflict=user_id,anime_id", session.accessToken)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(tablePayload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(tableReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) return@withContext Result.success(Unit)
                return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to save community note")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEpisodeProgress(
        session: AuthSession,
        anime: AnimeEntity,
        targetEpisodes: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        upsertAnimeCatalog(session, anime).onFailure { return@withContext Result.failure(it) }
        try {
            val payload = JSONObject()
                .put("p_anime_id", anime.id)
                .put("p_watched_episodes", targetEpisodes)
            val rpcReq = requestBuilder("/rest/v1/rpc/update_episode_progress", session.accessToken)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(rpcReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) return@withContext Result.success(Unit)
                if (resp.code != 404 && !body.contains("PGRST202", ignoreCase = true) && !body.contains("Could not find", ignoreCase = true)) {
                    return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to update episode progress")))
                }
            }
            // Fallback when RPC isn't deployed yet
            upsertUserAnimeEntry(session, anime.copy(watchedEpisodeCount = targetEpisodes))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserWatchlist(session: AuthSession, targetUserId: String = session.userId): Result<List<AnimeEntity>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Cloud account service is currently unavailable."))
        try {
            val homeIds: Set<Int>? = if (targetUserId == session.userId) {
                fetchHomeTrackingIds(session).getOrNull()
            } else {
                null
            }
            val compactSelect = "id,user_id,anime_id,status,episode_progress,total_episodes,personal_rating,review_notes,is_favorite,updated_at," +
                "anime(anilist_id,title_english,title_romaji,title_native,cover_image,banner_image,genres,release_year,season,episodes,average_score,popularity,format,status,studios,next_airing_at,next_episode_number)"
            val req = requestBuilder(
                "/rest/v1/user_anime?user_id=eq.$targetUserId&select=$compactSelect&order=updated_at.desc",
                session.accessToken
            ).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to fetch watchlist")))
                val arr = JSONArray(body)
                val items = buildList {
                    for (i in 0 until arr.length()) {
                        val row = arr.getJSONObject(i)
                        val a = row.optJSONObject("anime") ?: continue
                        val animeId = a.getInt("anilist_id")
                        val statusStr = row.optString("status", "plan_to_watch")
                        val watchStatus = WatchStatus.fromDbValue(statusStr)
                        val officialStatus = a.optString("status", "RELEASING")
                        val totalEps = if (a.isNull("episodes")) null else a.optInt("episodes")
                        val progress = row.optInt("episode_progress", 0)
                        val personalRating = if (row.isNull("personal_rating")) null else kotlin.math.round(row.optDouble("personal_rating") * 10.0).toInt()
                        val genresArr = a.optJSONArray("genres")
                        val genres = buildList {
                            if (genresArr != null) {
                                for (g in 0 until genresArr.length()) add(genresArr.optString(g))
                            }
                        }.joinToString(", ")
                        val studiosArr = a.optJSONArray("studios")
                        val studios = buildList {
                            if (studiosArr != null) {
                                for (s in 0 until studiosArr.length()) add(studiosArr.optString(s))
                            }
                        }.joinToString(", ")

                        val nextAiringAt = if (a.isNull("next_airing_at")) null else a.optLong("next_airing_at")
                        val resolvedDayOfWeek = if (nextAiringAt != null && nextAiringAt > 0L) {
                            java.time.Instant.ofEpochSecond(nextAiringAt)
                                .atZone(java.time.ZoneId.systemDefault())
                                .dayOfWeek.value
                        } else {
                            ((animeId.coerceAtLeast(1) % 7) + 1)
                        }
                        val autoRemoveFromHome = com.example.model.ProgressRules.shouldRemoveFromHome(
                            watchStatus = watchStatus,
                            officialStatus = officialStatus,
                            totalEpisodes = totalEps,
                            watchedEpisodes = progress
                        )
                        val resolvedInHome = if (homeIds != null) {
                            homeIds.contains(animeId) && !autoRemoveFromHome
                        } else {
                            watchStatus == WatchStatus.WATCHING && !autoRemoveFromHome
                        }

                        add(
                            AnimeEntity(
                                id = animeId,
                                titleRomaji = a.optString("title_romaji", "Untitled"),
                                titleEnglish = a.optString("title_english").takeIf { it.isNotBlank() && it != "null" },
                                titleNative = a.optString("title_native").takeIf { it.isNotBlank() && it != "null" },
                                coverImage = a.optString("cover_image", ""),
                                bannerImage = a.optString("banner_image").takeIf { it.isNotBlank() && it != "null" },
                                format = a.optString("format", "TV"),
                                status = officialStatus,
                                totalEpisodes = totalEps,
                                currentEpisode = maxOf(progress, 1),
                                watchedEpisodeCount = progress,
                                userSeason = row.optInt("user_season", 1).coerceIn(1, 99),
                                userStatus = watchStatus.dbValue,
                                userScore = personalRating,
                                airingDayOfWeek = resolvedDayOfWeek,
                                airingTimeUnix = nextAiringAt,
                                nextEpisodeNumber = if (a.isNull("next_episode_number")) null else a.optInt("next_episode_number"),
                                genres = genres,
                                studios = studios,
                                season = a.optString("season").takeIf { it.isNotBlank() && it != "null" },
                                seasonYear = if (a.isNull("release_year")) null else a.optInt("release_year"),
                                score = if (a.isNull("average_score")) null else a.optInt("average_score"),
                                popularity = if (a.isNull("popularity")) null else a.optInt("popularity"),
                                synopsis = a.optString("description").takeIf { it.isNotBlank() && it != "null" },
                                inHome = resolvedInHome,
                                inWatchlist = true,
                                isWishlist = watchStatus == WatchStatus.PLAN_TO_WATCH,
                                isFavorite = row.optBoolean("is_favorite", false),
                                isWatched = watchStatus == WatchStatus.COMPLETED,
                                userNotes = row.optString("review_notes").takeIf { it.isNotBlank() && it != "null" }
                            )
                        )
                    }
                }
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCollections(session: AuthSession, creatorUserId: String = session.userId): Result<List<AnimeCollection>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val encodedCreator = URLEncoder.encode(creatorUserId, "UTF-8")
            val req = requestBuilder(
                "/rest/v1/collections?creator_id=eq.$encodedCreator&select=*,collection_anime(anime_id)&order=updated_at.desc",
                session.accessToken
            ).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to load collections")))
                val arr = JSONArray(body)
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val itemsArr = obj.optJSONArray("collection_anime")
                        val animeIds = buildList {
                            if (itemsArr != null) {
                                for (j in 0 until itemsArr.length()) {
                                    add(itemsArr.getJSONObject(j).getInt("anime_id"))
                                }
                            }
                        }
                        add(
                            AnimeCollection(
                                id = obj.getString("id"),
                                creatorId = obj.getString("creator_id"),
                                name = obj.optString("name", "Untitled Collection"),
                                description = obj.optString("description", ""),
                                visibility = VisibilityLevel.fromDbValue(obj.optString("visibility")),
                                animeIds = animeIds,
                                updatedAt = obj.optString("updated_at")
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCollection(
        session: AuthSession,
        name: String,
        description: String,
        visibility: VisibilityLevel
    ): Result<AnimeCollection> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val payload = JSONObject()
                .put("creator_id", session.userId)
                .put("name", name.trim())
                .put("description", description.trim())
                .put("visibility", visibility.dbValue)

            val req = requestBuilder("/rest/v1/collections", session.accessToken)
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to create collection")))
                val obj = JSONArray(body).getJSONObject(0)
                Result.success(
                    AnimeCollection(
                        id = obj.getString("id"),
                        creatorId = obj.getString("creator_id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        visibility = VisibilityLevel.fromDbValue(obj.optString("visibility")),
                        animeIds = emptyList(),
                        updatedAt = obj.optString("updated_at")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCollection(
        session: AuthSession,
        collectionId: String,
        name: String,
        visibility: VisibilityLevel
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val payload = JSONObject()
                .put("name", name.trim())
                .put("visibility", visibility.dbValue)
            val req = requestBuilder("/rest/v1/collections?id=eq.$collectionId", session.accessToken)
                .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) Result.failure(Exception(parsePostgrestError(body, "Failed to update collection")))
                else Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCollection(session: AuthSession, collectionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val req = requestBuilder("/rest/v1/collections?id=eq.$collectionId", session.accessToken)
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) Result.failure(Exception(parsePostgrestError(body, "Failed to delete collection")))
                else Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addAnimeToCollection(session: AuthSession, collectionId: String, anime: AnimeEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        upsertAnimeCatalog(session, anime).onFailure { return@withContext Result.failure(it) }
        try {
            val payload = JSONObject()
                .put("collection_id", collectionId)
                .put("anime_id", anime.id)
            val req = requestBuilder("/rest/v1/collection_anime?on_conflict=collection_id,anime_id", session.accessToken)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) Result.failure(Exception(parsePostgrestError(body, "Failed to add anime to collection")))
                else Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAnimeFromCollection(session: AuthSession, collectionId: String, animeId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val req = requestBuilder(
                "/rest/v1/collection_anime?collection_id=eq.$collectionId&anime_id=eq.$animeId",
                session.accessToken
            ).delete().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) Result.failure(Exception(parsePostgrestError(body, "Failed to remove anime from collection")))
                else Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCommunityCapacity(session: AuthSession): Result<CommunityCapacity> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val req = requestBuilder("/rest/v1/rpc/get_community_capacity", session.accessToken)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to read capacity")))
                val obj = JSONObject(body)
                Result.success(
                    CommunityCapacity(
                        activeCount = obj.optInt("active_count", 0),
                        maxCapacity = obj.optInt("max_capacity", 50)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCommunityMembers(session: AuthSession): Result<List<CommunityMember>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val membersReq = requestBuilder(
                "/rest/v1/community_members?status=eq.active&select=*",
                session.accessToken
            ).get().build()
            val profilesReq = requestBuilder("/rest/v1/profiles?select=*", session.accessToken).get().build()
            val privacyReq = requestBuilder("/rest/v1/privacy_settings?select=*", session.accessToken).get().build()

            val membersArr = client.newCall(membersReq).execute().use { resp ->
                val b = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parsePostgrestError(b, "Failed to load members")))
                JSONArray(b)
            }
            val profilesArr = client.newCall(profilesReq).execute().use { resp ->
                if (resp.isSuccessful) JSONArray(resp.body?.string().orEmpty()) else JSONArray()
            }
            val privacyArr = client.newCall(privacyReq).execute().use { resp ->
                if (resp.isSuccessful) JSONArray(resp.body?.string().orEmpty()) else JSONArray()
            }

            val profileById = mutableMapOf<String, JSONObject>()
            for (i in 0 until profilesArr.length()) {
                val p = profilesArr.getJSONObject(i)
                profileById[p.getString("id")] = p
            }
            val privacyById = mutableMapOf<String, PrivacySettings>()
            for (i in 0 until privacyArr.length()) {
                val ps = privacyArr.getJSONObject(i)
                val uid = ps.getString("user_id")
                privacyById[uid] = PrivacySettings(
                    userId = uid,
                    watchlistVisibility = VisibilityLevel.fromDbValue(ps.optString("watchlist_visibility")),
                    reviewsVisibility = VisibilityLevel.fromDbValue(ps.optString("reviews_visibility")),
                    collectionsVisibility = VisibilityLevel.fromDbValue(ps.optString("collections_visibility")),
                    activityVisibility = VisibilityLevel.fromDbValue(ps.optString("activity_visibility"))
                )
            }

            val members = buildList {
                for (i in 0 until membersArr.length()) {
                    val m = membersArr.getJSONObject(i)
                    val uid = m.getString("user_id")
                    val p = profileById[uid] ?: continue
                    val rawAvatarKey = p.optString("avatar_key").takeIf { it.isNotBlank() && it != "null" }
                    val rawAvatarFrame = p.optString("avatar_frame").takeIf { it.isNotBlank() && it != "null" }
                    val rawAvatarUrl = p.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }
                    add(
                        CommunityMember(
                            userId = uid,
                            username = p.optString("username", "Member"),
                            avatarKey = BuiltInAvatarFrame.resolveCompositeAvatarKey(
                                rawAvatarKey = rawAvatarKey,
                                rawAvatarFrame = rawAvatarFrame,
                                rawAvatarUrl = rawAvatarUrl
                            ),
                            avatarUrl = rawAvatarUrl?.takeIf {
                                it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
                            },
                            bio = p.optString("bio", ""),
                            role = p.optString("role", m.optString("role", "member")),
                            status = MemberStatus.fromDbValue(m.optString("status")),
                            joinedAt = m.optString("joined_at"),
                            privacy = privacyById[uid] ?: PrivacySettings(userId = uid)
                        )
                    )
                }
            }.sortedBy { it.username.lowercase() }

            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCommunityFeed(
        session: AuthSession,
        members: List<CommunityMember>,
        animeIdFilter: Int? = null,
        limit: Int = 120,
        offset: Int = 0
    ): Result<Pair<List<CommunityReview>, List<CommunityActivityItem>>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val memberById = members.associateBy { it.userId }.toMutableMap()
            val animeFilterQuery = if (animeIdFilter != null) "&anime_id=eq.$animeIdFilter" else ""
            val compactFeedSelect = "id,user_id,anime_id,status,episode_progress,personal_rating,review_notes,updated_at," +
                "anime(anilist_id,title_english,title_romaji,cover_image,episodes,genres)"
            val req = requestBuilder(
                "/rest/v1/user_anime?select=$compactFeedSelect$animeFilterQuery&order=updated_at.desc&limit=${limit.coerceIn(10, 200)}&offset=${offset.coerceAtLeast(0)}",
                session.accessToken
            ).get().build()

            val arr = client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to load community feed")))
                JSONArray(body)
            }

            // Also fetch independent community_notes (when deployed) so Community works independently of Home/Library
            val communityNotesArr = runCatching {
                val notesSelect = "id,user_id,anime_id,rating,note_text,updated_at,anime(anilist_id,title_english,title_romaji,cover_image,episodes,genres)"
                val notesReq = requestBuilder(
                    "/rest/v1/community_notes?select=$notesSelect$animeFilterQuery&order=updated_at.desc&limit=${limit.coerceIn(10, 200)}&offset=${offset.coerceAtLeast(0)}",
                    session.accessToken
                ).get().build()
                client.newCall(notesReq).execute().use { notesResp ->
                    if (notesResp.isSuccessful) JSONArray(notesResp.body?.string().orEmpty()) else JSONArray()
                }
            }.getOrDefault(JSONArray())

            // Backfill any missing contributor profiles so new users always see every contributor's username and avatar
            val missingUserIds = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                val uid = arr.getJSONObject(i).optString("user_id")
                if (uid.isNotBlank() && !memberById.containsKey(uid)) {
                    missingUserIds.add(uid)
                }
            }
            for (i in 0 until communityNotesArr.length()) {
                val uid = communityNotesArr.getJSONObject(i).optString("user_id")
                if (uid.isNotBlank() && !memberById.containsKey(uid)) {
                    missingUserIds.add(uid)
                }
            }
            if (missingUserIds.isNotEmpty()) {
                runCatching {
                    val profReq = requestBuilder(
                        "/rest/v1/profiles?id=in.(${missingUserIds.joinToString(",")})&select=*",
                        session.accessToken
                    ).get().build()
                    val cmReq = requestBuilder(
                        "/rest/v1/community_members?user_id=in.(${missingUserIds.joinToString(",")})&select=user_id,status",
                        session.accessToken
                    ).get().build()
                    val statusById = mutableMapOf<String, MemberStatus>()
                    client.newCall(cmReq).execute().use { cmResp ->
                        if (cmResp.isSuccessful) {
                            val cmArr = JSONArray(cmResp.body?.string().orEmpty())
                            for (j in 0 until cmArr.length()) {
                                val cmObj = cmArr.getJSONObject(j)
                                statusById[cmObj.optString("user_id")] = MemberStatus.fromDbValue(cmObj.optString("status"))
                            }
                        }
                    }
                    client.newCall(profReq).execute().use { profResp ->
                        if (profResp.isSuccessful) {
                            val profArr = JSONArray(profResp.body?.string().orEmpty())
                            for (j in 0 until profArr.length()) {
                                val p = profArr.getJSONObject(j)
                                val uid = p.optString("id")
                                val mStatus = statusById[uid] ?: MemberStatus.ACTIVE
                                if (uid.isNotBlank() && mStatus != MemberStatus.LEFT && mStatus != MemberStatus.REMOVED) {
                                    val rawAvatarKey = p.optString("avatar_key").takeIf { it.isNotBlank() && it != "null" }
                                    val rawAvatarFrame = p.optString("avatar_frame").takeIf { it.isNotBlank() && it != "null" }
                                    val rawAvatarUrl = p.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }
                                    memberById[uid] = CommunityMember(
                                        userId = uid,
                                        username = p.optString("username", "Member"),
                                        avatarKey = BuiltInAvatarFrame.resolveCompositeAvatarKey(
                                            rawAvatarKey = rawAvatarKey,
                                            rawAvatarFrame = rawAvatarFrame,
                                            rawAvatarUrl = rawAvatarUrl
                                        ),
                                        avatarUrl = rawAvatarUrl?.takeIf {
                                            it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
                                        },
                                        bio = "",
                                        role = p.optString("role", "member"),
                                        status = MemberStatus.ACTIVE,
                                        joinedAt = null,
                                        privacy = PrivacySettings(
                                            userId = uid,
                                            reviewsVisibility = VisibilityLevel.COMMUNITY,
                                            activityVisibility = VisibilityLevel.COMMUNITY
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Batch-fetch review likes + independent community note likes (no N+1 queries)
            val reviewLikesCount = mutableMapOf<String, Int>()
            val likedByCurrentUser = mutableSetOf<String>()
            runCatching {
                val likesReq = requestBuilder(
                    "/rest/v1/review_likes?select=review_id,user_id&limit=1000",
                    session.accessToken
                ).get().build()
                client.newCall(likesReq).execute().use { likesResp ->
                    if (likesResp.isSuccessful) {
                        val likesArr = JSONArray(likesResp.body?.string().orEmpty())
                        for (i in 0 until likesArr.length()) {
                            val lObj = likesArr.getJSONObject(i)
                            val rId = lObj.optString("review_id")
                            val uId = lObj.optString("user_id")
                            if (rId.isNotBlank()) {
                                reviewLikesCount[rId] = (reviewLikesCount[rId] ?: 0) + 1
                                if (uId == session.userId) {
                                    likedByCurrentUser.add(rId)
                                }
                            }
                        }
                    }
                }
            }
            runCatching {
                val noteLikesReq = requestBuilder(
                    "/rest/v1/community_note_likes?select=note_id,user_id&limit=1000",
                    session.accessToken
                ).get().build()
                client.newCall(noteLikesReq).execute().use { likesResp ->
                    if (likesResp.isSuccessful) {
                        val likesArr = JSONArray(likesResp.body?.string().orEmpty())
                        for (i in 0 until likesArr.length()) {
                            val lObj = likesArr.getJSONObject(i)
                            val nId = lObj.optString("note_id")
                            val uId = lObj.optString("user_id")
                            if (nId.isNotBlank()) {
                                reviewLikesCount[nId] = (reviewLikesCount[nId] ?: 0) + 1
                                if (uId == session.userId) {
                                    likedByCurrentUser.add(nId)
                                }
                            }
                        }
                    }
                }
            }

            val reviewsByKey = LinkedHashMap<String, CommunityReview>()
            val activity = mutableListOf<CommunityActivityItem>()

            // First ingest independent community_notes
            for (i in 0 until communityNotesArr.length()) {
                val row = communityNotesArr.getJSONObject(i)
                val rowId = row.optString("id")
                val uid = row.optString("user_id")
                val member = memberById[uid] ?: continue
                if (member.status != MemberStatus.ACTIVE) continue
                val anime = row.optJSONObject("anime")
                val animeId = anime?.optInt("anilist_id")?.takeIf { it > 0 } ?: row.optInt("anime_id", 0)
                if (animeId <= 0) continue
                val animeTitle = anime?.optString("title_english")?.takeIf { it.isNotBlank() && it != "null" }
                    ?: anime?.optString("title_romaji")?.takeIf { it.isNotBlank() && it != "null" }
                    ?: "Anime #$animeId"
                val coverImage = anime?.optString("cover_image", "").orEmpty()
                val rating = if (row.isNull("rating")) null else row.optDouble("rating")
                val notes = row.optString("note_text", "").trim()
                val totalEps = if (anime == null || anime.isNull("episodes")) null else anime.optInt("episodes")
                val genresArr = anime?.optJSONArray("genres")
                val genresList = buildList {
                    if (genresArr != null) {
                        for (gIdx in 0 until genresArr.length()) {
                            val g = genresArr.optString(gIdx).trim()
                            if (g.isNotBlank()) add(g)
                        }
                    }
                }
                val updatedAt = row.optString("updated_at", "")
                val hasNoteOrRating = notes.isNotBlank() || (rating != null && rating > 0.0)
                if (hasNoteOrRating) {
                    reviewsByKey["$uid-$animeId"] = CommunityReview(
                        id = rowId,
                        userId = uid,
                        username = member.username,
                        avatarKey = member.avatarKey,
                        avatarUrl = member.avatarUrl,
                        animeId = animeId,
                        animeTitle = animeTitle,
                        animeCoverImage = coverImage,
                        rating = rating,
                        reviewNotes = notes,
                        status = WatchStatus.WATCHING,
                        episodeProgress = 0,
                        totalEpisodes = totalEps,
                        updatedAt = updatedAt,
                        likesCount = reviewLikesCount[rowId] ?: 0,
                        likedByMe = likedByCurrentUser.contains(rowId),
                        genres = genresList
                    )
                }
            }

            // Next ingest user_anime reviews & activity (enriching matching community_notes with watch progress)
            for (i in 0 until arr.length()) {
                val row = arr.getJSONObject(i)
                val rowId = row.getString("id")
                val uid = row.getString("user_id")
                val member = memberById[uid] ?: continue
                if (member.status != MemberStatus.ACTIVE) continue
                val anime = row.optJSONObject("anime")
                val animeId = anime?.optInt("anilist_id")?.takeIf { it > 0 } ?: row.optInt("anime_id", 0)
                if (animeId <= 0) continue
                val animeTitle = anime?.optString("title_english")?.takeIf { it.isNotBlank() && it != "null" }
                    ?: anime?.optString("title_romaji")?.takeIf { it.isNotBlank() && it != "null" }
                    ?: "Anime #$animeId"
                val coverImage = anime?.optString("cover_image", "").orEmpty()
                val rating = if (row.isNull("personal_rating")) null else row.optDouble("personal_rating")
                val notes = row.optString("review_notes", "").trim()
                val status = WatchStatus.fromDbValue(row.optString("status"))
                val progress = row.optInt("episode_progress", 0)
                val totalEps = if (anime == null || anime.isNull("episodes")) null else anime.optInt("episodes")
                val genresArr = anime?.optJSONArray("genres")
                val genresList = buildList {
                    if (genresArr != null) {
                        for (gIdx in 0 until genresArr.length()) {
                            val g = genresArr.optString(gIdx).trim()
                            if (g.isNotBlank()) add(g)
                        }
                    }
                }
                val updatedAt = row.optString("updated_at", "")

                val key = "$uid-$animeId"
                val existingNote = reviewsByKey[key]
                if (existingNote != null) {
                    val combinedGenres = (existingNote.genres + genresList).distinct()
                    reviewsByKey[key] = existingNote.copy(
                        status = status,
                        episodeProgress = progress,
                        totalEpisodes = totalEps ?: existingNote.totalEpisodes,
                        genres = combinedGenres,
                        isAdult = existingNote.isAdult || com.example.model.isHentaiOrAdultGenres(combinedGenres)
                    )
                } else {
                    val canShowReview = member.status == MemberStatus.ACTIVE
                    val hasNoteOrRating = notes.isNotBlank() || (rating != null && rating > 0.0)
                    if (canShowReview && hasNoteOrRating) {
                        reviewsByKey[key] = CommunityReview(
                            id = rowId,
                            userId = uid,
                            username = member.username,
                            avatarKey = member.avatarKey,
                            avatarUrl = member.avatarUrl,
                            animeId = animeId,
                            animeTitle = animeTitle,
                            animeCoverImage = coverImage,
                            rating = rating,
                            reviewNotes = notes,
                            status = status,
                            episodeProgress = progress,
                            totalEpisodes = totalEps,
                            updatedAt = updatedAt,
                            likesCount = reviewLikesCount[rowId] ?: 0,
                            likedByMe = likedByCurrentUser.contains(rowId),
                            genres = genresList
                        )
                    }
                }

                val canShowActivity = member.status == MemberStatus.ACTIVE || uid == session.userId || member.privacy.activityVisibility == VisibilityLevel.COMMUNITY
                if (canShowActivity) {
                    activity.add(
                        CommunityActivityItem(
                            id = rowId,
                            userId = uid,
                            username = member.username,
                            avatarKey = member.avatarKey,
                            avatarUrl = member.avatarUrl,
                            animeId = animeId,
                            animeTitle = animeTitle,
                            animeCoverImage = coverImage,
                            status = status,
                            episodeProgress = progress,
                            totalEpisodes = totalEps,
                            personalRating = rating,
                            updatedAt = updatedAt
                        )
                    )
                }
            }
            Result.success(reviewsByKey.values.toList() to activity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleReviewLike(
        session: AuthSession,
        reviewId: String,
        currentlyLiked: Boolean,
        currentCount: Int
    ): Result<Pair<Boolean, Int>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val payload = JSONObject().put("p_review_id", reviewId)
            val rpcReq = requestBuilder("/rest/v1/rpc/toggle_review_like", session.accessToken)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(rpcReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (resp.isSuccessful && body.startsWith("{")) {
                    val obj = JSONObject(body)
                    return@withContext Result.success(
                        obj.optBoolean("liked", !currentlyLiked) to obj.optInt("likes_count", maxOf(0, currentCount + if (!currentlyLiked) 1 else -1))
                    )
                }
            }

            // Try independent community_note_likes RPC when reviewId is a community_notes UUID
            val notePayload = JSONObject().put("p_note_id", reviewId)
            val noteRpcReq = requestBuilder("/rest/v1/rpc/toggle_community_note_like", session.accessToken)
                .post(notePayload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(noteRpcReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (resp.isSuccessful && body.startsWith("{")) {
                    val obj = JSONObject(body)
                    return@withContext Result.success(
                        obj.optBoolean("liked", !currentlyLiked) to obj.optInt("likes_count", maxOf(0, currentCount + if (!currentlyLiked) 1 else -1))
                    )
                }
            }

            // Fallback direct table operation if RPC is not deployed yet
            if (currentlyLiked) {
                val delReq = requestBuilder(
                    "/rest/v1/review_likes?user_id=eq.${session.userId}&review_id=eq.$reviewId",
                    session.accessToken
                ).delete().build()
                client.newCall(delReq).execute().close()
                Result.success(false to maxOf(0, currentCount - 1))
            } else {
                val insPayload = JSONObject()
                    .put("user_id", session.userId)
                    .put("review_id", reviewId)
                val insReq = requestBuilder("/rest/v1/review_likes?on_conflict=user_id,review_id", session.accessToken)
                    .addHeader("Prefer", "resolution=ignore-duplicates,return=minimal")
                    .post(insPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(insReq).execute().close()
                Result.success(true to (currentCount + 1))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchDatabaseStorageStats(session: AuthSession): Result<DatabaseStorageStats> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val rpcReq = requestBuilder("/rest/v1/rpc/get_database_storage_stats", session.accessToken)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(rpcReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (resp.isSuccessful && body.startsWith("{")) {
                    val obj = JSONObject(body)
                    val tablesArr = obj.optJSONArray("tables") ?: JSONArray()
                    val tables = buildList {
                        for (i in 0 until tablesArr.length()) {
                            val t = tablesArr.getJSONObject(i)
                            add(
                                TableStorageStat(
                                    tableName = t.optString("table_name", "table"),
                                    rowCount = t.optLong("row_count", 0L),
                                    tableBytes = t.optLong("table_bytes", 0L),
                                    indexBytes = t.optLong("index_bytes", 0L),
                                    totalBytes = t.optLong("total_bytes", 0L)
                                )
                            )
                        }
                    }
                    return@withContext Result.success(
                        DatabaseStorageStats(
                            totalBytes = obj.optLong("total_bytes", 8_912_896L),
                            limitBytes = obj.optLong("limit_bytes", 524_288_000L),
                            usagePercent = obj.optDouble("usage_percent", 1.7),
                            warningLevel = obj.optString("warning_level", "normal"),
                            userCount = obj.optInt("user_count", 1),
                            entryCount = obj.optInt("entry_count", 0),
                            reviewCount = obj.optInt("review_count", 0),
                            bytesPerUser = obj.optLong("bytes_per_user", 4096L),
                            bytesPerReview = obj.optLong("bytes_per_review", 380L),
                            estimatedRemainingUsers = obj.optLong("estimated_remaining_users", 125_000L),
                            tables = tables
                        )
                    )
                }
            }

            // Fallback estimator when get_database_storage_stats RPC hasn't been deployed in SQL Editor yet
            fun countRows(table: String): Long = runCatching {
                val req = requestBuilder("/rest/v1/$table?select=*", session.accessToken)
                    .addHeader("Prefer", "count=exact")
                    .head()
                    .build()
                client.newCall(req).execute().use { r ->
                    val range = r.header("Content-Range").orEmpty()
                    range.substringAfter("/", "0").toLongOrNull() ?: 0L
                }
            }.getOrDefault(0L)

            val users = maxOf(1L, countRows("profiles"))
            val animeRows = countRows("anime")
            val userAnimeRows = countRows("user_anime")
            val collectionRows = countRows("collections")

            val basePostgresBytes = 7_864_320L // ~7.5 MB PostgreSQL system catalog baseline
            val animeBytes = animeRows * 640L
            val userAnimeBytes = userAnimeRows * 380L
            val profileBytes = users * 320L
            val collectionBytes = collectionRows * 256L
            val totalEstBytes = basePostgresBytes + animeBytes + userAnimeBytes + profileBytes + collectionBytes
            val limitBytes = 524_288_000L
            val pct = (totalEstBytes.toDouble() / limitBytes.toDouble()) * 100.0
            val bytesPerUser = maxOf(2048L, (userAnimeBytes + profileBytes + collectionBytes) / users)
            val remainingUsers = maxOf(0L, limitBytes - totalEstBytes) / bytesPerUser

            Result.success(
                DatabaseStorageStats(
                    totalBytes = totalEstBytes,
                    limitBytes = limitBytes,
                    usagePercent = pct,
                    warningLevel = when {
                        pct >= 95.0 -> "critical_95"
                        pct >= 85.0 -> "critical_85"
                        pct >= 75.0 -> "warning_75"
                        pct >= 60.0 -> "warning_60"
                        else -> "normal"
                    },
                    userCount = users.toInt(),
                    entryCount = userAnimeRows.toInt(),
                    reviewCount = userAnimeRows.toInt(),
                    bytesPerUser = bytesPerUser,
                    bytesPerReview = 380L,
                    estimatedRemainingUsers = remainingUsers,
                    tables = listOf(
                        TableStorageStat("anime (shared catalog)", animeRows, animeBytes, animeBytes / 4, (animeBytes * 5) / 4),
                        TableStorageStat("user_anime (tracking)", userAnimeRows, userAnimeBytes, userAnimeBytes / 3, (userAnimeBytes * 4) / 3),
                        TableStorageStat("profiles", users, profileBytes, profileBytes / 2, (profileBytes * 3) / 2),
                        TableStorageStat("collections", collectionRows, collectionBytes, collectionBytes / 2, (collectionBytes * 3) / 2)
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchInvitations(session: AuthSession): Result<List<InvitationCode>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val req = requestBuilder("/rest/v1/invitations?select=*&order=created_at.desc", session.accessToken)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parsePostgrestError(body, "Unable to fetch invitations")))
                val arr = JSONArray(body)
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        add(
                            InvitationCode(
                                id = obj.getString("id"),
                                code = obj.getString("code"),
                                maxUses = obj.optInt("max_uses", 1),
                                usesCount = obj.optInt("uses_count", 0),
                                expiresAt = obj.optString("expires_at", ""),
                                isActive = obj.optBoolean("is_active", true),
                                createdAt = obj.optString("created_at")
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInvitation(session: AuthSession, maxUses: Int, expiresInHours: Int): Result<InvitationCode> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val payload = JSONObject()
                .put("p_max_uses", maxUses.coerceIn(1, 50))
                .put("p_expires_in_hours", expiresInHours.coerceIn(1, 8760))
            val req = requestBuilder("/rest/v1/rpc/create_invitation_code", session.accessToken)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parsePostgrestError(body, "Failed to generate invite")))
                val obj = JSONObject(body)
                Result.success(
                    InvitationCode(
                        id = obj.getString("id"),
                        code = obj.getString("code"),
                        maxUses = obj.optInt("max_uses", maxUses),
                        usesCount = obj.optInt("uses_count", 0),
                        expiresAt = obj.optString("expires_at", ""),
                        isActive = obj.optBoolean("is_active", true),
                        createdAt = obj.optString("created_at")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeInvitation(session: AuthSession, invitationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val payload = JSONObject().put("is_active", false)
            val req = requestBuilder("/rest/v1/invitations?id=eq.$invitationId", session.accessToken)
                .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) Result.failure(Exception(parsePostgrestError(body, "Failed to revoke invitation")))
                else Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveCommunity(session: AuthSession): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val req = requestBuilder("/rest/v1/rpc/leave_community", session.accessToken)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) Result.failure(Exception(parsePostgrestError(body, "Failed to leave community")))
                else Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parsePostgrestError(body: String, fallback: String): String {
        val raw = try {
            val obj = JSONObject(body)
            obj.optString("message").takeIf { it.isNotBlank() }
                ?: obj.optString("hint").takeIf { it.isNotBlank() }
                ?: fallback
        } catch (_: Exception) {
            fallback
        }
        return ErrorSanitizer.sanitize(raw, fallback)
    }
}
