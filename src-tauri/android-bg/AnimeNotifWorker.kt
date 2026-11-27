package com.animeuniverse.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object AnimeNotifScheduler {
    private const val WORK_NAME = "anime_universe_bg_notifications"
    const val CHANNEL_ID = "anime_community_alerts"
    const val CHANNEL_REMINDERS_ID = "anime_reminders"
    const val CHANNEL_UPDATES_ID = "anime_updates"

    fun ensureChannel(context: Context) {
        ensureChannels(context)
    }

    fun ensureChannels(context: Context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val ch = NotificationChannel(
                        CHANNEL_ID,
                        "Messages & Mentions",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Replies, mentions, and community discussion alerts from Anime Universe"
                        enableVibration(true)
                        enableLights(true)
                        setShowBadge(true)
                    }
                    nm.createNotificationChannel(ch)
                }

                if (nm.getNotificationChannel(CHANNEL_REMINDERS_ID) == null) {
                    val ch = NotificationChannel(
                        CHANNEL_REMINDERS_ID,
                        "Episode Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Upcoming episode release and schedule reminders"
                        enableVibration(true)
                        setShowBadge(true)
                    }
                    nm.createNotificationChannel(ch)
                }

                if (nm.getNotificationChannel(CHANNEL_UPDATES_ID) == null) {
                    val ch = NotificationChannel(
                        CHANNEL_UPDATES_ID,
                        "Updates & Announcements",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Catalog updates, announcements, and system notifications"
                        setShowBadge(true)
                    }
                    nm.createNotificationChannel(ch)
                }
            }
        }
    }

    fun channelForType(type: String?): String {
        return when (type?.trim()?.lowercase()) {
            "reminder" -> CHANNEL_REMINDERS_ID
            "update", "system" -> CHANNEL_UPDATES_ID
            else -> CHANNEL_ID
        }
    }

    /**
     * Resolves a valid monochrome status-bar icon for Android notifications.
     * Avoids passing adaptive launcher mipmaps which fail or render blank on Android 12–16.
     */
    fun resolveSmallIcon(context: Context): Int {
        val customIcon = runCatching {
            context.resources.getIdentifier("ic_stat_anime_notification", "drawable", context.packageName)
        }.getOrDefault(0)
        if (customIcon != 0) return customIcon
        return android.R.drawable.ic_popup_reminder
    }

    fun schedulePeriodic(context: Context) {
        runCatching {
            ensureChannels(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = PeriodicWorkRequestBuilder<AnimeNotifWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }

    fun triggerImmediateCheck(context: Context) {
        runCatching {
            ensureChannels(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = OneTimeWorkRequestBuilder<AnimeNotifWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                req
            )
        }
    }
}

class AnimeNotifBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        runCatching {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                AnimeNotifScheduler.schedulePeriodic(context)
            }
        }
    }
}

class AnimeNotifWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        AnimeNotifScheduler.ensureChannels(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }
        val areEnabled = runCatching {
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        }.getOrDefault(false)
        if (!areEnabled) return Result.success()

        // Check local notification preferences before polling
        val prefsFile = File(applicationContext.filesDir, "notif_prefs.json")
        var masterEnabled = true
        var messagesEnabled = true
        var updatesEnabled = true
        var remindersEnabled = true
        if (prefsFile.exists()) {
            runCatching {
                val p = JSONObject(prefsFile.readText())
                masterEnabled = p.optBoolean("master_enabled", true)
                messagesEnabled = p.optBoolean("messages_enabled", true)
                updatesEnabled = p.optBoolean("updates_enabled", true)
                remindersEnabled = p.optBoolean("reminders_enabled", true)
            }
        }
        if (!masterEnabled) return Result.success()

        val sessionFile = File(applicationContext.filesDir, "notif_session.json")
        if (!sessionFile.exists()) return Result.success()

        val cfg = runCatching { JSONObject(sessionFile.readText()) }.getOrNull() ?: return Result.success()
        val supabaseUrl = cfg.optString("supabase_url").trimEnd('/')
        val anonKey = cfg.optString("anon_key")
        var accessToken = cfg.optString("access_token")
        var refreshToken = cfg.optString("refresh_token")
        val userId = cfg.optString("user_id")
        if (supabaseUrl.isBlank() || anonKey.isBlank() || accessToken.isBlank() || userId.isBlank()) {
            return Result.success()
        }

        val primaryEndpoint = "$supabaseUrl/rest/v1/notifications?select=id,title,message,type,metadata,sender_user_id&user_id=eq.$userId&is_read=eq.false&order=created_at.desc&limit=10"
        var (code, body) = httpGet(primaryEndpoint, anonKey, accessToken)

        if (code == 401 && refreshToken.isNotBlank()) {
            val refreshed = refreshAccessToken(supabaseUrl, anonKey, refreshToken)
            if (refreshed != null) {
                accessToken = refreshed.first
                if (refreshed.second.isNotBlank()) refreshToken = refreshed.second
                cfg.put("access_token", accessToken)
                cfg.put("refresh_token", refreshToken)
                runCatching { sessionFile.writeText(cfg.toString()) }
                val retry = httpGet(primaryEndpoint, anonKey, accessToken)
                code = retry.first
                body = retry.second
            }
        }

        if (code !in 200..299) {
            val fallbackEndpoint = "$supabaseUrl/rest/v1/user_notifications?select=id,notification_type,preview_text,sender_user_id&recipient_user_id=eq.$userId&is_read=eq.false&order=created_at.desc&limit=10"
            val fallbackResp = httpGet(fallbackEndpoint, anonKey, accessToken)
            code = fallbackResp.first
            body = fallbackResp.second
        }

        if (code !in 200..299 || body.isBlank()) return Result.success()

        val rows = runCatching { JSONArray(body) }.getOrNull() ?: return Result.success()
        val seenFile = File(applicationContext.filesDir, "seen_notifs.json")
        val seen = LinkedHashSet<String>()
        if (seenFile.exists()) {
            runCatching {
                val arr = JSONArray(seenFile.readText())
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i)
                    if (id.isNotBlank()) seen.add(id)
                }
            }
        }

        var changed = false
        val nm = NotificationManagerCompat.from(applicationContext)
        val smallIconRes = AnimeNotifScheduler.resolveSmallIcon(applicationContext)

        for (i in rows.length() - 1 downTo 0) {
            val row = rows.optJSONObject(i) ?: continue
            val id = row.optString("id").trim()
            if (id.isBlank() || seen.contains(id)) continue
            seen.add(id)
            changed = true

            val type = row.optString("type").ifBlank { row.optString("notification_type", "message") }
            val allowed = when (type) {
                "message", "reply", "mention" -> messagesEnabled
                "update" -> updatesEnabled
                "reminder" -> remindersEnabled
                else -> true
            }
            if (!allowed) continue

            val title = row.optString("title").trim().ifEmpty {
                when (type) {
                    "reply" -> "Anime Universe • New Reply"
                    "mention" -> "Anime Universe • Tagged You"
                    "update" -> "Anime Universe • Update"
                    "reminder" -> "Anime Universe • Reminder"
                    else -> "Anime Universe • Notification"
                }
            }
            val preview = row.optString("message").trim()
                .ifEmpty { row.optString("preview_text").trim() }
                .ifEmpty { "You have a new notification in Anime Universe." }

            val metadata = row.optJSONObject("metadata")
            val targetScreen = metadata?.optString("target_screen")?.ifBlank { "notifications" } ?: "notifications"
            val topic = metadata?.optString("topic") ?: "general"

            val launchIntent = (applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
                ?: Intent(applicationContext, MainActivity::class.java)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("notification_id", id)
                putExtra("target_screen", targetScreen)
                putExtra("topic", topic)
                putExtra("type", type)
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(applicationContext, id.hashCode(), launchIntent, pendingFlags)

            val channelId = AnimeNotifScheduler.channelForType(type)
            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(smallIconRes)
                .setContentTitle(title)
                .setContentText(preview)
                .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setOnlyAlertOnce(false)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            runCatching { nm.notify(id.hashCode(), notification) }
        }

        if (changed) {
            val out = JSONArray()
            seen.toList().takeLast(400).forEach { out.put(it) }
            runCatching { seenFile.writeText(out.toString()) }
        }

        return Result.success()
    }

    private fun httpGet(urlStr: String, anonKey: String, accessToken: String): Pair<Int, String> {
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            code to text
        }.getOrDefault(0 to "")
    }

    private fun refreshAccessToken(supabaseUrl: String, anonKey: String, refreshToken: String): Pair<String, String>? {
        return runCatching {
            val conn = (URL("$supabaseUrl/auth/v1/token?grant_type=refresh_token").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Content-Type", "application/json")
            }
            val payload = JSONObject().put("refresh_token", refreshToken).toString()
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code !in 200..299) return null
            val json = JSONObject(text)
            val newAccess = json.optString("access_token")
            val newRefresh = json.optString("refresh_token")
            if (newAccess.isBlank()) null else (newAccess to newRefresh)
        }.getOrNull()
    }
}
