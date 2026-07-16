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

### Folders
- «Edit folders» in the multi-select chat list — add or remove chats from any folder; available from every tab, each folder marked all/partial/none of the selection

### Other
- Bookmarks manager in the chats menu (instead of a settings link)

## Technical
- Separate package `nu.gpu.nagramxturbo` — an independent app (doesn't update from Nagram X; chats live on the server)
- Separate keystore and remote-config channel
- Builds for `arm64-v8a` (64-bit) and `armeabi-v7a` (32-bit); in-app update auto-picks the matching APK by device arch

## Fixes
- Settings backup now keeps custom API (id/hash) and notification color (previously lost)
- Crash fix when loading incomplete emoji packs
- Fixed HDR photo (Ultra HDR) darkening in the media viewer on HDR screens; "Photo HDR" toggle (Chat → Media) is on by default
- Fixed contacts sync in the system phonebook and dialer
- Background music now pauses or ducks per the "Pause music on media" setting when playing voice messages, round videos, and videos
- Background music resumes automatically after playback
