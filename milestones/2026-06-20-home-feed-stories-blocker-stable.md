# Stable Home Feed + Story Strip Blocker Milestone

Date: 2026-06-20

Known-good point before starting Instagram web blocking.

- Native Instagram Reels detection and reverse/block actions are working well.
- Native Instagram search reels grid blocker is working.
- Native Instagram home feed blocker is separate from Reels/Search blockers.
- Home feed blocker can leave stories tappable while preventing feed scrolling.
- New `Block Home Stories` setting can make the home-feed blocker cover stories with the same solid blocker color and reject taps/drags.
- Posts/following samples are treated as anti-block surfaces so friends' posts and following-style pages do not get mistaken for home feed.
- Verified with `testDebugUnitTest`, `assembleDebug`, and APK install.

Important guardrail: do not change the native Reels/Search detector path while working on Instagram web blocking unless explicitly requested.

Note: the workspace contains a `.git` directory, but `.git/HEAD` is missing in this environment, so this milestone is recorded as a local file instead of a Git tag.
