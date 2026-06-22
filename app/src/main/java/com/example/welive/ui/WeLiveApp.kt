package com.example.welive.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.WindowSnapshot
import com.example.welive.diagnostics.DetectionDiagnostics
import com.example.welive.diagnostics.HomeDebugRecorder
import com.example.welive.diagnostics.HomeDebugRecorderState
import com.example.welive.settings.AppSettings
import com.example.welive.settings.UserRulesRepository
import com.example.welive.training.TrainingCaptureState
import com.example.welive.training.TrainingDataRepository
import com.example.welive.ui.theme.Graphite
import com.example.welive.ui.theme.Ink
import com.example.welive.ui.theme.Paper
import com.example.welive.ui.theme.SignalCyan
import com.example.welive.ui.theme.SignalGreen
import com.example.welive.ui.theme.SignalRed
import com.example.welive.ui.theme.Steel
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WeLiveApp() {
    val context = LocalContext.current
    val repository = remember { UserRulesRepository(context.applicationContext) }
    val trainingRepository = remember { TrainingDataRepository(context.applicationContext) }
    val settings by repository.settings.collectAsState(initial = AppSettings())
    val diagnostics by DetectionDiagnostics.recentResults.collectAsState()
    val homeDebugState by HomeDebugRecorder.state.collectAsState()
    val latestSnapshot by TrainingCaptureState.latestInstagramSnapshot.collectAsState()
    val scope = rememberCoroutineScope()
    var showInstagramSettings by remember { mutableStateOf(false) }
    var trainingCount by remember { mutableStateOf(0) }
    var trainingMessage by remember { mutableStateOf("Capture Instagram, return here, tap a label.") }
    var openLimitResetCountdown by remember { mutableStateOf(formatOpenLimitResetCountdown()) }

    LaunchedEffect(Unit) {
        trainingCount = trainingRepository.countSamples()
    }

    LaunchedEffect(Unit) {
        while (true) {
            openLimitResetCountdown = formatOpenLimitResetCountdown()
            delay(30_000L)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Ink
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Header(settings = settings)
            }

            item {
                PermissionPanel(
                    onOpenAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }

            item {
                RulePanel(
                    settings = settings,
                    onBlockReelsChange = { enabled ->
                        scope.launch { repository.setBlockInstagramReels(enabled) }
                    },
                    onBlockWebsiteChange = { enabled ->
                        scope.launch { repository.setBlockInstagramWebsite(enabled) }
                    },
                    onGrayscaleInstagramAppChange = { enabled ->
                        scope.launch { repository.setGrayscaleInstagramApp(enabled) }
                    },
                    onLimitInstagramOpensPerDayChange = { enabled ->
                        scope.launch { repository.setLimitInstagramOpensPerDay(enabled) }
                    },
                    onInstagramDailyOpenLimitChange = { limit ->
                        scope.launch { repository.setInstagramDailyOpenLimit(limit) }
                    },
                    onBlockHomeFeedChange = { enabled ->
                        scope.launch { repository.setBlockInstagramHomeFeed(enabled) }
                    },
                    onBlockHomeStoriesChange = { enabled ->
                        scope.launch { repository.setBlockInstagramHomeStories(enabled) }
                    },
                    onAllowStoriesChange = { enabled ->
                        scope.launch { repository.setAllowInstagramStories(enabled) }
                    },
                    onBlockSearchGridChange = { enabled ->
                        scope.launch { repository.setBlockInstagramSearchGrid(enabled) }
                    },
                    onAllowFriendReelsChange = { enabled ->
                        scope.launch { repository.setAllowInstagramReelsFromFriends(enabled) }
                    },
                    onReverseFromReelChange = { enabled ->
                        scope.launch { repository.setReverseFromReel(enabled) }
                    },
                    onPulseBlockScreenOnReverseChange = { enabled ->
                        scope.launch { repository.setPulseBlockScreenOnReverse(enabled) }
                    },
                    openLimitResetCountdown = openLimitResetCountdown,
                    showInstagramSettings = showInstagramSettings,
                    onInstagramSettingsClick = {
                        showInstagramSettings = !showInstagramSettings
                    }
                )
            }

            item {
                TrainingPanel(
                    latestSnapshot = latestSnapshot,
                    sampleCount = trainingCount,
                    message = trainingMessage,
                    filePath = trainingRepository.samplesFilePath,
                    onLabel = { label ->
                        val snapshot = latestSnapshot
                        if (snapshot == null) {
                            trainingMessage = "No Instagram snapshot yet."
                        } else {
                            scope.launch {
                                trainingCount = trainingRepository.saveInstagramSample(label, snapshot)
                                trainingMessage = "Saved $label sample."
                            }
                        }
                    }
                )
            }

            item {
                HomeDebugRecorderPanel(
                    state = homeDebugState,
                    onStart = {
                        HomeDebugRecorder.start(context.applicationContext)
                    },
                    onStop = {
                        scope.launch {
                            HomeDebugRecorder.stop(context.applicationContext)
                        }
                    }
                )
            }

            item {
                DiagnosticsHeader(count = diagnostics.size)
            }

            if (diagnostics.isEmpty()) {
                item {
                    EmptyDiagnostics()
                }
            } else {
                items(diagnostics) { result ->
                    DetectionRow(result = result)
                }
            }
        }
    }
}

