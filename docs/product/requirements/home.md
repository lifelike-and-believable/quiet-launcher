---
id: requirements-home
status: approved
derived-from:
  - concept#vision: b94432cae6a3
  - concept#non-goals: 2bae4f8d5c1f
---

# Home screen requirements

<!-- docs/product/requirements/home.md. What the screen shown by the Home button must do. The
project lead's answers of 2026-11-09 are written as decided; the project lead also accepted
the agent's proposals for the rest. REQ-home-3, 4, 9, and 11 were amended on 2026-11-09 after the
check against the personas, and REQ-home-3 and 4 moved to the second release. -->

## REQ-home-1: Home opens the quiet home screen

Release: first

Pressing Home on the remote, from any app or screen, shows this product's home screen, not Google
TV's.

## REQ-home-2: Apps and nothing else

Release: first

The home screen shows the installed apps that can be opened from a TV home screen, and nothing that
promotes content: no suggested shows, previews, featured banners, rows of content from apps,
advertising, or apps that are not installed.

Pressing Home never shows Google TV's home screen, even briefly, before this one. If Google TV
brings its home screen up on its own, this home screen covers it within 0.5 seconds. The one
exception is when someone asks for Google TV's home screen (REQ-home-12).

## REQ-home-3: The set-up person arranges the apps

Release: second

Every installed app appears unless hidden (REQ-home-4). The set-up person can move an app to a
different place in the list, and the order is kept after the TV restarts. Nobody else can change the
order, on purpose or by accident: a viewer pressing buttons on the remote never moves an app.

## REQ-home-4: Hiding apps

Release: second

The set-up person can hide an app from the home screen and show it again later; nobody else can
hide or show apps. A hidden app stays installed and still opens in other ways (from search results
or voice). The set-up person can always see which apps are hidden and bring any of them back.

## REQ-home-5: New and removed apps

Release: first

A newly installed app appears at the end of the list without anyone doing anything. An uninstalled
app disappears from the list. Neither changes the order of the other apps.

## REQ-home-6: One home screen for everyone

Release: first

Everyone in the household sees the same apps, order, and hidden apps, whichever Google TV profile
is active.

## REQ-home-7: TV settings

Release: first

The TV's own settings (network, display, accounts, and so on) can be opened from the home screen.

## REQ-home-8: Clock

Release: first

The home screen shows the current time, following the TV's time zone and its 12- or 24-hour
setting.

## REQ-home-9: The remote is enough

Release: first

Everything a viewer does on the home screen (opening apps, searching, opening settings) works with
the remote's direction pad, select, back, and home buttons. No keyboard, phone, or computer is
needed for it once the product is set up.

## REQ-home-10: Home is fast

Release: first

On the reference device, pressing Home from another app shows the home screen, with the apps
visible and selectable, within 1 second, and the search box (REQ-search-1) accepts input within the
same second.

## REQ-home-11: Apps look like themselves

Release: first

Each app appears with the artwork it provides for TV home screens (its banner, or its icon if it has
no banner) and its name, so someone who knows the app from elsewhere recognises it.

## REQ-home-12: Google TV's own home screen, on request

Release: first

The home screen has a control that opens Google TV's own home screen, for what only it has (the
watchlist, Google Play purchases, live TV channels). Anyone can use it. While Google TV's home
screen is open this way, it is not covered. The next Home press, or the TV going to sleep, brings
back this home screen, which covers Google TV's home screen again from then on (the project lead's decision, 2026-11-09).

## Constraints

- Reference device: Chromecast with Google TV, Android 14.
- The household does not use a Google TV kids profile, so the home screen does not have to apply a
  profile's limits on which apps it shows (REQ-home-6).
- No advertising, recommendations, or usage data, here or anywhere else in the product (concept,
  Non-goals).

## Success measures

- Over a two-week trial on the reference device, the household uses the quiet home
  screen without asking to go back to Google TV's.
