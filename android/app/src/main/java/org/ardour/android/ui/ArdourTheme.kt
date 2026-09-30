package org.ardour.android.ui

import androidx.compose.runtime.Composable
import org.oneui.compose.theme.OneUiTheme

/**
 * Ardour owns DAW layout and state, while Ragnarok93/oneui-compose owns the
 * application design system, semantic palette, shape, motion and interaction tokens.
 */
@Composable
fun ArdourTheme(content: @Composable () -> Unit) {
    OneUiTheme(
        dynamicColors = false,
        content = content,
    )
}
