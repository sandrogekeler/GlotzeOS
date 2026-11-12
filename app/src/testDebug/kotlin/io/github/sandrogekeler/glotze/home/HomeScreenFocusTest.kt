package io.github.sandrogekeler.glotze.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Remote-only use: focus starts somewhere and the D-pad moves it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class HomeScreenFocusTest {
    @get:Rule
    val compose = createComposeRule()

    private val apps = listOf("alpha", "bravo", "charlie", "delta").map { TvApp(it, it, "$it.Main") }

    @Test
    fun firstAppHasFocusWhenTheListArrives() {
        compose.setContent { HomeScreen(apps = apps, onOpen = {}) }
        compose.onNodeWithTag("alpha").assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dpadRightMovesToTheNextApp() {
        compose.setContent { HomeScreen(apps = apps, onOpen = {}) }
        compose.onNodeWithTag("alpha").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("bravo").assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun centreOpensTheFocusedApp() {
        var opened: TvApp? = null
        compose.setContent { HomeScreen(apps = apps, onOpen = { opened = it }) }
        compose.onNodeWithTag("alpha").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { org.junit.Assert.assertEquals("alpha", opened?.packageName) }
    }
}
