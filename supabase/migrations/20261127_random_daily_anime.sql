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
-- 3. ATOMIC, IDEMPOTENT DAILY SELECTION RPC (service_role only)
-- ----------------------------------------------------------------------------

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

    -- Idempotency check: if today already has a selection, return it without changes
    SELECT * INTO v_existing
    FROM public.daily_anime
    WHERE selection_date = p_selection_date
    LIMIT 1;

    IF FOUND THEN
        RETURN NEXT v_existing;
        RETURN;
    END IF;

    -- Atomically record in anime_history (raises unique_violation if already featured)
    INSERT INTO public.anime_history (anilist_id, selected_date)
    VALUES (p_anilist_id, p_selection_date);

    -- Atomically insert into daily_anime (guards against concurrent runs on same UTC date)
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
-- Uses server UTC date ONLY so clients can never backdate, overwrite, or forge picks.
-- Once today's canonical row exists, all callers receive the exact same locked row.
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
    v_today_utc DATE := (NOW() AT TIME ZONE 'UTC')::DATE;
    v_existing public.daily_anime%ROWTYPE;
    v_inserted public.daily_anime%ROWTYPE;
BEGIN
    -- Always return today's canonical pick immediately if already selected
    SELECT * INTO v_existing
    FROM public.daily_anime
    WHERE selection_date = v_today_utc
    LIMIT 1;

    IF FOUND THEN
        RETURN NEXT v_existing;
        RETURN;
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

    -- Never repeat a previously featured anime
    IF EXISTS (SELECT 1 FROM public.anime_history WHERE anilist_id = p_anilist_id) THEN
        RAISE EXCEPTION 'anime has already been featured previously';
    END IF;

    INSERT INTO public.anime_history (anilist_id, selected_date)
    VALUES (p_anilist_id, v_today_utc)
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
        v_today_utc,
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
        WHERE anilist_id = p_anilist_id AND selected_date = v_today_utc;

        SELECT * INTO v_existing
        FROM public.daily_anime
        WHERE selection_date = v_today_utc
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
-- 5. DAILY CRON SCHEDULE (Supabase pg_cron + pg_net)
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
--     '5 0 * * *', -- Runs once daily at 00:05 UTC
--     $$
--     SELECT net.http_post(
--       url := (SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = 'project_url')
--              || '/functions/v1/daily-random-anime',
--       headers := jsonb_build_object(
--         'Content-Type', 'application/json',
--         'x-cron-secret', (SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = 'daily_anime_cron_secret')
--       ),
--       body := jsonb_build_object('source', 'pg_cron')
--     ) AS request_id;
--     $$
--   );
