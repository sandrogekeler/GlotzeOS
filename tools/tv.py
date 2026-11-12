#!/usr/bin/env python3
"""GlotzeOS on the TV and the emulator, one command per step.

Standard library only, so it runs on Windows, macOS and Linux with nothing to
install beyond Python 3.9 and the Android SDK. Every command prints the adb or
Gradle call it makes, so nothing happens out of sight.

  python tools/tv.py connect 192.168.1.50:41234   # after pairing once
  python tools/tv.py info                         # the device table for docs/device.md
  python tools/tv.py install                      # release build, the one to judge speed by
  python tools/tv.py startup --runs 10 --compare-stock
  python tools/tv.py emulator create && python tools/tv.py emulator start

Target a device with -s SERIAL or ANDROID_SERIAL when more than one is attached,
exactly as with adb. See docs/testing.md for the whole loop.
"""

from __future__ import annotations

import argparse
import os
import re
import shutil
import statistics
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path

PACKAGE = "io.github.sandrogekeler.glotze"
ACTIVITY = f"{PACKAGE}/.MainActivity"
ROOT = Path(__file__).resolve().parent.parent
WINDOWS = os.name == "nt"

AVD_NAME = "GlotzeTV"
# Android TV 14 is what the Chromecast with Google TV runs. The image name is
# checked against `sdkmanager --list` before use, never assumed. API 34 TV images are
# x86 and arm64-v8a only; x86_64 TV images start at API 36.
DEFAULT_IMAGE_API = 34
DEFAULT_IMAGE_TAG = "android-tv"
DEFAULT_ABI = "x86"
DEFAULT_DEVICE = "tv_1080p"

# Activities that answer HOME but are not a launcher anyone chose.
NOT_A_LAUNCHER = ("FallbackHome", "com.android.tv.settings", "com.android.settings")

# Google TV re-enables its launcher through this package unless it is disabled
# too (community guides, tested on a Chromecast with Google TV; see docs/device.md).
GOOGLE_TV_SETUP = "com.google.android.tungsten.setupwraith"


class ToolError(Exception):
    """A problem the user can fix, reported without a traceback."""


# --- Pure helpers (unit-tested in test_tv.py) ---------------------------------


def parse_am_start(output: str) -> dict[str, int]:
    """Timings from `am start -W`, e.g. {"TotalTime": 812, "WaitTime": 830}.

    Missing keys are left out; older Android versions print ThisTime too.
    """
    times = {}
    for key, value in re.findall(r"^\s*(\w+Time):\s*(\d+)\s*$", output, re.MULTILINE):
        times[key] = int(value)
    return times


def am_start_failed(output: str) -> str | None:
    """The error line of a failed `am start`, or None when it started."""
    for line in output.splitlines():
        if line.startswith("Error") or "Exception" in line:
            return line.strip()
    status = re.search(r"^Status:\s*(\w+)", output, re.MULTILINE)
    if status and status.group(1) != "ok":
        return f"Status: {status.group(1)}"
    return None


@dataclass(frozen=True)
class Summary:
    runs: int
    median: float
    minimum: int
    maximum: int

    def line(self) -> str:
        return f"median {self.median:.0f} ms, min {self.minimum} ms, max {self.maximum} ms, {self.runs} runs"


def summarise(times_ms: list[int]) -> Summary:
    if not times_ms:
        raise ToolError("no successful runs to summarise")
    return Summary(len(times_ms), statistics.median(times_ms), min(times_ms), max(times_ms))


COMPONENT = re.compile(r"^\s*([A-Za-z][\w.]*/[\w.$]+)\s*$")


def parse_components(output: str) -> list[str]:
    """Component names (package/.Activity) from `cmd package` --brief output.

    The surrounding lines (priority=..., "N activities found") differ between
    Android versions, so only lines that are a component on their own count.
    """
    found = []
    for line in output.splitlines():
        match = COMPONENT.match(line)
        if match and match.group(1) not in found:
            found.append(match.group(1))
    return found


def other_launchers(components: list[str], own_package: str = PACKAGE) -> list[str]:
    """HOME activities that are a real launcher and not GlotzeOS."""
    return [
        c
        for c in components
        if not c.startswith(own_package + "/") and not any(skip in c for skip in NOT_A_LAUNCHER)
    ]


