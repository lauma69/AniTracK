import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-cron-secret",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const ANILIST_ENDPOINT = "https://graphql.anilist.co";
const YOUTUBE_VIDEO_ID_REGEX = /^[A-Za-z0-9_-]{11}$/;
const MIN_ANILIST_SCORE = 60; // 6.0 / 10 minimum rating
const IST_OFFSET_MS = (5 * 60 + 30) * 60 * 1000; // Asia/Kolkata (UTC+05:30)

const EXCLUDED_BL_GL_EXACT_TOKENS = new Set([
  "bl",
  "gl",
  "yaoi",
  "yuri",
  "boys' love",
  "boys love",
  "boy's love",
  "girls' love",
  "girls love",
  "girl's love",
  "shounen ai",
  "shonen ai",
  "shoujo ai",
  "shojo ai",
]);

const EXCLUDED_BL_GL_PHRASE_REGEX =
  /(^|[^a-z0-9])(boys'?\s*love|boy's\s*love|girls'?\s*love|girl's\s*love|yaoi|yuri|shou?nen\s*ai|shou?jo\s*ai)([^a-z0-9]|$)/i;

const SEQUEL_OR_MOVIE_TITLE_REGEX = new RegExp(
  [
    "\\bseason\\s*(?:0*[2-9]|\\d{2,}|ii|iii|iv|v|vi|vii|viii|ix|x)\\b",
    "\\b(?:2nd|3rd|[4-9]th|\\d{2,}th|second|third|fourth|fifth|sixth|final|last|climax)\\s+(?:season|series|cour|part|chapter|arc|act)\\b",
    "\\b(?:part|cour|act|chapter|phase|stage)\\s*(?:0*[2-9]|\\d{2,}|ii|iii|iv|v|vi)\\b",
    "\\bs(?:0*[2-9]|\\d{2,})\\b",
    "\\b(?:the\\s+movie|movie\\b|film\\b|ova\\b|oad\\b|recap\\b|special\\b|spin[\\s\\-]*off\\b|side[\\s\\-]*story\\b)",
    "\\b(?:shippu?den|next\\s+generations|thousand[\\s\\-]year\\s+blood\\s+war|entertainment\\s+district\\s+arc|swordsmith\\s+village\\s+arc|hashira\\s+training\\s+arc|mugen\\s+train\\s+arc|culling\\s+game|shibuya\\s+incident)\\b",
    "(?:\\s+|:|-)(?:ii|iii|iv|v|vi|r2)(?:\\s*$|\\s*[:\\-])",
  ].join("|"),
  "i"
);

function isSequelOrMovieTitle(rawTitle: string | null | undefined): boolean {
  if (!rawTitle) return false;
  const cleaned = String(rawTitle).trim();
  if (!cleaned) return false;
  return SEQUEL_OR_MOVIE_TITLE_REGEX.test(cleaned);
}

function fuzzyDateToSortableInt(d?: AniListFuzzyDate | null): number {
  if (!d || typeof d.year !== "number" || d.year <= 0) return 0;
  const m = Math.max(1, Math.min(12, Number(d.month ?? 1)));
  const day = Math.max(1, Math.min(31, Number(d.day ?? 1)));
  return d.year * 10000 + m * 100 + day;
}

