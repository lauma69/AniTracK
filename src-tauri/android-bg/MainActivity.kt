package com.animeuniverse.app

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : TauriActivity() {
    companion object {
        private const val REQ_CODE_POST_NOTIFICATIONS = 7001
        private const val PREFS_NAME = "anime_universe_notif_bridge"
        private const val KEY_HAS_USER_RESPONDED_ONCE = "has_user_responded_post_notifications_v2"
        private val NEXT_NOTIF_ID = AtomicInteger(10000)
    }

    private var activeWebView: WebView? = null
    private var autoPromptDispatchedThisLaunch: Boolean = false
    private var lastReportedPermissionStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create all notification channels immediately so Android Settings -> Notifications
        // lists every category cleanly on Android 8–16 without ever crashing startup.
        runCatching { AnimeNotifScheduler.ensureChannels(applicationContext) }
        runCatching { captureNotificationClickIntent(intent) }
        runCatching {
            addOnNewIntentListener { newIntent ->
                runCatching {
                    setIntent(newIntent)
                    captureNotificationClickIntent(newIntent)
                }
            }
        }
        runCatching {
            window?.decorView?.post {
                runCatching { AnimeNotifScheduler.schedulePeriodic(applicationContext) }
                runCatching { refreshFcmPushTokenIfAvailable() }
            }
        }.onFailure {
            runCatching { AnimeNotifScheduler.schedulePeriodic(applicationContext) }
            runCatching { refreshFcmPushTokenIfAvailable() }
        }

        // Proactively ask for real Android 13+ POST_NOTIFICATIONS runtime permission on launch
        // once the Activity window is attached, so the OS dialog always appears on fresh installs.
        runCatching {
            window?.decorView?.postDelayed({
                requestNotificationPermissionIfNeededOnLaunch()
            }, 900L)
        }
    }

    private fun requestNotificationPermissionIfNeededOnLaunch() {
        if (autoPromptDispatchedThisLaunch) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        runCatching {
            val runtimeGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (runtimeGranted) return

            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val respondedBefore = prefs.getBoolean(KEY_HAS_USER_RESPONDED_ONCE, false)
            val showRationale = runCatching {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }.getOrDefault(false)

            // Ask immediately on first launch (or when Android still allows showing rationale).
            if (!respondedBefore || showRationale) {
                autoPromptDispatchedThisLaunch = true
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_CODE_POST_NOTIFICATIONS
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Immediately notify WebView if user just enabled/disabled notifications in Android Settings
        runCatching {
            AnimeNotifScheduler.ensureChannels(applicationContext)
            val status = resolveCurrentOsPermissionStatus()
            val becameGranted = status == "granted" && lastReportedPermissionStatus != "granted"
            lastReportedPermissionStatus = status
            notifyWebViewPermissionChanged(status)
            if (becameGranted) {
                AnimeNotifScheduler.schedulePeriodic(applicationContext)
                AnimeNotifScheduler.triggerImmediateCheck(applicationContext)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Ensure background polling worker is armed when app moves to background
        runCatching {
            if (resolveCurrentOsPermissionStatus() == "granted") {
                AnimeNotifScheduler.schedulePeriodic(applicationContext)
            }
        }
    }

    override fun onWebViewCreate(webView: WebView) {
        super.onWebViewCreate(webView)
        activeWebView = webView
        runCatching {
            webView.addJavascriptInterface(AnimeAndroidNotifBridge(this), "AnimeNativeBridge")
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE_POST_NOTIFICATIONS) {
            runCatching {
                // Only mark responded AFTER Android actually returned a result from the dialog
                if (permissions.isNotEmpty() || grantResults.isNotEmpty()) {
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_HAS_USER_RESPONDED_ONCE, true)
                        .apply()
                }
                val status = resolveCurrentOsPermissionStatus()
                lastReportedPermissionStatus = status
                notifyWebViewPermissionResult(status)
                notifyWebViewPermissionChanged(status)
                if (status == "granted") {
                    AnimeNotifScheduler.schedulePeriodic(applicationContext)
                    AnimeNotifScheduler.triggerImmediateCheck(applicationContext)
                }
            }
        }
    }

    fun resolveCurrentOsPermissionStatus(): String {
        return runCatching {
            AnimeNotifScheduler.ensureChannels(applicationContext)
            val areEnabledInManager = runCatching {
                NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
            }.getOrDefault(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val runtimeGranted = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (runtimeGranted && areEnabledInManager) {
                    return@runCatching "granted"
                }
                if (runtimeGranted && !areEnabledInManager) {
                    return@runCatching "denied"
                }

                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val respondedBefore = prefs.getBoolean(KEY_HAS_USER_RESPONDED_ONCE, false)
                val showRationale = runCatching {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }.getOrDefault(false)
                if (!respondedBefore || showRationale) "prompt" else "denied"
            } else {
                if (areEnabledInManager) "granted" else "denied"
            }
        }.getOrDefault("prompt")
    }

    fun triggerNativePermissionRequest() {
        runOnUiThread {
            runCatching {
                AnimeNotifScheduler.ensureChannels(applicationContext)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val runtimeGranted = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (runtimeGranted) {
                        val status = resolveCurrentOsPermissionStatus()
                        notifyWebViewPermissionResult(status)
                        notifyWebViewPermissionChanged(status)
                        return@runCatching
                    }

                    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val respondedBefore = prefs.getBoolean(KEY_HAS_USER_RESPONDED_ONCE, false)
                    val showRationale = runCatching {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }.getOrDefault(false)

                    // Only skip the runtime dialog if Android previously returned a real denial
                    // and now refuses to show the rationale dialog again.
                    if (respondedBefore && !showRationale) {
                        notifyWebViewPermissionResult("denied")
                        return@runCatching
                    }

                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQ_CODE_POST_NOTIFICATIONS
                    )
                } else {
                    val status = resolveCurrentOsPermissionStatus()
                    notifyWebViewPermissionResult(status)
                    notifyWebViewPermissionChanged(status)
                }
            }.onFailure {
                notifyWebViewPermissionResult("denied")
            }
        }
    }

    fun launchAppNotificationSettings(): Boolean {
        runOnUiThread {
            runCatching {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                startActivity(intent)
            }.onFailure {
                runCatching {
                    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(fallback)
                }
            }
        }
        return true
    }

    fun postDirectLocalNotification(title: String, body: String, type: String = "message"): Boolean {
        if (resolveCurrentOsPermissionStatus() != "granted") {
            return false
        }
        return runCatching {
            AnimeNotifScheduler.ensureChannels(applicationContext)
            val notifIdInt = NEXT_NOTIF_ID.incrementAndGet()
            val launchIntent = (applicationContext.packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(applicationContext, MainActivity::class.java)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("notification_id", "local_${notifIdInt}_${System.currentTimeMillis()}")
                putExtra("target_screen", "notifications")
                putExtra("type", type)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                notifIdInt,
                launchIntent,
                flags
            )
            val cleanTitle = title.trim().ifEmpty { "Anime Universe" }
            val cleanBody = body.trim().ifEmpty { "You have a new notification." }
            val channelId = AnimeNotifScheduler.channelForType(type)
            val smallIconRes = AnimeNotifScheduler.resolveSmallIcon(applicationContext)

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(smallIconRes)
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
                .build()

            NotificationManagerCompat.from(applicationContext).notify(notifIdInt, notification)
            true
        }.getOrDefault(false)
    }

    fun writeAuthSessionToFilesDir(
        supabaseUrl: String,
        anonKey: String,
        accessToken: String,
        refreshToken: String,
        userId: String
    ): Boolean {
        return runCatching {
            val payload = JSONObject().apply {
                put("supabase_url", supabaseUrl.trimEnd('/'))
                put("anon_key", anonKey)
                put("access_token", accessToken)
                put("refresh_token", refreshToken)
                put("user_id", userId)
            }
            File(applicationContext.filesDir, "notif_session.json").writeText(payload.toString())
            if (resolveCurrentOsPermissionStatus() == "granted") {
                AnimeNotifScheduler.schedulePeriodic(applicationContext)
            }
            true
        }.getOrDefault(false)
    }

    fun clearAuthSessionInFilesDir(): Boolean {
        return runCatching {
            val file = File(applicationContext.filesDir, "notif_session.json")
            if (file.exists()) file.delete()
            true
        }.getOrDefault(false)
    }

    fun writeLocalPrefsToFilesDir(
        masterEnabled: Boolean,
        messagesEnabled: Boolean,
        updatesEnabled: Boolean,
        remindersEnabled: Boolean,
        osPermissionStatus: String
    ): Boolean {
        return runCatching {
            val payload = JSONObject().apply {
                put("master_enabled", masterEnabled)
                put("messages_enabled", messagesEnabled)
                put("updates_enabled", updatesEnabled)
                put("reminders_enabled", remindersEnabled)
                put("os_permission_status", osPermissionStatus)
            }
            File(applicationContext.filesDir, "notif_prefs.json").writeText(payload.toString())
            true
        }.getOrDefault(false)
    }

    fun mergeSeenIdsInFilesDir(idsJson: String): Boolean {
        return runCatching {
            val incoming = JSONArray(idsJson)
            if (incoming.length() == 0) return true
            val seenFile = File(applicationContext.filesDir, "seen_notifs.json")
            val seen = LinkedHashSet<String>()
            if (seenFile.exists()) {
                runCatching {
                    val existing = JSONArray(seenFile.readText())
                    for (i in 0 until existing.length()) {
                        val s = existing.optString(i).trim()
                        if (s.isNotEmpty()) seen.add(s)
                    }
                }
            }
            for (i in 0 until incoming.length()) {
                val s = incoming.optString(i).trim()
                if (s.isNotEmpty()) seen.add(s)
            }
            val out = JSONArray()
            seen.toList().takeLast(400).forEach { out.put(it) }
            seenFile.writeText(out.toString())
            true
        }.getOrDefault(false)
    }

    private fun notifyWebViewPermissionResult(status: String) {
        val safeStatus = status.replace("'", "")
        runOnUiThread {
            activeWebView?.evaluateJavascript(
                "window.__onAnimeNativeNotifPermResult && window.__onAnimeNativeNotifPermResult('$safeStatus');",
                null
            )
        }
    }

    private fun notifyWebViewPermissionChanged(status: String) {
        val safeStatus = status.replace("'", "")
        runOnUiThread {
            activeWebView?.evaluateJavascript(
                "window.__onAnimeNativeNotifPermChanged && window.__onAnimeNativeNotifPermChanged('$safeStatus');",
                null
            )
        }
    }

    private fun captureNotificationClickIntent(incomingIntent: Intent?) {
        val extras = incomingIntent?.extras ?: return
        val notificationId = extras.getString("notification_id")
            ?: extras.getString("google.message_id")
            ?: return
        val targetScreen = extras.getString("target_screen") ?: "notifications"
        val topic = extras.getString("topic") ?: "general"
        val messageId = extras.getString("message_id") ?: ""
        val type = extras.getString("type") ?: "message"

        runCatching {
            val payload = JSONObject().apply {
                put("notification_id", notificationId)
                put("target_screen", targetScreen)
                put("topic", topic)
                put("message_id", messageId)
                put("type", type)
                put("clicked_at", System.currentTimeMillis())
            }
            val file = File(applicationContext.filesDir, "pending_notif_click.json")
            file.writeText(payload.toString())
        }
    }

    private fun refreshFcmPushTokenIfAvailable() {
        runCatching {
            val fcmClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging")
            val getInstanceMethod = fcmClass.getMethod("getInstance")
            val fcmInstance = getInstanceMethod.invoke(null) ?: return
            val getTokenMethod = fcmClass.getMethod("getToken")
            val taskObj = getTokenMethod.invoke(fcmInstance) ?: return

            val listenerClass = Class.forName("com.google.android.gms.tasks.OnCompleteListener")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                if (method.name == "onComplete" && args != null && args.isNotEmpty()) {
                    val completedTask = args[0]
                    val isSuccessful = completedTask.javaClass.getMethod("isSuccessful").invoke(completedTask) as? Boolean ?: false
                    if (isSuccessful) {
                        val token = completedTask.javaClass.getMethod("getResult").invoke(completedTask) as? String
                        if (!token.isNullOrBlank()) {
                            val tokenJson = JSONObject().apply {
                                put("token", token.trim())
                                put("provider", "fcm")
                                put("updated_at", System.currentTimeMillis())
                            }
                            File(applicationContext.filesDir, "device_push_token.json").writeText(tokenJson.toString())
                        }
                    }
                }
                null
            }
            val addOnCompleteMethod = taskObj.javaClass.getMethod("addOnCompleteListener", listenerClass)
            addOnCompleteMethod.invoke(taskObj, proxy)
        }
    }
}

