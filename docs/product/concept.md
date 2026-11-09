---
id: concept
status: draft
---

# Quiet Launcher

<!-- docs/product/concept.md. The root artifact: it links to nothing. A first draft from the
conversation that started the project (2026-11-08); every claim the approver has not confirmed is
marked. -->

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
and find which app has it. [proposed]

## Audience

People who share a Google TV at home and want it to feel like their own device: they know which
apps they use, they find the promotion distracting, and they still want to find a show without
opening each app in turn. [proposed]

The first household is the project lead's own, on a Chromecast with Google TV. [assumed] whether
the product is for that household only or for anyone who installs it (see Open questions).

## Vision

Pressing Home shows the household's apps and nothing else: no suggestions, banners, or
advertising. From the same screen, anyone can type or say what they want to watch and get to it,
either inside one app or across all of them. Turning it off returns the TV to exactly how it was.
[proposed]

## Non-goals

- Recommending what to watch. The product never adds content the household did not ask for.
  [proposed]
- Building its own catalogue of what streams where, as long as Google's search or each app's own
  search can be reached instead. [proposed]
- Phones, tablets, or TVs that do not run Google TV or Android TV. [proposed]
- Changes that need root access, or that leave the TV without a working home screen if the
  product is removed. [proposed]
- Showing advertising, or collecting usage data. [proposed]

## Open questions

- [open] Is this for the project lead's household only, or something to publish (the Play Store,
  or a sideloaded release) for other Google TV owners? This decides how much setup a person can be
  asked to do (ADB, accessibility permissions).
- [open] Is typed search across all apps a must, or is searching inside one app at a time enough
  for a first release, if Google's search turns out not to accept a typed query from another app?
- [open] Should the household's other Google TV features stay reachable from the new home screen
  (the watchlist, Google Play purchases, live TV channels), or is leaving them behind acceptable?
