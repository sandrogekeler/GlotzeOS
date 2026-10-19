package io.github.sandrogekeler.glotze.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeOrderTest {
    private fun app(
        packageName: String,
        label: String = packageName,
    ) = TvApp(packageName, label, banner = null)

    @Test
    fun leavesOutGlotzeItself() {
        val apps = listOf(app("com.netflix"), app("own.package"))
        assertEquals(listOf("com.netflix"), homeOrder(apps, "own.package").map { it.packageName })
    }

    @Test
    fun sortsByLabelIgnoringCase() {
        val apps = listOf(app("a", "youTube"), app("b", "Netflix"), app("c", "plex"))
        assertEquals(listOf("Netflix", "plex", "youTube"), homeOrder(apps, "own").map { it.label })
    }

    @Test
    fun listsAnAppOnceWhenItHasTwoTvActivities() {
        val apps = listOf(app("com.plex", "Plex"), app("com.plex", "Plex Live"))
        assertEquals(1, homeOrder(apps, "own").size)
    }
}
