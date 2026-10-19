# 0001: A native Compose for TV launcher, not a fork

Date: 2026-10-19. Status: accepted.

## Context

The parents' TV shows a sponsored ad before any app can be reached. The goal is
a home screen Sandro builds and extends himself: an app grid first, later a
personalised home, an intro animation, transitions, and speed above all.

Three starting points were considered:

| | Source | Stack | Activity |
|---|---|---|---|
| Projectivy Launcher | Closed (proprietary freemium) | n/a | n/a |
| FLauncher (osrosal fork) | Open, GPL-3.0 | Flutter / Dart | Last commit and release 2025-07-27 |
| New app | Ours | Kotlin, Jetpack Compose, Compose for TV | n/a |

Projectivy cannot be built on. FLauncher can, but a launcher's core is small
(a `HOME` intent filter, a list of `LEANBACK_LAUNCHER` apps, a launch intent),
so a fork mostly hands over its decisions about categories, settings and look,
which are the parts this project wants to replace. Flutter also starts its own
engine on top of Android's; whether that costs startup time on a TV chip was
not measured.

## Decision

Write a new app in Kotlin with Jetpack Compose and Compose for TV
(`androidx.tv:tv-material` 1.1.0, stable since May 2026), on the latest stable
toolchain. FLauncher stays a reference for TV-specific details, not a base.

## Consequences

- Every line is ours to explain and change, and the licence is ours to choose.
  A fork would have stayed GPL-3.0.
- The project owns what FLauncher already solved: app list refresh, banners
  for apps without one, settings storage.
- Startup is measured, not assumed: cold start on the TV becomes a budget in
  `agent_docs/HEALTH_CHECKLIST.md`, with a Baseline Profile and Macrobenchmark.
- GlotzeOS is an app. The boot animation and power-off belong to the system;
  replacing the boot animation needs a protected path written over ADB or root,
  and may not be possible at all on a given TV. It stays in Later until
  researched for the actual model.

## Sources

- osrosal/flauncher, releases and commits: https://github.com/osrosal/flauncher
- Compose for TV releases: https://developer.android.com/jetpack/androidx/releases/tv
- AGP 9.4 compatibility and built-in Kotlin:
  https://developer.android.com/build/releases/gradle-plugin and
  https://developer.android.com/build/migrate-to-built-in-kotlin
