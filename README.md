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

The signed APK is published under **Releases** (`v1.0.0`, certificate `CN=Fly`). Verify it with
`SHA256SUMS.txt`. Sideload it after allowing installs from unknown sources.

## Features

### Wallpaper studio
- Eight generative recipes (Aurora, Ember, Mint, Midnight, Rosewater, Solar, Glacier, Void).
- Gradients are rendered pixel by pixel in C++ (`generateGradient`) with an optional separable
  box blur; the "Remix hue" button rotates the palette in real HSV space.
- Applying uses `WallpaperManager.setBitmap(...)` with `FLAG_SYSTEM`, `FLAG_LOCK` or both.
- Favourites and apply history are stored in SQLite.

### Theme studio
- Three base surfaces (Light, Dark, AMOLED) and six accent colours.
- Accents are real theme overlays applied with `Theme.applyStyle()` before content inflation, so
  every Material widget picks them up.
- System dynamic colour is available on Android 12+.
- Named theme combinations are saved to and read back from SQLite.

### Icon packs
- Packs are discovered through the standard theme intent actions
  (`org.adw.launcher.THEMES`, `com.gau.go.launcherex.theme`, `com.novalauncher.THEME`, …).
- Icons are resolved from each pack's `appfilter.xml`, read either as a compiled XML resource or
  from the pack's assets, using the pack's own `Resources`.
- Tapping an app pins a home-screen shortcut using the pack's artwork (`ShortcutManager` on
  Android 8+, legacy `INSTALL_SHORTCUT` broadcast below that).

### Widgets
- Lists every provider from `AppWidgetManager.installedProviders()` with its preview image,
  minimum size and resize mode.
- "Pin" issues a real `requestPinAppWidget` request when the launcher supports it.

### Animation studio
- Four motion presets (Spring, Orbit, Wave, Flip) driven by real `ObjectAnimator`/`AnimatorSet`.
- `CurveView` (Java, custom `View`) plots the exact `TimeInterpolator` curve with a live marker.
- Speed slider scales the duration (0.25×–3×) and is persisted.

### Dashboard
- Device model, Android version/API level, resolution, density bucket, default launcher,
  launchable app count, icon pack count and widget provider count.
- Recent wallpaper applies from SQLite and the native engine version string.

## Native engine

`app/src/main/cpp/`

- `native-lib.cpp` — JNI entry points:
  - `generateGradient` (four gradient styles, deterministic grain)
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
└── util/                      NativeFly, WallpaperStudio, IconPackLoader,
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
- `com.android.launcher.permission.INSTALL_SHORTCUT` — legacy shortcut path only.
- `<queries>` declarations cover launcher apps, home apps and the known icon-pack theme intents;
  the app does not request `QUERY_ALL_PACKAGES`.

## Notes

- Wallpapers are generated on device. Nothing is downloaded, and there is no sample or mock data.
- Icon-pack and widget pinning depend on the launcher; unsupported launchers get an explicit
  message instead of a fake success.
