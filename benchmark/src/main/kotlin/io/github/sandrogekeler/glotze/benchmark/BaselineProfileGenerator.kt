package io.github.sandrogekeler.glotze.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code startup and grid navigation run, so it is compiled at install time
 * instead of interpreted on the TV's first launches.
 *
 *   ./gradlew :app:generateBaselineProfile
 *
 * Needs API 33+ (no root), so the TV or an API 34 emulator. The result lands in
 * app/src/release/generated/baselineProfiles/ and is committed.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndBrowse() =
        rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
            startHome()
            // Move through the grid the way the remote does, so focus and scrolling code is included.
            repeat(3) { device.pressDPadRight() }
            repeat(3) { device.pressDPadDown() }
            repeat(3) { device.pressDPadUp() }
            device.waitForIdle()
        }
}
