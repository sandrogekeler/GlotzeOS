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

### Libraries

| Library | Why |
|---|---|
| `androidx.compose:compose-bom` | Keeps the Compose libraries on one tested set of versions |
| `androidx.compose.foundation:foundation` | Lazy grid, layout, `Image` |
| `androidx.tv:tv-material` | Google's TV components: focus-aware `Card`, `Surface`, theme |
| `androidx.activity:activity-compose` | `setContent` and `BackHandler` for the one activity |
| `androidx.core:core-ktx` | `Drawable.toBitmap()` for app banners |
| `androidx.compose.ui:ui-tooling(-preview)` | Compose Preview in Android Studio; the tooling half is debug-only |
| `junit:junit` (test) | Unit tests on the JVM |

## Removed

| Dependency | Removed in | Why |
|---|---|---|
| `androidx.tv:tv-foundation` | Never added | Its lazy lists were superseded by Compose foundation's; nothing else needed yet |
| `org.jetbrains.kotlin.android` | Never added | AGP 9 compiles Kotlin itself (built-in Kotlin) |
| FLauncher (fork) | Never added | See `docs/adr/0001-native-compose-launcher.md` |