@Composable
private fun Header(settings: AppSettings) {
    val activeTarget by animateFloatAsState(
        targetValue = if (settings.blockInstagramReels) 1f else 0.35f,
        animationSpec = tween(durationMillis = 520),
        label = "protection_intensity"
    )
    val transition = rememberInfiniteTransition(label = "scan_motion")
    val scanOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 54f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Graphite)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .offset(y = scanOffset.dp)
                        .background(SignalGreen.copy(alpha = activeTarget))
                )
            }
            Column {
                Text(
                    text = "WeLive",
                    color = Paper,
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "Instagram Reels shield",
                    color = Steel,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Graphite)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(activeTarget)
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(SignalGreen, SignalCyan, SignalRed)
                        )
                    )
            )
        }
    }
}

@Composable
private fun PermissionPanel(onOpenAccessibility: () -> Unit) {
    Panel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Accessibility service",
                    color = Paper,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Required for event-driven detection",
                    color = Steel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onOpenAccessibility,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Paper,
                    contentColor = Ink
                )
            ) {
                Text("Open")
            }
        }
    }
}

@Composable
private fun RulePanel(
    settings: AppSettings,
    onBlockReelsChange: (Boolean) -> Unit,
    onBlockWebsiteChange: (Boolean) -> Unit,
    onGrayscaleInstagramAppChange: (Boolean) -> Unit,
    onLimitInstagramOpensPerDayChange: (Boolean) -> Unit,
    onInstagramDailyOpenLimitChange: (Int) -> Unit,
    onBlockHomeFeedChange: (Boolean) -> Unit,
    onBlockHomeStoriesChange: (Boolean) -> Unit,
    onAllowStoriesChange: (Boolean) -> Unit,
    onBlockSearchGridChange: (Boolean) -> Unit,
    onAllowFriendReelsChange: (Boolean) -> Unit,
    onReverseFromReelChange: (Boolean) -> Unit,
    onPulseBlockScreenOnReverseChange: (Boolean) -> Unit,
    openLimitResetCountdown: String,
    showInstagramSettings: Boolean,
    onInstagramSettingsClick: () -> Unit
) {
    Panel {
        Text("Protected apps", color = Paper, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp, 42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SignalGreen)
                )
                Column {
                    Text("Instagram", color = Paper, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (showInstagramSettings) "Sub-settings open" else "Reels shield active",
                        color = Steel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onInstagramSettingsClick) {
                Text(
                    text = "⚙",
                    color = if (showInstagramSettings) SignalGreen else Paper,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (showInstagramSettings) {
            Spacer(modifier = Modifier.height(14.dp))
            ToggleRow(
                label = "Block Reels",
                detail = "High-confidence detector only",
                checked = settings.blockInstagramReels,
                accent = SignalGreen,
                onCheckedChange = onBlockReelsChange
            )
            ToggleRow(
                label = "Block Website",
                detail = "Blocks instagram.com in supported browsers",
                checked = settings.blockInstagramWebsite,
                accent = SignalRed,
                onCheckedChange = onBlockWebsiteChange
            )
            ToggleRow(
                label = "App Grayscale",
                detail = "Turns Instagram monochrome while the app is open",
                checked = settings.grayscaleInstagramApp,
                accent = SignalCyan,
                onCheckedChange = onGrayscaleInstagramAppChange
            )
            ToggleRow(
                label = "Daily Open Limit",
                detail = "${settings.instagramOpensToday()}/${settings.instagramDailyOpenLimit} opens used today, resets in $openLimitResetCountdown",
                checked = settings.limitInstagramOpensPerDay,
                accent = SignalRed,
                onCheckedChange = onLimitInstagramOpensPerDayChange
            )
            StepperRow(
                label = "Allowed Opens",
                detail = "After these are used, Instagram is sent back Home",
                value = settings.instagramDailyOpenLimit,
                accent = SignalRed,
                onValueChange = onInstagramDailyOpenLimitChange
            )
            ToggleRow(
                label = "Block Home Feed",
                detail = "Blocks only the center feed and leaves stories and navigation available",
                checked = settings.blockInstagramHomeFeed,
                accent = SignalGreen,
                onCheckedChange = onBlockHomeFeedChange
            )
            ToggleRow(
                label = "Block Home Stories",
                detail = "Covers the story strip while the home feed blocker is active",
                checked = settings.blockInstagramHomeStories,
                accent = SignalRed,
                onCheckedChange = onBlockHomeStoriesChange
            )
            ToggleRow(
                label = "Reverse from Blocker",
                detail = "Use Android back when blocked Instagram content is detected",
                checked = settings.reverseFromReel,
                accent = SignalCyan,
                onCheckedChange = onReverseFromReelChange
            )
            ToggleRow(
                label = "Pulse Block Screen",
                detail = "Brief cover while reversing",
                checked = settings.pulseBlockScreenOnReverse,
                accent = SignalRed,
                onCheckedChange = onPulseBlockScreenOnReverseChange
            )
            ToggleRow(
                label = "Allow Friend Reels",
                detail = "Message-shared Reels can pass",
                checked = settings.allowInstagramReelsFromFriends,
                accent = SignalGreen,
                onCheckedChange = onAllowFriendReelsChange
            )
            ToggleRow(
                label = "Allow Stories",
                detail = "Story viewer remains available",
                checked = settings.allowInstagramStories,
                accent = SignalCyan,
                onCheckedChange = onAllowStoriesChange
            )
            ToggleRow(
                label = "Block Search Grid",
                detail = "Blocks mini-reel explore tiles",
                checked = settings.blockInstagramSearchGrid,
                accent = SignalRed,
                onCheckedChange = onBlockSearchGridChange
            )
        }
    }
}

@Composable
private fun TrainingPanel(
    latestSnapshot: WindowSnapshot?,
    sampleCount: Int,
    message: String,
    filePath: String,
    onLabel: (String) -> Unit
) {
    val labels = listOf(
        "REELS",
        "FRIEND_REELS",
        "PROFILE",
        "POSTS",
        "DMS",
        "SEARCH_REELS_GRID",
        "STORY",
        "UNKNOWN"
    )

    Panel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Training data", color = Paper, style = MaterialTheme.typography.titleMedium)
                Text(message, color = Steel, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("$sampleCount saved", color = SignalGreen, style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (latestSnapshot == null) {
            Text("Latest Instagram snapshot: none", color = Steel)
        } else {
            Text(
                text = "Latest: ${latestSnapshot.nodeCount} nodes, ${latestSnapshot.viewIds.size} ids, ${latestSnapshot.texts.size + latestSnapshot.contentDescriptions.size} labels",
                color = Steel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.chunked(3).forEach { rowLabels ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowLabels.forEach { label ->
                        Button(
                            onClick = { onLabel(label) },
                            modifier = Modifier.weight(1f),
                            enabled = latestSnapshot != null,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (label) {
                                    "REELS" -> SignalGreen
                                    "FRIEND_REELS" -> SignalCyan
                                    "SEARCH_REELS_GRID" -> SignalRed
                                    else -> Graphite
                                },
                                contentColor = if (
                                    label == "REELS" ||
                                    label == "FRIEND_REELS" ||
                                    label == "SEARCH_REELS_GRID"
                                ) {
                                    Ink
                                } else {
                                    Paper
                                },
                                disabledContainerColor = Graphite.copy(alpha = 0.35f),
                                disabledContentColor = Steel
                            )
                        ) {
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    repeat(3 - rowLabels.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = filePath,
            color = Steel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun HomeDebugRecorderPanel(
    state: HomeDebugRecorderState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Panel(
        borderColor = if (state.isRecording) SignalRed.copy(alpha = 0.75f) else SignalCyan.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Home debug recorder", color = Paper, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = state.message,
                    color = if (state.isRecording) SignalRed else Steel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${state.eventCount} events",
                color = if (state.isRecording) SignalRed else SignalCyan,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStart,
                enabled = !state.isRecording,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SignalCyan,
                    contentColor = Ink,
                    disabledContainerColor = Graphite.copy(alpha = 0.45f),
                    disabledContentColor = Steel
                )
            ) {
                Text("Start")
            }
            Button(
                onClick = onStop,
                enabled = state.isRecording,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SignalRed,
                    contentColor = Ink,
                    disabledContainerColor = Graphite.copy(alpha = 0.45f),
                    disabledContentColor = Steel
                )
            ) {
                Text("Stop")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = state.lastSessionFilePath ?: "No saved session yet",
            color = Steel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    detail: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp, 36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = if (checked) 1f else 0.25f))
            )
            Column {
                Text(label, color = Paper, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Steel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = accent,
                uncheckedThumbColor = Steel,
                uncheckedTrackColor = Graphite
            )
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    detail: String,
    value: Int,
    accent: Color,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp, 36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent)
            )
            Column {
                Text(label, color = Paper, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Steel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onValueChange((value - 1).coerceAtLeast(1)) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Graphite,
                    contentColor = Paper
                )
            ) {
                Text("-")
            }
            Text(
                text = value.toString(),
                color = Paper,
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = { onValueChange((value + 1).coerceAtMost(99)) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Ink
                )
            ) {
                Text("+")
            }
        }
    }
}

@Composable
private fun DiagnosticsHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "Diagnostics",
            color = Paper,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "$count recent",
            color = Steel,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun EmptyDiagnostics() {
    Panel {
        Text(
            text = "No Instagram windows classified yet.",
            color = Steel
        )
    }
}

@Composable
private fun DetectionRow(result: DetectionResult) {
    val isBlocked = result.recommendedAction != InterventionAction.NONE
    Panel(
        borderColor = if (isBlocked) SignalGreen.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.surface.name,
                    color = Paper,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.packageName,
                    color = Steel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${(result.confidence * 100).toInt()}%",
                color = if (isBlocked) SignalGreen else Steel,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = when (result.recommendedAction) {
                InterventionAction.BLOCK_AND_RETURN -> "Action: block and return"
                InterventionAction.BLOCK -> "Action: block"
                InterventionAction.NONE -> "Action: observe only"
            },
            color = if (isBlocked) SignalGreen else Steel,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(6.dp))
        result.reasons.take(6).forEach { reason ->
            Text(
                text = reason,
                color = Steel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Panel(
    borderColor: Color = Color.White.copy(alpha = 0.10f),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Graphite.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .padding(16.dp),
        content = content
    )
}

private fun formatOpenLimitResetCountdown(now: LocalDateTime = LocalDateTime.now()): String {
    val nextReset = now.toLocalDate().plusDays(1).atStartOfDay()
    val duration = Duration.between(now, nextReset).coerceAtLeast(Duration.ZERO)
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
