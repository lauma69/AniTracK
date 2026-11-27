package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.AppConfig
import com.example.model.DatabaseStorageStats
import com.example.model.ThemeModePreference
import com.example.model.WatchlistLayoutMode
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.widgets.AppCard
import com.example.ui.widgets.AppIconButton
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.AppSecondaryButton
import com.example.ui.widgets.ObsidianAmbientBackground
import com.example.util.ErrorSanitizer
import com.example.viewmodel.AnimeViewModel

enum class SettingsCategory(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    ACCOUNT("Account", Icons.Filled.Person, "settings_category_account"),
    APPEARANCE("Appearance", Icons.Filled.Palette, "settings_category_appearance"),
    PREFERENCES("Preferences", Icons.Filled.Tune, "settings_category_preferences"),
    ABOUT("About", Icons.Filled.Info, "settings_category_about")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: AnimeViewModel,
    onBack: () -> Unit
) {
    val colors = AppTheme.colors
    val profile by viewModel.userProfile.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsState()
    val deleteAccountError by viewModel.deleteAccountError.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val isStorageStatsLoading by viewModel.isStorageStatsLoading.collectAsState()

    var selectedCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var deletePasswordVisible by remember { mutableStateOf(false) }

    val navigateBack: () -> Unit = {
        if (selectedCategory != null) {
            selectedCategory = null
        } else {
            onBack()
        }
    }

    BackHandler(onBack = navigateBack)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackupToUri(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackupFromUri(uri)
    }

    ObsidianAmbientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .testTag("settings_screen")
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                ) {
                    AppIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = navigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    )
                    Text(
                        text = selectedCategory?.title ?: "Settings",
                        fontFamily = CormorantSerifFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }

                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "settings_category_navigation",
                    modifier = Modifier.weight(1f)
                ) { category ->
                    when (category) {
                        null -> {
                            // Root Categories List Only
                            val themeLabel = when (preferences.themeMode) {
                                ThemeModePreference.DARK -> "Dark"
                                ThemeModePreference.LIGHT -> "Light"
                                ThemeModePreference.SYSTEM -> "System"
                            }
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = AppSpacing.md,
                                    end = AppSpacing.md,
                                    top = AppSpacing.xs,
                                    bottom = 56.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    AppCard(shape = RoundedCornerShape(24.dp)) {
                                        Column {
                                            SettingsCategoryRow(
                                                icon = SettingsCategory.ACCOUNT.icon,
                                                title = SettingsCategory.ACCOUNT.title,
                                                subtitle = profile?.username ?: "Guest mode",
                                                onClick = { selectedCategory = SettingsCategory.ACCOUNT },
                                                modifier = Modifier.testTag(SettingsCategory.ACCOUNT.testTag)
                                            )
                                            HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.45f), thickness = 1.dp)
                                            SettingsCategoryRow(
                                                icon = SettingsCategory.APPEARANCE.icon,
                                                title = SettingsCategory.APPEARANCE.title,
                                                subtitle = themeLabel,
                                                onClick = { selectedCategory = SettingsCategory.APPEARANCE },
                                                modifier = Modifier.testTag(SettingsCategory.APPEARANCE.testTag)
                                            )
                                            HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.45f), thickness = 1.dp)
                                            SettingsCategoryRow(
                                                icon = SettingsCategory.PREFERENCES.icon,
                                                title = SettingsCategory.PREFERENCES.title,
                                                subtitle = "Layout, language & backup",
                                                onClick = { selectedCategory = SettingsCategory.PREFERENCES },
                                                modifier = Modifier.testTag(SettingsCategory.PREFERENCES.testTag)
                                            )
                                            HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.45f), thickness = 1.dp)
                                            SettingsCategoryRow(
                                                icon = SettingsCategory.ABOUT.icon,
                                                title = SettingsCategory.ABOUT.title,
                                                subtitle = "v${AppConfig.APP_VERSION}",
                                                onClick = { selectedCategory = SettingsCategory.ABOUT },
                                                modifier = Modifier.testTag(SettingsCategory.ABOUT.testTag)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCategory.ACCOUNT -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = AppSpacing.md,
                                    end = AppSpacing.md,
                                    top = AppSpacing.xs,
                                    bottom = 56.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    SettingsSectionCard(title = "Account", icon = Icons.Filled.Person) {
                                        if (profile == null) {
                                            Text(
                                                text = "Guest mode",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.textSecondary
                                            )
                                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                                            AppPrimaryButton(
                                                text = "Sign In",
                                                onClick = { viewModel.exitLocalPreviewMode() },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Text(
                                                text = profile!!.username,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = colors.textPrimary
                                            )
                                            val maskedEmail = ErrorSanitizer.maskEmail(profile!!.email)
                                            if (maskedEmail.isNotBlank()) {
                                                Text(
                                                    text = maskedEmail,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.textSecondary,
                                                    modifier = Modifier.testTag("settings_masked_email")
                                                )
                                            }
                                            Text(
                                                text = profile!!.role.replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colors.accentPrimary
                                            )
                                        }
                                    }
                                }

                                if (profile != null) {
                                    item {
                                        OutlinedButton(
                                            onClick = { viewModel.signOut() },
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, colors.borderSubtle),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = colors.textPrimary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("sign_out_button")
                                        ) {
                                            Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                                            Text("Log Out", fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    item {
                                        OutlinedButton(
                                            onClick = {
                                                deletePassword = ""
                                                deletePasswordVisible = false
                                                viewModel.clearDeleteAccountError()
                                                showDeleteAccountDialog = true
                                            },
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, colors.error.copy(alpha = 0.55f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = colors.error.copy(alpha = 0.08f),
                                                contentColor = colors.error
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("delete_account_button")
                                        ) {
                                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                                            Text("Delete Account", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCategory.APPEARANCE -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = AppSpacing.md,
                                    end = AppSpacing.md,
                                    top = AppSpacing.xs,
                                    bottom = 56.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    SettingsSectionCard(title = "Appearance", icon = Icons.Filled.Palette) {
                                        Text("Theme", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                            ThemeModePreference.entries.forEach { mode ->
                                                FilterChip(
                                                    selected = preferences.themeMode == mode,
                                                    onClick = { viewModel.setThemeMode(mode) },
                                                    shape = CircleShape,
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                                                        selectedLabelColor = colors.highlight
                                                    ),
                                                    label = {
                                                        Text(
                                                            when (mode) {
                                                                ThemeModePreference.DARK -> "Dark"
                                                                ThemeModePreference.LIGHT -> "Light"
                                                                ThemeModePreference.SYSTEM -> "System"
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                                        Text("Accent", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(
                                                "CHAMPAGNE_GOLD" to "Champagne Gold",
                                                "SAKURA_PINK" to "Sakura Rose",
                                                "EMERALD_MINT" to "Emerald Mint",
                                                "INDIGO_VIOLET" to "Midnight Indigo"
                                            ).forEach { (key, label) ->
                                                val selected = preferences.accentTheme == key ||
                                                    (key == "CHAMPAGNE_GOLD" && preferences.accentTheme !in listOf("SAKURA_PINK", "EMERALD_MINT", "INDIGO_VIOLET"))
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = { viewModel.setAccentTheme(key) },
                                                    shape = CircleShape,
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                                                        selectedLabelColor = colors.highlight
                                                    ),
                                                    label = { Text(label) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCategory.PREFERENCES -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = AppSpacing.md,
                                    end = AppSpacing.md,
                                    top = AppSpacing.xs,
                                    bottom = 56.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    SettingsSectionCard(title = "Preferences", icon = Icons.Filled.Tune) {
                                        Text("Layout", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                            WatchlistLayoutMode.entries.forEach { layout ->
                                                FilterChip(
                                                    selected = preferences.watchlistLayout == layout,
                                                    onClick = { viewModel.setWatchlistLayout(layout) },
                                                    shape = CircleShape,
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                                                        selectedLabelColor = colors.highlight
                                                    ),
                                                    label = { Text(layout.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                                        Text("Title Language", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                            listOf("ENGLISH" to "English", "ROMAJI" to "Romaji", "NATIVE" to "Native").forEach { (key, label) ->
                                                FilterChip(
                                                    selected = preferences.titleLanguage == key,
                                                    onClick = { viewModel.setTitleLanguage(key) },
                                                    shape = CircleShape,
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                                                        selectedLabelColor = colors.highlight
                                                    ),
                                                    label = { Text(label) }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                                        HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                                        val context = LocalContext.current
                                        AppSecondaryButton(
                                            text = "Open Notification Settings",
                                            leadingIcon = Icons.Filled.Notifications,
                                            onClick = {
                                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                    }
                                                } else {
                                                    Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
                                                        putExtra("app_package", context.packageName)
                                                        putExtra("app_uid", context.applicationInfo.uid)
                                                    }
                                                }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                runCatching { context.startActivity(intent) }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("settings_open_notification_settings_button")
                                        )
                                        HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                                        SettingsSwitchRow(
                                            title = "Safe Mode",
                                            checked = preferences.safeModeEnabled,
                                            onCheckedChange = { viewModel.setSafeModeEnabled(it) }
                                        )

                                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                            AppSecondaryButton(
                                                text = "Export JSON",
                                                leadingIcon = Icons.Filled.Upload,
                                                onClick = { exportLauncher.launch("anime_universe_backup.json") },
                                                modifier = Modifier.weight(1f)
                                            )
                                            AppSecondaryButton(
                                                text = "Import JSON",
                                                leadingIcon = Icons.Filled.Download,
                                                onClick = { importLauncher.launch(arrayOf("application/json")) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                                        AppSecondaryButton(
                                            text = "Clear Cache",
                                            leadingIcon = Icons.Filled.DeleteForever,
                                            onClick = { viewModel.clearCatalogCache() },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        SettingsCategory.ABOUT -> {
                            LaunchedEffect(profile?.id) {
                                if (profile != null) {
                                    viewModel.refreshStorageStats()
                                }
                            }
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = AppSpacing.md,
                                    end = AppSpacing.md,
                                    top = AppSpacing.xs,
                                    bottom = 56.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    SettingsSectionCard(title = "About", icon = Icons.Filled.Info) {
                                        Text(
                                            text = "${AppConfig.APP_NAME} v${AppConfig.APP_VERSION}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                                        AppSecondaryButton(
                                            text = if (isCheckingUpdate) "Checking for Updates..." else "Check for Updates",
                                            leadingIcon = Icons.Filled.Download,
                                            enabled = !isCheckingUpdate,
                                            onClick = { viewModel.checkForUpdates(manual = true) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("settings_check_updates_button")
                                        )
                                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                            TextButton(onClick = { showPrivacyPolicyDialog = true }) {
                                                Text("Privacy Policy", color = colors.accentPrimary)
                                            }
                                            TextButton(onClick = { showTermsDialog = true }) {
                                                Text("Community Guidelines", color = colors.accentPrimary)
                                            }
                                        }
                                    }
                                }

                                if (profile != null) {
                                    item {
                                        SettingsSectionCard(title = "Cloud Storage & 500 MB Quota Monitor", icon = Icons.Filled.Tune) {
                                            val stats = storageStats ?: DatabaseStorageStats()
                                            val pctFloat = (stats.usagePercent.toFloat() / 100f).coerceIn(0.01f, 1f)
                                            val barColor = when {
                                                stats.usagePercent >= 85.0 -> colors.error
                                                stats.usagePercent >= 60.0 -> Color(0xFFFFB74D)
                                                else -> colors.accentPrimary
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${DatabaseStorageStats.formatBytes(stats.totalBytes)} / 500.00 MB",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = "${"%.2f".format(stats.usagePercent)}% Used",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = barColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = { pctFloat },
                                                color = barColor,
                                                trackColor = colors.borderSubtle,
                                                modifier = Modifier.fillMaxWidth().height(8.dp)
                                            )
                                            stats.warningBannerText?.let { warning ->
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = barColor.copy(alpha = 0.14f),
                                                    border = BorderStroke(1.dp, barColor.copy(alpha = 0.5f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = warning,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = barColor,
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Avg per User: ${DatabaseStorageStats.formatBytes(stats.bytesPerUser)} • Avg per Review: ${DatabaseStorageStats.formatBytes(stats.bytesPerReview)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary
                                            )
                                            Text(
                                                text = "Estimated Remaining Capacity: ~${stats.estimatedRemainingUsers} additional users",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.accentPrimary
                                            )
                                            if (stats.tables.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                stats.tables.take(6).forEach { tbl ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "${tbl.tableName} (${tbl.rowCount} rows)",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = colors.textSecondary
                                                        )
                                                        Text(
                                                            text = DatabaseStorageStats.formatBytes(tbl.totalBytes),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Medium,
                                                            color = colors.textPrimary
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AppSecondaryButton(
                                                text = if (isStorageStatsLoading) "Refreshing Storage Metrics..." else "Refresh Storage Diagnostics",
                                                enabled = !isStorageStatsLoading,
                                                onClick = { viewModel.refreshStorageStats() },
                                                modifier = Modifier.fillMaxWidth()
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
    }

    // Delete Account Password Verification Dialog
    if (showDeleteAccountDialog && profile != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
            shape = RoundedCornerShape(AppRadius.xl),
            containerColor = colors.backgroundSecondary,
            modifier = Modifier.testTag("delete_account_dialog"),
            title = {
                Text(
                    text = "Delete Account",
                    fontFamily = CormorantSerifFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your password to confirm. All account and watchlist data will be deleted within 5 minutes. Email re-registration unlocks after 5 minutes; username after 24 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = {
                            deletePassword = it
                            viewModel.clearDeleteAccountError()
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        isError = !deleteAccountError.isNullOrBlank(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (deletePasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { deletePasswordVisible = !deletePasswordVisible },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (deletePasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (deletePasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        shape = RoundedCornerShape(AppRadius.md),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_password_input")
                    )
                    if (!deleteAccountError.isNullOrBlank()) {
                        Text(
                            text = deleteAccountError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                            modifier = Modifier.testTag("delete_account_error")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDeletingAccount,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = Color.White
                    ),
                    onClick = {
                        viewModel.deleteAccount(deletePassword) {
                            showDeleteAccountDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Delete Forever", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = { showDeleteAccountDialog = false }
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            shape = RoundedCornerShape(AppRadius.xl),
            containerColor = colors.backgroundSecondary,
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Text(
                    "Your watchlist, ratings, and collections are stored securely under your account.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) { Text("Close", color = colors.accentPrimary) }
            }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            shape = RoundedCornerShape(AppRadius.xl),
            containerColor = colors.backgroundSecondary,
            title = { Text("Community Guidelines", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Text(
                    "Metadata tracking and community reviews only.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Close", color = colors.accentPrimary) }
            }
        )
    }
}

@Composable
private fun SettingsCategoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = colors.accentPrimary.copy(alpha = 0.14f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    AppCard(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Icon(icon, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = title.uppercase(),
                    style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 2.sp),
                    color = colors.accentPrimary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(end = AppSpacing.sm)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
