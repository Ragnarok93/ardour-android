package org.ardour.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Presentation-level window class shared with the Flux Explorer behavior model.
 *
 * Classification is based on the current window, not device identity or orientation:
 * - phones remain compact in landscape when vertical room is constrained;
 * - tablet, DeX and freeform windows become desktop-like once both dimensions can
 *   sustain persistent panes and desktop-density controls.
 */
enum class ArdourLayoutMode {
    COMPACT,
    DESKTOP,
}

internal fun ardourLayoutModeFor(widthDp: Int, heightDp: Int): ArdourLayoutMode {
    val desktop =
        (widthDp >= 840 && heightDp >= 480) ||
            (widthDp >= 720 && heightDp >= 600)

    return if (desktop) ArdourLayoutMode.DESKTOP else ArdourLayoutMode.COMPACT
}

@Composable
fun rememberArdourLayoutMode(): ArdourLayoutMode {
    val configuration = LocalConfiguration.current
    return ardourLayoutModeFor(
        widthDp = configuration.screenWidthDp,
        heightDp = configuration.screenHeightDp,
    )
}
