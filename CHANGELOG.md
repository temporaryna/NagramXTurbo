# Changelog

Differences between Nagram X Turbo and [Nagram X](https://github.com/risin42/NagramX).

## Upstream

Synced with the final Nagram X release (12.10.0). User-visible from upstream since 12.9.2:
- Welcome Messages for groups: auto-greet new members, admin permissions, revert to original
- Link / copy-text / profile buttons in the rich editor for channel posts; table styling in articles
- Gift messages: add a note to a gift, preview in chat, make the message public
- "Joined via community" service messages
- Nagram X's own last additions: Google Vertex LLM provider, font weight fallback option, video seek overlay hiding

## Features

### Share sheet / forwarding
- Folder tabs for quick chat/folder selection
- Swipe between folder tabs with the content following your finger (like in the chat list)
- Remembers the last opened folder across launches
- Forwarding toggles: hide sender / hide caption / silent
- Long-press "Send": "Send later" + "Send when online" (carries over the mute state)
- Comment position toggle (before/after the forwarded message) beside the send button when a comment is typed
- Forward from protected chats as copies: "Forward" works in chats with protected content (text, media, stickers, documents); asks before sending — once / always / never, setting in Nagram X → Chat
- Edit text when forwarding: pencil button in the share sheet loads the message text into the comment field — edit before sending; changed text is sent as a copy without a forward label, unchanged goes as a normal forward
- Text formatting in the share sheet comment field: select text to get the chat-style menu — bold, italic, mono, spoiler, links and more

### Custom fonts
- Pick a font by category: Regular / Bold / Italic / Mono
- Import your own `.ttf`/`.otf` straight from chat (tap — preview, long-press — apply)
- Text size in every input field + cursor scaling

### Media viewer
- Swipe GIFs with photos and videos (toggle, off by default)
- In large/old chats, swipe left/right works right away on open
- Optional scroll to the seen photo on close (Chat → Media)
- Seamless video: opens at the inline preview position, resumes on close, resumes on scroll-back (cached videos; Chat → Media, off by default; beta)
- Auto-rotate media button in the viewer with three modes: off / fit content / always (media turns to fill the screen by its aspect); the same setting with animated icons in Chat → Media, off by default

### Deleted messages
- Filtering by chat category

### Input field
- iOS-style input bar (Chat settings, off by default): attachment on the left, emoji inside-right, optional compact mode, glass capsule with round bubbles behind the buttons
- Action button style (Chat settings): Accent / Neutral / White for the send, voice and apply buttons — applies everywhere the action button shows (input bar, share sheet, photo picker, attach menu, rich editor), in glass form when the iOS bar is on and as a solid circle otherwise; replaces the white-send toggle
- Recorded voice review: the delete button is a separate round glass button matching the bar; the timeline has balanced insets

### App updates
- In-app check and download of new dev builds (manual button + automatic, once a day)

### Folders
- «Edit folders» in the multi-select chat list — add or remove chats from any folder; available from every tab, each folder marked all/partial/none of the selection
- The picker stays open so you can edit several folders at once; «Reset» reverts all changes made in that session
- Secret chats can't be added to folders — they're skipped with a notice

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
- Fixed stutter of the send/voice button background when typing or erasing text (iOS input appearance)
- Fixed the send-as avatar overlapping the delete icon when canceling a voice message
- Bot menu pill now sits inside the input capsule with proper text spacing (iOS appearance)
- Folder tabs, search results and forward toggles now fade out smoothly when picking a topic in the share sheet
- Fixed picking the wrong chat in the share menu after switching folders
- Rich editor send button now follows the input bar styling
- iOS input bar: send, voice and apply buttons now take the chat theme colors in glass form, keeping the icon visible on any theme; the long-press send preview matches the bar button