function hasSequelOrDerivativeRelation(candidate: {
  startDate?: AniListFuzzyDate | null;
  relations?: {
    edges?: Array<{
      relationType?: string | null;
      node?: {
        id?: number | null;
        type?: string | null;
        format?: string | null;
        startDate?: AniListFuzzyDate | null;
      } | null;
    } | null> | null;
  } | null;
}): boolean {
  const edges = candidate?.relations?.edges;
  if (!Array.isArray(edges)) return false;
  const candidateDate = fuzzyDateToSortableInt(candidate.startDate);
  for (const edge of edges) {
    if (!edge) continue;
    const rel = String(edge.relationType ?? "").trim().toUpperCase();
    const node = edge.node;
    const nodeType = String(node?.type ?? "").trim().toUpperCase();
    const nodeFormat = String(node?.format ?? "").trim().toUpperCase();
    const nodeDate = fuzzyDateToSortableInt(node?.startDate);
    if (nodeType !== "ANIME") continue;

    if (rel === "PARENT" || rel === "FULL_STORY") {
      return true;
    }

    if (rel === "PREQUEL") {
      if (nodeFormat === "TV") {
        if (!candidateDate || !nodeDate || nodeDate <= candidateDate) {
          return true;
        }
      } else if (nodeFormat === "TV_SHORT" || nodeFormat === "ONA" || nodeFormat === "MOVIE") {
        if (candidateDate > 0 && nodeDate > 0 && nodeDate <= candidateDate) {
          return true;
        }
      }
    }
  }
  return false;
}

const ANILIST_DAILY_POOL_QUERY = `
query ($page: Int!, $perPage: Int!, $sort: [MediaSort]) {
  Page(page: $page, perPage: $perPage) {
    pageInfo {
      total
      currentPage
      lastPage
      hasNextPage
    }
    media(
      type: ANIME
      format_in: [TV]
      isAdult: false
      averageScore_greater: 59
      genre_not_in: ["Hentai", "Boys' Love", "Girls' Love", "Yaoi", "Yuri", "Shounen Ai", "Shoujo Ai"]
      tag_not_in: ["Boys' Love", "Girls' Love", "Yuri", "Yaoi", "Shounen Ai", "Shoujo Ai"]
      sort: $sort
    ) {
      id
      title {
        english
        romaji
        native
      }
      coverImage {
        extraLarge
        large
        color
      }
      bannerImage
      averageScore
      episodes
      startDate {
        year
        month
        day
      }
      endDate {
        year
        month
        day
      }
      status
      format
      season
      seasonYear
      genres
      tags {
        name
        category
      }
      description(asHtml: false)
      duration
      source
      countryOfOrigin
      studios(isMain: true) {
        nodes {
          name
        }
      }
      trailer {
        id
        site
        thumbnail
      }
      relations {
        edges {
          relationType(version: 2)
          node {
            id
            type
            format
            startDate {
              year
              month
              day
            }
          }
        }
      }
    }
  }
}
`;

interface AniListFuzzyDate {
  year?: number | null;
  month?: number | null;
  day?: number | null;
}

interface AniListTagNode {
  name?: string | null;
  category?: string | null;
}

interface AniListMediaCandidate {
  id: number;
  title?: {
    english?: string | null;
    romaji?: string | null;
    native?: string | null;
  } | null;
  coverImage?: {
    extraLarge?: string | null;
    large?: string | null;
    color?: string | null;
  } | null;
  bannerImage?: string | null;
  averageScore?: number | null;
  episodes?: number | null;
  startDate?: AniListFuzzyDate | null;
  endDate?: AniListFuzzyDate | null;
  status?: string | null;
  format?: string | null;
  season?: string | null;
  seasonYear?: number | null;
  genres?: string[] | null;
  tags?: Array<AniListTagNode | string | null> | null;
  description?: string | null;
  duration?: number | null;
  source?: string | null;
  countryOfOrigin?: string | null;
  studios?: {
    nodes?: Array<{ name?: string | null }> | null;
  } | null;
  trailer?: {
    id?: string | null;
    site?: string | null;
    thumbnail?: string | null;
  } | null;
  relations?: {
    edges?: Array<{
      relationType?: string | null;
      node?: {
        id?: number | null;
        type?: string | null;
        format?: string | null;
        startDate?: AniListFuzzyDate | null;
      } | null;
    } | null> | null;
  } | null;
}

