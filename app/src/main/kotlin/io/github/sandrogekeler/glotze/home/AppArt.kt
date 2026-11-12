package io.github.sandrogekeler.glotze.home

import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/** What a card shows for an app: its TV banner, or its launcher icon when it has none. */
data class AppArt(
    val image: ImageBitmap,
    val isBanner: Boolean,
)

/** Where cards get their art. An interface so previews and tests need no PackageManager. */
interface ArtSource {
    /** Art already decoded, so a card coming back on screen draws it in its first frame. */
    fun cached(app: TvApp): AppArt?

    /** Decodes art no larger than [maxWidthPx] by [maxHeightPx]; null when the app has none or is gone. */
    suspend fun load(
        app: TvApp,
        maxWidthPx: Int,
        maxHeightPx: Int,
    ): AppArt?
}

object NoArt : ArtSource {
    override fun cached(app: TvApp): AppArt? = null

    override suspend fun load(
        app: TvApp,
        maxWidthPx: Int,
        maxHeightPx: Int,
    ): AppArt? = null
}

/**
 * Loads banners off the main thread, only for cards being drawn, at the card's pixel size.
 *
 * Icons are often 432 px adaptive icons and banners can be larger than the card, so decoding
 * at full size would cost both time before the first frame and memory on a 2 GB TV.
 */
class AppArtLoader(
    private val packageManager: PackageManager,
    maxCacheBytes: Int = (Runtime.getRuntime().maxMemory() / 8).toInt(),
) : ArtSource {
    private val cache =
        object : LruCache<String, AppArt>(maxCacheBytes) {
            override fun sizeOf(
                key: String,
                value: AppArt,
            ): Int = value.image.width * value.image.height * 4
        }

    override fun cached(app: TvApp): AppArt? = cache.get(key(app))

    override suspend fun load(
        app: TvApp,
        maxWidthPx: Int,
        maxHeightPx: Int,
    ): AppArt? =
        cached(app) ?: withContext(Dispatchers.IO) {
            try {
                val info = packageManager.getActivityInfo(ComponentName(app.packageName, app.activityName), 0)
                val banner = info.loadBanner(packageManager)
                val drawable = banner ?: info.loadIcon(packageManager)
                val (width, height) = fitWithin(drawable.intrinsicWidth, drawable.intrinsicHeight, maxWidthPx, maxHeightPx)
                AppArt(drawable.toBitmap(width, height).asImageBitmap(), isBanner = banner != null)
                    .also { cache.put(key(app), it) }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.i(TAG, "${app.packageName} is gone; the list reloads on its broadcast", e)
                null
            }
        }

    private fun key(app: TvApp) = app.packageName + "/" + app.activityName

    private companion object {
        const val TAG = "AppArtLoader"
    }
}

/**
 * The pixel size to decode art at: scaled down to fit in [maxWidth] by [maxHeight] with its
 * aspect ratio kept, never scaled up. Drawables without an intrinsic size (colours, some
 * vectors) take the whole box.
 */
fun fitWithin(
    sourceWidth: Int,
    sourceHeight: Int,
    maxWidth: Int,
    maxHeight: Int,
): Pair<Int, Int> {
    val boxWidth = maxWidth.coerceAtLeast(1)
    val boxHeight = maxHeight.coerceAtLeast(1)
    if (sourceWidth <= 0 || sourceHeight <= 0) return boxWidth to boxHeight
    val scale = min(1f, min(boxWidth.toFloat() / sourceWidth, boxHeight.toFloat() / sourceHeight))
    return (sourceWidth * scale).roundToInt().coerceAtLeast(1) to (sourceHeight * scale).roundToInt().coerceAtLeast(1)
}
