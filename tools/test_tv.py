"""Unit tests for the parts of tv.py that do not need a device.

  python3 -m unittest discover -s tools
"""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import tv  # noqa: E402


class AmStart(unittest.TestCase):
    OK = """Starting: Intent { cmp=io.github.sandrogekeler.glotze/.MainActivity }
Status: ok
LaunchState: COLD
Activity: io.github.sandrogekeler.glotze/.MainActivity
TotalTime: 812
WaitTime: 830
Complete
"""

    def test_reads_total_and_wait_time(self):
        self.assertEqual(tv.parse_am_start(self.OK), {"TotalTime": 812, "WaitTime": 830})

    def test_ok_output_is_not_a_failure(self):
        self.assertIsNone(tv.am_start_failed(self.OK))

    def test_missing_activity_is_a_failure(self):
        out = "Starting: Intent { cmp=x/.Y }\nError type 3\nError: Activity class {x/x.Y} does not exist.\n"
        self.assertEqual(tv.am_start_failed(out), "Error type 3")
        self.assertEqual(tv.parse_am_start(out), {})

    def test_non_ok_status_is_a_failure(self):
        self.assertEqual(tv.am_start_failed("Status: timeout\nTotalTime: 0\n"), "Status: timeout")


class Summarise(unittest.TestCase):
    def test_median_min_max(self):
        s = tv.summarise([900, 700, 800])
        self.assertEqual((s.median, s.minimum, s.maximum, s.runs), (800, 700, 900, 3))

    def test_even_count_median_is_the_mean_of_the_middle_two(self):
        self.assertEqual(tv.summarise([1, 2, 3, 4]).median, 2.5)

    def test_no_runs_is_an_error_not_a_zero(self):
        with self.assertRaises(tv.ToolError):
            tv.summarise([])


class Launchers(unittest.TestCase):
    QUERY = """3 activities found:
  Activity #0:
    priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true
    com.google.android.apps.tv.launcherx/.home.HomeActivity
  Activity #1:
    priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=false
    io.github.sandrogekeler.glotze/.MainActivity
  Activity #2:
    priority=-1000 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=false
    com.android.tv.settings/.FallbackHome
"""

    def test_reads_every_component(self):
        self.assertEqual(
            tv.parse_components(self.QUERY),
            [
                "com.google.android.apps.tv.launcherx/.home.HomeActivity",
                "io.github.sandrogekeler.glotze/.MainActivity",
                "com.android.tv.settings/.FallbackHome",
            ],
        )

    def test_resolve_activity_brief(self):
        out = "priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\ncom.x/.Home\n"
        self.assertEqual(tv.parse_components(out), ["com.x/.Home"])

    def test_other_launchers_leave_out_glotze_and_fallback(self):
        self.assertEqual(
            tv.other_launchers(tv.parse_components(self.QUERY)),
            ["com.google.android.apps.tv.launcherx/.home.HomeActivity"],
        )

    def test_nothing_found(self):
        self.assertEqual(tv.parse_components("No activity found\n"), [])


class DeviceFacts(unittest.TestCase):
    def test_meminfo_in_megabytes(self):
        self.assertEqual(tv.parse_meminfo_total_mb("MemTotal:        1955144 kB\nMemFree: 1 kB\n"), 1909)

    def test_meminfo_missing(self):
        self.assertIsNone(tv.parse_meminfo_total_mb(""))

    def test_debuggable_flag(self):
        self.assertTrue(tv.is_debuggable("    flags=[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ]"))
        self.assertFalse(tv.is_debuggable("    flags=[ HAS_CODE ALLOW_CLEAR_USER_DATA ]"))


class Sdk(unittest.TestCase):
    def test_windows_local_properties_are_unescaped(self):
        text = "sdk.dir=C\\:\\\\Users\\\\sandro\\\\AppData\\\\Local\\\\Android\\\\Sdk\n"
        self.assertEqual(tv.sdk_from_local_properties(text), "C:\\Users\\sandro\\AppData\\Local\\Android\\Sdk")

    def test_candidates_most_explicit_first(self):
        found = tv.sdk_candidates(
            {"ANDROID_HOME": "/a", "LOCALAPPDATA": "/l"}, "sdk.dir=/p\n", Path("/h")
        )
        self.assertEqual(found[:3], [Path("/a"), Path("/p"), Path("/l/Android/Sdk")])

    def test_tv_images_from_sdkmanager_list(self):
        listing = """Available Packages:
  system-images;android-34;android-tv;x86_64       | 3 | Android TV Intel x86_64 Atom System Image
  system-images;android-34;google-tv;x86_64        | 3 | Google TV Intel x86_64 Atom System Image
  system-images;android-34;google_apis;x86_64      | 14 | Google APIs Intel x86_64 Atom System Image
"""
        self.assertEqual(
            tv.tv_images(listing),
            ["system-images;android-34;android-tv;x86_64", "system-images;android-34;google-tv;x86_64"],
        )

    def test_system_image_name(self):
        self.assertEqual(tv.system_image(34, "android-tv", "x86_64"), "system-images;android-34;android-tv;x86_64")


class Cli(unittest.TestCase):
    def test_every_command_parses(self):
        parser = tv.build_parser()
        for argv in (
            ["info"],
            ["startup", "--runs", "3", "--compare-stock", "--compile", "profile"],
            ["home", "status"],
            ["emulator", "create", "--api", "34"],
            ["-s", "1.2.3.4:5555", "install", "--debug"],
        ):
            self.assertTrue(callable(parser.parse_args(argv).func), argv)


if __name__ == "__main__":
    unittest.main()
