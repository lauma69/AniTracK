package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.window.DialogProperties
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ui.screens.AnimeDetailScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyListScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AnimeUniverseTheme
import com.example.ui.theme.AppMotion
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.InterUiFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.widgets.ActionFeedbackBanner
import com.example.ui.widgets.MidnightBottomNavigationBar
import com.example.ui.widgets.MidnightCompactRail
import com.example.ui.widgets.MidnightDesktopSidebar
import com.example.ui.widgets.NavDestinationItem
import com.example.ui.widgets.ObsidianAmbientBackground
import com.example.ui.widgets.QuickSearchBottomSheet
import com.example.ui.widgets.rememberReducedMotion
import com.example.viewmodel.AnimeViewModel
import com.example.viewmodel.OnboardingStage
import com.example.worker.ScheduleUpdateWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class MainNavTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Discover("discover", "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    MyList("mylist", "Library", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
    Community("community", "Community", Icons.Filled.Group, Icons.Outlined.Group),
    Profile("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    private val viewModel: AnimeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        runCatching {
            ScheduleUpdateWorker.createNotificationChannel(applicationContext)
        }
        runCatching {
            window?.decorView?.post {
                runCatching {
                    val syncWorkRequest = PeriodicWorkRequestBuilder<ScheduleUpdateWorker>(
                        30, TimeUnit.MINUTES
                    ).build()
                    WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                        "anime_universe_schedule_sync_work",
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncWorkRequest
                    )
                }
            }
        }

        runCatching {
            viewModel.handleDeepLinkUri(intent?.data)
        }

        setContent {
            val preferences by viewModel.preferences.collectAsState()
            val authSession by viewModel.authSession.collectAsState()
            val isRestoringSession by viewModel.isRestoringSession.collectAsState()
            val needsInviteRedemption by viewModel.needsInviteRedemption.collectAsState()
            val onboardingStage by viewModel.onboardingStage.collectAsState()
            val isLocalPreviewMode by viewModel.isLocalPreviewMode.collectAsState()
            val isPasswordRecoveryPending by viewModel.isPasswordRecoveryPending.collectAsState()
            val authError by viewModel.authError.collectAsState()
            val userMessage by viewModel.userMessage.collectAsState()
            val availableUpdate by viewModel.availableUpdate.collectAsState()

            AnimeUniverseTheme(
                themeMode = preferences.themeMode,
                accentTheme = preferences.accentTheme
            ) {
                val colors = AppTheme.colors
                SideEffect {
                    val barStyle = if (colors.isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                    enableEdgeToEdge(
                        statusBarStyle = barStyle,
                        navigationBarStyle = barStyle
                    )
                }
                var showSplash by rememberSaveable { mutableStateOf(true) }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(userMessage, showSplash) {
                    if (!showSplash && userMessage != null) {
                        val msg = userMessage!!
                        val currentFeedback = viewModel.dynamicIslandFeedback.value
                        if (currentFeedback == null) {
                            snackbarHostState.showSnackbar(msg)
                        }
                        viewModel.clearUserMessage()
                    }
                }

                val hasSignedInAccount = authSession != null &&
                    !needsInviteRedemption &&
                    onboardingStage == OnboardingStage.NONE &&
                    authSession!!.onboardingCompleted
                val showWorkspace = hasSignedInAccount || isLocalPreviewMode

                var requestedPostNotifications by rememberSaveable { mutableStateOf(false) }
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(showWorkspace, showSplash) {
                    if (showWorkspace && !showSplash && !requestedPostNotifications) {
                        requestedPostNotifications = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                runCatching {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.backgroundMain)
                ) {
                    if (showWorkspace) {
                        AnimeUniverseMainScaffold(
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState
                        )
                    } else if (!isRestoringSession) {
                        AuthScreen(
                            viewModel = viewModel,
                            onboardingStage = onboardingStage,
                            needsInviteOnlyGate = authSession != null && needsInviteRedemption
                        )
                    }

                    if (isPasswordRecoveryPending) {
                        var newPassword by remember { mutableStateOf("") }
                        var confirmNewPassword by remember { mutableStateOf("") }
                        var localResetError by remember { mutableStateOf<String?>(null) }

                        AlertDialog(
                            onDismissRequest = { viewModel.dismissPasswordRecovery() },
                            shape = RoundedCornerShape(AppRadius.xl),
                            containerColor = colors.backgroundSecondary,
                            title = { Text("Set New Password", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                    Text(
                                        text = "Choose a new password (at least 6 characters) for your ANIME UNIVERSE account.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary
                                    )
                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it; localResetError = null },
                                        label = { Text("New Password") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        shape = RoundedCornerShape(AppRadius.md),
                                        modifier = Modifier.fillMaxWidth().testTag("recovery_new_password_input")
                                    )
                                    OutlinedTextField(
                                        value = confirmNewPassword,
                                        onValueChange = { confirmNewPassword = it; localResetError = null },
                                        label = { Text("Confirm Password") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        shape = RoundedCornerShape(AppRadius.md),
                                        modifier = Modifier.fillMaxWidth().testTag("recovery_confirm_password_input")
                                    )
                                    val msg = localResetError ?: authError
                                    if (!msg.isNullOrBlank()) {
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.error
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        when {
                                            newPassword.length < 6 -> localResetError = "Password must be at least 6 characters."
                                            confirmNewPassword.isNotBlank() && newPassword != confirmNewPassword -> {
                                                localResetError = "Passwords do not match."
                                            }
                                            else -> viewModel.submitNewPassword(newPassword)
                                        }
                                    },
                                    modifier = Modifier.testTag("recovery_submit_button")
                                ) {
                                    Text("Update Password")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.dismissPasswordRecovery() }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (availableUpdate != null && !showSplash) {
                        val updateInfo = availableUpdate!!
                        AlertDialog(
                            onDismissRequest = {
                                if (!updateInfo.isMandatory) viewModel.dismissUpdatePrompt()
                            },
                            properties = DialogProperties(
                                dismissOnBackPress = !updateInfo.isMandatory,
                                dismissOnClickOutside = !updateInfo.isMandatory,
                                usePlatformDefaultWidth = false
                            ),
                            shape = RoundedCornerShape(AppRadius.xl),
                            containerColor = colors.backgroundSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp)
                                .padding(horizontal = 24.dp)
                                .testTag("new_update_dialog"),
                            title = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "New Update Available",
                                        fontFamily = CormorantSerifFamily,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Version v${updateInfo.versionName}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.accentPrimary,
                                        modifier = Modifier.testTag("update_dialog_version")
                                    )
                                }
                            },
                            text = {
                                Text(
                                    text = updateInfo.releaseNotes.ifBlank {
                                        "A new release of ANIME UNIVERSE is ready with performance improvements and new features."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    modifier = Modifier.testTag("update_dialog_notes")
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val targetUrl = updateInfo.downloadUrl.ifBlank {
                                            "https://play.google.com/store/apps/details?id=$packageName"
                                        }
                                        runCatching {
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                                        }
                                        if (!updateInfo.isMandatory) {
                                            viewModel.dismissUpdatePrompt()
                                        }
                                    },
                                    modifier = Modifier.testTag("update_now_button")
                                ) {
                                    Text("Update Now", fontWeight = FontWeight.SemiBold)
                                }
                            },
                            dismissButton = if (!updateInfo.isMandatory) {
                                {
                                    TextButton(
                                        onClick = { viewModel.dismissUpdatePrompt() },
                                        modifier = Modifier.testTag("update_remind_later_button")
                                    ) {
                                        Text("Remind Me Later", color = colors.textSecondary)
                                    }
                                }
                            } else null
                        )
                    }

                    AnimatedVisibility(
                        visible = showSplash || isRestoringSession,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(260))
                    ) {
                        AnimeUniverseSplashOverlay(onSplashFinished = { showSplash = false })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        runCatching { viewModel.onAppForegrounded() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        runCatching {
            setIntent(intent)
            viewModel.handleDeepLinkUri(intent.data)
        }
    }
}

@Composable
private fun AnimeUniverseMainScaffold(
    viewModel: AnimeViewModel,
    snackbarHostState: SnackbarHostState
) {
    val colors = AppTheme.colors
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTab = MainNavTab.entries.firstOrNull { it.route == currentRoute }
    val showBottomDock = currentTab != null && currentTab != MainNavTab.Profile
    val showSidebar = currentTab != null

    val watchlist by viewModel.watchlist.collectAsState()
    val trending by viewModel.trendingAnime.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val feedback by viewModel.dynamicIslandFeedback.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val unseenDiscussionAnime by viewModel.unseenDiscussionAnime.collectAsState()
    val hasCommunityRedDot = unreadNotificationCount > 0 || unseenDiscussionAnime.isNotEmpty()

    var showQuickSearch by remember { mutableStateOf(false) }

    val allDestinations = remember(hasCommunityRedDot) {
        MainNavTab.entries.map {
            NavDestinationItem(
                route = it.route,
                title = it.title,
                selectedIcon = it.selectedIcon,
                unselectedIcon = it.unselectedIcon,
                showRedDot = (it == MainNavTab.Community && hasCommunityRedDot)
            )
        }
    }
    val bottomDestinations = remember(allDestinations) {
        allDestinations.filter { it.route != MainNavTab.Profile.route }
    }

    val navigateToTab: (MainNavTab) -> Unit = { tab ->
        if (currentRoute != tab.route) {
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundMain)
    ) {
        val isCompactScreen = maxWidth < 600.dp
        val isDesktopScreen = maxWidth >= 900.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (showSidebar && !isCompactScreen) {
                if (isDesktopScreen) {
                    MidnightDesktopSidebar(
                        destinations = allDestinations,
                        currentRoute = currentRoute,
                        onSelect = { dest ->
                            MainNavTab.entries.firstOrNull { it.route == dest.route }?.let(navigateToTab)
                        },
                        onOpenQuickSearch = { showQuickSearch = true }
                    )
                } else {
                    MidnightCompactRail(
                        destinations = allDestinations,
                        currentRoute = currentRoute,
                        onSelect = { dest ->
                            MainNavTab.entries.firstOrNull { it.route == dest.route }?.let(navigateToTab)
                        },
                        onOpenQuickSearch = { showQuickSearch = true }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = MainNavTab.Home.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(220)) + scaleIn(
                            initialScale = 0.985f,
                            animationSpec = tween(220)
                        )
                    },
                    exitTransition = { fadeOut(animationSpec = tween(170)) },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(220)) + scaleIn(
                            initialScale = 0.985f,
                            animationSpec = tween(220)
                        )
                    },
                    popExitTransition = { fadeOut(animationSpec = tween(170)) }
                ) {
                composable(MainNavTab.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onSelectAnime = { id -> navController.navigate("detail/$id") },
                        onNavigateToDiscover = { navigateToTab(MainNavTab.Discover) },
                        onNavigateToMyList = { navigateToTab(MainNavTab.MyList) },
                        onNavigateToCommunity = { navigateToTab(MainNavTab.Community) },
                        onNavigateToProfile = { navigateToTab(MainNavTab.Profile) },
                        onOpenQuickSearch = { showQuickSearch = true }
                    )
                }

                composable(MainNavTab.Discover.route) {
                    DiscoverScreen(
                        viewModel = viewModel,
                        onSelectAnime = { id -> navController.navigate("detail/$id") },
                        onNavigateToProfile = { navigateToTab(MainNavTab.Profile) }
                    )
                }

                composable(MainNavTab.MyList.route) {
                    MyListScreen(
                        viewModel = viewModel,
                        onSelectAnime = { id -> navController.navigate("detail/$id") },
                        onNavigateToDiscover = { navigateToTab(MainNavTab.Discover) },
                        onNavigateToCommunity = { navigateToTab(MainNavTab.Community) },
                        onNavigateToProfile = { navigateToTab(MainNavTab.Profile) }
                    )
                }

                composable(MainNavTab.Community.route) {
                    CommunityScreen(
                        viewModel = viewModel,
                        onSelectAnime = { id -> navController.navigate("detail/$id") },
                        onNavigateToProfile = { navigateToTab(MainNavTab.Profile) }
                    )
                }

                composable(MainNavTab.Profile.route) {
                    ProfileScreen(
                        viewModel = viewModel,
                        onBack = { navigateToTab(MainNavTab.Home) },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onSelectAnime = { id -> navController.navigate("detail/$id") }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "detail/{animeId}",
                    arguments = listOf(navArgument("animeId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getInt("animeId") ?: 0
                    AnimeDetailScreen(
                        animeId = animeId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onSelectAnime = { nextId -> navController.navigate("detail/$nextId") }
                    )
                }
            }

            // Action Feedback Toast Banner (+ Undo)
            ActionFeedbackBanner(
                feedback = feedback,
                onUndo = { viewModel.undoLastEpisodeAction() },
                onDismiss = { viewModel.dismissDynamicIslandFeedback() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .widthIn(max = 560.dp)
            )

            // Floating Capsule Bottom Navigation Bar on Compact Mobile
            if (showBottomDock && isCompactScreen) {
                MidnightBottomNavigationBar(
                    destinations = bottomDestinations,
                    currentRoute = currentRoute,
                    onSelect = { dest ->
                        MainNavTab.entries.firstOrNull { it.route == dest.route }?.let(navigateToTab)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showBottomDock && isCompactScreen) 84.dp else 16.dp)
            )
            }
        }
    }

    if (showQuickSearch) {
        BackHandler { showQuickSearch = false }
        QuickSearchBottomSheet(
            watchlist = watchlist,
            trending = trending,
            titlePreference = preferences.titleLanguage,
            onDismiss = { showQuickSearch = false },
            onSelectAnime = { id -> navController.navigate("detail/$id") },
            onSearchInCatalog = { q ->
                viewModel.updateSearchQuery(q)
                navigateToTab(MainNavTab.Discover)
            }
        )
    }
}

@Composable
fun AnimeUniverseSplashOverlay(onSplashFinished: () -> Unit) {
    val colors = AppTheme.colors
    val reducedMotion = rememberReducedMotion()
    val containerAlpha = remember { Animatable(1f) }
    val titleAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val dividerWidth = remember { Animatable(if (reducedMotion) 88f else 0f) }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            delay(320)
        } else {
            launch { titleAlpha.animateTo(1f, tween(340)) }
            delay(120)
            launch { dividerWidth.animateTo(88f, tween(320)) }
            delay(500)
            containerAlpha.animateTo(0f, tween(240))
        }
        onSplashFinished()
    }

    ObsidianAmbientBackground(
        modifier = Modifier.graphicsLayer { alpha = containerAlpha.value }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = AppSpacing.xl)
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("ANIME ")
                        withStyle(
                            SpanStyle(
                                color = colors.accentPrimary,
                                fontStyle = FontStyle.Italic
                            )
                        ) {
                            append("UNIVERSE")
                        }
                    },
                    style = TextStyle(
                        fontFamily = CormorantSerifFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 32.sp,
                        letterSpacing = 2.sp,
                        color = colors.textPrimary
                    ),
                    modifier = Modifier.graphicsLayer { alpha = titleAlpha.value }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(dividerWidth.value.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(colors.accentSecondary, colors.accentPrimary, colors.accentSecondary)
                            )
                        )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ANIME & DONGHUA TRACKER",
                    style = MicroUpperLabel.copy(
                        fontFamily = InterUiFamily,
                        fontSize = 9.5.sp,
                        letterSpacing = 2.8.sp
                    ),
                    color = colors.textMuted,
                    modifier = Modifier.graphicsLayer { alpha = titleAlpha.value }
                )
            }
        }
    }
}
