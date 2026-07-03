package com.example.welive.ui

import android.content.Intent
import android.provider.Settings
<<<<<<< HEAD:app/src/main/java/com/example/welive/ui/WeLiveApp.kt
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
=======
>>>>>>> 50e6c0991e5f361964e88ee4590d9e89c4334699:app/src/main/java/com/example/welive/ui/UnreelApp.kt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
<<<<<<< HEAD:app/src/main/java/com/example/welive/ui/WeLiveApp.kt
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
=======
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.welive.R
>>>>>>> 50e6c0991e5f361964e88ee4590d9e89c4334699:app/src/main/java/com/example/welive/ui/UnreelApp.kt
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.WindowSnapshot
import com.example.welive.diagnostics.DetectionDiagnostics
import com.example.welive.diagnostics.HomeDebugRecorder
import com.example.welive.diagnostics.HomeDebugRecorderState
import com.example.welive.settings.AppSettings
import com.example.welive.settings.DailyScheduleWindow
import com.example.welive.settings.InstagramAccessScheduleCodec
import com.example.welive.settings.UserRulesRepository
import com.example.welive.settings.crossesMidnight
import com.example.welive.training.TrainingCaptureState
import com.example.welive.training.TrainingDataRepository
import com.example.welive.ui.theme.Graphite
import com.example.welive.ui.theme.Ink
import com.example.welive.ui.theme.Paper
import com.example.welive.ui.theme.SignalCyan
import com.example.welive.ui.theme.SignalGreen
import com.example.welive.ui.theme.SignalRed
import com.example.welive.ui.theme.Steel
import java.time.LocalTime
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UnreelApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    var selectedTab by remember { mutableStateOf(AppDashboardTab.ProtectedApps) }
    var sessionUnlocked by rememberSaveable { mutableStateOf(false) }
    var unlockPin by rememberSaveable { mutableStateOf("") }
    var unlockMessage by remember { mutableStateOf<String?>(null) }
    var securityPin by rememberSaveable { mutableStateOf("") }
    var securityPinConfirm by rememberSaveable { mutableStateOf("") }
    var securityMessage by remember { mutableStateOf<String?>(null) }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val appLockConfigured = settings.appSecurityEnabled && settings.hasAppPinConfigured()
    val appIsLocked = settings.isAppLocked(currentTimeMillis)
    val needsPinUnlock = appLockConfigured && !appIsLocked && !sessionUnlocked

    LaunchedEffect(Unit) {
        trainingCount = trainingRepository.countSamples()
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            openLimitResetCountdown = formatOpenLimitResetCountdown()
            delay(1_000L)
        }
    }

    LaunchedEffect(settings.appSecurityEnabled, settings.appLockedUntilMillis) {
        if (!settings.appSecurityEnabled) {
            sessionUnlocked = true
        } else if (!settings.hasAppPinConfigured()) {
            sessionUnlocked = true
        } else if (settings.isAppLocked(currentTimeMillis)) {
            sessionUnlocked = false
            unlockPin = ""
            unlockMessage = null
        }
    }

    DisposableEffect(lifecycleOwner, settings.appSecurityEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && settings.appSecurityEnabled) {
                sessionUnlocked = false
                unlockPin = ""
                unlockMessage = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Ink
    ) {
        if (appIsLocked) {
            LockedAppScreen(
                lockedUntilMillis = settings.appLockedUntilMillis,
                currentTimeMillis = currentTimeMillis
            )
            return@Surface
        }

        if (needsPinUnlock) {
            AppUnlockScreen(
                pin = unlockPin,
                onPinChange = {
                    unlockPin = it.take(MAX_PIN_LENGTH).filter(Char::isDigit)
                    unlockMessage = null
                },
                message = unlockMessage,
                onUnlock = {
                    if (settings.verifyAppPin(unlockPin)) {
                        sessionUnlocked = true
                        unlockPin = ""
                        unlockMessage = null
                    } else {
                        unlockMessage = "Incorrect PIN."
                    }
                }
            )
            return@Surface
        }

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
                DashboardTabSwitcher(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            if (selectedTab == AppDashboardTab.ProtectedApps) {
                item {
                    AppSecurityPanel(
                        settings = settings,
                        currentTimeMillis = currentTimeMillis,
                        pin = securityPin,
                        pinConfirm = securityPinConfirm,
                        message = securityMessage,
                        onPinChange = {
                            securityPin = it.take(MAX_PIN_LENGTH).filter(Char::isDigit)
                            securityMessage = null
                        },
                        onPinConfirmChange = {
                            securityPinConfirm = it.take(MAX_PIN_LENGTH).filter(Char::isDigit)
                            securityMessage = null
                        },
                        onLockDurationHoursChange = { hours ->
                            scope.launch { repository.setAppLockDurationHours(hours) }
                        },
                        onEnableAndLock = {
                            val validationMessage = validatePinInputs(securityPin, securityPinConfirm)
                            if (validationMessage != null) {
                                securityMessage = validationMessage
                            } else {
                                scope.launch {
                                    repository.enableAppSecurity(
                                        pin = securityPin,
                                        durationHours = settings.appLockDurationHours
                                    )
                                    securityPin = ""
                                    securityPinConfirm = ""
                                    securityMessage = "App lock enabled."
                                    sessionUnlocked = false
                                }
                            }
                        },
                        onUpdatePin = {
                            val validationMessage = validatePinInputs(securityPin, securityPinConfirm)
                            if (validationMessage != null) {
                                securityMessage = validationMessage
                            } else {
                                scope.launch {
                                    repository.updateAppSecurityPin(securityPin)
                                    securityPin = ""
                                    securityPinConfirm = ""
                                    securityMessage = "PIN updated."
                                }
                            }
                        },
                        onLockNow = {
                            scope.launch {
                                repository.armAppSecurityLock()
                                sessionUnlocked = false
                            }
                        },
                        onDisable = {
                            scope.launch {
                                repository.disableAppSecurity()
                                sessionUnlocked = true
                                securityPin = ""
                                securityPinConfirm = ""
                                securityMessage = "App lock disabled."
                            }
                        }
                    )
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
                        onLimitInstagramToScheduleChange = { enabled ->
                            scope.launch { repository.setLimitInstagramToSchedule(enabled) }
                        },
                        onInstagramAccessScheduleChange = { windows ->
                            scope.launch { repository.setInstagramAccessSchedule(windows) }
                        },
                        onBlockHomeFeedChange = { enabled ->
                            scope.launch { repository.setBlockInstagramHomeFeed(enabled) }
                        },
                        onPreloadHomeFeedBlockOnInstagramOpenChange = { enabled ->
                            scope.launch { repository.setPreloadHomeFeedBlockOnInstagramOpen(enabled) }
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
            } else {
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
}

private enum class AppDashboardTab(val title: String, val subtitle: String) {
    ProtectedApps("Protected Apps", "Rules and access control"),
    Data("Data", "Training, recorder, diagnostics")
}

@Composable
private fun LockedAppScreen(
    lockedUntilMillis: Long,
    currentTimeMillis: Long
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Ink
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Panel(borderColor = SignalRed.copy(alpha = 0.55f)) {
                Text(
                    text = "Unreel Locked",
                    color = Paper,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Settings are locked to prevent tampering while your blockers are running.",
                    color = Steel
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = formatAppLockRemaining(lockedUntilMillis, currentTimeMillis),
                    color = SignalRed,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unlock becomes available after the timer expires.",
                    color = Steel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun AppUnlockScreen(
    pin: String,
    onPinChange: (String) -> Unit,
    message: String?,
    onUnlock: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Ink
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Panel(borderColor = SignalCyan.copy(alpha = 0.5f)) {
                Text(
                    text = "Enter PIN",
                    color = Paper,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This app is protected. Enter your PIN to open settings.",
                    color = Steel
                )
                Spacer(modifier = Modifier.height(18.dp))
                PinTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    label = "PIN"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message ?: "PIN unlock is required every time the app is reopened.",
                    color = if (message == null) Steel else SignalRed,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onUnlock,
                    enabled = pin.length >= MIN_PIN_LENGTH,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalCyan,
                        contentColor = Ink,
                        disabledContainerColor = Graphite.copy(alpha = 0.45f),
                        disabledContentColor = Steel
                    )
                ) {
                    Text("Unlock")
                }
            }
        }
    }
}

@Composable
private fun AppSecurityPanel(
    settings: AppSettings,
    currentTimeMillis: Long,
    pin: String,
    pinConfirm: String,
    message: String?,
    onPinChange: (String) -> Unit,
    onPinConfirmChange: (String) -> Unit,
    onLockDurationHoursChange: (Int) -> Unit,
    onEnableAndLock: () -> Unit,
    onUpdatePin: () -> Unit,
    onLockNow: () -> Unit,
    onDisable: () -> Unit
) {
    val configured = settings.hasAppPinConfigured()
    Panel(borderColor = SignalCyan.copy(alpha = 0.45f)) {
        Text("App lock", color = Paper, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (!settings.appSecurityEnabled) {
                "Set a PIN, then lock Unreel for a cooldown period so settings cannot be changed."
            } else if (settings.isAppLocked(currentTimeMillis)) {
                "Lock active. The app will stay closed until the timer expires."
            } else {
                "App lock is enabled. You can update the PIN, adjust the cooldown, or lock the app again now."
            },
            color = Steel
        )
        Spacer(modifier = Modifier.height(14.dp))

        StepperRow(
            label = "Lock Duration",
            detail = "How long Unreel stays inaccessible after you lock it",
            value = settings.appLockDurationHours,
            accent = SignalCyan,
            onValueChange = onLockDurationHoursChange
        )

        Spacer(modifier = Modifier.height(6.dp))
        PinTextField(
            value = pin,
            onValueChange = onPinChange,
            label = if (configured) "New PIN" else "PIN"
        )
        Spacer(modifier = Modifier.height(8.dp))
        PinTextField(
            value = pinConfirm,
            onValueChange = onPinConfirmChange,
            label = if (configured) "Confirm New PIN" else "Confirm PIN"
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message ?: if (configured) {
                "Use $MIN_PIN_LENGTH to $MAX_PIN_LENGTH digits."
            } else {
                "Choose a $MIN_PIN_LENGTH to $MAX_PIN_LENGTH digit PIN."
            },
            color = if (message == null) Steel else SignalRed,
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(14.dp))
        if (!settings.appSecurityEnabled || !configured) {
            Button(
                onClick = onEnableAndLock,
                enabled = pin.isNotBlank() && pinConfirm.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SignalCyan,
                    contentColor = Ink,
                    disabledContainerColor = Graphite.copy(alpha = 0.45f),
                    disabledContentColor = Steel
                )
            ) {
                Text("Enable & Lock")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLockNow,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalCyan,
                        contentColor = Ink
                    )
                ) {
                    Text("Lock Now")
                }
                Button(
                    onClick = onDisable,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Graphite,
                        contentColor = Paper
                    )
                ) {
                    Text("Disable")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onUpdatePin,
                enabled = pin.isNotBlank() && pinConfirm.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SignalGreen,
                    contentColor = Ink,
                    disabledContainerColor = Graphite.copy(alpha = 0.45f),
                    disabledContentColor = Steel
                )
            ) {
                Text("Update PIN")
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
                Image(
                    painter = painterResource(id = R.drawable.unreel_logo_mark),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                )
            }
            Column {
                Text(
                    text = "Unreel",
                    color = Paper,
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "Shortform shield",
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
private fun DashboardTabSwitcher(
    selectedTab: AppDashboardTab,
    onTabSelected: (AppDashboardTab) -> Unit
) {
    Panel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppDashboardTab.entries.forEach { tab ->
                val active = tab == selectedTab
                Button(
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) Paper else Graphite,
                        contentColor = if (active) Ink else Paper
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 14.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tab.subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) Ink.copy(alpha = 0.72f) else Steel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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
    onLimitInstagramToScheduleChange: (Boolean) -> Unit,
    onInstagramAccessScheduleChange: (List<DailyScheduleWindow>) -> Unit,
    onBlockHomeFeedChange: (Boolean) -> Unit,
    onPreloadHomeFeedBlockOnInstagramOpenChange: (Boolean) -> Unit,
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
    val scheduleWindows = settings.instagramAccessSchedule.ifEmpty {
        InstagramAccessScheduleCodec.defaultWindows()
    }
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
                label = "Scheduled Access",
                detail = formatInstagramScheduleDetail(settings),
                checked = settings.limitInstagramToSchedule,
                accent = SignalCyan,
                onCheckedChange = onLimitInstagramToScheduleChange
            )
            if (settings.limitInstagramToSchedule) {
                Spacer(modifier = Modifier.height(6.dp))
                InstagramScheduleEditor(
                    windows = scheduleWindows,
                    onWindowsChange = onInstagramAccessScheduleChange
                )
            }
            ToggleRow(
                label = "Block Home Feed",
                detail = "Blocks only the center feed and leaves stories and navigation available",
                checked = settings.blockInstagramHomeFeed,
                accent = SignalGreen,
                onCheckedChange = onBlockHomeFeedChange
            )
            ToggleRow(
                label = "Instant Home Block",
                detail = "Preloads the feed blocker as soon as Instagram opens",
                checked = settings.preloadHomeFeedBlockOnInstagramOpen,
                accent = SignalCyan,
                onCheckedChange = onPreloadHomeFeedBlockOnInstagramOpenChange
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
private fun InstagramScheduleEditor(
    windows: List<DailyScheduleWindow>,
    onWindowsChange: (List<DailyScheduleWindow>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Uses your current local timezone automatically.",
            color = Steel,
            style = MaterialTheme.typography.labelLarge
        )
        windows.forEachIndexed { index, window ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Window ${index + 1}",
                        color = Paper,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (windows.size > 1) {
                        Button(
                            onClick = {
                                onWindowsChange(
                                    windows.toMutableList().apply { removeAt(index) }.normalizeScheduleWindows()
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Graphite,
                                contentColor = Paper
                            )
                        ) {
                            Text("Remove")
                        }
                    }
                }
                TimeAdjusterRow(
                    label = "Start",
                    minuteOfDay = window.startMinuteOfDay,
                    accent = SignalGreen
                ) { minute ->
                    onWindowsChange(
                        windows.toMutableList().apply {
                            this[index] = window.copy(startMinuteOfDay = minute)
                        }.normalizeScheduleWindows(fallback = windows)
                    )
                }
                TimeAdjusterRow(
                    label = "End",
                    minuteOfDay = window.endMinuteOfDay,
                    accent = SignalRed
                ) { minute ->
                    onWindowsChange(
                        windows.toMutableList().apply {
                            this[index] = window.copy(endMinuteOfDay = minute)
                        }.normalizeScheduleWindows(fallback = windows)
                    )
                }
                Text(
                    text = formatScheduleWindowLabel(window),
                    color = Steel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        if (windows.size < MAX_SCHEDULE_WINDOWS) {
            Button(
                onClick = {
                    onWindowsChange(
                        (windows + defaultAdditionalScheduleWindow(windows)).normalizeScheduleWindows()
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SignalCyan,
                    contentColor = Ink
                )
            ) {
                Text("Add Time Window")
            }
        }
    }
}

@Composable
private fun TimeAdjusterRow(
    label: String,
    minuteOfDay: Int,
    accent: Color,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Paper, fontWeight = FontWeight.SemiBold)
            Text(
                text = formatMinuteOfDay(minuteOfDay),
                color = Steel,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onMinuteChange(shiftMinuteOfDay(minuteOfDay, -SCHEDULE_MINUTE_STEP)) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Graphite,
                    contentColor = Paper
                )
            ) {
                Text("-")
            }
            Button(
                onClick = { onMinuteChange(shiftMinuteOfDay(minuteOfDay, SCHEDULE_MINUTE_STEP)) },
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
        "POST_CREATOR",
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
                                    "POST_CREATOR" -> SignalCyan.copy(alpha = 0.72f)
                                    else -> Graphite
                                },
                                contentColor = if (
                                    label == "REELS" ||
                                    label == "FRIEND_REELS" ||
                                    label == "SEARCH_REELS_GRID" ||
                                    label == "POST_CREATOR"
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
private fun PinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Graphite,
            unfocusedContainerColor = Graphite,
            disabledContainerColor = Graphite.copy(alpha = 0.45f),
            focusedTextColor = Paper,
            unfocusedTextColor = Paper,
            focusedLabelColor = SignalCyan,
            unfocusedLabelColor = Steel,
            focusedIndicatorColor = SignalCyan,
            unfocusedIndicatorColor = Color.White.copy(alpha = 0.12f),
            cursorColor = SignalCyan
        )
    )
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

private fun validatePinInputs(pin: String, confirmPin: String): String? {
    if (pin.length < MIN_PIN_LENGTH) {
        return "PIN must be at least $MIN_PIN_LENGTH digits."
    }
    if (pin.length > MAX_PIN_LENGTH) {
        return "PIN must be at most $MAX_PIN_LENGTH digits."
    }
    if (pin != confirmPin) {
        return "PIN entries do not match."
    }
    if (!pin.all(Char::isDigit)) {
        return "PIN must use digits only."
    }
    return null
}

private fun formatAppLockRemaining(lockedUntilMillis: Long, currentTimeMillis: Long): String {
    val remainingMillis = (lockedUntilMillis - currentTimeMillis).coerceAtLeast(0L)
    val totalMinutes = remainingMillis / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m remaining"
        minutes > 0 -> "${minutes}m remaining"
        else -> "<1m remaining"
    }
}

private fun formatInstagramScheduleDetail(
    settings: AppSettings,
    now: ZonedDateTime = ZonedDateTime.now()
): String {
    if (!settings.limitInstagramToSchedule) {
        return "Allow Instagram only during scheduled local-time windows"
    }
    val windows = settings.instagramAccessSchedule.ifEmpty { InstagramAccessScheduleCodec.defaultWindows() }
    if (windows.isEmpty()) {
        return "No access windows set"
    }
    val status = settings.instagramScheduleStatus(now)
    val nextTransition = status.nextTransitionAt
    val timeText = nextTransition?.toLocalTime()?.format(scheduleTimeFormatter()).orEmpty()
    return if (status.isAllowedNow) {
        "Open now, locks at $timeText local time"
    } else if (nextTransition != null) {
        "Locked now, next opens at $timeText local time"
    } else {
        "Locked now, no upcoming window"
    }
}

private fun formatScheduleWindowLabel(window: DailyScheduleWindow): String {
    val suffix = if (window.crossesMidnight()) " next day" else ""
    return "${formatMinuteOfDay(window.startMinuteOfDay)} to ${formatMinuteOfDay(window.endMinuteOfDay)}$suffix"
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    return LocalTime.MIN
        .plusMinutes(minuteOfDay.toLong().coerceIn(0L, 1_439L))
        .format(scheduleTimeFormatter())
}

private fun shiftMinuteOfDay(currentMinute: Int, deltaMinutes: Int): Int {
    val total = 1_440
    val shifted = (currentMinute + deltaMinutes) % total
    return if (shifted < 0) shifted + total else shifted
}

private fun defaultAdditionalScheduleWindow(windows: List<DailyScheduleWindow>): DailyScheduleWindow {
    val baseStart = windows.lastOrNull()?.endMinuteOfDay ?: (18 * 60)
    val start = shiftMinuteOfDay(baseStart, 60)
    val end = shiftMinuteOfDay(start, 60)
    return DailyScheduleWindow(start, end)
}

private fun List<DailyScheduleWindow>.normalizeScheduleWindows(
    fallback: List<DailyScheduleWindow> = this
): List<DailyScheduleWindow> {
    val normalized = map { it.normalized() }
        .filter { it.isNonEmpty() }
        .distinctBy { "${it.startMinuteOfDay}-${it.endMinuteOfDay}" }
        .sortedBy { it.startMinuteOfDay }
    return if (normalized.isEmpty()) fallback else normalized
}

private fun scheduleTimeFormatter(): DateTimeFormatter {
    return DateTimeFormatter.ofPattern("h:mm a")
}

private const val SCHEDULE_MINUTE_STEP = 15
private const val MAX_SCHEDULE_WINDOWS = 4
private const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 8
