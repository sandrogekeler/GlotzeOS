package io.github.sandrogekeler.glotze.home

import android.content.Intent
import android.content.pm.PackageManager

/** An app the TV would show in its own app list. Its art is loaded separately, see [AppArtLoader]. */
data class TvApp(
    val packageName: String,
    val label: String,
    val activityName: String,
)

/** The intent every TV app's entry activity answers. */
fun tvLaunchableIntent(): Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

/** Every app the TV would show in its own app list. Blocking: call it on [kotlinx.coroutines.Dispatchers.IO]. */
fun loadTvApps(packageManager: PackageManager): List<TvApp> =
    packageManager.queryIntentActivities(tvLaunchableIntent(), 0).map { info ->
        TvApp(
            packageName = info.activityInfo.packageName,
            label = info.loadLabel(packageManager).toString(),
            activityName = info.activityInfo.name,
        )
    }

/** The order the home screen shows apps in: by name, once each, without GlotzeOS itself. */
fun homeOrder(
    apps: List<TvApp>,
    ownPackage: String,
): List<TvApp> =
    apps
        .filter { it.packageName != ownPackage }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
