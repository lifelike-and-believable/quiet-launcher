---
id: concept
status: approved
---

# Quiet Launcher

<!-- docs/product/concept.md. The root artifact: it links to nothing. A first draft from the
conversation that started the project (2026-11-08); the project lead confirmed every
claim on 2026-11-09. -->

## Problem

On a Google TV, the home screen is the first thing anyone sees, and most of it is promotion: large
tiles for apps that are not installed, rows of suggested shows, and a rotating banner of featured
content. The apps the household actually uses sit in one row among them.

Google offers two ways out, and each costs something people use:

- **Apps only mode** removes most suggestions, but also turns off search and voice, the watchlist,
  and playback of Google Play purchases, and it still shows a featured banner.
- **A replacement home screen** (Projectivy, FLauncher, and others) shows only the apps, but typed
  search across apps lives inside Google's home screen, so it is lost. Voice search still works.

So today a household chooses between a quiet home screen and being able to type the name of a show
and find which app has it.

## Audience

People who share a Google TV at home and want it to feel like their own device: they know which
apps they use, they find the promotion distracting, and they still want to find a show without
opening each app in turn.

The product is for one household: the project lead's own, on a Chromecast with Google TV. The
repository is public and others may use it, but it is not published or supported for other
households, so its setup can ask for things a published app could not (ADB from a computer,
accessibility permissions).

## Vision

Pressing Home shows the household's apps and nothing else: no suggestions, banners, or
advertising. From the same screen, anyone can type or say what they want to watch and find which
app has it, across all the household's apps. Typed search across apps is the reason to build this
rather than use an existing replacement home screen. Turning it off returns the TV to exactly how
it was.

## Non-goals

- Recommending what to watch. The product never adds content the household did not ask for.
- Building its own catalogue of what streams where, as long as Google's search or each app's own
  search can be reached instead.
- Phones, tablets, or TVs that do not run Google TV or Android TV.
- Changes that need root access, or that leave the TV without a working home screen if the
  product is removed.
- Showing advertising, or collecting usage data.
- Publishing to the Play Store, or supporting households other than the project lead's.
- Keeping Google TV's other features reachable from the new home screen (the watchlist, Google
  Play purchases, live TV channels), for now.

## Feasibility

Another app can reach Google's typed search. On the reference device, sending the text of a show's
name to Google's search opened Google TV's page for that show, with the app that has it (spike,
2026-11-09, in `docs/product/notes.md`). Two limits follow:

- The results page belongs to Google TV's own home app, so the product has to take over the Home
  button while leaving that app installed and working, rather than disabling or blocking it.
- Google's assistant decides what the text means: a vague query can get a chat reply instead of
  results.