class AnimeAndroidNotifBridge(private val activity: MainActivity) {
    @JavascriptInterface
    fun getPermissionStatus(): String {
        return activity.resolveCurrentOsPermissionStatus()
    }

    @JavascriptInterface
    fun requestPermission() {
        activity.triggerNativePermissionRequest()
    }

    @JavascriptInterface
    fun openNotificationSettings(): Boolean {
        return activity.launchAppNotificationSettings()
    }

    @JavascriptInterface
    fun sendLocalNotification(title: String, body: String): Boolean {
        return activity.postDirectLocalNotification(title, body, "message")
    }

    @JavascriptInterface
    fun sendLocalNotificationWithType(title: String, body: String, type: String): Boolean {
        return activity.postDirectLocalNotification(title, body, type)
    }

    @JavascriptInterface
    fun syncAuthSession(
        supabaseUrl: String,
        anonKey: String,
        accessToken: String,
        refreshToken: String,
        userId: String
    ): Boolean {
        return activity.writeAuthSessionToFilesDir(supabaseUrl, anonKey, accessToken, refreshToken, userId)
    }

    @JavascriptInterface
    fun clearAuthSession(): Boolean {
        return activity.clearAuthSessionInFilesDir()
    }

    @JavascriptInterface
    fun syncLocalPrefs(
        masterEnabled: Boolean,
        messagesEnabled: Boolean,
        updatesEnabled: Boolean,
        remindersEnabled: Boolean,
        osPermissionStatus: String
    ): Boolean {
        return activity.writeLocalPrefsToFilesDir(
            masterEnabled,
            messagesEnabled,
            updatesEnabled,
            remindersEnabled,
            osPermissionStatus
        )
    }

    @JavascriptInterface
    fun markSeenIds(idsJson: String): Boolean {
        return activity.mergeSeenIdsInFilesDir(idsJson)
    }
}
