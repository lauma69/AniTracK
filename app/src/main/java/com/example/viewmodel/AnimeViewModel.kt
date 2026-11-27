package com.example.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthCallbackOutcome
import com.example.auth.SignUpOutcome
import com.example.auth.SupabaseAuthService
import com.example.config.AppConfig
import com.example.data.local.AnimeEntity
import com.example.data.local.AppDatabase
import com.example.data.repository.AnimeRepository
import com.example.data.repository.AppPreferencesState
import com.example.data.repository.OwnerVaultStore
import com.example.data.repository.PreferencesRepository
import com.example.model.AnimeCollection
import com.example.model.AnimeCommentItem
import com.example.model.AnimeRichDetail
import com.example.model.AuthSession
import com.example.model.BuiltInAvatar
import com.example.model.CommunityActivityItem
import com.example.model.CommunityCapacity
import com.example.model.CommunityEmoji
import com.example.model.CommunityMember
import com.example.model.CommunityReview
import com.example.model.DatabaseStorageStats
import com.example.model.FlatReplyTo
import com.example.model.InvitationCode
import com.example.model.MemberStatus
import com.example.model.MentionableUser
import com.example.model.NotificationJumpTarget
import com.example.model.PrivacySettings
import com.example.model.StarRatingUtils
import com.example.model.ThemeModePreference
import com.example.model.UnseenDiscussionAnime
import com.example.model.UserNotificationItem
import com.example.model.UserProfile
import com.example.model.VisibilityLevel
import com.example.model.WatchStatus
import com.example.model.WatchlistLayoutMode
import com.example.service.AniListMetadataProvider
import com.example.service.AppUpdateInfo
import com.example.service.CommunityEmojiCacheService
import com.example.service.DiscussionRealtimeEvent
import com.example.service.NotificationRealtimeEvent
import com.example.service.SupabaseDatabaseService
import com.example.service.SupabaseDiscussionService
import com.example.util.DiscussionRules
import com.example.util.ErrorSanitizer
import com.example.worker.ScheduleUpdateWorker
import okhttp3.WebSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

enum class ReviewSortOption(val label: String) {
    NEWEST("Newest"),
    HIGHEST_RATED("Highest Rated"),
    MOST_LIKED("Most Liked")
}

enum class OnboardingStage {
    NONE,
    USERNAME,
    AVATAR
}

data class DynamicIslandFeedback(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val subtitle: String,
    val animeId: Int? = null,
    val previousEpisode: Int? = null,
    val previousStatus: WatchStatus? = null,
    val canUndo: Boolean = false
)

class AnimeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val metadataProvider = AniListMetadataProvider()
    private val repository = AnimeRepository(
        animeDao = database.animeDao(),
        historyDao = database.episodeHistoryDao(),
        apiCacheDao = database.apiCacheDao(),
        deletedAnimeDao = database.deletedAnimeDao(),
        savedAnimeDao = database.savedAnimeDao(),
        metadataProvider = metadataProvider,
        onCompactDatabase = { database.compactDatabase() }
    )
    private val preferencesRepo = PreferencesRepository(application)
    private val ownerVaultStore = OwnerVaultStore(application)
    private val supabaseAuth = SupabaseAuthService(application)
    private val supabaseDb = SupabaseDatabaseService()
    private val supabaseDiscussion = SupabaseDiscussionService()

    @Volatile
    private var activeOwnerKey: String? = null
    private val dirtyPendingSyncIds = mutableSetOf<Int>()

    // App Preferences (DataStore)
    val preferences: StateFlow<AppPreferencesState> = preferencesRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferencesState())

    // Global Feedback & Dynamic Island Morph State
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()
    fun clearUserMessage() { _userMessage.value = null }
    fun showMessage(message: String) { _userMessage.value = message }

    private val _dynamicIslandFeedback = MutableStateFlow<DynamicIslandFeedback?>(null)
    val dynamicIslandFeedback = _dynamicIslandFeedback.asStateFlow()
    fun dismissDynamicIslandFeedback() { _dynamicIslandFeedback.value = null }

    // Explicit Rating & Community Note Submission Feedback State
    private val _ratingFeedbackMessage = MutableStateFlow<String?>(null)
    val ratingFeedbackMessage = _ratingFeedbackMessage.asStateFlow()
    fun clearRatingFeedback() { _ratingFeedbackMessage.value = null }

    private val _isSubmittingCommunityNote = MutableStateFlow(false)
    val isSubmittingCommunityNote = _isSubmittingCommunityNote.asStateFlow()

    private val _communityNoteFeedback = MutableStateFlow<String?>(null)
    val communityNoteFeedback = _communityNoteFeedback.asStateFlow()

    private val _communityNoteError = MutableStateFlow<String?>(null)
    val communityNoteError = _communityNoteError.asStateFlow()
    fun clearCommunityNoteFeedback() {
        _communityNoteFeedback.value = null
        _communityNoteError.value = null
    }

    // Discussion Comments, Mentions & Threaded Replies State
    private var activeDiscussionAnimeId: Int? = null
    private val _discussionComments = MutableStateFlow<List<AnimeCommentItem>>(emptyList())
    val discussionComments = _discussionComments.asStateFlow()

    private val _discussionTotalCount = MutableStateFlow(0)
    val discussionTotalCount = _discussionTotalCount.asStateFlow()

    private val _discussionHasMore = MutableStateFlow(false)
    val discussionHasMore = _discussionHasMore.asStateFlow()

    private val _isDiscussionLoading = MutableStateFlow(false)
    val isDiscussionLoading = _isDiscussionLoading.asStateFlow()

    private val _isDiscussionSubmitting = MutableStateFlow(false)
    val isDiscussionSubmitting = _isDiscussionSubmitting.asStateFlow()

    private val _discussionSuccessMessage = MutableStateFlow<String?>(null)
    val discussionSuccessMessage = _discussionSuccessMessage.asStateFlow()

    private val _discussionErrorMessage = MutableStateFlow<String?>(null)
    val discussionErrorMessage = _discussionErrorMessage.asStateFlow()

    private val _mentionSuggestions = MutableStateFlow<List<MentionableUser>>(emptyList())
    val mentionSuggestions = _mentionSuggestions.asStateFlow()
    private var mentionSearchJob: Job? = null
    private var activeChatRealtimeSocket: WebSocket? = null
    private var activeRealtimeAnimeId: Int? = null
    private val chatProfileCache = LinkedHashMap<String, MentionableUser>()

    // Custom Community Emojis (Supabase Storage multi-layer cache)
    private val communityEmojiCache = CommunityEmojiCacheService(application)
    private val _communityEmojis = MutableStateFlow<List<CommunityEmoji>>(emptyList())
    val communityEmojis = _communityEmojis.asStateFlow()

    // Notifications State
    private val _notifications = MutableStateFlow<List<UserNotificationItem>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount = _unreadNotificationCount.asStateFlow()

    private val _unseenDiscussionAnime = MutableStateFlow<List<UnseenDiscussionAnime>>(emptyList())
    val unseenDiscussionAnime = _unseenDiscussionAnime.asStateFlow()

    private val _hasMoreNotifications = MutableStateFlow(false)
    val hasMoreNotifications = _hasMoreNotifications.asStateFlow()

    private val _isNotificationsLoading = MutableStateFlow(false)
    val isNotificationsLoading = _isNotificationsLoading.asStateFlow()

    private val _notificationsError = MutableStateFlow<String?>(null)
    val notificationsError = _notificationsError.asStateFlow()

    private val _pendingNotificationJump = MutableStateFlow<NotificationJumpTarget?>(null)
    val pendingNotificationJump = _pendingNotificationJump.asStateFlow()

    fun setPendingNotificationJump(target: NotificationJumpTarget) {
        _pendingNotificationJump.value = target
    }

    fun consumePendingNotificationJump() {
        _pendingNotificationJump.value = null
    }

    fun clearDiscussionFeedback() {
        _discussionSuccessMessage.value = null
        _discussionErrorMessage.value = null
    }

    private val _focusedArtworkUrl = MutableStateFlow<String?>(null)
    val focusedArtworkUrl = _focusedArtworkUrl.asStateFlow()
    fun updateFocusedArtwork(url: String?) {
        if (!url.isNullOrBlank()) {
            _focusedArtworkUrl.value = url
        }
    }

    // Auth & Membership State (synchronously hydrated from fastPrefs for signed-in users to eliminate startup login flash)
    private val initialFastSession = supabaseAuth.getFastCachedSession()
    private val _authSession = MutableStateFlow<AuthSession?>(initialFastSession)
    val authSession = _authSession.asStateFlow()

    private val _isRestoringSession = MutableStateFlow(false)
    val isRestoringSession = _isRestoringSession.asStateFlow()

    private val _isLocalPreviewMode = MutableStateFlow(false)
    val isLocalPreviewMode = _isLocalPreviewMode.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(
        if (initialFastSession != null && initialFastSession.onboardingCompleted) {
            UserProfile(
                id = initialFastSession.userId,
                email = initialFastSession.email,
                username = initialFastSession.pendingUsername.takeIf { it.isNotBlank() && !it.contains("@") } ?: "Member",
                avatarKey = initialFastSession.pendingAvatarKey.ifBlank { BuiltInAvatar.AVATAR_1.key },
                memberStatus = MemberStatus.ACTIVE
            )
        } else null
    )
    val userProfile = _userProfile.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _pendingEmailVerification = MutableStateFlow<String?>(null)
    val pendingEmailVerification = _pendingEmailVerification.asStateFlow()

    private val _needsInviteRedemption = MutableStateFlow(false)
    val needsInviteRedemption = _needsInviteRedemption.asStateFlow()

    private val _isSupabaseReady = MutableStateFlow(AppConfig.isSupabaseConfigured)
    val isSupabaseReady = _isSupabaseReady.asStateFlow()

    private val _isPasswordRecoveryPending = MutableStateFlow(false)
    val isPasswordRecoveryPending = _isPasswordRecoveryPending.asStateFlow()

    private val _onboardingStage = MutableStateFlow(OnboardingStage.NONE)
    val onboardingStage = _onboardingStage.asStateFlow()

    private val _pendingOnboardingUsername = MutableStateFlow("")
    val pendingOnboardingUsername = _pendingOnboardingUsername.asStateFlow()

    private val _isCheckingUsername = MutableStateFlow(false)
    val isCheckingUsername = _isCheckingUsername.asStateFlow()

    private val _usernameAvailabilityError = MutableStateFlow<String?>(null)
    val usernameAvailabilityError = _usernameAvailabilityError.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError = _profileError.asStateFlow()

    private val _isProfileSaving = MutableStateFlow(false)
    val isProfileSaving = _isProfileSaving.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount = _isDeletingAccount.asStateFlow()

    private val _deleteAccountError = MutableStateFlow<String?>(null)
    val deleteAccountError = _deleteAccountError.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate = _availableUpdate.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate = _isCheckingUpdate.asStateFlow()

    private var pendingSignUpEmail: String? = null
    private var pendingSignUpPassword: String? = null
    private var verificationPollJob: Job? = null
    private var notificationPollJob: Job? = null

    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings = _privacySettings.asStateFlow()

    // Watchlist (Library) & Independent Home Schedule
    val watchlist: StateFlow<List<AnimeEntity>> = repository.watchlist
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val homeAnime: StateFlow<List<AnimeEntity>> = repository.homeTrackedAnime
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<AnimeEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    @OptIn(ExperimentalCoroutinesApi::class)
    val dayAnime: StateFlow<List<AnimeEntity>> = selectedDay
        .flatMapLatest { day -> repository.getPlannerAnimeByDay(day) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val homeDayAnime: StateFlow<List<AnimeEntity>> = selectedDay
        .flatMapLatest { day -> repository.getHomeAnimeByDay(day) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Home Highlights
    private val _trendingAnime = MutableStateFlow<List<AnimeEntity>>(emptyList())
    val trendingAnime = _trendingAnime.asStateFlow()

    private val _isHomeRefreshing = MutableStateFlow(false)
    val isHomeRefreshing = _isHomeRefreshing.asStateFlow()

    private val _homeError = MutableStateFlow<String?>(null)
    val homeError = _homeError.asStateFlow()

    // Discover & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterState = MutableStateFlow(DiscoverFilterState())
    val filterState = _filterState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AnimeEntity>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _isNextPageLoading = MutableStateFlow(false)
    val isNextPageLoading = _isNextPageLoading.asStateFlow()

    private val _hasNextDiscoverPage = MutableStateFlow(true)
    val hasNextDiscoverPageState = _hasNextDiscoverPage.asStateFlow()

    private val _discoverLastPageStartIndex = MutableStateFlow(0)
    val discoverLastPageStartIndex = _discoverLastPageStartIndex.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private var currentDiscoverPage = 1
    private var hasNextDiscoverPage = true
    private var searchJob: Job? = null

    // Anime Detail
    private val seasonOverridePrefs = application.getSharedPreferences(
        "anime_universe_season_overrides_v1",
        android.content.Context.MODE_PRIVATE
    )
    private val _franchiseSeasonOverrides = MutableStateFlow<Map<Int, com.example.model.FranchiseSeasonOverrideMode>>(
        loadSavedFranchiseOverrides()
    )
    val franchiseSeasonOverrides = _franchiseSeasonOverrides.asStateFlow()
    private var lastDetailRootMedia: com.example.data.remote.AniListMedia? = null

    private fun loadSavedFranchiseOverrides(): Map<Int, com.example.model.FranchiseSeasonOverrideMode> {
        val result = mutableMapOf<Int, com.example.model.FranchiseSeasonOverrideMode>()
        for ((k, v) in seasonOverridePrefs.all) {
            val fid = k.toIntOrNull() ?: continue
            val mode = com.example.model.FranchiseSeasonOverrideMode.fromStorage(v as? String)
            if (mode != com.example.model.FranchiseSeasonOverrideMode.AUTO) {
                result[fid] = mode
            }
        }
        return result
    }

    private val _selectedAnimeId = MutableStateFlow<Int?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedAnimeDetail: StateFlow<AnimeEntity?> = _selectedAnimeId
        .flatMapLatest { id -> if (id != null) repository.getAnimeById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _selectedRichDetail = MutableStateFlow<AnimeRichDetail?>(null)
    val selectedRichDetail = _selectedRichDetail.asStateFlow()

    private val _isDetailLoading = MutableStateFlow(false)
    val isDetailLoading = _isDetailLoading.asStateFlow()

    private val _animeReviews = MutableStateFlow<List<CommunityReview>>(emptyList())
    val animeReviews = _animeReviews.asStateFlow()

    // Collections
    private val _collections = MutableStateFlow<List<AnimeCollection>>(emptyList())
    val collections = _collections.asStateFlow()

    // Community
    private val _communityCapacity = MutableStateFlow(CommunityCapacity())
    val communityCapacity = _communityCapacity.asStateFlow()

    private val _communityMembers = MutableStateFlow<List<CommunityMember>>(emptyList())
    val communityMembers = _communityMembers.asStateFlow()

    private val _communityReviews = MutableStateFlow<List<CommunityReview>>(emptyList())
    val communityReviews = _communityReviews.asStateFlow()

    private val _communityActivity = MutableStateFlow<List<CommunityActivityItem>>(emptyList())
    val communityActivity = _communityActivity.asStateFlow()

    private val _isCommunityLoading = MutableStateFlow(false)
    val isCommunityLoading = _isCommunityLoading.asStateFlow()

    private val _communityError = MutableStateFlow<String?>(null)
    val communityError = _communityError.asStateFlow()

    private val _reviewSortOption = MutableStateFlow(ReviewSortOption.NEWEST)
    val reviewSortOption = _reviewSortOption.asStateFlow()

    // Selected Member Inspection
    private val _inspectedMemberWatchlist = MutableStateFlow<List<AnimeEntity>>(emptyList())
    val inspectedMemberWatchlist = _inspectedMemberWatchlist.asStateFlow()

    private val _inspectedMemberCollections = MutableStateFlow<List<AnimeCollection>>(emptyList())
    val inspectedMemberCollections = _inspectedMemberCollections.asStateFlow()

    private val _isMemberInspectLoading = MutableStateFlow(false)
    val isMemberInspectLoading = _isMemberInspectLoading.asStateFlow()

    // Admin Invitations & 500 MB Storage Monitoring
    private val _invitations = MutableStateFlow<List<InvitationCode>>(emptyList())
    val invitations = _invitations.asStateFlow()

    private val _storageStats = MutableStateFlow<DatabaseStorageStats?>(null)
    val storageStats = _storageStats.asStateFlow()

    private val _isStorageStatsLoading = MutableStateFlow(false)
    val isStorageStatsLoading = _isStorageStatsLoading.asStateFlow()

    private var hasTriggeredInitialAuthRestore = false
    private val notificationSyncReducer = NotificationSyncReducer()
    private val alertedSystemNotificationIds = mutableSetOf<String>()
    private var hasPrimedSystemNotifications = false
    private var notificationRealtimeSocket: WebSocket? = null
    private var notificationRealtimeUserId: String? = null
    private var notificationRealtimeToken: String? = null
    private var notificationReconnectJob: Job? = null

    init {
        restoreSessionAndInitialData()
        observeAuthSessionForNotifications()
        startAutoCloudSyncLoop()
        startRealtimeNotificationPolling()
    }

    private fun buildOptimisticProfile(session: AuthSession): UserProfile {
        return _userProfile.value ?: UserProfile(
            id = session.userId,
            email = session.email,
            username = session.pendingUsername.takeIf { it.isNotBlank() && !it.contains("@") } ?: "Member",
            avatarKey = session.pendingAvatarKey.ifBlank { BuiltInAvatar.AVATAR_1.key },
            memberStatus = MemberStatus.ACTIVE
        )
    }

    private suspend fun persistCurrentOwnerVaultSnapshot() {
        val key = activeOwnerKey ?: return
        val items = repository.getTrackedAnimeSnapshot()
        val deletedIds = repository.getDeletedAnimeIdsSnapshot()
        ownerVaultStore.saveOwnerVault(key, items, deletedIds)
    }

    private suspend fun switchToOwnerVault(targetOwnerKey: String) {
        val currentKey = activeOwnerKey ?: ownerVaultStore.getLastActiveOwnerKey()
        if (currentKey != null && currentKey != targetOwnerKey) {
            val prevItems = repository.getTrackedAnimeSnapshot()
            val prevDeleted = repository.getDeletedAnimeIdsSnapshot()
            ownerVaultStore.saveOwnerVault(currentKey, prevItems, prevDeleted)
            dirtyPendingSyncIds.clear()
        }
        val alreadyOwnsRoom = currentKey == targetOwnerKey && activeOwnerKey == targetOwnerKey
        if (!alreadyOwnsRoom) {
            val targetSnap = ownerVaultStore.loadOwnerVault(targetOwnerKey)
            if (currentKey != targetOwnerKey) {
                repository.restoreOwnerTrackedItems(targetSnap.items, targetSnap.deletedAnimeIds)
            } else {
                // Same owner across app restart: merge snapshot safely without losing offline edits
                repository.reconcileSavedAnime()
                if (targetSnap.items.isNotEmpty()) {
                    val currentIds = repository.getTrackedAnimeSnapshot().map { it.id }.toSet()
                    val missingFromRoom = targetSnap.items.filter { it.id !in currentIds }
                    if (missingFromRoom.isNotEmpty()) {
                        repository.syncCloudWatchlistEntries(missingFromRoom, replaceMissingFromCloud = false)
                    }
                }
            }
            activeOwnerKey = targetOwnerKey
            ownerVaultStore.setLastActiveOwnerKey(targetOwnerKey)
            syncDisplayedCatalogPersonalFlags()
        }
    }

    private fun syncDisplayedCatalogPersonalFlags() {
        val trackedById = watchlist.value.associateBy { it.id }
        fun clearFlags(entity: AnimeEntity): AnimeEntity {
            val mine = trackedById[entity.id]
            return if (mine != null) {
                entity.copy(
                    inHome = mine.inHome,
                    inWatchlist = mine.inWatchlist,
                    isWishlist = mine.isWishlist,
                    isFavorite = mine.isFavorite,
                    isPinned = mine.isPinned,
                    watchedEpisodeCount = mine.watchedEpisodeCount,
                    userSeason = mine.userSeason,
                    userStatus = mine.userStatus,
                    userScore = mine.userScore,
                    isWatched = mine.isWatched,
                    userNotes = mine.userNotes
                )
            } else {
                entity.copy(
                    inHome = false,
                    inWatchlist = false,
                    isWishlist = false,
                    isFavorite = false,
                    isPinned = false,
                    watchedEpisodeCount = 0,
                    userSeason = 1,
                    userStatus = null,
                    userScore = null,
                    isWatched = false,
                    userNotes = null
                )
            }
        }
        _searchResults.value = _searchResults.value.map(::clearFlags)
        _trendingAnime.value = _trendingAnime.value.map(::clearFlags)
    }

    private suspend fun migrateGuestVaultToAccountIfNeeded(session: AuthSession) {
        val guestId = ownerVaultStore.getOrCreateGuestId()
        val guestKey = OwnerVaultStore.guestKey(guestId)
        val guestSnapshot = ownerVaultStore.loadOwnerVault(guestKey)
        if (guestSnapshot.items.isEmpty()) {
            ownerVaultStore.setGuestModeActive(false)
            return
        }
        val currentAccountItems = repository.getTrackedAnimeSnapshot()
        val toMigrate = OwnerVaultStore.computeNonDuplicatingMigrationItems(currentAccountItems, guestSnapshot.items)
        if (toMigrate.isNotEmpty()) {
            toMigrate.forEach { dirtyPendingSyncIds.add(it.id) }
            repository.syncCloudWatchlistEntries(toMigrate, replaceMissingFromCloud = false)
            toMigrate.forEach { item ->
                supabaseDb.upsertUserAnimeEntry(session, item).onSuccess {
                    dirtyPendingSyncIds.remove(item.id)
                }
            }
            persistCurrentOwnerVaultSnapshot()
        }
        ownerVaultStore.deleteOwnerVault(guestKey)
        ownerVaultStore.setGuestModeActive(false)
        ownerVaultStore.rotateGuestId()
    }

    private fun restoreSessionAndInitialData() {
        viewModelScope.launch {
            // Read saved session from DataStore immediately before any network calls
            val saved = supabaseAuth.getSavedSession()
            if (saved != null) {
                _authSession.value = saved
                _isLocalPreviewMode.value = false
                ownerVaultStore.setGuestModeActive(false)
                switchToOwnerVault(OwnerVaultStore.userKey(saved.userId))
                if (saved.onboardingCompleted && _userProfile.value == null) {
                    _userProfile.value = buildOptimisticProfile(saved)
                }
            } else if (!_isLocalPreviewMode.value) {
                // On Login screen with no active account: unblock AuthScreen immediately, clear transient Room rows while keeping per-guest vault intact on disk
                _isRestoringSession.value = false
                repository.clearUserTrackingStateOnly()
                ownerVaultStore.setLastActiveOwnerKey(null)
                activeOwnerKey = null
            }
            _isRestoringSession.value = false

            preferencesRepo.preferencesFlow.collect { prefs ->
                AppConfig.updateRuntimeSupabaseConfig(prefs.supabaseUrl, prefs.supabaseAnonKey)
                val ready = AppConfig.isSupabaseConfigured
                _isSupabaseReady.value = ready
                if (ready && !hasTriggeredInitialAuthRestore) {
                    hasTriggeredInitialAuthRestore = true
                    val currentSaved = _authSession.value ?: supabaseAuth.getSavedSession()
                    if (currentSaved != null) {
                        if (currentSaved.onboardingCompleted && _userProfile.value == null) {
                            _userProfile.value = buildOptimisticProfile(currentSaved)
                        }
                        val refreshed = supabaseAuth.refreshSessionIfNeeded(currentSaved)
                        if (refreshed.isSuccess) {
                            val activeSession = refreshed.getOrThrow()
                            _authSession.value = activeSession
                            val loaded = supabaseDb.fetchCurrentProfile(activeSession).getOrNull()
                            if (loaded != null) {
                                supabaseAuth.cacheProfileSnapshot(loaded.username, loaded.avatarKey)
                            }
                            val needsOnboarding = !activeSession.onboardingCompleted ||
                                (loaded != null && loaded.username.startsWith("traveler_", ignoreCase = true))
                            if (needsOnboarding) {
                                _userProfile.value = loaded
                                _onboardingStage.value = OnboardingStage.USERNAME
                            } else {
                                _onboardingStage.value = OnboardingStage.NONE
                                if (loaded != null) {
                                    _userProfile.value = loaded
                                }
                                loadAuthenticatedWorkspace(activeSession)
                            }
                        } else {
                            supabaseAuth.clearSession()
                            repository.clearUserTrackingStateOnly()
                            ownerVaultStore.setLastActiveOwnerKey(null)
                            activeOwnerKey = null
                            _authSession.value = null
                            _userProfile.value = null
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.pruneAndCompactStorage()
            refreshHome()
            checkForUpdates(manual = false)
        }
    }

    private fun startAutoCloudSyncLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                triggerSilentCloudSync()
            }
        }
    }

    fun triggerSilentCloudSync() {
        val current = _authSession.value ?: return
        if (_onboardingStage.value != OnboardingStage.NONE || !current.onboardingCompleted) return
        val capturedUserId = current.userId
        viewModelScope.launch {
            val activeSession = supabaseAuth.refreshSessionIfNeeded(current).getOrDefault(current)
            if (_authSession.value?.userId != capturedUserId) return@launch
            if (activeSession.accessToken != current.accessToken) {
                _authSession.value = activeSession
            }
            if (dirtyPendingSyncIds.isNotEmpty()) {
                val pendingSnapshot = repository.getTrackedAnimeSnapshot().filter { it.id in dirtyPendingSyncIds }
                pendingSnapshot.forEach { item ->
                    supabaseDb.upsertUserAnimeEntry(activeSession, item).onSuccess {
                        dirtyPendingSyncIds.remove(item.id)
                    }
                }
            }
            supabaseDb.fetchUserWatchlist(activeSession).onSuccess { cloudItems ->
                if (_authSession.value?.userId != capturedUserId) return@onSuccess
                repository.syncCloudWatchlistEntries(
                    cloudItems = cloudItems,
                    replaceMissingFromCloud = true,
                    protectedPendingIds = dirtyPendingSyncIds.toSet()
                )
                persistCurrentOwnerVaultSnapshot()
                syncDisplayedCatalogPersonalFlags()
            }
            supabaseDb.fetchCollections(activeSession, creatorUserId = capturedUserId).onSuccess { cols ->
                if (_authSession.value?.userId == capturedUserId) {
                    _collections.value = cols
                }
            }
            if (_userProfile.value?.memberStatus == MemberStatus.ACTIVE) {
                val membersRes = supabaseDb.fetchCommunityMembers(activeSession)
                membersRes.onSuccess { members ->
                    if (_authSession.value?.userId != capturedUserId) return@onSuccess
                    _communityMembers.value = members
                    supabaseDb.fetchCommunityFeed(activeSession, members).onSuccess { (reviews, activity) ->
                        if (_authSession.value?.userId == capturedUserId) {
                            _communityReviews.value = sortReviews(reviews, _reviewSortOption.value)
                            _communityActivity.value = activity
                        }
                    }
                }
            }
            ensureNotificationRealtimeConnected(activeSession)
            reconcileUnreadNotificationCount(activeSession)
        }
    }

    fun onAppForegrounded() {
        val current = _authSession.value
        if (current != null && AppConfig.isSupabaseConfigured) {
            ensureNotificationRealtimeConnected(current)
            reconcileUnreadNotificationCount(current)
        }
        triggerSilentCloudSync()
    }

    private fun observeAuthSessionForNotifications() {
        viewModelScope.launch {
            _authSession.collect { session ->
                if (session == null || session.userId.isBlank() || session.accessToken.isBlank()) {
                    stopNotificationRealtime()
                    notificationSyncReducer.reset()
                    _unreadNotificationCount.value = 0
                    _unseenDiscussionAnime.value = emptyList()
                    _notifications.value = emptyList()
                } else if (AppConfig.isSupabaseConfigured) {
                    if (notificationRealtimeUserId != session.userId) {
                        notificationSyncReducer.reset()
                    }
                    ensureNotificationRealtimeConnected(session)
                    reconcileUnreadNotificationCount(session)
                }
            }
        }
    }

    private fun ensureNotificationRealtimeConnected(session: AuthSession) {
        if (!AppConfig.isSupabaseConfigured || session.userId.isBlank() || session.accessToken.isBlank()) {
            stopNotificationRealtime()
            return
        }
        if (notificationRealtimeSocket != null &&
            notificationRealtimeUserId == session.userId &&
            notificationRealtimeToken == session.accessToken
        ) {
            return
        }
        stopNotificationRealtime()
        val capturedUserId = session.userId
        val capturedToken = session.accessToken
        notificationRealtimeUserId = capturedUserId
        notificationRealtimeToken = capturedToken
        notificationRealtimeSocket = supabaseDiscussion.subscribeToUserNotificationsRealtime(
            session = session,
            onEvent = { ev ->
                viewModelScope.launch {
                    handleNotificationRealtimeEvent(ev, capturedUserId)
                }
            },
            onConnected = {
                viewModelScope.launch {
                    val active = _authSession.value
                    if (active != null && active.userId == capturedUserId) {
                        reconcileUnreadNotificationCount(active)
                    }
                }
            },
            onDisconnected = {
                viewModelScope.launch {
                    if (notificationRealtimeUserId == capturedUserId) {
                        notificationRealtimeSocket = null
                    }
                    notificationReconnectJob?.cancel()
                    notificationReconnectJob = viewModelScope.launch {
                        delay(5_000L)
                        val latest = _authSession.value
                        if (latest != null && latest.userId == capturedUserId && AppConfig.isSupabaseConfigured) {
                            val refreshed = supabaseAuth.refreshSessionIfNeeded(latest).getOrDefault(latest)
                            if (refreshed.accessToken != latest.accessToken && _authSession.value?.userId == capturedUserId) {
                                _authSession.value = refreshed
                            }
                            ensureNotificationRealtimeConnected(refreshed)
                        }
                    }
                }
            }
        )
    }

    private fun stopNotificationRealtime() {
        notificationReconnectJob?.cancel()
        notificationReconnectJob = null
        runCatching { notificationRealtimeSocket?.close(1000, "closed") }
        notificationRealtimeSocket = null
        notificationRealtimeUserId = null
        notificationRealtimeToken = null
    }

    private fun handleNotificationRealtimeEvent(
        ev: NotificationRealtimeEvent,
        expectedUserId: String
    ) {
        val currentSession = _authSession.value ?: return
        if (currentSession.userId != expectedUserId) return

        when (ev) {
            is NotificationRealtimeEvent.Insert -> {
                val outcome = notificationSyncReducer.onRealtimeInsert(
                    notificationId = ev.id,
                    recipientUserId = ev.recipientUserId,
                    currentUserId = currentSession.userId,
                    isRead = ev.isRead
                )
                if (outcome.delta != 0) {
                    _unreadNotificationCount.value = outcome.unreadCount
                    if (!ev.isRead && ev.id.isNotBlank() && alertedSystemNotificationIds.add(ev.id)) {
                        val preview = ev.previewText.trim().ifEmpty { "You have a new reply or mention in Anime Universe." }
                        ScheduleUpdateWorker.sendCommunityNotification(
                            context = getApplication(),
                            notificationId = ev.id,
                            title = "Anime Universe • Community Alert",
                            message = preview
                        )
                    }
                    reconcileUnseenAnimeMetadataOnly(currentSession, newlyInsertedNotificationId = ev.id)
                }
            }
            is NotificationRealtimeEvent.Update -> {
                val outcome = notificationSyncReducer.onRealtimeUpdate(
                    notificationId = ev.id,
                    recipientUserId = ev.recipientUserId,
                    currentUserId = currentSession.userId,
                    oldIsRead = ev.oldIsRead,
                    newIsRead = ev.newIsRead,
                    readAt = ev.readAt
                )
                _notifications.value = _notifications.value.map { item ->
                    if (item.id == ev.id) {
                        item.copy(
                            isRead = ev.newIsRead,
                            readAt = ev.readAt ?: item.readAt
                        )
                    } else item
                }
                _unreadNotificationCount.value = outcome.unreadCount
                if (outcome.unreadCount == 0) {
                    _unseenDiscussionAnime.value = emptyList()
                } else if (ev.newIsRead && ev.animeId > 0) {
                    val stillUnreadForAnime = _notifications.value.any { it.animeId == ev.animeId && !it.isRead }
                    if (!stillUnreadForAnime && _notifications.value.any { it.id == ev.id }) {
                        _unseenDiscussionAnime.value = _unseenDiscussionAnime.value.filterNot { it.animeId == ev.animeId }
                    }
                }
                if (outcome.needsReconcile) {
                    reconcileUnreadNotificationCount(currentSession)
                }
            }
        }
    }

    private fun reconcileUnseenAnimeMetadataOnly(
        session: AuthSession,
        newlyInsertedNotificationId: String? = null
    ) {
        val capturedUserId = session.userId
        val startGen = notificationSyncReducer.currentGeneration()
        viewModelScope.launch {
            supabaseDiscussion.fetchUnseenDiscussionAnime(session).onSuccess { summary ->
                if (_authSession.value?.userId != capturedUserId) return@onSuccess
                if (startGen < notificationSyncReducer.currentGeneration()) return@onSuccess
                _unseenDiscussionAnime.value = if (_unreadNotificationCount.value == 0) emptyList() else summary.anime
                val top = summary.anime.firstOrNull()
                if (!newlyInsertedNotificationId.isNullOrBlank() && top != null) {
                    val action = if (top.latestNotificationType == "thread_reply") "replied to you" else "tagged you"
                    val richTitle = "${top.latestSenderUsername} $action • ${top.animeTitle}"
                    val richBody = top.latestPreview.trim().ifEmpty { "Open Anime Universe to view the discussion." }
                    ScheduleUpdateWorker.sendCommunityNotification(
                        context = getApplication(),
                        notificationId = newlyInsertedNotificationId,
                        title = richTitle,
                        message = richBody
                    )
                }
            }
        }
    }

    fun reconcileUnreadNotificationCount(explicitSession: AuthSession? = null) {
        val session = explicitSession ?: _authSession.value ?: return
        if (!AppConfig.isSupabaseConfigured || session.userId.isBlank()) return
        val capturedUserId = session.userId
        val startGen = notificationSyncReducer.currentGeneration()
        viewModelScope.launch {
            val activeSession = supabaseAuth.refreshSessionIfNeeded(session).getOrDefault(session)
            if (_authSession.value?.userId != capturedUserId) return@launch
            if (activeSession.accessToken != session.accessToken) {
                _authSession.value = activeSession
            }
            val snapRes = supabaseDiscussion.fetchAuthoritativeUnreadSnapshot(activeSession)
            val summaryRes = supabaseDiscussion.fetchUnseenDiscussionAnime(activeSession)
            if (_authSession.value?.userId != capturedUserId) return@launch

            val snap = snapRes.getOrNull()
            if (snap != null) {
                val newlyDetectedIds = if (hasPrimedSystemNotifications) {
                    snap.unreadIds.filter { it !in alertedSystemNotificationIds }
                } else {
                    emptyList()
                }
                alertedSystemNotificationIds.addAll(snap.unreadIds)
                hasPrimedSystemNotifications = true

                val applied = notificationSyncReducer.applyAuthoritativeUnreadIds(
                    fetchedUnreadIds = snap.unreadIds,
                    fetchStartedGeneration = startGen
                )
                if (applied) {
                    val exactCount = notificationSyncReducer.unreadCount
                    _unreadNotificationCount.value = exactCount
                    val unreadSet = snap.unreadIds
                    if (_notifications.value.isNotEmpty()) {
                        _notifications.value = _notifications.value.map { item ->
                            val shouldBeUnread = item.id in unreadSet
                            if (item.isRead == shouldBeUnread) {
                                item.copy(isRead = !shouldBeUnread)
                            } else item
                        }
                    }
                    val summaryAnime = summaryRes.getOrNull()?.anime
                    _unseenDiscussionAnime.value = if (exactCount == 0) {
                        emptyList()
                    } else {
                        summaryAnime?.filter { (snap.unreadCountByAnime[it.animeId] ?: 0) > 0 }
                            ?: _unseenDiscussionAnime.value.filter { (snap.unreadCountByAnime[it.animeId] ?: 0) > 0 }
                    }
                    if (newlyDetectedIds.isNotEmpty()) {
                        val top = summaryAnime?.firstOrNull()
                        val action = if (top?.latestNotificationType == "thread_reply") "replied to you" else "tagged you"
                        val title = if (top != null && top.animeTitle.isNotBlank()) {
                            "${top.latestSenderUsername} $action • ${top.animeTitle}"
                        } else {
                            "Anime Universe • New Notification"
                        }
                        val body = top?.latestPreview?.trim()?.takeIf { it.isNotEmpty() }
                            ?: "Open Anime Universe to view your new notification."
                        newlyDetectedIds.take(5).forEach { notifId ->
                            ScheduleUpdateWorker.sendCommunityNotification(
                                context = getApplication(),
                                notificationId = notifId,
                                title = title,
                                message = body
                            )
                        }
                    }
                }
            } else {
                summaryRes.onSuccess { summary ->
                    if (notificationSyncReducer.confirmServerUnreadCount(summary.totalUnseen, startGen)) {
                        _unreadNotificationCount.value = notificationSyncReducer.unreadCount
                        _unseenDiscussionAnime.value = if (notificationSyncReducer.unreadCount == 0) emptyList() else summary.anime
                    }
                }
            }
        }
    }

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            if (manual) _isCheckingUpdate.value = true
            val res = supabaseDb.checkAppUpdate(
                currentVersionCode = AppConfig.APP_VERSION_CODE,
                currentVersionName = AppConfig.APP_VERSION
            )
            res.onSuccess { info ->
                if (info != null) {
                    _availableUpdate.value = info
                } else if (manual) {
                    _userMessage.value = "You're on the latest version (v${AppConfig.APP_VERSION})."
                }
            }.onFailure {
                if (manual) {
                    _userMessage.value = "You're on the latest version (v${AppConfig.APP_VERSION})."
                }
            }
            if (manual) _isCheckingUpdate.value = false
        }
    }

    fun dismissUpdatePrompt() {
        val current = _availableUpdate.value
        if (current != null && !current.isMandatory) {
            _availableUpdate.value = null
        }
    }

    fun saveSupabaseConfig(url: String, anonKey: String) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = anonKey.trim()
        if (!cleanUrl.startsWith("https://") || !cleanUrl.contains(".")) {
            _authError.value = "Invalid cloud configuration."
            return
        }
        if (cleanKey.length <= 20) {
            _authError.value = "Invalid cloud configuration."
            return
        }
        viewModelScope.launch {
            _authError.value = null
            preferencesRepo.setSupabaseConfig(cleanUrl, cleanKey)
            AppConfig.updateRuntimeSupabaseConfig(cleanUrl, cleanKey)
            _isSupabaseReady.value = AppConfig.isSupabaseConfigured
            _userMessage.value = "Cloud sync configuration updated."
        }
    }

    // ------------------------------------------------------------------------
    // AUTH & INVITE FLOWS
    // ------------------------------------------------------------------------

    fun enterLocalPreviewMode() {
        val guestId = ownerVaultStore.getOrCreateGuestId()
        ownerVaultStore.setGuestModeActive(true)
        _authSession.value = null
        _userProfile.value = null
        _privacySettings.value = PrivacySettings()
        _collections.value = emptyList()
        _communityMembers.value = emptyList()
        _communityReviews.value = emptyList()
        _communityActivity.value = emptyList()
        _animeReviews.value = emptyList()
        _selectedAnimeId.value = null
        _selectedRichDetail.value = null
        _isLocalPreviewMode.value = true
        viewModelScope.launch {
            switchToOwnerVault(OwnerVaultStore.guestKey(guestId))
            refreshHome()
            ensureDiscoverLoaded()
        }
    }

    fun exitLocalPreviewMode() {
        _isLocalPreviewMode.value = false
        ownerVaultStore.setGuestModeActive(false)
        viewModelScope.launch {
            persistCurrentOwnerVaultSnapshot()
        }
    }

    fun clearAuthMessages() {
        _authError.value = null
        _pendingEmailVerification.value = null
        _usernameAvailabilityError.value = null
        verificationPollJob?.cancel()
        verificationPollJob = null
    }

    fun clearUsernameError() {
        _usernameAvailabilityError.value = null
    }

    fun clearProfileError() {
        _profileError.value = null
    }

    fun clearDeleteAccountError() {
        _deleteAccountError.value = null
    }

    fun signUpEmailOnly(email: String, password: String) {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            _authError.value = "Enter a valid email address."
            return
        }
        if (password.length < 6) {
            _authError.value = "Password must be at least 6 characters."
            return
        }
        pendingSignUpEmail = cleanEmail
        pendingSignUpPassword = password

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            _pendingEmailVerification.value = null

            val localWaitSec = supabaseAuth.checkLocalEmailCooldownSeconds(cleanEmail)
            if (localWaitSec > 0L) {
                val waitText = SupabaseAuthService.formatRemainingMinutes(localWaitSec)
                _authError.value = "This email was recently deleted. You can register with it again in $waitText."
                _isAuthLoading.value = false
                return@launch
            }

            val remoteCooldown = supabaseDb.checkEmailRegistrationAllowed(cleanEmail).getOrNull()
            if (remoteCooldown != null && !remoteCooldown.allowed) {
                val waitText = SupabaseAuthService.formatRemainingMinutes(remoteCooldown.retryAfterSeconds.coerceAtLeast(60L))
                _authError.value = "This email was recently deleted. You can register with it again in $waitText."
                _isAuthLoading.value = false
                return@launch
            }

            val result = supabaseAuth.signUpEmailOnly(
                email = cleanEmail,
                password = password
            )
            result.onSuccess { outcome ->
                when (outcome) {
                    is SignUpOutcome.Authenticated -> {
                        _authSession.value = outcome.session
                        _isLocalPreviewMode.value = false
                        _onboardingStage.value = OnboardingStage.USERNAME
                        _needsInviteRedemption.value = false
                    }
                    is SignUpOutcome.EmailVerificationRequired -> {
                        _pendingEmailVerification.value = outcome.message
                        startEmailVerificationPolling(cleanEmail, password)
                    }
                }
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Unable to create your account right now.")
            }
            _isAuthLoading.value = false
        }
    }

    private fun startEmailVerificationPolling(email: String, password: String) {
        verificationPollJob?.cancel()
        verificationPollJob = viewModelScope.launch {
            // Automatically detect when the user clicks the confirmation link in Gmail/Chrome
            repeat(60) {
                delay(3500L)
                if (!isActive || _authSession.value != null || _pendingEmailVerification.value.isNullOrBlank()) {
                    return@launch
                }
                val res = supabaseAuth.signInWithEmail(email, password)
                if (res.isSuccess) {
                    val session = res.getOrThrow()
                    _authSession.value = session
                    _pendingEmailVerification.value = null
                    _isLocalPreviewMode.value = false
                    _needsInviteRedemption.value = false
                    val existingProfile = supabaseDb.fetchCurrentProfile(session).getOrNull()
                    val needsOnboarding = !session.onboardingCompleted ||
                        existingProfile == null ||
                        existingProfile.username.startsWith("traveler_", ignoreCase = true)
                    if (needsOnboarding) {
                        _userProfile.value = existingProfile
                        _onboardingStage.value = OnboardingStage.USERNAME
                        _userMessage.value = "Email verified! Choose your unique username."
                    } else {
                        _onboardingStage.value = OnboardingStage.NONE
                        _userProfile.value = existingProfile
                        loadAuthenticatedWorkspace(session)
                    }
                    return@launch
                }
            }
        }
    }

    fun handlePastedAuthLink(rawLink: String) {
        val clean = rawLink.trim()
        if (clean.isBlank()) {
            _authError.value = "Paste the link from your email or browser address bar."
            return
        }
        runCatching {
            val uri = Uri.parse(clean)
            handleDeepLinkUri(uri, allowAnyScheme = true)
        }.onFailure {
            _authError.value = "Invalid link format."
        }
    }

    fun confirmEmailAndContinue(email: String, password: String) {
        val targetEmail = email.trim().ifBlank { pendingSignUpEmail.orEmpty() }
        val targetPassword = password.ifBlank { pendingSignUpPassword.orEmpty() }
        if (targetEmail.isBlank() || targetPassword.isBlank()) {
            _authError.value = "Sign in with your email and password to continue setup."
            return
        }
        signInWithIdentifier(targetEmail, targetPassword)
    }

    /**
     * Silently checks in the background while on the "Check Your Inbox" screen whether
     * the user clicked the verification link in Gmail (even if Chrome opened a blank page).
     * As soon as Supabase marks the email confirmed, this automatically signs the user in
     * and opens the Username + Royal PFP onboarding screen.
     */
    suspend fun checkEmailVerifiedSilently(email: String, password: String): Boolean {
        val targetEmail = email.trim().ifBlank { pendingSignUpEmail.orEmpty() }
        val targetPassword = password.ifBlank { pendingSignUpPassword.orEmpty() }
        if (targetEmail.isBlank() || targetPassword.isBlank() || _authSession.value != null) {
            return false
        }
        val res = supabaseAuth.signInWithEmail(targetEmail, targetPassword)
        if (res.isSuccess) {
            val session = res.getOrThrow()
            _authSession.value = session
            _pendingEmailVerification.value = null
            _authError.value = null
            _isLocalPreviewMode.value = false
            _needsInviteRedemption.value = false
            val loadedProfile = supabaseDb.fetchCurrentProfile(session).getOrNull()
            val needsOnboarding = !session.onboardingCompleted ||
                loadedProfile == null ||
                loadedProfile.username.startsWith("traveler_", ignoreCase = true)
            if (needsOnboarding) {
                _userProfile.value = loadedProfile
                _onboardingStage.value = OnboardingStage.USERNAME
                _userMessage.value = "Email verified! Claim your unique username."
            } else {
                _onboardingStage.value = OnboardingStage.NONE
                _userProfile.value = loadedProfile
                pendingSignUpEmail = null
                pendingSignUpPassword = null
                loadAuthenticatedWorkspace(session)
            }
            return true
        }
        return false
    }

    fun processPastedAuthLink(rawLink: String) {
        val trimmed = rawLink.trim()
        if (trimmed.isBlank()) {
            _authError.value = "Paste the link from your email or browser address bar."
            return
        }
        runCatching { Uri.parse(trimmed) }.getOrNull()?.let { uri ->
            handleDeepLinkUri(uri, allowAnyScheme = true)
        } ?: run {
            _authError.value = "Invalid link format."
        }
    }

    fun signInWithIdentifier(identifier: String, password: String) {
        val cleanIdentifier = identifier.trim()
        if (cleanIdentifier.isBlank() || password.isBlank()) {
            _authError.value = "Enter your email or username and password."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            _pendingEmailVerification.value = null

            val resolvedEmailRes = if (cleanIdentifier.contains("@") && !cleanIdentifier.startsWith("@")) {
                Result.success(cleanIdentifier)
            } else {
                supabaseDb.resolveLoginEmail(cleanIdentifier)
            }

            if (resolvedEmailRes.isFailure) {
                _authError.value = ErrorSanitizer.sanitize(
                    resolvedEmailRes.exceptionOrNull(),
                    "No account found with that username."
                )
                _isAuthLoading.value = false
                return@launch
            }

            val resolvedEmail = resolvedEmailRes.getOrThrow()
            val res = supabaseAuth.signInWithEmail(resolvedEmail, password)
            res.onSuccess { session ->
                _authSession.value = session
                _isLocalPreviewMode.value = false
                _needsInviteRedemption.value = false
                val existingProfile = supabaseDb.fetchCurrentProfile(session).getOrNull()
                val needsOnboarding = !session.onboardingCompleted ||
                    existingProfile == null ||
                    existingProfile.username.startsWith("traveler_", ignoreCase = true)

                if (needsOnboarding) {
                    _userProfile.value = existingProfile
                    _onboardingStage.value = OnboardingStage.USERNAME
                } else {
                    _onboardingStage.value = OnboardingStage.NONE
                    _userProfile.value = existingProfile
                    supabaseAuth.cacheProfileSnapshot(existingProfile.username, existingProfile.avatarKey)
                    pendingSignUpEmail = null
                    pendingSignUpPassword = null
                    loadAuthenticatedWorkspace(session)
                }
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Incorrect email/username or password.")
            }
            _isAuthLoading.value = false
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        pendingInviteCode: String = "",
        pendingAvatarKey: String = BuiltInAvatar.AVATAR_1.key
    ) = signInWithIdentifier(email, password)

    fun verifyAndAdvanceUsername(username: String) {
        val clean = username.trim()
        when {
            clean.length !in 2..32 -> {
                _usernameAvailabilityError.value = "Username must be between 2 and 32 characters."
                return
            }
            clean.contains("@") -> {
                _usernameAvailabilityError.value = "Usernames cannot contain the @ symbol."
                return
            }
            !clean.matches(Regex("^[A-Za-z0-9_ .-]+$")) -> {
                _usernameAvailabilityError.value = "Use letters, numbers, spaces, dots, hyphens, or underscores."
                return
            }
        }
        val session = _authSession.value
        viewModelScope.launch {
            _isCheckingUsername.value = true
            _usernameAvailabilityError.value = null
            _authError.value = null
            val localUserCooldown = supabaseAuth.checkLocalUsernameCooldownSeconds(clean)
            if (localUserCooldown > 0L) {
                _usernameAvailabilityError.value = "That username was recently deleted and will be available again after 24 hours."
                _isCheckingUsername.value = false
                return@launch
            }
            supabaseDb.checkUsernameStatus(
                username = clean,
                accessToken = session?.accessToken,
                currentUserId = session?.userId
            ).onSuccess { status ->
                if (status.available) {
                    _pendingOnboardingUsername.value = clean
                    _onboardingStage.value = OnboardingStage.AVATAR
                } else {
                    _usernameAvailabilityError.value = status.reason
                        ?: "That username is already taken. Please choose a different unique username."
                }
            }.onFailure { err ->
                _usernameAvailabilityError.value = ErrorSanitizer.sanitize(err, "Unable to check username right now.")
            }
            _isCheckingUsername.value = false
        }
    }

    fun backToUsernameStep() {
        _authError.value = null
        _onboardingStage.value = OnboardingStage.USERNAME
    }

    fun completeRoyalAvatarOnboarding(avatarKey: String) {
        val session = _authSession.value ?: return
        val chosenUsername = _pendingOnboardingUsername.value.trim()
        if (chosenUsername.length < 2) {
            _onboardingStage.value = OnboardingStage.USERNAME
            _usernameAvailabilityError.value = "Choose your unique username first."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            _usernameAvailabilityError.value = null

            val saveRes = supabaseDb.completeOnboardingProfile(
                session = session,
                username = chosenUsername,
                avatarKey = avatarKey
            )
            saveRes.onSuccess { profile ->
                val metaRes = supabaseAuth.updateAuthUserMetadata(
                    session = session,
                    username = chosenUsername,
                    avatarKey = avatarKey,
                    onboardingCompleted = true
                )
                val updatedSession = metaRes.getOrElse {
                    session.copy(
                        pendingUsername = chosenUsername,
                        pendingAvatarKey = avatarKey,
                        onboardingCompleted = true
                    )
                }
                _authSession.value = updatedSession
                _userProfile.value = profile
                _onboardingStage.value = OnboardingStage.NONE
                _needsInviteRedemption.value = false
                pendingSignUpEmail = null
                pendingSignUpPassword = null
                loadAuthenticatedWorkspace(updatedSession)
                _userMessage.value = "Welcome to ANIME UNIVERSE, $chosenUsername!"
            }.onFailure { err ->
                val cleanMsg = ErrorSanitizer.sanitize(err, "Unable to complete profile setup right now.")
                if (cleanMsg.contains("taken", ignoreCase = true) || cleanMsg.contains("username", ignoreCase = true)) {
                    _usernameAvailabilityError.value = cleanMsg
                    _onboardingStage.value = OnboardingStage.USERNAME
                } else {
                    _authError.value = cleanMsg
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun completeProfileSetup(
        username: String,
        avatarKey: String = BuiltInAvatar.AVATAR_1.key
    ) {
        _pendingOnboardingUsername.value = username.trim()
        completeRoyalAvatarOnboarding(avatarKey)
    }

    fun verifySignUpOtp(email: String, otpCode: String) {
        val targetEmail = email.trim().ifBlank { pendingSignUpEmail.orEmpty() }
        val cleanCode = otpCode.trim().replace(" ", "")
        if (targetEmail.isBlank()) {
            _authError.value = "Enter your email address first."
            return
        }
        if (cleanCode.length !in 6..8) {
            _authError.value = "Enter the verification code sent to your email."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            supabaseAuth.verifyEmailOtp(
                email = targetEmail,
                otpCode = cleanCode,
                type = "signup"
            ).onSuccess { session ->
                verificationPollJob?.cancel()
                _authSession.value = session
                _pendingEmailVerification.value = null
                _isLocalPreviewMode.value = false
                _needsInviteRedemption.value = false
                val existingProfile = supabaseDb.fetchCurrentProfile(session).getOrNull()
                val emailPrefix = targetEmail.substringBefore("@").trim()
                val looksLikeEmailOrPlaceholder = existingProfile == null ||
                    existingProfile.username.isBlank() ||
                    existingProfile.username.startsWith("traveler_", ignoreCase = true) ||
                    existingProfile.username.contains("@") ||
                    (emailPrefix.isNotBlank() && existingProfile.username.equals(emailPrefix, ignoreCase = true))
                val needsOnboarding = !session.onboardingCompleted || looksLikeEmailOrPlaceholder
                if (needsOnboarding) {
                    _pendingOnboardingUsername.value = ""
                    _userProfile.value = existingProfile
                    _onboardingStage.value = OnboardingStage.USERNAME
                    _userMessage.value = "Email verified! Choose your username and profile picture."
                } else {
                    _onboardingStage.value = OnboardingStage.NONE
                    _userProfile.value = existingProfile
                    loadAuthenticatedWorkspace(session)
                }
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Invalid or expired verification code.")
            }
            _isAuthLoading.value = false
        }
    }

    fun sendPasswordReset(identifier: String, onSent: () -> Unit = {}) {
        val clean = identifier.trim()
        if (clean.isBlank()) {
            _authError.value = "Enter your email address or username to reset your password."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val emailRes = if (clean.contains("@") && !clean.startsWith("@")) {
                Result.success(clean)
            } else {
                supabaseDb.resolveLoginEmail(clean)
            }
            emailRes.onSuccess { targetEmail ->
                supabaseAuth.sendPasswordReset(targetEmail).onSuccess { msg ->
                    _pendingEmailVerification.value = msg
                    onSent()
                }.onFailure { err ->
                    _authError.value = ErrorSanitizer.sanitize(err, "Unable to send password reset code right now.")
                }
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "No account found matching that username.")
            }
            _isAuthLoading.value = false
        }
    }

    fun verifyRecoveryCodeAndSetPassword(
        identifier: String,
        otpOrLink: String,
        newPassword: String,
        onCompleted: () -> Unit = {}
    ) {
        val cleanId = identifier.trim()
        val cleanCode = otpOrLink.trim().replace(" ", "")
        if (cleanId.isBlank()) {
            _authError.value = "Enter your email address or username."
            return
        }
        if (cleanCode.length != 8) {
            _authError.value = "Enter the 8-digit reset code from your email."
            return
        }
        if (newPassword.length < 6) {
            _authError.value = "New password must be at least 6 characters."
            return
        }
        if (cleanCode.startsWith("http://", ignoreCase = true) ||
            cleanCode.startsWith("https://", ignoreCase = true) ||
            cleanCode.startsWith("animeuniverse://", ignoreCase = true)
        ) {
            handlePastedAuthLink(cleanCode)
            onCompleted()
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val emailRes = if (cleanId.contains("@") && !cleanId.startsWith("@")) {
                Result.success(cleanId)
            } else {
                supabaseDb.resolveLoginEmail(cleanId)
            }
            if (emailRes.isFailure) {
                _authError.value = ErrorSanitizer.sanitize(emailRes.exceptionOrNull(), "No account found matching that username.")
                _isAuthLoading.value = false
                return@launch
            }
            val targetEmail = emailRes.getOrThrow()
            val verifyRes = supabaseAuth.verifyEmailOtp(
                email = targetEmail,
                otpCode = cleanCode,
                type = "recovery"
            )
            if (verifyRes.isFailure) {
                _authError.value = ErrorSanitizer.sanitize(verifyRes.exceptionOrNull(), "Invalid or expired reset code.")
                _isAuthLoading.value = false
                return@launch
            }
            val session = verifyRes.getOrThrow()
            supabaseAuth.updatePassword(session, newPassword).onSuccess {
                _authSession.value = session
                _isPasswordRecoveryPending.value = false
                _pendingEmailVerification.value = null
                loadAuthenticatedWorkspace(session)
                _userMessage.value = "Password updated successfully!"
                onCompleted()
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Unable to update password right now.")
            }
            _isAuthLoading.value = false
        }
    }

    fun resendVerificationEmail(email: String) {
        val target = email.trim().ifBlank { pendingSignUpEmail.orEmpty() }
        if (target.isBlank()) return
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            supabaseAuth.resendVerificationEmail(target).onSuccess { msg ->
                _pendingEmailVerification.value = msg
                _userMessage.value = msg
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Unable to resend verification link right now.")
            }
            _isAuthLoading.value = false
        }
    }

    fun handleDeepLinkUri(uri: Uri?, allowAnyScheme: Boolean = false) {
        if (uri == null) return
        val isCustomScheme = uri.scheme == "animeuniverse" && uri.host == "auth-callback"
        val hasAuthTokens = uri.fragment?.contains("access_token") == true ||
            uri.getQueryParameter("access_token") != null ||
            uri.getQueryParameter("token_hash") != null ||
            uri.getQueryParameter("token") != null
        if (!isCustomScheme && !(allowAnyScheme && hasAuthTokens)) return
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            supabaseAuth.handleAuthCallbackUri(uri).onSuccess { outcome ->
                when (outcome) {
                    is AuthCallbackOutcome.VerifiedSession -> {
                        val session = outcome.session
                        _authSession.value = session
                        _pendingEmailVerification.value = null
                        _isLocalPreviewMode.value = false
                        _needsInviteRedemption.value = false
                        val existingProfile = supabaseDb.fetchCurrentProfile(session).getOrNull()
                        val needsOnboarding = !session.onboardingCompleted ||
                            existingProfile == null ||
                            existingProfile.username.startsWith("traveler_", ignoreCase = true)
                        if (needsOnboarding) {
                            _userProfile.value = existingProfile
                            _onboardingStage.value = OnboardingStage.USERNAME
                            _userMessage.value = "Email verified! Claim your unique username."
                        } else {
                            _onboardingStage.value = OnboardingStage.NONE
                            _userProfile.value = existingProfile
                            pendingSignUpEmail = null
                            pendingSignUpPassword = null
                            loadAuthenticatedWorkspace(session)
                            _userMessage.value = "Email verified! Welcome to ANIME UNIVERSE."
                        }
                    }
                    is AuthCallbackOutcome.PasswordRecoverySession -> {
                        _authSession.value = outcome.session
                        _isPasswordRecoveryPending.value = true
                    }
                }
            }.onFailure { err ->
                val clean = ErrorSanitizer.sanitize(err, "Email confirmed! Sign in below to continue.")
                if (clean.contains("confirmed", ignoreCase = true)) {
                    _pendingEmailVerification.value = clean
                } else {
                    _authError.value = clean
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun submitNewPassword(newPassword: String) {
        val session = _authSession.value ?: return
        if (newPassword.length < 6) {
            _authError.value = "Password must be at least 6 characters."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            supabaseAuth.updatePassword(session, newPassword).onSuccess {
                _isPasswordRecoveryPending.value = false
                loadAuthenticatedWorkspace(session)
                _userMessage.value = "Password updated successfully!"
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Unable to update password right now.")
            }
            _isAuthLoading.value = false
        }
    }

    fun dismissPasswordRecovery() {
        _isPasswordRecoveryPending.value = false
    }

    fun signOut() {
        val current = _authSession.value
        viewModelScope.launch {
            persistCurrentOwnerVaultSnapshot()
            supabaseAuth.signOut(current)
            repository.clearUserTrackingStateOnly()
            dirtyPendingSyncIds.clear()
            activeOwnerKey = null
            ownerVaultStore.setLastActiveOwnerKey(null)
            ownerVaultStore.setGuestModeActive(false)
            ownerVaultStore.rotateGuestId()
            _authSession.value = null
            _userProfile.value = null
            _privacySettings.value = PrivacySettings()
            _collections.value = emptyList()
            _communityMembers.value = emptyList()
            _communityReviews.value = emptyList()
            _communityActivity.value = emptyList()
            _animeReviews.value = emptyList()
            _inspectedMemberWatchlist.value = emptyList()
            _inspectedMemberCollections.value = emptyList()
            _invitations.value = emptyList()
            _storageStats.value = null
            _selectedAnimeId.value = null
            _selectedRichDetail.value = null
            _dynamicIslandFeedback.value = null
            _needsInviteRedemption.value = false
            _onboardingStage.value = OnboardingStage.NONE
            _pendingOnboardingUsername.value = ""
            _usernameAvailabilityError.value = null
            _profileError.value = null
            _deleteAccountError.value = null
            pendingSignUpEmail = null
            pendingSignUpPassword = null
            _isLocalPreviewMode.value = false
            syncDisplayedCatalogPersonalFlags()
            _userMessage.value = "Signed out."
        }
    }

    fun deleteAccount(
        password: String,
        onSuccess: () -> Unit = {}
    ) {
        val session = _authSession.value
        val currentProfile = _userProfile.value
        if (session == null || currentProfile == null) {
            _deleteAccountError.value = "No active account session found."
            return
        }
        if (password.isBlank()) {
            _deleteAccountError.value = "Enter your password to confirm deletion."
            return
        }
        viewModelScope.launch {
            _isDeletingAccount.value = true
            _deleteAccountError.value = null

            val verifyRes = supabaseAuth.verifyPassword(session, password)
            if (verifyRes.isFailure) {
                _deleteAccountError.value = ErrorSanitizer.sanitize(
                    verifyRes.exceptionOrNull(),
                    "Incorrect password. Please try again."
                )
                _isDeletingAccount.value = false
                return@launch
            }

            val verifiedSession = verifyRes.getOrThrow()
            val deleteRes = supabaseDb.deleteAccountCompletely(verifiedSession)
            if (deleteRes.isFailure) {
                _deleteAccountError.value = ErrorSanitizer.sanitize(
                    deleteRes.exceptionOrNull(),
                    "Unable to delete account right now."
                )
                _isDeletingAccount.value = false
                return@launch
            }

            supabaseAuth.recordDeletionCooldowns(
                email = verifiedSession.email,
                username = currentProfile.username
            )
            supabaseAuth.clearSession()
            repository.clearAllUserData()
            ownerVaultStore.deleteOwnerVault(OwnerVaultStore.userKey(verifiedSession.userId))
            dirtyPendingSyncIds.clear()
            activeOwnerKey = null
            ownerVaultStore.setLastActiveOwnerKey(null)
            ownerVaultStore.setGuestModeActive(false)
            ownerVaultStore.rotateGuestId()

            _authSession.value = null
            _userProfile.value = null
            _privacySettings.value = PrivacySettings()
            _collections.value = emptyList()
            _communityMembers.value = emptyList()
            _communityReviews.value = emptyList()
            _communityActivity.value = emptyList()
            _animeReviews.value = emptyList()
            _invitations.value = emptyList()
            _inspectedMemberWatchlist.value = emptyList()
            _inspectedMemberCollections.value = emptyList()
            _storageStats.value = null
            _selectedAnimeId.value = null
            _selectedRichDetail.value = null
            _dynamicIslandFeedback.value = null
            _needsInviteRedemption.value = false
            _onboardingStage.value = OnboardingStage.NONE
            _pendingOnboardingUsername.value = ""
            _usernameAvailabilityError.value = null
            _profileError.value = null
            _deleteAccountError.value = null
            pendingSignUpEmail = null
            pendingSignUpPassword = null
            _isLocalPreviewMode.value = false
            syncDisplayedCatalogPersonalFlags()
            _isDeletingAccount.value = false
            _userMessage.value = "Account deleted. Your email can re-register after 5 minutes and your username after 24 hours."
            onSuccess()
        }
    }

    fun joinCommunity(inviteCode: String = "") {
        val session = _authSession.value
        val profile = _userProfile.value
        if (session == null || profile == null) {
            _userMessage.value = "Sign in first to join the community."
            return
        }
        viewModelScope.launch {
            _isCommunityLoading.value = true
            _communityError.value = null
            supabaseDb.joinCommunity(
                session = session,
                username = profile.username,
                avatarKey = profile.avatarKey,
                inviteCode = inviteCode
            ).onSuccess {
                _privacySettings.value = _privacySettings.value.copy(
                    reviewsVisibility = VisibilityLevel.COMMUNITY,
                    activityVisibility = VisibilityLevel.COMMUNITY
                )
                loadAuthenticatedWorkspace(session)
                _userMessage.value = "Joined community! Your notes and ratings are now shared automatically."
            }.onFailure { err ->
                val msg = ErrorSanitizer.sanitize(err, "Unable to join community.")
                _communityError.value = msg
                _userMessage.value = msg
            }
            _isCommunityLoading.value = false
        }
    }

    fun leaveCommunity() {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.leaveCommunity(session).onSuccess {
                _userProfile.value = _userProfile.value?.copy(memberStatus = MemberStatus.LEFT)
                _communityMembers.value = emptyList()
                _communityReviews.value = emptyList()
                _communityActivity.value = emptyList()
                _userMessage.value = "You have left the community."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Unable to leave community.")
            }
        }
    }

    private fun loadAuthenticatedWorkspace(session: AuthSession) {
        val capturedUserId = session.userId
        viewModelScope.launch {
            switchToOwnerVault(OwnerVaultStore.userKey(capturedUserId))
            val loaded = supabaseDb.fetchCurrentProfile(session).getOrNull()
                ?: supabaseDb.ensurePersonalProfile(session).getOrNull()
            if (_authSession.value?.userId != capturedUserId) return@launch
            if (loaded != null) {
                _userProfile.value = loaded
                supabaseAuth.cacheProfileSnapshot(loaded.username, loaded.avatarKey)
                _needsInviteRedemption.value = false
                supabaseDb.fetchPrivacySettings(session).onSuccess {
                    if (_authSession.value?.userId == capturedUserId) _privacySettings.value = it
                }
                val cloudWatchlistRes = supabaseDb.fetchUserWatchlist(session)
                cloudWatchlistRes.onSuccess { cloudItems ->
                    if (_authSession.value?.userId != capturedUserId) return@onSuccess
                    repository.syncCloudWatchlistEntries(
                        cloudItems = cloudItems,
                        replaceMissingFromCloud = true,
                        protectedPendingIds = dirtyPendingSyncIds.toSet()
                    )
                    migrateGuestVaultToAccountIfNeeded(session)
                    persistCurrentOwnerVaultSnapshot()
                    syncDisplayedCatalogPersonalFlags()
                }.onFailure {
                    migrateGuestVaultToAccountIfNeeded(session)
                    persistCurrentOwnerVaultSnapshot()
                }
                supabaseDb.fetchCollections(session, creatorUserId = capturedUserId).onSuccess {
                    if (_authSession.value?.userId == capturedUserId) _collections.value = it
                }
                if (loaded.memberStatus == MemberStatus.ACTIVE) {
                    refreshCommunityData()
                    if (loaded.isInviteCreator) {
                        fetchInvitations()
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // HOME & ANILIST METADATA DISCOVERY
    // ------------------------------------------------------------------------

    fun setSelectedDay(day: Int) {
        selectedDay.value = day.coerceIn(1, 7)
    }

    fun refreshHome() {
        viewModelScope.launch {
            _isHomeRefreshing.value = true
            _homeError.value = null
            val trendingRes = metadataProvider.getTrendingAnime(page = 1, perPage = 16)
            trendingRes.onSuccess { mediaList ->
                val entities = mediaList.map { AniListMetadataProvider.mapMediaToEntity(it) }
                repository.upsertCatalogItems(entities)
                _trendingAnime.value = entities
            }.onFailure { err ->
                _homeError.value = ErrorSanitizer.sanitize(err, "Unable to refresh schedule right now.")
            }
            repository.refreshSchedule()
            if (_authSession.value != null && _userProfile.value?.memberStatus == MemberStatus.ACTIVE) {
                refreshCommunityData()
            }
            _isHomeRefreshing.value = false
        }
    }

    fun ensureDiscoverLoaded() {
        if (_searchResults.value.isEmpty() && !_isSearching.value) {
            performSearch(resetPage = true)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        performSearch(resetPage = true, debounceMs = if (query.isBlank()) 0L else 380L)
    }

    fun updateFilterState(newState: DiscoverFilterState) {
        _filterState.value = newState
        performSearch(resetPage = true)
    }

    fun resetFilters() {
        _filterState.value = DiscoverFilterState()
        _searchQuery.value = ""
        performSearch(resetPage = true)
    }

    fun performSearch(resetPage: Boolean = true, debounceMs: Long = 0L) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMs > 0L) {
                delay(debounceMs)
            }
            if (resetPage) {
                currentDiscoverPage = 1
                hasNextDiscoverPage = true
                _hasNextDiscoverPage.value = true
                _discoverLastPageStartIndex.value = 0
                _isSearching.value = true
            } else {
                if (!hasNextDiscoverPage || _isNextPageLoading.value) return@launch
                _isNextPageLoading.value = true
            }
            _searchError.value = null

            val filters = _filterState.value
            val country = when {
                filters.mediaType == "DONGHUA" -> "CN"
                filters.country != "ALL" -> filters.country
                else -> null
            }
            val res = metadataProvider.searchCatalog(
                query = _searchQuery.value,
                genre = filters.selectedGenres.firstOrNull(),
                season = filters.season,
                seasonYear = filters.yearFrom,
                status = filters.status,
                format = filters.format,
                countryOfOrigin = country,
                sort = filters.sortBy,
                page = currentDiscoverPage,
                perPage = 24
            )
            res.onSuccess { (mediaList, hasNext) ->
                val entities = mediaList.map { media ->
                    val existing = repository.getAnimeByIdSync(media.id)
                    AniListMetadataProvider.mapMediaToEntity(media, existing)
                }.filter { entity ->
                    val safeOk = !preferences.value.safeModeEnabled || !entity.isAdult
                    val multiGenreOk = filters.selectedGenres.isEmpty() || filters.selectedGenres.all { g ->
                        entity.genres.contains(g, ignoreCase = true)
                    }
                    safeOk && multiGenreOk
                }
                repository.upsertCatalogItems(entities)
                hasNextDiscoverPage = hasNext
                _hasNextDiscoverPage.value = hasNext
                if (resetPage) {
                    _discoverLastPageStartIndex.value = 0
                    _searchResults.value = entities
                } else {
                    val prevCount = _searchResults.value.size
                    val merged = (_searchResults.value + entities).distinctBy { it.id }
                    if (merged.size > prevCount) {
                        _discoverLastPageStartIndex.value = prevCount
                    }
                    _searchResults.value = merged
                }
                if (hasNext) currentDiscoverPage += 1
            }.onFailure { err ->
                _searchError.value = ErrorSanitizer.sanitize(err, "Unable to load catalog right now.")
            }

            _isSearching.value = false
            _isNextPageLoading.value = false
        }
    }

    fun loadNextDiscoverPage() {
        if (hasNextDiscoverPage && !_isSearching.value && !_isNextPageLoading.value) {
            performSearch(resetPage = false)
        }
    }

    // ------------------------------------------------------------------------
    // DETAIL SCREEN
    // ------------------------------------------------------------------------

    fun selectAnimeDetail(animeId: Int) {
        _selectedAnimeId.value = animeId
        _selectedRichDetail.value = null
        _animeReviews.value = emptyList()
        _ratingFeedbackMessage.value = null
        _discussionComments.value = emptyList()
        _discussionTotalCount.value = 0
        _discussionHasMore.value = false
        _discussionSuccessMessage.value = null
        _discussionErrorMessage.value = null
        viewModelScope.launch {
            _isDetailLoading.value = true
            launch { loadAnimeDiscussion(animeId, reset = true) }
            metadataProvider.getMediaDetails(animeId).onSuccess { media ->
                if (media != null) {
                    lastDetailRootMedia = media
                    val existing = repository.getAnimeByIdSync(media.id)
                    val mapped = AniListMetadataProvider.mapMediaToEntity(media, existing)
                    repository.upsertCatalogItems(listOf(mapped), keepFullSynopsis = true)
                    _selectedRichDetail.value = AniListMetadataProvider.mapRichDetail(media)
                    val initialEnriched = metadataProvider.getRichDetailWithSeasons(
                        rootMedia = media,
                        overrideMode = com.example.model.FranchiseSeasonOverrideMode.AUTO
                    )
                    val savedMode = _franchiseSeasonOverrides.value[initialEnriched.franchiseId]
                        ?: com.example.model.FranchiseSeasonOverrideMode.AUTO
                    val finalEnriched = if (savedMode != com.example.model.FranchiseSeasonOverrideMode.AUTO) {
                        metadataProvider.getRichDetailWithSeasons(
                            rootMedia = media,
                            overrideMode = savedMode
                        )
                    } else {
                        initialEnriched
                    }
                    cacheFranchiseCatalogNodes(finalEnriched)
                    if (_selectedAnimeId.value == animeId) {
                        _selectedRichDetail.value = finalEnriched
                    }
                }
            }
            val session = _authSession.value
            if (session != null && _userProfile.value?.memberStatus == MemberStatus.ACTIVE) {
                supabaseDb.fetchCommunityFeed(session, _communityMembers.value, animeIdFilter = animeId)
                    .onSuccess { (reviews, _) ->
                        _animeReviews.value = reviews
                    }
            }
            _isDetailLoading.value = false
        }
    }

    private suspend fun cacheFranchiseCatalogNodes(richDetail: AnimeRichDetail) {
        val candidateIds = (
            richDetail.seasons.flatMap { s -> listOf(s.id) + s.subEntries.map { it.id } } +
                richDetail.moviesAndOvas.map { it.id }
            ).distinct()
        val toUpsert = mutableListOf<AnimeEntity>()
        for (cid in candidateIds) {
            val cachedMedia = metadataProvider.getCachedSeasonMedia(cid) ?: continue
            val existing = repository.getAnimeByIdSync(cid)
            toUpsert.add(AniListMetadataProvider.mapMediaToEntity(cachedMedia, existing))
        }
        if (toUpsert.isNotEmpty()) {
            repository.upsertCatalogItems(toUpsert)
        }
    }

    fun toggleFranchiseSeasonOverride(franchiseId: Int, currentlyMergedAsOne: Boolean) {
        if (franchiseId <= 0) return
        val nextMode = if (currentlyMergedAsOne) {
            com.example.model.FranchiseSeasonOverrideMode.SPLIT_ALL
        } else {
            com.example.model.FranchiseSeasonOverrideMode.MERGE_ONE
        }
        seasonOverridePrefs.edit().putString(franchiseId.toString(), nextMode.storageValue).apply()
        val updatedMap = _franchiseSeasonOverrides.value.toMutableMap()
        updatedMap[franchiseId] = nextMode
        _franchiseSeasonOverrides.value = updatedMap

        val media = lastDetailRootMedia
        val currentId = _selectedAnimeId.value
        if (media != null && currentId != null) {
            viewModelScope.launch {
                val reResolved = metadataProvider.getRichDetailWithSeasons(
                    rootMedia = media,
                    overrideMode = nextMode
                )
                cacheFranchiseCatalogNodes(reResolved)
                if (_selectedAnimeId.value == currentId) {
                    _selectedRichDetail.value = reResolved
                }
                _userMessage.value = if (nextMode == com.example.model.FranchiseSeasonOverrideMode.MERGE_ONE) {
                    "Merged franchise into 1 continuous season."
                } else {
                    "Split franchise into separate numbered seasons."
                }
            }
        }
    }

    fun computeContinuousWatchedProgress(
        seasonEntry: com.example.model.AnimeSeasonEntry?,
        currentEntity: AnimeEntity,
        trackedById: Map<Int, AnimeEntity>
    ): Int {
        if (seasonEntry == null || seasonEntry.subEntries.size <= 1) {
            return currentEntity.watchedEpisodeCount
        }
        var sumOfWatched = 0
        var maxOffsetPlusWatched = 0
        for (seg in seasonEntry.subEntries) {
            val entity = if (seg.id == currentEntity.id) currentEntity else trackedById[seg.id]
            val w = entity?.watchedEpisodeCount ?: 0
            if (w > 0) {
                val segCap = seg.episodes?.takeIf { it > 0 } ?: w
                sumOfWatched += w.coerceIn(0, segCap)
                val impliedContinuous = seg.startEpisodeOffset + w
                if (impliedContinuous > maxOffsetPlusWatched) {
                    maxOffsetPlusWatched = impliedContinuous
                }
            }
        }
        return maxOf(sumOfWatched, maxOffsetPlusWatched)
    }

    suspend fun commitMergedContinuousEpisodeProgress(
        seasonEntry: com.example.model.AnimeSeasonEntry,
        targetContinuousEpisode: Int
    ): Result<AnimeEntity> {
        val currentId = _selectedAnimeId.value ?: seasonEntry.id
        if (seasonEntry.subEntries.size <= 1) {
            return commitEpisodeProgress(currentId, targetContinuousEpisode)
        }
        val totalMerged = seasonEntry.episodes
        val validation = com.example.model.ProgressRules.validateEpisodeInput(
            rawInput = targetContinuousEpisode.toString(),
            totalEpisodes = totalMerged
        )
        val validatedContinuousEp = when (validation) {
            is com.example.model.EpisodeInputValidation.Invalid -> {
                return Result.failure(Exception(validation.message))
            }
            is com.example.model.EpisodeInputValidation.Valid -> validation.episodes
        }

        // Determine which segment owns validatedContinuousEp
        val activeSegment = seasonEntry.subEntries.lastOrNull { seg ->
            validatedContinuousEp > seg.startEpisodeOffset
        } ?: seasonEntry.subEntries.first()

        var primaryUpdated: AnimeEntity? = null
        for (seg in seasonEntry.subEntries) {
            val segMax = seg.episodes?.takeIf { it > 0 }
            val localEp = if (segMax != null) {
                (validatedContinuousEp - seg.startEpisodeOffset).coerceIn(0, segMax)
            } else {
                (validatedContinuousEp - seg.startEpisodeOffset).coerceAtLeast(0)
            }
            var existing = repository.getAnimeByIdSync(seg.id)
            if (existing == null) {
                val cachedMedia = metadataProvider.getCachedSeasonMedia(seg.id)
                if (cachedMedia != null) {
                    val mapped = AniListMetadataProvider.mapMediaToEntity(cachedMedia, null)
                    repository.upsertCatalogItems(listOf(mapped))
                    existing = repository.getAnimeByIdSync(seg.id)
                }
            }
            val shouldUpdateSeg = seg.id == currentId ||
                seg.id == activeSegment.id ||
                existing?.inWatchlist == true ||
                (existing?.watchedEpisodeCount ?: 0) > 0 ||
                localEp > 0
            if (shouldUpdateSeg && existing != null) {
                if (existing.watchedEpisodeCount != localEp || !existing.inWatchlist) {
                    val res = commitEpisodeProgress(seg.id, localEp)
                    if (seg.id == currentId) {
                        primaryUpdated = res.getOrNull()
                    }
                } else if (seg.id == currentId) {
                    primaryUpdated = existing
                }
            }
        }
        val finalEntity = primaryUpdated ?: repository.getAnimeByIdSync(currentId)
            ?: return Result.failure(Exception("Unable to save continuous episode progress."))
        return Result.success(finalEntity)
    }

    fun stepMergedContinuousEpisode(
        seasonEntry: com.example.model.AnimeSeasonEntry,
        currentContinuousWatched: Int,
        delta: Int
    ) {
        viewModelScope.launch {
            val maxEp = seasonEntry.episodes
            val nextEp = (currentContinuousWatched + delta).coerceAtLeast(0).let {
                if (maxEp != null && maxEp > 0) it.coerceAtMost(maxEp) else it
            }
            commitMergedContinuousEpisodeProgress(seasonEntry, nextEp).onFailure { err ->
                _userMessage.value = err.message ?: "Unable to update episode progress."
            }
        }
    }

    // ------------------------------------------------------------------------
    // WATCHLIST, PROGRESS, COLLECTIONS
    // ------------------------------------------------------------------------

    fun addToWatchlist(
        anime: AnimeEntity,
        status: WatchStatus = WatchStatus.WATCHING,
        addToHome: Boolean = status == WatchStatus.WATCHING
    ) {
        viewModelScope.launch {
            val saved = repository.addToWatchlist(anime, status, addToHome = addToHome)
            syncSingleEntryToCloud(saved)
            val session = _authSession.value
            if (session != null && saved.inHome) {
                supabaseDb.addToHome(session, saved)
            }
            syncDisplayedCatalogPersonalFlags()
            val syncText = if (session != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Added • $syncText",
                subtitle = "${saved.displayTitle} (${saved.watchStatusEnum.label})",
                animeId = saved.id
            )
            _userMessage.value = "Added ${saved.displayTitle} to ${saved.watchStatusEnum.label}."
        }
    }

    fun addToHome(anime: AnimeEntity) {
        viewModelScope.launch {
            val saved = repository.addToHome(anime)
            val session = _authSession.value
            if (session != null) {
                dirtyPendingSyncIds.add(saved.id)
                supabaseDb.addToHome(session, saved).onSuccess {
                    dirtyPendingSyncIds.remove(saved.id)
                }
            }
            persistCurrentOwnerVaultSnapshot()
            syncDisplayedCatalogPersonalFlags()
            val syncText = if (session != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Added to Home • $syncText",
                subtitle = saved.displayTitle,
                animeId = saved.id
            )
            _userMessage.value = "Added ${saved.displayTitle} to Home tracking."
        }
    }

    fun removeFromHome(animeId: Int) {
        viewModelScope.launch {
            val updated = repository.removeFromHomeOnly(animeId) ?: return@launch
            val session = _authSession.value
            if (session != null) {
                dirtyPendingSyncIds.add(animeId)
                supabaseDb.removeFromHome(session, animeId).onSuccess {
                    dirtyPendingSyncIds.remove(animeId)
                }
            }
            persistCurrentOwnerVaultSnapshot()
            syncDisplayedCatalogPersonalFlags()
            val syncText = if (session != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Removed from Home • $syncText",
                subtitle = "${updated.displayTitle} kept in Library",
                animeId = updated.id
            )
            _userMessage.value = "Removed ${updated.displayTitle} from Home. Kept in Library."
        }
    }

    fun incrementEpisode(animeId: Int) {
        viewModelScope.launch {
            val before = repository.getAnimeByIdSync(animeId) ?: return@launch
            val prevEpisode = before.watchedEpisodeCount
            val prevStatus = before.watchStatusEnum
            val updated = repository.incrementEpisode(animeId) ?: return@launch
            syncSingleEntryToCloud(updated)
            val session = _authSession.value
            if (session != null && before.inHome && !updated.inHome) {
                supabaseDb.removeFromHome(session, updated.id)
            }
            val syncText = if (session != null) "Synced" else "Saved"
            val autoCompleted = prevStatus != WatchStatus.COMPLETED && updated.watchStatusEnum == WatchStatus.COMPLETED
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = if (autoCompleted) {
                    "Completed • $syncText"
                } else {
                    "Ep ${updated.watchedEpisodeCount} Logged • $syncText"
                },
                subtitle = if (autoCompleted) "${updated.displayTitle} moved to Library" else updated.displayTitle,
                animeId = updated.id,
                previousEpisode = prevEpisode,
                previousStatus = prevStatus,
                canUndo = true
            )
            _userMessage.value = if (autoCompleted) {
                "Completed ${updated.displayTitle}! Removed from Home and kept in Library."
            } else {
                "${updated.displayTitle}: Ep ${updated.watchedEpisodeCount}"
            }
        }
    }

    fun decrementEpisode(animeId: Int) {
        viewModelScope.launch {
            val before = repository.getAnimeByIdSync(animeId) ?: return@launch
            if (before.watchedEpisodeCount <= 0) return@launch
            val prevEpisode = before.watchedEpisodeCount
            val prevStatus = before.watchStatusEnum
            val targetEpisode = (prevEpisode - 1).coerceAtLeast(0)
            val updated = repository.updateUserTrackingState(
                animeId = animeId,
                status = prevStatus,
                episodeProgress = targetEpisode,
                score = before.userScore,
                notes = before.userNotes,
                isFavorite = before.isFavorite
            ) ?: return@launch
            syncSingleEntryToCloud(updated)
            val syncText = if (_authSession.value != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Ep ${updated.watchedEpisodeCount} Logged • $syncText",
                subtitle = updated.displayTitle,
                animeId = updated.id,
                previousEpisode = prevEpisode,
                previousStatus = prevStatus,
                canUndo = true
            )
        }
    }

    suspend fun commitEpisodeProgress(animeId: Int, targetEpisode: Int): Result<AnimeEntity> {
        val before = repository.getAnimeByIdSync(animeId)
            ?: return Result.failure(Exception("Anime entry not found."))
        val validation = com.example.model.ProgressRules.validateEpisodeInput(
            rawInput = targetEpisode.toString(),
            totalEpisodes = before.totalEpisodes
        )
        val validatedEp = when (validation) {
            is com.example.model.EpisodeInputValidation.Invalid -> {
                return Result.failure(Exception(validation.message))
            }
            is com.example.model.EpisodeInputValidation.Valid -> validation.episodes
        }
        val autoComplete = before.watchStatusEnum != WatchStatus.DROPPED &&
            com.example.model.ProgressRules.shouldAutoComplete(
                officialStatus = before.status,
                totalEpisodes = before.totalEpisodes,
                watchedEpisodes = validatedEp
            )
        // Preserve existing status when editing progress; only transition to Completed on strict official completion
        val nextStatus = when {
            autoComplete -> WatchStatus.COMPLETED
            !before.inWatchlist -> WatchStatus.WATCHING
            else -> before.watchStatusEnum
        }
        val updated = repository.updateUserTrackingState(
            animeId = animeId,
            status = nextStatus,
            episodeProgress = validatedEp,
            score = before.userScore,
            notes = before.userNotes,
            isFavorite = before.isFavorite
        ) ?: return Result.failure(Exception("Unable to save episode progress."))

        val session = _authSession.value
        if (session != null) {
            dirtyPendingSyncIds.add(animeId)
            val cloudRes = supabaseDb.updateEpisodeProgress(session, updated, validatedEp)
            if (cloudRes.isFailure) {
                dirtyPendingSyncIds.remove(animeId)
                // Restore previous value if cloud update fails
                repository.restoreEntitySnapshot(before)
                persistCurrentOwnerVaultSnapshot()
                syncDisplayedCatalogPersonalFlags()
                val msg = ErrorSanitizer.sanitize(cloudRes.exceptionOrNull(), "Failed to sync episode progress.")
                return Result.failure(Exception(msg))
            }
            dirtyPendingSyncIds.remove(animeId)
            if (before.inHome && !updated.inHome) {
                supabaseDb.removeFromHome(session, animeId)
            }
        }
        persistCurrentOwnerVaultSnapshot()
        syncDisplayedCatalogPersonalFlags()

        val syncText = if (session != null) "Synced" else "Saved"
        _dynamicIslandFeedback.value = DynamicIslandFeedback(
            title = if (autoComplete) "Completed • $syncText" else "Ep ${updated.watchedEpisodeCount} Logged • $syncText",
            subtitle = if (autoComplete) "${updated.displayTitle} kept in Library" else updated.displayTitle,
            animeId = updated.id,
            previousEpisode = before.watchedEpisodeCount,
            previousStatus = before.watchStatusEnum,
            canUndo = true
        )
        return Result.success(updated)
    }

    fun setEpisodeProgress(animeId: Int, targetEpisode: Int) {
        viewModelScope.launch {
            commitEpisodeProgress(animeId, targetEpisode).onFailure { err ->
                _userMessage.value = err.message ?: "Unable to update episode progress."
            }
        }
    }

    fun undoLastEpisodeAction() {
        val feedback = _dynamicIslandFeedback.value ?: return
        val targetId = feedback.animeId ?: return
        val prevEp = feedback.previousEpisode ?: return
        val prevStatus = feedback.previousStatus ?: WatchStatus.WATCHING
        viewModelScope.launch {
            val current = repository.getAnimeByIdSync(targetId) ?: return@launch
            val restored = repository.updateUserTrackingState(
                animeId = targetId,
                status = prevStatus,
                episodeProgress = prevEp,
                score = current.userScore,
                notes = current.userNotes,
                isFavorite = current.isFavorite
            )
            if (restored != null) {
                syncSingleEntryToCloud(restored)
            }
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Reverted to Ep $prevEp",
                subtitle = feedback.subtitle,
                animeId = targetId,
                canUndo = false
            )
        }
    }

    fun updateTracking(
        animeId: Int,
        status: WatchStatus,
        episodeProgress: Int,
        scoreTenths: Int?,
        notes: String,
        isFavorite: Boolean,
        seasonNumber: Int? = null
    ) {
        viewModelScope.launch {
            val before = repository.getAnimeByIdSync(animeId)
            val updated = repository.updateUserTrackingState(
                animeId = animeId,
                status = status,
                episodeProgress = episodeProgress,
                seasonNumber = seasonNumber,
                score = scoreTenths,
                notes = notes,
                isFavorite = isFavorite
            ) ?: return@launch
            syncSingleEntryToCloud(updated)
            val session = _authSession.value
            if (session != null && before?.inHome == true && !updated.inHome) {
                supabaseDb.removeFromHome(session, updated.id)
            }
            syncDisplayedCatalogPersonalFlags()
            val syncText = if (session != null) "Synced" else "Saved"
            val hadPreviousScore = (before?.userScore ?: 0) > 0
            val newStars = StarRatingUtils.score100ToStars(updated.userScore)
            val ratingBanner = when {
                newStars > 0f && hadPreviousScore && before?.userScore != updated.userScore ->
                    "Rating updated to ${StarRatingUtils.formatStars(newStars)}★"
                newStars > 0f && !hadPreviousScore ->
                    "Rating saved (${StarRatingUtils.formatStars(newStars)}★)"
                !notes.isNullOrBlank() && notes.trim() != before?.userNotes?.trim().orEmpty() ->
                    "Review & tracking saved"
                else -> "Tracking saved for ${updated.displayTitle}"
            }
            _ratingFeedbackMessage.value = ratingBanner
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "$ratingBanner • $syncText",
                subtitle = "${updated.displayTitle} (S${updated.effectiveSeasonNumber} • ${updated.watchStatusEnum.label})",
                animeId = updated.id
            )
            _userMessage.value = when {
                updated.watchStatusEnum == WatchStatus.DROPPED && before?.inHome == true ->
                    "Marked ${updated.displayTitle} as Dropped. Removed from Home and preserved in Library."
                else -> ratingBanner
            }
        }
    }

    fun setSeasonProgress(
        animeId: Int,
        seasonNumber: Int,
        resetEpisodes: Boolean = false
    ) {
        viewModelScope.launch {
            val updated = repository.updateSeasonProgress(
                animeId = animeId,
                seasonNumber = seasonNumber,
                resetEpisodes = resetEpisodes
            ) ?: return@launch
            syncSingleEntryToCloud(updated)
            val syncText = if (_authSession.value != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Season ${updated.effectiveSeasonNumber} • $syncText",
                subtitle = if (resetEpisodes) "${updated.displayTitle} • Ready for Ep 1" else updated.displayTitle,
                animeId = updated.id
            )
        }
    }

    fun toggleFavorite(animeId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            val updated = repository.toggleFavorite(animeId, isFavorite) ?: return@launch
            syncSingleEntryToCloud(updated)
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = if (isFavorite) "Favorited" else "Removed Favorite",
                subtitle = updated.displayTitle,
                animeId = updated.id
            )
        }
    }

    fun removeFromWatchlist(animeId: Int) {
        viewModelScope.launch {
            repository.removeFromWatchlist(animeId)
            _searchResults.value = _searchResults.value.map { item ->
                if (item.id == animeId) {
                    item.copy(inHome = false, inWatchlist = false, isWishlist = false, isPinned = false, isWatched = false, userStatus = null)
                } else item
            }
            val session = _authSession.value
            if (session != null) {
                dirtyPendingSyncIds.remove(animeId)
                supabaseDb.removeFromHome(session, animeId)
                supabaseDb.deleteUserAnimeEntry(session, animeId)
            }
            persistCurrentOwnerVaultSnapshot()
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Removed from List",
                subtitle = "Watchlist entry cleared",
                animeId = animeId
            )
            _userMessage.value = "Removed from My List. Community notes remain preserved."
        }
    }

    private suspend fun syncSingleEntryToCloud(entity: AnimeEntity) {
        persistCurrentOwnerVaultSnapshot()
        val session = _authSession.value ?: return
        dirtyPendingSyncIds.add(entity.id)
        supabaseDb.upsertUserAnimeEntry(session, entity).onSuccess {
            dirtyPendingSyncIds.remove(entity.id)
            val profile = _userProfile.value
            if (profile != null && profile.memberStatus == MemberStatus.ACTIVE) {
                val trimmedNote = entity.userNotes?.trim().orEmpty()
                val rating10 = entity.userScore?.takeIf { it > 0 }?.let { it / 10.0 }
                if (entity.hasActiveUserState && (trimmedNote.isNotBlank() || rating10 != null)) {
                    val existing = _communityReviews.value.firstOrNull {
                        it.animeId == entity.id && it.userId == session.userId
                    }
                    val entityGenres = entity.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val optimisticReview = CommunityReview(
                        id = existing?.id ?: "${session.userId}-${entity.id}",
                        userId = session.userId,
                        username = profile.username,
                        avatarKey = profile.avatarKey,
                        avatarUrl = profile.avatarUrl,
                        animeId = entity.id,
                        animeTitle = entity.displayTitle,
                        animeCoverImage = entity.coverImage,
                        rating = rating10,
                        reviewNotes = trimmedNote,
                        status = entity.watchStatusEnum,
                        episodeProgress = entity.watchedEpisodeCount,
                        totalEpisodes = entity.totalEpisodes,
                        updatedAt = Instant.now().toString(),
                        likesCount = existing?.likesCount ?: 0,
                        likedByMe = existing?.likedByMe ?: false,
                        genres = entityGenres,
                        isAdult = entity.isAdult || com.example.model.isHentaiOrAdultGenres(entityGenres)
                    )
                    val updatedCommunity = listOf(optimisticReview) + _communityReviews.value.filterNot {
                        it.animeId == entity.id && it.userId == session.userId
                    }
                    _communityReviews.value = sortReviews(updatedCommunity, _reviewSortOption.value)
                    if (_selectedAnimeId.value == entity.id) {
                        _animeReviews.value = listOf(optimisticReview) + _animeReviews.value.filterNot {
                            it.userId == session.userId
                        }
                    }
                }
                refreshCommunityData()
            }
        }
    }

    /**
     * Independent Community note/rating submission:
     * Posting a note, comment, review, or rating MUST NEVER add the anime to Home.
     */
    fun submitCommunityCommentNote(
        animeId: Int,
        animeTitle: String,
        coverImage: String,
        ratingStars: Float,
        noteText: String,
        onSuccessCallback: () -> Unit = {}
    ) {
        if (_isSubmittingCommunityNote.value) return
        val cleanNote = noteText.trim()
        if (cleanNote.isBlank() && ratingStars <= 0f) {
            _communityNoteError.value = "Write a note or select a star rating first."
            _userMessage.value = "Write a note or select a star rating first."
            return
        }
        viewModelScope.launch {
            _isSubmittingCommunityNote.value = true
            _communityNoteError.value = null
            _communityNoteFeedback.value = null
            try {
                val existing = repository.getAnimeByIdSync(animeId)
                val resolvedScoreTenths = if (ratingStars > 0f) {
                    StarRatingUtils.starsToScore100(ratingStars)
                } else {
                    existing?.userScore
                }
                val rating10 = resolvedScoreTenths?.takeIf { it > 0 }?.let { it / 10.0 }
                val finalNote = cleanNote.ifBlank { existing?.userNotes.orEmpty() }

                // If the anime is ALREADY in the user's Library, mirror rating/note without altering inHome!
                if (existing != null && existing.inWatchlist) {
                    val updated = repository.updateUserTrackingState(
                        animeId = animeId,
                        status = existing.watchStatusEnum,
                        episodeProgress = existing.watchedEpisodeCount,
                        seasonNumber = existing.userSeason,
                        score = resolvedScoreTenths,
                        notes = finalNote,
                        isFavorite = existing.isFavorite,
                        explicitInHome = existing.inHome
                    )
                    if (updated != null) {
                        persistCurrentOwnerVaultSnapshot()
                    }
                }

                val session = _authSession.value
                val profile = _userProfile.value
                if (session != null) {
                    val existingReview = _communityReviews.value.firstOrNull {
                        it.animeId == animeId && it.userId == session.userId
                    }
                    val existingGenresStr = existing?.genres.orEmpty()
                    val existingGenresList = existingGenresStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val res = supabaseDb.upsertCommunityNote(
                        session = session,
                        animeId = animeId,
                        animeTitle = animeTitle.ifBlank { existing?.displayTitle ?: "Anime #$animeId" },
                        coverImage = coverImage.ifBlank { existing?.coverImage.orEmpty() },
                        rating10 = rating10,
                        reviewNotes = finalNote,
                        genres = existingGenresStr
                    )
                    res.onSuccess {
                        if (profile != null) {
                            val mergedGenres = (existingGenresList + existingReview?.genres.orEmpty()).distinct()
                            val optimisticReview = CommunityReview(
                                id = existingReview?.id ?: "note-${session.userId}-$animeId",
                                userId = session.userId,
                                username = profile.username,
                                avatarKey = profile.avatarKey,
                                avatarUrl = profile.avatarUrl,
                                animeId = animeId,
                                animeTitle = animeTitle.ifBlank { existing?.displayTitle ?: "Anime #$animeId" },
                                animeCoverImage = coverImage.ifBlank { existing?.coverImage.orEmpty() },
                                rating = rating10,
                                reviewNotes = finalNote,
                                status = existing?.watchStatusEnum ?: WatchStatus.WATCHING,
                                episodeProgress = existing?.watchedEpisodeCount ?: 0,
                                totalEpisodes = existing?.totalEpisodes,
                                updatedAt = Instant.now().toString(),
                                likesCount = existingReview?.likesCount ?: 0,
                                likedByMe = existingReview?.likedByMe ?: false,
                                genres = mergedGenres,
                                isAdult = (existing?.isAdult == true) ||
                                    (existingReview?.isAdult == true) ||
                                    com.example.model.isHentaiOrAdultGenres(mergedGenres)
                            )
                            _communityReviews.value = sortReviews(
                                listOf(optimisticReview) + _communityReviews.value.filterNot {
                                    it.animeId == animeId && it.userId == session.userId
                                },
                                _reviewSortOption.value
                            )
                            if (_selectedAnimeId.value == animeId) {
                                _animeReviews.value = listOf(optimisticReview) + _animeReviews.value.filterNot {
                                    it.userId == session.userId
                                }
                            }
                        }
                        refreshCommunityData()
                        val msg = when {
                            cleanNote.isNotBlank() && ratingStars > 0f ->
                                "Comment & ${StarRatingUtils.formatStars(ratingStars)}★ rating saved successfully"
                            cleanNote.isNotBlank() -> "Comment posted successfully"
                            existingReview != null -> "Rating updated to ${StarRatingUtils.formatStars(ratingStars)}★"
                            else -> "Rating saved (${StarRatingUtils.formatStars(ratingStars)}★)"
                        }
                        _communityNoteFeedback.value = msg
                        _dynamicIslandFeedback.value = DynamicIslandFeedback(
                            title = msg,
                            subtitle = animeTitle,
                            animeId = animeId
                        )
                        _userMessage.value = msg
                        onSuccessCallback()
                    }.onFailure { err ->
                        val msg = ErrorSanitizer.sanitize(err, "Unable to post community note right now.")
                        _communityNoteError.value = msg
                        _userMessage.value = msg
                    }
                } else {
                    _communityNoteError.value = "Sign in to post notes to Community."
                    _userMessage.value = "Sign in to post notes to Community."
                }
            } finally {
                _isSubmittingCommunityNote.value = false
            }
        }
    }

    fun createCollection(name: String, description: String, visibility: VisibilityLevel) {
        val session = _authSession.value
        if (session == null || _userProfile.value == null) {
            _userMessage.value = "Sign in to create cloud collections."
            return
        }
        if (name.isBlank()) return
        viewModelScope.launch {
            supabaseDb.createCollection(session, name, description, visibility).onSuccess { created ->
                _collections.value = listOf(created) + _collections.value
                _userMessage.value = "Created collection \"${created.name}\"."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Failed to create collection.")
            }
        }
    }

    fun renameOrUpdateCollection(collectionId: String, name: String, visibility: VisibilityLevel) {
        val session = _authSession.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            supabaseDb.updateCollection(session, collectionId, name, visibility).onSuccess {
                _collections.value = _collections.value.map {
                    if (it.id == collectionId) it.copy(name = name.trim(), visibility = visibility) else it
                }
                _userMessage.value = "Collection updated."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Failed to update collection.")
            }
        }
    }

    fun deleteCollection(collectionId: String) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.deleteCollection(session, collectionId).onSuccess {
                _collections.value = _collections.value.filterNot { it.id == collectionId }
                _userMessage.value = "Collection deleted."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Failed to delete collection.")
            }
        }
    }

    fun addAnimeToCollection(collectionId: String, anime: AnimeEntity) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.addAnimeToCollection(session, collectionId, anime).onSuccess {
                _collections.value = _collections.value.map { col ->
                    if (col.id == collectionId && !col.animeIds.contains(anime.id)) {
                        col.copy(animeIds = col.animeIds + anime.id)
                    } else col
                }
                _userMessage.value = "Added to collection."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Failed to add to collection.")
            }
        }
    }

    fun syncCloudNow() {
        val session = _authSession.value
        if (session == null) {
            _userMessage.value = "Sign in to sync your library to the cloud."
            return
        }
        val capturedUserId = session.userId
        viewModelScope.launch {
            supabaseDb.fetchUserWatchlist(session).onSuccess { cloudItems ->
                if (_authSession.value?.userId != capturedUserId) return@onSuccess
                repository.syncCloudWatchlistEntries(
                    cloudItems = cloudItems,
                    replaceMissingFromCloud = true,
                    protectedPendingIds = dirtyPendingSyncIds.toSet()
                )
                persistCurrentOwnerVaultSnapshot()
                syncDisplayedCatalogPersonalFlags()
                _userMessage.value = "Library synced cleanly."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Unable to sync right now.")
            }
        }
    }

    fun removeAnimeFromCollection(collectionId: String, animeId: Int) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.removeAnimeFromCollection(session, collectionId, animeId).onSuccess {
                _collections.value = _collections.value.map { col ->
                    if (col.id == collectionId) col.copy(animeIds = col.animeIds.filterNot { it == animeId }) else col
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // COMMUNITY, PROFILE, PRIVACY, INVITATIONS
    // ------------------------------------------------------------------------

    fun refreshCommunityData() {
        val session = _authSession.value ?: return
        if (_userProfile.value?.memberStatus != MemberStatus.ACTIVE) return
        viewModelScope.launch {
            _isCommunityLoading.value = true
            _communityError.value = null
            supabaseDb.fetchCommunityCapacity(session).onSuccess { _communityCapacity.value = it }
            val membersRes = supabaseDb.fetchCommunityMembers(session)
            membersRes.onSuccess { members ->
                _communityMembers.value = members
                supabaseDb.fetchCommunityFeed(session, members).onSuccess { (reviews, activity) ->
                    _communityReviews.value = sortReviews(reviews, _reviewSortOption.value)
                    _communityActivity.value = activity
                }.onFailure { err ->
                    _communityError.value = ErrorSanitizer.sanitize(err, "Unable to load community feed.")
                }
            }.onFailure { err ->
                _communityError.value = ErrorSanitizer.sanitize(err, "Unable to load community members.")
            }
            refreshUnseenDiscussionAnime()
            _isCommunityLoading.value = false
        }
    }

    fun setReviewSortOption(option: ReviewSortOption) {
        _reviewSortOption.value = option
        _communityReviews.value = sortReviews(_communityReviews.value, option)
    }

    private fun sortReviews(list: List<CommunityReview>, option: ReviewSortOption): List<CommunityReview> {
        return when (option) {
            ReviewSortOption.NEWEST -> list.sortedByDescending { it.updatedAt }
            ReviewSortOption.HIGHEST_RATED -> list.sortedWith(
                compareByDescending<CommunityReview> { it.rating ?: -1.0 }
                    .thenByDescending { it.updatedAt }
            )
            ReviewSortOption.MOST_LIKED -> list.sortedWith(
                compareByDescending<CommunityReview> { it.likesCount }
                    .thenByDescending { it.rating ?: -1.0 }
                    .thenByDescending { it.updatedAt }
            )
        }
    }

    fun toggleReviewLike(review: CommunityReview) {
        val session = _authSession.value ?: return
        val optimisticLiked = !review.likedByMe
        val optimisticCount = maxOf(0, review.likesCount + if (optimisticLiked) 1 else -1)
        _communityReviews.value = _communityReviews.value.map {
            if (it.id == review.id) it.copy(likedByMe = optimisticLiked, likesCount = optimisticCount) else it
        }
        _animeReviews.value = _animeReviews.value.map {
            if (it.id == review.id) it.copy(likedByMe = optimisticLiked, likesCount = optimisticCount) else it
        }
        viewModelScope.launch {
            supabaseDb.toggleReviewLike(session, review.id, review.likedByMe, review.likesCount)
                .onSuccess { (liked, count) ->
                    _communityReviews.value = _communityReviews.value.map {
                        if (it.id == review.id) it.copy(likedByMe = liked, likesCount = count) else it
                    }
                    _animeReviews.value = _animeReviews.value.map {
                        if (it.id == review.id) it.copy(likedByMe = liked, likesCount = count) else it
                    }
                }
        }
    }

    fun refreshStorageStats() {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            _isStorageStatsLoading.value = true
            supabaseDb.fetchDatabaseStorageStats(session).onSuccess { stats ->
                _storageStats.value = stats
            }
            _isStorageStatsLoading.value = false
        }
    }

    fun inspectMemberProfile(member: CommunityMember) {
        val session = _authSession.value ?: return
        _inspectedMemberWatchlist.value = emptyList()
        _inspectedMemberCollections.value = emptyList()
        viewModelScope.launch {
            _isMemberInspectLoading.value = true
            val canViewWatchlist = member.userId == session.userId ||
                member.privacy.watchlistVisibility == VisibilityLevel.COMMUNITY
            if (canViewWatchlist) {
                supabaseDb.fetchUserWatchlist(session, member.userId).onSuccess {
                    _inspectedMemberWatchlist.value = it
                }
            }
            val canViewCollections = member.userId == session.userId ||
                member.privacy.collectionsVisibility == VisibilityLevel.COMMUNITY
            if (canViewCollections) {
                supabaseDb.fetchCollections(session, creatorUserId = member.userId).onSuccess { allCols ->
                    _inspectedMemberCollections.value = allCols.filter {
                        it.creatorId == member.userId && (member.userId == session.userId || it.visibility == VisibilityLevel.COMMUNITY)
                    }
                }
            }
            _isMemberInspectLoading.value = false
        }
    }

    fun updateProfile(
        username: String,
        avatarKey: String,
        clearCustomAvatar: Boolean = true,
        bio: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val session = _authSession.value ?: return
        val current = _userProfile.value ?: return
        val cleanName = username.trim()
        if (cleanName.length !in 2..32) {
            _profileError.value = "Username must be between 2 and 32 characters."
            return
        }
        if (cleanName.contains("@")) {
            _profileError.value = "Usernames cannot contain the @ symbol."
            return
        }
        val targetAvatarUrl = if (clearCustomAvatar) null else current.avatarUrl
        viewModelScope.launch {
            _isProfileSaving.value = true
            _profileError.value = null
            if (!cleanName.equals(current.username, ignoreCase = true) &&
                supabaseAuth.checkLocalUsernameCooldownSeconds(cleanName) > 0L
            ) {
                _profileError.value = "That username was recently deleted and will be available again after 24 hours."
                _isProfileSaving.value = false
                return@launch
            }
            supabaseDb.updateProfile(session, cleanName, avatarKey, targetAvatarUrl, bio).onSuccess {
                _userProfile.value = current.copy(
                    username = cleanName,
                    avatarKey = avatarKey,
                    avatarUrl = targetAvatarUrl,
                    bio = ""
                )
                supabaseAuth.cacheProfileSnapshot(cleanName, avatarKey)
                refreshCommunityData()
                _userMessage.value = "Profile updated."
                onSuccess()
            }.onFailure { err ->
                val clean = ErrorSanitizer.sanitize(err, "Unable to update profile.")
                _profileError.value = clean
            }
            _isProfileSaving.value = false
        }
    }

    fun updatePrivacySettings(updated: PrivacySettings) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            val previous = _privacySettings.value
            _privacySettings.value = updated
            supabaseDb.savePrivacySettings(session, updated).onSuccess {
                refreshCommunityData()
                _userMessage.value = "Privacy settings updated."
            }.onFailure { err ->
                _privacySettings.value = previous
                _userMessage.value = ErrorSanitizer.sanitize(err, "Unable to save privacy settings.")
            }
        }
    }

    fun fetchInvitations() {
        val session = _authSession.value ?: return
        if (_userProfile.value?.isInviteCreator != true) return
        viewModelScope.launch {
            supabaseDb.fetchInvitations(session).onSuccess { _invitations.value = it }
        }
    }

    fun generateInvitation(maxUses: Int = 1, expiresInHours: Int = 168) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.createInvitation(session, maxUses, expiresInHours).onSuccess { invite ->
                _invitations.value = listOf(invite) + _invitations.value
                _userMessage.value = "Generated invite code: ${invite.code}"
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Unable to create invitation.")
            }
        }
    }

    fun revokeInvitation(invitationId: String) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.revokeInvitation(session, invitationId).onSuccess {
                _invitations.value = _invitations.value.map {
                    if (it.id == invitationId) it.copy(isActive = false) else it
                }
                _userMessage.value = "Invitation revoked."
            }
        }
    }

    // ------------------------------------------------------------------------
    // APP PREFERENCES & BACKUP
    // ------------------------------------------------------------------------

    fun setThemeMode(mode: ThemeModePreference) = viewModelScope.launch { preferencesRepo.setThemeMode(mode) }
    fun setAccentTheme(accent: String) = viewModelScope.launch { preferencesRepo.setAccentTheme(accent) }
    fun setWatchlistLayout(layout: WatchlistLayoutMode) = viewModelScope.launch { preferencesRepo.setWatchlistLayout(layout) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepo.setNotificationsEnabled(enabled) }
    fun setTitleLanguage(lang: String) = viewModelScope.launch { preferencesRepo.setTitleLanguage(lang) }
    fun setSafeModeEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepo.setSafeModeEnabled(enabled)
        performSearch(resetPage = true)
    }

    fun clearCatalogCache() {
        viewModelScope.launch {
            val count = repository.clearMetadataAndApiCache()
            _userMessage.value = "Cleared $count cached catalog entries. Your saved list is safe."
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = repository.exportBackupJson()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                _userMessage.value = "Backup exported successfully."
            } catch (e: Exception) {
                _userMessage.value = "Export failed: ${ErrorSanitizer.sanitize(e, "Unable to write backup file.")}"
            }
        }
    }

    // ------------------------------------------------------------------------
    // ANIME DISCUSSION COMMENTS, @MENTIONS, REPLY THREADS & NOTIFICATIONS
    // ------------------------------------------------------------------------

    fun loadAnimeDiscussion(animeId: Int, reset: Boolean = true) {
        val session = _authSession.value ?: return
        val switchingAnime = activeDiscussionAnimeId != animeId
        if (reset) {
            activeDiscussionAnimeId = animeId
            if (switchingAnime) {
                _discussionComments.value = emptyList()
                _discussionTotalCount.value = 0
                _discussionHasMore.value = false
                _discussionErrorMessage.value = null
                _discussionSuccessMessage.value = null
            }
        }
        viewModelScope.launch {
            _isDiscussionLoading.value = true
            val offset = if (reset) 0 else _discussionComments.value.size
            supabaseDiscussion.fetchAnimeDiscussionPage(session, animeId, limit = 25, offset = offset)
                .onSuccess { page ->
                    if (activeDiscussionAnimeId == animeId) {
                        page.comments.forEach { c ->
                            if (c.userId.isNotBlank() && c.username.isNotBlank()) {
                                chatProfileCache[c.userId] = MentionableUser(
                                    id = c.userId,
                                    username = c.username,
                                    avatarKey = c.avatarKey,
                                    avatarUrl = c.avatarUrl
                                )
                            }
                        }
                        _discussionTotalCount.value = page.totalComments
                        _discussionHasMore.value = page.hasMore
                        _discussionComments.value = if (reset) {
                            page.comments
                        } else {
                            val existingIds = _discussionComments.value.map { it.id }.toSet()
                            (_discussionComments.value + page.comments.filterNot { it.id in existingIds })
                                .sortedByDescending { it.createdAt }
                        }
                    }
                    if (reset) {
                        markAnimeDiscussionSeen(animeId)
                    }
                }
                .onFailure { err ->
                    if (reset && activeDiscussionAnimeId == animeId) {
                        _discussionErrorMessage.value = err.message
                    }
                }
            _isDiscussionLoading.value = false
        }
    }

    fun startAnimeChatRealtime(animeId: Int) {
        val session = _authSession.value
        if (session == null || animeId <= 0 || !AppConfig.isSupabaseConfigured) {
            stopAnimeChatRealtime()
            return
        }
        if (activeRealtimeAnimeId == animeId && activeChatRealtimeSocket != null) return
        stopAnimeChatRealtime()
        activeDiscussionAnimeId = animeId
        activeRealtimeAnimeId = animeId
        activeChatRealtimeSocket = supabaseDiscussion.subscribeToAnimeChatRealtime(session, animeId) { ev ->
            viewModelScope.launch {
                if (activeDiscussionAnimeId != animeId) return@launch
                when (ev) {
                    is DiscussionRealtimeEvent.InsertComment -> {
                        val raw = ev.row
                        if (_discussionComments.value.any { it.id == raw.id }) return@launch
                        val selfProfile = _userProfile.value
                        val resolvedProfile = when {
                            raw.userId == session.userId && selfProfile != null -> MentionableUser(
                                id = selfProfile.id,
                                username = selfProfile.username,
                                avatarKey = selfProfile.avatarKey,
                                avatarUrl = selfProfile.avatarUrl
                            )
                            chatProfileCache.containsKey(raw.userId) -> chatProfileCache[raw.userId]
                            else -> supabaseDiscussion.fetchUserProfileSnippet(session, raw.userId)?.also {
                                chatProfileCache[raw.userId] = it
                            }
                        }
                        if (activeDiscussionAnimeId != animeId) return@launch
                        if (_discussionComments.value.any { it.id == raw.id }) return@launch
                        val enriched = raw.copy(
                            username = resolvedProfile?.username ?: raw.username,
                            avatarKey = resolvedProfile?.avatarKey ?: raw.avatarKey,
                            avatarUrl = resolvedProfile?.avatarUrl ?: raw.avatarUrl
                        )
                        _discussionComments.value = listOf(enriched) + _discussionComments.value
                        _discussionTotalCount.value = maxOf(_discussionComments.value.size, _discussionTotalCount.value + 1)
                    }
                    is DiscussionRealtimeEvent.UpdateMessage -> {
                        _discussionComments.value = _discussionComments.value.map { c ->
                            if (c.id == ev.id) {
                                c.copy(
                                    content = ev.content.ifBlank { c.content },
                                    updatedAt = ev.updatedAt.ifBlank { c.updatedAt },
                                    replyTo = ev.replyTo ?: c.replyTo
                                )
                            } else c
                        }
                    }
                    is DiscussionRealtimeEvent.DeleteMessage -> {
                        val before = _discussionComments.value.size
                        val next = _discussionComments.value.filterNot { it.id == ev.id }
                        if (next.size != before) {
                            _discussionComments.value = next
                            _discussionTotalCount.value = (before - 1).coerceAtLeast(0)
                        }
                    }
                }
            }
        }
    }

    fun stopAnimeChatRealtime() {
        runCatching { activeChatRealtimeSocket?.close(1000, "closed") }
        activeChatRealtimeSocket = null
        activeRealtimeAnimeId = null
    }

    fun refreshUnseenDiscussionAnime() {
        reconcileUnreadNotificationCount()
    }

    fun loadCommunityEmojis(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { communityEmojiCache.readCachedSync() }
            if (cached.isNotEmpty() && _communityEmojis.value.isEmpty()) {
                _communityEmojis.value = cached
                communityEmojiCache.preloadTopEmojis(cached, count = 8)
            }
            val emojis = communityEmojiCache.getEmojis(_authSession.value, forceRefresh = forceRefresh)
            if (emojis.isNotEmpty()) {
                _communityEmojis.value = emojis
                communityEmojiCache.preloadTopEmojis(emojis, count = 8)
            }
        }
    }

    fun recordCustomEmojiUsage(text: String) {
        val ids = CommunityEmojiCacheService.extractEmojiIds(text)
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val updated = communityEmojiCache.recordUsage(_authSession.value, ids)
            if (updated.isNotEmpty()) {
                _communityEmojis.value = updated
            }
        }
    }

    fun markAnimeDiscussionSeen(animeId: Int) {
        val session = _authSession.value ?: return
        if (animeId <= 0) return
        val readAtIso = Instant.now().toString()
        val unreadIdsForAnime = _notifications.value
            .filter { it.animeId == animeId && !it.isRead }
            .map { it.id }
        val hadUnseen = _unseenDiscussionAnime.value.any { it.animeId == animeId }
        val mutGen = if (unreadIdsForAnime.isNotEmpty()) {
            notificationSyncReducer.markLocalReadMany(unreadIdsForAnime)
            notificationSyncReducer.currentGeneration()
        } else {
            notificationSyncReducer.beginLocalMutation()
        }
        if (unreadIdsForAnime.isNotEmpty()) {
            _notifications.value = _notifications.value.map {
                if (it.animeId == animeId && !it.isRead) it.copy(isRead = true, readAt = readAtIso) else it
            }
        }
        if (hadUnseen) {
            val remainingList = _unseenDiscussionAnime.value.filterNot { it.animeId == animeId }
            _unseenDiscussionAnime.value = remainingList
            val optimisticCount = if (unreadIdsForAnime.isNotEmpty()) {
                notificationSyncReducer.unreadCount
            } else {
                remainingList.sumOf { it.unseenCount }
            }
            _unreadNotificationCount.value = optimisticCount
        } else if (unreadIdsForAnime.isNotEmpty()) {
            _unreadNotificationCount.value = notificationSyncReducer.unreadCount
        }
        viewModelScope.launch {
            val activeSession = supabaseAuth.refreshSessionIfNeeded(session).getOrDefault(session)
            supabaseDiscussion.markAnimeNotificationsRead(activeSession, animeId, readAtIso)
                .onSuccess { remaining ->
                    if (notificationSyncReducer.confirmServerUnreadCount(remaining, mutGen)) {
                        _unreadNotificationCount.value = notificationSyncReducer.unreadCount
                        if (remaining == 0) {
                            _unseenDiscussionAnime.value = emptyList()
                            _notifications.value = _notifications.value.map {
                                if (!it.isRead) it.copy(isRead = true, readAt = it.readAt ?: readAtIso) else it
                            }
                        } else {
                            _unseenDiscussionAnime.value = _unseenDiscussionAnime.value.filterNot { it.animeId == animeId }
                        }
                    }
                    reconcileUnreadNotificationCount(activeSession)
                }
                .onFailure {
                    reconcileUnreadNotificationCount(activeSession)
                }
        }
    }

    fun loadMoreAnimeDiscussion(animeId: Int) {
        if (_isDiscussionLoading.value || !_discussionHasMore.value) return
        loadAnimeDiscussion(animeId, reset = false)
    }

    fun searchMentionUsers(query: String) {
        val session = _authSession.value ?: return
        val selfId = _userProfile.value?.id ?: session.userId
        val clean = query.trim().lowercase()

        // Instant local matches from community members & reviews on 1st character
        val localCandidates = buildList {
            _communityMembers.value.forEach { m ->
                if (m.userId != selfId && m.username.isNotBlank() && !m.username.contains("@")) {
                    add(MentionableUser(id = m.userId, username = m.username, avatarKey = m.avatarKey, avatarUrl = m.avatarUrl))
                }
            }
            _communityReviews.value.forEach { r ->
                if (r.userId != selfId && r.username.isNotBlank() && !r.username.contains("@")) {
                    add(MentionableUser(id = r.userId, username = r.username, avatarKey = r.avatarKey, avatarUrl = r.avatarUrl))
                }
            }
        }.distinctBy { it.id }
            .filter { u ->
                clean.isEmpty() || u.username.lowercase().startsWith(clean) || u.username.lowercase().contains(clean)
            }
            .sortedWith(
                compareBy<MentionableUser> { !(clean.isNotEmpty() && it.username.lowercase().startsWith(clean)) }
                    .thenBy { it.username.lowercase() }
            )
            .take(10)

        if (localCandidates.isNotEmpty()) {
            _mentionSuggestions.value = localCandidates
        }

        mentionSearchJob?.cancel()
        mentionSearchJob = viewModelScope.launch {
            delay(110)
            supabaseDiscussion.searchMentionableUsers(session, query, limit = 12)
                .onSuccess { remote ->
                    val merged = (remote + localCandidates)
                        .distinctBy { it.id }
                        .filter { it.id != selfId }
                        .sortedWith(
                            compareBy<MentionableUser> { !(clean.isNotEmpty() && it.username.lowercase().startsWith(clean)) }
                                .thenBy { it.username.lowercase() }
                        )
                        .take(12)
                    _mentionSuggestions.value = merged
                }
        }
    }

    fun clearMentionSuggestions() {
        mentionSearchJob?.cancel()
        _mentionSuggestions.value = emptyList()
    }

    fun postAnimeComment(
        animeId: Int,
        animeTitle: String,
        animeCover: String,
        content: String,
        taggedUserIds: List<String> = emptyList(),
        replyTo: FlatReplyTo? = null,
        onSuccess: () -> Unit = {}
    ) {
        if (_isDiscussionSubmitting.value) return
        val session = _authSession.value
        if (session == null) {
            _discussionErrorMessage.value = "Sign in to post a message."
            return
        }
        val check = DiscussionRules.validateContentMentions(content, explicitTagCount = taggedUserIds.size)
        if (!check.isValid) {
            _discussionErrorMessage.value = check.errorMessage ?: "Invalid message."
            return
        }
        viewModelScope.launch {
            _isDiscussionSubmitting.value = true
            _discussionErrorMessage.value = null
            _discussionSuccessMessage.value = null
            supabaseDiscussion.createAnimeComment(
                session = session,
                animeId = animeId,
                content = content,
                animeTitle = animeTitle,
                animeCover = animeCover,
                taggedUserIds = taggedUserIds,
                replyTo = replyTo
            )
                .onSuccess { (created, total) ->
                    val hydrated = if (created.replyTo == null && replyTo != null) created.copy(replyTo = replyTo) else created
                    _discussionComments.value = listOf(hydrated) + _discussionComments.value.filterNot { it.id == hydrated.id }
                    _discussionTotalCount.value = maxOf(_discussionComments.value.size, total)
                    _discussionSuccessMessage.value = if (replyTo != null) "Reply sent" else "Message sent"
                    recordCustomEmojiUsage(content)
                    onSuccess()
                }
                .onFailure { err ->
                    _discussionErrorMessage.value = err.message ?: "Unable to post message right now."
                }
            _isDiscussionSubmitting.value = false
        }
    }

    fun editAnimeComment(
        commentId: String,
        newContent: String,
        taggedUserIds: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (_isDiscussionSubmitting.value) return
        val session = _authSession.value ?: return
        val check = DiscussionRules.validateContentMentions(newContent, explicitTagCount = taggedUserIds.size)
        if (!check.isValid) {
            _discussionErrorMessage.value = check.errorMessage ?: "Invalid message."
            return
        }
        val target = _discussionComments.value.firstOrNull { it.id == commentId }
        viewModelScope.launch {
            _isDiscussionSubmitting.value = true
            _discussionErrorMessage.value = null
            val res = if (target?.sourceType == "reply") {
                supabaseDiscussion.updateThreadReply(session, commentId, newContent, taggedUserIds)
            } else {
                supabaseDiscussion.updateAnimeComment(session, commentId, newContent, taggedUserIds)
            }
            res
                .onSuccess { (updatedText, updatedAt, mentions) ->
                    _discussionComments.value = _discussionComments.value.map { c ->
                        if (c.id == commentId) c.copy(content = updatedText, updatedAt = updatedAt, mentions = mentions) else c
                    }
                    _discussionSuccessMessage.value = "Message updated"
                    onSuccess()
                }
                .onFailure { err ->
                    _discussionErrorMessage.value = err.message ?: "Unable to update message."
                }
            _isDiscussionSubmitting.value = false
        }
    }

    fun deleteAnimeComment(commentId: String) {
        if (_isDiscussionSubmitting.value) return
        val session = _authSession.value ?: return
        val target = _discussionComments.value.firstOrNull { it.id == commentId }
        viewModelScope.launch {
            _isDiscussionSubmitting.value = true
            _discussionErrorMessage.value = null
            if (target?.sourceType == "reply") {
                supabaseDiscussion.deleteThreadReply(session, commentId)
                    .onSuccess {
                        _discussionComments.value = _discussionComments.value.filterNot { it.id == commentId }
                        _discussionTotalCount.value = (_discussionTotalCount.value - 1).coerceAtLeast(0)
                        _discussionSuccessMessage.value = "Message deleted"
                    }
                    .onFailure { err ->
                        _discussionErrorMessage.value = err.message ?: "Unable to delete message."
                    }
            } else {
                supabaseDiscussion.deleteAnimeComment(session, commentId)
                    .onSuccess { total ->
                        _discussionComments.value = _discussionComments.value.filterNot { it.id == commentId }
                        _discussionTotalCount.value = total
                        _discussionSuccessMessage.value = "Message deleted"
                    }
                    .onFailure { err ->
                        _discussionErrorMessage.value = err.message ?: "Unable to delete message."
                    }
            }
            _isDiscussionSubmitting.value = false
        }
    }

    fun startRealtimeNotificationPolling() {
        if (notificationPollJob?.isActive == true) return
        notificationPollJob = viewModelScope.launch {
            while (isActive) {
                delay(20_000L)
                val session = _authSession.value
                if (session != null && AppConfig.isSupabaseConfigured) {
                    ensureNotificationRealtimeConnected(session)
                    reconcileUnreadNotificationCount(session)
                }
            }
        }
    }

    fun postThreadReply(
        commentId: String,
        targetUserId: String,
        content: String,
        taggedUserIds: List<String> = emptyList(),
        quotedReplyId: String? = null,
        quotedUsername: String? = null,
        quotedPreview: String? = null,
        onSuccess: (threadId: String) -> Unit = {}
    ) {
        if (_isDiscussionSubmitting.value) return
        val session = _authSession.value
        if (session == null) {
            _discussionErrorMessage.value = "Sign in to post a reply."
            return
        }
        val check = DiscussionRules.validateContentMentions(content, explicitTagCount = taggedUserIds.size)
        if (!check.isValid) {
            _discussionErrorMessage.value = check.errorMessage ?: "Invalid reply."
            return
        }
        viewModelScope.launch {
            _isDiscussionSubmitting.value = true
            _discussionErrorMessage.value = null
            _discussionSuccessMessage.value = null
            supabaseDiscussion.createThreadReply(
                session = session,
                commentId = commentId,
                targetUserId = targetUserId,
                content = content,
                taggedUserIds = taggedUserIds,
                quotedReplyId = quotedReplyId,
                quotedUsername = quotedUsername,
                quotedPreview = quotedPreview
            )
                .onSuccess { created ->
                    val mergedReply = created.reply.copy(
                        parentCommentId = created.reply.parentCommentId
                            ?: created.reply.quotedReplyId
                            ?: quotedReplyId
                            ?: commentId,
                        quotedCommentId = created.reply.quotedCommentId ?: commentId,
                        quotedReplyId = created.reply.quotedReplyId ?: quotedReplyId,
                        quotedUsername = created.reply.quotedUsername?.takeIf { it.isNotBlank() } ?: quotedUsername,
                        quotedPreview = created.reply.quotedPreview?.takeIf { it.isNotBlank() } ?: quotedPreview
                    )
                    _discussionComments.value = _discussionComments.value.map { comment ->
                        if (comment.id == commentId) {
                            DiscussionRules.mergeReplyIntoComment(comment, created.thread, mergedReply)
                        } else {
                            comment
                        }
                    }
                    _discussionSuccessMessage.value = "Reply sent"
                    recordCustomEmojiUsage(content)
                    onSuccess(created.thread.id)
                }
                .onFailure { err ->
                    _discussionErrorMessage.value = err.message ?: "Unable to post reply right now."
                }
            _isDiscussionSubmitting.value = false
        }
    }

    fun editThreadReply(
        commentId: String,
        threadId: String,
        replyId: String,
        newContent: String,
        taggedUserIds: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (_isDiscussionSubmitting.value) return
        val session = _authSession.value ?: return
        val check = DiscussionRules.validateContentMentions(newContent, explicitTagCount = taggedUserIds.size)
        if (!check.isValid) {
            _discussionErrorMessage.value = check.errorMessage ?: "Invalid reply."
            return
        }
        viewModelScope.launch {
            _isDiscussionSubmitting.value = true
            _discussionErrorMessage.value = null
            supabaseDiscussion.updateThreadReply(session, replyId, newContent, taggedUserIds)
                .onSuccess { (updatedText, updatedAt, mentions) ->
                    _discussionComments.value = _discussionComments.value.map { comment ->
                        if (comment.id != commentId) comment
                        else comment.copy(
                            threads = comment.threads.map { thread ->
                                if (thread.id != threadId) thread
                                else thread.copy(
                                    replies = thread.replies.map { r ->
                                        if (r.id == replyId) r.copy(content = updatedText, updatedAt = updatedAt, mentions = mentions) else r
                                    }
                                )
                            }
                        )
                    }
                    _discussionSuccessMessage.value = "Reply updated successfully"
                    onSuccess()
                }
                .onFailure { err ->
                    _discussionErrorMessage.value = err.message ?: "Unable to update reply."
                }
            _isDiscussionSubmitting.value = false
        }
    }

    fun deleteThreadReply(commentId: String, threadId: String, replyId: String) {
        if (_isDiscussionSubmitting.value) return
        val session = _authSession.value ?: return
        viewModelScope.launch {
            _isDiscussionSubmitting.value = true
            _discussionErrorMessage.value = null
            supabaseDiscussion.deleteThreadReply(session, replyId)
                .onSuccess { (_, threadRemoved) ->
                    _discussionComments.value = _discussionComments.value.map { comment ->
                        if (comment.id != commentId) comment
                        else {
                            val nextThreads = comment.threads.mapNotNull { thread ->
                                if (thread.id != threadId) thread
                                else {
                                    val remaining = thread.replies.filterNot { it.id == replyId }
                                    if (threadRemoved || remaining.isEmpty()) null else thread.copy(replies = remaining)
                                }
                            }
                            comment.copy(threads = nextThreads)
                        }
                    }
                    _discussionSuccessMessage.value = "Reply deleted"
                }
                .onFailure { err ->
                    _discussionErrorMessage.value = err.message ?: "Unable to delete reply."
                }
            _isDiscussionSubmitting.value = false
        }
    }

    fun refreshNotifications(reset: Boolean = true) {
        val session = _authSession.value ?: return
        val startGen = notificationSyncReducer.currentGeneration()
        viewModelScope.launch {
            _isNotificationsLoading.value = true
            _notificationsError.value = null
            val activeSession = supabaseAuth.refreshSessionIfNeeded(session).getOrDefault(session)
            val offset = if (reset) 0 else _notifications.value.size
            supabaseDiscussion.fetchNotifications(activeSession, limit = 30, offset = offset)
                .onSuccess { page ->
                    _hasMoreNotifications.value = page.hasMore
                    val merged = if (reset) {
                        page.notifications
                    } else {
                        val existing = _notifications.value.map { it.id }.toSet()
                        _notifications.value + page.notifications.filterNot { it.id in existing }
                    }
                    _notifications.value = merged
                    if (notificationSyncReducer.applyFetchedPage(
                            items = page.notifications.map { it.id to it.isRead },
                            authoritativeUnreadCount = page.unreadCount,
                            fetchStartedGeneration = startGen
                        )
                    ) {
                        _unreadNotificationCount.value = notificationSyncReducer.unreadCount
                        if (notificationSyncReducer.unreadCount == 0) {
                            _unseenDiscussionAnime.value = emptyList()
                        }
                    }
                }
                .onFailure { err ->
                    _notificationsError.value = err.message
                }
            _isNotificationsLoading.value = false
        }
    }

    fun loadMoreNotifications() {
        if (_isNotificationsLoading.value || !_hasMoreNotifications.value) return
        refreshNotifications(reset = false)
    }

    fun markNotificationRead(notificationId: String) {
        val session = _authSession.value ?: return
        if (notificationId.isBlank()) return
        val readAtIso = Instant.now().toString()
        val target = _notifications.value.firstOrNull { it.id == notificationId }
        val nextCount = notificationSyncReducer.markLocalRead(notificationId)
        val mutGen = notificationSyncReducer.currentGeneration()

        val nextNotifications = _notifications.value.map {
            if (it.id == notificationId && !it.isRead) it.copy(isRead = true, readAt = readAtIso) else it
        }
        _notifications.value = nextNotifications
        _unreadNotificationCount.value = nextCount
        if (nextCount == 0) {
            _unseenDiscussionAnime.value = emptyList()
        } else if (target != null && nextNotifications.none { it.animeId == target.animeId && !it.isRead }) {
            _unseenDiscussionAnime.value = _unseenDiscussionAnime.value.filterNot { it.animeId == target.animeId }
        }

        viewModelScope.launch {
            val activeSession = supabaseAuth.refreshSessionIfNeeded(session).getOrDefault(session)
            supabaseDiscussion.markNotificationRead(activeSession, notificationId, readAtIso)
                .onSuccess { unread ->
                    if (notificationSyncReducer.confirmServerUnreadCount(unread, mutGen)) {
                        _unreadNotificationCount.value = notificationSyncReducer.unreadCount
                        if (unread == 0) {
                            _unseenDiscussionAnime.value = emptyList()
                            _notifications.value = _notifications.value.map {
                                if (!it.isRead) it.copy(isRead = true, readAt = it.readAt ?: readAtIso) else it
                            }
                        } else {
                            reconcileUnreadNotificationCount(activeSession)
                        }
                    }
                }
                .onFailure {
                    reconcileUnreadNotificationCount(activeSession)
                }
        }
    }

    fun markAllNotificationsRead() {
        val session = _authSession.value ?: return
        val readAtIso = Instant.now().toString()
        notificationSyncReducer.markAllLocalRead()
        val mutGen = notificationSyncReducer.currentGeneration()
        _notifications.value = _notifications.value.map {
            if (!it.isRead) it.copy(isRead = true, readAt = readAtIso) else it
        }
        _unseenDiscussionAnime.value = emptyList()
        _unreadNotificationCount.value = 0
        viewModelScope.launch {
            val activeSession = supabaseAuth.refreshSessionIfNeeded(session).getOrDefault(session)
            supabaseDiscussion.markAllNotificationsRead(activeSession, readAtIso)
                .onSuccess { unread ->
                    if (notificationSyncReducer.confirmServerUnreadCount(unread, mutGen)) {
                        _unreadNotificationCount.value = notificationSyncReducer.unreadCount
                        if (notificationSyncReducer.unreadCount == 0) {
                            _unseenDiscussionAnime.value = emptyList()
                        }
                    }
                }
                .onFailure {
                    reconcileUnreadNotificationCount(activeSession)
                }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: throw IllegalStateException("Unable to read selected file.")
                val res = repository.importBackupJson(json)
                _userMessage.value = res.message
            } catch (e: Exception) {
                _userMessage.value = "Import failed: ${ErrorSanitizer.sanitize(e, "Unable to read backup file.")}"
            }
        }
    }

    override fun onCleared() {
        stopAnimeChatRealtime()
        stopNotificationRealtime()
        super.onCleared()
    }
}
