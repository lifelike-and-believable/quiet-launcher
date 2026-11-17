// The TV's banners, from the file quiet-banners saves on the PC (docs/specs/tv-banners.md, AC-7 to AC-9).

/** The file's apps, each with a name, package, and PNG picture; null if it is not a TV banners file. */
export function readBannersFile(text) {
  throw new Error('tv-banners#AC-9: not yet');
}

/** Each service matched to at most one app: { pictures: { id: picture }, unmatched: [name] }. */
export function matchBanners(services, apps) {
  throw new Error('tv-banners#AC-7: not yet');
}

/** The status line after an import. */
export function importStatus(matched, total, unmatched) {
  throw new Error('tv-banners#AC-8: not yet');
}