function timingSafeEqual(a: string, b: string): boolean {
  if (!a || !b || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
}

function getIstDateString(now = new Date()): string {
  try {
    const parts = new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Kolkata",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).formatToParts(now);
    const y = parts.find((p) => p.type === "year")?.value;
    const m = parts.find((p) => p.type === "month")?.value;
    const d = parts.find((p) => p.type === "day")?.value;
    if (y && m && d) return `${y}-${m}-${d}`;
  } catch {
    // Fall back to fixed UTC+05:30 offset below
  }
  return new Date(now.getTime() + IST_OFFSET_MS).toISOString().slice(0, 10);
}

function isExcludedBlGlToken(raw: string | null | undefined): boolean {
  if (!raw) return false;
  const normalized = String(raw)
    .trim()
    .toLowerCase()
    .replace(/[’‘`]/g, "'")
    .replace(/[_\-]+/g, " ")
    .replace(/\s+/g, " ");
  if (!normalized) return false;
  if (EXCLUDED_BL_GL_EXACT_TOKENS.has(normalized)) return true;
  return EXCLUDED_BL_GL_PHRASE_REGEX.test(normalized);
}

function extractTagNames(tags?: Array<AniListTagNode | string | null> | null): string[] {
  if (!Array.isArray(tags)) return [];
  const result: string[] = [];
  for (const t of tags) {
    if (!t) continue;
    if (typeof t === "string") {
      const s = t.trim();
      if (s) result.push(s);
    } else if (typeof t === "object") {
      const name = String(t.name ?? "").trim();
      if (name) result.push(name);
      const cat = String(t.category ?? "").trim();
      if (cat && isExcludedBlGlToken(cat)) result.push(cat);
    }
  }
  return result;
}

function hasExcludedBlGlGenreOrTag(
  genres?: string[] | null,
  tags?: Array<AniListTagNode | string | null> | null
): boolean {
  const cleanGenres = (genres ?? []).map((g) => String(g ?? "").trim()).filter(Boolean);
  const cleanTags = extractTagNames(tags);
  if (cleanGenres.length === 0 || cleanTags.length === 0) return true;
  for (const g of cleanGenres) {
    if (isExcludedBlGlToken(g)) return true;
  }
  for (const t of cleanTags) {
    if (isExcludedBlGlToken(t)) return true;
  }
  return false;
}

function sanitizeDescription(raw: string | null | undefined): string {
  if (!raw) return "";
  return raw
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/?[^>]+(>|$)/g, "")
    .replace(/&quot;/g, '"')
    .replace(/&#039;|&apos;/g, "'")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function formatFuzzyDate(d?: AniListFuzzyDate | null): string | null {
  if (!d || !d.year) return null;
  const year = String(d.year).padStart(4, "0");
  if (!d.month) return year;
  const month = String(d.month).padStart(2, "0");
  if (!d.day) return `${year}-${month}`;
  const day = String(d.day).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function isValidYouTubeId(id: string | null | undefined): id is string {
  return typeof id === "string" && YOUTUBE_VIDEO_ID_REGEX.test(id.trim());
}

function shuffleInPlace<T>(items: T[]): T[] {
  for (let i = items.length - 1; i > 0; i--) {
    const buf = new Uint32Array(1);
    crypto.getRandomValues(buf);
    const j = buf[0] % (i + 1);
    [items[i], items[j]] = [items[j], items[i]];
  }
  return items;
}

function randomInt(min: number, max: number): number {
  if (max <= min) return min;
  const buf = new Uint32Array(1);
  crypto.getRandomValues(buf);
  return min + (buf[0] % (max - min + 1));
}

async function fetchAniListPage(
  page: number,
  perPage: number,
  sort: string
): Promise<{ media: AniListMediaCandidate[]; lastPage: number }> {
  const response = await fetch(ANILIST_ENDPOINT, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({
      query: ANILIST_DAILY_POOL_QUERY,
      variables: { page, perPage, sort: [sort] },
    }),
  });

  if (!response.ok) {
    throw new Error(`AniList request failed with HTTP ${response.status}`);
  }

  const json = await response.json();
  const pageData = json?.data?.Page;
  const media = Array.isArray(pageData?.media) ? (pageData.media as AniListMediaCandidate[]) : [];
  const lastPage = Number(pageData?.pageInfo?.lastPage ?? 1);
  return { media, lastPage: Number.isFinite(lastPage) && lastPage >= 1 ? lastPage : 1 };
}

async function verifyYouTubeVideoEligible(
  videoId: string,
  youtubeApiKey: string
): Promise<boolean> {
  if (!isValidYouTubeId(videoId)) return false;

  // Prefer YouTube Data API v3 verification when YOUTUBE_API_KEY is configured
  if (youtubeApiKey) {
    try {
      const params = new URLSearchParams({
        part: "status,contentDetails",
        id: videoId,
        key: youtubeApiKey,
      });
      const res = await fetch(`https://www.googleapis.com/youtube/v3/videos?${params.toString()}`);
      if (res.ok) {
        const data = await res.json();
        const item = Array.isArray(data?.items) ? data.items[0] : null;
        if (!item) return false;
        const status = item.status ?? {};
        const contentDetails = item.contentDetails ?? {};
        const isPublic = status.privacyStatus === "public";
        const isProcessed = !status.uploadStatus || status.uploadStatus === "processed";
        const isEmbeddable = status.embeddable === true;
        const isAgeRestricted = Boolean(contentDetails?.contentRating?.ytRating);
        return isPublic && isProcessed && isEmbeddable && !isAgeRestricted;
      }
    } catch {
      // Fall through to lightweight oEmbed check below
    }
  }

  // Zero-quota fallback check via YouTube oEmbed (returns 200 only for public embeddable videos)
  try {
    const oembedUrl = `https://www.youtube.com/oembed?url=${encodeURIComponent(
      `https://www.youtube.com/watch?v=${videoId}`
    )}&format=json`;
    const res = await fetch(oembedUrl);
    return res.ok;
  } catch {
    return false;
  }
}

async function searchYouTubeTrailerFallback(
  animeTitle: string,
  releaseYear: number | null,
  youtubeApiKey: string
): Promise<string | null> {
  if (!youtubeApiKey || !animeTitle.trim()) return null;
  try {
    const query = [animeTitle.trim(), releaseYear ? String(releaseYear) : "", "anime official trailer"]
      .filter(Boolean)
      .join(" ");
    const params = new URLSearchParams({
      part: "snippet",
      type: "video",
      videoEmbeddable: "true",
      safeSearch: "strict",
      maxResults: "3",
      q: query,
      key: youtubeApiKey,
    });
    const res = await fetch(`https://www.googleapis.com/youtube/v3/search?${params.toString()}`);
    if (!res.ok) return null;
    const data = await res.json();
    const items = Array.isArray(data?.items) ? data.items : [];
    for (const item of items) {
      const candidateId = item?.id?.videoId;
      if (isValidYouTubeId(candidateId)) {
        const eligible = await verifyYouTubeVideoEligible(candidateId, youtubeApiKey);
        if (eligible) return candidateId;
      }
    }
    return null;
  } catch {
    return null;
  }
}

