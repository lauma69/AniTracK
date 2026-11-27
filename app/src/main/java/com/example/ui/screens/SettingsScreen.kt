package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import com.example.ui.theme.BuiltInAccentPresets
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.DarkAppColors
import com.example.ui.theme.LightAppColors
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.extractCustomAccentHex
import com.example.ui.theme.normalizeAccentThemeKey
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.resolveAccentPalette
import com.example.ui.theme.sanitizeHexColor
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
                            AppearanceSettingsTabContent(
                                themeMode = preferences.themeMode,
                                accentTheme = preferences.accentTheme,
                                onSelectThemeMode = { viewModel.setThemeMode(it) },
                                onSelectAccentTheme = { viewModel.setAccentTheme(it) }
                            )
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
                                                stats.usagePercent >= 60.0 -> colors.warning
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

private val QuickCustomAccentColors = listOf(
    "#38BDF8" to "Sky",
    "#F97316" to "Sunset",
    "#F43F5E" to "Crimson",
    "#14B8A6" to "Teal",
    "#A855F7" to "Orchid",
    "#EAB308" to "Amber"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSettingsTabContent(
    themeMode: ThemeModePreference,
    accentTheme: String,
    onSelectThemeMode: (ThemeModePreference) -> Unit,
    onSelectAccentTheme: (String) -> Unit
) {
    val colors = AppTheme.colors
    val normalizedAccent = remember(accentTheme) { normalizeAccentThemeKey(accentTheme) }
    val customHex = remember(normalizedAccent) { extractCustomAccentHex(normalizedAccent) }
    val isCustomSelected = customHex != null

    var showCustomPicker by rememberSaveable(isCustomSelected) { mutableStateOf(isCustomSelected) }
    var hexDraft by remember(customHex) { mutableStateOf(customHex ?: "#38BDF8") }
    val parsedDraftColor = remember(hexDraft) { parseHexColor(hexDraft) }

    val activeAccentLabel = remember(normalizedAccent, customHex) {
        if (customHex != null) "Custom ($customHex)"
        else BuiltInAccentPresets.firstOrNull { it.key == normalizedAccent }?.label ?: "Champagne Gold"
    }

    val darkAccentPreview = remember(normalizedAccent) { resolveAccentPalette(normalizedAccent, isDark = true).primary }
    val lightAccentPreview = remember(normalizedAccent) { resolveAccentPalette(normalizedAccent, isDark = false).primary }

    LazyColumn(
        contentPadding = PaddingValues(
            start = AppSpacing.md,
            end = AppSpacing.md,
            top = AppSpacing.xs,
            bottom = 64.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.testTag("appearance_settings_content")
    ) {
        // Section 1: Theme Selection with realistic visual preview cards
        item(key = "appearance_theme_card") {
            SettingsSectionCard(title = "Theme", icon = Icons.Filled.Palette) {
                Text(
                    text = "Choose how Anime Universe looks across every screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemePreviewCard(
                        mode = ThemeModePreference.DARK,
                        label = "Dark",
                        selected = themeMode == ThemeModePreference.DARK,
                        darkAccent = darkAccentPreview,
                        lightAccent = lightAccentPreview,
                        onClick = { onSelectThemeMode(ThemeModePreference.DARK) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_preview_card_dark")
                    )
                    ThemePreviewCard(
                        mode = ThemeModePreference.LIGHT,
                        label = "Light",
                        selected = themeMode == ThemeModePreference.LIGHT,
                        darkAccent = darkAccentPreview,
                        lightAccent = lightAccentPreview,
                        onClick = { onSelectThemeMode(ThemeModePreference.LIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_preview_card_light")
                    )
                    ThemePreviewCard(
                        mode = ThemeModePreference.SYSTEM,
                        label = "System",
                        selected = themeMode == ThemeModePreference.SYSTEM,
                        darkAccent = darkAccentPreview,
                        lightAccent = lightAccentPreview,
                        onClick = { onSelectThemeMode(ThemeModePreference.SYSTEM) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_preview_card_system")
                    )
                }
            }
        }

        // Section 2: Accent Color Circular Swatches + Custom Color
        item(key = "appearance_accent_card") {
            SettingsSectionCard(title = "Accent Color", icon = Icons.Filled.ColorLens) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Highlights, buttons & active tabs",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = CircleShape,
                        color = colors.accentPrimary.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = activeAccentLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.highlight,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Evenly spaced circular swatches (4 built-in presets + Custom swatch)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    BuiltInAccentPresets.forEach { preset ->
                        val isSelected = normalizedAccent == preset.key
                        val displayColor = remember(preset.key, colors.isDark) {
                            resolveAccentPalette(preset.key, colors.isDark).primary
                        }
                        AccentCircleSwatch(
                            label = preset.label.substringBefore(" "),
                            color = displayColor,
                            selected = isSelected,
                            onClick = {
                                showCustomPicker = false
                                onSelectAccentTheme(preset.key)
                            },
                            modifier = Modifier.testTag("accent_swatch_${preset.key.lowercase()}")
                        )
                    }

                    // Custom color circular swatch
                    val customSwatchColor = if (customHex != null) {
                        parseHexColor(customHex) ?: colors.accentPrimary
                    } else null
                    AccentCircleSwatch(
                        label = "Custom",
                        color = customSwatchColor,
                        isRainbowPlaceholder = customSwatchColor == null,
                        selected = isCustomSelected || showCustomPicker,
                        onClick = { showCustomPicker = !showCustomPicker },
                        modifier = Modifier.testTag("accent_swatch_custom")
                    )
                }

                AnimatedVisibility(visible = showCustomPicker) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(color = colors.borderSubtle)
                        Text(
                            text = "Custom Color",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )

                        // Curated quick-pick custom swatches
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickCustomAccentColors.forEach { (hex, name) ->
                                val swatchColor = parseHexColor(hex) ?: colors.accentPrimary
                                val isActive = customHex.equals(hex, ignoreCase = true)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isActive) 2.5.dp else 1.dp,
                                            color = if (isActive) colors.textPrimary else colors.borderSubtle,
                                            shape = CircleShape
                                        )
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .clickable {
                                            hexDraft = hex
                                            onSelectAccentTheme("CUSTOM:$hex")
                                        }
                                        .testTag("custom_accent_quick_${name.lowercase()}")
                                ) {
                                    if (isActive) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "$name selected",
                                            tint = if (swatchColor.luminance() > 0.45f) Color(0xFF101218) else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Hex Input + Live Preview + Apply
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(parsedDraftColor ?: colors.elevatedSurface)
                                    .border(1.5.dp, colors.borderStrong, CircleShape)
                            )
                            OutlinedTextField(
                                value = hexDraft,
                                onValueChange = { input ->
                                    val filtered = input.uppercase().filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }.take(7)
                                    hexDraft = if (filtered.isNotEmpty() && !filtered.startsWith("#")) "#$filtered" else filtered
                                },
                                label = { Text("Hex Code") },
                                placeholder = { Text("#38BDF8") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_accent_hex_input")
                            )
                            Button(
                                onClick = {
                                    sanitizeHexColor(hexDraft)?.let { validHex ->
                                        hexDraft = validHex
                                        onSelectAccentTheme("CUSTOM:$validHex")
                                    }
                                },
                                enabled = parsedDraftColor != null,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accentPrimary,
                                    contentColor = colors.onAccentPrimary
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("custom_accent_apply_button")
                            ) {
                                Text("Apply", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    mode: ThemeModePreference,
    label: String,
    selected: Boolean,
    darkAccent: Color,
    lightAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.accentPrimary.copy(alpha = if (colors.isDark) 0.14f else 0.10f) else colors.backgroundSecondary,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) colors.accentPrimary else colors.borderSubtle
        ),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Realistic miniature app UI preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
            ) {
                when (mode) {
                    ThemeModePreference.DARK -> MiniThemePreviewCanvas(
                        bg = DarkAppColors.backgroundMain,
                        surface = DarkAppColors.cardBackground,
                        textPrimary = DarkAppColors.textPrimary,
                        textMuted = DarkAppColors.textMuted,
                        accent = darkAccent
                    )
                    ThemeModePreference.LIGHT -> MiniThemePreviewCanvas(
                        bg = LightAppColors.backgroundMain,
                        surface = LightAppColors.cardBackground,
                        textPrimary = LightAppColors.textPrimary,
                        textMuted = LightAppColors.textMuted,
                        accent = lightAccent
                    )
                    ThemeModePreference.SYSTEM -> Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            MiniThemePreviewCanvas(
                                bg = DarkAppColors.backgroundMain,
                                surface = DarkAppColors.cardBackground,
                                textPrimary = DarkAppColors.textPrimary,
                                textMuted = DarkAppColors.textMuted,
                                accent = darkAccent
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            MiniThemePreviewCanvas(
                                bg = LightAppColors.backgroundMain,
                                surface = LightAppColors.cardBackground,
                                textPrimary = LightAppColors.textPrimary,
                                textMuted = LightAppColors.textMuted,
                                accent = lightAccent
                            )
                        }
                    }
                }

                if (selected) {
                    Surface(
                        shape = CircleShape,
                        color = colors.accentPrimary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "$label selected",
                                tint = colors.onAccentPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) colors.highlight else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniThemePreviewCanvas(
    bg: Color,
    surface: Color,
    textPrimary: Color,
    textMuted: Color,
    accent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Mini top title bar + accent dot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(textPrimary.copy(alpha = 0.85f))
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
        // Mini card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(7.dp))
                .background(surface)
                .padding(5.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(textPrimary.copy(alpha = 0.72f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .height(3.5.dp)
                        .clip(CircleShape)
                        .background(textMuted.copy(alpha = 0.55f))
                )
            }
            // Mini accent action pill
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

@Composable
private fun AccentCircleSwatch(
    label: String,
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRainbowPlaceholder: Boolean = false
) {
    val colors = AppTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) colors.textPrimary else colors.borderSubtle,
                    shape = CircleShape
                )
                .padding(4.dp)
                .clip(CircleShape)
                .then(
                    if (isRainbowPlaceholder || color == null) {
                        Modifier.background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFF43F5E),
                                    Color(0xFFF59E0B),
                                    Color(0xFF10B981),
                                    Color(0xFF38BDF8),
                                    Color(0xFF8B5CF6),
                                    Color(0xFFF43F5E)
                                )
                            )
                        )
                    } else {
                        Modifier.background(color)
                    }
                )
        ) {
            if (selected) {
                val iconTint = if (color != null && color.luminance() > 0.45f) Color(0xFF101218) else Color.White
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "$label selected",
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) colors.highlight else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

