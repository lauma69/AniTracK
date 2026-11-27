package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ThemeModePreference
import com.example.model.WatchlistLayoutMode
import com.example.util.SecureStorageCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore by preferencesDataStore(name = "anime_universe_settings")

data class AppPreferencesState(
    val themeMode: ThemeModePreference = ThemeModePreference.DARK,
    val accentTheme: String = "CHAMPAGNE_GOLD",
    val watchlistLayout: WatchlistLayoutMode = WatchlistLayoutMode.GRID,
    val notificationsEnabled: Boolean = true,
    val titleLanguage: String = "ENGLISH",
    val safeModeEnabled: Boolean = true,
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = ""
)

class PreferencesRepository(context: Context) {
    private val appContext = context.applicationContext

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val WATCHLIST_LAYOUT = stringPreferencesKey("watchlist_layout")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val TITLE_LANGUAGE = stringPreferencesKey("title_language")
        val SAFE_MODE = booleanPreferencesKey("safe_mode")
        val SUPABASE_URL = stringPreferencesKey("supabase_url")
        val SUPABASE_ANON_KEY = stringPreferencesKey("supabase_anon_key")
    }

    val preferencesFlow: Flow<AppPreferencesState> = appContext.appPrefsDataStore.data.map { prefs ->
        val themeMode = when (prefs[Keys.THEME_MODE]) {
            ThemeModePreference.LIGHT.name -> ThemeModePreference.LIGHT
            ThemeModePreference.SYSTEM.name -> ThemeModePreference.SYSTEM
            else -> ThemeModePreference.DARK
        }
        val layout = when (prefs[Keys.WATCHLIST_LAYOUT]) {
            WatchlistLayoutMode.LIST.name -> WatchlistLayoutMode.LIST
            else -> WatchlistLayoutMode.GRID
        }
        val rawAccent = prefs[Keys.ACCENT_THEME]
        val resolvedAccent = when {
            rawAccent.isNullOrBlank() || rawAccent.equals("COSMIC_CYAN", ignoreCase = true) -> "CHAMPAGNE_GOLD"
            else -> rawAccent
        }
        AppPreferencesState(
            themeMode = themeMode,
            accentTheme = resolvedAccent,
            watchlistLayout = layout,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            titleLanguage = prefs[Keys.TITLE_LANGUAGE] ?: "ENGLISH",
            safeModeEnabled = prefs[Keys.SAFE_MODE] ?: true,
            supabaseUrl = SecureStorageCodec.decrypt(prefs[Keys.SUPABASE_URL].orEmpty()),
            supabaseAnonKey = SecureStorageCodec.decrypt(prefs[Keys.SUPABASE_ANON_KEY].orEmpty())
        )
    }

    suspend fun setThemeMode(mode: ThemeModePreference) {
        appContext.appPrefsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentTheme(accent: String) {
        appContext.appPrefsDataStore.edit { it[Keys.ACCENT_THEME] = accent }
    }

    suspend fun setWatchlistLayout(layout: WatchlistLayoutMode) {
        appContext.appPrefsDataStore.edit { it[Keys.WATCHLIST_LAYOUT] = layout.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appContext.appPrefsDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setTitleLanguage(lang: String) {
        appContext.appPrefsDataStore.edit { it[Keys.TITLE_LANGUAGE] = lang.uppercase() }
    }

    suspend fun setSafeModeEnabled(enabled: Boolean) {
        appContext.appPrefsDataStore.edit { it[Keys.SAFE_MODE] = enabled }
    }

    suspend fun setSupabaseConfig(url: String, anonKey: String) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = anonKey.trim()
        appContext.appPrefsDataStore.edit { prefs ->
            prefs[Keys.SUPABASE_URL] = SecureStorageCodec.encrypt(cleanUrl)
            prefs[Keys.SUPABASE_ANON_KEY] = SecureStorageCodec.encrypt(cleanKey)
        }
    }
}
