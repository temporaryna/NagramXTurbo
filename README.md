# Nagram X Turbo

A fork of the [Nagram X](https://github.com/risin42/NagramX) **Telegram client for Android** with bug fixes and focused UX tweaks — no flashy widgets, just the small things that make everyday use a bit nicer. Separate package (`nu.gpu.nagramxturbo`), keystore and remote-config: an independent app that installs alongside Nagram X or the official Telegram client (chats are shared per account).

Based on Nagram X [12.9.2-2f0de6a](https://github.com/risin42/NagramX/commit/2f0de6a06520bb44b27e189cf71846e6543d063e).

Turbo updates on its own — in-app updates work independently of Nagram X. I plan to keep tracking upstream and shipping Turbo builds for new Nagram X releases.

## Features (vs Nagram X)

- **Share sheet**: folder tabs with an animated swipe between folders, remembers the last opened folder, forwarding toggles (hide sender / hide caption / silent), "Send later" and "Send when online".
- **iOS-style input bar** (Chat settings, off by default): glass capsule field with round button bubbles — three independent toggles (button placement, iOS appearance, compact mode).
- **Custom fonts**: pick by category (Regular / Bold / Italic / Mono), import your own `.ttf`/`.otf` from chat, text size in every input field.
- **Media viewer**: swipe GIFs with photos and videos (toggle, off by default), instant swipe in large chats, optional scroll-to-seen-photo on close, seamless video handoff (opens at preview position, resumes on close/scroll-back; off by default), force media auto-rotate, Photo HDR toggle (on by default).
- **Deleted messages**: save with filtering by chat category.
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
