---
id: story-arranging-from-pc
status: draft
derived-from:
  - epic-arranging#outcome: 32b124ae0684
  - persona-setup-person#goals: 1b5715ea18d9
---

# Arrange the apps from the PC

<!-- docs/product/stories/arranging/from-pc.md. Release: second (epic-arranging). REQ-home-3 and 4;
the project lead's choice, 2026-11-10: edit a text file in Notepad. -->

As the set-up person, I want to set the order and the hidden apps from my PC, so that a large
change, or putting back an order I saved, does not take dozens of presses on the remote.

## Acceptance criteria

- Running `quiet-arrange` (by double-clicking it, or from PowerShell) opens the TV's current
  arrangement in Notepad: one app per line, by name, in order, with hidden apps under a **Hidden**
  heading.
- When I move lines, save, and close Notepad, the TV takes the new arrangement. The script says how
  many apps are shown and how many are hidden, and the quiet home screen shows the new order the
  next time Home is pressed.
- A line for an app that is not on the TV is skipped, and the script names it. An installed app
  that is not in the file stays on the home screen, at the end.
- Closing Notepad without a change sends nothing, and the script says **Nothing changed.**
- The file stays on the PC, so after the quiet home screen is removed and set up again I can send
  the same file to put the order back.
- The TV and the PC change the same arrangement: whichever was changed last is what shows.
