---
id: persona-setup-person
status: approved
derived-from:
  - concept#audience: be038af174a9
---

# The set-up person

<!-- docs/product/personas/setup-person.md. The project lead in the role of the one who installs,
recovers, and removes the product. They are also a viewer (persona-viewer); this persona covers only
what differs. Stories link to a section, for example persona-setup-person#goals. -->

## Goals

- Give the household a home screen with only their apps, and typed search across them.
- Set it up once, put it back after a Google TV update or reset, and remove it cleanly if needed,
  each by following a written procedure.
- Not be the household's help desk: the others should manage on their own.

## Skills and tools

- A software developer, comfortable with the command line, git, and ADB.
- A Windows PC on the same network as the TV, paired for wireless debugging.
- Knows Google TV's settings, and has tried apps only mode and replacement home screens.

## Context of use

At the PC, rarely: once to set up, and again only when a Google TV update or reset undoes it, or
when the household reports a problem. At the TV with the remote otherwise, as a viewer. Changes to
the TV affect the whole household, so they want each change to be deliberate and reversible.

## Frustrations

- Apps only mode removes the promotion but also typed search, and still shows a featured banner.
- Replacement home screens show only the apps, but break typed search across apps.
- Google TV can change with an update, undoing what was set up.
- Being called over when something on the TV changes or goes wrong.

## Evidence

- The project lead's answers on 2026-11-09 (setup by them from a PC over ADB, one written undo,
  re-running setup after an update or reset being acceptable, the others asking them for help) and
  their own tries of apps only mode and Projectivy on 2026-11-08 (recorded in the concept's Problem
  section).
- Comes from the web-app profile's `administrator` starter, narrowed to one person.
- That Google TV updates may undo the setup is an expectation, not yet seen on the reference
  device. [to validate]
