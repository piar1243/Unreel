package com.example.welive.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.welive.detection.WindowSnapshot
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.diagnostics.HomeDebugRecorderState
import com.example.welive.deviceowner.DeviceOwnerPolicyController
import com.example.welive.deviceowner.UninstallProtectionStatus
import com.example.welive.analytics.ScreenTimeCategory
import com.example.welive.settings.AppSettings
import com.example.welive.settings.DailyScheduleWindow
import com.example.welive.settings.InstagramAccessScheduleCodec
import com.example.welive.settings.UserRulesRepository
import com.example.welive.settings.crossesMidnight
import com.example.welive.protection.ProtectedApp
import com.example.welive.R
import com.example.welive.training.TrainingCaptureState
import com.example.welive.training.TrainingDataRepository
import com.example.welive.ui.theme.Carbon
import com.example.welive.ui.theme.Graphite
import com.example.welive.ui.theme.Ink
import com.example.welive.ui.theme.Paper
import com.example.welive.ui.theme.SignalCyan
import com.example.welive.ui.theme.SignalGreen
import com.example.welive.ui.theme.SignalRed
import com.example.welive.ui.theme.Steel
import com.example.welive.visibility.AppVisibilityController
import java.time.LocalTime
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WeLiveApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { UserRulesRepository(context.applicationContext) }
    val trainingRepository = remember { TrainingDataRepository(context.applicationContext) }
    val deviceOwnerPolicyController = remember { DeviceOwnerPolicyController(context.applicationContext) }
    val appVisibilityController = remember { AppVisibilityController(context.applicationContext) }
    val settings by repository.settings.collectAsState(initial = AppSettings())
    val latestSnapshot by TrainingCaptureState.latestInstagramSnapshot.collectAsState()
    val latestSettingsSnapshot by TrainingCaptureState.latestSettingsSnapshot.collectAsState()
    val scope = rememberCoroutineScope()
    val dashboardListState = rememberLazyListState()
    var expandedProtectedApp by remember { mutableStateOf<ProtectedApp?>(ProtectedApp.INSTAGRAM) }
    var trainingCount by remember { mutableStateOf(0) }
    var trainingMessage by remember { mutableStateOf("Capture Instagram, return here, tap a label.") }
    var settingsTrainingCount by remember { mutableStateOf(0) }
    var settingsTrainingMessage by remember {
        mutableStateOf("Open Unreel uninstall or Accessibility permission screens, return here, tap a label.")
    }
    var openLimitResetCountdown by remember { mutableStateOf(formatOpenLimitResetCountdown()) }
    var selectedTab by remember { mutableStateOf(AppDashboardTab.Home) }
    var showUnavailableProtectedApps by rememberSaveable { mutableStateOf(false) }
    var onboardingWeeklyMinutes by rememberSaveable { mutableStateOf(14 * 60) }
    var sessionUnlocked by rememberSaveable { mutableStateOf(false) }
    var unlockPin by rememberSaveable { mutableStateOf("") }
    var unlockMessage by remember { mutableStateOf<String?>(null) }
    var securityPin by rememberSaveable { mutableStateOf("") }
    var securityPinConfirm by rememberSaveable { mutableStateOf("") }
    var securityMessage by remember { mutableStateOf<String?>(null) }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var pendingCredentialRequest by remember { mutableStateOf<DeviceCredentialRequest?>(null) }

    val appLockConfigured = settings.appSecurityEnabled && settings.hasAppPinConfigured()
    val appIsLocked = settings.isAppLocked(currentTimeMillis)
    val needsPinUnlock = appLockConfigured && !appIsLocked && !sessionUnlocked
    val uninstallProtectionStatus = remember(
        settings.protectAppUninstall,
        settings.uninstallBypassUntilMillis,
        currentTimeMillis
    ) {
        deviceOwnerPolicyController.status(settings, currentTimeMillis)
    }
    val credentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val request = pendingCredentialRequest
        pendingCredentialRequest = null
        if (request == null) {
            return@rememberLauncherForActivityResult
        }
        if (result.resultCode != Activity.RESULT_OK) {
            securityMessage = "Phone credential check was cancelled."
            return@rememberLauncherForActivityResult
        }
        when (request) {
            DeviceCredentialRequest.AllowTemporaryUninstall -> {
                val now = System.currentTimeMillis()
                val bypassUntilMillis = now + APP_UNINSTALL_BYPASS_DURATION_MILLIS
                scope.launch {
                    repository.allowAppUninstallTemporarily(APP_UNINSTALL_BYPASS_DURATION_MILLIS)
                    deviceOwnerPolicyController.syncPolicies(
                        settings.copy(uninstallBypassUntilMillis = bypassUntilMillis),
                        nowMillis = now
                    )
                    securityMessage = "Uninstall allowed for 2 minutes."
                    context.startActivity(deviceOwnerPolicyController.createAppInfoIntent())
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        trainingCount = trainingRepository.countSamples()
        settingsTrainingCount = trainingRepository.countSettingsSamples()
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

    LaunchedEffect(settings.protectAppUninstall, settings.uninstallBypassUntilMillis) {
        deviceOwnerPolicyController.syncPolicies(settings)
    }

    LaunchedEffect(settings.hideLauncherIcon) {
        appVisibilityController.syncLauncherVisibility(settings.hideLauncherIcon)
    }

    LaunchedEffect(selectedTab) {
        dashboardListState.scrollToItem(0)
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
        color = Carbon
    ) {
        if (appIsLocked) {
            LockedAppScreen(
                lockedUntilMillis = settings.appLockedUntilMillis,
                currentTimeMillis = currentTimeMillis,
                uninstallProtectionStatus = uninstallProtectionStatus,
                onTemporarilyAllowUninstall = {
                    requestDeviceCredentialForTemporaryUninstall(
                        context = context,
                        canManageUninstallBlock = uninstallProtectionStatus.canControlUninstallBlock,
                        onLaunch = { intent ->
                            pendingCredentialRequest = DeviceCredentialRequest.AllowTemporaryUninstall
                            credentialLauncher.launch(intent)
                        },
                        onError = { message ->
                            securityMessage = message
                        }
                    )
                }
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

        if (!settings.onboardingCompleted) {
            OnboardingScreen(
                averageWeeklyMinutes = onboardingWeeklyMinutes,
                onAverageWeeklyMinutesChange = { onboardingWeeklyMinutes = it },
                onComplete = {
                    scope.launch {
                        repository.completeOnboarding(onboardingWeeklyMinutes)
                    }
                }
            )
            return@Surface
        }

        LazyColumn(
            state = dashboardListState,
            modifier = Modifier
                .fillMaxSize()
                .background(Carbon)
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 28.dp,
                end = 20.dp,
                bottom = 44.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Header(
                    dataSelected = selectedTab == AppDashboardTab.Data,
                    onDataClick = {
                        selectedTab = if (selectedTab == AppDashboardTab.Data) {
                            AppDashboardTab.Home
                        } else {
                            AppDashboardTab.Data
                        }
                    }
                )
            }

            item {
                DashboardTabSwitcher(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            if (selectedTab == AppDashboardTab.Home) {
                item {
                    HomeDashboard(
                        settings = settings,
                        currentTimeMillis = currentTimeMillis
                    )
                }
            } else if (selectedTab == AppDashboardTab.ProtectedApps) {
                item {
                    ProtectedAppsPanel(
                        settings = settings,
                        expandedApp = expandedProtectedApp,
                        onExpandedAppChange = { app ->
                            expandedProtectedApp = if (expandedProtectedApp == app) null else app
                        },
                        onTotalAppBlockChange = { app, enabled ->
                            scope.launch { repository.setTotalAppBlock(app, enabled) }
                        },
                        onTotalWebsiteBlockChange = { app, enabled ->
                            scope.launch { repository.setTotalWebsiteBlock(app, enabled) }
                        },
                        onBlockReelsChange = { enabled ->
                            scope.launch { repository.setBlockInstagramReels(enabled) }
                        },
                        onInstagramReelsAllowanceEnabledChange = { enabled ->
                            scope.launch { repository.setInstagramReelsAllowanceEnabled(enabled) }
                        },
                        onInstagramReelsAllowanceMinutesChange = { minutes ->
                            scope.launch { repository.setInstagramReelsDailyAllowanceMinutes(minutes) }
                        },
                        onBlockYouTubeShortsWebsiteChange = { enabled ->
                            scope.launch { repository.setBlockYouTubeShortsWebsite(enabled) }
                        },
                        onAllowYouTubeFriendShortsChange = { enabled ->
                            scope.launch { repository.setAllowYouTubeFriendShorts(enabled) }
                        },
                        onBlockYouTubeShortsInAppChange = { enabled ->
                            scope.launch { repository.setBlockYouTubeShortsInApp(enabled) }
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
                        onBlockTikTokShortFormChange = { enabled ->
                            scope.launch { repository.setBlockTikTokShortForm(enabled) }
                        },
                        onBlockLinkedInShortFormChange = { enabled ->
                            scope.launch { repository.setBlockLinkedInShortForm(enabled) }
                        },
                        openLimitResetCountdown = openLimitResetCountdown
                    )
                }
            } else if (selectedTab == AppDashboardTab.Security) {
                item {
                    AppSecurityPanel(
                        settings = settings,
                        currentTimeMillis = currentTimeMillis,
                        uninstallProtectionStatus = uninstallProtectionStatus,
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
                        onLockDurationMinutesChange = { minutes ->
                            scope.launch { repository.setAppLockDurationMinutes(minutes) }
                        },
                        onEnableAndLock = {
                            val validationMessage = validatePinInputs(securityPin, securityPinConfirm)
                            if (validationMessage != null) {
                                securityMessage = validationMessage
                            } else {
                                scope.launch {
                                    repository.enableAppSecurity(
                                        pin = securityPin,
                                        durationMinutes = settings.appLockDurationMinutes
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
                        onProtectAppUninstallChange = { enabled ->
                            scope.launch {
                                repository.setProtectAppUninstall(enabled)
                                deviceOwnerPolicyController.syncPolicies(
                                    settings.copy(
                                        protectAppUninstall = enabled,
                                        uninstallBypassUntilMillis = if (enabled) {
                                            settings.uninstallBypassUntilMillis
                                        } else {
                                            0L
                                        }
                                    )
                                )
                            }
                        },
                        onTemporarilyAllowUninstall = {
                            requestDeviceCredentialForTemporaryUninstall(
                                context = context,
                                canManageUninstallBlock = uninstallProtectionStatus.canControlUninstallBlock,
                                onLaunch = { intent ->
                                    pendingCredentialRequest = DeviceCredentialRequest.AllowTemporaryUninstall
                                    credentialLauncher.launch(intent)
                                },
                                onError = { message ->
                                    securityMessage = message
                                }
                            )
                        },
                        onHideLauncherIconChange = { enabled ->
                            scope.launch {
                                repository.setHideLauncherIcon(enabled)
                            }
                        },
                        onOpenHiddenEntry = {
                            context.startActivity(appVisibilityController.createReopenIntent())
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
                    SettingsTrainingPanel(
                        latestSnapshot = latestSettingsSnapshot,
                        sampleCount = settingsTrainingCount,
                        message = settingsTrainingMessage,
                        filePath = trainingRepository.settingsSamplesFilePath,
                        onLabel = { label ->
                            val snapshot = latestSettingsSnapshot
                            if (snapshot == null) {
                                settingsTrainingMessage = "No Settings snapshot yet."
                            } else {
                                scope.launch {
                                    val result = trainingRepository.saveSettingsSample(label, snapshot)
                                    settingsTrainingCount = result.count
                                    settingsTrainingMessage = when {
                                        result.saved -> "Saved $label sample."
                                        result.conflictingLabel != null ->
                                            "This exact snapshot already exists as ${result.conflictingLabel}. Capture a different Settings screen before relabeling."
                                        result.duplicateLabel != null ->
                                            "That exact $label snapshot is already saved."
                                        else -> "Settings sample was not saved."
                                    }
                                }
                            }
                        }
                    )
                }

            }
        }
    }
}

private enum class AppDashboardTab(val title: String, val subtitle: String) {
    Home("Home", "Your progress"),
    ProtectedApps("Apps", "Protection rules"),
    Security("Security", "Lock and permissions"),
    Data("Data", "Training tools")
}

@Composable
private fun OnboardingScreen(
    averageWeeklyMinutes: Int,
    onAverageWeeklyMinutesChange: (Int) -> Unit,
    onComplete: () -> Unit
) {
    var step by rememberSaveable { mutableStateOf(0) }
    val titles = listOf(
        "Start with your real baseline.",
        "Use YouTube on the web.",
        "Keep Instagram in the app."
    )
    val descriptions = listOf(
        "Your answer powers the reclaimed-time estimate on Home. You can refine this model as Unreel grows.",
        "Unreel blocks the native YouTube app, then filters Shorts by their /shorts URL while regular web videos remain available.",
        "Instagram protection is strongest in the Android app, where Unreel can distinguish Reels, search grids, stories, and messages."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Carbon)
            .padding(horizontal = 26.dp, vertical = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Paper),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "U",
                        color = Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "UNREEL",
                        color = Paper,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "INITIAL SETUP",
                        color = Steel,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(
                text = "0${step + 1} / 03",
                color = SignalGreen,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(46.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(if (index == step) 2f else 1f)
                        .height(3.dp)
                        .background(
                            if (index <= step) SignalGreen else Graphite,
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = titles[step],
            color = Paper,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = descriptions[step],
            color = Steel,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(36.dp))

        when (step) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Ink, RoundedCornerShape(8.dp))
                        .border(1.dp, Graphite, RoundedCornerShape(8.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "AVERAGE WEEKLY SHORT-FORM TIME",
                        color = Steel,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = formatWeeklyMinutes(averageWeeklyMinutes),
                        color = Paper,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatDailyBaseline(averageWeeklyMinutes)} per day",
                        color = SignalCyan,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Slider(
                        value = averageWeeklyMinutes.toFloat(),
                        onValueChange = { value ->
                            onAverageWeeklyMinutesChange(
                                ((value / 30f).toInt() * 30).coerceIn(0, MAX_WEEKLY_MINUTES)
                            )
                        },
                        valueRange = 0f..MAX_WEEKLY_MINUTES.toFloat(),
                        steps = 139,
                        colors = SliderDefaults.colors(
                            thumbColor = SignalGreen,
                            activeTrackColor = SignalGreen,
                            inactiveTrackColor = Graphite
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0h", color = Steel, style = MaterialTheme.typography.labelMedium)
                        Text("70h", color = Steel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            1 -> SetupInstruction(
                index = "01",
                title = "Open youtube.com",
                detail = "Long-form viewing stays available. A Shorts URL is covered and reversed automatically.",
                accent = SignalRed
            )
            else -> SetupInstruction(
                index = "02",
                title = "Use the Instagram app",
                detail = "The native app gives Unreel the strongest surface-level detection and the fewest false positives.",
                accent = SignalCyan
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step > 0) {
                Button(
                    onClick = { step -= 1 },
                    modifier = Modifier.weight(0.4f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Graphite,
                        contentColor = Paper
                    )
                ) {
                    Text("Back")
                }
            }
            Button(
                onClick = {
                    if (step < 2) {
                        step += 1
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Paper,
                    contentColor = Ink
                )
            ) {
                Text(if (step < 2) "Continue" else "Enter Unreel")
            }
        }
    }
}

@Composable
private fun SetupInstruction(
    index: String,
    title: String,
    detail: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink, RoundedCornerShape(8.dp))
            .border(1.dp, Graphite, RoundedCornerShape(8.dp))
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = index,
            color = accent,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Paper,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = detail, color = Steel)
        }
    }
}

@Composable
private fun HomeDashboard(
    settings: AppSettings,
    currentTimeMillis: Long
) {
    val totalBlocks = settings.totalProtectionCount()
    val timeSavedMillis = settings.estimatedTimeSavedMillis(currentTimeMillis)
    val activeDays = (
        (currentTimeMillis - settings.onboardingCompletedAtMillis).coerceAtLeast(0L) /
            DAY_IN_MILLIS
        ) + 1L

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Paper, RoundedCornerShape(8.dp))
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ATTENTION RECLAIMED",
                    color = Ink.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "DAY $activeDays",
                    color = SignalRed,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = formatDurationCompact(timeSavedMillis),
                color = Ink,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Estimated from your ${formatWeeklyMinutes(settings.averageWeeklyShortFormMinutes)} weekly baseline.",
                color = Ink.copy(alpha = 0.64f),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Ink.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            (totalBlocks / HOME_PROGRESS_BLOCK_TARGET.toFloat()).coerceIn(0.04f, 1f)
                        )
                        .height(4.dp)
                        .background(SignalGreen, RoundedCornerShape(4.dp))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeMetric(
                label = "BLOCKS",
                value = totalBlocks.toString(),
                detail = "all interventions",
                accent = SignalGreen,
                modifier = Modifier.weight(1f)
            )
            HomeMetric(
                label = "EXPOSURE",
                value = formatDurationCompact(settings.observedShortFormMillis),
                detail = "short-form observed",
                accent = SignalCyan,
                modifier = Modifier.weight(1f)
            )
        }

        ProtectionBreakdown(settings)
        ScreenTimeBreakdown(settings)
    }
}

@Composable
private fun HomeMetric(
    label: String,
    value: String,
    detail: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Ink, RoundedCornerShape(8.dp))
            .border(1.dp, Graphite, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp, 3.dp)
                .background(accent, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(label, color = Steel, style = MaterialTheme.typography.labelMedium)
        Text(
            text = value,
            color = Paper,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(detail, color = Steel, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ProtectionBreakdown(settings: AppSettings) {
    val rows = listOf(
        Triple("Instagram Reels", settings.instagramReelsBlockedCount, SignalGreen),
        Triple("Instagram Search", settings.instagramSearchGridBlockedCount, SignalRed),
        Triple("YouTube app", settings.youtubeAppBlockedCount, SignalCyan),
        Triple("YouTube Shorts", settings.youtubeShortsBlockedCount, SignalRed)
    )
    val maxCount = rows.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink, RoundedCornerShape(8.dp))
            .border(1.dp, Graphite, RoundedCornerShape(8.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "PROTECTION BREAKDOWN",
            color = Steel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        rows.forEach { (label, count, accent) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Paper, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = count.toString(),
                        color = Paper,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Graphite, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                if (count == 0) 0f else count / maxCount.toFloat()
                            )
                            .height(3.dp)
                            .background(accent, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenTimeBreakdown(settings: AppSettings) {
    val rows = ScreenTimeCategory.entries
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink, RoundedCornerShape(8.dp))
            .border(1.dp, Graphite, RoundedCornerShape(8.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SCREEN TIME",
            color = Steel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Observed while Unreel could identify a supported app or website.",
            color = Steel,
            style = MaterialTheme.typography.bodySmall
        )
        rows.forEach { category ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (category.isWebsite) SignalCyan else SignalGreen)
                    )
                    Text(category.displayName, color = Paper, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = formatDurationCompact(settings.screenTimeMillis(category)),
                    color = Paper,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BestSetupStatus(settings: AppSettings) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "BEST SETUP",
            color = Steel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        SetupStatusRow(
            title = "YouTube on the web",
            detail = "Native app entry is blocked",
            active = settings.blockYouTubeApp
        )
        SetupStatusRow(
            title = "Instagram in the app",
            detail = "Instagram website is blocked",
            active = settings.blockInstagramWebsite
        )
    }
}

@Composable
private fun SetupStatusRow(
    title: String,
    detail: String,
    active: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink, RoundedCornerShape(8.dp))
            .border(1.dp, Graphite, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(
                    if (active) SignalGreen else Steel,
                    RoundedCornerShape(5.dp)
                )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Paper, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Steel, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            text = if (active) "READY" else "OFF",
            color = if (active) SignalGreen else Steel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LockedAppScreen(
    lockedUntilMillis: Long,
    currentTimeMillis: Long,
    uninstallProtectionStatus: UninstallProtectionStatus,
    onTemporarilyAllowUninstall: () -> Unit
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
                if (uninstallProtectionStatus.protectionEnabled) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onTemporarilyAllowUninstall,
                        enabled = uninstallProtectionStatus.canControlUninstallBlock,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Graphite,
                            contentColor = Paper,
                            disabledContainerColor = Graphite.copy(alpha = 0.45f),
                            disabledContentColor = Steel
                        )
                    ) {
                        Text(
                            if (uninstallProtectionStatus.uninstallBypassActive) {
                                "Uninstall already allowed"
                            } else {
                                "Allow uninstall with phone credential"
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uninstallProtectionStatus.canControlUninstallBlock) {
                            "This opens a short uninstall window after a phone PIN, pattern, or password check."
                        } else {
                            "Uninstall protection needs device-owner provisioning before it can be enforced."
                        },
                        color = Steel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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
    uninstallProtectionStatus: UninstallProtectionStatus,
    pin: String,
    pinConfirm: String,
    message: String?,
    onPinChange: (String) -> Unit,
    onPinConfirmChange: (String) -> Unit,
    onLockDurationMinutesChange: (Int) -> Unit,
    onEnableAndLock: () -> Unit,
    onUpdatePin: () -> Unit,
    onLockNow: () -> Unit,
    onProtectAppUninstallChange: (Boolean) -> Unit,
    onTemporarilyAllowUninstall: () -> Unit,
    onHideLauncherIconChange: (Boolean) -> Unit,
    onOpenHiddenEntry: () -> Unit,
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

        LockDurationRow(
            label = "Lock Duration",
            detail = "How long Unreel stays inaccessible after you lock it",
            minutes = settings.appLockDurationMinutes,
            accent = SignalCyan,
            onValueChange = onLockDurationMinutesChange
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
        Text(
            text = "LOCK PROTECTIONS",
            color = Steel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        ToggleRow(
            label = "Block Uninstall",
            detail = when {
                uninstallProtectionStatus.canControlUninstallBlock && uninstallProtectionStatus.uninstallBypassActive ->
                    "Temporarily unlocked for removal"
                uninstallProtectionStatus.canControlUninstallBlock ->
                    "Requires phone credential before removal"
                else ->
                    "Requires device-owner setup to enforce"
            },
            checked = settings.protectAppUninstall,
            accent = SignalRed,
            onCheckedChange = onProtectAppUninstallChange,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                uninstallProtectionStatus.isDeviceOwner ->
                    "Device-owner control is active."
                uninstallProtectionStatus.isDeviceAdminActive ->
                    "Device admin is active, but uninstall blocking still needs full device-owner provisioning."
                else ->
                    "Regular installs cannot block uninstall. Provision Unreel as the device owner to turn this on for real."
            },
            color = Steel,
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onTemporarilyAllowUninstall,
            enabled = settings.protectAppUninstall && uninstallProtectionStatus.canControlUninstallBlock,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SignalRed,
                contentColor = Ink,
                disabledContainerColor = Graphite.copy(alpha = 0.45f),
                disabledContentColor = Steel
            )
        ) {
            Text(
                if (uninstallProtectionStatus.uninstallBypassActive) {
                    "Uninstall Open for 2 Minutes"
                } else {
                    "Allow Uninstall with Phone Credential"
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        ToggleRow(
            label = "Hide App Icon",
            detail = if (settings.hideLauncherIcon) {
                "Launcher icon hidden. Reopen with unreel://open"
            } else {
                "Remove Unreel from the launcher and app drawer"
            },
            checked = settings.hideLauncherIcon,
            accent = SignalCyan,
            onCheckedChange = onHideLauncherIconChange,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (settings.hideLauncherIcon) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Reopen path: unreel://open",
                color = Steel,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onOpenHiddenEntry,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Graphite,
                    contentColor = Paper
                )
            ) {
                Text("Test Hidden Entry")
            }
        }

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
private fun Header(
    dataSelected: Boolean,
    onDataClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Paper),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.unreel_launcher_mark),
                    contentDescription = "Unreel",
                    modifier = Modifier.size(40.dp)
                )
            }
            Column {
                Text(
                    text = "Unreel",
                    color = Paper,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Attention protection",
                    color = Steel,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (dataSelected) "Close data" else "Data",
                modifier = Modifier
                    .clickable(onClick = onDataClick)
                    .padding(top = 7.dp, start = 8.dp, bottom = 4.dp),
                color = if (dataSelected) SignalGreen else Steel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DashboardTabSwitcher(
    selectedTab: AppDashboardTab,
    onTabSelected: (AppDashboardTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Graphite,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AppDashboardTab.entries.filterNot { it == AppDashboardTab.Data }.forEach { tab ->
            val active = tab == selectedTab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) Paper else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 6.dp, vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Ink else Steel,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
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
private fun ProtectedAppsPanel(
    settings: AppSettings,
    expandedApp: ProtectedApp?,
    onExpandedAppChange: (ProtectedApp) -> Unit,
    onTotalAppBlockChange: (ProtectedApp, Boolean) -> Unit,
    onTotalWebsiteBlockChange: (ProtectedApp, Boolean) -> Unit,
    onBlockReelsChange: (Boolean) -> Unit,
    onInstagramReelsAllowanceEnabledChange: (Boolean) -> Unit,
    onInstagramReelsAllowanceMinutesChange: (Int) -> Unit,
    onBlockYouTubeShortsWebsiteChange: (Boolean) -> Unit,
    onAllowYouTubeFriendShortsChange: (Boolean) -> Unit,
    onBlockYouTubeShortsInAppChange: (Boolean) -> Unit,
    onGrayscaleInstagramAppChange: (Boolean) -> Unit,
    onLimitInstagramOpensPerDayChange: (Boolean) -> Unit,
    onInstagramDailyOpenLimitChange: (Int) -> Unit,
    onLimitInstagramToScheduleChange: (Boolean) -> Unit,
    onInstagramAccessScheduleChange: (List<DailyScheduleWindow>) -> Unit,
    onBlockHomeFeedChange: (Boolean) -> Unit,
    onBlockHomeStoriesChange: (Boolean) -> Unit,
    onAllowStoriesChange: (Boolean) -> Unit,
    onBlockSearchGridChange: (Boolean) -> Unit,
    onAllowFriendReelsChange: (Boolean) -> Unit,
    onReverseFromReelChange: (Boolean) -> Unit,
    onBlockTikTokShortFormChange: (Boolean) -> Unit,
    onBlockLinkedInShortFormChange: (Boolean) -> Unit,
    openLimitResetCountdown: String
) {
    val context = LocalContext.current
    val installedApps = ProtectedApp.entries.filter { app ->
        app.packageNames.any { packageName ->
            runCatching { context.packageManager.getApplicationInfo(packageName, 0) }.isSuccess
        }
    }
    val unavailableApps = ProtectedApp.entries - installedApps.toSet()
    var showUnavailableApps by rememberSaveable { mutableStateOf(false) }
    val scheduleWindows = settings.instagramAccessSchedule.ifEmpty {
        InstagramAccessScheduleCodec.defaultWindows()
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Protected apps",
            color = Paper,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose targeted protection or close an app and its website completely.",
            color = Steel,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (installedApps.isEmpty()) {
            Text(
                text = "No protected apps installed on this device.",
                color = Steel,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        @Composable
        fun AppCard(app: ProtectedApp) {
            val expanded = expandedApp == app
            Panel(borderColor = if (expanded) Paper.copy(alpha = 0.18f) else Graphite) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedAppChange(app) },
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(appAccent(app).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ProtectedAppIcon(app)
                        }
                        Column {
                            Text(app.displayName, color = Paper, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = protectedAppSummary(app, settings),
                                color = Steel,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = if (expanded) "Close" else "Manage",
                        color = appAccent(app),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Graphite))
                    Spacer(modifier = Modifier.height(6.dp))
                    ToggleRow(
                        label = "Total app block",
                        detail = "Immediately returns Home whenever ${app.displayName} opens",
                        checked = settings.isTotalAppBlocked(app),
                        accent = SignalRed,
                        onCheckedChange = { onTotalAppBlockChange(app, it) }
                    )
                    ToggleRow(
                        label = "Total website block",
                        detail = "Blocks loaded ${app.domains.first()} pages",
                        checked = settings.isTotalWebsiteBlocked(app),
                        accent = SignalRed,
                        onCheckedChange = { onTotalWebsiteBlockChange(app, it) }
                    )

                    when (app) {
                        ProtectedApp.INSTAGRAM -> {
                            ToggleRow("Block Reels", "High-confidence Reels detector", settings.blockInstagramReels, SignalGreen, onBlockReelsChange)
                            ToggleRow(
                                "Daily Reels allowance",
                                "Use a timed Reel window before blocking; resets at local midnight",
                                settings.instagramReelsAllowanceEnabled,
                                SignalCyan,
                                onInstagramReelsAllowanceEnabledChange
                            )
                            if (settings.instagramReelsAllowanceEnabled) {
                                val remainingMinutes =
                                    ((settings.instagramReelsAllowanceRemainingMillis() + 59_999L) / 60_000L).toInt()
                                StepperRow(
                                    "Allowance minutes",
                                    "$remainingMinutes min remaining today",
                                    settings.instagramReelsDailyAllowanceMinutes,
                                    SignalCyan,
                                    onInstagramReelsAllowanceMinutesChange
                                )
                            }
                            ToggleRow("Block Home Feed", "Stories and navigation remain available", settings.blockInstagramHomeFeed, SignalGreen, onBlockHomeFeedChange)
                            ToggleRow("Block Home Stories", "Covers stories with the feed", settings.blockInstagramHomeStories, SignalRed, onBlockHomeStoriesChange)
                            ToggleRow("Block Search Grid", "Blocks the mini-Reels explore grid", settings.blockInstagramSearchGrid, SignalRed, onBlockSearchGridChange)
                            ToggleRow("Reverse from blocked content", "Returns to the previous Instagram screen", settings.reverseFromReel, SignalCyan, onReverseFromReelChange)
                            ToggleRow("Allow friend Reels", "Allows Reels shared in messages", settings.allowInstagramReelsFromFriends, SignalGreen, onAllowFriendReelsChange)
                            ToggleRow("Allow Stories", "Keeps the story viewer available", settings.allowInstagramStories, SignalCyan, onAllowStoriesChange)
                            ToggleRow("App grayscale", "Makes Instagram monochrome", settings.grayscaleInstagramApp, SignalCyan, onGrayscaleInstagramAppChange)
                            ToggleRow(
                                "Daily open limit",
                                "${settings.instagramOpensToday()}/${settings.instagramDailyOpenLimit} used; resets in $openLimitResetCountdown",
                                settings.limitInstagramOpensPerDay,
                                SignalRed,
                                onLimitInstagramOpensPerDayChange
                            )
                            if (settings.limitInstagramOpensPerDay) {
                                StepperRow("Allowed opens", "Instagram sessions per day", settings.instagramDailyOpenLimit, SignalRed, onInstagramDailyOpenLimitChange)
                            }
                            ToggleRow("Scheduled access", formatInstagramScheduleDetail(settings), settings.limitInstagramToSchedule, SignalCyan, onLimitInstagramToScheduleChange)
                            if (settings.limitInstagramToSchedule) {
                                InstagramScheduleEditor(scheduleWindows, onInstagramAccessScheduleChange)
                            }
                        }
                        ProtectedApp.YOUTUBE -> {
                            ToggleRow(
                                "Block web Shorts",
                                "Blocks /shorts while regular YouTube remains available",
                                settings.blockYouTubeShortsWebsite,
                                SignalCyan,
                                onBlockYouTubeShortsWebsiteChange
                            )
                            ToggleRow(
                                "Allow friend Shorts",
                                "Allows the first Shorts page opened from another app or website",
                                settings.allowYouTubeFriendShorts,
                                SignalGreen,
                                onAllowYouTubeFriendShortsChange
                            )
                            ToggleRow(
                                "Block in-app Shorts",
                                "Keeps the YouTube app available while covering detected Shorts",
                                settings.blockYouTubeShortsInApp,
                                SignalRed,
                                onBlockYouTubeShortsInAppChange
                            )
                        }
                        ProtectedApp.TIKTOK -> {
                            ToggleRow(
                                "Messages only",
                                "Blocks detected vertical video while leaving inbox and chats available",
                                settings.blockTikTokShortForm,
                                SignalGreen,
                                onBlockTikTokShortFormChange
                            )
                        }
                        ProtectedApp.LINKEDIN -> {
                            ToggleRow(
                                "Block short videos",
                                "Blocks LinkedIn's immersive vertical-video feed",
                                settings.blockLinkedInShortForm,
                                SignalCyan,
                                onBlockLinkedInShortFormChange
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }

        installedApps.forEach { app -> AppCard(app) }

        if (unavailableApps.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Ink)
                    .border(1.dp, Graphite, RoundedCornerShape(8.dp))
                    .clickable { showUnavailableApps = !showUnavailableApps }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Available to add", color = Paper, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${unavailableApps.size} protected apps not installed",
                        color = Steel,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = if (showUnavailableApps) "Hide" else "Show",
                    color = SignalCyan,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (showUnavailableApps) {
                unavailableApps.forEach { app -> AppCard(app) }
            }
        }
    }
}

@Composable
private fun ProtectedAppIcon(app: ProtectedApp) {
    val context = LocalContext.current
    val drawable = remember(app) {
        app.packageNames.asSequence()
            .mapNotNull { packageName ->
                runCatching {
                    val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    context.packageManager.getApplicationIcon(applicationInfo)
                }.getOrNull()
            }
            .firstOrNull()
    }

    if (drawable == null) {
        Text(
            text = app.displayName.take(1),
            color = appAccent(app),
            fontWeight = FontWeight.Bold
        )
    } else {
        val bitmap = remember(drawable) { drawable.toBitmap() }
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = "${app.displayName} icon",
            modifier = Modifier.size(30.dp)
        )
    }
}

private fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    // Adaptive launcher drawables can report invalid intrinsic bounds. Draw them
    // into a stable bitmap so the row never receives a blank or 1px icon.
    val size = 96
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        setBounds(0, 0, size, size)
        draw(canvas)
    }
}

private fun appAccent(app: ProtectedApp): Color = when (app) {
    ProtectedApp.INSTAGRAM, ProtectedApp.THREADS -> SignalGreen
    ProtectedApp.YOUTUBE, ProtectedApp.REDDIT -> SignalRed
    ProtectedApp.TIKTOK, ProtectedApp.X -> Paper
    ProtectedApp.SNAPCHAT, ProtectedApp.LINKEDIN -> SignalCyan
}

private fun protectedAppSummary(app: ProtectedApp, settings: AppSettings): String = when {
    settings.isTotalAppBlocked(app) && settings.isTotalWebsiteBlocked(app) -> "App and website closed"
    settings.isTotalAppBlocked(app) -> "App closed"
    settings.isTotalWebsiteBlocked(app) -> "Website closed"
    app == ProtectedApp.INSTAGRAM -> "Reels and feed controls"
    app == ProtectedApp.YOUTUBE -> "Shorts and app controls"
    app == ProtectedApp.TIKTOK && settings.blockTikTokShortForm -> "Messages-only protection"
    app == ProtectedApp.LINKEDIN && settings.blockLinkedInShortForm -> "Short-video protection"
    else -> "Protection ready"
}

@Composable
private fun RulePanel(
    settings: AppSettings,
    onBlockReelsChange: (Boolean) -> Unit,
    onBlockWebsiteChange: (Boolean) -> Unit,
    onBlockYouTubeAppChange: (Boolean) -> Unit,
    onBlockYouTubeShortsWebsiteChange: (Boolean) -> Unit,
    onGrayscaleInstagramAppChange: (Boolean) -> Unit,
    onLimitInstagramOpensPerDayChange: (Boolean) -> Unit,
    onInstagramDailyOpenLimitChange: (Int) -> Unit,
    onLimitInstagramToScheduleChange: (Boolean) -> Unit,
    onInstagramAccessScheduleChange: (List<DailyScheduleWindow>) -> Unit,
    onBlockHomeFeedChange: (Boolean) -> Unit,
    onBlockHomeStoriesChange: (Boolean) -> Unit,
    onAllowStoriesChange: (Boolean) -> Unit,
    onBlockSearchGridChange: (Boolean) -> Unit,
    onAllowFriendReelsChange: (Boolean) -> Unit,
    onReverseFromReelChange: (Boolean) -> Unit,
    openLimitResetCountdown: String,
    showInstagramSettings: Boolean,
    onInstagramSettingsClick: () -> Unit,
    showYouTubeSettings: Boolean,
    onYouTubeSettingsClick: () -> Unit
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
                    text = "Edit",
                    color = if (showInstagramSettings) SignalGreen else Paper,
                    style = MaterialTheme.typography.labelLarge
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

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Spacer(modifier = Modifier.height(16.dp))

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
                        .background(SignalRed)
                )
                Column {
                    Text("YouTube", color = Paper, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (showYouTubeSettings) {
                            "Sub-settings open"
                        } else {
                            "App blocked, long-form web available"
                        },
                        color = Steel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onYouTubeSettingsClick) {
                Text(
                    text = "Edit",
                    color = if (showYouTubeSettings) SignalRed else Paper,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (showYouTubeSettings) {
            Spacer(modifier = Modifier.height(14.dp))
            ToggleRow(
                label = "Block YouTube App",
                detail = "Returns Home whenever the native YouTube app opens",
                checked = settings.blockYouTubeApp,
                accent = SignalRed,
                onCheckedChange = onBlockYouTubeAppChange
            )
            ToggleRow(
                label = "Block Web Shorts",
                detail = "Blocks loaded youtube.com/shorts pages only",
                checked = settings.blockYouTubeShortsWebsite,
                accent = SignalCyan,
                onCheckedChange = onBlockYouTubeShortsWebsiteChange
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
private fun SettingsTrainingPanel(
    latestSnapshot: WindowSnapshot?,
    sampleCount: Int,
    message: String,
    filePath: String,
    onLabel: (String) -> Unit
) {
    val labels = listOf(
        "UNINSTALL_UNREEL",
        "ACCESSIBILITY_BLOCKER",
        "SETTINGS_SAFE",
        "UNKNOWN"
    )

    Panel(borderColor = SignalRed.copy(alpha = 0.45f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Settings guard training", color = Paper, style = MaterialTheme.typography.titleMedium)
                Text(message, color = Steel, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("$sampleCount saved", color = SignalRed, style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (latestSnapshot == null) {
            Text("Latest Settings snapshot: none", color = Steel)
        } else {
            Text(
                text = "Latest: ${latestSnapshot.packageName}, ${latestSnapshot.nodeCount} nodes, ${latestSnapshot.viewIds.size} ids",
                color = Steel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        labels.chunked(2).forEach { rowLabels ->
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
                                "UNINSTALL_UNREEL" -> SignalRed
                                "ACCESSIBILITY_BLOCKER" -> SignalGreen
                                "SETTINGS_SAFE" -> SignalCyan
                                else -> Graphite
                            },
                            contentColor = if (label == "UNKNOWN") Paper else Ink,
                            disabledContainerColor = Graphite.copy(alpha = 0.35f),
                            disabledContentColor = Steel
                        )
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(2 - rowLabels.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(2.dp))
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
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 14.dp)) {
            Text(label, color = Paper, fontWeight = FontWeight.SemiBold)
            Text(
                detail,
                color = Steel,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
private fun LockDurationRow(
    label: String,
    detail: String,
    minutes: Int,
    accent: Color,
    onValueChange: (Int) -> Unit
) {
    var selectedUnitName by rememberSaveable { mutableStateOf(LockDurationUnit.HOURS.name) }
    val selectedUnit = LockDurationUnit.valueOf(selectedUnitName)
    val safeMinutes = minutes.coerceIn(MIN_LOCK_DURATION_MINUTES, MAX_LOCK_DURATION_MINUTES)

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(label, color = Paper, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Steel, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Text(formatLockDurationMinutes(safeMinutes), color = Paper, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LockDurationUnit.entries.forEach { unit ->
                Button(
                    onClick = { selectedUnitName = unit.name },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (unit == selectedUnit) accent else Graphite,
                        contentColor = if (unit == selectedUnit) Ink else Paper
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                ) {
                    Text(unit.label)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onValueChange((safeMinutes - selectedUnit.stepMinutes).coerceAtLeast(MIN_LOCK_DURATION_MINUTES)) },
                colors = ButtonDefaults.buttonColors(containerColor = Graphite, contentColor = Paper),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) { Text("-") }
            Text(
                text = "${selectedUnit.label} increments",
                color = Steel,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(2f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = { onValueChange((safeMinutes + selectedUnit.stepMinutes).coerceAtMost(MAX_LOCK_DURATION_MINUTES)) },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) { Text("+") }
        }
    }
}

private enum class LockDurationUnit(val label: String, val stepMinutes: Int) {
    MINUTES("Minutes", 1),
    HOURS("Hours", 60),
    DAYS("Days", 24 * 60)
}

private fun formatLockDurationMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % (24 * 60) == 0 -> "${minutes / (24 * 60)}d"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
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
            focusedLabelColor = SignalGreen,
            unfocusedLabelColor = Steel,
            focusedIndicatorColor = SignalGreen,
            unfocusedIndicatorColor = Graphite,
            cursorColor = SignalGreen
        )
    )
}

@Composable
private fun Panel(
    borderColor: Color = Graphite,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Ink)
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

private fun formatWeeklyMinutes(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun formatDailyBaseline(weeklyMinutes: Int): String {
    return formatWeeklyMinutes((weeklyMinutes / 7f).toInt())
}

private fun formatDurationCompact(durationMillis: Long): String {
    val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
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
private const val MAX_WEEKLY_MINUTES = 70 * 60
private const val HOME_PROGRESS_BLOCK_TARGET = 100
private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
private const val MIN_LOCK_DURATION_MINUTES = 1
private const val MAX_LOCK_DURATION_MINUTES = 30 * 24 * 60
private const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 8
private const val APP_UNINSTALL_BYPASS_DURATION_MILLIS = 2L * 60L * 1000L

private enum class DeviceCredentialRequest {
    AllowTemporaryUninstall
}

@Suppress("DEPRECATION")
private fun requestDeviceCredentialForTemporaryUninstall(
    context: Context,
    canManageUninstallBlock: Boolean,
    onLaunch: (Intent) -> Unit,
    onError: (String) -> Unit
) {
    if (!canManageUninstallBlock) {
        onError("Uninstall protection needs device-owner provisioning first.")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onError("Couldn't open the phone credential prompt.")
        return
    }
    val keyguardManager = activity.getSystemService(KeyguardManager::class.java)
    if (keyguardManager?.isDeviceSecure != true) {
        onError("Set a phone PIN, pattern, or password first.")
        return
    }
    val intent = keyguardManager.createConfirmDeviceCredentialIntent(
        "Allow uninstall",
        "Use your phone credential to temporarily allow Unreel to be removed."
    )
    if (intent == null) {
        onError("Phone credential confirmation isn't available on this device.")
        return
    }
    onLaunch(intent)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
