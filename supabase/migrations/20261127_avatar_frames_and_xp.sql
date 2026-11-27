-- ============================================================================
-- ANIME UNIVERSE — Migration: Avatar Frames & 50 XP Unlock System
-- Date: 2026-11-27
-- Safe, idempotent, non-destructive forward migration.
-- ============================================================================

-- 1. ADD avatar_frame AND xp COLUMNS TO public.profiles
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS avatar_frame TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS xp INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'profiles_avatar_frame_valid_check'
          AND conrelid = 'public.profiles'::regclass
    ) THEN
        ALTER TABLE public.profiles
            ADD CONSTRAINT profiles_avatar_frame_valid_check
            CHECK (
                avatar_frame IS NULL OR avatar_frame IN (
                    'frame_1', 'frame_2', 'frame_3', 'frame_4', 'frame_5',
                    'frame_6', 'frame_7', 'frame_8', 'frame_9', 'frame_10'
                )
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'profiles_xp_non_negative_check'
          AND conrelid = 'public.profiles'::regclass
    ) THEN
        ALTER TABLE public.profiles
            ADD CONSTRAINT profiles_xp_non_negative_check
            CHECK (xp >= 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_profiles_avatar_frame
    ON public.profiles (avatar_frame)
    WHERE avatar_frame IS NOT NULL;

-- 2. HELPER FUNCTION: COMPUTE USER XP FROM UNIQUE PROGRESS ENTRIES
CREATE OR REPLACE FUNCTION public.compute_user_xp(p_user_id UUID)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_xp INTEGER := 0;
    v_has_community_notes BOOLEAN := FALSE;
BEGIN
    IF p_user_id IS NULL THEN
        RETURN 0;
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'community_notes'
    ) INTO v_has_community_notes;

    IF v_has_community_notes THEN
        EXECUTE $q$
            SELECT COUNT(DISTINCT anime_id)::INTEGER
            FROM (
                SELECT ua.anime_id
                FROM public.user_anime ua
                WHERE ua.user_id = $1
                  AND ua.personal_rating IS NOT NULL
                  AND ua.personal_rating > 0
                UNION
                SELECT cn.anime_id
                FROM public.community_notes cn
                WHERE cn.user_id = $1
                  AND cn.rating IS NOT NULL
                  AND cn.rating > 0
            ) combined
        $q$
        INTO v_xp
        USING p_user_id;
    ELSE
        SELECT COUNT(DISTINCT ua.anime_id)::INTEGER
        INTO v_xp
        FROM public.user_anime ua
        WHERE ua.user_id = p_user_id
          AND ua.personal_rating IS NOT NULL
          AND ua.personal_rating > 0;
    END IF;

    RETURN COALESCE(v_xp, 0);
END;
$$;

-- 3. BACKFILL XP & SYNC ANY EXISTING COMPOSITE avatar_key FRAMES
UPDATE public.profiles p
SET
    xp = GREATEST(COALESCE(p.xp, 0), public.compute_user_xp(p.id)),
    avatar_frame = COALESCE(
        p.avatar_frame,
        (regexp_match(lower(COALESCE(p.avatar_key, '')), '\|(frame_(?:10|[1-9]))$'))[1]
    );

-- 4. AUTO-REFRESH XP WHEN USER ACTIVITY CHANGES
CREATE OR REPLACE FUNCTION public.sync_profile_xp_trigger()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_target_user UUID;
BEGIN
    v_target_user := COALESCE(NEW.user_id, OLD.user_id);
    IF v_target_user IS NOT NULL THEN
        UPDATE public.profiles
        SET xp = GREATEST(COALESCE(xp, 0), public.compute_user_xp(v_target_user))
        WHERE id = v_target_user;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$;

DROP TRIGGER IF EXISTS trg_user_anime_sync_xp ON public.user_anime;
CREATE TRIGGER trg_user_anime_sync_xp
    AFTER INSERT OR UPDATE OR DELETE ON public.user_anime
    FOR EACH ROW
    EXECUTE FUNCTION public.sync_profile_xp_trigger();

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'community_notes'
    ) THEN
        DROP TRIGGER IF EXISTS trg_community_notes_sync_xp ON public.community_notes;
        CREATE TRIGGER trg_community_notes_sync_xp
            AFTER INSERT OR UPDATE OR DELETE ON public.community_notes
            FOR EACH ROW
            EXECUTE FUNCTION public.sync_profile_xp_trigger();
    END IF;
END $$;

-- 5. ENFORCE 50 XP REQUIREMENT & KEEP avatar_key / avatar_frame SYNCED FOR ALL USERS
-- Keeping avatar_key synced as "avatar_X|frame_Y" ensures EVERY existing RPC
-- (Community, Reviews, Live Chat, Replies, Mentions, Notifications) automatically
-- displays every user's equipped avatar frame to all other users!
CREATE OR REPLACE FUNCTION public.enforce_avatar_frame_xp_and_sync()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_base_avatar TEXT;
    v_key_frame TEXT;
    v_effective_frame TEXT;
    v_effective_xp INTEGER;
