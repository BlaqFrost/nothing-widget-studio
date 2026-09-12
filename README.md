# Nothing OS (1) × Typebase Widget Studio

A custom Android widget studio tailored for the **Nothing Phone (1)** and **Nothing OS**, featuring a blend of **Nothing OS aesthetic** (transparent frosted glass, monochrome, stark black typography, and Nothing Red accents) and **Typebase** style.

## Features

- **Spotify Liked Song Widgets**:
  - **2×2 Full Album Cover Widget**: Displays full-bleed high-res album cover of your most recently liked Spotify song, with floating frosted glass status pills (`[LIKED]`) and a direct play pill (`▶ PLAY SPOTIFY`).
  - **4×2 Typebase Media Widget**: Oversized typography with track title, artist, album, thumbnail, and live sync indicator.
- **Hardware & Utility Widgets**:
  - **Typebase Digital Clock**: Bold typographic hour/minute with Nothing Red colon separator and date metadata.
  - **Nothing Phone (1) Battery Gauge**: Battery percentage and charging indicator.
- **Spotify Web API & PKCE Auth**:
  - Direct OAuth 2.0 PKCE flow (no client secret stored on device).
  - Background updates via Android `WorkManager` (15-min periodic sync + manual on-widget sync trigger).
- **Interactive Web Simulator**:
  - Built-in previewer in `web-studio/index.html` to test layouts and shuffle demo tracks on a Phone (1) frame.

## Project Structure

```
nothing-widget-studio/
├── android-app/          # Native Android Project (Kotlin, Material, WorkManager, OkHttp)
│   ├── app/
│   │   ├── src/main/java/com/nothing/widgets/
│   │   │   ├── MainActivity.kt
│   │   │   ├── NothingWidgetApplication.kt
│   │   │   ├── TypebaseClockWidgetProvider.kt
│   │   │   ├── BatteryRingWidgetProvider.kt
│   │   │   └── spotify/
│   │   │       ├── SpotifyAuthManager.kt
│   │   │       ├── SpotifyRepository.kt
│   │   │       ├── SpotifyLikedWidgetProvider.kt
│   │   │       ├── SpotifyLiked2x2WidgetProvider.kt
│   │   │       └── SpotifySyncWorker.kt
│   │   └── src/main/res/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── web-studio/
    └── index.html       # Interactive Live Preview Studio
```
