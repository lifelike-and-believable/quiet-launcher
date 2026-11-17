---
id: story-chromebook-go-back
status: approved
derived-from:
  - epic-chromebook#outcome: d32a14d5a50e
  - persona-chromebook-viewer#goals: 322e3fb02f13
  - persona-chromebook-viewer#frustrations: 574438d89687
---

# Go back to the launcher, and the video pauses

<!-- docs/product/stories/chromebook/go-back.md. Release: first (epic-chromebook).
REQ-chromebook-6, 7. -->

As the Chromebook viewer, I want the launcher's shortcut to work like Home on the TV, so that I can
leave a service without its video playing on behind me, and get back to it just as quickly.

## Acceptance criteria

- Pressing Alt+Shift+H in a service's window brings the launcher forward, still full screen, within
  1 second.
- Any video playing in that window pauses, and is still paused when I go back to it. Nothing else
  in the window changes.
- If nothing was playing, nothing is touched.
- Pressing Alt+Shift+J on the launcher goes straight back to the last service I used, as I left it.
- The shortcuts work while the service's own player is full screen.
- For a service whose site I did not give the extension permission on, the launcher still comes
  back, but its video is not paused.
