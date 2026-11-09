# quiet-launcher

A home screen for Google TV that shows the household's apps without suggestions or advertising,
and keeps search. The intent is in `docs/product/concept.md`; device facts and findings are in
`docs/product/notes.md`.

## Development process

This project uses the rfr-process development process from
[rules-for-robots](https://github.com/lifelike-and-believable/rules-for-robots/blob/main/plugins/process/README.md),
in solo mode (`docs/product/process.yml`).

- Agents do not merge pull requests that touch `docs/product/`, `docs/design/`, `docs/user/`, or
  `docs/tutorials/` unless the approver explicitly says to, and then comment on the pull request
  quoting the instruction.
- An artifact is approved by the pull request that sets its `status: approved` and records its
  link hashes (`node ../rules-for-robots/checks/process-relink.mjs <id>`, from the repository root,
  with rules-for-robots checked out beside this repository). Merging a pull request whose artifacts
  still say `draft` approves nothing.
- Check the artifacts with `node ../rules-for-robots/checks/process-trace.mjs .`.

## Project type

An Android TV app. No rfr-process profile fits it, so it uses the `web-app` profile's templates
(`plugins/process/templates/profiles/web-app/` in rules-for-robots) for personas, user
documentation, and tutorials. Tutorials will need `runner: manual` until there is an on-device
runner.

## Working with the reference device

The reference device is reachable over ADB (see `docs/product/notes.md`). Changing its settings,
installing or removing apps, or changing which app is the home screen affects the household's TV:
ask the project lead before each such change.
