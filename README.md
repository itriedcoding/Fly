# Fly — Android Customization Studio

Fly is a real, working Android customization app: generative wallpapers rendered by a native
C/C++ engine, runtime themes with accent colours, icon-pack discovery and themed shortcuts,
widget pinning, an animation studio, and SQLite-backed favourites, history and saved themes.

No mock data, no placeholders: every screen reads from Android's own APIs.

| Layer | Technology |
| --- | --- |
| App logic | Kotlin (AndroidX, Material 3, coroutines) |
| Compatibility layer | Java (`HomeShortcutCompat`, `CurveView`) |
| UI | XML layouts with View Binding |
| Storage | SQLite via `SQLiteOpenHelper` |
| Native engine | C++17 + C11 built with CMake/NDK into `libflycore.so` |

## Download

The signed APK is published under **Releases** (latest `v1.4.0`, certificate `CN=Fly`). Verify it
with `SHA256SUMS.txt`. Sideload it after allowing installs from unknown sources. The app's own
Settings → App updates checks the release page for newer builds.

## Features

### Wallpaper studio
- **Thirty static generative recipes** (Aurora, Ember, Mint, Midnight, Rosewater, Solar, Glacier,
  Void, Sakura, Copper, Lagoon, Nebula, Moss, Horizon, Sorbet, Onyx, Orchid bloom, Sage,
  Tangerine, Cobalt, Bubblegum, Forest, Wine, Arctic, Sepia, Electric, Peach, Indigo night,
  Pistachio, Storm) plus **six animated ones** below.
- Gradients are rendered pixel by pixel in C++ (`generateGradient`) across six static styles —
  linear, radial, diagonal, banded, angular sweep and dual glow — with an optional separable box
  blur; the "Remix hue" button rotates the palette in real HSV space.
- **Animated wallpapers** (Aurora drift, Pulse rings, Orbit, Plasma tide, Comet sweep, Caustics)
  are real live wallpapers: `FlyWallpaperService` renders every frame in C++ with
  `generateFrame` and blits it to the wallpaper surface. Rendering stops when the wallpaper is
  hidden, honours the reduce-motion and frame-rate settings, and scrolls with a parallax factor
  from `onOffsetsChanged`. The grid shows a live native preview of each animation.
- **Shuffle** picks a random recipe with a random hue shift and applies it in one tap.
- **Favourites filter** narrows the grid to starred wallpapers only.
- **Long-press** exports the wallpaper to `Pictures/Fly` through MediaStore (Android 10+).
- Applying uses `WallpaperManager.setBitmap(...)` with `FLAG_SYSTEM`, `FLAG_LOCK` or both.
- Favourites and apply history are stored in SQLite.

### Theme studio
- Three base surfaces (Light, Dark, AMOLED) and **sixteen accent colours** (Violet, Azure,
  Emerald, Amber, Rose, Citrine, Teal, Coral, Orchid, Slate, Sand, Crimson, Indigo, Mint, Plum,
  Copper).
- **Forty-four stock themes** applied with a single tap, from Obsidian, Midnight ink, Aurora and
  Neon Tokyo through Graphite, Pine, Tide and Dusk to Solar flare, Cobalt, Reactor, Jade noir,
  Ruby noir, Indigo dusk, Periwinkle, Terracotta, Seafoam, Cherry and Lilac.
- **Match wallpaper**: samples the live wallpaper, runs native dominant-colour extraction and
  picks the closest accent.
- Accents are real theme overlays applied with `Theme.applyStyle()` before content inflation, so
  every Material widget picks them up.
- System dynamic colour is available on Android 12+.
- Named theme combinations are saved to and read back from SQLite.

### Icon packs
- **Twelve built-in packs** (Aurora, Midnight, Neon, Pastel, Mono, Sunset, Forest, Ocean, Candy,
  Slate, Copper, Cyber) across six shapes — circle, squircle, rounded square, hexagon, pentagon
  and diamond — and twelve palettes. Every icon is drawn on device with Canvas from the app's own
  label, a deterministic gradient angle derived from its component and the native luminance
  function to pick a readable letter colour. Selecting a pack themes every installed app in the
  grid and every shortcut pinned from Fly, and the choice survives restarts.
- The built-in catalogue always renders, so the Icons tab is never empty even when the device has
  no third-party pack installed.
