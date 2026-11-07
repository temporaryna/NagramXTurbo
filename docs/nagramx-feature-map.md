# Feature-map форка — карта зон наследия для upstream-мерджей

> Рабочий input для feature-impact матрицы: на входе мерджа дельта-ченджлог
> × ЭТОТ список → per-зона вердикт в спеке ДО apply. Обновлять при новых
> фичах и после каждого мерджа (выпавшие зоны — сюда же).

## После 13.2.1 (07.11) — новые дома fork-кода

- ChatMessageCell split: `ChatMessageLocation`, `ChatMessageTransitionParams`
  (наши lastDrawingEdited `|| ayuDeleted`, lastBackgroundRight — в нём),
  `ChatMessagePoll` (переводы через `messageOwner.translatedPoll`),
  `ChatMessageLinkPreview` (isWebP/didPressImage, colored-background flag),
  `ChatMessageAccessibilityNodeProvider` (ReactionFilter/TURBO_BASE, FORWARD child).
- blur3/source → `utils/glass/source` (GlassSource*); старый PiP
  (`messenger/pip/*`, PipVideoOverlay, IPipActivity) удалён → `utils/pip`.
- TURBO_BASE: BuildVars += SUPPORTS_FPNV (Firebase pnv, BoM 34.19.0).
- Theme.getColor → ThemeBind.textColor по всему UI; наши
  applyCustomRegularFont/TypefaceHelper вставки рядом с ThemeBind.
- text-format editTextItem-меню умерло (хоткеи + format-кнопка в CAEV);
  наши text_code/mention/translate пункты не восстановлены.


## Слой A — NagramX/Neko-наследие (чужие фичи, наша зона защиты)

**Конфиг-носители** (ключи = маркеры для grep против дельты):
- `tw/nekomimi/nekogram/NekoConfig.kt` — нео-настройки
- `xyz/nextalone/nagram/NaConfig.kt` — nax-настройки
- `tw/nekomimi/nekogram/config/**` — конфиг-фреймворк
- Neko-экраны: `NekoSettingsActivity` + `tw/nekomimi/nekogram/settings/**`, `ui/components/**`

**Аю-слой** (`com.radolyn.ayugram.**` + `xyz.nextalone.nagram.helper.*`):
- save-deleted, ghost-режимы и пр. — ВСЕ call-sites гейтятся `!BuildVars.TURBO_BASE`
  (R8 `-checkdiscard`); тронул аю-зону → BASE-сборка обязательна.

**Инлайн-патчи Neko/nax в upstream-файлах** (главные конфликт-магниты):
- voip custom-audio-bitrate (13.0.2: **поглощено upstream'ом**): JNI-хук
  `makeGroupNativeInstance(..., jshort customBitrate)` в `org_telegram_messenger_
  voip_Instance.cpp` (наш, жив) → штатный `descriptor.outgoingAudioBitrateKbit` →
  субм `jni/tgcalls` (ccbb57965, GroupInstanceCustomImpl/ReferenceImpl opus-битрейты).
  Наш static-хак (voice_engine-мост + field_trials) снят в мердже 13.0.2.
- tgcalls с 13.0.2 — **субмодуль** `jni/tgcalls` (vendored-дерево jni/voip/tgcalls
  upstream удалил, 267 файлов); наш webrtc-дерево jni/voip/webrtc — жив (предкурсор).
- `DatacenterActivity`: ping.neko + dcId → ProxySettings.builder
- `ChatActivityEnterView`: `pendingCameraFront` + `setVideoRecordingCameraFront`
  (nax Ask-режим) + настройка `cameraInVideoMessages` (Фронт/Тыл/Спрашивать,
  NekoChatSettings) — терминальное звено `InstantCameraViewBase.setUseFrontCamera`
  (восстановлено в 12.10.6: Base default + обе реализации + `setInitialFacing`
  по cameraMode в InstantCameraView2, 351dd20294; пережило 13.0.2 без конфликтов)
- `Emoji.java`: Neko-ветка useSystemEmoji + EmojiHelper-шрифт
- `VideoPlayer`: `getPlayerExtensionRendererMode()` (NaConfig)
- `ApplicationLoader`: TFoss Local Push Service
- `LocaleController`: deprecated getString(String) name-резолв + getContextLocale/fixContextLocale
- `ConnectionsManager.{java,cpp}`: неко-imports, DnsFactory/ProxyUtil-хуки
- `StoriesController`: NaConfig disableStories-гейты
- CAEV/ChatActivity: observers-хвосты Ayu, allow-list анимаций
- CodeHighlighting (13.0.2): класс upstream'а переехал `org.telegram.messenger` →
  `org.telegram.utils.code.highlight`; наш neko-слой (MessageDetailsActivity,
  NekoMessageCell, EntitiesHelper) импортирует новый путь.
- ChatActivity OPTION_-пространство: наши 110–117/150–152; upstream 13.x добавил
  `OPTION_SEND_MONEY` — сдвинут на 118 (правило: их смещаем, наши не трогаем).

## Слой B — наши Turbo-фичи

- TurboSettingsActivity + ConfigCell-семья (RLottieDrawable-адаптации ×2)
- message-date-selector: `TimeStringHelper` + CMC time-цепочка
- шрифты-унификация: TypefaceHelper-вставки после создания view (~63 файла)
- iOS-input / CAEV-режимы, action-button-style (`Components/ActionButtonStyle`)
- blur3 (`ui/Components/blur3/**` + glass-провайдеры)
- forward-edit (`ProtectedForward`), edit-before-send
- notification icons (res/raw Turbo-набор + tools/icons)
- remote-config: `#emojiv2` / `#pagepreview` / `#update*` (манифесты msgID ↔ канал)
- Neko/Turbo строки: `strings_*.xml` (values + values-ru) + buildSrc include-инпуты

## Инфра-зоны мерджа (не фичи, но конфликтуют)

- buildSrc: include-инпуты генераторов (`TelegramBuildAppPlugin.kt`, `TelegramBuildPlugin.kt`)
- media3: settings-лист (CoD-фикс) + `TMessagesProj_Modules/media` gitlink + локальный
  kotlin-drop-патч (`docs/patches/0001-...`) — CI применяет после checkout
- tlottie gitlink (prebuild `.a` ×4), prebuild-скрипты (`jni/prebuild/*`)
- gradle: `gradle.properties` (APP_PACKAGE наш!), signing-зона build.gradle,
  noCompress `'pack'`, proguard keep-зона
