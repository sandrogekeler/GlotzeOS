package io.github.sandrogekeler.glotze.home

import org.junit.Assert.assertEquals
import org.junit.Test

class FitWithinTest {
    @Test
    fun scalesABannerDownToTheCard() {
        assertEquals(400 to 225, fitWithin(640, 360, 400, 225))
    }

    @Test
    fun keepsASquareIconSquareInsideAWideCard() {
        assertEquals(225 to 225, fitWithin(432, 432, 400, 225))
    }

    @Test
    fun neverScalesUp() {
        assertEquals(48 to 48, fitWithin(48, 48, 400, 225))
    }

    @Test
    fun takesTheWholeBoxWhenTheDrawableHasNoSize() {
        assertEquals(400 to 225, fitWithin(-1, -1, 400, 225))
    }

    @Test
    fun neverReturnsZero() {
        assertEquals(1 to 1, fitWithin(10_000, 1, 1, 1))
    }
}
