# GlotzeOS

A fast, personal Android TV home screen. It replaces the TV's stock launcher
(which shows ads before you reach an app) with one Sandro builds himself: app
grid first, then a personalised home, intro animation and transitions.

It is an app, not an operating system: it runs on the TV's own Android and
cannot change what happens below it (boot animation, power-off). See
`docs/adr/0001-native-compose-launcher.md`.

## Stack

- Kotlin 2.4, built-in Kotlin of AGP 9.4 (no `kotlin-android` plugin)
- Jetpack Compose (BOM) and Compose for TV (`androidx.tv:tv-material`)
- Gradle 9.8 with the Kotlin DSL; versions in `gradle/libs.versions.toml`
- Spotless with ktlint for formatting, Android Lint with warnings as errors
- JUnit 4 for unit tests

## Project structure

| Path | What |
|---|---|
| `app/` | The launcher, the only module so far |
| `app/src/main/kotlin/.../MainActivity.kt` | The single activity, registered as `HOME` and in the TV's app list |
| `app/src/main/kotlin/.../home/` | The home screen: loading apps (`TvApp.kt`) and drawing them (`HomeScreen.kt`) |
| `app/src/test/` | JVM unit tests, no device needed |
| `docs/adr/` | Decisions and why. Read the relevant one before changing what it decided |
| `docs/mockups/` | Visual direction only. The content in them is invented; never build from it |
| `docs/device.md` | Connecting the TV, installing, making GlotzeOS the home screen, and undoing it |

## Architecture rules

- **No network.** The manifest has no `INTERNET` permission. Adding one is an
  ADR, not a line in a pull request.
- **Everything works with a remote.** Every interactive element is focusable,
  something has focus when a screen opens, and Back never leaves the home
  screen. There is no pointer and no touch.
- **Nothing slow on the main thread.** `PackageManager` queries, file reads and
  bitmap decoding run on `Dispatchers.IO`. Startup speed is the product.
- **Logic is pure, Android is thin.** What the screen shows is decided by plain
  functions (`homeOrder`) tested on the JVM; the Android-facing code only
  gathers inputs and draws results.
- **The app never changes the system.** No root, no shell, no `pm` calls from
  the app. Disabling the stock launcher is done by hand over ADB, written down
  with its undo in `docs/device.md`.
- **One activity.** Screens are composables, not activities.
- **No DI framework or reflection-heavy library.** They cost startup time. A
  dependency is recorded in `agent_docs/DEPENDENCIES.md` with its reason.

## Build & dev commands

```sh
./gradlew check            # Spotless, Android Lint, unit tests: the health command
./gradlew spotlessApply    # format
./gradlew assembleDebug    # debug APK in app/build/outputs/apk/debug/
./gradlew installDebug     # install on the connected TV (see docs/device.md)
./gradlew assembleRelease  # R8-shrunk release APK
python3 .claude/suite-check.py   # every health check in .claude/suite.json
```

Needs JDK 17 or newer and the Android SDK (`ANDROID_HOME`, or `sdk.dir` in
`local.properties`). Android Studio sets both up.

Day-to-day iteration: Compose Preview for layout, Live Edit to the emulator or
TV for changes inside a function (Android 11+ device), `installDebug` for the
rest. Use an Android TV emulator for routine work and the real TV to confirm.

## Task tracking

Work is tracked in GitHub Issues. Every issue carries a `type:`, an `area:` and
a priority (`p0`-`p3`) label. Close issues from the pull request with
`Fixes #N`.

## Commits & pull requests

- A merged pull request's title is public release-notes copy: imperative,
  sentence case, no trailing period, no em dashes.
- One concern per pull request.
- Every pull request needs a `type:` and an `area:` label before it can merge
  (the `pr-labelled` status). Which `type:`: see `.github/pull_request_template.md`.

## Files that come from the template

This repo was created from sandrogekeler/AllesSandro and is kept in step with it
by Renovate (`.copier-answers.yml` records the version). Two kinds of file:

- **Locked** (re-applied on every template update): `.claude/suite-*.py`,
  `.github/workflows/{codeql,scorecard,pr-labelled,labels,template-guard}.yml`
  and the optional `claude.yml`, `.github/scripts/release-notes.py`. Do not
  edit them here: change AllesSandro instead. A local edit is kept but shows up
  as a merge conflict on the next template update.
- **Seeded** (created once, owned by this repo): everything else, including
  this file and `.github/workflows/ci.yml`.

A template-update PR with `<<<<<<< before updating` markers: see
`.claude/rules/template-files.md`.

## Do not

- Commit secrets, `.env` files, keystores or `local.properties`.
- Edit generated files, the Gradle wrapper or lockfiles by hand; regenerate them.
- Build a feature from a later roadmap section before the current one is done.

## Deeper rules, loaded on demand

`.claude/rules/*.md`: each one names the paths that load it.
