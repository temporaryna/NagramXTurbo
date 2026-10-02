<div align="center">
  <img src="logo.png" width="96" alt="Nagram X Turbo logo">
</div>

# Nagram X Turbo

A fork of the [Nagram X](https://github.com/risin42/NagramX) **Telegram client for Android** with bug fixes and focused UX tweaks — no flashy widgets, just the small things that make everyday use a bit nicer. Separate package (`nu.gpu.nagramxturbo`), keystore and remote-config: an independent app that installs alongside Nagram X or the official Telegram client (chats are shared per account).

Based on Nagram X [12.10.0-a6c7d0a](https://github.com/risin42/NagramX/commit/a6c7d0aec95f829a63aaa7bc591b8e809af84636) — the final Nagram X release (the project is now archived) — with Telegram updates applied directly from the official client source; currently synced to Telegram [12.10.6-f2908b1](https://github.com/DrKLO/Telegram/commit/f2908b14133bbffbf7ab04f641ecb5bfaf533242).

Turbo updates on its own — in-app updates work independently of Nagram X.

## Features (vs Nagram X)

- **Share sheet**: folder tabs with an animated swipe between folders, remembers the last opened folder, forwarding toggles (hide sender / hide caption / silent / comment position before-after), "Send later" and "Send when online", forward from protected chats as copies (ask / always / never; missing media downloads in the background with a progress notification and the copies are sent when ready; the ask covers the media viewer, profile shared-media tabs and the audio player too), edit the message text before forwarding (changed text is sent as a copy; the pencil is available in the share sheet, beside the comment field in the chat picker and in the preview bar menu), text formatting in the comment field (long-press to select and style). The media viewer's forward menu, the message menu, the multi-select menu, the profile media bar and the audio player all offer a Fast forward item for this share sheet; the selection bar's right button is configurable (Forward / Fast forward / no-quote) via long-press like the left one.
- **Turbo settings section**: all Turbo settings in one place — app icon picker, input bar (iOS), action button style, media viewer, forwarding, deleted messages and fonts, next to the Nagram X settings groups.
- **Deleted messages**: per-category save settings (text and media for private chats, channels and groups) as a checkbox list directly in the settings — no more hidden popups; all categories are on by default.
- **About screen**: the project's own links — the Turbo channel and GitHub — plus the current build version; also opens from the Settings version menu.
- **App icon**: 15 exclusive themes in three picker sections (Standard / Turbo / Hidden easter egg), the Turbo theme as the new default, classic themes with a "modern" look (full background, bigger plane), monochrome icons sized for the system stroke, notification mark follows the app icon (or Telegram / NekoX), test notification by long-pressing the preview; switching applies instantly on tap with an optional restart, and the mark matches the icon's visual style (classic icons use the plain Telegram plane).
- **iOS-style input bar** (Turbo → Input Bar, off by default): glass capsule field with round button bubbles — three independent toggles (button placement, iOS appearance, compact mode).
- **Input bar text size** (Turbo → Input Bar): turn "Same as chat" off and pick a separate 12–30 text size for the message input field; changes apply to the open chat instantly, with a live "Hello" preview in the input bar mock above the setting.
- **Action button style** (Turbo → Input Bar): Accent / Neutral / White for the send, voice and apply buttons everywhere they show (input bar in glass form, share sheet / photo picker / attach menu / rich editor as a solid circle); replaces the white-send toggle. The neutral and white styles come with a "Button outline" toggle — a thin outline colored like the button icon (theme palette), in both themes and both bar looks; the accent style has no outline.
- **Message date in bubbles** (Turbo → Media, off by default): the sending date next to the time in every message — no need to open the message details to see when it was sent; date format selector (9 patterns with a live preview in the settings).
- **Custom fonts**: pick by category (Regular / Bold / Italic / Mono), import your own `.ttf`/`.otf` from chat, text size in every input field.
- **Numeric-id profile links**: the `tg://openmessage?user_id=`, `tg://user?id=` and `t.me/@id` links our profile sheet copies now open the chat even for users not in your local cache (server-side resolve); broken links show a clear message.
- **Media viewer**: swipe GIFs with photos and videos (toggle, off by default), instant swipe in large chats, optional scroll-to-seen-photo on close, seamless video handoff (opens at preview position, resumes on close/scroll-back; off by default), auto-rotate button (off / fit content / always), Photo HDR toggle (on by default), editor pencil auto-downloads missing media (progress dialog, asks for files over 100 MB) and is available for videos too.
- **Deleted messages**: save with filtering by chat category.
- **Base build**: a ToS-friendlier APK without the disputable features (deleted/edited saving, ghost mode, last seen history, regex filters, protected-forward, local premium), published side by side with the regular one, with its own in-app update channel.
- **App updates**: in-app check and download of new builds (manual + automatic, once a day).
- **Bookmarks manager** in the chats menu (instead of a settings link).
- **Folders**: edit folder membership from the chat list — add or remove chats to/from any folder in one go.
- **Audio**: background music pauses or ducks when playing videos, voice messages, and round videos (resumes after).

Full list & history — [CHANGELOG.md](CHANGELOG.md).

## Download

- [Latest release](https://github.com/temporaryna/NagramXTurbo/releases/latest) — APKs for `arm64-v8a` (64-bit) and `armeabi-v7a` (32-bit); in-app update picks the right one automatically
- [Telegram channel](https://t.me/nagramxturbo) — dev builds

## Discussion & support

- [4pda topic](https://4pda.to/forum/index.php?showtopic=1123710)
- [Report a bug](https://github.com/temporaryna/NagramXTurbo/issues)

## Verify APK

- Package name: `nu.gpu.nagramxturbo`
- Signing certificate SHA-256: `97:A8:4E:DE:76:3B:91:F4:F3:C8:6D:AD:1F:27:BD:1A:20:61:84:81:4A:DA:B1:B5:B7:10:13:45:E8:E6:AE:8D`

## Build

You will require Android Studio, Android NDK 27.2.12479018 and Android SDK 36/37.

1. Clone with submodules (third_party — ffmpeg/dav1d/libvpx — are submodules):
   ```bash
   git clone --recursive https://github.com/temporaryna/NagramXTurbo.git
   ```
   Already cloned without submodules:
   ```bash
   git submodule update --init --recursive
   ```
2. Put `TELEGRAM_APP_ID` / `TELEGRAM_APP_HASH` and keystore creds in `local.properties` (see the [Nagram X](https://github.com/risin42/NagramX) build guide).
3. `./gradlew :TMessagesProj:assembleStaging` (or `assembleRelease`).

## Acknowledgments

Based on [Nagram X](https://github.com/risin42/NagramX).
