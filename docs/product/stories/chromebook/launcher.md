---
id: story-chromebook-launcher
status: approved
derived-from:
  - epic-chromebook#outcome: d32a14d5a50e
  - persona-chromebook-viewer#goals: 322e3fb02f13
  - persona-chromebook-viewer#context-of-use: b331802b2c97
---

# Bring up the launcher with one shortcut

<!-- docs/product/stories/chromebook/launcher.md. Release: first (epic-chromebook).
REQ-chromebook-1, 5, 10. -->

As the Chromebook viewer, I want one shortcut to bring up my streaming services full screen from
wherever I am in Chrome, so that I can switch from browsing to watching without hunting for sites.

## Acceptance criteria

- Pressing Alt+Shift+H anywhere in Chrome, including on an ordinary web page, shows the launcher in
  its own full-screen window: no tabs, no address bar.
- The window I was browsing in is left as it was, and is there when I leave full screen or switch
  back to it.
- If the launcher is already open, the shortcut brings that one forward; there is never a second.
- Opening a new tab or a new window in Chrome shows Chrome's own new tab page, not the launcher.
- Everything on the launcher works with the keyboard alone, with the touchpad, and with the
  touchscreen.
