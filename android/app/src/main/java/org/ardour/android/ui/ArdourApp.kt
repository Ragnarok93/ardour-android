package org.ardour.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ardour.android.engine.ArdourNative
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

private data class AudioProbeUiState(
    val running: Boolean,
    val status: String,
)

@Composable
fun ArdourApp(nativeStatus: String) {
    val layoutMode = rememberArdourLayoutMode()
    var audioProbe by remember {
        mutableStateOf(
            AudioProbeUiState(
                running = false,
                status = runCatching { ArdourNative.audioProbeState() }
                    .getOrDefault("audio probe unavailable"),
            )
        )
    }

    fun toggleAudioProbe() {
        if (audioProbe.running) {
            runCatching { ArdourNative.stopAudioProbe() }
            audioProbe = AudioProbeUiState(
                running = false,
                status = runCatching { ArdourNative.audioProbeState() }
                    .getOrDefault("audio probe stopped"),
            )
        } else {
            val started = runCatching { ArdourNative.startAudioProbe() }
                .getOrDefault(false)
            audioProbe = AudioProbeUiState(
                running = started,
                status = runCatching { ArdourNative.audioProbeState() }
                    .getOrDefault(if (started) "audio running" else "audio start failed"),
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ArdourNative.stopAudioProbe() }
        }
    }

    ArdourTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = OneUiTheme.colors.background,
        ) {
            when (layoutMode) {
                ArdourLayoutMode.COMPACT -> CompactWorkspace(
                    nativeStatus = nativeStatus,
                    audioProbe = audioProbe,
                    onAudioProbeToggle = ::toggleAudioProbe,
                )
                ArdourLayoutMode.DESKTOP -> DesktopWorkspace(
                    nativeStatus = nativeStatus,
                    audioProbe = audioProbe,
                    onAudioProbeToggle = ::toggleAudioProbe,
                )
            }
        }
    }
}

@Composable
private fun CompactWorkspace(
    nativeStatus: String,
    audioProbe: AudioProbeUiState,
    onAudioProbeToggle: () -> Unit,
) {
    var workspace by remember { mutableStateOf(Workspace.Arrange) }

    Column(Modifier.fillMaxSize()) {
        CompactTransportBar(
            nativeStatus = nativeStatus,
            audioProbe = audioProbe,
            onAudioProbeToggle = onAudioProbeToggle,
        )
        WorkspaceContent(
            workspace = workspace,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            compact = true,
        )
        WorkspaceBar(
            selected = workspace,
            compact = true,
            onSelected = { workspace = it },
        )
    }
}

@Composable
private fun DesktopWorkspace(
    nativeStatus: String,
    audioProbe: AudioProbeUiState,
    onAudioProbeToggle: () -> Unit,
) {
    var workspace by remember { mutableStateOf(Workspace.Arrange) }
    val colors = OneUiTheme.colors

    Column(Modifier.fillMaxSize()) {
        DesktopTransportBar(
            nativeStatus = nativeStatus,
            audioProbe = audioProbe,
            onAudioProbeToggle = onAudioProbeToggle,
        )

        HorizontalDivider(color = colors.divider)

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val showBrowser = maxWidth >= 1180.dp
            val inspectorWidth = if (maxWidth >= 1080.dp) 244.dp else 216.dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
            ) {
                InspectorPane(
                    modifier = Modifier
                        .width(inspectorWidth)
                        .fillMaxHeight(),
                    desktop = true,
                )

                Spacer(Modifier.width(10.dp))

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = OneUiTheme.shapes.card,
                    color = colors.background,
                ) {
                    WorkspaceContent(
                        workspace = workspace,
                        modifier = Modifier.fillMaxSize(),
                        compact = false,
                    )
                }

                if (showBrowser) {
                    Spacer(Modifier.width(10.dp))
                    BrowserPane(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight(),
                        desktop = true,
                    )
                }
            }
        }

        WorkspaceBar(
            selected = workspace,
            compact = false,
            onSelected = { workspace = it },
        )
    }
}

@Composable
private fun WorkspaceContent(
    workspace: Workspace,
    modifier: Modifier,
    compact: Boolean,
) {
    when (workspace) {
        Workspace.Arrange -> ArrangePane(modifier, compact)
        Workspace.Mixer -> MixerPane(modifier, compact)
        Workspace.Edit -> CenterWorkspace(
            modifier = modifier,
            title = "Editor",
            detail = "Piano roll, audio detail, automation and tempo editing land here.",
            compact = compact,
        )
        Workspace.Plugins -> CenterWorkspace(
            modifier = modifier,
            title = "Plugins",
            detail = "Native LV2/VST3/AAP and compatibility-hosted editors share this workspace.",
            compact = compact,
        )
    }
}

