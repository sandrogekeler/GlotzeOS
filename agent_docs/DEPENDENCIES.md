# GlotzeOS: dependency policy and inventory

A decision record, not a lockfile mirror. Every direct dependency gets one line
saying why it is here. Removed or rejected candidates go under "Removed" with
the reason, so the same discussion does not happen twice.

## Policy

- Prefer the platform and AndroidX, then what is already in the tree.
- A new dependency needs a reason a reviewer can check: what it does that the
  tree cannot reasonably do itself, and what it gets access to.
- Startup cost counts. A library that initialises itself on launch, scans
  classes or uses reflection needs a measured reason.
- Dependabot opens update PRs weekly (`.github/dependabot.yml`).

## Current inventory

### Gradle plugins (`gradle/libs.versions.toml`)

| Plugin | Why |
|---|---|
| `com.android.application` (AGP) | Builds the app. Compiles Kotlin itself since AGP 9 |
| `org.jetbrains.kotlin.plugin.compose` | The Compose compiler; its version is the Kotlin version |
| `com.diffplug.spotless` | Runs ktlint as part of `./gradlew check` |
| `com.android.test` (AGP) | The `:benchmark` module that measures `:app` from outside |
| `androidx.baselineprofile` | Generates the Baseline Profile into `:app` and adds the benchmark build types |

### Libraries

| Library | Why |
|---|---|
| `androidx.compose:compose-bom` | Keeps the Compose libraries on one tested set of versions |
| `androidx.compose.foundation:foundation` | Lazy grid, layout, `Image` |
| `androidx.tv:tv-material` | Google's TV components: focus-aware `Card`, `Surface`, theme |
| `androidx.activity:activity-compose` | `setContent` and `BackHandler` for the one activity |
| `androidx.core:core-ktx` | `Drawable.toBitmap()` for app banners, `ContextCompat.registerReceiver` |
| `androidx.profileinstaller:profileinstaller` | Applies the Baseline Profile on a sideloaded install, where no app store does. Runs once after install, off the startup path |
| `androidx.compose.ui:ui-tooling(-preview)` | Compose Preview in Android Studio; the tooling half is debug-only |
| `junit:junit` (test) | Unit tests on the JVM |
| `org.robolectric:robolectric` (test) | Android framework on the JVM, so focus and activity tests run in `check` without a device |
| `androidx.compose.ui:ui-test-junit4` (test), `ui-test-manifest` (debug) | Compose UI tests: D-pad key input and focus assertions. The manifest artefact only adds a test host activity to debug builds |
| `androidx.benchmark:benchmark-macro-junit4` (`:benchmark`) | Macrobenchmark and `BaselineProfileRule` |
| `androidx.test.ext:junit` (`:benchmark`) | The JUnit runner for the on-device tests |

## Removed

| Dependency | Removed in | Why |
|---|---|---|
| `androidx.tv:tv-foundation` | Never added | Its lazy lists were superseded by Compose foundation's; nothing else needed yet |
| `org.jetbrains.kotlin.android` | Never added | AGP 9 compiles Kotlin itself (built-in Kotlin) |
| FLauncher (fork) | Never added | See `docs/adr/0001-native-compose-launcher.md` |
