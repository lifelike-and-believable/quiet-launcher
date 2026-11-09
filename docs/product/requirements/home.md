---
id: requirements-home
status: draft
derived-from:
  - concept#vision:
  - concept#non-goals:
---

# Home screen requirements

<!-- docs/product/requirements/home.md. What the screen shown by the Home button must do. The
project lead's answers of 2026-11-09 are written as decided; the project lead also accepted
the agent's proposals for the rest. -->

## REQ-home-1: Home opens the quiet home screen

Release: first

Pressing Home on the remote, from any app or screen, shows this product's home screen, not Google
TV's.

## REQ-home-2: Apps and nothing else

Release: first

The home screen shows the installed apps that can be opened from a TV home screen, and nothing that
promotes content: no suggested shows, previews, featured banners, rows of content from apps,
advertising, or apps that are not installed. This holds at every moment, including while Home is
being pressed: Google TV's home screen must not show, even briefly, before this one.

## REQ-home-3: The household arranges the apps

Release: first

Every installed app appears unless hidden (REQ-home-4). Anyone in the household can move an app to
a different place in the list, using only the remote, and the order is kept after the TV restarts.

## REQ-home-4: Hiding apps

Release: first

Anyone can hide an app from the home screen and show it again later. A hidden app stays installed
and still opens in other ways (from search results or voice). The hidden apps are listed somewhere
reachable from the home screen, so a hidden app can always be brought back.

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

Everything on the home screen, including arranging and hiding apps, works with the remote's
direction pad, select, back, and home buttons. No keyboard, phone, or computer is needed once the
product is set up.

## REQ-home-10: Home is fast

Release: first

On the reference device, pressing Home from another app shows the home screen, with the apps
visible and selectable, within 1 second, and the search box (REQ-search-1) accepts input within the
same second.

## Constraints

- Reference device: Chromecast with Google TV, Android 14.
- The household does not use a Google TV kids profile, so the home screen does not have to apply a
  profile's limits on which apps it shows (REQ-home-6).
- No advertising, recommendations, or usage data, here or anywhere else in the product (concept,
  Non-goals).

## Success measures

- Over a two-week trial on the reference device, the household uses the quiet home
  screen without asking to go back to Google TV's.
