package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.repository.AnimeRepository
import java.time.LocalDate

class ScheduleUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(context)
            val repository = AnimeRepository(
                animeDao = database.animeDao(),
                historyDao = database.episodeHistoryDao(),
                apiCacheDao = database.apiCacheDao(),
                deletedAnimeDao = database.deletedAnimeDao(),
                savedAnimeDao = database.savedAnimeDao()
            )
            val result = repository.refreshSchedule()
            if (result.isSuccess) {
                checkAndNotifyUpcoming(context, database)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private suspend fun checkAndNotifyUpcoming(context: Context, database: AppDatabase) {
        val todayValue = LocalDate.now().dayOfWeek.value
        val nowSeconds = System.currentTimeMillis() / 1000
        val animeList = database.animeDao().getAllAnimeSync()
        val todayAnime = animeList.filter { it.inWatchlist && it.notificationsEnabled && it.airingDayOfWeek == todayValue }

        createNotificationChannel(context)

        for (anime in todayAnime) {
            val airingTime = anime.getEffectiveAiringTime(nowSeconds)
            val nextEp = anime.getEffectiveNextEpisodeNumber(nowSeconds)
            if (airingTime != null && nextEp != null && (airingTime - nowSeconds) in -1800L..3600L) {
                sendReleaseNotification(
                    context = context,
                    animeId = anime.id,
                    title = anime.displayTitle,
                    episodeNumber = nextEp
                )
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "anime_universe_release_notifications"
        const val CHANNEL_NAME = "Anime Universe Releases"
        const val COMMUNITY_CHANNEL_ID = "anime_universe_community_alerts"
        const val COMMUNITY_CHANNEL_NAME = "Anime Universe Community & Replies"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val releaseChannel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications when a tracked airing anime episode releases"
                }
                notificationManager.createNotificationChannel(releaseChannel)

                val communityChannel = NotificationChannel(
                    COMMUNITY_CHANNEL_ID,
                    COMMUNITY_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Mentions, replies, and community discussion alerts"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(communityChannel)
            }
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun sendReleaseNotification(
            context: Context,
            animeId: Int,
            title: String,
            episodeNumber: Int
        ) {
            if (!hasNotificationPermission(context)) return
            createNotificationChannel(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info)
                .setContentTitle("New Episode Alert")
                .setContentText("$title Episode $episodeNumber is airing soon!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(animeId, builder.build())
        }

        fun sendCommunityNotification(
            context: Context,
            notificationId: String,
            title: String,
            message: String
        ) {
            if (!hasNotificationPermission(context)) return
            runCatching {
                createNotificationChannel(context)
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("notification_id", notificationId)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId.hashCode(),
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val cleanTitle = title.ifBlank { "Anime Universe • New Notification" }
                val cleanBody = message.ifBlank { "Open Anime Universe to view your notification." }

                val builder = NotificationCompat.Builder(context, COMMUNITY_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(cleanTitle)
                    .setContentText(cleanBody)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(cleanBody))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setOnlyAlertOnce(false)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(true)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                notificationManager.notify(notificationId.hashCode(), builder.build())
            }
        }
    }
}

