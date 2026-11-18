---
id: tutorial-chromebook-watch-something
status: approved
runner: manual
derived-from:
  - docs-chromebook#watch-something: dad567bc8618
  - docs-chromebook#find-which-service-has-a-show: 01b59f5064c5
---

# Watch something on the Chromebook, trying a few services first

For **the Chromebook viewer** (persona-chromebook-viewer).

<!-- docs/tutorials/chromebook-watch-something.md. Followed by a person on the Chromebook: no
runner drives a Chrome extension's own windows on ChromeOS yet. -->

## Before you start

- The quiet launcher is installed from the repository's `chromebook` folder, at least as new as
  this tutorial (docs-chromebook, Install it and Update it), with at least two of your services on
  it.
- No service is open: if the launcher marks one **Open**, choose **Back to browsing** first.

## Steps

1. Press **Alt+Shift+H**. You see the launcher, full screen, with no service marked **Open**.
2. Choose your first service. It opens full screen in its own window. (manual)
3. Press **Alt+Shift+H**. You see the launcher again, and only that service is marked **Open**.
4. Choose a second service. It opens full screen. (manual)
5. Press **Alt+Shift+H**. You see **Open** on the second service only.
6. Open ChromeOS's window overview (swipe up and hold, or press the overview key). Only the launcher,
   the second service, and your own browsing windows are there: the first service's window has
   closed. Choose the launcher. (manual)
7. Choose the second service again. It comes back exactly as you left it, without loading again.
   (manual)
8. Press **Alt+Shift+H**, type the name of a show in **Search for a show**, and press **Enter**.
   JustWatch's results open full screen. (manual)
9. Press **Alt+Shift+H**. No service is marked **Open**: the search closed the second service.
10. Choose your first service. In the window overview, the results' window has closed. (manual)

## What you made

You tried services and a search, and at every point only one of them was open behind the launcher.
When you're done, choose **Back to browsing**.