def parse_meminfo_total_mb(meminfo: str) -> int | None:
    match = re.search(r"^MemTotal:\s*(\d+)\s*kB", meminfo, re.MULTILINE)
    return round(int(match.group(1)) / 1024) if match else None


def is_debuggable(dumpsys_package: str) -> bool:
    """True when `dumpsys package <pkg>` lists the DEBUGGABLE flag."""
    return re.search(r"(pkgFlags|flags)=\[[^\]]*\bDEBUGGABLE\b", dumpsys_package) is not None


def system_image(api: int, tag: str, abi: str) -> str:
    return f"system-images;android-{api};{tag};{abi}"


def tv_images(sdkmanager_list: str) -> list[str]:
    """Every TV system image sdkmanager offers, installed or not, as `;` package ids.

    Newer cmdline-tools list packages with `/` instead of `;`; both are read.
    """
    found = re.findall(r"(system-images[;/]android-[\w.-]+[;/](?:android-tv|google-tv)[\w-]*[;/][\w-]+)", sdkmanager_list)
    return sorted({image.replace("/", ";") for image in found})


def sdk_from_local_properties(text: str) -> str | None:
    match = re.search(r"^\s*sdk\.dir\s*=\s*(.+?)\s*$", text, re.MULTILINE)
    if not match:
        return None
    # local.properties escapes ':' and '\' on Windows (C\:\\Users\\...).
    return match.group(1).replace("\\:", ":").replace("\\\\", "\\")


def sdk_candidates(env: dict[str, str], local_properties: str | None, home: Path) -> list[Path]:
    """Where the Android SDK may be, most explicit first."""
    found = []
    for key in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if env.get(key):
            found.append(Path(env[key]))
    if local_properties:
        sdk = sdk_from_local_properties(local_properties)
        if sdk:
            found.append(Path(sdk))
    if env.get("LOCALAPPDATA"):
        found.append(Path(env["LOCALAPPDATA"]) / "Android" / "Sdk")  # Android Studio on Windows
    found.append(home / "Library" / "Android" / "sdk")  # macOS
    found.append(home / "Android" / "Sdk")  # Linux
    return found


# --- Running things -------------------------------------------------------------


def find_sdk() -> Path | None:
    props = ROOT / "local.properties"
    text = props.read_text(encoding="utf-8") if props.is_file() else None
    for candidate in sdk_candidates(dict(os.environ), text, Path.home()):
        if candidate.is_dir():
            return candidate
    return None


def sdk_tool(*parts: str, windows_suffix: str = ".exe") -> str:
    """Path of an SDK tool, falling back to PATH."""
    name = parts[-1] + (windows_suffix if WINDOWS else "")
    sdk = find_sdk()
    if sdk:
        path = sdk.joinpath(*parts[:-1], name)
        if path.is_file():
            return str(path)
    on_path = shutil.which(parts[-1])
    if on_path:
        return on_path
    raise ToolError(
        f"{parts[-1]} not found. Install it with Android Studio's SDK Manager and set ANDROID_HOME, "
        "or put sdk.dir in local.properties."
    )


def adb_path() -> str:
    return sdk_tool("platform-tools", "adb")


def run(cmd: list[str], check: bool = True, capture: bool = True, quiet: bool = False) -> str:
    if not quiet:
        print("$ " + " ".join(cmd), file=sys.stderr)
    result = subprocess.run(cmd, capture_output=capture, text=True, encoding="utf-8", errors="replace")
    out = (result.stdout or "") + (result.stderr or "") if capture else ""
    if check and result.returncode != 0:
        raise ToolError(f"{cmd[0]} exited with {result.returncode}\n{out.strip()}")
    return out


class Adb:
    def __init__(self, serial: str | None):
        self.base = [adb_path()] + (["-s", serial] if serial else [])

    def __call__(self, *args: str, check: bool = True, quiet: bool = False) -> str:
        return run(self.base + list(args), check=check, quiet=quiet)

    def shell(self, command: str, check: bool = True, quiet: bool = False) -> str:
        return self(*["shell", command], check=check, quiet=quiet)

    def prop(self, name: str) -> str:
        return self.shell(f"getprop {name}", quiet=True).strip()

    def ensure_device(self) -> None:
        state = self("get-state", check=False, quiet=True).strip()
        if state != "device":
            raise ToolError(
                "No device ready (adb get-state said: " + (state or "nothing") + ").\n"
                "Connect the TV with `tv.py connect`, start the emulator with `tv.py emulator start`, "
                "or pick one with -s when several are attached."
            )

    def home_activities(self) -> list[str]:
        out = self.shell(
            "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME",
            check=False,
            quiet=True,
        )
        return parse_components(out)

    def current_home(self) -> str | None:
        out = self.shell(
            "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME",
            check=False,
            quiet=True,
        )
        found = parse_components(out)
        return found[0] if found else None


