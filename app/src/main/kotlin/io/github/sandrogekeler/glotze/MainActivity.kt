package io.github.sandrogekeler.glotze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import io.github.sandrogekeler.glotze.home.HomeScreen
import io.github.sandrogekeler.glotze.home.TvApp
import io.github.sandrogekeler.glotze.home.homeOrder
import io.github.sandrogekeler.glotze.home.loadTvApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // The home screen is the bottom of the stack: Back has nowhere to go.
            BackHandler {}
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val apps by produceState(emptyList<TvApp>()) {
                        value = withContext(Dispatchers.IO) { homeOrder(loadTvApps(packageManager), packageName) }
                    }
                    HomeScreen(apps = apps, onOpen = ::open)
                }
            }
        }
    }

    private fun open(app: TvApp) {
        val launch =
            packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
                ?: packageManager.getLaunchIntentForPackage(app.packageName)
        // Null only if the app was uninstalled after the list was loaded.
        if (launch != null) startActivity(launch)
    }
}
