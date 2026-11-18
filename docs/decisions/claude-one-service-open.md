# One service open at a time on the Chromebook

## Revision

The project lead's change, 2026-11-17: on the Chromebook launcher, opening a different service
closes the window of the one that was open, so at most one service is open behind the launcher.
Their reason: "keeping 7 or more video players open just because I've been browsing around for
something to watch seems an unnecessary use of battery and memory, and it's confusing seeing all
these 'open' badges across all the services." Choosing the service that is open still brings it
back as it was left, and Alt+Shift+J still goes back to it. JustWatch's results window counts as
the open service: searching closes the service that was open, and choosing a service on the
launcher closes the results. That last part is this revision's proposal, so that only one window
is ever open behind the launcher; the project lead can ask for the results to stay open instead.

Changed: requirements-chromebook (REQ-chromebook-7, REQ-chromebook-8), story-chromebook-open-service
(a new acceptance criterion), design-chromebook (Flow steps 2 and 4, the Open marker in Layout and
Mock-ups, How it works: windows), and docs-chromebook (Watch something, Find which service has a
show).

Mock-ups re-rendered: `default.png` and `open.png`, which showed **Open** on both Prime Video and
MUBI and now show it on Prime Video only (`mockup.html`). They were rendered with Playwright from
`mockup.html`, as `render.js` does, but on this PC, so the system font differs slightly from the
other mock-ups.

- epic-chromebook: unaffected; its stories "go back to it where it was left" and "straight back to
  the last service" still hold for the one open service, and its outcome ("without a video playing
  on behind them") is what this change strengthens.

No spec cites the changed sections, so the extension's code is not reported as an open revision.
The specs that implement the windows (the Chromebook launcher's services and search slices) will
need a revision delivered after this merges.
