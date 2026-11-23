package io.github.sandrogekeler.glotze.home

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppArtLoaderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val loader = AppArtLoader(context.packageManager)
    private val app = TvApp("com.example.player", "Player", "com.example.player.Main")

    @Test
    fun artIsCachedForTheSameRevision() {
        install(app)
        runBlocking { assertNotNull(loader.load(app, 320, 180)) }
        assertNotNull(loader.cached(app))
    }

    @Test
    fun artFromAnOlderRevisionIsNotShown() {
        install(app)
        runBlocking { loader.load(app, 320, 180) }
        assertNull(loader.cached(app.copy(revision = app.revision + 1)))
    }

    private fun install(app: TvApp) {
        val packageManager = shadowOf(context.packageManager)
        packageManager.installPackage(
            PackageInfo().apply {
                packageName = app.packageName
                applicationInfo = ApplicationInfo().apply { packageName = app.packageName }
            },
        )
        packageManager.addActivityIfNotPresent(ComponentName(app.packageName, app.activityName))
    }
}
