# Stable Instagram Blockers + App Open Limit Milestone

Date: 2026-06-20

Known-good point after adding the Instagram app open-limit gate.

- Native Instagram Reels, Search Grid, and Home Feed blockers are working and should not be changed casually.
- Instagram website blocking is working through its separate partial web overlay.
- App Grayscale is enabled through a separate service-side observer, not through the blocker router.
- Daily Open Limit is implemented as a separate service-side gate:
  - Allowed opens are counted per local date.
  - The last allowed open remains usable for that session.
  - After the allowed opens are used, launching native Instagram immediately sends the user Home.
  - The gate does not call the blocker router or draw a content blocker.
- The Instagram settings UI shows today's open count and a countdown until the daily count resets.

Important guardrail: keep app-level features like grayscale/open limits separate from the content detector/router path unless explicitly requested.

Note: the workspace contains a `.git` directory, but `.git/HEAD` is missing in this environment, so this milestone is recorded as a local file instead of a Git tag.
