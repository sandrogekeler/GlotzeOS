# The TV: connecting, installing, making GlotzeOS the home screen

Runbook for a local session on a computer on the same network as the TV. The
cloud session that set this repo up could not reach the TV; nothing below has
been run on it yet. Steps marked *per model* differ between TV makers: check
on the TV, and correct this file once they are confirmed.

## This TV

| | |
|---|---|
| Model | Chromecast with Google TV on the living-room Hisense; 4K or HD [unknown], see step 4 |
| Android TV version / API level | [unknown], expected 14 / 34 after Google's 2025 update |
| Stock launcher package | [unknown], find it with the command in step 5 |
| ADB over Wi-Fi survives a reboot | [unknown], check before step 6 |

## 1. Developer options

Settings, then System (or Device Preferences), then About. Select the build
entry (*per model*: "Build", "Android TV OS build") about seven times until the
TV says you are a developer. Developer options appear in the same menu.

## 2. Connect ADB over the network

Needs `adb` from Android's platform-tools on the computer (Android Studio
installs it).

- **Android 11 and newer:** Developer options, Wireless debugging, on. Choose
  *Pair device with pairing code*, then on the computer:

  ```sh
  adb pair <tv-ip>:<pairing-port>     # enter the 6-digit code shown on the TV
  adb connect <tv-ip>:<connect-port>  # the port on the Wireless debugging screen, not the pairing one
  ```

  The connect port changes every time Wireless debugging is turned on.
- **Android 9 and 10:** Developer options, turn on USB debugging and, where the
  TV has it, network or ADB debugging (*per model*). Then
  `adb connect <tv-ip>:5555` and accept the prompt on the TV.

`adb devices` should list the TV as `device`, not `unauthorized`.

## 3. Install and open

```sh
./gradlew installDebug
adb shell am start -n io.github.sandrogekeler.glotze/.MainActivity
```

GlotzeOS also appears in the TV's own app list, so it can be opened with the
remote while the stock launcher is still the home screen. Check that every app
appears, that the remote moves between them, and that each one opens.

## 4. Fill in the table above

```sh
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release   # Android version
adb shell getprop ro.build.version.sdk       # API level: set minSdk to this
```

## 5. Find the current home screen

```sh
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

The first line of the result is the package that answers the Home button.

## 6. Make GlotzeOS the home screen

Do not start this until steps 3 to 5 work and you know whether ADB over Wi-Fi
is still on after the TV restarts. If the stock launcher is disabled, GlotzeOS
crashes, and ADB is off after a reboot, the way back is USB debugging or a
factory reset.

1. **Settings first.** If the TV has Settings, Apps, Default apps, Home app
   (*per model*), pick GlotzeOS there. Nothing needs disabling.
2. **Otherwise disable the stock launcher**, leaving it installed:

   ```sh
   adb shell pm disable-user --user 0 <stock-launcher-package>
   ```

   On Google TV, community guides (tested on a Chromecast with Google TV, not
   on this TV) name `com.google.android.apps.tv.launcherx` and say
   `com.google.android.tungsten.setupwraith` turns it back on unless it is
   disabled too. They also report the remote's YouTube button stops working.
3. Press Home. GlotzeOS should open.

### Undo

```sh
adb shell pm enable <stock-launcher-package>
adb shell pm enable com.google.android.tungsten.setupwraith   # only if you disabled it
```

Run the undo once, before relying on step 6, to know it works on this TV.

Use `disable-user`, never `pm uninstall`: disabling is undone with one
command, and an uninstalled system app's recovery varies by device.
