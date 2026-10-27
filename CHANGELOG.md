# Changelog

Differences between Nagram X Turbo and [Nagram X](https://github.com/risin42/NagramX).

## Upstream

Synced with Telegram 13.0.3. User-visible from upstream since 13.0.2:
- Voice messages: optional new recorder with noise suppression (RNNoise) — toggle in Settings
- Bot keyboards: the system Back button no longer gets stuck when a bot keyboard stays open
- Sharing: "Send as file" works again when sharing photos and videos from the gallery
- Email login: clearer error when your email provider is not allowed
- Links opened from Instant View now respect the in-app browser settings
- Profile gifts: fixed item alignment
- Bots: new type labels (Agent, Game, App, AI)

## Features

### Settings
- Download speed boost (Turbo → Network, off by default): lifts download limits to Premium level, so big files download faster without a Premium subscription; stories, audio and secret-chat files download faster too. Experimental: server limits may sometimes slow downloads back down
- One place for all Turbo settings: a dedicated Turbo section in the Nagram X settings — app icon picker, input bar (iOS), action button style, media viewer, forwarding, deleted messages and fonts now live together
- Search fields follow the input field text size setting — chats list, in-chat search, settings, shared media, emoji and stickers; changes apply live
- Deleted messages settings redesign: chat-type categories (private chats, channels, groups) as a two-column Text/Media checkbox list right in the settings instead of hidden popups; all categories are on by default when the feature is first enabled
- About screen with the project's own links: the Turbo channel, our GitHub repository, and the current build version; opens from the version menu too, with a logo header
- Setting descriptions cleanup: removed subtitles that just repeated the setting name, and reworded two unclear ones (seamless video handoff, unlimited pinned chats)

### App identity
- Own launcher identity: the Turbo theme as the new default and 15 icon themes in three picker sections (Standard / Turbo / hidden easter egg); monochrome icons sized for themed launchers
- "Modern classic" toggle for the classic themes
- Notification mark follows the app icon — one badge, no duplicates; long-press the preview for a test notification

### Base build
- ToS-friendly "base" APK variant: built and published alongside the regular one (download links side by side in the release post), without the disputable features — deleted/edited message saving, ghost mode, last seen history, regex filters, forward from protected chats, local premium. Base builds receive their own in-app updates.
- Self-identifying base variant: the version number of base builds carries a "_base" suffix (e.g. "12.10.6-T71_base") in About and in settings; the version string in settings is now two lines with the architecture and build type on the second line.

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
- "Thicken bold text" toggle (Turbo → Fonts): when your bold font has no real bold weight, turn thickening off to render bold text with the font's own weight — for thin font pairs like Light + Regular
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
- Copies without re-uploading: edited-text forwards and protected-chat copies now reference the media on Telegram's servers instead of re-downloading and re-uploading it — the copy leaves instantly even for albums (sent as one group); if the server refuses the reference, the app falls back to the background download queue as before

### Deleted messages
- Filtering by chat category

### Chat
- Message date in bubbles (Turbo → Media, off by default): the sending date next to the time in every message — no need to open the message details to see when it was sent; date format selector (9 patterns with a live preview in the settings)
- Numeric-id profile links open any chat: tg://openmessage?user_id=, tg://user?id= and t.me/@id links (the ones our own profile sheet copies) now resolve unknown ids on the server and open the chat; failures show a clear message instead of a dead screen
- In-chat search by numeric user id: from:<id> (or id:<id>) in the group search field lists all messages from that sender, even one who left the group; add words after the id to search inside their messages; unknown ids are resolved on the server

### Notifications
- Auto-mute on join (Turbo → Notifications, off by default): channels and groups you join — by the Join button, from search or through an invite link — start muted, as if you muted them right after joining; unmuting stays one tap away

### Calls
- Hide encryption emoji (Turbo → Calls, off by default): removes the emoji row in one-on-one calls and the emoji inside the group-call "Encrypted" cell; the cell itself stays

### Input field
- iOS-style input bar (Turbo → Input Bar, off by default): attachment on the left, emoji inside-right, optional compact mode, glass capsule with round bubbles behind the buttons
- Slow mode dial: the wait timer in the send slot is now always a round dial — it starts full and melts counter-clockwise while you wait, with the countdown in the middle; it matches the send button size in both bar looks, and tapping it opens the boost bypass when the channel allows it
- Input bar text size (Turbo → Input Bar): a separate text size for the message input field, independent from the chat text size — turn "Same as chat" off and pick a size on the 12–30 slider; changes apply to the open chat instantly and the input bar preview above the setting shows the result live; the same size is used by every message-typing field outside the chat bar — the media send caption, the attach menu, gallery and file picker captions, the share sheet comment (including edit-forward-text) and the group-call chat
- Action button style (Turbo → Input Bar): Accent / Neutral / White for the send, voice and apply buttons — applies everywhere the action button shows (input bar, share sheet, photo picker, attach menu, rich editor), in glass form when the iOS bar is on and as a solid circle otherwise; replaces the white-send toggle
- Button outline (Turbo → Input Bar): a thin outline on the action buttons, colored exactly like the icon inside (theme palette), for the neutral and white styles in both themes and both bar looks — including the round iOS button bubbles; the row is hidden for the accent style, and the toggle removes the outlines everywhere
- Hide AI button (Turbo → Input Bar): turns off the AI button that appears over the input field on long texts — in the chat bar and the caption fields (media send, attach menu, photo viewer); a saved rich draft keeps its button
- Recorded voice review: the delete button is a separate round glass button matching the bar; the timeline has balanced insets

### App updates
- In-app check and download of new dev builds (manual button + automatic, once a day)
- Beta update channel: pick "Beta" in the update channel switcher (settings version menu or right in the update screen) to get every staging build as an in-app update; the update screen shows the current channel and lets you switch it on the spot

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
- Turbo → Network now holds both speed settings together, download and upload, with clear names; the upload boost previously hid in the experimental Neko settings under a misleading label
- Bold text in messages looks properly bold again on devices where the system font has no separate medium weight (common on Chinese ROMs), matching Nagram X; custom fonts without a bold variant now get a synthetic bold too
- Photo thumbnails no longer stay blank forever after fast scrolling through media: a download cancelled mid-scroll is no longer mistaken for a permanent failure
- In-app update popup: the change list starts with the "What's new" release highlights again instead of a bare list of commits
- Messages no longer go missing from chat history after being offline: posts the app missed while closed are now fetched back from the server instead of leaving permanent gaps
- Beta channel posts in Telegram now say "Beta version" instead of "Dev version" — one name for the same channel everywhere
- Default Turbo icon: clean cloudless sky background — same blue gradient and light, no more puffy clouds
- Launcher icons on every set (classic, modern, all 15 Turbo themes): the plane is optically centered again, and its wing and flame no longer clip at the edge on round launcher icon shapes
- In-app update download no longer freezes at 100% when the connection drops: it resumes from where it stopped and retries automatically, and shows a clear error with a retry button if all attempts fail
- Forward with edited text: GIFs arrive as animated GIFs instead of mp4 files — already-viewed (cached) GIFs now take the same server-reference send path as everything else
- Media viewer: the description text under a photo/video follows the chat text size instead of staying small
- Italian: fork menus and settings show translated text again instead of English (12.10 build pipeline regression)
- Profile colors: the "Change profile color" screen opens again instead of crashing (12.10.3 rebase regression)
- Downloads no longer crash the app for Premium users (13.0.2 regression: parallel download connections mismatch)
- Chinese input on ColorOS 17: the animated text effect no longer breaks character composition — devices detected as ColorOS now skip it (upstream Nagram fix)
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
- Turbo notification icons in the status bar are now the same size as the classic Telegram one (the glyph no longer sits small inside the icon canvas)
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
- View-deleted: photos can be saved to the gallery again — a photo now downloads properly, and if the full size is unavailable the best available one is saved (down to the preview) instead of an error
- Uploading videos and GIFs on mobile data no longer hangs forever when the connection silently drops: a frozen upload restarts itself, and after a few failed tries shows an error you can retry
- Cancelling a video or GIF that is still converting or uploading no longer marks the message with a red error
- Sending a video shows its processing progress — "Processing video… 74%" in the chat and in the upload notification — instead of a size counter that looks stuck
- A video upload no longer hangs forever when the video itself stops converting: after 90 seconds without progress the message shows an honest error you can retry, and the stuck conversion is cancelled
- Saving several deleted media to the gallery in a row no longer slows down with each item: a save now gives its current download time to recover instead of piling on new requests
- Removed the "Download Speed Boost" experimental setting — it had no effect after the 13.0.2 update and only made downloads hit server rate limits sooner
- Fixed chats with old last messages staying hidden from the chat list until you scrolled it all the way to the bottom — the list now finishes loading by itself
- Chat list stays reliable on flaky connections: the app no longer floods the server during the chat list sync (which could make it reset the local cache), and after any reset the list picks loading back up by itself instead of silently stopping
- Video calls on tablets: the person you call no longer sees your video rotated sideways when you hold the tablet in landscape — outgoing video now follows the device orientation, also when screen rotation is locked