- Third-party packs are discovered through the standard theme intent actions
  (`org.adw.launcher.THEMES`, `com.gau.go.launcherex.theme`, `com.novalauncher.THEME`, …).
- Icons are resolved from each pack's `appfilter.xml`, read either as a compiled XML resource or
  from the pack's assets, using the pack's own `Resources`.
- Live **app search** filters the themed grid by name.
- **Custom icons**: long-press any app, pick an image from the system picker and Fly copies it into
  private storage, downscales it and shows it in the grid and on pinned shortcuts. The mapping is
  stored in SQLite; "Remove custom icon" deletes both the row and the file.
- Tapping an app pins a home-screen shortcut using the custom icon, the pack's artwork or the
  system icon (`ShortcutManager` on Android 8+, legacy `INSTALL_SHORTCUT` broadcast below that).

### Widgets
- Lists every provider from `AppWidgetManager.installedProviders()` with its preview image,
  minimum size, resize mode and how many instances are already live on the home screen
  (`getAppWidgetIds`).
- "Pin" issues a real `requestPinAppWidget` request with a **success `PendingIntent`**; the
  system-filled `EXTRA_APPWIDGET_ID` arrives at `WidgetPinReceiver`, which records the confirmed
  pin in SQLite and shows a toast. A declined or unsupported launcher gets an explicit message,
  never a fake success.

### Animation studio
- **Twelve presets**: Spring, Orbit, Wave, Flip, Slide, Zoom, Drop, Shake, Pendulum, Jelly,
  Spiral and Stagger (three dots entering in sequence), all driven by real
  `ObjectAnimator`/`AnimatorSet`.
- `CurveView` (Java, custom `View`) plots the exact `TimeInterpolator` curve with a live marker.
- Speed slider scales the duration (0.25×–3×) and is persisted.

### Updates
- Settings checks the public GitHub Releases API (`UpdateChecker`), compares the tag against
  `BuildConfig.VERSION_NAME` with a numeric version comparison, and offers **Download update**
  (the APK asset) or **Open releases page** when GitHub cannot be reached.
- No account or token is needed; the check uses the public unauthenticated endpoint.

### Settings
- **Motion & feedback**: reduce motion freezes animated previews and holds a single live
  wallpaper frame; haptic feedback for long-press; a compact five-column app grid.
- **Wallpaper rendering**: Small / Balanced / Full resolution, used by both static renders and
  the live wallpaper frame buffer, plus a live wallpaper frame rate (24 / 30 / 45 fps).
- **Startup**: check GitHub for updates when Fly opens, or turn it off for fully offline use.
- **Storage**: real byte counts for the database and icon folder, remove all custom icon files,
  clear the preview cache, and reset every row and preference behind a confirmation dialog.
- **Backup**: export a JSON snapshot to `Downloads/Fly` and import one back through the system
  document picker; imported icons are re-decoded and written into private storage.

### Dashboard
- **Current wallpaper card**: renders the live wallpaper and shows the five dominant colours
  sampled natively from it, plus whether Fly's own live wallpaper is currently the active one.
- Device model, Android version/API level, resolution, density bucket, default launcher,
  launchable app count, icon pack count and widget provider count.
- Recent wallpaper applies from SQLite and the native engine version string.

### Data & backup
- **Export backup** writes a JSON snapshot of appearance and rendering settings, saved themes,
  wallpaper favourites, pinned widgets and custom icons (PNG bytes embedded as base64) to
  `Downloads/Fly` via MediaStore.
- **Import backup** parses that file, skips ids that no longer exist in the catalogues, restores
  the custom icons as real files and reports exactly what was restored.
- Clearing history, favourites, custom icons and the full reset all operate on real SQLite rows
  and real files.

## Native engine

`app/src/main/cpp/`

- `native-lib.cpp` — JNI entry points:
  - `generateGradient` (six gradient styles, deterministic grain)
  - `generateFrame` (six time-animated styles: flowing curtains, pulsing rings, orbit, plasma,
    comet sweep and caustics, with a static grain pattern so frames do not flicker)
  - `boxBlur` (separable box blur blended with original detail)
  - `dominantColors` (bucketed histogram, up to eight colours)
  - `contrastRatio`, `luminance`, `paletteHash`, `version`
