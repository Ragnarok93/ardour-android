package org.ardour.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.oneui.compose.components.buttons.OneUiButton
import org.oneui.compose.components.buttons.OneUiButtonDefaults
import org.oneui.compose.components.surface.OneUiSurface
import org.oneui.compose.components.surface.OneUiSurfaceBox
import org.oneui.compose.icons.OneUiIconButton
import org.oneui.compose.icons.OneUiIcons
import org.oneui.compose.oneui8.components.OneUI8ListItem
import org.oneui.compose.theme.OneUiTheme

private enum class Workspace(val label: String) {
    Arrange("Arrange"),
    Mixer("Mixer"),
    Edit("Edit"),
    Plugins("Plugins"),
}

private data class TrackUiState(
    val name: String,
    val detail: String,
    val regionWidth: Float,
    val secondary: Boolean = false,
)

private val previewTracks = listOf(
    TrackUiState("Lead Vocal", "Audio 1", .72f),
    TrackUiState("Guitar", "Audio 2", .54f, secondary = true),
    TrackUiState("Bass", "Audio 3", .83f),
    TrackUiState("Drums", "MIDI 1", .91f, secondary = true),
)

@Composable
fun ArdourApp(nativeStatus: String) {
    ArdourTheme {
        val colors = OneUiTheme.colors

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                when {
                    maxWidth >= 1080.dp -> DesktopWorkspace(nativeStatus)
                    maxWidth >= 720.dp -> TabletWorkspace(nativeStatus)
                    else -> PhoneWorkspace(nativeStatus)
                }
            }
        }
    }
}

@Composable
private fun DesktopWorkspace(nativeStatus: String) {
    var workspace by remember { mutableStateOf(Workspace.Arrange) }
    val colors = OneUiTheme.colors

    Column(Modifier.fillMaxSize()) {
        TransportBar(nativeStatus)
        HorizontalDivider(color = colors.divider)

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            InspectorPane(Modifier.width(244.dp).fillMaxHeight())
            WorkspaceContent(workspace, Modifier.weight(1f).fillMaxHeight())
            BrowserPane(Modifier.width(272.dp).fillMaxHeight())
        }

        WorkspaceBar(workspace) { workspace = it }
    }
}

@Composable
private fun TabletWorkspace(nativeStatus: String) {
    var workspace by remember { mutableStateOf(Workspace.Arrange) }

    Column(Modifier.fillMaxSize()) {
        TransportBar(nativeStatus)

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            InspectorPane(Modifier.width(216.dp).fillMaxHeight())
            WorkspaceContent(workspace, Modifier.weight(1f).fillMaxHeight())
        }

        WorkspaceBar(workspace) { workspace = it }
    }
}

@Composable
private fun PhoneWorkspace(nativeStatus: String) {
    var workspace by remember { mutableStateOf(Workspace.Arrange) }

    Column(Modifier.fillMaxSize()) {
        TransportBar(nativeStatus, compact = true)
        WorkspaceContent(workspace, Modifier.weight(1f).fillMaxWidth())
        WorkspaceBar(workspace) { workspace = it }
    }
}

@Composable
private fun WorkspaceContent(workspace: Workspace, modifier: Modifier) {
    when (workspace) {
        Workspace.Arrange -> ArrangePane(modifier)
        Workspace.Mixer -> MixerPane(modifier)
        Workspace.Edit -> CenterWorkspace(
            modifier = modifier,
            title = "Editor",
            detail = "Piano roll, audio detail, automation and tempo editing land here.",
        )
        Workspace.Plugins -> CenterWorkspace(
            modifier = modifier,
            title = "Plugins",
            detail = "Native LV2/VST3/AAP and compatibility-hosted editors share this workspace.",
        )
    }
}