def gradlew() -> str:
    return str(ROOT / ("gradlew.bat" if WINDOWS else "gradlew"))


def confirm(question: str) -> bool:
    try:
        return input(question + " [y/N] ").strip().lower() in ("y", "yes")
    except EOFError:
        return False


# --- Commands -------------------------------------------------------------------


def cmd_pair(args: argparse.Namespace) -> None:
    adb = Adb(None)
    print(adb("pair", args.address, *([args.code] if args.code else [])))
    print("Paired. Now run: tv.py connect <tv-ip>:<port shown on the Wireless debugging screen>")


def cmd_connect(args: argparse.Namespace) -> None:
    address = args.address if ":" in args.address else args.address + ":5555"
    out = Adb(None)("connect", address)
    print(out.strip())
    if "connected" not in out or "cannot" in out or "failed" in out:
        raise ToolError("Not connected. Is Wireless debugging on, and is the port the one on the TV's screen?")
    print(f"Use it with: tv.py -s {address} ... (or set ANDROID_SERIAL={address})")


def cmd_devices(args: argparse.Namespace) -> None:
    print(Adb(None)("devices", "-l", quiet=True).strip())


def cmd_info(args: argparse.Namespace) -> None:
    adb = Adb(args.serial)
    adb.ensure_device()
    model = adb.prop("ro.product.model")
    maker = adb.prop("ro.product.manufacturer")
    release = adb.prop("ro.build.version.release")
    sdk = adb.prop("ro.build.version.sdk")
    build = adb.prop("ro.build.id")
    abi = adb.prop("ro.product.cpu.abi")
    ram = parse_meminfo_total_mb(adb.shell("cat /proc/meminfo", quiet=True))
    size = adb.shell("wm size", quiet=True).strip().splitlines()
    density = adb.shell("wm density", quiet=True).strip().splitlines()
    home = adb.current_home()
    others = other_launchers(adb.home_activities())
    print("| | |")
    print("|---|---|")
    print(f"| Model | {maker} {model} |")
    print(f"| Android TV version / API level | {release} / {sdk} (build {build}) |")
    print(f"| CPU ABI / RAM | {abi} / {ram} MB |" if ram else f"| CPU ABI | {abi} |")
    print(f"| Screen | {'; '.join(size)}; {'; '.join(density)} |")
    print(f"| Current home activity | {home or 'unknown'} |")
    print(f"| Other launchers installed | {', '.join(others) or 'none found'} |")


def cmd_install(args: argparse.Namespace) -> None:
    task = "installDebug" if args.debug else "installRelease"
    env = dict(os.environ)
    if args.serial:
        env["ANDROID_SERIAL"] = args.serial  # the Gradle install tasks honour it like adb does
    subprocess.run([gradlew(), task], cwd=ROOT, env=env, check=True)
    if not args.debug:
        print("Installed the release build (R8, no debugger): the one to measure.")


def cmd_open(args: argparse.Namespace) -> None:
    adb = Adb(args.serial)
    adb.ensure_device()
    out = adb.shell(f"am start -n {ACTIVITY}")
    error = am_start_failed(out)
    if error:
        raise ToolError(f"GlotzeOS did not start: {error}. Is it installed? Try `tv.py install`.")
    print("GlotzeOS is open on the TV.")


def cmd_logs(args: argparse.Namespace) -> None:
    adb = Adb(args.serial)
    adb.ensure_device()
    pid = adb.shell(f"pidof {PACKAGE}", check=False, quiet=True).strip()
    if not pid:
        raise ToolError("GlotzeOS is not running. Open it with `tv.py open` first.")
    cmd = adb.base + ["logcat", "--pid", pid.split()[0]]
    print("$ " + " ".join(cmd) + "   (Ctrl+C to stop)", file=sys.stderr)
    try:
        subprocess.run(cmd)
    except KeyboardInterrupt:
        pass