function isCandidateMetadataValid(
  candidate: AniListMediaCandidate,
  historyIds: Set<number>
): boolean {
  if (!candidate || typeof candidate.id !== "number" || candidate.id <= 0) return false;
  if (historyIds.has(candidate.id)) return false;

  // Strict rating rule: minimum 60 (6.0/10), never null/missing, never < 60
  if (typeof candidate.averageScore !== "number" || !Number.isFinite(candidate.averageScore)) {
    return false;
  }
  if (candidate.averageScore < MIN_ANILIST_SCORE || candidate.averageScore > 100) {
    return false;
  }

  const displayTitle =
    candidate.title?.english?.trim() ||
    candidate.title?.romaji?.trim() ||
    candidate.title?.native?.trim() ||
    "";
  if (!displayTitle) return false;

  // Only Season 1 / standalone TV entry-point anime (no movies, OVAs, specials, or Season 2/3+ sequels)
  if (String(candidate.format ?? "").trim().toUpperCase() !== "TV") {
    return false;
  }
  if (hasSequelOrDerivativeRelation(candidate)) {
    return false;
  }
  if (
    isSequelOrMovieTitle(displayTitle) ||
    isSequelOrMovieTitle(candidate.title?.english) ||
    isSequelOrMovieTitle(candidate.title?.romaji) ||
    isSequelOrMovieTitle(candidate.title?.native)
  ) {
    return false;
  }

  const cover = candidate.coverImage?.extraLarge?.trim() || candidate.coverImage?.large?.trim() || "";
  if (!cover || !/^https?:\/\//i.test(cover)) return false;

  // Secondary validation: exclude BL, GL, Yaoi, Yuri, Shounen Ai, Shoujo Ai, and missing genre/tag metadata
  if (hasExcludedBlGlGenreOrTag(candidate.genres, candidate.tags)) {
    return false;
  }

  return true;
}

function buildStoredMetadata(candidate: AniListMediaCandidate, trailerId: string) {
  const studios = (candidate.studios?.nodes ?? [])
    .map((s) => s?.name?.trim() ?? "")
    .filter((name) => name.length > 0);
  const genres = (candidate.genres ?? [])
    .map((g) => String(g).trim())
    .filter((g) => g.length > 0);
  const tags = extractTagNames(candidate.tags);

  return {
    anilist_id: candidate.id,
    title: {
      english: candidate.title?.english?.trim() || null,
      romaji: candidate.title?.romaji?.trim() || null,
      native: candidate.title?.native?.trim() || null,
    },
    cover_image: candidate.coverImage?.extraLarge?.trim() || candidate.coverImage?.large?.trim() || "",
    cover_color: candidate.coverImage?.color?.trim() || null,
    banner_image: candidate.bannerImage?.trim() || null,
    average_score: candidate.averageScore,
    episodes: typeof candidate.episodes === "number" && candidate.episodes > 0 ? candidate.episodes : null,
    start_date: formatFuzzyDate(candidate.startDate),
    end_date: formatFuzzyDate(candidate.endDate),
    status: candidate.status || null,
    format: candidate.format || "TV",
    is_entry_point: true,
    season: candidate.season || null,
    season_year: candidate.seasonYear ?? candidate.startDate?.year ?? null,
    genres,
    tags,
    description: sanitizeDescription(candidate.description),
    duration: typeof candidate.duration === "number" && candidate.duration > 0 ? candidate.duration : null,
    source: candidate.source || null,
    studios,
    country_of_origin: candidate.countryOfOrigin || null,
    trailer: {
      id: trailerId,
      site: "youtube",
      thumbnail: `https://i.ytimg.com/vi/${trailerId}/hqdefault.jpg`,
    },
  };
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ error: "Method not allowed. Use POST." }),
      { status: 405, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  const cronSecret = Deno.env.get("CRON_SECRET") ?? "";
  const youtubeApiKey = Deno.env.get("YOUTUBE_API_KEY") ?? "";

  if (!supabaseUrl || !supabaseServiceKey) {
    return new Response(
      JSON.stringify({ error: "Server configuration incomplete." }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  // Authorize caller via CRON_SECRET, service_role Bearer token, or verified authenticated user JWT
  // (all selection logic & UNIQUE(selection_date) atomic RPC remain 100% backend-controlled & idempotent)
  const authHeader = req.headers.get("Authorization") ?? "";
  const bearerToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
  const providedCronSecret = (req.headers.get("x-cron-secret") ?? "").trim();

  const authorizedByServiceRole = timingSafeEqual(bearerToken, supabaseServiceKey);
  const authorizedByCronSecret = Boolean(cronSecret) && timingSafeEqual(providedCronSecret, cronSecret);
  let authorizedByUserJwt = false;

  if (!authorizedByServiceRole && !authorizedByCronSecret && bearerToken && supabaseAnonKey) {
    try {
      const userClient = createClient(supabaseUrl, supabaseAnonKey, {
        global: { headers: { Authorization: authHeader } },
        auth: { persistSession: false, autoRefreshToken: false },
      });
      const { data: userData, error: userErr } = await userClient.auth.getUser();
      authorizedByUserJwt = !userErr && Boolean(userData?.user?.id);
    } catch {
      authorizedByUserJwt = false;
    }
  }

  if (!authorizedByServiceRole && !authorizedByCronSecret && !authorizedByUserJwt) {
    return new Response(
      JSON.stringify({ error: "Unauthorized" }),
      { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  const adminClient = createClient(supabaseUrl, supabaseServiceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const todayIst = getIstDateString();

  try {
    // 1. Idempotency check: return today's IST selection if already chosen and valid (non-BL/GL, score >= 60, Season 1 TV entry-point)
    const { data: existingToday, error: existingErr } = await adminClient
      .from("daily_anime")
      .select("*")
      .eq("selection_date", todayIst)
      .maybeSingle();

    if (existingErr) {
      throw new Error("Failed to check existing daily selection.");
    }

    if (
      existingToday &&
      Number(existingToday.rating ?? 0) >= MIN_ANILIST_SCORE &&
      String(existingToday?.anime_metadata?.format ?? "").trim().toUpperCase() === "TV" &&
      existingToday?.anime_metadata?.is_entry_point !== false &&
      !isSequelOrMovieTitle(existingToday?.title) &&
      !isSequelOrMovieTitle(existingToday?.anime_metadata?.title?.english) &&
      !isSequelOrMovieTitle(existingToday?.anime_metadata?.title?.romaji) &&
      !hasExcludedBlGlGenreOrTag(
        existingToday?.anime_metadata?.genres,
        existingToday?.anime_metadata?.tags
      )
    ) {
      return new Response(
        JSON.stringify({
          status: "already_selected",
          selection_date: todayIst,
          anime: existingToday,
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 2. Load previously featured AniList IDs to prevent repeats
    const { data: historyRows, error: historyErr } = await adminClient
      .from("anime_history")
      .select("anilist_id");

    if (historyErr) {
      throw new Error("Failed to load anime selection history.");
    }

    const historyIds = new Set<number>(
      (historyRows ?? [])
        .map((row: { anilist_id?: number }) => Number(row.anilist_id))
        .filter((id) => Number.isFinite(id) && id > 0)
    );

    // 3. Pick randomly from eligible AniList catalog across multiple sorts & random pages
    const sortPool = shuffleInPlace([
      "POPULARITY_DESC",
      "FAVOURITES_DESC",
      "TRENDING_DESC",
      "SCORE_DESC",
      "START_DATE_DESC",
    ]);

    const perPage = 50;
    const maxPageAttempts = 5;
    let selectedCandidate: AniListMediaCandidate | null = null;
    let selectedTrailerId: string | null = null;

    // Probe pool size once (lightweight page 1 metadata) so random pages stay within valid bounds
    let estimatedMaxPages = 60;
    try {
      const probe = await fetchAniListPage(1, perPage, sortPool[0]);
      estimatedMaxPages = Math.max(1, Math.min(probe.lastPage, 80));
    } catch {
      estimatedMaxPages = 40;
    }

    for (let attempt = 0; attempt < maxPageAttempts && !selectedCandidate; attempt++) {
      const sort = sortPool[attempt % sortPool.length];
      // Bias early attempts across a wide random range (not always top page)
      const targetPage = attempt < 4 ? randomInt(1, estimatedMaxPages) : randomInt(1, Math.min(10, estimatedMaxPages));

      let media: AniListMediaCandidate[] = [];
      try {
        const pageResult = await fetchAniListPage(targetPage, perPage, sort);
        media = pageResult.media;
      } catch (err) {
        console.error(`[daily-random-anime] AniList page fetch failed on attempt ${attempt + 1}:`, String(err));
        continue;
      }

      const eligible = shuffleInPlace(
        media.filter((item) => isCandidateMetadataValid(item, historyIds))
      );

      // Prefer candidates that already provide a YouTube trailer in AniList metadata
      const withTrailer = eligible.filter(
        (m) => m.trailer?.site?.toLowerCase() === "youtube" && isValidYouTubeId(m.trailer?.id)
      );
      const withoutTrailer = eligible.filter(
        (m) => !(m.trailer?.site?.toLowerCase() === "youtube" && isValidYouTubeId(m.trailer?.id))
      );

      // Check up to 12 candidates with AniList YouTube trailers first
      for (const candidate of withTrailer.slice(0, 12)) {
        const candidateTrailerId = candidate.trailer!.id!.trim();
        const isEligibleTrailer = await verifyYouTubeVideoEligible(candidateTrailerId, youtubeApiKey);
        if (isEligibleTrailer) {
          selectedCandidate = candidate;
          selectedTrailerId = candidateTrailerId;
          break;
        }
      }

      // If none had a verified AniList trailer and YOUTUBE_API_KEY is available, search YouTube for up to 3 candidates
      if (!selectedCandidate && youtubeApiKey) {
        for (const candidate of withoutTrailer.slice(0, 3)) {
          const title =
            candidate.title?.english?.trim() ||
            candidate.title?.romaji?.trim() ||
            candidate.title?.native?.trim() ||
            "";
          const fallbackTrailerId = await searchYouTubeTrailerFallback(
            title,
            candidate.seasonYear ?? candidate.startDate?.year ?? null,
            youtubeApiKey
          );
          if (fallbackTrailerId) {
            selectedCandidate = candidate;
            selectedTrailerId = fallbackTrailerId;
            break;
          }
        }
      }
    }

    // 4. If no valid candidate found, NEVER publish invalid data — preserve last valid selection
    if (!selectedCandidate || !selectedTrailerId) {
      const { data: lastValid } = await adminClient
        .from("daily_anime")
        .select("selection_date, anilist_id, title")
        .order("selection_date", { ascending: false })
        .limit(1)
        .maybeSingle();

      console.error("[daily-random-anime] No eligible anime with verified trailer found; preserving fallback.");
      return new Response(
        JSON.stringify({
          status: "no_eligible_candidate",
          selection_date: todayIst,
          fallback_preserved: Boolean(lastValid),
          last_valid_selection_date: lastValid?.selection_date ?? null,
        }),
        { status: 503, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 5. Atomically persist daily selection + history via SQL RPC
    const displayTitle =
      selectedCandidate.title?.english?.trim() ||
      selectedCandidate.title?.romaji?.trim() ||
      selectedCandidate.title?.native?.trim() ||
      `Anime #${selectedCandidate.id}`;
    const coverImage =
      selectedCandidate.coverImage?.extraLarge?.trim() ||
      selectedCandidate.coverImage?.large?.trim() ||
      "";
    const metadata = buildStoredMetadata(selectedCandidate, selectedTrailerId);

    const { data: savedRows, error: saveErr } = await adminClient.rpc("save_daily_anime_selection", {
      p_selection_date: todayIst,
      p_anilist_id: selectedCandidate.id,
      p_title: displayTitle,
      p_rating: selectedCandidate.averageScore,
      p_cover_image: coverImage,
      p_banner_image: selectedCandidate.bannerImage?.trim() || null,
      p_trailer_id: selectedTrailerId,
      p_anime_metadata: metadata,
    });

    if (saveErr) {
      console.error("[daily-random-anime] Atomic save RPC failed:", saveErr.message);
      throw new Error("Failed to persist daily anime selection.");
    }

    const saved = Array.isArray(savedRows) ? savedRows[0] : savedRows;

    return new Response(
      JSON.stringify({
        status: "selected",
        selection_date: todayIst,
        anime: saved,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    console.error("[daily-random-anime] Unexpected failure:", error instanceof Error ? error.message : String(error));
    return new Response(
      JSON.stringify({
        error: "Unable to complete daily anime selection right now. Previous valid selection remains active.",
      }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
