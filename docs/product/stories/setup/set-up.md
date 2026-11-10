---
id: story-setup-set-up
status: approved
derived-from:
  - epic-setup#outcome: fcfa341d2d11
  - persona-setup-person#goals: 1b5715ea18d9
  - persona-setup-person#skills-and-tools: 0ba05e70b88d
---

# Set it up from the PC

<!-- docs/product/stories/setup/set-up.md. Release: first. -->

As the set-up person, I want to make the quiet home screen the TV's home screen by following one
written procedure from my PC, so that the change is deliberate and written down.

## Acceptance criteria

- The written procedure starts from a PC that has ADB and the product's install file, and a TV that
  is not yet paired with it: it covers turning on wireless debugging, pairing, installing, and
  turning the product on.
- Following it takes under 15 minutes, and needs nothing on the TV beyond what the procedure says to
  do with the remote.
- Before changing anything, the procedure records the TV's accessibility settings, so that undo can
  restore them.
- Afterwards, pressing Home on the TV shows the quiet home screen.
- The procedure ends with checks the set-up person runs: Google TV's own home app is still installed
  and enabled, and searching for a named show from the quiet home screen opens Google's page for it.
- Nobody else in the household has to do anything.
