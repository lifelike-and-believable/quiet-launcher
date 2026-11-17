---
id: concept
status: approved
---

# Quiet Launcher

<!-- docs/product/concept.md. The root artifact: it links to nothing. A first draft from the
conversation that started the project (2026-11-08); the project lead confirmed every
claim on 2026-11-09. On 2026-11-16 the project lead asked for a matching dashboard on the
household's Chromebook, as a Chrome extension with search, arranging, and media controls, built
in this project; the same day they said what it is for (one place for every streaming service, in
the quiet home screen's familiar interface), and confirmed every claim as written. Later that day
they dropped the media controls, which ChromeOS already provides, and said it runs full screen,
switching between the page and the services without the browser coming back into view. After
the full-screen spike, they decided that going back to the page pauses the video left behind. -->

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

The household also watches on a Chromebook, where each streaming service is a separate website in
Chrome. There is no one place that gathers them, as the quiet home screen does on the TV.

## Audience

People who share a Google TV at home and want it to feel like their own device: they know which
apps they use, they find the promotion distracting, and they still want to find a show without
opening each app in turn.

The product is for one household: the project lead's own, on a Chromecast with Google TV and on
the household's Chromebook. The
repository is public and others may use it, but it is not published or supported for other
households, so its setup can ask for things a published app could not (ADB from a computer,
accessibility permissions).

## Vision

Pressing Home shows the household's apps and nothing else: no suggestions, banners, or
advertising. From the same screen, anyone can type or say what they want to watch and find which
app has it, across all the household's apps. Typed search across apps is the reason to build this
rather than use an existing replacement home screen. Turning it off returns the TV to exactly how
it was.

On the household's Chromebook, a Chrome extension is the one place to reach every streaming service
the household uses, in the interface the household already knows from the TV. Its page shows those
services and nothing else, under the same names and in the same look as the TV's home screen; the
services can be put in order and hidden, as on the TV. Its search box takes the name of a show and
finds which service has it. It runs full screen, like the TV: the page and the services switch back
and forth without the browser's tabs and toolbar coming back into view. Removing the extension
returns Chrome to exactly how it was.

## Non-goals

- Recommending what to watch. The product never adds content the household did not ask for.
- Building its own catalogue of what streams where, as long as Google's search or each app's own
  search can be reached instead.
- Phones, tablets, or TVs that do not run Google TV or Android TV, and computers other than the
  household's Chromebook.
- Controlling one device from the other: the Chromebook does not control the TV, and the TV does
  not control the Chromebook.
- Controls for what plays in Chrome's tabs, such as play and pause buttons. ChromeOS's own media
  controls, in the shelf and Quick Settings, already do that (the project lead's decision,
  2026-11-16). The one exception: going back to the extension's page pauses the video left behind,
  as Home does on the TV (the project lead's decision, 2026-11-16, after the full-screen spike).
- Changes that need root access, or that leave the TV without a working home screen if the
  product is removed.
- Showing advertising, or collecting usage data.
- Publishing to the Play Store or the Chrome Web Store, or supporting households other than the
  project lead's. The extension is loaded from the repository.
- Bringing Google TV's other features (the watchlist, Google Play purchases, live TV channels) into
  the new home screen. They stay reachable through Google TV's own home screen, which a button on
  the new home screen opens on request (the project lead's decision, 2026-11-09).

## Feasibility

Another app can reach Google's typed search. On the reference device, sending the text of a show's
name to Google's search opened Google TV's page for that show, with the app that has it (spike,
2026-11-09, in `docs/product/notes.md`). Two limits follow:

- The results page belongs to Google TV's own home app, so the product has to take over the Home
  button while leaving that app installed and working, rather than disabling or blocking it.
- Google's assistant decides what the text means: a vague query can get a chat reply instead of
  results.

On the Chromebook, a Chrome extension can replace the new tab page; a plain web page cannot. It can
also pause a site's video, by running a small script in that site's pages, which Chrome allows only
for the sites the extension is granted when it is installed. For
search, the extension sends the text to JustWatch's search for Canada, which lists every film and
show with that name and, for each, the services that stream it, with links to them [proposed]. It
keeps no catalogue of its own. Google's web search was tried first and did not do this: a search
for "Carrie" gave ordinary results, and with "where to watch" added it gave only the film, not the
show (spike, 2026-11-16, in `docs/product/notes.md`).
