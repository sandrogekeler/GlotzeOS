# 0002: Local storage for the personalised home

Date: 2026-11-23. Status: proposed. The cold-start number below decides it (#30).

## Context

The personalised home (ROADMAP "Next") needs three small things to survive a
restart: favourites, the user's own order, and hidden apps. A fourth one would
help startup: the last app list, so a cold start can draw the grid from disk
before `PackageManager` answers, and correct it a moment later.

All of it is small (well under 10 KB for a few hundred apps), written seldom
(when the user changes something, or when the list changes), and read once per
process, at startup. That read sits on the startup path, which is the
product. The data never leaves the device (no-network rule).

## Options

| | Dependency | First read | Writes | Code to own |
|---|---|---|---|---|
| `SharedPreferences` | Platform | Synchronous on first access; blocks whichever thread touches it first | `apply()` is async but blocks the main thread on `fsync()` when an activity or service starts or stops | None |
| DataStore Preferences 1.3.0 | AndroidX, coroutines-based | Async on `Dispatchers.IO`, as a `Flow` | Async, transactional | None, plus its file format |
| Own file via `android.util.AtomicFile` | Platform | One small file read on `Dispatchers.IO` | Write-to-temp and rename: a crash mid-write leaves the old file | A few dozen lines: a text format and its parser, pure and JVM-testable |

`SharedPreferences` behaviour is from Google's "Prefer storing data with
Jetpack DataStore" post (2020). DataStore's version is from its release
notes. The DataStore library's startup cost on the TV is **not measured**.

## Proposal

The own file, with `AtomicFile`, read on `Dispatchers.IO` in
`GlotzeApplication`:

- No dependency and no library initialisation on the startup path, in line
  with the dependency policy ("startup cost counts").
- The format is plain lines (`favourite <package>`, `hidden <package>`, the
  order as a list), parsed by a pure function. That fits "logic is pure, Android
  is thin" and is tested on the JVM like `homeOrder`.
- The last-list snapshot goes in a second file of the same kind, so a broken
  snapshot can be deleted without losing favourites.

DataStore is the fallback if the own file grows past a few keys or needs
schema migration, which is what DataStore is built for.

## Decides it

Two `tools/tv.py startup --runs 10` medians on the TV, release build: one with
the own-file read on the startup path, one with DataStore. If DataStore is
within noise (a few milliseconds), prefer it: less code to own. Write both
numbers here and change the status to accepted.

## Sources

- DataStore releases: https://developer.android.com/jetpack/androidx/releases/datastore
- "Prefer storing data with Jetpack DataStore", Android Developers Blog,
  2020-09-02: https://android-developers.googleblog.com/2020/09/prefer-storing-data-with-jetpack.html
- `AtomicFile`: https://developer.android.com/reference/android/util/AtomicFile
