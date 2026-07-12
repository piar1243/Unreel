# Unreel

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/unreel_launcher_mark.png" width="120" alt="Unreel logo" />
</p>

<p align="center"><strong>Make room for the things you meant to do.</strong></p>

<p align="center">
  Unreel is an Android short-form content blocker that uses event-driven accessibility detection to interrupt Reels, Shorts, and other high-distraction surfaces while keeping intentional parts of supported apps available.
</p>

<p align="center">
  <a href="#features">Features</a> &bull;
  <a href="#supported-platforms">Supported platforms</a> &bull;
  <a href="#setup">Setup</a> &bull;
  <a href="#architecture">Architecture</a> &bull;
  <a href="#troubleshooting">Troubleshooting</a>
</p>

![Android](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Status](https://img.shields.io/badge/status-active%20development-111111)

## Overview

Unreel is designed for people who want to keep useful parts of social platforms without being pulled into endless short-form feeds.

The app observes the active Android window, reads the visible accessibility tree, classifies supported surfaces, and applies a targeted intervention when a rule matches. Depending on the setting, an intervention can:

- Cover only the content region while leaving navigation available.
- Mute media while a blocker is active.
- Return to the previous screen with Android's Back action.
- Return Home for a total app block.
- Allow an intentional exception, such as a Reel or Short opened from a message.

Detection is event-driven. Unreel does not rely on an always-running high-frequency polling loop.

> **Current status:** Unreel is an active personal project and is not yet a general-purpose production release. Android UI trees and browser behavior can change between app versions, so supported detectors should be validated on the target device and app versions.

## Features

### Home dashboard

The Home tab provides a compact progress view with:

- Estimated time saved based on the onboarding baseline and observed short-form exposure.
- Total blocked events.
- Short-form exposure observed by Unreel.
- Platform activity summaries.
- Current protection status.

The time-saved value is an estimate. It is not a replacement for Android's device-wide screen-time measurement.

### Instagram controls

Instagram has the most complete targeted protection set in the app:

- **Block Reels:** Blocks high-confidence Instagram Reels while keeping normal areas available.
- **Reverse from blocked content:** Uses Android's Back action to leave a blocked Reel or Search Grid.
- **Allow friend Reels:** Allows Reels identified as coming through messages or friends.
- **Block Home Feed:** Covers the central feed region while preserving Stories and bottom navigation when possible.
- **Block Home Stories:** Adds a separate opaque blocker over the Stories region.
- **Block Search Grid:** Blocks the mini-Reels grid on Search or Explore without blocking all Search.
- **Allow Stories:** Keeps the Story viewer available when other Instagram protections are enabled.
- **App grayscale:** Applies grayscale while Instagram is active and restores normal color when leaving it.
- **Daily open limit:** Limits the number of Instagram sessions per local calendar day.
- **Scheduled access:** Allows Instagram only during configured daily windows using the device's current local timezone.
- **Total app block:** Returns Home whenever the Instagram app opens.
- **Total website block:** Blocks Instagram website access in supported browsers.

### YouTube controls

YouTube protections are independent from Instagram settings:

- **Total app block:** Prevents entry to the native YouTube app and returns Home.
- **Block web Shorts:** Blocks loaded YouTube `/shorts` pages while regular web videos remain available.
- **Block in-app Shorts:** Keeps the YouTube app usable while covering detected Shorts player surfaces.
- **Allow friend Shorts:** Allows the first Shorts page opened after entering from another app or website, then resumes normal blocking.
- **Total website block:** Blocks supported YouTube website access when enabled.

For targeted native Shorts protection, disable **Total app block** and enable **Block in-app Shorts**.

### TikTok controls

TikTok currently supports:

- **Total app block.**
- **Total website block.**
- **Short-form blocking:** Covers detected vertical-video surfaces while leaving the Messages area available.
- **Audio suppression:** Mutes TikTok media while the short-form blocker is active, including volume-button attempts while the blocker remains visible.

### Other protected apps

Snapchat, X, Threads, Reddit, and LinkedIn currently share the common protection model:

- Total app block.
- Total website block.

Their platform-specific content detectors can be added without changing the shared protected-app settings model.

The Apps tab separates installed protected apps from apps that are not installed on the device. Launcher icons are resolved from the installed package when Android exposes it.

## Supported platforms

| Platform | Native app protection | Website protection | Targeted content protection |
| --- | --- | --- | --- |
| Instagram | Yes | Yes | Reels, friend Reels, Search Grid, Home Feed, Stories |
| YouTube | Yes | Yes | Shorts in app and web, friend Shorts exception |
| TikTok | Yes | Yes | Short-form surfaces with Messages available |
| Snapchat | Yes | Yes | Total block only |
| X | Yes | Yes | Total block only |
| Threads | Yes | Yes | Total block only |
| Reddit | Yes | Yes | Total block only |
| LinkedIn | Yes | Yes | Total block only |

Website behavior depends on the browser exposing the active URL or visible address field through accessibility. Browser UI differs across Chrome, Brave, Samsung Internet, and custom tabs.

## How it works

1. Android sends an accessibility event for a window, content change, click, scroll, or related interaction.
2. `WeLiveAccessibilityService` selects the most relevant active window and safely reads its accessibility nodes.
3. `AccessibilityEventRouter` routes the snapshot to the platform detectors.
4. A detector returns a `DetectionResult` containing the platform, surface, confidence, reasons, package name, and recommended action.
5. The router applies the appropriate intervention and clears it when the user leaves the protected surface.

The detection and intervention layers are intentionally separated. This allows platform classifiers to evolve independently from overlays, audio control, navigation, and settings persistence.

## Permissions and security model

Unreel requires elevated Android capabilities because it must observe and intervene in other apps:

### Accessibility service

The Accessibility Service is required for detection and blocking. It reads visible UI labels, content descriptions, view IDs, window structure, browser address fields, and interaction events. It is also used to perform Android's Back action and targeted gestures where needed.

Enable it from:

**Android Settings > Accessibility > Installed apps > Unreel**

### Overlay behavior

Unreel uses accessibility overlays where possible. Some interventions are full-screen, while Home Feed and website interventions are regional so system navigation or intentional navigation controls can remain available.

### App lock

The Security tab supports:

- PIN-protected settings access.
- Lock durations in hours or days.
- Lock Now using the configured duration.
- A hidden launcher icon option.
- Uninstall protection controls.
- A narrowly targeted guard for Unreel's Accessibility permission page.

### Device-owner support

When provisioned as a device owner or profile owner, Unreel can ask Android's device policy manager to block its own uninstall. Device-owner provisioning is an Android management operation and normally requires a managed or reset test device. This feature is not equivalent to an unbreakable security boundary and should not be used as a substitute for the phone's own PIN or enterprise management.

## Data and privacy

Unreel has no server component in this repository and does not require an account. Settings are persisted locally with DataStore. Detector training samples and debug recordings are written to the app's private files directory as JSONL files.

Training captures are intentionally reduced for tuning:

- Known UI terms are retained as normalized tokens.
- Other visible text is represented by a redacted length token.
- Node structure, view IDs, classes, bounds, and interaction flags are retained for classifier work.

The Data tab is hidden behind the small data control in the top navigation. It contains:

- Instagram surface capture for Reels, friend Reels, Search Grid, Home Feed, Following, Posts, Stories, DMs, Profile, and Post Creator.
- Settings-page capture for uninstall and Accessibility permission guards.
- Home Debug Recorder for event sequences while moving between Instagram surfaces.
- Recent diagnostics with surface, confidence, package, reasons, and action.

These tools are intended for development and false-positive investigation. They are not required for normal use.

## Setup

### Requirements

- Android Studio with the Android SDK installed.
- Android 12 or newer device or emulator.
- JDK 11-compatible Android build environment.
- A physical test device is recommended for Accessibility, overlay, audio, browser, and device-owner behavior.
- The app's current package ID is `com.example.welive`.

### Open the project

Use the `Unreel` folder as the canonical project source:

```text
C:\Users\frank\AndroidStudioProjects\Unreel
```

Open that folder in Android Studio. If editing in VS Code, make sure Android Studio is also opened against the same folder.

### Build and test

From the project root:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Install it on a connected device with:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The `-r` flag preserves existing local settings. To test first-install defaults, uninstall the existing app or clear its app data before launching the new build.

### First launch

1. Complete the short onboarding flow and enter an estimate of weekly short-form usage.
2. Enable Unreel's Accessibility Service.
3. Open the Apps tab and select the protected app you want to configure.
4. Start with one targeted rule and validate it in the target app.
5. Use the Security tab only after the detection setup is working.

## Recommended configurations

### Targeted Instagram setup

- Keep **Block Reels** enabled.
- Enable **Reverse from blocked content** if you prefer leaving the surface instead of showing a blocker.
- Enable **Block Search Grid** if Explore's mini-Reels grid is distracting.
- Enable **Block Home Feed** if Stories are useful but the feed is not.
- Leave **Allow Stories** enabled when Stories should remain available.

### Targeted YouTube setup

- Disable **Total app block**.
- Enable **Block in-app Shorts**.
- Enable **Block web Shorts** if YouTube is used in a browser.
- Enable **Allow friend Shorts** only if shared Shorts should receive a one-entry exception.

### Cold-turkey setup

- Enable **Total app block** for the relevant apps.
- Enable **Total website block** for the corresponding domains.
- Configure the App Lock PIN and duration from Security.

## Architecture

```text
app/src/main/java/com/example/welive/
├── accessibility/
│   ├── AccessibilityEventRouter.kt
│   ├── WeLiveAccessibilityService.kt
│   └── WindowSnapshotReader.kt
├── detection/
│   ├── ContentDetector.kt
│   ├── DetectionModels.kt
│   ├── WindowSnapshot.kt
│   └── platforms/
│       ├── instagram/
│       ├── instagramweb/
│       ├── protectedweb/
│       ├── settings/
│       ├── tiktok/
│       └── youtube/
├── intervention/
│   ├── OverlayController.kt
│   ├── HomeFeedOverlayController.kt
│   ├── InstagramWebOverlayController.kt
│   ├── TikTokOverlayController.kt
│   └── YouTubeShortsOverlayController.kt
├── settings/
│   ├── AppSettings.kt
│   ├── AppSecurity.kt
│   ├── InstagramAccessSchedule.kt
│   └── UserRulesRepository.kt
├── diagnostics/
├── training/
├── deviceowner/
├── protection/
└── ui/
```

The UI is built with Jetpack Compose and Material 3 primitives with a custom dark visual system. State is collected from `UserRulesRepository`, while the Accessibility Service owns event processing and intervention lifecycles.

## Troubleshooting

### A blocker does not appear

- Confirm the Accessibility Service is enabled.
- Confirm the relevant setting is enabled in the Apps tab.
- For YouTube targeted blocking, make sure **Total app block** is disabled.
- Reopen or toggle the Accessibility Service after installing a build that changes its service configuration.

### A blocker remains after leaving the app

- Leave the protected surface using a normal navigation action and wait for the next accessibility window event.
- Confirm the app is running the newest APK from the `Unreel` folder.
- Check the Data tab diagnostics for the active package and surface.

### A setting change does not appear

- Confirm VS Code and Android Studio are using the same `Unreel` directory.
- Save the file, sync Gradle, rebuild, and run the `app` configuration again.
- Remember that reinstalling with `adb install -r` preserves DataStore settings.

### A false positive occurs

Open the Data tab and use Diagnostics or the relevant capture tool. Record the incorrect surface and the intended safe surface separately. Keep captures from the same app version and device UI when possible.

### Protected-app icons are missing

The Apps tab resolves icons from installed package metadata. Installed apps appear first; apps that are not installed are listed under **Available to add**. If an installed icon is still missing, rebuild and reinstall the current APK so the package-visibility declarations are applied.

## Contributing

When adding a new protected platform:

1. Add package and domain configuration.
2. Implement a focused detector that returns `DetectionResult`.
3. Add an intervention controller only when the shared overlay behavior is insufficient.
4. Keep settings isolated in `AppSettings` and `UserRulesRepository`.
5. Add unit tests for safe surfaces and false-positive boundaries.
6. Update `Documentation/FEATURES.md` and this README.

Detector changes should preserve the existing Instagram Reels, Instagram Search Grid, and YouTube Shorts behavior unless the change is explicitly scoped to those surfaces.

## License

No open-source license has been declared yet. Until a license is added to this repository, all rights are reserved by the project owner.
