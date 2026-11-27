package com.example.service

import com.example.data.local.AnimeEntity
import com.example.data.remote.AniListAiringSchedule
import com.example.data.remote.AniListApiService
import com.example.data.remote.AniListMedia
import com.example.data.remote.AniListQueries
import com.example.data.remote.AniListRequest
import com.example.data.remote.AnimeMetadataProvider
import com.example.model.AnimeCharacterItem
import com.example.model.AnimeRecommendationItem
import com.example.model.AnimeRelationItem
import com.example.model.AnimeRichDetail
import com.example.model.AnimeSeasonEntry
import com.example.model.DailyAnimeFormatters
import com.example.model.TopAnimeRankedItem
import com.example.model.TopAnimeTimeframe
import com.example.util.EnglishTitleMapper
import com.example.util.EpisodeDataParser
import com.example.util.ErrorSanitizer
import com.example.data.remote.AniListCoverImage
import com.example.data.remote.AniListFuzzyDate
import com.example.data.remote.AniListTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Response
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AniListMetadataProvider(
    private val apiService: AniListApiService = AniListApiService.create()
) : AnimeMetadataProvider {

    private data class CachedSearchPage(
        val timestampMs: Long,
        val media: List<AniListMedia>,
        val hasNext: Boolean
    )

    private data class CachedTopRanking(
        val timestampMs: Long,
        val items: List<TopAnimeRankedItem>
    )

    private val seasonNodeCache = ConcurrentHashMap<Int, AniListMedia>()
    private val searchCache = ConcurrentHashMap<String, CachedSearchPage>()
    private val topRankingCache = ConcurrentHashMap<String, CachedTopRanking>()
    private val fallbackHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun <T> executeWithRetry(
        maxAttempts: Int = 3,
        call: suspend () -> Response<T>
    ): Response<T>? {
        var lastResponse: Response<T>? = null
        for (attempt in 0 until maxAttempts) {
            try {
                val resp = call()
                lastResponse = resp
                if (resp.isSuccessful) return resp
                if (resp.code() == 429 || resp.code() in 500..599) {
                    if (attempt < maxAttempts - 1) {
                        val retryAfterSec = resp.headers()["Retry-After"]?.toLongOrNull()
                        val waitMs = if (retryAfterSec != null && retryAfterSec in 1..4) {
                            retryAfterSec * 1000L
                        } else {
                            800L * (attempt + 1)
                        }
                        delay(waitMs)
                        continue
                    }
                }
                return resp
            } catch (_: Exception) {
                if (attempt < maxAttempts - 1) {
                    delay(650L * (attempt + 1))
                }
            }
        }
        return lastResponse
    }

    private fun fetchJikanFallback(
        query: String,
        format: String?,
        status: String?,
        page: Int,
        perPage: Int
    ): Pair<List<AniListMedia>, Boolean>? {
        return runCatching {
            val encodedQ = URLEncoder.encode(query, "UTF-8")
            val limit = perPage.coerceIn(1, 25)
            val sb = StringBuilder("https://api.jikan.moe/v4/anime?q=$encodedQ&page=$page&limit=$limit&sfw=true")
            when (format?.uppercase()) {
                "TV" -> sb.append("&type=tv")
                "MOVIE" -> sb.append("&type=movie")
                "OVA" -> sb.append("&type=ova")
                "ONA" -> sb.append("&type=ona")
                "SPECIAL" -> sb.append("&type=special")
            }
            when (status?.uppercase()) {
                "RELEASING" -> sb.append("&status=airing")
                "FINISHED" -> sb.append("&status=complete")
                "NOT_YET_RELEASED" -> sb.append("&status=upcoming")
            }
            val req = Request.Builder()
                .url(sb.toString())
                .header("Accept", "application/json")
                .build()
            fallbackHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val body = resp.body?.string().orEmpty()
                val root = JSONObject(body)
                val dataArr = root.optJSONArray("data") ?: return@runCatching null
                val hasNext = root.optJSONObject("pagination")?.optBoolean("has_next_page", false) ?: false
                val items = ArrayList<AniListMedia>(dataArr.length())
                for (i in 0 until dataArr.length()) {
                    val obj = dataArr.optJSONObject(i) ?: continue
                    val malId = obj.optInt("mal_id", 0)
                    if (malId <= 0) continue
                    val titleDefault = obj.optString("title").takeIf { it.isNotBlank() }
                    val titleEng = obj.optString("title_english").takeIf { it.isNotBlank() }
                    val titleJp = obj.optString("title_japanese").takeIf { it.isNotBlank() }
                    val synArr = obj.optJSONArray("title_synonyms")
                    val synonyms = mutableListOf<String>()
                    if (synArr != null) {
                        for (j in 0 until synArr.length()) {
                            synArr.optString(j).takeIf { it.isNotBlank() }?.let { synonyms.add(it) }
                        }
                    }
                    val imgJpg = obj.optJSONObject("images")?.optJSONObject("jpg")
                    val cover = imgJpg?.optString("large_image_url")?.takeIf { it.isNotBlank() }
                        ?: imgJpg?.optString("image_url").orEmpty()
                    val jikanType = obj.optString("type").uppercase()
                    val mappedFormat = when (jikanType) {
                        "TV" -> "TV"
                        "MOVIE" -> "MOVIE"
                        "OVA" -> "OVA"
                        "ONA" -> "ONA"
                        "SPECIAL", "TV SPECIAL" -> "SPECIAL"
                        else -> "TV"
                    }
                    val jikanStatus = obj.optString("status").lowercase()
                    val mappedStatus = when {
                        jikanStatus.contains("currently airing") -> "RELEASING"
                        jikanStatus.contains("not yet") -> "NOT_YET_RELEASED"
                        else -> "FINISHED"
                    }
                    val eps = obj.optInt("episodes", -1).takeIf { it > 0 }
                    val year = obj.optInt("year", -1).takeIf { it > 0 }
                    val scoreFloat = obj.optDouble("score", -1.0)
                    val score100 = if (scoreFloat > 0.0) (scoreFloat * 10.0).toInt().coerceIn(1, 100) else null
                    val genresArr = obj.optJSONArray("genres")
                    val genres = mutableListOf<String>()
                    if (genresArr != null) {
                        for (g in 0 until genresArr.length()) {
                            genresArr.optJSONObject(g)?.optString("name")?.takeIf { it.isNotBlank() }?.let { genres.add(it) }
                        }
                    }
                    items.add(
                        AniListMedia(
                            id = malId,
                            type = "ANIME",
                            title = AniListTitle(
                                romaji = titleDefault,
                                english = titleEng,
                                native = titleJp,
                                userPreferred = titleEng ?: titleDefault
                            ),
                            coverImage = AniListCoverImage(large = cover, extraLarge = cover),
                            format = mappedFormat,
                            status = mappedStatus,
                            episodes = eps,
                            genres = genres,
                            averageScore = score100,
                            popularity = obj.optInt("members", 0),
                            seasonYear = year,
                            description = obj.optString("synopsis").takeIf { it.isNotBlank() },
                            countryOfOrigin = if (mappedFormat == "ONA" && EnglishTitleMapper.isLikelyPinyin(titleDefault.orEmpty())) "CN" else "JP",
                            synonyms = synonyms,
                            startDate = year?.let { AniListFuzzyDate(year = it) }
                        )
                    )
                }
                items to hasNext
            }
        }.getOrNull()
    }

    override suspend fun searchCatalog(
        query: String?,
        genre: String?,
        season: String?,
        seasonYear: Int?,
        status: String?,
        format: String?,
        countryOfOrigin: String?,
        sort: String,
        page: Int,
        perPage: Int
    ): Result<Pair<List<AniListMedia>, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query?.trim()?.takeIf { it.isNotBlank() }
            val cleanGenre = genre?.takeIf { it.isNotBlank() && it != "ALL" }
            val cleanSeason = season?.takeIf { it.isNotBlank() && it != "ALL" }
            val cleanStatus = status?.takeIf { it.isNotBlank() && it != "ALL" }
            val cleanFormat = format?.takeIf { it.isNotBlank() && it != "ALL" }
            val cleanCountry = countryOfOrigin?.takeIf { it.isNotBlank() && it != "ALL" }

            val cacheKey = listOf(
                cleanQuery?.lowercase().orEmpty(),
                cleanGenre.orEmpty(),
                cleanSeason.orEmpty(),
                seasonYear?.toString().orEmpty(),
                cleanStatus.orEmpty(),
                cleanFormat.orEmpty(),
                cleanCountry.orEmpty(),
                sort,
                page.toString(),
                perPage.toString()
            ).joinToString("|")

            val cached = searchCache[cacheKey]
            if (cached != null && (System.currentTimeMillis() - cached.timestampMs) < 60_000L) {
                return@withContext Result.success(cached.media to cached.hasNext)
            }

            val effectiveSort = if (cleanQuery != null) {
                listOf("SEARCH_MATCH", sort, "POPULARITY_DESC").distinct()
            } else {
                listOf(sort)
            }

            val extraTerms = if (cleanQuery != null && page == 1 && cleanQuery.length >= 2) {
                EnglishTitleMapper.expandSearchQueries(
                    rawQuery = cleanQuery,
                    donghuaOnly = cleanCountry.equals("CN", ignoreCase = true),
                    maxExtra = 2
                )
            } else {
                emptyList()
            }

            var mergedMedia: List<AniListMedia>? = null
            var hasNext = false

            if (cleanQuery != null && extraTerms.isNotEmpty()) {
                // Execute primary + dictionary aliases in ONE single AniList HTTP request!
                val batchedQuery = AniListQueries.buildBatchedSearchQuery(
                    aliasCount = extraTerms.size,
                    hasGenre = cleanGenre != null,
                    hasSeason = cleanSeason != null,
                    hasSeasonYear = seasonYear != null,
                    hasStatus = cleanStatus != null,
                    hasFormat = cleanFormat != null,
                    hasCountryOfOrigin = cleanCountry != null
                )
                val variables = mutableMapOf<String, Any?>(
                    "page" to page,
                    "perPage" to perPage,
                    "sort" to effectiveSort,
                    "search" to cleanQuery
                )
                if (cleanGenre != null) variables["genre"] = cleanGenre
                if (cleanSeason != null) variables["season"] = cleanSeason
                if (seasonYear != null) variables["seasonYear"] = seasonYear
                if (cleanStatus != null) variables["status"] = cleanStatus
                if (cleanFormat != null) variables["format"] = cleanFormat
                if (cleanCountry != null) variables["countryOfOrigin"] = cleanCountry
                extraTerms.forEachIndexed { idx, term -> variables["alias$idx"] = term }

                val response = executeWithRetry {
                    apiService.queryMultiPageMedia(AniListRequest(query = batchedQuery, variables = variables))
                }
                if (response?.isSuccessful == true) {
                    val data = response.body()?.data
                    val primaryList = data?.primary?.media.orEmpty()
                    val alias0List = data?.alias0?.media.orEmpty()
                    val alias1List = data?.alias1?.media.orEmpty()
                    hasNext = (data?.primary?.pageInfo?.hasNextPage ?: false) && primaryList.isNotEmpty()
                    mergedMedia = (primaryList + alias0List + alias1List).distinctBy { it.id }
                }
            } else {
                val gqlQuery = AniListQueries.buildMediaQuery(
                    hasSearch = cleanQuery != null,
                    hasGenre = cleanGenre != null,
                    hasSeason = cleanSeason != null,
                    hasSeasonYear = seasonYear != null,
                    hasStatus = cleanStatus != null,
                    hasFormat = cleanFormat != null,
                    hasCountryOfOrigin = cleanCountry != null
                )
                val variables = mutableMapOf<String, Any?>(
                    "page" to page,
                    "perPage" to perPage,
                    "sort" to effectiveSort
                )
                if (cleanQuery != null) variables["search"] = cleanQuery
                if (cleanGenre != null) variables["genre"] = cleanGenre
                if (cleanSeason != null) variables["season"] = cleanSeason
                if (seasonYear != null) variables["seasonYear"] = seasonYear
                if (cleanStatus != null) variables["status"] = cleanStatus
                if (cleanFormat != null) variables["format"] = cleanFormat
                if (cleanCountry != null) variables["countryOfOrigin"] = cleanCountry

                val response = executeWithRetry {
                    apiService.queryMedia(AniListRequest(query = gqlQuery, variables = variables))
                }
                if (response?.isSuccessful == true) {
                    val pageData = response.body()?.data?.Page
                    mergedMedia = pageData?.media.orEmpty()
                    hasNext = (pageData?.pageInfo?.hasNextPage ?: false) && mergedMedia.isNotEmpty()
                }
            }

            // Automatic Jikan v4 fallback if AniList rate-limited (429) or failed during a search
            if (mergedMedia == null && cleanQuery != null) {
                val jikan = fetchJikanFallback(cleanQuery, cleanFormat, cleanStatus, page, perPage)
                if (jikan != null) {
                    mergedMedia = jikan.first
                    hasNext = jikan.second
                }
            }

            if (mergedMedia == null) {
                return@withContext Result.failure(Exception("Catalog is temporarily rate-limited. Tap Retry to search again."))
            }

            var finalList = mergedMedia
            if (cleanQuery != null && page == 1) {
                finalList = finalList.sortedWith(
                    compareByDescending<AniListMedia> { media ->
                        val isDonghua = media.countryOfOrigin.equals("CN", ignoreCase = true) ||
                            media.genres.orEmpty().any { it.contains("Donghua", ignoreCase = true) }
                        val resolved = EnglishTitleMapper.resolve(
                            titleEnglish = media.title?.english,
                            titleRomaji = media.title?.romaji,
                            synonyms = media.synonyms,
                            isDonghua = isDonghua
                        )
                        EnglishTitleMapper.scoreKeywordMatch(
                            rawQuery = cleanQuery,
                            resolvedEnglish = resolved,
                            officialEnglish = media.title?.english,
                            romaji = media.title?.romaji,
                            synonyms = media.synonyms
                        )
                    }.thenByDescending { it.popularity ?: 0 }
                )
            }

            if (searchCache.size > 80) searchCache.clear()
            searchCache[cacheKey] = CachedSearchPage(System.currentTimeMillis(), finalList, hasNext)

            Result.success(finalList to hasNext)
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to reach catalog right now.")))
        }
    }

    override suspend fun getTrendingAnime(page: Int, perPage: Int): Result<List<AniListMedia>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.queryMedia(
                AniListRequest(
                    query = AniListQueries.TRENDING_MEDIA_QUERY,
                    variables = mapOf("page" to page, "perPage" to perPage)
                )
            )
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Unable to load trending titles right now."))
            }
            Result.success(response.body()?.data?.Page?.media.orEmpty())
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to reach catalog right now.")))
        }
    }

    override suspend fun getPopularReleasing(page: Int, perPage: Int): Result<List<AniListMedia>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.queryMedia(
                AniListRequest(
                    query = AniListQueries.POPULAR_RELEASING_MEDIA_QUERY,
                    variables = mapOf("page" to page, "perPage" to perPage)
                )
            )
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Unable to load currently airing titles right now."))
            }
            Result.success(response.body()?.data?.Page?.media.orEmpty())
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to reach catalog right now.")))
        }
    }

    override suspend fun getAiringSchedule(
        startUnix: Int,
        endUnix: Int,
        page: Int
    ): Result<List<AniListAiringSchedule>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.querySchedule(
                AniListRequest(
                    query = AniListQueries.AIRING_SCHEDULE_QUERY,
                    variables = mapOf(
                        "airingAt_greater" to startUnix,
                        "airingAt_lesser" to endUnix,
                        "page" to page
                    )
                )
            )
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Unable to load weekly schedule right now."))
            }
            Result.success(response.body()?.data?.Page?.airingSchedules.orEmpty())
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to load weekly schedule right now.")))
        }
    }

    override suspend fun getMediaDetails(anilistId: Int): Result<AniListMedia?> = withContext(Dispatchers.IO) {
        try {
            val response = executeWithRetry {
                apiService.querySingleMedia(
                    AniListRequest(
                        query = AniListQueries.MEDIA_DETAILS_QUERY,
                        variables = mapOf("id" to anilistId)
                    )
                )
            }
            if (response?.isSuccessful != true) {
                return@withContext Result.failure(Exception("Unable to load series details right now."))
            }
            val media = response.body()?.data?.Media
            if (media != null) {
                seasonNodeCache[media.id] = media
            }
            Result.success(media)
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to load series details right now.")))
        }
    }

    private suspend fun fetchChainBatch(anilistIds: List<Int>): List<AniListMedia> {
        if (anilistIds.isEmpty()) return emptyList()
        val cached = mutableListOf<AniListMedia>()
        val missing = mutableListOf<Int>()
        for (id in anilistIds.distinct()) {
            val hit = seasonNodeCache[id]
            if (hit != null) cached.add(hit) else missing.add(id)
        }
        if (missing.isEmpty()) return cached

        val fetched = mutableListOf<AniListMedia>()
        var attempt = 0
        while (attempt < 2) {
            attempt++
            try {
                val response = apiService.queryMedia(
                    AniListRequest(
                        query = AniListQueries.MEDIA_SEASON_BATCH_QUERY,
                        variables = mapOf("ids" to missing)
                    )
                )
                if (response.isSuccessful) {
                    val nodes = response.body()?.data?.Page?.media.orEmpty()
                    for (node in nodes) {
                        seasonNodeCache[node.id] = node
                        fetched.add(node)
                    }
                    break
                } else if (response.code() == 429 && attempt < 2) {
                    delay(700L)
                } else {
                    break
                }
            } catch (_: Exception) {
                if (attempt < 2) delay(450L)
            }
        }
        // Fallback to single-media lookup if the batch endpoint failed or threw
        if (fetched.isEmpty() && missing.size <= 10) {
            for (id in missing) {
                try {
                    val singleResp = apiService.querySingleMedia(
                        AniListRequest(
                            query = AniListQueries.MEDIA_DETAILS_QUERY,
                            variables = mapOf("id" to id)
                        )
                    )
                    val node = singleResp.body()?.data?.Media
                    if (node != null && node.id > 0) {
                        seasonNodeCache[node.id] = node
                        fetched.add(node)
                    }
                } catch (_: Exception) {
                }
            }
        }
        return cached + fetched
    }

    override fun getCachedSeasonMedia(anilistId: Int): AniListMedia? = seasonNodeCache[anilistId]

    override suspend fun getRichDetailWithSeasons(
        rootMedia: AniListMedia,
        overrideMode: com.example.model.FranchiseSeasonOverrideMode
    ): AnimeRichDetail = withContext(Dispatchers.IO) {
        seasonNodeCache[rootMedia.id] = rootMedia
        val base = mapRichDetail(rootMedia)

        val hasImmediateLink = rootMedia.relations?.edges.orEmpty().any { edge ->
            val rel = edge.relationType?.uppercase()?.trim().orEmpty()
            val node = edge.node ?: return@any false
            rel in setOf("PREQUEL", "SEQUEL", "PARENT", "SIDE_STORY", "SPIN_OFF", "SUMMARY", "ALTERNATIVE") &&
                isAnimeMediaType(node.type)
        }
        if (!hasImmediateLink) {
            val isSelfMovieOvaSpecial = rootMedia.format?.uppercase()?.trim() in setOf("MOVIE", "OVA", "SPECIAL")
            val selfMoviesAndOvas = if (isSelfMovieOvaSpecial) {
                val fmt = rootMedia.format?.uppercase()?.trim() ?: "SPECIAL"
                listOf(
                    com.example.model.AnimeMovieOvaEntry(
                        id = rootMedia.id,
                        title = base.relations.firstOrNull { it.id == rootMedia.id }?.title
                            ?: rootMedia.title?.english
                            ?: rootMedia.title?.userPreferred
                            ?: rootMedia.title?.romaji
                            ?: "Special",
                        coverImage = rootMedia.coverImage?.extraLarge ?: rootMedia.coverImage?.large.orEmpty(),
                        formatBadge = if (fmt == "MOVIE" || fmt == "OVA") fmt else "SPECIAL",
                        status = rootMedia.status,
                        episodes = rootMedia.episodes?.takeIf { it > 0 } ?: 1,
                        releaseYear = rootMedia.startDate?.year ?: rootMedia.seasonYear,
                        isCurrent = true
                    )
                )
            } else {
                emptyList()
            }
            return@withContext base.copy(
                seasons = emptyList(),
                moviesAndOvas = selfMoviesAndOvas,
                franchiseId = rootMedia.id,
                rawMainSeasonNodeCount = if (isSelfMovieOvaSpecial) 0 else 1
            )
        }

        val resolved = SeasonChainResolver.resolve(
            rootMedia = rootMedia,
            overrideMode = overrideMode,
            fetchBatch = { batchIds -> fetchChainBatch(batchIds) }
        )
        val mergedRelations = (base.relations + resolved.bridgeRelations).distinctBy { it.id }

        base.copy(
            relations = mergedRelations,
            seasons = resolved.seasons,
            moviesAndOvas = resolved.moviesAndOvas,
            franchiseId = resolved.franchiseId,
            rawMainSeasonNodeCount = resolved.rawMainSeasonNodeCount
        )
    }

    suspend fun fetchTopAnimeRankings(
        timeframe: TopAnimeTimeframe,
        limit: Int,
        forceRefresh: Boolean = false
    ): Result<List<TopAnimeRankedItem>> = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(10, 100)
        val cacheKey = "${timeframe.key}|$safeLimit"
        if (!forceRefresh) {
            val cached = topRankingCache[cacheKey]
            if (cached != null && (System.currentTimeMillis() - cached.timestampMs) < 300_000L) {
                return@withContext Result.success(cached.items)
            }
        }

        try {
            val now = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
            val currentYear = now.year
            val currentSeason = when (now.monthValue) {
                in 1..3 -> "WINTER"
                in 4..6 -> "SPRING"
                in 7..9 -> "SUMMER"
                else -> "FALL"
            }
            val sixMonthsAgo = now.minusMonths(6)
            val sixMonthsFuzzy = sixMonthsAgo.year * 10000 + sixMonthsAgo.monthValue * 100 + sixMonthsAgo.dayOfMonth

            val hasSeason = timeframe == TopAnimeTimeframe.SEASONAL
            val hasSeasonYear = timeframe == TopAnimeTimeframe.SEASONAL || timeframe == TopAnimeTimeframe.YEARLY
            val hasStartDateGreater = timeframe == TopAnimeTimeframe.MONTHLY
            val gqlQuery = AniListQueries.buildTopRankingsQuery(
                hasSeason = hasSeason,
                hasSeasonYear = hasSeasonYear,
                hasStartDateGreater = hasStartDateGreater
            )

            suspend fun fetchPage(page: Int, perPage: Int): List<AniListMedia> {
                val vars = mutableMapOf<String, Any?>(
                    "page" to page,
                    "perPage" to perPage
                )
                when (timeframe) {
                    TopAnimeTimeframe.WEEKLY -> {
                        vars["sort"] = listOf("TRENDING_DESC", "SCORE_DESC")
                    }
                    TopAnimeTimeframe.MONTHLY -> {
                        vars["sort"] = listOf("POPULARITY_DESC", "SCORE_DESC")
                        vars["startDateGreater"] = sixMonthsFuzzy
                    }
                    TopAnimeTimeframe.SEASONAL -> {
                        vars["sort"] = listOf("SCORE_DESC", "POPULARITY_DESC")
                        vars["season"] = currentSeason
                        vars["seasonYear"] = currentYear
                    }
                    TopAnimeTimeframe.YEARLY -> {
                        vars["sort"] = listOf("SCORE_DESC", "POPULARITY_DESC")
                        vars["seasonYear"] = currentYear
                    }
                    TopAnimeTimeframe.ALL_TIME -> {
                        vars["sort"] = listOf("SCORE_DESC", "POPULARITY_DESC")
                    }
                }
                val resp = executeWithRetry {
                    apiService.queryMedia(
                        AniListRequest(
                            query = gqlQuery,
                            variables = vars
                        )
                    )
                }
                if (resp == null || !resp.isSuccessful) {
                    throw Exception("Unable to load rankings right now.")
                }
                return resp.body()?.data?.Page?.media.orEmpty()
            }

            val rawMedia = try {
                if (safeLimit <= 50) {
                    fetchPage(1, safeLimit)
                } else {
                    val p1 = fetchPage(1, 50)
                    val p2 = fetchPage(2, 50)
                    p1 + p2
                }
            } catch (_: Exception) {
                fetchJikanTopFallback(timeframe, safeLimit)
            }

            if (rawMedia.isEmpty()) {
                val fallback = fetchJikanTopFallback(timeframe, safeLimit)
                if (fallback.isNotEmpty()) {
                    val mappedFallback = mapRankedItems(fallback, safeLimit)
                    topRankingCache[cacheKey] = CachedTopRanking(System.currentTimeMillis(), mappedFallback)
                    return@withContext Result.success(mappedFallback)
                }
            }

            val ranked = mapRankedItems(rawMedia, safeLimit)
            topRankingCache[cacheKey] = CachedTopRanking(System.currentTimeMillis(), ranked)
            Result.success(ranked)
        } catch (e: Exception) {
            Result.failure(Exception("Could not load rankings right now. Please try again."))
        }
    }

    private fun mapRankedItems(rawMedia: List<AniListMedia>, safeLimit: Int): List<TopAnimeRankedItem> {
        val seen = HashSet<Int>()
        val ranked = ArrayList<TopAnimeRankedItem>(safeLimit)
        for (media in rawMedia) {
            if (media.id <= 0 || !seen.add(media.id)) continue
            val isDonghua = media.countryOfOrigin.equals("CN", ignoreCase = true)
            val resolvedTitle = EnglishTitleMapper.resolve(
                titleEnglish = media.title?.english,
                titleRomaji = media.title?.romaji ?: media.title?.userPreferred,
                synonyms = media.synonyms,
                isDonghua = isDonghua
            )?.takeIf { it.isNotBlank() }
                ?: media.title?.english?.takeIf { it.isNotBlank() }
                ?: media.title?.userPreferred?.takeIf { it.isNotBlank() }
                ?: media.title?.romaji?.takeIf { it.isNotBlank() }
                ?: "Anime #${media.id}"
            val avgScore = media.averageScore
            val scoreDisplay = if (avgScore != null && avgScore > 0) {
                "%.1f".format(java.util.Locale.US, avgScore / 10.0)
            } else {
                "—"
            }
            ranked.add(
                TopAnimeRankedItem(
                    animeId = media.id,
                    title = resolvedTitle,
                    coverImage = media.coverImage?.extraLarge ?: media.coverImage?.large.orEmpty(),
                    scoreDisplay = scoreDisplay,
                    voteCount = null,
                    formatLabel = media.format?.let { DailyAnimeFormatters.formatFormat(it) }.orEmpty(),
                    releaseYear = media.seasonYear ?: media.startDate?.year,
                    episodes = media.episodes,
                    genres = media.genres.orEmpty().take(3)
                )
            )
            if (ranked.size >= safeLimit) break
        }
        return ranked
    }

    private fun fetchJikanTopFallback(timeframe: TopAnimeTimeframe, limit: Int): List<AniListMedia> {
        return runCatching {
            val filter = when (timeframe) {
                TopAnimeTimeframe.WEEKLY, TopAnimeTimeframe.MONTHLY, TopAnimeTimeframe.SEASONAL -> "&filter=airing"
                else -> ""
            }
            val perPage = 25
            val pages = ((limit + perPage - 1) / perPage).coerceIn(1, 4)
            val collected = mutableListOf<AniListMedia>()
            for (page in 1..pages) {
                val req = Request.Builder()
                    .url("https://api.jikan.moe/v4/top/anime?page=$page&limit=$perPage&sfw=true$filter")
                    .header("Accept", "application/json")
                    .build()
                fallbackHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val dataArr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("data") ?: return@use
                    for (i in 0 until dataArr.length()) {
                        val obj = dataArr.optJSONObject(i) ?: continue
                        val malId = obj.optInt("mal_id", 0)
                        if (malId <= 0) continue
                        val titleDefault = obj.optString("title").takeIf { it.isNotBlank() }
                        val titleEng = obj.optString("title_english").takeIf { it.isNotBlank() }
                        val imgJpg = obj.optJSONObject("images")?.optJSONObject("jpg")
                        val cover = imgJpg?.optString("large_image_url")?.takeIf { it.isNotBlank() }
                            ?: imgJpg?.optString("image_url").orEmpty()
                        val scoreFloat = obj.optDouble("score", -1.0)
                        val score100 = if (scoreFloat > 0.0) (scoreFloat * 10.0).toInt().coerceIn(1, 100) else null
                        val genresArr = obj.optJSONArray("genres")
                        val genres = mutableListOf<String>()
                        if (genresArr != null) {
                            for (g in 0 until genresArr.length()) {
                                genresArr.optJSONObject(g)?.optString("name")?.takeIf { it.isNotBlank() }?.let { genres.add(it) }
                            }
                        }
                        collected.add(
                            AniListMedia(
                                id = malId,
                                type = "ANIME",
                                title = AniListTitle(romaji = titleDefault, english = titleEng, userPreferred = titleEng ?: titleDefault),
                                coverImage = AniListCoverImage(large = cover, extraLarge = cover),
                                format = obj.optString("type").uppercase().ifBlank { "TV" },
                                episodes = obj.optInt("episodes", -1).takeIf { it > 0 },
                                genres = genres,
                                averageScore = score100,
                                seasonYear = obj.optInt("year", -1).takeIf { it > 0 }
                            )
                        )
                    }
                }
                if (collected.size >= limit) break
            }
            collected
        }.getOrDefault(emptyList())
    }

    companion object {
        fun isAnimeMediaType(type: String?): Boolean {
            return type.isNullOrBlank() || type.equals("ANIME", ignoreCase = true)
        }

        fun isEpisodicSeasonFormat(format: String?): Boolean {
            if (format.isNullOrBlank()) return true
            return format.uppercase().trim() in setOf("TV", "TV_SHORT", "ONA")
        }

        private fun toRelationItem(node: AniListMedia, rawRelationType: String?): AnimeRelationItem? {
            if (!isAnimeMediaType(node.type)) return null
            val isDonghua = node.countryOfOrigin.equals("CN", ignoreCase = true) ||
                node.genres.orEmpty().any { it.contains("Donghua", ignoreCase = true) }
            val title = EnglishTitleMapper.resolve(
                titleEnglish = node.title?.english,
                titleRomaji = node.title?.romaji,
                synonyms = node.synonyms,
                isDonghua = isDonghua
            )?.takeIf { it.isNotBlank() }
                ?: node.title?.english?.takeIf { it.isNotBlank() }
                ?: node.title?.userPreferred?.takeIf { it.isNotBlank() }
                ?: node.title?.romaji?.takeIf { it.isNotBlank() }
                ?: return null
            return AnimeRelationItem(
                id = node.id,
                relationType = rawRelationType?.replace('_', ' ') ?: "Related",
                title = title,
                coverImage = node.coverImage?.large ?: node.coverImage?.medium.orEmpty(),
                format = node.format,
                status = node.status,
                averageScore = node.averageScore,
                mediaType = node.type ?: "ANIME",
                episodes = node.episodes,
                seasonYear = node.seasonYear
            )
        }
        fun cleanDescription(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            return raw
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#39;", "'")
                .trim()
        }

        fun mapMediaToEntity(media: AniListMedia, existing: AnimeEntity? = null): AnimeEntity {
            val genresList = media.genres.orEmpty()
            val studiosList = media.studios?.nodes.orEmpty().mapNotNull { it.name?.trim()?.takeIf { n -> n.isNotBlank() } }
            val isDonghua = media.countryOfOrigin.equals("CN", ignoreCase = true) ||
                genresList.any { it.contains("Donghua", ignoreCase = true) }

            val resolvedEnglish = EnglishTitleMapper.resolve(
                titleEnglish = media.title?.english,
                titleRomaji = media.title?.romaji,
                synonyms = media.synonyms,
                isDonghua = isDonghua
            ) ?: media.title?.english?.trim()?.takeIf { it.isNotBlank() }
                ?: existing?.titleEnglish

            val parsedEp = EpisodeDataParser.parseAniListEpisodes(
                apiEpisodes = media.episodes,
                statusStr = media.status,
                nextAiringEpisodeNumber = media.nextAiringEpisode?.episode,
                nextAiringAtUnix = media.nextAiringEpisode?.airingAt,
                existingAired = existing?.currentEpisode ?: 0,
                mediaId = media.id,
                title = resolvedEnglish ?: media.title?.romaji,
                isDonghua = isDonghua,
                startYear = media.startDate?.year,
                startMonth = media.startDate?.month,
                startDay = media.startDate?.day,
                format = media.format
            )

            val airingTime = media.nextAiringEpisode?.airingAt ?: existing?.airingTimeUnix
            val dayOfWeek = if (airingTime != null && airingTime > 0) {
                Instant.ofEpochSecond(airingTime).atZone(ZoneId.systemDefault()).dayOfWeek.value
            } else {
                existing?.airingDayOfWeek ?: 1
            }

            return AnimeEntity(
                id = media.id,
                titleRomaji = media.title?.romaji ?: existing?.titleRomaji ?: "Untitled Anime",
                titleEnglish = resolvedEnglish,
                titleNative = media.title?.native ?: existing?.titleNative,
                titlePinyin = if (isDonghua) media.title?.romaji else existing?.titlePinyin,
                metadataSource = "anilist",
                coverImage = media.coverImage?.extraLarge
                    ?: media.coverImage?.large
                    ?: media.coverImage?.medium
                    ?: existing?.coverImage.orEmpty(),
                bannerImage = media.bannerImage ?: existing?.bannerImage,
                format = media.format ?: existing?.format ?: "TV",
                status = media.status ?: existing?.status ?: "RELEASING",
                totalEpisodes = parsedEp.totalEpisodes,
                currentEpisode = parsedEp.airedEpisodeCount,
                watchedEpisodeCount = existing?.watchedEpisodeCount ?: 0,
                userStatus = existing?.userStatus,
                userScore = existing?.userScore,
                startDate = existing?.startDate,
                finishDate = existing?.finishDate,
                airingDayOfWeek = dayOfWeek,
                airingTimeUnix = airingTime,
                nextEpisodeNumber = media.nextAiringEpisode?.episode ?: parsedEp.nextEpisodeNumber,
                genres = if (genresList.isNotEmpty()) genresList.joinToString(", ") else existing?.genres.orEmpty(),
                studios = if (studiosList.isNotEmpty()) studiosList.joinToString(", ") else existing?.studios.orEmpty(),
                season = media.season ?: existing?.season,
                seasonYear = media.seasonYear ?: existing?.seasonYear,
                score = media.averageScore ?: existing?.score,
                popularity = media.popularity ?: existing?.popularity,
                synopsis = cleanDescription(media.description) ?: existing?.synopsis,
                countryOfOrigin = media.countryOfOrigin ?: existing?.countryOfOrigin ?: "JP",
                inWatchlist = existing?.inWatchlist ?: false,
                isWishlist = existing?.isWishlist ?: false,
                isFavorite = existing?.isFavorite ?: false,
                isPinned = existing?.isPinned ?: false,
                notificationsEnabled = existing?.notificationsEnabled ?: true,
                isWatched = existing?.isWatched ?: false,
                lastWatchedTime = existing?.lastWatchedTime,
                userNotes = existing?.userNotes,
                lastCachedAt = System.currentTimeMillis()
            )
        }

        fun mapRichDetail(media: AniListMedia): AnimeRichDetail {
            val studios = media.studios?.nodes.orEmpty()
                .mapNotNull { it.name?.trim()?.takeIf { n -> n.isNotBlank() } }
            val characters = media.characters?.nodes.orEmpty().mapNotNull { ch ->
                val full = ch.name?.full?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                AnimeCharacterItem(
                    id = ch.id ?: full.hashCode(),
                    name = full,
                    imageUrl = ch.image?.large ?: ch.image?.medium.orEmpty()
                )
            }
            val relations = media.relations?.edges.orEmpty().mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null
                toRelationItem(node, edge.relationType)
            }.distinctBy { it.id }
            val recs = media.recommendations?.nodes.orEmpty().mapNotNull { node ->
                val rec = node.mediaRecommendation ?: return@mapNotNull null
                val recIsDonghua = rec.countryOfOrigin.equals("CN", ignoreCase = true) ||
                    rec.genres.orEmpty().any { it.contains("Donghua", ignoreCase = true) }
                val title = EnglishTitleMapper.resolve(
                    titleEnglish = rec.title?.english,
                    titleRomaji = rec.title?.romaji,
                    synonyms = rec.synonyms,
                    isDonghua = recIsDonghua
                )?.takeIf { it.isNotBlank() }
                    ?: rec.title?.english?.takeIf { it.isNotBlank() }
                    ?: rec.title?.romaji?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                AnimeRecommendationItem(
                    id = rec.id,
                    title = title,
                    coverImage = rec.coverImage?.large ?: rec.coverImage?.medium.orEmpty(),
                    format = rec.format,
                    averageScore = rec.averageScore
                )
            }
            return AnimeRichDetail(
                animeId = media.id,
                studios = studios,
                characters = characters,
                relations = relations,
                recommendations = recs
            )
        }
    }
}
