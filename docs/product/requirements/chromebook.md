---
id: requirements-chromebook
status: approved
derived-from:
  - concept#vision: 0028ebdf37ce
  - concept#non-goals: f049f9bec803
  - concept#feasibility: aaa8e96fc36e
---

# Chromebook requirements

<!-- docs/product/requirements/chromebook.md. The launcher on the household's Chromebook: a Chrome
extension that is one place for every streaming service, in the TV's familiar interface (concept,
Vision). Written on 2026-11-17 from the project lead's answers of 2026-11-16 (its own list and
order; one user; full screen; a window per service; pause when going back; JustWatch for search)
and the full-screen spike in docs/product/notes.md. What the project lead decided is written as
decided. On 2026-11-17 the project lead accepted the agent's proposals and changed one thing: the
launcher does not replace Chrome's new tab page, and comes up only with its shortcut
(REQ-chromebook-1). Amended the same day for tablet mode: REQ-chromebook-12, and pausing however the
launcher comes forward (REQ-chromebook-6). -->

## REQ-chromebook-1: The launcher is one key away

Release: first

A keyboard shortcut (Alt+Shift+H unless changed) brings up the launcher in its own full-screen
window, from anywhere in Chrome: from a service's window, the way Home does on the TV
(REQ-chromebook-6), or from ordinary browsing. There is never more than one launcher: asking for it
again brings the open one forward.

Chrome stays a browser: the launcher does not replace the new tab page, and opening a new tab or
window works as it always has. The launcher never takes over a window the project lead is browsing
in (the project lead's decision, 2026-11-17).

## REQ-chromebook-2: Services and nothing else

Release: first

The launcher shows the streaming services the project lead has added, and nothing that promotes
content: no suggested shows, previews, banners, or advertising. Each service appears under its own
name, in the same look as the TV's home screen.

## REQ-chromebook-3: Adding and removing services

Release: first

The project lead adds a service by choosing it from a list of streaming services available in
Canada, or by giving the address of any other site, and can remove one again. Adding a
service asks Chrome for permission on that service's site, which pausing needs
(REQ-chromebook-6); if the permission is refused, the service is still added and opens, but is not
paused.

## REQ-chromebook-4: Arranging and hiding

Release: first

The project lead can move a service to a different place in the list, and hide a service and show
it again. There is no guard against arranging by accident, because one person uses the Chromebook
(the project lead's answer, 2026-11-16). The list, the order, and what is hidden are the
Chromebook's own, separate from the TV's, and are kept when Chrome or the Chromebook restarts.

## REQ-chromebook-5: Full screen, with nothing of the browser showing

Release: first

The launcher opens full screen. Each service opens in its own full-screen window, and switching
between the launcher and the services never shows Chrome's tabs or address bar, even briefly (the
full-screen spike: switching tabs flashed them; switching windows did not). Leaving full screen
with the Chromebook's own full-screen key still works, and gives ordinary Chrome back.

## REQ-chromebook-6: Going back to the launcher pauses the video

Release: first

Pressing the launcher's shortcut (Alt+Shift+H unless changed) from a service brings the launcher
forward within 1 second and pauses any video playing in that service's window, which stays paused
until someone plays it again (concept, Non-goals: the project lead's decision of 2026-11-16).
Nothing that is not playing is touched. The same pause happens however the launcher comes forward:
by the shortcut, by the touch control in REQ-chromebook-12, or by switching windows in ChromeOS
(the tablet-mode overview, Alt+Tab, or a click).

## REQ-chromebook-7: Back to a service where it was left

Release: first

Choosing a service whose window is already open brings that window back as it was left, rather than
opening the service again. A second shortcut (Alt+Shift+J unless changed) goes from the launcher
straight back to the last service used.

## REQ-chromebook-8: Search shows which service has it

Release: first

The launcher has a search box. Submitting text opens JustWatch's search for Canada for exactly that
text, in its own full-screen window: it lists every film and show with that name and the services
that stream each one, with links (concept, Feasibility). Following a link to one of the project
lead's services makes that window the service's window, so the shortcuts and pausing work in it as
in any other.

## REQ-chromebook-9: The shortcuts can be changed

Release: first

Both shortcuts can be changed in Chrome's own shortcut settings (`chrome://extensions/shortcuts`),
and the launcher shows the shortcuts in use.

## REQ-chromebook-10: Keyboard, touchpad, or touch

Release: first

Everything on the launcher (opening a service, searching, arranging) works with the keyboard alone,
and with the touchpad or the touchscreen.

## REQ-chromebook-11: Loaded from the repository, removed cleanly

Release: first

The project lead installs the extension from the repository with Chrome's developer mode for
extensions, following written steps; nothing is published (concept, Non-goals). Removing the
extension returns Chrome to exactly how it was, with no leftover windows or settings.

## REQ-chromebook-12: Back to the launcher by touch

Release: first

With no keyboard, as in tablet mode, touching a service's page shows a control for a few seconds
that brings the launcher back, as the shortcut does (REQ-chromebook-6). It shows over the site's own
full-screen player too, and does not get in the way of the site (the project lead's request,
2026-11-17; tried in the tablet-mode spike).

## Constraints

- The project lead's Chromebook, ChromeOS with Chrome 150 or later. One person uses it.
- Search goes to JustWatch's Canadian search; the extension keeps no catalogue and stores no
  queries.
- No advertising, recommendations, or usage data (concept, Non-goals).
- No controls for what plays beyond the pause in REQ-chromebook-6 (concept, Non-goals).

## Success measures

- For each of the project lead's services, going to the launcher and back with the shortcuts, in
  full screen, shows no tabs or address bar and leaves the video paused, every time.
- Searching for ten shows or films the project lead watches lists the service that has each one
  for at least nine of the ten.
- Over a two-week trial, the project lead reaches streaming services through the launcher rather
  than Chrome's tabs and bookmarks.
