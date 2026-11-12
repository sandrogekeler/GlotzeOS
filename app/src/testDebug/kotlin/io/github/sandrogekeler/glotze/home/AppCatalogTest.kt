package io.github.sandrogekeler.glotze.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppCatalogTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    // Unconfined: loads run inline, so the test reads the result without waiting.
    private val catalog = AppCatalog(context, CoroutineScope(Dispatchers.Unconfined), io = Dispatchers.Unconfined)

    @After
    fun stop() = catalog.stop()

    @Test
    fun listsTvAppsButNotItself() {
        installTvApp("com.example.player")
        catalog.start()
        assertEquals(listOf("com.example.player"), catalog.apps.value?.map { it.packageName })
    }

    @Test
    fun picksUpAnAppInstalledWhileRunning() {
        catalog.start()
        assertTrue(catalog.apps.value.orEmpty().none { it.packageName == "com.example.new" })

        installTvApp("com.example.new")
        context.sendBroadcast(Intent(Intent.ACTION_PACKAGE_ADDED, Uri.parse("package:com.example.new")))
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(catalog.apps.value.orEmpty().any { it.packageName == "com.example.new" })
    }

    private fun installTvApp(packageName: String) {
        val packageManager = shadowOf(context.packageManager)
        packageManager.installPackage(
            PackageInfo().apply {
                this.packageName = packageName
                applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
            },
        )
        val activity = ComponentName(packageName, "$packageName.Main")
        packageManager.addActivityIfNotPresent(activity)
        packageManager.addIntentFilterForActivity(
            activity,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) },
        )
    }
}
