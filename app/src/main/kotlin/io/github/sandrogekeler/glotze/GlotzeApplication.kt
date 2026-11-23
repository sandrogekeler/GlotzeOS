package io.github.sandrogekeler.glotze

import android.app.Application
import io.github.sandrogekeler.glotze.home.AppArtLoader
import io.github.sandrogekeler.glotze.home.AppCatalog
import kotlinx.coroutines.MainScope

/**
 * Holds what the home screen needs for as long as the process lives, not as long as one
 * activity: when the system recreates [MainActivity], the app list and the decoded banners are
 * still there and the grid draws them in its first frame instead of loading again.
 *
 * Both are created on first use, from [MainActivity.onCreate], so nothing here runs before
 * the activity's StrictMode policy is set.
 */
class GlotzeApplication : Application() {
    private val scope = MainScope()

    /** Started on first use and kept current by package broadcasts until the process ends. */
    val catalog: AppCatalog by lazy { AppCatalog(this, scope).also { it.start() } }

    val art: AppArtLoader by lazy { AppArtLoader(packageManager) }
}