@Composable
private fun TransportBar(nativeStatus: String, compact: Boolean = false) {
    val colors = OneUiTheme.colors

    Surface(color = colors.surfaceElevated) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 64.dp else 72.dp)
                .padding(horizontal = if (compact) 10.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
                Column(Modifier.width(230.dp)) {
                    Text(
                        text = "Ardour",
                        color = colors.primaryText,
                        style = OneUiTheme.typography.title,
                    )
                    Text(
                        text = nativeStatus,
                        color = colors.secondaryText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OneUiIconButton(
                    icon = OneUiIcons.Back,
                    contentDescription = "Return to start",
                    onClick = {},
                )
                OneUiIconButton(
                    icon = OneUiIcons.Pause,
                    contentDescription = "Stop",
                    onClick = {},
                )
                RecordButton()
                OneUiIconButton(
                    icon = OneUiIcons.Play,
                    contentDescription = "Play",
                    onClick = {},
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "01:01:000",
                    color = colors.primaryText,
                    fontSize = if (compact) 18.sp else 22.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (!compact) {
                Row(
                    modifier = Modifier.width(230.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    CompactBadge("120 BPM")
                    Spacer(Modifier.width(8.dp))
                    CompactBadge("4/4")
                }
            }
        }
    }
}

@Composable
private fun RecordButton() {
    val colors = OneUiTheme.colors

    OneUiButton(
        onClick = {},
        modifier = Modifier.size(48.dp),
        minWidth = 48.dp,
        minHeight = 48.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = OneUiButtonDefaults.tonalColors(
            containerColor = colors.destructive.copy(alpha = 0.14f),
            contentColor = colors.destructive,
        ),
        shape = OneUiTheme.shapes.iconButton,
    ) {
        Text("●", fontSize = 17.sp)
    }
}

@Composable
private fun CompactBadge(label: String) {
    val colors = OneUiTheme.colors

    OneUiSurfaceBox(
        containerColor = colors.surface,
        shape = OneUiTheme.shapes.control,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = colors.secondaryText,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun InspectorPane(modifier: Modifier) {
    val colors = OneUiTheme.colors

    Surface(modifier = modifier, color = colors.surfaceElevated) {
        Column(Modifier.padding(14.dp)) {
            PaneHeader("Inspector")
            Spacer(Modifier.height(12.dp))

            OneUiSurface(
                containerColor = colors.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LabeledValue("Track", "Lead Vocal")
                LabeledValue("Input", "Input 1")
                LabeledValue("Output", "Master")
                LabeledValue("Gain", "0.0 dB")
                LabeledValue("Pan", "Center")
            }

            Spacer(Modifier.height(18.dp))
            PaneHeader("Inserts")
            Spacer(Modifier.height(8.dp))
            InsertSlot("Compressor")
            Spacer(Modifier.height(6.dp))
            InsertSlot("EQ")
            Spacer(Modifier.height(6.dp))
            InsertSlot("+ Add plugin")
        }
    }
}

@Composable
private fun ArrangePane(modifier: Modifier) {
    val colors = OneUiTheme.colors

    Surface(modifier = modifier, color = colors.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = OneUiTheme.spacing.screenHorizontal,
                        vertical = 12.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PaneHeader("Tracks", Modifier.weight(1f))
                OneUiIconButton(
                    icon = OneUiIcons.Add,
                    contentDescription = "Add track",
                    onClick = {},
                )
            }

            HorizontalDivider(color = colors.divider)

            LazyColumn(Modifier.fillMaxSize()) {
                items(previewTracks) { track ->
                    TrackLane(track)
                }
            }
        }
    }
}

@Composable
private fun TrackLane(track: TrackUiState) {
    val colors = OneUiTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .border(width = 0.5.dp, color = colors.divider)
    ) {
        Column(
            modifier = Modifier
                .width(154.dp)
                .fillMaxHeight()
                .background(colors.surfaceElevated)
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = track.name,
                color = colors.primaryText,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
            Text(
                text = track.detail,
                color = colors.secondaryText,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "M   S   R",
                color = colors.secondaryText,
                fontSize = 10.sp,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            val regionColor = if (track.secondary) {
                colors.functionalPositive.copy(alpha = 0.70f)
            } else {
                colors.accentStrong.copy(alpha = 0.72f)
            }

            OneUiSurfaceBox(
                modifier = Modifier
                    .fillMaxWidth(track.regionWidth)
                    .fillMaxHeight(),
                containerColor = regionColor,
                shape = OneUiTheme.shapes.control,
            ) {
                Text(
                    text = track.name,
                    modifier = Modifier.padding(10.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun BrowserPane(modifier: Modifier) {
    val colors = OneUiTheme.colors

    Surface(modifier = modifier, color = colors.surfaceElevated) {
        Column(Modifier.padding(14.dp)) {
            PaneHeader("Browser")
            Spacer(Modifier.height(12.dp))

            OneUI8ListItem(
                title = "Instruments",
                subtitle = "Native and external instruments",
                onClick = {},
            )
            Spacer(Modifier.height(6.dp))
            OneUI8ListItem(
                title = "Audio FX",
                subtitle = "Dynamics, EQ and spatial",
                onClick = {},
            )
            Spacer(Modifier.height(6.dp))
            OneUI8ListItem(
                title = "MIDI FX",
                subtitle = "Processors and generators",
                onClick = {},
            )
            Spacer(Modifier.height(6.dp))
            OneUI8ListItem(
                title = "Loops",
                subtitle = "Project and user content",
                onClick = {},
            )
            Spacer(Modifier.height(6.dp))
            OneUI8ListItem(
                title = "Files",
                subtitle = "Device and document storage",
                onClick = {},
            )
        }
    }
}

@Composable
private fun MixerPane(modifier: Modifier) {
    val colors = OneUiTheme.colors

    Surface(modifier = modifier, color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            PaneHeader("Mixer")
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                previewTracks.forEach { track ->
                    ChannelStrip(track.name, Modifier.weight(1f))
                }
                ChannelStrip("Master", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChannelStrip(name: String, modifier: Modifier) {
    val colors = OneUiTheme.colors

    OneUiSurface(
        modifier = modifier.fillMaxHeight(),
        containerColor = colors.surfaceElevated,
    ) {
        Text(
            text = name,
            color = colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(14.dp))
        InsertSlot("EQ")
        Spacer(Modifier.height(6.dp))
        InsertSlot("Insert")
        Spacer(Modifier.weight(1f))

        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(12.dp)
                .height(180.dp)
                .background(
                    color = colors.surface,
                    shape = OneUiTheme.shapes.control,
                )
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "0.0",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = colors.secondaryText,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun CenterWorkspace(modifier: Modifier, title: String, detail: String) {
    val colors = OneUiTheme.colors

    Surface(modifier = modifier, color = colors.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            OneUiSurface(
                modifier = Modifier.width(420.dp),
                containerColor = colors.surfaceElevated,
            ) {
                Text(
                    text = title,
                    color = colors.primaryText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = detail,
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun WorkspaceBar(selected: Workspace, onSelected: (Workspace) -> Unit) {
    val colors = OneUiTheme.colors

    Surface(color = colors.navigationBackground) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Workspace.entries.forEach { workspace ->
                val active = workspace == selected

                OneUiButton(
                    onClick = { onSelected(workspace) },
                    minHeight = 42.dp,
                    minWidth = 76.dp,
                    colors = OneUiButtonDefaults.toggleColors(active),
                    shape = OneUiTheme.shapes.control,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp,
                    ),
                ) {
                    Text(
                        text = workspace.label,
                        style = if (active) {
                            OneUiTheme.typography.navigationLabelSelected
                        } else {
                            OneUiTheme.typography.navigationLabel
                        },
                    )
                }

                Spacer(Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun PaneHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier,
        color = OneUiTheme.colors.primaryText,
        style = OneUiTheme.typography.sectionLabel,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun LabeledValue(label: String, value: String) {
    val colors = OneUiTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = colors.secondaryText,
            fontSize = 11.sp,
        )
        Text(
            text = value,
            color = colors.primaryText,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun InsertSlot(label: String) {
    val colors = OneUiTheme.colors

    OneUiButton(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        colors = OneUiButtonDefaults.neutralColors(
            containerColor = colors.background,
            contentColor = colors.primaryText,
        ),
        shape = OneUiTheme.shapes.control,
        minHeight = 40.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 11.sp,
        )
    }
}