@Composable
private fun CompactTransportBar(
    nativeStatus: String,
    audioProbe: AudioProbeUiState,
    onAudioProbeToggle: () -> Unit,
) {
    val colors = OneUiTheme.colors

    Surface(color = colors.surfaceElevated) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            val showReturnToStart = maxWidth >= 390.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showReturnToStart) {
                    OneUiIconButton(
                        icon = OneUiIcons.ChevronLeft,
                        contentDescription = "Return to start",
                        onClick = {},
                    )
                }

                OneUiIconButton(
                    icon = OneUiIcons.Pause,
                    contentDescription = "Pause",
                    onClick = {},
                )

                RecordButton()

                OneUiIconButton(
                    icon = OneUiIcons.Play,
                    contentDescription = "Play",
                    onClick = {},
                )

                Text(
                    text = "01:01:000",
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    color = colors.primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )

                OneUiIconButton(
                    icon = OneUiIcons.Motion,
                    contentDescription = if (audioProbe.running) {
                        "Stop audio probe"
                    } else {
                        "Start audio probe"
                    },
                    onClick = onAudioProbeToggle,
                    tint = if (audioProbe.running) colors.accent else colors.secondaryText,
                )
            }
        }
    }
}

@Composable
private fun DesktopTransportBar(
    nativeStatus: String,
    audioProbe: AudioProbeUiState,
    onAudioProbeToggle: () -> Unit,
) {
    val colors = OneUiTheme.colors

    Surface(color = colors.surfaceElevated) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            val narrowDesktop = maxWidth < 1020.dp
            val leftWidth = if (narrowDesktop) 170.dp else 240.dp
            val rightWidth = if (narrowDesktop) 220.dp else 300.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.width(leftWidth),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Ardour",
                        color = colors.primaryText,
                        style = OneUiTheme.typography.title,
                        maxLines = 1,
                    )
                    Text(
                        text = if (audioProbe.running) audioProbe.status else nativeStatus,
                        color = colors.secondaryText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OneUiIconButton(
                        icon = OneUiIcons.ChevronLeft,
                        contentDescription = "Return to start",
                        onClick = {},
                    )
                    OneUiIconButton(
                        icon = OneUiIcons.Pause,
                        contentDescription = "Pause",
                        onClick = {},
                    )
                    RecordButton()
                    OneUiIconButton(
                        icon = OneUiIcons.Play,
                        contentDescription = "Play",
                        onClick = {},
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "01:01:000",
                        modifier = Modifier.widthIn(min = 104.dp),
                        color = colors.primaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                }

                Row(
                    modifier = Modifier.width(rightWidth),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (narrowDesktop) {
                        OneUiIconButton(
                            icon = OneUiIcons.Motion,
                            contentDescription = if (audioProbe.running) {
                                "Stop audio probe"
                            } else {
                                "Start audio probe"
                            },
                            onClick = onAudioProbeToggle,
                            tint = if (audioProbe.running) colors.accent else colors.secondaryText,
                        )
                    } else {
                        OneUiButton(
                            onClick = onAudioProbeToggle,
                            colors = OneUiButtonDefaults.toggleColors(audioProbe.running),
                            shape = OneUiTheme.shapes.control,
                            minHeight = 40.dp,
                            minWidth = 92.dp,
                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = 8.dp,
                            ),
                        ) {
                            Text(
                                text = if (audioProbe.running) "Audio on" else "Audio probe",
                                fontSize = 11.sp,
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))
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
        contentPadding = PaddingValues(0.dp),
        colors = OneUiButtonDefaults.neutralColors(
            containerColor = Color.Transparent,
            contentColor = colors.destructive,
        ),
        shape = OneUiTheme.shapes.iconButton,
    ) {
        Text("●", fontSize = 18.sp)
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
            maxLines = 1,
        )
    }
}

@Composable
private fun InspectorPane(
    modifier: Modifier,
    desktop: Boolean,
) {
    val colors = OneUiTheme.colors

    Surface(
        modifier = modifier,
        color = colors.surfaceElevated,
        shape = if (desktop) OneUiTheme.shapes.card else androidx.compose.ui.graphics.RectangleShape,
    ) {
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
private fun ArrangePane(
    modifier: Modifier,
    compact: Boolean,
) {
    val colors = OneUiTheme.colors

    Surface(modifier = modifier, color = colors.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (compact) 16.dp else OneUiTheme.spacing.screenHorizontal,
                        vertical = if (compact) 8.dp else 10.dp,
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
                    TrackLane(track = track, compact = compact)
                }
            }
        }
    }
}

