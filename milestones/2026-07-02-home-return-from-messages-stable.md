# 2026-07-02 Home Return From Messages Stable

This milestone captures the improved Instagram Home return behavior after tightening the home navigation event handling.

## What is stable here

- Reels blocker behavior remains unchanged from the prior working state.
- Search grid blocker behavior remains unchanged from the prior working state.
- Home feed classifier has an earlier `HOME_FEED` path based on stable top chrome and story tray markers.
- Returning from Instagram Messages back to Home is much more reliable.
- The home blocker no longer depends on a small manual scroll or "wiggle" to reappear after returning to Home.

## Key change that made this better

- `feed_tab` re-entry now counts on both `TYPE_VIEW_CLICKED` and `TYPE_VIEW_SELECTED`.
- After a Home-tab navigation signal, stale `direct_tab` events are ignored for a short guard window so they do not keep re-suppressing the home blocker while Home is already visible.

## Files central to this milestone

- `app/src/main/java/com/example/welive/accessibility/AccessibilityEventRouter.kt`
- `app/src/main/java/com/example/welive/detection/platforms/instagram/InstagramHomeFeedClassifier.kt`
- `app/src/test/java/com/example/welive/detection/platforms/instagram/InstagramHomeFeedClassifierTest.kt`

## Verified at this point

- `.\gradlew.bat assembleDebug` passed.
- `.\gradlew.bat testDebugUnitTest` passed.
- Debug APK installed successfully with `adb install -r`.
