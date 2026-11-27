const CACHE_NAME = 'anime-universe-pwa-v2.28.0';
const EMOJI_CACHE_NAME = 'community-emoji-cache-v1';
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/styles.css',
  '/assets-bundle.js',
  '/app.js',
  '/manifest.json',
  '/icon-192.png',
  '/icon-512.png',
  '/assets/img_login_mascot.png',
  '/assets/sandrone/sandrone.png',
  '/assets/avatar_1.png',
  '/assets/avatar_2.png',
  '/assets/avatar_3.png',
  '/assets/avatar_4.png',
  '/assets/avatar_5.png',
  '/assets/avatar_6.png',
  '/assets/avatar_7.png',
  '/assets/avatar_8.png',
  '/assets/avatar_9.png',
  '/assets/frames/frame_1.png',
  '/assets/frames/frame_2.png',
  '/assets/frames/frame_3.png',
  '/assets/frames/frame_4.png',
  '/assets/frames/frame_5.png',
  '/assets/frames/frame_6.png',
  '/assets/frames/frame_7.png',
  '/assets/frames/frame_8.png',
  '/assets/frames/frame_9.png',
  '/assets/frames/frame_10.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(STATIC_ASSETS)).catch(() => {})
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.filter((key) => key !== CACHE_NAME && key !== EMOJI_CACHE_NAME).map((key) => caches.delete(key))
      )
    )
  );
  self.clients.claim();
});

self.addEventListener('push', (event) => {
  let payload = {};
  try {
    payload = event.data ? event.data.json() : {};
  } catch (_) {
    payload = { body: event.data ? event.data.text() : '' };
  }
  const title = payload.title || 'Anime Universe';
  const body = payload.body || 'You have a new community notification.';
  const data = payload.data || {};
  event.waitUntil(
    self.registration.showNotification(title, {
      body,
      icon: '/icon-192.png',
      badge: '/icon-192.png',
      tag: data.notificationId ? `au-notif-${data.notificationId}` : 'au-notif',
      renotify: true,
      data
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const data = event.notification.data || {};
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) {
          client.focus();
          client.postMessage({ type: 'AU_NOTIFICATION_CLICK', data });
          return;
        }
      }
      if (self.clients.openWindow) {
        return self.clients.openWindow('/');
      }
      return undefined;
    })
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (/\/storage\/v1\/object\/public\/community[_-]emojis\//i.test(url.pathname)) {
    event.respondWith(
      caches.open(EMOJI_CACHE_NAME).then(async (cache) => {
        const cached = await cache.match(request);
        if (cached) return cached;
        const net = await fetch(request);
        if (net && net.status === 200) {
          cache.put(request, net.clone()).catch(() => {});
        }
        return net;
      })
    );
    return;
  }

  if (
    url.pathname.startsWith('/api/') ||
    url.pathname.startsWith('/downloads/') ||
    url.hostname.includes('supabase.co') ||
    url.hostname.includes('anilist.co') ||
    url.hostname.includes('jikan.moe')
  ) {
    return;
  }

  event.respondWith(
    fetch(request)
      .then((networkResponse) => {
        if (networkResponse && networkResponse.status === 200 && url.origin === self.location.origin) {
          const ct = (networkResponse.headers.get('content-type') || '').toLowerCase();
          const isAssetPath = /\.(png|jpg|jpeg|webp|svg|gif|mp3|css|js|json|woff2?)$/i.test(url.pathname);
          if (!(isAssetPath && ct.includes('text/html'))) {
            const clone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, clone)).catch(() => {});
          }
        }
        return networkResponse;
      })
      .catch(() => caches.match(request).then((cached) => cached || caches.match('/index.html')))
  );
});
