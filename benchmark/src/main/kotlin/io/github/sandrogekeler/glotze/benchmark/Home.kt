package io.github.sandrogekeler.glotze.benchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope

const val PACKAGE = "io.github.sandrogekeler.glotze"

/**
 * Starts GlotzeOS by its component and waits for the app list (it reports fully drawn then).
 *
 * Explicit rather than through Home: if GlotzeOS is the TV's home app, pressing Home would
 * start it outside the measurement. GlotzeOS has no phone launcher entry either, so the
 * default launch intent would not find it.
 */
fun MacrobenchmarkScope.startHome() {
    startActivityAndWait(
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setComponent(ComponentName(PACKAGE, "$PACKAGE.MainActivity")),
    )
}
