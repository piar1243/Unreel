# Stable Web + Home Feed Fast Navigation Milestone

Date: 2026-06-21

Known-good point before experimenting with Instagram bottom-nav selected-state detection.

- Native Instagram Reels detection and reverse/block behavior are working.
- Native Instagram Search Grid blocking is working.
- Home Feed blocker is fast when leaving Home for Stories, DMs, and other tabs.
- Home Feed blocker reappears quickly on return to Home using:
  - post-navigation re-check bursts,
  - cached Home Feed blocker geometry,
  - immediate Home overlay dismissal without fade.
- Home Stories can be optionally blocked separately.
- Instagram website blocking is stricter:
  - blocks loaded Instagram web pages,
  - ignores focused browser/search inputs,
  - avoids Brave suggestion-row false positives.
- App Grayscale and Daily Open Limit remain service-side features, separate from content blockers.

Experiment starting after this milestone: add `AccessibilityNodeInfo.isSelected` to captured nodes and use selected bottom-nav state, if Instagram exposes it, to make Home Feed detection more immediate and accurate.
