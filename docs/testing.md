# Testing GlotzeOS: on the PC and on the TV

Four loops, fastest first. Each catches what the one before cannot. Commands
are written for Windows (`python`, `gradlew.bat`); on macOS or Linux use
`python3` and `./gradlew`.

| Loop | Takes | Catches | Never use it for |
|---|---|---|---|
| 1. JVM tests | seconds | logic, focus order, Back, crashes on launch | how it looks or how fast it is |
| 2. Compose Preview | instant | layout, text, states | focus, real apps |
| 3. Android TV emulator | a minute | real apps, the remote, the manifest | **speed** |
| 4. The TV | a minute | everything, and the only numbers that count | |

## 1. JVM tests

```sh
gradlew.bat check                         # what CI runs: format, Lint, all unit tests
gradlew.bat testDebugUnitTest             # only the tests
python -m unittest discover -s tools      # tools/tv.py
```

- `app/src/test/`: pure functions (`homeOrder`, `fitWithin`). Plain JUnit, no
  Android.
- `app/src/testDebug/`: Robolectric, an Android framework on the JVM pinned to
  SDK 34 (the TV's expected level). Compose UI tests drive the grid with
  D-pad keys and check focus; activity tests check Back and launching. Debug
  only, because `createComposeRule()` needs a test activity that only the debug
  build carries.

Add a test here for every rule the remote depends on. It is the only loop that
runs on every pull request.

## 2. Compose Preview

Open `HomeScreen.kt` in Android Studio; the `tv_1080p` preview renders without
a device. Previews use `NoArt`, so cards show labels.

## 3. Android TV emulator

Once:

```sh
python tools/tv.py emulator create        # API 34 Android TV, 1080p, x86
```

The tool checks the image name against what `sdkmanager --list` offers and
lists the TV images it does offer if the default is missing. Use `--tag
google-tv` for the Google TV image, `--abi arm64-v8a` on Apple silicon. API 34
TV images exist only for x86 and arm64-v8a.

Then, each session:

```sh
python tools/tv.py emulator start
python tools/tv.py install --debug        # debug: debugger, Live Edit, StrictMode logging
python tools/tv.py open
python tools/tv.py logs                   # StrictMode reports main-thread disk work here
```

The emulator's keyboard arrows and Enter are the remote's D-pad and centre.
Android Studio's Live Edit works on it (API 30+) for changes inside a
function.

The emulator runs on the PC's CPU and GPU. Its timings say nothing about a
Cortex-A55 with 2 GB, so never quote them.

## 4. The TV

Connecting the first time, and the launcher swap with its undo, are in
[`device.md`](device.md). Day to day:

```sh
python tools/tv.py connect <tv-ip>:<port>
python tools/tv.py install                # release: R8, no debugger, the build to judge
python tools/tv.py open
```

### Quick speed check after a change

```sh
python tools/tv.py startup --runs 10 --compare-stock
```

Force-stops and cold-starts GlotzeOS ten times with `am start -W` and prints
the median `TotalTime` (intent to first frame), then does the same for the
stock launcher. Rules:

- Only a release build counts; the tool warns about a debug build.
- Compare medians from the same session; the TV's background work varies.
- `--compile profile` measures as if the Baseline Profile were applied,
  `--compile none` the worst case right after an install.

### The real number: Macrobenchmark

```sh
gradlew.bat :benchmark:connectedBenchmarkReleaseAndroidTest
```

Ten cold starts with and without the Baseline Profile, reporting time to
first frame (`timeToInitialDisplayMs`) and to the app list on screen
(`timeToFullDisplayMs`, from `ReportDrawnWhen`). Results are under
`benchmark/build/outputs/connected_android_test_additional_output/`. The
`withBaselineProfile` median of `timeToFullDisplayMs` is the budget in
`agent_docs/HEALTH_CHECKLIST.md`.

Run it while the stock launcher is still the home app: Macrobenchmark kills
GlotzeOS between runs, and as the home app the system would restart it.

### Baseline Profile

```sh
gradlew.bat :app:generateBaselineProfile
```

Runs `BaselineProfileGenerator` on the connected TV (or an API 33+ emulator)
and writes the profile to `app/src/release/generated/baselineProfiles/`.
Commit it. Regenerate after a change to startup or the grid, and check the
gain with the benchmark above.
