# Nagram X Turbo

A fork of the [Nagram X](https://github.com/risin42/NagramX) **Telegram client for Android** with bug fixes and focused UX tweaks — no flashy widgets, just the small things that make everyday use a bit nicer. Separate package (`nu.gpu.nagramxturbo`), keystore and remote-config: an independent app that installs alongside Nagram X or the official Telegram client (chats are shared per account).

Based on Nagram X [12.9.0-80c6040](https://github.com/risin42/NagramX/commit/80c604047e80fe424bbe11087c357b688e8fbc63).

Turbo updates on its own — in-app updates work independently of Nagram X. I plan to keep tracking upstream and shipping Turbo builds for new Nagram X releases.

## Features (vs Nagram X)

- **Share sheet**: folder tabs, remembers the last opened folder, forwarding toggles (hide sender / hide caption / silent), "Send later" and "Send when online".
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

## Acknowledgments

Based on [Nagram X](https://github.com/risin42/NagramX).
