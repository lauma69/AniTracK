-- ============================================================================
-- ANIME UNIVERSE — Random Anime of the Day (Daily Selection & History Schema)
-- Lightweight, atomic, zero-duplication daily pick integrated into Supabase.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. TABLES
-- ----------------------------------------------------------------------------

-- 1.1 Daily selected anime (one canonical row per UTC calendar date)
CREATE TABLE IF NOT EXISTS public.daily_anime (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    selection_date DATE NOT NULL UNIQUE,
    anilist_id INTEGER NOT NULL,
    title TEXT NOT NULL CHECK (char_length(trim(title)) BETWEEN 1 AND 300),
    rating INTEGER NOT NULL CHECK (rating >= 60 AND rating <= 100),
    cover_image TEXT NOT NULL CHECK (char_length(trim(cover_image)) > 0),
    banner_image TEXT,
    trailer_id TEXT NOT NULL CHECK (trailer_id ~ '^[A-Za-z0-9_-]{11}$'),
    anime_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.2 Featured history (guarantees previously selected anime are never repeated)
CREATE TABLE IF NOT EXISTS public.anime_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anilist_id INTEGER NOT NULL UNIQUE,
    selected_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 2. INDEXES
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_daily_anime_selection_date_desc
    ON public.daily_anime (selection_date DESC);

CREATE INDEX IF NOT EXISTS idx_daily_anime_anilist_id
    ON public.daily_anime (anilist_id);

CREATE INDEX IF NOT EXISTS idx_anime_history_selected_date_desc
    ON public.anime_history (selected_date DESC);

-- ----------------------------------------------------------------------------
-- 3. ATOMIC, IDEMPOTENT DAILY SELECTION RPC (IST Midnight Reset + BL/GL Guard)
-- ----------------------------------------------------------------------------

-- 3.0 Server-side BL / GL / Yaoi / Yuri / Movie / Season 2+ sequel / missing-metadata exclusion check
CREATE OR REPLACE FUNCTION public.is_bl_gl_metadata_excluded(
    p_anime_metadata JSONB
)
RETURNS BOOLEAN
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    v_genres JSONB;
    v_tags JSONB;
    v_token TEXT;
    v_format TEXT;
    v_title_en TEXT;
    v_title_ro TEXT;
    v_sequel_movie_pattern TEXT := '(\mseason\s*(0*[2-9]|\d{2,}|ii|iii|iv|v|vi|vii|viii|ix|x)\M|\m(2nd|3rd|[4-9]th|\d{2,}th|second|third|fourth|fifth|sixth|final|last|climax)\s+(season|series|cour|part|chapter|arc|act)\M|\m(part|cour|act|chapter|phase|stage)\s*(0*[2-9]|\d{2,}|ii|iii|iv|v|vi)\M|\ms(0*[2-9]|\d{2,})\M|\m(the\s+movie|movie|film|ova|oad|recap|special|spin[\s\-]*off|side[\s\-]*story)\M|\m(shippu?den|next\s+generations|thousand[\s\-]year\s+blood\s+war|entertainment\s+district\s+arc|swordsmith\s+village\s+arc|hashira\s+training\s+arc|mugen\s+train\s+arc|culling\s+game|shibuya\s+incident)\M)';
BEGIN
    IF p_anime_metadata IS NULL OR jsonb_typeof(p_anime_metadata) <> 'object' THEN
        RETURN TRUE;
    END IF;

    -- Must be a Season 1 / standalone TV series (exclude MOVIE, OVA, ONA, SPECIAL, TV_SHORT, and sequels)
    v_format := upper(trim(COALESCE(p_anime_metadata ->> 'format', '')));
    IF v_format <> 'TV' THEN
        RETURN TRUE;
    END IF;

    IF COALESCE((p_anime_metadata ->> 'is_entry_point')::BOOLEAN, TRUE) = FALSE THEN
        RETURN TRUE;
    END IF;

    v_title_en := lower(trim(COALESCE(p_anime_metadata #>> '{title,english}', '')));
    v_title_ro := lower(trim(COALESCE(p_anime_metadata #>> '{title,romaji}', '')));
    IF (v_title_en <> '' AND v_title_en ~* v_sequel_movie_pattern)
       OR (v_title_ro <> '' AND v_title_ro ~* v_sequel_movie_pattern) THEN
        RETURN TRUE;
    END IF;

    v_genres := p_anime_metadata -> 'genres';
    v_tags := p_anime_metadata -> 'tags';

    -- If genre or tag metadata is missing or empty, reject rather than risk recommending BL/GL
    IF v_genres IS NULL OR jsonb_typeof(v_genres) <> 'array' OR jsonb_array_length(v_genres) = 0 THEN
        RETURN TRUE;
    END IF;

    IF v_tags IS NULL OR jsonb_typeof(v_tags) <> 'array' OR jsonb_array_length(v_tags) = 0 THEN
        RETURN TRUE;
    END IF;

    FOR v_token IN
        SELECT lower(regexp_replace(regexp_replace(trim(val), '[’‘`]', '''', 'g'), '[_\-\s]+', ' ', 'g'))
        FROM (
            SELECT jsonb_array_elements_text(v_genres) AS val
            UNION ALL
            SELECT CASE
                WHEN jsonb_typeof(elem) = 'string' THEN elem #>> '{}'
                WHEN jsonb_typeof(elem) = 'object' THEN COALESCE(elem ->> 'name', '')
                ELSE ''
            END AS val
            FROM jsonb_array_elements(v_tags) AS elem
        ) AS combined
    LOOP
        IF v_token IN (
            'bl',
            'gl',
            'yaoi',
            'yuri',
            'boys'' love',
            'boys love',
            'boy''s love',
            'girls'' love',
            'girls love',
            'girl''s love',
            'shounen ai',
            'shonen ai',
            'shoujo ai',
            'shojo ai'
        ) OR v_token ~ '(^|[^a-z0-9])(boys''?\s*love|boy''s\s*love|girls''?\s*love|girl''s\s*love|yaoi|yuri|shou?nen\s*ai|shou?jo\s*ai)([^a-z0-9]|$)' THEN
            RETURN TRUE;
        END IF;
    END LOOP;

    RETURN FALSE;
END;
$$;

CREATE OR REPLACE FUNCTION public.save_daily_anime_selection(
    p_selection_date DATE,
    p_anilist_id INTEGER,
    p_title TEXT,
    p_rating INTEGER,
    p_cover_image TEXT,
    p_banner_image TEXT,
    p_trailer_id TEXT,
    p_anime_metadata JSONB
)
RETURNS SETOF public.daily_anime
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_existing public.daily_anime%ROWTYPE;
    v_inserted public.daily_anime%ROWTYPE;
BEGIN
    -- Strict server-side rule enforcement (defense in depth)
    IF p_selection_date IS NULL THEN
        RAISE EXCEPTION 'selection_date is required';
    END IF;

    IF p_anilist_id IS NULL OR p_anilist_id <= 0 THEN
        RAISE EXCEPTION 'anilist_id must be a positive integer';
    END IF;

    IF p_rating IS NULL OR p_rating < 60 OR p_rating > 100 THEN
        RAISE EXCEPTION 'rating must be between 60 and 100 (minimum 6.0/10)';
    END IF;

    IF p_title IS NULL OR char_length(trim(p_title)) = 0 THEN
        RAISE EXCEPTION 'title is required';
    END IF;

    IF p_cover_image IS NULL OR char_length(trim(p_cover_image)) = 0 THEN
        RAISE EXCEPTION 'cover_image is required';
    END IF;

    IF p_trailer_id IS NULL OR p_trailer_id !~ '^[A-Za-z0-9_-]{11}$' THEN
        RAISE EXCEPTION 'trailer_id must be a valid 11-character YouTube video ID';
    END IF;

    IF public.is_bl_gl_metadata_excluded(p_anime_metadata) THEN
        RAISE EXCEPTION 'BL, GL, Yaoi, Yuri, or incomplete genre/tag metadata is excluded from daily recommendations';
    END IF;

    -- Idempotency check: if today already has a valid selection, return it without changes
    SELECT * INTO v_existing
    FROM public.daily_anime
    WHERE selection_date = p_selection_date
    LIMIT 1;

    IF FOUND AND NOT public.is_bl_gl_metadata_excluded(v_existing.anime_metadata) AND v_existing.rating >= 60 THEN
        RETURN NEXT v_existing;
        RETURN;
    ELSIF FOUND THEN
        DELETE FROM public.daily_anime WHERE id = v_existing.id;
    END IF;

    -- Atomically record in anime_history (raises unique_violation if already featured)
    INSERT INTO public.anime_history (anilist_id, selected_date)
    VALUES (p_anilist_id, p_selection_date)
    ON CONFLICT (anilist_id) DO NOTHING;

    -- Atomically insert into daily_anime (guards against concurrent runs on same IST date)
    INSERT INTO public.daily_anime (
        selection_date,
        anilist_id,
        title,
        rating,
        cover_image,
        banner_image,
        trailer_id,
        anime_metadata
    )
    VALUES (
        p_selection_date,
        p_anilist_id,
        left(trim(p_title), 300),
        p_rating,
        trim(p_cover_image),
        NULLIF(trim(COALESCE(p_banner_image, '')), ''),
        p_trailer_id,
        COALESCE(p_anime_metadata, '{}'::jsonb)
    )
    ON CONFLICT (selection_date) DO NOTHING
    RETURNING * INTO v_inserted;

    IF v_inserted.id IS NULL THEN
        -- Another concurrent invocation already won for p_selection_date; undo our history row
        DELETE FROM public.anime_history
        WHERE anilist_id = p_anilist_id AND selected_date = p_selection_date;

        SELECT * INTO v_existing
        FROM public.daily_anime
        WHERE selection_date = p_selection_date
        LIMIT 1;

        IF FOUND THEN
            RETURN NEXT v_existing;
        END IF;
        RETURN;
    END IF;

    RETURN NEXT v_inserted;
END;
$$;

REVOKE ALL ON FUNCTION public.save_daily_anime_selection(
    DATE, INTEGER, TEXT, INTEGER, TEXT, TEXT, TEXT, JSONB
) FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION public.save_daily_anime_selection(
    DATE, INTEGER, TEXT, INTEGER, TEXT, TEXT, TEXT, JSONB
) TO service_role;

-- 3.2 Automatic zero-CLI daily claim RPC (authenticated & service_role)
-- Uses server IST date (Asia/Kolkata, midnight 12:00 AM IST reset) ONLY so clients can never backdate, overwrite, or forge picks.
-- Once today's canonical IST row exists, all callers receive the exact same locked row.
CREATE OR REPLACE FUNCTION public.claim_daily_anime_selection(
    p_anilist_id INTEGER,
    p_title TEXT,
    p_rating INTEGER,
    p_cover_image TEXT,
    p_banner_image TEXT,
    p_trailer_id TEXT,
    p_anime_metadata JSONB
)
RETURNS SETOF public.daily_anime
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_today_ist DATE := (NOW() AT TIME ZONE 'Asia/Kolkata')::DATE;
    v_existing public.daily_anime%ROWTYPE;
    v_inserted public.daily_anime%ROWTYPE;
BEGIN
    -- Always return today's canonical IST pick immediately if already selected and valid
    SELECT * INTO v_existing
    FROM public.daily_anime
    WHERE selection_date = v_today_ist
    LIMIT 1;

    IF FOUND AND NOT public.is_bl_gl_metadata_excluded(v_existing.anime_metadata) AND v_existing.rating >= 60 THEN
        RETURN NEXT v_existing;
        RETURN;
    ELSIF FOUND THEN
        DELETE FROM public.daily_anime WHERE id = v_existing.id;
    END IF;

    -- Require an authenticated session (or service_role) to claim today's empty slot
    IF auth.uid() IS NULL AND COALESCE(current_setting('request.jwt.claim.role', true), '') <> 'service_role' THEN
        RAISE EXCEPTION 'Authentication required to initialize today''s daily anime pick';
    END IF;

    -- Strict server-side rule enforcement
    IF p_anilist_id IS NULL OR p_anilist_id <= 0 THEN
        RAISE EXCEPTION 'anilist_id must be a positive integer';
    END IF;
    IF p_rating IS NULL OR p_rating < 60 OR p_rating > 100 THEN
        RAISE EXCEPTION 'rating must be between 60 and 100 (minimum 6.0/10)';
    END IF;
    IF p_title IS NULL OR char_length(trim(p_title)) = 0 THEN
        RAISE EXCEPTION 'title is required';
    END IF;
    IF p_cover_image IS NULL OR char_length(trim(p_cover_image)) = 0 THEN
        RAISE EXCEPTION 'cover_image is required';
    END IF;
    IF p_trailer_id IS NULL OR p_trailer_id !~ '^[A-Za-z0-9_-]{11}$' THEN
        RAISE EXCEPTION 'trailer_id must be a valid 11-character YouTube video ID';
    END IF;
    IF public.is_bl_gl_metadata_excluded(p_anime_metadata) THEN
        RAISE EXCEPTION 'BL, GL, Yaoi, Yuri, or incomplete genre/tag metadata is excluded from daily recommendations';
    END IF;

    -- Never repeat a previously featured anime
    IF EXISTS (SELECT 1 FROM public.anime_history WHERE anilist_id = p_anilist_id) THEN
        RAISE EXCEPTION 'anime has already been featured previously';
    END IF;

    INSERT INTO public.anime_history (anilist_id, selected_date)
    VALUES (p_anilist_id, v_today_ist)
    ON CONFLICT (anilist_id) DO NOTHING;

    INSERT INTO public.daily_anime (
        selection_date,
        anilist_id,
        title,
        rating,
        cover_image,
        banner_image,
        trailer_id,
        anime_metadata
    )
    VALUES (
        v_today_ist,
        p_anilist_id,
        left(trim(p_title), 300),
        p_rating,
        trim(p_cover_image),
        NULLIF(trim(COALESCE(p_banner_image, '')), ''),
        p_trailer_id,
        COALESCE(p_anime_metadata, '{}'::jsonb)
    )
    ON CONFLICT (selection_date) DO NOTHING
    RETURNING * INTO v_inserted;

    IF v_inserted.id IS NULL THEN
        -- Another concurrent user won today's slot first; roll back our history row and return the winner
        DELETE FROM public.anime_history
        WHERE anilist_id = p_anilist_id AND selected_date = v_today_ist;

        SELECT * INTO v_existing
        FROM public.daily_anime
        WHERE selection_date = v_today_ist
        LIMIT 1;

        IF FOUND THEN
            RETURN NEXT v_existing;
        END IF;
        RETURN;
    END IF;

    RETURN NEXT v_inserted;
END;
$$;

REVOKE ALL ON FUNCTION public.claim_daily_anime_selection(
    INTEGER, TEXT, INTEGER, TEXT, TEXT, TEXT, JSONB
) FROM PUBLIC, anon;

GRANT EXECUTE ON FUNCTION public.claim_daily_anime_selection(
    INTEGER, TEXT, INTEGER, TEXT, TEXT, TEXT, JSONB
) TO authenticated, service_role;

-- 3.3 Read-only helper returning previously featured anime IDs (prevents repeats)
CREATE OR REPLACE FUNCTION public.get_featured_anime_ids()
RETURNS INTEGER[]
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
    SELECT COALESCE(ARRAY_AGG(anilist_id), ARRAY[]::INTEGER[])
    FROM public.anime_history;
$$;

REVOKE ALL ON FUNCTION public.get_featured_anime_ids() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_featured_anime_ids() TO anon, authenticated, service_role;

-- ----------------------------------------------------------------------------
-- 4. ROW LEVEL SECURITY (Zero-Trust Client Read-Only)
-- ----------------------------------------------------------------------------

ALTER TABLE public.daily_anime ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.anime_history ENABLE ROW LEVEL SECURITY;

-- Public/authenticated clients may ONLY read daily_anime (never direct insert/update/delete)
DROP POLICY IF EXISTS "daily_anime_read_all" ON public.daily_anime;
CREATE POLICY "daily_anime_read_all"
    ON public.daily_anime
    FOR SELECT
    TO anon, authenticated
    USING (true);

REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON public.daily_anime FROM anon, authenticated;
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT SELECT ON public.daily_anime TO anon, authenticated;

-- anime_history is strictly backend-only (no client policies granted)
REVOKE ALL ON public.anime_history FROM anon, authenticated;

-- Force PostgREST API schema cache to immediately recognize tables & RPCs
NOTIFY pgrst, 'reload schema';

-- ----------------------------------------------------------------------------
-- 5. DAILY CRON SCHEDULE (Supabase pg_cron + pg_net — 12:00 AM IST / 18:30 UTC)
-- ----------------------------------------------------------------------------
-- Run the snippet below ONCE in the Supabase SQL Editor after deploying the
-- `daily-random-anime` Edge Function and setting `CRON_SECRET` as an Edge
-- Function secret + storing your project URL and cron secret in Supabase Vault:
--
--   CREATE EXTENSION IF NOT EXISTS pg_cron;
--   CREATE EXTENSION IF NOT EXISTS pg_net;
--
--   SELECT vault.create_secret('https://<YOUR_PROJECT_REF>.supabase.co', 'project_url');
--   SELECT vault.create_secret('<YOUR_STRONG_RANDOM_CRON_SECRET>', 'daily_anime_cron_secret');
--
--   SELECT cron.schedule(
--     'daily-random-anime-job',
--     '30 18 * * *', -- Runs once daily at 18:30 UTC (exactly 12:00 AM midnight IST / Asia/Kolkata)
--     $$
--     SELECT net.http_post(
--       url := (SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = 'project_url')
--              || '/functions/v1/daily-random-anime',
--       headers := jsonb_build_object(
--         'Content-Type', 'application/json',
--         'x-cron-secret', (SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = 'daily_anime_cron_secret')
--       ),
--       body := jsonb_build_object('source', 'pg_cron_ist_midnight')
--     ) AS request_id;
--     $$
--   );

