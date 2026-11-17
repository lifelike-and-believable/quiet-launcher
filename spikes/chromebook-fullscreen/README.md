# Spike: the Chromebook launcher in full screen

Throwaway code, not the product. Questions, from the concept's Chromebook vision (the launcher and
the services switch back and forth in full screen, without the browser's tabs and toolbar coming
back into view):

1. Does an extension's keyboard shortcut still work while a streaming site's own player is full
   screen?
2. Does the window stay full screen when the shortcut switches tabs, and when a service opens in a
   new tab?
3. Does any service's player take the key combination before Chrome sees it?
4. Does Google's search show where a named show streams? A plain search did not (2026-11-16), so
   the launcher offers three to compare: Google, Google with "where to watch" added, and JustWatch.
5. In full screen, ChromeOS briefly shows the tabs and address bar when the shortcut switches tabs
   or a service opens (first run, 2026-11-16). Does giving each service its own full-screen window,
   with no tab strip, avoid that? The launcher page has a switch between the two modes, and each
   log line says which mode it came from.
6. Does pausing the video when the launcher comes back work on each service, and does it stay
   paused? The log says how many videos it paused and whether any started again within 2 seconds.
   This needs the streaming sites as permissions, which Chrome lists when the extension loads.
7. In tablet mode, with no keyboard, how does the project lead get back to the launcher?
   - Touching a service's page shows a Home button in its bottom-left corner for 4 seconds;
     tapping it does what Alt+Shift+H does. Does it show, including over the site's own full-screen
     player, and does it get in the way of the site?
   - Bringing the launcher's window forward any other way (in tablet mode, swipe up from the bottom
     and hold, then tap the launcher; or Alt+Tab) also pauses the service left behind. The log says
     "came forward another way".
   In the agent's container (headless Chromium with touch emulation, 2026-11-17): the button showed
   on a tap, tapping it brought the launcher back and paused a stand-in video, and it moved into the
   page's own full-screen element. The other route could not be tried there, because headless
   Chromium does not report window focus changes.

Results go in `docs/product/notes.md`.

The extension replaces the new tab page with a stand-in launcher: a tile for each service in
`services.js` (edit the list to match yours), a search box, and a log. Choosing a service opens it
in its own tab, or switches to its tab if it is already open. **Alt+Shift+H** goes back to the
launcher (the TV's Home button), and **Alt+Shift+J** goes back to the last service. The log
records each shortcut press and whether the window was still full screen half a second after each
switch. A line with `<< LEFT FULL SCREEN` means the window dropped out of full screen; the spike
then puts it back and logs whether that worked, so a run shows both the raw behaviour and whether
the launcher could repair it. A shortcut press that never shows up in the log means the shortcut
did not reach Chrome.

Tried in headless Chromium in the agent's container on 2026-11-16 (not a Chromebook, and its
window states are simulated): switching between existing tabs kept full screen; opening a service
in a new tab dropped it, and putting it back worked. The Chromebook run is what counts.

## Load it on the Chromebook

1. Get this folder onto the Chromebook: download the branch as a ZIP
   (<https://github.com/lifelike-and-believable/quiet-launcher/archive/refs/heads/claude/busy-allen-wfpnec.zip>,
   or on GitHub open the branch and choose **Code**, then **Download ZIP**); in the Files app, open the ZIP and copy `spikes/chromebook-fullscreen` into
   **My files**.
2. In Chrome, go to `chrome://extensions`, turn on **Developer mode** (top right), choose **Load
   unpacked**, and pick the copied `chromebook-fullscreen` folder.
3. Open a new tab. If the log says a shortcut is `NOT SET`, something else already uses it: set
   another at `chrome://extensions/shortcuts`.
4. Follow the steps on the page, then choose **Copy results** and paste them to the agent.

Undo: remove the extension at `chrome://extensions`, and turn **Developer mode** off if it was off
before.
