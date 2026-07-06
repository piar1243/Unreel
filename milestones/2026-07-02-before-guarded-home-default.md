# 2026-07-02 Before Guarded Home Default

This milestone captures the current working state before trying the "home feed guarded by default" workaround.

## Current behavior

- Reels and search blockers are treated as stable and should not be changed by the next experiment.
- Home feed blocker works, but timing can lag when moving to or from Instagram Home.
- `Instant Home Block` setting exists and preloads the home feed blocker on Instagram app open.
- The instant blocker uses the same home-feed region resolver or cached normal home-feed geometry.
- Top app bar and bottom nav passthrough lanes exist for the home feed overlay.
- Pass-through taps currently suppress/clear the home overlay briefly, restored after reverting the scroll-lock experiment.

## Verified before this milestone

- `.\gradlew.bat assembleDebug` passed.
- `.\gradlew.bat testDebugUnitTest` passed.
- Debug APK was installed with `adb install -r`.

## Dirty working tree at milestone

- `app/src/main/java/com/example/welive/accessibility/AccessibilityEventRouter.kt`
- `app/src/main/java/com/example/welive/accessibility/WeLiveAccessibilityService.kt`
- `app/src/main/java/com/example/welive/detection/platforms/instagram/InstagramHomeFeedClassifier.kt`
- `app/src/main/java/com/example/welive/intervention/HomeFeedOverlayController.kt`
- `app/src/main/java/com/example/welive/settings/AppSettings.kt`
- `app/src/main/java/com/example/welive/settings/UserRulesRepository.kt`
- `app/src/main/java/com/example/welive/ui/WeLiveApp.kt`
- `app/src/test/java/com/example/welive/detection/platforms/instagram/InstagramHomeFeedClassifierTest.kt`

Untracked IDE/debug artifacts were present and intentionally left alone.

## Next experiment

Try recommendation #5 only:

- When Instagram is active and home feed blocking is enabled, treat the home feed as guarded by default.
- Keep the center feed blocked while the surface is unknown or likely home.
- Remove/replace the home blocker only after detecting a clear non-home surface such as stories, DMs, profile, Reels, search grid, browser, or other Instagram surface.
- Do not change Reels/Search detection or blocker behavior.