@Composable
private fun TrackLane(
    track: TrackUiState,
    compact: Boolean,
) {
    val colors = OneUiTheme.colors
    val laneHeight = if (compact) 100.dp else 92.dp
    val metadataWidth = if (compact) 142.dp else 168.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(laneHeight)
            .border(width = 0.5.dp, color = colors.divider)
    ) {
        Column(
            modifier = Modifier
                .width(metadataWidth)
                .fillMaxHeight()
                .background(colors.surfaceElevated)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = track.name,
                color = colors.primaryText,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = track.detail,
                color = colors.secondaryText,
                fontSize = 11.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("M", color = colors.secondaryText, fontSize = 10.sp, maxLines = 1)
                Text("S", color = colors.secondaryText, fontSize = 10.sp, maxLines = 1)
                Text("R", color = colors.secondaryText, fontSize = 10.sp, maxLines = 1)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 7.dp)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BrowserPane(
    modifier: Modifier,
    desktop: Boolean,
) {
    val colors = OneUiTheme.colors

    Surface(
        modifier = modifier,
        color = colors.surfaceElevated,
        shape = if (desktop) OneUiTheme.shapes.card else androidx.compose.ui.graphics.RectangleShape,
    ) {
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
private fun MixerPane(
    modifier: Modifier,
    compact: Boolean,
) {
    val colors = OneUiTheme.colors
    val channels = remember {
        previewTracks.map { it.name } + "Master"
    }

    Surface(modifier = modifier, color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) 14.dp else 18.dp,
                    end = if (compact) 14.dp else 18.dp,
                    top = if (compact) 12.dp else 16.dp,
                    bottom = 10.dp,
                )
        ) {
            PaneHeader("Mixer")
            Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp),
            ) {
                items(channels) { name ->
                    ChannelStrip(
                        name = name,
                        modifier = Modifier
                            .width(if (compact) 118.dp else 148.dp)
                            .fillMaxHeight(),
                        compact = compact,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelStrip(
    name: String,
    modifier: Modifier,
    compact: Boolean,
) {
    val colors = OneUiTheme.colors

    OneUiSurfaceBox(
        modifier = modifier,
        containerColor = colors.surfaceElevated,
        shape = OneUiTheme.shapes.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = name,
                modifier = Modifier.fillMaxWidth(),
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))
            InsertSlot("EQ", minHeight = 38)
            Spacer(Modifier.height(6.dp))
            InsertSlot("Insert", minHeight = 38)
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(12.dp)
                        .fillMaxHeight(if (compact) 0.82f else 0.76f)
                        .heightIn(min = 72.dp, max = 220.dp)
                        .background(
                            color = colors.surface,
                            shape = OneUiTheme.shapes.control,
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "0.0",
                color = colors.secondaryText,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CenterWorkspace(
    modifier: Modifier,
    title: String,
    detail: String,
    compact: Boolean,
) {
    val colors = OneUiTheme.colors

    Surface(modifier = modifier, color = colors.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 18.dp else 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            OneUiSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                containerColor = colors.surfaceElevated,
            ) {
                Text(
                    text = title,
                    color = colors.primaryText,
                    fontSize = if (compact) 20.sp else 22.sp,
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
private fun WorkspaceBar(
    selected: Workspace,
    compact: Boolean,
    onSelected: (Workspace) -> Unit,
) {
    val colors = OneUiTheme.colors

    Surface(color = colors.navigationBackground) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(if (compact) 68.dp else 58.dp)
                .padding(
                    horizontal = if (compact) 8.dp else 16.dp,
                    vertical = if (compact) 8.dp else 6.dp,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Workspace.entries.forEachIndexed { index, workspace ->
                val active = workspace == selected

                OneUiButton(
                    onClick = { onSelected(workspace) },
                    modifier = if (compact) {
                        Modifier.weight(1f)
                    } else {
                        Modifier.width(92.dp)
                    },
                    minHeight = if (compact) 46.dp else 42.dp,
                    minWidth = 0.dp,
                    colors = OneUiButtonDefaults.toggleColors(active),
                    shape = OneUiTheme.shapes.control,
                    contentPadding = PaddingValues(
                        horizontal = if (compact) 8.dp else 12.dp,
                        vertical = 8.dp,
                    ),
                ) {
                    Text(
                        text = workspace.label,
                        maxLines = 1,
                        softWrap = false,
                        style = if (active) {
                            OneUiTheme.typography.navigationLabelSelected
                        } else {
                            OneUiTheme.typography.navigationLabel
                        },
                    )
                }

                if (index != Workspace.entries.lastIndex) {
                    Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
                }
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
        maxLines = 1,
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
            maxLines = 1,
        )
        Text(
            text = value,
            color = colors.primaryText,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun InsertSlot(
    label: String,
    minHeight: Int = 40,
) {
    val colors = OneUiTheme.colors

    OneUiButton(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        colors = OneUiButtonDefaults.neutralColors(
            containerColor = colors.background,
            contentColor = colors.primaryText,
        ),
        shape = OneUiTheme.shapes.control,
        minHeight = minHeight.dp,
        minWidth = 0.dp,
        contentPadding = PaddingValues(
            horizontal = 10.dp,
            vertical = 6.dp,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
