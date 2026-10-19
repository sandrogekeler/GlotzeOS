package io.github.sandrogekeler.glotze.home

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class TvApp(
    val packageName: String,
    val label: String,
    val banner: ImageBitmap?,
)

/** Every app the TV would show in its own app list, with its banner, or its icon when it has none. */
fun loadTvApps(packageManager: PackageManager): List<TvApp> {
    val tvLaunchable = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    return packageManager.queryIntentActivities(tvLaunchable, 0).map { info ->
        val activity = info.activityInfo
        val art = activity.loadBanner(packageManager) ?: activity.loadIcon(packageManager)
        TvApp(
            packageName = activity.packageName,
            label = info.loadLabel(packageManager).toString(),
            banner = art?.toBitmap()?.asImageBitmap(),
        )
    }
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
