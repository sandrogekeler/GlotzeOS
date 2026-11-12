package io.github.sandrogekeler.glotze.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
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

    // Tests start in touch mode, where Compose does not focus clickable items. A TV has no
    // touchscreen and is always in key mode, so the tests run in key mode too.
    private val keyMode =
        object : InputModeManager {
            override val inputMode = InputMode.Keyboard

            override fun requestInputMode(inputMode: InputMode) = inputMode == InputMode.Keyboard
        }

    private fun show(onOpen: (TvApp) -> Unit = {}) {
        compose.setContent { OnTv { HomeScreen(apps = apps, onOpen = onOpen) } }
    }

    @Composable
    private fun OnTv(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalInputModeManager provides keyMode, content = content)
    }

    @Test
    fun firstAppHasFocusWhenTheListArrives() {
        show()
        compose.onNodeWithTag("alpha").assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dpadRightMovesToTheNextApp() {
        show()
        compose.onNodeWithTag("alpha").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("bravo").assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun centreOpensTheFocusedApp() {
        var opened: TvApp? = null
        show(onOpen = { opened = it })
        compose.onNodeWithTag("alpha").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals("alpha", opened?.packageName) }
    }
}
