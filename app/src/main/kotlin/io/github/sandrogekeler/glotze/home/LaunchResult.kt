package io.github.sandrogekeler.glotze.home

import android.content.ActivityNotFoundException
import android.content.Context

sealed interface LaunchResult {
    data object Launched : LaunchResult

    /** Uninstalled or disabled since the list was loaded. */
    data object NotInstalled : LaunchResult

    /** The system refused, for example a permission the app's activity requires. */
    data class Refused(
        val reason: String,
    ) : LaunchResult
}

/**
 * Opens an app without ever throwing: the home screen is the one activity that must not
 * crash, because with the stock launcher disabled there is nothing behind it.
 */
fun launchApp(
    context: Context,
    packageName: String,
): LaunchResult {
    val packageManager = context.packageManager
    val intent =
        packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: packageManager.getLaunchIntentForPackage(packageName)
            ?: return LaunchResult.NotInstalled
    return try {
        context.startActivity(intent)
        LaunchResult.Launched
    } catch (e: ActivityNotFoundException) {
        LaunchResult.NotInstalled
    } catch (e: SecurityException) {
        LaunchResult.Refused(e.message ?: e.javaClass.simpleName)
    }
}
