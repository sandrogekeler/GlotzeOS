package io.github.sandrogekeler.glotze

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import io.github.sandrogekeler.glotze.home.AppArtLoader
import io.github.sandrogekeler.glotze.home.AppCatalog
import io.github.sandrogekeler.glotze.home.HomeScreen
import io.github.sandrogekeler.glotze.home.LaunchResult
import io.github.sandrogekeler.glotze.home.TvApp
import io.github.sandrogekeler.glotze.home.launchApp

class MainActivity : ComponentActivity() {
    private lateinit var catalog: AppCatalog

    override fun onCreate(savedInstanceState: Bundle?) {
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) flagSlowMainThreadWork()
        super.onCreate(savedInstanceState)
        catalog = AppCatalog(applicationContext, lifecycleScope).also { it.start() }
        val art = AppArtLoader(packageManager)
        setContent {
            // The home screen is the bottom of the stack: Back has nowhere to go.
            BackHandler {}
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val apps by catalog.apps.collectAsState()
                    // Startup ends when the list is on screen, not at the first empty frame.
                    ReportDrawnWhen { apps != null }
                    HomeScreen(apps = apps.orEmpty(), onOpen = ::open, art = art)
                }
            }
        }
    }

    override fun onDestroy() {
        catalog.stop()
        super.onDestroy()
    }

    private fun open(app: TvApp) {
        when (val result = launchApp(this, app.packageName)) {
            LaunchResult.Launched -> {
                Unit
            }

            LaunchResult.NotInstalled -> {
                Log.i(TAG, "${app.packageName} is no longer installed; reloading the list")
                catalog.reload()
            }

            is LaunchResult.Refused -> {
                Log.w(TAG, "Could not open ${app.packageName}: ${result.reason}")
            }
        }
    }

    /** Debug builds only: logs disk and network access on the main thread, and leaks. */
    private fun flagSlowMainThreadWork() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy
                .Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy
                .Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
    }

    private companion object {
        const val TAG = "GlotzeOS"
    }
}
