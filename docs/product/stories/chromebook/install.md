---
id: story-chromebook-install
status: approved
derived-from:
  - epic-chromebook#outcome: d32a14d5a50e
  - persona-chromebook-viewer#skills-and-tools: 9bbd81df7f2e
---

# Install it, change its shortcuts, and remove it cleanly

<!-- docs/product/stories/chromebook/install.md. Release: first (epic-chromebook).
REQ-chromebook-9, 11. -->

As the Chromebook viewer, I want to install the launcher from the repository by following written
steps, and remove it without trace, so that trying it costs me nothing.

## Acceptance criteria

- Written steps take me from the repository to a working launcher with Chrome's developer mode for
  extensions; nothing comes from the Chrome Web Store.
- After installing, Alt+Shift+H and Alt+Shift+J work, or, if something else on the Chromebook
  already uses them, the launcher says so and how to set others.
- I can change both shortcuts in Chrome's own shortcut settings, and the launcher shows the ones in
  use.
- Removing the extension leaves Chrome exactly as it was: its own new tab page, no launcher or
  service windows left open by it, and no settings left behind.