BEGIN
    v_base_avatar := COALESCE(
        (regexp_match(lower(COALESCE(NEW.avatar_key, '')), '(avatar_[1-9])'))[1],
        'avatar_1'
    );
    v_key_frame := (regexp_match(lower(COALESCE(NEW.avatar_key, '')), '\|(frame_(?:10|[1-9]))$'))[1];

    IF TG_OP = 'UPDATE' AND (NEW.avatar_frame IS DISTINCT FROM OLD.avatar_frame) THEN
        v_effective_frame := NULLIF(lower(trim(COALESCE(NEW.avatar_frame, ''))), '');
    ELSIF TG_OP = 'UPDATE' AND (NEW.avatar_key IS DISTINCT FROM OLD.avatar_key) THEN
        v_effective_frame := COALESCE(v_key_frame, NULLIF(lower(trim(COALESCE(NEW.avatar_frame, ''))), ''));
    ELSE
        v_effective_frame := COALESCE(
            NULLIF(lower(trim(COALESCE(NEW.avatar_frame, ''))), ''),
            v_key_frame
        );
    END IF;

    IF v_effective_frame IS NOT NULL THEN
        IF v_effective_frame NOT IN (
            'frame_1', 'frame_2', 'frame_3', 'frame_4', 'frame_5',
            'frame_6', 'frame_7', 'frame_8', 'frame_9', 'frame_10'
        ) THEN
            RAISE EXCEPTION 'Invalid avatar frame selection.';
        END IF;

        v_effective_xp := GREATEST(COALESCE(NEW.xp, 0), public.compute_user_xp(NEW.id));
        NEW.xp := v_effective_xp;

        IF v_effective_xp < 50 THEN
            RAISE EXCEPTION 'Requires at least 50 XP to unlock Avatar Frames.';
        END IF;

        NEW.avatar_frame := v_effective_frame;
        NEW.avatar_key := v_base_avatar || '|' || v_effective_frame;
    ELSE
        NEW.avatar_frame := NULL;
        NEW.avatar_key := v_base_avatar;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_profiles_avatar_frame_sync ON public.profiles;
CREATE TRIGGER trg_profiles_avatar_frame_sync
    BEFORE INSERT OR UPDATE OF avatar_key, avatar_frame, xp ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.enforce_avatar_frame_xp_and_sync();

-- 6. RPC: equip_avatar_frame(p_frame_key TEXT DEFAULT NULL)
-- Works across all 9 built-in avatars while enforcing 50 XP server-side.
CREATE OR REPLACE FUNCTION public.equip_avatar_frame(p_frame_key TEXT DEFAULT NULL)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_clean_frame TEXT := NULLIF(lower(trim(COALESCE(p_frame_key, ''))), '');
    v_xp INTEGER;
    v_avatar_key TEXT;
    v_avatar_frame TEXT;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in to equip an avatar frame.';
    END IF;

    IF v_clean_frame IS NOT NULL AND v_clean_frame NOT IN (
        'frame_1', 'frame_2', 'frame_3', 'frame_4', 'frame_5',
        'frame_6', 'frame_7', 'frame_8', 'frame_9', 'frame_10'
    ) THEN
        RAISE EXCEPTION 'Invalid avatar frame.';
    END IF;

    SELECT GREATEST(COALESCE(p.xp, 0), public.compute_user_xp(v_uid))
    INTO v_xp
    FROM public.profiles p
    WHERE p.id = v_uid;

    IF v_xp IS NULL THEN
        RAISE EXCEPTION 'Profile not found.';
    END IF;

    IF v_clean_frame IS NOT NULL AND v_xp < 50 THEN
        RAISE EXCEPTION 'Requires at least 50 XP to unlock Avatar Frames.';
    END IF;

    UPDATE public.profiles
    SET
        xp = v_xp,
        avatar_frame = v_clean_frame,
        updated_at = NOW()
    WHERE id = v_uid
    RETURNING avatar_key, avatar_frame, xp
    INTO v_avatar_key, v_avatar_frame, v_xp;

    RETURN jsonb_build_object(
        'avatar_key', v_avatar_key,
        'avatar_frame', v_avatar_frame,
        'xp', v_xp,
        'unlocked', (v_xp >= 50)
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.equip_avatar_frame(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.compute_user_xp(UUID) TO authenticated;

-- 7. ENSURE ALL AUTHENTICATED USERS CAN READ EVERY MEMBER'S PROFILE & FRAME
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'profiles'
          AND policyname = 'profiles_readable_by_authenticated_users'
    ) THEN
        CREATE POLICY profiles_readable_by_authenticated_users
            ON public.profiles
            FOR SELECT
            TO authenticated
            USING (TRUE);
    END IF;
END $$;

-- 8. ENABLE REALTIME FOR PROFILES SO FRAME UPDATES PROPAGATE CLEANLY
ALTER TABLE public.profiles REPLICA IDENTITY FULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        BEGIN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.profiles;
        EXCEPTION WHEN duplicate_object THEN
            NULL;
        END;
    END IF;
END $$;
