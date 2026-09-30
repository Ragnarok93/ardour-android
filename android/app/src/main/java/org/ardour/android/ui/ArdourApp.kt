package org.ardour.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
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

    Column(Modifier.fillMaxSize()) {
        TransportBar(nativeStatus)
        HorizontalDivider(color = ArdourPalette.Outline)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            InspectorPane(Modifier.width(244.dp).fillMaxHeight())
            WorkspaceContent(workspace, Modifier.weight(1f).fillMaxHeight())
            BrowserPane(Modifier.width(260.dp).fillMaxHeight())
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
            modifier,
            "Editor",
            "Piano roll, audio detail, automation and tempo editing land here.",
        )
        Workspace.Plugins -> CenterWorkspace(
            modifier,
            "Plugins",
            "Native LV2/VST3/AAP and compatibility-hosted editors share this workspace.",
        )
    }
}

@Composable
private fun TransportBar(nativeStatus: String, compact: Boolean = false) {
    Surface(color = ArdourPalette.Surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 64.dp else 72.dp)
                .padding(horizontal = if (compact) 10.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
                Column(Modifier.width(210.dp)) {
                    Text(
                        text = "Ardour",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = nativeStatus,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                TransportButton("◀")
                TransportButton("■")
                TransportButton("●", ArdourPalette.Record)
                TransportButton("▶")
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "01:01:000",
                    fontSize = if (compact) 18.sp else 22.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (!compact) {
                Row(
                    modifier = Modifier.width(210.dp),
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
private fun TransportButton(label: String, tint: Color = MaterialTheme.colorScheme.onSurface) {
    TextButton(
        onClick = {},
        modifier = Modifier.size(44.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = tint),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(label, fontSize = 18.sp)
    }
}

@Composable
private fun CompactBadge(label: String) {
    Box(
        modifier = Modifier
            .background(ArdourPalette.SurfaceRaised, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InspectorPane(modifier: Modifier) {
    Surface(modifier = modifier, color = ArdourPalette.Surface) {
        Column(Modifier.padding(14.dp)) {
            PaneHeader("Inspector")
            Spacer(Modifier.height(12.dp))
            LabeledValue("Track", "Lead Vocal")
            LabeledValue("Input", "Input 1")
            LabeledValue("Output", "Master")
            LabeledValue("Gain", "0.0 dB")
            LabeledValue("Pan", "Center")
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
    Surface(modifier = modifier, color = ArdourPalette.Background) {
        Column(Modifier.fillMaxSize()) {
            PaneHeader(
                title = "Tracks",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider(color = ArdourPalette.Outline)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .border(width = 0.5.dp, color = ArdourPalette.Outline)
    ) {
        Column(
            modifier = Modifier
                .width(154.dp)
                .fillMaxHeight()
                .background(ArdourPalette.Surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(track.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(
                track.detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "M   S   R",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(track.regionWidth)
                    .fillMaxHeight()
                    .background(
                        if (track.secondary) ArdourPalette.RegionSecondary else ArdourPalette.Region,
                        RoundedCornerShape(10.dp),
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = track.name,
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
    Surface(modifier = modifier, color = ArdourPalette.Surface) {
        Column(Modifier.padding(14.dp)) {
            PaneHeader("Browser")
            Spacer(Modifier.height(12.dp))
            BrowserItem("Instruments", "Native and external instruments")
            BrowserItem("Audio FX", "Dynamics, EQ and spatial")
            BrowserItem("MIDI FX", "Processors and generators")
            BrowserItem("Loops", "Project and user content")
            BrowserItem("Files", "Device and document storage")
        }
    }
}

@Composable
private fun MixerPane(modifier: Modifier) {
    Surface(modifier = modifier, color = ArdourPalette.Background) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
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
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = ArdourPalette.Surface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                name,
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
                    .width(12.dp)
                    .height(180.dp)
                    .background(ArdourPalette.SurfaceSelected, RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text("0.0", fontSize = 11.sp)
        }
    }
}

@Composable
private fun CenterWorkspace(modifier: Modifier, title: String, detail: String) {
    Surface(modifier = modifier, color = ArdourPalette.Background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun WorkspaceBar(selected: Workspace, onSelected: (Workspace) -> Unit) {
    Surface(color = ArdourPalette.Surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Workspace.entries.forEach { workspace ->
                val active = workspace == selected
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .background(
                            if (active) ArdourPalette.SurfaceSelected else Color.Transparent,
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { onSelected(workspace) }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        workspace.label,
                        color = if (active) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    )
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
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(value, fontSize = 11.sp)
    }
}

@Composable
private fun InsertSlot(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ArdourPalette.SurfaceRaised, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun BrowserItem(title: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(vertical = 10.dp)
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
            detail,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
