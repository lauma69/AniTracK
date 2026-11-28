// ============================================================================
// ANIME UNIVERSE — 1:1 ANDROID COMPOSE UI & SUPABASE/ANILIST PARITY (WEB & PWA)
// Exact visual & functional match to the 5 user screenshots + auto cloud sync
// ============================================================================

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  });
}

let deferredInstallPrompt = null;
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredInstallPrompt = e;
  renderApp();
});

// Hardened 4-Shard Bit-Rotated Vault Material (Matches Android & Desktop VaultMaterial)
const V_U_SHARDS = [
  [122, 155, 146, 165, 244, 26, 51, 60, 154, 168],
  [240, 60, 74, 204, 143, 231, 223, 175, 178, 32],
  [85, 29, 14, 44, 1, 104, 158, 164, 175, 109],
  [134, 93, 99, 51, 141, 35, 252, 172, 23, 201]
];
const V_U_KEY = [5, 155, 136, 193, 243, 21, 119, 100, 170, 218, 252, 131, 133, 15, 25, 82, 120, 148, 226, 71, 211, 148, 250, 25, 196, 147, 156, 171, 148, 191, 75, 151];
const V_K_SHARDS = [
  [34, 4, 67, 148, 18, 93, 205, 57, 54, 74, 16, 215],
  [186, 113, 144, 138, 96, 215, 143, 229, 30, 194, 74, 121],
  [53, 207, 73, 120, 33, 171, 232, 142, 185, 22, 216],
  [207, 192, 33, 75, 28, 172, 50, 117, 66, 179, 98]
];
const V_K_KEY = [182, 155, 35, 209, 181, 196, 21, 96, 79, 21, 7, 30, 198, 11, 157, 31, 10, 197, 46, 182, 196, 15, 183, 73, 169, 140, 192, 144, 141, 134, 144, 148];

function unsealVaultV2(shards, keyStream, len, salt) {
  const out = new Uint8Array(len);
  const idx = [0, 0, 0, 0];
  for (let i = 0; i < len; i++) {
    const lane = i & 3;
    const obf = shards[lane][idx[lane]++];
    const rot = (i % 7) + 1;
    const unrot = ((obf >>> rot) | ((obf << (8 - rot)) & 0xff)) & 0xff;
    const k1 = keyStream[i % keyStream.length];
    const k2 = (i * 131 + salt * 37 + 0x5d) & 0xff;
    out[i] = (unrot ^ k1 ^ k2) & 0xff;
  }
  return new TextDecoder().decode(out);
}

function getVaultBaseUrl() {
  return unsealVaultV2(V_U_SHARDS, V_U_KEY, 40, 0x37).replace(/\/+$/, '');
}

function getVaultPublicKey() {
  return unsealVaultV2(V_K_SHARDS, V_K_KEY, 46, 0x6b).trim();
}

const BUILT_IN_AVATARS = [
  { key: 'avatar_1', name: 'Crimson Wolf', img: '/assets/avatar_1.png', c1: '#9F1239', c2: '#334155' },
  { key: 'avatar_2', name: 'Azure Sovereign', img: '/assets/avatar_2.png', c1: '#0284C7', c2: '#6366F1' },
  { key: 'avatar_3', name: 'Amethyst Dragon', img: '/assets/avatar_3.png', c1: '#A855F7', c2: '#E11D48' },
  { key: 'avatar_4', name: 'Frost Empress', img: '/assets/avatar_4.png', c1: '#38BDF8', c2: '#818CF8' },
  { key: 'avatar_5', name: 'Emerald Flora', img: '/assets/avatar_5.png', c1: '#10B981', c2: '#38BDF8' },
  { key: 'avatar_6', name: 'Amber Scholar', img: '/assets/avatar_6.png', c1: '#F59E0B', c2: '#92400E' },
  { key: 'avatar_7', name: 'Autumn Vanguard', img: '/assets/avatar_7.png', c1: '#EA580C', c2: '#06B6D4' },
  { key: 'avatar_8', name: 'Indigo Flame', img: '/assets/avatar_8.png', c1: '#4F46E5', c2: '#0EA5E9' },
  { key: 'avatar_9', name: 'Solar Halo', img: '/assets/avatar_9.png', c1: '#FBBF24', c2: '#64748B' }
];

const AVATAR_FRAME_UNLOCK_XP = 50;

const BUILT_IN_AVATAR_FRAMES = [
  { key: 'frame_1', name: 'Moonlit Deer', img: '/assets/frames/frame_1.png', accent: '#7DD3FC' },
  { key: 'frame_2', name: 'Sakura Kitsune', img: '/assets/frames/frame_2.png', accent: '#F472B6' },
  { key: 'frame_3', name: 'Frost Dew', img: '/assets/frames/frame_3.png', accent: '#93C5FD' },
  { key: 'frame_4', name: 'Sakura Crescent', img: '/assets/frames/frame_4.png', accent: '#FBCFE8' },
  { key: 'frame_5', name: 'Obsidian Dragon', img: '/assets/frames/frame_5.png', accent: '#C084FC' },
  { key: 'frame_6', name: 'Crimson Lantern', img: '/assets/frames/frame_6.png', accent: '#F87171' },
  { key: 'frame_7', name: 'Spectral Lantern', img: '/assets/frames/frame_7.png', accent: '#60A5FA' },
  { key: 'frame_8', name: 'Crimson Eclipse', img: '/assets/frames/frame_8.png', accent: '#EF4444' },
  { key: 'frame_9', name: 'Astral Cube', img: '/assets/frames/frame_9.png', accent: '#818CF8' },
  { key: 'frame_10', name: 'Lunar Seraph', img: '/assets/frames/frame_10.png', accent: '#BAE6FD' }
];

const SANDRONE_IMAGE_SRC = '/assets/sandrone/sandrone.png';
const SANDRONE_VOICE_CLIPS = [
  '/assets/sandrone/s.mp3',
  '/assets/sandrone/s1.mp3',
  '/assets/sandrone/s2.mp3',
  '/assets/sandrone/s3.mp3',
  '/assets/sandrone/s4.mp3',
  '/assets/sandrone/s5.mp3',
  '/assets/sandrone/s6.mp3',
  '/assets/sandrone/s7.mp3',
  '/assets/sandrone/s8.mp3',
  '/assets/sandrone/s9.mp3'
];

let sandroneAudio = null;
let sandroneLastClipIndex = -1;
let sandroneIsPlaying = false;
let sandronePlayToken = 0;

function isTauriAppEnvironment() {
  if (typeof window === 'undefined') return false;
  try {
    const params = new URLSearchParams(window.location.search || '');
    if (params.get('tauri') === '1') {
      sessionStorage.setItem('au_tauri_app', '1');
      return true;
    }
    if (sessionStorage.getItem('au_tauri_app') === '1') {
      return true;
    }
  } catch (_) {}
  if (window.__TAURI_INTERNALS__ || window.__TAURI__ || window.__ANIME_UNIVERSE_TAURI__) {
    return true;
  }
  if (window.ipc && typeof window.ipc.postMessage === 'function') {
    return true;
  }
  if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.ipc) {
    return true;
  }
  if (window.chrome && window.chrome.webview) {
    return true;
  }
  const proto = String(window.location?.protocol || '').toLowerCase();
  const host = String(window.location?.hostname || '').toLowerCase();
  if (proto === 'tauri:' || host === 'tauri.localhost' || host.endsWith('.tauri.localhost')) {
    return true;
  }
  const ua = String(navigator.userAgent || '');
  if (/\bTauri\b/i.test(ua)) {
    return true;
  }
  if (/\bAndroid\b/i.test(ua) && /\bwv\b/i.test(ua)) {
    return true;
  }
  return false;
}

function isPcSandroneEnvironment() {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') return false;
  const ua = String(navigator.userAgent || '');
  // Never show Sandrone on Android (mobile browser or APK WebView), iPhone, iPad, iPod, or any mobile device
  if (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile|Silk|Kindle/i.test(ua)) {
    return false;
  }
  if (navigator.userAgentData && navigator.userAgentData.mobile === true) {
    return false;
  }
  // Touch-primary / coarse-pointer handheld devices are excluded even if "Request Desktop Site" is enabled
  if (typeof window.matchMedia === 'function') {
    if (window.matchMedia('(hover: none), (pointer: coarse)').matches) {
      return false;
    }
    if (window.matchMedia('(max-width: 899px)').matches) {
      return false;
    }
  }
  return true;
}

function syncSandronePlayingUI() {
  document.querySelectorAll('[data-sandrone-mascot]').forEach((el) => {
    el.classList.toggle('is-playing', sandroneIsPlaying);
  });
}

function playRandomSandroneVoice(triggerEl) {
  if (!isPcSandroneEnvironment()) return;
  if (!SANDRONE_VOICE_CLIPS.length) return;
  let nextIdx = Math.floor(Math.random() * SANDRONE_VOICE_CLIPS.length);
  if (SANDRONE_VOICE_CLIPS.length > 1 && nextIdx === sandroneLastClipIndex) {
    nextIdx = (nextIdx + 1 + Math.floor(Math.random() * (SANDRONE_VOICE_CLIPS.length - 1))) % SANDRONE_VOICE_CLIPS.length;
  }
  sandroneLastClipIndex = nextIdx;

  if (triggerEl) {
    triggerEl.classList.remove('is-tapped');
    void triggerEl.offsetWidth;
    triggerEl.classList.add('is-tapped');
    setTimeout(() => triggerEl.classList.remove('is-tapped'), 170);
  }

  const token = ++sandronePlayToken;
  if (sandroneAudio) {
    try {
      sandroneAudio.pause();
      sandroneAudio.currentTime = 0;
    } catch (_) {}
  }

  const audio = new Audio(SANDRONE_VOICE_CLIPS[nextIdx]);
  audio.preload = 'auto';
  sandroneAudio = audio;
  sandroneIsPlaying = true;
  syncSandronePlayingUI();

  const handleFinish = () => {
    if (token !== sandronePlayToken) return;
    sandroneIsPlaying = false;
    syncSandronePlayingUI();
  };
  audio.addEventListener('ended', handleFinish);
  audio.addEventListener('error', handleFinish);

  const playPromise = audio.play();
  if (playPromise && typeof playPromise.catch === 'function') {
    playPromise.catch(() => {
      handleFinish();
    });
  }
}

const SENTIMENTS = [
  { key: 'cinema', shortLabel: 'Cinema', fullLabel: 'Absolute Cinema', minScore100: 90, suggestedStars: 4.5 },
  { key: 'worth', shortLabel: 'Worth', fullLabel: 'Worth Watching', minScore100: 75, suggestedStars: 4.0 },
  { key: 'avg', shortLabel: 'Avg', fullLabel: 'Average', minScore100: 55, suggestedStars: 3.0 },
  { key: 'boring', shortLabel: 'Boring', fullLabel: 'Boring', minScore100: 35, suggestedStars: 2.0 },
  { key: 'waste', shortLabel: 'Waste', fullLabel: 'Waste of Time', minScore100: 1, suggestedStars: 1.0 }
];

function roundStarsTenth(raw) {
  const n = Number(raw);
  if (!Number.isFinite(n) || n <= 0) return 0;
  return Math.round(Math.min(5.0, Math.max(0.1, n)) * 10) / 10;
}

let activeRatingSliderDragCount = 0;
let deferredRenderAfterSliderDrag = false;
let commQuickRateAutoSaveTimer = null;
let detailRatingAutoSaveTimer = null;
let detailExplicitSaveSeq = 0;
let detailUserTouchedRating = false;
let detailUserTouchedNote = false;

function clearCommQuickRateAutoSave() {
  if (commQuickRateAutoSaveTimer) {
    clearTimeout(commQuickRateAutoSaveTimer);
    commQuickRateAutoSaveTimer = null;
  }
}

function clearDetailRatingAutoSave() {
  if (detailRatingAutoSaveTimer) {
    clearTimeout(detailRatingAutoSaveTimer);
    detailRatingAutoSaveTimer = null;
  }
}

function starSliderPct(raw) {
  const n = Number(raw);
  const clamped = Number.isFinite(n) ? Math.min(5.0, Math.max(0.1, n)) : 0.1;
  return `${(((clamped - 0.1) / 4.9) * 100).toFixed(2)}%`;
}

function updateSmoothSliderScaleMarks(sliderEl, stars) {
  const wrap = sliderEl?.closest('.smooth-rating-slider-wrap');
  if (!wrap) return;
  wrap.querySelectorAll('[data-scale-val]').forEach((sp) => {
    const v = Number(sp.getAttribute('data-scale-val'));
    sp.classList.toggle('lit', stars >= v - 0.05);
  });
}

function animateSmoothSliderToStars(sliderEl, nextStars) {
  if (!sliderEl) return;
  const clean = roundStarsTenth(nextStars) || 0.1;
  sliderEl.classList.add('is-animating');
  sliderEl.style.setProperty('--slider-pct', starSliderPct(clean));
  sliderEl.setAttribute('data-slider-val', clean.toFixed(1));
  sliderEl.setAttribute('aria-valuenow', clean.toFixed(1));
  const bubble = sliderEl.querySelector('.smooth-rating-slider-bubble');
  if (bubble) bubble.textContent = `${clean.toFixed(1)} ★`;
  updateSmoothSliderScaleMarks(sliderEl, clean);
  clearTimeout(sliderEl.__animTimer);
  sliderEl.__animTimer = setTimeout(() => {
    sliderEl.classList.remove('is-animating');
  }, 260);
}

function renderSmoothRatingSliderHtml({ id, value = 4.0, large = false, showScale = true, ariaLabel = 'Star rating slider' }) {
  const numVal = Number(value);
  const safeVal = Number.isFinite(numVal) && numVal > 0 ? Math.min(5.0, Math.max(0.1, numVal)) : 0.1;
  const pct = starSliderPct(safeVal);
  const scaleMarks = [
    { v: 0.1, label: '0.1 ★' },
    { v: 1, label: '1' },
    { v: 2, label: '2' },
    { v: 3, label: '3' },
    { v: 4, label: '4' },
    { v: 5, label: '5.0 ★' }
  ];
  return `
    <div class="smooth-rating-slider-wrap">
      <div
        id="${id}"
        class="smooth-rating-slider${large ? ' lg' : ''}"
        role="slider"
        tabindex="0"
        aria-label="${ariaLabel}"
        aria-valuemin="0.1"
        aria-valuemax="5.0"
        aria-valuenow="${safeVal.toFixed(1)}"
        data-slider-val="${safeVal.toFixed(2)}"
        style="--slider-pct:${pct};"
      >
        <div class="smooth-rating-slider-track">
          <div class="smooth-rating-slider-fill"></div>
          <div class="smooth-rating-slider-thumb">
            <div class="smooth-rating-slider-bubble">${safeVal.toFixed(1)} ★</div>
          </div>
        </div>
      </div>
      ${
        showScale
          ? `<div class="smooth-rating-slider-scale">${scaleMarks
              .map((m) => `<span data-scale-val="${m.v}" class="${safeVal >= m.v - 0.05 ? 'lit' : ''}">${m.label}</span>`)
              .join('')}</div>`
          : ''
      }
    </div>
  `;
}

function bindSmoothRatingSlider(sliderEl, { stopProp = false, onDragStart, onLive, onCommit } = {}) {
  if (!sliderEl) return;
  const trackEl = sliderEl.querySelector('.smooth-rating-slider-track') || sliderEl;
  const bubbleEl = sliderEl.querySelector('.smooth-rating-slider-bubble');
  let dragging = false;
  let activePointerId = null;
  let activeTouchId = null;
  let lastStars = roundStarsTenth(sliderEl.getAttribute('data-slider-val') || 0.1) || 0.1;
  let lastRaw = Number(sliderEl.getAttribute('data-slider-val')) || lastStars;
  let targetRatio = Math.min(1, Math.max(0, (lastRaw - 0.1) / 4.9));
  let displayedRatio = targetRatio;
  let rafId = null;

  const paintRatio = (ratio) => {
    const pct = `${(Math.min(1, Math.max(0, ratio)) * 100).toFixed(2)}%`;
    sliderEl.style.setProperty('--slider-pct', pct);
  };

  const stepAnimation = () => {
    const diff = targetRatio - displayedRatio;
    if (Math.abs(diff) < 0.0012) {
      displayedRatio = targetRatio;
      paintRatio(displayedRatio);
      rafId = null;
      return;
    }
    displayedRatio += diff * 0.42;
    paintRatio(displayedRatio);
    rafId = requestAnimationFrame(stepAnimation);
  };

  const applyClientX = (clientX, immediate = false) => {
    const rect = trackEl.getBoundingClientRect();
    if (!rect || rect.width <= 0) return;
    const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
    const raw = 0.1 + ratio * 4.9;
    const stars = roundStarsTenth(raw);
    lastRaw = raw;
    lastStars = stars;
    targetRatio = ratio;
    if (immediate) {
      displayedRatio = ratio;
      paintRatio(ratio);
      if (rafId) {
        cancelAnimationFrame(rafId);
        rafId = null;
      }
    } else if (!rafId) {
      rafId = requestAnimationFrame(stepAnimation);
    }
    sliderEl.setAttribute('data-slider-val', raw.toFixed(2));
    sliderEl.setAttribute('aria-valuenow', stars.toFixed(1));
    if (bubbleEl) bubbleEl.textContent = `${stars.toFixed(1)} ★`;
    updateSmoothSliderScaleMarks(sliderEl, stars);
    if (onLive) onLive(stars, raw);
  };

  const cleanupWindowListeners = () => {
    window.removeEventListener('pointermove', onPointerMove, true);
    window.removeEventListener('pointerup', onPointerEnd, true);
    window.removeEventListener('pointercancel', onPointerEnd, true);
    window.removeEventListener('touchmove', onTouchMove, { capture: true });
    window.removeEventListener('touchend', onTouchEnd, true);
    window.removeEventListener('touchcancel', onTouchEnd, true);
    window.removeEventListener('blur', onBlurEnd);
  };

  const finishDrag = () => {
    if (!dragging) return;
    dragging = false;
    const pid = activePointerId;
    activePointerId = null;
    activeTouchId = null;
    cleanupWindowListeners();
    if (rafId) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
    sliderEl.classList.remove('is-dragging');
    if (pid !== null && sliderEl.releasePointerCapture) {
      try { sliderEl.releasePointerCapture(pid); } catch (_) {}
    }
    const finalStars = roundStarsTenth(lastRaw) || lastStars || 0.1;
    lastStars = finalStars;
    lastRaw = finalStars;
    const snappedRatio = Math.min(1, Math.max(0, (finalStars - 0.1) / 4.9));
    targetRatio = snappedRatio;
    displayedRatio = snappedRatio;
    sliderEl.classList.add('is-animating');
    sliderEl.style.setProperty('--slider-pct', starSliderPct(finalStars));
    sliderEl.setAttribute('data-slider-val', finalStars.toFixed(1));
    sliderEl.setAttribute('aria-valuenow', finalStars.toFixed(1));
    if (bubbleEl) bubbleEl.textContent = `${finalStars.toFixed(1)} ★`;
    updateSmoothSliderScaleMarks(sliderEl, finalStars);
    clearTimeout(sliderEl.__animTimer);
    sliderEl.__animTimer = setTimeout(() => {
      sliderEl.classList.remove('is-animating');
    }, 240);
    activeRatingSliderDragCount = Math.max(0, activeRatingSliderDragCount - 1);
    const hadDeferred = deferredRenderAfterSliderDrag;
    deferredRenderAfterSliderDrag = false;
    if (onCommit) {
      onCommit(finalStars);
    } else if (hadDeferred && activeRatingSliderDragCount === 0) {
      renderApp();
    }
  };

  const onPointerMove = (e) => {
    if (!dragging) return;
    if (activePointerId !== null && e.pointerId !== activePointerId) return;
    if (e.cancelable) e.preventDefault();
    if (stopProp) e.stopPropagation();
    applyClientX(e.clientX, false);
  };

  const onPointerEnd = (e) => {
    if (!dragging) return;
    if (activePointerId !== null && e.pointerId !== activePointerId) return;
    if (stopProp) e.stopPropagation();
    finishDrag();
  };

  const onTouchMove = (e) => {
    if (!dragging) return;
    if (e.cancelable) e.preventDefault();
    if (stopProp) e.stopPropagation();
    if (activeTouchId !== null && e.touches) {
      for (let i = 0; i < e.touches.length; i++) {
        if (e.touches[i].identifier === activeTouchId) {
          applyClientX(e.touches[i].clientX, false);
          return;
        }
      }
    }
    if (e.touches && e.touches[0]) {
      applyClientX(e.touches[0].clientX, false);
    }
  };

  const onTouchEnd = (e) => {
    if (!dragging) return;
    if (activeTouchId !== null && e.touches) {
      for (let i = 0; i < e.touches.length; i++) {
        if (e.touches[i].identifier === activeTouchId) return;
      }
    }
    if (stopProp) e.stopPropagation();
    finishDrag();
  };

  const onBlurEnd = () => finishDrag();

  const beginDrag = (clientX, pointerId = null, touchId = null) => {
    if (dragging) return;
    dragging = true;
    activePointerId = pointerId;
    activeTouchId = touchId;
    activeRatingSliderDragCount += 1;
    sliderEl.classList.remove('is-animating');
    sliderEl.classList.add('is-dragging');
    if (onDragStart) onDragStart();
    // Sync displayedRatio with current DOM state before gliding smoothly to press point
    const curVal = Number(sliderEl.getAttribute('data-slider-val')) || lastRaw;
    displayedRatio = Math.min(1, Math.max(0, (curVal - 0.1) / 4.9));
    applyClientX(clientX, false);
    if (pointerId !== null) {
      if (sliderEl.setPointerCapture) {
        try { sliderEl.setPointerCapture(pointerId); } catch (_) {}
      }
      window.addEventListener('pointermove', onPointerMove, { capture: true, passive: false });
      window.addEventListener('pointerup', onPointerEnd, true);
      window.addEventListener('pointercancel', onPointerEnd, true);
      window.addEventListener('touchmove', onTouchMove, { capture: true, passive: false });
      window.addEventListener('touchend', onTouchEnd, true);
      window.addEventListener('touchcancel', onTouchEnd, true);
    } else {
      window.addEventListener('touchmove', onTouchMove, { capture: true, passive: false });
      window.addEventListener('touchend', onTouchEnd, true);
      window.addEventListener('touchcancel', onTouchEnd, true);
    }
    window.addEventListener('blur', onBlurEnd);
  };

  sliderEl.addEventListener('click', (e) => {
    e.stopPropagation();
  });

  sliderEl.addEventListener(
    'touchstart',
    (e) => {
      if (e.cancelable) e.preventDefault();
      e.stopPropagation();
      const t = e.changedTouches && e.changedTouches[0];
      if (!t) return;
      beginDrag(t.clientX, null, t.identifier);
    },
    { passive: false }
  );

  sliderEl.addEventListener('pointerdown', (e) => {
    if (e.pointerType === 'touch') return;
    if (e.button !== 0) return;
    if (e.cancelable) e.preventDefault();
    e.stopPropagation();
    beginDrag(e.clientX, e.pointerId, null);
  });

  sliderEl.addEventListener('keydown', (e) => {
    let delta = 0;
    if (e.key === 'ArrowRight' || e.key === 'ArrowUp') delta = 0.1;
    else if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') delta = -0.1;
    else if (e.key === 'Home') delta = -(lastStars - 0.1);
    else if (e.key === 'End') delta = 5.0 - lastStars;
    else return;
    e.preventDefault();
    e.stopPropagation();
    const curVal = roundStarsTenth(sliderEl.getAttribute('data-slider-val') || lastStars) || 0.1;
    const nextStars = roundStarsTenth(Math.min(5.0, Math.max(0.1, curVal + delta)));
    lastStars = nextStars;
    lastRaw = nextStars;
    targetRatio = Math.min(1, Math.max(0, (nextStars - 0.1) / 4.9));
    displayedRatio = targetRatio;
    animateSmoothSliderToStars(sliderEl, nextStars);
    if (onLive) onLive(nextStars, nextStars);
    if (onCommit) onCommit(nextStars);
  });
}

function starsToRating10(stars) {
  const clean = roundStarsTenth(stars);
  if (clean <= 0) return null;
  return Number((clean * 2).toFixed(1));
}

function clampRating10(rawRating) {
  if (rawRating === null || rawRating === undefined) return null;
  const num = Number(rawRating);
  if (!Number.isFinite(num) || num <= 0) return null;
  return Number(Math.min(10, Math.max(0.2, num)).toFixed(1));
}

function normalizeRating10(rawRating) {
  return clampRating10(rawRating);
}

function rating10ToStars(rawRating) {
  const r10 = normalizeRating10(rawRating);
  if (!r10) return null;
  return roundStarsTenth(r10 / 2);
}

function sentimentFromRating10(rating10) {
  const r10 = normalizeRating10(rating10);
  if (!r10 || r10 <= 0) return null;
  const s100 = Math.round(r10 * 10);
  return SENTIMENTS.find((s) => s100 >= s.minScore100) || SENTIMENTS[SENTIMENTS.length - 1];
}

const WATCH_STATUSES = [
  { dbValue: 'watching', label: 'Watching' },
  { dbValue: 'completed', label: 'Completed' },
  { dbValue: 'plan_to_watch', label: 'Plan to Watch' },
  { dbValue: 'on_hold', label: 'On Hold' },
  { dbValue: 'dropped', label: 'Dropped' }
];

const ANILIST_GENRES = [
  'Action', 'Adventure', 'Comedy', 'Drama', 'Fantasy',
  'Horror', 'Mystery', 'Psychological', 'Romance', 'Sci-Fi',
  'Slice of Life', 'Sports', 'Supernatural', 'Thriller'
];

const DAY_LABELS = [
  { dow: 1, short: 'MON', full: 'Monday' },
  { dow: 2, short: 'TUE', full: 'Tuesday' },
  { dow: 3, short: 'WED', full: 'Wednesday' },
  { dow: 4, short: 'THU', full: 'Thursday' },
  { dow: 5, short: 'FRI', full: 'Friday' },
  { dow: 6, short: 'SAT', full: 'Saturday' },
  { dow: 7, short: 'SUN', full: 'Sunday' }
];

const SVG_ICONS = {
  home: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>`,
  explore: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm2.19 12.19L6 18l3.81-8.19L18 6l-3.81 8.19z"/></svg>`,
  library: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z"/></svg>`,
  community: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>`,
  profile: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>`,
  search: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>`,
  settings: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>`,
  back: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>`,
  grid: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 6h4v4H4V6zm6 0h10v4H10V6zM4 14h4v4H4v-4zm6 0h10v4H10v-4z"/></svg>`,
  list: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 6h4v4H4V6zm6 0h10v4H10V6zM4 14h4v4H4v-4zm6 0h10v4H10v-4z"/></svg>`,
  tune: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"/></svg>`,
  collectionIcon: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H9V9h10v2zm-4 4H9v-2h6v2zm4-8H9V5h10v2z"/></svg>`,
  shield: `<svg viewBox="0 0 24 24" fill="currentColor" style="width:14px;height:14px;color:var(--success);"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/></svg>`,
  key: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12.65 10C11.83 7.67 9.61 6 7 6c-3.31 0-6 2.69-6 6s2.69 6 6 6c2.61 0 4.83-1.67 5.65-4H17v4h4v-4h2v-4H12.65zM7 14c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/></svg>`,
  eye: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>`,
  heartFilled: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>`,
  heartOutline: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z"/></svg>`,
  download: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>`,
  android: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M17.6 9.48l1.84-3.18c.16-.31.04-.69-.26-.85a.637.637 0 0 0-.83.22l-1.88 3.24a11.463 11.463 0 0 0-8.94 0L5.65 5.67a.643.643 0 0 0-.87-.2c-.28.18-.37.54-.22.83L6.4 9.48A10.78 10.78 0 0 0 1 18h22a10.78 10.78 0 0 0-5.4-8.52zM7 15.25a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5zm10 0a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5z"/></svg>`,
  windows: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 3h8.5v8.5H3V3zm9.5 0H21v8.5h-8.5V3zM3 12.5h8.5V21H3v-8.5zm9.5 0H21V21h-8.5v-8.5z"/></svg>`,
  apple: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83zM15.96 6.33c.67-.81 1.12-1.94 1-3.08-.96.04-2.13.64-2.82 1.45-.61.7-.115 1.86-.99 2.97 1.08.08 2.14-.53 2.81-1.34z"/></svg>`,
  clock: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>`,
  checkCircle: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>`,
  comment: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/></svg>`,
  bell: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>`
};

function getCurrentAnimeSeason() {
  const m = new Date().getMonth() + 1;
  if (m <= 3) return 'WINTER';
  if (m <= 6) return 'SPRING';
  if (m <= 9) return 'SUMMER';
  return 'FALL';
}

function getDayNumberForDow(dow) {
  const now = new Date();
  const todayDow = ((now.getDay() + 6) % 7) + 1; // 1=Mon..7=Sun
  const diff = dow - todayDow;
  const d = new Date(now);
  d.setDate(now.getDate() + diff);
  return String(d.getDate());
}

function formatReviewDate(isoStr) {
  if (!isoStr) return '';
  try {
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return String(isoStr).slice(0, 10);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  } catch (_) {
    return String(isoStr).slice(0, 10);
  }
}

// Cleanup legacy shared storage keys that previously caused cross-user/guest data leakage
try {
  localStorage.removeItem('anime_universe_local_vault');
  localStorage.removeItem('anime_universe_collections');
  localStorage.removeItem('anime_universe_profile');
} catch (_) {}

function generateUniqueGuestId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `guest_${crypto.randomUUID()}`;
  }
  return `guest_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`;
}

function getOrCreateGuestId() {
  let gid = localStorage.getItem('anime_universe_guest_id');
  if (!gid || !gid.startsWith('guest_')) {
    gid = generateUniqueGuestId();
    localStorage.setItem('anime_universe_guest_id', gid);
  }
  return gid;
}

function rotateGuestId() {
  const gid = generateUniqueGuestId();
  localStorage.setItem('anime_universe_guest_id', gid);
  return gid;
}

function getStorageKeyForIdentity(kind, sessionObj, isGuest, guestId) {
  if (sessionObj && sessionObj.userId) {
    return `anime_universe_${kind}_user_${sessionObj.userId}`;
  }
  if (isGuest) {
    const gid = guestId || getOrCreateGuestId();
    return `anime_universe_${kind}_guest_${gid}`;
  }
  return null;
}

function readScopedArray(kind, sessionObj, isGuest, guestId) {
  const key = getStorageKeyForIdentity(kind, sessionObj, isGuest, guestId);
  if (!key) return [];
  try {
    const parsed = JSON.parse(localStorage.getItem(key) || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch (_) {
    return [];
  }
}

function isPlaceholderOrEmailName(name) {
  const clean = String(name || '').trim();
  return !clean || clean.includes('@') || /^traveler_/i.test(clean);
}

function readScopedProfile(sessionObj, isGuest, guestId) {
  const key = getStorageKeyForIdentity('profile', sessionObj, isGuest, guestId);
  const safeSessionName = sessionObj && !isPlaceholderOrEmailName(sessionObj.username) ? sessionObj.username : 'Member';
  const defaultUsername = sessionObj
    ? safeSessionName
    : isGuest
    ? 'Guest Tracker'
    : 'Member';
  if (!key) {
    return {
      userId: null,
      username: defaultUsername,
      avatarKey: 'avatar_1',
      bio: '',
      memberStatus: isGuest ? 'guest' : 'active',
      isAdmin: false
    };
  }
  try {
    const parsed = JSON.parse(localStorage.getItem(key) || '{}') || {};
    return {
      userId: sessionObj ? sessionObj.userId : guestId,
      username: parsed.username || defaultUsername,
      avatarKey: parsed.avatarKey || 'avatar_1',
      xp: Number(parsed.xp || 0),
      bio: parsed.bio || '',
      memberStatus: parsed.memberStatus || (isGuest ? 'guest' : 'active'),
      isAdmin: Boolean(parsed.isAdmin)
    };
  } catch (_) {
    return {
      userId: sessionObj ? sessionObj.userId : guestId,
      username: defaultUsername,
      avatarKey: 'avatar_1',
      bio: '',
      memberStatus: isGuest ? 'guest' : 'active',
      isAdmin: false
    };
  }
}

// Persistent State (Strictly Scoped per User ID or Unique Guest ID)
const savedPrefs = JSON.parse(localStorage.getItem('anime_universe_prefs') || '{}');
const initialSession = JSON.parse(localStorage.getItem('anime_universe_session') || localStorage.getItem('auralog_session') || 'null');
const initialGuestMode = !initialSession && localStorage.getItem('anime_universe_guest') === 'true';
const initialGuestId = getOrCreateGuestId();

const state = {
  session: initialSession,
  isGuestMode: initialGuestMode,
  guestId: initialGuestId,
  tab: 'home', // 'home' | 'discover' | 'mylist' | 'community' | 'profile' | 'settings' | 'detail'
  previousTab: 'home',
  settingsCategory: null,

  prefs: {
    themeMode: savedPrefs.themeMode || 'DARK',
    accentTheme: savedPrefs.accentTheme || 'GOLD',
    customAccentHex: savedPrefs.customAccentHex || '#38BDF8',
    watchlistLayout: savedPrefs.watchlistLayout || 'GRID',
    titleLanguage: savedPrefs.titleLanguage || 'ENGLISH',
    showAdultContent: Boolean(savedPrefs.showAdultContent),
    reviewsVisibility: savedPrefs.reviewsVisibility || 'community'
  },

  // Auth State
  authMode: 'signin', // 'signin' | 'signup' | 'verify_otp' | 'forgot_otp' | 'onboard_username' | 'onboard_avatar'
  identifier: '',
  password: '',
  confirmPassword: '',
  showPassword: false,
  pendingVerifyEmail: '',
  pendingVerifyPassword: '',
  signupOtpCode: '',
  resetOtpCode: '',
  resetNewPassword: '',
  onboardUsername: '',
  onboardAvatarKey: 'avatar_1',
  authError: '',
  authInfo: '',
  loading: false,

  // Profile State (Isolated to active user or active guest)
  profile: readScopedProfile(initialSession, initialGuestMode, initialGuestId),
  showAvatarModal: false,
  showAvatarFramesModal: false,
  showProfileEditModal: false,

  // Home Schedule State (Strictly independent from Library & Community)
  homeAllWeekDays: true,
  selectedDay: ((new Date().getDay() + 6) % 7) + 1,
  homeTrackingIds: new Set(readScopedArray('home_ids', initialSession, initialGuestMode, initialGuestId).map(Number)),
  homeTrackingHydrated: false,

  // Library (MyList) State (Isolated to active user or active guest)
  vault: readScopedArray('vault', initialSession, initialGuestMode, initialGuestId),
  collections: readScopedArray('collections', initialSession, initialGuestMode, initialGuestId),
  myListStatusFilter: 'ALL',
  myListSentimentFilter: null,
  myListCollectionFilter: null,
  myListQuery: '',
  myListSort: 'RECENT',
  showCreateCollectionModal: false,

  // Discover State
  discover: [],
  discoverMode: 'popular',
  searchQuery: '',
  discoverFormat: 'ALL',
  discoverStatus: 'ALL',
  discoverGenre: null,
  discoverSort: 'POPULARITY_DESC',
  discoverPage: 1,
  discoverHasNextPage: true,
  discoverLoadingMore: false,
  discoverLastPageStartIndex: 0,
  discoverError: '',
  isSearching: false,

  // Community State
  communityReviews: [],
  communityViewTab: 'feed', // 'feed' | 'random' | 'top' | 'tagged'
  communitySentimentFilter: null,
  dailyRandom: {
    loading: false,
    error: '',
    data: null,
    fetchedDate: '',
    detailsExpanded: false,
    trailerPlaying: false
  },
  randomSectionCategory: 'daily_pick', // 'daily_pick' | 'my_shuffle'
  myShuffle: {
    loading: false,
    animating: false,
    animPhase: '', // '' | 'flip-out-left' | 'flip-out-right' | 'flip-in-left' | 'flip-in-right'
    swipeDir: 1,
    error: '',
    data: null,
    detailsExpanded: false,
    sequence: 0
  },
  topAnime: {
    source: 'all', // 'community' | 'all'
    limit: 10, // 10 | 25 | 50 | 100
    timeframe: 'all_time', // 'weekly' | 'monthly' | 'seasonal' | 'yearly' | 'all_time'
    loading: false,
    error: '',
    items: [],
    cacheKey: ''
  },
  communitySort: 'RECENT',
  communityUserFilterOpen: false,
  communitySelectedUsers: [], // up to 3 users: [{ key, userId, username, avatarKey }]
  communityUserSearchQuery: '',
  communityCommentsAnimeId: null,
  communityCommentsAnimeFallback: null, // fallback template metadata when opening older anime from Tagged You
  communityCommentsTab: 'discussion', // 'discussion' (Live Chat) | 'notes' (Reviews)
  pendingCommunityReplyUser: null, // { id, username, avatar_key }
  communityCommentDraftStars: 0,
  communityCommentDraftNote: '',
  communityNoteSaving: false,
  communityNoteFeedback: '',
  communityNoteError: '',
  editingCommunityReviewAnimeId: null,
  editingCommunityReviewStars: 0,
  editingCommunityReviewNote: '',
  editingCommunityReviewSaving: false,

  // Detail Rating & Note Feedback
  detailRatingSaving: false,
  detailRatingFeedback: '',
  detailRatingError: '',
  detailNoteDraft: '',
  lastRenderedTab: null,

  // Anime Discussion (Flat Live Chat, Custom Stickers, Inline Quotes & True Realtime)
  discussion: {
    animeId: null,
    comments: [],
    totalComments: 0,
    hasMore: false,
    loading: false,
    loadingMore: false,
    submitting: false,
    success: '',
    error: '',
    draft: '',
    commentTags: [], // [{ id, username, avatar_key }]
    activeReplyTarget: null, // { messageId, senderName, previewText, stickerCode, targetUserId, sourceType, rootCommentId }
    replyDraft: '',
    replyTags: [],
    editingCommentId: null,
    editingCommentDraft: '',
    editingReplyId: null,
    editingReplyCommentId: null,
    editingReplyThreadId: null,
    editingReplyDraft: '',
    mentionSuggestions: [],
    mentionActiveScope: null, // 'comment' | 'edit_comment'
    mentionActiveStart: -1,
    mentionActiveEnd: -1,
    highlightTarget: null, // { commentId, threadId, replyId }
    contextMenu: null, // { x, y, type: 'comment'|'reply'|'comm_note', commentId, sourceType, userId, username, previewText, stickerCode, isOwner }
    emojiTrayOpen: false
  },

  // Unseen Discussion Red Dot State (shown on Community tab & specific anime cards)
  notifications: {
    items: [],
    unseenAnime: [],
    unreadCount: 0,
    totalCount: 0,
    hasMore: false,
    loading: false,
    loadingMore: false,
    error: ''
  },
  pendingNotificationJump: null, // { animeId, commentId, threadId, replyId }

  // Modals, Context Menu, Inline Episode Editor & Detail View
  detailAnime: null,
  detailRich: null,
  detailLoading: false,
  quickEditAnime: null,
  contextMenu: null, // { animeId, x, y }
  collectionContextMenu: null, // { id, name, count, x, y }
  confirmRemoveHomeAnime: null, // { animeId, title }
  inlineEpEdit: null, // { animeId, scope: 'card'|'detail', draft, saving, error }
  toastMessage: '',
  toastError: false,
  communityQuickRate: null, // { animeId, stars, saving }
  inAppNotifBanner: null, // { id, title, body, senderName, senderAvatar, animeId, animeTitle, animeCover, commentId, threadId, replyId }
  osNotifPermission: typeof Notification !== 'undefined' ? Notification.permission : 'default',
  showFirstTimeNotifPopup: false,
  firstTimeNotifDeniedGuide: false,
  notifPopupRequesting: false,
  notificationSettings: {
    master_enabled: true,
    messages_enabled: true,
    updates_enabled: true,
    reminders_enabled: true,
    os_permission_status: typeof Notification !== 'undefined' ? Notification.permission : 'prompt',
    onboarding_dismissed: localStorage.getItem('au_notif_onboarding_dismissed_v1') === 'true',
    saving: false,
    loaded: false
  },
  deviceRegistration: {
    device_id: localStorage.getItem('au_persistent_device_id_v1') || '',
    platform: 'web',
    push_provider: 'tauri_local',
    push_token: null,
    permission_status: 'prompt',
    is_active: false,
    registered_at: null,
    syncing: false
  },

  // Download Center Configuration State (Web-only)
  downloadsConfig: {
    platforms: {
      android: {
        id: 'android',
        platformName: 'Android APK',
        appName: 'Anime Universe for Android',
        osSupported: 'Android 8.0 (Oreo) & above • ARM64 / Universal',
        fileName: 'AnimeUniverse-Android.apk',
        packageType: 'Android Package (.apk)',
        available: true,
        downloadUrl: 'https://drive.google.com/file/d/1PgQkk8PTjrJspjx5uOvCx3ZNPrwWyKSx/view?usp=drivesdk'
      },
      windows: {
        id: 'windows',
        platformName: 'Windows EXE',
        appName: 'Anime Universe for Windows',
        osSupported: 'Windows 10 & Windows 11 (64-bit x64)',
        fileName: 'AnimeUniverse-Windows-Installer.exe',
        packageType: 'Windows Installer (.exe)',
        available: true,
        downloadUrl: 'https://drive.google.com/file/d/1hNlNCL_K6-GjkktUfPNk_qY3Vjxqrckt/view?usp=drivesdk'
      },
      macos: {
        id: 'macos',
        platformName: 'macOS DMG',
        appName: 'Anime Universe for macOS',
        osSupported: 'macOS 12 Monterey & newer (Apple Silicon & Intel)',
        fileName: 'AnimeUniverse-macOS-Installer.dmg',
        packageType: 'macOS Disk Image (.dmg)',
        available: true,
        downloadUrl: 'https://drive.google.com/file/d/1IHvY4Tru4KpvEg_rcuD7BE6iu4EOai5R/view?usp=drivesdk'
      }
    }
  },
  downloadTasks: {}
};

const syncedCatalogHashes = new Map();

const WEB_ACCENT_PRESETS = [
  { id: 'GOLD', label: 'Champagne Gold', shortLabel: 'Gold', darkHex: '#D8B778', lightHex: '#9A670E' },
  { id: 'ROSE', label: 'Sakura Rose', shortLabel: 'Rose', darkHex: '#F48FB1', lightHex: '#AD2E75' },
  { id: 'EMERALD', label: 'Emerald Mint', shortLabel: 'Mint', darkHex: '#5CE098', lightHex: '#127A4E' },
  { id: 'INDIGO', label: 'Midnight Indigo', shortLabel: 'Indigo', darkHex: '#93A8FF', lightHex: '#4F3ED4' }
];
const WEB_CUSTOM_QUICK_HEXES = ['#38BDF8', '#F97316', '#A855F7', '#EF4444', '#14B8A6', '#EAB308'];

function normalizeWebAccentKey(raw) {
  const k = String(raw || 'GOLD').trim().toUpperCase();
  if (k === 'CHAMPAGNE_GOLD' || k === 'COSMIC_CYAN' || k === 'GOLD') return 'GOLD';
  if (k === 'SAKURA_PINK' || k === 'ROSE') return 'ROSE';
  if (k === 'EMERALD_MINT' || k === 'EMERALD') return 'EMERALD';
  if (k === 'INDIGO_VIOLET' || k === 'INDIGO') return 'INDIGO';
  if (k === 'CUSTOM' || k.startsWith('CUSTOM:')) return 'CUSTOM';
  return 'GOLD';
}

function sanitizeWebHexColor(raw) {
  const cleaned = String(raw || '').trim().replace(/^#/, '');
  const expanded =
    cleaned.length === 3
      ? cleaned.split('').map((c) => c + c).join('')
      : cleaned;
  return /^[0-9A-Fa-f]{6}$/.test(expanded) ? `#${expanded.toUpperCase()}` : null;
}

function hexToRgbObj(hex) {
  const safe = sanitizeWebHexColor(hex) || '#38BDF8';
  return {
    r: parseInt(safe.slice(1, 3), 16),
    g: parseInt(safe.slice(3, 5), 16),
    b: parseInt(safe.slice(5, 7), 16)
  };
}

function rgbToHexStr(r, g, b) {
  const toHex = (n) => Math.max(0, Math.min(255, Math.round(n))).toString(16).padStart(2, '0').toUpperCase();
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

function rgbToHslObj(r, g, b) {
  const rn = r / 255;
  const gn = g / 255;
  const bn = b / 255;
  const max = Math.max(rn, gn, bn);
  const min = Math.min(rn, gn, bn);
  const d = max - min;
  let h = 0;
  const l = (max + min) / 2;
  const s = d === 0 ? 0 : d / (1 - Math.abs(2 * l - 1));
  if (d !== 0) {
    if (max === rn) h = ((gn - bn) / d) % 6;
    else if (max === gn) h = (bn - rn) / d + 2;
    else h = (rn - gn) / d + 4;
    h *= 60;
    if (h < 0) h += 360;
  }
  return { h, s, l };
}

function hslToRgbObj(h, s, l) {
  const c = (1 - Math.abs(2 * l - 1)) * s;
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
  const m = l - c / 2;
  let r1 = 0, g1 = 0, b1 = 0;
  if (h < 60) { r1 = c; g1 = x; }
  else if (h < 120) { r1 = x; g1 = c; }
  else if (h < 180) { g1 = c; b1 = x; }
  else if (h < 240) { g1 = x; b1 = c; }
  else if (h < 300) { r1 = x; b1 = c; }
  else { r1 = c; b1 = x; }
  return {
    r: Math.round((r1 + m) * 255),
    g: Math.round((g1 + m) * 255),
    b: Math.round((b1 + m) * 255)
  };
}

function pickAccessibleOnAccentHex(r, g, b) {
  const lin = (v) => {
    const n = v / 255;
    return n <= 0.03928 ? n / 12.92 : Math.pow((n + 0.055) / 1.055, 2.4);
  };
  const lum = 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
  return lum > 0.36 ? '#12141A' : '#FFFFFF';
}

function computeCustomAccentTokens(hex, isDark) {
  const baseRgb = hexToRgbObj(hex);
  const { h, s, l } = rgbToHslObj(baseRgb.r, baseRgb.g, baseRgb.b);
  const sat = s < 0.08 ? s : Math.max(0.45, Math.min(0.92, s));
  const primaryL = isDark
    ? Math.max(0.64, Math.min(0.78, l < 0.56 ? 0.70 : l))
    : Math.max(0.32, Math.min(0.44, l > 0.48 ? 0.39 : l));
  const secondaryL = isDark ? Math.max(0.44, primaryL - 0.16) : Math.max(0.24, primaryL - 0.08);
  const highlightL = isDark ? Math.min(0.90, primaryL + 0.14) : Math.max(0.22, primaryL - 0.09);

  const pRgb = hslToRgbObj(h, sat, primaryL);
  const sRgb = hslToRgbObj(h, Math.min(0.92, sat + 0.04), secondaryL);
  const hRgb = hslToRgbObj(h, Math.min(0.95, sat + 0.02), highlightL);

  return {
    rgb: `${pRgb.r}, ${pRgb.g}, ${pRgb.b}`,
    primary: rgbToHexStr(pRgb.r, pRgb.g, pRgb.b),
    secondary: rgbToHexStr(sRgb.r, sRgb.g, sRgb.b),
    highlight: rgbToHexStr(hRgb.r, hRgb.g, hRgb.b),
    onAccent: pickAccessibleOnAccentHex(pRgb.r, pRgb.g, pRgb.b)
  };
}

function getEffectiveWebThemeMode() {
  const mode = state.prefs.themeMode || 'DARK';
  if (mode === 'LIGHT') return 'light';
  if (mode === 'SYSTEM') {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }
  return 'dark';
}

function getActiveAccentDisplayInfo() {
  const key = normalizeWebAccentKey(state.prefs.accentTheme);
  if (key === 'CUSTOM') {
    const hex = sanitizeWebHexColor(state.prefs.customAccentHex) || '#38BDF8';
    const darkTok = computeCustomAccentTokens(hex, true);
    const lightTok = computeCustomAccentTokens(hex, false);
    return {
      key: 'CUSTOM',
      label: `Custom (${hex})`,
      darkHex: darkTok.primary,
      lightHex: lightTok.primary,
      rawHex: hex
    };
  }
  const preset = WEB_ACCENT_PRESETS.find((p) => p.id === key) || WEB_ACCENT_PRESETS[0];
  return {
    key: preset.id,
    label: preset.label,
    darkHex: preset.darkHex,
    lightHex: preset.lightHex,
    rawHex: preset.darkHex
  };
}

function applyTheme() {
  const root = document.documentElement;
  const effective = getEffectiveWebThemeMode();
  const accentKey = normalizeWebAccentKey(state.prefs.accentTheme);
  state.prefs.accentTheme = accentKey;
  if (sanitizeWebHexColor(state.prefs.customAccentHex)) {
    state.prefs.customAccentHex = sanitizeWebHexColor(state.prefs.customAccentHex);
  } else {
    state.prefs.customAccentHex = '#38BDF8';
  }

  root.setAttribute('data-theme', effective);
  root.setAttribute('data-accent', accentKey);

  const customProps = ['--accent-rgb', '--accent-primary', '--accent-secondary', '--highlight', '--on-accent'];
  if (accentKey === 'CUSTOM') {
    const tok = computeCustomAccentTokens(state.prefs.customAccentHex, effective === 'dark');
    root.style.setProperty('--accent-rgb', tok.rgb);
    root.style.setProperty('--accent-primary', tok.primary);
    root.style.setProperty('--accent-secondary', tok.secondary);
    root.style.setProperty('--highlight', tok.highlight);
    root.style.setProperty('--on-accent', tok.onAccent);
  } else {
    customProps.forEach((prop) => root.style.removeProperty(prop));
  }

  const themeMeta = document.querySelector('meta[name="theme-color"]');
  if (themeMeta) {
    themeMeta.setAttribute('content', effective === 'light' ? '#F5F6FA' : '#090B10');
  }
}

applyTheme();

if (window.matchMedia) {
  const sysMedia = window.matchMedia('(prefers-color-scheme: light)');
  const onSysThemeChange = () => {
    if (state.prefs.themeMode === 'SYSTEM') {
      applyTheme();
      if (state.tab === 'settings' && state.settingsCategory === 'APPEARANCE') {
        renderApp();
      }
    }
  };
  if (typeof sysMedia.addEventListener === 'function') {
    sysMedia.addEventListener('change', onSysThemeChange);
  } else if (typeof sysMedia.addListener === 'function') {
    sysMedia.addListener(onSysThemeChange);
  }
}

function savePrefs() {
  localStorage.setItem('anime_universe_prefs', JSON.stringify(state.prefs));
  applyTheme();
}

function getActiveOwnerId() {
  if (state.session && state.session.userId) return state.session.userId;
  if (state.isGuestMode) return state.guestId || getOrCreateGuestId();
  return null;
}

function saveLocalVault() {
  const key = getStorageKeyForIdentity('vault', state.session, state.isGuestMode, state.guestId);
  if (!key) return;
  const ownerId = getActiveOwnerId();
  const tagged = state.vault.map((v) => ({ ...v, ownerId }));
  state.vault = tagged;
  localStorage.setItem(key, JSON.stringify(tagged));
}

function saveCollections() {
  const key = getStorageKeyForIdentity('collections', state.session, state.isGuestMode, state.guestId);
  if (!key) return;
  const ownerId = getActiveOwnerId();
  const tagged = state.collections.map((c) => ({ ...c, ownerId }));
  state.collections = tagged;
  localStorage.setItem(key, JSON.stringify(tagged));
}

function saveProfileCache() {
  const key = getStorageKeyForIdentity('profile', state.session, state.isGuestMode, state.guestId);
  if (!key) return;
  const ownerId = getActiveOwnerId();
  state.profile = { ...state.profile, userId: ownerId };
  localStorage.setItem(key, JSON.stringify(state.profile));
}

let toastTimer = null;
function showToast(message, isError = false) {
  state.toastMessage = String(message || '');
  state.toastError = Boolean(isError);
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    state.toastMessage = '';
    state.toastError = false;
    const existingToast = document.getElementById('au-toast-banner');
    if (existingToast) existingToast.remove();
  }, 3200);
}

// Strict Lifecycle & Episode Validation Rules (1:1 Parity with Android, Desktop & Supabase)
function isOfficiallyFinished(mediaStatus) {
  const s = String(mediaStatus || '').trim().toUpperCase();
  return s === 'FINISHED' || s === 'FINISHED_AIRING' || s === 'COMPLETED';
}

function shouldAutoComplete(mediaStatus, totalEpisodes, watchedEpisodes) {
  const total = totalEpisodes !== null && totalEpisodes !== undefined ? Number(totalEpisodes) : null;
  const watched = Number(watchedEpisodes || 0);
  return isOfficiallyFinished(mediaStatus) && total !== null && Number.isFinite(total) && total > 0 && watched >= total;
}

function shouldRemoveFromHome(userStatus, mediaStatus, totalEpisodes, watchedEpisodes) {
  if (String(userStatus || '').trim().toLowerCase() === 'dropped') return true;
  return shouldAutoComplete(mediaStatus, totalEpisodes, watchedEpisodes);
}

function isAnimeOnHome(item) {
  if (!item || !item.animeId) return false;
  if (!state.homeTrackingIds.has(Number(item.animeId))) return false;
  return !shouldRemoveFromHome(item.status, item.mediaStatus, item.totalEpisodes, item.watchedEpisodes);
}

function validateEpisodeInput(rawText, totalEpisodes) {
  const trimmed = String(rawText ?? '').trim();
  if (!trimmed) {
    return { valid: false, error: 'Enter an episode number.' };
  }
  if (trimmed.startsWith('-')) {
    return { valid: false, error: 'Episode number cannot be negative.' };
  }
  if (trimmed.includes('.') || trimmed.includes(',')) {
    return { valid: false, error: 'Enter a whole number only.' };
  }
  if (!/^\d+$/.test(trimmed)) {
    return { valid: false, error: 'Enter digits only.' };
  }
  const num = Number.parseInt(trimmed, 10);
  if (!Number.isFinite(num) || num < 0) {
    return { valid: false, error: 'Enter a valid non-negative number.' };
  }
  const total = totalEpisodes !== null && totalEpisodes !== undefined ? Number(totalEpisodes) : null;
  if (total !== null && Number.isFinite(total) && total > 0 && num > total) {
    return { valid: false, error: `Cannot exceed ${total} episodes.` };
  }
  if ((total === null || total <= 0) && num > 50000) {
    return { valid: false, error: 'Episode number is out of range.' };
  }
  return { valid: true, value: num };
}

function saveHomeTrackingIds() {
  const key = getStorageKeyForIdentity('home_ids', state.session, state.isGuestMode, state.guestId);
  if (!key) return;
  localStorage.setItem(key, JSON.stringify(Array.from(state.homeTrackingIds)));
}

function clearActiveUserMemoryState() {
  state.vault = [];
  state.collections = [];
  state.homeTrackingIds = new Set();
  state.homeTrackingHydrated = false;
  state.profile = {
    userId: null,
    username: 'Member',
    avatarKey: 'avatar_1',
    bio: '',
    memberStatus: 'active',
    isAdmin: false
  };
  state.detailAnime = null;
  state.detailRich = null;
  state.quickEditAnime = null;
  state.contextMenu = null;
  state.confirmRemoveHomeAnime = null;
  state.inlineEpEdit = null;
  state.myListStatusFilter = 'ALL';
  state.myListSentimentFilter = null;
  state.myListCollectionFilter = null;
  state.myListQuery = '';
}

function hydrateActiveIdentityState() {
  if (state.session && state.session.userId) {
    state.vault = readScopedArray('vault', state.session, false, null).filter(
      (v) => !v.ownerId || v.ownerId === state.session.userId
    );
    state.collections = readScopedArray('collections', state.session, false, null).filter(
      (c) => !c.ownerId || c.ownerId === state.session.userId
    );
    state.homeTrackingIds = new Set(readScopedArray('home_ids', state.session, false, null).map(Number));
    state.homeTrackingHydrated = false;
    state.profile = readScopedProfile(state.session, false, null);
    state.detailAnime = null;
    state.quickEditAnime = null;
    state.contextMenu = null;
    state.confirmRemoveHomeAnime = null;
    state.inlineEpEdit = null;
  } else if (state.isGuestMode) {
    state.guestId = getOrCreateGuestId();
    state.vault = readScopedArray('vault', null, true, state.guestId).filter(
      (v) => !v.ownerId || v.ownerId === state.guestId
    );
    state.collections = readScopedArray('collections', null, true, state.guestId).filter(
      (c) => !c.ownerId || c.ownerId === state.guestId
    );
    const savedHomeIds = readScopedArray('home_ids', null, true, state.guestId).map(Number);
    if (savedHomeIds.length > 0) {
      state.homeTrackingIds = new Set(savedHomeIds);
    } else {
      state.homeTrackingIds = new Set(
        state.vault
          .filter((v) => v.inHome !== false && !shouldRemoveFromHome(v.status, v.mediaStatus, v.totalEpisodes, v.watchedEpisodes))
          .map((v) => Number(v.animeId))
      );
    }
    state.homeTrackingHydrated = true;
    state.profile = readScopedProfile(null, true, state.guestId);
    state.detailAnime = null;
    state.quickEditAnime = null;
    state.contextMenu = null;
    state.confirmRemoveHomeAnime = null;
    state.inlineEpEdit = null;
  } else {
    clearActiveUserMemoryState();
  }
}

let pendingGuestMigrationItems = [];
let pendingGuestMigrationGuestId = null;

function saveSession(session) {
  if (session && session.userId) {
    // Capture any active guest session items for safe non-duplicating migration
    const activeGuestId = state.guestId || localStorage.getItem('anime_universe_guest_id');
    if (activeGuestId) {
      const guestItems = readScopedArray('vault', null, true, activeGuestId);
      if (guestItems.length > 0) {
        pendingGuestMigrationItems = guestItems.map((it) => ({ ...it }));
        pendingGuestMigrationGuestId = activeGuestId;
      }
    }
    state.session = session;
    localStorage.setItem('anime_universe_session', JSON.stringify(session));
    localStorage.setItem('auralog_session', JSON.stringify(session));
    state.isGuestMode = false;
    localStorage.removeItem('anime_universe_guest');
    hydrateActiveIdentityState();
    if (typeof NativeNotificationBridge !== 'undefined') {
      NativeNotificationBridge.syncAuthToTauri(session);
      NativeNotificationBridge.ensurePermissionSilently();
    }
  } else {
    state.session = null;
    state.isGuestMode = false;
    localStorage.removeItem('anime_universe_session');
    localStorage.removeItem('auralog_session');
    localStorage.removeItem('anime_universe_guest');
    if (typeof NotificationRealtimeManager !== 'undefined') NotificationRealtimeManager.disconnect();
    if (typeof NotificationSyncState !== 'undefined') NotificationSyncState.reset();
    if (typeof NativeNotificationBridge !== 'undefined') NativeNotificationBridge.clearAuthInTauri();
    clearActiveUserMemoryState();
  }
}

function enterGuestSession(forceNewGuestId = false) {
  state.session = null;
  localStorage.removeItem('anime_universe_session');
  localStorage.removeItem('auralog_session');
  if (typeof NotificationRealtimeManager !== 'undefined') NotificationRealtimeManager.disconnect();
  if (typeof NotificationSyncState !== 'undefined') NotificationSyncState.reset();
  if (typeof NativeNotificationBridge !== 'undefined') NativeNotificationBridge.clearAuthInTauri();
  state.guestId = forceNewGuestId ? rotateGuestId() : getOrCreateGuestId();
  state.isGuestMode = true;
  localStorage.setItem('anime_universe_guest', 'true');
  hydrateActiveIdentityState();
}

function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

const AVATAR_IMG_ONERROR =
  "if(!this.dataset.fb){this.dataset.fb='1';const s=this.getAttribute('src')||'';this.src=s.startsWith('/public/')?s.replace('/public/','/'):('/public'+s);}else{this.style.opacity='0';}";

function getMascotImgUrl() {
  if (typeof window !== 'undefined' && window.ANIME_UNIVERSE_ASSETS && window.ANIME_UNIVERSE_ASSETS.img_login_mascot) {
    return window.ANIME_UNIVERSE_ASSETS.img_login_mascot;
  }
  return '/assets/img_login_mascot.png';
}

function normalizeAvatarKey(rawKey, rawUrl = '') {
  const candidates = [rawKey, rawUrl].map((v) => String(v || '').trim()).filter((v) => v && v !== 'null');
  for (const c of candidates) {
    const basePart = c.split('|')[0].trim();
    const lower = basePart.toLowerCase();
    const exact = BUILT_IN_AVATARS.find((a) => a.key === lower || a.name.toLowerCase() === lower);
    if (exact) return exact.key;
    const m = lower.match(/avatar[_-]?([1-9])/) || lower.match(/^([1-9])$/);
    if (m && m[1]) return `avatar_${m[1]}`;
  }
  return 'avatar_1';
}

function extractAvatarFrameKey(rawKey, rawUrl = '', rawFrame = '') {
  const candidates = [rawFrame, rawKey, rawUrl]
    .map((v) => String(v || '').trim())
    .filter((v) => v && v !== 'null');
  for (const c of candidates) {
    const lower = c.toLowerCase();
    const m = lower.match(/(?:^|\||:)frame[_-]?(10|[1-9])(?:\b|$)/);
    if (m && m[1]) return `frame_${m[1]}`;
  }
  return '';
}

function composeAvatarKey(baseAvatarKey, frameKey = '') {
  const base = normalizeAvatarKey(baseAvatarKey);
  const frame = extractAvatarFrameKey(frameKey);
  return frame ? `${base}|${frame}` : base;
}

function normalizeAvatarCompositeKey(rawKey, rawUrl = '', rawFrame = '') {
  const base = normalizeAvatarKey(rawKey, rawUrl);
  const frame = extractAvatarFrameKey(rawKey, rawUrl, rawFrame);
  return frame ? `${base}|${frame}` : base;
}

function getAvatarFramePreset(avatarKey, avatarUrl = '', rawFrame = '') {
  const fKey = extractAvatarFrameKey(avatarKey, avatarUrl, rawFrame);
  if (!fKey) return null;
  return BUILT_IN_AVATAR_FRAMES.find((f) => f.key === fKey) || null;
}

function getUserXp() {
  const progressIds = new Set();
  if (Array.isArray(state.vault)) {
    state.vault.forEach((v) => {
      if ((Number(v?.rating10) || 0) > 0 && v?.animeId != null) {
        progressIds.add(Number(v.animeId));
      }
    });
  }
  if (state.session?.userId && Array.isArray(state.communityReviews)) {
    state.communityReviews.forEach((r) => {
      if (r?.user_id === state.session.userId && (Number(r?.rating) || 0) > 0 && r?.anime_id != null) {
        progressIds.add(Number(r.anime_id));
      }
    });
  }
  const serverXp = Math.max(0, Number(state.profile?.xp || 0));
  return Math.max(serverXp, progressIds.size);
}

function getAvatarPreset(avatarKey, avatarUrl = '') {
  const norm = normalizeAvatarKey(avatarKey, avatarUrl);
  const found = BUILT_IN_AVATARS.find((a) => a.key === norm) || BUILT_IN_AVATARS[0];
  const embedded =
    typeof window !== 'undefined' && window.ANIME_UNIVERSE_ASSETS && window.ANIME_UNIVERSE_ASSETS[found.key]
      ? window.ANIME_UNIVERSE_ASSETS[found.key]
      : found.img;
  return { ...found, img: embedded };
}

function getAvatarImgUrl(avatarKey, avatarUrl = '') {
  return getAvatarPreset(avatarKey, avatarUrl).img;
}

function renderAvatarCircle(avatarKey, username = 'A', sizePx = 28, avatarUrl = '', frameKeyOverride = undefined) {
  const av = getAvatarPreset(avatarKey, avatarUrl);
  const frame =
    frameKeyOverride !== undefined
      ? getAvatarFramePreset(frameKeyOverride)
      : getAvatarFramePreset(avatarKey, avatarUrl);
  const initial = String(username || 'A').trim().charAt(0).toUpperCase() || 'A';
  const frameSize = Math.round(sizePx * 1.38);
  const frameMargin = frame ? Math.max(2, Math.round(sizePx * 0.14)) : 0;
  return `
    <div class="avatar-framed-wrap${frame ? ' has-frame' : ''}" style="position:relative;width:${sizePx}px;height:${sizePx}px;flex-shrink:0;display:inline-flex;align-items:center;justify-content:center;overflow:visible;${frameMargin ? `margin:${frameMargin}px;` : ''}">
      <div style="position:relative;width:100%;height:100%;border-radius:50%;overflow:hidden;border:${frame ? '1px solid rgba(255,255,255,0.18)' : '1.5px solid rgba(255,255,255,0.32)'};background:linear-gradient(135deg,${av.c1},${av.c2});display:inline-flex;align-items:center;justify-content:center;">
        <span style="position:absolute;color:#FFF;font-size:${Math.max(10, Math.round(sizePx * 0.44))}px;font-weight:700;pointer-events:none;">${escapeHtml(initial)}</span>
        <img src="${av.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" style="position:relative;z-index:1;width:100%;height:100%;object-fit:cover;display:block;border-radius:50%;" />
      </div>
      ${
        frame
          ? `<img class="avatar-frame-overlay" src="${frame.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" style="width:${frameSize}px;height:${frameSize}px;" />`
          : ''
      }
    </div>
  `;
}

function maskEmailAddress(email) {
  const clean = String(email || '').trim();
  const atIdx = clean.indexOf('@');
  if (atIdx <= 0) return clean;
  const local = clean.slice(0, atIdx);
  const domain = clean.slice(atIdx + 1);
  const maskedLocal =
    local.length <= 2
      ? `${local.charAt(0)}*`
      : `${local.slice(0, 2)}${'*'.repeat(Math.min(5, Math.max(2, local.length - 3)))}${local.slice(-1)}`;
  return `${maskedLocal}@${domain}`;
}

// Refresh JWT Access Token if expired or on 401
let isRefreshingToken = false;
async function refreshSessionTokenIfNeeded(force = false) {
  if (!state.session || !state.session.refreshToken || isRefreshingToken) return false;
  const nowSec = Math.floor(Date.now() / 1000);
  if (!force && state.session.expiresAt && state.session.expiresAt > nowSec + 90) {
    return true;
  }
  isRefreshingToken = true;
  try {
    const baseUrl = getVaultBaseUrl();
    const anonKey = getVaultPublicKey();
    const resp = await fetch(`${baseUrl}/auth/v1/token?grant_type=refresh_token`, {
      method: 'POST',
      headers: {
        apikey: anonKey,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ refresh_token: state.session.refreshToken })
    });
    if (!resp.ok) return false;
    const data = await resp.json();
    if (data.access_token) {
      saveSession({
        ...state.session,
        accessToken: data.access_token,
        refreshToken: data.refresh_token || state.session.refreshToken,
        expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600)
      });
      return true;
    }
    return false;
  } catch (_) {
    return false;
  } finally {
    isRefreshingToken = false;
  }
}

// Supabase Request Helper (with automatic token refresh on 401)
async function supabaseRequest(path, method = 'GET', body = undefined, prefer = '', allowRetry = true) {
  await refreshSessionTokenIfNeeded(false);
  const accessToken = state.session?.accessToken || '';
  const baseUrl = getVaultBaseUrl();
  const anonKey = getVaultPublicKey();
  const headers = {
    apikey: anonKey,
    Authorization: `Bearer ${accessToken || anonKey}`,
    'Content-Type': 'application/json'
  };
  if (prefer) headers.Prefer = prefer;
  const resp = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  });
  if (resp.status === 401 && allowRetry && state.session?.refreshToken) {
    const ok = await refreshSessionTokenIfNeeded(true);
    if (ok) {
      return supabaseRequest(path, method, body, prefer, false);
    }
  }
  const text = await resp.text();
  let data = {};
  try { data = JSON.parse(text); } catch (_) { data = text; }
  if (!resp.ok) {
    throw new Error((data && (data.msg || data.error_description || data.message || data.error)) || 'Cloud request failed');
  }
  return data;
}

// Resolve Email OR Username via Supabase RPC (matches Android SupabaseDatabaseService.resolveLoginEmail)
async function resolveLoginIdentifier(identifier) {
  const clean = String(identifier || '').trim().replace(/^@+/, '').trim();
  if (!clean) throw new Error('Enter your email address or username.');
  if (clean.includes('@') && clean.includes('.')) {
    return clean.toLowerCase();
  }
  const baseUrl = getVaultBaseUrl();
  const anonKey = getVaultPublicKey();
  const resp = await fetch(`${baseUrl}/rest/v1/rpc/resolve_login_email`, {
    method: 'POST',
    headers: {
      apikey: anonKey,
      Authorization: `Bearer ${anonKey}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ p_identifier: clean })
  });
  const text = (await resp.text()).trim().replace(/^"|"$/g, '');
  if (!resp.ok || !text || text === 'null' || !text.includes('@')) {
    throw new Error(`No account found matching "${clean}". Try your email address or Sign Up.`);
  }
  return text;
}

// ============================================================================
// ENGLISH & ALTERNATE ENGLISH TITLE MAPPER (DONGHUA + ANIME) & KEYWORD SEARCH
// ============================================================================

const TITLE_DICTIONARY_ENTRIES = [
  // Chinese Donghua (Pinyin -> Official & Alternate English Titles)
  ['Wanmei Shijie', 'Perfect World', ['Perfect World Donghua'], true],
  ['Xian Ni', 'Renegade Immortal', ['Immortal Renegade', 'Wang Lin'], true],
  ['Tunshi Xingkong', 'Swallowed Star', ['Swallow the Starry Sky', 'Devouring the Starry Sky'], true],
  ['Douluo Dalu 2: Jueshi Tangmen', 'Soul Land 2: The Peerless Tang Clan', ['Unrivaled Tang Sect', 'Soul Land II'], true],
  ['Douluo Dalu II', 'Soul Land 2: The Peerless Tang Clan', ['Soul Land 2', 'Peerless Tang Clan'], true],
  ['Douluo Dalu', 'Soul Land', ['Combat Continent', 'Douluo Continent'], true],
  ['Dou Po Cang Qiong', 'Battle Through the Heavens', ['Fights Break Sphere', 'Fight Break Sphere', 'BTTH'], true],
  ['Doupo Cangqiong', 'Battle Through the Heavens', ['Fights Break Sphere', 'BTTH'], true],
  ['Fan Ren Xiu Xian Chuan', "A Record of a Mortal's Journey to Immortality", ["Mortal's Journey to Immortality", "Record of a Mortal's Journey to Immortality", 'RMJI'], true],
  ['Fanren Xiuxian Zhuan', "A Record of a Mortal's Journey to Immortality", ["Mortal's Journey to Immortality"], true],
  ['Zhe Tian', 'Shrouding the Heavens', ['Shroud the Heavens', 'Covering the Sky'], true],
  ['Shen Yin Wang Zuo', 'Throne of Seal', ['Sealed Divine Throne', 'Divine Throne'], true],
  ['Shenyin Wangzuo', 'Throne of Seal', ['Sealed Divine Throne'], true],
  ['Mu Shen Ji', 'Tales of Herding Gods', ['Herding Gods', 'Record of Herding Gods'], true],
  ['Jian Lai', 'Sword of Coming', ['Sword Come', 'Unsheathed'], true],
  ['Gui Mi Zhi Zhu', 'Lord of the Mysteries', ['Lord of Mysteries', 'LOTM'], true],
  ['Guimi Zhi Zhu', 'Lord of the Mysteries', ['Lord of Mysteries'], true],
  ['Shi Xiong A Shi Xiong', 'Big Brother', ['My Senior Brother Is Too Steady', 'Senior Brother Is Too Steady'], true],
  ['Cang Yuan Tu', 'The Demon Hunter', ['Archean Eon Art'], true],
  ['Bai Lian Cheng Shen', 'Apotheosis', ['Elevation to the Status of a God', 'Refining into a God'], true],
  ['Wu Shen Zhu Zai', 'Martial Master', ['Dominant Martial God', 'God of Martial Arts'], true],
  ['Lian Qi Shi Wan Nian', 'One Hundred Thousand Years of Qi Refining', ['100,000 Years of Qi Refining', '100000 Years of Qi Refining'], true],
  ['Zhu Xian', 'Jade Dynasty', ['Legend of Chusen'], true],
  ['Da Zhu Zai', 'The Great Ruler', ['Great Ruler'], true],
  ['Ni Tian Xie Shen', 'Against the Gods', ['Against the Sky Supreme'], true],
  ['Ni Tian Zhizun', 'Against the Sky Supreme', ['Against the Gods', 'Supreme Against the Sky'], true],
  ['Shiguang Dailiren', 'Link Click', ['Time Agent', 'Time Agents'], true],
  ['Tian Guan Ci Fu', "Heaven Official's Blessing", ['Heaven Official Blessing', 'TGCF'], true],
  ['Mo Dao Zu Shi', 'The Founder of Diabolism', ['Grandmaster of Demonic Cultivation', 'The Grandmaster of Demonic Cultivation', 'MDZS'], true],
  ['Yi Nian Yong Heng', 'A Will Eternal', ['One Thought Eternity', 'Will Eternal'], true],
  ['Wu Dong Qian Kun', 'Martial Universe', ['Martial Movement Upheaval'], true],
  ['Xing Chen Bian', 'Stellar Transformations', ['Stellar Transformation', 'Legend of Immortals'], true],
  ['Yao Shen Ji', 'Tales of Demons and Gods', ['Demons and Gods'], true],
  ['Xian Wang de Richang Shenghuo', 'The Daily Life of the Immortal King', ['Daily Life of the Immortal King', 'Immortal King'], true],
  ['Quan Zhi Gao Shou', "The King's Avatar", ["King's Avatar", 'Full-Time Expert'], true],
  ['Quanzhi Gaoshou', "The King's Avatar", ["King's Avatar"], true],
  ['Quanzhi Fashi', 'Full-Time Magister', ['Versatile Mage'], true],
  ['Quan Zhi Fa Shi', 'Full-Time Magister', ['Versatile Mage'], true],
  ['Cike Wu Liuqi', 'Scissor Seven', ['Killer Seven'], true],
  ['Wu Liuqi', 'Scissor Seven', ['Killer Seven'], true],
  ['Wu Shan Wu Xing', 'Fog Hill of Five Elements', ['Fog Hill of the Five Elements'], true],
  ['Zhan Shen', 'Slay the Gods', ['I Learned to Slay Gods in a Mental Hospital', 'God Slayer'], true],
  ['Huo Feng Liao Yuan', 'The Ravages of Time', ['Ravages of Time'], true],
  ['Juan Siliang', 'The Island of Siliang', ['Island of Siliang'], true],
  ['Si Liang Dao', 'The Island of Siliang', ['Island of Siliang'], true],
  ['Xian Wu Chuan', 'Legend of Xianwu', ['Legend of Martial Immortal', 'Xianwu Emperor'], true],
  ['Xian Wu Di Zun', 'Legend of Xianwu', ['Xianwu Emperor', 'Immortal Martial Emperor'], true],
  ['Yuan Zun', 'Dragon Prince Yuan', ['Dragon Prince Yuan'], true],
  ['Sheng Xu', 'The Sacred Ruins', ['Sacred Ruins'], true],
  ['Hua Jianghu: Bu Liang Ren', 'A Portrait of Jianghu: The Degenerate', ['The Degenerate Drawing Jianghu', 'The Degenerate'], true],
  ['Bu Liang Ren', 'The Degenerate: Drawing Jianghu', ['The Degenerate', 'Bad Guys'], true],
  ['Da Wang Rao Ming', 'Spare Me, Great Lord!', ['Spare Me Great Lord'], true],
  ['Yi Ren Zhi Xia', 'The Outcast', ['Hitori no Shita: The Outcast', 'Under One Person'], true],
  ['Zhen Hun Jie', 'Rakshasa Street', ['Requiem Street'], true],
  ['Ling Long: Incarnation', 'Ling Cage: Incarnation', ['Spirit Cage: Incarnation', 'Ling Cage'], true],
  ['Ling Long', 'Ling Cage: Incarnation', ['Spirit Cage'], true],
  ['Wan Jie Duzun', 'Ten Thousand Worlds', ['Lord of Ten Thousand Worlds'], true],
  ['Wan Jie Xian Zong', 'Wonderland of Ten Thousand Deities', ['Wonderland'], true],
  ['Wan Jie Fa Shen', 'God of All Realms', ['Law God of Ten Thousand Worlds'], true],
  ['Wan Jie Shen Zhu', 'Lord of the Universe', ['God of Ten Thousand Worlds'], true],
  ['Ling Jian Zun', 'Spirit Sword Sovereign', ['Sword Spirit Sovereign'], true],
  ['Du Bu Xiao Yao', 'One Step Toward Freedom', ['Step to Freedom'], true],
  ['Shen Wu Tian Zun', 'Supreme Martial God', ['Legend of the Supreme Soldier'], true],
  ['Dan Dao Zhi Zun', 'Alchemy Supreme', ['Supreme Alchemist'], true],
  ['Jue Shi Wu Hun', 'Peerless Battle Spirit', ['Peerless Martial Spirit'], true],
  ['Jue Shi Wu Shen', 'Peerless Martial God', ['Peerless Martial God'], true],
  ['Shen Mu', 'Tomb of Fallen Gods', ['Divine Tomb'], true],
  ['Long She Yan Yi', "Dragon's Disciple", ['Dragon Snake Evolution'], true],
  ['Chao Neng Lifang', 'Super Cube', ['Super Cube Donghua'], true],
  ['Tu Bian Yingxiong X', 'To Be Hero X', ['To Be Hero X'], true],
  ['Tu Bian Yingxiong', 'To Be Hero', ['To Be Hero'], true],
  ['Shi Kun', 'All Devouring Whale: Homecoming', ['All Devouring Whale'], true],
  ['Kun Tun Tianxia', 'All Devouring Whale: Homecoming', ['All Devouring Whale'], true],
  ['Tian Xing Jiu Ge', 'Nine Songs of the Moving Heavens', ['Nine Songs'], true],
  ['Qin Shi Ming Yue', 'The Legend of Qin', ["Qin's Moon"], true],
  ['Wu Geng Ji', 'The Legend and the Hero', ["Chronicles of the God's Order"], true],
  ['Xi Xing Ji', 'The Westward', ['Journey to the Westward'], true],
  ['Shao Nian Ge Xing', 'Great Journey of Teenagers', ['The Blood of Youth', 'Youths and Golden Coffin'], true],
  ['Shao Nian Bai Ma Zui Chun Feng', "Young Brewmaster's Adventure", ['Dashing Youth'], true],
  ['An He Chuan', 'Tales of Dark River', ['Blood River'], true],
  ['Cang Lan Jue', 'Love Between Fairy and Devil', ['Love Between Fairy and Devil'], true],
  ['Cang Lan Zhi Jue', 'Love Between Fairy and Devil', ['Love Between Fairy and Devil'], true],
  ['Shan He Jian Xin', 'Thousand Autumns', ['Thousand Autumns'], true],
  ['Chuan Shu Zijiu Zhinan', 'Scumbag System', ["The Scum Villain's Self-Saving System"], true],
  ['Tian Bao Fuyao Lu', 'Legend of Exorcism', ['Legend of Exorcism'], true],
  ['Dinghai Fusheng Lu', 'Dinghai Fusheng Records', ['Epic of Divinity Light'], true],
  ['Liang Bu Yi', 'No Doubt in Us', ['No Doubt in Us'], true],
  ['You Shou Yan', 'Fabulous Beasts', ['Fabulous Beasts'], true],
  ['Fei Ren Zai', 'Non-Human', ['Fabulous Beasts'], true],
  ['Luo Xiaohei Zhan Ji', 'The Legend of Hei', ['The Legend of Luo Xiaohei'], true],
  ['Bai She: Yuan Qi', 'White Snake', ['White Snake Origin'], true],
  ['Qing She: Jie Qi', 'Green Snake', ['White Snake 2: Green Snake'], true],
  ['Da Yu Hai Tang', 'Big Fish & Begonia', ['Big Fish and Begonia'], true],
  ['Nezha: Mo Tong Jiang Shi', 'Ne Zha', ['Nezha: Birth of the Demon Child'], true],
  ['Jiang Ziya', 'Jiang Ziya: Legend of Deification', ['Legend of Deification'], true],
  ['Hu Yao Xiao Hongniang', 'Fox Spirit Matchmaker', ['Fox Spirit Matchmaker'], true],
  ['Bing Huo Mo Chu', 'The Magic Chef of Ice and Fire', ['Magic Chef of Ice and Fire'], true],
  ['Xue Ying Ling Zhu', 'Snow Eagle Lord', ['Lord Xue Ying'], true],
  ['Yong Sheng', 'Immortality', ['Eternal Life'], true],
  ['Xing Wu Shen Jue', 'Star Martial God Technique', ['Star Martial God'], true],
  ['Wan Gu Jian Shen', 'Eternal Sword God', ['Ten Thousand Ancient Sword God'], true],
  ['Wan Gu Shen Hua', 'My Heroic Husband', ['Eternal Myth'], true],
  ['Jian Yu Feng Yun', 'Sword Domain', ['The Wind and Cloud of Sword Domain'], true],
  ['Shen Lan Qi Yu Wushuang Zhu', 'The Land of Miracles', ['Land of Miracles'], true],
  ['Chang Sheng Jie', 'World of Immortals', ['Immortal World'], true],
  ['Yuan Long', 'Carp Reborn', ['First Dragon'], true],
  ['Wu Lian Dian Feng', 'Martial Peak', ['Peak of Martial Arts'], true],
  ['Zhen Dao Ge', 'Song of the Real Blade', ['True Blade Song'], true],
  ['Hu He Yao Shi Lu', 'Tiger Crane', ['Record of Tiger and Crane'], true],
  ['Shi Fang Wu Sheng', 'Ten Directions Martial Saint', ['Martial Saint of Ten Directions'], true],
  ['Zhi Zun Shen Ming', 'Supreme God Emperor', ['Supreme God'], true],
  ['Tian Huang Zhan Shen', 'Desolate God of War', ['God of War'], true],
  ['Zui Qiang Fan Pai', 'Strongest Anti M.E.T.A', ['Ultimate Scheming System'], true],
  ['Zui Hou De Zhao Huan Shi', 'The Last Summoner', ['Last Summoner'], true],
  ['Tong Ling Fei', 'Psychic Princess', ['Psychic Princess'], true],
  ['Xiao Lu He Xiao Lan', 'Beryl and Sapphire', ['Beryl and Sapphire'], true],
  ['Guan Hai Ce', 'Watching the Sea', ['Strategies of Watching the Sea'], true],
  ['Feng Shen Bang', 'New Gods: Yang Jian', ['Investiture of the Gods'], true],
  ['Tai Yi Jian Xian Chuan', 'Legend of Taiyi Sword Immortal', ['Taiyi Sword Immortal'], true],
  ['Wu Ying Sanqian Dao', 'Three Thousand Paths of Martial Arts', ['Three Thousand Martial Paths'], true],
  ['Jiu Tian Xuan Di Jue', 'Nine Heavens Profound Emperor', ['Nine Star Hegemon'], true],
  ['Zhen Wu Dian Feng', 'True Martial World', ['Peak of True Martial Arts'], true],
  ['Wan Gu Zui Qiang Zong', 'The All-Devouring Sect', ['Strongest Sect of All Times'], true],
  ['Da Dao Chao Tian', 'The Path Toward Heaven', ['Path Toward Heaven'], true],
  ['Shen Lan Zhi Yue', 'Deep Blue Moon', ['Deep Blue Moon'], true],
  ['Xinghai Qihang', 'Starry Journey', ['Starry Journey'], true],
  ['Bi Lan Zhi Yu', 'Azure Legacy', ['Azure Legacy'], true],
  ['Jiyi Guanli Ju', 'False Memory', ['Memory Management Bureau'], true],
  ['Bai Ri Cheng Wang', 'Crowned in a Hundred Days', ['Crowned in a Hundred Days'], true],
  ['Shi Yi Ci Yuan', 'Eleventh Dimension', ['Eleventh Dimension'], true],
  ['Lu Shidai', "Lu's Time", ["Lu's Time"], true],
  ['Xian Zun Nai Ba Dang Dao', 'Immortal Sovereign Dad', ['Immortal Dad'], true],
  ['Wo De Shi Xiong Tai Wen Jian Le', 'My Senior Brother Is Too Steady', ['Big Brother'], true],
  ['Tun Tian Ji', 'Heaven Swallowing Record', ['Swallowing the Heavens'], true],
  ['Chao Shen Xue Yuan', 'Transcend the Gods', ['Super God Academy'], true],
  ['Xiong Bing Lian', 'The Black Troop', ['Transcend the Gods: The Black Troop'], true],
  ['Bai Yao Pu', 'Fairies Albums', ['Manual of Hundred Demons'], true],
  ['Da Li Si Ri Zhi', 'White Cat Legend', ['Journal of Dali Temple'], true],
  ['Huan Chong Shi', 'The Monster Pet Evolution', ['Monster Pet Evolution'], true],
  // Major Japanese Anime (Romaji -> Official English)
  ['Tensei Shitara Slime Datta Ken', 'That Time I Got Reincarnated as a Slime', ['Tensura', 'Slime Isekai'], false],
  ['Mairimashita! Iruma-kun', 'Welcome to Demon School! Iruma-kun', ['Iruma-kun'], false],
  ['Re:Zero kara Hajimeru Isekai Seikatsu', 'Re:ZERO -Starting Life in Another World-', ['Re:Zero', 'Starting Life in Another World'], false],
  ['Mushoku Tensei: Isekai Ittara Honki Dasu', 'Mushoku Tensei: Jobless Reincarnation', ['Jobless Reincarnation'], false],
  ['Kimetsu no Yaiba', 'Demon Slayer: Kimetsu no Yaiba', ['Demon Slayer'], false],
  ['Shingeki no Kyojin', 'Attack on Titan', ['AOT'], false],
  ['Boku no Hero Academia', 'My Hero Academia', ['MHA'], false],
  ['Sousou no Frieren', "Frieren: Beyond Journey's End", ['Frieren'], false],
  ['Kusuriya no Hitorigoto', 'The Apothecary Diaries', ['Apothecary Diaries'], false],
  ['Kage no Jitsuryokusha ni Naritakute!', 'The Eminence in Shadow', ['Eminence in Shadow'], false],
  ['Tate no Yuusha no Nariagari', 'The Rising of the Shield Hero', ['Shield Hero'], false],
  ['Kono Subarashii Sekai ni Shukufuku o!', "KONOSUBA -God's blessing on this wonderful world!", ['Konosuba'], false],
  ['Kaguya-sama wa Kokurasetai', 'Kaguya-sama: Love is War', ['Love is War'], false],
  ['Youjo Senki', 'Saga of Tanya the Evil', ['Tanya the Evil'], false],
  ['BLEACH: Sennen Kessen-hen', 'BLEACH: Thousand-Year Blood War', ['Bleach TYBW'], false],
  ['Ore dake Level Up na Ken', 'Solo Leveling', ['Solo Leveling'], false],
  ['Meitantei Conan', 'Detective Conan', ['Case Closed'], false],
  ['Kaijuu 8-gou', 'Kaiju No. 8', ['Kaiju 8'], false],
  ['Dungeon Meshi', 'Delicious in Dungeon', ['Delicious in Dungeon'], false],
  ['Boku no Kokoro no Yabai Yatsu', 'The Dangers in My Heart', ['Dangers in My Heart'], false],
  ['Yofukashi no Uta', 'Call of the Night', ['Call of the Night'], false],
  ['Youkoso Jitsuryoku Shijou Shugi no Kyoushitsu e', 'Classroom of the Elite', ['COTE'], false],
  ['Kami no Tou', 'Tower of God', ['Tower of God'], false],
  ['Hagane no Renkinjutsushi', 'Fullmetal Alchemist', ['FMA', 'Fullmetal Alchemist: Brotherhood'], false],
  ['Enen no Shouboutai', 'Fire Force', ['Fire Force'], false],
  ['Nanatsu no Taizai', 'The Seven Deadly Sins', ['Seven Deadly Sins'], false],
  ['Yakusoku no Neverland', 'The Promised Neverland', ['Promised Neverland'], false],
  ['Ansatsu Kyoushitsu', 'Assassination Classroom', ['Assassination Classroom'], false],
  ['Seishun Buta Yarou wa Bunny Girl Senpai no Yume wo Minai', 'Rascal Does Not Dream of Bunny Girl Senpai', ['Bunny Girl Senpai'], false],
  ['Dungeon ni Deai wo Motomeru no wa Machigatteiru Darou ka', 'Is It Wrong to Try to Pick Up Girls in a Dungeon?', ['DanMachi'], false],
  ['Shigatsu wa Kimi no Uso', 'Your Lie in April', ['Your Lie in April'], false],
  ['Koe no Katachi', 'A Silent Voice', ['Silent Voice'], false],
  ['Kimi no Na wa.', 'Your Name.', ['Your Name'], false],
  ['Tenki no Ko', 'Weathering With You', ['Weathering With You'], false],
  ['Sen to Chihiro no Kamikakushi', 'Spirited Away', ['Spirited Away'], false],
  ['Mononoke Hime', 'Princess Mononoke', ['Princess Mononoke'], false],
  ['Howl no Ugoku Shiro', "Howl's Moving Castle", ["Howl's Moving Castle"], false]
];

const ENGLISH_CORE_WORDS = new Set([
  'the', 'of', 'in', 'to', 'and', 'for', 'with', 'from', 'by', 'on', 'at', 'as', 'my', 'our', 'your',
  'world', 'god', 'gods', 'immortal', 'immortality', 'eternal', 'eternity', 'heaven', 'heavens', 'heavenly',
  'sky', 'star', 'stars', 'starry', 'soul', 'spirit', 'martial', 'sword', 'blade', 'master', 'lord', 'king',
  'emperor', 'prince', 'princess', 'queen', 'demon', 'demons', 'devil', 'angel', 'dragon', 'phoenix', 'tiger',
  'wolf', 'beast', 'beasts', 'monster', 'hero', 'legend', 'legends', 'tales', 'tale', 'story', 'chronicles',
  'record', 'records', 'journey', 'adventure', 'quest', 'battle', 'war', 'fight', 'fights', 'slay', 'slayer',
  'hunter', 'assassin', 'killer', 'shadow', 'dark', 'light', 'blood', 'fire', 'ice', 'wind', 'thunder',
  'earth', 'sea', 'river', 'mountain', 'island', 'land', 'realm', 'realms', 'universe', 'cosmos', 'space',
  'time', 'life', 'death', 'dead', 'tomb', 'grave', 'ruins', 'sacred', 'divine', 'holy', 'supreme', 'sovereign',
  'peerless', 'unrivaled', 'great', 'grand', 'ancient', 'mortal', 'renegade', 'against', 'through', 'beyond',
  'swallowed', 'devouring', 'shrouding', 'herding', 'coming', 'throne', 'seal', 'sealed', 'dynasty', 'jade',
  'cultivation', 'alchemy', 'refining', 'sect', 'clan', 'school', 'academy', 'brother', 'sister', 'senior',
  'steady', 'perfect', 'stellar', 'transformations', 'apotheosis', 'mysteries', 'mystery', 'magic', 'magister',
  'mage', 'summoner', 'outcast', 'degenerate', 'street', 'cage', 'incarnation', 'click', 'link', 'blessing',
  'official', 'avatar', 'scissor', 'seven', 'fog', 'hill', 'five', 'elements', 'ravages', 'spare', 'me',
  'daily', 'will', 'thousand', 'ten', 'hundred', 'years', 'step', 'freedom', 'disciple', 'super', 'cube',
  'whale', 'songs', 'youth', 'love', 'between', 'fairy', 'autumns', 'system', 'exorcism', 'doubt', 'fabulous',
  'snake', 'white', 'green', 'black', 'red', 'blue', 'gold', 'golden', 'silver', 'big', 'fish', 'begonia',
  'fox', 'matchmaker', 'chef', 'snow', 'eagle', 'peak', 'domain', 'miracles', 'carp', 'reborn', 'path',
  'toward', 'season', 'part', 'movie', 'special', 'edition', 'reincarnated', 'slime', 'titan', 'attack',
  'apothecary', 'diaries', 'shield', 'eminence', 'classroom', 'elite', 'tower', 'solo', 'leveling', 'online',
  'piece', 'punch', 'note', 'detective', 'silent', 'voice', 'name', 'weathering', 'spirited', 'away',
  'moving', 'castle', 'promised', 'neverland', 'deadly', 'sins', 'clover', 'stone', 'force'
]);

const PINYIN_MARKERS = new Set([
  'zh', 'ch', 'sh', 'xi', 'qi', 'ji', 'yu', 'lv', 'xue', 'zhe', 'tian', 'xian', 'shen', 'dou', 'luo',
  'wang', 'zuo', 'cang', 'qiong', 'ling', 'zun', 'jian', 'wan', 'jie', 'wu', 'dao', 'mo', 'feng', 'yun',
  'long', 'huo', 'bing', 'lei', 'xing', 'kong', 'tun', 'shi', 'ren', 'chuan', 'zhuan', 'zhi', 'bu', 'nian',
  'fan', 'bai', 'hei', 'qing', 'hong', 'jin', 'yin', 'yuan', 'mu', 'gu', 'shan', 'he', 'xiao', 'da', 'tai',
  'jue', 'yao', 'shijie', 'dalu', 'xingkong', 'cangqiong', 'xiuxian', 'wangzuo', 'dailiren', 'gaoshou',
  'fashi', 'liuqi', 'liang', 'jianghu', 'siliang', 'yongheng', 'qiankun', 'zhan', 'gong', 'cheng', 'ming',
  'shuang', 'huan', 'chong', 'shao', 'chun', 'qiu', 'dong', 'xia', 'dizi', 'zong', 'men'
]);

const NON_ENGLISH_LATIN_MARKERS = new Set([
  'el', 'la', 'los', 'las', 'del', 'una', 'con', 'para', 'por', 'der', 'die', 'das', 'und', 'ein', 'eine',
  'les', 'des', 'une', 'dans', 'pour', 'avec', 'cua', 'nguoi', 'dau', 'pha', 'gioi', 'tinh', 'tien'
]);

const JAPANESE_GRAMMAR_TOKENS = new Set([
  'no', 'wa', 'ga', 'wo', 'ni', 'kara', 'made', 'datta', 'ken', 'naritakute', 'shitara', 'desu', 'deshita',
  'monogatari', 'sekai', 'isekai', 'yuusha', 'maou', 'mahou', 'shoujo', 'shounen', 'gakuen', 'koukou',
  'kyoushitsu', 'senpai', 'sensei', 'tachi', 'tensei', 'akuyaku', 'reijou'
]);

function normalizeTitleKey(raw) {
  if (!raw) return '';
  return String(raw)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()
    .replace(/\s+/g, ' ');
}

const TITLE_DICT_MAP = new Map();
const TITLE_DICT_PREFIXES = [];
for (const [pinyinOrRomaji, englishTitle, alts, isDonghua] of TITLE_DICTIONARY_ENTRIES) {
  const entry = { pinyinOrRomaji, englishTitle, alternateEnglish: alts || [], isDonghua: Boolean(isDonghua) };
  const key = normalizeTitleKey(pinyinOrRomaji);
  if (key && !TITLE_DICT_MAP.has(key)) TITLE_DICT_MAP.set(key, entry);
  const compact = key.replace(/\s+/g, '');
  if (compact.length >= 6 && !TITLE_DICT_MAP.has(compact)) TITLE_DICT_MAP.set(compact, entry);
  if (key.length >= 5) TITLE_DICT_PREFIXES.push([key, entry]);
}
TITLE_DICT_PREFIXES.sort((a, b) => b[0].length - a[0].length);

function hasNonLatinScript(text) {
  return /[\u0400-\u052F\u0590-\u06FF\u0900-\u097F\u0E00-\u0E7F\u3040-\u30FF\u3400-\u9FFF\uAC00-\uD7AF\uF900-\uFAFF]/.test(
    String(text || '')
  );
}

function hasNonEnglishDiacritics(text) {
  return /[\u1E00-\u1EFFđĐăĂơƠưƯñÑßçÇłŁżŻąĄęĘ]/.test(String(text || ''));
}

function applyEnglishTitlePatterns(text) {
  return String(text || '')
    .trim()
    .replace(/\b4th Season\b/gi, 'Season 4')
    .replace(/\b3rd Season\b/gi, 'Season 3')
    .replace(/\b2nd Season\b/gi, 'Season 2')
    .replace(/\b1st Season\b/gi, 'Season 1')
    .replace(/\bDai\s*(\d+)\s*Ki\b/gi, 'Season $1')
    .replace(/\bDi\s*Er\s*Ji\b/gi, 'Season 2')
    .replace(/\bDi\s*San\s*Ji\b/gi, 'Season 3')
    .replace(/\bDi\s*Si\s*Ji\b/gi, 'Season 4')
    .replace(/\bDi\s*Wu\s*Ji\b/gi, 'Season 5')
    .replace(/\bDi\s*Liu\s*Ji\b/gi, 'Season 6')
    .replace(/\bDi\s*(\d+)\s*Ji\b/gi, 'Season $1')
    .replace(/\bNian\s*Fan\b/gi, 'Annual Edition')
    .replace(/\bTebie\s*Pian\b/gi, 'Special')
    .replace(/\bFanwai\b/gi, 'Special')
    .replace(/\bQianzhuan\b/gi, 'Prequel')
    .replace(/\bJuchang\s*Ban\b/gi, 'Movie')
    .replace(/\s+/g, ' ')
    .trim();
}

function isLikelyPinyin(candidate, romaji = '') {
  const trimmed = String(candidate || '').trim();
  if (!trimmed || hasNonLatinScript(trimmed)) return false;

  const normCand = normalizeTitleKey(trimmed);
  const compactCand = normCand.replace(/\s+/g, '');
  const compactRom = normalizeTitleKey(romaji).replace(/\s+/g, '');

  if (
    compactRom.length >= 4 &&
    (compactCand === compactRom ||
      (compactCand.startsWith(compactRom) && compactCand.length - compactRom.length <= 4))
  ) {
    const toks = normCand.split(' ').filter(Boolean);
    const engHits = toks.filter((t) => ENGLISH_CORE_WORDS.has(t) && t.length >= 3).length;
    if (engHits === 0) return true;
  }

  if (TITLE_DICT_MAP.get(normCand)?.isDonghua || TITLE_DICT_MAP.get(compactCand)?.isDonghua) {
    return true;
  }

  const tokens = normCand.split(' ').filter((t) => t && !/^\d+$/.test(t));
  if (!tokens.length) return false;

  const engHits = tokens.filter((t) => ENGLISH_CORE_WORDS.has(t)).length;
  if (engHits >= 2 || (engHits === 1 && tokens.length <= 2)) return false;

  let pinyinScore = 0;
  for (const tok of tokens) {
    if (PINYIN_MARKERS.has(tok)) pinyinScore += 2;
    else if (/^(zh|ch|sh|x|q|z|c)[aeiou][a-z]{0,4}$/.test(tok)) pinyinScore += 1;
    else if (/^[a-z]*(ang|eng|ing|ong|uan|ian|iao|iang|iong|ue|uo|ou|ai|ei)$/.test(tok) && !ENGLISH_CORE_WORDS.has(tok)) {
      pinyinScore += 1;
    }
  }
  return pinyinScore >= Math.max(2, tokens.length);
}

function scoreEnglishSynonym(candidate, romaji, isDonghua) {
  const clean = String(candidate || '').trim();
  if (clean.length < 3 || clean.length > 120) return -100;
  if (hasNonLatinScript(clean)) return -100;
  if (hasNonEnglishDiacritics(clean)) return -80;

  const norm = normalizeTitleKey(clean);
  if (!norm) return -100;
  const compact = norm.replace(/\s+/g, '');
  const compactRom = normalizeTitleKey(romaji).replace(/\s+/g, '');

  const lettersOnly = clean.replace(/[^a-zA-Z]/g, '');
  if (!lettersOnly) return -100;
  if (lettersOnly.length <= 5 && lettersOnly === lettersOnly.toUpperCase() && !clean.includes(' ')) {
    return -60;
  }

  if (compactRom.length >= 4 && compact === compactRom) return -50;
  if (isDonghua && isLikelyPinyin(clean, romaji)) return -70;

  const tokens = norm.split(' ').filter(Boolean);
  if (!tokens.length) return -100;

  const engHits = tokens.filter((t) => ENGLISH_CORE_WORDS.has(t)).length;
  const jpHits = tokens.filter((t) => JAPANESE_GRAMMAR_TOKENS.has(t)).length;
  if (jpHits >= 2 || (jpHits >= 1 && engHits === 0)) return -50;

  const foreignHits = tokens.filter((t) => NON_ENGLISH_LATIN_MARKERS.has(t)).length;
  if (foreignHits >= 1 && engHits === 0) return -45;

  const lowerClean = clean.toLowerCase();
  for (const [, engTitle, alts] of TITLE_DICTIONARY_ENTRIES) {
    if (engTitle.toLowerCase() === lowerClean || (alts || []).some((a) => a.toLowerCase() === lowerClean)) {
      return 200;
    }
  }

  let score = 0;
  if (engHits > 0) {
    score += 40 + engHits * 18;
  } else if (isDonghua) {
    const isAscii = /^[\x00-\x7F]+$/.test(clean);
    if (isAscii && !isLikelyPinyin(clean, romaji) && tokens.length >= 1 && tokens.length <= 10) {
      score += 18;
    } else {
      return -20;
    }
  } else {
    return -10;
  }

  if (tokens.length >= 2 && tokens.length <= 9) score += 12;
  if (clean.length >= 6 && clean.length <= 75) score += 8;
  if (clean.includes('(') && clean.includes(')')) score -= 6;
  if (/^[\x00-\x7F]+$/.test(clean)) score += 6;
  return score;
}

function pickBestEnglishSynonym(synonyms, romaji, isDonghua) {
  if (!Array.isArray(synonyms) || !synonyms.length) return null;
  let best = null;
  let bestScore = 0;
  for (const syn of synonyms) {
    const clean = String(syn || '').trim();
    if (!clean) continue;
    const sc = scoreEnglishSynonym(clean, romaji, isDonghua);
    if (sc > bestScore) {
      bestScore = sc;
      best = clean;
    }
  }
  return best;
}

function extractTitleTrailingSuffix(raw, matchedPrefixNorm) {
  const normFull = normalizeTitleKey(raw);
  if (!normFull.startsWith(matchedPrefixNorm) || normFull.length === matchedPrefixNorm.length) return '';
  const rem = normFull.slice(matchedPrefixNorm.length).trim();
  if (!rem) return '';

  let m = rem.match(/^(?:season\s*)?(\d+)$/);
  if (m) {
    const n = Number(m[1]);
    if (n >= 2 && n <= 20) return `Season ${n}`;
  }
  m = rem.match(/^(ii|iii|iv|v|vi|vii|viii|ix|x)$/);
  if (m) {
    const roman = { ii: 2, iii: 3, iv: 4, v: 5, vi: 6, vii: 7, viii: 8, ix: 9, x: 10 };
    if (roman[m[1]]) return `Season ${roman[m[1]]}`;
  }
  m = rem.match(/^(\d+)(?:nd|rd|th)\s+season$/);
  if (m) return `Season ${m[1]}`;
  m = rem.match(/^di\s*(\d+|er|san|si|wu|liu|qi|ba)\s*ji$/);
  if (m) {
    const cn = { er: '2', san: '3', si: '4', wu: '5', liu: '6', qi: '7', ba: '8' };
    return `Season ${cn[m[1]] || m[1]}`;
  }
  if (rem.includes('nian fan')) return 'Annual Edition';
  if (rem.includes('movie') || rem.includes('juchang')) return 'Movie';
  if (rem.includes('special') || rem.includes('tebie') || rem.includes('fanwai')) return 'Special';
  if (rem.includes('qianzhuan') || rem.includes('prequel')) return 'Prequel';
  m = rem.match(/^part\s*(\d+)$/);
  if (m) return `Part ${m[1]}`;
  return '';
}

function findInTitleDictionary(query) {
  const clean = String(query || '').trim();
  if (!clean) return null;
  const norm = normalizeTitleKey(clean);
  const compact = norm.replace(/\s+/g, '');

  const exact = TITLE_DICT_MAP.get(norm) || TITLE_DICT_MAP.get(compact);
  if (exact) return exact.englishTitle;

  for (const [prefixNorm, entry] of TITLE_DICT_PREFIXES) {
    if (norm === prefixNorm || norm.startsWith(`${prefixNorm} `)) {
      const suffix = extractTitleTrailingSuffix(clean, prefixNorm);
      if (suffix && !entry.englishTitle.toLowerCase().includes(suffix.toLowerCase())) {
        return `${entry.englishTitle} ${suffix}`;
      }
      return entry.englishTitle;
    }
  }
  return null;
}

function resolveEnglishTitle(titleEnglish, titleRomaji, synonyms = [], isDonghua = false) {
  const eng = String(titleEnglish || '').trim();
  const rom = String(titleRomaji || '').trim();
  const effectiveDonghua =
    Boolean(isDonghua) ||
    Boolean(rom && TITLE_DICT_MAP.get(normalizeTitleKey(rom))?.isDonghua) ||
    Boolean(eng && TITLE_DICT_MAP.get(normalizeTitleKey(eng))?.isDonghua);

  if (eng) {
    const mappedEng = findInTitleDictionary(eng);
    if (mappedEng) return mappedEng;
    const engIsPinyin = effectiveDonghua && isLikelyPinyin(eng, rom);
    if (!engIsPinyin && !hasNonLatinScript(eng)) {
      return applyEnglishTitlePatterns(eng);
    }
  }

  if (rom) {
    const mappedRom = findInTitleDictionary(rom);
    if (mappedRom) return mappedRom;
  }

  if (Array.isArray(synonyms)) {
    for (const syn of synonyms) {
      const mapped = findInTitleDictionary(syn);
      if (mapped) return mapped;
    }
  }

  const bestSyn = pickBestEnglishSynonym(synonyms, rom, effectiveDonghua);
  if (bestSyn) return applyEnglishTitlePatterns(bestSyn);

  if (rom) {
    const trans = applyEnglishTitlePatterns(rom);
    if (trans !== rom) return trans;
  }

  if (eng) return applyEnglishTitlePatterns(eng);
  return rom || '';
}

function scoreSingleTextMatch(normQuery, queryTokens, rawText) {
  const normText = normalizeTitleKey(rawText);
  if (!normText) return 0;

  if (normText === normQuery) return 140;
  if (normText.startsWith(`${normQuery} `)) return 120;
  if (normText.startsWith(normQuery)) return 110;
  if (normText.includes(` ${normQuery} `)) return 100;
  if (normText.includes(normQuery)) return 90;

  const textTokens = normText.split(' ').filter(Boolean);
  if (!textTokens.length) return 0;

  let exactWord = 0;
  let prefixWord = 0;
  let substrWord = 0;
  for (const qt of queryTokens) {
    if (textTokens.some((t) => t === qt)) exactWord++;
    else if (textTokens.some((t) => t.startsWith(qt))) prefixWord++;
    else if (normText.includes(qt)) substrWord++;
  }

  const total = exactWord + prefixWord + substrWord;
  if (!total) return 0;
  const allMatched = total === queryTokens.length;
  if (allMatched && exactWord === queryTokens.length) return 95;
  if (allMatched && exactWord + prefixWord === queryTokens.length) return 82;
  if (allMatched) return 70;
  return exactWord * 28 + prefixWord * 20 + substrWord * 10;
}

const SEARCH_STOP_WORDS = new Set([
  'of', 'the', 'a', 'an', 'in', 'to', 'and', 'or', 'for', 'on', 'at', 'by', 'as', 'is', 'it', 'my'
]);

function expandSearchQueries(rawQuery, donghuaOnly = false, maxExtra = 2) {
  const normQuery = normalizeTitleKey(rawQuery);
  if (normQuery.length < 2) return [];
  const rawTokens = normQuery.split(' ').filter((t) => t.length >= 2);
  if (!rawTokens.length) return [];
  const contentTokens = rawTokens.filter((t) => !SEARCH_STOP_WORDS.has(t));
  const effectiveTokens = contentTokens.length ? contentTokens : rawTokens;

  const scored = [];
  const seen = new Set();
  for (const [pinyinOrRomaji, englishTitle, alts, isDonghua] of TITLE_DICTIONARY_ENTRIES) {
    if (donghuaOnly && !isDonghua) continue;
    const pinyinNorm = normalizeTitleKey(pinyinOrRomaji);
    if (pinyinNorm === normQuery || seen.has(pinyinNorm)) continue;
    seen.add(pinyinNorm);

    const candidates = [englishTitle, ...(alts || [])];
    let best = 0;
    for (const c of candidates) {
      const cNorm = normalizeTitleKey(c);
      if (contentTokens.length && !contentTokens.some((tok) => cNorm.includes(tok))) continue;
      const sc = scoreSingleTextMatch(normQuery, effectiveTokens, c);
      if (sc > best) best = sc;
    }
    if (best >= 40) {
      scored.push([pinyinOrRomaji, best + (isDonghua ? 15 : 0)]);
    }
  }

  scored.sort((a, b) => b[1] - a[1]);
  return scored.slice(0, maxExtra).map((x) => x[0]);
}

function scoreKeywordMatch(rawQuery, resolvedEnglish, officialEnglish, romaji, synonyms = []) {
  const normQuery = normalizeTitleKey(rawQuery);
  if (!normQuery) return 0;
  const queryTokens = normQuery.split(' ').filter(Boolean);
  if (!queryTokens.length) return 0;

  let best = 0;
  const fields = [];
  if (resolvedEnglish) fields.push([resolvedEnglish, 12]);
  if (officialEnglish) fields.push([officialEnglish, 10]);
  if (romaji) fields.push([romaji, 6]);
  if (Array.isArray(synonyms)) {
    for (const syn of synonyms) {
      if (syn) fields.push([syn, 8]);
    }
  }
  for (const [text, bonus] of fields) {
    const sc = scoreSingleTextMatch(normQuery, queryTokens, text);
    if (sc > 0 && sc + bonus > best) {
      best = sc + bonus;
    }
  }
  return best;
}

function matchesAllKeywords(rawQuery, ...fields) {
  const normQuery = normalizeTitleKey(rawQuery);
  if (!normQuery) return true;
  const tokens = normQuery.split(' ').filter(Boolean);
  const combined = fields
    .flat()
    .filter(Boolean)
    .map((f) => normalizeTitleKey(f))
    .join(' ');
  if (!combined) return false;
  return tokens.every((tok) => combined.includes(tok));
}

function resolveTitle(item) {
  if (!item) return 'Anime';
  const pref = state.prefs.titleLanguage;
  if (pref === 'ROMAJI') return item.titleRomaji || item.title || item.titleEnglish || 'Anime';
  if (pref === 'NATIVE') return item.titleNative || item.titleEnglish || item.titleRomaji || item.title || 'Anime';
  const isDonghua = String(item.countryOfOrigin || '').toUpperCase() === 'CN';
  const resolved = resolveEnglishTitle(
    item.titleEnglish || item.title || '',
    item.titleRomaji || '',
    item.synonyms || [],
    isDonghua
  );
  return resolved || item.titleEnglish || item.title || item.titleRomaji || 'Anime';
}

function formatCompactCountdown(airingUnix) {
  if (!airingUnix) return '';
  const diff = airingUnix - Math.floor(Date.now() / 1000);
  if (diff <= 0) return 'AIRING';
  const d = Math.floor(diff / 86400);
  const h = Math.floor((diff % 86400) / 3600);
  const m = Math.floor((diff % 3600) / 60);
  if (d > 0) return `${d}d ${h}h`;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

function formatAiringClock(airingUnix) {
  if (!airingUnix || airingUnix <= 0) return '';
  try {
    const d = new Date(airingUnix * 1000);
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  } catch (_) {
    return '';
  }
}

// Fetch User Watchlist, Home Tracking, Collections, Profile & Community Reviews (Strictly isolated per user_id)
async function loadCloudData(silent = false) {
  const activeUserId = state.session?.userId;
  if (!activeUserId) return;
  try {
    const uid = encodeURIComponent(activeUserId);
    const watchlistSelect =
      'id,user_id,anime_id,status,episode_progress,total_episodes,personal_rating,review_notes,is_favorite,updated_at,' +
      'anime(anilist_id,title_english,title_romaji,title_native,cover_image,banner_image,genres,release_year,season,episodes,average_score,popularity,format,status,studios,next_airing_at,next_episode_number)';

    const [normRows, homeRows, colRows, prof, cmRows] = await Promise.all([
      supabaseRequest(`/rest/v1/user_anime?user_id=eq.${uid}&select=${watchlistSelect}&order=updated_at.desc`).catch(() => null),
      supabaseRequest(`/rest/v1/home_tracking?user_id=eq.${uid}&select=anime_id,airing_day_of_week`).catch(() => null),
      supabaseRequest(`/rest/v1/collections?creator_id=eq.${uid}&select=*,collection_anime(anime_id)&order=updated_at.desc`).catch(() => null),
      supabaseRequest(`/rest/v1/profiles?id=eq.${uid}&select=*`).catch(() => []),
      supabaseRequest(`/rest/v1/community_members?user_id=eq.${uid}&select=status`).catch(() => [])
    ]);

    // Guard against session switching while request was in flight
    if (!state.session || state.session.userId !== activeUserId) return;

    const hasServerHomeTable = Array.isArray(homeRows);
    if (hasServerHomeTable) {
      state.homeTrackingIds = new Set(homeRows.map((h) => Number(h.anime_id)).filter(Boolean));
      state.homeTrackingHydrated = true;
      saveHomeTrackingIds();
    }

    if (Array.isArray(normRows)) {
      const userScopedLocal = readScopedArray('vault', state.session, false, null).filter(
        (v) => !v.ownerId || v.ownerId === activeUserId
      );
      const localHomeWasEmpty = !state.homeTrackingHydrated && state.homeTrackingIds.size === 0;
      state.vault = normRows
        .filter((r) => !r.user_id || r.user_id === activeUserId)
        .map((r) => {
          const anime = r.anime || {};
          const existingLocal = userScopedLocal.find((v) => v.animeId === r.anime_id) || {};
          const eng = anime.title_english && anime.title_english !== 'null' ? anime.title_english : existingLocal.titleEnglish || '';
          const rom = anime.title_romaji && anime.title_romaji !== 'null' ? anime.title_romaji : existingLocal.titleRomaji || '';
          const nextAir = anime.next_airing_at ? Number(anime.next_airing_at) : existingLocal.airingTimeUnix || null;
          const dow = nextAir && nextAir > 0
            ? (((new Date(nextAir * 1000).getDay() + 6) % 7) + 1)
            : existingLocal.airingDayOfWeek || ((r.anime_id % 7) + 1);
          const mediaStatus = anime.status || existingLocal.mediaStatus || 'RELEASING';
          const totalEp = r.total_episodes || anime.episodes || existingLocal.totalEpisodes || null;
          const watchedEp = r.episode_progress || 0;
          const userStatus = r.status || 'watching';

          if (!hasServerHomeTable && localHomeWasEmpty) {
            const eligiblePreMigration =
              userStatus !== 'dropped' &&
              userStatus !== 'completed' &&
              !shouldAutoComplete(mediaStatus, totalEp, watchedEp);
            if (eligiblePreMigration) {
              state.homeTrackingIds.add(Number(r.anime_id));
            }
          }

          const onHome =
            state.homeTrackingIds.has(Number(r.anime_id)) &&
            !shouldRemoveFromHome(userStatus, mediaStatus, totalEp, watchedEp);

          return {
            ownerId: activeUserId,
            animeId: r.anime_id,
            title: eng || rom || existingLocal.title || `Anime #${r.anime_id}`,
            titleEnglish: eng || rom || 'Anime',
            titleRomaji: rom || eng || 'Anime',
            titleNative: anime.title_native || existingLocal.titleNative || '',
            coverImage: anime.cover_image || existingLocal.coverImage || '',
            bannerImage: anime.banner_image || existingLocal.bannerImage || '',
            format: anime.format || existingLocal.format || 'TV',
            mediaStatus,
            seasonYear: anime.release_year || existingLocal.seasonYear || null,
            averageScore: anime.average_score || existingLocal.averageScore || null,
            countryOfOrigin: existingLocal.countryOfOrigin || 'JP',
            airingDayOfWeek: dow,
            airingTimeUnix: nextAir,
            nextEpisodeNumber: anime.next_episode_number || existingLocal.nextEpisodeNumber || null,
            status: userStatus,
            season: existingLocal.season || 1,
            watchedEpisodes: watchedEp,
            totalEpisodes: totalEp,
            rating10: r.personal_rating !== null && r.personal_rating !== undefined ? Number(r.personal_rating) : null,
            reviewNotes: r.review_notes || '',
            isPinned: Boolean(existingLocal.isPinned),
            isFavorite: Boolean(r.is_favorite ?? existingLocal.isFavorite),
            genres: Array.isArray(anime.genres) ? anime.genres : existingLocal.genres || [],
            synopsis: existingLocal.synopsis || '',
            inWatchlist: true,
            inHome: onHome
          };
        });
      if (!hasServerHomeTable && localHomeWasEmpty) {
        state.homeTrackingHydrated = true;
        saveHomeTrackingIds();
      }
      saveLocalVault();
    }

    // Safe non-duplicating migration if the user had tracked items in a Guest session prior to logging in
    if (pendingGuestMigrationItems.length > 0) {
      const itemsToMigrate = [...pendingGuestMigrationItems];
      const oldGuestId = pendingGuestMigrationGuestId;
      pendingGuestMigrationItems = [];
      pendingGuestMigrationGuestId = null;
      const existingIds = new Set(state.vault.map((v) => v.animeId));
      for (const gItem of itemsToMigrate) {
        if (!existingIds.has(gItem.animeId)) {
          existingIds.add(gItem.animeId);
          await saveVaultEntry({ ...gItem, ownerId: activeUserId }, { addToHome: Boolean(gItem.inHome) });
        }
      }
      if (oldGuestId) {
        localStorage.removeItem(`anime_universe_vault_guest_${oldGuestId}`);
        localStorage.removeItem(`anime_universe_home_ids_guest_${oldGuestId}`);
        localStorage.removeItem(`anime_universe_collections_guest_${oldGuestId}`);
        localStorage.removeItem(`anime_universe_profile_guest_${oldGuestId}`);
      }
      state.guestId = rotateGuestId();
    }

    if (Array.isArray(colRows)) {
      state.collections = colRows
        .filter((c) => !c.creator_id || c.creator_id === activeUserId)
        .map((c) => ({
          ownerId: activeUserId,
          id: c.id,
          name: c.name || 'Collection',
          description: c.description || '',
          visibility: c.visibility || 'community',
          animeIds: Array.isArray(c.collection_anime) ? c.collection_anime.map((ca) => ca.anime_id) : []
        }));
      saveCollections();
    }

    const cmStatus = Array.isArray(cmRows) && cmRows.length > 0 ? cmRows[0].status : '';
    if (Array.isArray(prof) && prof.length > 0) {
      const p = prof.find((row) => !row.id || row.id === activeUserId) || prof[0];
      const loadedName = String(p.username || '').trim();
      const needsProfileSetup =
        state.session?.onboardingCompleted === false || isPlaceholderOrEmailName(loadedName);
      state.profile = {
        userId: activeUserId,
        username: isPlaceholderOrEmailName(loadedName) ? 'Member' : loadedName,
        avatarKey: normalizeAvatarCompositeKey(p.avatar_key, p.avatar_url, p.avatar_frame),
        xp: Math.max(0, Number(p.xp || 0)),
        bio: p.bio || '',
        memberStatus: cmStatus || p.member_status || 'active',
        isAdmin: Boolean(p.is_admin)
      };
      saveProfileCache();
      if (needsProfileSetup && state.session) {
        if (state.session.onboardingCompleted !== false) {
          saveSession({ ...state.session, onboardingCompleted: false, username: '' });
        }
        if (state.authMode !== 'onboard_username' && state.authMode !== 'onboard_avatar') {
          state.onboardUsername = '';
          state.authMode = 'onboard_username';
        }
      } else if (state.session && !isPlaceholderOrEmailName(loadedName) && state.session.username !== loadedName) {
        saveSession({ ...state.session, username: loadedName, onboardingCompleted: true });
      }
    }

    await Promise.all([loadCommunityReviews(), refreshUnreadNotificationCount()]);
    const hasUnsavedDetailDraft = Boolean(
      state.tab === 'detail' &&
        state.detailAnime &&
        String(state.detailNoteDraft || '').trim() !== String(state.detailAnime.reviewNotes || '').trim()
    );
    const hasActiveUserWork =
      Boolean(state.inlineEpEdit) ||
      Boolean(state.contextMenu) ||
      Boolean(state.collectionContextMenu) ||
      Boolean(state.communityCommentsAnimeId !== null) ||
      Boolean(state.quickEditAnime) ||
      Boolean(state.showAvatarModal) ||
      Boolean(state.showAvatarFramesModal) ||
      Boolean(state.confirmRemoveHomeAnime) ||
      Boolean(state.tab === 'detail') ||
      hasUnsavedDetailDraft ||
      Boolean(String(state.communityCommentDraftNote || '').trim()) ||
      Boolean(String(state.discussion.draft || '').trim()) ||
      Boolean(String(state.discussion.replyDraft || '').trim()) ||
      Boolean(state.discussion.activeReplyTarget) ||
      Boolean(state.discussion.editingCommentId) ||
      Boolean(state.discussion.editingReplyId) ||
      Boolean(state.discussion.contextMenu) ||
      window.scrollY > 80 ||
      (document.activeElement && ['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName));
    if (!silent || !hasActiveUserWork) {
      renderApp();
    } else {
      updateCommunityRedDotsInPlace();
    }
  } catch (_) {}
}

function isHentaiOrAdultAnime(genres, isAdult = false) {
  if (isAdult === true) return true;
  if (!Array.isArray(genres)) return false;
  return genres.some((g) => {
    const s = String(g || '').trim().toLowerCase();
    return s === 'hentai' || s === 'adult' || s.includes('18+');
  });
}

function normalizeAnimeGenresWithAdult(genres, isAdult = false) {
  const list = Array.isArray(genres) ? genres.filter(Boolean) : [];
  if (isAdult === true && !list.some((g) => String(g).trim().toLowerCase() === 'hentai')) {
    return [...list, 'Hentai'];
  }
  return list;
}

function updateCommunityRedDotsInPlace() {
  const exactUnread = Math.max(0, Number(state.notifications.unreadCount || 0));
  if (exactUnread === 0 && (state.notifications.unseenAnime || []).length > 0) {
    state.notifications.unseenAnime = [];
  }
  const hasUnseen = exactUnread > 0;
  document.querySelectorAll('[data-nav="community"]').forEach((btn) => {
    const existingDot = btn.querySelector('.nav-red-dot');
    if (hasUnseen && !existingDot) {
      const iconWrap = btn.querySelector('.nav-icon-badge-wrap');
      const dot = document.createElement('span');
      dot.className = iconWrap ? 'nav-red-dot' : 'nav-red-dot inline';
      dot.setAttribute('aria-label', 'Unseen discussion messages');
      (iconWrap || btn).appendChild(dot);
    } else if (!hasUnseen && existingDot) {
      existingDot.remove();
    }
  });
  document.querySelectorAll('[data-comm-view-tab="tagged"]').forEach((tabBtn) => {
    const existingBadge = tabBtn.querySelector('.tagged-count-badge');
    if (exactUnread > 0) {
      const text = exactUnread > 99 ? '99+' : String(exactUnread);
      if (existingBadge) {
        existingBadge.textContent = text;
      } else {
        const badge = document.createElement('span');
        badge.className = 'tagged-count-badge';
        badge.textContent = text;
        tabBtn.appendChild(badge);
      }
    } else if (existingBadge) {
      existingBadge.remove();
    }
  });
}

// Fetch Community Reviews — Independent of Home membership; merges community_notes + user_anime
async function loadCommunityReviews() {
  try {
    const feedSelect =
      'id,user_id,anime_id,status,episode_progress,personal_rating,review_notes,updated_at,' +
      'anime(anilist_id,title_english,title_romaji,cover_image,episodes,genres)';
    const notesSelect =
      'id,user_id,anime_id,rating,note_text,updated_at,' +
      'anime(anilist_id,title_english,title_romaji,cover_image,episodes,genres)';
    const [noteRows, normRows, memberRows] = await Promise.all([
      supabaseRequest(`/rest/v1/community_notes?select=${notesSelect}&order=updated_at.desc&limit=150`).catch(() => []),
      supabaseRequest(`/rest/v1/user_anime?select=${feedSelect}&order=updated_at.desc&limit=150`).catch(() => []),
      supabaseRequest('/rest/v1/community_members?select=user_id,status').catch(() => [])
    ]);

    const excludedUsers = new Set();
    if (Array.isArray(memberRows)) {
      memberRows.forEach((m) => {
        const st = String(m.status || '').toLowerCase();
        if (st === 'left' || st === 'removed') {
          excludedUsers.add(m.user_id);
        }
      });
    }

    const combinedRaw = [];
    if (Array.isArray(noteRows)) {
      noteRows.forEach((n) => {
        if (!n || !n.user_id || excludedUsers.has(n.user_id)) return;
        const hasRating = n.rating !== null && n.rating !== undefined && Number(n.rating) > 0;
        const hasNote = String(n.note_text || '').trim().length > 0;
        if (!hasRating && !hasNote) return;
        combinedRaw.push({
          id: n.id,
          source: 'community_notes',
          user_id: n.user_id,
          anime_id: Number(n.anime_id),
          anime: n.anime || {},
          rating: hasRating ? Number(n.rating) : null,
          review_notes: String(n.note_text || '').trim(),
          updated_at: n.updated_at || new Date().toISOString()
        });
      });
    }

    if (Array.isArray(normRows)) {
      normRows.forEach((r) => {
        if (!r || !r.user_id || excludedUsers.has(r.user_id)) return;
        const hasRating = r.personal_rating !== null && r.personal_rating !== undefined && Number(r.personal_rating) > 0;
        const hasNote = String(r.review_notes || '').trim().length > 0;
        if (!hasRating && !hasNote) return;
        combinedRaw.push({
          id: r.id,
          source: 'user_anime',
          user_id: r.user_id,
          anime_id: Number(r.anime_id),
          anime: r.anime || {},
          rating: hasRating ? Number(r.personal_rating) : null,
          review_notes: String(r.review_notes || '').trim(),
          updated_at: r.updated_at || new Date().toISOString()
        });
      });
    }

    // Deduplicate per (user_id, anime_id) keeping the most recently updated entry (preferring community_notes on tie)
    combinedRaw.sort((a, b) => {
      const cmp = String(b.updated_at || '').localeCompare(String(a.updated_at || ''));
      if (cmp !== 0) return cmp;
      return a.source === 'community_notes' ? -1 : 1;
    });
    const dedupMap = new Map();
    combinedRaw.forEach((row) => {
      const key = `${row.user_id}_${row.anime_id}`;
      if (!dedupMap.has(key)) {
        dedupMap.set(key, row);
      }
    });
    const valid = Array.from(dedupMap.values());

    const userIds = Array.from(new Set(valid.map((r) => r.user_id).filter(Boolean)));
    const noteIds = valid.filter((r) => r.source === 'community_notes').map((r) => r.id).filter(Boolean);
    const reviewIds = valid.filter((r) => r.source === 'user_anime').map((r) => r.id).filter(Boolean);

    const [profRows, reviewLikeRows, noteLikeRows] = await Promise.all([
      userIds.length > 0
        ? supabaseRequest(`/rest/v1/profiles?id=in.(${userIds.join(',')})&select=*`).catch(() => [])
        : Promise.resolve([]),
      reviewIds.length > 0
        ? supabaseRequest(`/rest/v1/review_likes?review_id=in.(${reviewIds.join(',')})&select=review_id,user_id`).catch(() => [])
        : Promise.resolve([]),
      noteIds.length > 0
        ? supabaseRequest(`/rest/v1/community_note_likes?note_id=in.(${noteIds.join(',')})&select=note_id,user_id`).catch(() => [])
        : Promise.resolve([])
    ]);

    const profMap = {};
    if (Array.isArray(profRows)) {
      profRows.forEach((p) => {
        const resolvedComposite = normalizeAvatarCompositeKey(p.avatar_key, p.avatar_url, p.avatar_frame);
        profMap[p.id] = {
          username: p.username || 'Member',
          avatar_key: resolvedComposite
        };
        if (typeof DiscussionRealtimeManager !== 'undefined' && p.id) {
          DiscussionRealtimeManager.profileCache.set(p.id, {
            username: p.username || 'Member',
            avatar_key: resolvedComposite,
            avatar_url: p.avatar_url || null
          });
        }
      });
    }

    const likesCountMap = {};
    const likedByMeSet = new Set();
    if (Array.isArray(reviewLikeRows)) {
      reviewLikeRows.forEach((l) => {
        if (!l.review_id) return;
        likesCountMap[l.review_id] = (likesCountMap[l.review_id] || 0) + 1;
        if (state.session && l.user_id === state.session.userId) {
          likedByMeSet.add(l.review_id);
        }
      });
    }
    if (Array.isArray(noteLikeRows)) {
      noteLikeRows.forEach((l) => {
        if (!l.note_id) return;
        likesCountMap[l.note_id] = (likesCountMap[l.note_id] || 0) + 1;
        if (state.session && l.user_id === state.session.userId) {
          likedByMeSet.add(l.note_id);
        }
      });
    }

    const mapped = valid.map((r) => {
      const anime = r.anime || {};
      const prof = profMap[r.user_id] || {
        username: r.user_id === state.session?.userId ? state.profile.username : 'Member',
        avatar_key: r.user_id === state.session?.userId ? state.profile.avatarKey : 'avatar_1'
      };
      const animeGenres = Array.isArray(anime.genres) ? anime.genres : [];
      return {
        id: r.id,
        source: r.source,
        user_id: r.user_id,
        username: prof.username,
        avatar_key: prof.avatar_key,
        anime_id: Number(r.anime_id),
        anime_title:
          (anime.title_english && anime.title_english !== 'null' ? anime.title_english : '') ||
          anime.title_romaji ||
          'Anime',
        anime_cover_image: anime.cover_image || '',
        genres: animeGenres,
        is_adult: isHentaiOrAdultAnime(animeGenres, Boolean(anime.is_adult)),
        rating: r.rating,
        review_notes: r.review_notes,
        updated_at: r.updated_at || new Date().toISOString(),
        likes_count: likesCountMap[r.id] || 0,
        liked_by_me: likedByMeSet.has(r.id)
      };
    });

    // Ensure active signed-in member's own local Library notes/ratings are immediately present
    if (state.session && state.profile.memberStatus !== 'left' && state.profile.memberStatus !== 'removed') {
      state.vault.forEach((v) => {
        const hasRating = v.rating10 !== null && Number(v.rating10) > 0;
        const hasNote = String(v.reviewNotes || '').trim().length > 0;
        if (!hasRating && !hasNote) return;
        const alreadyIdx = mapped.findIndex(
          (m) => m.user_id === state.session.userId && Number(m.anime_id) === Number(v.animeId)
        );
        if (alreadyIdx === -1) {
          const vGenres = Array.isArray(v.genres) ? v.genres : [];
          mapped.unshift({
            id: `local_${state.session.userId}_${v.animeId}`,
            source: 'user_anime',
            user_id: state.session.userId,
            username: state.profile.username || 'Member',
            avatar_key: state.profile.avatarKey || 'avatar_1',
            anime_id: Number(v.animeId),
            anime_title: v.titleEnglish || v.title || 'Anime',
            anime_cover_image: v.coverImage || '',
            genres: vGenres,
            is_adult: isHentaiOrAdultAnime(vGenres, Boolean(v.isAdult)),
            rating: hasRating ? Number(v.rating10) : null,
            review_notes: String(v.reviewNotes || '').trim(),
            updated_at: new Date().toISOString(),
            likes_count: 0,
            liked_by_me: false
          });
        }
      });
    }

    state.communityReviews = mapped;
  } catch (_) {}
}

// Group all community reviews/notes by anime_id so every anime has strictly ONE template card
function groupCommunityReviewsByAnime(reviews) {
  const byAnime = new Map();
  (reviews || []).forEach((rev) => {
    if (!rev || !rev.anime_id) return;
    const hasRating = rev.rating !== null && Number(rev.rating) > 0;
    const hasNote = String(rev.review_notes || '').trim().length > 0;
    if (!hasRating && !hasNote) return;
    const aid = Number(rev.anime_id);
    if (!byAnime.has(aid)) {
      byAnime.set(aid, []);
    }
    byAnime.get(aid).push(rev);
  });

  const groups = [];
  byAnime.forEach((animeReviews, animeId) => {
    // Deduplicate per user_id inside the same anime (keep most recent)
    const sorted = [...animeReviews].sort((a, b) =>
      String(b.updated_at || '').localeCompare(String(a.updated_at || ''))
    );
    const seenUsers = new Set();
    const uniqueUserReviews = [];
    sorted.forEach((r) => {
      const uid = r.user_id || r.id;
      if (seenUsers.has(uid)) return;
      seenUsers.add(uid);
      uniqueUserReviews.push(r);
    });

    if (uniqueUserReviews.length === 0) return;

    const first = uniqueUserReviews[0];
    const rated = uniqueUserReviews.filter((r) => r.rating !== null && Number(r.rating) > 0);
    const noted = uniqueUserReviews.filter((r) => String(r.review_notes || '').trim().length > 0);
    const avgRating10 =
      rated.length > 0
        ? rated.reduce((acc, r) => acc + Number(r.rating || 0), 0) / rated.length
        : null;
    const leadReview = noted.length > 0 ? noted[0] : uniqueUserReviews[0];
    const totalLikesCount = uniqueUserReviews.reduce((acc, r) => acc + Number(r.likes_count || 0), 0);

    const mergedGenres = Array.from(
      new Set(uniqueUserReviews.flatMap((r) => (Array.isArray(r.genres) ? r.genres : [])))
    );
    const localMatch =
      state.vault.find((v) => Number(v.animeId) === animeId) ||
      state.discover.find((d) => Number(d.animeId) === animeId);
    const allGenres = Array.from(
      new Set([...mergedGenres, ...(Array.isArray(localMatch?.genres) ? localMatch.genres : [])])
    );
    const isAdultFlag =
      uniqueUserReviews.some((r) => Boolean(r.is_adult)) ||
      isHentaiOrAdultAnime(allGenres, Boolean(localMatch?.isAdult));

    groups.push({
      anime_id: animeId,
      anime_title: first.anime_title || 'Anime',
      anime_cover_image: first.anime_cover_image || '',
      genres: allGenres,
      is_adult: isAdultFlag,
      average_rating10: avgRating10,
      rated_count: rated.length,
      noted_count: noted.length,
      total_count: uniqueUserReviews.length,
      latest_updated_at: first.updated_at || '',
      total_likes_count: totalLikesCount,
      lead_review: leadReview,
      reviews: uniqueUserReviews
    });
  });

  return groups;
}

function normalizeCommunityUserKey(revOrUser) {
  if (!revOrUser) return '';
  const uid = String(revOrUser.user_id || revOrUser.userId || '').trim();
  const uname = String(revOrUser.username || '').trim().toLowerCase();
  return uid || uname;
}

function extractCommunityPosters(reviews) {
  const posterMap = new Map();
  const allGroups = groupCommunityReviewsByAnime(reviews);
  allGroups.forEach((grp) => {
    (grp.reviews || []).forEach((r) => {
      const key = normalizeCommunityUserKey(r);
      const username = String(r.username || 'Member').trim() || 'Member';
      if (!key) return;
      const existing = posterMap.get(key);
      if (existing) {
        existing.animeIds.add(Number(grp.anime_id));
        if (!existing.avatarKey && r.avatar_key) existing.avatarKey = r.avatar_key;
      } else {
        posterMap.set(key, {
          key,
          userId: String(r.user_id || ''),
          username,
          avatarKey: r.avatar_key || 'avatar_1',
          animeIds: new Set([Number(grp.anime_id)])
        });
      }
    });
  });
  return Array.from(posterMap.values())
    .map((p) => ({
      key: p.key,
      userId: p.userId,
      username: p.username,
      avatarKey: p.avatarKey,
      postCount: p.animeIds.size
    }))
    .sort((a, b) => b.postCount - a.postCount || a.username.localeCompare(b.username));
}

function doesReviewMatchSelectedUsers(rev, selectedUsers) {
  if (!Array.isArray(selectedUsers) || selectedUsers.length === 0) return true;
  const revKey = normalizeCommunityUserKey(rev);
  const revUname = String(rev?.username || '').trim().toLowerCase();
  return selectedUsers.some(
    (u) =>
      (u.key && u.key === revKey) ||
      (u.username && String(u.username).trim().toLowerCase() === revUname)
  );
}

async function deleteCollectionCategoryById(colId) {
  if (!colId) return;
  const target = state.collections.find((c) => String(c.id) === String(colId));
  const colName = target?.name || 'Category';
  state.collections = state.collections.filter((c) => String(c.id) !== String(colId));
  if (String(state.myListCollectionFilter || '') === String(colId)) {
    state.myListCollectionFilter = null;
  }
  state.collectionContextMenu = null;
  saveCollections();
  renderApp();
  showToast(`Deleted category "${colName}"`);

  if (state.session && !String(colId).startsWith('local_')) {
    const enc = encodeURIComponent(String(colId));
    await supabaseRequest(`/rest/v1/collection_anime?collection_id=eq.${enc}`, 'DELETE').catch(() => {});
    await supabaseRequest(`/rest/v1/collections?id=eq.${enc}`, 'DELETE').catch(() => {});
  }
}

let discoverFetchToken = 0;
let discoverScrollRaf = null;
let discoverSearchDebounceTimer = null;

function mapDiscoverMediaItem(m) {
  const airingAt = m.nextAiringEpisode?.airingAt || null;
  const nextEpNum = m.nextAiringEpisode?.episode || null;
  const dow = airingAt ? (((new Date(airingAt * 1000).getDay() + 6) % 7) + 1) : ((m.id % 7) + 1);
  const genres = normalizeAnimeGenresWithAdult(m.genres || [], Boolean(m.isAdult));
  const synonyms = Array.isArray(m.synonyms) ? m.synonyms.filter(Boolean) : [];
  const country = m.countryOfOrigin || 'JP';
  const isDonghua = String(country).toUpperCase() === 'CN';
  const officialEnglish = m.title?.english || '';
  const romaji = m.title?.romaji || '';
  const resolvedEnglish = resolveEnglishTitle(officialEnglish, romaji, synonyms, isDonghua) || officialEnglish || romaji || 'Anime';
  const fmt = String(m.format || 'TV').toUpperCase();
  const medStatus = String(m.status || 'RELEASING').toUpperCase();
  const rawEps = m.episodes && Number(m.episodes) > 0 ? Number(m.episodes) : null;
  // Normalize totalEpisodes: Movies default to 1; ongoing Donghua with stale lower episode cap become ongoing (null)
  const effectiveTotalEpisodes =
    fmt === 'MOVIE'
      ? (rawEps || 1)
      : medStatus === 'RELEASING' && isDonghua && rawEps && nextEpNum && nextEpNum > rawEps
      ? null
      : rawEps;
  return {
    animeId: m.id,
    title: resolvedEnglish,
    titleEnglish: resolvedEnglish,
    officialTitleEnglish: officialEnglish,
    titleRomaji: romaji,
    titleNative: m.title?.native || '',
    synonyms,
    popularity: m.popularity || 0,
    coverImage: m.coverImage?.extraLarge || m.coverImage?.large || '',
    bannerImage: m.bannerImage || '',
    format: fmt,
    mediaStatus: medStatus,
    duration: m.duration || null,
    seasonYear: m.seasonYear || null,
    countryOfOrigin: country,
    totalEpisodes: effectiveTotalEpisodes,
    averageScore: m.averageScore || null,
    genres,
    isAdult: isHentaiOrAdultAnime(genres, Boolean(m.isAdult)),
    synopsis: (m.description || '').replace(/<[^>]+>/g, ''),
    airingDayOfWeek: dow,
    airingTimeUnix: airingAt,
    nextEpisodeNumber: nextEpNum,
    status: 'watching',
    season: 1,
    watchedEpisodes: 0,
    rating10: null,
    reviewNotes: '',
    isPinned: false,
    isFavorite: false
  };
}

function renderDiscoverResultsShellHtml() {
  if (state.isSearching) {
    return `<div class="app-card" style="text-align:center;padding:48px;"><p style="color:var(--accent-primary);">Searching Anime &amp; Donghua catalog...</p></div>`;
  }
  if (!state.discover.length && state.discoverError) {
    return `<div class="app-card" style="text-align:center;padding:44px 20px;">
      <p style="color:var(--text-primary);font-size:16px;font-weight:700;margin:0 0 8px;">Catalog temporarily busy</p>
      <p style="color:var(--text-muted);font-size:13px;margin:0 0 18px;">${escapeHtml(state.discoverError)}</p>
      <button type="button" class="capsule-pill active" id="btn-discover-retry" style="height:44px;padding:0 22px;">Retry Search</button>
    </div>`;
  }
  if (!state.discover.length) {
    const q = state.searchQuery.trim();
    return `<div class="app-card" style="text-align:center;padding:44px 20px;">
      <p style="color:var(--text-primary);font-size:16px;font-weight:700;margin:0 0 6px;">${q ? `No matches found for "${escapeHtml(q)}"` : 'No titles available'}</p>
      <p style="color:var(--text-muted);font-size:13px;margin:0;">Try searching with English or alternate Donghua keywords, or clear active filters.</p>
    </div>`;
  }
  return `
    <div class="poster-grid" id="discover-poster-grid">${state.discover.map((it) => renderPosterTrackerCard(it, false)).join('')}</div>
    <div id="discover-sentinel-wrap">${renderDiscoverFooterHtml()}</div>
  `;
}

function updateDiscoverResultsShellDom() {
  const countEl = document.getElementById('discover-count-label');
  if (countEl) {
    countEl.textContent = `${state.discover.length} TITLES`;
  }
  const clearBtn = document.getElementById('btn-clear-disc-search');
  if (clearBtn) {
    clearBtn.style.display = state.searchQuery ? '' : 'none';
  }
  const shell = document.getElementById('discover-results-shell');
  if (shell) {
    shell.innerHTML = renderDiscoverResultsShellHtml();
    bindCardInteractionHandlers(shell);
    const btnMore = document.getElementById('btn-discover-load-more');
    if (btnMore) {
      btnMore.onclick = () => fetchDiscoverCatalog(true);
    }
    const btnRetry = document.getElementById('btn-discover-retry');
    if (btnRetry) {
      btnRetry.onclick = () => fetchDiscoverCatalog(false, { inPlace: true, forceRefresh: true });
    }
    return true;
  }
  return false;
}

function renderDiscoverFooterHtml() {
  if (state.isSearching) return '';
  if (state.discoverLoadingMore) {
    return `<div id="discover-load-sentinel" style="text-align:center;padding:24px 16px;color:var(--accent-primary);font-size:13px;font-weight:600;letter-spacing:0.6px;">Loading more anime...</div>`;
  }
  if (state.discoverHasNextPage && state.discover.length > 0) {
    return `
      <div id="discover-load-sentinel" style="display:flex;justify-content:center;padding:20px 16px;">
        <button type="button" class="capsule-pill" id="btn-discover-load-more" style="height:44px;padding:0 22px;color:var(--accent-primary);border-color:var(--accent-border);">
          Load More Anime
        </button>
      </div>
    `;
  }
  if (state.discover.length > 0) {
    return `<div id="discover-load-sentinel" style="text-align:center;padding:20px 16px;color:var(--text-muted);font-size:12px;">All ${state.discover.length} matching titles loaded</div>`;
  }
  return `<div id="discover-load-sentinel"></div>`;
}

function updateDiscoverFooterDom() {
  const countEl = document.getElementById('discover-count-label');
  if (countEl) {
    countEl.textContent = `${state.discover.length} TITLES`;
  }
  const sentinelWrap = document.getElementById('discover-sentinel-wrap');
  if (sentinelWrap) {
    sentinelWrap.innerHTML = renderDiscoverFooterHtml();
    const btnMore = document.getElementById('btn-discover-load-more');
    if (btnMore) {
      btnMore.onclick = () => fetchDiscoverCatalog(true);
    }
  }
}

function bindCardInteractionHandlers(rootEl) {
  if (!rootEl) return;
  rootEl.querySelectorAll('[data-open-detail]').forEach((el) => {
    el.onclick = (e) => {
      e.stopPropagation();
      if (state._suppressNextCardClick) {
        state._suppressNextCardClick = false;
        return;
      }
      openAnimeDetail(Number(el.getAttribute('data-open-detail')));
    };
  });

  rootEl.querySelectorAll('[data-card-anime-id]').forEach((card) => {
    const aid = Number(card.getAttribute('data-card-anime-id'));
    card.oncontextmenu = (e) => {
      e.preventDefault();
      e.stopPropagation();
      state.contextMenu = { animeId: aid, x: e.clientX, y: e.clientY };
      renderApp();
    };

    let touchTimer = null;
    let startX = 0;
    let startY = 0;
    card.ontouchstart = (e) => {
      if (!e.touches || e.touches.length !== 1) return;
      startX = e.touches[0].clientX;
      startY = e.touches[0].clientY;
      touchTimer = setTimeout(() => {
        touchTimer = null;
        state._suppressNextCardClick = true;
        state.contextMenu = { animeId: aid, x: startX, y: startY };
        renderApp();
      }, 500);
    };
    card.ontouchmove = (e) => {
      if (!touchTimer || !e.touches || e.touches.length !== 1) return;
      const dx = Math.abs(e.touches[0].clientX - startX);
      const dy = Math.abs(e.touches[0].clientY - startY);
      if (dx > 10 || dy > 10) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };
    card.ontouchend = () => {
      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };
    card.ontouchcancel = () => {
      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };
  });

  rootEl.querySelectorAll('[data-quick-add]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-quick-add'));
      const existing = state.vault.find((v) => Number(v.animeId) === id);
      if (existing) {
        state.quickEditAnime = { ...existing };
        renderApp();
        return;
      }
      const disc = state.discover.find((d) => Number(d.animeId) === id);
      if (disc) {
        await addToHome(disc);
      }
    };
  });

  rootEl.querySelectorAll('[data-quick-edit]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-quick-edit'));
      const item = state.vault.find((v) => Number(v.animeId) === id);
      if (item) {
        state.quickEditAnime = { ...item };
        renderApp();
      }
    };
  });
}

function checkDiscoverInfiniteScroll() {
  if (state.tab !== 'discover') return;
  if (state.isSearching || state.discoverLoadingMore || !state.discoverHasNextPage) return;
  if (!state.discover.length) return;

  const grid = document.getElementById('discover-poster-grid');
  if (!grid || !grid.children.length) return;

  const cards = grid.children;
  const firstTop = cards[0].offsetTop;
  let cols = 0;
  for (let i = 0; i < cards.length; i++) {
    if (Math.abs(cards[i].offsetTop - firstTop) <= 8) cols++;
    else break;
  }
  cols = Math.max(1, cols);

  // Automatically load the next page once the user crosses the 3rd row of the active batch
  const triggerIdx = Math.min(
    cards.length - 1,
    Math.max(0, state.discoverLastPageStartIndex + cols * 3 - 1)
  );
  const triggerCard = cards[triggerIdx];
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 800;

  let shouldLoad = false;
  if (triggerCard) {
    const rect = triggerCard.getBoundingClientRect();
    // Trigger as soon as the 3rd row crosses into / above the lower viewport
    if (rect.top <= viewportHeight + 120) {
      shouldLoad = true;
    }
  }
  if (!shouldLoad) {
    const sentinel = document.getElementById('discover-load-sentinel');
    if (sentinel) {
      const sRect = sentinel.getBoundingClientRect();
      if (sRect.top <= viewportHeight + 480) {
        shouldLoad = true;
      }
    }
  }

  if (shouldLoad) {
    fetchDiscoverCatalog(true);
  }
}

window.addEventListener(
  'scroll',
  () => {
    if (state.tab !== 'discover') return;
    if (discoverScrollRaf) return;
    discoverScrollRaf = requestAnimationFrame(() => {
      discoverScrollRaf = null;
      checkDiscoverInfiniteScroll();
    });
  },
  { passive: true }
);

const discoverSearchCache = new Map();

async function fetchJikanSearchFallback(query, format, status, page, perPage, showAdult) {
  try {
    const params = new URLSearchParams({
      q: query,
      page: String(page),
      limit: String(Math.min(25, Math.max(1, perPage)))
    });
    if (!showAdult) params.set('sfw', 'true');
    const fmtMap = { TV: 'tv', MOVIE: 'movie', OVA: 'ova', ONA: 'ona', SPECIAL: 'special' };
    if (format && fmtMap[format]) params.set('type', fmtMap[format]);
    const stMap = { RELEASING: 'airing', FINISHED: 'complete', NOT_YET_RELEASED: 'upcoming' };
    if (status && stMap[status]) params.set('status', stMap[status]);

    const resp = await fetch(`https://api.jikan.moe/v4/anime?${params.toString()}`);
    if (!resp.ok) return null;
    const json = await resp.json();
    const list = Array.isArray(json?.data) ? json.data : [];
    const hasNext = Boolean(json?.pagination?.has_next_page) && list.length > 0;
    const mapped = list
      .map((item) => {
        const malId = Number(item?.mal_id || 0);
        if (!malId) return null;
        const jType = String(item?.type || 'TV').toUpperCase();
        const mappedFmt =
          jType === 'MOVIE'
            ? 'MOVIE'
            : jType === 'OVA'
            ? 'OVA'
            : jType === 'ONA'
            ? 'ONA'
            : jType === 'SPECIAL' || jType === 'TV SPECIAL'
            ? 'SPECIAL'
            : 'TV';
        const jStatus = String(item?.status || '').toLowerCase();
        const mappedStatus = jStatus.includes('currently airing')
          ? 'RELEASING'
          : jStatus.includes('not yet')
          ? 'NOT_YET_RELEASED'
          : 'FINISHED';
        const romaji = item?.title || '';
        const isDonghua = mappedFmt === 'ONA' && isLikelyPinyin(romaji);
        return mapDiscoverMediaItem({
          id: malId,
          title: {
            english: item?.title_english || '',
            romaji,
            native: item?.title_japanese || ''
          },
          synonyms: Array.isArray(item?.title_synonyms) ? item.title_synonyms : [],
          popularity: Number(item?.members || 0),
          coverImage: {
            extraLarge: item?.images?.jpg?.large_image_url || item?.images?.jpg?.image_url || '',
            large: item?.images?.jpg?.image_url || ''
          },
          bannerImage: '',
          format: mappedFmt,
          status: mappedStatus,
          seasonYear: item?.year || null,
          episodes: item?.episodes || (mappedFmt === 'MOVIE' ? 1 : null),
          averageScore: item?.score ? Math.round(Number(item.score) * 10) : null,
          countryOfOrigin: isDonghua ? 'CN' : 'JP',
          genres: Array.isArray(item?.genres) ? item.genres.map((g) => g?.name).filter(Boolean) : [],
          isAdult: false,
          description: item?.synopsis || ''
        });
      })
      .filter(Boolean);
    return { items: mapped, hasNext };
  } catch (_) {
    return null;
  }
}

async function fetchDiscoverCatalog(loadMore = false, options = {}) {
  const inPlace = Boolean(options?.inPlace);
  const forceRefresh = Boolean(options?.forceRefresh);
  if (loadMore) {
    if (state.isSearching || state.discoverLoadingMore || !state.discoverHasNextPage) return;
    state.discoverLoadingMore = true;
    updateDiscoverFooterDom();
  } else {
    state.isSearching = true;
    state.discoverError = '';
    state.discoverPage = 1;
    state.discoverHasNextPage = true;
    state.discoverLoadingMore = false;
    state.discoverLastPageStartIndex = 0;
    if (!(inPlace && state.tab === 'discover' && updateDiscoverResultsShellDom())) {
      renderApp();
    }
  }

  const targetPage = loadMore ? state.discoverPage + 1 : 1;
  const myToken = loadMore ? discoverFetchToken : ++discoverFetchToken;
  const trimmedSearch = state.searchQuery.trim();
  const baseSort = state.discoverSort || 'POPULARITY_DESC';
  const effectiveSort = trimmedSearch
    ? ['SEARCH_MATCH', ...(baseSort !== 'SEARCH_MATCH' ? [baseSort] : [])]
    : [baseSort];

  const cacheKey = JSON.stringify({
    q: trimmedSearch.toLowerCase(),
    mode: state.discoverMode,
    sort: baseSort,
    fmt: state.discoverFormat,
    st: state.discoverStatus,
    genre: state.discoverGenre || '',
    adult: Boolean(state.prefs.showAdultContent),
    page: targetPage
  });

  if (!forceRefresh) {
    const cached = discoverSearchCache.get(cacheKey);
    if (cached && Date.now() - cached.ts < 60_000) {
      if (myToken !== discoverFetchToken) return;
      if (loadMore) {
        const existingIds = new Set(state.discover.map((d) => Number(d.animeId)));
        const uniqueNew = cached.items.filter((item) => !existingIds.has(Number(item.animeId)));
        const prevCount = state.discover.length;
        state.discoverPage = targetPage;
        state.discoverHasNextPage = cached.hasNext;
        state.discoverLoadingMore = false;
        if (uniqueNew.length > 0) {
          state.discoverLastPageStartIndex = prevCount;
          state.discover = [...state.discover, ...uniqueNew];
        }
        if (state.tab === 'discover' && updateDiscoverResultsShellDom()) {
          updateDiscoverFooterDom();
        } else {
          renderApp();
        }
      } else {
        state.discover = cached.items;
        state.discoverError = '';
        state.discoverPage = 1;
        state.discoverHasNextPage = cached.hasNext;
        state.discoverLastPageStartIndex = 0;
        state.isSearching = false;
        if (!(inPlace && state.tab === 'discover' && updateDiscoverResultsShellDom())) {
          renderApp();
        }
        requestAnimationFrame(() => checkDiscoverInfiniteScroll());
      }
      return;
    }
  }

  const baseVariables = {
    page: targetPage,
    perPage: 30,
    sort: effectiveSort
  };
  // CRITICAL: Only pass isAdult: false when Safe Mode is ON; OMIT isAdult when adult content is enabled
  // (Passing isAdult: true tells AniList to return ONLY adult/hentai entries and hides all normal anime/donghua!)
  if (!state.prefs.showAdultContent) baseVariables.isAdult = false;
  if (trimmedSearch) baseVariables.search = trimmedSearch;
  if (state.discoverFormat !== 'ALL') baseVariables.format = state.discoverFormat;
  if (state.discoverStatus !== 'ALL') baseVariables.status = state.discoverStatus;
  if (state.discoverGenre) baseVariables.genre = state.discoverGenre;
  if (state.discoverMode === 'donghua') baseVariables.countryOfOrigin = 'CN';
  if (state.discoverMode === 'seasonal') {
    baseVariables.season = getCurrentAnimeSeason();
    baseVariables.seasonYear = new Date().getFullYear();
  }

  const extraQueries =
    !loadMore && trimmedSearch.length >= 2
      ? expandSearchQueries(trimmedSearch, state.discoverMode === 'donghua', 2)
      : [];

  const mediaFields = `
    id
    title { english romaji native }
    synonyms
    popularity
    coverImage { extraLarge large }
    bannerImage
    format
    status
    seasonYear
    episodes
    duration
    averageScore
    countryOfOrigin
    genres
    isAdult
    description(asHtml: false)
    nextAiringEpisode { airingAt episode }
  `;

  const varDecls = [
    '$page: Int',
    '$perPage: Int',
    '$search: String',
    '$sort: [MediaSort]',
    '$format: MediaFormat',
    '$status: MediaStatus',
    '$genre: String',
    '$countryOfOrigin: CountryCode',
    '$season: MediaSeason',
    '$seasonYear: Int',
    '$isAdult: Boolean'
  ];
  const aliasBlocks = extraQueries
    .map((alias, idx) => {
      varDecls.push(`$alias${idx}: String`);
      baseVariables[`alias${idx}`] = alias;
      return `
        alias${idx}: Page(page: 1, perPage: 12) {
          media(
            type: ANIME,
            search: $alias${idx},
            sort: [SEARCH_MATCH, POPULARITY_DESC],
            format: $format,
            status: $status,
            genre: $genre,
            countryOfOrigin: $countryOfOrigin,
            season: $season,
            seasonYear: $seasonYear,
            isAdult: $isAdult
          ) {
            ${mediaFields}
          }
        }
      `;
    })
    .join('\n');

  // Execute primary + dictionary aliases in ONE single batched GraphQL HTTP request (prevents AniList 429 bursts!)
  const batchedGql = `
    query(${varDecls.join(', ')}) {
      primary: Page(page: $page, perPage: $perPage) {
        pageInfo {
          hasNextPage
          currentPage
        }
        media(
          type: ANIME,
          search: $search,
          sort: $sort,
          format: $format,
          status: $status,
          genre: $genre,
          countryOfOrigin: $countryOfOrigin,
          season: $season,
          seasonYear: $seasonYear,
          isAdult: $isAdult
        ) {
          ${mediaFields}
        }
      }
      ${aliasBlocks}
    }
  `;

  const runBatchedAniListQuery = async () => {
    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        const resp = await fetch('https://graphql.anilist.co', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ query: batchedGql, variables: baseVariables })
        });
        if (resp.status === 429 || (resp.status >= 500 && resp.status < 600)) {
          if (attempt < 2) {
            const retryAfter = Number(resp.headers.get('Retry-After') || 0);
            const waitMs = retryAfter >= 1 && retryAfter <= 4 ? retryAfter * 1000 : 850 * (attempt + 1);
            await new Promise((r) => setTimeout(r, waitMs));
            continue;
          }
          return null;
        }
        if (!resp.ok) return null;
        return await resp.json();
      } catch (_) {
        if (attempt < 2) await new Promise((r) => setTimeout(r, 650 * (attempt + 1)));
      }
    }
    return null;
  };

  try {
    const json = await runBatchedAniListQuery();
    if (myToken !== discoverFetchToken) return;

    let mapped = [];
    let hasNext = false;
    let sourceSucceeded = false;

    if (json?.data?.primary) {
      sourceSucceeded = true;
      const primaryPage = json.data.primary;
      hasNext = Boolean(primaryPage?.pageInfo?.hasNextPage) && (primaryPage?.media?.length || 0) > 0;
      const mergedById = new Map();
      const pages = [primaryPage, ...extraQueries.map((_, idx) => json.data[`alias${idx}`])];
      for (const p of pages) {
        const list = p?.media || [];
        for (const m of list) {
          if (m?.id && !mergedById.has(Number(m.id))) {
            mergedById.set(Number(m.id), mapDiscoverMediaItem(m));
          }
        }
      }
      mapped = Array.from(mergedById.values());
    } else if (trimmedSearch) {
      // Automatic Jikan v4 fallback whenever AniList rate-limits (429) or is temporarily unreachable
      const jikan = await fetchJikanSearchFallback(
        trimmedSearch,
        state.discoverFormat !== 'ALL' ? state.discoverFormat : null,
        state.discoverStatus !== 'ALL' ? state.discoverStatus : null,
        targetPage,
        24,
        Boolean(state.prefs.showAdultContent)
      );
      if (myToken !== discoverFetchToken) return;
      if (jikan) {
        sourceSucceeded = true;
        mapped = jikan.items;
        hasNext = jikan.hasNext;
      }
    }

    if (!loadMore && trimmedSearch && mapped.length > 0) {
      mapped.sort((a, b) => {
        const scoreA = scoreKeywordMatch(trimmedSearch, a.title, a.officialTitleEnglish, a.titleRomaji, a.synonyms);
        const scoreB = scoreKeywordMatch(trimmedSearch, b.title, b.officialTitleEnglish, b.titleRomaji, b.synonyms);
        if (scoreB !== scoreA) return scoreB - scoreA;
        return (b.popularity || 0) - (a.popularity || 0);
      });
    }

    if (sourceSucceeded) {
      if (discoverSearchCache.size > 60) discoverSearchCache.clear();
      discoverSearchCache.set(cacheKey, { ts: Date.now(), items: mapped, hasNext });
    }

    if (loadMore) {
      const existingIds = new Set(state.discover.map((d) => Number(d.animeId)));
      const uniqueNew = mapped.filter((item) => !existingIds.has(Number(item.animeId)));
      const prevCount = state.discover.length;
      state.discoverPage = targetPage;
      state.discoverHasNextPage = hasNext;
      state.discoverLoadingMore = false;

      if (uniqueNew.length > 0) {
        state.discoverLastPageStartIndex = prevCount;
        state.discover = [...state.discover, ...uniqueNew];
        const grid = document.getElementById('discover-poster-grid');
        if (state.tab === 'discover' && grid) {
          const tempWrap = document.createElement('div');
          tempWrap.innerHTML = uniqueNew.map((it) => renderPosterTrackerCard(it, false)).join('');
          bindCardInteractionHandlers(tempWrap);
          const frag = document.createDocumentFragment();
          while (tempWrap.firstChild) {
            frag.appendChild(tempWrap.firstChild);
          }
          grid.appendChild(frag);
          updateDiscoverFooterDom();
        } else if (state.tab === 'discover') {
          renderApp();
        }
      } else {
        updateDiscoverFooterDom();
      }
    } else {
      state.discover = mapped;
      state.discoverError = sourceSucceeded
        ? ''
        : 'AniList rate-limited or could not be reached right now. Tap Retry to search again.';
      state.discoverPage = 1;
      state.discoverHasNextPage = hasNext;
      state.discoverLastPageStartIndex = 0;
      state.isSearching = false;
      if (!(inPlace && state.tab === 'discover' && updateDiscoverResultsShellDom())) {
        renderApp();
      }
      if (sourceSucceeded) {
        requestAnimationFrame(() => checkDiscoverInfiniteScroll());
      }
    }
  } catch (_) {
    if (myToken !== discoverFetchToken) return;
    if (loadMore) {
      state.discoverLoadingMore = false;
      updateDiscoverFooterDom();
    } else {
      state.discoverError = 'Unable to reach the anime catalog right now. Tap Retry to try again.';
      state.isSearching = false;
      if (!(inPlace && state.tab === 'discover' && updateDiscoverResultsShellDom())) {
        renderApp();
      }
    }
  }
}

const anilistSeasonNodeCache = new Map();
let detailFetchToken = 0;

const ANILIST_SEASON_BATCH_QUERY = `
  query($ids: [Int]) {
    Page(page: 1, perPage: 50) {
      media(id_in: $ids, type: ANIME) {
        id
        type
        countryOfOrigin
        title { english romaji userPreferred }
        synonyms
        coverImage { extraLarge large medium }
        format
        status
        episodes
        duration
        seasonYear
        startDate { year month day }
        endDate { year month day }
        averageScore
        relations {
          edges {
            relationType
            node {
              id
              type
              countryOfOrigin
              title { english romaji userPreferred }
              synonyms
              coverImage { extraLarge large medium }
              format
              status
              episodes
              duration
              seasonYear
              startDate { year month day }
              endDate { year month day }
              averageScore
            }
          }
        }
      }
    }
  }
`;

const WEB_NON_SEASON_FORMATS = new Set(['MOVIE', 'OVA', 'SPECIAL', 'MUSIC', 'MANGA', 'NOVEL', 'ONE_SHOT']);
const WEB_MOVIE_OVA_SPECIAL_FORMATS = new Set(['MOVIE', 'OVA', 'SPECIAL']);
const WEB_MAIN_EPISODIC_FORMATS = new Set(['TV', 'ONA', 'TV_SHORT']);
const WEB_MAINLINE_TRAVERSAL_RELS = new Set(['SEQUEL', 'PREQUEL', 'PARENT']);
const WEB_SIDE_COLLECTION_RELS = new Set(['SIDE_STORY', 'SPIN_OFF', 'SUMMARY', 'ALTERNATIVE']);
const WEB_CONTINUOUS_MAX_GAP_DAYS = 21;
const FRANCHISE_SEASON_OVERRIDE_STORAGE_KEY = 'anime_universe_season_overrides_v1';

const WEB_EXPLICIT_SEASON_TITLE_RE = /(?:\bseason\s*(\d{1,2})\b|\b(\d{1,2})(?:st|nd|rd|th)\s+season\b|\bs(\d{1,2})\b|\bdi\s*(\d{1,2})\s*ji\b|\bdai\s*(\d{1,2})\s*ki\b|\bfinal\s+season\b|\bfinal\s+chapters?\b)/i;
const WEB_EXPLICIT_SEASON_NUM_RE = /(?:\bseason\s*(\d{1,2})\b|\b(\d{1,2})(?:st|nd|rd|th)\s+season\b|\bs(\d{1,2})\b|\bdi\s*(\d{1,2})\s*ji\b|\bdai\s*(\d{1,2})\s*ki\b)/i;
const WEB_PINYIN_SEASON_NUM_RE = /\bdi\s*(yi|er|san|si|wu|liu|qi|ba|jiu|shi)\s*ji\b/i;
const WEB_EXPLICIT_PART_RE = /(?:\bpart\s*(\d{1,2})\b|\bcour\s*(\d{1,2})\b|\b(\d{1,2})(?:st|nd|rd|th)\s+(?:part|cour)\b)/i;
const WEB_SECOND_HALF_RE = /\b(?:second\s+half|2nd\s+half|xia\s*ji|xia\s*pian)\b/i;
const WEB_ANNUAL_RE = /\b(?:nian\s*fan|annual\s+(?:edition|season))\b/i;
const WEB_TRAILING_ROMAN_RE = /(?:^|\s)(II|III|IV|V|VI)(?:$|[:\-])/;
const WEB_NON_SEASON_BEFORE_ROMAN_RE = /\b(?:part|cour|movie|film|special|ova|oad|episode|ep|chapter|act|vol|volume)\s*$/i;
const WEB_NON_SEASON_SIDE_RE = /(?:\bmini\s*theater\b|\bxiao\s*juchang\b|\bchibi\b|\bq[- ]?ban\b|\bpetite\b|\bmini\s*anime\b|\bshort\s*theater\b|\bgekijou\b|\blittle\s*theater\b|\brecap\b|\bsummary\b|\bdigest\b|\bpreview\b|\bteaser\b|\btrailer\b|\bpv\b|\bcm\b|\bcommercial\b|\bpromotional\b|\bspecial\s+program\b|\bmusic\s+video\b|\btebie\s*pian\b|\bfanwai\b|\bomake\b|\bpicture\s*drama\b|\bprologue\b|\bepisode\s*0\b|\bep\.?\s*0\b|\bova\b|\boad\b|\bmovie\b|\bjuchang\s*ban\b|\bgekijouban\b|\bspecials?\b|\bspecial\s*\d+\b|\bsan\s*nian\s*zhi\s*yue\b|\bthree[- ]year\s+agreement\b|\byuan\s*qi\b|\borigin\s*(?::|$))/i;
const WEB_BASE_TITLE_SPLIT_SUFFIX_RE = /(?:[:\-\s]+(?:(?:season\s*1\s*)?(?:part|cour)\s*\d{1,2}|\d{1,2}(?:st|nd|rd|th)\s+(?:part|cour)|second\s+half|2nd\s+half|xia\s*ji|xia\s*pian|nian\s*fan|annual\s*(?:edition|season)?)|(?<!\bseason)(?<!\bs)(?<!\bno\.)(?<!\bno)(?<!#)\s+[2-9])\s*$/i;

function getFranchiseSeasonOverride(franchiseId) {
  if (!franchiseId) return 'auto';
  try {
    const raw = localStorage.getItem(FRANCHISE_SEASON_OVERRIDE_STORAGE_KEY);
    if (!raw) return 'auto';
    const parsed = JSON.parse(raw);
    const mode = parsed && parsed[String(franchiseId)];
    if (mode === 'merge_one' || mode === 'split_all') return mode;
  } catch (_) {}
  return 'auto';
}

function setFranchiseSeasonOverride(franchiseId, mode) {
  if (!franchiseId) return;
  try {
    const raw = localStorage.getItem(FRANCHISE_SEASON_OVERRIDE_STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : {};
    if (mode === 'merge_one' || mode === 'split_all') {
      parsed[String(franchiseId)] = mode;
    } else {
      delete parsed[String(franchiseId)];
    }
    localStorage.setItem(FRANCHISE_SEASON_OVERRIDE_STORAGE_KEY, JSON.stringify(parsed));
  } catch (_) {}
}

function extractWebSeasonAndPart(node) {
  const primaryList = [node?.title?.english, node?.title?.userPreferred, node?.title?.romaji].filter(Boolean);
  const primary = primaryList.join(' | ');
  const combined = [...primaryList, ...(Array.isArray(node?.synonyms) ? node.synonyms : [])].filter(Boolean).join(' | ');
  const isAnnualEdition = WEB_ANNUAL_RE.test(combined);
  const hasFinalSeasonMarker = /\b(?:final\s+season|final\s+chapters?)\b/i.test(primary);

  let seasonNumber = null;
  const pinyinMap = { yi: 1, er: 2, san: 3, si: 4, wu: 5, liu: 6, qi: 7, ba: 8, jiu: 9, shi: 10 };
  for (const src of [primary, combined]) {
    if (seasonNumber !== null) break;
    const m = WEB_EXPLICIT_SEASON_NUM_RE.exec(src);
    if (m) {
      const raw = m.slice(1).find(Boolean);
      const n = raw ? Number.parseInt(raw, 10) : NaN;
      if (Number.isFinite(n) && n >= 1 && n <= 20) seasonNumber = n;
    }
    if (seasonNumber === null) {
      const pm = WEB_PINYIN_SEASON_NUM_RE.exec(src);
      if (pm && pinyinMap[pm[1].toLowerCase()]) seasonNumber = pinyinMap[pm[1].toLowerCase()];
    }
  }

  if (seasonNumber === null && !WEB_NON_SEASON_SIDE_RE.test(primary)) {
    const romanMap = { II: 2, III: 3, IV: 4, V: 5, VI: 6 };
    for (const t of primaryList) {
      const rm = WEB_TRAILING_ROMAN_RE.exec(t);
      if (rm) {
        const before = t.slice(0, rm.index).trim();
        if (!WEB_NON_SEASON_BEFORE_ROMAN_RE.test(before) && romanMap[rm[1]]) {
          seasonNumber = romanMap[rm[1]];
          break;
        }
      }
    }
  }

  let partNumber = null;
  for (const src of [primary, combined]) {
    if (partNumber !== null) break;
    const pm = WEB_EXPLICIT_PART_RE.exec(src);
    if (pm) {
      const raw = pm.slice(1).find(Boolean);
      const p = raw ? Number.parseInt(raw, 10) : NaN;
      if (Number.isFinite(p) && p >= 1 && p <= 6) partNumber = p;
    }
    if (partNumber === null && WEB_SECOND_HALF_RE.test(src)) partNumber = 2;
  }

  return { seasonNumber, partNumber, isAnnualEdition, hasFinalSeasonMarker };
}

function isSeasonCandidateMedia(node, franchiseHasFullSeries = false) {
  if (!node) return false;
  if (node.type && String(node.type).toUpperCase() !== 'ANIME') return false;
  if (String(node.status || '').toUpperCase() === 'CANCELLED') return false;
  const fmt = String(node.format || '').toUpperCase().trim();
  if (WEB_NON_SEASON_FORMATS.has(fmt)) return false;
  if (fmt && !WEB_MAIN_EPISODIC_FORMATS.has(fmt)) return false;

  const primary = [node.title?.english, node.title?.userPreferred, node.title?.romaji].filter(Boolean).join(' | ');
  const hasExplicitSeason = WEB_EXPLICIT_SEASON_TITLE_RE.test(primary);
  const hasSideMarker = WEB_NON_SEASON_SIDE_RE.test(primary);

  if (hasSideMarker && !hasExplicitSeason) return false;

  if (franchiseHasFullSeries) {
    if (fmt === 'TV_SHORT' && !hasExplicitSeason) return false;
    const dur = Number(node.duration || 0);
    if (dur > 0 && dur <= 6 && !hasExplicitSeason) return false;
    const eps = Number(node.episodes || 0);
    const parsed = extractWebSeasonAndPart(node);
    if (
      fmt === 'ONA' &&
      String(node.status || '').toUpperCase() === 'FINISHED' &&
      eps >= 1 &&
      eps <= 3 &&
      parsed.seasonNumber === null &&
      parsed.partNumber === null &&
      !parsed.isAnnualEdition
    ) {
      return false;
    }
  }
  return true;
}

function normalizeWebBaseTitle(rawTitle) {
  if (!rawTitle) return '';
  let cleaned = String(rawTitle).trim();
  for (let i = 0; i < 3; i++) {
    const next = cleaned.replace(WEB_BASE_TITLE_SPLIT_SUFFIX_RE, '').trim();
    if (!next || next === cleaned) break;
    cleaned = next;
  }
  return cleaned
    .toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fff]+/g, ' ')
    .trim();
}

function extractNodeNormalizedBaseTitles(node, rootIsDonghua = false) {
  const snIsDonghua = rootIsDonghua || String(node?.countryOfOrigin || '').toUpperCase() === 'CN';
  const mapped = resolveEnglishTitle(
    node?.title?.english || '',
    node?.title?.romaji || node?.title?.userPreferred || '',
    node?.synonyms || [],
    snIsDonghua
  );
  const candidates = [mapped, node?.title?.english, node?.title?.userPreferred, node?.title?.romaji]
    .map((t) => normalizeWebBaseTitle(t))
    .filter((t) => t.length >= 2);
  return new Set(candidates);
}

function sharesSameWebBaseTitle(prevNode, currNode, rootIsDonghua = false) {
  const prevSet = extractNodeNormalizedBaseTitles(prevNode, rootIsDonghua);
  const currSet = extractNodeNormalizedBaseTitles(currNode, rootIsDonghua);
  if (!prevSet.size || !currSet.size) return false;
  for (const a of prevSet) {
    if (currSet.has(a)) return true;
    for (const b of currSet) {
      if (a.length >= 5 && b.length >= 5 && (a.startsWith(`${b} `) || b.startsWith(`${a} `))) {
        return true;
      }
    }
  }
  return false;
}

function toWebEpochDay(fuzzyDate) {
  const y = Number(fuzzyDate?.year || 0);
  if (!y || y < 1900 || y > 2100) return null;
  const m = Math.min(12, Math.max(1, Number(fuzzyDate?.month || 1)));
  const d = Math.min(31, Math.max(1, Number(fuzzyDate?.day || 1)));
  return Math.floor(Date.UTC(y, m - 1, d) / 86400000);
}

function computeWebBroadcastGapDays(prevNode, currNode) {
  const currStart = toWebEpochDay(currNode?.startDate);
  if (currStart === null) return null;
  const prevEndDirect = toWebEpochDay(prevNode?.endDate);
  if (prevEndDirect !== null) {
    return currStart - prevEndDirect;
  }
  const prevStart = toWebEpochDay(prevNode?.startDate);
  if (prevStart === null) return null;
  const prevEps = Number(prevNode?.episodes || 0);
  if (prevEps > 0) {
    const estimatedEnd = prevStart + Math.max(0, prevEps - 1) * 7;
    return currStart - estimatedEnd;
  }
  return null;
}

function hasExplicitWebSeasonBreakLabel(prevNode, currNode) {
  const prevParsed = extractWebSeasonAndPart(prevNode);
  const currParsed = extractWebSeasonAndPart(currNode);
  if (currParsed.hasFinalSeasonMarker && !prevParsed.hasFinalSeasonMarker) return true;
  if (currParsed.seasonNumber === null) return false;
  const prevSeason = prevParsed.seasonNumber ?? 1;
  return currParsed.seasonNumber >= 2 && currParsed.seasonNumber !== prevSeason;
}

function shouldMergeWebContinuousRun(prevNode, currNode, hadBridgeBetween, rootIsDonghua = false) {
  if (hadBridgeBetween) return false;
  if (hasExplicitWebSeasonBreakLabel(prevNode, currNode)) return false;
  if (!sharesSameWebBaseTitle(prevNode, currNode, rootIsDonghua)) return false;
  const gapDays = computeWebBroadcastGapDays(prevNode, currNode);
  if (gapDays !== null) {
    return gapDays >= -14 && gapDays <= WEB_CONTINUOUS_MAX_GAP_DAYS;
  }
  return false;
}

function compareSeasonNodesChronologically(a, b, aId, bId) {
  const aYear = a?.startDate?.year ?? a?.seasonYear ?? null;
  const bYear = b?.startDate?.year ?? b?.seasonYear ?? null;
  const aUndated = aYear == null && String(a?.status || '').toUpperCase() === 'NOT_YET_RELEASED' ? 1 : 0;
  const bUndated = bYear == null && String(b?.status || '').toUpperCase() === 'NOT_YET_RELEASED' ? 1 : 0;
  if (aUndated !== bUndated) return aUndated - bUndated;
  const yDiff = (aYear ?? 9999) - (bYear ?? 9999);
  if (yDiff !== 0) return yDiff;
  const mDiff = (a?.startDate?.month ?? 13) - (b?.startDate?.month ?? 13);
  if (mDiff !== 0) return mDiff;
  const dDiff = (a?.startDate?.day ?? 32) - (b?.startDate?.day ?? 32);
  if (dDiff !== 0) return dDiff;
  return aId - bId;
}

async function fetchAniListSeasonBatch(ids) {
  const uniqueIds = [...new Set(ids.map(Number).filter((id) => id > 0))];
  if (!uniqueIds.length) return [];
  const cached = [];
  const missing = [];
  for (const id of uniqueIds) {
    const hit = anilistSeasonNodeCache.get(id);
    if (hit) cached.push(hit);
    else missing.push(id);
  }
  if (!missing.length) return cached;

  let fetched = [];
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const resp = await fetch('https://graphql.anilist.co', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: ANILIST_SEASON_BATCH_QUERY, variables: { ids: missing } })
      });
      if (resp.status === 429 && attempt < 2) {
        const retryAfter = Number(resp.headers.get('Retry-After') || 0);
        await new Promise((r) => setTimeout(r, retryAfter >= 1 && retryAfter <= 4 ? retryAfter * 1000 : 900 * (attempt + 1)));
        continue;
      }
      if (!resp.ok) break;
      const json = await resp.json();
      const mediaList = json?.data?.Page?.media || [];
      for (const node of mediaList) {
        if (node?.id) {
          anilistSeasonNodeCache.set(Number(node.id), node);
          fetched.push(node);
        }
      }
      break;
    } catch (_) {
      if (attempt < 2) await new Promise((r) => setTimeout(r, 500 * (attempt + 1)));
    }
  }
  return [...cached, ...fetched];
}

// ============================================================================
// COMMUNITY — RANDOM ANIME OF THE DAY & TOP ANIME CATALOGUE
// ============================================================================

const DAILY_RANDOM_CACHE_KEY = 'au_daily_random_anime_v3_ist_entrypoint';
const LEGACY_DAILY_RANDOM_CACHE_KEY = 'au_daily_random_anime_v1';
const LEGACY_DAILY_RANDOM_CACHE_KEY_V2 = 'au_daily_random_anime_v2_ist';
const YOUTUBE_VIDEO_ID_RE = /^[A-Za-z0-9_-]{11}$/;
const IST_OFFSET_MS = (5 * 60 + 30) * 60 * 1000; // UTC+05:30 (Asia/Kolkata, no DST)
const topAnimeCatalogCache = new Map();

const EXCLUDED_ANILIST_GENRES_WEB = [
  'Hentai',
  "Boys' Love",
  "Girls' Love",
  'Yaoi',
  'Yuri',
  'Shounen Ai',
  'Shoujo Ai'
];

const EXCLUDED_ANILIST_TAGS_WEB = [
  "Boys' Love",
  "Girls' Love",
  'Yuri',
  'Yaoi',
  'Shounen Ai',
  'Shoujo Ai'
];

const EXCLUDED_BL_GL_EXACT_TOKENS_WEB = new Set([
  'bl',
  'gl',
  'yaoi',
  'yuri',
  "boys' love",
  'boys love',
  "boy's love",
  "girls' love",
  'girls love',
  "girl's love",
  'shounen ai',
  'shonen ai',
  'shoujo ai',
  'shojo ai'
]);

const EXCLUDED_BL_GL_PHRASE_RE_WEB =
  /(^|[^a-z0-9])(boys'?\s*love|boy's\s*love|girls'?\s*love|girl's\s*love|yaoi|yuri|shou?nen\s*ai|shou?jo\s*ai)([^a-z0-9]|$)/i;

const SEQUEL_OR_MOVIE_TITLE_RE_WEB = new RegExp(
  [
    '\\bseason\\s*(?:0*[2-9]|\\d{2,}|ii|iii|iv|v|vi|vii|viii|ix|x)\\b',
    '\\b(?:2nd|3rd|[4-9]th|\\d{2,}th|second|third|fourth|fifth|sixth|final|last|climax)\\s+(?:season|series|cour|part|chapter|arc|act)\\b',
    '\\b(?:part|cour|act|chapter|phase|stage)\\s*(?:0*[2-9]|\\d{2,}|ii|iii|iv|v|vi)\\b',
    '\\bs(?:0*[2-9]|\\d{2,})\\b',
    '\\b(?:the\\s+movie|movie\\b|film\\b|ova\\b|oad\\b|recap\\b|special\\b|spin[\\s\\-]*off\\b|side[\\s\\-]*story\\b)',
    '\\b(?:shippu?den|next\\s+generations|thousand[\\s\\-]year\\s+blood\\s+war|entertainment\\s+district\\s+arc|swordsmith\\s+village\\s+arc|hashira\\s+training\\s+arc|mugen\\s+train\\s+arc|culling\\s+game|shibuya\\s+incident)\\b',
    '(?:\\s+|:|-)(?:ii|iii|iv|v|vi|r2)(?:\\s*$|\\s*[:\\-])'
  ].join('|'),
  'i'
);

function isSequelOrMovieTitleWeb(rawTitle) {
  if (!rawTitle) return false;
  const cleaned = String(rawTitle).trim();
  if (!cleaned) return false;
  return SEQUEL_OR_MOVIE_TITLE_RE_WEB.test(cleaned);
}

function fuzzyDateToSortableIntWeb(d) {
  if (!d || typeof d !== 'object') return 0;
  const y = Number(d.year || 0);
  if (!y || y <= 0) return 0;
  const m = Math.max(1, Math.min(12, Number(d.month || 1)));
  const day = Math.max(1, Math.min(31, Number(d.day || 1)));
  return y * 10000 + m * 100 + day;
}

function hasSequelOrDerivativeRelationWeb(media) {
  const edges = media?.relations?.edges;
  if (!Array.isArray(edges)) return false;
  const candidateDate = fuzzyDateToSortableIntWeb(media?.startDate);
  for (const edge of edges) {
    if (!edge || typeof edge !== 'object') continue;
    const rel = String(edge.relationType || '').trim().toUpperCase();
    const node = edge.node;
    const nodeType = String(node?.type || '').trim().toUpperCase();
    const nodeFormat = String(node?.format || '').trim().toUpperCase();
    const nodeDate = fuzzyDateToSortableIntWeb(node?.startDate);
    if (nodeType !== 'ANIME') continue;

    // Spin-offs, side stories, or sub-entries with a parent anime story
    if (rel === 'PARENT' || rel === 'FULL_STORY') {
      return true;
    }

    // Sequel seasons have a PREQUEL anime relation released before or alongside this season
    if (rel === 'PREQUEL') {
      if (nodeFormat === 'TV') {
        if (!candidateDate || !nodeDate || nodeDate <= candidateDate) {
          return true;
        }
      } else if (nodeFormat === 'TV_SHORT' || nodeFormat === 'ONA' || nodeFormat === 'MOVIE') {
        if (candidateDate > 0 && nodeDate > 0 && nodeDate <= candidateDate) {
          return true;
        }
      }
    }
  }
  return false;
}

function getIstDateKey(date = new Date()) {
  const d = date instanceof Date ? date : new Date(date);
  try {
    const parts = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Kolkata',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).formatToParts(d);
    const y = parts.find((p) => p.type === 'year')?.value;
    const m = parts.find((p) => p.type === 'month')?.value;
    const day = parts.find((p) => p.type === 'day')?.value;
    if (y && m && day) {
      return `${y}-${m}-${day}`;
    }
  } catch (_) {}
  const istShifted = new Date(d.getTime() + IST_OFFSET_MS);
  return istShifted.toISOString().slice(0, 10);
}

// Keep getUtcDateKey as an alias to getIstDateKey for any callers expecting the daily key
function getUtcDateKey(date = new Date()) {
  return getIstDateKey(date);
}

function getMillisUntilNextIstMidnight(nowMs = Date.now()) {
  const istElapsedToday = ((nowMs + IST_OFFSET_MS) % 86400000 + 86400000) % 86400000;
  return Math.max(1000, 86400000 - istElapsedToday);
}

function isValidYouTubeVideoId(id) {
  return typeof id === 'string' && YOUTUBE_VIDEO_ID_RE.test(id.trim());
}

function isValidYouTubeId(id) {
  return isValidYouTubeVideoId(id);
}

function isExcludedBlGlTokenWeb(raw) {
  if (raw === null || raw === undefined) return false;
  const normalized = String(raw)
    .trim()
    .toLowerCase()
    .replace(/[’‘`]/g, "'")
    .replace(/[_\-]+/g, ' ')
    .replace(/\s+/g, ' ');
  if (!normalized) return false;
  if (EXCLUDED_BL_GL_EXACT_TOKENS_WEB.has(normalized)) return true;
  return EXCLUDED_BL_GL_PHRASE_RE_WEB.test(normalized);
}

function extractTagNamesWeb(tagsInput) {
  if (!Array.isArray(tagsInput)) return [];
  const out = [];
  for (const item of tagsInput) {
    if (!item) continue;
    if (typeof item === 'string') {
      const s = item.trim();
      if (s) out.push(s);
    } else if (typeof item === 'object') {
      const name = String(item.name || '').trim();
      if (name) out.push(name);
      const category = String(item.category || '').trim();
      if (category && isExcludedBlGlTokenWeb(category)) {
        out.push(category);
      }
    }
  }
  return out;
}

function extractGenreNamesWeb(genresInput) {
  if (!Array.isArray(genresInput)) return [];
  return genresInput
    .map((g) => String(g || '').trim())
    .filter(Boolean);
}

function hasExcludedBlGlGenreOrTagWeb(genresInput, tagsInput) {
  const genres = extractGenreNamesWeb(genresInput);
  const tags = extractTagNamesWeb(tagsInput);
  // If genre or tag metadata is missing or empty, skip the anime rather than risk recommending an excluded category
  if (genres.length === 0 || tags.length === 0) return true;
  for (const g of genres) {
    if (isExcludedBlGlTokenWeb(g)) return true;
  }
  for (const t of tags) {
    if (isExcludedBlGlTokenWeb(t)) return true;
  }
  return false;
}

function isDailyAnimeRowEligibleWeb(row, expectedIstDate = null) {
  if (!row || typeof row !== 'object') return false;
  const meta = row.anime_metadata && typeof row.anime_metadata === 'object' ? row.anime_metadata : {};
  const anilistId = Number(row.anilist_id || meta.anilist_id || 0);
  if (!Number.isFinite(anilistId) || anilistId <= 0) return false;

  const ratingInt = Number(row.rating ?? meta.average_score ?? 0);
  if (!Number.isFinite(ratingInt) || ratingInt < 60 || ratingInt > 100) return false;

  const titleObj = meta.title && typeof meta.title === 'object' ? meta.title : {};
  const displayTitle = String(
    row.title || titleObj.english || titleObj.romaji || titleObj.native || ''
  ).trim();
  if (!displayTitle || displayTitle === 'Unknown') return false;

  // Exclude movies, OVAs, ONAs, specials, and Season 2/3+ sequels — only standalone / Season 1 TV entry-point anime
  const rawFormat = String(meta.format || '').trim().toUpperCase();
  if (rawFormat !== 'TV') return false;
  if (meta.is_entry_point === false) return false;
  if (hasSequelOrDerivativeRelationWeb(meta)) return false;
  if (
    isSequelOrMovieTitleWeb(displayTitle) ||
    isSequelOrMovieTitleWeb(titleObj.english) ||
    isSequelOrMovieTitleWeb(titleObj.romaji) ||
    isSequelOrMovieTitleWeb(titleObj.native)
  ) {
    return false;
  }

  const coverImage = String(row.cover_image || meta.cover_image || '').trim();
  if (!/^https?:\/\//i.test(coverImage)) return false;

  const trailerId = String(row.trailer_id || meta?.trailer?.id || '').trim();
  if (!isValidYouTubeVideoId(trailerId)) return false;

  const genres = extractGenreNamesWeb(meta.genres);
  const tags = extractTagNamesWeb(meta.tags);
  if (genres.length === 0 || tags.length === 0) return false;
  if (hasExcludedBlGlGenreOrTagWeb(genres, tags)) return false;

  if (expectedIstDate && String(row.selection_date || '') !== expectedIstDate) {
    return false;
  }
  return true;
}

function formatAiringStatusLabel(status) {
  switch (String(status || '').toUpperCase().trim()) {
    case 'RELEASING':
      return 'Currently airing';
    case 'FINISHED':
      return 'Finished airing';
    case 'NOT_YET_RELEASED':
      return 'Upcoming';
    case 'CANCELLED':
      return 'Cancelled';
    default:
      return 'Unknown';
  }
}

function formatAnimeFormatLabel(format) {
  switch (String(format || '').toUpperCase().trim()) {
    case 'TV':
      return 'TV';
    case 'TV_SHORT':
      return 'TV Short';
    case 'MOVIE':
      return 'Movie';
    case 'OVA':
      return 'OVA';
    case 'ONA':
      return 'ONA';
    case 'SPECIAL':
      return 'Special';
    case 'MUSIC':
      return 'Music';
    default:
      return 'Unknown';
  }
}

function formatAnimeSourceLabel(source) {
  switch (String(source || '').toUpperCase().trim()) {
    case 'ORIGINAL':
      return 'Original';
    case 'MANGA':
      return 'Manga';
    case 'LIGHT_NOVEL':
      return 'Light Novel';
    case 'VISUAL_NOVEL':
      return 'Visual Novel';
    case 'VIDEO_GAME':
      return 'Video Game';
    case 'WEB_NOVEL':
      return 'Web Novel';
    case 'NOVEL':
      return 'Novel';
    case 'DOUJINSHI':
      return 'Doujinshi';
    case 'ANIME':
      return 'Anime';
    case 'COMIC':
      return 'Comic';
    case 'GAME':
      return 'Game';
    case 'OTHER':
      return 'Other';
    default:
      return 'Unknown';
  }
}

function formatAnimeSeasonLabel(season) {
  switch (String(season || '').toUpperCase().trim()) {
    case 'WINTER':
      return 'Winter';
    case 'SPRING':
      return 'Spring';
    case 'SUMMER':
      return 'Summer';
    case 'FALL':
      return 'Fall';
    default:
      return 'Unknown';
  }
}

function formatIsoDateReadable(rawDate) {
  if (!rawDate || typeof rawDate !== 'string') return 'Unknown';
  const trimmed = rawDate.trim();
  if (!trimmed) return 'Unknown';
  const parts = trimmed.split('-').map((p) => Number(p));
  if (!parts[0] || !Number.isFinite(parts[0])) return 'Unknown';
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  if (parts.length >= 3 && parts[1] >= 1 && parts[1] <= 12 && parts[2] >= 1 && parts[2] <= 31) {
    return `${months[parts[1] - 1]} ${parts[2]}, ${parts[0]}`;
  }
  if (parts.length >= 2 && parts[1] >= 1 && parts[1] <= 12) {
    return `${months[parts[1] - 1]} ${parts[0]}`;
  }
  return String(parts[0]);
}

function stripAnimeHtml(raw) {
  if (!raw) return '';
  return String(raw)
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/?[^>]+(>|$)/g, '')
    .replace(/&quot;/g, '"')
    .replace(/&#039;|&apos;/g, "'")
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

function normalizeDailyAnimeRow(row) {
  if (!row || typeof row !== 'object') return null;
  const meta = row.anime_metadata && typeof row.anime_metadata === 'object' ? row.anime_metadata : {};
  const ratingInt = Number(row.rating ?? meta.average_score ?? 0);
  if (!Number.isFinite(ratingInt) || ratingInt < 60 || ratingInt > 100) return null;
  const rawFormat = String(meta.format || '').trim().toUpperCase();
  if (rawFormat !== 'TV' || meta.is_entry_point === false || hasSequelOrDerivativeRelationWeb(meta)) {
    return null;
  }
  const trailerId = String(row.trailer_id || meta?.trailer?.id || '').trim();
  const studiosRaw = Array.isArray(meta.studios) ? meta.studios.filter(Boolean) : [];
  const genresRaw = extractGenreNamesWeb(meta.genres);
  const tagsRaw = extractTagNamesWeb(meta.tags);
  if (hasExcludedBlGlGenreOrTagWeb(genresRaw, tagsRaw)) return null;

  const titleObj = meta.title && typeof meta.title === 'object' ? meta.title : {};
  const displayTitle =
    String(row.title || titleObj.english || titleObj.romaji || titleObj.native || '').trim() || 'Unknown';
  const secondaryTitle =
    titleObj.romaji && titleObj.romaji !== displayTitle
      ? titleObj.romaji
      : titleObj.native && titleObj.native !== displayTitle
      ? titleObj.native
      : '';
  if (
    isSequelOrMovieTitleWeb(displayTitle) ||
    isSequelOrMovieTitleWeb(secondaryTitle) ||
    isSequelOrMovieTitleWeb(titleObj.english) ||
    isSequelOrMovieTitleWeb(titleObj.romaji) ||
    isSequelOrMovieTitleWeb(titleObj.native)
  ) {
    return null;
  }

  return {
    id: row.id || '',
    selectionDate: String(row.selection_date || ''),
    anilistId: Number(row.anilist_id || meta.anilist_id || 0),
    title: displayTitle,
    secondaryTitle: String(secondaryTitle || '').trim(),
    rating100: Number.isFinite(ratingInt) && ratingInt > 0 ? ratingInt : null,
    rating10: Number.isFinite(ratingInt) && ratingInt > 0 ? (ratingInt / 10).toFixed(1) : null,
    coverImage: String(row.cover_image || meta.cover_image || '').trim(),
    bannerImage: String(row.banner_image || meta.banner_image || '').trim(),
    trailerId: isValidYouTubeVideoId(trailerId) ? trailerId : '',
    episodes: typeof meta.episodes === 'number' && meta.episodes > 0 ? meta.episodes : null,
    startDate: formatIsoDateReadable(meta.start_date),
    endDate: formatIsoDateReadable(meta.end_date),
    statusLabel: formatAiringStatusLabel(meta.status),
    rawStatus: String(meta.status || '').toUpperCase(),
    formatLabel: formatAnimeFormatLabel(meta.format),
    rawFormat,
    isEntryPoint: true,
    seasonLabel: formatAnimeSeasonLabel(meta.season),
    releaseYear: meta.season_year ? String(meta.season_year) : 'Unknown',
    durationLabel: typeof meta.duration === 'number' && meta.duration > 0 ? `${meta.duration} min` : 'Unknown',
    sourceLabel: formatAnimeSourceLabel(meta.source),
    studiosLabel: studiosRaw.length > 0 ? studiosRaw.join(', ') : 'Unknown',
    countryLabel: meta.country_of_origin ? String(meta.country_of_origin) : 'Unknown',
    genres: genresRaw,
    tags: tagsRaw,
    description: stripAnimeHtml(meta.description || '')
  };
}

let lastCommunityTabRect = null;
let lastCommunitySlideDir = 0;
let lastTopSourceSlideFrom = null;
const COMMUNITY_TAB_ORDER = ['feed', 'tagged', 'random', 'top'];

function switchCommunityViewTab(rawNext) {
  const next = COMMUNITY_TAB_ORDER.includes(rawNext) ? rawNext : 'feed';
  const prev = COMMUNITY_TAB_ORDER.includes(state.communityViewTab) ? state.communityViewTab : 'feed';
  if (next === prev) {
    if (
      next === 'random' &&
      (!state.dailyRandom.data || state.dailyRandom.fetchedDate !== getIstDateKey()) &&
      !state.dailyRandom.loading
    ) {
      loadDailyRandomAnime(false);
    } else if (next === 'top' && state.topAnime.items.length === 0 && !state.topAnime.loading) {
      loadTopAnimeCatalog(false);
    }
    return;
  }
  const activeEl = document.querySelector('.community-view-switcher .community-view-tab.active');
  if (activeEl) {
    lastCommunityTabRect = {
      left: activeEl.offsetLeft,
      width: activeEl.offsetWidth
    };
  }
  const prevIdx = COMMUNITY_TAB_ORDER.indexOf(prev);
  const nextIdx = COMMUNITY_TAB_ORDER.indexOf(next);
  lastCommunitySlideDir = nextIdx > prevIdx ? 1 : -1;
  state.communityViewTab = next;
  renderApp();
  if (next === 'tagged' && state.session) {
    loadUserNotifications(true);
  } else if (next === 'random') {
    loadDailyRandomAnime(false);
  } else if (next === 'top') {
    loadTopAnimeCatalog(false);
  }
}

function switchTopAnimeSource(rawSrc) {
  const nextSrc = rawSrc === 'community' ? 'community' : 'all';
  if (state.topAnime.source === nextSrc) return;
  lastTopSourceSlideFrom = state.topAnime.source;
  state.topAnime.source = nextSrc;
  loadTopAnimeCatalog(false);
}

function createDailyUtcPrng(seedKey) {
  let stateVal = 0x811c9dc5 >>> 0;
  for (let i = 0; i < seedKey.length; i++) {
    stateVal = Math.imul(stateVal ^ seedKey.charCodeAt(i), 0x01000193) >>> 0;
  }
  return {
    nextUInt() {
      stateVal = (stateVal + 0x6d2b79f5) >>> 0;
      let t = Math.imul(stateVal ^ (stateVal >>> 15), (1 | stateVal) >>> 0) >>> 0;
      t = ((t + Math.imul(t ^ (t >>> 7), (61 | t) >>> 0)) ^ t) >>> 0;
      return (t ^ (t >>> 14)) >>> 0;
    },
    nextInt(bound) {
      if (bound <= 1) return 0;
      return this.nextUInt() % bound;
    }
  };
}

function formatFuzzyDateNode(obj) {
  if (!obj || !obj.year) return null;
  const y = String(obj.year).padStart(4, '0');
  if (obj.month && obj.day) {
    return `${y}-${String(obj.month).padStart(2, '0')}-${String(obj.day).padStart(2, '0')}`;
  }
  if (obj.month) {
    return `${y}-${String(obj.month).padStart(2, '0')}`;
  }
  return y;
}

async function isYouTubeTrailerEmbeddableWeb(videoId) {
  if (!isValidYouTubeVideoId(videoId)) return false;
  try {
    const resp = await fetch(
      `https://www.youtube.com/oembed?url=${encodeURIComponent(`https://www.youtube.com/watch?v=${videoId}`)}&format=json`,
      { method: 'GET' }
    );
    return resp.ok;
  } catch (_) {
    // Some browsers block CORS on youtube.com/oembed; fall back to high-res thumbnail probe
    try {
      const thumb = await fetch(`https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`, { method: 'HEAD' });
      return thumb.ok;
    } catch (__) {
      return true;
    }
  }
}

async function pickAutomaticDailyAnimeRowWeb(todayIst, excludedIds) {
  const prng = createDailyUtcPrng(`anime-universe-daily-ist-v3-entrypoint:${todayIst}`);
  const pagePool = Array.from({ length: 32 }, (_, i) => i + 1);
  for (let i = pagePool.length - 1; i >= 1; i--) {
    const j = prng.nextInt(i + 1);
    const tmp = pagePool[i];
    pagePool[i] = pagePool[j];
    pagePool[j] = tmp;
  }

  const query = `
query ($page: Int!, $perPage: Int!) {
  Page(page: $page, perPage: $perPage) {
    media(
      type: ANIME,
      format_in: [TV],
      isAdult: false,
      averageScore_greater: 59,
      status_in: [FINISHED, RELEASING],
      genre_not_in: ["Hentai", "Boys' Love", "Girls' Love", "Yaoi", "Yuri", "Shounen Ai", "Shoujo Ai"],
      tag_not_in: ["Boys' Love", "Girls' Love", "Yuri", "Yaoi", "Shounen Ai", "Shoujo Ai"],
      sort: [POPULARITY_DESC, ID]
    ) {
      id
      title { english romaji native }
      coverImage { extraLarge large color }
      bannerImage
      averageScore
      episodes
      startDate { year month day }
      endDate { year month day }
      status
      format
      season
      seasonYear
      genres
      tags { name category }
      description(asHtml: false)
      duration
      source
      countryOfOrigin
      studios(isMain: true) { nodes { name } }
      trailer { id site thumbnail }
      relations {
        edges {
          relationType(version: 2)
          node {
            id
            type
            format
            startDate { year month day }
          }
        }
      }
    }
  }
}
`;

  for (let attempt = 0; attempt < 5; attempt++) {
    const page = pagePool[attempt % pagePool.length];
    let mediaList = [];
    try {
      const resp = await fetch('https://graphql.anilist.co', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ query, variables: { page, perPage: 50 } })
      });
      if (!resp.ok) continue;
      const json = await resp.json();
      mediaList = Array.isArray(json?.data?.Page?.media) ? json.data.Page.media : [];
    } catch (_) {
      continue;
    }

    const candidates = mediaList.filter((m) => {
      const id = Number(m?.id || 0);
      const score = Number(m?.averageScore || 0);
      if (!id || excludedIds.has(id) || score < 60 || score > 100) return false;
      const englishTitle = String(m?.title?.english || '').trim();
      const romajiTitle = String(m?.title?.romaji || '').trim();
      const nativeTitle = String(m?.title?.native || '').trim();
      const title = englishTitle || romajiTitle || nativeTitle || '';
      const cover = m?.coverImage?.extraLarge || m?.coverImage?.large || '';
      if (!title || !cover || !/^https?:\/\//i.test(cover)) return false;

      // Only standalone / Season 1 TV entry-point anime (no movies, OVAs, specials, or Season 2/3+ sequels)
      if (String(m?.format || '').trim().toUpperCase() !== 'TV') return false;
      if (hasSequelOrDerivativeRelationWeb(m)) return false;
      if (
        isSequelOrMovieTitleWeb(title) ||
        isSequelOrMovieTitleWeb(englishTitle) ||
        isSequelOrMovieTitleWeb(romajiTitle) ||
        isSequelOrMovieTitleWeb(nativeTitle)
      ) {
        return false;
      }

      // Secondary client-side genre + tag validation (reject BL/GL/Yaoi/Yuri or missing metadata)
      if (hasExcludedBlGlGenreOrTagWeb(m?.genres, m?.tags)) return false;

      const site = String(m?.trailer?.site || '').toLowerCase();
      const tid = String(m?.trailer?.id || '').trim();
      return Boolean(site === 'youtube' && isValidYouTubeVideoId(tid));
    });

    for (let i = candidates.length - 1; i >= 1; i--) {
      const j = prng.nextInt(i + 1);
      const tmp = candidates[i];
      candidates[i] = candidates[j];
      candidates[j] = tmp;
    }

    let checked = 0;
    for (const m of candidates) {
      if (checked >= 8) break;
      checked++;
      const trailerId = String(m.trailer.id).trim();
      const ok = await isYouTubeTrailerEmbeddableWeb(trailerId);
      if (!ok) continue;

      const id = Number(m.id);
      const score = Number(m.averageScore);
      const english = m?.title?.english || null;
      const romaji = m?.title?.romaji || null;
      const nativeTitle = m?.title?.native || null;
      const displayTitle = english || romaji || nativeTitle || 'Unknown';
      const coverImage = m?.coverImage?.extraLarge || m?.coverImage?.large || '';
      const bannerImage = m?.bannerImage || null;
      const studios = Array.isArray(m?.studios?.nodes)
        ? m.studios.nodes.map((s) => String(s?.name || '').trim()).filter(Boolean)
        : [];
      const genres = extractGenreNamesWeb(m?.genres);
      const tags = extractTagNamesWeb(m?.tags);
      if (hasExcludedBlGlGenreOrTagWeb(genres, tags)) continue;

      const startDateStr = formatFuzzyDateNode(m?.startDate);
      const endDateStr = formatFuzzyDateNode(m?.endDate);
      const seasonYear = m?.seasonYear || m?.startDate?.year || null;

      const candidateRow = {
        id: `auto-${todayIst}-${id}`,
        selection_date: todayIst,
        anilist_id: id,
        title: displayTitle,
        rating: score,
        cover_image: coverImage,
        banner_image: bannerImage,
        trailer_id: trailerId,
        anime_metadata: {
          anilist_id: id,
          title: { english, romaji, native: nativeTitle },
          cover_image: coverImage,
          banner_image: bannerImage,
          average_score: score,
          episodes: m?.episodes ?? null,
          start_date: startDateStr,
          end_date: endDateStr,
          status: m?.status || null,
          format: m?.format || 'TV',
          is_entry_point: true,
          season: m?.season || null,
          season_year: seasonYear,
          genres,
          tags,
          description: m?.description || '',
          duration: m?.duration ?? null,
          source: m?.source || null,
          country_of_origin: m?.countryOfOrigin || null,
          studios,
          trailer: { id: trailerId, site: 'youtube' }
        }
      };

      if (isDailyAnimeRowEligibleWeb(candidateRow, todayIst)) {
        return candidateRow;
      }
    }
  }
  return null;
}

let dailyRandomInFlightPromise = null;
let dailyRandomIstMidnightTimerId = null;

function scheduleNextIstMidnightResetWeb() {
  if (typeof window === 'undefined') return;
  if (dailyRandomIstMidnightTimerId) {
    clearTimeout(dailyRandomIstMidnightTimerId);
    dailyRandomIstMidnightTimerId = null;
  }
  const waitMs = getMillisUntilNextIstMidnight() + 350;
  dailyRandomIstMidnightTimerId = setTimeout(() => {
    const newIstDate = getIstDateKey();
    const dr = state.dailyRandom;
    if (!dr.data || dr.fetchedDate !== newIstDate || dr.data.selectionDate !== newIstDate) {
      dr.data = null;
      dr.fetchedDate = '';
      loadDailyRandomAnime(false);
    }
    scheduleNextIstMidnightResetWeb();
  }, waitMs);
}

function checkDailyRandomIstRolloverOnResumeWeb() {
  const todayIst = getIstDateKey();
  const dr = state.dailyRandom;
  if (dr.data && (dr.fetchedDate !== todayIst || dr.data.selectionDate !== todayIst)) {
    dr.data = null;
    dr.fetchedDate = '';
    loadDailyRandomAnime(false);
  } else if (!dr.data && !dr.loading && state.tab === 'community' && state.communityViewTab === 'random') {
    loadDailyRandomAnime(false);
  }
}

let lastDailyRandomSilentSyncMsWeb = 0;
let dailyRandomAutoRetryTimerId = null;

async function reconcileDailyRandomInBackgroundWeb(todayIst) {
  const now = Date.now();
  if (dailyRandomInFlightPromise || now - lastDailyRandomSilentSyncMsWeb < 5000) {
    return;
  }
  lastDailyRandomSilentSyncMsWeb = now;
  try {
    const baseUrl = getVaultBaseUrl();
    const apiKey = getVaultPublicKey();
    const headers = { apikey: apiKey, Accept: 'application/json' };
    if (state.session?.accessToken) {
      headers.Authorization = `Bearer ${state.session.accessToken}`;
    }
    const resp = await fetch(
      `${baseUrl}/rest/v1/daily_anime?select=*&selection_date=eq.${encodeURIComponent(todayIst)}&limit=1`,
      { method: 'GET', headers }
    );
    if (!resp.ok) return;
    const rows = await resp.json().catch(() => []);
    const dbRow = Array.isArray(rows) && rows.length > 0 ? rows[0] : null;

    if (dbRow && isDailyAnimeRowEligibleWeb(dbRow, todayIst)) {
      const normalized = normalizeDailyAnimeRow(dbRow);
      if (normalized) {
        const dr = state.dailyRandom;
        const changed =
          !dr.data ||
          dr.data.anilistId !== normalized.anilistId ||
          dr.data.selectionDate !== normalized.selectionDate;
        dr.data = normalized;
        dr.fetchedDate = todayIst;
        dr.error = '';
        try {
          localStorage.setItem(
            DAILY_RANDOM_CACHE_KEY,
            JSON.stringify({ istDate: todayIst, cachedDate: todayIst, row: dbRow })
          );
        } catch (_) {}
        if (changed && state.tab === 'community' && state.communityViewTab === 'random') {
          renderApp();
        }
      }
    } else if (Array.isArray(rows) && rows.length === 0 && state.session?.accessToken) {
      // Today's row was cleared in Supabase via SQL; clear local cache and seamlessly re-initialize
      try {
        localStorage.removeItem(DAILY_RANDOM_CACHE_KEY);
      } catch (_) {}
      state.dailyRandom.data = null;
      state.dailyRandom.fetchedDate = '';
      loadDailyRandomAnime(true);
    }
  } catch (_) {}
}

async function loadDailyRandomAnime(forceRefresh = false) {
  scheduleNextIstMidnightResetWeb();
  const todayIst = getIstDateKey();
  const dr = state.dailyRandom;

  // If the stored recommendation in memory belongs to a previous IST date or fails validation, clear it
  if (
    dr.data &&
    (dr.fetchedDate !== todayIst ||
      dr.data.selectionDate !== todayIst ||
      !dr.data.rating100 ||
      dr.data.rating100 < 60 ||
      dr.data.rawFormat !== 'TV' ||
      dr.data.isEntryPoint === false ||
      isSequelOrMovieTitleWeb(dr.data.title) ||
      isSequelOrMovieTitleWeb(dr.data.secondaryTitle) ||
      hasExcludedBlGlGenreOrTagWeb(dr.data.genres, dr.data.tags))
  ) {
    dr.data = null;
    dr.fetchedDate = '';
  }

  // Keep valid daily recommendation on the same IST date and silently verify with Supabase in the background
  if (
    dr.data &&
    dr.fetchedDate === todayIst &&
    dr.data.selectionDate === todayIst &&
    !dr.error
  ) {
    reconcileDailyRandomInBackgroundWeb(todayIst);
    return dr.data;
  }

  // Restore persisted IST daily recommendation from localStorage if valid for today's IST date
  if (!forceRefresh || !dr.data) {
    try {
      localStorage.removeItem(LEGACY_DAILY_RANDOM_CACHE_KEY);
      localStorage.removeItem(LEGACY_DAILY_RANDOM_CACHE_KEY_V2);
      const cachedRaw = localStorage.getItem(DAILY_RANDOM_CACHE_KEY);
      if (cachedRaw) {
        const cached = JSON.parse(cachedRaw);
        const cachedIstDate = String(cached?.istDate || cached?.cachedDate || '');
        if (
          cached &&
          cachedIstDate === todayIst &&
          isDailyAnimeRowEligibleWeb(cached.row, todayIst)
        ) {
          const normalized = normalizeDailyAnimeRow(cached.row);
          if (normalized) {
            dr.data = normalized;
            dr.fetchedDate = todayIst;
            dr.error = '';
            dr.loading = false;
            if (state.tab === 'community' && state.communityViewTab === 'random') {
              renderApp();
            }
            reconcileDailyRandomInBackgroundWeb(todayIst);
            return normalized;
          }
        }
      }
    } catch (_) {}
  }

  // Prevent multiple simultaneous requests from generating conflicting recommendations
  if (dailyRandomInFlightPromise) {
    return dailyRandomInFlightPromise;
  }

  dr.loading = true;
  dr.error = '';
  dr.trailerPlaying = false;
  if (state.tab === 'community' && state.communityViewTab === 'random') {
    renderApp();
  }

  dailyRandomInFlightPromise = (async () => {
    try {
      const baseUrl = getVaultBaseUrl();
      const apiKey = getVaultPublicKey();
      const headers = {
        apikey: apiKey,
        Accept: 'application/json'
      };
      if (state.session?.accessToken) {
        headers.Authorization = `Bearer ${state.session.accessToken}`;
      }

      let row = null;
      const excludedIds = new Set();

      // Stage 1: Read canonical row for today's IST date from Supabase public.daily_anime
      try {
        const resp = await fetch(
          `${baseUrl}/rest/v1/daily_anime?select=*&selection_date=eq.${encodeURIComponent(todayIst)}&limit=1`,
          {
            method: 'GET',
            headers
          }
        );
        if (resp.ok) {
          const rows = await resp.json().catch(() => []);
          if (Array.isArray(rows) && rows.length > 0 && isDailyAnimeRowEligibleWeb(rows[0], todayIst)) {
            row = rows[0];
          }
        }
      } catch (_) {}

      // Stage 2: If today's IST pick isn't stored yet, try Edge Function if deployed
      if (!isDailyAnimeRowEligibleWeb(row, todayIst) && state.session?.accessToken) {
        try {
          const fnResp = await fetch(`${baseUrl}/functions/v1/daily-random-anime`, {
            method: 'POST',
            headers: {
              apikey: apiKey,
              Authorization: `Bearer ${state.session.accessToken}`,
              'Content-Type': 'application/json',
              Accept: 'application/json'
            },
            body: JSON.stringify({ source: 'client_ensure_today_ist', selection_date: todayIst })
          });
          if (fnResp.ok) {
            const fnJson = await fnResp.json().catch(() => null);
            const candidate = fnJson?.data || fnJson?.anime || null;
            if (isDailyAnimeRowEligibleWeb(candidate, todayIst)) {
              row = candidate;
            }
          }
        } catch (_) {}
      }

      // Stage 3: Complete automatic daily selector (deterministic IST-date seed + AniList BL/GL filter + YouTube check)
      if (!isDailyAnimeRowEligibleWeb(row, todayIst)) {
        try {
          const histResp = await fetch(
            `${baseUrl}/rest/v1/daily_anime?select=anilist_id,selection_date&limit=1000`,
            {
              method: 'GET',
              headers
            }
          );
          if (histResp.ok) {
            const histRows = await histResp.json().catch(() => []);
            if (Array.isArray(histRows)) {
              histRows.forEach((r) => {
                const n = Number(r?.anilist_id || 0);
                const sDate = String(r?.selection_date || '');
                if (n > 0 && sDate !== todayIst) {
                  excludedIds.add(n);
                }
              });
            }
          }
        } catch (_) {}

        if (excludedIds.size === 0) {
          try {
            const rpcIdsResp = await fetch(`${baseUrl}/rest/v1/rpc/get_featured_anime_ids`, {
              method: 'POST',
              headers: { ...headers, 'Content-Type': 'application/json' },
              body: '{}'
            });
            if (rpcIdsResp.ok) {
              const idsArr = await rpcIdsResp.json().catch(() => []);
              if (Array.isArray(idsArr)) {
                idsArr.forEach((id) => {
                  const n = Number(id);
                  if (n > 0) excludedIds.add(n);
                });
              }
            }
          } catch (_) {}
        }

        const autoRow = await pickAutomaticDailyAnimeRowWeb(todayIst, excludedIds);
        if (autoRow && isDailyAnimeRowEligibleWeb(autoRow, todayIst)) {
          row = autoRow;
          // Stage 4: Atomically persist into Supabase via claim_daily_anime_selection RPC (if available & authenticated)
          if (state.session?.accessToken) {
            try {
              const claimResp = await fetch(`${baseUrl}/rest/v1/rpc/claim_daily_anime_selection`, {
                method: 'POST',
                headers: {
                  apikey: apiKey,
                  Authorization: `Bearer ${state.session.accessToken}`,
                  'Content-Type': 'application/json',
                  Accept: 'application/json'
                },
                body: JSON.stringify({
                  p_anilist_id: autoRow.anilist_id,
                  p_title: autoRow.title,
                  p_rating: autoRow.rating,
                  p_cover_image: autoRow.cover_image,
                  p_banner_image: autoRow.banner_image,
                  p_trailer_id: autoRow.trailer_id,
                  p_anime_metadata: autoRow.anime_metadata
                })
              });
              if (claimResp.ok) {
                const claimJson = await claimResp.json().catch(() => null);
                const claimed = Array.isArray(claimJson) ? claimJson[0] : claimJson;
                if (isDailyAnimeRowEligibleWeb(claimed, todayIst)) {
                  row = claimed;
                }
              }
            } catch (_) {}
          }
        }
      }

      const activeIstDate = getIstDateKey();
      const normalized = isDailyAnimeRowEligibleWeb(row, activeIstDate)
        ? normalizeDailyAnimeRow(row)
        : null;

      if (!normalized) {
        dr.data = null;
        dr.fetchedDate = '';
        dr.error = 'Unable to load today’s featured anime right now. Please try again.';
        return null;
      } else {
        dr.data = normalized;
        dr.fetchedDate = activeIstDate;
        dr.error = '';
        try {
          localStorage.setItem(
            DAILY_RANDOM_CACHE_KEY,
            JSON.stringify({ istDate: activeIstDate, cachedDate: activeIstDate, row })
          );
        } catch (_) {}
        return normalized;
      }
    } catch (err) {
      dr.error = err?.message || 'Could not load today’s anime pick. Please try again.';
      return null;
    } finally {
      dr.loading = false;
      dailyRandomInFlightPromise = null;
      if (state.tab === 'community' && state.communityViewTab === 'random') {
        renderApp();
      }
    }
  })();

  return dailyRandomInFlightPromise;
}

function getCurrentAnimeSeasonAndYear() {
  const now = new Date();
  const month = now.getUTCMonth() + 1;
  const year = now.getUTCFullYear();
  let season = 'WINTER';
  if (month >= 4 && month <= 6) season = 'SPRING';
  else if (month >= 7 && month <= 9) season = 'SUMMER';
  else if (month >= 10) season = 'FALL';
  return { season, year };
}

function getFuzzyDateMonthsAgo(monthsAgo) {
  const d = new Date();
  d.setUTCMonth(d.getUTCMonth() - monthsAgo);
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, '0');
  const day = String(d.getUTCDate()).padStart(2, '0');
  return Number(`${y}${m}${day}`);
}

function computeCommunityTopAnime(timeframe, limit) {
  const nowMs = Date.now();
  const cutoffDays =
    timeframe === 'weekly'
      ? 7
      : timeframe === 'monthly'
      ? 30
      : timeframe === 'seasonal'
      ? 90
      : timeframe === 'yearly'
      ? 365
      : null;
  const cutoffMs = cutoffDays ? nowMs - cutoffDays * 86400000 : 0;

  const byAnime = new Map();
  for (const rev of state.communityReviews || []) {
    const rating10 = Number(rev.rating ?? rev.personal_rating ?? 0);
    if (!Number.isFinite(rating10) || rating10 <= 0) continue;
    if (cutoffMs > 0) {
      const ts = Date.parse(rev.updated_at || rev.created_at || '');
      if (!Number.isFinite(ts) || ts < cutoffMs) continue;
    }
    const aid = Number(rev.anime_id || 0);
    if (!aid) continue;
    let bucket = byAnime.get(aid);
    if (!bucket) {
      bucket = {
        animeId: aid,
        title: rev.anime_title || `Anime #${aid}`,
        coverImage: rev.anime_cover_image || '',
        format: rev.anime_format || '',
        releaseYear: rev.anime_year || null,
        episodes: rev.anime_episodes || null,
        genres: Array.isArray(rev.anime_genres) ? rev.anime_genres : [],
        sumRating10: 0,
        voteCount: 0
      };
      byAnime.set(aid, bucket);
    }
    bucket.sumRating10 += rating10;
    bucket.voteCount += 1;
  }

  return Array.from(byAnime.values())
    .map((b) => ({
      animeId: b.animeId,
      title: b.title,
      coverImage: b.coverImage,
      scoreDisplay: (b.sumRating10 / Math.max(1, b.voteCount) / 2).toFixed(1),
      scoreScale: '5',
      voteCount: b.voteCount,
      format: b.format ? formatAnimeFormatLabel(b.format) : '',
      releaseYear: b.releaseYear || null,
      episodes: b.episodes || null,
      genres: b.genres.slice(0, 3),
      sortScore: b.sumRating10 / Math.max(1, b.voteCount)
    }))
    .sort((a, b) => {
      if (b.sortScore !== a.sortScore) return b.sortScore - a.sortScore;
      return b.voteCount - a.voteCount;
    })
    .slice(0, limit);
}

function buildTopAnimeGraphQLQuery({ hasSeason, hasSeasonYear, hasStartDateGreater }) {
  const varDefs = ['$page: Int!', '$perPage: Int!', '$sort: [MediaSort]'];
  const mediaArgs = ['type: ANIME', 'isAdult: false', 'sort: $sort', 'averageScore_greater: 1'];
  if (hasSeason) {
    varDefs.push('$season: MediaSeason');
    mediaArgs.push('season: $season');
  }
  if (hasSeasonYear) {
    varDefs.push('$seasonYear: Int');
    mediaArgs.push('seasonYear: $seasonYear');
  }
  if (hasStartDateGreater) {
    varDefs.push('$startDateGreater: FuzzyDateInt');
    mediaArgs.push('startDate_greater: $startDateGreater');
  }

  return `
query (${varDefs.join(', ')}) {
  Page(page: $page, perPage: $perPage) {
    media(${mediaArgs.join(', ')}) {
      id
      title {
        english
        romaji
        userPreferred
      }
      coverImage {
        large
        extraLarge
      }
      averageScore
      format
      episodes
      seasonYear
      startDate {
        year
      }
      genres
      countryOfOrigin
      synonyms
    }
  }
}
`;
}

async function fetchJikanTopAnimeFallback(page, perPage, timeframe) {
  const limit = Math.min(25, Math.max(10, Number(perPage) || 25));
  const url =
    timeframe === 'seasonal'
      ? `https://api.jikan.moe/v4/seasons/now?page=${page}&limit=${limit}&sfw=true`
      : timeframe === 'weekly' || timeframe === 'monthly'
      ? `https://api.jikan.moe/v4/top/anime?filter=airing&page=${page}&limit=${limit}&sfw=true`
      : `https://api.jikan.moe/v4/top/anime?page=${page}&limit=${limit}&sfw=true`;

  const resp = await fetch(url, { method: 'GET', headers: { Accept: 'application/json' } });
  if (!resp.ok) return [];
  const json = await resp.json().catch(() => null);
  const rows = Array.isArray(json?.data) ? json.data : [];
  return rows.map((item) => ({
    id: Number(item?.mal_id || 0),
    title: {
      english: item?.title_english || item?.title || '',
      romaji: item?.title || '',
      userPreferred: item?.title_english || item?.title || ''
    },
    coverImage: {
      extraLarge: item?.images?.webp?.large_image_url || item?.images?.jpg?.large_image_url || '',
      large: item?.images?.webp?.image_url || item?.images?.jpg?.image_url || ''
    },
    averageScore: item?.score ? Math.round(Number(item.score) * 10) : 0,
    format: String(item?.type || '').toUpperCase(),
    episodes: item?.episodes || null,
    seasonYear: item?.year || item?.aired?.prop?.from?.year || null,
    startDate: { year: item?.year || item?.aired?.prop?.from?.year || null },
    genres: Array.isArray(item?.genres) ? item.genres.map((g) => g?.name).filter(Boolean) : [],
    countryOfOrigin: 'JP',
    synonyms: Array.isArray(item?.title_synonyms) ? item.title_synonyms : []
  }));
}

async function fetchTopAnimeCatalogPage(page, perPage, timeframe) {
  const { season, year } = getCurrentAnimeSeasonAndYear();
  const variables = { page, perPage };

  switch (timeframe) {
    case 'weekly':
      variables.sort = ['TRENDING_DESC', 'SCORE_DESC'];
      break;
    case 'monthly':
      variables.sort = ['POPULARITY_DESC', 'SCORE_DESC'];
      variables.startDateGreater = getFuzzyDateMonthsAgo(6);
      break;
    case 'seasonal':
      variables.sort = ['SCORE_DESC', 'POPULARITY_DESC'];
      variables.season = season;
      variables.seasonYear = year;
      break;
    case 'yearly':
      variables.sort = ['SCORE_DESC', 'POPULARITY_DESC'];
      variables.seasonYear = year;
      break;
    case 'all_time':
    default:
      variables.sort = ['SCORE_DESC', 'POPULARITY_DESC'];
      break;
  }

  const query = buildTopAnimeGraphQLQuery({
    hasSeason: Boolean(variables.season),
    hasSeasonYear: Boolean(variables.seasonYear),
    hasStartDateGreater: Boolean(variables.startDateGreater)
  });

  try {
    for (let attempt = 0; attempt < 3; attempt++) {
      const resp = await fetch('https://graphql.anilist.co', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ query, variables })
      });
      if (resp.status === 429 && attempt < 2) {
        const retryAfter = Number(resp.headers.get('Retry-After') || 0);
        await new Promise((r) => setTimeout(r, retryAfter >= 1 && retryAfter <= 4 ? retryAfter * 1000 : 900 * (attempt + 1)));
        continue;
      }
      if (resp.ok) {
        const json = await resp.json();
        const media = Array.isArray(json?.data?.Page?.media) ? json.data.Page.media : [];
        if (media.length > 0) return media;
      }
      break;
    }
  } catch (_) {}

  // Reliable fallback if primary catalog API is rate-limited or unreachable
  const fallback = await fetchJikanTopAnimeFallback(page, perPage, timeframe);
  if (fallback.length > 0) return fallback;
  throw new Error('Unable to load top rankings right now.');
}

async function loadTopAnimeCatalog(forceRefresh = false) {
  const top = state.topAnime;
  const cacheKey = `${top.source}|${top.timeframe}|${top.limit}`;
  top.cacheKey = cacheKey;

  if (top.source === 'community') {
    top.loading = false;
    top.error = '';
    top.items = computeCommunityTopAnime(top.timeframe, top.limit);
    if (state.tab === 'community' && state.communityViewTab === 'top') {
      renderApp();
    }
    return;
  }

  if (!forceRefresh && topAnimeCatalogCache.has(cacheKey)) {
    const hit = topAnimeCatalogCache.get(cacheKey);
    if (Date.now() - hit.ts < 5 * 60 * 1000) {
      top.items = hit.items;
      top.loading = false;
      top.error = '';
      if (state.tab === 'community' && state.communityViewTab === 'top') {
        renderApp();
      }
      return;
    }
  }

  top.loading = true;
  top.error = '';
  if (state.tab === 'community' && state.communityViewTab === 'top') {
    renderApp();
  }

  try {
    const requestedLimit = Number(top.limit) || 10;
    let rawList = [];
    if (requestedLimit <= 50) {
      rawList = await fetchTopAnimeCatalogPage(1, requestedLimit, top.timeframe);
    } else {
      const page1 = await fetchTopAnimeCatalogPage(1, 50, top.timeframe);
      const page2 = await fetchTopAnimeCatalogPage(2, 50, top.timeframe);
      rawList = [...page1, ...page2];
    }

    if (state.topAnime.cacheKey !== cacheKey) return;

    const seen = new Set();
    const mapped = [];
    for (const node of rawList) {
      const id = Number(node?.id || 0);
      if (!id || seen.has(id)) continue;
      seen.add(id);
      const isDonghua = String(node.countryOfOrigin || '').toUpperCase() === 'CN';
      const title =
        resolveEnglishTitle(
          node?.title?.english || '',
          node?.title?.romaji || node?.title?.userPreferred || '',
          node?.synonyms || [],
          isDonghua
        ) ||
        node?.title?.english ||
        node?.title?.userPreferred ||
        node?.title?.romaji ||
        `Anime #${id}`;
      const avgScore = Number(node?.averageScore || 0);
      mapped.push({
        animeId: id,
        title,
        coverImage: node?.coverImage?.extraLarge || node?.coverImage?.large || '',
        scoreDisplay: avgScore > 0 ? (avgScore / 10).toFixed(1) : '—',
        scoreScale: '10',
        voteCount: null,
        format: node?.format ? formatAnimeFormatLabel(node.format) : '',
        releaseYear: node?.seasonYear || node?.startDate?.year || null,
        episodes: node?.episodes || null,
        genres: Array.isArray(node?.genres) ? node.genres.slice(0, 3) : []
      });
      if (mapped.length >= requestedLimit) break;
    }

    topAnimeCatalogCache.set(cacheKey, { ts: Date.now(), items: mapped });
    top.items = mapped;
    top.error = '';
  } catch (err) {
    if (state.topAnime.cacheKey === cacheKey) {
      top.error = 'Could not load rankings right now. Please try again.';
    }
  } finally {
    if (state.topAnime.cacheKey === cacheKey) {
      top.loading = false;
      if (state.tab === 'community' && state.communityViewTab === 'top') {
        renderApp();
      }
    }
  }
}

// ============================================================================
// COMMUNITY — MY SHUFFLE (Personal Random Anime Generator • Zero Storage • Zero YouTube API)
// ============================================================================

const myShuffleBatchPoolWeb = [];
let myShuffleInFlightPromiseWeb = null;
let lastRandomCategorySlideFrom = null;
const MY_SHUFFLE_SORT_MODES_WEB = [
  'POPULARITY_DESC',
  'SCORE_DESC',
  'FAVOURITES_DESC',
  'TRENDING_DESC'
];

function isReducedMotionPreferredWeb() {
  try {
    return Boolean(
      typeof window !== 'undefined' &&
        window.matchMedia &&
        window.matchMedia('(prefers-reduced-motion: reduce)').matches
    );
  } catch (_) {
    return false;
  }
}

function cryptoRandomIntWeb(min, max) {
  if (max <= min) return min;
  try {
    if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
      const buf = new Uint32Array(1);
      crypto.getRandomValues(buf);
      return min + (buf[0] % (max - min + 1));
    }
  } catch (_) {}
  return min + Math.floor(Math.random() * (max - min + 1));
}

function shuffleArrayInMemoryWeb(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = cryptoRandomIntWeb(0, i);
    const tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }
  return arr;
}

function parseAndValidateMyShuffleCandidateWeb(m) {
  if (!m || typeof m !== 'object') return null;
  const id = Number(m.id || 0);
  const score = Number(m.averageScore || 0);
  if (!Number.isFinite(id) || id <= 0 || !Number.isFinite(score) || score < 60 || score > 100) {
    return null;
  }

  const english = String(m?.title?.english || '').trim();
  const romaji = String(m?.title?.romaji || '').trim();
  const nativeTitle = String(m?.title?.native || '').trim();
  const displayTitle = english || romaji || nativeTitle;
  if (!displayTitle) return null;

  const secondaryTitle =
    romaji && romaji !== displayTitle
      ? romaji
      : nativeTitle && nativeTitle !== displayTitle
      ? nativeTitle
      : '';

  const coverImage = String(m?.coverImage?.extraLarge || m?.coverImage?.large || '').trim();
  if (!/^https?:\/\//i.test(coverImage)) return null;

  const genres = extractGenreNamesWeb(m.genres);
  const tags = extractTagNamesWeb(m.tags);
  // Strict BL/GL/Yaoi/Yuri exclusion & conservative missing genre/tag check (ordinary Romance passes)
  if (hasExcludedBlGlGenreOrTagWeb(genres, tags)) {
    return null;
  }

  const studios = Array.isArray(m?.studios?.nodes)
    ? m.studios.nodes.map((s) => String(s?.name || '').trim()).filter(Boolean)
    : [];
  const startDateIso = formatFuzzyDateNode(m.startDate);
  const endDateIso = formatFuzzyDateNode(m.endDate);
  const seasonYear = m.seasonYear || m?.startDate?.year || null;
  const startDateFormatted = formatIsoDateReadable(startDateIso);
  const releaseYearStr = seasonYear
    ? String(seasonYear)
    : startDateIso && startDateIso.length >= 4
    ? startDateIso.slice(0, 4)
    : 'Unknown';

  return {
    anilistId: id,
    title: displayTitle,
    secondaryTitle,
    rating100: score,
    rating10: (score / 10).toFixed(1),
    coverImage,
    bannerImage: String(m.bannerImage || '').trim(),
    episodes: typeof m.episodes === 'number' && m.episodes > 0 ? m.episodes : null,
    episodesLabel:
      typeof m.episodes === 'number' && m.episodes > 0 ? `${m.episodes} eps` : 'Unknown eps',
    startDate: startDateFormatted,
    endDate: formatIsoDateReadable(endDateIso),
    releaseYear: releaseYearStr,
    releaseDateOrYear: startDateFormatted !== 'Unknown' ? startDateFormatted : releaseYearStr,
    statusLabel: formatAiringStatusLabel(m.status),
    rawStatus: String(m.status || '').toUpperCase(),
    formatLabel: formatAnimeFormatLabel(m.format),
    rawFormat: String(m.format || '').toUpperCase(),
    seasonLabel: formatAnimeSeasonLabel(m.season),
    durationLabel: typeof m.duration === 'number' && m.duration > 0 ? `${m.duration} min` : 'Unknown',
    sourceLabel: formatAnimeSourceLabel(m.source),
    studiosLabel: studios.length > 0 ? studios.join(', ') : 'Unknown',
    countryLabel: m.countryOfOrigin ? String(m.countryOfOrigin) : 'Unknown',
    genres,
    description: stripAnimeHtml(m.description || '')
  };
}

async function fetchMyShuffleAniListBatchWeb(page, perPage, sortMode) {
  const query = `
query ($page: Int!, $perPage: Int!, $sort: [MediaSort]) {
  Page(page: $page, perPage: $perPage) {
    media(
      type: ANIME,
      isAdult: false,
      averageScore_greater: 59,
      status_in: [FINISHED, RELEASING],
      genre_not_in: ["Hentai", "Boys' Love", "Girls' Love", "Yaoi", "Yuri", "Shounen Ai", "Shoujo Ai"],
      tag_not_in: ["Boys' Love", "Girls' Love", "Yuri", "Yaoi", "Shounen Ai", "Shoujo Ai"],
      sort: $sort
    ) {
      id
      title { english romaji native }
      coverImage { extraLarge large }
      bannerImage
      averageScore
      episodes
      startDate { year month day }
      endDate { year month day }
      status
      format
      season
      seasonYear
      genres
      tags { name category }
      description(asHtml: false)
      duration
      source
      countryOfOrigin
      studios(isMain: true) { nodes { name } }
    }
  }
}
`;

  const resp = await fetch('https://graphql.anilist.co', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({
      query,
      variables: { page, perPage, sort: [sortMode, 'ID'] }
    })
  });
  if (!resp.ok) {
    throw new Error(`AniList request failed with HTTP ${resp.status}`);
  }
  const json = await resp.json();
  const rawList = Array.isArray(json?.data?.Page?.media) ? json.data.Page.media : [];
  const validItems = [];
  for (const m of rawList) {
    const parsed = parseAndValidateMyShuffleCandidateWeb(m);
    if (parsed) validItems.push(parsed);
  }
  return validItems;
}

function drawFromMyShufflePoolWeb(currentAnilistId) {
  if (myShuffleBatchPoolWeb.length === 0) return null;
  if (
    myShuffleBatchPoolWeb.length === 1 &&
    currentAnilistId &&
    myShuffleBatchPoolWeb[0].anilistId === currentAnilistId
  ) {
    myShuffleBatchPoolWeb.length = 0;
    return null;
  }
  const first = myShuffleBatchPoolWeb.shift();
  if (currentAnilistId && first.anilistId === currentAnilistId && myShuffleBatchPoolWeb.length > 0) {
    const second = myShuffleBatchPoolWeb.shift();
    myShuffleBatchPoolWeb.push(first);
    return second;
  }
  return first;
}

async function generateNextMyShuffleAnimeWeb() {
  const currentId = state.myShuffle.data?.anilistId || null;
  const buffered = drawFromMyShufflePoolWeb(currentId);
  if (buffered) return buffered;

  let lastErr = null;
  for (let attempt = 0; attempt < 4; attempt++) {
    const sortMode =
      MY_SHUFFLE_SORT_MODES_WEB[cryptoRandomIntWeb(0, MY_SHUFFLE_SORT_MODES_WEB.length - 1)];
    const maxPage = attempt < 2 ? 28 : 12;
    const randomPage = cryptoRandomIntWeb(1, maxPage);
    try {
      const batch = await fetchMyShuffleAniListBatchWeb(randomPage, 50, sortMode);
      if (batch.length > 0) {
        shuffleArrayInMemoryWeb(batch);
        myShuffleBatchPoolWeb.length = 0;
        myShuffleBatchPoolWeb.push(...batch);
        const picked = drawFromMyShufflePoolWeb(currentId);
        if (picked) return picked;
      }
    } catch (err) {
      lastErr = err;
    }
  }
  throw lastErr || new Error('Unable to shuffle anime from AniList right now. Please try again.');
}

function renderTactileDiceSvgWeb(face = 5, size = 22, className = 'my-shuffle-die-svg') {
  const safeFace = Math.max(1, Math.min(6, Number(face) || 5));
  const coordsByFace = {
    1: [[12, 12]],
    2: [
      [7.2, 7.2],
      [16.8, 16.8]
    ],
    3: [
      [7.2, 7.2],
      [12, 12],
      [16.8, 16.8]
    ],
    4: [
      [7.2, 7.2],
      [16.8, 7.2],
      [7.2, 16.8],
      [16.8, 16.8]
    ],
    5: [
      [7.2, 7.2],
      [16.8, 7.2],
      [12, 12],
      [7.2, 16.8],
      [16.8, 16.8]
    ],
    6: [
      [7.2, 6.8],
      [16.8, 6.8],
      [7.2, 12],
      [16.8, 12],
      [7.2, 17.2],
      [16.8, 17.2]
    ]
  };
  const pips = (coordsByFace[safeFace] || coordsByFace[5])
    .map(([cx, cy]) => `<circle cx="${cx}" cy="${cy}" r="1.85" fill="currentColor" />`)
    .join('');
  return `<svg class="${className}" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="2.25" y="2.25" width="19.5" height="19.5" rx="5.5" fill="currentColor" fill-opacity="0.16" stroke="currentColor" stroke-width="1.9" />
    ${pips}
  </svg>`;
}

let myShuffleDustMotesPoolWeb = null;

function getMyShuffleDustMotesPoolWeb() {
  if (myShuffleDustMotesPoolWeb) return myShuffleDustMotesPoolWeb;
  const motes = [];
  for (let i = 0; i < 145; i++) {
    const nx = Math.random();
    const ny = Math.random();
    // Diagonal disintegration wave from bottom-left to top-right
    const delayWave = Math.min(0.44, Math.max(0, (nx * 0.65 + (1 - ny) * 0.35) * 0.44));
    motes.push({
      normX: nx,
      normY: ny,
      driftDx: 32 + Math.random() * 84,
      driftDy: -(24 + Math.random() * 76),
      radius: 1.5 + Math.random() * 3.8,
      delayWave,
      swirlFreq: 2.2 + Math.random() * 4.2,
      swirlAmp: 6 + Math.random() * 18,
      colorType: i % 4,
      peakAlpha: 0.58 + Math.random() * 0.42
    });
  }
  myShuffleDustMotesPoolWeb = motes;
  return motes;
}

function runMyShuffleDustCanvasAnimationWeb(mode = 'dissolve', durationMs = 330) {
  if (typeof window === 'undefined' || typeof document === 'undefined') return Promise.resolve();
  const canvas = document.getElementById('my-shuffle-dust-canvas');
  const stage = document.getElementById('my-shuffle-card-stage');
  if (!canvas || !stage) {
    return new Promise((r) => setTimeout(r, durationMs));
  }

  const rect = stage.getBoundingClientRect();
  const pad = 20;
  const cssW = Math.max(240, rect.width + pad * 2);
  const cssH = Math.max(260, rect.height + pad * 2);
  const dpr = Math.min(2, window.devicePixelRatio || 1);
  canvas.width = Math.round(cssW * dpr);
  canvas.height = Math.round(cssH * dpr);

  const ctx = canvas.getContext('2d');
  if (!ctx) {
    return new Promise((r) => setTimeout(r, durationMs));
  }

  const styles = window.getComputedStyle(document.documentElement);
  const accentColor = styles.getPropertyValue('--accent-primary').trim() || '#D97757';
  const highlightColor = styles.getPropertyValue('--highlight').trim() || '#F4B084';
  const primaryText = styles.getPropertyValue('--text-primary').trim() || '#F5F2EB';
  const secondaryText = styles.getPropertyValue('--text-secondary').trim() || '#A6ADBF';
  const palette = [highlightColor, accentColor, primaryText, secondaryText];
  const motes = getMyShuffleDustMotesPoolWeb();
  const startTime = performance.now();

  return new Promise((resolve) => {
    function step(now) {
      const elapsed = now - startTime;
      const prog = Math.min(1, Math.max(0, elapsed / durationMs));

      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.clearRect(0, 0, cssW, cssH);

      const cardW = cssW - pad * 2;
      const cardH = cssH - pad * 2;

      for (let i = 0; i < motes.length; i++) {
        const m = motes[i];
        const denom = Math.max(0.25, 1 - m.delayWave);
        const localT = Math.min(1, Math.max(0, (prog - m.delayWave) / denom));
        if (localT <= 0 || localT >= 1) continue;

        // mode === 'dissolve': particles break off card and billow outward into dust
        // mode === 'materialize': particles swirl inward from dust cloud to form new card
        const dispT = mode === 'dissolve' ? localT : 1 - localT;
        const angle = dispT * m.swirlFreq * Math.PI;
        const swirlX = Math.sin(angle) * m.swirlAmp;
        const swirlY = Math.cos(angle) * (m.swirlAmp * 0.45);

        const px = pad + m.normX * cardW + m.driftDx * dispT + swirlX;
        const py = pad + m.normY * cardH + m.driftDy * dispT + swirlY;
        const env = Math.sin(localT * Math.PI);
        const alpha = Math.min(1, Math.max(0, env * m.peakAlpha));
        const r = m.radius * (0.65 + 0.55 * env);
        const col = palette[m.colorType];

        ctx.save();
        ctx.fillStyle = col;
        ctx.globalAlpha = alpha * 0.28;
        ctx.beginPath();
        ctx.arc(px, py, r * 2.2, 0, Math.PI * 2);
        ctx.fill();

        ctx.globalAlpha = alpha;
        ctx.beginPath();
        ctx.arc(px, py, r, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
      }

      if (prog < 1) {
        requestAnimationFrame(step);
      } else {
        ctx.clearRect(0, 0, cssW, cssH);
        resolve();
      }
    }
    requestAnimationFrame(step);
  });
}

async function triggerMyShuffleAgainWeb() {
  const ms = state.myShuffle;
  if (ms.loading || ms.animating || myShuffleInFlightPromiseWeb) return;

  const reducedMotion = isReducedMotionPreferredWeb();
  ms.diceFace = ms.diceFace || 5;

  // Phase 1: Old Card Template Disintegrates Into Swirling Dust (~320ms)
  let diceFaceTimer = null;
  if (ms.data && !reducedMotion) {
    ms.animating = true;
    ms.animPhase = 'dust-dissolve';
    const rollSequence = [3, 6, 1, 4, 2, 5];
    let stepIdx = 0;
    ms.diceFace = rollSequence[0];
    if (state.tab === 'community' && state.communityViewTab === 'random') {
      renderApp();
    }
    diceFaceTimer = setInterval(() => {
      stepIdx = (stepIdx + 1) % rollSequence.length;
      ms.diceFace = rollSequence[stepIdx];
      const liveBtnDie = document.getElementById('my-shuffle-live-btn-die');
      if (liveBtnDie) {
        liveBtnDie.innerHTML = renderTactileDiceSvgWeb(ms.diceFace, 22, 'my-shuffle-die-svg');
      }
    }, 48);
    await runMyShuffleDustCanvasAnimationWeb('dissolve', 320);
    if (diceFaceTimer) {
      clearInterval(diceFaceTimer);
      diceFaceTimer = null;
    }
  }

  ms.diceFace = cryptoRandomIntWeb(1, 6);
  ms.loading = true;
  ms.error = '';
  ms.detailsExpanded = false;
  if (!ms.data && state.tab === 'community' && state.communityViewTab === 'random') {
    renderApp();
  }

  myShuffleInFlightPromiseWeb = (async () => {
    try {
      const nextItem = await generateNextMyShuffleAnimeWeb();
      ms.data = nextItem;
      ms.sequence = (ms.sequence || 0) + 1;
      ms.error = '';
      ms.loading = false;

      // Phase 2: New Card Template Materializes Out of the Swirling Dust (~380ms)
      if (!reducedMotion) {
        ms.animating = true;
        ms.animPhase = 'dust-materialize';
        if (state.tab === 'community' && state.communityViewTab === 'random') {
          renderApp();
        }
        await runMyShuffleDustCanvasAnimationWeb('materialize', 380);
      }
    } catch (err) {
      ms.error = err?.message || 'Unable to shuffle anime right now. Please try again.';
    } finally {
      if (diceFaceTimer) clearInterval(diceFaceTimer);
      ms.loading = false;
      ms.animating = false;
      ms.animPhase = '';
      myShuffleInFlightPromiseWeb = null;
      if (state.tab === 'community' && state.communityViewTab === 'random') {
        renderApp();
      }
    }
  })();

  return myShuffleInFlightPromiseWeb;
}

function switchRandomSectionCategoryWeb(rawCat) {
  const nextCat = rawCat === 'my_shuffle' ? 'my_shuffle' : 'daily_pick';
  if (state.randomSectionCategory === nextCat) return;
  lastRandomCategorySlideFrom = state.randomSectionCategory;
  state.randomSectionCategory = nextCat;
  renderApp();
  if (nextCat === 'my_shuffle' && !state.myShuffle.data && !state.myShuffle.loading) {
    triggerMyShuffleAgainWeb();
  } else if (nextCat === 'daily_pick' && !state.dailyRandom.data && !state.dailyRandom.loading) {
    loadDailyRandomAnime(false);
  }
}

function renderRandomSectionCategoryBarWeb() {
  const cat = state.randomSectionCategory === 'my_shuffle' ? 'my_shuffle' : 'daily_pick';
  const isDaily = cat === 'daily_pick';
  return `
    <div class="random-category-bar-card" aria-label="Random Section Categories">
      <div class="random-category-bar-top">
        <div class="random-category-meta">
          <span class="random-category-kicker">${
            isDaily ? '★ DAILY GLOBAL FEATURE' : '⚄ PERSONAL DISCOVERY DECK'
          }</span>
          <span class="random-category-sub">${
            isDaily
              ? 'Global recommendation • Resets 12:00 AM IST'
              : 'Personal anime discovery • Roll the dice anytime'
          }</span>
        </div>
      </div>
      <div class="random-category-toggle" id="random-category-toggle" role="tablist" aria-label="Random Discovery Mode" data-active-cat="${cat}">
        <span class="random-category-slider ${isDaily ? 'is-daily' : 'is-shuffle'}" id="random-category-slider" aria-hidden="true"></span>
        <button type="button" role="tab" aria-selected="${isDaily ? 'true' : 'false'}" class="random-category-btn ${isDaily ? 'active' : ''}" data-random-cat="daily_pick">
          <span>★</span>
          <span>Daily Pick</span>
        </button>
        <button type="button" role="tab" aria-selected="${!isDaily ? 'true' : 'false'}" class="random-category-btn ${!isDaily ? 'active' : ''}" data-random-cat="my_shuffle">
          <span>⚄</span>
          <span>My Shuffle</span>
        </button>
      </div>
    </div>
  `;
}

function renderMyShuffleSectionWeb() {
  const ms = state.myShuffle;
  const item = ms.data;
  const isBusy = Boolean(ms.loading || ms.animating);
  const currentDiceFace = ms.diceFace || 5;

  if (!item && !ms.loading && !ms.error && typeof window !== 'undefined') {
    setTimeout(() => {
      if (
        state.randomSectionCategory === 'my_shuffle' &&
        !state.myShuffle.data &&
        !state.myShuffle.loading
      ) {
        triggerMyShuffleAgainWeb();
      }
    }, 30);
  }

  if (!item && ms.loading) {
    return `
      <div class="daily-random-card my-shuffle-card my-shuffle-transition-state" aria-live="polite">
        <div class="my-shuffle-shuffling-pill">
          <span class="my-shuffle-inline-rolling-die" aria-hidden="true">${renderTactileDiceSvgWeb(currentDiceFace, 18)}</span>
          <span>ROLLING FOR YOUR NEXT ANIME…</span>
        </div>
        <div class="daily-random-main">
          <div class="skeleton-poster"></div>
          <div style="flex:1;display:flex;flex-direction:column;gap:10px;">
            <div class="skeleton-line" style="width:78%;height:24px;"></div>
            <div class="skeleton-line" style="width:45%;height:16px;"></div>
            <div class="skeleton-line" style="width:100%;height:14px;"></div>
            <div class="skeleton-line" style="width:92%;height:14px;"></div>
            <div class="skeleton-line" style="width:66%;height:14px;"></div>
          </div>
        </div>
      </div>
    `;
  }

  if (!item && ms.error) {
    return `
      <div class="daily-random-card my-shuffle-card" style="text-align:center;padding:32px 22px;">
        <div style="display:inline-flex;padding:12px;border-radius:18px;background:var(--accent-soft);color:var(--highlight);margin-bottom:12px;">
          ${renderTactileDiceSvgWeb(currentDiceFace, 36)}
        </div>
        <div class="serif-heading" style="font-size:24px;margin-bottom:8px;">Shuffle Deck Unavailable</div>
        <p style="color:var(--text-secondary);font-size:13.5px;margin-bottom:16px;">${escapeHtml(ms.error)}</p>
        <button type="button" class="my-shuffle-primary-btn" id="btn-my-shuffle-again" style="max-width:260px;margin:0 auto;">
          <span class="my-shuffle-dice-icon-wrap">${renderTactileDiceSvgWeb(currentDiceFace, 20)}</span>
          <span>SHUFFLE ANIME</span>
        </button>
      </div>
    `;
  }

  if (!item) {
    return '';
  }

  const shortDesc =
    item.description.length > 280 && !ms.detailsExpanded
      ? `${item.description.slice(0, 280).trim()}…`
      : item.description || 'No description available.';

  const animClass = ms.animPhase ? `is-${ms.animPhase}` : '';
  const bannerUrl = item.bannerImage || item.coverImage || '';
  const firstStudio =
    item.studiosLabel && item.studiosLabel !== 'Unknown'
      ? item.studiosLabel.split(',')[0].trim()
      : '';

  return `
    <div class="my-shuffle-deck-wrapper">
      <div class="my-shuffle-deck-backplate" aria-hidden="true"></div>
      <section
        class="daily-random-card my-shuffle-card ${animClass}"
        id="my-shuffle-card-stage"
        aria-label="My Shuffle Personal Random Anime"
      >
        ${
          bannerUrl
            ? `<div class="daily-random-banner my-shuffle-hero-banner" style="background-image:url('${escapeHtml(bannerUrl)}');"></div>`
            : ''
        }
        <div class="daily-random-inner my-shuffle-card-inner">
          <div class="my-shuffle-top-row">
            <div class="my-shuffle-badge-cluster">
              <span class="daily-pick-badge">✦ MY SHUFFLE</span>
              <span class="daily-meta-pill status-${escapeHtml(item.rawStatus.toLowerCase() || 'unknown')}">${escapeHtml(item.statusLabel)}</span>
            </div>
            ${
              item.anilistId
                ? `<button type="button" class="capsule-pill active my-shuffle-open-btn" data-comm-open-anime="${item.anilistId}">
                    <span>Open Page</span>
                    <span aria-hidden="true">↗</span>
                  </button>`
                : ''
            }
          </div>

          <div class="my-shuffle-showcase-row my-shuffle-stagger-body">
            <div class="my-shuffle-poster-frame" ${item.anilistId ? `data-comm-open-anime="${item.anilistId}" style="cursor:pointer;"` : ''}>
              ${
                item.coverImage
                  ? `<img class="daily-random-poster my-shuffle-poster" src="${escapeHtml(item.coverImage)}" alt="${escapeHtml(item.title)}" loading="lazy" draggable="false" />`
                  : `<div class="daily-random-poster daily-random-poster-fallback">✦</div>`
              }
              <span class="my-shuffle-poster-format-tag">${escapeHtml(item.formatLabel)}</span>
            </div>

            <div class="daily-random-info my-shuffle-info-animate">
              <h3 class="daily-random-title my-shuffle-anime-title">${escapeHtml(item.title)}</h3>
              ${
                item.secondaryTitle
                  ? `<div class="daily-random-subtitle">${escapeHtml(item.secondaryTitle)}</div>`
                  : ''
              }

              <div class="my-shuffle-highlight-pills">
                <span class="daily-rating-pill">★ ${escapeHtml(`${item.rating10} / 10`)}</span>
                ${
                  firstStudio
                    ? `<span class="daily-meta-pill">${escapeHtml(firstStudio)}</span>`
                    : ''
                }
              </div>

              ${
                item.genres.length > 0
                  ? `<div class="daily-genre-row">
                      ${item.genres.slice(0, 5).map((g) => `<span class="daily-genre-chip">${escapeHtml(g)}</span>`).join('')}
                    </div>`
                  : ''
              }
            </div>
          </div>

          <div class="my-shuffle-specs-strip my-shuffle-info-animate">
            <div class="my-shuffle-spec-cell">
              <span class="my-shuffle-spec-label">SCORE</span>
              <span class="my-shuffle-spec-val is-accent">★ ${escapeHtml(item.rating10)}</span>
            </div>
            <div class="my-shuffle-spec-divider" aria-hidden="true"></div>
            <div class="my-shuffle-spec-cell">
              <span class="my-shuffle-spec-label">FORMAT</span>
              <span class="my-shuffle-spec-val">${escapeHtml(item.formatLabel)}</span>
            </div>
            <div class="my-shuffle-spec-divider" aria-hidden="true"></div>
            <div class="my-shuffle-spec-cell">
              <span class="my-shuffle-spec-label">EPISODES</span>
              <span class="my-shuffle-spec-val">${escapeHtml(item.episodes != null ? String(item.episodes) : 'TBA')}</span>
            </div>
            <div class="my-shuffle-spec-divider" aria-hidden="true"></div>
            <div class="my-shuffle-spec-cell">
              <span class="my-shuffle-spec-label">YEAR</span>
              <span class="my-shuffle-spec-val">${escapeHtml(item.releaseYear)}</span>
            </div>
          </div>

          <div class="my-shuffle-synopsis-box my-shuffle-info-animate">
            <div class="my-shuffle-synopsis-label">SYNOPSIS</div>
            <p class="daily-random-description my-shuffle-synopsis-text">${escapeHtml(shortDesc)}</p>
          </div>

          <div class="my-shuffle-subactions-row">
            <button type="button" class="daily-details-toggle" id="btn-my-shuffle-toggle-details" aria-expanded="${ms.detailsExpanded ? 'true' : 'false'}">
              <span>${ms.detailsExpanded ? 'Hide Details' : 'View Details'}</span>
              <span class="daily-chevron ${ms.detailsExpanded ? 'open' : ''}">▾</span>
            </button>
            <span class="daily-date-chip">Released: ${escapeHtml(item.releaseDateOrYear)}</span>
          </div>

          ${
            ms.detailsExpanded
              ? `
                <div class="daily-details-grid">
                  <div class="daily-detail-cell">
                    <span class="daily-detail-label">Total Episodes</span>
                    <span class="daily-detail-value">${escapeHtml(item.episodes != null ? String(item.episodes) : 'Unknown')}</span>
                  </div>
                  <div class="daily-detail-cell">
                    <span class="daily-detail-label">Format</span>
                    <span class="daily-detail-value">${escapeHtml(item.formatLabel)}</span>
                  </div>
                  <div class="daily-detail-cell">
                    <span class="daily-detail-label">Airing Status</span>
                    <span class="daily-detail-value">${escapeHtml(item.statusLabel)}</span>
                  </div>
                  <div class="daily-detail-cell">
                    <span class="daily-detail-label">Release Date</span>
                    <span class="daily-detail-value">${escapeHtml(item.releaseDateOrYear)}</span>
                  </div>
                  <div class="daily-detail-cell">
                    <span class="daily-detail-label">Season</span>
                    <span class="daily-detail-value">${escapeHtml(item.seasonLabel)}</span>
                  </div>
                  <div class="daily-detail-cell">
                    <span class="daily-detail-label">Release Year</span>
                    <span class="daily-detail-value">${escapeHtml(item.releaseYear)}</span>
                  </div>
                  <div class="daily-detail-cell">
                    <span class="daily-detail-label">Episode Duration</span>
                    <span class="daily-detail-value">${escapeHtml(item.durationLabel)}</span>
                  </div>
                  <div class="daily-detail-cell">
                    <span class="daily-detail-label">Source Material</span>
                    <span class="daily-detail-value">${escapeHtml(item.sourceLabel)}</span>
                  </div>
                  <div class="daily-detail-cell">
                    <span class="daily-detail-label">Animation Studio</span>
                    <span class="daily-detail-value">${escapeHtml(item.studiosLabel)}</span>
                  </div>
                  <div class="daily-detail-cell">
                    <span class="daily-detail-label">Country</span>
                    <span class="daily-detail-value">${escapeHtml(item.countryLabel)}</span>
                  </div>
                </div>
              `
              : ''
          }

          <div class="my-shuffle-controls-bar">
            <button
              type="button"
              class="my-shuffle-primary-btn ${isBusy ? 'is-loading' : ''}"
              id="btn-my-shuffle-again"
              ${isBusy ? 'disabled' : ''}
            >
              <span class="my-shuffle-dice-icon-wrap ${isBusy ? 'is-rolling' : ''}" id="my-shuffle-live-btn-die" aria-hidden="true">
                ${renderTactileDiceSvgWeb(currentDiceFace, 22, 'my-shuffle-die-svg')}
              </span>
              <span>${isBusy ? 'SHUFFLING…' : 'SHUFFLE ANIME'}</span>
            </button>
          </div>
        </div>
      </section>
      <canvas class="my-shuffle-dust-canvas" id="my-shuffle-dust-canvas" aria-hidden="true"></canvas>
    </div>
  `;
}

function renderRandomAnimeSection() {
  const categoryBar = renderRandomSectionCategoryBarWeb();
  if (state.randomSectionCategory === 'my_shuffle') {
    return `${categoryBar}${renderMyShuffleSectionWeb()}`;
  }

  const dr = state.dailyRandom;
  const pick = dr.data;

  if (!pick) {
    if (!dr.loading && typeof window !== 'undefined' && !dailyRandomAutoRetryTimerId) {
      const delayMs = dr.error ? 2500 : 80;
      dailyRandomAutoRetryTimerId = setTimeout(() => {
        dailyRandomAutoRetryTimerId = null;
        if (!state.dailyRandom.data && !state.dailyRandom.loading) {
          loadDailyRandomAnime(false);
        }
      }, delayMs);
    }
    return `
      ${categoryBar}
      <div class="daily-random-card daily-random-skeleton">
        <div class="daily-random-header">
          <div>
            <div class="skeleton-pill" style="width:110px;height:22px;margin-bottom:10px;"></div>
            <div class="skeleton-line" style="width:240px;height:28px;"></div>
          </div>
        </div>
        <div class="daily-random-main">
          <div class="skeleton-poster"></div>
          <div style="flex:1;display:flex;flex-direction:column;gap:10px;">
            <div class="skeleton-line" style="width:78%;height:24px;"></div>
            <div class="skeleton-line" style="width:45%;height:16px;"></div>
            <div class="skeleton-line" style="width:100%;height:14px;"></div>
            <div class="skeleton-line" style="width:92%;height:14px;"></div>
            <div class="skeleton-line" style="width:66%;height:14px;"></div>
          </div>
        </div>
        <div class="skeleton-trailer"></div>
      </div>
    `;
  }

  const shortDesc =
    pick.description.length > 260 && !dr.detailsExpanded
      ? `${pick.description.slice(0, 260).trim()}…`
      : pick.description || 'No description available.';
  const trailerId = pick.trailerId;
  const trailerThumb = trailerId ? `https://i.ytimg.com/vi/${trailerId}/hqdefault.jpg` : '';
  const youtubeWatchUrl = trailerId ? `https://www.youtube.com/watch?v=${trailerId}` : '';
  const safeOriginParam =
    typeof window !== 'undefined' && /^https?:\/\//i.test(window.location?.origin || '')
      ? `&origin=${encodeURIComponent(window.location.origin)}`
      : '';
  const youtubeEmbedUrl = trailerId
    ? `https://www.youtube.com/embed/${trailerId}?autoplay=1&playsinline=1&rel=0&modestbranding=1&enablejsapi=1${safeOriginParam}`
    : '';

  return `
    ${categoryBar}
    <section class="daily-random-card" aria-label="Random Anime of the Day">
      ${
        pick.bannerImage
          ? `<div class="daily-random-banner" style="background-image:url('${escapeHtml(pick.bannerImage)}');"></div>`
          : ''
      }
      <div class="daily-random-inner">
        <div class="daily-random-header">
          <div>
            <div class="daily-random-badge-row">
              <span class="daily-pick-badge">★ TODAY’S PICK</span>
              ${
                pick.selectionDate
                  ? `<span class="daily-date-chip">${escapeHtml(formatIsoDateReadable(pick.selectionDate))}</span>`
                  : ''
              }
            </div>
            <h2 class="serif-heading daily-random-heading">Random Anime of the Day</h2>
          </div>
          ${
            pick.anilistId
              ? `<button type="button" class="capsule-pill active" data-comm-open-anime="${pick.anilistId}">Open Page</button>`
              : ''
          }
        </div>

        <div class="daily-random-main">
          <div class="daily-random-poster-wrap" ${pick.anilistId ? `data-comm-open-anime="${pick.anilistId}" style="cursor:pointer;"` : ''}>
            ${
              pick.coverImage
                ? `<img class="daily-random-poster" src="${escapeHtml(pick.coverImage)}" alt="${escapeHtml(pick.title)}" loading="lazy" />`
                : `<div class="daily-random-poster daily-random-poster-fallback">★</div>`
            }
          </div>

          <div class="daily-random-info">
            <h3 class="daily-random-title">${escapeHtml(pick.title)}</h3>
            ${
              pick.secondaryTitle
                ? `<div class="daily-random-subtitle">${escapeHtml(pick.secondaryTitle)}</div>`
                : ''
            }

            <div class="daily-random-stats-row">
              <span class="daily-rating-pill">★ ${escapeHtml(pick.rating10 ? `${pick.rating10} / 10` : 'Unknown')}</span>
              <span class="daily-meta-pill">${escapeHtml(pick.formatLabel)}</span>
              <span class="daily-meta-pill status-${escapeHtml(pick.rawStatus.toLowerCase() || 'unknown')}">${escapeHtml(pick.statusLabel)}</span>
            </div>

            ${
              pick.genres.length > 0
                ? `<div class="daily-genre-row">
                    ${pick.genres.map((g) => `<span class="daily-genre-chip">${escapeHtml(g)}</span>`).join('')}
                  </div>`
                : ''
            }

            <p class="daily-random-description">${escapeHtml(shortDesc)}</p>

            <div class="daily-random-actions">
              <button type="button" class="daily-details-toggle" id="btn-random-toggle-details" aria-expanded="${dr.detailsExpanded ? 'true' : 'false'}">
                <span>${dr.detailsExpanded ? 'Hide Details' : 'View Details'}</span>
                <span class="daily-chevron ${dr.detailsExpanded ? 'open' : ''}">▾</span>
              </button>
            </div>
          </div>
        </div>

        ${
          dr.detailsExpanded
            ? `
              <div class="daily-details-grid">
                <div class="daily-detail-cell">
                  <span class="daily-detail-label">Total Episodes</span>
                  <span class="daily-detail-value">${escapeHtml(pick.episodes != null ? String(pick.episodes) : 'Unknown')}</span>
                </div>
                <div class="daily-detail-cell">
                  <span class="daily-detail-label">Airing Status</span>
                  <span class="daily-detail-value">${escapeHtml(pick.statusLabel)}</span>
                </div>
                <div class="daily-detail-cell">
                  <span class="daily-detail-label">Release Date</span>
                  <span class="daily-detail-value">${escapeHtml(pick.startDate)}</span>
                </div>
                <div class="daily-detail-cell">
                  <span class="daily-detail-label">Ending Date</span>
                  <span class="daily-detail-value">${escapeHtml(pick.endDate)}</span>
                </div>
                <div class="daily-detail-cell">
                  <span class="daily-detail-label">Season</span>
                  <span class="daily-detail-value">${escapeHtml(pick.seasonLabel)}</span>
                </div>
                <div class="daily-detail-cell">
                  <span class="daily-detail-label">Release Year</span>
                  <span class="daily-detail-value">${escapeHtml(pick.releaseYear)}</span>
                </div>
                <div class="daily-detail-cell">
                  <span class="daily-detail-label">Episode Duration</span>
                  <span class="daily-detail-value">${escapeHtml(pick.durationLabel)}</span>
                </div>
                <div class="daily-detail-cell">
                  <span class="daily-detail-label">Source Material</span>
                  <span class="daily-detail-value">${escapeHtml(pick.sourceLabel)}</span>
                </div>
                <div class="daily-detail-cell">
                  <span class="daily-detail-label">Animation Studio</span>
                  <span class="daily-detail-value">${escapeHtml(pick.studiosLabel)}</span>
                </div>
                <div class="daily-detail-cell">
                  <span class="daily-detail-label">Country</span>
                  <span class="daily-detail-value">${escapeHtml(pick.countryLabel)}</span>
                </div>
              </div>
            `
            : ''
        }

        <div class="daily-trailer-section">
          <div class="daily-trailer-header">
            <span class="daily-trailer-label">OFFICIAL TRAILER</span>
            ${
              youtubeWatchUrl
                ? `<a class="daily-yt-fallback-link" href="${escapeHtml(youtubeWatchUrl)}" target="_blank" rel="noopener noreferrer">Watch on YouTube ↗</a>`
                : ''
            }
          </div>

          ${
            !trailerId
              ? `<div class="daily-trailer-unavailable">Trailer preview is currently unavailable for this title.</div>`
              : dr.trailerPlaying
              ? `
                <div class="daily-trailer-player-wrap">
                  <iframe
                    class="daily-trailer-iframe"
                    src="${escapeHtml(youtubeEmbedUrl)}"
                    title="${escapeHtml(pick.title)} Official Trailer"
                    frameborder="0"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                    referrerpolicy="strict-origin-when-cross-origin"
                    allowfullscreen
                  ></iframe>
                </div>
                <div class="daily-trailer-hint">
                  If playback is restricted by the video owner, use <a href="${escapeHtml(youtubeWatchUrl)}" target="_blank" rel="noopener noreferrer">Watch on YouTube</a>.
                </div>
              `
              : `
                <button type="button" class="daily-trailer-poster-btn" id="btn-random-play-trailer" aria-label="Play trailer">
                  <img class="daily-trailer-thumb" src="${escapeHtml(trailerThumb)}" alt="${escapeHtml(pick.title)} trailer thumbnail" loading="lazy" />
                  <span class="daily-trailer-overlay">
                    <span class="daily-play-circle">▶</span>
                    <span class="daily-play-caption">Play Trailer</span>
                  </span>
                </button>
              `
          }
        </div>
      </div>
    </section>
  `;
}

function renderTopAnimeSection() {
  const top = state.topAnime;
  const limits = [10, 25, 50, 100];
  const timeframes = [
    { key: 'weekly', label: 'Weekly' },
    { key: 'monthly', label: 'Monthly' },
    { key: 'seasonal', label: 'Seasonal' },
    { key: 'yearly', label: 'Yearly' },
    { key: 'all_time', label: 'All Time' }
  ];

  const crownSvg = `
    <svg class="top-anime-crown-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M2 18h20" />
      <path d="M4 15L2.5 6l5.5 4 4-6 4 6 5.5-4L20 15H4z" />
    </svg>
  `;

  return `
    <section class="top-anime-section" aria-label="Top Anime">
      <div class="top-anime-header-card">
        <div class="top-anime-title-row">
          <div class="top-anime-heading-group">
            ${crownSvg}
            <h2 class="top-anime-heading">Top Anime</h2>
          </div>

          <div class="top-source-toggle" id="top-source-toggle" role="tablist" aria-label="Ranking Source" data-active-source="${top.source === 'community' ? 'community' : 'all'}">
            <span class="top-source-slider ${top.source === 'all' ? 'is-all' : 'is-community'}" id="top-source-slider" aria-hidden="true"></span>
            <button type="button" role="tab" aria-selected="${top.source === 'community' ? 'true' : 'false'}" class="top-source-btn ${top.source === 'community' ? 'active' : ''}" data-top-source="community">
              Community
            </button>
            <button type="button" role="tab" aria-selected="${top.source === 'all' ? 'true' : 'false'}" class="top-source-btn ${top.source === 'all' ? 'active' : ''}" data-top-source="all">
              All
            </button>
          </div>
        </div>

        <div class="top-pill-row">
          ${limits
            .map(
              (lim) => `
                <button type="button" class="top-filter-pill ${top.limit === lim ? 'active' : ''}" data-top-limit="${lim}">
                  Top ${lim}
                </button>
              `
            )
            .join('')}
        </div>

        <div class="top-filter-divider"></div>

        <div class="top-pill-row">
          ${timeframes
            .map(
              (tf) => `
                <button type="button" class="top-filter-pill ${top.timeframe === tf.key ? 'active' : ''}" data-top-timeframe="${tf.key}">
                  ${tf.label}
                </button>
              `
            )
            .join('')}
        </div>
      </div>

      ${
        top.loading
          ? `
            <div class="top-anime-list">
              ${Array.from({ length: 6 })
                .map(
                  () => `
                    <div class="top-anime-row-card top-skeleton-row">
                      <div class="skeleton-pill" style="width:36px;height:36px;border-radius:12px;"></div>
                      <div class="skeleton-pill" style="width:54px;height:76px;border-radius:10px;"></div>
                      <div style="flex:1;display:flex;flex-direction:column;gap:8px;">
                        <div class="skeleton-line" style="width:72%;height:16px;"></div>
                        <div class="skeleton-line" style="width:45%;height:12px;"></div>
                      </div>
                    </div>
                  `
                )
                .join('')}
            </div>
          `
          : top.error
          ? `
            <div class="app-card" style="text-align:center;padding:34px 20px;">
              <p style="color:var(--text-secondary);font-size:13.5px;margin-bottom:14px;">${escapeHtml(top.error)}</p>
              <button type="button" class="btn-primary" id="btn-top-retry">TRY AGAIN</button>
            </div>
          `
          : top.items.length === 0
          ? `
            <div class="app-card" style="text-align:center;padding:38px 22px;">
              <div class="serif-heading" style="font-size:22px;margin-bottom:6px;">No rankings yet</div>
              <p style="color:var(--text-secondary);font-size:13.5px;">
                ${
                  top.source === 'community'
                    ? 'No community ratings match this timeframe yet. Try switching to All Time or rate an anime!'
                    : 'No titles matched this ranking filter right now.'
                }
              </p>
            </div>
          `
          : `
            <div class="top-anime-list">
              ${top.items
                .map((item, idx) => {
                  const rank = idx + 1;
                  const rankClass = rank === 1 ? 'rank-1' : rank === 2 ? 'rank-2' : rank === 3 ? 'rank-3' : '';
                  const metaBits = [
                    item.format || null,
                    item.releaseYear ? String(item.releaseYear) : null,
                    item.episodes ? `${item.episodes} eps` : null
                  ].filter(Boolean);
                  return `
                    <button
                      type="button"
                      class="top-anime-row-card"
                      data-comm-open-anime="${item.animeId}"
                    >
                      <span class="top-rank-badge ${rankClass}">#${rank}</span>
                      ${
                        item.coverImage
                          ? `<img class="top-anime-poster" src="${escapeHtml(item.coverImage)}" alt="${escapeHtml(item.title)}" loading="lazy" />`
                          : `<div class="top-anime-poster top-anime-poster-fallback">★</div>`
                      }
                      <div class="top-anime-info">
                        <div class="top-anime-name">${escapeHtml(item.title)}</div>
                        ${
                          metaBits.length > 0
                            ? `<div class="top-anime-meta">${escapeHtml(metaBits.join(' • '))}</div>`
                            : ''
                        }
                        ${
                          item.genres && item.genres.length > 0
                            ? `<div class="top-anime-genres">${item.genres
                                .slice(0, 3)
                                .map((g) => `<span class="top-anime-genre-tag">${escapeHtml(g)}</span>`)
                                .join('')}</div>`
                            : ''
                        }
                      </div>
                      <div class="top-anime-score-col">
                        <span class="top-anime-score-pill">★ ${escapeHtml(item.scoreDisplay)}</span>
                        ${
                          item.voteCount != null
                            ? `<span class="top-anime-votes">${item.voteCount} ${item.voteCount === 1 ? 'rating' : 'ratings'}</span>`
                            : ''
                        }
                      </div>
                    </button>
                  `;
                })
                .join('')}
            </div>
          `
      }
    </section>
  `;
}

async function resolveFullSeasonChain(rootMedia, explicitOverrideMode = null) {
  if (!rootMedia?.id) {
    return { seasons: [], moviesAndOvas: [], relatedMedia: [], franchiseId: 0, rawMainSeasonNodeCount: 0 };
  }
  anilistSeasonNodeCache.set(Number(rootMedia.id), rootMedia);

  const rootId = Number(rootMedia.id);
  const rootIsDonghua = String(rootMedia.countryOfOrigin || '').toUpperCase() === 'CN';

  const resolveCleanNodeTitle = (node, fallback = '') => {
    const snIsDonghua = rootIsDonghua || String(node?.countryOfOrigin || '').toUpperCase() === 'CN';
    return (
      resolveEnglishTitle(
        node?.title?.english || '',
        node?.title?.romaji || node?.title?.userPreferred || '',
        node?.synonyms || [],
        snIsDonghua
      ) ||
      node?.title?.english ||
      node?.title?.userPreferred ||
      node?.title?.romaji ||
      fallback ||
      `Anime #${node?.id || ''}`
    );
  };

  const toRelatedEntry = (node, relLabel = '') => {
    const title = resolveCleanNodeTitle(node, `Anime #${node.id}`);
    return {
      id: Number(node.id),
      title,
      coverImage: node?.coverImage?.extraLarge || node?.coverImage?.large || node?.coverImage?.medium || '',
      format: String(node?.format || 'TV').toUpperCase(),
      relationType: relLabel || String(node?.format || 'RELATED').replace(/_/g, ' '),
      status: node?.status || null,
      episodes: node?.episodes ?? (String(node?.format || '').toUpperCase() === 'MOVIE' ? 1 : null),
      seasonYear: node?.seasonYear ?? node?.startDate?.year ?? null
    };
  };

  const hasImmediateLink = (rootMedia.relations?.edges || []).some((e) => {
    const rel = String(e?.relationType || '').toUpperCase().trim();
    return (
      e?.node &&
      (!e.node.type || String(e.node.type).toUpperCase() === 'ANIME') &&
      (WEB_MAINLINE_TRAVERSAL_RELS.has(rel) || WEB_SIDE_COLLECTION_RELS.has(rel))
    );
  });

  if (!hasImmediateLink) {
    const directRelated = (rootMedia.relations?.edges || [])
      .filter((e) => e?.node?.id && (!e.node.type || String(e.node.type).toUpperCase() === 'ANIME') && Number(e.node.id) !== rootId)
      .map((e) => toRelatedEntry(e.node, String(e.relationType || '').replace(/_/g, ' ')));
    const rootFmt = String(rootMedia?.format || '').toUpperCase().trim();
    const selfMovies = WEB_MOVIE_OVA_SPECIAL_FORMATS.has(rootFmt)
      ? [
          {
            id: rootId,
            title: resolveCleanNodeTitle(rootMedia),
            coverImage: rootMedia?.coverImage?.extraLarge || rootMedia?.coverImage?.large || '',
            formatBadge: rootFmt === 'MOVIE' || rootFmt === 'OVA' ? rootFmt : 'SPECIAL',
            status: rootMedia?.status || null,
            episodes: Number(rootMedia?.episodes) > 0 ? Number(rootMedia.episodes) : 1,
            releaseYear: rootMedia?.startDate?.year ?? rootMedia?.seasonYear ?? null,
            isCurrent: true
          }
        ]
      : [];
    return {
      seasons: [],
      moviesAndOvas: selfMovies,
      relatedMedia: directRelated.slice(0, 12),
      franchiseId: rootId,
      rawMainSeasonNodeCount: WEB_MOVIE_OVA_SPECIAL_FORMATS.has(rootFmt) ? 0 : 1
    };
  }

  const MAX_WAVES = 25;
  const MAX_NODES = 60;
  const BATCH_SIZE = 25;

  const nodesById = new Map();
  const outgoing = new Map();
  const mainlineEligibleIds = new Set([rootId]);
  const sideOnlyAttachedIds = new Set();
  const expandedIds = new Set();
  const queuedIds = new Set();
  const frontier = [];

  const mergeNode = (candidate) => {
    const id = Number(candidate.id);
    const existing = nodesById.get(id);
    if (!existing) {
      nodesById.set(id, candidate);
      return;
    }
    const existingHasRels = Array.isArray(existing.relations?.edges) && existing.relations.edges.length > 0;
    const candidateHasRels = Array.isArray(candidate.relations?.edges) && candidate.relations.edges.length > 0;
    if (candidateHasRels && !existingHasRels) {
      nodesById.set(id, {
        ...existing,
        ...candidate,
        startDate: candidate.startDate || existing.startDate,
        endDate: candidate.endDate || existing.endDate,
        seasonYear: candidate.seasonYear ?? existing.seasonYear,
        episodes: candidate.episodes ?? existing.episodes,
        duration: candidate.duration ?? existing.duration
      });
    } else {
      nodesById.set(id, {
        ...candidate,
        ...existing,
        startDate: existing.startDate || candidate.startDate,
        endDate: existing.endDate || candidate.endDate,
        seasonYear: existing.seasonYear ?? candidate.seasonYear,
        episodes: existing.episodes ?? candidate.episodes,
        duration: existing.duration ?? candidate.duration,
        relations: existingHasRels ? existing.relations : candidate.relations
      });
    }
  };

  const addDirectedEdge = (fromId, toId) => {
    if (!outgoing.has(fromId)) outgoing.set(fromId, new Set());
    outgoing.get(fromId).add(toId);
  };

  const enqueue = (id) => {
    if (!id || expandedIds.has(id) || queuedIds.has(id)) return;
    if (nodesById.size >= MAX_NODES && !nodesById.has(id)) return;
    queuedIds.add(id);
    frontier.push(id);
  };

  const ingestRelations = (media) => {
    const mediaId = Number(media.id);
    mergeNode(media);
    expandedIds.add(mediaId);
    const mediaFmt = String(media?.format || '').toUpperCase().trim();
    const isMainlineOrBridge = mainlineEligibleIds.has(mediaId) || WEB_MOVIE_OVA_SPECIAL_FORMATS.has(mediaFmt);

    for (const edge of media.relations?.edges || []) {
      const rel = String(edge?.relationType || '').toUpperCase().trim();
      const target = edge.node;
      if (!target?.id) continue;
      if (target.type && String(target.type).toUpperCase() !== 'ANIME') continue;
      const targetId = Number(target.id);
      if (!nodesById.has(targetId) && nodesById.size >= MAX_NODES) continue;
      const targetFmt = String(target?.format || '').toUpperCase().trim();

      if (WEB_MAINLINE_TRAVERSAL_RELS.has(rel)) {
        mergeNode(target);
        if (isMainlineOrBridge) {
          mainlineEligibleIds.add(targetId);
          sideOnlyAttachedIds.delete(targetId);
        }
        if (rel === 'SEQUEL') addDirectedEdge(mediaId, targetId);
        else if (rel === 'PREQUEL') addDirectedEdge(targetId, mediaId);
        enqueue(targetId);
      } else if (WEB_SIDE_COLLECTION_RELS.has(rel)) {
        mergeNode(target);
        if (!mainlineEligibleIds.has(targetId)) {
          sideOnlyAttachedIds.add(targetId);
        }
        // Walk through MOVIE, OVA, SPECIAL nodes even when linked via SIDE_STORY / SUMMARY
        if (WEB_MOVIE_OVA_SPECIAL_FORMATS.has(targetFmt)) {
          enqueue(targetId);
        }
      }
    }
  };

  ingestRelations(rootMedia);

  let wave = 0;
  while (frontier.length > 0 && wave < MAX_WAVES && expandedIds.size < MAX_NODES) {
    wave++;
    const batch = [];
    while (frontier.length > 0 && batch.length < BATCH_SIZE) {
      const nextId = frontier.shift();
      if (!expandedIds.has(nextId)) batch.push(nextId);
    }
    if (!batch.length) break;

    const fetched = await fetchAniListSeasonBatch(batch);
    const fetchedIds = new Set();
    for (const node of fetched) {
      fetchedIds.add(Number(node.id));
      ingestRelations(node);
    }
    for (const reqId of batch) {
      if (!fetchedIds.has(reqId)) expandedIds.add(reqId);
    }
  }

  const franchiseHasFullSeries = Array.from(nodesById.values()).some((n) => {
    const fmt = String(n?.format || '').toUpperCase().trim();
    const dur = Number(n?.duration || 0);
    const eps = Number(n?.episodes || 0);
    return (
      (fmt === 'TV' || fmt === 'ONA') &&
      (dur >= 12 || eps >= 10 || (String(n?.status || '').toUpperCase() === 'RELEASING' && (!dur || dur >= 10)))
    );
  });

  const seasonIds = new Set();
  for (const [id, node] of nodesById.entries()) {
    if (
      mainlineEligibleIds.has(id) &&
      !sideOnlyAttachedIds.has(id) &&
      isSeasonCandidateMedia(node, franchiseHasFullSeries)
    ) {
      seasonIds.add(id);
    }
  }

  // Merge all collected MOVIE, OVA, and SPECIAL entries from the franchise graph into one unified list sorted by release date
  const movieOvaSpecialNodes = Array.from(nodesById.values())
    .filter((node) => {
      if (node?.type && String(node.type).toUpperCase() !== 'ANIME') return false;
      if (String(node?.status || '').toUpperCase() === 'CANCELLED') return false;
      const fmt = String(node?.format || '').toUpperCase().trim();
      return WEB_MOVIE_OVA_SPECIAL_FORMATS.has(fmt);
    })
    .sort((a, b) => compareSeasonNodesChronologically(a, b, Number(a.id), Number(b.id)));

  const moviesAndOvas = movieOvaSpecialNodes.map((node) => {
    const fmt = String(node?.format || 'SPECIAL').toUpperCase().trim();
    const badge = fmt === 'MOVIE' || fmt === 'OVA' ? fmt : 'SPECIAL';
    const totalEps = Number(node?.episodes) > 0 ? Number(node.episodes) : 1;
    return {
      id: Number(node.id),
      title: resolveCleanNodeTitle(node),
      coverImage: node?.coverImage?.extraLarge || node?.coverImage?.large || node?.coverImage?.medium || '',
      formatBadge: badge,
      status: node?.status || null,
      episodes: totalEps,
      releaseYear: node?.startDate?.year ?? node?.seasonYear ?? null,
      isCurrent: Number(node.id) === rootId
    };
  });

  const movieOvaIds = new Set(moviesAndOvas.map((m) => m.id));

  const allFranchiseIds = [...seasonIds, ...movieOvaIds, rootId].filter((id) => id > 0);
  const franchiseId = allFranchiseIds.length ? Math.min(...allFranchiseIds) : rootId;
  const overrideMode = explicitOverrideMode || getFranchiseSeasonOverride(franchiseId);

  const relatedById = new Map();
  for (const [id, node] of nodesById.entries()) {
    if (id === rootId || String(node?.status || '').toUpperCase() === 'CANCELLED') continue;
    if (!seasonIds.has(id) && !movieOvaIds.has(id)) {
      relatedById.set(id, toRelatedEntry(node));
    }
  }
  for (const edge of rootMedia.relations?.edges || []) {
    const node = edge?.node;
    if (!node?.id || Number(node.id) === rootId) continue;
    if (node.type && String(node.type).toUpperCase() !== 'ANIME') continue;
    const nid = Number(node.id);
    if (!seasonIds.has(nid) && !movieOvaIds.has(nid) && !relatedById.has(nid)) {
      relatedById.set(nid, toRelatedEntry(node, String(edge.relationType || '').replace(/_/g, ' ')));
    }
  }
  const relatedMedia = Array.from(relatedById.values()).slice(0, 14);

  if (seasonIds.size === 0) {
    return {
      seasons: [],
      moviesAndOvas,
      relatedMedia,
      franchiseId,
      rawMainSeasonNodeCount: 0
    };
  }

  const seasonOutgoing = new Map();
  const directNonBridgeOutgoing = new Map();
  const inDegree = new Map();
  for (const sid of seasonIds) {
    seasonOutgoing.set(sid, new Set());
    directNonBridgeOutgoing.set(sid, new Set());
    inDegree.set(sid, 0);
  }

  for (const startId of seasonIds) {
    for (const directNext of outgoing.get(startId) || []) {
      if (seasonIds.has(directNext) && directNext !== startId) {
        directNonBridgeOutgoing.get(startId).add(directNext);
      }
    }
    const seen = new Set();
    const queue = [];
    for (const next of outgoing.get(startId) || []) {
      if (!seen.has(next)) {
        seen.add(next);
        queue.push(next);
      }
    }
    while (queue.length > 0) {
      const curr = queue.shift();
      if (curr === startId) continue;
      if (seasonIds.has(curr)) {
        const targets = seasonOutgoing.get(startId);
        if (!targets.has(curr)) {
          targets.add(curr);
          inDegree.set(curr, (inDegree.get(curr) || 0) + 1);
        }
      } else {
        for (const next of outgoing.get(curr) || []) {
          if (!seen.has(next)) {
            seen.add(next);
            queue.push(next);
          }
        }
      }
    }
  }

  const cmp = (aId, bId) => compareSeasonNodesChronologically(nodesById.get(aId), nodesById.get(bId), aId, bId);
  const ready = [...seasonIds].filter((sid) => (inDegree.get(sid) || 0) === 0).sort(cmp);
  const orderedIds = [];

  while (ready.length > 0) {
    const curr = ready.shift();
    orderedIds.push(curr);
    let added = false;
    for (const next of seasonOutgoing.get(curr) || []) {
      const rem = (inDegree.get(next) || 1) - 1;
      inDegree.set(next, rem);
      if (rem === 0) {
        ready.push(next);
        added = true;
      }
    }
    if (added) ready.sort(cmp);
  }

  if (orderedIds.length < seasonIds.size) {
    const placed = new Set(orderedIds);
    const remaining = [...seasonIds].filter((id) => !placed.has(id)).sort(cmp);
    orderedIds.push(...remaining);
  }

  const sortedMainNodes = orderedIds.map((id) => nodesById.get(id)).filter(Boolean);

  if (sortedMainNodes.length <= 1 && moviesAndOvas.length === 0) {
    return {
      seasons: [],
      moviesAndOvas: [],
      relatedMedia,
      franchiseId,
      rawMainSeasonNodeCount: sortedMainNodes.length
    };
  }

  let groupedMainNodes = [];
  if (overrideMode === 'merge_one') {
    groupedMainNodes = [sortedMainNodes];
  } else if (overrideMode === 'split_all') {
    groupedMainNodes = sortedMainNodes.map((n) => [n]);
  } else {
    for (const node of sortedMainNodes) {
      if (groupedMainNodes.length === 0) {
        groupedMainNodes.push([node]);
      } else {
        const currentGroup = groupedMainNodes[groupedMainNodes.length - 1];
        const prevNode = currentGroup[currentGroup.length - 1];
        const prevId = Number(prevNode.id);
        const currId = Number(node.id);
        const connectedViaBridge =
          seasonOutgoing.has(prevId) &&
          seasonOutgoing.get(prevId).has(currId) &&
          !(directNonBridgeOutgoing.get(prevId)?.has(currId));
        if (shouldMergeWebContinuousRun(prevNode, node, connectedViaBridge, rootIsDonghua)) {
          currentGroup.push(node);
        } else {
          groupedMainNodes.push([node]);
        }
      }
    }
  }

  const seasons = groupedMainNodes.map((groupNodes, idx) => {
    const seasonNum = idx + 1;
    const leadNode = groupNodes.find((n) => Number(n.id) === rootId) || groupNodes[0];
    const isCurrentSeason = groupNodes.some((n) => Number(n.id) === rootId);

    let runningOffset = 0;
    let knownEpisodesSum = 0;
    const subEntries = groupNodes.map((segNode) => {
      const segEps = Number(segNode?.episodes) > 0 ? Number(segNode.episodes) : null;
      const startEpisodeOffset = runningOffset;
      if (segEps !== null) {
        runningOffset += segEps;
        knownEpisodesSum += segEps;
      }
      return {
        id: Number(segNode.id),
        title: resolveCleanNodeTitle(segNode),
        episodes: segEps,
        startEpisodeOffset,
        status: segNode?.status || null,
        format: segNode?.format || null,
        seasonYear: segNode?.seasonYear ?? segNode?.startDate?.year ?? null
      };
    });

    const mergedEpisodes =
      groupNodes.length === 1
        ? (Number(leadNode?.episodes) > 0 ? Number(leadNode.episodes) : null)
        : (knownEpisodesSum > 0 ? knownEpisodesSum : null);

    const mergedStatus = groupNodes.some((n) => String(n?.status || '').toUpperCase() === 'RELEASING')
      ? 'RELEASING'
      : groupNodes.every((n) => String(n?.status || '').toUpperCase() === 'FINISHED')
      ? 'FINISHED'
      : leadNode?.status || null;

    const shortLabel = String(seasonNum);
    const displayLabel =
      groupNodes.length > 1
        ? `Season ${seasonNum} (${groupNodes.length} Parts Merged)`
        : `Season ${seasonNum}`;

    return {
      id: Number(leadNode.id),
      seasonNumber: seasonNum,
      partNumber: null,
      shortLabel,
      displayLabel,
      title: resolveCleanNodeTitle(groupNodes[0], displayLabel),
      seasonYear: groupNodes[0]?.seasonYear ?? groupNodes[0]?.startDate?.year ?? null,
      status: mergedStatus,
      format: leadNode?.format || null,
      episodes: mergedEpisodes,
      isCurrent: isCurrentSeason,
      subEntries,
      isMergedContinuousRun: groupNodes.length > 1
    };
  });

  return {
    seasons,
    moviesAndOvas,
    relatedMedia,
    franchiseId,
    rawMainSeasonNodeCount: sortedMainNodes.length
  };
}

function getActiveMergedSeasonForDetail() {
  const a = state.detailAnime;
  const seasons = state.detailRich?.seasons || [];
  if (!a || !seasons.length) return null;
  const active = seasons.find(
    (s) => s.isCurrent || s.id === Number(a.animeId) || (Array.isArray(s.subEntries) && s.subEntries.some((seg) => seg.id === Number(a.animeId)))
  );
  if (active && Array.isArray(active.subEntries) && active.subEntries.length > 1) {
    return active;
  }
  return null;
}

function computeWebContinuousWatched(seasonEntry, detailAnime) {
  if (!seasonEntry || !Array.isArray(seasonEntry.subEntries) || seasonEntry.subEntries.length <= 1) {
    return Number(detailAnime?.watchedEpisodes || 0);
  }
  let sumOfWatched = 0;
  let maxOffsetPlusWatched = 0;
  for (const seg of seasonEntry.subEntries) {
    const vaultEntry = state.vault.find((v) => Number(v.animeId) === Number(seg.id));
    const rawW =
      Number(seg.id) === Number(detailAnime?.animeId)
        ? Math.max(Number(detailAnime?.watchedEpisodes || 0), Number(vaultEntry?.watchedEpisodes || 0))
        : Number(vaultEntry?.watchedEpisodes || 0);
    if (rawW > 0) {
      const cap = seg.episodes && seg.episodes > 0 ? seg.episodes : rawW;
      sumOfWatched += Math.min(cap, Math.max(0, rawW));
      const implied = (seg.startEpisodeOffset || 0) + rawW;
      if (implied > maxOffsetPlusWatched) {
        maxOffsetPlusWatched = implied;
      }
    }
  }
  return Math.max(sumOfWatched, maxOffsetPlusWatched);
}

async function commitWebContinuousEpisodeProgress(seasonEntry, targetContinuousEp) {
  const a = state.detailAnime;
  if (!a) return { ok: false, error: 'Anime not loaded' };
  if (!seasonEntry || !Array.isArray(seasonEntry.subEntries) || seasonEntry.subEntries.length <= 1) {
    return commitEpisodeProgress(a.animeId, targetContinuousEp);
  }
  const totalMerged = seasonEntry.episodes || a.totalEpisodes || null;
  const check = validateEpisodeInput(String(targetContinuousEp), totalMerged);
  if (!check.valid) {
    return { ok: false, error: check.error };
  }
  const validatedContinuous = check.value;
  let activeSeg = seasonEntry.subEntries[0];
  for (const seg of seasonEntry.subEntries) {
    if (validatedContinuous > (seg.startEpisodeOffset || 0)) {
      activeSeg = seg;
    }
  }

  let lastRes = { ok: true };
  for (const seg of seasonEntry.subEntries) {
    const segMax = seg.episodes && seg.episodes > 0 ? seg.episodes : null;
    const rawLocal = Math.max(0, validatedContinuous - (seg.startEpisodeOffset || 0));
    const localEp = segMax !== null ? Math.min(segMax, rawLocal) : rawLocal;
    let existing = state.vault.find((v) => Number(v.animeId) === Number(seg.id));
    const shouldUpdate =
      Number(seg.id) === Number(a.animeId) ||
      Number(seg.id) === Number(activeSeg.id) ||
      Boolean(existing) ||
      localEp > 0;
    if (!shouldUpdate) continue;

    if (!existing) {
      const cachedNode = anilistSeasonNodeCache.get(Number(seg.id));
      const segEntry = {
        ...a,
        animeId: Number(seg.id),
        title: seg.title || a.title,
        titleEnglish: seg.title || a.titleEnglish,
        coverImage: cachedNode?.coverImage?.extraLarge || cachedNode?.coverImage?.large || a.coverImage,
        totalEpisodes: seg.episodes || null,
        mediaStatus: seg.status || a.mediaStatus,
        format: seg.format || a.format,
        seasonYear: seg.seasonYear || a.seasonYear,
        watchedEpisodes: localEp,
        status: segMax && localEp >= segMax && String(seg.status || '').toUpperCase() === 'FINISHED' ? 'completed' : 'watching',
        inWatchlist: true
      };
      await saveVaultEntry(segEntry, { addToHome: Number(seg.id) === Number(a.animeId) });
      existing = state.vault.find((v) => Number(v.animeId) === Number(seg.id));
    }
    if (existing && Number(existing.watchedEpisodes || 0) !== localEp) {
      const res = await commitEpisodeProgress(seg.id, localEp);
      if (Number(seg.id) === Number(a.animeId)) {
        lastRes = res;
      }
    }
  }
  return lastRes;
}

async function openAnimeDetail(animeId) {
  if (state.tab !== 'detail') {
    state.previousTab = state.tab;
  }
  state.tab = 'detail';
  clearDetailRatingAutoSave();
  detailUserTouchedRating = false;
  detailUserTouchedNote = false;
  const inVault = state.vault.find((v) => Number(v.animeId) === Number(animeId));
  const inDisc = state.discover.find((d) => Number(d.animeId) === Number(animeId));
  const myCommRev = state.session
    ? state.communityReviews.find((r) => r.user_id === state.session.userId && Number(r.anime_id) === Number(animeId))
    : null;
  const initialRating10 = inVault?.rating10 ?? myCommRev?.rating ?? null;
  const initialReviewNotes = inVault?.reviewNotes || myCommRev?.review_notes || '';
  state.detailAnime = inVault
    ? { ...inVault, rating10: initialRating10, reviewNotes: initialReviewNotes, inWatchlist: true }
    : inDisc
    ? { ...inDisc, rating10: initialRating10, reviewNotes: initialReviewNotes, inWatchlist: false }
    : {
        animeId,
        title: 'Loading...',
        coverImage: '',
        status: 'watching',
        season: 1,
        watchedEpisodes: 0,
        totalEpisodes: null,
        rating10: initialRating10,
        reviewNotes: initialReviewNotes,
        inWatchlist: false
      };
  state.detailRich = null;
  state.detailRootMedia = null;
  state.detailLoading = true;
  state.detailRatingSaving = false;
  state.detailRatingFeedback = '';
  state.detailRatingError = '';
  state.detailNoteDraft = initialReviewNotes;
  state.discussion = {
    ...state.discussion,
    animeId,
    comments: state.discussion.animeId === animeId ? state.discussion.comments : [],
    totalComments: state.discussion.animeId === animeId ? state.discussion.totalComments : 0,
    hasMore: false,
    loading: true,
    loadingMore: false,
    submitting: false,
    success: '',
    error: '',
    draft: '',
    commentTags: [],
    activeReplyTarget: null,
    replyDraft: '',
    replyTags: [],
    editingCommentId: null,
    editingCommentDraft: '',
    editingReplyId: null,
    editingReplyCommentId: null,
    editingReplyThreadId: null,
    editingReplyDraft: '',
    mentionSuggestions: [],
    mentionActiveScope: null,
    contextMenu: null
  };
  window.scrollTo({ top: 0, behavior: 'smooth' });
  renderApp();
  loadAnimeDiscussion(animeId, true);

  const gql = `
    query($id: Int) {
      Media(id: $id, type: ANIME) {
        id
        type
        title { english romaji native userPreferred }
        synonyms
        coverImage { extraLarge large medium }
        bannerImage
        format
        status
        episodes
        duration
        averageScore
        season
        seasonYear
        startDate { year month day }
        endDate { year month day }
        countryOfOrigin
        genres
        isAdult
        description(asHtml: false)
        nextAiringEpisode { airingAt episode }
        studios(isMain: true) { nodes { name } }
        relations {
          edges {
            relationType
            node {
              id
              type
              countryOfOrigin
              title { english romaji userPreferred }
              synonyms
              coverImage { extraLarge large medium }
              format
              status
              episodes
              duration
              seasonYear
              startDate { year month day }
              endDate { year month day }
              averageScore
            }
          }
        }
      }
    }
  `;

  const myToken = ++detailFetchToken;

  try {
    let json = null;
    for (let attempt = 0; attempt < 3; attempt++) {
      const resp = await fetch('https://graphql.anilist.co', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: gql, variables: { id: animeId } })
      });
      if ((resp.status === 429 || resp.status >= 500) && attempt < 2) {
        const retryAfter = Number(resp.headers.get('Retry-After') || 0);
        await new Promise((r) => setTimeout(r, retryAfter >= 1 && retryAfter <= 4 ? retryAfter * 1000 : 850 * (attempt + 1)));
        continue;
      }
      if (resp.ok) {
        json = await resp.json();
      }
      break;
    }
    if (myToken !== detailFetchToken) return;
    const m = json?.data?.Media;
    if (m) {
      state.detailRootMedia = m;
      const existing = state.vault.find((v) => v.animeId === animeId);
      const detailCountry = m.countryOfOrigin || 'JP';
      const detailIsDonghua = String(detailCountry).toUpperCase() === 'CN';
      const detailSynonyms = Array.isArray(m.synonyms) ? m.synonyms.filter(Boolean) : [];
      const detailResolvedTitle =
        resolveEnglishTitle(m.title?.english || '', m.title?.romaji || '', detailSynonyms, detailIsDonghua) ||
        m.title?.english ||
        m.title?.romaji ||
        'Anime';
      const detailFmt = String(m.format || 'TV').toUpperCase();
      const detailMedStatus = String(m.status || 'RELEASING').toUpperCase();
      const detailRawEps = m.episodes && Number(m.episodes) > 0 ? Number(m.episodes) : null;
      const detailNextEp = m.nextAiringEpisode?.episode || null;
      const detailTotalEps =
        detailFmt === 'MOVIE'
          ? (detailRawEps || 1)
          : detailMedStatus === 'RELEASING' && detailIsDonghua && detailRawEps && detailNextEp && detailNextEp > detailRawEps
          ? null
          : (detailRawEps || existing?.totalEpisodes || null);
      state.detailAnime = {
        ...(existing || {}),
        animeId: m.id,
        title: detailResolvedTitle,
        titleEnglish: detailResolvedTitle,
        titleRomaji: m.title?.romaji || '',
        titleNative: m.title?.native || '',
        synonyms: detailSynonyms,
        coverImage: m.coverImage?.extraLarge || m.coverImage?.large || '',
        bannerImage: m.bannerImage || '',
        format: detailFmt,
        mediaStatus: detailMedStatus,
        duration: m.duration || null,
        seasonYear: m.seasonYear || m.startDate?.year || null,
        countryOfOrigin: detailCountry,
        totalEpisodes: detailTotalEps,
        averageScore: m.averageScore || null,
        genres: normalizeAnimeGenresWithAdult(m.genres || [], Boolean(m.isAdult)),
        isAdult: isHentaiOrAdultAnime(m.genres || [], Boolean(m.isAdult)),
        synopsis: (m.description || '').replace(/<[^>]+>/g, ''),
        airingTimeUnix: m.nextAiringEpisode?.airingAt || null,
        nextEpisodeNumber: detailNextEp,
        status: existing?.status || 'watching',
        season: existing?.season || 1,
        watchedEpisodes: existing?.watchedEpisodes || 0,
        rating10: detailUserTouchedRating
          ? state.detailAnime?.rating10 ?? null
          : (existing?.rating10 ?? state.detailAnime?.rating10 ?? null),
        reviewNotes: detailUserTouchedNote
          ? (state.detailNoteDraft ?? state.detailAnime?.reviewNotes ?? '')
          : (existing?.reviewNotes || state.detailAnime?.reviewNotes || ''),
        isPinned: Boolean(existing?.isPinned),
        isFavorite: Boolean(existing?.isFavorite),
        inWatchlist: Boolean(existing)
      };
      if (!detailUserTouchedNote && !state.detailNoteDraft && state.detailAnime.reviewNotes) {
        state.detailNoteDraft = state.detailAnime.reviewNotes;
      }

      // Render detail page immediately, then resolve the complete multi-season franchise chain via batched BFS
      state.detailLoading = false;
      renderApp();

      const chainResult = await resolveFullSeasonChain(m);
      if (myToken !== detailFetchToken) return;
      state.detailRich = {
        seasons: chainResult.seasons || [],
        moviesAndOvas: chainResult.moviesAndOvas || [],
        relatedMedia: chainResult.relatedMedia || [],
        franchiseId: chainResult.franchiseId || m.id,
        rawMainSeasonNodeCount: chainResult.rawMainSeasonNodeCount || 0
      };
      renderApp();
    }
  } catch (_) {
  } finally {
    if (myToken === detailFetchToken) {
      state.detailLoading = false;
      renderApp();
    }
  }
}

async function ensureCatalogAnimeSynced(item) {
  if (!state.session || !item || !item.animeId) return;
  const catalogKey = `${item.titleEnglish || item.title}|${item.coverImage}|${item.totalEpisodes || 0}|${item.mediaStatus || ''}`;
  if (syncedCatalogHashes.get(item.animeId) === catalogKey) return;
  await supabaseRequest(
    '/rest/v1/anime?on_conflict=anilist_id',
    'POST',
    {
      anilist_id: Number(item.animeId),
      title_english: String(item.titleEnglish || item.title || 'Anime').slice(0, 140),
      title_romaji: String(item.titleRomaji || item.title || 'Anime').slice(0, 140),
      title_native: String(item.titleNative || '').slice(0, 100) || null,
      cover_image: item.coverImage || '',
      banner_image: item.bannerImage || null,
      episodes: item.totalEpisodes || null,
      release_year: item.seasonYear || null,
      average_score: item.averageScore || null,
      status: item.mediaStatus || 'RELEASING',
      format: item.format || 'TV',
      genres: normalizeAnimeGenresWithAdult(item.genres || [], Boolean(item.isAdult)).slice(0, 6),
      next_airing_at: item.airingTimeUnix || null,
      next_episode_number: item.nextEpisodeNumber || null
    },
    'resolution=merge-duplicates,return=minimal'
  ).catch(() => {});
  syncedCatalogHashes.set(item.animeId, catalogKey);
}

// Save Library Entry to Supabase — NEVER implicitly adds to Home unless opts.addToHome === true
async function saveVaultEntry(item, opts = {}) {
  const animeId = Number(item.animeId);
  const existing = state.vault.find((v) => Number(v.animeId) === animeId);
  let nextStatus = item.status || existing?.status || 'watching';
  const totalEp = item.totalEpisodes ?? existing?.totalEpisodes ?? null;
  const watchedEp = Math.max(0, Number(item.watchedEpisodes ?? existing?.watchedEpisodes ?? 0));
  const mediaStatus = item.mediaStatus || existing?.mediaStatus || 'RELEASING';

  // Part 2: Strict automatic completion ONLY when officially FINISHED + known total + watched >= total
  if (shouldAutoComplete(mediaStatus, totalEp, watchedEp)) {
    nextStatus = 'completed';
  }

  // Part 1, 2, 3: Determine Home membership independently
  let nextInHome = opts.addToHome === true
    ? true
    : opts.removeFromHome === true
    ? false
    : state.homeTrackingIds.has(animeId);

  if (shouldRemoveFromHome(nextStatus, mediaStatus, totalEp, watchedEp)) {
    nextInHome = false;
  }

  if (nextInHome) {
    state.homeTrackingIds.add(animeId);
  } else {
    state.homeTrackingIds.delete(animeId);
  }
  saveHomeTrackingIds();

  const cleanItem = {
    ...(existing || {}),
    ...item,
    animeId,
    status: nextStatus,
    watchedEpisodes: watchedEp,
    totalEpisodes: totalEp,
    mediaStatus,
    inWatchlist: true,
    inHome: nextInHome
  };

  state.vault = [cleanItem, ...state.vault.filter((v) => Number(v.animeId) !== animeId)];
  if (state.detailAnime && Number(state.detailAnime.animeId) === animeId) {
    state.detailAnime = { ...state.detailAnime, ...cleanItem };
  }
  saveLocalVault();

  if (!state.session) return;
  try {
    await ensureCatalogAnimeSynced(cleanItem);

    await supabaseRequest(
      '/rest/v1/user_anime?on_conflict=user_id,anime_id',
      'POST',
      {
        user_id: state.session.userId,
        anime_id: animeId,
        status: cleanItem.status,
        episode_progress: cleanItem.watchedEpisodes,
        total_episodes: cleanItem.totalEpisodes || null,
        personal_rating: clampRating10(cleanItem.rating10),
        review_notes: String(cleanItem.reviewNotes || '').trim().slice(0, 320),
        is_favorite: Boolean(cleanItem.isFavorite)
      },
      'resolution=merge-duplicates,return=minimal'
    );

    if (opts.addToHome === true && nextInHome) {
      await syncAddToHomeCloud(animeId, cleanItem.airingDayOfWeek || null);
    } else if (!nextInHome && (opts.removeFromHome === true || shouldRemoveFromHome(nextStatus, mediaStatus, totalEp, watchedEp))) {
      await syncRemoveFromHomeCloud(animeId);
    }

    const hasRating = cleanItem.rating10 !== null && Number(cleanItem.rating10) > 0;
    const hasNote = String(cleanItem.reviewNotes || '').trim().length > 0;
    if ((hasRating || hasNote) && state.profile.memberStatus !== 'left' && state.profile.memberStatus !== 'removed') {
      await syncCommunityNoteCloud(animeId, hasRating ? Number(cleanItem.rating10) : null, String(cleanItem.reviewNotes || '').trim());
    }

    await loadCommunityReviews();
  } catch (err) {
    if (opts.throwOnError) throw err;
  }
}

async function syncAddToHomeCloud(animeId, airingDay = null) {
  if (!state.session) return;
  try {
    await supabaseRequest('/rest/v1/rpc/add_to_home', 'POST', {
      p_anime_id: Number(animeId),
      p_airing_day: airingDay && airingDay >= 1 && airingDay <= 7 ? Number(airingDay) : null
    });
  } catch (_) {
    await supabaseRequest(
      '/rest/v1/home_tracking?on_conflict=user_id,anime_id',
      'POST',
      {
        user_id: state.session.userId,
        anime_id: Number(animeId),
        airing_day_of_week: airingDay && airingDay >= 1 && airingDay <= 7 ? Number(airingDay) : null
      },
      'resolution=merge-duplicates,return=minimal'
    ).catch(() => {});
  }
}

async function syncRemoveFromHomeCloud(animeId) {
  if (!state.session) return;
  try {
    await supabaseRequest('/rest/v1/rpc/remove_from_home', 'POST', {
      p_anime_id: Number(animeId)
    });
  } catch (_) {
    const uid = encodeURIComponent(state.session.userId);
    await supabaseRequest(`/rest/v1/home_tracking?user_id=eq.${uid}&anime_id=eq.${Number(animeId)}`, 'DELETE').catch(() => {});
  }
}

async function syncCommunityNoteCloud(animeId, rating10Val, noteText) {
  if (!state.session) return;
  await Promise.all([
    supabaseRequest(
      '/rest/v1/community_members?on_conflict=user_id',
      'POST',
      { user_id: state.session.userId, role: 'member', status: 'active' },
      'resolution=merge-duplicates,return=minimal'
    ).catch(() => {}),
    supabaseRequest(
      '/rest/v1/privacy_settings?on_conflict=user_id',
      'POST',
      {
        user_id: state.session.userId,
        reviews_visibility: 'community',
        activity_visibility: 'community'
      },
      'resolution=merge-duplicates,return=minimal'
    ).catch(() => {})
  ]);
  try {
    await supabaseRequest('/rest/v1/rpc/upsert_community_note', 'POST', {
      p_anime_id: Number(animeId),
      p_rating: clampRating10(rating10Val),
      p_note_text: String(noteText || '').trim().slice(0, 600)
    });
  } catch (_) {
    await supabaseRequest(
      '/rest/v1/community_notes?on_conflict=user_id,anime_id',
      'POST',
      {
        user_id: state.session.userId,
        anime_id: Number(animeId),
        rating: clampRating10(rating10Val),
        note_text: String(noteText || '').trim().slice(0, 600)
      },
      'resolution=merge-duplicates,return=minimal'
    ).catch(() => {});
  }
}

// Part 1: Explicitly Add Anime to Home active tracking (and Library if not yet present)
async function addToHome(item) {
  if (!item || !item.animeId) return;
  const animeId = Number(item.animeId);
  const existing = state.vault.find((v) => Number(v.animeId) === animeId);
  const base = existing ? { ...existing, ...item } : { ...item, status: 'watching', watchedEpisodes: 0 };
  if (base.status === 'dropped') {
    base.status = 'watching';
  }
  state.homeTrackingIds.add(animeId);
  saveHomeTrackingIds();
  await saveVaultEntry(base, { addToHome: true });
  showToast('Added to Home tracking');
  renderApp();
}

// Part 1 & Part 4: Remove ONLY from Home; preserve Library, progress, notes, ratings & Community
async function removeFromHomeOnly(animeId) {
  const aid = Number(animeId);
  state.homeTrackingIds.delete(aid);
  saveHomeTrackingIds();
  state.vault = state.vault.map((v) => (Number(v.animeId) === aid ? { ...v, inHome: false } : v));
  if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
    state.detailAnime = { ...state.detailAnime, inHome: false };
  }
  saveLocalVault();
  showToast('Removed from Home — preserved in Library');
  renderApp();
  await syncRemoveFromHomeCloud(aid);
}

// Part 1: Post or update a Community note/rating WITHOUT ever adding the anime to Home
async function submitCommunityCommentNote(animeId, rating10Val, noteText, animeMeta = null, opts = {}) {
  const aid = Number(animeId);
  const cleanNote = String(noteText ?? '').trim();
  const cleanRating = clampRating10(rating10Val);
  const replaceNote = Boolean(opts.replaceNote);
  const silentRender = Boolean(opts.silentRender);
  if (!cleanNote && !cleanRating) {
    const errMsg = 'Please select a rating or write a note first.';
    if (opts.throwOnError) throw new Error(errMsg);
    return { ok: false, error: errMsg };
  }

  const existingVault = state.vault.find((v) => Number(v.animeId) === aid);
  if (existingVault) {
    const resolvedNotes = replaceNote ? cleanNote : (cleanNote || existingVault.reviewNotes || '');
    // Preserve existing Home membership strictly; NEVER add to Home just because user rated/noted
    await saveVaultEntry(
      {
        ...existingVault,
        rating10: cleanRating ?? existingVault.rating10,
        reviewNotes: resolvedNotes
      },
      { addToHome: false, throwOnError: Boolean(opts.throwOnError) }
    );
    if (!opts.silentToast) showToast('Rating & note saved');
    if (!silentRender) renderApp();
    return { ok: true };
  }

  // Anime is NOT in Home or Library: save purely to Community (community_notes) — NEVER adds to Home!
  const meta = animeMeta || state.detailAnime || state.discover.find((d) => Number(d.animeId) === aid) || { animeId: aid };
  const existingCommRev = state.session
    ? state.communityReviews.find((r) => r.user_id === state.session.userId && Number(r.anime_id) === aid)
    : null;
  const resolvedRating = cleanRating ?? (existingCommRev?.rating ?? null);
  const resolvedNote = replaceNote ? cleanNote : (cleanNote || existingCommRev?.review_notes || '');

  if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
    state.detailAnime = {
      ...state.detailAnime,
      rating10: resolvedRating ?? state.detailAnime.rating10,
      reviewNotes: replaceNote ? cleanNote : (resolvedNote || state.detailAnime.reviewNotes || '')
    };
  }

  // Update local community preview immediately
  if (state.session) {
    const metaGenres = normalizeAnimeGenresWithAdult(meta.genres || [], Boolean(meta.isAdult || meta.is_adult));
    const newEntry = {
      id: existingCommRev?.id || `local_note_${state.session.userId}_${aid}`,
      source: 'community_notes',
      user_id: state.session.userId,
      username: state.profile.username || 'Member',
      avatar_key: state.profile.avatarKey || 'avatar_1',
      anime_id: aid,
      anime_title: meta.titleEnglish || meta.title || meta.anime_title || 'Anime',
      anime_cover_image: meta.coverImage || meta.anime_cover_image || '',
      genres: metaGenres,
      is_adult: isHentaiOrAdultAnime(metaGenres, Boolean(meta.isAdult || meta.is_adult)),
      rating: resolvedRating,
      review_notes: resolvedNote,
      updated_at: new Date().toISOString(),
      likes_count: existingCommRev?.likes_count || 0,
      liked_by_me: Boolean(existingCommRev?.liked_by_me)
    };
    state.communityReviews = [
      newEntry,
      ...state.communityReviews.filter((r) => !(r.user_id === state.session.userId && Number(r.anime_id) === aid))
    ];
    if (!silentRender) renderApp();

    try {
      await ensureCatalogAnimeSynced({
        animeId: aid,
        title: meta.title || meta.anime_title || 'Anime',
        titleEnglish: meta.titleEnglish || meta.title || meta.anime_title || 'Anime',
        titleRomaji: meta.titleRomaji || meta.title || meta.anime_title || 'Anime',
        coverImage: meta.coverImage || meta.anime_cover_image || '',
        genres: metaGenres,
        isAdult: isHentaiOrAdultAnime(metaGenres, Boolean(meta.isAdult || meta.is_adult))
      });
      await syncCommunityNoteCloud(aid, resolvedRating, resolvedNote);
      await loadCommunityReviews();
    } catch (err) {
      if (opts.throwOnError) throw err;
    }
  }
  if (!opts.silentToast) showToast('Posted to Community (not added to Home)');
  if (!silentRender) renderApp();
  return { ok: true };
}

// ============================================================================
// ANIME DISCUSSION, @MENTIONS, TARGET-USER REPLY THREADS & NOTIFICATIONS RPCs
// ============================================================================

const MAX_MENTIONS_PER_CONTENT = 5;
const MENTION_REGEX = /(^|[^A-Za-z0-9_])@([A-Za-z0-9_]{2,32})\b/g;
let mentionSearchTimer = null;
let mentionSearchSeq = 0;

function extractMentionTokens(text) {
  const input = String(text || '');
  const tokens = [];
  let m;
  MENTION_REGEX.lastIndex = 0;
  while ((m = MENTION_REGEX.exec(input)) !== null) {
    const prefixLen = m[1] ? m[1].length : 0;
    const start = m.index + prefixLen;
    const username = m[2];
    tokens.push({
      username,
      normalized: username.toLowerCase(),
      start,
      end: start + 1 + username.length
    });
  }
  return tokens;
}

function validateContentMentions(text, explicitTagCount = 0) {
  const tokens = extractMentionTokens(text);
  const totalCount = tokens.length + Math.max(0, Number(explicitTagCount || 0));
  const uniqueSet = new Set(tokens.map((t) => t.normalized));
  if (totalCount > MAX_MENTIONS_PER_CONTENT) {
    return {
      ok: false,
      totalMentions: totalCount,
      uniqueCount: uniqueSet.size,
      error: `Maximum ${MAX_MENTIONS_PER_CONTENT} tag entries allowed per comment or reply (currently ${totalCount}).`
    };
  }
  return {
    ok: true,
    totalMentions: totalCount,
    uniqueCount: uniqueSet.size,
    error: ''
  };
}

function detectActiveMentionQuery(text, cursorPos) {
  const safeText = String(text || '');
  const pos = Math.max(0, Math.min(safeText.length, Number(cursorPos || 0)));
  const before = safeText.slice(0, pos);
  const m = before.match(/(?:^|[^A-Za-z0-9_])@([A-Za-z0-9_]{0,32})$/);
  if (!m) return null;
  const query = m[1] || '';
  const start = pos - query.length - 1;
  return { query, start, end: pos };
}

function stripMentionTrigger(text, start, end) {
  const safeText = String(text || '');
  const safeStart = Math.max(0, Math.min(safeText.length, start));
  const safeEnd = Math.max(safeStart, Math.min(safeText.length, end));
  let prefix = safeText.slice(0, safeStart);
  let suffix = safeText.slice(safeEnd);
  if (prefix.endsWith(' ') && suffix.startsWith(' ')) {
    suffix = suffix.slice(1);
  }
  return {
    text: `${prefix}${suffix}`,
    cursor: prefix.length
  };
}

function renderSelectedTagChipsHtml(scope, tags) {
  if (!Array.isArray(tags) || tags.length === 0) return '';
  return `
    <div class="tag-chips-row" id="tag-chips-${escapeHtml(scope)}">
      ${tags
        .map(
          (u, idx) => `
          <span class="tag-chip">
            <span>Tagged: ${escapeHtml(u.username)}</span>
            <button type="button" class="tag-chip-remove" data-remove-tag-scope="${escapeHtml(scope)}" data-remove-tag-idx="${idx}" aria-label="Remove tag">×</button>
          </span>`
        )
        .join('')}
    </div>
  `;
}

function renderMentionsDisplayHtml(mentions) {
  if (!Array.isArray(mentions) || mentions.length === 0) return '';
  const map = new Map();
  for (const m of mentions) {
    const uname = m?.mentioned_username || m?.username || '';
    const uid = m?.mentioned_user_id || m?.user_id || '';
    if (!uname) continue;
    const prev = map.get(uname);
    map.set(uname, { uid: prev?.uid || uid, count: (prev?.count || 0) + 1 });
  }
  if (map.size === 0) return '';
  return `
    <div class="tag-chips-row posted">
      ${Array.from(map.entries())
        .map(
          ([uname, info]) =>
            `<button type="button" class="tag-chip posted clickable-tag" data-mention-click-name="${escapeHtml(uname)}" data-mention-click-uid="${escapeHtml(info.uid || '')}">@${escapeHtml(uname)}${info.count > 1 ? ` ×${info.count}` : ''}</button>`
        )
        .join('')}
    </div>
  `;
}

// ============================================================================
// CUSTOM EMOJI MULTI-LAYER CACHE ENGINE (SUPABASE STORAGE + CACHE API + LAZY)
// - Stores ONLY emoji IDs ([emoji:<id>]) in DB/messages; never duplicates images
// - Reuses single canonical public Storage URL across all users & messages
// - Layer 1: In-memory objectURL Map (0ms lookup)
// - Layer 2: Browser Cache API ('livtrak-emoji-cache-v1') w/ immutable headers
// - Layer 3: localStorage metadata cache (15m TTL + stale-while-revalidate)
// - Supports both "community_emojis" and "community-emojis" public buckets
// ============================================================================

const EMOJI_META_STORAGE_KEY = 'livtrak_community_emojis_meta_v1';
const EMOJI_USAGE_LOCAL_KEY = 'livtrak_emoji_local_usage_v1';
const EMOJI_META_TTL_MS = 15 * 60 * 1000;
const EMOJI_IMAGE_CACHE_NAME = 'livtrak-emoji-cache-v1';
const EMOJI_TOKEN_REGEX = /\[emoji:([a-zA-Z0-9_-]{1,64})\]/g;
const EMOJI_BUCKET_CANDIDATES = ['community_emojis', 'community-emojis'];

const EmojiCacheManager = {
  items: [],
  byId: new Map(),
  objectUrls: new Map(),
  inflightImages: new Map(),
  failedIds: new Set(),
  localUsage: {},
  fetchedAt: 0,
  metaPromise: null,
  observer: null,
  loadedFromStorage: false,

  slugFromPath(path) {
    const file = String(path || '').replace(/^.*\//, '');
    const base = file.replace(/\.[^.]+$/, '');
    const slug = base.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
    return slug ? slug.slice(0, 64) : `emoji_${Math.abs(file.length)}`;
  },

  buildCanonicalUrl(bucketName, storagePath) {
    const base = getVaultBaseUrl();
    const bucket = encodeURIComponent(bucketName || 'community_emojis');
    const cleanPath = String(storagePath || '')
      .replace(/^\/+/, '')
      .split('/')
      .map((seg) => encodeURIComponent(seg))
      .join('/');
    return `${base}/storage/v1/object/public/${bucket}/${cleanPath}`;
  },

  hydrateFromLocalStorage() {
    if (this.loadedFromStorage) return;
    this.loadedFromStorage = true;
    try {
      const usageRaw = localStorage.getItem(EMOJI_USAGE_LOCAL_KEY);
      if (usageRaw) this.localUsage = JSON.parse(usageRaw) || {};
    } catch (_) {
      this.localUsage = {};
    }
    try {
      const raw = localStorage.getItem(EMOJI_META_STORAGE_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw);
      if (parsed && Array.isArray(parsed.items)) {
        this.fetchedAt = Number(parsed.fetchedAt || 0);
        this.applyItems(parsed.items, false);
      }
    } catch (_) {}
  },

  applyItems(rawList, persist = true) {
    const normalized = [];
    const map = new Map();
    for (const raw of rawList || []) {
      const id = String(raw.id || '').trim().toLowerCase();
      const storagePath = String(raw.storage_path || raw.name || '').trim();
      if (!id || !storagePath) continue;
      const bucketName = String(raw.bucket_name || 'community_emojis').trim();
      const serverUsage = Math.max(0, Number(raw.usage_count || 0));
      const localBoost = Math.max(0, Number(this.localUsage[id] || 0));
      const item = {
        id,
        bucket_name: bucketName,
        storage_path: storagePath,
        file_name: raw.file_name || storagePath.replace(/^.*\//, ''),
        mime_type: raw.mime_type || 'image/gif',
        usage_count: serverUsage,
        score: serverUsage + localBoost,
        url: this.buildCanonicalUrl(bucketName, storagePath)
      };
      if (!map.has(id)) {
        map.set(id, item);
        normalized.push(item);
      }
    }
    normalized.sort((a, b) => (b.score - a.score) || a.id.localeCompare(b.id));
    this.items = normalized;
    this.byId = map;
    if (persist) {
      this.fetchedAt = Date.now();
      try {
        localStorage.setItem(
          EMOJI_META_STORAGE_KEY,
          JSON.stringify({
            fetchedAt: this.fetchedAt,
            items: normalized.map((e) => ({
              id: e.id,
              bucket_name: e.bucket_name,
              storage_path: e.storage_path,
              file_name: e.file_name,
              mime_type: e.mime_type,
              usage_count: e.usage_count
            }))
          })
        );
      } catch (_) {}
    }
  },

  async fetchStorageFallback() {
    const base = getVaultBaseUrl();
    const key = getVaultPublicKey();
    const token = state.session?.accessToken || key;
    const allowedExt = /\.(gif|webp|png|jpe?g|apng)$/i;
    for (const bucket of EMOJI_BUCKET_CANDIDATES) {
      try {
        const res = await fetch(`${base}/storage/v1/object/list/${encodeURIComponent(bucket)}`, {
          method: 'POST',
          headers: {
            apikey: key,
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ prefix: '', limit: 250, offset: 0, sortBy: { column: 'name', order: 'asc' } })
        });
        if (!res.ok) continue;
        const files = await res.json();
        if (!Array.isArray(files) || files.length === 0) continue;
        const mapped = files
          .filter((f) => f && f.name && !f.name.startsWith('.') && allowedExt.test(f.name))
          .map((f) => ({
            id: this.slugFromPath(f.name),
            bucket_name: bucket,
            storage_path: f.name,
            file_name: f.name,
            mime_type: f.metadata?.mimetype || 'image/gif',
            usage_count: 0
          }));
        if (mapped.length > 0) return mapped;
      } catch (_) {}
    }
    return [];
  },

  async ensureMetadata(force = false) {
    this.hydrateFromLocalStorage();
    const isFresh = this.items.length > 0 && Date.now() - this.fetchedAt < EMOJI_META_TTL_MS;
    if (!force && isFresh) {
      this.preloadTopEmojis();
      return this.items;
    }
    if (this.metaPromise) return this.metaPromise;
    this.metaPromise = (async () => {
      try {
        let fetched = [];
        try {
          const data = await supabaseRequest('/rest/v1/rpc/get_community_emojis', 'POST', {});
          if (data && Array.isArray(data.emojis)) {
            fetched = data.emojis;
          } else if (Array.isArray(data)) {
            fetched = data;
          }
        } catch (_) {}
        if (!fetched.length) {
          fetched = await this.fetchStorageFallback();
        }
        if (fetched.length > 0) {
          const prevCount = this.items.length;
          this.applyItems(fetched, true);
          this.preloadTopEmojis();
          if (prevCount === 0 && (state.discussion.emojiTrayOpen || state.communityCommentsAnimeId !== null || state.tab === 'detail')) {
            hydrateVisibleEmojiNodes();
          }
        }
      } catch (_) {
      } finally {
        this.metaPromise = null;
      }
      return this.items;
    })();
    return this.metaPromise;
  },

  getTopEmojis(limit = 8) {
    this.hydrateFromLocalStorage();
    return [...this.items]
      .sort((a, b) => (b.score - a.score) || a.id.localeCompare(b.id))
      .slice(0, limit);
  },

  getAllEmojis() {
    this.hydrateFromLocalStorage();
    return this.items;
  },

  getById(id) {
    this.hydrateFromLocalStorage();
    return this.byId.get(String(id || '').trim().toLowerCase()) || null;
  },

  recordUsage(emojiIds) {
    if (!Array.isArray(emojiIds) || emojiIds.length === 0) return;
    const clean = emojiIds
      .map((x) => String(x || '').trim().toLowerCase())
      .filter((x) => x && this.byId.has(x));
    if (!clean.length) return;
    for (const id of clean) {
      this.localUsage[id] = Number(this.localUsage[id] || 0) + 1;
      const item = this.byId.get(id);
      if (item) {
        item.usage_count += 1;
        item.score = item.usage_count + Number(this.localUsage[id] || 0);
      }
    }
    this.items.sort((a, b) => (b.score - a.score) || a.id.localeCompare(b.id));
    try {
      localStorage.setItem(EMOJI_USAGE_LOCAL_KEY, JSON.stringify(this.localUsage));
    } catch (_) {}
    // Fire-and-forget batch usage sync (never blocks message send)
    if (state.session) {
      supabaseRequest('/rest/v1/rpc/increment_emoji_usage', 'POST', {
        p_emoji_ids: clean.slice(0, 20)
      }).catch(() => {});
    }
  },

  async resolveImageSrc(emojiId) {
    const id = String(emojiId || '').trim().toLowerCase();
    if (!id) return null;
    if (this.objectUrls.has(id)) return this.objectUrls.get(id);
    if (this.failedIds.has(id)) return null;

    let item = this.byId.get(id);
    if (!item) {
      await this.ensureMetadata(false);
      item = this.byId.get(id);
      if (!item) return null;
    }

    if (this.inflightImages.has(id)) {
      return this.inflightImages.get(id);
    }

    const url = item.url;
    const task = (async () => {
      try {
        if (typeof caches !== 'undefined') {
          const expectedMime = /\.gif($|\?)/i.test(url)
            ? 'image/gif'
            : /\.webp($|\?)/i.test(url)
            ? 'image/webp'
            : /\.png($|\?)/i.test(url)
            ? 'image/png'
            : (item.mime_type || 'image/gif');
          const cache = await caches.open(EMOJI_IMAGE_CACHE_NAME);
          const cachedResp = await cache.match(url);
          if (cachedResp && cachedResp.ok) {
            const rawBlob = await cachedResp.blob();
            const typedBlob = (rawBlob.type && rawBlob.type.startsWith('image/'))
              ? rawBlob
              : new Blob([rawBlob], { type: expectedMime });
            const objUrl = URL.createObjectURL(typedBlob);
            this.objectUrls.set(id, objUrl);
            return objUrl;
          }
          const netResp = await fetch(url, { mode: 'cors', cache: 'force-cache' });
          if (!netResp.ok) throw new Error(`HTTP ${netResp.status}`);
          const buf = await netResp.arrayBuffer();
          const rawCt = netResp.headers.get('content-type') || '';
          const contentType = rawCt.startsWith('image/') ? rawCt : expectedMime;
          const cacheResp = new Response(buf, {
            status: 200,
            headers: {
              'Content-Type': contentType,
              'Cache-Control': 'public, max-age=31536000, immutable'
            }
          });
          cache.put(url, cacheResp).catch(() => {});
          const objUrl = URL.createObjectURL(new Blob([buf], { type: contentType }));
          this.objectUrls.set(id, objUrl);
          return objUrl;
        }
        // Fallback if Cache API unavailable: reuse canonical HTTP URL directly
        this.objectUrls.set(id, url);
        return url;
      } catch (_) {
        // Fallback to direct canonical URL once before marking failed
        return url;
      } finally {
        this.inflightImages.delete(id);
      }
    })();

    this.inflightImages.set(id, task);
    return task;
  },

  preloadTopEmojis(count = 8) {
    const top = this.getTopEmojis(count);
    if (!top.length) return;
    const schedule = typeof requestIdleCallback === 'function' ? requestIdleCallback : (fn) => setTimeout(fn, 60);
    schedule(() => {
      top.forEach((em) => {
        if (!this.objectUrls.has(em.id) && !this.inflightImages.has(em.id)) {
          this.resolveImageSrc(em.id).catch(() => {});
        }
      });
    });
  },

  bindImageElement(imgEl) {
    if (!imgEl || imgEl.dataset.emojiBound === '1') return;
    imgEl.dataset.emojiBound = '1';
    const emojiId = imgEl.getAttribute('data-emoji-id');
    const slot = imgEl.closest('.custom-emoji-slot');
    if (!emojiId) return;

    const finishLoad = (src) => {
      if (!src) {
        if (slot) {
          slot.classList.remove('loading');
          slot.innerHTML = `<span class="custom-emoji-fallback" title=":${escapeHtml(emojiId)}:">✦</span>`;
        }
        return;
      }
      imgEl.onload = () => {
        imgEl.classList.add('is-loaded');
        if (slot) slot.classList.remove('loading');
      };
      imgEl.onerror = () => {
        this.failedIds.add(emojiId);
        if (slot) {
          slot.classList.remove('loading');
          slot.innerHTML = `<span class="custom-emoji-fallback" title=":${escapeHtml(emojiId)}:">✦</span>`;
        }
      };
      imgEl.src = src;
      if (imgEl.complete && imgEl.naturalWidth > 0) {
        imgEl.classList.add('is-loaded');
        if (slot) slot.classList.remove('loading');
      }
    };

    const cached = this.objectUrls.get(emojiId);
    if (cached) {
      finishLoad(cached);
      return;
    }

    const triggerFetch = () => {
      this.resolveImageSrc(emojiId).then(finishLoad).catch(() => finishLoad(null));
    };

    const isLazy = imgEl.getAttribute('data-emoji-lazy') === '1';
    if (isLazy && typeof IntersectionObserver !== 'undefined') {
      if (!this.observer) {
        this.observer = new IntersectionObserver(
          (entries) => {
            entries.forEach((entry) => {
              if (!entry.isIntersecting) return;
              const target = entry.target;
              this.observer.unobserve(target);
              const eid = target.getAttribute('data-emoji-id');
              if (eid && !target.getAttribute('src')) {
                this.resolveImageSrc(eid)
                  .then((src) => {
                    const s = target.closest('.custom-emoji-slot');
                    if (!src) {
                      if (s) {
                        s.classList.remove('loading');
                        s.innerHTML = `<span class="custom-emoji-fallback">✦</span>`;
                      }
                      return;
                    }
                    target.onload = () => {
                      target.classList.add('is-loaded');
                      if (s) s.classList.remove('loading');
                    };
                    target.onerror = () => {
                      this.failedIds.add(eid);
                      if (s) {
                        s.classList.remove('loading');
                        s.innerHTML = `<span class="custom-emoji-fallback">✦</span>`;
                      }
                    };
                    target.src = src;
                  })
                  .catch(() => {});
              }
            });
          },
          { rootMargin: '80px' }
        );
      }
      this.observer.observe(imgEl);
    } else {
      triggerFetch();
    }
  }
};

function hydrateVisibleEmojiNodes() {
  document.querySelectorAll('img[data-emoji-id]').forEach((img) => {
    EmojiCacheManager.bindImageElement(img);
  });
}

function extractEmojiIdsFromText(text) {
  const raw = String(text || '');
  const ids = [];
  EMOJI_TOKEN_REGEX.lastIndex = 0;
  let match;
  while ((match = EMOJI_TOKEN_REGEX.exec(raw)) !== null) {
    if (match[1]) ids.push(match[1].toLowerCase());
  }
  return ids;
}

function renderCustomEmojiSlotHtml(emojiId, { lazy = false, sizeClass = '' } = {}) {
  const cleanId = String(emojiId || '').trim().toLowerCase();
  const cachedSrc = EmojiCacheManager.objectUrls.get(cleanId) || '';
  const loadedClass = cachedSrc ? 'is-loaded' : '';
  const loadingClass = cachedSrc ? '' : 'loading';
  return `<span class="custom-emoji-slot ${sizeClass} ${loadingClass}" title=":${escapeHtml(cleanId)}:"><img class="custom-emoji ${loadedClass}" data-emoji-id="${escapeHtml(cleanId)}" data-emoji-lazy="${lazy ? '1' : '0'}" ${cachedSrc ? `src="${escapeHtml(cachedSrc)}"` : ''} alt=":${escapeHtml(cleanId)}:" loading="${lazy ? 'lazy' : 'eager'}" decoding="auto" /></span>`;
}

function extractSoloEmojiId(text) {
  const cleaned = String(text || '').replace(/[\s\u200B-\u200D\uFEFF]+/g, '');
  const m = /^\[emoji:([^\]]{1,64})\]$/i.exec(cleaned);
  return m ? m[1].trim().toLowerCase() : null;
}

function isSoloUnicodeEmoji(text) {
  const cleaned = String(text || '').replace(/[\s\u200B-\u200D\uFEFF]+/g, '');
  if (!cleaned || cleaned.length > 16) return false;
  return /^(?:\p{Extended_Pictographic}(?:\uFE0F|\u200D\p{Extended_Pictographic})*){1,3}$/u.test(cleaned);
}

function extractFirstStickerCode(text) {
  const raw = String(text || '');
  const m1 = /\[emoji:([a-zA-Z0-9_-]{1,64})\]/i.exec(raw);
  if (m1 && m1[1]) return m1[1].trim().toLowerCase();
  const m2 = /(?:^|[\s(]):([a-zA-Z0-9][a-zA-Z0-9_-]{0,63}):(?=$|[\s).,!?])/i.exec(raw);
  if (m2 && m2[1]) return m2[1].trim().toLowerCase();
  return null;
}

function formatPlainPreviewWithEmojis(text) {
  return String(text || '').replace(EMOJI_TOKEN_REGEX, (_, id) => `:${id}:`);
}

function renderQuotedPreviewContentHtml(previewText, stickerCode = null) {
  const raw = String(previewText || '').trim();
  const cleanSticker = stickerCode ? String(stickerCode).trim().toLowerCase() : null;
  const tokenRegex = /\[emoji:([a-zA-Z0-9_-]{1,64})\]|:([a-zA-Z0-9][a-zA-Z0-9_-]{0,63}):/gi;
  let out = '';
  let last = 0;
  let renderedSticker = false;
  let match;
  while ((match = tokenRegex.exec(raw)) !== null) {
    if (match.index > last) {
      out += escapeHtml(raw.slice(last, match.index));
    }
    const sid = (match[1] || match[2] || '').trim().toLowerCase();
    if (sid) {
      out += renderCustomEmojiSlotHtml(sid, { lazy: false, sizeClass: 'size-quote' });
      renderedSticker = true;
    }
    last = match.index + match[0].length;
  }
  if (last < raw.length) {
    out += escapeHtml(raw.slice(last));
  }
  if (cleanSticker && !renderedSticker) {
    const stickerBadge = renderCustomEmojiSlotHtml(cleanSticker, { lazy: false, sizeClass: 'size-quote' });
    return out.trim() ? `${stickerBadge}<span>${out}</span>` : stickerBadge;
  }
  return out || (cleanSticker ? renderCustomEmojiSlotHtml(cleanSticker, { lazy: false, sizeClass: 'size-quote' }) : 'Original message');
}

function normalizeFlatReplyTo(rawReplyTo, fallbackParentComment = null, fallbackRepliesMap = null) {
  if (rawReplyTo && typeof rawReplyTo === 'object') {
    const mid = String(rawReplyTo.messageId || rawReplyTo.message_id || '').trim();
    const sName = String(rawReplyTo.senderName || rawReplyTo.sender_name || '').trim() || 'Member';
    const pText = String(rawReplyTo.previewText || rawReplyTo.preview_text || '').trim();
    const sCode =
      String(rawReplyTo.stickerCode || rawReplyTo.sticker_code || '').trim().toLowerCase() ||
      extractFirstStickerCode(pText) ||
      null;
    if (mid || pText || sCode) {
      return { messageId: mid, senderName: sName, previewText: pText, stickerCode: sCode };
    }
  }
  return null;
}

function flattenDiscussionMessages(rawList) {
  const flatMap = new Map();
  const list = Array.isArray(rawList) ? rawList : [];
  for (const item of list) {
    if (!item || !item.id) continue;
    const directReplyTo = normalizeFlatReplyTo(item.reply_to || item.replyTo);
    const body = item.content ?? item.body ?? '';
    flatMap.set(item.id, {
      ...item,
      content: body,
      source_type: item.source_type || 'comment',
      replyTo: directReplyTo,
      threads: []
    });
    // Backward-compatible flattening if backend still returns nested threads
    const threads = Array.isArray(item.threads) ? item.threads : [];
    const replyLookup = new Map();
    for (const th of threads) {
      for (const r of th.replies || []) {
        if (r && r.id) replyLookup.set(r.id, r);
      }
    }
    for (const th of threads) {
      for (const r of th.replies || []) {
        if (!r || !r.id || flatMap.has(r.id)) continue;
        const rBody = r.content ?? r.body ?? '';
        const refReplyId =
          r.quoted_reply_id ||
          (r.parent_comment_id && r.parent_comment_id !== item.id ? r.parent_comment_id : null);
        const refReply = refReplyId ? replyLookup.get(refReplyId) : null;
        const rawQuotePreview = refReplyId
          ? r.quoted_preview || refReply?.content || refReply?.body || ''
          : r.quoted_preview || body;
        const quoteAuthor = refReplyId
          ? refReply?.username || r.quoted_username || 'Member'
          : r.quoted_username || item.username || 'Member';
        flatMap.set(r.id, {
          id: r.id,
          anime_id: item.anime_id,
          user_id: r.user_id,
          username: r.username || 'Member',
          avatar_key: r.avatar_key || 'avatar_1',
          avatar_url: r.avatar_url || null,
          content: rBody,
          created_at: r.created_at,
          updated_at: r.updated_at,
          mentions: Array.isArray(r.mentions) ? r.mentions : [],
          source_type: 'reply',
          thread_id: th.id,
          _rootCommentId: item.id,
          replyTo: {
            messageId: refReplyId || r.quoted_comment_id || item.id,
            senderName: quoteAuthor,
            previewText: String(rawQuotePreview || '').slice(0, 180),
            stickerCode: extractFirstStickerCode(rawQuotePreview)
          },
          threads: []
        });
      }
    }
  }
  return Array.from(flatMap.values()).sort(
    (a, b) => new Date(b.created_at || 0).getTime() - new Date(a.created_at || 0).getTime()
  );
}

function renderMentionSegmentHtml(segmentText) {
  const raw = String(segmentText || '');
  const tokens = extractMentionTokens(raw);
  if (tokens.length === 0) return escapeHtml(raw);
  let out = '';
  let last = 0;
  for (const t of tokens) {
    out += escapeHtml(raw.slice(last, t.start));
    const handleText = raw.slice(t.start, t.end);
    const uname = t.username || handleText.replace(/^@/, '');
    out += `<button type="button" class="mention c-mention-btn" data-mention-click-name="${escapeHtml(uname)}">${escapeHtml(handleText)}</button>`;
    last = t.end;
  }
  out += escapeHtml(raw.slice(last));
  return out;
}

function renderMentionHighlightedHtml(text) {
  const raw = String(text || '');
  EMOJI_TOKEN_REGEX.lastIndex = 0;
  let out = '';
  let last = 0;
  let match;
  while ((match = EMOJI_TOKEN_REGEX.exec(raw)) !== null) {
    if (match.index > last) {
      out += renderMentionSegmentHtml(raw.slice(last, match.index));
    }
    out += renderCustomEmojiSlotHtml(match[1], { lazy: false });
    last = match.index + match[0].length;
  }
  if (last < raw.length) {
    out += renderMentionSegmentHtml(raw.slice(last));
  }
  return out;
}

function formatDiscussionTimestamp(isoStr) {
  if (!isoStr) return '';
  try {
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return String(isoStr).slice(0, 10);
    const diffSec = Math.max(0, Math.floor((Date.now() - d.getTime()) / 1000));
    if (diffSec < 60) return 'Just now';
    if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
    if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
    if (diffSec < 86400 * 7) return `${Math.floor(diffSec / 86400)}d ago`;
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  } catch (_) {
    return String(isoStr).slice(0, 10);
  }
}

function scrollChatFeedSmoothly(force = false) {
  setTimeout(() => {
    const stream = document.getElementById('livtrak-chat-messages');
    if (!stream) return;
    const distanceFromBottom = stream.scrollHeight - stream.scrollTop - stream.clientHeight;
    if (force || distanceFromBottom < 220) {
      stream.scrollTo({ top: stream.scrollHeight, behavior: 'smooth' });
    }
  }, 50);
}

const DiscussionRealtimeManager = {
  ws: null,
  heartbeatId: null,
  reconnectId: null,
  subscribedAnimeId: null,
  refCounter: 1,
  profileCache: new Map(),

  cacheProfilesFromMessages(list) {
    for (const m of list || []) {
      if (m?.user_id && m?.username) {
        const normalizedKey = normalizeAvatarCompositeKey(m.avatar_key, m.avatar_url, m.avatar_frame);
        const existing = this.profileCache.get(m.user_id);
        const keepExistingFrame =
          existing && extractAvatarFrameKey(existing.avatar_key) && !extractAvatarFrameKey(normalizedKey);
        this.profileCache.set(m.user_id, {
          username: m.username,
          avatar_key: keepExistingFrame ? existing.avatar_key : normalizedKey,
          avatar_url: m.avatar_url || null
        });
      }
    }
  },

  async hydrateProfilesForMessages(list) {
    const msgs = Array.isArray(list) ? list : [];
    const userIds = Array.from(new Set(msgs.map((m) => m?.user_id).filter(Boolean)));
    if (userIds.length > 0 && state.session) {
      try {
        const rows = await supabaseRequest(
          `/rest/v1/profiles?id=in.(${userIds.map((id) => encodeURIComponent(id)).join(',')})&select=*`,
          'GET'
        );
        if (Array.isArray(rows)) {
          rows.forEach((p) => {
            if (!p?.id) return;
            this.profileCache.set(p.id, {
              username: p.username || 'Member',
              avatar_key: normalizeAvatarCompositeKey(p.avatar_key, p.avatar_url, p.avatar_frame),
              avatar_url: p.avatar_url || null
            });
          });
        }
      } catch (_) {}
    }
    return msgs.map((m) => {
      if (!m) return m;
      const isOwn = Boolean(state.session?.userId && m.user_id === state.session.userId);
      const cached = this.profileCache.get(m.user_id);
      const resolvedAvatarKey = isOwn
        ? (state.profile?.avatarKey || cached?.avatar_key || normalizeAvatarCompositeKey(m.avatar_key, m.avatar_url, m.avatar_frame))
        : (cached?.avatar_key || normalizeAvatarCompositeKey(m.avatar_key, m.avatar_url, m.avatar_frame));
      const resolvedUsername = isOwn
        ? (state.profile?.username || cached?.username || m.username || 'Member')
        : (cached?.username || m.username || 'Member');
      return {
        ...m,
        username: resolvedUsername,
        avatar_key: resolvedAvatarKey
      };
    });
  },

  async resolveSenderProfile(userId) {
    if (!userId) return { username: 'Member', avatar_key: 'avatar_1', avatar_url: null };
    if (state.session?.userId === userId) {
      const own = {
        username: state.profile?.username || state.session.username || 'Member',
        avatar_key: state.profile?.avatarKey || 'avatar_1',
        avatar_url: null
      };
      this.profileCache.set(userId, own);
      return own;
    }
    if (this.profileCache.has(userId)) return this.profileCache.get(userId);
    try {
      const rows = await supabaseRequest(
        `/rest/v1/profiles?id=eq.${encodeURIComponent(userId)}&select=*&limit=1`,
        'GET'
      );
      const p = Array.isArray(rows) ? rows[0] : null;
      if (p) {
        const resolved = {
          username: p.username || 'Member',
          avatar_key: normalizeAvatarCompositeKey(p.avatar_key, p.avatar_url, p.avatar_frame),
          avatar_url: p.avatar_url || null
        };
        this.profileCache.set(userId, resolved);
        return resolved;
      }
    } catch (_) {}
    return { username: 'Member', avatar_key: 'avatar_1', avatar_url: null };
  },

  ensureSubscribed(animeId) {
    const aid = Number(animeId);
    const token = state.session?.accessToken;
    if (!aid || !token) {
      this.disconnect();
      return;
    }
    if (this.subscribedAnimeId === aid && this.ws && this.ws.readyState <= 1) return;
    this.disconnect();
    const baseUrl = getVaultBaseUrl();
    const pubKey = getVaultPublicKey();
    if (!baseUrl.startsWith('http')) return;
    const wsUrl = `${baseUrl.replace(/^http/, 'ws')}/realtime/v1/websocket?apikey=${encodeURIComponent(pubKey)}&vsn=1.0.0`;
    try {
      const ws = new WebSocket(wsUrl);
      this.ws = ws;
      this.subscribedAnimeId = aid;
      const topic = `realtime:public:anime_comments:${aid}`;
      ws.onopen = () => {
        ws.send(JSON.stringify({
          topic,
          event: 'phx_join',
          payload: {
            config: {
              broadcast: { self: false },
              presence: { key: '' },
              postgres_changes: [
                { event: '*', schema: 'public', table: 'anime_comments', filter: `anime_id=eq.${aid}` },
                { event: 'UPDATE', schema: 'public', table: 'comment_replies' },
                { event: 'DELETE', schema: 'public', table: 'comment_replies' },
                { event: 'UPDATE', schema: 'public', table: 'profiles' }
              ]
            },
            access_token: token
          },
          ref: String(this.refCounter++)
        }));
        ws.send(JSON.stringify({ topic, event: 'access_token', payload: { access_token: token }, ref: String(this.refCounter++) }));
        this.heartbeatId = setInterval(() => {
          if (ws.readyState === 1) {
            ws.send(JSON.stringify({ topic: 'phoenix', event: 'heartbeat', payload: {}, ref: String(this.refCounter++) }));
          }
        }, 25000);
      };
      ws.onmessage = async (evt) => {
        try {
          if (Number(state.discussion.animeId) !== aid) return;
          const msg = JSON.parse(evt.data);
          if (msg?.event !== 'postgres_changes' || !msg?.payload?.data) return;
          const data = msg.payload.data;
          const evType = String(data.type || '').toUpperCase();
          const table = String(data.table || '');
          const rec = data.record || null;
          const oldRec = data.old_record || null;

          if (evType === 'INSERT' && table === 'anime_comments' && rec && rec.id) {
            if (Number(rec.anime_id) !== aid) return;
            if (state.discussion.comments.some((m) => m.id === rec.id)) return;
            const sender = await this.resolveSenderProfile(rec.user_id);
            if (Number(state.discussion.animeId) !== aid) return;
            const flatMsg = flattenDiscussionMessages([
              {
                ...rec,
                username: sender.username,
                avatar_key: sender.avatar_key,
                avatar_url: sender.avatar_url,
                source_type: 'comment',
                mentions: []
              }
            ])[0];
            if (flatMsg && !state.discussion.comments.some((m) => m.id === flatMsg.id)) {
              state.discussion.comments = [flatMsg, ...state.discussion.comments];
              state.discussion.totalComments = Math.max(state.discussion.comments.length, state.discussion.totalComments + 1);
              renderApp();
              scrollChatFeedSmoothly(rec.user_id === state.session?.userId);
            }
          } else if (evType === 'UPDATE' && table === 'profiles' && rec && rec.id) {
            const nextAvatarKey = normalizeAvatarCompositeKey(rec.avatar_key, rec.avatar_url, rec.avatar_frame);
            const nextUsername = rec.username || this.profileCache.get(rec.id)?.username || 'Member';
            this.profileCache.set(rec.id, {
              username: nextUsername,
              avatar_key: nextAvatarKey,
              avatar_url: rec.avatar_url || null
            });
            let changed = false;
            if (Array.isArray(state.discussion.comments)) {
              state.discussion.comments = state.discussion.comments.map((m) => {
                if (m.user_id === rec.id) {
                  changed = true;
                  return { ...m, username: nextUsername, avatar_key: nextAvatarKey };
                }
                return m;
              });
            }
            if (Array.isArray(state.communityReviews)) {
              state.communityReviews.forEach((r) => {
                if (r && r.user_id === rec.id) {
                  r.username = nextUsername;
                  r.avatar_key = nextAvatarKey;
                  changed = true;
                }
              });
            }
            if (changed) renderApp();
          } else if (evType === 'UPDATE' && rec && rec.id) {
            const existing = state.discussion.comments.find((m) => m.id === rec.id);
            if (!existing) return;
            const nextReplyTo = rec.reply_to ? normalizeFlatReplyTo(rec.reply_to) : existing.replyTo;
            state.discussion.comments = state.discussion.comments.map((m) =>
              m.id === rec.id
                ? {
                    ...m,
                    content: rec.content ?? m.content,
                    updated_at: rec.updated_at || m.updated_at,
                    replyTo: nextReplyTo
                  }
                : m
            );
            renderApp();
          } else if (evType === 'DELETE') {
            const delId = oldRec?.id || rec?.id;
            if (!delId) return;
            const beforeLen = state.discussion.comments.length;
            state.discussion.comments = state.discussion.comments.filter((m) => m.id !== delId);
            if (state.discussion.comments.length !== beforeLen) {
              state.discussion.totalComments = Math.max(0, state.discussion.totalComments - 1);
              renderApp();
            }
          }
        } catch (_) {}
      };
      ws.onclose = () => {
        if (this.heartbeatId) clearInterval(this.heartbeatId);
        this.heartbeatId = null;
        if (this.subscribedAnimeId === aid && Number(state.discussion.animeId) === aid && getActiveDiscussionAnime()) {
          this.reconnectId = setTimeout(() => this.ensureSubscribed(aid), 5000);
        }
      };
      ws.onerror = () => {};
    } catch (_) {}
  },

  disconnect() {
    if (this.heartbeatId) clearInterval(this.heartbeatId);
    if (this.reconnectId) clearTimeout(this.reconnectId);
    this.heartbeatId = null;
    this.reconnectId = null;
    this.subscribedAnimeId = null;
    if (this.ws) {
      try { this.ws.close(); } catch (_) {}
      this.ws = null;
    }
  }
};

function applyPendingDiscussionHighlight(animeId) {
  const jump = state.pendingNotificationJump;
  if (!jump || Number(jump.animeId) !== Number(animeId)) return;
  const targetMessageId = jump.replyId || jump.commentId || null;
  state.discussion.highlightTarget = {
    commentId: targetMessageId,
    threadId: null,
    replyId: null
  };
  state.pendingNotificationJump = null;
  renderApp();
  setTimeout(() => {
    const targetId = targetMessageId ? `discussion-comment-${targetMessageId}` : 'anime-discussion-section';
    const el = document.getElementById(targetId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, 120);
}

async function loadAnimeDiscussion(animeId, reset = true) {
  const aid = Number(animeId);
  if (!aid) return;
  const switchingAnime = Number(state.discussion.animeId) !== aid;
  if (reset) {
    state.discussion.animeId = aid;
    if (switchingAnime) {
      state.discussion.comments = [];
      state.discussion.totalComments = 0;
      state.discussion.hasMore = false;
      state.discussion.draft = '';
      state.discussion.commentTags = [];
      state.discussion.activeReplyTarget = null;
      state.discussion.replyDraft = '';
      state.discussion.replyTags = [];
      state.discussion.editingCommentId = null;
      state.discussion.contextMenu = null;
      state.discussion.success = '';
    }
    state.discussion.loading = true;
    state.discussion.error = '';
  } else {
    state.discussion.loadingMore = true;
  }
  const offset = reset ? 0 : state.discussion.comments.length;
  renderApp();
  try {
    const data = await supabaseRequest('/rest/v1/rpc/get_anime_discussion_page', 'POST', {
      p_anime_id: aid,
      p_limit: 25,
      p_offset: offset
    });
    if (Number(state.discussion.animeId) !== aid) return;
    const rawIncoming = Array.isArray(data?.comments) ? data.comments : [];
    const incomingFlatRaw = flattenDiscussionMessages(rawIncoming);
    DiscussionRealtimeManager.cacheProfilesFromMessages(incomingFlatRaw);
    const incomingFlat = await DiscussionRealtimeManager.hydrateProfilesForMessages(incomingFlatRaw);
    if (Number(state.discussion.animeId) !== aid) return;
    const merged = reset
      ? incomingFlat
      : flattenDiscussionMessages([
          ...state.discussion.comments,
          ...incomingFlat.filter((inc) => !state.discussion.comments.some((old) => old.id === inc.id))
        ]);
    state.discussion.comments = merged;
    state.discussion.totalComments = Number(data?.total_comments ?? merged.length);
    state.discussion.hasMore = Boolean(data?.has_more);
    state.discussion.loading = false;
    state.discussion.loadingMore = false;
    DiscussionRealtimeManager.ensureSubscribed(aid);
    if (state.pendingNotificationJump && Number(state.pendingNotificationJump.animeId) === aid) {
      applyPendingDiscussionHighlight(aid);
    } else {
      renderApp();
    }
    if (reset) {
      markAnimeDiscussionSeen(aid);
    }
  } catch (err) {
    if (Number(state.discussion.animeId) !== aid) return;
    state.discussion.loading = false;
    state.discussion.loadingMore = false;
    state.discussion.error = err?.message || 'Unable to load live chat right now.';
    renderApp();
  }
}

function updateMentionDropdownDom(scope, suggestions) {
  const box = document.getElementById(`mention-dropdown-${scope}`);
  if (!box) return;
  if (!suggestions || suggestions.length === 0) {
    box.classList.remove('open');
    box.style.display = 'none';
    box.innerHTML = '';
    return;
  }
  box.classList.add('open');
  box.style.display = 'flex';
  box.innerHTML = suggestions
    .map(
      (u) => `
      <button
        type="button"
        class="m-opt"
        data-pick-mention-id="${escapeHtml(u.id || u.user_id || '')}"
        data-pick-mention-name="${escapeHtml(u.username)}"
        data-pick-mention-avatar="${escapeHtml(u.avatar_key || 'avatar_1')}"
        data-pick-mention-scope="${escapeHtml(scope)}"
      >
        ${renderAvatarCircle(u.avatar_key || 'avatar_1', u.username, 28)}
        <span class="m-name">${escapeHtml(u.username)}</span>
        <span class="m-tag-hint">@Tag</span>
      </button>`
    )
    .join('');
  box.querySelectorAll('[data-pick-mention-name]').forEach((btn) => {
    btn.onmousedown = (e) => {
      e.preventDefault();
      applySelectedMention(btn.getAttribute('data-pick-mention-scope'), {
        id: btn.getAttribute('data-pick-mention-id'),
        username: btn.getAttribute('data-pick-mention-name'),
        avatar_key: btn.getAttribute('data-pick-mention-avatar') || 'avatar_1'
      });
    };
  });
}

function getTagsForScope(scope) {
  if (scope === 'comment') return state.discussion.commentTags;
  if (scope === 'reply') return state.discussion.replyTags;
  return [];
}

function applySelectedMention(scope, userObj) {
  const inputMap = {
    comment: 'inp-discussion-comment',
    reply: 'inp-discussion-reply',
    edit_comment: 'inp-discussion-edit-comment',
    edit_reply: 'inp-discussion-edit-reply'
  };
  const el = document.getElementById(inputMap[scope]);
  if (!el) return;
  const start = state.discussion.mentionActiveStart;
  const end = state.discussion.mentionActiveEnd;
  if (start < 0 || end < start) return;
  // Strip @query from text so username is NOT written into the message text
  const res = stripMentionTrigger(el.value, start, end);
  el.value = res.text;
  if (scope === 'comment') {
    state.discussion.draft = res.text;
    const check = validateContentMentions(res.text, state.discussion.commentTags.length);
    if (check.totalMentions < MAX_MENTIONS_PER_CONTENT && userObj?.id) {
      state.discussion.commentTags = [...state.discussion.commentTags, userObj];
    }
  } else if (scope === 'reply') {
    state.discussion.replyDraft = res.text;
    const check = validateContentMentions(res.text, state.discussion.replyTags.length);
    if (check.totalMentions < MAX_MENTIONS_PER_CONTENT && userObj?.id) {
      state.discussion.replyTags = [...state.discussion.replyTags, userObj];
    }
  } else if (scope === 'edit_comment') {
    state.discussion.editingCommentDraft = res.text;
  } else if (scope === 'edit_reply') {
    state.discussion.editingReplyDraft = res.text;
  }
  state.discussion.mentionSuggestions = [];
  state.discussion.mentionActiveScope = null;
  renderApp();
  setTimeout(() => {
    const nextEl = document.getElementById(inputMap[scope]);
    if (nextEl) {
      nextEl.focus();
      nextEl.setSelectionRange(res.cursor, res.cursor);
    }
  }, 20);
}

function updateMentionCounterDom(scope, text) {
  const counterEl = document.getElementById(`mention-counter-${scope}`);
  if (!counterEl) return;
  const tagCount = getTagsForScope(scope).length;
  const v = validateContentMentions(text, tagCount);
  counterEl.textContent = `${v.totalMentions}/${MAX_MENTIONS_PER_CONTENT} tagged`;
  counterEl.classList.toggle('over-limit', !v.ok);
}

function getLocalMentionMatches(query) {
  const q = String(query || '').trim().toLowerCase();
  const selfId = state.session?.userId || '';
  const map = new Map();
  for (const r of state.communityReviews || []) {
    if (r.user_id && r.user_id !== selfId && r.username) {
      map.set(r.user_id, { id: r.user_id, username: r.username, avatar_key: r.avatar_key || 'avatar_1' });
    }
  }
  for (const c of state.discussion.comments || []) {
    if (c.user_id && c.user_id !== selfId && c.username) {
      map.set(c.user_id, { id: c.user_id, username: c.username, avatar_key: c.avatar_key || 'avatar_1' });
    }
    for (const th of c.threads || []) {
      for (const r of th.replies || []) {
        if (r.user_id && r.user_id !== selfId && r.username) {
          map.set(r.user_id, { id: r.user_id, username: r.username, avatar_key: r.avatar_key || 'avatar_1' });
        }
      }
    }
  }
  return Array.from(map.values())
    .filter((u) => !q || u.username.toLowerCase().includes(q))
    .sort((a, b) => {
      const aStarts = q && a.username.toLowerCase().startsWith(q) ? 0 : 1;
      const bStarts = q && b.username.toLowerCase().startsWith(q) ? 0 : 1;
      if (aStarts !== bStarts) return aStarts - bStarts;
      return a.username.localeCompare(b.username);
    })
    .slice(0, 8);
}

function handleDiscussionInputMentionCheck(scope, inputEl) {
  const text = inputEl.value;
  if (scope === 'comment') state.discussion.draft = text;
  else if (scope === 'reply') state.discussion.replyDraft = text;
  else if (scope === 'edit_comment') state.discussion.editingCommentDraft = text;
  else if (scope === 'edit_reply') state.discussion.editingReplyDraft = text;
  updateMentionCounterDom(scope, text);

  if (!state.session) return;
  const active = detectActiveMentionQuery(text, inputEl.selectionStart ?? text.length);
  if (!active) {
    state.discussion.mentionSuggestions = [];
    state.discussion.mentionActiveScope = null;
    updateMentionDropdownDom(scope, []);
    return;
  }
  state.discussion.mentionActiveScope = scope;
  state.discussion.mentionActiveStart = active.start;
  state.discussion.mentionActiveEnd = active.end;

  // Immediate local suggestion feedback on @ + 1st letter
  const localMatches = getLocalMentionMatches(active.query);
  if (localMatches.length > 0) {
    state.discussion.mentionSuggestions = localMatches;
    updateMentionDropdownDom(scope, localMatches);
  }

  clearTimeout(mentionSearchTimer);
  const seq = ++mentionSearchSeq;
  mentionSearchTimer = setTimeout(async () => {
    try {
      const rows = await supabaseRequest('/rest/v1/rpc/search_mentionable_users', 'POST', {
        p_query: active.query,
        p_limit: 10
      });
      if (seq !== mentionSearchSeq) return;
      const remoteList = Array.isArray(rows) ? rows : [];
      const mergedMap = new Map();
      for (const u of [...remoteList, ...localMatches]) {
        const uid = u.id || u.user_id;
        if (uid && uid !== state.session?.userId && !mergedMap.has(uid)) {
          mergedMap.set(uid, { id: uid, username: u.username, avatar_key: u.avatar_key || 'avatar_1' });
        }
      }
      const list = Array.from(mergedMap.values()).slice(0, 10);
      state.discussion.mentionSuggestions = list;
      updateMentionDropdownDom(scope, list);
    } catch (_) {
      if (seq !== mentionSearchSeq) return;
      updateMentionDropdownDom(scope, localMatches);
    }
  }, 90);
}

function getActiveDiscussionAnime() {
  if (state.communityCommentsAnimeId !== null) {
    const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
    const grp = allGroups.find((g) => Number(g.anime_id) === Number(state.communityCommentsAnimeId));
    if (grp) {
      return {
        animeId: Number(grp.anime_id),
        title: grp.anime_title || 'Anime',
        titleEnglish: grp.anime_title || 'Anime',
        coverImage: grp.anime_cover_image || ''
      };
    }
    if (
      state.communityCommentsAnimeFallback &&
      Number(state.communityCommentsAnimeFallback.anime_id) === Number(state.communityCommentsAnimeId)
    ) {
      const fb = state.communityCommentsAnimeFallback;
      return {
        animeId: Number(fb.anime_id),
        title: fb.anime_title || `Anime #${fb.anime_id}`,
        titleEnglish: fb.anime_title || `Anime #${fb.anime_id}`,
        coverImage: fb.anime_cover_image || ''
      };
    }
  }
  if (state.detailAnime && state.detailAnime.animeId) return state.detailAnime;
  return null;
}

function openCommunityHubForAnime(animeId, options = {}) {
  const aid = Number(animeId);
  if (!aid) return;
  const {
    tab = 'discussion',
    fallbackTitle = '',
    fallbackCover = '',
    jumpTarget = null,
    notificationId = null,
    startEditingReview = false
  } = options;

  const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
  const grp = allGroups.find((g) => Number(g.anime_id) === aid);
  const vaultItem = state.vault.find((v) => Number(v.animeId) === aid);
  const myCommRev = state.session
    ? (grp?.reviews || []).find((r) => r.user_id === state.session.userId) ||
      state.communityReviews.find((r) => r.user_id === state.session.userId && Number(r.anime_id) === aid)
    : null;
  const unseenItem = (state.notifications.unseenAnime || []).find((u) => Number(u.anime_id) === aid);

  if (!grp) {
    state.communityCommentsAnimeFallback = {
      anime_id: aid,
      anime_title: fallbackTitle || vaultItem?.titleEnglish || vaultItem?.title || unseenItem?.anime_title || `Anime #${aid}`,
      anime_cover_image: fallbackCover || vaultItem?.coverImage || unseenItem?.anime_cover_image || '',
      is_adult: Boolean(vaultItem?.isAdult),
      reviews: [],
      total_count: 0,
      rated_count: 0,
      noted_count: 0,
      average_rating10: null,
      latest_updated_at: new Date().toISOString()
    };
  } else {
    state.communityCommentsAnimeFallback = null;
  }

  const existingRating10 = myCommRev?.rating ?? vaultItem?.rating10 ?? null;
  const existingNote = myCommRev?.review_notes || vaultItem?.reviewNotes || '';
  const existingStars = existingRating10 ? rating10ToStars(existingRating10) || 0 : 0;

  state.communityCommentsAnimeId = aid;
  state.communityCommentsTab = startEditingReview || tab === 'notes' ? 'notes' : 'discussion';
  state.pendingCommunityReplyUser = null;
  state.discussion.emojiTrayOpen = false;
  state.communityCommentDraftStars = existingStars;
  state.communityCommentDraftNote = existingNote;
  state.communityNoteSaving = false;
  state.communityNoteFeedback = '';
  state.communityNoteError = '';
  state.editingCommunityReviewAnimeId = startEditingReview && myCommRev ? aid : null;
  state.editingCommunityReviewStars = existingStars;
  state.editingCommunityReviewNote = existingNote;
  state.editingCommunityReviewSaving = false;

  if (jumpTarget) {
    state.pendingNotificationJump = {
      animeId: aid,
      commentId: jumpTarget.commentId || null,
      threadId: jumpTarget.threadId || null,
      replyId: jumpTarget.replyId || null
    };
  }

  renderApp();
  EmojiCacheManager.ensureMetadata(false);

  if (notificationId && state.session) {
    markNotificationRead(notificationId);
  }
  if (state.session && state.communityCommentsTab === 'discussion') {
    markAnimeDiscussionSeen(aid);
  }

  loadAnimeDiscussion(aid, true).then(() => {
    applyPendingCommunityReplyTarget();
    if (!state.pendingNotificationJump) {
      setTimeout(() => {
        const stream = document.getElementById('livtrak-chat-messages');
        if (stream) stream.scrollTop = stream.scrollHeight;
      }, 60);
    }
  });
}

function applyPendingCommunityReplyTarget() {
  const pending = state.pendingCommunityReplyUser;
  if (!pending) return;
  const targetAid = Number(state.communityCommentsAnimeId || state.discussion.animeId || 0);
  if (targetAid && Number(state.discussion.animeId) !== targetAid) return;
  if (state.discussion.loading) return;

  const existingComment = (state.discussion.comments || []).find(
    (c) =>
      (pending.id && c.user_id === pending.id) ||
      (pending.username && String(c.username || '').toLowerCase() === String(pending.username).toLowerCase())
  );
  state.pendingCommunityReplyUser = null;

  if (existingComment) {
    const existingBody = existingComment.content ?? existingComment.body ?? '';
    state.discussion.activeReplyTarget = {
      messageId: existingComment.id,
      senderName: existingComment.username || pending.username || 'Member',
      previewText: existingBody.slice(0, 180),
      stickerCode: extractFirstStickerCode(existingBody),
      targetUserId: existingComment.user_id,
      sourceType: existingComment.source_type || 'comment',
      rootCommentId: existingComment._rootCommentId || existingComment.id
    };
    state.discussion.error = '';
    renderApp();
    setTimeout(() => document.getElementById('inp-discussion-comment')?.focus(), 40);
  } else if (pending.id) {
    const currentTags = state.discussion.commentTags || [];
    const check = validateContentMentions(state.discussion.draft || '', currentTags.length);
    if (
      check.totalMentions < MAX_MENTIONS_PER_CONTENT &&
      !currentTags.some((t) => t.id === pending.id)
    ) {
      state.discussion.commentTags = [
        ...currentTags,
        { id: pending.id, username: pending.username || 'Member', avatar_key: pending.avatar_key || 'avatar_1' }
      ];
    }
    renderApp();
    setTimeout(() => document.getElementById('inp-discussion-comment')?.focus(), 40);
  }
}

async function postAnimeComment() {
  const activeAnime = getActiveDiscussionAnime();
  if (!state.session || !activeAnime || state.discussion.submitting) return;
  const body = (document.getElementById('inp-discussion-comment')?.value ?? state.discussion.draft ?? '').trim();
  const taggedIds = (state.discussion.commentTags || []).map((t) => t.id).filter(Boolean);
  const replyTarget = state.discussion.activeReplyTarget;
  if (!body) {
    state.discussion.error = 'Please write a message before sending.';
    state.discussion.success = '';
    renderApp();
    return;
  }
  const check = validateContentMentions(body, taggedIds.length);
  if (!check.ok) {
    state.discussion.error = check.error;
    state.discussion.success = '';
    renderApp();
    return;
  }
  state.discussion.submitting = true;
  state.discussion.error = '';
  state.discussion.success = '';
  renderApp();
  try {
    const a = activeAnime;
    const flatReplyTo = replyTarget
      ? {
          messageId: replyTarget.messageId,
          senderName: replyTarget.senderName,
          previewText: replyTarget.previewText || '',
          stickerCode: replyTarget.stickerCode || extractFirstStickerCode(replyTarget.previewText || '')
        }
      : null;
    let res = null;
    try {
      res = await supabaseRequest('/rest/v1/rpc/create_anime_comment', 'POST', {
        p_anime_id: Number(a.animeId),
        p_content: body,
        p_anime_title: a.titleEnglish || a.title || 'Anime',
        p_anime_cover: a.coverImage || '',
        p_tagged_user_ids: taggedIds,
        p_reply_to: flatReplyTo
      });
    } catch (rpcErr) {
      const msg = String(rpcErr?.message || '');
      if (flatReplyTo && /could not find the function|p_reply_to|PGRST202/i.test(msg)) {
        const legacyRes = await supabaseRequest('/rest/v1/rpc/create_thread_reply', 'POST', {
          p_comment_id: replyTarget.rootCommentId || replyTarget.messageId,
          p_target_user_id: replyTarget.targetUserId,
          p_content: body,
          p_tagged_user_ids: taggedIds,
          p_quoted_reply_id: replyTarget.sourceType === 'reply' ? replyTarget.messageId : null,
          p_quoted_username: replyTarget.senderName || null,
          p_quoted_preview: replyTarget.previewText || null
        });
        if (legacyRes?.reply) {
          const legFlat = {
            ...legacyRes.reply,
            anime_id: Number(a.animeId),
            source_type: 'reply',
            replyTo: flatReplyTo,
            threads: []
          };
          state.discussion.comments = flattenDiscussionMessages([legFlat, ...state.discussion.comments]);
          state.discussion.totalComments = Math.max(state.discussion.comments.length, state.discussion.totalComments + 1);
        }
        res = null;
      } else {
        throw rpcErr;
      }
    }
    const created = res?.comment || res;
    if (created && created.id) {
      const normalized = flattenDiscussionMessages([
        {
          ...created,
          reply_to: created.reply_to || flatReplyTo,
          threads: []
        }
      ])[0];
      if (normalized) {
        state.discussion.comments = [
          normalized,
          ...state.discussion.comments.filter((c) => c.id !== normalized.id)
        ];
      }
      state.discussion.totalComments = Number(
        res?.total_comments ?? Math.max(state.discussion.comments.length, state.discussion.totalComments + 1)
      );
    }
    state.discussion.draft = '';
    state.discussion.commentTags = [];
    state.discussion.activeReplyTarget = null;
    state.discussion.replyDraft = '';
    state.discussion.replyTags = [];
    state.discussion.mentionSuggestions = [];
    state.discussion.mentionActiveScope = null;
    state.discussion.emojiTrayOpen = false;
    state.discussion.success = '';
    EmojiCacheManager.recordUsage(extractEmojiIdsFromText(body));
  } catch (err) {
    state.discussion.error = err?.message || 'Failed to send message. Please try again.';
    showToast(state.discussion.error, true);
  } finally {
    state.discussion.submitting = false;
    renderApp();
    scrollChatFeedSmoothly(true);
  }
}

async function saveEditedAnimeComment(commentId) {
  if (!state.session || state.discussion.submitting) return;
  const body = (document.getElementById('inp-discussion-edit-comment')?.value ?? state.discussion.editingCommentDraft ?? '').trim();
  if (!body) {
    state.discussion.error = 'Message cannot be empty.';
    renderApp();
    return;
  }
  const check = validateContentMentions(body);
  if (!check.ok) {
    state.discussion.error = check.error;
    renderApp();
    return;
  }
  const targetMsg = state.discussion.comments.find((c) => c.id === commentId);
  const isLegacyReply = targetMsg?.source_type === 'reply';
  state.discussion.submitting = true;
  state.discussion.error = '';
  state.discussion.success = '';
  renderApp();
  try {
    const updated = isLegacyReply
      ? await supabaseRequest('/rest/v1/rpc/update_thread_reply', 'POST', {
          p_reply_id: commentId,
          p_content: body
        })
      : await supabaseRequest('/rest/v1/rpc/update_anime_comment', 'POST', {
          p_comment_id: commentId,
          p_content: body
        });
    state.discussion.comments = state.discussion.comments.map((c) =>
      c.id === commentId
        ? { ...c, content: updated?.content || body, updated_at: updated?.updated_at || new Date().toISOString() }
        : c
    );
    state.discussion.editingCommentId = null;
    state.discussion.editingCommentDraft = '';
    state.discussion.success = 'Message updated';
    showToast('Message updated');
  } catch (err) {
    state.discussion.error = err?.message || 'Failed to update message.';
  } finally {
    state.discussion.submitting = false;
    renderApp();
  }
}

async function deleteAnimeComment(commentId) {
  if (!state.session || state.discussion.submitting) return;
  const targetMsg = state.discussion.comments.find((c) => c.id === commentId);
  const isLegacyReply = targetMsg?.source_type === 'reply';
  state.discussion.submitting = true;
  state.discussion.error = '';
  state.discussion.success = '';
  renderApp();
  try {
    if (isLegacyReply) {
      await supabaseRequest('/rest/v1/rpc/delete_thread_reply', 'POST', { p_reply_id: commentId });
    } else {
      await supabaseRequest('/rest/v1/rpc/delete_anime_comment', 'POST', { p_comment_id: commentId });
    }
    state.discussion.comments = state.discussion.comments.filter((c) => c.id !== commentId);
    state.discussion.totalComments = Math.max(0, state.discussion.totalComments - 1);
    if (state.discussion.activeReplyTarget?.messageId === commentId) {
      state.discussion.activeReplyTarget = null;
    }
    state.discussion.success = 'Message deleted';
    showToast('Message deleted');
  } catch (err) {
    state.discussion.error = err?.message || 'Failed to delete message.';
  } finally {
    state.discussion.submitting = false;
    renderApp();
  }
}

async function postThreadReply() {
  return postAnimeComment();
}

// Deterministic, deduplicated unread count & Realtime synchronization tracker
const NotificationSyncState = {
  knownReadState: new Map(), // notificationId -> boolean (is_read)
  unreadIds: new Set(),
  processedEventKeys: new Set(),
  mutationGeneration: 0,
  unreadCount: 0,
  reset() {
    this.knownReadState.clear();
    this.unreadIds.clear();
    this.processedEventKeys.clear();
    this.mutationGeneration = 0;
    this.unreadCount = 0;
  },
  currentGeneration() {
    return this.mutationGeneration;
  },
  beginLocalMutation() {
    this.mutationGeneration += 1;
    return this.mutationGeneration;
  },
  _recordEventKey(key) {
    if (this.processedEventKeys.has(key)) return false;
    this.processedEventKeys.add(key);
    if (this.processedEventKeys.size > 500) {
      const oldest = this.processedEventKeys.values().next().value;
      if (oldest !== undefined) this.processedEventKeys.delete(oldest);
    }
    return true;
  },
  applyAuthoritativeUnreadIds(idIterable, fetchStartedGen) {
    if (fetchStartedGen < this.mutationGeneration) return false;
    const cleanIds = new Set();
    for (const raw of idIterable || []) {
      const id = String(raw || '').trim();
      if (id) cleanIds.add(id);
    }
    for (const prevId of this.unreadIds) {
      if (!cleanIds.has(prevId)) {
        this.knownReadState.set(prevId, true);
      }
    }
    this.unreadIds.clear();
    for (const id of cleanIds) {
      this.unreadIds.add(id);
      this.knownReadState.set(id, false);
    }
    this.unreadCount = this.unreadIds.size;
    return true;
  },
  applyFetchedPage(items, authoritativeUnreadCount, fetchStartedGen) {
    if (fetchStartedGen < this.mutationGeneration) return false;
    for (const item of items || []) {
      const id = String(item?.id || '').trim();
      if (!id) continue;
      const isRead = Boolean(item.is_read);
      this.knownReadState.set(id, isRead);
      if (isRead) {
        this.unreadIds.delete(id);
      } else {
        this.unreadIds.add(id);
      }
    }
    if (typeof authoritativeUnreadCount === 'number' && !Number.isNaN(authoritativeUnreadCount)) {
      this.unreadCount = Math.max(0, authoritativeUnreadCount);
      if (this.unreadCount === 0) {
        for (const id of this.unreadIds) this.knownReadState.set(id, true);
        this.unreadIds.clear();
      }
    }
    return true;
  },
  markLocalRead(notificationId) {
    const id = String(notificationId || '').trim();
    if (!id) return this.unreadCount;
    this.mutationGeneration += 1;
    const prev = this.knownReadState.get(id);
    const wasInSet = this.unreadIds.delete(id);
    this.knownReadState.set(id, true);
    if (wasInSet || prev === false) {
      this.unreadCount = Math.max(0, this.unreadCount - 1);
    }
    return this.unreadCount;
  },
  markLocalReadMany(notificationIds) {
    if (!notificationIds || notificationIds.length === 0) return this.unreadCount;
    this.mutationGeneration += 1;
    let decremented = 0;
    for (const raw of notificationIds) {
      const id = String(raw || '').trim();
      if (!id) continue;
      const prev = this.knownReadState.get(id);
      const wasInSet = this.unreadIds.delete(id);
      this.knownReadState.set(id, true);
      if (wasInSet || prev === false) decremented += 1;
    }
    if (decremented > 0) {
      this.unreadCount = Math.max(0, this.unreadCount - decremented);
    }
    return this.unreadCount;
  },
  markAllLocalRead() {
    this.mutationGeneration += 1;
    for (const key of this.knownReadState.keys()) {
      this.knownReadState.set(key, true);
    }
    this.unreadIds.clear();
    this.unreadCount = 0;
    return 0;
  },
  confirmServerUnreadCount(serverCount, mutationGen) {
    if (mutationGen < this.mutationGeneration) return false;
    const safe = Math.max(0, Number(serverCount || 0));
    if (Number.isNaN(safe)) return false;
    this.unreadCount = safe;
    if (safe === 0) {
      for (const id of this.unreadIds) this.knownReadState.set(id, true);
      this.unreadIds.clear();
    }
    return true;
  },
  onRealtimeInsert(record, currentUid) {
    const id = String(record?.id || '').trim();
    const recipientId = String(record?.recipient_user_id || '').trim();
    if (!id || !currentUid || recipientId !== currentUid) {
      return { unreadCount: this.unreadCount, delta: 0, needsReconcile: false };
    }
    const isRead = Boolean(record?.is_read);
    if (!this._recordEventKey(`INSERT:${id}:${isRead}`)) {
      return { unreadCount: this.unreadCount, delta: 0, needsReconcile: false };
    }
    if (this.knownReadState.has(id)) {
      return { unreadCount: this.unreadCount, delta: 0, needsReconcile: false };
    }
    this.knownReadState.set(id, isRead);
    if (!isRead) {
      this.unreadIds.add(id);
      this.unreadCount += 1;
      return { unreadCount: this.unreadCount, delta: 1, needsReconcile: false };
    }
    return { unreadCount: this.unreadCount, delta: 0, needsReconcile: false };
  },
  onRealtimeUpdate(oldRecord, record, currentUid) {
    const id = String(record?.id || '').trim();
    const recipientId = String(record?.recipient_user_id || '').trim();
    if (!id || !currentUid || recipientId !== currentUid) {
      return { unreadCount: this.unreadCount, delta: 0, needsReconcile: false };
    }
    const newIsRead = Boolean(record?.is_read);
    const oldIsRead = oldRecord && typeof oldRecord.is_read === 'boolean' ? oldRecord.is_read : null;
    const readAtKey = String(record?.read_at || '');
    if (!this._recordEventKey(`UPDATE:${id}:${newIsRead}:${readAtKey}`)) {
      return { unreadCount: this.unreadCount, delta: 0, needsReconcile: false };
    }
    const known = this.knownReadState.get(id);
    if (newIsRead) {
      if (known === true) {
        this.unreadIds.delete(id);
        return { unreadCount: this.unreadCount, delta: 0, needsReconcile: false };
      }
      const wasInSet = this.unreadIds.delete(id);
      this.knownReadState.set(id, true);
      if (oldIsRead === false || known === false || wasInSet) {
        this.unreadCount = Math.max(0, this.unreadCount - 1);
        return { unreadCount: this.unreadCount, delta: -1, needsReconcile: false };
      }
      return { unreadCount: this.unreadCount, delta: 0, needsReconcile: true };
    } else {
      if (known === false || this.unreadIds.has(id)) {
        return { unreadCount: this.unreadCount, delta: 0, needsReconcile: false };
      }
      this.knownReadState.set(id, false);
      this.unreadIds.add(id);
      if (oldIsRead === true || known === true) {
        this.unreadCount += 1;
        return { unreadCount: this.unreadCount, delta: 1, needsReconcile: false };
      }
      return { unreadCount: this.unreadCount, delta: 0, needsReconcile: true };
    }
  }
};

function isMissingReadAtColumnError(err) {
  const msg = String(err?.message || err || '').toLowerCase();
  return msg.includes('read_at') && (
    msg.includes('pgrst204') ||
    msg.includes('42703') ||
    msg.includes('column') ||
    msg.includes('schema cache')
  );
}

// RLS-scoped direct UPDATE on public.user_notifications setting is_read = true and read_at = now
async function patchUserNotificationsRead(queryFilter, readAtIso) {
  const path = `/rest/v1/user_notifications?${queryFilter}`;
  try {
    await supabaseRequest(path, 'PATCH', { is_read: true, read_at: readAtIso }, 'return=representation');
    return true;
  } catch (err) {
    if (isMissingReadAtColumnError(err)) {
      await supabaseRequest(path, 'PATCH', { is_read: true }, 'return=representation');
      return true;
    }
    throw err;
  }
}

// Authoritative unread query: recipient_user_id = authenticated user ID AND is_read = false
async function fetchAuthoritativeUnreadSnapshot() {
  const uid = state.session?.userId;
  if (!uid) {
    return { unreadIds: new Set(), unreadCount: 0, unreadCountByAnime: new Map(), rowsById: new Map() };
  }
  const [legacyRows, modernRows] = await Promise.all([
    supabaseRequest(
      `/rest/v1/user_notifications?select=id,anime_id,notification_type,preview_text&recipient_user_id=eq.${encodeURIComponent(uid)}&is_read=eq.false&order=created_at.desc&limit=1000`,
      'GET'
    ).catch(() => []),
    supabaseRequest(
      `/rest/v1/notifications?select=id,legacy_notification_id,title,message,type,metadata&user_id=eq.${encodeURIComponent(uid)}&is_read=eq.false&order=created_at.desc&limit=1000`,
      'GET'
    ).catch(() => [])
  ]);
  const unreadIds = new Set();
  const unreadCountByAnime = new Map();
  const rowsById = new Map();
  const seenLegacyIds = new Set();
  if (Array.isArray(legacyRows)) {
    legacyRows.forEach((row) => {
      const id = String(row?.id || '').trim();
      if (!id) return;
      unreadIds.add(id);
      seenLegacyIds.add(id);
      rowsById.set(id, {
        id,
        type: row?.notification_type || 'message',
        title: row?.notification_type === 'reply'
          ? 'Anime Universe • New Reply'
          : row?.notification_type === 'mention'
          ? 'Anime Universe • Tagged You'
          : 'Anime Universe • Community Alert',
        body: row?.preview_text || '',
        animeId: row?.anime_id ? Number(row.anime_id) : null
      });
      const aid = Number(row?.anime_id || 0);
      if (aid > 0) {
        unreadCountByAnime.set(aid, (unreadCountByAnime.get(aid) || 0) + 1);
      }
    });
  }
  if (Array.isArray(modernRows)) {
    modernRows.forEach((row) => {
      const legacyId = row?.legacy_notification_id != null ? String(row.legacy_notification_id).trim() : '';
      if (legacyId && seenLegacyIds.has(legacyId)) return;
      const id = String(row?.id || '').trim();
      if (!id) return;
      unreadIds.add(id);
      rowsById.set(id, {
        id,
        type: row?.type || 'message',
        title: row?.title || 'Anime Universe • Notification',
        body: row?.message || '',
        animeId: row?.metadata?.anime_id ? Number(row.metadata.anime_id) : null,
        senderName: row?.metadata?.sender_name || '',
        senderAvatar: row?.metadata?.sender_avatar || 'avatar_1'
      });
      const aid = Number(row?.metadata?.anime_id || 0);
      if (aid > 0) {
        unreadCountByAnime.set(aid, (unreadCountByAnime.get(aid) || 0) + 1);
      }
    });
  }
  return { unreadIds, unreadCount: unreadIds.size, unreadCountByAnime, rowsById };
}

async function refreshUnreadNotificationCount() {
  const uid = state.session?.userId;
  if (!uid) {
    NotificationSyncState.reset();
    state.notifications.unreadCount = 0;
    state.notifications.unseenAnime = [];
    return;
  }
  const startGen = NotificationSyncState.currentGeneration();
  try {
    const [snap, summary] = await Promise.all([
      fetchAuthoritativeUnreadSnapshot(),
      supabaseRequest('/rest/v1/rpc/get_unseen_discussion_anime', 'POST', {}).catch(() => null)
    ]);
    if (state.session?.userId !== uid) return;
    const prevKnownIds = typeof NativeNotificationBridge !== 'undefined' ? NativeNotificationBridge.knownUnreadIds : null;
    const isInitialSync = typeof NativeNotificationBridge !== 'undefined' ? !NativeNotificationBridge.hasPrimedUnread : true;
    if (!NotificationSyncState.applyAuthoritativeUnreadIds(snap.unreadIds, startGen)) {
      return;
    }
    state.notifications.unreadCount = NotificationSyncState.unreadCount;
    if (typeof NativeNotificationBridge !== 'undefined') {
      const newlyDetectedIds = [];
      snap.unreadIds.forEach((nid) => {
        if (!isInitialSync && prevKnownIds && !prevKnownIds.has(nid)) {
          newlyDetectedIds.push(nid);
        }
        NativeNotificationBridge.knownUnreadIds.add(nid);
      });
      NativeNotificationBridge.hasPrimedUnread = true;
      if (isInitialSync && snap.unreadIds.size > 0) {
        NativeNotificationBridge.markSeenInNative(Array.from(snap.unreadIds));
      }
      if (newlyDetectedIds.length > 0) {
        // Deliver ALL newly detected unread notifications (up to 5 per batch), not just the first one
        const batch = newlyDetectedIds.slice(0, 5).reverse();
        for (const nid of batch) {
          const rowMeta = snap.rowsById ? snap.rowsById.get(nid) : null;
          NativeNotificationBridge.alertNewlyDetectedUnread(nid, summary, rowMeta);
        }
      }
    }
    if (state.notifications.items.length > 0) {
      state.notifications.items = state.notifications.items.map((n) => {
        const shouldBeUnread = snap.unreadIds.has(String(n.id));
        return Boolean(n.is_read) === shouldBeUnread ? { ...n, is_read: !shouldBeUnread } : n;
      });
    }
    if (state.notifications.unreadCount === 0) {
      state.notifications.unseenAnime = [];
    } else {
      const rawList = Array.isArray(summary?.anime) ? summary.anime : [];
      state.notifications.unseenAnime = rawList
        .filter((a) => (snap.unreadCountByAnime.get(Number(a.anime_id)) || 0) > 0)
        .map((a) => ({
          ...a,
          unseen_count: snap.unreadCountByAnime.get(Number(a.anime_id)) || Number(a.unseen_count || 1)
        }));
    }
  } catch (_) {
    try {
      const summary = await supabaseRequest('/rest/v1/rpc/get_unseen_discussion_anime', 'POST', {});
      if (state.session?.userId !== uid) return;
      const list = Array.isArray(summary?.anime) ? summary.anime : [];
      const count = Math.max(0, Number(summary?.total_unseen ?? list.reduce((acc, a) => acc + Number(a.unseen_count || 1), 0)));
      if (NotificationSyncState.confirmServerUnreadCount(count, startGen)) {
        state.notifications.unreadCount = NotificationSyncState.unreadCount;
        state.notifications.unseenAnime = NotificationSyncState.unreadCount === 0 ? [] : list;
      }
    } catch (_2) {
      try {
        const count = await supabaseRequest('/rest/v1/rpc/get_unread_notification_count', 'POST', {});
        const numeric = Number(count || 0);
        if (!Number.isNaN(numeric) && NotificationSyncState.confirmServerUnreadCount(numeric, startGen)) {
          state.notifications.unreadCount = NotificationSyncState.unreadCount;
          if (state.notifications.unreadCount === 0) state.notifications.unseenAnime = [];
        }
      } catch (_3) {}
    }
  }
}

async function markAnimeDiscussionSeen(animeId) {
  const aid = Number(animeId);
  const uid = state.session?.userId;
  if (!uid || !aid) return;
  const readAtIso = new Date().toISOString();
  const unreadIdsForAnime = (state.notifications.items || [])
    .filter((n) => Number(n.anime_id) === aid && !n.is_read)
    .map((n) => n.id);
  const hadUnseen = (state.notifications.unseenAnime || []).some((u) => Number(u.anime_id) === aid);
  if (!hadUnseen && unreadIdsForAnime.length === 0 && state.notifications.unreadCount <= 0) return;

  const mutGen = unreadIdsForAnime.length > 0
    ? (NotificationSyncState.markLocalReadMany(unreadIdsForAnime), NotificationSyncState.currentGeneration())
    : NotificationSyncState.beginLocalMutation();

  if (unreadIdsForAnime.length > 0) {
    state.notifications.items = state.notifications.items.map((n) =>
      Number(n.anime_id) === aid && !n.is_read ? { ...n, is_read: true, read_at: readAtIso } : n
    );
  }
  state.notifications.unseenAnime = (state.notifications.unseenAnime || []).filter((u) => Number(u.anime_id) !== aid);
  state.notifications.unreadCount = unreadIdsForAnime.length > 0
    ? NotificationSyncState.unreadCount
    : state.notifications.unseenAnime.reduce((acc, u) => acc + Math.max(1, Number(u.unseen_count || 1)), 0);
  updateCommunityRedDotsInPlace();
  renderApp();

  try {
    let persisted = false;
    try {
      await patchUserNotificationsRead(
        `recipient_user_id=eq.${encodeURIComponent(uid)}&anime_id=eq.${aid}&is_read=eq.false`,
        readAtIso
      );
      persisted = true;
    } catch (_) {}
    if (!persisted) {
      await supabaseRequest('/rest/v1/rpc/mark_anime_notifications_read', 'POST', { p_anime_id: aid });
    }
    const snap = await fetchAuthoritativeUnreadSnapshot();
    if (state.session?.userId === uid && NotificationSyncState.applyAuthoritativeUnreadIds(snap.unreadIds, mutGen)) {
      state.notifications.unreadCount = NotificationSyncState.unreadCount;
      if (state.notifications.unreadCount === 0) {
        state.notifications.unseenAnime = [];
      }
      updateCommunityRedDotsInPlace();
    }
  } catch (_) {
    await refreshUnreadNotificationCount();
    updateCommunityRedDotsInPlace();
  }
}

function normalizeModernNotificationRow(row) {
  const meta = row?.metadata && typeof row.metadata === 'object' ? row.metadata : {};
  return {
    id: String(row.id),
    modern_id: String(row.id),
    legacy_notification_id: row.legacy_notification_id != null ? String(row.legacy_notification_id) : null,
    title: row.title || 'Anime Universe',
    message: row.message || '',
    preview_text: row.message || '',
    type: row.type || 'message',
    notification_type: row.type || 'message',
    is_read: Boolean(row.is_read),
    read_at: row.read_at || null,
    created_at: row.created_at || new Date().toISOString(),
    sender_user_id: row.sender_user_id || meta.sender_user_id || null,
    sender_username: meta.sender_name || meta.sender_username || 'Anime Universe',
    sender_avatar_key: meta.sender_avatar || meta.sender_avatar_key || 'avatar_1',
    anime_id: Number(meta.anime_id || 0) || null,
    anime_title: meta.anime_title || '',
    anime_cover_image: meta.anime_cover_image || '',
    comment_id: meta.comment_id || meta.message_id || null,
    thread_id: meta.thread_id || meta.parent_id || null,
    reply_id: meta.reply_id || null,
    metadata: meta
  };
}

async function loadUserNotifications(reset = true) {
  const uid = state.session?.userId;
  if (!uid) {
    NotificationSyncState.reset();
    state.notifications.items = [];
    state.notifications.unreadCount = 0;
    state.notifications.unseenAnime = [];
    state.notifications.loading = false;
    renderApp();
    return;
  }
  const offset = reset ? 0 : state.notifications.items.length;
  const startGen = NotificationSyncState.currentGeneration();
  if (reset) {
    state.notifications.loading = true;
    state.notifications.error = '';
  } else {
    state.notifications.loadingMore = true;
  }
  renderApp();
  try {
    const [data, modernRows, snap] = await Promise.all([
      supabaseRequest('/rest/v1/rpc/get_user_notifications', 'POST', {
        p_limit: 25,
        p_offset: offset
      }).catch(() => ({ notifications: [] })),
      supabaseRequest(
        `/rest/v1/notifications?select=*&user_id=eq.${encodeURIComponent(uid)}&order=created_at.desc&limit=25&offset=${offset}`,
        'GET'
      ).catch(() => []),
      fetchAuthoritativeUnreadSnapshot().catch(() => null)
    ]);
    if (state.session?.userId !== uid) return;
    const legacyIncoming = Array.isArray(data?.notifications) ? data.notifications : [];
    const legacyIds = new Set(legacyIncoming.map((n) => String(n.id)));

    const modernIncoming = Array.isArray(modernRows)
      ? modernRows
          .filter((r) => {
            const legId = r?.legacy_notification_id != null ? String(r.legacy_notification_id) : '';
            return !legId || !legacyIds.has(legId);
          })
          .map(normalizeModernNotificationRow)
      : [];

    const mergedIncoming = [...legacyIncoming, ...modernIncoming].sort(
      (a, b) => new Date(b.created_at || 0).getTime() - new Date(a.created_at || 0).getTime()
    );

    state.notifications.items = reset
      ? mergedIncoming
      : [
          ...state.notifications.items,
          ...mergedIncoming.filter((n) => !state.notifications.items.some((old) => String(old.id) === String(n.id)))
        ];
    if (snap) {
      if (NotificationSyncState.applyAuthoritativeUnreadIds(snap.unreadIds, startGen)) {
        state.notifications.unreadCount = NotificationSyncState.unreadCount;
        if (state.notifications.unreadCount === 0) state.notifications.unseenAnime = [];
      }
    } else if (NotificationSyncState.applyFetchedPage(mergedIncoming, Number(data?.unread_count ?? 0), startGen)) {
      state.notifications.unreadCount = NotificationSyncState.unreadCount;
      if (state.notifications.unreadCount === 0) state.notifications.unseenAnime = [];
    }
    state.notifications.totalCount = Math.max(Number(data?.total_count ?? 0), state.notifications.items.length);
    state.notifications.hasMore = Boolean(data?.has_more) || modernIncoming.length >= 25;
    state.notifications.loading = false;
    state.notifications.loadingMore = false;
    updateCommunityRedDotsInPlace();
    renderApp();
  } catch (err) {
    state.notifications.loading = false;
    state.notifications.loadingMore = false;
    state.notifications.error = err?.message || 'Unable to load notifications.';
    renderApp();
  }
}

async function markNotificationRead(notificationId) {
  const uid = state.session?.userId;
  if (!uid || !notificationId) return;
  const readAtIso = new Date().toISOString();
  let targetAnimeId = 0;
  let targetItem = null;
  state.notifications.items = state.notifications.items.map((n) => {
    if (String(n.id) === String(notificationId)) {
      targetItem = n;
      targetAnimeId = Number(n.anime_id || 0);
      if (!n.is_read) {
        return { ...n, is_read: true, read_at: readAtIso };
      }
    }
    return n;
  });

  const nextUnread = NotificationSyncState.markLocalRead(notificationId);
  const mutGen = NotificationSyncState.currentGeneration();
  state.notifications.unreadCount = nextUnread;
  if (nextUnread === 0) {
    state.notifications.unseenAnime = [];
  } else if (targetAnimeId > 0) {
    const stillUnreadForAnime = state.notifications.items.some(
      (n) => Number(n.anime_id) === targetAnimeId && !n.is_read
    );
    if (!stillUnreadForAnime) {
      state.notifications.unseenAnime = (state.notifications.unseenAnime || []).filter(
        (u) => Number(u.anime_id) !== targetAnimeId
      );
    }
  }
  updateCommunityRedDotsInPlace();
  renderApp();

  try {
    await Promise.allSettled([
      supabaseRequest(
        `/rest/v1/notifications?id=eq.${encodeURIComponent(notificationId)}&user_id=eq.${encodeURIComponent(uid)}`,
        'PATCH',
        { is_read: true, read_at: readAtIso }
      ),
      targetItem?.legacy_notification_id
        ? patchUserNotificationsRead(
            `id=eq.${encodeURIComponent(targetItem.legacy_notification_id)}&recipient_user_id=eq.${encodeURIComponent(uid)}&is_read=eq.false`,
            readAtIso
          )
        : patchUserNotificationsRead(
            `id=eq.${encodeURIComponent(notificationId)}&recipient_user_id=eq.${encodeURIComponent(uid)}&is_read=eq.false`,
            readAtIso
          ),
      supabaseRequest('/rest/v1/rpc/mark_notification_read', 'POST', {
        p_notification_id: notificationId
      })
    ]);
    const snap = await fetchAuthoritativeUnreadSnapshot();
    if (state.session?.userId === uid && NotificationSyncState.applyAuthoritativeUnreadIds(snap.unreadIds, mutGen)) {
      state.notifications.unreadCount = NotificationSyncState.unreadCount;
      if (state.notifications.unreadCount === 0) {
        state.notifications.unseenAnime = [];
      }
      updateCommunityRedDotsInPlace();
    }
  } catch (_) {
    await refreshUnreadNotificationCount();
    updateCommunityRedDotsInPlace();
  }
}

async function markAllNotificationsRead() {
  const uid = state.session?.userId;
  if (!uid) return;
  const readAtIso = new Date().toISOString();
  NotificationSyncState.markAllLocalRead();
  const mutGen = NotificationSyncState.currentGeneration();
  state.notifications.items = state.notifications.items.map((n) =>
    n.is_read ? n : { ...n, is_read: true, read_at: readAtIso }
  );
  state.notifications.unseenAnime = [];
  state.notifications.unreadCount = 0;
  updateCommunityRedDotsInPlace();
  renderApp();
  try {
    await Promise.allSettled([
      supabaseRequest(
        `/rest/v1/notifications?user_id=eq.${encodeURIComponent(uid)}&is_read=eq.false`,
        'PATCH',
        { is_read: true, read_at: readAtIso }
      ),
      patchUserNotificationsRead(
        `recipient_user_id=eq.${encodeURIComponent(uid)}&is_read=eq.false`,
        readAtIso
      ),
      supabaseRequest('/rest/v1/rpc/mark_all_notifications_read', 'POST', {})
    ]);
    const snap = await fetchAuthoritativeUnreadSnapshot();
    if (state.session?.userId === uid && NotificationSyncState.applyAuthoritativeUnreadIds(snap.unreadIds, mutGen)) {
      state.notifications.unreadCount = NotificationSyncState.unreadCount;
      if (state.notifications.unreadCount === 0) state.notifications.unseenAnime = [];
      updateCommunityRedDotsInPlace();
    }
    showToast('All notifications marked as read');
  } catch (_) {
    await refreshUnreadNotificationCount();
    updateCommunityRedDotsInPlace();
  }
}

async function deleteUserNotification(notificationId) {
  const uid = state.session?.userId;
  if (!uid || !notificationId) return;
  const target = state.notifications.items.find((n) => String(n.id) === String(notificationId));
  state.notifications.items = state.notifications.items.filter((n) => String(n.id) !== String(notificationId));
  if (target && !target.is_read) {
    const nextUnread = NotificationSyncState.markLocalRead(notificationId);
    state.notifications.unreadCount = nextUnread;
  }
  state.notifications.totalCount = Math.max(0, (state.notifications.totalCount || 1) - 1);
  updateCommunityRedDotsInPlace();
  renderApp();

  try {
    await Promise.allSettled([
      supabaseRequest(
        `/rest/v1/notifications?id=eq.${encodeURIComponent(notificationId)}&user_id=eq.${encodeURIComponent(uid)}`,
        'DELETE'
      ),
      target?.legacy_notification_id
        ? supabaseRequest(
            `/rest/v1/user_notifications?id=eq.${encodeURIComponent(target.legacy_notification_id)}&recipient_user_id=eq.${encodeURIComponent(uid)}`,
            'DELETE'
          )
        : supabaseRequest(
            `/rest/v1/user_notifications?id=eq.${encodeURIComponent(notificationId)}&recipient_user_id=eq.${encodeURIComponent(uid)}`,
            'DELETE'
          )
    ]);
    showToast('Notification deleted');
  } catch (_) {
    await refreshUnreadNotificationCount();
  }
}

// Part 2 & Part 5: Atomic Episode Progress Update with strict auto-completion & rollback on cloud failure
async function commitEpisodeProgress(animeId, targetEpisode) {
  const aid = Number(animeId);
  const existing = state.vault.find((v) => Number(v.animeId) === aid);
  if (!existing) return { ok: false, error: 'Anime is not in your library yet.' };

  const prevSnapshot = { ...existing };
  const prevHomeHad = state.homeTrackingIds.has(aid);
  const totalEp = existing.totalEpisodes ? Number(existing.totalEpisodes) : null;
  const clamped = totalEp && totalEp > 0 ? Math.min(totalEp, Math.max(0, Number(targetEpisode))) : Math.max(0, Number(targetEpisode));
  const autoCompleted = shouldAutoComplete(existing.mediaStatus, totalEp, clamped);
  const nextStatus = autoCompleted ? 'completed' : existing.status;
  const nextInHome = autoCompleted ? false : prevHomeHad && !shouldRemoveFromHome(nextStatus, existing.mediaStatus, totalEp, clamped);

  if (nextInHome) state.homeTrackingIds.add(aid);
  else state.homeTrackingIds.delete(aid);
  saveHomeTrackingIds();

  const updated = {
    ...existing,
    watchedEpisodes: clamped,
    status: nextStatus,
    inHome: nextInHome
  };
  state.vault = state.vault.map((v) => (Number(v.animeId) === aid ? updated : v));
  if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
    state.detailAnime = { ...state.detailAnime, ...updated };
  }
  saveLocalVault();

  if (!state.session) {
    if (autoCompleted) showToast('Completed! Moved from Home to Library.');
    return { ok: true, autoCompleted };
  }

  try {
    let rpcSucceeded = false;
    try {
      await supabaseRequest('/rest/v1/rpc/update_episode_progress', 'POST', {
        p_anime_id: aid,
        p_watched_episodes: clamped
      });
      rpcSucceeded = true;
    } catch (rpcErr) {
      const msg = String(rpcErr?.message || '');
      // Fall back to direct table update ONLY if RPC does not exist yet (pre-migration)
      if (msg.includes('404') || msg.includes('PGRST202') || msg.toLowerCase().includes('could not find the function')) {
        await supabaseRequest(
          '/rest/v1/user_anime?on_conflict=user_id,anime_id',
          'POST',
          {
            user_id: state.session.userId,
            anime_id: aid,
            status: nextStatus,
            episode_progress: clamped,
            total_episodes: totalEp || null,
            personal_rating: clampRating10(updated.rating10),
            review_notes: String(updated.reviewNotes || '').trim().slice(0, 320),
            is_favorite: Boolean(updated.isFavorite)
          },
          'resolution=merge-duplicates,return=minimal'
        );
        if (autoCompleted) {
          await syncRemoveFromHomeCloud(aid);
        }
        rpcSucceeded = true;
      } else {
        throw rpcErr;
      }
    }
    if (rpcSucceeded && autoCompleted) {
      showToast('Completed! Moved from Home to Library.');
    }
    return { ok: true, autoCompleted };
  } catch (err) {
    // Rollback to previous episode progress, status & Home membership on failure
    if (prevHomeHad) state.homeTrackingIds.add(aid);
    else state.homeTrackingIds.delete(aid);
    saveHomeTrackingIds();
    state.vault = state.vault.map((v) => (Number(v.animeId) === aid ? prevSnapshot : v));
    if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
      state.detailAnime = { ...state.detailAnime, ...prevSnapshot };
    }
    saveLocalVault();
    return { ok: false, error: err?.message || 'Failed to sync episode progress. Restored previous value.' };
  }
}

async function removeFromVault(animeId) {
  const aid = Number(animeId);
  state.vault = state.vault.filter((v) => Number(v.animeId) !== aid);
  state.homeTrackingIds.delete(aid);
  saveHomeTrackingIds();
  if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
    state.detailAnime.inWatchlist = false;
    state.detailAnime.inHome = false;
  }
  saveLocalVault();
  renderApp();
  if (!state.session) return;
  try {
    const uid = encodeURIComponent(state.session.userId);
    await Promise.all([
      supabaseRequest(`/rest/v1/user_anime?user_id=eq.${uid}&anime_id=eq.${aid}`, 'DELETE').catch(() => {}),
      syncRemoveFromHomeCloud(aid)
    ]);
  } catch (_) {}
}

// ============================================================================
// SHARED WIDGETS: OBSIDIAN TOP BAR & POSTER TRACKER CARD
// ============================================================================

function renderObsidianTopBar({ title, accentTitleTail = null, subtitle, leftActionHtml = '', actionsHtml = '' }) {
  const av = getAvatarPreset(state.profile.avatarKey);
  const frame = getAvatarFramePreset(state.profile.avatarKey);
  const initial = String(state.profile.username || 'A').trim().charAt(0).toUpperCase() || 'A';
  return `
    <header class="obsidian-topbar">
      <div class="topbar-left">
        ${leftActionHtml}
        <div style="min-width:0;">
          ${
            accentTitleTail
              ? `<div class="topbar-title-brand">${escapeHtml(title)}<em>${escapeHtml(accentTitleTail)}</em></div>`
              : `<div class="topbar-title-main">${escapeHtml(title)}</div>`
          }
          <div class="topbar-subtitle">${escapeHtml(subtitle)}</div>
        </div>
      </div>
      <div class="topbar-right">
        ${actionsHtml}
        <button class="profile-avatar-btn${frame ? ' has-frame' : ''}" data-nav="profile" title="Open Profile">
          <span class="profile-avatar-inner" style="background:linear-gradient(135deg,${av.c1},${av.c2});">
            <span style="position:absolute;color:#FFF;font-size:15px;font-weight:700;pointer-events:none;">${escapeHtml(initial)}</span>
            <img src="${av.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" style="position:relative;z-index:1;width:100%;height:100%;object-fit:cover;border-radius:50%;display:block;" />
          </span>
          ${
            frame
              ? `<img class="avatar-frame-overlay" src="${frame.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" style="width:58px;height:58px;" />`
              : ''
          }
        </button>
      </div>
    </header>
  `;
}

function renderInlineEpisodeStepper(effective, scope = 'card') {
  const aid = Number(effective.animeId);
  const total = effective.totalEpisodes && Number(effective.totalEpisodes) > 0 ? Number(effective.totalEpisodes) : null;
  const watched = Number(effective.watchedEpisodes || 0);
  const canDec = watched > 0;
  const canInc = !total || watched < total;
  const isDetail = scope === 'detail';
  const isEditing = Boolean(state.inlineEpEdit && Number(state.inlineEpEdit.animeId) === aid && state.inlineEpEdit.scope === scope);
  const editObj = isEditing ? state.inlineEpEdit : null;

  if (isEditing && editObj) {
    return `
      <div class="inline-ep-wrap" onclick="event.stopPropagation()">
        <div class="capsule-stepper editing ${isDetail ? 'detail-stepper' : ''}">
          <div class="inline-ep-input-group">
            <input
              id="inline-ep-input-${scope}-${aid}"
              class="inline-ep-input"
              type="text"
              inputmode="numeric"
              pattern="[0-9]*"
              maxlength="6"
              value="${escapeHtml(editObj.draft)}"
              data-inline-ep-input="${aid}"
              data-inline-ep-scope="${scope}"
              aria-label="Watched episodes"
              ${editObj.saving ? 'disabled' : ''}
            />
            <span class="inline-ep-total">/ ${total || '∞'}</span>
          </div>
          <button
            type="button"
            class="inline-ep-action-btn save"
            data-inline-ep-save="${aid}"
            data-inline-ep-scope="${scope}"
            title="Save episode (Enter)"
            ${editObj.saving ? 'disabled' : ''}
          >
            ✓
          </button>
          <button
            type="button"
            class="inline-ep-action-btn cancel"
            data-inline-ep-cancel="${aid}"
            title="Cancel (Esc)"
            ${editObj.saving ? 'disabled' : ''}
          >✕</button>
        </div>
        ${editObj.error ? `<div class="inline-ep-error">${escapeHtml(editObj.error)}</div>` : ''}
      </div>
    `;
  }

  return `
    <div class="inline-ep-wrap" onclick="event.stopPropagation()">
      <div class="capsule-stepper ${isDetail ? 'detail-stepper' : ''}">
        <button
          type="button"
          class="stepper-circle-btn"
          style="${isDetail ? 'width:36px;height:36px;' : ''}"
          data-dec-id="${aid}"
          ${!canDec ? 'disabled' : ''}
        >−</button>
        <div
          class="stepper-center-count editable-ep-trigger"
          style="${isDetail ? 'font-size:18px;' : ''}"
          data-inline-ep-trigger="${aid}"
          data-inline-ep-scope="${scope}"
          title="Click to type episode number directly"
        >
          ${isDetail ? `EP ${watched}` : `${watched}`}<span> / ${total || '∞'}</span>
        </div>
        <button
          type="button"
          class="stepper-circle-btn plus"
          style="${isDetail ? 'width:36px;height:36px;' : ''}"
          data-inc-id="${aid}"
          ${!canInc ? 'disabled' : ''}
        >+</button>
      </div>
    </div>
  `;
}

function renderPosterTrackerCard(item, showTrackingStepper = true) {
  const inVault = state.vault.find((v) => Number(v.animeId) === Number(item.animeId));
  const effective = inVault ? { ...item, ...inVault } : item;
  const isInUserList = Boolean(inVault);
  const title = resolveTitle(effective);
  const total = effective.totalEpisodes;
  const watched = effective.watchedEpisodes || 0;
  const sentiment = sentimentFromRating10(effective.rating10);
  const starsVal = rating10ToStars(effective.rating10);
  const userStars = starsVal ? starsVal.toFixed(1) : null;
  const catalogStars = effective.averageScore ? (effective.averageScore / 20).toFixed(1) : null;
  const displayStars = userStars || catalogStars;
  const isDonghua = String(effective.countryOfOrigin || '').toUpperCase() === 'CN';
  const rawFmt = String(effective.format || 'TV').toUpperCase();
  const isMovie = rawFmt === 'MOVIE';
  const formatLabel = isMovie
    ? (isDonghua ? 'DONGHUA MOVIE' : 'MOVIE')
    : rawFmt === 'OVA' || rawFmt === 'SPECIAL'
    ? rawFmt
    : isDonghua
    ? 'DONGHUA'
    : rawFmt.replace(/_/g, ' ');
  const epBadge = isMovie
    ? (isInUserList && watched >= (total || 1) ? 'MOVIE • ✓' : 'MOVIE')
    : isInUserList
    ? total && total > 0
      ? `EP ${watched}/${total}`
      : `EP ${Math.max(watched, effective.nextEpisodeNumber ? Math.max(0, effective.nextEpisodeNumber - 1) : 0)}`
    : formatLabel;
  const airClock = formatAiringClock(effective.airingTimeUnix) || (effective.seasonYear ? String(effective.seasonYear) : '');

  const isCompleted = isInUserList && (effective.status === 'completed' || shouldAutoComplete(effective.mediaStatus, total, watched));
  const progressPct = total && total > 0 ? Math.min(100, Math.round((watched / total) * 100)) : watched > 0 ? 50 : 4;
  const hasNote = Boolean(effective.reviewNotes && effective.reviewNotes.trim());

  let subStatusHtml = '';
  if (isCompleted) {
    subStatusHtml = `<div class="poster-sub-pill completed">✓ COMPLETED</div>`;
  } else if (effective.airingTimeUnix && effective.airingTimeUnix > Math.floor(Date.now() / 1000) && effective.nextEpisodeNumber) {
    subStatusHtml = `
      <div class="poster-sub-pill airing">
        <span>EP ${effective.nextEpisodeNumber}</span>
        <span class="tabular-num" style="color:var(--highlight);">${formatCompactCountdown(effective.airingTimeUnix)}</span>
      </div>
    `;
  } else if (isInUserList) {
    const stObj = WATCH_STATUSES.find((s) => s.dbValue === effective.status) || WATCH_STATUSES[0];
    subStatusHtml = `<div class="micro-label" style="text-align:center;color:var(--text-muted);font-size:8px;">${escapeHtml(stObj.label)}</div>`;
  }

  return `
    <div class="poster-tracker-card" data-card-anime-id="${effective.animeId}">
      <div class="poster-frame" data-open-detail="${effective.animeId}">
        <img src="${escapeHtml(effective.coverImage)}" alt="${escapeHtml(title)}" loading="lazy" />
        <div class="poster-scrim"></div>
        ${sentiment ? `<div class="poster-top-left"><span class="sentiment-pill solid ${sentiment.key}">${sentiment.shortLabel}</span></div>` : ''}
        ${displayStars ? `<div class="poster-top-right"><span class="rating-badge"><span style="color:var(--accent-primary);">★</span> ${displayStars}</span></div>` : ''}
        ${airClock ? `<div class="poster-bottom-left">${escapeHtml(airClock)}</div>` : ''}
        <div class="poster-bottom-right">${escapeHtml(epBadge)}</div>
      </div>

      <div class="poster-info-col">
        <div class="poster-card-title" data-open-detail="${effective.animeId}">${escapeHtml(title)}</div>
        ${
          isInUserList && showTrackingStepper
            ? `
              <div class="app-progress-bar">
                <div class="app-progress-fill ${isCompleted ? 'completed' : ''}" style="width:${progressPct}%;"></div>
              </div>
              ${renderInlineEpisodeStepper(effective, 'card')}
              ${subStatusHtml}
              <div class="rate-note-row">
                <button class="rate-note-pill ${userStars ? 'active' : ''}" data-quick-edit="${effective.animeId}">
                  ${userStars ? `★ ${userStars}` : '☆ Rate'}
                </button>
                <button class="rate-note-pill ${hasNote ? 'active' : ''}" data-quick-edit="${effective.animeId}">
                  ${hasNote ? '✎ Noted' : '✎ Note'}
                </button>
              </div>
            `
            : `
              <div style="display:flex;align-items:center;justify-content:space-between;gap:6px;">
                <span class="tabular-num" style="font-size:10.5px;color:var(--text-secondary);">
                  ${isInUserList ? `${watched}/${total || '∞'}` : `${formatLabel}${total ? ` • ${total} ep` : ''}`}
                </span>
                <button class="stepper-circle-btn ${isInUserList ? '' : 'plus'}" data-quick-add="${effective.animeId}" title="${isInUserList ? 'Edit tracking' : 'Add to Home & Library'}">
                  ${isInUserList ? '✓' : '+'}
                </button>
              </div>
            `
        }
      </div>
    </div>
  `;
}

// ============================================================================
// SCREEN 1: HOME SCREEN (Strictly displays ONLY explicitly tracked Home anime)
// ============================================================================

function renderHomeScreen() {
  const todayDow = ((new Date().getDay() + 6) % 7) + 1;
  const activeShows = state.vault.filter((v) => isAnimeOnHome(v));
  const dayBuckets = {};
  for (let d = 1; d <= 7; d++) dayBuckets[d] = [];
  const anytimeShows = [];

  activeShows.forEach((v) => {
    const dow = v.airingDayOfWeek || ((v.animeId % 7) + 1);
    if (dow >= 1 && dow <= 7) {
      dayBuckets[dow].push(v);
    } else {
      anytimeShows.push(v);
    }
  });

  const topBar = renderObsidianTopBar({
    title: 'ANIME ',
    accentTitleTail: 'UNIVERSE',
    subtitle: 'WEEKLY SCHEDULE',
    actionsHtml: `
      ${isTauriAppEnvironment() ? '' : `<button class="app-icon-btn" data-nav="downloads" title="Download Applications">${SVG_ICONS.download}</button>`}
      <button class="app-icon-btn" id="btn-open-quick-search" title="Quick Search">${SVG_ICONS.search}</button>
    `
  });

  const weekStripHtml = `
    <div class="section-header-row">
      <h2 class="serif-heading" style="font-size:28px;">This Week</h2>
      <button class="library-jump-pill" data-nav="mylist">LIBRARY ›</button>
    </div>
    <div class="week-strip" data-hscroll-key="home-week-strip">
      <button class="week-day-pill all-pill ${state.homeAllWeekDays ? 'selected' : ''}" id="btn-home-all-days">
        <span class="all-label">All</span>
        ${state.homeAllWeekDays ? `<span class="all-dot"></span>` : ''}
      </button>
      ${DAY_LABELS.map((d) => {
        const count = (dayBuckets[d.dow] || []).length;
        const isSelected = !state.homeAllWeekDays && state.selectedDay === d.dow;
        const isToday = d.dow === todayDow;
        return `
          <button class="week-day-pill ${isSelected ? 'selected' : ''}" data-select-day="${d.dow}">
            <span class="week-day-top">${d.short}</span>
            <span class="week-day-mid">${getDayNumberForDow(d.dow)}</span>
            ${
              isToday
                ? `<span class="week-day-bottom today-tag">TODAY</span>`
                : `<span class="week-day-bottom">${count}</span>`
            }
          </button>
        `;
      }).join('')}
    </div>
  `;

  let bodyHtml = '';
  if (activeShows.length === 0) {
    bodyHtml = `
      <div class="app-card" style="text-align:center;padding:42px 24px;margin-top:12px;">
        <h3 class="serif-heading" style="font-size:24px;margin-bottom:8px;">Your Weekly Schedule is Empty</h3>
        <p style="color:var(--text-secondary);font-size:13.5px;max-width:420px;margin:0 auto 18px;">
          Discover airing &amp; seasonal anime to build your personal day-by-day release tracker.
        </p>
        <button class="btn-primary" data-nav="discover">Explore Catalog</button>
      </div>
    `;
  } else if (state.homeAllWeekDays) {
    const orderedDays = Array.from({ length: 7 }, (_, i) => ((todayDow - 1 + i) % 7) + 1);
    const daySections = orderedDays
      .map((dow) => {
        const list = dayBuckets[dow] || [];
        if (list.length === 0) return '';
        const dInfo = DAY_LABELS.find((x) => x.dow === dow);
        const isToday = dow === todayDow;
        return `
          <section style="margin-bottom:26px;">
            <div class="section-header-row" style="padding:2px 0;">
              <h3 class="serif-heading" style="font-size:26px;color:var(--text-primary);">
                ${isToday ? `Today • ${dInfo.full}` : dInfo.full}
              </h3>
              <span class="show-count-pill">${list.length} SHOW${list.length === 1 ? '' : 'S'}</span>
            </div>
            <div class="poster-grid">${list.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>
          </section>
        `;
      })
      .join('');

    const anytimeSection =
      anytimeShows.length > 0
        ? `
          <section style="margin-bottom:26px;">
            <div class="section-header-row">
              <h3 class="serif-heading" style="font-size:26px;">Anytime &amp; Completed</h3>
              <span class="show-count-pill">${anytimeShows.length} SHOW${anytimeShows.length === 1 ? '' : 'S'}</span>
            </div>
            <div class="poster-grid">${anytimeShows.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>
          </section>
        `
        : '';
    bodyHtml = daySections + anytimeSection;
  } else {
    const dInfo = DAY_LABELS.find((x) => x.dow === state.selectedDay) || DAY_LABELS[0];
    const list = dayBuckets[state.selectedDay] || [];
    bodyHtml = `
      <section>
        <div class="section-header-row">
          <h3 class="serif-heading" style="font-size:26px;">${dInfo.full}</h3>
          <span class="show-count-pill">${list.length} SHOW${list.length === 1 ? '' : 'S'}</span>
        </div>
        ${
          list.length === 0
            ? `<div class="app-card" style="text-align:center;padding:36px;"><p style="color:var(--text-secondary);">No shows scheduled for ${dInfo.full}.</p></div>`
            : `<div class="poster-grid">${list.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>`
        }
      </section>
    `;
  }

  return `${topBar}${weekStripHtml}${bodyHtml}`;
}

// ============================================================================
// SCREEN 2: DISCOVER / EXPLORE SCREEN (Matches Screenshot 4 1:1)
// ============================================================================

function renderDiscoverScreen() {
  const modes = [
    { id: 'trending', label: 'TRENDING', title: 'Trending', sort: 'TRENDING_DESC' },
    { id: 'popular', label: 'POPULAR', title: 'Popular', sort: 'POPULARITY_DESC' },
    { id: 'highest_rated', label: 'TOP RATED', title: 'Top Rated', sort: 'SCORE_DESC' },
    { id: 'seasonal', label: 'SEASONAL', title: 'Seasonal', sort: 'POPULARITY_DESC' },
    { id: 'donghua', label: 'DONGHUA', title: 'Donghua', sort: 'POPULARITY_DESC' },
    { id: 'airing', label: 'AIRING', title: 'Airing', sort: 'TRENDING_DESC' }
  ];

  const activeModeObj = modes.find((m) => m.id === state.discoverMode) || modes[1];

  const topBar = renderObsidianTopBar({
    title: 'Discover',
    subtitle: 'THE GLOBAL CATALOG',
    actionsHtml: `
      <button class="app-icon-btn" id="btn-toggle-layout" title="Toggle Grid/List">
        ${SVG_ICONS.list}
      </button>
      <button class="app-icon-btn" id="btn-open-filters" title="Filters">
        ${SVG_ICONS.tune}
      </button>
    `
  });

  return `
    ${topBar}
    <div class="app-search-bar">
      <span class="search-icon">${SVG_ICONS.search}</span>
      <input id="inp-discover-search" autocomplete="off" placeholder="Search anime &amp; Donghua by keywords..." value="${escapeHtml(state.searchQuery)}" />
      <button class="clear-btn" id="btn-clear-disc-search" style="display:${state.searchQuery ? '' : 'none'};">✕</button>
    </div>

    <div class="capsule-strip">
      ${modes
        .map(
          (m) => `
          <button class="capsule-pill gold-solid ${state.discoverMode === m.id ? 'active' : ''}" data-disc-mode="${m.id}" data-disc-sort="${m.sort}">
            ${m.label}
          </button>`
        )
        .join('')}
      <button class="capsule-pill ${state.prefs.showAdultContent ? 'active' : ''}" id="btn-toggle-safe" style="border-color:rgba(80,200,130,0.45);color:var(--success);">
        ${SVG_ICONS.shield}
        <span>${state.prefs.showAdultContent ? '18+ SHOWN' : 'SAFE ON'}</span>
      </button>
    </div>

    <div class="capsule-strip">
      <button class="capsule-pill ${!state.discoverGenre ? 'active' : ''}" data-disc-genre="">All Genres</button>
      ${ANILIST_GENRES.map(
        (g) => `<button class="capsule-pill ${state.discoverGenre === g ? 'active' : ''}" data-disc-genre="${g}">${g}</button>`
      ).join('')}
    </div>

    <div class="section-header-row" style="margin-top:8px;">
      <h2 class="serif-heading" style="font-size:26px;">${escapeHtml(activeModeObj.title)}</h2>
      <span class="micro-label" id="discover-count-label" style="color:var(--accent-primary);">${state.discover.length} TITLES</span>
    </div>

    <div id="discover-results-shell">
      ${renderDiscoverResultsShellHtml()}
    </div>
  `;
}

// ============================================================================
// SCREEN 3: LIBRARY / MY LIST SCREEN (Matches Screenshot 2 1:1)
// ============================================================================

function renderMyListScreen() {
  const myQuery = state.myListQuery.trim();
  const filtered = state.vault
    .filter((item) => {
      if (state.myListStatusFilter !== 'ALL' && item.status !== state.myListStatusFilter) return false;
      if (state.myListSentimentFilter) {
        const s = sentimentFromRating10(item.rating10);
        if (!s || s.key !== state.myListSentimentFilter) return false;
      }
      if (state.myListCollectionFilter) {
        const col = state.collections.find((c) => c.id === state.myListCollectionFilter);
        if (col && !(col.animeIds || []).includes(item.animeId)) return false;
      }
      if (myQuery) {
        if (
          !matchesAllKeywords(
            myQuery,
            resolveTitle(item),
            item.title,
            item.titleEnglish,
            item.titleRomaji,
            item.synonyms || [],
            item.genres || []
          )
        ) {
          return false;
        }
      }
      return true;
    })
    .sort((a, b) => {
      if (myQuery) {
        const scA = scoreKeywordMatch(myQuery, resolveTitle(a), a.titleEnglish, a.titleRomaji, a.synonyms || []);
        const scB = scoreKeywordMatch(myQuery, resolveTitle(b), b.titleEnglish, b.titleRomaji, b.synonyms || []);
        if (scB !== scA) return scB - scA;
      }
      if (state.myListSort === 'TITLE') return resolveTitle(a).localeCompare(resolveTitle(b));
      if (state.myListSort === 'RATING') return (b.rating10 || 0) - (a.rating10 || 0);
      if (state.myListSort === 'PROGRESS') return (b.watchedEpisodes || 0) - (a.watchedEpisodes || 0);
      return 0;
    });

  const topBar = renderObsidianTopBar({
    title: 'Library',
    subtitle: `${state.vault.length} TRACKED • ${state.collections.length} COLLECTIONS`,
    actionsHtml: `
      <button class="app-icon-btn" id="btn-create-collection" title="Collections">${SVG_ICONS.collectionIcon}</button>
      <button class="app-icon-btn" id="btn-toggle-layout" title="Toggle Grid/List">${SVG_ICONS.list}</button>
    `
  });

  return `
    ${topBar}
    <!-- Status Capsule Tabs with Circular Count Badges (Screenshot 2 Row 1) -->
    <div class="capsule-strip">
      <button class="capsule-pill ${state.myListStatusFilter === 'ALL' ? 'active' : ''}" data-mylist-status="ALL">
        <span>ALL</span>
        <span class="pill-count-circle">${state.vault.length}</span>
      </button>
      ${WATCH_STATUSES.map((st) => {
        const count = state.vault.filter((v) => v.status === st.dbValue).length;
        return `
          <button class="capsule-pill ${state.myListStatusFilter === st.dbValue ? 'active' : ''}" data-mylist-status="${st.dbValue}">
            <span>${st.label.toUpperCase()}</span>
            <span class="pill-count-circle">${count}</span>
          </button>
        `;
      }).join('')}
    </div>

    <!-- Sentiment Filter Strip with Colored Names + Counts (Screenshot 2 Row 2) -->
    <div class="capsule-strip">
      <button class="capsule-pill ${!state.myListSentimentFilter ? 'active' : ''}" data-mylist-sentiment="">ALL RATINGS</button>
      ${SENTIMENTS.map((s) => {
        const count = state.vault.filter((v) => sentimentFromRating10(v.rating10)?.key === s.key).length;
        return `
          <button class="capsule-pill sentiment-filter-pill ${state.myListSentimentFilter === s.key ? 'active' : ''}" data-mylist-sentiment="${s.key}">
            <span class="sent-name ${s.key}">${s.shortLabel.toUpperCase()}</span>
            <span class="sent-count">${count}</span>
          </button>
        `;
      }).join('')}
    </div>

    <div class="app-search-bar">
      <span class="search-icon">${SVG_ICONS.search}</span>
      <input id="inp-mylist-search" placeholder="Filter your collection..." value="${escapeHtml(state.myListQuery)}" />
    </div>

    <div class="capsule-strip">
      ${[
        { k: 'RECENT', label: 'Sort: Recent' },
        { k: 'TITLE', label: 'Sort: Title' },
        { k: 'RATING', label: 'Sort: Rating' },
        { k: 'PROGRESS', label: 'Sort: Progress' }
      ]
        .map(
          (s) =>
            `<button class="capsule-pill ${state.myListSort === s.k ? 'active' : ''}" data-mylist-sort="${s.k}">${s.label}</button>`
        )
        .join('')}
    </div>

    <!-- Collections Section (Matches Screenshot 2) -->
    <div style="margin: 14px 0 18px;">
      <div class="section-header-row" style="margin-bottom:10px;">
        <div style="display:flex;align-items:baseline;gap:10px;flex-wrap:wrap;">
          <h3 class="serif-heading" style="font-size:24px;">Collections</h3>
          ${
            state.collections.length > 0
              ? `<span style="font-size:11px;color:var(--text-muted);">Right-click (PC) or Long-press (Phone) a category to delete</span>`
              : ''
          }
        </div>
        <button id="btn-new-collection-inline" style="background:none;border:none;color:var(--accent-primary);font-size:11.5px;font-weight:700;letter-spacing:1.4px;cursor:pointer;font-family:inherit;">
          + NEW
        </button>
      </div>
      ${
        state.collections.length === 0
          ? `
            <div style="background:var(--card-bg);border:1px solid var(--border-subtle);border-radius:18px;padding:16px 18px;color:var(--text-secondary);font-size:13.5px;">
              Create custom collections such as "Absolute Cinema" or "Donghua Favorites".
            </div>
          `
          : `
            <div class="capsule-strip" data-hscroll-key="library-collections-strip">
              <button class="capsule-pill ${!state.myListCollectionFilter ? 'active' : ''}" data-mylist-col="">All Collections</button>
              ${state.collections
                .map(
                  (c) => `
                  <button
                    type="button"
                    class="capsule-pill collection-category-pill ${state.myListCollectionFilter === c.id ? 'active' : ''}"
                    data-mylist-col="${escapeHtml(c.id)}"
                    data-mylist-col-name="${escapeHtml(c.name)}"
                    data-mylist-col-count="${(c.animeIds || []).length}"
                    title="Click to filter • Right-click (PC) or Long-press (Phone) to delete"
                  >
                    <span>${escapeHtml(c.name)}</span>
                    <span class="pill-count-circle">${(c.animeIds || []).length}</span>
                  </button>`
                )
                .join('')}
            </div>
          `
      }
    </div>

    ${
      filtered.length === 0
        ? `<div class="app-card" style="text-align:center;padding:42px;"><p style="color:var(--text-secondary);">No matching anime entries in your library.</p></div>`
        : `<div class="poster-grid">${filtered.map((it) => renderPosterTrackerCard(it, true)).join('')}</div>`
    }
  `;
}

// ============================================================================
// SCREEN 4: COMMUNITY SCREEN (Matches Screenshot 5 1:1)
// ============================================================================

function renderFiveStarsHtml(starsVal) {
  const val = Math.max(0, Math.min(5, Number(starsVal) || 0));
  let html = '';
  for (let i = 1; i <= 5; i++) {
    const fillPct = Math.round(Math.max(0, Math.min(1, val - (i - 1))) * 100);
    html += `<span class="star-frac-wrap">★<span class="star-frac-fill" style="width:${fillPct}%;">★</span></span>`;
  }
  return html;
}

function renderCommunityScreen() {
  const allUnfilteredGroups = groupCommunityReviewsByAnime(state.communityReviews);
  const allPosters = extractCommunityPosters(state.communityReviews);
  const validPosterKeys = new Set(allPosters.map((p) => p.key));
  const selectedUsers = (state.communitySelectedUsers || []).filter((u) => validPosterKeys.has(u.key)).slice(0, 3);
  if (selectedUsers.length !== (state.communitySelectedUsers || []).length) {
    state.communitySelectedUsers = selectedUsers;
  }

  // If 1-3 users are selected, scope templates and card reviews strictly to those selected users
  const activeReviewsPool =
    selectedUsers.length > 0
      ? (state.communityReviews || []).filter((r) => doesReviewMatchSelectedUsers(r, selectedUsers))
      : state.communityReviews;
  const allGroups = groupCommunityReviewsByAnime(activeReviewsPool);
  const totalNotesAndRatings = allGroups.reduce((acc, g) => acc + g.total_count, 0);

  const topBar = renderObsidianTopBar({
    title: 'Community',
    subtitle: `${allGroups.length} ANIME • ${totalNotesAndRatings} NOTES & RATINGS`
  });

  if (!state.session) {
    return `
      ${topBar}
      <div class="app-card" style="text-align:center;padding:42px 24px;max-width:480px;margin:28px auto;">
        <h2 class="serif-heading" style="font-size:28px;margin-bottom:8px;">Sign In for Community Lounge</h2>
        <p style="color:var(--text-secondary);font-size:13.5px;margin-bottom:20px;">
          Sign in to browse rated &amp; noted anime showcases shared by members and share your own taste.
        </p>
        <button class="btn-primary" id="btn-exit-guest" style="width:100%;">SIGN IN</button>
      </div>
    `;
  }

  const unseenByAnimeId = new Map(
    (state.notifications.unseenAnime || []).map((u) => [Number(u.anime_id), u])
  );

  const filtered = allGroups
    .filter((grp) => {
      if (!state.communitySentimentFilter) return true;
      const s = grp.average_rating10 ? sentimentFromRating10(grp.average_rating10) : null;
      return s && s.key === state.communitySentimentFilter;
    })
    .sort((a, b) => {
      if (state.communitySort === 'HIGHEST') return (b.average_rating10 || 0) - (a.average_rating10 || 0);
      return String(b.latest_updated_at || '').localeCompare(String(a.latest_updated_at || ''));
    });

  const shownAnimeIds = new Set(filtered.map((g) => Number(g.anime_id)));
  const unseenOnlyCards =
    selectedUsers.length > 0
      ? []
      : (state.notifications.unseenAnime || []).filter((u) => !shownAnimeIds.has(Number(u.anime_id)));

  const validCommunityTabs = new Set(COMMUNITY_TAB_ORDER);
  const viewTab = validCommunityTabs.has(state.communityViewTab) ? state.communityViewTab : 'feed';
  const stageSlideClass =
    lastCommunitySlideDir > 0
      ? 'slide-from-right'
      : lastCommunitySlideDir < 0
      ? 'slide-from-left'
      : '';
  lastCommunitySlideDir = 0;

  const unreadTaggedCount = Math.max(
    Number(state.notifications.unreadCount || 0),
    (state.notifications.unseenAnime || []).reduce((acc, u) => acc + Math.max(1, Number(u.unseen_count || 1)), 0)
  );
  const nState = state.notifications;

  return `
    ${topBar}
    <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;margin-bottom:6px;">
      <div class="community-view-switcher" id="community-view-switcher" role="tablist" aria-label="Community Sections">
        <span class="community-view-slider" id="community-view-slider" aria-hidden="true"></span>
        <button type="button" role="tab" aria-selected="${viewTab === 'feed' ? 'true' : 'false'}" class="community-view-tab ${viewTab === 'feed' ? 'active' : ''}" data-comm-view-tab="feed">
          Community Feed
        </button>
        <button type="button" role="tab" aria-selected="${viewTab === 'tagged' ? 'true' : 'false'}" class="community-view-tab ${viewTab === 'tagged' ? 'active' : ''}" data-comm-view-tab="tagged">
          <span>Tagged You</span>
          ${unreadTaggedCount > 0 ? `<span class="tagged-count-badge">${unreadTaggedCount > 99 ? '99+' : unreadTaggedCount}</span>` : ''}
        </button>
        <button type="button" role="tab" aria-selected="${viewTab === 'random' ? 'true' : 'false'}" class="community-view-tab ${viewTab === 'random' ? 'active' : ''}" data-comm-view-tab="random">
          Random
        </button>
        <button type="button" role="tab" aria-selected="${viewTab === 'top' ? 'true' : 'false'}" class="community-view-tab ${viewTab === 'top' ? 'active' : ''}" data-comm-view-tab="top">
          Top
        </button>
      </div>
      ${
        viewTab === 'tagged' && unreadTaggedCount > 0
          ? `<button type="button" class="capsule-pill active" id="btn-comm-tagged-mark-all">Mark all read</button>`
          : ''
      }
    </div>

    <div class="community-section-stage ${stageSlideClass}" id="community-section-stage">
    ${
      viewTab === 'random'
        ? renderRandomAnimeSection()
        : viewTab === 'top'
        ? renderTopAnimeSection()
        : viewTab === 'tagged'
        ? `
          ${nState.error ? `<div class="feedback-banner error" style="margin-bottom:12px;">! ${escapeHtml(nState.error)}</div>` : ''}
          ${
            nState.loading && nState.items.length === 0
              ? `<div class="app-card" style="text-align:center;padding:40px 20px;color:var(--text-secondary);">Loading tagged notifications...</div>`
              : nState.items.length === 0
              ? `<div class="app-card" style="text-align:center;padding:42px 24px;">
                   <div class="serif-heading" style="font-size:24px;margin-bottom:6px;">All caught up</div>
                   <p style="color:var(--text-secondary);font-size:13.5px;">When someone tags you in any anime template — new or old — it appears here for instant one-tap jump.</p>
                 </div>`
              : `
                <div class="tagged-you-list">
                  ${nState.items
                    .map((n) => {
                      const nType = n.notification_type || n.type || 'mention_comment';
                      const actionLabel =
                        nType === 'thread_reply'
                          ? 'replied to you'
                          : nType === 'mention_reply'
                          ? 'tagged you in a reply'
                          : 'tagged you';
                      const snippet = formatPlainPreviewWithEmojis(n.preview_text || n.content_preview || '');
                      const commName = (n.community_name && !/livtrak/i.test(n.community_name)) ? n.community_name : 'Community';
                      return `
                        <button
                          type="button"
                          class="tagged-you-card ${n.is_read ? '' : 'unread'}"
                          data-open-tagged-notif="${escapeHtml(n.id)}"
                          data-notif-anime="${Number(n.anime_id || 0)}"
                          data-notif-title="${escapeHtml(n.anime_title || '')}"
                          data-notif-cover="${escapeHtml(n.anime_cover_image || '')}"
                          data-notif-comment="${escapeHtml(n.comment_id || '')}"
                          data-notif-thread="${escapeHtml(n.thread_id || '')}"
                          data-notif-reply="${escapeHtml(n.reply_id || '')}"
                        >
                          ${
                            n.anime_cover_image
                              ? `<img class="tagged-you-thumb" src="${escapeHtml(n.anime_cover_image)}" alt="" loading="lazy" />`
                              : `<div class="tagged-you-thumb" style="display:grid;place-items:center;color:var(--accent-primary);font-weight:700;">★</div>`
                          }
                          <div class="tagged-you-body">
                            <div class="tagged-you-meta-top">
                              <span class="tagged-you-community-pill">${escapeHtml(commName)}</span>
                              <span class="tagged-you-time">${escapeHtml(formatDiscussionTimestamp(n.created_at))}</span>
                            </div>
                            <div class="tagged-you-anime-title">${escapeHtml(n.anime_title || `Anime #${n.anime_id}`)}</div>
                            <div class="tagged-you-who">
                              ${renderAvatarCircle(n.sender_avatar_key || 'avatar_1', n.sender_username || 'Member', 20)}
                              <span><strong style="color:var(--highlight);">@${escapeHtml(n.sender_username || 'Member')}</strong> ${actionLabel}</span>
                            </div>
                            ${snippet ? `<div class="tagged-you-snippet">“${escapeHtml(snippet)}”</div>` : ''}
                          </div>
                          ${!n.is_read ? `<span class="community-card-red-dot" title="Unread"></span>` : ''}
                        </button>
                      `;
                    })
                    .join('')}
                </div>
                ${
                  nState.hasMore
                    ? `<button type="button" class="btn-secondary" id="btn-comm-tagged-load-more" style="width:100%;margin-top:12px;" ${nState.loadingMore ? 'disabled' : ''}>
                         ${nState.loadingMore ? 'LOADING...' : 'LOAD OLDER TAGS'}
                       </button>`
                    : ''
                }
              `
          }
        `
        : `
    <div class="capsule-strip">
      <button class="capsule-pill ${!state.communitySentimentFilter ? 'active' : ''}" data-comm-sentiment="">
        ALL (${allGroups.length})
      </button>
      ${SENTIMENTS.map(
        (s) =>
          `<button class="capsule-pill ${state.communitySentimentFilter === s.key ? 'active' : ''}" data-comm-sentiment="${s.key}">
            ${s.shortLabel.toUpperCase()}
          </button>`
      ).join('')}
    </div>

    <div class="capsule-strip" style="align-items:center;">
      ${[
        { k: 'RECENT', label: 'Newest' },
        { k: 'HIGHEST', label: 'Highest Rated' }
      ]
        .map(
          (opt) =>
            `<button class="capsule-pill ${state.communitySort === opt.k ? 'active' : ''}" data-comm-sort="${opt.k}">${opt.label}</button>`
        )
        .join('')}
      <button
        type="button"
        class="capsule-pill ${state.communityUserFilterOpen || selectedUsers.length > 0 ? 'active' : ''}"
        id="btn-comm-toggle-user-filter"
        title="Filter templates by posted username (up to 3 users)"
      >
        <span>Posted By${selectedUsers.length > 0 ? ` (${selectedUsers.length}/3)` : ''}</span>
        <span style="font-size:10px;opacity:0.85;">${state.communityUserFilterOpen ? '▴' : '▾'}</span>
      </button>
    </div>

    ${
      selectedUsers.length > 0
        ? `
          <div class="community-user-active-bar">
            <span class="community-user-by-label">by →</span>
            <div class="community-user-applied-chips">
              ${selectedUsers
                .map(
                  (u, idx) => `
                  <button
                    type="button"
                    class="community-user-applied-chip"
                    data-comm-remove-user="${escapeHtml(u.key)}"
                    title="Remove @${escapeHtml(u.username)} from filter"
                  >
                    ${renderAvatarCircle(u.avatarKey || 'avatar_1', u.username, 18)}
                    <span>${escapeHtml(u.username)}</span>
                    <span class="chip-close-x">✕</span>
                  </button>${idx < selectedUsers.length - 1 ? `<span class="community-user-comma">,</span>` : ''}`
                )
                .join('')}
            </div>
            <span class="community-user-limit-tag">${selectedUsers.length}/3 max</span>
            <button type="button" class="community-user-clear-btn" id="btn-comm-clear-user-filter">Clear</button>
          </div>
        `
        : ''
    }

    ${
      state.communityUserFilterOpen
        ? `
          <div class="community-user-picker-panel">
            <div class="community-user-picker-header">
              <div>
                <div class="micro-label" style="color:var(--accent-primary);">FILTER TEMPLATES BY POSTED USERNAME</div>
                <div style="font-size:12px;color:var(--text-secondary);margin-top:2px;">
                  Select up to <strong>3 users max</strong> (by → 1, 2, 3) to show only templates posted by them (${selectedUsers.length}/3 selected • ${allUnfilteredGroups.length} total templates).
                </div>
              </div>
              ${
                allPosters.length > 5
                  ? `<input
                       id="inp-comm-user-search"
                       class="auth-input"
                       style="height:34px;max-width:210px;font-size:12.5px;padding:0 12px;"
                       placeholder="Search username..."
                       value="${escapeHtml(state.communityUserSearchQuery || '')}"
                     />`
                  : ''
              }
            </div>
            <div class="community-user-picker-grid">
              ${
                allPosters
                  .filter((p) => {
                    const q = String(state.communityUserSearchQuery || '').trim().toLowerCase();
                    return !q || p.username.toLowerCase().includes(q);
                  })
                  .map((p) => {
                    const selIdx = selectedUsers.findIndex((u) => u.key === p.key);
                    const isSelected = selIdx !== -1;
                    const isDisabled = !isSelected && selectedUsers.length >= 3;
                    return `
                      <button
                        type="button"
                        class="community-user-option-pill ${isSelected ? 'selected' : ''} ${isDisabled ? 'disabled-limit' : ''}"
                        data-comm-pick-user="${escapeHtml(p.key)}"
                        data-comm-pick-uid="${escapeHtml(p.userId)}"
                        data-comm-pick-name="${escapeHtml(p.username)}"
                        data-comm-pick-av="${escapeHtml(p.avatarKey || 'avatar_1')}"
                      >
                        ${renderAvatarCircle(p.avatarKey || 'avatar_1', p.username, 22)}
                        <span class="community-user-option-name">${escapeHtml(p.username)}</span>
                        <span class="pill-count-circle">${p.postCount}</span>
                        ${isSelected ? `<span class="community-user-order-badge">#${selIdx + 1}</span>` : ''}
                      </button>
                    `;
                  })
                  .join('') || `<div style="color:var(--text-muted);font-size:12.5px;padding:6px 4px;">No matching usernames found.</div>`
              }
            </div>
          </div>
        `
        : ''
    }

    ${
      filtered.length === 0 && unseenOnlyCards.length === 0
        ? `<div class="app-card" style="text-align:center;padding:42px;"><p style="color:var(--text-secondary);">No rated &amp; noted showcases match this filter yet.</p></div>`
        : `<div class="community-review-list">
            ${unseenOnlyCards
              .map((u) => {
                const actionWord = u.latest_notification_type === 'reply' ? 'replied to you' : 'tagged you in discussion';
                const extraCount = Number(u.unseen_count || 1) > 1 ? ` • ${u.unseen_count} unseen` : '';
                return `
                  <div
                    class="community-review-card unseen-highlight-card"
                    data-open-unseen-anime="${Number(u.anime_id)}"
                    data-unseen-comment="${escapeHtml(u.latest_comment_id || '')}"
                    data-unseen-thread="${escapeHtml(u.latest_thread_id || '')}"
                    data-unseen-reply="${escapeHtml(u.latest_reply_id || '')}"
                  >
                    <div style="display:flex;gap:14px;align-items:center;">
                      ${
                        u.anime_cover_image
                          ? `<img src="${escapeHtml(u.anime_cover_image)}" alt="" style="width:60px;height:86px;object-fit:cover;border-radius:14px;flex-shrink:0;" />`
                          : `<div style="width:60px;height:86px;border-radius:14px;background:var(--elevated-surface);display:flex;align-items:center;justify-content:center;color:var(--accent-primary);font-weight:700;">★</div>`
                      }
                      <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:6px;">
                        <div style="display:flex;align-items:center;gap:8px;">
                          <span class="community-card-red-dot" title="Unseen discussion message"></span>
                          <div class="serif-heading" style="font-size:21px;line-height:25px;color:var(--text-primary);">${escapeHtml(u.anime_title || `Anime #${u.anime_id}`)}</div>
                        </div>
                        <div class="unseen-tag-pill">
                          <span class="community-card-red-dot small"></span>
                          <span>${escapeHtml(u.latest_sender_username || 'Member')} ${actionWord}${extraCount}</span>
                        </div>
                        ${
                          u.latest_preview
                            ? `<div style="font-size:12.5px;color:var(--text-secondary);font-style:italic;">“${escapeHtml(u.latest_preview)}”</div>`
                            : ''
                        }
                        <div style="font-size:11.5px;font-weight:600;color:var(--accent-primary);">Open anime discussion to read &amp; clear red dot →</div>
                      </div>
                    </div>
                  </div>
                `;
              })
              .join('')}
            ${filtered
              .map((grp) => {
                const unseen = unseenByAnimeId.get(Number(grp.anime_id));
                const starsNum = grp.average_rating10 ? rating10ToStars(grp.average_rating10) || 0 : 0;
                const stars = starsNum > 0 ? starsNum.toFixed(1) : '';
                const sentiment = grp.average_rating10 ? sentimentFromRating10(grp.average_rating10) : null;
                const lead = grp.lead_review || grp.reviews[0];
                const extraContributors = Math.max(0, grp.reviews.length - 1);
                const leadText =
                  lead && String(lead.review_notes || '').trim().length > 0
                    ? `“${escapeHtml(lead.review_notes)}”`
                    : `${escapeHtml(lead?.username || 'Member')} rated this ${stars || '5.0'} ★`;
                const stackedAvatarsHtml = grp.reviews
                  .slice(0, 3)
                  .map((r, idx) => {
                    const isOwnRev = Boolean(state.session?.userId && r.user_id === state.session.userId);
                    const cachedP = typeof DiscussionRealtimeManager !== 'undefined' ? DiscussionRealtimeManager.profileCache.get(r.user_id) : null;
                    const resolvedAvKey = isOwnRev
                      ? (state.profile.avatarKey || cachedP?.avatar_key || r.avatar_key || 'avatar_1')
                      : (cachedP?.avatar_key || r.avatar_key || 'avatar_1');
                    const hasFrame = Boolean(getAvatarFramePreset(resolvedAvKey));
                    return `<div style="margin-left:${idx === 0 ? '0' : hasFrame ? '2px' : '-6px'};${hasFrame ? '' : 'border:1.5px solid var(--card-bg);border-radius:999px;'}display:inline-flex;overflow:visible;">
                      ${renderAvatarCircle(resolvedAvKey, r.username, 28)}
                    </div>`;
                  })
                  .join('');

                return `
                  <div
                    class="community-review-card ${unseen ? 'unseen-highlight-card' : ''}"
                    ${
                      unseen
                        ? `data-open-unseen-anime="${grp.anime_id}" data-unseen-comment="${escapeHtml(unseen.latest_comment_id || '')}" data-unseen-thread="${escapeHtml(unseen.latest_thread_id || '')}" data-unseen-reply="${escapeHtml(unseen.latest_reply_id || '')}"`
                        : `data-open-detail="${grp.anime_id}"`
                    }
                  >
                    <div style="display:flex;gap:16px;align-items:flex-start;">
                      <img src="${escapeHtml(grp.anime_cover_image)}" alt="" style="width:68px;height:96px;object-fit:cover;border-radius:14px;flex-shrink:0;" />
                      <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:6px;">
                        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;min-width:0;">
                            <div class="serif-heading" style="font-size:23px;line-height:27px;color:var(--text-primary);">${escapeHtml(grp.anime_title)}</div>
                            ${grp.is_adult ? `<span class="adult-18-badge">18+</span>` : ''}
                          </div>
                          ${unseen ? `<span class="community-card-red-dot" title="Unseen tag or reply"></span>` : ''}
                        </div>
                        ${
                          unseen
                            ? `<div class="unseen-tag-pill">
                                <span class="community-card-red-dot small"></span>
                                <span>${escapeHtml(unseen.latest_sender_username || 'Member')} ${unseen.latest_notification_type === 'reply' ? 'replied' : 'tagged you'}${Number(unseen.unseen_count || 1) > 1 ? ` (+${Number(unseen.unseen_count) - 1})` : ''} • Open</span>
                              </div>`
                            : ''
                        }
                        ${
                          starsNum > 0
                            ? `<div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
                                <div style="display:inline-flex;gap:2px;">${renderFiveStarsHtml(starsNum)}</div>
                                <span class="tabular-num" style="font-size:15px;font-weight:600;color:var(--text-primary);">${stars}</span>
                                ${
                                  grp.rated_count > 1
                                    ? `<span style="font-size:11.5px;color:var(--text-muted);">(${grp.rated_count} ratings)</span>`
                                    : ''
                                }
                              </div>`
                            : `<div><span class="sentiment-pill soft cinema">COMMUNITY NOTES</span></div>`
                        }
                        ${sentiment ? `<div><span class="sentiment-pill soft ${sentiment.key}">${sentiment.fullLabel.toUpperCase()}</span></div>` : ''}
                      </div>
                    </div>

                    <div class="review-quote-box" data-open-comments="${grp.anime_id}" title="View all ${grp.total_count} notes &amp; ratings">
                      <div class="review-quote-bar"></div>
                      <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:4px;">
                        <div class="review-quote-text">${leadText}</div>
                        ${
                          extraContributors > 0
                            ? `<div style="font-size:11.5px;font-weight:600;color:var(--accent-primary);">+${extraContributors} more member note${extraContributors > 1 ? 's' : ''} &amp; rating${extraContributors > 1 ? 's' : ''} inside</div>`
                            : ''
                        }
                      </div>
                    </div>

                    <div style="display:flex;align-items:center;justify-content:space-between;padding-top:2px;gap:10px;">
                      <div style="display:flex;align-items:center;gap:10px;min-width:0;">
                        <div style="display:inline-flex;align-items:center;">${stackedAvatarsHtml}</div>
                        <span style="font-size:13.5px;font-weight:500;color:var(--text-secondary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
                          ${escapeHtml(lead?.username || 'Member')}${extraContributors > 0 ? ` +${extraContributors}` : ''}
                        </span>
                      </div>

                      <div style="display:flex;align-items:center;gap:8px;flex-shrink:0;">
                        <span style="font-size:12.5px;color:var(--text-muted);">${escapeHtml(formatReviewDate(grp.latest_updated_at))}</span>
                        ${
                          state.session
                            ? (() => {
                                const myRev = grp.reviews.find((r) => r.user_id === state.session?.userId);
                                const myVault = state.vault.find((v) => Number(v.animeId) === Number(grp.anime_id));
                                const myStars = rating10ToStars(myRev?.rating ?? myVault?.rating10) || 0;
                                const isQrOpen = state.communityQuickRate && Number(state.communityQuickRate.animeId) === Number(grp.anime_id);
                                return `
                                  ${
                                    myRev
                                      ? `<button
                                          type="button"
                                          class="community-note-edit-btn"
                                          data-open-edit-review="${grp.anime_id}"
                                          title="Edit your review for ${escapeHtml(grp.anime_title)}"
                                        >✎ Edit</button>`
                                      : ''
                                  }
                                  <button
                                    type="button"
                                    class="community-quick-rate-btn ${isQrOpen ? 'active' : ''}"
                                    data-comm-quick-rate="${grp.anime_id}"
                                    data-comm-my-stars="${myStars}"
                                    title="Rate ${escapeHtml(grp.anime_title)} directly with 0.1 precision"
                                  >
                                    <span>★</span>
                                    <span class="tabular-num">${myStars > 0 ? myStars.toFixed(1) : 'Rate'}</span>
                                  </button>
                                `;
                              })()
                            : ''
                        }
                        <button
                          type="button"
                          class="community-comment-btn"
                          data-open-comments="${grp.anime_id}"
                          title="Open all notes &amp; ratings for ${escapeHtml(grp.anime_title)}"
                        >
                          <span class="comment-icon-svg">${SVG_ICONS.comment}</span>
                          <span class="tabular-num">${grp.total_count}</span>
                        </button>
                      </div>
                    </div>
                    ${
                      state.session && state.communityQuickRate && Number(state.communityQuickRate.animeId) === Number(grp.anime_id)
                        ? (() => {
                            const qrStars = Number(state.communityQuickRate.stars || 0);
                            const qrSent = qrStars > 0 ? sentimentFromRating10(starsToRating10(qrStars)) : null;
                            return `
                              <div class="community-quick-rate-drawer" onclick="event.stopPropagation()">
                                <div class="community-qr-header">
                                  <div class="community-qr-header-left">
                                    <span class="micro-label" style="color:var(--accent-primary);white-space:nowrap;">DIRECT RATING</span>
                                    <span id="comm-qr-val-label" class="tabular-num community-qr-val">${qrStars > 0 ? `${qrStars.toFixed(1)} ★` : '4.0 ★'}</span>
                                  </div>
                                  <div id="comm-qr-sentiment-slot" class="community-qr-sent-slot">${
                                    qrSent
                                      ? `<span class="sentiment-pill soft ${qrSent.key}" style="font-size:9px;padding:2px 8px;white-space:nowrap;">${qrSent.shortLabel.toUpperCase()}</span>`
                                      : ''
                                  }</div>
                                </div>
                                <div style="padding:2px 0;">
                                  ${renderSmoothRatingSliderHtml({
                                    id: 'comm-qr-slider',
                                    value: qrStars > 0 ? qrStars : 4.0,
                                    large: true,
                                    showScale: true,
                                    ariaLabel: 'Community direct rating slider'
                                  })}
                                </div>
                                <div class="community-qr-footer">
                                  <div style="display:flex;gap:6px;flex-shrink:0;">
                                    <button type="button" class="capsule-pill" id="btn-comm-qr-minus" style="height:30px;padding:0 10px;font-size:11.5px;">−0.1 ★</button>
                                    <button type="button" class="capsule-pill" id="btn-comm-qr-plus" style="height:30px;padding:0 10px;font-size:11.5px;">+0.1 ★</button>
                                  </div>
                                  <div style="display:flex;gap:8px;flex-shrink:0;">
                                    <button type="button" class="btn-secondary" id="btn-comm-qr-close" style="height:34px;padding:0 13px;font-size:12px;">Cancel</button>
                                    <button type="button" class="btn-primary" id="btn-comm-qr-save" style="height:34px;padding:0 15px;font-size:12px;" ${
                                      state.communityQuickRate.saving || qrStars <= 0 ? 'disabled' : ''
                                    }>${state.communityQuickRate.saving ? 'SAVING...' : 'SAVE'}</button>
                                  </div>
                                </div>
                              </div>
                            `;
                          })()
                        : ''
                    }
                  </div>
                `;
              })
              .join('')}
          </div>`
    }
    `
    }
    </div>
  `;
}

// ============================================================================
// SCREEN 5: PROFILE SCREEN
// ============================================================================

function renderProfileScreen() {
  const ratedItems = state.vault.filter((v) => (v.rating10 || 0) > 0);
  const userXp = getUserXp();
  const watchedCount = state.vault.filter((v) => (v.watchedEpisodes || 0) >= 1).length;
  const meanScore =
    ratedItems.length > 0
      ? `★ ${(ratedItems.reduce((acc, v) => acc + (rating10ToStars(v.rating10) || 0), 0) / ratedItems.length).toFixed(1)}`
      : '—';
  const favorites = state.vault.filter((v) => v.isFavorite);
  const avPreset = getAvatarPreset(state.profile.avatarKey);
  const activeFrame = getAvatarFramePreset(state.profile.avatarKey);
  const avatarImg = avPreset.img;
  const framesUnlocked = userXp >= AVATAR_FRAME_UNLOCK_XP;

  return `
    <div style="max-width:760px;margin:0 auto;">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:6px 0 14px;">
        <button class="app-icon-btn" data-nav="home" title="Back">${SVG_ICONS.back}</button>
        <h1 class="serif-heading" style="font-size:24px;">Profile</h1>
        <button class="app-icon-btn" data-nav="settings" title="Settings">${SVG_ICONS.settings}</button>
      </div>

      <div style="display:flex;flex-direction:column;align-items:center;gap:8px;padding:18px 0 18px;">
        <div id="btn-open-avatar-modal" style="position:relative;width:108px;height:108px;cursor:pointer;overflow:visible;display:flex;align-items:center;justify-content:center;${activeFrame ? 'margin:16px 0;' : ''}" title="Tap to change portrait">
          <div style="width:104px;height:104px;border-radius:50%;padding:3px;background:linear-gradient(135deg,${avPreset.c1},${avPreset.c2});box-shadow:0 12px 32px var(--accent-soft);">
            <img src="${avatarImg}" alt="" onerror="${AVATAR_IMG_ONERROR}" style="width:100%;height:100%;border-radius:50%;object-fit:cover;border:2.5px solid var(--bg-main);display:block;" />
          </div>
          ${
            activeFrame
              ? `<img class="avatar-frame-overlay" src="${activeFrame.img}" alt="${escapeHtml(activeFrame.name)}" onerror="${AVATAR_IMG_ONERROR}" style="width:146px;height:146px;" />`
              : ''
          }
          <div style="position:absolute;bottom:0;right:0;z-index:4;width:28px;height:28px;border-radius:50%;background:var(--accent-primary);color:var(--on-accent);border:2px solid var(--bg-main);display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:700;">✎</div>
        </div>

        <div id="btn-open-profile-edit" style="display:flex;align-items:center;gap:6px;cursor:pointer;padding:4px 12px;border-radius:14px;">
          <h2 class="serif-heading" style="font-size:28px;font-weight:600;">${escapeHtml(state.session ? state.profile.username : 'Guest Tracker')}</h2>
          <span style="color:var(--accent-primary);font-size:14px;">✎</span>
        </div>

        <div class="micro-label" style="color:var(--text-muted);letter-spacing:1.6px;">
          ${state.session ? 'COMMUNITY MEMBER' : 'LOCAL VAULT'}${activeFrame ? ` • ${escapeHtml(activeFrame.name.toUpperCase())}` : ''}
        </div>
      </div>

      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-bottom:16px;">
        ${[
          { label: 'XP', val: userXp },
          { label: 'WATCHED', val: watchedCount },
          { label: 'SAVED', val: state.vault.length },
          { label: 'MEAN', val: meanScore }
        ]
          .map(
            (st) => `
            <div class="app-card" style="padding:14px 8px;text-align:center;border-radius:20px;">
              <div class="serif-heading tabular-num" style="font-size:24px;font-weight:600;color:var(--highlight);">${st.val}</div>
              <div class="micro-label" style="color:var(--text-muted);margin-top:4px;font-size:8px;">${st.label}</div>
            </div>`
          )
          .join('')}
      </div>

      <button
        type="button"
        id="btn-open-avatar-frames"
        class="app-card clickable avatar-frames-trigger-btn"
        style="width:100%;margin-bottom:16px;text-align:left;"
      >
        <div class="avatar-frames-trigger-left">
          <div class="avatar-frames-trigger-preview">
            ${renderAvatarCircle(
              state.profile.avatarKey,
              state.profile.username || 'A',
              42,
              '',
              activeFrame ? activeFrame.key : 'frame_1'
            )}
          </div>
          <div style="min-width:0;">
            <div class="micro-label" style="color:var(--accent-primary);letter-spacing:1.8px;">COLLECTOR COSMETICS</div>
            <div class="serif-heading" style="font-size:22px;line-height:1.15;margin-top:2px;">Avatar Frames</div>
            <div style="font-size:12px;color:var(--text-secondary);margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
              ${
                activeFrame
                  ? `Equipped: ${escapeHtml(activeFrame.name)} • All avatars`
                  : '10 Collector Frames • Usable on all avatars'
              }
            </div>
          </div>
        </div>
        <div class="avatar-frames-trigger-right">
          <span class="avatar-frames-status-pill ${framesUnlocked ? 'unlocked' : 'locked'}">
            ${framesUnlocked ? '★ UNLOCKED' : `🔒 ${userXp} / ${AVATAR_FRAME_UNLOCK_XP} XP`}
          </span>
          <span style="color:var(--accent-primary);font-size:20px;line-height:1;">›</span>
        </div>
      </button>

      <div class="app-card" style="background:linear-gradient(135deg,var(--accent-soft),var(--card-bg));border-color:var(--accent-border);margin-bottom:16px;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
          <span class="micro-label" style="color:var(--accent-primary);letter-spacing:2px;">COMMUNITY STATUS</span>
          <span class="sentiment-pill soft worth">${state.session ? 'Active' : 'Local Preview'}</span>
        </div>
        <div class="serif-heading" style="font-size:22px;margin-bottom:4px;">
          ${state.session ? 'Verified Lounge Member' : 'Local Device Vault'}
        </div>
        <p style="font-size:13px;color:var(--text-secondary);line-height:1.5;">
          ${
            state.session
              ? 'Your profile showcase and frames sync automatically across Android, Web, and PC.'
              : 'Sign in anytime from Settings to sync your library and join the community.'
          }
        </p>
      </div>

      ${
        favorites.length > 0
          ? `
            <div style="margin-top:18px;">
              <h3 class="serif-heading" style="font-size:22px;margin-bottom:10px;">Favorites (${favorites.length})</h3>
              <div class="poster-grid">${favorites.map((it) => renderPosterTrackerCard(it, false)).join('')}</div>
            </div>
          `
          : ''
      }
    </div>
  `;
}

// ============================================================================
// SCREEN 6: SETTINGS SCREEN
// ============================================================================

function renderSettingsScreen() {
  const cat = state.settingsCategory;
  const ns = state.notificationSettings || {};
  const dr = state.deviceRegistration || {};
  const osPerm = String(state.osNotifPermission || ns.os_permission_status || 'prompt').toLowerCase();
  const osPermLabel =
    osPerm === 'granted' ? 'GRANTED' : osPerm === 'denied' ? 'DENIED' : 'NOT REQUESTED';
  const osPermColor =
    osPerm === 'granted' ? '#4ADE80' : osPerm === 'denied' ? '#F87171' : '#FBBF24';

  return `
    <div style="max-width:720px;margin:0 auto;">
      <div style="display:flex;align-items:center;gap:12px;padding:6px 0 16px;">
        <button class="app-icon-btn" id="btn-settings-back" title="Back">${SVG_ICONS.back}</button>
        <div>
          <h1 class="serif-heading" style="font-size:26px;">${cat ? cat.charAt(0) + cat.slice(1).toLowerCase() : 'Settings'}</h1>
          <div class="topbar-subtitle">${cat === 'NOTIFICATIONS' ? 'SYSTEM NOTIFICATION SETTINGS' : cat ? 'PREFERENCES & CONTROLS' : 'SYSTEM & PREFERENCES'}</div>
        </div>
      </div>

      ${
        !cat
          ? `
            <div style="display:flex;flex-direction:column;gap:10px;">
              ${[
                { id: 'ACCOUNT', title: 'Account', sub: state.session ? `Signed in as ${state.profile.username} • Auto-Sync Active` : 'Local Preview Mode • Sign in' },
                {
                  id: 'NOTIFICATIONS',
                  title: 'Notifications',
                  sub: `Status: ${osPermLabel} • Open device notification settings`
                },
                {
                  id: 'APPEARANCE',
                  title: 'Appearance',
                  sub: `${state.prefs.themeMode === 'DARK' ? 'Dark' : state.prefs.themeMode === 'LIGHT' ? 'Light' : 'System'} • ${getActiveAccentDisplayInfo().label}`
                },
                { id: 'PREFERENCES', title: 'Preferences', sub: `Title Language (${state.prefs.titleLanguage})` },
                { id: 'ABOUT', title: 'About', sub: 'Anime Universe v2.4.0 • Architecture & Credits' }
              ]
                .map(
                  (row) => `
                  <div class="app-card clickable" data-settings-cat="${row.id}" style="display:flex;align-items:center;justify-content:space-between;">
                    <div>
                      <div class="serif-heading" style="font-size:22px;">${row.title}</div>
                      <div style="font-size:12.5px;color:var(--text-secondary);margin-top:2px;">${escapeHtml(row.sub)}</div>
                    </div>
                    <span style="color:var(--accent-primary);font-size:20px;">›</span>
                  </div>`
                )
                .join('')}
            </div>
          `
          : cat === 'NOTIFICATIONS'
          ? `
            <div style="display:flex;flex-direction:column;gap:14px;">
              <div class="app-card" style="display:flex;flex-direction:column;gap:14px;">
                <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;">
                  <div>
                    <div class="micro-label" style="color:var(--accent-primary);">DEVICE NOTIFICATIONS</div>
                    <div class="serif-heading" style="font-size:22px;margin-top:2px;">System Notification Settings</div>
                  </div>
                  <span style="padding:4px 12px;border-radius:999px;font-size:11px;font-weight:800;letter-spacing:0.08em;background:var(--surface-subtle);border:1px solid ${osPermColor};color:${osPermColor};">
                    PERMISSION: ${osPermLabel}
                  </span>
                </div>

                <p style="font-size:13px;color:var(--text-secondary);line-height:1.5;margin:0;">
                  Manage notification permissions, alert style, and sound directly in your device's system settings for <strong>Anime Universe</strong>.
                </p>

                ${
                  osPerm === 'denied'
                    ? `
                      <div class="notif-perm-denied-box" style="margin:0;">
                        <div class="notif-perm-denied-title">Notifications are turned off in system settings</div>
                        <div class="notif-perm-denied-text">
                          Open your device's notification settings for <strong>Anime Universe</strong> and turn on <strong>Allow Notifications</strong> to receive alerts.
                        </div>
                      </div>
                    `
                    : ''
                }

                <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
                  ${
                    osPerm !== 'granted'
                      ? `<button type="button" class="btn-primary" id="btn-enable-os-notifs" style="height:40px;padding:0 16px;font-size:12.5px;">
                           Allow Notifications
                         </button>`
                      : ''
                  }
                  <button type="button" class="${osPerm === 'granted' ? 'btn-primary' : 'btn-secondary'}" id="btn-open-os-notif-settings" style="height:40px;padding:0 16px;font-size:12.5px;">
                    Open Notification Settings
                  </button>
                </div>
              </div>
            </div>
          `
          : cat === 'APPEARANCE'
          ? (() => {
              const effectiveTheme = getEffectiveWebThemeMode();
              const isDarkEffective = effectiveTheme === 'dark';
              const activeAccentInfo = getActiveAccentDisplayInfo();
              const isCustomSelected = activeAccentInfo.key === 'CUSTOM';
              const currentCustomHex = sanitizeWebHexColor(state.prefs.customAccentHex) || '#38BDF8';
              const themeCards = [
                { id: 'DARK', label: 'Dark', type: 'dark' },
                { id: 'LIGHT', label: 'Light', type: 'light' },
                { id: 'SYSTEM', label: 'System', type: 'system' }
              ];
              const renderMiniPane = (paneKind) => {
                const dotColor = paneKind === 'light' ? activeAccentInfo.lightHex : activeAccentInfo.darkHex;
                return `
                  <div class="theme-mini-pane ${paneKind === 'light' ? 'is-light' : 'is-dark'}">
                    <div class="theme-mini-top">
                      <div class="theme-mini-bar primary"></div>
                      <span class="theme-mini-dot" style="background:${dotColor};"></span>
                    </div>
                    <div class="theme-mini-card">
                      <div class="theme-mini-bar muted"></div>
                      <div class="theme-mini-pill" style="background:${dotColor};"></div>
                    </div>
                  </div>
                `;
              };
              const themeLabel =
                state.prefs.themeMode === 'DARK'
                  ? 'Dark'
                  : state.prefs.themeMode === 'LIGHT'
                  ? 'Light'
                  : `System (${isDarkEffective ? 'Dark' : 'Light'})`;
              return `
                <div class="appearance-settings-stack">
                  <div class="app-card">
                    <div class="appearance-section-header">
                      <div>
                        <div class="micro-label" style="color:var(--accent-primary);">THEME</div>
                        <div class="serif-heading" style="font-size:21px;margin-top:2px;">Interface Surface</div>
                        <div class="appearance-section-subtitle">Choose how Anime Universe looks across all screens, cards, and modals.</div>
                      </div>
                      <span class="appearance-active-badge">${escapeHtml(themeLabel)}</span>
                    </div>
                    <div class="appearance-theme-grid" role="radiogroup" aria-label="Theme Mode">
                      ${themeCards
                        .map((card) => {
                          const selected = state.prefs.themeMode === card.id;
                          return `
                            <button
                              type="button"
                              class="theme-preview-card ${selected ? 'selected' : ''}"
                              data-set-theme="${card.id}"
                              role="radio"
                              aria-checked="${selected ? 'true' : 'false'}"
                            >
                              <div class="theme-preview-mini">
                                ${
                                  card.type === 'system'
                                    ? `${renderMiniPane('dark')}${renderMiniPane('light')}`
                                    : renderMiniPane(card.type)
                                }
                                ${selected ? `<span class="theme-preview-check" aria-hidden="true">✓</span>` : ''}
                              </div>
                              <span class="theme-preview-label">${card.label}</span>
                            </button>
                          `;
                        })
                        .join('')}
                    </div>
                  </div>

                  <div class="app-card">
                    <div class="appearance-section-header">
                      <div>
                        <div class="micro-label" style="color:var(--accent-primary);">ACCENT COLOR</div>
                        <div class="serif-heading" style="font-size:21px;margin-top:2px;">Signature Highlight</div>
                        <div class="appearance-section-subtitle">Applies to buttons, active navigation, tabs, and highlights in both Dark &amp; Light modes.</div>
                      </div>
                      <span class="appearance-active-badge">${escapeHtml(activeAccentInfo.label)}</span>
                    </div>

                    <div class="accent-swatch-grid" role="radiogroup" aria-label="Accent Color">
                      ${WEB_ACCENT_PRESETS.map((preset) => {
                        const selected = activeAccentInfo.key === preset.id;
                        const fillHex = isDarkEffective ? preset.darkHex : preset.lightHex;
                        const rgb = hexToRgbObj(fillHex);
                        const checkColor = pickAccessibleOnAccentHex(rgb.r, rgb.g, rgb.b);
                        return `
                          <button
                            type="button"
                            class="accent-swatch-item ${selected ? 'selected' : ''}"
                            data-set-accent="${preset.id}"
                            role="radio"
                            aria-checked="${selected ? 'true' : 'false'}"
                            title="${escapeHtml(preset.label)}"
                          >
                            <span class="accent-swatch-circle">
                              <span class="accent-swatch-fill" style="background:${fillHex};color:${checkColor};">
                                ${selected ? '✓' : ''}
                              </span>
                            </span>
                            <span class="accent-swatch-name">${escapeHtml(preset.shortLabel)}</span>
                          </button>
                        `;
                      }).join('')}

                      <button
                        type="button"
                        class="accent-swatch-item ${isCustomSelected ? 'selected' : ''}"
                        data-set-accent="CUSTOM"
                        role="radio"
                        aria-checked="${isCustomSelected ? 'true' : 'false'}"
                        title="Custom Accent Color"
                      >
                        <span class="accent-swatch-circle">
                          ${
                            isCustomSelected
                              ? (() => {
                                  const customFill = isDarkEffective ? activeAccentInfo.darkHex : activeAccentInfo.lightHex;
                                  const cRgb = hexToRgbObj(customFill);
                                  const cCheck = pickAccessibleOnAccentHex(cRgb.r, cRgb.g, cRgb.b);
                                  return `<span class="accent-swatch-fill" style="background:${customFill};color:${cCheck};">✓</span>`;
                                })()
                              : `<span class="accent-swatch-fill rainbow"></span>`
                          }
                        </span>
                        <span class="accent-swatch-name">Custom</span>
                      </button>
                    </div>

                    ${
                      isCustomSelected
                        ? `
                          <div class="custom-accent-panel">
                            <div class="micro-label" style="color:var(--text-secondary);">CUSTOM COLOR PICKER</div>
                            <div class="custom-accent-quick-row">
                              ${WEB_CUSTOM_QUICK_HEXES.map((hex) => {
                                const qSelected = currentCustomHex === hex;
                                const qRgb = hexToRgbObj(hex);
                                const qCheck = pickAccessibleOnAccentHex(qRgb.r, qRgb.g, qRgb.b);
                                return `
                                  <button
                                    type="button"
                                    class="custom-quick-dot ${qSelected ? 'selected' : ''}"
                                    data-set-custom-quick="${hex}"
                                    aria-label="Select custom color ${hex}"
                                    title="${hex}"
                                  >
                                    <span class="custom-quick-dot-inner" style="background:${hex};color:${qCheck};">
                                      ${qSelected ? '✓' : ''}
                                    </span>
                                  </button>
                                `;
                              }).join('')}
                            </div>
                            <div class="custom-accent-controls">
                              <input
                                type="color"
                                id="inp-custom-accent-color"
                                class="custom-color-native"
                                value="${escapeHtml(currentCustomHex)}"
                                aria-label="Pick custom accent color"
                              />
                              <input
                                type="text"
                                id="inp-custom-accent-hex"
                                class="custom-hex-input"
                                value="${escapeHtml(currentCustomHex)}"
                                maxlength="7"
                                spellcheck="false"
                                placeholder="#38BDF8"
                                aria-label="Custom hex color code"
                              />
                              <button type="button" class="btn-primary" id="btn-apply-custom-accent" style="height:44px;padding:0 18px;font-size:13px;">
                                Apply
                              </button>
                            </div>
                          </div>
                        `
                        : ''
                    }
                  </div>
                </div>
              `;
            })()
          : cat === 'PREFERENCES'
          ? `
            <div class="app-card" style="display:flex;flex-direction:column;gap:18px;">
              <div>
                <div class="micro-label" style="color:var(--accent-primary);margin-bottom:8px;">PREFERRED TITLE LANGUAGE</div>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                  ${['ENGLISH', 'ROMAJI', 'NATIVE']
                    .map((l) => `<button class="capsule-pill gold-solid ${state.prefs.titleLanguage === l ? 'active' : ''}" data-set-lang="${l}">${l}</button>`)
                    .join('')}
                </div>
              </div>
            </div>
          `
          : cat === 'ACCOUNT'
          ? `
            <div class="app-card" style="display:flex;flex-direction:column;gap:14px;">
              ${
                state.session
                  ? `
                    <div class="serif-heading" style="font-size:22px;">Signed in as ${escapeHtml(state.profile.username)}</div>
                    <p style="font-size:13px;color:var(--text-secondary);">Real-time cross-platform cloud sync is active automatically.</p>
                    <div style="display:flex;gap:10px;flex-wrap:wrap;">
                      <button class="btn-secondary" id="btn-signout">Sign Out</button>
                    </div>
                  `
                  : `
                    <div class="serif-heading" style="font-size:22px;">Local Preview Mode</div>
                    <p style="font-size:13px;color:var(--text-secondary);">Sign in to sync your tracked anime across Android, Desktop &amp; Web.</p>
                    <button class="btn-primary" id="btn-exit-guest">SIGN IN</button>
                  `
              }
            </div>
          `
          : `
            <div class="app-card" style="display:flex;flex-direction:column;gap:10px;">
              <div class="serif-heading" style="font-size:24px;">Anime Universe v2.4.0</div>
              <p style="font-size:13.5px;color:var(--text-secondary);line-height:1.55;">
                Designed with the Obsidian &amp; Champagne Gold editorial aesthetic. Powered by AniList GraphQL &amp; Supabase Cloud Vault.
              </p>
            </div>
          `
      }
    </div>
  `;
}

// ============================================================================
// SCREEN 7: ANIME DETAIL SCREEN
// ============================================================================

function renderAnimeDetailScreen() {
  const a = state.detailAnime;
  if (!a) return '';
  const title = resolveTitle(a);
  const subJp = a.titleNative || (a.titleRomaji !== title ? a.titleRomaji : '');
  const rawFmtUpper = String(a.format || '').toUpperCase();
  const isMovie = rawFmtUpper === 'MOVIE';
  const isOvaOrSpecial = rawFmtUpper === 'OVA' || rawFmtUpper === 'SPECIAL';
  const isDonghua = String(a.countryOfOrigin || '').toUpperCase() === 'CN';
  const seasons = state.detailRich?.seasons || [];
  const moviesAndOvas = state.detailRich?.moviesAndOvas || [];
  const relatedMedia = state.detailRich?.relatedMedia || [];
  const franchiseId = state.detailRich?.franchiseId || a.animeId;
  const rawMainSeasonNodeCount = state.detailRich?.rawMainSeasonNodeCount || seasons.length;

  const activeMergedSeason = getActiveMergedSeasonForDetail();
  const total = activeMergedSeason
    ? (activeMergedSeason.episodes || a.totalEpisodes || null)
    : (a.totalEpisodes || (isMovie || isOvaOrSpecial ? 1 : null));
  const watched = activeMergedSeason
    ? computeWebContinuousWatched(activeMergedSeason, a)
    : (a.watchedEpisodes || 0);
  const isTrackedInVault =
    Boolean(a.inWatchlist) ||
    Boolean(
      activeMergedSeason &&
        activeMergedSeason.subEntries.some((seg) => state.vault.some((v) => Number(v.animeId) === Number(seg.id)))
    );

  const isFranchiseMergedAsOne =
    seasons.length === 1 && ((seasons[0]?.subEntries?.length || 0) > 1 || rawMainSeasonNodeCount > 1);
  const canToggleSeasonMerge =
    rawMainSeasonNodeCount > 1 ||
    seasons.length > 1 ||
    seasons.some((s) => (s.subEntries?.length || 0) > 1);
  const showSeasonsSelector =
    seasons.length > 1 ||
    isFranchiseMergedAsOne ||
    (seasons.length > 0 && moviesAndOvas.length > 0);

  const stars = rating10ToStars(a.rating10) || 0;
  const sentiment = sentimentFromRating10(a.rating10);
  const formatTag = isMovie
    ? (isDonghua ? 'DONGHUA MOVIE' : 'MOVIE')
    : isOvaOrSpecial
    ? rawFmtUpper
    : isDonghua
    ? 'DONGHUA'
    : String(a.format || 'TV').replace(/_/g, ' ');
  const statusTag =
    String(a.mediaStatus || '').toUpperCase() === 'FINISHED'
      ? 'FINISHED'
      : String(a.mediaStatus || '').toUpperCase() === 'NOT_YET_RELEASED'
      ? 'UPCOMING'
      : String(a.mediaStatus || '').toUpperCase() === 'HIATUS'
      ? 'HIATUS'
      : 'ONGOING';

  return `
    <div style="max-width:940px;margin:0 auto;">
      <div style="position:relative;width:100%;height:300px;border-radius:26px;overflow:hidden;margin-bottom:16px;background:var(--card-bg);">
        <img src="${escapeHtml(a.bannerImage || a.coverImage)}" alt="" style="width:100%;height:100%;object-fit:cover;display:block;" />
        <div style="position:absolute;inset:0;background:linear-gradient(180deg,rgba(9,11,16,0.45) 0%,rgba(9,11,16,0.22) 32%,rgba(9,11,16,0.86) 72%,rgba(9,11,16,0.95) 100%);"></div>

        <div style="position:absolute;top:14px;left:14px;right:14px;display:flex;justify-content:space-between;">
          <button class="app-icon-btn" id="btn-detail-back" style="background:rgba(8,8,12,0.68);color:#FFF;">${SVG_ICONS.back}</button>
          <button class="app-icon-btn" id="btn-detail-fav" style="background:rgba(8,8,12,0.68);color:${a.isFavorite ? 'var(--error)' : '#FFF'};">
            ${a.isFavorite ? SVG_ICONS.heartFilled : SVG_ICONS.heartOutline}
          </button>
        </div>

        <div style="position:absolute;bottom:12px;left:16px;right:16px;display:flex;align-items:flex-end;gap:14px;">
          <img src="${escapeHtml(a.coverImage)}" alt="" style="width:104px;height:152px;object-fit:cover;border-radius:18px;border:1px solid var(--border-strong);flex-shrink:0;box-shadow:0 10px 28px rgba(0,0,0,0.7);" />
          <div style="flex:1;min-width:0;">
            <h1 class="serif-heading" style="font-size:28px;line-height:30px;color:#FFF;">${escapeHtml(title)}</h1>
            ${subJp ? `<div class="micro-label" style="color:rgba(255,255,255,0.78);margin-top:4px;">${escapeHtml(subJp)}</div>` : ''}
            <div style="display:flex;align-items:center;gap:8px;margin-top:8px;flex-wrap:wrap;">
              <span class="rating-badge" style="color:var(--accent-primary);">${escapeHtml(formatTag)}</span>
              <span class="rating-badge" style="color:var(--text-secondary);">${escapeHtml(statusTag)}${a.seasonYear ? ` • ${a.seasonYear}` : ''}</span>
              ${stars > 0 ? `<span class="rating-badge">★ ${stars.toFixed(1)}</span>` : a.averageScore ? `<span class="rating-badge">★ ${(a.averageScore / 20).toFixed(1)}</span>` : ''}
              ${sentiment ? `<span class="sentiment-pill solid ${sentiment.key}">${sentiment.fullLabel}</span>` : ''}
            </div>
          </div>
        </div>
      </div>

      <div class="app-card" style="border-radius:24px;margin-bottom:16px;display:flex;flex-direction:column;gap:14px;">
        ${
          showSeasonsSelector
            ? (() => {
                const activeSeason = seasons.find(
                  (s) => s.isCurrent || s.id === a.animeId || (Array.isArray(s.subEntries) && s.subEntries.some((seg) => seg.id === a.animeId))
                );
                const distinctSeasonCount = new Set(seasons.map((s) => s.seasonNumber)).size;
                const totalLabel =
                  distinctSeasonCount === 1
                    ? (activeSeason?.subEntries?.length > 1 ? `1 Continuous Season (${activeSeason.subEntries.length} Parts)` : '1 Season')
                    : `${distinctSeasonCount} Seasons`;
                const activeMeta = activeSeason
                  ? `${activeSeason.displayLabel || `Season ${activeSeason.seasonNumber}`} • ${totalLabel}${activeSeason.seasonYear ? ` • ${activeSeason.seasonYear}` : ''}`
                  : totalLabel;
                const toggleLabel = isFranchiseMergedAsOne ? 'Split Seasons' : 'Merge as 1 Season';
                return `
              <div>
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;gap:8px;flex-wrap:wrap;">
                  <div>
                    <div class="micro-label" style="color:var(--accent-primary);">SEASONS</div>
                    <div class="micro-label" style="color:var(--text-secondary);margin-top:2px;">${escapeHtml(activeMeta)}</div>
                  </div>
                  ${
                    canToggleSeasonMerge
                      ? `<button type="button" class="capsule-pill" id="btn-detail-season-override-toggle" data-franchise-id="${franchiseId}" data-merged-one="${isFranchiseMergedAsOne ? '1' : '0'}" style="height:32px;padding:0 12px;font-size:10px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:var(--accent-primary);border-color:var(--accent-border);">
                          ${escapeHtml(toggleLabel)}
                        </button>`
                      : ''
                  }
                </div>
                <div class="capsule-strip" style="margin-bottom:4px;">
                  ${seasons
                    .map((s) => {
                      const isUpcoming = String(s.status || '').toUpperCase() === 'NOT_YET_RELEASED';
                      const isSel = Boolean(
                        s.isCurrent || s.id === a.animeId || (Array.isArray(s.subEntries) && s.subEntries.some((seg) => seg.id === a.animeId))
                      );
                      const tooltip = `${s.title || s.displayLabel || `Season ${s.seasonNumber}`}${s.seasonYear ? ` (${s.seasonYear})` : ''}`;
                      return `
                      <button class="capsule-pill gold-solid ${isSel ? 'active' : ''}" data-open-detail="${s.id}" title="${escapeHtml(tooltip)}" style="min-width:52px;height:44px;justify-content:center;font-size:14px;gap:6px;padding:0 14px;">
                        <span>${escapeHtml(s.shortLabel || String(s.seasonNumber))}</span>
                        ${s.isMergedContinuousRun && s.episodes ? `<span style="font-size:10px;opacity:0.85;">• ${s.episodes} EPS</span>` : ''}
                        ${isUpcoming && !isSel ? '<span style="width:6px;height:6px;border-radius:50%;background:var(--warning);display:inline-block;"></span>' : ''}
                      </button>`;
                    })
                    .join('')}
                </div>
              </div>
            `;
              })()
            : ''
        }

        ${
          moviesAndOvas.length > 0
            ? `
              <div style="${showSeasonsSelector ? 'border-top:1px solid var(--border-subtle);padding-top:12px;' : ''}">
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;gap:8px;">
                  <div class="micro-label" style="color:var(--accent-primary);">MOVIES &amp; OVAs</div>
                  <div class="micro-label" style="color:var(--text-secondary);">${moviesAndOvas.length} ${moviesAndOvas.length === 1 ? 'ITEM' : 'ITEMS'}</div>
                </div>
                <div class="capsule-strip" style="margin-bottom:2px;gap:10px;">
                  ${moviesAndOvas
                    .map((m) => {
                      const isSel = Boolean(m.isCurrent || Number(m.id) === Number(a.animeId));
                      const vaultItem = Number(m.id) === Number(a.animeId) ? a : state.vault.find((v) => Number(v.animeId) === Number(m.id));
                      const mWatched = Number(vaultItem?.watchedEpisodes || 0);
                      const mTotal = Math.max(1, Number(vaultItem?.totalEpisodes || m.episodes || 1));
                      return `
                      <button type="button" data-open-detail="${m.id}" style="display:flex;flex-direction:column;align-items:flex-start;gap:4px;min-width:170px;max-width:240px;padding:10px 12px;border-radius:16px;background:${isSel ? 'rgba(229,193,128,0.16)' : 'var(--elevated-surface)'};border:1px solid ${isSel ? 'var(--accent-primary)' : 'var(--border-subtle)'};cursor:pointer;text-align:left;flex-shrink:0;">
                        <div style="display:flex;justify-content:space-between;align-items:center;width:100%;gap:8px;">
                          <span style="font-size:9px;font-weight:700;letter-spacing:0.08em;padding:2px 6px;border-radius:6px;background:${isSel ? 'var(--accent-primary)' : 'var(--card-bg)'};color:${isSel ? '#08080C' : 'var(--accent-primary)'};">${escapeHtml(m.formatBadge)}</span>
                          <span class="tabular-num" style="font-size:10px;font-weight:600;color:${isSel ? 'var(--accent-primary)' : 'var(--text-secondary)'};">${m.releaseYear || 'TBA'} • EP ${mWatched} / ${mTotal}</span>
                        </div>
                        <div style="font-size:12.5px;font-weight:600;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;width:100%;">${escapeHtml(m.title)}</div>
                      </button>`;
                    })
                    .join('')}
                </div>
              </div>
            `
            : ''
        }

        <div class="micro-label" style="color:var(--accent-primary);">${
          isMovie
            ? 'MOVIE PROGRESS • 0 = UNWATCHED, 1 = WATCHED'
            : activeMergedSeason
            ? `${escapeHtml(activeMergedSeason.displayLabel.toUpperCase())} • CONTINUOUS EPISODE COUNTER`
            : 'EPISODE PROGRESS • CLICK NUMBER TO EDIT'
        }</div>
        ${
          !isTrackedInVault
            ? `<button class="btn-primary" id="btn-detail-add" style="width:100%;">+ ADD TO HOME &amp; LIBRARY</button>`
            : `
              ${renderInlineEpisodeStepper({ ...a, watchedEpisodes: watched, totalEpisodes: total }, 'detail')}
              <div class="capsule-strip" style="margin-bottom:0;">
                ${WATCH_STATUSES.map(
                  (st) => `
                  <button class="capsule-pill ${a.status === st.dbValue ? 'active' : ''}" data-detail-status="${st.dbValue}">
                    ${st.label}
                  </button>`
                ).join('')}
              </div>
              <div style="display:flex;flex-wrap:wrap;gap:8px;margin-top:2px;">
                ${
                  isAnimeOnHome(a)
                    ? `<button type="button" class="capsule-pill" id="btn-detail-remove-home" style="color:var(--accent-primary);border-color:var(--accent-border);">Remove from Home (Keep in Library)</button>`
                    : `<button type="button" class="capsule-pill active" id="btn-detail-add-home">+ Add to Home Tracking</button>`
                }
                <button type="button" class="capsule-pill" id="btn-detail-remove" style="color:var(--error);border-color:rgba(229,90,107,0.4);">Remove from Library</button>
              </div>
            `
        }
      </div>

      <div class="app-card" style="border-radius:24px;margin-bottom:16px;display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <div>
            <div class="micro-label" style="color:var(--accent-primary);">YOUR STAR RATING</div>
            <div id="detail-star-heading" class="serif-heading" style="font-size:28px;color:${stars > 0 ? 'var(--highlight)' : 'var(--text-muted)'};">
              ${stars > 0 ? `${stars.toFixed(1)} ★` : 'Unrated'}
            </div>
          </div>
          <div style="display:flex;gap:6px;">
            <button class="capsule-pill" id="btn-star-minus">−0.1 ★</button>
            <button class="capsule-pill" id="btn-star-plus">+0.1 ★</button>
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:10px;background:var(--elevated-surface);border-radius:20px;padding:12px 14px;">
          <div id="detail-stars-row" style="display:flex;justify-content:space-evenly;">
            ${[1, 2, 3, 4, 5]
              .map(
                (idx) => `
                <button data-star-val="${idx}" style="background:none;border:none;font-size:32px;cursor:pointer;color:${
                  stars >= idx - 0.5 ? 'var(--accent-primary)' : 'var(--text-muted)'
                };">★</button>`
              )
              .join('')}
          </div>
          ${renderSmoothRatingSliderHtml({
            id: 'detail-star-slider',
            value: stars > 0 ? stars : 0.1,
            large: true,
            showScale: true,
            ariaLabel: 'Anime star rating slider'
          })}
        </div>

        <div id="detail-sentiment-strip" class="capsule-strip" style="margin-bottom:0;">
          ${SENTIMENTS.map(
            (s) => `
            <button class="capsule-pill ${sentiment?.key === s.key ? 'active' : ''}" data-sentiment-key="${s.key}" data-pick-sentiment-stars="${s.suggestedStars}">
              ${s.shortLabel.toUpperCase()} • ${s.suggestedStars.toFixed(1)}★
            </button>`
          ).join('')}
        </div>

        <div class="micro-label" style="color:var(--accent-primary);margin-top:4px;">WRITTEN REVIEW NOTE</div>
        <textarea id="inp-detail-notes" class="auth-input" style="height:96px;padding:12px;resize:vertical;" placeholder="Write your thoughts on this anime...">${escapeHtml(
          state.detailNoteDraft !== undefined && state.detailNoteDraft !== null ? state.detailNoteDraft : (a.reviewNotes || '')
        )}</textarea>
        <button class="btn-primary" id="btn-detail-save-note" ${state.detailRatingSaving ? 'disabled' : ''}>
          ${state.detailRatingSaving ? 'SAVING...' : 'SAVE RATING & NOTE'}
        </button>
        ${
          state.detailRatingFeedback
            ? `<div class="feedback-banner success" id="detail-rating-feedback">✓ ${escapeHtml(state.detailRatingFeedback)}</div>`
            : ''
        }
        ${
          state.detailRatingError
            ? `<div class="feedback-banner error" id="detail-rating-error">! ${escapeHtml(state.detailRatingError)}</div>`
            : ''
        }
      </div>

      <div class="app-card" style="border-radius:24px;margin-bottom:16px;display:flex;flex-direction:column;gap:12px;">
        <div class="micro-label" style="color:var(--accent-primary);">SYNOPSIS</div>
        <p style="font-size:13.5px;color:var(--text-secondary);line-height:1.6;">
          ${escapeHtml(a.synopsis || 'No synopsis available.')}
        </p>
        <div class="capsule-strip" style="margin-bottom:0;">
          ${(a.genres || []).map((g) => `<span class="capsule-pill">${escapeHtml(g)}</span>`).join('')}
        </div>
      </div>

      ${
        relatedMedia.length > 0
          ? `
        <div class="app-card" style="border-radius:24px;margin-bottom:16px;display:flex;flex-direction:column;gap:12px;">
          <div class="micro-label" style="color:var(--accent-primary);">RELATED MOVIES, SPECIALS &amp; SERIES</div>
          <div style="display:flex;gap:12px;overflow-x:auto;padding-bottom:6px;">
            ${relatedMedia
              .map(
                (rel) => `
              <button type="button" data-open-detail="${rel.id}" style="flex:0 0 128px;background:var(--elevated-surface);border:1px solid var(--border-subtle);border-radius:16px;padding:8px;text-align:left;cursor:pointer;display:flex;flex-direction:column;gap:6px;color:var(--text-primary);">
                <img src="${escapeHtml(rel.coverImage)}" alt="${escapeHtml(rel.title)}" loading="lazy" style="width:100%;height:156px;object-fit:cover;border-radius:10px;background:var(--card-bg);" />
                <div class="micro-label" style="color:var(--accent-primary);font-size:9px;">${escapeHtml(rel.format || rel.relationType)}${rel.seasonYear ? ` • ${rel.seasonYear}` : ''}</div>
                <div style="font-size:12px;font-weight:600;line-height:1.25;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">${escapeHtml(rel.title)}</div>
              </button>
            `
              )
              .join('')}
          </div>
        </div>
      `
          : ''
      }

      ${renderAnimeDiscussionSection(a)}
    </div>
  `;
}

function renderEmojiTrayHtml(scope) {
  EmojiCacheManager.hydrateFromLocalStorage();
  const topEmojis = EmojiCacheManager.getTopEmojis(8);
  const allEmojis = EmojiCacheManager.getAllEmojis();
  const isOpen = Boolean(state.discussion.emojiTrayOpen);
  return `
    <div class="emoji-tray ${isOpen ? 'open' : ''}" id="livtrak-emoji-tray">
      <div class="emoji-tray-head">
        <span class="emoji-tray-label">Top Emojis</span>
        <span class="emoji-tray-label" style="color:var(--text-muted);">${allEmojis.length} available</span>
      </div>
      ${
        allEmojis.length === 0
          ? `<div style="padding:14px 4px;text-align:center;color:var(--text-secondary);font-size:12px;">
               No custom emojis found yet. Upload GIF/PNG/WebP files to your Supabase <strong>community_emojis</strong> bucket.
             </div>`
          : `
            <div class="emoji-top-bar">
              ${topEmojis
                .map(
                  (em) => `
                    <button
                      type="button"
                      class="emoji-btn"
                      data-insert-emoji-id="${escapeHtml(em.id)}"
                      data-insert-emoji-scope="${escapeHtml(scope)}"
                      title=":${escapeHtml(em.id)}:"
                    >
                      ${renderCustomEmojiSlotHtml(em.id, { lazy: false, sizeClass: 'size-lg' })}
                    </button>
                  `
                )
                .join('')}
            </div>
            <div class="emoji-grid">
              ${allEmojis
                .map(
                  (em) => `
                    <button
                      type="button"
                      class="emoji-btn"
                      data-insert-emoji-id="${escapeHtml(em.id)}"
                      data-insert-emoji-scope="${escapeHtml(scope)}"
                      title=":${escapeHtml(em.id)}:"
                    >
                      ${renderCustomEmojiSlotHtml(em.id, { lazy: true, sizeClass: 'size-lg' })}
                    </button>
                  `
                )
                .join('')}
            </div>
          `
      }
    </div>
  `;
}

function renderComposerEmojiPreviewHtml(draftText) {
  const ids = extractEmojiIdsFromText(draftText);
  if (!ids.length) return '';
  const uniquePreview = ids.slice(0, 8);
  return `
    <div class="chat-composer-emoji-preview">
      ${uniquePreview
        .map(
          (eid) => `
            <span class="chat-composer-emoji-chip">
              ${renderCustomEmojiSlotHtml(eid, { lazy: false })}
              <span>:${escapeHtml(eid)}:</span>
            </span>
          `
        )
        .join('')}
    </div>
  `;
}

function renderAnimeDiscussionSection(anime, options = {}) {
  const inModal = Boolean(options.inModal);
  const d = state.discussion;
  const currentUserId = state.session?.userId || null;
  const hl = d.highlightTarget || {};
  const activeScope = 'comment';
  const activeDraft = d.draft || '';
  const activeTags = d.commentTags || [];

  // Single flat chronological stream (oldest -> newest at bottom)
  const chronological = [...(d.comments || [])].sort(
    (a, b) => new Date(a.created_at || 0).getTime() - new Date(b.created_at || 0).getTime()
  );

  return `
    <div class="livtrak-chat-shell ${inModal ? '' : 'inline-detail-chat'}" id="anime-discussion-section">
      ${
        !inModal
          ? `<div style="padding:12px 18px;border-bottom:1px solid var(--border-subtle);display:flex;align-items:center;justify-content:space-between;">
               <span style="font-size:13px;font-weight:700;color:var(--accent-primary);letter-spacing:0.4px;">Live Chat</span>
               <span class="tabular-num" style="font-size:11.5px;color:var(--text-secondary);">${d.totalComments} ${d.totalComments === 1 ? 'message' : 'messages'}</span>
             </div>`
          : ''
      }

      ${d.error ? `<div class="feedback-banner error" style="margin:10px 16px 0;">! ${escapeHtml(d.error)}</div>` : ''}

      <div class="chat-messages" id="livtrak-chat-messages">
        ${
          d.hasMore
            ? `<button type="button" class="c-micro-action" id="btn-discussion-load-more" style="align-self:center;padding:5px 12px;border-radius:999px;background:var(--accent-soft);border:1px solid var(--accent-border);color:var(--accent-primary);" ${d.loadingMore ? 'disabled' : ''}>
                 ${d.loadingMore ? 'Loading earlier messages...' : 'Load earlier messages'}
               </button>`
            : ''
        }

        ${
          d.loading && chronological.length === 0
            ? `<div style="margin:auto;text-align:center;color:var(--text-secondary);font-size:13px;padding:30px 12px;">Loading chat...</div>`
            : chronological.length === 0
            ? `<div style="margin:auto;text-align:center;color:var(--text-secondary);font-size:13px;padding:34px 16px;">
                 No messages yet for <strong style="color:var(--text-primary);">${escapeHtml(resolveTitle(anime))}</strong>. Say hi!
               </div>`
            : chronological
                .map((c) => {
                  const isMine = Boolean(currentUserId && c.user_id === currentUserId);
                  const isEditingThis = d.editingCommentId === c.id;
                  const isCommentHighlighted = hl.commentId === c.id || hl.replyId === c.id;
                  const rawBody = c.content ?? c.body ?? '';
                  const soloEid = extractSoloEmojiId(rawBody);
                  const isUnicodeSolo = !soloEid && isSoloUnicodeEmoji(rawBody);
                  const replyTo = c.replyTo || null;
                  const quoteButtonHtml = (standalone = false) =>
                    replyTo && (replyTo.previewText || replyTo.stickerCode)
                      ? `<button
                           type="button"
                           class="c-quote-preview ${standalone ? 'standalone-quote' : ''}"
                           data-jump-comment="${escapeHtml(replyTo.messageId || '')}"
                           title="Jump to quoted message"
                         >
                           <span class="c-quote-author">@${escapeHtml(replyTo.senderName || 'Member')}</span>
                           <span class="c-quote-text">${renderQuotedPreviewContentHtml(replyTo.previewText, replyTo.stickerCode)}</span>
                         </button>`
                      : '';

                  const cachedProfile = DiscussionRealtimeManager.profileCache.get(c.user_id);
                  const effectiveAvatarKey = isMine
                    ? (state.profile.avatarKey || cachedProfile?.avatar_key || c.avatar_key || 'avatar_1')
                    : (cachedProfile?.avatar_key || c.avatar_key || 'avatar_1');
                  const effectiveUsername = isMine
                    ? (state.profile.username || cachedProfile?.username || c.username || 'Member')
                    : (cachedProfile?.username || c.username || 'Member');
                  const timestampText = `${formatDiscussionTimestamp(c.created_at)}${c.updated_at && c.updated_at !== c.created_at ? ' • edited' : ''}`;

                  return `
                    <div class="c-msg-group ${isCommentHighlighted ? 'highlight-pulse' : ''}" id="discussion-comment-${escapeHtml(c.id)}">
                      <div
                        class="c-msg ${isMine ? 'mine' : ''}"
                        data-ctx-comment="${escapeHtml(c.id)}"
                        data-ctx-source="${escapeHtml(c.source_type || 'comment')}"
                        data-ctx-uid="${escapeHtml(c.user_id)}"
                        data-ctx-name="${escapeHtml(effectiveUsername)}"
                        data-ctx-owner="${isMine ? '1' : '0'}"
                      >
                        <div class="c-msg-avatar-wrap">${renderAvatarCircle(effectiveAvatarKey, effectiveUsername, 32)}</div>
                        <div class="c-bubble-wrap">
                          ${
                            !isMine
                              ? `<div class="c-user">
                                   <span>${escapeHtml(effectiveUsername)}</span>
                                   <span class="c-time">${escapeHtml(timestampText)}</span>
                                 </div>`
                              : `<div class="c-user mine">
                                   <span class="c-time">${escapeHtml(timestampText)}</span>
                                   <span>${escapeHtml(effectiveUsername)}</span>
                                 </div>`
                          }

                          ${
                            isEditingThis
                              ? `
                                <div class="c-bubble" style="min-width:220px;">
                                  <textarea id="inp-discussion-edit-comment" class="auth-input" style="height:64px;padding:8px;resize:vertical;width:100%;">${escapeHtml(
                                    d.editingCommentDraft
                                  )}</textarea>
                                  <div id="mention-dropdown-edit_comment" class="mention-dropdown"></div>
                                  <div style="display:flex;justify-content:flex-end;gap:6px;margin-top:6px;">
                                    <button type="button" class="c-micro-action" data-cancel-edit-comment>Cancel</button>
                                    <button type="button" class="btn-primary" data-save-edit-comment="${escapeHtml(c.id)}" style="height:30px;padding:0 12px;font-size:11.5px;" ${
                                      d.submitting ? 'disabled' : ''
                                    }>Save</button>
                                  </div>
                                </div>
                              `
                              : soloEid
                              ? `
                                ${quoteButtonHtml(true)}
                                <div class="c-solo-emoji ${isCommentHighlighted ? 'highlight-pulse' : ''}">${renderCustomEmojiSlotHtml(soloEid, { lazy: false, sizeClass: 'size-solo' })}</div>
                                ${renderMentionsDisplayHtml(c.mentions)}
                              `
                              : isUnicodeSolo
                              ? `
                                ${quoteButtonHtml(true)}
                                <div class="c-solo-unicode-emoji ${isCommentHighlighted ? 'highlight-pulse' : ''}">${escapeHtml(rawBody.trim())}</div>
                                ${renderMentionsDisplayHtml(c.mentions)}
                              `
                              : `
                                <div class="c-bubble ${isCommentHighlighted ? 'highlight-pulse' : ''}">
                                  ${quoteButtonHtml(false)}
                                  <div class="c-bubble-body-text">${renderMentionHighlightedHtml(rawBody)}</div>
                                </div>
                                ${renderMentionsDisplayHtml(c.mentions)}
                              `
                          }
                        </div>
                      </div>
                    </div>
                  `;
                })
                .join('')
        }
      </div>

      ${
        state.session
          ? `
            <div class="chat-composer-dock">
              <div id="mention-dropdown-${activeScope}" class="mention-dropdown"></div>

              ${
                d.activeReplyTarget
                  ? `
                    <div class="chat-reply-banner">
                      <div class="chat-reply-banner-quote">
                        <span>Replying to <strong>@${escapeHtml(d.activeReplyTarget.senderName)}</strong></span>
                        ${
                          d.activeReplyTarget.previewText || d.activeReplyTarget.stickerCode
                            ? `<span class="chat-reply-banner-snippet">${renderQuotedPreviewContentHtml(d.activeReplyTarget.previewText, d.activeReplyTarget.stickerCode)}</span>`
                            : ''
                        }
                      </div>
                      <button type="button" class="c-micro-action" id="btn-cancel-reply-composer" style="color:var(--accent-primary);flex-shrink:0;">✕ Cancel</button>
                    </div>
                  `
                  : ''
              }

              ${activeTags.length > 0 ? `<div style="padding:4px 16px 2px;background:var(--surface-modal);">${renderSelectedTagChipsHtml(activeScope, activeTags)}</div>` : ''}
              ${renderComposerEmojiPreviewHtml(activeDraft)}
              ${renderEmojiTrayHtml(activeScope)}

              <div class="chat-input-bar">
                <div class="chat-input-wrap">
                  <button
                    type="button"
                    class="emoji-toggle-btn ${d.emojiTrayOpen ? 'active' : ''}"
                    id="btn-toggle-emoji-tray"
                    title="Custom Emojis"
                    aria-label="Toggle custom emojis"
                  >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <circle cx="12" cy="12" r="10"></circle>
                      <path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
                      <line x1="9" y1="9" x2="9.01" y2="9"></line>
                      <line x1="15" y1="9" x2="15.01" y2="9"></line>
                    </svg>
                  </button>
                  <textarea
                    id="inp-discussion-comment"
                    class="chat-input"
                    rows="1"
                    placeholder="${d.activeReplyTarget ? `Reply to @${escapeHtml(d.activeReplyTarget.senderName)}...` : 'Message...'}"
                    ${d.submitting ? 'disabled' : ''}
                  >${escapeHtml(activeDraft)}</textarea>
                </div>
                <button
                  type="button"
                  class="chat-send"
                  id="btn-post-discussion-comment"
                  aria-label="Send message"
                  ${d.submitting ? 'disabled' : ''}
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="22" y1="2" x2="11" y2="13"></line>
                    <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                  </svg>
                </button>
              </div>
            </div>
          `
          : `
            <div style="padding:12px 16px;border-top:1px solid var(--border-subtle);background:var(--bg-secondary);font-size:12.5px;color:var(--text-secondary);text-align:center;">
              Sign in to join Live Chat, send custom emojis, and tag members with @.
            </div>
          `
      }
    </div>
  `;
}

// ============================================================================
// SCREEN 8: NOTIFICATIONS SCREEN
// ============================================================================

function renderNotificationsScreen() {
  const nState = state.notifications;
  return `
    <div style="max-width:860px;margin:0 auto;display:flex;flex-direction:column;gap:16px;">
      <div style="display:flex;align-items:flex-end;justify-content:space-between;gap:12px;flex-wrap:wrap;">
        <div>
          <div class="micro-label" style="color:var(--accent-primary);">IN-APP NOTIFICATION CENTER</div>
          <h1 class="serif-heading" style="font-size:34px;line-height:38px;">Notifications</h1>
          <div style="font-size:13px;color:var(--text-secondary);margin-top:4px;">
            ${nState.unreadCount > 0 ? `${nState.unreadCount} unread notification${nState.unreadCount === 1 ? '' : 's'}` : 'All caught up'}
          </div>
        </div>
        <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
          <button type="button" class="btn-secondary" id="btn-open-notif-settings" style="height:40px;padding:0 14px;font-size:12.5px;">
            Notification Settings
          </button>
          <button type="button" class="btn-secondary" id="btn-notif-refresh" style="height:40px;padding:0 14px;font-size:12.5px;">Refresh</button>
          ${
            nState.unreadCount > 0
              ? `<button type="button" class="btn-primary" id="btn-notif-mark-all" style="height:40px;padding:0 16px;font-size:12.5px;">Mark all read</button>`
              : ''
          }
        </div>
      </div>

      ${nState.error ? `<div class="feedback-banner error">! ${escapeHtml(nState.error)}</div>` : ''}

      ${
        !state.session
          ? `<div class="app-card" style="border-radius:24px;text-align:center;padding:32px 20px;color:var(--text-secondary);">
               Sign in to receive messages, reminders, updates, @mentions, and thread reply notifications.
             </div>`
          : nState.loading
          ? `<div class="app-card" style="border-radius:24px;text-align:center;padding:32px 20px;color:var(--text-secondary);">
               Loading notifications...
             </div>`
          : nState.items.length === 0
          ? `<div class="app-card" style="border-radius:24px;text-align:center;padding:40px 20px;color:var(--text-secondary);">
               <div style="font-size:15px;font-weight:600;color:var(--text-primary);margin-bottom:6px;">No notifications yet</div>
               <div style="font-size:13px;">Messages, reminders, updates, @mentions, and thread replies will appear here in real time.</div>
             </div>`
          : `
            <div style="display:flex;flex-direction:column;gap:10px;">
              ${nState.items
                .map((n) => {
                  const nType = String(n.notification_type || n.type || 'message').toLowerCase();
                  const nPreview = formatPlainPreviewWithEmojis(n.message || n.preview_text || n.content_preview || '');
                  const badgeLabel =
                    nType === 'thread_reply' || nType === 'reply'
                      ? 'REPLY'
                      : nType === 'mention_reply' || nType === 'mention_comment' || nType === 'mention'
                      ? 'MENTION'
                      : nType === 'update'
                      ? 'UPDATE'
                      : nType === 'reminder'
                      ? 'REMINDER'
                      : 'MESSAGE';
                  const computedTitle =
                    n.title ||
                    (nType === 'thread_reply' || nType === 'reply'
                      ? `@${n.sender_username || 'Member'} replied to your thread${n.anime_title ? ` on ${n.anime_title}` : ''}`
                      : nType === 'mention_reply' || nType === 'mention_comment' || nType === 'mention'
                      ? `@${n.sender_username || 'Member'} mentioned you${n.anime_title ? ` on ${n.anime_title}` : ''}`
                      : 'Anime Universe Notification');
                  return `
                    <div
                      class="notification-item ${n.is_read ? '' : 'unread'}"
                      style="display:flex;align-items:flex-start;gap:12px;text-align:left;position:relative;"
                    >
                      <button
                        type="button"
                        data-open-notification="${escapeHtml(n.id)}"
                        data-notif-anime="${Number(n.anime_id || 0)}"
                        data-notif-comment="${escapeHtml(n.comment_id || '')}"
                        data-notif-thread="${escapeHtml(n.thread_id || '')}"
                        data-notif-reply="${escapeHtml(n.reply_id || '')}"
                        style="flex:1;min-width:0;display:flex;align-items:flex-start;gap:12px;background:transparent;border:none;padding:0;color:inherit;text-align:left;cursor:pointer;"
                      >
                        ${renderAvatarCircle(n.sender_avatar_key || 'avatar_1', n.sender_username || 'Anime Universe', 40)}
                        <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:5px;">
                          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
                            <span style="padding:2px 8px;border-radius:999px;font-size:10px;font-weight:700;letter-spacing:0.08em;background:rgba(232,200,143,0.14);color:var(--accent-primary);border:1px solid rgba(232,200,143,0.3);">
                              ${escapeHtml(badgeLabel)}
                            </span>
                            <span style="font-size:11.5px;color:var(--text-muted);">${escapeHtml(formatDiscussionTimestamp(n.created_at))}</span>
                            ${!n.is_read ? '<span class="notification-unread-dot" title="Unread" style="position:static;margin-left:2px;"></span>' : ''}
                          </div>
                          <div style="font-size:14px;font-weight:600;color:var(--text-primary);line-height:1.4;">
                            ${escapeHtml(computedTitle)}
                          </div>
                          ${
                            nPreview
                              ? `<div style="font-size:13px;color:var(--text-secondary);line-height:1.45;">${escapeHtml(nPreview)}</div>`
                              : ''
                          }
                        </div>
                      </button>
                      <div style="display:flex;align-items:center;gap:6px;flex-shrink:0;">
                        ${
                          !n.is_read
                            ? `<button
                                 type="button"
                                 class="capsule-pill"
                                 data-mark-read-notif="${escapeHtml(n.id)}"
                                 title="Mark as read"
                                 style="height:30px;padding:0 10px;font-size:11px;"
                               >
                                 Mark read
                               </button>`
                            : ''
                        }
                        <button
                          type="button"
                          class="app-icon-btn"
                          data-delete-notif="${escapeHtml(n.id)}"
                          title="Delete notification"
                          aria-label="Delete notification"
                          style="width:32px;height:32px;font-size:13px;color:var(--text-muted);"
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  `;
                })
                .join('')}
            </div>
            ${
              nState.hasMore
                ? `<button type="button" class="btn-secondary" id="btn-notif-load-more" style="width:100%;" ${nState.loadingMore ? 'disabled' : ''}>
                     ${nState.loadingMore ? 'LOADING...' : 'LOAD MORE NOTIFICATIONS'}
                   </button>`
                : ''
            }
          `
      }
    </div>
  `;
}

// ============================================================================
// AUTH SCREEN WITH MASCOT (Matches Screenshot 1 1:1)
// ============================================================================

function renderAuthScreen() {
  const isSignup = state.authMode === 'signup';
  const mascotSrc = getMascotImgUrl();
  return `
    <div class="auth-viewport">
      <div class="auth-mascot-stack">
        <img class="auth-mascot-img" src="${mascotSrc}" alt="" onerror="${AVATAR_IMG_ONERROR}" />
        <div class="glass-auth-card">
          ${
            state.authMode === 'verify_otp'
              ? `
                <div style="display:flex;flex-direction:column;align-items:center;gap:12px;">
                  <div style="width:48px;height:48px;border-radius:50%;background:rgba(216,183,120,0.14);border:1px solid var(--accent-border);display:flex;align-items:center;justify-content:center;color:var(--accent-primary);">
                    <svg viewBox="0 0 24 24" fill="currentColor" style="width:22px;height:22px;"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                  </div>
                  <h2 class="serif-heading" style="font-size:28px;text-align:center;">Check Your Inbox</h2>
                  <div style="padding:5px 14px;border-radius:999px;background:rgba(216,183,120,0.12);border:1px solid var(--accent-border);color:var(--highlight);font-size:12.5px;font-weight:600;">
                    ${escapeHtml(maskEmailAddress(state.pendingVerifyEmail || state.identifier))}
                  </div>
                  <p style="font-size:13px;color:var(--text-secondary);text-align:center;line-height:1.5;">
                    Enter the 8-digit verification code from your email to continue.
                  </p>
                </div>

                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                ${state.authInfo ? `<div style="color:var(--accent-primary);font-size:12.5px;text-align:center;">${escapeHtml(state.authInfo)}</div>` : ''}

                <label class="auth-field-label">8-Digit Verification Code</label>
                <div class="auth-input-wrap">
                  <span class="auth-input-icon">${SVG_ICONS.key}</span>
                  <input
                    id="inp-signup-otp"
                    class="auth-input with-left-icon tabular-num"
                    type="text"
                    inputmode="numeric"
                    maxlength="8"
                    placeholder="ex-12345678"
                    value="${escapeHtml(state.signupOtpCode)}"
                    style="letter-spacing:3px;font-weight:700;font-size:16px;"
                  />
                </div>

                <button class="btn-primary" id="btn-verify-signup-otp" style="width:100%;margin-top:4px;" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'VERIFYING...' : 'CONFIRM & CONTINUE'}
                </button>

                <div style="display:flex;gap:10px;width:100%;">
                  <button type="button" class="btn-secondary" id="btn-resend-signup-otp" style="flex:1;height:42px;font-size:12.5px;" ${state.loading ? 'disabled' : ''}>
                    Resend Code
                  </button>
                  <button type="button" class="btn-secondary" id="btn-change-signup-email" style="flex:1;height:42px;font-size:12.5px;" ${state.loading ? 'disabled' : ''}>
                    Change Email
                  </button>
                </div>
              `
              : state.authMode === 'forgot_otp'
              ? `
                <h2 class="serif-heading" style="font-size:26px;text-align:center;">Reset Password</h2>
                <p style="font-size:12.5px;color:var(--text-secondary);text-align:center;line-height:1.5;">
                  Enter the 8-digit recovery code sent to <strong>${escapeHtml(maskEmailAddress(state.pendingVerifyEmail || state.identifier))}</strong> and choose a new password.
                </p>
                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                ${state.authInfo ? `<div style="color:var(--accent-primary);font-size:12.5px;text-align:center;">${escapeHtml(state.authInfo)}</div>` : ''}

                <label class="auth-field-label">8-Digit Recovery Code</label>
                <input id="inp-reset-otp" class="auth-input tabular-num" type="text" inputmode="numeric" maxlength="8" placeholder="ex-12345678" value="${escapeHtml(state.resetOtpCode)}" style="letter-spacing:3px;font-weight:700;" />

                <label class="auth-field-label">New Password (8+ characters)</label>
                <input id="inp-reset-new-pass" class="auth-input" type="password" placeholder="••••••••" value="${escapeHtml(state.resetNewPassword)}" />

                <button class="btn-primary" id="btn-confirm-reset-otp" style="width:100%;margin-top:4px;" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'UPDATING...' : 'VERIFY & UPDATE PASSWORD'}
                </button>
                <button type="button" class="btn-secondary" id="btn-cancel-reset-otp" style="width:100%;height:42px;font-size:12.5px;">
                  Back to Sign In
                </button>
              `
              : state.authMode === 'onboard_username'
              ? `
                <div class="micro-label" style="color:var(--accent-primary);text-align:center;">STEP 1 OF 2 • USERNAME</div>
                <h2 class="serif-heading" style="font-size:26px;text-align:center;">Choose Your Username</h2>
                <p style="font-size:12.5px;color:var(--text-secondary);text-align:center;">Pick a unique display name so your email stays private in Community.</p>
                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                ${state.authInfo ? `<div style="color:var(--accent-primary);font-size:12.5px;text-align:center;">${escapeHtml(state.authInfo)}</div>` : ''}
                <input id="inp-onboard-user" class="auth-input" maxlength="32" placeholder="Choose a username (e.g. GojoFan)" value="${escapeHtml(state.onboardUsername)}" />
                <button class="btn-primary" id="btn-onboard-next" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'CHECKING...' : 'CONTINUE TO PROFILE PICTURE ›'}
                </button>
              `
              : state.authMode === 'onboard_avatar'
              ? `
                <div class="micro-label" style="color:var(--accent-primary);text-align:center;">STEP 2 OF 2 • PROFILE PICTURE</div>
                <h2 class="serif-heading" style="font-size:26px;text-align:center;">Choose Your Profile Picture</h2>
                <p style="font-size:12.5px;color:var(--text-secondary);text-align:center;">Pick an avatar for <strong style="color:var(--text-primary);">${escapeHtml(state.onboardUsername)}</strong>.</p>
                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                <div class="round-avatar-carousel">
                  ${BUILT_IN_AVATARS.map(
                    (av) => `
                    <div class="avatar-carousel-item ${state.onboardAvatarKey === av.key ? 'selected' : ''}" data-onboard-avatar="${av.key}">
                      <div class="avatar-ring-wrap" style="background:linear-gradient(135deg,${av.c1},${av.c2});">
                        <img src="${av.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" />
                        ${state.onboardAvatarKey === av.key ? `<div class="avatar-check-badge">✓</div>` : ''}
                      </div>
                      <span style="font-size:11px;color:${state.onboardAvatarKey === av.key ? 'var(--highlight)' : 'var(--text-secondary)'};text-align:center;">
                        ${escapeHtml(av.name)}
                      </span>
                    </div>`
                  ).join('')}
                </div>
                <button class="btn-primary" id="btn-onboard-finish" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'SAVING PROFILE...' : 'ENTER ANIME UNIVERSE'}
                </button>
                <button type="button" class="btn-secondary" id="btn-onboard-back" style="width:100%;height:42px;font-size:12.5px;" ${state.loading ? 'disabled' : ''}>
                  ‹ Change Username
                </button>
              `
              : `
                ${state.authError ? `<div style="color:var(--error);font-size:12.5px;text-align:center;">${escapeHtml(state.authError)}</div>` : ''}
                ${state.authInfo ? `<div style="color:var(--accent-primary);font-size:12.5px;text-align:center;">${escapeHtml(state.authInfo)}</div>` : ''}

                <label class="auth-field-label">${isSignup ? 'Email Address' : 'Email or Username'}</label>
                <div class="auth-input-wrap">
                  <span class="auth-input-icon">${SVG_ICONS.profile}</span>
                  <input id="inp-identifier" class="auth-input with-left-icon" type="text" placeholder="${isSignup ? 'you@example.com' : 'you@example.com or username'}" value="${escapeHtml(state.identifier)}" />
                </div>

                <label class="auth-field-label">Password</label>
                <div class="auth-input-wrap">
                  <span class="auth-input-icon">${SVG_ICONS.key}</span>
                  <input id="inp-pass" class="auth-input with-left-icon with-right-icon" type="${state.showPassword ? 'text' : 'password'}" placeholder="••••••••" value="${escapeHtml(state.password)}" />
                  <button type="button" class="auth-eye-btn" id="btn-toggle-pass" title="Toggle Password Visibility">
                    ${SVG_ICONS.eye}
                  </button>
                </div>

                ${
                  isSignup
                    ? `
                      <label class="auth-field-label">Confirm Password</label>
                      <div class="auth-input-wrap">
                        <span class="auth-input-icon">${SVG_ICONS.key}</span>
                        <input id="inp-confirm-pass" class="auth-input with-left-icon" type="${state.showPassword ? 'text' : 'password'}" placeholder="Re-enter password" value="${escapeHtml(state.confirmPassword)}" />
                      </div>
                    `
                    : `
                      <div class="auth-forgot-row">
                        <button type="button" class="auth-forgot-btn" id="btn-forgot-pass">Forgot Password?</button>
                      </div>
                    `
                }

                <button class="btn-primary" id="btn-auth-submit" style="width:100%;margin-top:4px;" ${state.loading ? 'disabled' : ''}>
                  ${state.loading ? 'PLEASE WAIT...' : isSignup ? 'CREATE ACCOUNT' : 'SIGN IN'}
                </button>
              `
          }
        </div>

        ${
          state.authMode === 'signin' || state.authMode === 'signup'
            ? `
              <div class="auth-footer-links">
                <div style="font-size:14.5px;color:var(--text-secondary);">
                  ${isSignup ? 'Already have an account?' : "Don't have an account?"}
                  <button type="button" id="btn-switch-auth-mode" style="background:none;border:none;color:var(--accent-primary);font-weight:700;font-size:14.5px;margin-left:6px;cursor:pointer;font-family:inherit;">
                    ${isSignup ? 'Sign In' : 'Sign Up'}
                  </button>
                </div>
                <button type="button" id="btn-guest-preview" style="background:none;border:none;color:var(--text-secondary);font-size:13.5px;cursor:pointer;font-family:inherit;">
                  Continue without an account
                </button>
              </div>
            `
            : ''
        }
      </div>
    </div>
  `;
}

// ============================================================================
// SCREEN 8: DOWNLOAD APPLICATIONS CENTER (Android APK, Windows EXE, macOS DMG)
// ============================================================================

function resolveHttpsDownloadUrl(url) {
  if (!url || typeof url !== 'string') return '';
  const trimmed = url.trim();
  if (!trimmed) return '';
  if (trimmed.includes('<') || trimmed.includes('>') || trimmed.includes('example.com')) return '';
  // Automatically convert any Google Drive /file/d/<ID>/view share URL into a direct binary stream URL
  const gdriveMatch =
    trimmed.match(/drive\.google\.com\/file\/d\/([a-zA-Z0-9_-]+)/i) ||
    trimmed.match(/drive\.google\.com\/open\?id=([a-zA-Z0-9_-]+)/i) ||
    trimmed.match(/drive\.google\.com\/uc\?.*id=([a-zA-Z0-9_-]+)/i);
  if (gdriveMatch && gdriveMatch[1]) {
    return `https://drive.usercontent.google.com/download?id=${gdriveMatch[1]}&export=download&confirm=t`;
  }
  if (trimmed.startsWith('https://')) return trimmed;
  if (trimmed.startsWith('/')) {
    try {
      const resolved = new URL(trimmed, window.location.origin).href;
      if (
        resolved.startsWith('https://') ||
        window.location.hostname === 'localhost' ||
        window.location.hostname === '127.0.0.1'
      ) {
        return resolved;
      }
    } catch (_) {}
  }
  return '';
}

function isHttpsUrlReady(url) {
  return Boolean(resolveHttpsDownloadUrl(url));
}

function isPlatformDownloadReady(plat) {
  if (!plat) return false;
  if (plat.available === false) return false;
  return isHttpsUrlReady(plat.downloadUrl);
}

function detectVisitorPlatformKey() {
  const ua = (navigator.userAgent || '').toLowerCase();
  if (ua.includes('android')) return 'android';
  if (ua.includes('win')) return 'windows';
  if (ua.includes('mac') && !ua.includes('iphone') && !ua.includes('ipad')) return 'macos';
  return null;
}

function mergePlatformConfig(basePlat = {}, overridePlat = {}) {
  const merged = { ...basePlat, ...overridePlat };
  if (!isHttpsUrlReady(overridePlat.downloadUrl) && isHttpsUrlReady(basePlat.downloadUrl)) {
    merged.downloadUrl = basePlat.downloadUrl;
    merged.available = basePlat.available !== false;
    merged.fileSize = basePlat.fileSize || merged.fileSize;
    merged.fileName = basePlat.fileName || merged.fileName;
  }
  return merged;
}

async function loadDownloadsConfig() {
  if (isTauriAppEnvironment()) return;
  try {
    const resp = await fetch(`/downloads-config.json?_t=${Date.now()}`, { cache: 'no-store' });
    if (resp.ok) {
      const fileCfg = await resp.json();
      state.downloadsConfig = {
        ...state.downloadsConfig,
        ...fileCfg,
        platforms: {
          android: mergePlatformConfig(state.downloadsConfig.platforms?.android, fileCfg.platforms?.android),
          windows: mergePlatformConfig(state.downloadsConfig.platforms?.windows, fileCfg.platforms?.windows),
          macos: mergePlatformConfig(state.downloadsConfig.platforms?.macos, fileCfg.platforms?.macos)
        }
      };
    }
  } catch (_) {}

  if (state.tab === 'downloads') renderApp();
}

async function startPlatformDownload(taskKey, downloadUrl, fileName) {
  const resolvedUrl = resolveHttpsDownloadUrl(downloadUrl);
  if (!resolvedUrl) return;
  if (state.downloadTasks[taskKey]?.active) return;

  state.downloadTasks[taskKey] = {
    active: true,
    progress: 12,
    loadedMB: '0.0',
    totalMB: null,
    statusText: 'Preparing download...'
  };
  renderApp();

  try {
    const isSameOrigin = resolvedUrl.startsWith(window.location.origin);
    if (isSameOrigin) {
      const response = await fetch(resolvedUrl, { mode: 'same-origin', credentials: 'omit' });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const contentType = (response.headers.get('content-type') || '').toLowerCase();
      if (contentType.includes('text/html')) {
        throw new Error('Package binary not found at endpoint');
      }

      const contentLength = Number(response.headers.get('content-length') || 0);
      const totalMB = contentLength > 0 ? (contentLength / (1024 * 1024)).toFixed(1) : null;

      if (response.body && typeof response.body.getReader === 'function') {
        const reader = response.body.getReader();
        const chunks = [];
        let receivedLength = 0;
        let lastRender = 0;

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          if (value) {
            chunks.push(value);
            receivedLength += value.length;
            const loadedMB = (receivedLength / (1024 * 1024)).toFixed(1);
            const pct = contentLength > 0
              ? Math.min(99, Math.max(5, Math.round((receivedLength / contentLength) * 100)))
              : Math.min(92, (state.downloadTasks[taskKey].progress || 10) + 7);

            state.downloadTasks[taskKey] = {
              active: true,
              progress: pct,
              loadedMB,
              totalMB,
              statusText: totalMB ? `Downloading package... ${loadedMB} MB / ${totalMB} MB` : `Downloading package... ${loadedMB} MB`
            };

            const now = Date.now();
            if (now - lastRender > 120) {
              lastRender = now;
              renderApp();
            }
          }
        }

        const blob = new Blob(chunks, { type: response.headers.get('content-type') || 'application/octet-stream' });
        const blobUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = fileName || downloadUrl.split('/').pop() || 'AnimeUniverse-Package';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(() => URL.revokeObjectURL(blobUrl), 15000);

        state.downloadTasks[taskKey] = {
          active: false,
          progress: 100,
          loadedMB: totalMB || (receivedLength / (1024 * 1024)).toFixed(1),
          totalMB,
          statusText: 'Download complete ✓'
        };
        renderApp();
        return;
      }
    }
  } catch (_) {
    // Fall through to native browser download
  }

  state.downloadTasks[taskKey] = {
    active: true,
    progress: 70,
    loadedMB: null,
    totalMB: null,
    statusText: 'Starting download...'
  };
  renderApp();

  const a = document.createElement('a');
  a.href = resolvedUrl;
  a.download = fileName || resolvedUrl.split('/').pop() || 'AnimeUniverse-Package';
  a.target = '_blank';
  a.rel = 'noopener noreferrer';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);

  await new Promise((r) => setTimeout(r, 320));
  state.downloadTasks[taskKey] = {
    active: false,
    progress: 100,
    loadedMB: null,
    totalMB: null,
    statusText: 'Download started ✓'
  };
  renderApp();
}

function renderDownloadsScreen() {
  if (isTauriAppEnvironment()) {
    return renderHomeScreen();
  }
  const cfg = state.downloadsConfig || {};
  const platforms = cfg.platforms || {};
  const visitorOs = detectVisitorPlatformKey();

  const topBar = renderObsidianTopBar({
    title: 'Downloads',
    subtitle: 'OFFICIAL NATIVE APPLICATIONS',
    leftActionHtml: `<button class="app-icon-btn" data-nav="home" title="Back to Home" style="margin-right:6px;">${SVG_ICONS.back}</button>`
  });

  const platformOrder = [
    { key: 'android', icon: SVG_ICONS.android, badgeLabel: 'MOBILE & TABLET' },
    { key: 'windows', icon: SVG_ICONS.windows, badgeLabel: 'WINDOWS PC' },
    { key: 'macos', icon: SVG_ICONS.apple, badgeLabel: 'APPLE MAC' }
  ];

  return `
    ${topBar}

    <!-- Three Platform Download Cards (Android APK, Windows EXE, macOS DMG) -->
    <div class="downloads-cards-grid">
      ${platformOrder
        .map(({ key, icon, badgeLabel }) => {
          const p = platforms[key] || {};
          const ready = isPlatformDownloadReady(p);
          const isRecommended = visitorOs === key;
          const taskKey = `main_${key}`;
          const task = state.downloadTasks[taskKey];

          return `
            <div class="download-platform-card ${isRecommended ? 'recommended' : ''}">
              ${isRecommended ? `<div class="recommended-ribbon">Recommended for Your Device</div>` : ''}

              <div style="display:flex;flex-direction:column;gap:14px;">
                <div style="display:flex;align-items:center;gap:14px;">
                  <div class="platform-emblem ${key}">
                    ${icon}
                  </div>
                  <div style="min-width:0;flex:1;">
                    <div class="micro-label" style="color:var(--accent-primary);margin-bottom:3px;">${escapeHtml(badgeLabel)}</div>
                    <h3 class="serif-heading" style="font-size:25px;line-height:28px;color:var(--text-primary);">
                      ${escapeHtml(p.platformName || key.toUpperCase())}
                    </h3>
                    <div style="font-size:12.5px;color:var(--text-secondary);margin-top:2px;">
                      ${escapeHtml(p.appName || 'Anime Universe')}
                    </div>
                  </div>
                </div>

                <!-- Supported Operating System -->
                <div class="download-os-box">
                  <span class="download-meta-label">Supported Operating System</span>
                  <span style="font-size:12.5px;color:var(--text-secondary);line-height:1.45;">
                    ${escapeHtml(p.osSupported || '')}
                  </span>
                </div>
              </div>

              <!-- Download Action + Progress Indicator -->
              <div style="display:flex;flex-direction:column;gap:10px;">
                ${
                  task
                    ? `
                      <div class="download-progress-wrap">
                        <div style="display:flex;justify-content:space-between;align-items:center;font-size:11.5px;">
                          <span style="color:var(--text-primary);font-weight:600;">${escapeHtml(task.statusText)}</span>
                          <span class="tabular-num" style="color:var(--highlight);font-weight:700;">${task.progress}%</span>
                        </div>
                        <div class="download-progress-track">
                          <div class="download-progress-bar" style="width:${task.progress}%;"></div>
                        </div>
                      </div>
                    `
                    : ''
                }

                ${
                  ready
                    ? `
                      <button
                        class="btn-primary"
                        data-trigger-download="${taskKey}"
                        data-dl-url="${escapeHtml(p.downloadUrl)}"
                        data-dl-filename="${escapeHtml(p.fileName || '')}"
                        style="width:100%;"
                        ${task?.active ? 'disabled' : ''}
                      >
                        <span style="width:18px;height:18px;display:inline-flex;">${SVG_ICONS.download}</span>
                        <span>${task?.active ? 'DOWNLOADING...' : `DOWNLOAD ${escapeHtml((p.platformName || '').toUpperCase())}`}</span>
                      </button>
                      <div style="display:flex;align-items:center;justify-content:center;gap:6px;font-size:11px;color:var(--text-muted);">
                        <span style="width:13px;height:13px;display:inline-flex;color:var(--success);">${SVG_ICONS.checkCircle}</span>
                        <span>${escapeHtml(p.fileName || p.packageType || 'Official Build')}</span>
                      </div>
                    `
                    : `
                      <div class="btn-coming-soon">
                        <span style="width:17px;height:17px;display:inline-flex;">${SVG_ICONS.clock}</span>
                        <span>COMING SOON</span>
                      </div>
                      <div style="text-align:center;font-size:11px;color:var(--text-muted);">
                        Official ${escapeHtml(p.platformName || '')} package will unlock automatically once published.
                      </div>
                    `
                }
              </div>
            </div>
          `;
        })
        .join('')}
    </div>
  `;
}



// ============================================================================
// MODALS: QUICK CONTEXT EDIT, COLLECTION MODAL & AVATAR CAROUSEL MODAL
// ============================================================================

function findAnimeByIdAnywhere(animeId) {
  const aid = Number(animeId);
  return (
    state.vault.find((v) => Number(v.animeId) === aid) ||
    state.discover.find((d) => Number(d.animeId) === aid) ||
    (state.detailAnime && Number(state.detailAnime.animeId) === aid ? state.detailAnime : null)
  );
}

function renderContextMenuOverlay() {
  if (!state.contextMenu) return '';
  const { animeId, x, y } = state.contextMenu;
  const item = findAnimeByIdAnywhere(animeId);
  if (!item) return '';
  const title = resolveTitle(item);
  const onHome = isAnimeOnHome(item);
  const menuW = 248;
  const menuH = 264;
  const safeX = Math.max(12, Math.min(Number(x || 20), (window.innerWidth || 1024) - menuW - 12));
  const safeY = Math.max(12, Math.min(Number(y || 20), (window.innerHeight || 768) - menuH - 12));

  return `
    <div class="anime-context-backdrop" id="anime-context-backdrop">
      <div class="anime-context-menu" id="anime-context-menu" style="left:${safeX}px;top:${safeY}px;" onclick="event.stopPropagation()">
        <div class="anime-context-header">
          ${item.coverImage ? `<img src="${escapeHtml(item.coverImage)}" alt="" />` : ''}
          <div class="anime-context-title">${escapeHtml(title)}</div>
        </div>
        ${
          onHome
            ? `<button type="button" class="anime-context-item danger" data-ctx-action="remove_home" data-ctx-anime-id="${animeId}">
                 <span>✕</span><span>Remove from Home</span>
               </button>`
            : `<button type="button" class="anime-context-item accent" data-ctx-action="add_home" data-ctx-anime-id="${animeId}">
                 <span>＋</span><span>Add to Home</span>
               </button>`
        }
        <button type="button" class="anime-context-item" data-ctx-action="view_details" data-ctx-anime-id="${animeId}">
          <span>ⓘ</span><span>View Anime Details</span>
        </button>
        <button type="button" class="anime-context-item" data-ctx-action="view_library" data-ctx-anime-id="${animeId}">
          <span>▤</span><span>View in Library</span>
        </button>
        <button type="button" class="anime-context-item" data-ctx-action="view_community" data-ctx-anime-id="${animeId}">
          <span>✦</span><span>View Community</span>
        </button>
      </div>
    </div>
  `;
}

function renderCollectionContextMenuOverlay() {
  if (!state.collectionContextMenu) return '';
  const { id, name, count, x, y } = state.collectionContextMenu;
  const menuW = 248;
  const menuH = 148;
  const safeX = Math.max(12, Math.min(Number(x || 20), (window.innerWidth || 1024) - menuW - 12));
  const safeY = Math.max(12, Math.min(Number(y || 20), (window.innerHeight || 768) - menuH - 12));

  return `
    <div class="anime-context-backdrop" id="collection-context-backdrop">
      <div class="anime-context-menu" id="collection-context-menu" style="left:${safeX}px;top:${safeY}px;" onclick="event.stopPropagation()">
        <div class="anime-context-header">
          <div style="width:32px;height:32px;border-radius:10px;background:rgba(216,183,120,0.16);display:grid;place-items:center;color:var(--accent-primary);flex-shrink:0;">
            ${SVG_ICONS.collectionIcon}
          </div>
          <div style="min-width:0;">
            <div class="anime-context-title">${escapeHtml(name || 'Category')}</div>
            <div style="font-size:11px;color:var(--text-muted);">${Number(count || 0)} anime in category</div>
          </div>
        </div>
        <button type="button" class="anime-context-item danger" id="btn-ctx-delete-collection" data-delete-col-id="${escapeHtml(id)}">
          <span>🗑</span><span>Delete Category</span>
        </button>
        <button type="button" class="anime-context-item" id="btn-ctx-cancel-collection">
          <span>✕</span><span>Cancel</span>
        </button>
      </div>
    </div>
  `;
}

function renderInAppNotifBannerOverlay() {
  const b = state.inAppNotifBanner;
  if (!b) return '';
  return `
    <div id="in-app-notif-banner" class="in-app-notif-banner" role="alert">
      <div style="flex-shrink:0;">
        ${renderAvatarCircle(b.senderAvatar || 'avatar_1', b.senderName || 'Anime Universe', 36)}
      </div>
      <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:2px;">
        <div style="font-size:11px;font-weight:700;color:var(--accent-primary);letter-spacing:0.06em;text-transform:uppercase;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
          ${escapeHtml(b.title || 'Anime Universe')}
        </div>
        <div style="font-size:13px;font-weight:500;color:var(--text-primary);line-height:1.35;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
          ${escapeHtml(b.body || 'New community notification')}
        </div>
      </div>
      <button type="button" class="in-app-notif-close" id="btn-dismiss-in-app-notif" aria-label="Dismiss notification">✕</button>
    </div>
  `;
}

function renderToastOverlay() {
  if (!state.toastMessage) return '';
  return `<div id="au-toast-banner" class="au-toast ${state.toastError ? 'error' : ''}">${escapeHtml(state.toastMessage)}</div>`;
}

function getFirstTimeNotifPopupFooterHtml() {
  const dr = state.deviceRegistration || {};
  const isDenied = Boolean(state.firstTimeNotifDeniedGuide || state.osNotifPermission === 'denied');
  const sysGuideText =
    dr.platform === 'android'
      ? 'Open Android Settings → Apps → Anime Universe → Notifications → Turn ON "Allow notifications". (If the toggle is disabled on an older APK, install the latest APK release.)'
      : dr.platform === 'macos'
      ? 'Open macOS System Settings → Notifications → Anime Universe → Turn ON "Allow Notifications".'
      : dr.platform === 'windows'
      ? 'Open Windows Settings → System → Notifications → Turn ON notifications for Anime Universe.'
      : 'Open your browser or OS notification settings and allow notifications for Anime Universe.';

  if (isDenied) {
    return `
      <div class="notif-perm-denied-box">
        <div style="font-size:12.5px;font-weight:700;color:#E57373;margin-bottom:4px;">
          App does not have OS notification permission
        </div>
        <div style="font-size:12px;color:var(--text-secondary);line-height:1.5;">
          ${escapeHtml(sysGuideText)}
        </div>
      </div>
      <div class="notif-perm-actions">
        <button type="button" class="btn-primary notif-perm-btn-primary" id="btn-notif-popup-open-settings">
          Open System Settings
        </button>
        <button type="button" class="btn-secondary notif-perm-btn-secondary" id="btn-notif-popup-maybe-later">
          Maybe Later
        </button>
      </div>
    `;
  }

  return `
    <div class="notif-perm-actions">
      <button
        type="button"
        class="btn-primary notif-perm-btn-primary"
        id="btn-notif-popup-enable"
        ${state.notifPopupRequesting ? 'disabled' : ''}
      >
        ${state.notifPopupRequesting ? 'Requesting OS Permission...' : 'Enable Notifications'}
      </button>
      <button
        type="button"
        class="btn-secondary notif-perm-btn-secondary"
        id="btn-notif-popup-maybe-later"
        ${state.notifPopupRequesting ? 'disabled' : ''}
      >
        Maybe Later
      </button>
    </div>
  `;
}

function bindFirstTimeNotifPopupEvents(backdropEl) {
  if (!backdropEl) return;
  backdropEl.onclick = (e) => {
    if (e.target === backdropEl && !state.notifPopupRequesting) {
      NativeNotificationBridge.handleFirstTimeMaybeLaterClicked();
    }
  };
  const btnEnable = backdropEl.querySelector('#btn-notif-popup-enable');
  if (btnEnable) {
    btnEnable.onclick = (e) => {
      e.preventDefault();
      e.stopPropagation();
      NativeNotificationBridge.handleFirstTimeEnableClicked();
    };
  }
  const btnMaybeLater = backdropEl.querySelector('#btn-notif-popup-maybe-later');
  if (btnMaybeLater) {
    btnMaybeLater.onclick = (e) => {
      e.preventDefault();
      e.stopPropagation();
      NativeNotificationBridge.handleFirstTimeMaybeLaterClicked();
    };
  }
  const btnOpenSettings = backdropEl.querySelector('#btn-notif-popup-open-settings');
  if (btnOpenSettings) {
    btnOpenSettings.onclick = (e) => {
      e.preventDefault();
      e.stopPropagation();
      state.notifPopupRequesting = false;
      state.showFirstTimeNotifPopup = false;
      state.firstTimeNotifDeniedGuide = false;
      state.notificationSettings.onboarding_dismissed = true;
      try { localStorage.setItem('au_notif_onboarding_dismissed_v1', 'true'); } catch (_) {}
      syncFirstTimeNotifPopupOverlayDOM();
      NativeNotificationBridge.openOsNotificationSettings();
    };
  }
}

function syncFirstTimeNotifPopupOverlayDOM(forceHide = false) {
  if (typeof document === 'undefined' || !document.body) return;
  const existing = document.getElementById('first-time-notif-popup');
  if (forceHide || !state.showFirstTimeNotifPopup) {
    if (existing) existing.remove();
    return;
  }

  const isDenied = Boolean(state.firstTimeNotifDeniedGuide || state.osNotifPermission === 'denied');
  const stateKey = `${isDenied ? 'denied' : 'prompt'}:${state.notifPopupRequesting ? '1' : '0'}:${state.deviceRegistration?.platform || ''}`;

  if (existing) {
    if (existing.getAttribute('data-popup-state') === stateKey) {
      return;
    }
    existing.setAttribute('data-popup-state', stateKey);
    const footerEl = existing.querySelector('#notif-popup-dynamic-footer');
    if (footerEl) {
      footerEl.innerHTML = getFirstTimeNotifPopupFooterHtml();
    }
    bindFirstTimeNotifPopupEvents(existing);
    return;
  }

  const backdrop = document.createElement('div');
  backdrop.className = 'notif-perm-popup-backdrop';
  backdrop.id = 'first-time-notif-popup';
  backdrop.setAttribute('role', 'dialog');
  backdrop.setAttribute('aria-modal', 'true');
  backdrop.setAttribute('aria-labelledby', 'notif-popup-heading');
  backdrop.setAttribute('data-popup-state', stateKey);
  backdrop.innerHTML = `
    <div class="notif-perm-popup-card" onclick="event.stopPropagation()">
      <div class="notif-perm-popup-glow"></div>
      <div class="notif-perm-bell-stage">
        <div class="notif-perm-bell-ring ring-1"></div>
        <div class="notif-perm-bell-ring ring-2"></div>
        <div class="notif-perm-bell-circle">
          <svg class="notif-perm-bell-svg" width="34" height="34" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M12 22C13.38 22 14.5 20.88 14.5 19.5H9.5C9.5 20.88 10.62 22 12 22Z" fill="currentColor"/>
            <path d="M18.5 16V10.8C18.5 7.43 16.42 4.62 13.5 3.88V3.25C13.5 2.42 12.83 1.75 12 1.75C11.17 1.75 10.5 2.42 10.5 3.25V3.88C7.58 4.62 5.5 7.43 5.5 10.8V16L3.5 18V19H20.5V18L18.5 16Z" fill="currentColor"/>
          </svg>
          <span class="notif-perm-bell-sparkle">✦</span>
        </div>
      </div>

      <div class="micro-label" style="color:var(--accent-primary);letter-spacing:0.16em;">REAL-TIME ALERTS</div>
      <h2 id="notif-popup-heading" class="serif-heading" style="font-size:28px;line-height:1.15;color:var(--text-primary);margin:2px 0 4px;">
        Stay in the Loop!
      </h2>
      <p style="font-size:14px;color:var(--text-secondary);line-height:1.58;margin:0;">
        Enable notifications to receive important messages, reminders and updates, even when you're away.
      </p>

      <div id="notif-popup-dynamic-footer" style="width:100%;">
        ${getFirstTimeNotifPopupFooterHtml()}
      </div>
    </div>
  `;

  bindFirstTimeNotifPopupEvents(backdrop);
  document.body.appendChild(backdrop);
}

function renderModals() {
  let overlays = `${renderContextMenuOverlay()}${renderCollectionContextMenuOverlay()}${renderInAppNotifBannerOverlay()}${renderToastOverlay()}`;

  if (state.confirmRemoveHomeAnime) {
    const target = state.confirmRemoveHomeAnime;
    return `
      ${overlays}
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet" style="max-width:440px;" onclick="event.stopPropagation()">
          <h3 class="serif-heading" style="font-size:26px;color:var(--text-primary);">Remove from Home?</h3>
          ${target.title ? `<div style="font-size:13px;font-weight:700;color:var(--accent-primary);margin-top:-6px;">${escapeHtml(target.title)}</div>` : ''}
          <p style="font-size:14px;color:var(--text-secondary);line-height:1.6;">
            This anime will be removed from your Home tracking list but will remain in your Library. Your notes, ratings, reviews, and Community discussions will not be affected.
          </p>
          <div style="display:flex;gap:10px;margin-top:6px;">
            <button type="button" class="btn-secondary" id="btn-cancel-remove-home" style="flex:1;">Cancel</button>
            <button type="button" class="btn-danger" id="btn-confirm-remove-home" style="flex:1;">Remove from Home</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.quickEditAnime) {
    const a = state.quickEditAnime;
    const title = resolveTitle(a);
    const stars = rating10ToStars(a.rating10) || 0;
    return `
      ${overlays}
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet" onclick="event.stopPropagation()">
          <div style="display:flex;gap:12px;align-items:center;">
            <img src="${escapeHtml(a.coverImage)}" alt="" style="width:54px;height:78px;border-radius:14px;object-fit:cover;" />
            <div style="flex:1;min-width:0;">
              <div class="serif-heading" style="font-size:22px;">${escapeHtml(title)}</div>
              <div class="tabular-num" style="font-size:12px;color:var(--text-secondary);">Of ${a.totalEpisodes || '∞'} Episodes</div>
            </div>
          </div>

          <div class="micro-label" style="color:var(--accent-primary);">EPISODE PROGRESS</div>
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="stepper-circle-btn" id="qe-dec" style="width:40px;height:40px;">−</button>
            <input id="qe-ep-input" class="auth-input tabular-num" type="number" min="0" value="${a.watchedEpisodes || 0}" style="text-align:center;font-size:18px;font-weight:700;" />
            <button class="stepper-circle-btn plus" id="qe-inc" style="width:40px;height:40px;">+</button>
          </div>

          <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
            <div id="qe-star-label" class="micro-label" style="color:var(--accent-primary);">STAR RATING (${stars > 0 ? `${stars.toFixed(1)} ★` : 'UNRATED'})</div>
            <div style="display:flex;gap:6px;">
              <button type="button" class="capsule-pill" id="qe-star-minus" style="height:28px;padding:0 10px;font-size:11.5px;">−0.1 ★</button>
              <button type="button" class="capsule-pill" id="qe-star-plus" style="height:28px;padding:0 10px;font-size:11.5px;">+0.1 ★</button>
            </div>
          </div>
          <div style="display:flex;flex-direction:column;gap:8px;background:var(--elevated-surface);border-radius:18px;padding:10px 12px;">
            <div id="qe-stars-row" style="display:flex;justify-content:space-evenly;">
              ${[1, 2, 3, 4, 5]
                .map(
                  (idx) => `
                  <button data-qe-star="${idx}" style="background:none;border:none;font-size:28px;cursor:pointer;color:${
                    stars >= idx - 0.5 ? 'var(--accent-primary)' : 'var(--text-muted)'
                  };">★</button>`
                )
                .join('')}
            </div>
            ${renderSmoothRatingSliderHtml({
              id: 'qe-star-slider',
              value: stars > 0 ? stars : 0.1,
              large: true,
              showScale: true,
              ariaLabel: 'Quick edit rating slider'
            })}
          </div>

          <textarea id="qe-notes" class="auth-input" style="height:78px;padding:10px;" placeholder="Add a review note...">${escapeHtml(a.reviewNotes || '')}</textarea>

          <div style="display:flex;gap:10px;">
            <button class="btn-secondary" id="btn-close-modal" style="flex:1;">Cancel</button>
            <button class="btn-primary" id="qe-save" style="flex:1;">SAVE</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.showCreateCollectionModal) {
    return `
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet" onclick="event.stopPropagation()">
          <h3 class="serif-heading" style="font-size:24px;">Create Collection</h3>
          <input id="inp-new-col-name" class="auth-input" placeholder='e.g. "Absolute Cinema" or "Donghua Favorites"' />
          <div style="display:flex;gap:10px;">
            <button class="btn-secondary" id="btn-close-modal" style="flex:1;">Cancel</button>
            <button class="btn-primary" id="btn-save-collection" style="flex:1;">CREATE</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.showAvatarModal) {
    return `
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet" onclick="event.stopPropagation()">
          <div class="micro-label" style="color:var(--accent-primary);text-align:center;">ROYAL PORTRAITS</div>
          <h3 class="serif-heading" style="font-size:24px;text-align:center;">Choose Your Avatar</h3>
          <div class="round-avatar-carousel">
            ${BUILT_IN_AVATARS.map(
              (av) => {
                const isSelectedBase = normalizeAvatarKey(state.profile.avatarKey) === av.key;
                return `
              <div class="avatar-carousel-item ${isSelectedBase ? 'selected' : ''}" data-pick-avatar="${av.key}">
                <div class="avatar-ring-wrap" style="background:linear-gradient(135deg,${av.c1},${av.c2});">
                  <img src="${av.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" />
                  ${isSelectedBase ? `<div class="avatar-check-badge">✓</div>` : ''}
                </div>
                <span style="font-size:11px;color:var(--text-secondary);">${escapeHtml(av.name)}</span>
              </div>`;
              }
            ).join('')}
          </div>
          <button class="btn-secondary" id="btn-close-modal" style="width:100%;">Done</button>
        </div>
      </div>
    `;
  }

  if (state.showAvatarFramesModal) {
    const avPreset = getAvatarPreset(state.profile.avatarKey);
    const activeFrameKey = extractAvatarFrameKey(state.profile.avatarKey);
    const activeFrame = getAvatarFramePreset(state.profile.avatarKey);
    const userXp = getUserXp();
    const framesUnlocked = userXp >= AVATAR_FRAME_UNLOCK_XP;
    const remainingXp = Math.max(0, AVATAR_FRAME_UNLOCK_XP - userXp);
    const xpProgressPct = Math.min(100, Math.round((userXp / AVATAR_FRAME_UNLOCK_XP) * 100));

    return `
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet avatar-frames-modal-sheet" onclick="event.stopPropagation()">
          <div class="avatar-frames-header" style="align-items:center;">
            <div>
              <div class="micro-label" style="color:var(--accent-primary);letter-spacing:2px;">COLLECTOR COSMETICS</div>
              <h3 class="serif-heading" style="font-size:25px;margin-top:2px;">Avatar Frames</h3>
            </div>
            <button type="button" class="hub-close" id="btn-close-modal" aria-label="Close">✕</button>
          </div>

          <div class="avatar-frames-hero-preview">
            ${renderAvatarCircle(state.profile.avatarKey, state.profile.username || 'A', 84)}
            <div style="text-align:center;">
              <div style="font-size:14px;font-weight:700;color:var(--text-primary);">
                ${activeFrame ? escapeHtml(activeFrame.name) : 'No Frame Equipped'}
              </div>
              <div style="font-size:11.5px;color:var(--text-secondary);margin-top:2px;">
                Usable on all avatars • Visible to all users
              </div>
            </div>
            <span class="avatar-frames-status-pill ${framesUnlocked ? 'unlocked' : 'locked'}">
              ${framesUnlocked ? `★ ${userXp} XP • UNLOCKED` : `🔒 ${userXp} / ${AVATAR_FRAME_UNLOCK_XP} XP`}
            </span>
          </div>

          <p class="avatar-frames-desc" style="margin-bottom:8px;text-align:center;">
            ${
              framesUnlocked
                ? 'You have 50+ XP! Equip any frame below across any avatar — visible to everyone in Community & Chat.'
                : `Unlock all 10 collector frames at ${AVATAR_FRAME_UNLOCK_XP} XP (${remainingXp} XP remaining).`
            }
          </p>

          <div class="avatar-frames-progress-wrap">
            <div class="avatar-frames-progress-track">
              <div class="avatar-frames-progress-fill" style="width:${xpProgressPct}%;"></div>
            </div>
            <span class="avatar-frames-progress-label tabular-num">${Math.min(userXp, AVATAR_FRAME_UNLOCK_XP)}/${AVATAR_FRAME_UNLOCK_XP} XP</span>
          </div>

          <div class="avatar-frames-grid avatar-frames-modal-grid">
            <button
              type="button"
              class="avatar-frame-tile ${!activeFrameKey ? 'selected' : ''}"
              data-pick-avatar-frame=""
              title="Use default portrait without a frame"
            >
              <div class="avatar-frame-preview-stage">
                <div class="avatar-frame-preview-circle" style="background:linear-gradient(135deg,${avPreset.c1},${avPreset.c2});">
                  <img src="${avPreset.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" />
                </div>
                ${!activeFrameKey ? `<div class="avatar-frame-check-badge">✓</div>` : ''}
              </div>
              <span class="avatar-frame-tile-name">None</span>
            </button>

            ${BUILT_IN_AVATAR_FRAMES.map((fr) => {
              const isSelected = activeFrameKey === fr.key;
              return `
                <button
                  type="button"
                  class="avatar-frame-tile ${isSelected ? 'selected' : ''} ${!framesUnlocked ? 'is-locked' : ''}"
                  data-pick-avatar-frame="${fr.key}"
                  title="${framesUnlocked ? `Equip ${escapeHtml(fr.name)}` : `Requires ${AVATAR_FRAME_UNLOCK_XP} XP to unlock`}"
                >
                  <div class="avatar-frame-preview-stage">
                    <div class="avatar-frame-preview-circle" style="background:linear-gradient(135deg,${avPreset.c1},${avPreset.c2});">
                      <img src="${avPreset.img}" alt="" onerror="${AVATAR_IMG_ONERROR}" />
                    </div>
                    <img class="avatar-frame-overlay" src="${fr.img}" alt="${escapeHtml(fr.name)}" onerror="${AVATAR_IMG_ONERROR}" style="width:82px;height:82px;" />
                    ${isSelected ? `<div class="avatar-frame-check-badge">✓</div>` : ''}
                    ${!framesUnlocked ? `<div class="avatar-frame-lock-badge" aria-label="Locked">🔒</div>` : ''}
                  </div>
                  <span class="avatar-frame-tile-name">${escapeHtml(fr.name)}</span>
                </button>
              `;
            }).join('')}
          </div>
        </div>
      </div>
    `;
  }

  if (state.showProfileEditModal) {
    return `
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal-sheet" onclick="event.stopPropagation()">
          <h3 class="serif-heading" style="font-size:24px;">Edit Username</h3>
          <input id="inp-prof-username" class="auth-input" value="${escapeHtml(state.profile.username)}" />
          <div style="display:flex;gap:10px;">
            <button class="btn-secondary" id="btn-close-modal" style="flex:1;">Cancel</button>
            <button class="btn-primary" id="btn-save-profile" style="flex:1;">SAVE</button>
          </div>
        </div>
      </div>
    `;
  }

  if (state.communityCommentsAnimeId !== null) {
    const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
    const grp =
      allGroups.find((g) => Number(g.anime_id) === Number(state.communityCommentsAnimeId)) ||
      (state.communityCommentsAnimeFallback &&
      Number(state.communityCommentsAnimeFallback.anime_id) === Number(state.communityCommentsAnimeId)
        ? state.communityCommentsAnimeFallback
        : null);
    if (grp) {
      const avgStarsNum = grp.average_rating10 ? rating10ToStars(grp.average_rating10) || 0 : 0;
      const draftStars = Number(state.communityCommentDraftStars || 0);
      const modalTab = state.communityCommentsTab === 'notes' ? 'notes' : 'discussion';
      const hasUnseen = (state.notifications.unseenAnime || []).some((u) => Number(u.anime_id) === Number(grp.anime_id));
      const modalAnime = {
        animeId: Number(grp.anime_id),
        title: grp.anime_title || `Anime #${grp.anime_id}`,
        titleEnglish: grp.anime_title || `Anime #${grp.anime_id}`,
        coverImage: grp.anime_cover_image || ''
      };
      overlays += `
        <div class="modal-backdrop" id="modal-backdrop">
          <div class="modal-sheet livtrak-hub-modal" onclick="event.stopPropagation()">
            <div class="hub-header">
              ${
                grp.anime_cover_image
                  ? `<img class="hub-poster" src="${escapeHtml(grp.anime_cover_image)}" alt="" data-open-detail="${Number(grp.anime_id)}" />`
                  : `<div class="hub-poster" style="display:grid;place-items:center;background:var(--elevated-surface);color:var(--accent-primary);font-weight:700;" data-open-detail="${Number(grp.anime_id)}">★</div>`
              }
              <div class="hub-info">
                <div class="hub-eyebrow">COMMUNITY HUB</div>
                <div class="hub-title-row">
                  <div class="hub-title" data-open-detail="${Number(grp.anime_id)}">${escapeHtml(grp.anime_title)}</div>
                  ${grp.is_adult ? `<span class="adult-18-badge" style="flex-shrink:0;">18+</span>` : ''}
                </div>
                <div class="hub-meta">
                  ${avgStarsNum > 0 ? `<span class="hub-avg">★ ${avgStarsNum.toFixed(1)} avg</span><span>•</span>` : ''}
                  <span>${grp.total_count} member${grp.total_count === 1 ? '' : 's'}</span>
                </div>
              </div>
              <button type="button" class="hub-close" id="btn-close-modal" aria-label="Close">✕</button>
            </div>

            <div class="hub-tabs">
              <button
                type="button"
                class="hub-tab ${modalTab === 'discussion' ? 'active' : ''}"
                data-comm-modal-tab="discussion"
              >
                ${hasUnseen && modalTab !== 'discussion' ? `<span class="community-card-red-dot small"></span>` : ''}
                Live Chat
              </button>
              <button
                type="button"
                class="hub-tab ${modalTab === 'notes' ? 'active' : ''}"
                data-comm-modal-tab="notes"
              >
                Reviews (${grp.reviews.length})
              </button>
            </div>

            <div class="hub-tab-panel">
              ${
                modalTab === 'discussion'
                  ? renderAnimeDiscussionSection(modalAnime, { inModal: true })
                  : `
                    <div class="hub-reviews-scroll">
                      ${
                        grp.reviews.length === 0
                          ? `<div style="text-align:center;color:var(--text-secondary);font-size:13px;padding:34px 12px;">No written reviews yet. Share your star rating or note below!</div>`
                          : grp.reviews
                              .map((rev) => {
                                const rStars = rev.rating ? rating10ToStars(rev.rating) || 0 : 0;
                                const rSent = rev.rating ? sentimentFromRating10(rev.rating) : null;
                                const hasNote = String(rev.review_notes || '').trim().length > 0;
                                const isMyRev = Boolean(state.session && rev.user_id === state.session.userId);
                                const isEditingMyRev = Boolean(
                                  isMyRev && Number(state.editingCommunityReviewAnimeId) === Number(grp.anime_id)
                                );
                                const editStars = Number(state.editingCommunityReviewStars || 0);
                                const cachedRevProf =
                                  typeof DiscussionRealtimeManager !== 'undefined'
                                    ? DiscussionRealtimeManager.profileCache.get(rev.user_id)
                                    : null;
                                const revAvatarKey = isMyRev
                                  ? (state.profile.avatarKey || cachedRevProf?.avatar_key || rev.avatar_key || 'avatar_1')
                                  : (cachedRevProf?.avatar_key || rev.avatar_key || 'avatar_1');
                                const revDisplayName = isMyRev
                                  ? (state.profile.username || cachedRevProf?.username || rev.username || 'Member')
                                  : (cachedRevProf?.username || rev.username || 'Member');
                                return `
                                  <div
                                    class="community-comment-item"
                                    data-ctx-comm-note="1"
                                    data-ctx-uid="${escapeHtml(rev.user_id || '')}"
                                    data-ctx-name="${escapeHtml(revDisplayName)}"
                                    data-ctx-av="${escapeHtml(revAvatarKey)}"
                                  >
                                    <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
                                      <div style="display:flex;align-items:center;gap:10px;min-width:0;overflow:visible;">
                                        ${renderAvatarCircle(revAvatarKey, revDisplayName, 34)}
                                        <div style="min-width:0;">
                                          <div style="font-size:14px;font-weight:600;color:var(--text-primary);">${escapeHtml(revDisplayName)}</div>
                                          <div style="font-size:11.5px;color:var(--text-muted);">${escapeHtml(formatReviewDate(rev.updated_at))}</div>
                                        </div>
                                      </div>
                                      <div style="display:flex;align-items:center;gap:6px;flex-shrink:0;">
                                        ${
                                          isMyRev && !isEditingMyRev
                                            ? `<button
                                                 type="button"
                                                 class="community-note-edit-btn"
                                                 data-comm-note-edit="1"
                                                 data-comm-note-edit-stars="${rStars}"
                                               >✎ Edit</button>`
                                            : ''
                                        }
                                        ${
                                          state.session && !isEditingMyRev
                                            ? `<button
                                                 type="button"
                                                 class="community-note-reply-btn"
                                                 data-comm-note-reply-uid="${escapeHtml(rev.user_id || '')}"
                                                 data-comm-note-reply-name="${escapeHtml(rev.username || 'Member')}"
                                                 data-comm-note-reply-av="${escapeHtml(rev.avatar_key || 'avatar_1')}"
                                               >↩ Reply</button>`
                                            : ''
                                        }
                                        ${
                                          !isEditingMyRev
                                            ? rStars > 0
                                              ? `<div style="display:flex;flex-direction:column;align-items:flex-end;gap:3px;">
                                                  <div style="display:inline-flex;align-items:center;gap:4px;">
                                                    <span style="display:inline-flex;gap:1px;">${renderFiveStarsHtml(rStars)}</span>
                                                    <span class="tabular-num" style="font-size:13.5px;font-weight:700;color:var(--highlight);">${rStars.toFixed(1)}</span>
                                                  </div>
                                                  ${rSent ? `<span class="sentiment-pill soft ${rSent.key}" style="font-size:9px;padding:2px 7px;">${rSent.shortLabel.toUpperCase()}</span>` : ''}
                                                </div>`
                                              : `<span class="sentiment-pill soft cinema" style="font-size:9px;padding:2px 8px;">NOTE</span>`
                                            : ''
                                        }
                                      </div>
                                    </div>
                                    ${
                                      isEditingMyRev
                                        ? `
                                          <div class="community-review-edit-box">
                                            <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                                              <div style="display:flex;align-items:center;gap:8px;">
                                                <span class="micro-label" style="color:var(--accent-primary);">EDIT YOUR REVIEW</span>
                                                <span id="comm-edit-rev-star-label" class="tabular-num" style="font-size:13px;font-weight:800;color:var(--highlight);">${
                                                  editStars > 0 ? `${editStars.toFixed(1)} ★` : 'Unrated'
                                                }</span>
                                              </div>
                                              <div style="display:inline-flex;align-items:center;gap:5px;">
                                                <button type="button" class="capsule-pill" id="btn-comm-edit-rev-minus" style="height:26px;padding:0 9px;font-size:11px;">−0.1 ★</button>
                                                <button type="button" class="capsule-pill" id="btn-comm-edit-rev-plus" style="height:26px;padding:0 9px;font-size:11px;">+0.1 ★</button>
                                              </div>
                                            </div>
                                            <div style="padding:2px 0;">
                                              ${renderSmoothRatingSliderHtml({
                                                id: 'comm-edit-rev-slider',
                                                value: editStars > 0 ? editStars : 4.0,
                                                large: false,
                                                showScale: true,
                                                ariaLabel: 'Edit review star rating slider'
                                              })}
                                            </div>
                                            <textarea
                                              id="inp-comm-edit-rev-note"
                                              class="auth-input"
                                              style="height:82px;padding:10px;resize:vertical;width:100%;"
                                              placeholder="Edit your review note..."
                                              ${state.editingCommunityReviewSaving ? 'disabled' : ''}
                                            >${escapeHtml(state.editingCommunityReviewNote || '')}</textarea>
                                            <div style="display:flex;justify-content:flex-end;gap:8px;">
                                              <button type="button" class="btn-secondary" id="btn-comm-edit-rev-cancel" style="height:34px;padding:0 13px;font-size:12px;" ${
                                                state.editingCommunityReviewSaving ? 'disabled' : ''
                                              }>Cancel</button>
                                              <button type="button" class="btn-primary" id="btn-comm-edit-rev-save" style="height:34px;padding:0 16px;font-size:12px;" ${
                                                state.editingCommunityReviewSaving ? 'disabled' : ''
                                              }>${state.editingCommunityReviewSaving ? 'SAVING...' : 'SAVE REVIEW'}</button>
                                            </div>
                                          </div>
                                        `
                                        : hasNote
                                        ? `<div class="review-quote-box" style="padding:11px 14px;">
                                            <div class="review-quote-bar"></div>
                                            <div class="review-quote-text" style="font-size:13.5px;">“${escapeHtml(rev.review_notes)}”</div>
                                          </div>`
                                        : ''
                                    }
                                  </div>
                                `;
                              })
                              .join('')
                      }
                    </div>

                    ${
                      state.session && Number(state.editingCommunityReviewAnimeId) !== Number(grp.anime_id)
                        ? (() => {
                            const myExistingRev = grp.reviews.find((r) => r.user_id === state.session.userId);
                            const hasMyRev = Boolean(myExistingRev);
                            return `<div class="hub-reviews-composer">
                            <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;flex-wrap:nowrap;min-height:28px;">
                              <div style="display:flex;align-items:center;gap:8px;min-width:0;">
                                <span class="micro-label" style="color:var(--accent-primary);white-space:nowrap;">${
                                  hasMyRev ? 'UPDATE YOUR RATING &amp; REVIEW' : 'YOUR RATING &amp; REVIEW'
                                }</span>
                                <span id="comm-modal-star-label" class="tabular-num" style="font-size:13.5px;font-weight:800;color:var(--highlight);min-width:44px;white-space:nowrap;">${
                                  draftStars > 0 ? `${draftStars.toFixed(1)} ★` : ''
                                }</span>
                              </div>
                              <div style="display:inline-flex;align-items:center;gap:5px;flex-shrink:0;">
                                <button type="button" class="capsule-pill" id="btn-comm-modal-star-minus" style="height:26px;padding:0 9px;font-size:11px;">−0.1 ★</button>
                                <button type="button" class="capsule-pill" id="btn-comm-modal-star-plus" style="height:26px;padding:0 9px;font-size:11px;">+0.1 ★</button>
                              </div>
                            </div>
                            <div style="padding:2px 0;">
                              ${renderSmoothRatingSliderHtml({
                                id: 'comm-modal-star-slider',
                                value: draftStars > 0 ? draftStars : 4.0,
                                large: true,
                                showScale: true,
                                ariaLabel: 'Community review rating slider'
                              })}
                            </div>
                            <div style="display:flex;gap:8px;align-items:center;">
                              <input
                                id="inp-comm-modal-note"
                                class="auth-input"
                                style="height:42px;flex:1;"
                                placeholder="${hasMyRev ? 'Edit your review for this anime...' : 'Write your review for this anime...'}"
                                value="${escapeHtml(state.communityCommentDraftNote || '')}"
                                ${state.communityNoteSaving ? 'disabled' : ''}
                              />
                              <button type="button" class="btn-primary" id="btn-comm-modal-submit" style="height:42px;padding:0 16px;" ${
                                state.communityNoteSaving ? 'disabled' : ''
                              }>${state.communityNoteSaving ? 'SAVING...' : hasMyRev ? 'UPDATE' : 'POST'}</button>
                            </div>
                            ${
                              state.communityNoteFeedback
                                ? `<div class="feedback-banner success" style="margin-top:4px;">✓ ${escapeHtml(state.communityNoteFeedback)}</div>`
                                : ''
                            }
                            ${
                              state.communityNoteError
                                ? `<div class="feedback-banner error" style="margin-top:4px;">! ${escapeHtml(state.communityNoteError)}</div>`
                                : ''
                            }
                          </div>`;
                          })()
                        : ''
                    }
                  `
              }
            </div>
          </div>
        </div>
      `;
    }
  }

  if (state.discussion.contextMenu && state.session) {
    const cm = state.discussion.contextMenu;
    const menuH = cm.isOwner ? 148 : 56;
    const dockEl = document.querySelector('#anime-discussion-section .chat-composer-dock');
    const bottomSafeLimit = dockEl ? dockEl.getBoundingClientRect().top - 8 : window.innerHeight - 14;
    const clampedX = Math.max(12, Math.min(window.innerWidth - 216, cm.x));
    const clampedY = Math.max(12, Math.min(Math.max(12, bottomSafeLimit - menuH), cm.y));
    overlays += `
      <div class="discussion-ctx-backdrop" id="discussion-ctx-backdrop"></div>
      <div class="discussion-context-menu" style="left:${clampedX}px;top:${clampedY}px;">
        <button type="button" class="discussion-ctx-item" id="ctx-btn-reply">
          <span>↩ Reply to @${escapeHtml(cm.username)}</span>
        </button>
        ${
          cm.isOwner
            ? `
              <button type="button" class="discussion-ctx-item" id="ctx-btn-edit">
                <span>✎ Edit Message</span>
              </button>
              <button type="button" class="discussion-ctx-item danger" id="ctx-btn-delete">
                <span>🗑 Delete Message</span>
              </button>
            `
            : ''
        }
      </div>
    `;
  }

  return overlays;
}

// ============================================================================
// ROOT RENDER & EVENT BINDING
// ============================================================================

function renderApp() {
  if (activeRatingSliderDragCount > 0) {
    deferredRenderAfterSliderDrag = true;
    return;
  }
  const root = document.getElementById('app');
  if (!root) return;

  if (isTauriAppEnvironment() && state.tab === 'downloads') {
    state.tab = 'home';
  }

  const liveDetailNotesEl = document.getElementById('inp-detail-notes');
  if (liveDetailNotesEl && state.tab === 'detail') {
    state.detailNoteDraft = liveDetailNotesEl.value;
  }
  const prevScrollY = window.scrollY;
  const prevCommListScroll = document.querySelector('.community-comments-list')?.scrollTop ?? null;
  const prevModalDiscScroll = document.querySelector('.community-modal-discussion-scroll')?.scrollTop ?? null;
  const prevHScrollMap = new Map();
  document.querySelectorAll('[data-hscroll-key]').forEach((el) => {
    const key = el.getAttribute('data-hscroll-key');
    if (key) prevHScrollMap.set(key, el.scrollLeft);
  });
  const prevCapsuleScrolls = Array.from(document.querySelectorAll('.capsule-strip')).map((el) => el.scrollLeft);
  const weekStripBefore = document.querySelector('.week-strip');
  if (weekStripBefore) {
    state._weekStripScrollLeft = weekStripBefore.scrollLeft;
    prevHScrollMap.set('home-week-strip', weekStripBefore.scrollLeft);
  }
  const sameTab = state.lastRenderedTab === state.tab;

  const needsOnboarding = Boolean(
    state.session &&
      (state.authMode === 'onboard_username' ||
        state.authMode === 'onboard_avatar' ||
        state.session.onboardingCompleted === false)
  );
  if (needsOnboarding && state.authMode !== 'onboard_username' && state.authMode !== 'onboard_avatar') {
    state.authMode = 'onboard_username';
  }

  if (((!state.session && !state.isGuestMode) || needsOnboarding) && state.tab !== 'downloads') {
    syncFirstTimeNotifPopupOverlayDOM(true);
    root.innerHTML = renderAuthScreen();
    bindAuthEvents();
    return;
  }

  const hasCommunityUnseen = state.notifications.unreadCount > 0 || (state.notifications.unseenAnime || []).length > 0;

  const sidebarTabs = [
    { id: 'home', title: 'Home', icon: SVG_ICONS.home },
    { id: 'discover', title: 'Explore', icon: SVG_ICONS.explore },
    { id: 'mylist', title: 'Library', icon: SVG_ICONS.library },
    { id: 'community', title: 'Community', icon: SVG_ICONS.community, showRedDot: hasCommunityUnseen },
    { id: 'profile', title: 'Profile', icon: SVG_ICONS.profile }
  ];

  const bottomDockTabs = [
    { id: 'home', title: 'Home', icon: SVG_ICONS.home },
    { id: 'discover', title: 'Explore', icon: SVG_ICONS.explore },
    { id: 'mylist', title: 'Library', icon: SVG_ICONS.library },
    { id: 'community', title: 'Community', icon: SVG_ICONS.community, showRedDot: hasCommunityUnseen }
  ];

  let screenHtml = '';
  if (state.tab === 'home') screenHtml = renderHomeScreen();
  else if (state.tab === 'discover') screenHtml = renderDiscoverScreen();
  else if (state.tab === 'mylist') screenHtml = renderMyListScreen();
  else if (state.tab === 'community') screenHtml = renderCommunityScreen();
  else if (state.tab === 'downloads') screenHtml = renderDownloadsScreen();
  else if (state.tab === 'profile') screenHtml = renderProfileScreen();
  else if (state.tab === 'settings') screenHtml = renderSettingsScreen();
  else if (state.tab === 'detail') screenHtml = renderAnimeDetailScreen();

  root.innerHTML = `
    <div class="universe-scaffold">
      <!-- MidnightDesktopSidebar (>=900px) -->
      <aside class="midnight-sidebar">
        <button class="quick-jump-box" id="btn-sidebar-search">
          <span style="width:16px;height:16px;display:inline-flex;">${SVG_ICONS.search}</span>
          <span>Quick Jump...</span>
        </button>
        ${sidebarTabs
          .map(
            (t) => `
            <button class="sidebar-item ${state.tab === t.id ? 'active' : ''}" data-nav="${t.id}">
              ${t.icon}
              <span>${t.title}</span>
              ${t.showRedDot ? `<span class="nav-red-dot inline" aria-label="Unseen discussion messages"></span>` : ''}
            </button>`
          )
          .join('')}
        ${
          isPcSandroneEnvironment()
            ? `<div class="sidebar-sandrone-slot">
                 <button type="button" class="sandrone-mascot-btn ${sandroneIsPlaying ? 'is-playing' : ''}" data-sandrone-mascot="sidebar" aria-label="Play a random Sandrone voice line" title="Click Sandrone">
                   <img class="sandrone-mascot-img" src="${SANDRONE_IMAGE_SRC}" alt="Sandrone" draggable="false" />
                 </button>
               </div>`
            : ''
        }
      </aside>

      <!-- MidnightCompactRail (600-899px) -->
      <aside class="midnight-compact-rail">
        ${sidebarTabs
          .map(
            (t) => `
            <button class="rail-item ${state.tab === t.id ? 'active' : ''}" data-nav="${t.id}">
              <span class="nav-icon-badge-wrap">
                ${t.icon}
                ${t.showRedDot ? `<span class="nav-red-dot" aria-label="Unseen discussion messages"></span>` : ''}
              </span>
              <span>${t.title}</span>
            </button>`
          )
          .join('')}
      </aside>

      <!-- Main Content Stage -->
      <main class="universe-stage">
        ${screenHtml}
      </main>

      <!-- MidnightBottomNavigationBar (<600px, Capsule Tabs) -->
      ${
        state.tab !== 'detail' && state.tab !== 'profile' && state.tab !== 'settings' && state.tab !== 'downloads'
          ? `
            <div class="midnight-bottom-dock-wrap">
              <nav class="midnight-bottom-dock">
                ${bottomDockTabs
                  .map(
                    (t) => `
                    <button class="dock-tab ${state.tab === t.id ? 'active' : ''}" data-nav="${t.id}">
                      <span class="nav-icon-badge-wrap">
                        ${t.icon}
                        ${t.showRedDot ? `<span class="nav-red-dot" aria-label="Unseen discussion messages"></span>` : ''}
                      </span>
                      <span class="dock-tab-label">${t.title}</span>
                    </button>`
                  )
                  .join('')}
              </nav>
            </div>
          `
          : ''
      }
    </div>
    ${renderModals()}
  `;

  bindAppEvents();
  syncFirstTimeNotifPopupOverlayDOM(false);
  state.lastRenderedTab = state.tab;

  const restoreHorizontalScrolls = () => {
    prevHScrollMap.forEach((leftVal, key) => {
      const targetEl = document.querySelector(`[data-hscroll-key="${key}"]`);
      if (targetEl && typeof leftVal === 'number') {
        targetEl.scrollLeft = leftVal;
      }
    });
    if (sameTab && prevCapsuleScrolls.length > 0) {
      const newCapsules = document.querySelectorAll('.capsule-strip');
      if (newCapsules.length === prevCapsuleScrolls.length) {
        newCapsules.forEach((el, idx) => {
          if (!el.hasAttribute('data-hscroll-key')) {
            el.scrollLeft = prevCapsuleScrolls[idx];
          }
        });
      }
    }
    const weekStripAfter = document.querySelector('.week-strip');
    if (weekStripAfter && typeof state._weekStripScrollLeft === 'number') {
      weekStripAfter.scrollLeft = state._weekStripScrollLeft;
    }
  };

  restoreHorizontalScrolls();
  requestAnimationFrame(() => restoreHorizontalScrolls());

  if (prevCommListScroll !== null) {
    const commListEl = document.querySelector('.community-comments-list');
    if (commListEl) commListEl.scrollTop = prevCommListScroll;
  }
  if (prevModalDiscScroll !== null) {
    const modalDiscEl = document.querySelector('.community-modal-discussion-scroll');
    if (modalDiscEl) modalDiscEl.scrollTop = prevModalDiscScroll;
  }
  if (sameTab && prevScrollY > 0) {
    window.scrollTo(0, prevScrollY);
  }
}

function bindAuthEvents() {
  const inpIdentifier = document.getElementById('inp-identifier');
  const inpPass = document.getElementById('inp-pass');
  const inpConfirmPass = document.getElementById('inp-confirm-pass');
  if (inpIdentifier) inpIdentifier.oninput = (e) => { state.identifier = e.target.value; };
  if (inpPass) {
    inpPass.oninput = (e) => { state.password = e.target.value; };
    inpPass.onkeydown = (e) => {
      if (e.key === 'Enter' && state.authMode !== 'signup') document.getElementById('btn-auth-submit')?.click();
    };
  }
  if (inpConfirmPass) {
    inpConfirmPass.oninput = (e) => { state.confirmPassword = e.target.value; };
    inpConfirmPass.onkeydown = (e) => {
      if (e.key === 'Enter') document.getElementById('btn-auth-submit')?.click();
    };
  }

  const btnTogglePass = document.getElementById('btn-toggle-pass');
  if (btnTogglePass) {
    btnTogglePass.onclick = () => {
      state.showPassword = !state.showPassword;
      renderApp();
    };
  }

  const btnSwitchMode = document.getElementById('btn-switch-auth-mode');
  if (btnSwitchMode) {
    btnSwitchMode.onclick = () => {
      state.authMode = state.authMode === 'signup' ? 'signin' : 'signup';
      state.authError = '';
      state.authInfo = '';
      renderApp();
    };
  }

  const btnForgot = document.getElementById('btn-forgot-pass');
  if (btnForgot) {
    btnForgot.onclick = async () => {
      if (!state.identifier.trim()) {
        state.authError = 'Enter your email address or username first.';
        renderApp();
        return;
      }
      state.loading = true;
      state.authError = '';
      state.authInfo = '';
      renderApp();
      try {
        const targetEmail = await resolveLoginIdentifier(state.identifier);
        await supabaseRequest('/auth/v1/recover', 'POST', { email: targetEmail });
        state.pendingVerifyEmail = targetEmail;
        state.resetOtpCode = '';
        state.resetNewPassword = '';
        state.authMode = 'forgot_otp';
        state.authInfo = `Recovery code sent to ${maskEmailAddress(targetEmail)}.`;
      } catch (err) {
        state.authError = err.message;
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const inpResetOtp = document.getElementById('inp-reset-otp');
  const inpResetNewPass = document.getElementById('inp-reset-new-pass');
  if (inpResetOtp) {
    inpResetOtp.oninput = (e) => {
      const clean = e.target.value.replace(/\D/g, '').slice(0, 8);
      state.resetOtpCode = clean;
      if (e.target.value !== clean) e.target.value = clean;
    };
  }
  if (inpResetNewPass) {
    inpResetNewPass.oninput = (e) => { state.resetNewPassword = e.target.value; };
  }
  const btnCancelResetOtp = document.getElementById('btn-cancel-reset-otp');
  if (btnCancelResetOtp) {
    btnCancelResetOtp.onclick = () => {
      state.authMode = 'signin';
      state.authError = '';
      state.authInfo = '';
      renderApp();
    };
  }
  const btnConfirmResetOtp = document.getElementById('btn-confirm-reset-otp');
  if (btnConfirmResetOtp) {
    btnConfirmResetOtp.onclick = async () => {
      const cleanOtp = String(state.resetOtpCode || '').replace(/\D/g, '');
      if (cleanOtp.length < 6) {
        state.authError = 'Enter the verification code from your email.';
        renderApp();
        return;
      }
      if (!state.resetNewPassword || state.resetNewPassword.length < 8) {
        state.authError = 'New password must be at least 8 characters.';
        renderApp();
        return;
      }
      state.loading = true;
      state.authError = '';
      renderApp();
      try {
        const targetEmail = state.pendingVerifyEmail || state.identifier.trim().toLowerCase();
        const verifyData = await supabaseRequest('/auth/v1/verify', 'POST', {
          email: targetEmail,
          token: cleanOtp,
          type: 'recovery'
        });
        if (!verifyData.access_token) throw new Error('Invalid or expired recovery code.');
        const recoveredMetaName = String(verifyData.user?.user_metadata?.username || '').trim();
        const recoveredOnboarded =
          verifyData.user?.user_metadata?.onboarding_completed !== false && !isPlaceholderOrEmailName(recoveredMetaName);
        saveSession({
          accessToken: verifyData.access_token,
          refreshToken: verifyData.refresh_token || '',
          expiresAt: Math.floor(Date.now() / 1000) + (verifyData.expires_in || 3600),
          userId: verifyData.user?.id,
          email: verifyData.user?.email || targetEmail,
          username: recoveredOnboarded ? recoveredMetaName : '',
          onboardingCompleted: recoveredOnboarded
        });
        await supabaseRequest('/auth/v1/user', 'PUT', { password: state.resetNewPassword });
        await loadCloudData();
      } catch (err) {
        state.authError = err.message;
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const btnGuest = document.getElementById('btn-guest-preview');
  if (btnGuest) {
    btnGuest.onclick = () => {
      enterGuestSession(false);
      renderApp();
    };
  }

  // Sign-Up 8-Digit OTP Verification bindings
  const inpSignupOtp = document.getElementById('inp-signup-otp');
  if (inpSignupOtp) {
    inpSignupOtp.oninput = (e) => {
      const clean = e.target.value.replace(/\D/g, '').slice(0, 8);
      state.signupOtpCode = clean;
      if (e.target.value !== clean) e.target.value = clean;
    };
    inpSignupOtp.onkeydown = (e) => {
      if (e.key === 'Enter') document.getElementById('btn-verify-signup-otp')?.click();
    };
  }

  const btnVerifySignupOtp = document.getElementById('btn-verify-signup-otp');
  if (btnVerifySignupOtp) {
    btnVerifySignupOtp.onclick = async () => {
      const cleanOtp = String(state.signupOtpCode || '').replace(/\D/g, '');
      if (cleanOtp.length < 6) {
        state.authError = 'Please enter the verification code from your email.';
        renderApp();
        return;
      }
      state.loading = true;
      state.authError = '';
      state.authInfo = '';
      renderApp();
      try {
        const targetEmail = (state.pendingVerifyEmail || state.identifier).trim().toLowerCase();
        const data = await supabaseRequest('/auth/v1/verify', 'POST', {
          email: targetEmail,
          token: cleanOtp,
          type: 'signup'
        });
        if (!data || !data.access_token) {
          throw new Error('Invalid or expired verification code. Please check your inbox or tap Resend Code.');
        }
        saveSession({
          accessToken: data.access_token,
          refreshToken: data.refresh_token || '',
          expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600),
          userId: data.user?.id,
          email: data.user?.email || targetEmail,
          username: '',
          onboardingCompleted: false
        });
        state.onboardUsername = '';
        state.onboardAvatarKey = 'avatar_1';
        state.authInfo = 'Email verified! Choose your username and profile picture.';
        state.authMode = 'onboard_username';
      } catch (err) {
        state.authError = err.message || 'Invalid or expired verification code.';
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const btnResendSignupOtp = document.getElementById('btn-resend-signup-otp');
  if (btnResendSignupOtp) {
    btnResendSignupOtp.onclick = async () => {
      const targetEmail = (state.pendingVerifyEmail || state.identifier).trim().toLowerCase();
      if (!targetEmail) return;
      state.loading = true;
      state.authError = '';
      state.authInfo = '';
      renderApp();
      try {
        await supabaseRequest('/auth/v1/resend', 'POST', {
          type: 'signup',
          email: targetEmail
        });
        state.authInfo = 'A fresh 8-digit verification code has been sent to your email.';
      } catch (err) {
        state.authError = err.message || 'Could not resend verification code.';
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const btnChangeSignupEmail = document.getElementById('btn-change-signup-email');
  if (btnChangeSignupEmail) {
    btnChangeSignupEmail.onclick = () => {
      state.authMode = 'signup';
      state.signupOtpCode = '';
      state.authError = '';
      state.authInfo = '';
      renderApp();
    };
  }

  const btnSubmit = document.getElementById('btn-auth-submit');
  if (btnSubmit) {
    btnSubmit.onclick = async () => {
      if (!state.identifier.trim() || !state.password) {
        state.authError = 'Enter your email/username and password.';
        renderApp();
        return;
      }
      state.authError = '';
      state.authInfo = '';
      state.loading = true;
      renderApp();
      try {
        if (state.authMode === 'signup') {
          const email = state.identifier.trim().toLowerCase();
          if (!email.includes('@') || !email.includes('.')) {
            throw new Error('Please enter a valid email address.');
          }
          if (state.password.length < 8) {
            throw new Error('Password must be at least 8 characters.');
          }
          if (state.confirmPassword && state.password !== state.confirmPassword) {
            throw new Error('Passwords do not match.');
          }
          const data = await supabaseRequest('/auth/v1/signup', 'POST', {
            email,
            password: state.password,
            data: {
              onboarding_completed: false
            }
          });
          // Supabase returns an empty identities array if the email is already registered
          const identities = data?.user?.identities || data?.identities;
          if (Array.isArray(identities) && identities.length === 0) {
            throw new Error('An account with this email already exists. Please Sign In instead.');
          }
          if (data && data.access_token) {
            saveSession({
              accessToken: data.access_token,
              refreshToken: data.refresh_token || '',
              expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600),
              userId: data.user?.id,
              email,
              username: '',
              onboardingCompleted: false
            });
            state.onboardUsername = '';
            state.onboardAvatarKey = 'avatar_1';
            state.authMode = 'onboard_username';
          } else {
            state.pendingVerifyEmail = email;
            state.pendingVerifyPassword = state.password;
            state.signupOtpCode = '';
            state.authMode = 'verify_otp';
            state.authInfo = 'We sent an 8-digit verification code to your email.';
          }
        } else {
          const resolvedEmail = await resolveLoginIdentifier(state.identifier);
          try {
            const data = await supabaseRequest('/auth/v1/token?grant_type=password', 'POST', {
              email: resolvedEmail,
              password: state.password
            });
            const metaUsername = String(data.user?.user_metadata?.username || '').trim();
            const metaOnboarded = data.user?.user_metadata?.onboarding_completed !== false;
            saveSession({
              accessToken: data.access_token,
              refreshToken: data.refresh_token || '',
              expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600),
              userId: data.user?.id,
              email: data.user?.email || resolvedEmail,
              username: !isPlaceholderOrEmailName(metaUsername) ? metaUsername : '',
              onboardingCompleted: metaOnboarded
            });
            if (!metaOnboarded) {
              state.onboardUsername = '';
              state.onboardAvatarKey = 'avatar_1';
              state.authMode = 'onboard_username';
            } else {
              await loadCloudData();
            }
          } catch (signInErr) {
            const msg = String(signInErr.message || '');
            if (msg.toLowerCase().includes('not confirmed') || msg.toLowerCase().includes('email_not_confirmed')) {
              await supabaseRequest('/auth/v1/resend', 'POST', {
                type: 'signup',
                email: resolvedEmail
              }).catch(() => {});
              state.pendingVerifyEmail = resolvedEmail;
              state.pendingVerifyPassword = state.password;
              state.signupOtpCode = '';
              state.authMode = 'verify_otp';
              state.authInfo = 'Your email is not verified yet. Enter the 8-digit code sent to your inbox.';
            } else {
              throw signInErr;
            }
          }
        }
      } catch (err) {
        state.authError = err.message;
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const inpOnboardUser = document.getElementById('inp-onboard-user');
  if (inpOnboardUser) {
    inpOnboardUser.oninput = (e) => {
      state.onboardUsername = e.target.value;
    };
    inpOnboardUser.onkeydown = (e) => {
      if (e.key === 'Enter') document.getElementById('btn-onboard-next')?.click();
    };
  }

  const btnOnboardNext = document.getElementById('btn-onboard-next');
  if (btnOnboardNext) {
    btnOnboardNext.onclick = async () => {
      const cleanName = String(state.onboardUsername || '').trim();
      if (cleanName.length < 2 || cleanName.length > 32) {
        state.authError = 'Username must be between 2 and 32 characters.';
        renderApp();
        return;
      }
      if (cleanName.includes('@')) {
        state.authError = 'Usernames cannot contain @ or be an email address.';
        renderApp();
        return;
      }
      if (!/^[A-Za-z0-9_ .-]+$/.test(cleanName)) {
        state.authError = 'Use letters, numbers, spaces, dots, hyphens, or underscores.';
        renderApp();
        return;
      }
      state.loading = true;
      state.authError = '';
      state.authInfo = '';
      renderApp();
      try {
        const statusRes = await supabaseRequest('/rest/v1/rpc/check_username_status', 'POST', {
          p_username: cleanName
        }).catch(() => ({ available: true }));
        if (statusRes && statusRes.available === false) {
          state.authError = statusRes.reason || 'That username is already taken. Please pick another.';
          return;
        }
        state.onboardUsername = cleanName;
        state.authMode = 'onboard_avatar';
      } catch (err) {
        state.authError = err?.message || 'Unable to verify username availability.';
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }

  const btnOnboardBack = document.getElementById('btn-onboard-back');
  if (btnOnboardBack) {
    btnOnboardBack.onclick = () => {
      state.authError = '';
      state.authMode = 'onboard_username';
      renderApp();
    };
  }

  document.querySelectorAll('[data-onboard-avatar]').forEach((el) => {
    el.onclick = () => {
      state.onboardAvatarKey = el.getAttribute('data-onboard-avatar');
      renderApp();
    };
  });

  const btnOnboardFinish = document.getElementById('btn-onboard-finish');
  if (btnOnboardFinish) {
    btnOnboardFinish.onclick = async () => {
      const cleanName = String(state.onboardUsername || '').trim();
      if (cleanName.length < 2 || cleanName.includes('@')) {
        state.authError = 'Choose a valid username first.';
        state.authMode = 'onboard_username';
        renderApp();
        return;
      }
      const chosenAvatar = normalizeAvatarKey(state.onboardAvatarKey);
      state.loading = true;
      state.authError = '';
      renderApp();
      try {
        if (state.session) {
          const uid = encodeURIComponent(state.session.userId);
          const patched = await supabaseRequest(
            `/rest/v1/profiles?id=eq.${uid}`,
            'PATCH',
            {
              username: cleanName,
              avatar_key: chosenAvatar,
              avatar_url: null,
              updated_at: new Date().toISOString()
            },
            'return=representation'
          ).catch(() => null);
          if (!Array.isArray(patched) || patched.length === 0) {
            await supabaseRequest(
              '/rest/v1/profiles?on_conflict=id',
              'POST',
              {
                id: state.session.userId,
                username: cleanName,
                avatar_key: chosenAvatar,
                avatar_url: null,
                role: 'member',
                can_generate_invites: false
              },
              'resolution=merge-duplicates,return=minimal'
            );
          }
          await supabaseRequest('/auth/v1/user', 'PUT', {
            data: {
              username: cleanName,
              avatar_key: chosenAvatar,
              onboarding_completed: true
            }
          }).catch(() => {});
          await Promise.all([
            supabaseRequest(
              '/rest/v1/community_members?on_conflict=user_id',
              'POST',
              { user_id: state.session.userId, role: 'member', status: 'active' },
              'resolution=merge-duplicates,return=minimal'
            ).catch(() => {}),
            supabaseRequest(
              '/rest/v1/privacy_settings?on_conflict=user_id',
              'POST',
              {
                user_id: state.session.userId,
                reviews_visibility: 'community',
                activity_visibility: 'community'
              },
              'resolution=merge-duplicates,return=minimal'
            ).catch(() => {})
          ]);
          saveSession({
            ...state.session,
            username: cleanName,
            onboardingCompleted: true
          });
        }
        state.profile.username = cleanName;
        state.profile.avatarKey = chosenAvatar;
        saveProfileCache();
        state.authMode = 'signin';
        state.tab = 'home';
        if (state.session) {
          await loadCloudData();
        }
      } catch (err) {
        const msg = err?.message || 'Unable to save your profile right now.';
        state.authError = msg;
        if (msg.toLowerCase().includes('username') || msg.toLowerCase().includes('taken') || msg.toLowerCase().includes('duplicate')) {
          state.authMode = 'onboard_username';
        }
      } finally {
        state.loading = false;
        renderApp();
      }
    };
  }
}

function bindAppEvents() {
  document.querySelectorAll('[data-nav]').forEach((el) => {
    el.onclick = () => {
      const target = el.getAttribute('data-nav');
      if (target === 'downloads' && isTauriAppEnvironment()) {
        return;
      }
      state.settingsCategory = null;
      state.tab = target;
      if (target !== 'detail' && !state.communityCommentsAnimeId) {
        DiscussionRealtimeManager.disconnect();
      }
      window.scrollTo({ top: 0, behavior: 'smooth' });
      renderApp();
      if (target === 'community') {
        EmojiCacheManager.ensureMetadata(false);
        refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
        if (state.communityViewTab === 'tagged' && state.session) {
          loadUserNotifications(true);
        } else if (state.communityViewTab === 'random') {
          loadDailyRandomAnime(false);
        } else if (state.communityViewTab === 'top') {
          loadTopAnimeCatalog(false);
        }
      }
    };
  });

  document.querySelectorAll('[data-sandrone-mascot]').forEach((el) => {
    el.onclick = (e) => {
      e.preventDefault();
      playRandomSandroneVoice(el);
    };
  });

  const btnSidebarSearch = document.getElementById('btn-sidebar-search');
  const btnOpenQuickSearch = document.getElementById('btn-open-quick-search');
  if (btnSidebarSearch) {
    btnSidebarSearch.onclick = () => {
      state.tab = 'discover';
      renderApp();
      setTimeout(() => document.getElementById('inp-discover-search')?.focus(), 50);
    };
  }
  if (btnOpenQuickSearch) {
    btnOpenQuickSearch.onclick = () => {
      state.tab = 'discover';
      renderApp();
      setTimeout(() => document.getElementById('inp-discover-search')?.focus(), 50);
    };
  }

  const weekStripEl = document.querySelector('.week-strip');
  if (weekStripEl) {
    weekStripEl.onscroll = () => {
      state._weekStripScrollLeft = weekStripEl.scrollLeft;
    };
  }

  const btnAllDays = document.getElementById('btn-home-all-days');
  if (btnAllDays) {
    btnAllDays.onclick = () => {
      const ws = document.querySelector('.week-strip');
      if (ws) state._weekStripScrollLeft = ws.scrollLeft;
      state.homeAllWeekDays = true;
      renderApp();
    };
  }

  document.querySelectorAll('[data-select-day]').forEach((btn) => {
    btn.onclick = () => {
      const ws = document.querySelector('.week-strip');
      if (ws) state._weekStripScrollLeft = ws.scrollLeft;
      state.homeAllWeekDays = false;
      state.selectedDay = Number(btn.getAttribute('data-select-day'));
      renderApp();
    };
  });

  document.querySelectorAll('[data-open-detail]').forEach((el) => {
    el.onclick = (e) => {
      if (e.target.closest('[data-open-comments]') || e.target.closest('[data-open-edit-review]')) return;
      e.stopPropagation();
      if (state._suppressNextCardClick) {
        state._suppressNextCardClick = false;
        return;
      }
      openAnimeDetail(Number(el.getAttribute('data-open-detail')));
    };
  });

  // Part 4: Right-click (Desktop/Web) & Long-Press (Mobile Touch) Context Menu on Anime Cards
  document.querySelectorAll('[data-card-anime-id]').forEach((card) => {
    const aid = Number(card.getAttribute('data-card-anime-id'));
    card.oncontextmenu = (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (state.contextMenu && Date.now() - Number(state.contextMenu.openedAt || 0) < 800) {
        return;
      }
      state.contextMenu = { animeId: aid, x: e.clientX, y: e.clientY, openedAt: Date.now() };
      renderApp();
    };

    let touchTimer = null;
    let longPressFired = false;
    let startX = 0;
    let startY = 0;
    card.ontouchstart = (e) => {
      if (!e.touches || e.touches.length !== 1) return;
      longPressFired = false;
      startX = e.touches[0].clientX;
      startY = e.touches[0].clientY;
      touchTimer = setTimeout(() => {
        touchTimer = null;
        longPressFired = true;
        state._suppressNextCardClick = true;
        state._ignoreContextBackdropUntil = Date.now() + 750;
        state.contextMenu = { animeId: aid, x: startX, y: startY, openedAt: Date.now(), fromTouch: true };
        renderApp();
      }, 480);
    };
    card.ontouchmove = (e) => {
      if (!touchTimer || !e.touches || e.touches.length !== 1) return;
      const dx = Math.abs(e.touches[0].clientX - startX);
      const dy = Math.abs(e.touches[0].clientY - startY);
      if (dx > 10 || dy > 10) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };
    card.ontouchend = (e) => {
      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
      if (longPressFired) {
        e.preventDefault();
        e.stopPropagation();
        longPressFired = false;
      }
    };
    card.ontouchcancel = () => {
      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
      longPressFired = false;
    };
  });

  const ctxBackdrop = document.getElementById('anime-context-backdrop');
  if (ctxBackdrop) {
    let ctxBackdropArmed = false;
    ctxBackdrop.onpointerdown = (e) => {
      if (e.target === ctxBackdrop) ctxBackdropArmed = true;
    };
    ctxBackdrop.ontouchstart = (e) => {
      if (e.target === ctxBackdrop) ctxBackdropArmed = true;
    };
    ctxBackdrop.onmousedown = (e) => {
      if (e.target === ctxBackdrop) ctxBackdropArmed = true;
    };
    ctxBackdrop.onclick = (e) => {
      if (e.target !== ctxBackdrop || !ctxBackdropArmed) {
        e.preventDefault();
        e.stopPropagation();
        return;
      }
      ctxBackdropArmed = false;
      state.contextMenu = null;
      renderApp();
    };
    ctxBackdrop.oncontextmenu = (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (
        !ctxBackdropArmed ||
        Date.now() < Number(state._ignoreContextBackdropUntil || 0) ||
        Date.now() - Number(state.contextMenu?.openedAt || 0) < 900
      ) {
        return;
      }
      ctxBackdropArmed = false;
      state.contextMenu = null;
      renderApp();
    };
  }

  document.querySelectorAll('[data-ctx-action]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const action = btn.getAttribute('data-ctx-action');
      const aid = Number(btn.getAttribute('data-ctx-anime-id'));
      const item = findAnimeByIdAnywhere(aid);
      state.contextMenu = null;
      if (action === 'remove_home') {
        state.confirmRemoveHomeAnime = { animeId: aid, title: item ? resolveTitle(item) : '' };
        renderApp();
      } else if (action === 'add_home') {
        if (item) await addToHome(item);
        else renderApp();
      } else if (action === 'view_details') {
        openAnimeDetail(aid);
      } else if (action === 'view_library') {
        state.tab = 'mylist';
        state.myListStatusFilter = 'ALL';
        state.myListSentimentFilter = null;
        state.myListCollectionFilter = null;
        window.scrollTo({ top: 0, behavior: 'smooth' });
        renderApp();
      } else if (action === 'view_community') {
        state.tab = 'community';
        window.scrollTo({ top: 0, behavior: 'smooth' });
        renderApp();
      }
    };
  });

  const btnCancelRemoveHome = document.getElementById('btn-cancel-remove-home');
  if (btnCancelRemoveHome) {
    btnCancelRemoveHome.onclick = () => {
      state.confirmRemoveHomeAnime = null;
      renderApp();
    };
  }
  const btnConfirmRemoveHome = document.getElementById('btn-confirm-remove-home');
  if (btnConfirmRemoveHome) {
    btnConfirmRemoveHome.onclick = async () => {
      const target = state.confirmRemoveHomeAnime;
      state.confirmRemoveHomeAnime = null;
      if (target && target.animeId) {
        await removeFromHomeOnly(target.animeId);
      } else {
        renderApp();
      }
    };
  }

  // Part 5: Inline Custom Episode Number Editor
  const submitInlineEpisodeEdit = async (aid, scope) => {
    const item = state.vault.find((v) => Number(v.animeId) === Number(aid));
    if (!item || !state.inlineEpEdit || state.inlineEpEdit.saving) return;
    const activeMergedSeason =
      scope === 'detail' && state.tab === 'detail' && state.detailAnime && Number(state.detailAnime.animeId) === Number(aid)
        ? getActiveMergedSeasonForDetail()
        : null;
    const effectiveTotal = activeMergedSeason ? activeMergedSeason.episodes : item.totalEpisodes;
    const inputEl = document.getElementById(`inline-ep-input-${scope}-${aid}`);
    const rawDraft = inputEl ? inputEl.value : state.inlineEpEdit.draft;
    const check = validateEpisodeInput(rawDraft, effectiveTotal);
    if (!check.valid) {
      state.inlineEpEdit = { ...state.inlineEpEdit, draft: rawDraft, error: check.error };
      renderApp();
      setTimeout(() => {
        const el = document.getElementById(`inline-ep-input-${scope}-${aid}`);
        if (el) el.focus();
      }, 15);
      return;
    }

    state.inlineEpEdit = { ...state.inlineEpEdit, draft: String(check.value), saving: true, error: '' };
    renderApp();
    const res = activeMergedSeason
      ? await commitWebContinuousEpisodeProgress(activeMergedSeason, check.value)
      : await commitEpisodeProgress(aid, check.value);
    if (res.ok) {
      state.inlineEpEdit = null;
      renderApp();
    } else {
      const restored = state.vault.find((v) => Number(v.animeId) === Number(aid));
      const fallbackWatched = activeMergedSeason
        ? computeWebContinuousWatched(activeMergedSeason, restored || item)
        : restored
        ? restored.watchedEpisodes
        : item.watchedEpisodes || 0;
      state.inlineEpEdit = {
        animeId: aid,
        scope,
        draft: String(fallbackWatched),
        saving: false,
        error: res.error || 'Failed to save episode progress.'
      };
      renderApp();
    }
  };

  document.querySelectorAll('[data-inline-ep-trigger]').forEach((el) => {
    el.onclick = (e) => {
      e.stopPropagation();
      const aid = Number(el.getAttribute('data-inline-ep-trigger'));
      const scope = el.getAttribute('data-inline-ep-scope') || 'card';
      const item = state.vault.find((v) => Number(v.animeId) === aid);
      if (!item) return;
      const activeMergedSeason =
        scope === 'detail' && state.tab === 'detail' && state.detailAnime && Number(state.detailAnime.animeId) === aid
          ? getActiveMergedSeasonForDetail()
          : null;
      const currentWatched = activeMergedSeason
        ? computeWebContinuousWatched(activeMergedSeason, item)
        : item.watchedEpisodes || 0;
      state.inlineEpEdit = {
        animeId: aid,
        scope,
        draft: String(currentWatched),
        saving: false,
        error: ''
      };
      renderApp();
      setTimeout(() => {
        const inp = document.getElementById(`inline-ep-input-${scope}-${aid}`);
        if (inp) {
          inp.focus();
          inp.select();
        }
      }, 15);
    };
  });

  document.querySelectorAll('[data-inline-ep-input]').forEach((inp) => {
    const aid = Number(inp.getAttribute('data-inline-ep-input'));
    const scope = inp.getAttribute('data-inline-ep-scope') || 'card';
    inp.oninput = (e) => {
      if (state.inlineEpEdit && Number(state.inlineEpEdit.animeId) === aid) {
        state.inlineEpEdit.draft = e.target.value;
        state.inlineEpEdit.error = '';
      }
    };
    inp.onkeydown = (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        e.stopPropagation();
        submitInlineEpisodeEdit(aid, scope);
      } else if (e.key === 'Escape') {
        e.preventDefault();
        e.stopPropagation();
        state.inlineEpEdit = null;
        renderApp();
      }
    };
  });

  document.querySelectorAll('[data-inline-ep-save]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const aid = Number(btn.getAttribute('data-inline-ep-save'));
      const scope = btn.getAttribute('data-inline-ep-scope') || 'card';
      submitInlineEpisodeEdit(aid, scope);
    };
  });

  document.querySelectorAll('[data-inline-ep-cancel]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      state.inlineEpEdit = null;
      renderApp();
    };
  });

  document.querySelectorAll('[data-inc-id]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-inc-id'));
      const item = state.vault.find((v) => Number(v.animeId) === id);
      if (!item) return;
      const activeMergedSeason =
        state.tab === 'detail' && state.detailAnime && Number(state.detailAnime.animeId) === id
          ? getActiveMergedSeasonForDetail()
          : null;
      if (activeMergedSeason) {
        const cur = computeWebContinuousWatched(activeMergedSeason, item);
        const next = cur + 1;
        const clamped =
          activeMergedSeason.episodes && activeMergedSeason.episodes > 0
            ? Math.min(activeMergedSeason.episodes, next)
            : next;
        const res = await commitWebContinuousEpisodeProgress(activeMergedSeason, clamped);
        if (!res.ok) showToast(res.error || 'Failed to update progress', true);
        renderApp();
        return;
      }
      const next = (item.watchedEpisodes || 0) + 1;
      const clamped = item.totalEpisodes && item.totalEpisodes > 0 ? Math.min(item.totalEpisodes, next) : next;
      const res = await commitEpisodeProgress(id, clamped);
      if (!res.ok) showToast(res.error || 'Failed to update progress', true);
      renderApp();
    };
  });

  document.querySelectorAll('[data-dec-id]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-dec-id'));
      const item = state.vault.find((v) => Number(v.animeId) === id);
      if (!item) return;
      const activeMergedSeason =
        state.tab === 'detail' && state.detailAnime && Number(state.detailAnime.animeId) === id
          ? getActiveMergedSeasonForDetail()
          : null;
      if (activeMergedSeason) {
        const cur = computeWebContinuousWatched(activeMergedSeason, item);
        const next = Math.max(0, cur - 1);
        const res = await commitWebContinuousEpisodeProgress(activeMergedSeason, next);
        if (!res.ok) showToast(res.error || 'Failed to update progress', true);
        renderApp();
        return;
      }
      const next = Math.max(0, (item.watchedEpisodes || 0) - 1);
      const res = await commitEpisodeProgress(id, next);
      if (!res.ok) showToast(res.error || 'Failed to update progress', true);
      renderApp();
    };
  });

  document.querySelectorAll('[data-quick-add]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-quick-add'));
      const existing = state.vault.find((v) => Number(v.animeId) === id);
      if (existing) {
        state.quickEditAnime = { ...existing };
        renderApp();
        return;
      }
      const disc = state.discover.find((d) => Number(d.animeId) === id);
      if (disc) {
        await addToHome(disc);
      }
    };
  });

  document.querySelectorAll('[data-quick-edit]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const id = Number(btn.getAttribute('data-quick-edit'));
      const item = state.vault.find((v) => Number(v.animeId) === id);
      if (item) {
        state.quickEditAnime = { ...item };
        renderApp();
      }
    };
  });

  // Discover events (live debounced keyword search without losing focus)
  const inpDisc = document.getElementById('inp-discover-search');
  const btnClearDisc = document.getElementById('btn-clear-disc-search');
  const btnDiscRetry = document.getElementById('btn-discover-retry');
  if (btnDiscRetry) {
    btnDiscRetry.onclick = () => fetchDiscoverCatalog(false, { inPlace: true, forceRefresh: true });
  }
  if (inpDisc) {
    inpDisc.oninput = (e) => {
      const nextVal = e.target.value;
      state.searchQuery = nextVal;
      if (btnClearDisc) btnClearDisc.style.display = nextVal ? '' : 'none';
      if (discoverSearchDebounceTimer) clearTimeout(discoverSearchDebounceTimer);
      discoverSearchDebounceTimer = setTimeout(() => {
        discoverSearchDebounceTimer = null;
        fetchDiscoverCatalog(false, { inPlace: true });
      }, nextVal.trim() ? 380 : 80);
    };
    inpDisc.onkeydown = (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        if (discoverSearchDebounceTimer) {
          clearTimeout(discoverSearchDebounceTimer);
          discoverSearchDebounceTimer = null;
        }
        state.searchQuery = e.target.value;
        fetchDiscoverCatalog(false, { inPlace: true });
      }
    };
  }
  if (btnClearDisc) {
    btnClearDisc.onclick = () => {
      if (discoverSearchDebounceTimer) {
        clearTimeout(discoverSearchDebounceTimer);
        discoverSearchDebounceTimer = null;
      }
      state.searchQuery = '';
      if (inpDisc) {
        inpDisc.value = '';
        inpDisc.focus();
      }
      btnClearDisc.style.display = 'none';
      fetchDiscoverCatalog(false, { inPlace: true });
    };
  }

  document.querySelectorAll('[data-disc-mode]').forEach((btn) => {
    btn.onclick = () => {
      state.discoverMode = btn.getAttribute('data-disc-mode');
      state.discoverSort = btn.getAttribute('data-disc-sort') || 'POPULARITY_DESC';
      state.discoverStatus = state.discoverMode === 'airing' ? 'RELEASING' : 'ALL';
      fetchDiscoverCatalog();
    };
  });

  document.querySelectorAll('[data-disc-genre]').forEach((btn) => {
    btn.onclick = () => {
      state.discoverGenre = btn.getAttribute('data-disc-genre') || null;
      fetchDiscoverCatalog();
    };
  });

  const btnSafe = document.getElementById('btn-toggle-safe');
  if (btnSafe) {
    btnSafe.onclick = () => {
      state.prefs.showAdultContent = !state.prefs.showAdultContent;
      savePrefs();
      fetchDiscoverCatalog();
    };
  }

  const btnDiscoverMore = document.getElementById('btn-discover-load-more');
  if (btnDiscoverMore) {
    btnDiscoverMore.onclick = () => fetchDiscoverCatalog(true);
  }
  if (state.tab === 'discover' && !state.isSearching && state.discover.length > 0) {
    requestAnimationFrame(() => checkDiscoverInfiniteScroll());
  }

  // Library events
  document.querySelectorAll('[data-mylist-status]').forEach((btn) => {
    btn.onclick = () => {
      state.myListStatusFilter = btn.getAttribute('data-mylist-status');
      renderApp();
    };
  });
  document.querySelectorAll('[data-mylist-sentiment]').forEach((btn) => {
    btn.onclick = () => {
      state.myListSentimentFilter = btn.getAttribute('data-mylist-sentiment') || null;
      renderApp();
    };
  });
  document.querySelectorAll('[data-mylist-sort]').forEach((btn) => {
    btn.onclick = () => {
      state.myListSort = btn.getAttribute('data-mylist-sort');
      renderApp();
    };
  });
  document.querySelectorAll('[data-mylist-col]').forEach((btn) => {
    const colId = btn.getAttribute('data-mylist-col') || '';
    const colName = btn.getAttribute('data-mylist-col-name') || 'Category';
    const colCount = Number(btn.getAttribute('data-mylist-col-count') || 0);

    btn.onclick = (e) => {
      if (state._suppressNextCategoryClick) {
        state._suppressNextCategoryClick = false;
        e.preventDefault();
        e.stopPropagation();
        return;
      }
      state.myListCollectionFilter = colId || null;
      renderApp();
    };

    // Right-click on PC & Long-press on Phone to delete user-created category
    if (colId) {
      btn.oncontextmenu = (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (state.collectionContextMenu && Date.now() - Number(state.collectionContextMenu.openedAt || 0) < 850) {
          return;
        }
        state.collectionContextMenu = {
          id: colId,
          name: colName,
          count: colCount,
          x: e.clientX || 120,
          y: e.clientY || 160,
          openedAt: Date.now()
        };
        renderApp();
      };

      let colTouchTimer = null;
      let colLongPressFired = false;
      let startX = 0;
      let startY = 0;
      btn.ontouchstart = (e) => {
        if (!e.touches || e.touches.length !== 1) return;
        colLongPressFired = false;
        startX = e.touches[0].clientX;
        startY = e.touches[0].clientY;
        colTouchTimer = setTimeout(() => {
          colTouchTimer = null;
          colLongPressFired = true;
          state._suppressNextCategoryClick = true;
          state._ignoreContextBackdropUntil = Date.now() + 800;
          try {
            if (navigator.vibrate) navigator.vibrate(18);
          } catch (_) {}
          state.collectionContextMenu = {
            id: colId,
            name: colName,
            count: colCount,
            x: startX,
            y: startY,
            openedAt: Date.now(),
            fromTouch: true
          };
          renderApp();
        }, 450);
      };
      btn.ontouchmove = (e) => {
        if (!colTouchTimer || !e.touches || e.touches.length !== 1) return;
        const dx = Math.abs(e.touches[0].clientX - startX);
        const dy = Math.abs(e.touches[0].clientY - startY);
        if (dx > 10 || dy > 10) {
          clearTimeout(colTouchTimer);
          colTouchTimer = null;
        }
      };
      btn.ontouchend = (e) => {
        if (colTouchTimer) {
          clearTimeout(colTouchTimer);
          colTouchTimer = null;
        }
        if (colLongPressFired) {
          e.preventDefault();
          e.stopPropagation();
          colLongPressFired = false;
        }
      };
      btn.ontouchcancel = () => {
        if (colTouchTimer) {
          clearTimeout(colTouchTimer);
          colTouchTimer = null;
        }
        colLongPressFired = false;
      };
    }
  });

  const colCtxBackdrop = document.getElementById('collection-context-backdrop');
  if (colCtxBackdrop) {
    let colBackdropArmed = false;
    colCtxBackdrop.onpointerdown = (e) => {
      if (e.target === colCtxBackdrop) colBackdropArmed = true;
    };
    colCtxBackdrop.ontouchstart = (e) => {
      if (e.target === colCtxBackdrop) colBackdropArmed = true;
    };
    colCtxBackdrop.onmousedown = (e) => {
      if (e.target === colCtxBackdrop) colBackdropArmed = true;
    };
    colCtxBackdrop.onclick = (e) => {
      if (e.target !== colCtxBackdrop || !colBackdropArmed) {
        e.preventDefault();
        e.stopPropagation();
        return;
      }
      colBackdropArmed = false;
      state.collectionContextMenu = null;
      renderApp();
    };
    colCtxBackdrop.oncontextmenu = (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (
        !colBackdropArmed ||
        Date.now() < Number(state._ignoreContextBackdropUntil || 0) ||
        Date.now() - Number(state.collectionContextMenu?.openedAt || 0) < 950
      ) {
        return;
      }
      colBackdropArmed = false;
      state.collectionContextMenu = null;
      renderApp();
    };
  }
  const btnCtxCancelCol = document.getElementById('btn-ctx-cancel-collection');
  if (btnCtxCancelCol) {
    btnCtxCancelCol.onclick = (e) => {
      e.stopPropagation();
      state.collectionContextMenu = null;
      renderApp();
    };
  }
  const btnCtxDeleteCol = document.getElementById('btn-ctx-delete-collection');
  if (btnCtxDeleteCol) {
    btnCtxDeleteCol.onclick = async (e) => {
      e.stopPropagation();
      const targetId = btnCtxDeleteCol.getAttribute('data-delete-col-id');
      await deleteCollectionCategoryById(targetId);
    };
  }
  const inpMyList = document.getElementById('inp-mylist-search');
  if (inpMyList) {
    inpMyList.oninput = (e) => {
      const pos = e.target.selectionStart;
      state.myListQuery = e.target.value;
      renderApp();
      const refreshed = document.getElementById('inp-mylist-search');
      if (refreshed) {
        refreshed.focus();
        if (typeof pos === 'number') {
          try {
            refreshed.setSelectionRange(pos, pos);
          } catch (_) {}
        }
      }
    };
  }
  const btnCreateCol = document.getElementById('btn-create-collection');
  const btnNewColInline = document.getElementById('btn-new-collection-inline');
  if (btnCreateCol) btnCreateCol.onclick = () => { state.showCreateCollectionModal = true; renderApp(); };
  if (btnNewColInline) btnNewColInline.onclick = () => { state.showCreateCollectionModal = true; renderApp(); };

  // Community events & sliding pill animations
  const commSwitcher = document.getElementById('community-view-switcher');
  const commSlider = document.getElementById('community-view-slider');
  if (commSwitcher && commSlider) {
    const activeTabBtn = commSwitcher.querySelector('.community-view-tab.active');
    if (activeTabBtn) {
      const targetLeft = activeTabBtn.offsetLeft;
      const targetWidth = activeTabBtn.offsetWidth;
      if (lastCommunityTabRect && (lastCommunityTabRect.left !== targetLeft || lastCommunityTabRect.width !== targetWidth)) {
        commSlider.style.transition = 'none';
        commSlider.style.transform = `translate3d(${lastCommunityTabRect.left}px, 0, 0)`;
        commSlider.style.width = `${lastCommunityTabRect.width}px`;
        commSlider.style.opacity = '1';
        void commSlider.offsetWidth;
        commSlider.style.transition = '';
        requestAnimationFrame(() => {
          commSlider.style.transform = `translate3d(${targetLeft}px, 0, 0)`;
          commSlider.style.width = `${targetWidth}px`;
        });
      } else {
        commSlider.style.transition = 'none';
        commSlider.style.transform = `translate3d(${targetLeft}px, 0, 0)`;
        commSlider.style.width = `${targetWidth}px`;
        commSlider.style.opacity = '1';
        void commSlider.offsetWidth;
        commSlider.style.transition = '';
      }
      lastCommunityTabRect = { left: targetLeft, width: targetWidth };
      try {
        activeTabBtn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
      } catch (_) {}
    }
  }

  const commStage = document.getElementById('community-section-stage');
  if (commStage) {
    let touchStartX = 0;
    let touchStartY = 0;
    let ignoreSwipe = false;
    commStage.addEventListener(
      'touchstart',
      (e) => {
        if (!e.touches || e.touches.length !== 1) return;
        const target = e.target;
        ignoreSwipe = Boolean(
          target &&
            target.closest &&
            target.closest('.capsule-strip, .top-pill-row, .top-source-toggle, .random-category-toggle, .my-shuffle-card, .my-shuffle-deck-wrapper, .community-view-switcher, iframe, input, textarea')
        );
        touchStartX = e.touches[0].clientX;
        touchStartY = e.touches[0].clientY;
      },
      { passive: true }
    );
    commStage.addEventListener(
      'touchend',
      (e) => {
        if (ignoreSwipe || !e.changedTouches || e.changedTouches.length !== 1) return;
        const dx = e.changedTouches[0].clientX - touchStartX;
        const dy = e.changedTouches[0].clientY - touchStartY;
        if (Math.abs(dx) > 56 && Math.abs(dx) > Math.abs(dy) * 1.45) {
          const curIdx = COMMUNITY_TAB_ORDER.indexOf(state.communityViewTab);
          if (curIdx >= 0) {
            const nextIdx = dx < 0 ? curIdx + 1 : curIdx - 1;
            if (nextIdx >= 0 && nextIdx < COMMUNITY_TAB_ORDER.length) {
              switchCommunityViewTab(COMMUNITY_TAB_ORDER[nextIdx]);
            }
          }
        }
      },
      { passive: true }
    );
  }

  document.querySelectorAll('[data-comm-view-tab]').forEach((btn) => {
    btn.onclick = () => {
      switchCommunityViewTab(btn.getAttribute('data-comm-view-tab') || 'feed');
    };
  });
  const btnRandomToggleDetails = document.getElementById('btn-random-toggle-details');
  if (btnRandomToggleDetails) {
    btnRandomToggleDetails.onclick = () => {
      state.dailyRandom.detailsExpanded = !state.dailyRandom.detailsExpanded;
      renderApp();
    };
  }
  const btnRandomPlayTrailer = document.getElementById('btn-random-play-trailer');
  if (btnRandomPlayTrailer) {
    btnRandomPlayTrailer.onclick = () => {
      state.dailyRandom.trailerPlaying = true;
      renderApp();
    };
  }
  const btnRandomRetry = document.getElementById('btn-random-retry');
  if (btnRandomRetry) {
    btnRandomRetry.onclick = () => loadDailyRandomAnime(true);
  }

  // Random Section: Daily Pick | My Shuffle Category Toggle & Interactive Swipe/Flip
  const randomCatToggle = document.getElementById('random-category-toggle');
  const randomCatSlider = document.getElementById('random-category-slider');
  if (randomCatToggle && randomCatSlider) {
    const activeCat = state.randomSectionCategory === 'my_shuffle' ? 'my_shuffle' : 'daily_pick';
    if (lastRandomCategorySlideFrom && lastRandomCategorySlideFrom !== activeCat) {
      randomCatSlider.style.transition = 'none';
      randomCatSlider.classList.toggle('is-daily', lastRandomCategorySlideFrom === 'daily_pick');
      randomCatSlider.classList.toggle('is-shuffle', lastRandomCategorySlideFrom === 'my_shuffle');
      void randomCatSlider.offsetWidth;
      randomCatSlider.style.transition = '';
      requestAnimationFrame(() => {
        randomCatSlider.classList.toggle('is-daily', activeCat === 'daily_pick');
        randomCatSlider.classList.toggle('is-shuffle', activeCat === 'my_shuffle');
      });
    }
    lastRandomCategorySlideFrom = null;
  }

  document.querySelectorAll('[data-random-cat]').forEach((btn) => {
    btn.onclick = () => {
      switchRandomSectionCategoryWeb(btn.getAttribute('data-random-cat') || 'daily_pick');
    };
  });

  const btnMyShuffleToggleDetails = document.getElementById('btn-my-shuffle-toggle-details');
  if (btnMyShuffleToggleDetails) {
    btnMyShuffleToggleDetails.onclick = () => {
      state.myShuffle.detailsExpanded = !state.myShuffle.detailsExpanded;
      renderApp();
    };
  }
  const btnMyShuffleAgain = document.getElementById('btn-my-shuffle-again');
  if (btnMyShuffleAgain) {
    btnMyShuffleAgain.onclick = () => triggerMyShuffleAgainWeb();
  }

  const topSourceToggle = document.getElementById('top-source-toggle');
  const topSourceSlider = document.getElementById('top-source-slider');
  if (topSourceToggle && topSourceSlider) {
    const activeSrc = state.topAnime.source === 'community' ? 'community' : 'all';
    if (lastTopSourceSlideFrom && lastTopSourceSlideFrom !== activeSrc) {
      topSourceSlider.style.transition = 'none';
      topSourceSlider.classList.toggle('is-all', lastTopSourceSlideFrom === 'all');
      topSourceSlider.classList.toggle('is-community', lastTopSourceSlideFrom === 'community');
      void topSourceSlider.offsetWidth;
      topSourceSlider.style.transition = '';
      requestAnimationFrame(() => {
        topSourceSlider.classList.toggle('is-all', activeSrc === 'all');
        topSourceSlider.classList.toggle('is-community', activeSrc === 'community');
      });
    }
    lastTopSourceSlideFrom = null;

    let toggleDragStartX = 0;
    let toggleDragging = false;
    topSourceToggle.addEventListener(
      'pointerdown',
      (e) => {
        toggleDragStartX = e.clientX;
        toggleDragging = true;
      },
      { passive: true }
    );
    topSourceToggle.addEventListener(
      'pointerup',
      (e) => {
        if (!toggleDragging) return;
        toggleDragging = false;
        const dx = e.clientX - toggleDragStartX;
        if (Math.abs(dx) >= 24) {
          switchTopAnimeSource(dx > 0 ? 'all' : 'community');
        }
      },
      { passive: true }
    );
  }

  document.querySelectorAll('[data-top-source]').forEach((btn) => {
    btn.onclick = () => {
      switchTopAnimeSource(btn.getAttribute('data-top-source'));
    };
  });
  document.querySelectorAll('[data-top-limit]').forEach((btn) => {
    btn.onclick = () => {
      const lim = Number(btn.getAttribute('data-top-limit') || 10);
      if ([10, 25, 50, 100].includes(lim) && state.topAnime.limit !== lim) {
        state.topAnime.limit = lim;
        loadTopAnimeCatalog(false);
      }
    };
  });
  document.querySelectorAll('[data-top-timeframe]').forEach((btn) => {
    btn.onclick = () => {
      const tf = btn.getAttribute('data-top-timeframe') || 'all_time';
      if (['weekly', 'monthly', 'seasonal', 'yearly', 'all_time'].includes(tf) && state.topAnime.timeframe !== tf) {
        state.topAnime.timeframe = tf;
        loadTopAnimeCatalog(false);
      }
    };
  });
  const btnTopRetry = document.getElementById('btn-top-retry');
  if (btnTopRetry) {
    btnTopRetry.onclick = () => loadTopAnimeCatalog(true);
  }
  document.querySelectorAll('[data-comm-open-anime]').forEach((el) => {
    el.onclick = () => {
      const aid = Number(el.getAttribute('data-comm-open-anime') || 0);
      if (aid > 0) openAnimeDetail(aid);
    };
  });
  const btnCommTaggedMarkAll = document.getElementById('btn-comm-tagged-mark-all');
  if (btnCommTaggedMarkAll) {
    btnCommTaggedMarkAll.onclick = () => markAllNotificationsRead();
  }
  const btnCommTaggedLoadMore = document.getElementById('btn-comm-tagged-load-more');
  if (btnCommTaggedLoadMore) {
    btnCommTaggedLoadMore.onclick = () => loadUserNotifications(false);
  }
  document.querySelectorAll('[data-open-tagged-notif]').forEach((btn) => {
    btn.onclick = () => {
      const notifId = btn.getAttribute('data-open-tagged-notif');
      const aid = Number(btn.getAttribute('data-notif-anime') || 0);
      if (!aid) return;
      openCommunityHubForAnime(aid, {
        tab: 'discussion',
        fallbackTitle: btn.getAttribute('data-notif-title') || '',
        fallbackCover: btn.getAttribute('data-notif-cover') || '',
        notificationId: notifId,
        jumpTarget: {
          commentId: btn.getAttribute('data-notif-comment') || null,
          threadId: btn.getAttribute('data-notif-thread') || null,
          replyId: btn.getAttribute('data-notif-reply') || null
        }
      });
    };
  });
  document.querySelectorAll('[data-comm-sentiment]').forEach((btn) => {
    btn.onclick = () => {
      state.communitySentimentFilter = btn.getAttribute('data-comm-sentiment') || null;
      renderApp();
    };
  });
  document.querySelectorAll('[data-comm-sort]').forEach((btn) => {
    btn.onclick = () => {
      state.communitySort = btn.getAttribute('data-comm-sort');
      renderApp();
    };
  });
  const btnToggleCommUserFilter = document.getElementById('btn-comm-toggle-user-filter');
  if (btnToggleCommUserFilter) {
    btnToggleCommUserFilter.onclick = () => {
      state.communityUserFilterOpen = !state.communityUserFilterOpen;
      renderApp();
    };
  }
  const btnClearCommUserFilter = document.getElementById('btn-comm-clear-user-filter');
  if (btnClearCommUserFilter) {
    btnClearCommUserFilter.onclick = () => {
      state.communitySelectedUsers = [];
      renderApp();
    };
  }
  document.querySelectorAll('[data-comm-remove-user]').forEach((btn) => {
    btn.onclick = () => {
      const key = btn.getAttribute('data-comm-remove-user');
      state.communitySelectedUsers = (state.communitySelectedUsers || []).filter((u) => u.key !== key);
      renderApp();
    };
  });
  document.querySelectorAll('[data-comm-pick-user]').forEach((btn) => {
    btn.onclick = () => {
      const key = btn.getAttribute('data-comm-pick-user');
      const uid = btn.getAttribute('data-comm-pick-uid') || '';
      const username = btn.getAttribute('data-comm-pick-name') || 'Member';
      const avatarKey = btn.getAttribute('data-comm-pick-av') || 'avatar_1';
      if (!key) return;
      const cur = Array.isArray(state.communitySelectedUsers) ? [...state.communitySelectedUsers] : [];
      const existsIdx = cur.findIndex((u) => u.key === key);
      if (existsIdx !== -1) {
        cur.splice(existsIdx, 1);
        state.communitySelectedUsers = cur;
        renderApp();
      } else {
        if (cur.length >= 3) {
          showToast('Max 3 users can be selected at once (by → 1, 2, 3)', true);
          return;
        }
        cur.push({ key, userId: uid, username, avatarKey });
        state.communitySelectedUsers = cur;
        renderApp();
      }
    };
  });
  const inpCommUserSearch = document.getElementById('inp-comm-user-search');
  if (inpCommUserSearch) {
    inpCommUserSearch.oninput = (e) => {
      const pos = e.target.selectionStart;
      state.communityUserSearchQuery = e.target.value;
      renderApp();
      const refreshed = document.getElementById('inp-comm-user-search');
      if (refreshed) {
        refreshed.focus();
        if (typeof pos === 'number') {
          try {
            refreshed.setSelectionRange(pos, pos);
          } catch (_) {}
        }
      }
    };
  }
  document.querySelectorAll('[data-open-comments]').forEach((el) => {
    el.onclick = (e) => {
      e.stopPropagation();
      const aid = Number(el.getAttribute('data-open-comments'));
      openCommunityHubForAnime(aid, { tab: 'discussion' });
    };
  });
  document.querySelectorAll('[data-open-edit-review]').forEach((el) => {
    el.onclick = (e) => {
      e.stopPropagation();
      const aid = Number(el.getAttribute('data-open-edit-review'));
      openCommunityHubForAnime(aid, { tab: 'notes', startEditingReview: true });
    };
  });

  document.querySelectorAll('[data-comm-modal-tab]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const tab = btn.getAttribute('data-comm-modal-tab') === 'discussion' ? 'discussion' : 'notes';
      state.communityCommentsTab = tab;
      renderApp();
      const aid = Number(state.communityCommentsAnimeId || 0);
      if (tab === 'discussion' && aid) {
        if (Number(state.discussion.animeId) !== aid) {
          loadAnimeDiscussion(aid, true).then(() => applyPendingCommunityReplyTarget());
        } else {
          applyPendingCommunityReplyTarget();
        }
        if (state.session) {
          markAnimeDiscussionSeen(aid);
        }
      }
    };
  });

  const startReplyToCommunityMember = (uid, username, avatarKey) => {
    const aid = Number(state.communityCommentsAnimeId || 0);
    if (!aid || !state.session) return;
    state.communityCommentsTab = 'discussion';
    state.discussion.contextMenu = null;
    state.pendingCommunityReplyUser = {
      id: uid || '',
      username: username || 'Member',
      avatar_key: avatarKey || 'avatar_1'
    };
    renderApp();
    markAnimeDiscussionSeen(aid);
    if (Number(state.discussion.animeId) === aid && !state.discussion.loading) {
      applyPendingCommunityReplyTarget();
    } else {
      loadAnimeDiscussion(aid, true).then(() => applyPendingCommunityReplyTarget());
    }
  };

  document.querySelectorAll('[data-comm-note-reply-uid]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      startReplyToCommunityMember(
        btn.getAttribute('data-comm-note-reply-uid'),
        btn.getAttribute('data-comm-note-reply-name') || 'Member',
        btn.getAttribute('data-comm-note-reply-av') || 'avatar_1'
      );
    };
  });
  // Direct inline Community card rating with 0.1 precision & silent 3s untouched auto-commit
  const updateCommQrVisualsInPlace = (nextStars) => {
    if (!state.communityQuickRate) return;
    state.communityQuickRate.stars = nextStars;
    const valEl = document.getElementById('comm-qr-val-label');
    if (valEl) valEl.textContent = `${nextStars.toFixed(1)} ★`;
    const sentSlot = document.getElementById('comm-qr-sentiment-slot');
    if (sentSlot) {
      const sent = sentimentFromRating10(starsToRating10(nextStars));
      sentSlot.innerHTML = sent
        ? `<span class="sentiment-pill soft ${sent.key}" style="font-size:9px;padding:2px 8px;white-space:nowrap;">${sent.shortLabel.toUpperCase()}</span>`
        : '';
    }
    const saveBtn = document.getElementById('btn-comm-qr-save');
    if (saveBtn && !state.communityQuickRate.saving) saveBtn.disabled = nextStars <= 0;
  };

  const commitCommunityQuickRate = async ({ silent = false } = {}) => {
    clearCommQuickRateAutoSave();
    if (!state.communityQuickRate || state.communityQuickRate.saving) return;
    const aid = Number(state.communityQuickRate.animeId);
    const starsVal = roundStarsTenth(state.communityQuickRate.stars);
    const rating10Val = starsToRating10(starsVal);
    if (!aid || !rating10Val) return;
    const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
    const grp = allGroups.find((g) => Number(g.anime_id) === aid);
    const myRev = grp?.reviews?.find((r) => r.user_id === state.session?.userId);
    const myVault = state.vault.find((v) => Number(v.animeId) === aid);
    const existingNote = myRev?.review_notes || myVault?.reviewNotes || '';
    state.communityQuickRate.saving = true;
    if (!silent) renderApp();
    try {
      await submitCommunityCommentNote(aid, rating10Val, existingNote, grp || null, { throwOnError: true, silentToast: true });
      state.communityQuickRate = null;
      if (!silent) showToast(`Rated ${starsVal.toFixed(1)} ★`);
    } catch (err) {
      if (state.communityQuickRate) state.communityQuickRate.saving = false;
      if (!silent) showToast(err?.message || 'Failed to save rating', true);
    } finally {
      renderApp();
    }
  };

  const scheduleCommQuickRateAutoSave = () => {
    clearCommQuickRateAutoSave();
    if (!state.communityQuickRate || state.communityQuickRate.saving) return;
    const targetAid = Number(state.communityQuickRate.animeId);
    commQuickRateAutoSaveTimer = setTimeout(() => {
      commQuickRateAutoSaveTimer = null;
      if (activeRatingSliderDragCount > 0) return;
      if (!state.communityQuickRate || state.communityQuickRate.saving) return;
      if (Number(state.communityQuickRate.animeId) !== targetAid) return;
      commitCommunityQuickRate({ silent: true });
    }, 3000);
  };

  document.querySelectorAll('[data-comm-quick-rate]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      clearCommQuickRateAutoSave();
      const aid = Number(btn.getAttribute('data-comm-quick-rate'));
      const existingStars = Number(btn.getAttribute('data-comm-my-stars') || 0);
      if (state.communityQuickRate && Number(state.communityQuickRate.animeId) === aid) {
        state.communityQuickRate = null;
      } else {
        state.communityQuickRate = {
          animeId: aid,
          stars: existingStars > 0 ? roundStarsTenth(existingStars) : 4.0,
          saving: false
        };
      }
      renderApp();
    };
  });
  const commQrSlider = document.getElementById('comm-qr-slider');
  const btnCommQrMinus = document.getElementById('btn-comm-qr-minus');
  if (btnCommQrMinus && state.communityQuickRate) {
    btnCommQrMinus.onclick = (e) => {
      e.stopPropagation();
      const cur = Number(state.communityQuickRate.stars || 0);
      const next = roundStarsTenth(Math.max(0.1, (cur > 0 ? cur : 0.2) - 0.1));
      updateCommQrVisualsInPlace(next);
      animateSmoothSliderToStars(commQrSlider, next);
      scheduleCommQuickRateAutoSave();
    };
  }
  const btnCommQrPlus = document.getElementById('btn-comm-qr-plus');
  if (btnCommQrPlus && state.communityQuickRate) {
    btnCommQrPlus.onclick = (e) => {
      e.stopPropagation();
      const cur = Number(state.communityQuickRate.stars || 0);
      const next = roundStarsTenth(Math.min(5.0, (cur > 0 ? cur : 0) + 0.1));
      updateCommQrVisualsInPlace(next);
      animateSmoothSliderToStars(commQrSlider, next);
      scheduleCommQuickRateAutoSave();
    };
  }
  if (commQrSlider && state.communityQuickRate) {
    bindSmoothRatingSlider(commQrSlider, {
      stopProp: true,
      onDragStart: () => clearCommQuickRateAutoSave(),
      onLive: (nextStars) => updateCommQrVisualsInPlace(nextStars),
      onCommit: (nextStars) => {
        updateCommQrVisualsInPlace(nextStars);
        scheduleCommQuickRateAutoSave();
      }
    });
  }
  const btnCommQrClose = document.getElementById('btn-comm-qr-close');
  if (btnCommQrClose) {
    btnCommQrClose.onclick = (e) => {
      e.stopPropagation();
      clearCommQuickRateAutoSave();
      state.communityQuickRate = null;
      renderApp();
    };
  }
  const btnCommQrSave = document.getElementById('btn-comm-qr-save');
  if (btnCommQrSave && state.communityQuickRate) {
    btnCommQrSave.onclick = async (e) => {
      e.stopPropagation();
      await commitCommunityQuickRate({ silent: false });
    };
  }

  // In-App Floating Notification Banner click & dismiss
  const inAppBanner = document.getElementById('in-app-notif-banner');
  if (inAppBanner && state.inAppNotifBanner) {
    inAppBanner.onclick = () => {
      const b = state.inAppNotifBanner;
      state.inAppNotifBanner = null;
      if (b?.animeId) {
        openCommunityHubForAnime(b.animeId, {
          tab: 'discussion',
          fallbackTitle: b.animeTitle || '',
          fallbackCover: b.animeCover || '',
          notificationId: b.id || null,
          jumpTarget: b.commentId || b.threadId || b.replyId ? {
            commentId: b.commentId || null,
            threadId: b.threadId || null,
            replyId: b.replyId || null
          } : null
        });
      } else {
        state.tab = 'community';
        state.communityViewTab = 'tagged';
        loadUserNotifications(true);
        renderApp();
      }
    };
  }
  const btnDismissInAppNotif = document.getElementById('btn-dismiss-in-app-notif');
  if (btnDismissInAppNotif) {
    btnDismissInAppNotif.onclick = (e) => {
      e.stopPropagation();
      state.inAppNotifBanner = null;
      renderApp();
    };
  }

  // First-Time Notification Permission Popup ("Stay in the Loop!") bindings
  const firstTimePopupBackdrop = document.getElementById('first-time-notif-popup');
  if (firstTimePopupBackdrop) {
    firstTimePopupBackdrop.onclick = (e) => {
      if (e.target === firstTimePopupBackdrop) {
        NativeNotificationBridge.handleFirstTimeMaybeLaterClicked();
      }
    };
  }
  const btnNotifPopupEnable = document.getElementById('btn-notif-popup-enable');
  if (btnNotifPopupEnable) {
    btnNotifPopupEnable.onclick = (e) => {
      e.preventDefault();
      e.stopPropagation();
      NativeNotificationBridge.handleFirstTimeEnableClicked();
    };
  }
  const btnNotifPopupMaybeLater = document.getElementById('btn-notif-popup-maybe-later');
  if (btnNotifPopupMaybeLater) {
    btnNotifPopupMaybeLater.onclick = (e) => {
      e.preventDefault();
      e.stopPropagation();
      NativeNotificationBridge.handleFirstTimeMaybeLaterClicked();
    };
  }
  const btnNotifPopupOpenSettings = document.getElementById('btn-notif-popup-open-settings');
  if (btnNotifPopupOpenSettings) {
    btnNotifPopupOpenSettings.onclick = (e) => {
      e.preventDefault();
      e.stopPropagation();
      state.showFirstTimeNotifPopup = false;
      state.firstTimeNotifDeniedGuide = false;
      state.notificationSettings.onboarding_dismissed = true;
      try { localStorage.setItem('au_notif_onboarding_dismissed_v1', 'true'); } catch (_) {}
      renderApp();
      NativeNotificationBridge.openOsNotificationSettings();
    };
  }

  // Settings: Enable / Test / Configure System & Background Notifications
  const btnEnableOsNotifs = document.getElementById('btn-enable-os-notifs');
  if (btnEnableOsNotifs) {
    btnEnableOsNotifs.onclick = async () => {
      const granted = await NativeNotificationBridge.requestPermissionInteractive();
      if (granted) {
        await NativeNotificationBridge.registerDeviceWithBackend('granted');
        await NativeNotificationBridge.saveNotificationSettingsToBackend({
          os_permission_status: 'granted',
          onboarding_dismissed: true
        });
      }
      showToast(granted ? 'Notifications enabled!' : 'Please allow notifications in system settings', !granted);
      renderApp();
    };
  }
  const btnOpenOsNotifSettings = document.getElementById('btn-open-os-notif-settings');
  if (btnOpenOsNotifSettings) {
    btnOpenOsNotifSettings.onclick = () => {
      NativeNotificationBridge.openOsNotificationSettings();
    };
  }

  document.querySelectorAll('[data-comm-modal-star]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const clicked = Number(btn.getAttribute('data-comm-modal-star'));
      const cur = Number(state.communityCommentDraftStars || 0);
      state.communityCommentDraftStars = cur === clicked ? roundStarsTenth(Math.max(0.1, clicked - 0.1)) : clicked;
      const inp = document.getElementById('inp-comm-modal-note');
      if (inp) state.communityCommentDraftNote = inp.value;
      renderApp();
    };
  });
  const commModalStarSlider = document.getElementById('comm-modal-star-slider');
  const updateCommModalStarsInPlace = (nextStars) => {
    const inp = document.getElementById('inp-comm-modal-note');
    if (inp) state.communityCommentDraftNote = inp.value;
    state.communityCommentDraftStars = nextStars;
    const lbl = document.getElementById('comm-modal-star-label');
    if (lbl) lbl.textContent = `${nextStars.toFixed(1)} ★`;
  };
  const btnCommModalStarMinus = document.getElementById('btn-comm-modal-star-minus');
  if (btnCommModalStarMinus) {
    btnCommModalStarMinus.onclick = (e) => {
      e.stopPropagation();
      const cur = Number(state.communityCommentDraftStars || 0);
      const next = roundStarsTenth(Math.max(0.1, (cur > 0 ? cur : 0.2) - 0.1));
      updateCommModalStarsInPlace(next);
      animateSmoothSliderToStars(commModalStarSlider, next);
    };
  }
  const btnCommModalStarPlus = document.getElementById('btn-comm-modal-star-plus');
  if (btnCommModalStarPlus) {
    btnCommModalStarPlus.onclick = (e) => {
      e.stopPropagation();
      const cur = Number(state.communityCommentDraftStars || 0);
      const next = roundStarsTenth(Math.min(5.0, (cur > 0 ? cur : 0) + 0.1));
      updateCommModalStarsInPlace(next);
      animateSmoothSliderToStars(commModalStarSlider, next);
    };
  }
  if (commModalStarSlider) {
    bindSmoothRatingSlider(commModalStarSlider, {
      stopProp: true,
      onLive: (nextStars) => updateCommModalStarsInPlace(nextStars)
    });
  }
  const inpCommModalNote = document.getElementById('inp-comm-modal-note');
  if (inpCommModalNote) {
    inpCommModalNote.oninput = (e) => {
      state.communityCommentDraftNote = e.target.value;
    };
    inpCommModalNote.onkeydown = (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        document.getElementById('btn-comm-modal-submit')?.click();
      }
    };
  }
  document.querySelectorAll('[data-comm-note-edit]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const aid = Number(state.communityCommentsAnimeId);
      if (!aid || !state.session) return;
      const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
      const grp = allGroups.find((g) => Number(g.anime_id) === aid);
      const myRev = (grp?.reviews || []).find((r) => r.user_id === state.session.userId);
      const myVault = state.vault.find((v) => Number(v.animeId) === aid);
      const existingRating10 = myRev?.rating ?? myVault?.rating10 ?? null;
      const existingStars = existingRating10 ? rating10ToStars(existingRating10) || 0 : 0;
      const existingNote = myRev?.review_notes || myVault?.reviewNotes || '';
      state.editingCommunityReviewAnimeId = aid;
      state.editingCommunityReviewStars = existingStars;
      state.editingCommunityReviewNote = existingNote;
      state.editingCommunityReviewSaving = false;
      state.communityNoteFeedback = '';
      state.communityNoteError = '';
      renderApp();
      setTimeout(() => {
        const ta = document.getElementById('inp-comm-edit-rev-note');
        if (ta) {
          ta.focus();
          try { ta.setSelectionRange(ta.value.length, ta.value.length); } catch (_) {}
        }
      }, 30);
    };
  });

  const commEditRevSlider = document.getElementById('comm-edit-rev-slider');
  const updateCommEditRevStarsInPlace = (nextStars) => {
    const ta = document.getElementById('inp-comm-edit-rev-note');
    if (ta) state.editingCommunityReviewNote = ta.value;
    state.editingCommunityReviewStars = nextStars;
    const lbl = document.getElementById('comm-edit-rev-star-label');
    if (lbl) lbl.textContent = `${nextStars.toFixed(1)} ★`;
  };
  const btnCommEditRevMinus = document.getElementById('btn-comm-edit-rev-minus');
  if (btnCommEditRevMinus) {
    btnCommEditRevMinus.onclick = (e) => {
      e.stopPropagation();
      const cur = Number(state.editingCommunityReviewStars || 0);
      const next = roundStarsTenth(Math.max(0.1, (cur > 0 ? cur : 0.2) - 0.1));
      updateCommEditRevStarsInPlace(next);
      animateSmoothSliderToStars(commEditRevSlider, next);
    };
  }
  const btnCommEditRevPlus = document.getElementById('btn-comm-edit-rev-plus');
  if (btnCommEditRevPlus) {
    btnCommEditRevPlus.onclick = (e) => {
      e.stopPropagation();
      const cur = Number(state.editingCommunityReviewStars || 0);
      const next = roundStarsTenth(Math.min(5.0, (cur > 0 ? cur : 0) + 0.1));
      updateCommEditRevStarsInPlace(next);
      animateSmoothSliderToStars(commEditRevSlider, next);
    };
  }
  if (commEditRevSlider) {
    bindSmoothRatingSlider(commEditRevSlider, {
      stopProp: true,
      onLive: (nextStars) => updateCommEditRevStarsInPlace(nextStars)
    });
  }
  const inpCommEditRevNote = document.getElementById('inp-comm-edit-rev-note');
  if (inpCommEditRevNote) {
    inpCommEditRevNote.oninput = (e) => {
      state.editingCommunityReviewNote = e.target.value;
    };
  }
  const btnCommEditRevCancel = document.getElementById('btn-comm-edit-rev-cancel');
  if (btnCommEditRevCancel) {
    btnCommEditRevCancel.onclick = (e) => {
      e.stopPropagation();
      state.editingCommunityReviewAnimeId = null;
      state.editingCommunityReviewSaving = false;
      renderApp();
    };
  }
  const btnCommEditRevSave = document.getElementById('btn-comm-edit-rev-save');
  if (btnCommEditRevSave) {
    btnCommEditRevSave.onclick = async (e) => {
      e.stopPropagation();
      if (state.editingCommunityReviewSaving) return;
      const aid = Number(state.editingCommunityReviewAnimeId || state.communityCommentsAnimeId);
      if (!aid) return;
      const noteText = (document.getElementById('inp-comm-edit-rev-note')?.value ?? state.editingCommunityReviewNote ?? '').trim();
      const stars = roundStarsTenth(state.editingCommunityReviewStars || 0);
      if (!noteText && stars <= 0) {
        state.communityNoteError = 'Please select a star rating or enter a review note.';
        state.communityNoteFeedback = '';
        renderApp();
        return;
      }
      const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
      const grp = allGroups.find((g) => Number(g.anime_id) === aid);
      const rating10Val = stars > 0 ? starsToRating10(stars) : null;

      state.editingCommunityReviewSaving = true;
      state.communityNoteFeedback = '';
      state.communityNoteError = '';
      renderApp();
      try {
        await submitCommunityCommentNote(aid, rating10Val, noteText, grp || null, {
          throwOnError: true,
          silentToast: true,
          replaceNote: true
        });
        state.editingCommunityReviewAnimeId = null;
        state.communityCommentDraftStars = stars;
        state.communityCommentDraftNote = noteText;
        if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
          state.detailNoteDraft = noteText;
        }
        state.communityNoteFeedback = 'Review updated successfully';
        showToast(state.communityNoteFeedback);
      } catch (err) {
        state.communityNoteError = err?.message || 'Failed to update review. Please try again.';
        showToast(state.communityNoteError, true);
      } finally {
        state.editingCommunityReviewSaving = false;
        renderApp();
      }
    };
  }

  const btnCommModalSubmit = document.getElementById('btn-comm-modal-submit');
  if (btnCommModalSubmit) {
    btnCommModalSubmit.onclick = async () => {
      if (state.communityNoteSaving) return;
      const aid = Number(state.communityCommentsAnimeId);
      if (!aid) return;
      const noteText = (document.getElementById('inp-comm-modal-note')?.value ?? state.communityCommentDraftNote ?? '').trim();
      const stars = roundStarsTenth(state.communityCommentDraftStars || 0);
      if (!noteText && stars <= 0) {
        state.communityNoteError = 'Please select a star rating or enter a comment.';
        state.communityNoteFeedback = '';
        renderApp();
        return;
      }

      const allGroups = groupCommunityReviewsByAnime(state.communityReviews);
      const grp = allGroups.find((g) => Number(g.anime_id) === aid);
      const hadMyRev = Boolean(state.session && grp?.reviews?.some((r) => r.user_id === state.session.userId));
      const rating10Val = stars > 0 ? starsToRating10(stars) : null;

      state.communityNoteSaving = true;
      state.communityNoteFeedback = '';
      state.communityNoteError = '';
      renderApp();
      try {
        // Part 1: Adding or updating a community note/comment/rating NEVER adds the anime to Home
        await submitCommunityCommentNote(aid, rating10Val, noteText, grp || null, {
          throwOnError: true,
          silentToast: true,
          replaceNote: true
        });
        state.communityCommentDraftNote = noteText;
        state.communityCommentDraftStars = stars;
        if (state.detailAnime && Number(state.detailAnime.animeId) === aid) {
          state.detailNoteDraft = noteText;
        }
        state.communityNoteFeedback = hadMyRev
          ? 'Review updated successfully'
          : noteText
          ? 'Review posted successfully'
          : `Rated ${stars.toFixed(1)} ★`;
        showToast(state.communityNoteFeedback);
      } catch (err) {
        state.communityNoteError = err?.message || 'Failed to post. Please try again.';
        showToast(state.communityNoteError, true);
      } finally {
        state.communityNoteSaving = false;
        renderApp();
      }
    };
  }

  // Profile & Settings events
  const openAv = document.getElementById('btn-open-avatar-modal');
  if (openAv) openAv.onclick = () => { state.showAvatarModal = true; renderApp(); };
  const openAvFrames = document.getElementById('btn-open-avatar-frames');
  if (openAvFrames) openAvFrames.onclick = () => { state.showAvatarFramesModal = true; renderApp(); };
  const openProfEdit = document.getElementById('btn-open-profile-edit');
  if (openProfEdit) openProfEdit.onclick = () => { state.showProfileEditModal = true; renderApp(); };

  const btnSettingsBack = document.getElementById('btn-settings-back');
  if (btnSettingsBack) {
    btnSettingsBack.onclick = () => {
      if (state.settingsCategory) state.settingsCategory = null;
      else state.tab = 'profile';
      renderApp();
    };
  }
  document.querySelectorAll('[data-settings-cat]').forEach((el) => {
    el.onclick = () => {
      const cat = el.getAttribute('data-settings-cat');
      if (cat === 'DOWNLOADS_LINK') {
        state.settingsCategory = null;
        state.tab = isTauriAppEnvironment() ? 'home' : 'downloads';
      } else {
        state.settingsCategory = cat;
      }
      renderApp();
    };
  });
  document.querySelectorAll('[data-set-theme]').forEach((btn) => {
    btn.onclick = () => {
      state.prefs.themeMode = btn.getAttribute('data-set-theme');
      savePrefs();
      renderApp();
    };
  });
  document.querySelectorAll('[data-set-accent]').forEach((btn) => {
    btn.onclick = () => {
      state.prefs.accentTheme = normalizeWebAccentKey(btn.getAttribute('data-set-accent'));
      savePrefs();
      renderApp();
    };
  });
  document.querySelectorAll('[data-set-custom-quick]').forEach((btn) => {
    btn.onclick = () => {
      const hex = sanitizeWebHexColor(btn.getAttribute('data-set-custom-quick'));
      if (!hex) return;
      state.prefs.accentTheme = 'CUSTOM';
      state.prefs.customAccentHex = hex;
      savePrefs();
      renderApp();
    };
  });
  const inpCustomColor = document.getElementById('inp-custom-accent-color');
  const inpCustomHex = document.getElementById('inp-custom-accent-hex');
  const btnApplyCustomAccent = document.getElementById('btn-apply-custom-accent');
  if (inpCustomColor) {
    inpCustomColor.oninput = (e) => {
      const hex = sanitizeWebHexColor(e.target.value);
      if (!hex) return;
      state.prefs.accentTheme = 'CUSTOM';
      state.prefs.customAccentHex = hex;
      if (inpCustomHex) inpCustomHex.value = hex;
      savePrefs();
    };
    inpCustomColor.onchange = () => {
      renderApp();
    };
  }
  const applyCustomHexInput = () => {
    if (!inpCustomHex) return;
    const parsed = sanitizeWebHexColor(inpCustomHex.value);
    if (!parsed) {
      showToast('Enter a valid hex color like #38BDF8', true);
      renderApp();
      return;
    }
    state.prefs.accentTheme = 'CUSTOM';
    state.prefs.customAccentHex = parsed;
    savePrefs();
    renderApp();
  };
  if (btnApplyCustomAccent) {
    btnApplyCustomAccent.onclick = applyCustomHexInput;
  }
  if (inpCustomHex) {
    inpCustomHex.onkeydown = (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        applyCustomHexInput();
      }
    };
  }
  document.querySelectorAll('[data-set-lang]').forEach((btn) => {
    btn.onclick = () => {
      state.prefs.titleLanguage = btn.getAttribute('data-set-lang');
      savePrefs();
      renderApp();
    };
  });

  const btnExitGuest = document.getElementById('btn-exit-guest');
  if (btnExitGuest) {
    btnExitGuest.onclick = () => {
      state.isGuestMode = false;
      localStorage.removeItem('anime_universe_guest');
      clearActiveUserMemoryState();
      renderApp();
    };
  }
  const btnSignout = document.getElementById('btn-signout');
  if (btnSignout) {
    btnSignout.onclick = () => {
      saveSession(null);
      state.tab = 'home';
      renderApp();
    };
  }

  // Detail Screen events
  const btnDetailBack = document.getElementById('btn-detail-back');
  if (btnDetailBack) {
    btnDetailBack.onclick = () => {
      state.tab = state.previousTab || 'home';
      renderApp();
    };
  }
  const btnDetailSeasonOverrideToggle = document.getElementById('btn-detail-season-override-toggle');
  if (btnDetailSeasonOverrideToggle) {
    btnDetailSeasonOverrideToggle.onclick = async (e) => {
      e.stopPropagation();
      const fid = Number(
        btnDetailSeasonOverrideToggle.getAttribute('data-franchise-id') ||
          state.detailRich?.franchiseId ||
          state.detailAnime?.animeId ||
          0
      );
      if (!fid) return;
      const seasonsList = state.detailRich?.seasons || [];
      const currentMode = getFranchiseSeasonOverride(fid);
      const isCurrentlyMerged =
        currentMode === 'merge_one' ||
        (currentMode === 'auto' &&
          (seasonsList.length === 1 || seasonsList.some((s) => Array.isArray(s.subEntries) && s.subEntries.length > 1)));
      const nextMode = isCurrentlyMerged ? 'split_all' : 'merge_one';
      setFranchiseSeasonOverride(fid, nextMode);
      if (state.detailRootMedia) {
        const resolved = await resolveFullSeasonChain(state.detailRootMedia, nextMode);
        if (state.detailRich) {
          state.detailRich = {
            ...state.detailRich,
            seasons: resolved.seasons,
            moviesAndOvas: resolved.moviesAndOvas,
            franchiseId: resolved.franchiseId,
            rawMainSeasonNodeCount: resolved.rawMainSeasonNodeCount
          };
        }
      } else if (state.detailAnime?.animeId) {
        await openAnimeDetail(state.detailAnime.animeId);
        return;
      }
      showToast(nextMode === 'merge_one' ? 'Merged franchise as 1 continuous season' : 'Split franchise into separate seasons');
      renderApp();
    };
  }
  const btnDetailFav = document.getElementById('btn-detail-fav');
  if (btnDetailFav && state.detailAnime) {
    btnDetailFav.onclick = async () => {
      state.detailAnime.isFavorite = !state.detailAnime.isFavorite;
      await saveVaultEntry(state.detailAnime, { addToHome: false });
      renderApp();
    };
  }
  const btnDetailAdd = document.getElementById('btn-detail-add');
  if (btnDetailAdd && state.detailAnime) {
    btnDetailAdd.onclick = async () => {
      await addToHome({ ...state.detailAnime, status: 'watching' });
    };
  }
  const btnDetailRemoveHome = document.getElementById('btn-detail-remove-home');
  if (btnDetailRemoveHome && state.detailAnime) {
    btnDetailRemoveHome.onclick = () => {
      state.confirmRemoveHomeAnime = {
        animeId: state.detailAnime.animeId,
        title: resolveTitle(state.detailAnime)
      };
      renderApp();
    };
  }
  const btnDetailAddHome = document.getElementById('btn-detail-add-home');
  if (btnDetailAddHome && state.detailAnime) {
    btnDetailAddHome.onclick = async () => {
      await addToHome(state.detailAnime);
    };
  }
  document.querySelectorAll('[data-detail-status]').forEach((btn) => {
    btn.onclick = async () => {
      if (!state.detailAnime) return;
      const nextStatus = btn.getAttribute('data-detail-status');
      state.detailAnime.status = nextStatus;
      // Part 3: Setting status to Dropped removes ONLY from Home, preserving Library, progress, notes, ratings & Community
      await saveVaultEntry(state.detailAnime, { removeFromHome: nextStatus === 'dropped' });
      if (nextStatus === 'dropped') {
        showToast('Marked Dropped — removed from Home, kept in Library');
      }
      renderApp();
    };
  });
  const btnDetailRemove = document.getElementById('btn-detail-remove');
  if (btnDetailRemove && state.detailAnime) {
    btnDetailRemove.onclick = () => removeFromVault(state.detailAnime.animeId);
  }
  const saveDetailRatingOrNote = async (rating10Val, noteText, actionKind = 'rating', { silent = false } = {}) => {
    clearDetailRatingAutoSave();
    if (!state.detailAnime) return;
    if (!silent && state.detailRatingSaving) return;

    const liveNoteEl = document.getElementById('inp-detail-notes');
    const currentLiveNote = liveNoteEl ? liveNoteEl.value : (state.detailNoteDraft ?? state.detailAnime.reviewNotes ?? '');
    const nextRating10 = rating10Val !== undefined ? rating10Val : state.detailAnime.rating10;
    const nextNotes = noteText !== undefined ? noteText : String(currentLiveNote || '').trim();

    const cleanRating = clampRating10(nextRating10);
    const cleanNotes = String(nextNotes || '').trim();

    if (!cleanRating && !cleanNotes) {
      if (!silent) {
        state.detailRatingFeedback = '';
        state.detailRatingError = 'Please select a star rating or write a review note first.';
        renderApp();
      }
      return;
    }

    const hadRatingBefore = Boolean(state.detailAnime.rating10 && Number(state.detailAnime.rating10) > 0);
    const mySeq = !silent ? ++detailExplicitSaveSeq : detailExplicitSaveSeq;

    // Never disable the Save button or trigger DOM re-renders during silent background auto-save
    if (!silent) {
      state.detailRatingSaving = true;
      state.detailRatingFeedback = '';
      state.detailRatingError = '';
    }

    state.detailAnime = { ...state.detailAnime, rating10: cleanRating, reviewNotes: cleanNotes };
    if (!silent) {
      state.detailNoteDraft = cleanNotes;
      renderApp();
    }

    try {
      if (state.detailAnime.inWatchlist) {
        await saveVaultEntry(
          {
            ...state.detailAnime,
            rating10: cleanRating,
            reviewNotes: cleanNotes
          },
          { addToHome: false, throwOnError: true }
        );
      } else {
        await submitCommunityCommentNote(
          state.detailAnime.animeId,
          cleanRating,
          cleanNotes,
          state.detailAnime,
          {
            throwOnError: true,
            silentToast: true,
            silentRender: silent,
            replaceNote: true
          }
        );
      }
      if (!silent) {
        const starsStr = cleanRating ? `${(Number(cleanRating) / 2).toFixed(1)} ★` : '';
        const msg =
          actionKind === 'note'
            ? cleanNotes && starsStr
              ? 'Review note & rating saved successfully'
              : cleanNotes
              ? 'Review note saved successfully'
              : 'Rating saved successfully'
            : hadRatingBefore
            ? `Rating updated to ${starsStr}`
            : `Rating saved (${starsStr})`;
        state.detailRatingFeedback = msg;
        showToast(msg);
      }
    } catch (err) {
      if (!silent) {
        state.detailRatingError = err?.message || 'Failed to save rating & note. Please try again.';
        showToast(state.detailRatingError, true);
      }
    } finally {
      if (!silent && mySeq === detailExplicitSaveSeq) {
        state.detailRatingSaving = false;
        renderApp();
      }
    }
  };
  const detailStarSlider = document.getElementById('detail-star-slider');
  const updateDetailStarsInPlace = (nextStars) => {
    if (!state.detailAnime) return;
    detailUserTouchedRating = true;
    const r10 = starsToRating10(nextStars);
    state.detailAnime = { ...state.detailAnime, rating10: r10 };
    const headingEl = document.getElementById('detail-star-heading');
    if (headingEl) {
      headingEl.textContent = `${nextStars.toFixed(1)} ★`;
      headingEl.style.color = 'var(--highlight)';
    }
    document.querySelectorAll('#detail-stars-row [data-star-val]').forEach((b) => {
      const idx = Number(b.getAttribute('data-star-val'));
      b.style.color = nextStars >= idx - 0.5 ? 'var(--accent-primary)' : 'var(--text-muted)';
    });
    const sent = sentimentFromRating10(r10);
    document.querySelectorAll('#detail-sentiment-strip [data-sentiment-key]').forEach((pill) => {
      pill.classList.toggle('active', Boolean(sent && pill.getAttribute('data-sentiment-key') === sent.key));
    });
  };
  const scheduleDetailRatingAutoSave = (nextStars) => {
    clearDetailRatingAutoSave();
    if (!state.detailAnime) return;
    const targetAid = Number(state.detailAnime.animeId);
    detailRatingAutoSaveTimer = setTimeout(() => {
      detailRatingAutoSaveTimer = null;
      if (activeRatingSliderDragCount > 0) return;
      if (state.detailRatingSaving) return;
      if (!state.detailAnime || Number(state.detailAnime.animeId) !== targetAid) return;
      // Do not interrupt while user is actively typing a note
      if (document.activeElement && document.activeElement.id === 'inp-detail-notes') return;
      saveDetailRatingOrNote(starsToRating10(nextStars), undefined, 'rating', { silent: true });
    }, 3000);
  };
  document.querySelectorAll('[data-star-val]').forEach((btn) => {
    btn.onclick = () => {
      if (!state.detailAnime) return;
      const clicked = Number(btn.getAttribute('data-star-val'));
      const currentStars = rating10ToStars(state.detailAnime.rating10) || 0;
      const nextStars = currentStars === clicked ? roundStarsTenth(Math.max(0.1, clicked - 0.1)) : clicked;
      updateDetailStarsInPlace(nextStars);
      animateSmoothSliderToStars(detailStarSlider, nextStars);
      scheduleDetailRatingAutoSave(nextStars);
    };
  });
  document.querySelectorAll('[data-pick-sentiment-stars]').forEach((btn) => {
    btn.onclick = () => {
      if (!state.detailAnime) return;
      const st = Number(btn.getAttribute('data-pick-sentiment-stars'));
      updateDetailStarsInPlace(st);
      animateSmoothSliderToStars(detailStarSlider, st);
      scheduleDetailRatingAutoSave(st);
    };
  });
  const btnStarMinus = document.getElementById('btn-star-minus');
  if (btnStarMinus && state.detailAnime) {
    btnStarMinus.onclick = () => {
      const cur = rating10ToStars(state.detailAnime.rating10) || 0;
      const nextStars = roundStarsTenth(Math.max(0.1, (cur > 0 ? cur : 0.2) - 0.1));
      updateDetailStarsInPlace(nextStars);
      animateSmoothSliderToStars(detailStarSlider, nextStars);
      scheduleDetailRatingAutoSave(nextStars);
    };
  }
  const btnStarPlus = document.getElementById('btn-star-plus');
  if (btnStarPlus && state.detailAnime) {
    btnStarPlus.onclick = () => {
      const cur = rating10ToStars(state.detailAnime.rating10) || 0;
      const nextStars = roundStarsTenth(Math.min(5.0, cur + 0.1));
      updateDetailStarsInPlace(nextStars);
      animateSmoothSliderToStars(detailStarSlider, nextStars);
      scheduleDetailRatingAutoSave(nextStars);
    };
  }
  if (detailStarSlider && state.detailAnime) {
    bindSmoothRatingSlider(detailStarSlider, {
      onDragStart: () => clearDetailRatingAutoSave(),
      onLive: (nextStars) => updateDetailStarsInPlace(nextStars),
      onCommit: (nextStars) => {
        updateDetailStarsInPlace(nextStars);
        scheduleDetailRatingAutoSave(nextStars);
      }
    });
  }
  const inpDetailNotes = document.getElementById('inp-detail-notes');
  if (inpDetailNotes) {
    inpDetailNotes.onfocus = () => {
      clearDetailRatingAutoSave();
    };
    inpDetailNotes.oninput = (e) => {
      clearDetailRatingAutoSave();
      detailUserTouchedNote = true;
      state.detailNoteDraft = e.target.value;
    };
  }
  const btnSaveNote = document.getElementById('btn-detail-save-note');
  if (btnSaveNote && state.detailAnime) {
    btnSaveNote.onclick = async () => {
      clearDetailRatingAutoSave();
      const notes = (document.getElementById('inp-detail-notes')?.value ?? state.detailNoteDraft ?? '').trim();
      detailUserTouchedNote = true;
      state.detailNoteDraft = notes;
      await saveDetailRatingOrNote(state.detailAnime.rating10, notes, 'note', { silent: false });
    };
  }

  // Anime Discussion & LivTrAk Live Chat + Custom Emoji Tray events
  const autoGrowTextarea = (el) => {
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 92)}px`;
  };
  const inpDiscComment = document.getElementById('inp-discussion-comment');
  if (inpDiscComment) {
    autoGrowTextarea(inpDiscComment);
    inpDiscComment.oninput = () => {
      autoGrowTextarea(inpDiscComment);
      handleDiscussionInputMentionCheck('comment', inpDiscComment);
    };
    inpDiscComment.onclick = () => handleDiscussionInputMentionCheck('comment', inpDiscComment);
    inpDiscComment.onkeydown = (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        postAnimeComment();
      }
    };
  }
  const inpDiscReply = document.getElementById('inp-discussion-reply');
  if (inpDiscReply) {
    autoGrowTextarea(inpDiscReply);
    inpDiscReply.oninput = () => {
      autoGrowTextarea(inpDiscReply);
      handleDiscussionInputMentionCheck('reply', inpDiscReply);
    };
    inpDiscReply.onclick = () => handleDiscussionInputMentionCheck('reply', inpDiscReply);
    inpDiscReply.onkeydown = (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        postThreadReply();
      }
    };
  }
  const btnToggleEmojiTray = document.getElementById('btn-toggle-emoji-tray');
  if (btnToggleEmojiTray) {
    btnToggleEmojiTray.onclick = (e) => {
      e.stopPropagation();
      state.discussion.emojiTrayOpen = !state.discussion.emojiTrayOpen;
      renderApp();
      if (state.discussion.emojiTrayOpen) {
        EmojiCacheManager.ensureMetadata(false);
        hydrateVisibleEmojiNodes();
      }
    };
  }
  document.querySelectorAll('[data-insert-emoji-id]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const eid = btn.getAttribute('data-insert-emoji-id');
      const scope = btn.getAttribute('data-insert-emoji-scope') === 'reply' ? 'reply' : 'comment';
      if (!eid) return;
      const token = `[emoji:${eid}]`;
      const inputId = scope === 'reply' ? 'inp-discussion-reply' : 'inp-discussion-comment';
      const el = document.getElementById(inputId);
      const currentVal = el ? el.value : (scope === 'reply' ? state.discussion.replyDraft : state.discussion.draft) || '';
      const pos = el && typeof el.selectionStart === 'number' ? el.selectionStart : currentVal.length;
      const before = currentVal.slice(0, pos);
      const after = currentVal.slice(pos);
      const spacerBefore = before && !/\s$/.test(before) ? ' ' : '';
      const spacerAfter = after && !/^\s/.test(after) ? ' ' : ' ';
      const nextText = `${before}${spacerBefore}${token}${spacerAfter}${after}`;
      if (scope === 'reply') state.discussion.replyDraft = nextText;
      else state.discussion.draft = nextText;
      renderApp();
      setTimeout(() => {
        const nextEl = document.getElementById(inputId);
        if (nextEl) {
          nextEl.focus();
          const nextCursor = (before + spacerBefore + token + spacerAfter).length;
          nextEl.setSelectionRange(nextCursor, nextCursor);
          autoGrowTextarea(nextEl);
        }
      }, 20);
    };
  });
  const inpDiscEditComment = document.getElementById('inp-discussion-edit-comment');
  if (inpDiscEditComment) {
    inpDiscEditComment.oninput = () => handleDiscussionInputMentionCheck('edit_comment', inpDiscEditComment);
  }
  const inpDiscEditReply = document.getElementById('inp-discussion-edit-reply');
  if (inpDiscEditReply) {
    inpDiscEditReply.oninput = () => handleDiscussionInputMentionCheck('edit_reply', inpDiscEditReply);
  }
  const btnPostDiscComment = document.getElementById('btn-post-discussion-comment');
  if (btnPostDiscComment) btnPostDiscComment.onclick = () => postAnimeComment();
  const activeDiscAnime = getActiveDiscussionAnime();
  const btnLoadMoreDisc = document.getElementById('btn-discussion-load-more');
  if (btnLoadMoreDisc && activeDiscAnime) {
    btnLoadMoreDisc.onclick = () => loadAnimeDiscussion(activeDiscAnime.animeId, false);
  }
  // Open unseen tagged anime card directly in Live Chat modal (supports both new & older templates)
  document.querySelectorAll('[data-open-unseen-anime]').forEach((el) => {
    el.onclick = (e) => {
      if (e.target.closest('[data-open-comments]') || e.target.closest('[data-open-edit-review]')) return;
      const aid = Number(el.getAttribute('data-open-unseen-anime'));
      if (!aid) return;
      openCommunityHubForAnime(aid, {
        tab: 'discussion',
        jumpTarget: {
          commentId: el.getAttribute('data-unseen-comment') || null,
          threadId: el.getAttribute('data-unseen-thread') || null,
          replyId: el.getAttribute('data-unseen-reply') || null
        }
      });
    };
  });

  // Remove selected tag chip
  document.querySelectorAll('[data-remove-tag-scope]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const scope = btn.getAttribute('data-remove-tag-scope');
      const idx = Number(btn.getAttribute('data-remove-tag-idx'));
      if (scope === 'comment') {
        state.discussion.commentTags = state.discussion.commentTags.filter((_, i) => i !== idx);
      } else if (scope === 'reply') {
        state.discussion.replyTags = state.discussion.replyTags.filter((_, i) => i !== idx);
      }
      renderApp();
    };
  });

  // Helper to set flat reply target with { messageId, senderName, previewText, stickerCode }
  const openReplyForTarget = (messageId, targetUserId, targetUsername) => {
    const msg = state.discussion.comments.find((c) => c.id === messageId);
    const rawBody = msg ? (msg.content ?? msg.body ?? '') : '';
    const sCode = extractFirstStickerCode(rawBody);
    state.discussion.activeReplyTarget = {
      messageId,
      senderName: msg?.username || targetUsername || 'Member',
      previewText: rawBody.slice(0, 180),
      stickerCode: sCode,
      targetUserId: msg?.user_id || targetUserId,
      sourceType: msg?.source_type || 'comment',
      rootCommentId: msg?._rootCommentId || messageId
    };
    state.discussion.error = '';
    state.discussion.contextMenu = null;
    renderApp();
    setTimeout(() => document.getElementById('inp-discussion-comment')?.focus(), 40);
  };

  // Clickable quoted preview inside flat messages -> jump to referenced message and pulse
  document.querySelectorAll('[data-jump-comment]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const targetId = btn.getAttribute('data-jump-comment');
      if (!targetId) return;
      state.discussion.highlightTarget = { commentId: targetId, threadId: null, replyId: null };
      renderApp();
      setTimeout(() => {
        document.getElementById(`discussion-comment-${targetId}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 60);
    };
  });

  // Clickable @mention username badges -> tag user directly in active composer
  document.querySelectorAll('[data-mention-click-name]').forEach((btn) => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const uname = btn.getAttribute('data-mention-click-name') || '';
      if (!uname) return;
      if (!state.session) {
        showToast('Sign in to tag or reply to members');
        return;
      }
      const uid = btn.getAttribute('data-mention-click-uid') || '';
      const addMentionToComposer = async () => {
        let resolvedUser = uid ? { id: uid, username: uname, avatar_key: 'avatar_1' } : null;
        if (!resolvedUser) {
          try {
            const found = await searchMentionableUsers(uname);
            resolvedUser = (found || []).find((u) => String(u.username || '').toLowerCase() === uname.toLowerCase()) || null;
          } catch (_) {}
        }
        if (resolvedUser?.id) {
          const cur = state.discussion.commentTags || [];
          const check = validateContentMentions(state.discussion.draft || '', cur.length);
          if (check.totalMentions < MAX_MENTIONS_PER_CONTENT && !cur.some((t) => t.id === resolvedUser.id)) {
            state.discussion.commentTags = [...cur, resolvedUser];
            renderApp();
          }
          showToast(`Tagged @${resolvedUser.username}`);
        } else {
          const current = state.discussion.draft || '';
          const addition = current && !/\s$/.test(current) ? ` @${uname} ` : `@${uname} `;
          state.discussion.draft = current + addition;
          renderApp();
          setTimeout(() => document.getElementById('inp-discussion-comment')?.focus(), 40);
        }
      };
      addMentionToComposer();
    };
  });

  // Right-click (Desktop) + Long-press (450ms Mobile) with scroll cancel & scale feedback
  const bindContextTriggers = (el, payloadFactory) => {
    if (!state.session) return;
    el.oncontextmenu = (e) => {
      if (e.target.closest('textarea, input, button')) return;
      e.preventDefault();
      e.stopPropagation();
      el.classList.remove('is-pressing');
      state.discussion.contextMenu = { ...payloadFactory(), x: e.clientX, y: e.clientY };
      renderApp();
    };
    let lpTimer = null;
    let startX = 0;
    let startY = 0;
    const clearLp = () => {
      if (lpTimer) {
        clearTimeout(lpTimer);
        lpTimer = null;
      }
      el.classList.remove('is-pressing');
    };
    el.addEventListener(
      'touchstart',
      (e) => {
        if (e.target.closest('textarea, input, button')) return;
        const touch = e.touches && e.touches[0];
        if (!touch) return;
        startX = touch.clientX;
        startY = touch.clientY;
        el.classList.add('is-pressing');
        lpTimer = setTimeout(() => {
          lpTimer = null;
          el.classList.remove('is-pressing');
          try { window.getSelection()?.removeAllRanges(); } catch (_) {}
          try { navigator.vibrate?.(12); } catch (_) {}
          state.discussion.contextMenu = { ...payloadFactory(), x: startX, y: startY };
          renderApp();
        }, 450);
      },
      { passive: true }
    );
    el.addEventListener(
      'touchmove',
      (e) => {
        const touch = e.touches && e.touches[0];
        if (!touch) {
          clearLp();
          return;
        }
        if (Math.hypot(touch.clientX - startX, touch.clientY - startY) > 8) {
          clearLp();
        }
      },
      { passive: true }
    );
    el.addEventListener('touchend', clearLp, { passive: true });
    el.addEventListener('touchcancel', clearLp, { passive: true });
  };

  document.querySelectorAll('[data-ctx-comment]').forEach((card) => {
    bindContextTriggers(card, () => ({
      type: 'comment',
      commentId: card.getAttribute('data-ctx-comment'),
      sourceType: card.getAttribute('data-ctx-source') || 'comment',
      userId: card.getAttribute('data-ctx-uid'),
      username: card.getAttribute('data-ctx-name') || 'Member',
      isOwner: card.getAttribute('data-ctx-owner') === '1'
    }));
  });

  document.querySelectorAll('[data-ctx-comm-note]').forEach((item) => {
    bindContextTriggers(item, () => ({
      type: 'comm_note',
      userId: item.getAttribute('data-ctx-uid') || '',
      username: item.getAttribute('data-ctx-name') || 'Member',
      avatarKey: item.getAttribute('data-ctx-av') || 'avatar_1',
      isOwner: false
    }));
  });

  const discCtxBackdrop = document.getElementById('discussion-ctx-backdrop');
  if (discCtxBackdrop) {
    discCtxBackdrop.onclick = () => {
      state.discussion.contextMenu = null;
      renderApp();
    };
  }
  const ctxBtnReply = document.getElementById('ctx-btn-reply');
  if (ctxBtnReply && state.discussion.contextMenu) {
    ctxBtnReply.onclick = () => {
      const cm = state.discussion.contextMenu;
      if (cm.type === 'comm_note') {
        startReplyToCommunityMember(cm.userId, cm.username, cm.avatarKey);
      } else {
        openReplyForTarget(cm.commentId, cm.userId, cm.username);
      }
    };
  }
  const ctxBtnEdit = document.getElementById('ctx-btn-edit');
  if (ctxBtnEdit && state.discussion.contextMenu) {
    ctxBtnEdit.onclick = () => {
      const cm = state.discussion.contextMenu;
      state.discussion.contextMenu = null;
      const target = state.discussion.comments.find((c) => c.id === cm.commentId);
      if (target) {
        state.discussion.editingCommentId = cm.commentId;
        state.discussion.editingCommentDraft = target.content ?? target.body ?? '';
      }
      renderApp();
    };
  }
  const ctxBtnDelete = document.getElementById('ctx-btn-delete');
  if (ctxBtnDelete && state.discussion.contextMenu) {
    ctxBtnDelete.onclick = () => {
      const cm = state.discussion.contextMenu;
      state.discussion.contextMenu = null;
      deleteAnimeComment(cm.commentId);
    };
  }

  const btnCancelReply = document.getElementById('btn-cancel-reply-composer');
  if (btnCancelReply) {
    btnCancelReply.onclick = () => {
      state.discussion.activeReplyTarget = null;
      renderApp();
    };
  }

  document.querySelectorAll('[data-cancel-edit-comment]').forEach((btn) => {
    btn.onclick = () => {
      state.discussion.editingCommentId = null;
      state.discussion.editingCommentDraft = '';
      renderApp();
    };
  });
  document.querySelectorAll('[data-save-edit-comment]').forEach((btn) => {
    btn.onclick = () => saveEditedAnimeComment(btn.getAttribute('data-save-edit-comment'));
  });

  // Notifications Screen events
  const btnOpenNotifSettings = document.getElementById('btn-open-notif-settings');
  if (btnOpenNotifSettings) {
    btnOpenNotifSettings.onclick = () => {
      state.previousTab = state.tab;
      state.tab = 'settings';
      state.settingsCategory = 'NOTIFICATIONS';
      renderApp();
    };
  }
  const btnNotifRefresh = document.getElementById('btn-notif-refresh');
  if (btnNotifRefresh) btnNotifRefresh.onclick = () => loadUserNotifications(true);
  const btnNotifMarkAll = document.getElementById('btn-notif-mark-all');
  if (btnNotifMarkAll) btnNotifMarkAll.onclick = () => markAllNotificationsRead();
  const btnNotifLoadMore = document.getElementById('btn-notif-load-more');
  if (btnNotifLoadMore) btnNotifLoadMore.onclick = () => loadUserNotifications(false);
  document.querySelectorAll('[data-mark-read-notif]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const notifId = btn.getAttribute('data-mark-read-notif');
      if (notifId) await markNotificationRead(notifId);
    };
  });
  document.querySelectorAll('[data-delete-notif]').forEach((btn) => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const notifId = btn.getAttribute('data-delete-notif');
      if (notifId) await deleteUserNotification(notifId);
    };
  });
  document.querySelectorAll('[data-open-notification]').forEach((btn) => {
    btn.onclick = async () => {
      const notifId = btn.getAttribute('data-open-notification');
      const animeId = Number(btn.getAttribute('data-notif-anime'));
      const commentId = btn.getAttribute('data-notif-comment') || null;
      const threadId = btn.getAttribute('data-notif-thread') || null;
      const replyId = btn.getAttribute('data-notif-reply') || null;
      await markNotificationRead(notifId);
      if (animeId > 0) {
        state.pendingNotificationJump = { animeId, commentId, threadId, replyId };
        await openAnimeDetail(animeId);
      } else {
        renderApp();
      }
    };
  });

  // Modal bindings
  const backdrop = document.getElementById('modal-backdrop');
  const btnClose = document.getElementById('btn-close-modal');
  const closeModals = () => {
    state.quickEditAnime = null;
    state.showAvatarModal = false;
    state.showAvatarFramesModal = false;
    state.showProfileEditModal = false;
    state.showCreateCollectionModal = false;
    state.communityCommentsAnimeId = null;
    state.communityCommentsAnimeFallback = null;
    state.pendingCommunityReplyUser = null;
    state.editingCommunityReviewAnimeId = null;
    state.editingCommunityReviewSaving = false;
    state.confirmRemoveHomeAnime = null;
    state.contextMenu = null;
    state.collectionContextMenu = null;
    state.discussion.contextMenu = null;
    state.discussion.emojiTrayOpen = false;
    if (!getActiveDiscussionAnime()) {
      DiscussionRealtimeManager.disconnect();
    }
    renderApp();
  };
  if (backdrop) backdrop.onclick = closeModals;
  if (btnClose) btnClose.onclick = closeModals;

  // Hydrate all lazy/cached custom emoji slots after every render without blocking UI
  hydrateVisibleEmojiNodes();

  // Download Center events
  document.querySelectorAll('[data-trigger-download]').forEach((btn) => {
    btn.onclick = () => {
      const taskKey = btn.getAttribute('data-trigger-download');
      const dlUrl = btn.getAttribute('data-dl-url');
      const dlFile = btn.getAttribute('data-dl-filename');
      startPlatformDownload(taskKey, dlUrl, dlFile);
    };
  });

  const btnSaveCollection = document.getElementById('btn-save-collection');
  if (btnSaveCollection) {
    btnSaveCollection.onclick = async () => {
      const name = document.getElementById('inp-new-col-name')?.value?.trim();
      if (!name) return;
      state.showCreateCollectionModal = false;
      if (state.session) {
        try {
          const created = await supabaseRequest(
            '/rest/v1/collections',
            'POST',
            {
              creator_id: state.session.userId,
              name,
              description: '',
              visibility: 'community'
            },
            'return=representation'
          );
          if (Array.isArray(created) && created[0]) {
            state.collections.unshift({
              id: created[0].id,
              name: created[0].name,
              description: '',
              visibility: 'community',
              animeIds: []
            });
            saveCollections();
          }
        } catch (_) {}
      } else {
        state.collections.unshift({
          id: `local_${Date.now()}`,
          name,
          description: '',
          visibility: 'community',
          animeIds: []
        });
        saveCollections();
      }
      renderApp();
    };
  }

  const syncOwnAvatarAcrossViews = (compositeKey) => {
    const myId = state.session?.userId;
    if (!myId) return;
    if (Array.isArray(state.communityReviews)) {
      state.communityReviews.forEach((r) => {
        if (r && r.user_id === myId) r.avatar_key = compositeKey;
      });
    }
    if (Array.isArray(state.discussion?.comments)) {
      state.discussion.comments.forEach((c) => {
        if (c && c.user_id === myId) c.avatar_key = compositeKey;
      });
    }
    if (typeof DiscussionRealtimeManager !== 'undefined') {
      DiscussionRealtimeManager.profileCache.set(myId, {
        username: state.profile?.username || state.session?.username || 'Member',
        avatar_key: compositeKey,
        avatar_url: null
      });
    }
  };

  document.querySelectorAll('[data-pick-avatar]').forEach((el) => {
    el.onclick = async () => {
      const baseKey = normalizeAvatarKey(el.getAttribute('data-pick-avatar'));
      const currentFrame = extractAvatarFrameKey(state.profile.avatarKey);
      const nextComposite = composeAvatarKey(baseKey, currentFrame);
      state.profile.avatarKey = nextComposite;
      syncOwnAvatarAcrossViews(nextComposite);
      saveProfileCache();
      renderApp();
      if (state.session) {
        const uid = encodeURIComponent(state.session.userId);
        try {
          await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'PATCH', {
            avatar_key: nextComposite,
            avatar_frame: currentFrame || null,
            avatar_url: null
          });
        } catch (_) {
          await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'PATCH', {
            avatar_key: nextComposite,
            avatar_url: null
          }).catch(() => {});
        }
      }
    };
  });

  document.querySelectorAll('[data-pick-avatar-frame]').forEach((el) => {
    el.onclick = async () => {
      const rawFrame = String(el.getAttribute('data-pick-avatar-frame') || '').trim();
      const targetFrame = rawFrame ? extractAvatarFrameKey(rawFrame) : '';
      const userXp = getUserXp();
      if (targetFrame && userXp < AVATAR_FRAME_UNLOCK_XP) {
        showToast(
          `Requires ${AVATAR_FRAME_UNLOCK_XP} XP to unlock Avatar Frames (${userXp}/${AVATAR_FRAME_UNLOCK_XP} XP).`
        );
        return;
      }
      const baseKey = normalizeAvatarKey(state.profile.avatarKey);
      const nextComposite = composeAvatarKey(baseKey, targetFrame);
      if (state.profile.avatarKey === nextComposite) return;
      state.profile.avatarKey = nextComposite;
      syncOwnAvatarAcrossViews(nextComposite);
      saveProfileCache();
      const chosenPreset = getAvatarFramePreset(targetFrame);
      showToast(chosenPreset ? `Equipped "${chosenPreset.name}" avatar frame!` : 'Avatar frame removed.');
      renderApp();
      if (state.session) {
        const uid = encodeURIComponent(state.session.userId);
        let syncedViaRpc = false;
        try {
          const rpcRes = await supabaseRequest('/rest/v1/rpc/equip_avatar_frame', 'POST', {
            p_frame_key: targetFrame || null
          });
          if (rpcRes && typeof rpcRes === 'object') {
            syncedViaRpc = true;
            if (typeof rpcRes.xp === 'number') {
              state.profile.xp = rpcRes.xp;
              saveProfileCache();
            }
          }
        } catch (_) {
          syncedViaRpc = false;
        }
        if (!syncedViaRpc) {
          try {
            await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'PATCH', {
              avatar_key: nextComposite,
              avatar_frame: targetFrame || null,
              avatar_url: null
            });
          } catch (_) {
            await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'PATCH', {
              avatar_key: nextComposite,
              avatar_url: null
            }).catch(() => {});
          }
        }
      }
    };
  });

  const btnSaveProf = document.getElementById('btn-save-profile');
  if (btnSaveProf) {
    btnSaveProf.onclick = async () => {
      const u = document.getElementById('inp-prof-username')?.value?.trim();
      if (u) {
        state.profile.username = u;
        saveProfileCache();
      }
      state.showProfileEditModal = false;
      renderApp();
      if (state.session && u) {
        const uid = encodeURIComponent(state.session.userId);
        await supabaseRequest(`/rest/v1/profiles?id=eq.${uid}`, 'PATCH', { username: u }).catch(() => {});
      }
    };
  }

  // Quick Edit Sheet events
  if (state.quickEditAnime) {
    const qeDec = document.getElementById('qe-dec');
    const qeInc = document.getElementById('qe-inc');
    const qeInput = document.getElementById('qe-ep-input');
    if (qeDec) {
      qeDec.onclick = () => {
        state.quickEditAnime.watchedEpisodes = Math.max(0, (state.quickEditAnime.watchedEpisodes || 0) - 1);
        renderApp();
      };
    }
    if (qeInc) {
      qeInc.onclick = () => {
        const next = (state.quickEditAnime.watchedEpisodes || 0) + 1;
        state.quickEditAnime.watchedEpisodes = state.quickEditAnime.totalEpisodes
          ? Math.min(state.quickEditAnime.totalEpisodes, next)
          : next;
        renderApp();
      };
    }
    if (qeInput) {
      qeInput.onfocus = () => qeInput.select();
      qeInput.onkeydown = (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          document.getElementById('qe-save')?.click();
        } else if (e.key === 'Escape') {
          e.preventDefault();
          state.quickEditAnime = null;
          renderApp();
        }
      };
    }
    const qeStarSlider = document.getElementById('qe-star-slider');
    const updateQeStarsInPlace = (nextStars) => {
      if (!state.quickEditAnime) return;
      state.quickEditAnime.rating10 = starsToRating10(nextStars);
      const lbl = document.getElementById('qe-star-label');
      if (lbl) lbl.textContent = `STAR RATING (${nextStars.toFixed(1)} ★)`;
      document.querySelectorAll('#qe-stars-row [data-qe-star]').forEach((b) => {
        const idx = Number(b.getAttribute('data-qe-star'));
        b.style.color = nextStars >= idx - 0.5 ? 'var(--accent-primary)' : 'var(--text-muted)';
      });
    };
    document.querySelectorAll('[data-qe-star]').forEach((btn) => {
      btn.onclick = () => {
        const clicked = Number(btn.getAttribute('data-qe-star'));
        const cur = rating10ToStars(state.quickEditAnime.rating10) || 0;
        const next = cur === clicked ? roundStarsTenth(Math.max(0.1, clicked - 0.1)) : clicked;
        updateQeStarsInPlace(next);
        animateSmoothSliderToStars(qeStarSlider, next);
      };
    });
    const qeStarMinus = document.getElementById('qe-star-minus');
    if (qeStarMinus) {
      qeStarMinus.onclick = () => {
        const cur = rating10ToStars(state.quickEditAnime.rating10) || 0;
        const next = roundStarsTenth(Math.max(0.1, (cur > 0 ? cur : 0.2) - 0.1));
        updateQeStarsInPlace(next);
        animateSmoothSliderToStars(qeStarSlider, next);
      };
    }
    const qeStarPlus = document.getElementById('qe-star-plus');
    if (qeStarPlus) {
      qeStarPlus.onclick = () => {
        const cur = rating10ToStars(state.quickEditAnime.rating10) || 0;
        const next = roundStarsTenth(Math.min(5.0, cur + 0.1));
        updateQeStarsInPlace(next);
        animateSmoothSliderToStars(qeStarSlider, next);
      };
    }
    if (qeStarSlider) {
      bindSmoothRatingSlider(qeStarSlider, {
        onLive: (nextStars) => updateQeStarsInPlace(nextStars)
      });
    }
    const qeSave = document.getElementById('qe-save');
    if (qeSave) {
      qeSave.onclick = async () => {
        const rawEp = document.getElementById('qe-ep-input')?.value ?? String(state.quickEditAnime.watchedEpisodes || 0);
        const check = validateEpisodeInput(rawEp, state.quickEditAnime.totalEpisodes);
        if (!check.valid) {
          showToast(check.error, true);
          renderApp();
          return;
        }
        const notes = document.getElementById('qe-notes')?.value || '';
        state.quickEditAnime.watchedEpisodes = check.value;
        state.quickEditAnime.reviewNotes = notes.trim();
        const toSave = { ...state.quickEditAnime };
        state.quickEditAnime = null;
        await saveVaultEntry(toSave, { addToHome: false });
        renderApp();
      };
    }
  }
}

window.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    if (state.contextMenu || state.confirmRemoveHomeAnime) {
      state.contextMenu = null;
      state.confirmRemoveHomeAnime = null;
      renderApp();
    }
  }
});

// Initial Boot & Automatic Background Sync Across All Platforms
renderApp();
loadDownloadsConfig();
fetchDiscoverCatalog();
if (state.session) {
  loadCloudData(false);
}

// Silent OTP email verification check every 4 seconds (only during OTP verification)
setInterval(async () => {
  if (
    !state.session &&
    state.authMode === 'verify_otp' &&
    state.pendingVerifyEmail &&
    state.pendingVerifyPassword &&
    !state.loading &&
    !document.hidden
  ) {
    try {
      const data = await supabaseRequest('/auth/v1/token?grant_type=password', 'POST', {
        email: state.pendingVerifyEmail,
        password: state.pendingVerifyPassword
      });
      if (data && data.access_token) {
        saveSession({
          accessToken: data.access_token,
          refreshToken: data.refresh_token || '',
          expiresAt: Math.floor(Date.now() / 1000) + (data.expires_in || 3600),
          userId: data.user?.id,
          email: data.user?.email || state.pendingVerifyEmail,
          username: '',
          onboardingCompleted: false
        });
        state.onboardUsername = '';
        state.onboardAvatarKey = 'avatar_1';
        state.authInfo = 'Email verified! Choose your username and profile picture.';
        state.authMode = 'onboard_username';
        renderApp();
      }
    } catch (_) {}
  }
}, 4000);

// Native Tauri v2 + Web OS Notification Bar & In-App Notification Banner Bridge
let inAppBannerTimer = null;
const NativeNotificationBridge = {
  knownUnreadIds: new Set(),
  hasPrimedUnread: false,
  alertedIds: new Set(),
  onboardingEvaluated: false,

  getTauriInvoke() {
    if (typeof window === 'undefined') return null;
    if (typeof window.__TAURI__?.core?.invoke === 'function') return window.__TAURI__.core.invoke;
    if (typeof window.__TAURI_INTERNALS__?.invoke === 'function') return window.__TAURI_INTERNALS__.invoke;
    return null;
  },

  detectClientPlatform() {
    const ua = typeof navigator !== 'undefined' ? String(navigator.userAgent || '').toLowerCase() : '';
    if (ua.includes('android')) return 'android';
    if (ua.includes('mac os') || ua.includes('macintosh')) return isTauriAppEnvironment() ? 'macos' : 'web';
    if (ua.includes('windows')) return isTauriAppEnvironment() ? 'windows' : 'web';
    return isTauriAppEnvironment() ? 'windows' : 'web';
  },

  getOrCreateDeviceId() {
    if (state.deviceRegistration?.device_id && state.deviceRegistration.device_id.length >= 8) {
      return state.deviceRegistration.device_id;
    }
    let existing = '';
    try {
      existing = localStorage.getItem('au_persistent_device_id_v1') || '';
    } catch (_) {}
    if (!existing || existing.length < 8) {
      const plat = this.detectClientPlatform();
      const rand =
        typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
          ? crypto.randomUUID()
          : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
      existing = `au-${plat}-${rand}`;
      try {
        localStorage.setItem('au_persistent_device_id_v1', existing);
      } catch (_) {}
    }
    state.deviceRegistration.device_id = existing;
    state.deviceRegistration.platform = this.detectClientPlatform();
    return existing;
  },

  async syncAuthToTauri(session) {
    if (!session?.userId || !session?.accessToken) return;
    const supabaseUrl = getVaultBaseUrl();
    const anonKey = getVaultPublicKey();
    const accessToken = session.accessToken;
    const refreshToken = session.refreshToken || '';
    const userId = session.userId;

    if (typeof window !== 'undefined' && window.AnimeNativeBridge && typeof window.AnimeNativeBridge.syncAuthSession === 'function') {
      try {
        window.AnimeNativeBridge.syncAuthSession(supabaseUrl, anonKey, accessToken, refreshToken, userId);
      } catch (_) {}
    }

    const invoke = this.getTauriInvoke();
    if (!invoke) return;
    try {
      await invoke('sync_notification_auth', {
        supabaseUrl,
        anonKey,
        accessToken,
        refreshToken,
        userId
      });
    } catch (_) {}
  },

  async clearAuthInTauri() {
    this.knownUnreadIds.clear();
    this.hasPrimedUnread = false;
    this.alertedIds.clear();
    this.onboardingEvaluated = false;
    if (state.session) {
      await this.unregisterDeviceFromBackend().catch(() => {});
    }
    if (typeof window !== 'undefined' && window.AnimeNativeBridge && typeof window.AnimeNativeBridge.clearAuthSession === 'function') {
      try { window.AnimeNativeBridge.clearAuthSession(); } catch (_) {}
    }
    const invoke = this.getTauriInvoke();
    if (!invoke) return;
    try {
      await invoke('clear_notification_auth');
    } catch (_) {}
  },

  async markSeenInNative(ids) {
    if (!Array.isArray(ids) || ids.length === 0) return;
    const cleanIds = ids.map(String);
    if (typeof window !== 'undefined' && window.AnimeNativeBridge && typeof window.AnimeNativeBridge.markSeenIds === 'function') {
      try {
        window.AnimeNativeBridge.markSeenIds(JSON.stringify(cleanIds));
      } catch (_) {}
    }
    const invoke = this.getTauriInvoke();
    if (!invoke) return;
    try {
      await invoke('mark_seen_notification_ids', { ids: cleanIds });
    } catch (_) {}
  },

  async syncLocalPrefsToNative() {
    const ns = state.notificationSettings || {};
    const masterEnabled = ns.master_enabled !== false;
    const messagesEnabled = ns.messages_enabled !== false;
    const updatesEnabled = ns.updates_enabled !== false;
    const remindersEnabled = ns.reminders_enabled !== false;
    const osPermissionStatus = String(state.osNotifPermission || ns.os_permission_status || 'prompt');

    if (typeof window !== 'undefined' && window.AnimeNativeBridge && typeof window.AnimeNativeBridge.syncLocalPrefs === 'function') {
      try {
        window.AnimeNativeBridge.syncLocalPrefs(
          masterEnabled,
          messagesEnabled,
          updatesEnabled,
          remindersEnabled,
          osPermissionStatus
        );
      } catch (_) {}
    }

    const invoke = this.getTauriInvoke();
    if (!invoke) return;
    try {
      await invoke('sync_local_notification_preferences', {
        masterEnabled,
        messagesEnabled,
        updatesEnabled,
        remindersEnabled,
        osPermissionStatus
      });
    } catch (_) {}
  },

  normalizePermissionValue(val) {
    if (val === true) return 'granted';
    if (val === false) return 'denied';
    if (typeof val === 'string') {
      const s = val.toLowerCase().trim();
      if (s === 'granted') return 'granted';
      if (s === 'denied') return 'denied';
      if (s === 'prompt' || s === 'prompt-with-rationale' || s === 'default') return 'prompt';
    }
    if (val && typeof val === 'object') {
      const raw = String(
        val.permissionState ??
        val.notification ??
        val.os_permission_status ??
        val.permission_status ??
        val.state ??
        val.status ??
        ''
      ).toLowerCase().trim();
      if (raw === 'granted') return 'granted';
      if (raw === 'denied') return 'denied';
      if (raw === 'prompt' || raw === 'prompt-with-rationale' || raw === 'default') return 'prompt';
    }
    return null;
  },

  applyResolvedOsPermission(perm) {
    const clean = perm === 'granted' ? 'granted' : perm === 'denied' ? 'denied' : 'prompt';
    state.osNotifPermission = clean;
    state.notificationSettings.os_permission_status = clean;
    state.deviceRegistration.permission_status = clean;
    return clean;
  },

  /**
   * Detects the actual OS notification permission WITHOUT triggering any OS prompt
   * and WITHOUT ever faking 'granted'.
   */
  async detectActualOsPermission() {
    this.getOrCreateDeviceId();
    try { localStorage.removeItem('au_os_notif_permission_v1'); } catch (_) {}

    // 1. Direct Android Kotlin @JavascriptInterface bridge (MainActivity.kt)
    if (typeof window !== 'undefined' && window.AnimeNativeBridge && typeof window.AnimeNativeBridge.getPermissionStatus === 'function') {
      try {
        const androidPerm = this.normalizePermissionValue(window.AnimeNativeBridge.getPermissionStatus());
        if (androidPerm) {
          state.deviceRegistration.platform = 'android';
          return this.applyResolvedOsPermission(androidPerm);
        }
      } catch (_) {}
    }

    const invoke = this.getTauriInvoke();
    if (invoke) {
      let detectedPlatform = '';
      let detectedPerm = null;

      // 2. Custom Rust command:
      //    - On macOS: queries real UNUserNotificationCenter.getNotificationSettings
      //    - On Windows: queries real HKCU ToastEnabled & Notifications\Settings\com.animeuniverse.app\Enabled + user consent
      //    - On Android: queries tauri-plugin-notification permission_state & device metadata
      try {
        const status = await invoke('get_device_notification_status');
        if (status && typeof status === 'object') {
          if (status.device_id) {
            state.deviceRegistration.device_id = status.device_id;
            try { localStorage.setItem('au_persistent_device_id_v1', status.device_id); } catch (_) {}
          }
          if (status.platform) {
            detectedPlatform = String(status.platform).toLowerCase();
            state.deviceRegistration.platform = detectedPlatform;
          }
          if (status.push_provider) state.deviceRegistration.push_provider = status.push_provider;
          if (status.push_token) state.deviceRegistration.push_token = status.push_token;
          const p = this.normalizePermissionValue(status.os_permission_status);
          if (p) detectedPerm = p;
        }
      } catch (_) {}

      // On macOS and Windows, trust our native UNUserNotificationCenter / Windows Registry status directly.
      // Do NOT let desktop tauri-plugin-notification's is_permission_granted() override it with a false 'granted'.
      if ((detectedPlatform === 'macos' || detectedPlatform === 'windows') && detectedPerm) {
        return this.applyResolvedOsPermission(detectedPerm);
      }

      // 3. Official tauri-plugin-notification mobile permission check commands (for Android APKs without AnimeNativeBridge)
      for (const cmd of ['plugin:notification|check_permissions', 'plugin:notification|permission_state']) {
        try {
          const checkRes = await invoke(cmd);
          const p = this.normalizePermissionValue(checkRes);
          if (p) {
            return this.applyResolvedOsPermission(p);
          }
        } catch (_) {}
      }

      if (detectedPerm) {
        return this.applyResolvedOsPermission(detectedPerm);
      }
    }

    // 4. Standard Web Browser (outside Tauri WebView)
    try {
      if (!isTauriAppEnvironment() && typeof Notification !== 'undefined' && typeof Notification.permission === 'string') {
        const rawPerm = Notification.permission.toLowerCase();
        const perm = rawPerm === 'default' ? 'prompt' : rawPerm;
        return this.applyResolvedOsPermission(perm);
      }
    } catch (_) {}

    return this.applyResolvedOsPermission(state.osNotifPermission === 'denied' ? 'denied' : 'prompt');
  },

  /**
   * Checks permission state on app launch and shows the custom "Stay in the Loop!"
   * first-time notification popup if permission is not granted and not dismissed.
   */
  async ensurePermissionSilently() {
    if (this._ensurePermInFlight) return this._ensurePermInFlight;
    this._ensurePermInFlight = (async () => {
      try {
        const perm = await this.detectActualOsPermission();
        await this.loadNotificationSettingsFromBackend();
        await this.consumePendingNotificationClick();

        if (perm === 'granted') {
          state.showFirstTimeNotifPopup = false;
          syncFirstTimeNotifPopupOverlayDOM();
          await this.registerDeviceWithBackend('granted');
          return true;
        }

        const dismissedLocal = localStorage.getItem('au_notif_onboarding_dismissed_v1') === 'true';
        const dismissedCloud = Boolean(state.notificationSettings?.onboarding_dismissed);
        // On native apps (Android/Windows/macOS), never let another device's cloud onboarding_dismissed
        // flag block asking for OS notification permission on a fresh device install.
        const shouldSuppressPrompt = isTauriAppEnvironment()
          ? dismissedLocal
          : (dismissedLocal || dismissedCloud);
        if (!this.onboardingEvaluated && !shouldSuppressPrompt && state.session) {
          this.onboardingEvaluated = true;
          state.showFirstTimeNotifPopup = true;
          state.firstTimeNotifDeniedGuide = perm === 'denied';
          syncFirstTimeNotifPopupOverlayDOM();
        }
        return false;
      } finally {
        this._ensurePermInFlight = null;
      }
    })();
    return this._ensurePermInFlight;
  },

  /**
   * Triggers the real native OS notification permission flow (Android POST_NOTIFICATIONS,
   * macOS UNUserNotificationCenter, Windows Toast registration, or Web Notification API).
   * Never fakes 'granted' — returns true ONLY if the OS actually granted permission.
   */
  async requestPermissionInteractive() {
    // 1. Direct Android Kotlin @JavascriptInterface bridge (MainActivity.kt)
    if (typeof window !== 'undefined' && window.AnimeNativeBridge && typeof window.AnimeNativeBridge.requestPermission === 'function') {
      try {
        const current = this.normalizePermissionValue(window.AnimeNativeBridge.getPermissionStatus());
        if (current === 'granted') {
          this.applyResolvedOsPermission('granted');
          return true;
        }
        const bridgeStatus = await new Promise((resolve) => {
          let settled = false;
          const finish = (val) => {
            if (settled) return;
            settled = true;
            delete window.__onAnimeNativeNotifPermResult;
            resolve(val);
          };
          window.__onAnimeNativeNotifPermResult = (statusStr) => {
            finish(this.normalizePermissionValue(statusStr) || 'denied');
          };
          setTimeout(() => {
            try {
              finish(this.normalizePermissionValue(window.AnimeNativeBridge.getPermissionStatus()) || 'denied');
            } catch (_) {
              finish('denied');
            }
          }, 45000);
          window.AnimeNativeBridge.requestPermission();
        });
        const finalPerm = this.applyResolvedOsPermission(bridgeStatus === 'granted' ? 'granted' : 'denied');
        return finalPerm === 'granted';
      } catch (_) {}
    }

    const invoke = this.getTauriInvoke();
    if (invoke) {
      const plat = state.deviceRegistration?.platform || this.detectClientPlatform();

      // 2. On macOS & Windows desktop, ALWAYS call our native Rust command FIRST:
      //    - macOS: triggers real UNUserNotificationCenter.requestAuthorization dialog
      //    - Windows: registers AUMID in HKCU + verifies real Windows Settings registry toggles
      if (plat === 'macos' || plat === 'windows') {
        try {
          const rustPerm = await invoke('request_os_notification_permission');
          const parsedRust = this.normalizePermissionValue(rustPerm);
          if (parsedRust === 'granted') {
            this.applyResolvedOsPermission('granted');
            return true;
          }
          if (parsedRust === 'denied') {
            this.applyResolvedOsPermission('denied');
            return false;
          }
        } catch (_) {}
      }

      // 3. Official tauri-plugin-notification mobile permission request commands (Android fallback)
      for (const checkCmd of ['plugin:notification|check_permissions', 'plugin:notification|permission_state']) {
        try {
          const preCheck = await invoke(checkCmd);
          if (this.normalizePermissionValue(preCheck) === 'granted') {
            this.applyResolvedOsPermission('granted');
            return true;
          }
        } catch (_) {}
      }

      for (const reqCmd of [
        'plugin:notification|request_permissions',
        'plugin:notification|request_permission',
        'request_os_notification_permission'
      ]) {
        try {
          const reqRes = await Promise.race([
            invoke(reqCmd),
            new Promise((resolve) => setTimeout(() => resolve('__timeout__'), 12000))
          ]);
          if (reqRes !== '__timeout__') {
            const parsed = this.normalizePermissionValue(reqRes);
            if (parsed === 'granted') {
              this.applyResolvedOsPermission('granted');
              return true;
            }
            if (parsed === 'denied') {
              this.applyResolvedOsPermission('denied');
              return false;
            }
          }
        } catch (_) {}
      }
    }

    // 4. Standard Web Browser Notification API (outside Tauri)
    try {
      if (!isTauriAppEnvironment() && typeof Notification !== 'undefined' && typeof Notification.requestPermission === 'function') {
        const perm = await Notification.requestPermission();
        const parsed = this.normalizePermissionValue(perm);
        const finalPerm = this.applyResolvedOsPermission(parsed === 'granted' ? 'granted' : 'denied');
        return finalPerm === 'granted';
      }
    } catch (_) {}

    const verified = await this.detectActualOsPermission();
    if (verified === 'granted') return true;
    this.applyResolvedOsPermission('denied');
    return false;
  },

  async handleFirstTimeEnableClicked() {
    if (state.notifPopupRequesting) return;
    state.notifPopupRequesting = true;
    syncFirstTimeNotifPopupOverlayDOM();

    let granted = false;
    try {
      granted = await this.requestPermissionInteractive();
    } catch (_) {}

    state.notifPopupRequesting = false;

    if (granted) {
      state.showFirstTimeNotifPopup = false;
      state.firstTimeNotifDeniedGuide = false;
      state.notificationSettings.onboarding_dismissed = true;
      try { localStorage.setItem('au_notif_onboarding_dismissed_v1', 'true'); } catch (_) {}
      syncFirstTimeNotifPopupOverlayDOM();
      if (state.tab === 'settings') renderApp();
      showToast('Notifications enabled!');
      this.registerDeviceWithBackend('granted').catch(() => {});
      this.saveNotificationSettingsToBackend({
        os_permission_status: 'granted',
        onboarding_dismissed: true
      }).catch(() => {});
    } else {
      // Do NOT fake success when the OS did not grant permission:
      // Show the honest system settings guide inside the popup without re-mounting or glitching
      state.firstTimeNotifDeniedGuide = true;
      syncFirstTimeNotifPopupOverlayDOM();
      if (state.tab === 'settings') renderApp();
      showToast('OS notification permission is not granted. Enable it in System Settings.', true);
      this.registerDeviceWithBackend('denied').catch(() => {});
      this.saveNotificationSettingsToBackend({
        os_permission_status: 'denied'
      }).catch(() => {});
    }
  },

  async handleFirstTimeMaybeLaterClicked() {
    state.notifPopupRequesting = false;
    state.showFirstTimeNotifPopup = false;
    state.firstTimeNotifDeniedGuide = false;
    state.notificationSettings.onboarding_dismissed = true;
    try { localStorage.setItem('au_notif_onboarding_dismissed_v1', 'true'); } catch (_) {}
    syncFirstTimeNotifPopupOverlayDOM();
    if (state.session) {
      this.saveNotificationSettingsToBackend({
        onboarding_dismissed: true,
        os_permission_status: String(state.osNotifPermission || 'prompt')
      }).catch(() => {});
    }
  },

  async openOsNotificationSettings() {
    if (typeof window !== 'undefined' && window.AnimeNativeBridge && typeof window.AnimeNativeBridge.openNotificationSettings === 'function') {
      try {
        const opened = window.AnimeNativeBridge.openNotificationSettings();
        if (opened) {
          showToast('Opened Android notification settings');
          return;
        }
      } catch (_) {}
    }
    const invoke = this.getTauriInvoke();
    if (invoke) {
      try {
        const opened = await invoke('open_os_notification_settings');
        if (opened) {
          showToast('Opened system notification settings');
          return;
        }
      } catch (_) {}
    }
    const plat = state.deviceRegistration?.platform || this.detectClientPlatform();
    if (plat === 'android') {
      showToast('Open Android Settings → Apps → Anime Universe → Notifications → Turn ON Allow notifications');
    } else if (plat === 'macos') {
      showToast('Open macOS System Settings → Notifications → Anime Universe');
    } else if (plat === 'windows') {
      showToast('Open Windows Settings → System → Notifications → Anime Universe');
    } else {
      showToast('Click the lock/settings icon in your address bar to allow notifications');
    }
  },

  async invokeEdgeFunction(functionName, payload = {}) {
    const token = state.session?.accessToken;
    if (!token) throw new Error('Authentication required');
    const url = `${getVaultBaseUrl()}/functions/v1/${functionName}`;
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        apikey: getVaultPublicKey(),
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify(payload)
    });
    if (!res.ok) {
      const errBody = await res.json().catch(() => ({}));
      throw new Error(errBody.error || `Edge Function ${functionName} failed (${res.status})`);
    }
    return res.json();
  },

  async registerDeviceWithBackend(permissionOverride = null) {
    if (!state.session?.userId) return null;
    await this.detectActualOsPermission();
    const deviceId = this.getOrCreateDeviceId();
    const platform = state.deviceRegistration.platform || this.detectClientPlatform();
    const perm = String(permissionOverride || state.osNotifPermission || 'prompt').toLowerCase();
    const pushToken = state.deviceRegistration.push_token || null;
    const defaultProvider =
      pushToken
        ? (platform === 'android' ? 'fcm' : platform === 'macos' ? 'apns' : platform === 'windows' ? 'wns' : 'webpush')
        : 'tauri_local';
    const pushProvider = state.deviceRegistration.push_provider || defaultProvider;

    state.deviceRegistration.permission_status = perm;
    state.deviceRegistration.is_active = perm === 'granted' && state.notificationSettings.master_enabled !== false;
    await this.syncLocalPrefsToNative();

    // 1. Try register-device Edge Function first
    try {
      const resp = await this.invokeEdgeFunction('register-device', {
        device_id: deviceId,
        platform,
        push_token: pushToken,
        push_provider: pushProvider,
        permission_status: perm,
        device_name: `${platform.toUpperCase()} Client`,
        app_version: '2.1.0'
      });
      if (resp?.device) {
        state.deviceRegistration.is_active = Boolean(resp.device.is_active);
        state.deviceRegistration.registered_at = resp.device.updated_at || new Date().toISOString();
        return resp.device;
      }
    } catch (_) {}

    // 2. Fallback to upsert_user_device RPC or direct RLS table upsert on public.user_devices
    try {
      const rpcRow = await supabaseRequest('/rest/v1/rpc/upsert_user_device', 'POST', {
        p_device_id: deviceId,
        p_platform: platform,
        p_push_token: pushToken,
        p_push_provider: pushProvider,
        p_permission_status: perm,
        p_device_name: `${platform.toUpperCase()} Client`,
        p_app_version: '2.1.0'
      });
      if (rpcRow) {
        state.deviceRegistration.is_active = perm === 'granted';
        state.deviceRegistration.registered_at = new Date().toISOString();
        return rpcRow;
      }
    } catch (_) {
      try {
        await supabaseRequest(
          '/rest/v1/user_devices?on_conflict=user_id,device_id',
          'POST',
          {
            user_id: state.session.userId,
            device_id: deviceId,
            platform,
            push_token: pushToken,
            push_provider: pushProvider,
            permission_status: perm,
            is_active: perm === 'granted',
            last_seen_at: new Date().toISOString()
          },
          'resolution=merge-duplicates,return=minimal'
        );
        state.deviceRegistration.is_active = perm === 'granted';
        state.deviceRegistration.registered_at = new Date().toISOString();
      } catch (_2) {}
    }
    return null;
  },

  async unregisterDeviceFromBackend() {
    if (!state.session?.userId) return;
    const deviceId = this.getOrCreateDeviceId();
    try {
      await this.invokeEdgeFunction('unregister-device', { device_id: deviceId });
      return;
    } catch (_) {}
    try {
      await supabaseRequest(
        `/rest/v1/user_devices?user_id=eq.${encodeURIComponent(state.session.userId)}&device_id=eq.${encodeURIComponent(deviceId)}`,
        'PATCH',
        { is_active: false, push_token: null }
      );
    } catch (_) {}
  },

  async loadNotificationSettingsFromBackend() {
    const uid = state.session?.userId;
    if (!uid) return;
    try {
      const rows = await supabaseRequest(
        `/rest/v1/user_notification_settings?select=*&user_id=eq.${encodeURIComponent(uid)}&limit=1`,
        'GET'
      );
      if (Array.isArray(rows) && rows[0]) {
        const r = rows[0];
        state.notificationSettings = {
          ...state.notificationSettings,
          master_enabled: r.master_enabled !== false,
          messages_enabled: r.messages_enabled !== false,
          updates_enabled: r.updates_enabled !== false,
          reminders_enabled: r.reminders_enabled !== false,
          os_permission_status: state.osNotifPermission || r.os_permission_status || 'prompt',
          onboarding_dismissed:
            Boolean(r.onboarding_dismissed) ||
            localStorage.getItem('au_notif_onboarding_dismissed_v1') === 'true',
          loaded: true
        };
        if (r.onboarding_dismissed) {
          try { localStorage.setItem('au_notif_onboarding_dismissed_v1', 'true'); } catch (_) {}
        }
      }
      const devRows = await supabaseRequest(
        `/rest/v1/user_devices?select=*&user_id=eq.${encodeURIComponent(uid)}&device_id=eq.${encodeURIComponent(this.getOrCreateDeviceId())}&limit=1`,
        'GET'
      ).catch(() => []);
      if (Array.isArray(devRows) && devRows[0]) {
        state.deviceRegistration.is_active = Boolean(devRows[0].is_active);
        state.deviceRegistration.registered_at = devRows[0].updated_at;
        if (devRows[0].push_token && !state.deviceRegistration.push_token) {
          state.deviceRegistration.push_token = devRows[0].push_token;
        }
      }
      await this.syncLocalPrefsToNative();
    } catch (_) {}
  },

  async saveNotificationSettingsToBackend(patch = {}) {
    const uid = state.session?.userId;
    Object.assign(state.notificationSettings, patch);
    await this.syncLocalPrefsToNative();
    if (!uid) return;

    const payload = {
      master_enabled: state.notificationSettings.master_enabled !== false,
      messages_enabled: state.notificationSettings.messages_enabled !== false,
      updates_enabled: state.notificationSettings.updates_enabled !== false,
      reminders_enabled: state.notificationSettings.reminders_enabled !== false,
      os_permission_status: String(state.osNotifPermission || state.notificationSettings.os_permission_status || 'prompt'),
      onboarding_dismissed: Boolean(state.notificationSettings.onboarding_dismissed),
      device_id: this.getOrCreateDeviceId()
    };

    try {
      await this.invokeEdgeFunction('update-notification-settings', payload);
      return;
    } catch (_) {}

    try {
      await supabaseRequest(
        '/rest/v1/user_notification_settings?on_conflict=user_id',
        'POST',
        {
          user_id: uid,
          master_enabled: payload.master_enabled,
          messages_enabled: payload.messages_enabled,
          updates_enabled: payload.updates_enabled,
          reminders_enabled: payload.reminders_enabled,
          os_permission_status: payload.os_permission_status,
          onboarding_dismissed: payload.onboarding_dismissed
        },
        'resolution=merge-duplicates,return=minimal'
      );
    } catch (_) {}
  },

  async updateNotificationSetting(key, nextValue) {
    state.notificationSettings[key] = Boolean(nextValue);
    renderApp();
    await this.saveNotificationSettingsToBackend({ [key]: Boolean(nextValue) });
    showToast('Notification preferences saved');
  },

  async consumePendingNotificationClick() {
    const invoke = this.getTauriInvoke();
    if (!invoke) return;
    try {
      const clickData = await invoke('consume_pending_notification_click');
      if (!clickData || typeof clickData !== 'object') return;
      if (clickData.notification_id) {
        await markNotificationRead(String(clickData.notification_id)).catch(() => {});
      }
      const aid = Number(clickData.anime_id || 0);
      if (aid > 0) {
        await openAnimeDetail(aid);
      } else if (clickData.target_screen === 'community') {
        state.tab = 'community';
        state.communityViewTab = 'tagged';
        loadUserNotifications(true);
        renderApp();
      } else {
        state.tab = 'notifications';
        loadUserNotifications(true);
        renderApp();
      }
    } catch (_) {}
  },

  async sendTestNotification() {
    await this.detectActualOsPermission();
    if (state.notificationSettings?.master_enabled === false) {
      showToast('Master notifications are turned off. Enable them first.', true);
      return;
    }
    if (state.osNotifPermission !== 'granted') {
      const granted = await this.requestPermissionInteractive();
      if (!granted) {
        showToast('App does not have OS notification permission. Tap "System Notification Settings" to allow notifications.', true);
        renderApp();
        return;
      }
      await this.registerDeviceWithBackend('granted');
    }

    const title = 'Anime Universe • Notification Verified';
    const message = `Native & cloud notification system is active on ${String(state.deviceRegistration.platform || 'your device').toUpperCase()}.`;
    const testId = `test_${Date.now()}`;

    const osDelivered = await this.sendOsNotificationBar(title, message, {
      notificationId: testId,
      type: 'system'
    });

    if (!osDelivered) {
      showToast('OS did not allow sending the notification. Enable notifications for Anime Universe in your system settings.', true);
      renderApp();
      return;
    }

    // Persist in Supabase via send-notification Edge Function (or direct table insert fallback)
    if (state.session?.userId) {
      let sentViaEdge = false;
      try {
        await this.invokeEdgeFunction('send-notification', {
          recipient_user_id: state.session.userId,
          title,
          message,
          type: 'system',
          metadata: {
            target_screen: 'notifications',
            sender_name: state.profile.username || 'Anime Universe',
            sender_avatar: state.profile.avatarKey || 'avatar_1'
          },
          deliver_remote_push: true
        });
        sentViaEdge = true;
      } catch (_) {}

      if (!sentViaEdge) {
        try {
          await supabaseRequest('/rest/v1/notifications', 'POST', {
            user_id: state.session.userId,
            sender_user_id: state.session.userId,
            title,
            message,
            type: 'system',
            is_read: false,
            metadata: {
              target_screen: 'notifications',
              sender_name: state.profile.username || 'Anime Universe',
              sender_avatar: state.profile.avatarKey || 'avatar_1'
            }
          });
        } catch (_) {}
      }
      refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
    }

    showToast('OS notification delivered!');
  },

  isCategoryEnabled(type) {
    const ns = state.notificationSettings || {};
    if (ns.master_enabled === false) return false;
    const t = String(type || 'message').toLowerCase();
    if (t === 'message' || t === 'reply' || t === 'mention' || t === 'thread_reply' || t === 'mention_reply' || t === 'mention_comment') {
      return ns.messages_enabled !== false;
    }
    if (t === 'update') return ns.updates_enabled !== false;
    if (t === 'reminder') return ns.reminders_enabled !== false;
    return true;
  },

  showInAppBanner(alertData) {
    if (!this.isCategoryEnabled(alertData?.type)) return;
    state.inAppNotifBanner = alertData;
    if (inAppBannerTimer) clearTimeout(inAppBannerTimer);
    inAppBannerTimer = setTimeout(() => {
      if (state.inAppNotifBanner && state.inAppNotifBanner.id === alertData.id) {
        state.inAppNotifBanner = null;
        const el = document.getElementById('in-app-notif-banner');
        if (el) el.remove();
      }
    }, 7000);
    renderApp();
  },

  async sendOsNotificationBar(title, body, data = {}) {
    if (!this.isCategoryEnabled(data?.type)) return false;
    const actualPerm = await this.detectActualOsPermission();
    if (actualPerm !== 'granted') return false;

    const cleanTitle = String(title || 'Anime Universe');
    const cleanBody = String(body || 'You have a new notification.');
    const notifType = String(data?.type || 'message');

    // 1) Direct Android Kotlin @JavascriptInterface bridge (MainActivity.kt)
    if (typeof window !== 'undefined' && window.AnimeNativeBridge) {
      try {
        if (typeof window.AnimeNativeBridge.sendLocalNotificationWithType === 'function') {
          if (window.AnimeNativeBridge.sendLocalNotificationWithType(cleanTitle, cleanBody, notifType)) {
            return true;
          }
        } else if (typeof window.AnimeNativeBridge.sendLocalNotification === 'function') {
          if (window.AnimeNativeBridge.sendLocalNotification(cleanTitle, cleanBody)) {
            return true;
          }
        }
      } catch (_) {}
    }

    // 2) Tauri v2 native Rust notification command (macOS UNUserNotificationCenter / Windows AUMID Toast)
    //    and official plugin:notification|notify fallback
    const invoke = this.getTauriInvoke();
    if (invoke) {
      try {
        await invoke('send_native_notification', { title: cleanTitle, body: cleanBody });
        return true;
      } catch (_) {}

      try {
        const numericId = Math.floor(Math.random() * 899999 + 100000);
        await invoke('plugin:notification|notify', {
          options: {
            id: numericId,
            title: cleanTitle,
            body: cleanBody
          }
        });
        return true;
      } catch (_) {}
    }

    // 3) Web Service Worker / Browser Notification API (outside Tauri)
    if (!isTauriAppEnvironment() && typeof Notification !== 'undefined' && Notification.permission === 'granted') {
      try {
        if (navigator.serviceWorker && navigator.serviceWorker.ready) {
          const reg = await navigator.serviceWorker.ready;
          if (reg && typeof reg.showNotification === 'function') {
            await reg.showNotification(cleanTitle, {
              body: cleanBody,
              icon: '/icon-192.png',
              badge: '/icon-192.png',
              tag: data.notificationId ? `au-notif-${data.notificationId}` : 'au-notif',
              renotify: true,
              data
            });
            return true;
          }
        }
        new Notification(cleanTitle, { body: cleanBody, icon: '/icon-192.png' });
        return true;
      } catch (_) {}
    }
    return false;
  },

  async dispatchAlert(payload) {
    if (!this.isCategoryEnabled(payload?.type)) return;
    const nid = String(payload?.id || '');
    if (!payload?.forceShow && nid && this.alertedIds.has(nid)) return;
    if (nid) {
      this.alertedIds.add(nid);
      this.knownUnreadIds.add(nid);
    }
    this.showInAppBanner(payload);
    const delivered = await this.sendOsNotificationBar(payload.title, payload.body, {
      notificationId: nid,
      type: payload.type || 'message',
      animeId: payload.animeId || null,
      animeTitle: payload.animeTitle || '',
      animeCover: payload.animeCover || '',
      commentId: payload.commentId || null,
      threadId: payload.threadId || null,
      replyId: payload.replyId || null
    });
    if (nid && (delivered || !isTauriAppEnvironment())) {
      this.markSeenInNative([nid]);
    }
  },

  async alertNewlyDetectedUnread(notifId, summary, rowMeta = null) {
    if (!notifId || this.alertedIds.has(notifId)) return;
    const rawList = Array.isArray(summary?.anime) ? summary.anime : [];
    const matchingAnime = rowMeta?.animeId
      ? rawList.find((a) => Number(a.anime_id) === Number(rowMeta.animeId))
      : null;
    const top = matchingAnime || rawList[0] || null;
    const ntype = rowMeta?.type || top?.latest_notification_type || 'message';
    if (!this.isCategoryEnabled(ntype)) return;
    const actionWord = ntype === 'reply' ? 'replied to you' : 'tagged you';
    const sender = rowMeta?.senderName || top?.latest_sender_username || 'Community Member';
    const title =
      (rowMeta?.title && String(rowMeta.title).trim()) ||
      (top?.anime_title ? `${sender} ${actionWord} • ${top.anime_title}` : 'Anime Universe • New Notification');
    const rawBody = (rowMeta?.body && String(rowMeta.body).trim()) || top?.latest_preview || '';
    const body = rawBody
      ? formatPlainPreviewWithEmojis(rawBody).slice(0, 140)
      : 'Open Anime Universe to view your new notification.';
    await this.dispatchAlert({
      id: notifId,
      title,
      body,
      type: ntype,
      senderName: sender,
      senderAvatar: rowMeta?.senderAvatar || top?.latest_sender_avatar_key || 'avatar_1',
      animeId: rowMeta?.animeId || top?.anime_id || null,
      animeTitle: top?.anime_title || '',
      animeCover: top?.anime_cover_image || '',
      commentId: top?.latest_comment_id || null,
      threadId: top?.latest_thread_id || null,
      replyId: top?.latest_reply_id || null
    });
  }
};

if (typeof navigator !== 'undefined' && navigator.serviceWorker) {
  navigator.serviceWorker.addEventListener('message', (event) => {
    if (event?.data?.type === 'AU_NOTIFICATION_CLICK') {
      const d = event.data.data || {};
      if (d.animeId) {
        openCommunityHubForAnime(d.animeId, {
          tab: 'discussion',
          fallbackTitle: d.animeTitle || '',
          fallbackCover: d.animeCover || '',
          notificationId: d.notificationId || null,
          jumpTarget: d.commentId || d.threadId || d.replyId ? {
            commentId: d.commentId || null,
            threadId: d.threadId || null,
            replyId: d.replyId || null
          } : null
        });
      } else {
        state.tab = 'notifications';
        loadUserNotifications(true);
        renderApp();
      }
    }
  });
}

// Real-time Supabase Notification Socket (public.notifications + public.user_notifications)
const NotificationRealtimeManager = {
  ws: null,
  heartbeatId: null,
  reconnectId: null,
  connectedUserId: null,
  connectedToken: null,
  lastActivityAt: 0,
  refCounter: 1,
  forceReconnect() {
    this.disconnect();
    this.ensureConnected();
  },
  ensureConnected() {
    const uid = state.session?.userId;
    const token = state.session?.accessToken;
    if (!uid || !token) {
      this.disconnect();
      return;
    }
    const staleSocket =
      this.ws &&
      this.ws.readyState === 1 &&
      this.lastActivityAt > 0 &&
      Date.now() - this.lastActivityAt > 60000;
    if (
      !staleSocket &&
      this.connectedUserId === uid &&
      this.connectedToken === token &&
      this.ws &&
      this.ws.readyState <= 1
    ) {
      return;
    }
    this.disconnect();
    const baseUrl = getVaultBaseUrl();
    const pubKey = getVaultPublicKey();
    if (!baseUrl.startsWith('http')) return;
    const wsUrl = `${baseUrl.replace(/^http/, 'ws')}/realtime/v1/websocket?apikey=${encodeURIComponent(pubKey)}&vsn=1.0.0`;
    const topic = `realtime:public:notifications_and_legacy:${uid}`;
    const legacyFilter = `recipient_user_id=eq.${uid}`;
    const modernFilter = `user_id=eq.${uid}`;
    try {
      const ws = new WebSocket(wsUrl);
      this.ws = ws;
      this.connectedUserId = uid;
      this.connectedToken = token;
      this.lastActivityAt = Date.now();
      ws.onopen = () => {
        this.lastActivityAt = Date.now();
        const joinRef = String(this.refCounter++);
        ws.send(JSON.stringify({
          topic,
          event: 'phx_join',
          payload: {
            config: {
              broadcast: { self: false },
              presence: { key: '' },
              postgres_changes: [
                { event: 'INSERT', schema: 'public', table: 'user_notifications', filter: legacyFilter },
                { event: 'UPDATE', schema: 'public', table: 'user_notifications', filter: legacyFilter },
                { event: 'DELETE', schema: 'public', table: 'user_notifications', filter: legacyFilter },
                { event: 'INSERT', schema: 'public', table: 'notifications', filter: modernFilter },
                { event: 'UPDATE', schema: 'public', table: 'notifications', filter: modernFilter },
                { event: 'DELETE', schema: 'public', table: 'notifications', filter: modernFilter }
              ]
            },
            access_token: token
          },
          ref: joinRef
        }));
        ws.send(JSON.stringify({ topic, event: 'access_token', payload: { access_token: token }, ref: String(this.refCounter++) }));
        this.heartbeatId = setInterval(() => {
          if (ws.readyState === 1) {
            if (this.lastActivityAt > 0 && Date.now() - this.lastActivityAt > 60000) {
              // Zombie TCP socket (common on Android after switching to Settings/background)
              this.forceReconnect();
              return;
            }
            try {
              ws.send(JSON.stringify({ topic: 'phoenix', event: 'heartbeat', payload: {}, ref: String(this.refCounter++) }));
            } catch (_) {
              this.forceReconnect();
            }
          }
        }, 25000);
        refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
      };
      ws.onmessage = (evt) => {
        this.lastActivityAt = Date.now();
        try {
          const msg = JSON.parse(evt.data);
          if (msg?.event !== 'postgres_changes') return;
          const data = msg?.payload?.data;
          if (!data || (data.table !== 'user_notifications' && data.table !== 'notifications')) return;
          const evType = String(data.type || '').toUpperCase();
          const rec = data.record;
          const oldRec = data.old_record;

          if (data.table === 'notifications') {
            if (evType === 'INSERT' && rec && rec.user_id === uid) {
              // Skip if this row is simply a bridge mirror of a legacy notification we already handled
              if (rec.legacy_notification_id && NativeNotificationBridge.knownUnreadIds.has(String(rec.legacy_notification_id))) {
                return;
              }
              const normalized = normalizeModernNotificationRow(rec);
              const outcome = NotificationSyncState.onRealtimeInsert({ id: normalized.id, recipient_user_id: uid, is_read: normalized.is_read }, uid);
              state.notifications.unreadCount = outcome.unreadCount;
              updateCommunityRedDotsInPlace();
              if (state.tab === 'notifications' || (state.tab === 'community' && state.communityViewTab === 'tagged')) {
                loadUserNotifications(true);
              } else {
                refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
              }
              NativeNotificationBridge.dispatchAlert({
                id: normalized.id,
                title: normalized.title,
                body: formatPlainPreviewWithEmojis(normalized.message).slice(0, 140),
                type: normalized.type,
                senderName: normalized.sender_username,
                senderAvatar: normalized.sender_avatar_key,
                animeId: normalized.anime_id,
                commentId: normalized.comment_id,
                threadId: normalized.thread_id,
                replyId: normalized.reply_id
              });
            } else if ((evType === 'UPDATE' || evType === 'DELETE') && (rec?.user_id === uid || oldRec?.user_id === uid)) {
              refreshUnreadNotificationCount().then(() => {
                updateCommunityRedDotsInPlace();
                if (state.tab === 'notifications' || (state.tab === 'community' && state.communityViewTab === 'tagged')) {
                  loadUserNotifications(true);
                }
              });
            }
            return;
          }

          if (!rec && !oldRec) return;
          if (rec && rec.recipient_user_id !== uid) return;

          if (evType === 'INSERT') {
            const outcome = NotificationSyncState.onRealtimeInsert(rec, uid);
            if (outcome.delta !== 0) {
              state.notifications.unreadCount = outcome.unreadCount;
              updateCommunityRedDotsInPlace();
              if (state.tab === 'notifications' || (state.tab === 'community' && state.communityViewTab === 'tagged')) {
                loadUserNotifications(true);
              } else {
                refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
              }
              if (rec.sender_user_id && rec.sender_user_id !== uid) {
                const preview = formatPlainPreviewWithEmojis(rec.preview_text || '').slice(0, 120);
                const notifTitle =
                  rec.notification_type === 'reply'
                    ? 'Anime Universe • New Reply'
                    : rec.notification_type === 'mention'
                    ? 'Anime Universe • Tagged You'
                    : 'Anime Universe • Community Alert';
                NativeNotificationBridge.dispatchAlert({
                  id: String(rec.id || `rt_${Date.now()}`),
                  title: notifTitle,
                  body: preview || 'You have a new reply or @mention in Community!',
                  type: rec.notification_type || 'message',
                  senderName: rec.sender_username || 'Member',
                  senderAvatar: rec.sender_avatar_key || 'avatar_1',
                  animeId: rec.anime_id ? Number(rec.anime_id) : null,
                  commentId: rec.comment_id || null,
                  threadId: rec.thread_id || null,
                  replyId: rec.reply_id || null
                });
              }
            }
          } else if (evType === 'UPDATE') {
            const outcome = NotificationSyncState.onRealtimeUpdate(oldRec, rec, uid);
            const updatedId = String(rec.id || '').trim();
            const newIsRead = Boolean(rec.is_read);
            const newReadAt = rec.read_at || null;
            if (updatedId && state.notifications.items.length > 0) {
              let changedItem = false;
              state.notifications.items = state.notifications.items.map((n) => {
                if (String(n.id) === updatedId) {
                  changedItem = true;
                  return { ...n, is_read: newIsRead, read_at: newReadAt || n.read_at || null };
                }
                return n;
              });
              if (changedItem && (state.tab === 'notifications' || (state.tab === 'community' && state.communityViewTab === 'tagged'))) {
                renderApp();
              }
            }
            state.notifications.unreadCount = outcome.unreadCount;
            if (outcome.unreadCount === 0) {
              state.notifications.unseenAnime = [];
            } else if (newIsRead && rec.anime_id) {
              const aid = Number(rec.anime_id);
              const stillUnreadForAnime = state.notifications.items.some((n) => Number(n.anime_id) === aid && !n.is_read);
              if (!stillUnreadForAnime && state.notifications.items.some((n) => String(n.id) === updatedId)) {
                state.notifications.unseenAnime = (state.notifications.unseenAnime || []).filter((u) => Number(u.anime_id) !== aid);
              }
            }
            updateCommunityRedDotsInPlace();
            if (outcome.needsReconcile) {
              refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
            }
          } else if (evType === 'DELETE') {
            refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
          }
        } catch (_) {}
      };
      ws.onclose = () => {
        if (this.heartbeatId) clearInterval(this.heartbeatId);
        this.heartbeatId = null;
        this.ws = null;
        if (state.session?.userId === uid) {
          this.reconnectId = setTimeout(() => this.ensureConnected(), 5000);
        }
      };
      ws.onerror = () => {};
    } catch (_) {}
  },
  disconnect() {
    if (this.heartbeatId) clearInterval(this.heartbeatId);
    if (this.reconnectId) clearTimeout(this.reconnectId);
    this.heartbeatId = null;
    this.reconnectId = null;
    this.connectedUserId = null;
    this.connectedToken = null;
    if (this.ws) {
      try { this.ws.close(); } catch (_) {}
      this.ws = null;
    }
  }
};

// Reconcile notifications immediately on return to app / reconnect (unthrottled)
let lastNotifReconcileAt = 0;
function reconcileNotificationsOnReturn(force = false) {
  if (!state.session) return;
  const now = Date.now();
  if (!force && now - lastNotifReconcileAt < 2500) return;
  lastNotifReconcileAt = now;
  NativeNotificationBridge.syncAuthToTauri(state.session);
  NativeNotificationBridge.syncLocalPrefsToNative();
  NativeNotificationBridge.consumePendingNotificationClick();
  if (force) {
    NotificationRealtimeManager.forceReconnect();
  } else {
    NotificationRealtimeManager.ensureConnected();
  }
  refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
}

// Gentle background sync every 30s + fast notification unread reconcile every 15s
let lastSilentSyncAt = 0;
function triggerThrottledSilentSync() {
  const now = Date.now();
  if (!state.session || document.hidden) return;
  reconcileNotificationsOnReturn(false);
  if (now - lastSilentSyncAt < 15000) return;
  lastSilentSyncAt = now;
  loadCloudData(true);
}

setInterval(() => {
  if (!document.hidden && state.tab === 'community' && state.communityViewTab === 'random') {
    loadDailyRandomAnime(false);
  }
  if (state.session && (!document.hidden || isTauriAppEnvironment())) {
    NotificationRealtimeManager.ensureConnected();
    refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
  }
}, 15000);

setInterval(() => {
  if (state.session && !document.hidden) {
    lastSilentSyncAt = Date.now();
    loadCloudData(true);
  }
}, 30000);

if (state.session) {
  NativeNotificationBridge.syncAuthToTauri(state.session);
  NativeNotificationBridge.ensurePermissionSilently();
  NotificationRealtimeManager.ensureConnected();
  refreshUnreadNotificationCount().then(() => updateCommunityRedDotsInPlace());
} else {
  NativeNotificationBridge.detectActualOsPermission();
}

function handleNativePermissionStateSync(perm) {
  const normalized = NativeNotificationBridge.normalizePermissionValue(perm);
  if (!normalized) return;
  const prev = state.osNotifPermission;
  NativeNotificationBridge.applyResolvedOsPermission(normalized);

  if (normalized === 'granted') {
    if (state.showFirstTimeNotifPopup) {
      state.showFirstTimeNotifPopup = false;
      state.firstTimeNotifDeniedGuide = false;
      state.notificationSettings.onboarding_dismissed = true;
      try { localStorage.setItem('au_notif_onboarding_dismissed_v1', 'true'); } catch (_) {}
      syncFirstTimeNotifPopupOverlayDOM();
      showToast('Notifications enabled in system settings!');
    }
    if (state.session) {
      NativeNotificationBridge.syncAuthToTauri(state.session);
      NativeNotificationBridge.syncLocalPrefsToNative();
      if (prev !== 'granted') {
        NativeNotificationBridge.registerDeviceWithBackend('granted').catch(() => {});
        NativeNotificationBridge.saveNotificationSettingsToBackend({
          os_permission_status: 'granted',
          onboarding_dismissed: true
        }).catch(() => {});
        reconcileNotificationsOnReturn(true);
      }
    }
  } else if (state.session && prev !== normalized) {
    NativeNotificationBridge.registerDeviceWithBackend(normalized).catch(() => {});
    NativeNotificationBridge.saveNotificationSettingsToBackend({
      os_permission_status: normalized
    }).catch(() => {});
  }

  if (state.tab === 'settings' && prev !== normalized) {
    renderApp();
  }
}

if (typeof window !== 'undefined') {
  window.__onAnimeNativeNotifPermChanged = (statusStr) => {
    handleNativePermissionStateSync(statusStr);
  };
}

scheduleNextIstMidnightResetWeb();

window.addEventListener('focus', () => {
  checkDailyRandomIstRolloverOnResumeWeb();
  reconcileNotificationsOnReturn(true);
  triggerThrottledSilentSync();
  NativeNotificationBridge.detectActualOsPermission()
    .then((perm) => handleNativePermissionStateSync(perm))
    .catch(() => {});
});

document.addEventListener('visibilitychange', () => {
  if (!document.hidden) {
    checkDailyRandomIstRolloverOnResumeWeb();
    reconcileNotificationsOnReturn(true);
    triggerThrottledSilentSync();
  }
});

window.addEventListener('online', () => {
  checkDailyRandomIstRolloverOnResumeWeb();
  reconcileNotificationsOnReturn(true);
});

// ==========================================================================
// NATIVE SHELL GUARDS (Context Menu & Browser Shortcut Lockdown with Cleanup)
// ==========================================================================
let teardownNativeShellGuards = null;

function isEditableNativeTarget(target) {
  if (!target || typeof target !== 'object') return false;
  if (typeof target.closest === 'function') {
    return Boolean(target.closest('input, textarea, [contenteditable="true"]'));
  }
  const tag = String(target.tagName || '').toUpperCase();
  return tag === 'INPUT' || tag === 'TEXTAREA' || Boolean(target.isContentEditable);
}

function mountNativeShellGuards() {
  if (typeof teardownNativeShellGuards === 'function') {
    teardownNativeShellGuards();
  }

  const handleContextMenu = (e) => {
    if (isEditableNativeTarget(e.target)) return;
    e.preventDefault();
  };

  const handleKeyDown = (e) => {
    if (!(e.ctrlKey || e.metaKey)) return;
    const key = String(e.key || '').toLowerCase();
    if (key === 's' || key === 'p' || key === 'u') {
      e.preventDefault();
    }
  };

  const cleanup = () => {
    document.removeEventListener('contextmenu', handleContextMenu);
    window.removeEventListener('keydown', handleKeyDown);
    window.removeEventListener('pagehide', cleanup);
    if (teardownNativeShellGuards === cleanup) {
      teardownNativeShellGuards = null;
    }
  };

  document.addEventListener('contextmenu', handleContextMenu);
  window.addEventListener('keydown', handleKeyDown);
  window.addEventListener('pagehide', cleanup);
  teardownNativeShellGuards = cleanup;
  return cleanup;
}

mountNativeShellGuards();

