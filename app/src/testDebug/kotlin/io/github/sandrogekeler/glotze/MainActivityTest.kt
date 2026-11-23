package io.github.sandrogekeler.glotze

import android.content.res.Configuration
import io.github.sandrogekeler.glotze.home.LaunchResult
import io.github.sandrogekeler.glotze.home.launchApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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

    @Test
    fun connectingARemoteDoesNotRecreateTheHomeScreen() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val before = controller.get()
        val remote =
            Configuration(before.resources.configuration).apply {
                navigation = Configuration.NAVIGATION_DPAD
                keyboard = Configuration.KEYBOARD_QWERTY
            }
        controller.configurationChange(remote)
        assertSame(before, controller.get())
    }

    @Test
    fun aRecreatedHomeScreenKeepsTheLoadedList() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val catalog = (controller.get().application as GlotzeApplication).catalog
        controller.recreate()
        assertSame(catalog, (controller.get().application as GlotzeApplication).catalog)
    }
}
