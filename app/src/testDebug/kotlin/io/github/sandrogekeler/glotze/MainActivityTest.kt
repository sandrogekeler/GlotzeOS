package io.github.sandrogekeler.glotze

import io.github.sandrogekeler.glotze.home.LaunchResult
import io.github.sandrogekeler.glotze.home.launchApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {
    @Test
    fun backNeverLeavesTheHomeScreen() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.onBackPressedDispatcher.onBackPressed()
        assertFalse(activity.isFinishing)
    }

    @Test
    fun openingAnAppThatIsGoneDoesNotCrash() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assertEquals(LaunchResult.NotInstalled, launchApp(activity, "com.example.uninstalled"))
        assertFalse(activity.isFinishing)
    }
}
