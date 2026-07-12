# Unreel Features

Unreel reduces access to short-form content and helps keep social apps intentional. It uses Android Accessibility Services to observe the active screen, classify supported surfaces, and place a blocker over content when a rule matches.

Accessibility is required because Unreel reads visible labels, controls, window structure, and browser address fields. Unreel does not need aggressive polling; detection is event-driven.

## Home

The Home tab shows the current protection state and progress, including:

- Estimated time saved
- Total blocked events
- Short-form exposure
- Platform activity summaries
- Whether protection is currently active

The time-saved estimate uses the onboarding baseline and observed short-form exposure. It is an estimate, not a device-wide screen-time measurement.

## Protected Apps

Open the Apps tab and select an app to expand its settings.

### Total App Block

Immediately prevents the native app from being used. The user is returned Home when the protected app opens.

Use this for a complete break from an app. For YouTube and TikTok, turn this off before using their targeted content blockers.

### Total Website Block

Blocks supported website access in supported browsers. This is separate from native-app protection.

## Instagram

Instagram has targeted controls as well as total app and website controls.

### Block Reels

Blocks high-confidence Instagram Reels. Normal Home, Profile, Direct Messages, Stories, and Search remain available unless their own settings are enabled.

### Reverse From Blocked Content

Uses Android's Back action after a Reel or Search Grid is detected. This returns to the screen that came before the blocked surface instead of leaving the whole app.

### Allow Friend Reels

Allows Reels identified as coming from messages or friends while continuing to block ordinary Reels.

### Allow Stories

Keeps Instagram Stories available when other Instagram protections are active.

### Block Home Feed

Covers the central Home Feed content while leaving Stories and bottom navigation available. This is useful when Stories are the only Home content you want to access.

### Block Home Stories

Adds a separate opaque block over the Stories area on the Instagram Home screen. When enabled, Story taps are not passed through.

### Block Search Grid

Blocks the mini-Reels grid on Instagram Search or Explore without blocking the rest of Instagram Search.

### App Grayscale

Applies grayscale while Instagram is active. The display returns to normal when leaving Instagram.

### Daily Open Limit

Limits the number of Instagram sessions that can start each day. The counter resets according to the device's local date.

### Scheduled Access

Allows Instagram only during configured daily windows. Schedule times use the device's current local timezone. Adjust the windows in the Instagram settings panel.

## YouTube

YouTube has independent native-app, web, and Shorts controls.

### Block YouTube App

Completely blocks the native YouTube app and returns Home. Turn this off if you want to use the targeted in-app Shorts blocker.

### Block Web Shorts

Blocks YouTube Shorts pages identified by a loaded `/shorts` URL while leaving ordinary YouTube web videos available.

### Allow Friend Shorts

When enabled, the first YouTube Shorts page opened after entering from another app or website is allowed. Later Shorts pages are blocked normally. This is intended for a Shorts link shared by a friend.

### Block In-App Shorts

Keeps the native YouTube app usable while covering high-confidence Shorts player screens. Normal videos, Home, Search, subscriptions, and other app areas remain available.

Recommended targeted setup:

1. Turn off **Block YouTube App**.
2. Turn on **Block In-App Shorts**.
3. Optionally turn on **Block Web Shorts** as well.

## TikTok

TikTok supports:

- Total app block
- Total website block
- Short-form blocking while keeping the Messages area available

The TikTok short-form blocker is a dedicated overlay that leaves the lower navigation area available. TikTok media is muted while the blocker is visible, including after volume-button presses.

## Snapchat, X, Threads, Reddit, and LinkedIn

These apps currently support the common controls:

- Total app block
- Total website block

Platform-specific content detectors can be added later without changing the shared protection model.

## Security

Open the Security tab for settings protection and permissions.

### App Lock

Set a PIN and choose a lock duration in hours or days. When enabled, Unreel requires the PIN before settings can be changed.

### Enable & Lock

Saves the PIN and starts the selected lock duration. During the lock period, settings are protected from casual changes.

### Lock Now

Immediately starts the configured lock duration using the existing PIN.

### Block Uninstall

Uses the Settings-page guard and, when available, device-owner policy to make uninstalling Unreel more difficult. This is not a substitute for Android device security.

### Hide App Icon

Removes the normal launcher entry when supported. Use the configured hidden entry or Android Settings to open Unreel again.

### Accessibility Service

The service must remain enabled for detection and blocking to work. Unreel has a narrowly targeted guard for the Unreel Accessibility permission page when protection is enabled.

## Data Tab

The Data tab is hidden behind the small data icon in the top navigation so normal settings stay uncluttered.

It contains tools for tuning detectors:

- Instagram training capture for Reels, friend Reels, Search Grid, Home Feed, Following, Posts, Stories, DMs, Profile, and Post Creator surfaces
- Settings guard capture for uninstall and accessibility-permission pages
- Home debug recorder for recording event sequences while moving between Instagram Home, Stories, Messages, and other tabs
- Diagnostics showing recent classifications, confidence, detected surface, package, and reasons

These tools are primarily for development and false-positive investigation. They are not required for ordinary use.

## Blocker Behavior

- Blockers are event-driven and use cooldowns or solid holds where needed to avoid flicker.
- Instagram Reels and Search Grid use the configured reverse behavior when enabled.
- Home Feed blocking is regional so Stories and navigation can remain available.
- Website blockers leave browser navigation areas available where possible.
- Leaving the target app or surface clears its overlay.
- Settings actions on a blocker open Unreel and dismiss the active intervention.

## Development and Installation

Use this folder as the single project source:

`C:\Users\frank\AndroidStudioProjects\Unreel`

After editing source in VS Code:

1. Save the file.
2. In Android Studio choose **File > Sync Project with Gradle Files**.
3. Choose **Build > Rebuild Project**.
4. Run the `app` configuration to install the new APK.

Command-line build:

```powershell
cd C:\Users\frank\AndroidStudioProjects\Unreel
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Installing with `adb install -r` preserves existing DataStore settings. Fresh-install defaults apply only when no previous settings data exists.

## Troubleshooting

### A blocker is not appearing

- Confirm the Accessibility Service is enabled.
- Confirm the relevant app or website toggle is on.
- For YouTube targeted blocking, make sure **Block YouTube App** is off.
- Reopen or toggle the Accessibility Service after installing a build that changes the service configuration.

### A setting change is not visible

- Confirm Android Studio and VS Code are both using the `Unreel` folder.
- Sync Gradle, rebuild, and run the app again.
- Do not expect an APK update to erase existing DataStore settings.

### A false positive occurs

Open the Data tab and use Diagnostics or the relevant recorder. Capture the incorrect surface and the correct surface separately so the detector can be tuned against both.