def measure(adb: Adb, component: str, runs: int) -> Summary:
    package = component.split("/")[0]
    totals = []
    for i in range(1, runs + 1):
        adb.shell(f"am force-stop {package}", quiet=True)
        out = adb.shell(f"am start -W -S -n {component}", quiet=True)
        error = am_start_failed(out)
        times = parse_am_start(out)
        if error or "TotalTime" not in times:
            print(f"  run {i}: failed ({error or 'no TotalTime in output'})")
            continue
        totals.append(times["TotalTime"])
        print(f"  run {i}: {times['TotalTime']} ms")
    return summarise(totals)


def cmd_startup(args: argparse.Namespace) -> None:
    adb = Adb(args.serial)
    adb.ensure_device()
    if is_debuggable(adb.shell(f"dumpsys package {PACKAGE}", quiet=True)):
        print("Warning: the installed GlotzeOS is a debug build. Its numbers do not count; run `tv.py install`.\n")
    if args.compile == "none":
        adb.shell(f"cmd package compile --reset {PACKAGE}")
    elif args.compile == "profile":
        adb.shell(f"cmd package compile -f -m speed-profile {PACKAGE}")
    elif args.compile == "speed":
        adb.shell(f"cmd package compile -f -m speed {PACKAGE}")

    if adb.current_home() == ACTIVITY:
        print("Note: GlotzeOS is the home app, so the system may restart it between runs.\n")

    print(f"Cold start of GlotzeOS ({args.runs} runs):")
    ours = measure(adb, ACTIVITY, args.runs)
    print("GlotzeOS: " + ours.line())

    if args.compare_stock:
        others = other_launchers(adb.home_activities())
        if not others:
            print("No other launcher found to compare with.")
            return
        stock = others[0]
        print(f"\nCold start of {stock} ({args.runs} runs):")
        theirs = measure(adb, stock, args.runs)
        print(f"{stock}: " + theirs.line())
        print(f"\nGlotzeOS is {theirs.median - ours.median:+.0f} ms against it (positive: GlotzeOS is faster).")
        # Leave the TV on its usual home screen.
        adb.shell("input keyevent KEYCODE_HOME", quiet=True)

    print("\nTotalTime is from the launch intent to the first frame (am start -W).")


def cmd_home(args: argparse.Namespace) -> None:
    adb = Adb(args.serial)
    adb.ensure_device()
    current = adb.current_home()
    others = other_launchers(adb.home_activities())
    print(f"Home now: {current or 'unknown'}")
    print(f"Other launchers: {', '.join(others) or 'none'}")
    if args.action == "status":
        return

    stock = args.package or (others[0].split("/")[0] if others else None)
    if not stock:
        raise ToolError("No stock launcher found. Pass it with --package.")
    extra = [GOOGLE_TV_SETUP] if args.google_tv_setup else []

    undo = [f"adb shell pm enable {p}" for p in [stock, *extra]]
    if args.action == "undo":
        for p in [stock, *extra]:
            print(adb.shell(f"pm enable {p}").strip())
        print("Stock launcher enabled. Press Home on the remote.")
        return

    # set
    print("\nThis disables (does not uninstall) the stock launcher so Home opens GlotzeOS.")
    print("Undo, also printed in docs/device.md:")
    for line in undo:
        print("  " + line)
    print("Before going on: the undo has been tried once, and Wireless debugging survives a TV restart.")
    if not confirm(f"Disable {', '.join([stock, *extra])} now?"):
        print("Nothing changed.")
        return
    for p in [stock, *extra]:
        print(adb.shell(f"pm disable-user --user 0 {p}").strip())
    adb.shell("input keyevent KEYCODE_HOME", quiet=True)
    print("Done. Press Home: GlotzeOS should open.")


