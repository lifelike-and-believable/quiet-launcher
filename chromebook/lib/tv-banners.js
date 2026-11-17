// The TV's banners, from the file quiet-banners saves on the PC (docs/specs/tv-banners.md, AC-7 to AC-9).

const PNG = /^data:image\/png;base64,[A-Za-z0-9+/=]+$/;

/** The file's apps, each with a name, package, and PNG picture; null if it is not a TV banners file. */
export function readBannersFile(text) {
  let file;
  try {
    file = JSON.parse(String(text).replace(/^﻿/, ''));
  } catch {
    return null;
  }
  if (!file || !Array.isArray(file.apps)) return null;
  const apps = file.apps.filter(a => a && typeof a.name === 'string' && typeof a.package === 'string' &&
    typeof a.picture === 'string' && PNG.test(a.picture));
  return apps.length ? apps.map(({ name, package: pkg, picture }) => ({ name, package: pkg, picture })) : null;
}

// AC-7: case, spaces, punctuation, a leading "The", and a trailing "+" do not count.
const nameKey = name => name.toLowerCase().trim().replace(/^the\s+/, '').replace(/[^a-z0-9]/g, '');

// The site's own name: youtube for youtube.com, www.youtube.com, or m.youtube.com; cbc for gem.cbc.ca.
function siteName(site) {
  const parts = String(site || '').toLowerCase().split('.').filter(Boolean);
  if (parts.length < 2) return parts[0] || '';
  return parts[parts.length - 2];
}

/** Each service matched to at most one app: { pictures: { id: picture }, unmatched: [name] }. */
export function matchBanners(services, apps) {
  const pictures = {};
  const unmatched = [];
  for (const s of services) {
    const key = nameKey(s.name);
    let app = apps.find(a => nameKey(a.name) === key);
    if (!app) {
      const label = siteName(s.site);
      const byPackage = label ? apps.filter(a => a.package.toLowerCase().split('.').includes(label)) : [];
      if (byPackage.length === 1) app = byPackage[0];
    }
    if (app) pictures[s.id] = app.picture;
    else unmatched.push(s.name);
  }
  return { pictures, unmatched };
}

/** The status line after an import (AC-8). */
export function importStatus(matched, total, unmatched) {
  const set = `Pictures set for ${matched} of ${total} services.`;
  return unmatched.length ? `${set} No TV banner for ${unmatched.join(', ')}.` : set;
}
