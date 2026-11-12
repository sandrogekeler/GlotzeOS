package io.github.sandrogekeler.glotze.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The TV's apps in home-screen order, kept current while the home screen exists.
 *
 * [apps] is null until the first load finishes, so "still loading" and "no apps" differ.
 * Installs, updates and removals arrive as package broadcasts; a burst of them (an update
 * sends removed, then added) collapses into one reload, and an unchanged list is not
 * emitted again because [StateFlow] skips equal values.
 */
class AppCatalog(
    private val context: Context,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val state = MutableStateFlow<List<TvApp>?>(null)
    val apps: StateFlow<List<TvApp>?> = state.asStateFlow()

    private val reloads = Channel<Unit>(Channel.CONFLATED)

    private val packageChanges =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                reload()
            }
        }

    /** Loads the list now and keeps it current until [stop]. */
    fun start() {
        scope.launch {
            for (request in reloads) {
                try {
                    state.value = withContext(io) { homeOrder(loadTvApps(context.packageManager), context.packageName) }
                } catch (e: RuntimeException) {
                    // PackageManager calls go over binder; keep showing the last good list.
                    Log.w(TAG, "Could not load the app list", e)
                }
            }
        }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
        // Package broadcasts come from the system and are protected: no other app can send them,
        // and the broadcasts guide says to export a receiver that listens for system broadcasts.
        ContextCompat.registerReceiver(context, packageChanges, filter, ContextCompat.RECEIVER_EXPORTED)
        reload()
    }

    /** Asks for a fresh list; requests made while one is loading collapse into one more load. */
    fun reload() {
        reloads.trySend(Unit)
    }

    fun stop() {
        context.unregisterReceiver(packageChanges)
        reloads.close()
    }

    private companion object {
        const val TAG = "AppCatalog"
    }
}