def cmd_emulator(args: argparse.Namespace) -> None:
    if args.action == "create":
        sdkmanager = sdk_tool("cmdline-tools", "latest", "bin", "sdkmanager", windows_suffix=".bat")
        avdmanager = sdk_tool("cmdline-tools", "latest", "bin", "avdmanager", windows_suffix=".bat")
        image = system_image(args.api, args.tag, args.abi)
        offered = tv_images(run([sdkmanager, "--list"], quiet=True))
        if image not in offered:
            raise ToolError(
                f"{image} is not offered by this SDK. TV images it offers:\n  "
                + ("\n  ".join(offered) or "none (update the SDK command-line tools)")
                + "\nPick one with --api, --tag and --abi."
            )
        subprocess.run([sdkmanager, image, "emulator", "platform-tools"], check=True)
        # avdmanager asks about a custom hardware profile; "no" keeps the device definition.
        subprocess.run(
            [avdmanager, "create", "avd", "--force", "-n", AVD_NAME, "-k", image, "-d", args.device],
            input="no\n",
            text=True,
            check=True,
        )
        print(f"Created {AVD_NAME}. Start it with: tv.py emulator start")
    elif args.action == "start":
        emulator = sdk_tool("emulator", "emulator")
        cmd = [emulator, "-avd", AVD_NAME, "-netdelay", "none", "-netspeed", "full"]
        if args.cold:
            cmd.append("-no-snapshot-load")
        print("$ " + " ".join(cmd), file=sys.stderr)
        flags = {"creationflags": subprocess.DETACHED_PROCESS} if WINDOWS else {"start_new_session": True}
        subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, **flags)
        print("Waiting for the emulator to boot...")
        adb = Adb(None)
        adb("wait-for-device", quiet=True)
        while adb.shell("getprop sys.boot_completed", check=False, quiet=True).strip() != "1":
            time.sleep(2)
        print("Booted. Install with: tv.py install --debug")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="tv.py", description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("-s", "--serial", default=os.environ.get("ANDROID_SERIAL"), help="device serial or ip:port")
    sub = parser.add_subparsers(dest="command", required=True)

    p = sub.add_parser("pair", help="pair with the TV (Wireless debugging, Android 11+)")
    p.add_argument("address", help="ip:pairing-port shown under 'Pair device with pairing code'")
    p.add_argument("code", nargs="?", help="the 6-digit code; asked for when left out")
    p.set_defaults(func=cmd_pair)

    p = sub.add_parser("connect", help="connect to the TV over Wi-Fi")
    p.add_argument("address", help="ip:port from the Wireless debugging screen (port 5555 when left out)")
    p.set_defaults(func=cmd_connect)

    sub.add_parser("devices", help="list attached devices").set_defaults(func=cmd_devices)
    sub.add_parser("info", help="print the device table for docs/device.md").set_defaults(func=cmd_info)

    p = sub.add_parser("install", help="build and install (release unless --debug)")
    p.add_argument("--debug", action="store_true", help="debug build: debugger and Live Edit, not for measuring")
    p.set_defaults(func=cmd_install)

    sub.add_parser("open", help="open GlotzeOS").set_defaults(func=cmd_open)
    sub.add_parser("logs", help="follow GlotzeOS's log").set_defaults(func=cmd_logs)

    p = sub.add_parser("startup", help="measure cold start with am start -W")
    p.add_argument("--runs", type=int, default=10)
    p.add_argument("--compare-stock", action="store_true", help="measure the stock launcher the same way")
    p.add_argument(
        "--compile",
        choices=["installed", "none", "profile", "speed"],
        default="installed",
        help="ART compilation before measuring: as installed (default), none, speed-profile, or full speed",
    )
    p.set_defaults(func=cmd_startup)

    p = sub.add_parser("home", help="see, set or undo GlotzeOS as the home screen")
    p.add_argument("action", choices=["status", "set", "undo"])
    p.add_argument("--package", help="stock launcher package (found automatically when left out)")
    p.add_argument(
        "--google-tv-setup",
        action="store_true",
        help=f"also disable/enable {GOOGLE_TV_SETUP}, which re-enables Google TV's launcher",
    )
    p.set_defaults(func=cmd_home)

    p = sub.add_parser("emulator", help="create or start the Android TV emulator")
    p.add_argument("action", choices=["create", "start"])
    p.add_argument("--api", type=int, default=DEFAULT_IMAGE_API)
    p.add_argument("--tag", default=DEFAULT_IMAGE_TAG, help="android-tv or google-tv")
    p.add_argument("--abi", default=DEFAULT_ABI, help="x86 on Intel/AMD, arm64-v8a on Apple silicon")
    p.add_argument("--device", default=DEFAULT_DEVICE)
    p.add_argument("--cold", action="store_true", help="boot without loading the snapshot")
    p.set_defaults(func=cmd_emulator)
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        args.func(args)
    except ToolError as error:
        print(f"tv.py: {error}", file=sys.stderr)
        return 1
    except subprocess.CalledProcessError as error:
        print(f"tv.py: {error.cmd[0]} failed with exit code {error.returncode}", file=sys.stderr)
        return error.returncode or 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
