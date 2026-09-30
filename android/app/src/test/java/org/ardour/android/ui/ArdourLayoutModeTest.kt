package org.ardour.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ArdourLayoutModeTest {
    @Test
    fun portraitPhoneRemainsCompact() {
        assertEquals(
            ArdourLayoutMode.COMPACT,
            ardourLayoutModeFor(widthDp = 412, heightDp = 915),
        )
    }

    @Test
    fun landscapePhoneRemainsCompactWhenHeightIsConstrained() {
        assertEquals(
            ArdourLayoutMode.COMPACT,
            ardourLayoutModeFor(widthDp = 915, heightDp = 430),
        )
    }

    @Test
    fun tabletSizedWindowUsesDesktopMode() {
        assertEquals(
            ArdourLayoutMode.DESKTOP,
            ardourLayoutModeFor(widthDp = 720, heightDp = 600),
        )
    }

    @Test
    fun dexStyleWideWindowUsesDesktopMode() {
        assertEquals(
            ArdourLayoutMode.DESKTOP,
            ardourLayoutModeFor(widthDp = 1080, heightDp = 600),
        )
    }

    @Test
    fun wideButShortFreeformWindowStaysCompact() {
        assertEquals(
            ArdourLayoutMode.COMPACT,
            ardourLayoutModeFor(widthDp = 839, heightDp = 479),
        )
    }
}