- `flycore.c` / `flycore.h` — pure C11 colour maths: WCAG relative luminance and contrast,
  linear mixing, clamping, FNV-1a hashing and deterministic dither offsets.
- `CMakeLists.txt` — builds one shared library for `arm64-v8a`, `armeabi-v7a` and `x86_64`.

The C core can be tested on the host without an emulator:

```bash
gcc -std=c11 -Wall -Wextra -O2 -o harness harness.c flycore.c -lm && ./harness
```

## Building

Requirements: JDK 17, Android SDK with platform 34, build-tools 34.0.0, NDK 26.3.11579264 and
CMake 3.22.1.

```bash
export ANDROID_HOME=/path/to/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:assembleDebug      # debug APK
./gradlew :app:testDebugUnitTest  # JVM unit tests
./gradlew :app:assembleRelease    # release APK
```

Release signing reads `keystore/fly-release.jks` (or `FLY_KEYSTORE`) with
`FLY_STORE_PASSWORD`, `FLY_KEY_ALIAS` and `FLY_KEY_PASSWORD`. If the keystore is missing the
release build falls back to the debug signing config so the project still builds.

Generate a keystore once and keep it safe — an existing signature must be reused for updates:

```bash
keytool -genkeypair -v -keystore keystore/fly-release.jks \
  -alias fly -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass flycustomizer -keypass flycustomizer \
  -dname "CN=Fly, OU=Customization, O=Fly, L=Internet, S=Web, C=US"
```

## Publishing a release

`.github/workflows/release.yml` publishes the APK committed under `releases/` as a GitHub
Release named `v<versionName>`, using the repository's own `GITHUB_TOKEN` (no extra secrets).

To ship a new version:

1. Bump `versionCode` / `versionName` in `app/build.gradle.kts`.
2. Build with the same keystore and copy the APK to `releases/Fly-<version>.apk`.
3. Commit and push; the workflow creates (or updates) the release and uploads the APK plus
   `SHA256SUMS.txt`.

Keep `keystore/fly-release.jks` out of the repository (it is gitignored) and reuse it for every
release, otherwise Android will refuse to install an update over the previous version.

## Project layout

```
app/src/main/java/com/fly/customizer/
├── FlyApp.kt                  Application: prefs + repository
├── compat/HomeShortcutCompat.java
├── core/                      Prefs, ThemeCatalog, ThemeController
├── data/                      FlyDatabase (SQLite), FlyRepository
├── ui/
│   ├── MainActivity.kt
│   ├── home/                  Dashboard
│   ├── wallpapers/            Studio, adapter
│   ├── themes/                Studio, accent + saved theme adapters
│   ├── icons/                 Pack + app grid
│   ├── widgets/               Provider list
│   ├── animations/            AnimationsFragment + CurveView.java
│   └── settings/
├── wallpaper/FlyWallpaperService.kt  Live wallpaper engine (real frames)
├── widgets/WidgetPinReceiver.kt  App-widget pin success callback
└── util/                      NativeFly, WallpaperStudio, AnimatedWallpaperCatalog,
                               LiveWallpaperController, BackupExporter, BackupImporter,
                               StorageManager, CustomIconStore, FlyIconPacks, IconPackLoader,
                               AppFilterParser, ColorMath, DeviceInfo
app/src/main/cpp/              C++ / C native engine
app/src/main/res/              XML layouts, themes, strings, launcher icons
app/src/test/java/             JVM unit tests
releases/                      Verified APKs published by the release workflow
scripts/make_launcher_icons.py Reproducible PNG launcher icon generator
.github/workflows/release.yml  Release automation
```

## Permissions

- `SET_WALLPAPER`, `SET_WALLPAPER_HINTS` — applying wallpapers.
- `BIND_WALLPAPER` — held by the system when it binds `FlyWallpaperService`.
- `com.android.launcher.permission.INSTALL_SHORTCUT` — legacy shortcut path only.
- `<queries>` declarations cover launcher apps, home apps and the known icon-pack theme intents;
  the app does not request `QUERY_ALL_PACKAGES`.

## Notes

- Wallpapers are generated on device. Nothing is downloaded, and there is no sample or mock data.
- Icon-pack and widget pinning depend on the launcher; unsupported launchers get an explicit
  message instead of a fake success.
