---
id: docs-chromebook
status: approved
derived-from:
  - design-chromebook: 1bf61f23d651
  - story-chromebook-launcher: aeb2d1142c30
  - story-chromebook-open-service: 0115b9f34e7e
  - story-chromebook-go-back: 34b16f0ab619
  - story-chromebook-search: e76b0c88e535
  - story-chromebook-services: 368e4fdb201a
  - story-chromebook-install: 7dd879387fea
---

# The quiet launcher on your Chromebook

<!-- docs/user/chromebook.md. For the Chromebook viewer (persona-chromebook-viewer): the project
lead, the only person who uses the Chromebook. Written from design-chromebook as if the extension
exists. Specs cite sections by heading, for example docs-chromebook#install-it. -->

The quiet launcher is a Chrome extension that puts all your streaming services in one place, full
screen, in the same look as the TV's quiet home screen. One shortcut brings it up from anywhere in
Chrome; the rest of the time Chrome works as it always has.

## Install it

You need the repository's `chromebook` folder on the Chromebook. Nothing is published to the
Chrome Web Store; this is the only way to get it.

1. Download the repository as a ZIP:
   <https://github.com/lifelike-and-believable/quiet-launcher/archive/refs/heads/main.zip>.
2. In the Files app, open the ZIP and copy its `chromebook` folder into **My files**. Keep it there:
   Chrome loads the extension from that folder every time it starts.
3. In Chrome, go to `chrome://extensions` and turn on **Developer mode** (top right).
4. Choose **Load unpacked** and pick the `chromebook` folder you copied.

**Quiet Launcher** appears in the list of extensions. Press **Alt+Shift+H**: the launcher opens,
full screen, with no services yet.

If Chrome says a shortcut could not be set, see [If something goes wrong](#if-something-goes-wrong).

## Add your services

1. On the launcher, choose **Edit services**, then **Add a service**.
2. Choose a service from the list (Prime Video, YouTube, Apple TV+, Criterion Channel, MUBI, CBC Gem,
   Tubi, Plex, myfilmfriend, and others available in Canada), or type another site's address and
   choose **Add**.
3. Chrome asks whether the extension may "read and change your data" on that service's site.
   Choose **Allow**: the launcher needs this to pause the service's video when you go back to the
   launcher, and to show its Home button. It does nothing else on the site.
4. Add the rest the same way, then choose **Done**.

If you choose **Deny**, the service is still added and opens, but its videos will not pause when
you leave them, and the launcher says **Videos won't pause** under it. That is all you need to do
about it. If you change your mind, choose **Edit services**, select the service, and choose
**Allow pausing**.

## Watch something

1. Press **Alt+Shift+H** from anywhere in Chrome. The launcher comes up full screen. The window you
   were browsing in stays as it was, behind it.
2. Choose a service: click or tap it, or move to it with the arrow keys and press **Enter**.

The service opens full screen in its own window, with no tabs or address bar. If it was already
open (the launcher marks it **Open**), you are back exactly where you left it.

## Go back to the launcher

- With a keyboard: press **Alt+Shift+H**.
- In tablet mode, or with no keyboard to hand: touch the service's page. A round Home button
  appears in the bottom-left corner for a few seconds; tap it.
- Or switch windows the ChromeOS way: swipe up from the bottom and hold (tablet mode), or press
  **Alt+Tab**, and choose the launcher.

Whichever way you go, the launcher comes forward and the video you left pauses. It stays paused
until you play it again.

To go straight back to the last service you used, without choosing it, press **Alt+Shift+J** on the
launcher.

## Find which service has a show

1. On the launcher, type the name of a show or film in **Search for a show** and press **Enter**.
2. JustWatch's results for Canada open, full screen, in their own window. They list every film and
   show with that name, and the services that stream each one.
3. Choose one of your services from the results. It opens in that same window, and from then on it
   is that service's window: Alt+Shift+H, the Home button, and pausing all work in it.

The launcher keeps no record of what you search for.

## Arrange your services

1. Choose **Edit services**.
2. Select a service. Its menu offers **Move**, **Hide**, **Remove**, and **Cancel**.
   - **Move**: move it with the arrow keys, or drag it with the touchpad or your finger. Press
     **Enter** or let go to put it down; **Esc** puts it back where it was.
   - **Hide**: it leaves the launcher but stays in your list. Hidden services are shown after the
     others while you edit; select one and choose **Show** to bring it back.
   - **Remove**: it leaves your list altogether, and its window closes. To have it again, add it
     again.
3. Choose **Done**.

Changes are saved as you make them, and are kept when Chrome or the Chromebook restarts. Your
Chromebook's list is its own: it does not change the TV's, or the other way round.

## Go back to browsing

Choose **Back to browsing** on the launcher. This turns the launcher off: its window and your
services' windows close, and the Chrome window you were last browsing in comes forward. The next
Alt+Shift+H starts it afresh, and your services open from the beginning.

New tabs and new windows are Chrome's own, as always: the launcher only comes up when you ask for
it.

## Change the shortcuts

1. Go to `chrome://extensions/shortcuts`.
2. Under **Quiet Launcher**, set new keys for **Back to the launcher** and **Back to the last
   service**.

The launcher's hint line, at the bottom, shows the shortcuts in use.

## Update it

1. Download the ZIP again (step 1 of [Install it](#install-it)) and replace the `chromebook`
   folder in **My files** with the new one.
2. In `chrome://extensions`, choose the reload arrow on **Quiet Launcher**.

Your services, their order, and what is hidden are kept.

## Remove it

1. If the launcher is on, choose **Back to browsing** first, so its windows close.
2. In `chrome://extensions`, choose **Remove** on **Quiet Launcher**, and confirm.
3. If you turned on **Developer mode** only for this, turn it off.
4. You can delete the `chromebook` folder from **My files**.

Chrome is exactly as it was before: no launcher or service windows are left open, and nothing it
stored is left behind.

## If something goes wrong

- The launcher says a shortcut is used by something else, or Alt+Shift+H does nothing: another
  extension or ChromeOS already uses those keys. Set different ones (see [Change the
  shortcuts](#change-the-shortcuts)). Until then, the launcher's window can be reached with
  Alt+Tab or the tablet-mode overview, and services' Home buttons still work.
- A video keeps playing when I go back: check that the service does not say **Videos won't
  pause** while you edit; if it does, allow its site. A few players start again by themselves; the
  launcher does not fight them.
- The Home button does not appear: it appears only on a touch, not with the touchpad or a mouse.
  If it never appears on one service, that service's site permission is probably missing (as
  above).
- Tabs or the address bar show: the window has left full screen, for example after the
  Chromebook's full-screen key. Press Alt+Shift+H, or choose the service again, to go back to full
  screen.
- I closed the launcher's window: press Alt+Shift+H to open it again.
- "Load unpacked" is missing: turn on **Developer mode** at the top right of
  `chrome://extensions`. If your Chromebook is managed by a school or workplace, its
  administrator may have turned developer mode off.
