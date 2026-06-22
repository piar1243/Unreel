# Stable Instagram Blockers Milestone

Date: 2026-06-20

This is the known-good point before adding `POSTS` training.

- Reels detection is high confidence and should not be changed casually.
- Search reels grid blocking is working.
- Home feed blocker is working again after removing the broad `for you` false Explore signal.
- Home feed blocker is opaque, mutes media while active, and uses faster home-feed event handling.
- Stories, DMs, and Profile are treated as clear surfaces for the home-feed blocker.
- Verified before this milestone with `testDebugUnitTest`, `assembleDebug`, and APK install.

Note: the workspace contains a `.git` directory, but `.git/HEAD` is missing in this environment, so this milestone is recorded as a local file instead of a Git tag.
