---
id: requirements-setup
status: approved
derived-from:
  - concept#audience: be038af174a9
  - concept#vision: b94432cae6a3
  - concept#non-goals: 2bae4f8d5c1f
  - concept#feasibility: 5a6fcb1f845f
---

# Setup requirements

<!-- docs/product/requirements/setup.md. Installing the product, making it the home screen, keeping
it there, and removing it. The project lead's answers of 2026-11-09 are written as decided; the
project lead also accepted the agent's proposals for the rest. -->

## REQ-setup-1: Set up once, by the project lead, from a computer

Release: first

The project lead installs the product and makes it the home screen by following one written
procedure from their computer, over ADB. Nobody else in the household has to do anything.

## REQ-setup-2: Google TV's home app keeps working

Release: first

After setup, Google TV's own home app is still installed and enabled, and its pages still open when
Google's search sends the household to them. (Search results are its pages; see the concept's
Feasibility section.)

## REQ-setup-3: It stays the home screen after a restart

Release: first

After the TV restarts or loses power, pressing Home still shows this product's home screen, with the
same apps, order, and hidden apps.

## REQ-setup-4: Recovering after an update or reset

Release: first

If a system update or a reset brings Google TV's home screen back, running the setup procedure
(REQ-setup-1) again makes this product the home screen again. After a system update the order and
hidden apps are kept; after a reset they may be lost.

## REQ-setup-5: One documented undo

Release: first

One written procedure, run by the project lead from their computer, removes the product and makes
Google TV's home screen the Home button again, with its search, apps, and settings as they were
before setup.

## Constraints

- No root access (concept, Non-goals).
- Not published on the Play Store; installed from the computer (concept, Non-goals).
- Setup may use ADB and accessibility permissions, because the product is for one household
  (concept, Audience).

## Success measures

- On the reference device, setup and undo each take the project lead under 15 minutes
  following the written procedure, and after undo Google TV's home screen looks as it did before
  setup, apart from the content in its rows.
