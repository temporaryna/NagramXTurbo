# Changelog

Differences between Nagram X Turbo and [Nagram X](https://github.com/risin42/NagramX).

## Upstream

Synced with Telegram 12.10.3. User-visible from upstream since 12.9.2:
- Welcome Messages for groups: auto-greet new members, admin permissions, revert to original
- WEB proxies: web-proxy transport in the proxy list (bypass blocking without MTProto)
- Upstream fixes and improvements from 12.10.1–12.10.3 (voip vibration, media player, misc)
- Link / copy-text / profile buttons in the rich editor for channel posts; table styling in articles
- Gift messages: add a note to a gift, preview in chat, make the message public
- "Joined via community" service messages
- Nagram X's own last additions: Google Vertex LLM provider, font weight fallback option, video seek overlay hiding

## Features

### Settings
- One place for all Turbo settings: a dedicated Turbo section in the Nagram X settings — app icon picker, input bar (iOS), action button style, media viewer, forwarding, deleted messages and fonts now live together
- Deleted messages settings redesign: chat-type categories (private chats, channels, groups) as a two-column Text/Media checkbox list right in the settings instead of hidden popups; all categories are on by default when the feature is first enabled
- About screen with the project's own links: the Turbo channel, our GitHub repository, and the current build version; opens from the version menu too, with a logo header

### App identity
- Own launcher identity: the Turbo theme as the new default and 15 icon themes in three picker sections (Standard / Turbo / hidden easter egg); monochrome icons sized for themed launchers
- "Modern classic" toggle for the classic themes
- Notification mark follows the app icon — one badge, no duplicates; long-press the preview for a test notification

### Base build
- ToS-friendly "base" APK variant: built and published alongside the regular one (download links side by side in the release post), without the disputable features — deleted/edited message saving, ghost mode, last seen history, regex filters, forward from protected chats, local premium. Base builds receive their own in-app updates.

### Share sheet / forwarding
- Folder tabs for quick chat/folder selection
- Swipe between folder tabs with the content following your finger (like in the chat list)
- Remembers the last opened folder across launches
- Forwarding toggles: hide sender / hide caption / silent
- Long-press "Send": "Send later" + "Send when online" (carries over the mute state)
- Comment position toggle (before/after the forwarded message) beside the send button when a comment is typed
- Forward from protected chats as copies: "Forward" works in chats with protected content (text, media, stickers, documents); asks before sending — once / always / never, setting in Turbo → Forwarding. Protected media shared from the media viewer, profile shared-media lists and the audio player asks too (no more silent sends)
- Media viewer share sheet: the viewer's forward menu now has all three ways side by side — Forward (chat picker), Fast forward (the full share sheet with folder tabs, toggles, text editing and comment position) and Forward without quote
- Fast forward everywhere: the share sheet is available as a "Fast forward" item in the message menu, the multi-select menu, the profile shared-media bar and the audio player; the option previously called "Direct share" (bottom bar left button) is renamed to Fast forward
- Configurable forward buttons: both selection-bar buttons work alike — tap performs the saved action (default Forward / no-quote), long-press picks the action; the two settings rows ("Left/Right action button") now live in Turbo → Forwarding, and the media viewer menu has its own Fast forward toggle
- Search results forwarding: protected content from chat search results now asks before sending copies instead of sending silently
- Edit text when forwarding: pencil button in the share sheet loads the message text into the comment field — edit before sending; changed text is sent as a copy without a forward label, unchanged goes as a normal forward; the caret lands at the end of the loaded text. The same pencil now lives in the standard forward flow too: beside the comment field in the chat picker and as an "Edit text" row in the single-target preview bar menu
- Text formatting in the share sheet comment field: select text to get the chat-style menu — bold, italic, mono, spoiler, links and more

### Custom fonts
- Pick a font by category: Regular / Bold / Italic / Mono
- Import your own `.ttf`/`.otf` straight from chat (tap — preview, long-press — apply)
- Text size in every input field + cursor scaling

### Media viewer
- Swipe GIFs with photos and videos (toggle, off by default)
- In large/old chats, swipe left/right works right away on open
- Optional scroll to the seen photo on close (Turbo → Media)
- Seamless video: opens at the inline preview position, resumes on close, resumes on scroll-back (cached videos; Turbo → Media, off by default; beta)
- Auto-rotate media button in the viewer with three modes: off / fit content / always (media turns to fill the screen by its aspect); the same setting with animated icons in Turbo → Media, off by default
- Media edit download: the editor pencil downloads missing media for you — files up to 100 MB start downloading right away with a progress dialog and the editor opens when done, larger files ask first; the pencil is now available for videos too
- Copy-forward download queue: forwarding from protected chats or with edited text no longer fails when the media isn't downloaded — missing files download in the background with a progress notification (asks for batches over 500 MB) and the copies are sent automatically; the comment now goes after the copies, and the result is reported honestly (sent X of Y)

### Deleted messages
- Filtering by chat category

### Chat
- Message date in bubbles (Turbo → Media, off by default): the sending date next to the time in every message — no need to open the message details to see when it was sent; date format selector (9 patterns with a live preview in the settings)
- Numeric-id profile links open any chat: tg://openmessage?user_id=, tg://user?id= and t.me/@id links (the ones our own profile sheet copies) now resolve unknown ids on the server and open the chat; failures show a clear message instead of a dead screen

### Input field
- iOS-style input bar (Turbo → Input Bar, off by default): attachment on the left, emoji inside-right, optional compact mode, glass capsule with round bubbles behind the buttons
- Input bar text size (Turbo → Input Bar): a separate text size for the message input field, independent from the chat text size — turn "Same as chat" off and pick a size on the 12–30 slider; changes apply to the open chat instantly and the input bar preview above the setting shows the result live
- Action button style (Turbo → Input Bar): Accent / Neutral / White for the send, voice and apply buttons — applies everywhere the action button shows (input bar, share sheet, photo picker, attach menu, rich editor), in glass form when the iOS bar is on and as a solid circle otherwise; replaces the white-send toggle
- Button outline (Turbo → Input Bar): a thin outline on the action buttons, colored exactly like the icon inside (theme palette), for the neutral and white styles in both themes and both bar looks — including the round iOS button bubbles; the row is hidden for the accent style, and the toggle removes the outlines everywhere
- Recorded voice review: the delete button is a separate round glass button matching the bar; the timeline has balanced insets

### App updates
- In-app check and download of new dev builds (manual button + automatic, once a day)

### Folders
- «Edit folders» in the multi-select chat list — add or remove chats from any folder; available from every tab, each folder marked all/partial/none of the selection
- The picker stays open so you can edit several folders at once; «Reset» reverts all changes made in that session
- Secret chats can't be added to folders — they're skipped with a notice

### Other
- Bookmarks manager in the chats menu (instead of a settings link)
- In-app update popup shows a random app icon sticker instead of the ducks; a Halloween one joins the rotation from October 10 to November 3

## Technical
- Separate package `nu.gpu.nagramxturbo` — an independent app (doesn't update from Nagram X; chats live on the server)
- Separate keystore and remote-config channel
- Builds for `arm64-v8a` (64-bit) and `armeabi-v7a` (32-bit); in-app update auto-picks the matching APK by device arch

## Fixes
- App icon: switching applies instantly on tap (with an optional restart prompt), survives process death, and no longer silently reverts to the default
- Notification mark in follow mode matches the icon's visual style: classic icons now show the plain Telegram plane instead of the turbo flame
- View-deleted: saving old deleted media no longer fails silently — when the file can't be recovered you now get a clear message instead of nothing happening
- Fixed plural string forms showing as raw placeholder text
- White action-button outline follows the same toggle and icon color as the neutral style, in both themes
- The disabled send button (slow mode) icon is now visible with the white action button style in the dark theme — it used to blend into the button
- The field text no longer slides under the silent-post bell in channels where you post (classic and iOS layouts)
- The sending date is visible again next to the time on edited messages («date in messages» setting); the edit mark now comes first, then the date and time
- View-deleted: the list no longer jumps to the newest message when something gets deleted while you scroll up; saving deleted media to the gallery works again (re-downloads the file when needed, shows a clear message when recovery is impossible); the interface no longer briefly freezes when messages are deleted
- Custom fonts now apply to posts with the new markup (text, tables, button labels), the post editor fields and Instant View articles, including code blocks and bold-italic headings
- Custom font is now used across the whole settings interface — rows, hints, dialogs and input fields — instead of a mix with the system font
- The notification icon picker now highlights your choice with a neat animated ring, like the app icon section, instead of the clipped enlarge
- Settings backup now keeps custom API (id/hash) and notification color (previously lost)
- Crash fix when loading incomplete emoji packs
- Fixed HDR photo (Ultra HDR) darkening in the media viewer on HDR screens; "Photo HDR" toggle (Turbo → Media) is on by default
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
