# Delivery: one service open at a time on the Chromebook

Delivers the revision merged in #76 (`docs/decisions/claude-one-service-open.md`).

- **A new spec, not an amendment.** The Chromebook's existing specs (`chromebook-services`,
  `chromebook-search`, and the rest) have no `id` or `derived-from`, so the process does not count
  them, and the whole Chromebook feature still shows as an open delivery. This run writes one linked
  spec for this change, `docs/specs/chromebook-one-service.md`, and leaves bringing the older specs
  into the process for a later run.
- **Order of opening and closing.** The new window opens and comes forward first, and the old one
  closes after, so the browsing window never shows in between (spec, APIs and assumptions).
- **Existing tests changed** (listed under the spec's `unlocks:`): two `chromebook-search#AC-4`
  tests and `chromebook-back-to-browsing#AC-1`'s test assumed that services stayed open when another
  was opened or a search was made, which the approved revision changes. Each still checks what its
  own criterion promises.
- **Tutorial.** A new one, `docs/tutorials/chromebook-watch-something.md`, for the "Watch something"
  task: there was no tutorial for it to extend. It is `runner: manual`, because no runner drives
  the extension's windows on ChromeOS. It has not been walked: the author cannot reach the
  Chromebook. The project lead's walk of it on the Chromebook is the validation.
- **Tests run here.** The controller's unit tests all pass on this PC. The in-Chromium tests
  (`*-extension.test.js`, including the new `one-service-extension.test.js`) need Playwright and a
  display, which this PC lacks; CI runs them.
