---
id: story-quiet-home-home-key
status: approved
derived-from:
  - epic-quiet-home#outcome: ad1e8e9b386c
  - persona-viewer#goals: 24b5a84b1f16
  - persona-viewer#frustrations: 8722d20eb95e
---

# Home brings up the quiet home screen

<!-- docs/product/stories/quiet-home/home-key.md. Release: first. -->

As a household viewer, I want Home to bring up a screen with only our apps, from anywhere, so that
promoted shows never get in my way.

## Acceptance criteria

- From any app, from Google's pages (a show's page, a chat reply), or from the TV's settings,
  pressing Home shows the quiet home screen (this product's home screen, as opposed to Google TV's
  home screen with its rows and banners), with the apps visible and selectable, within 1 second of
  the press.
- Pressing Home never shows Google TV's home screen first, not even for a moment, as checked frame
  by frame in a screen recording of 20 presses from different apps.
- Holding Home down shows the quiet home screen too, and does nothing else: Google TV's own long-
  press Home action is not available.
- If Google TV's home screen comes up without Home being pressed (for example when an app closes, or
  the TV wakes or starts), the quiet home screen covers it within half a second.
- The remote's app buttons (YouTube, Netflix) and its voice button still work, from the quiet home
  screen and from apps.
- Pressing Home on the quiet home screen leaves it there.
