# Changelog

Differences between Nagram X Turbo and [Nagram X](https://github.com/risin42/NagramX).

## Features

### Share sheet / forwarding
- Folder tabs for quick chat/folder selection
- Remembers the last opened folder across launches
- Forwarding toggles: hide sender / hide caption / silent
- Long-press "Send": "Send later" + "Send when online" (carries over the mute state)

### Custom fonts
- Pick a font by category: Regular / Bold / Italic / Mono
- Import your own `.ttf`/`.otf` straight from chat (tap — preview, long-press — apply)
- Text size in every input field + cursor scaling

### Media viewer
- Swipe GIFs with photos and videos (toggle, off by default)
- In large/old chats, swipe left/right works right away on open
- Optional scroll to the seen photo on close (Chat → Media)
- Seamless video: opens at the inline preview position, resumes on close, resumes on scroll-back (cached videos; Chat → Media, off by default; beta)
- Force media auto-rotate (Chat → Media, off by default)

### Deleted messages
- Filtering by chat category

### App updates
- In-app check and download of new dev builds (manual button + automatic, once a day)

### Other
- Bookmarks manager in the chats menu (instead of a settings link)

## Technical
- Separate package `nu.gpu.nagramxturbo` — an independent app (doesn't update from Nagram X; chats live on the server)
- Separate keystore and remote-config channel

## Fixes
- Settings backup now keeps custom API (id/hash) and notification color (previously lost)
- Crash fix when loading incomplete emoji packs
- Worked around HDR photo (Ultra HDR) darkening in the media viewer: HDR display for photos is off by default (the HDR render path still darkens photos on some screens — root cause not fixed); new "Photo HDR" toggle (Chat → Media) re-enables it
