// The changes editing makes to the list of services (docs/specs/chromebook-editing.md). Each
// returns a new list and leaves the one it was given alone.

// A service's site: its address's host, without "www." (chromebook-editing#AC-7).
export function siteOf(url) {
  return new URL(url).hostname.replace(/^www\./, '');
}

// The permission asked for on a site (chromebook-editing#AC-7, AC-11): in the service's own scheme,
// since Chrome refuses "*://" against a manifest that lists https://*/* and http://*/*; covering
// the site's subdomains, except for localhost or an IP address, which have none.
export function permissionPattern(site, url = 'https://') {
  const scheme = url.startsWith('http://') ? 'http' : 'https';
  const bare = !site.includes('.') || /^\d+(\.\d+){3}$/.test(site) || site.includes(':');
  return `${scheme}://${bare ? '' : '*.'}${site}/*`;
}

// A service from an address the viewer typed, or null if it is not a site address (AC-7).
export function serviceFromAddress(text) {
  let raw = (text || '').trim();
  if (!raw || /\s/.test(raw)) return null;
  // "host:port/path" has no scheme; "javascript:..." and other bare schemes are refused below.
  if (!/^[a-z][a-z0-9+.-]*:\/\//i.test(raw)) {
    if (/^[a-z][a-z0-9+.-]*:(?!\d)/i.test(raw)) return null;
    raw = `https://${raw}`;
  }
  let url;
  try { url = new URL(raw); } catch { return null; }
  if (!['http:', 'https:'].includes(url.protocol) || !url.hostname) return null;
  if (!url.hostname.includes('.') && url.hostname !== 'localhost') return null;
  const site = siteOf(url.href);
  return { name: site, url: url.href, site, colour: '#2b343c' };
}

const newId = name => `${name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'service'}-${Math.random().toString(36).slice(2, 8)}`;

// Adds a service at the end, visible, with an id of its own (AC-7).
export function addService(list, { name, url, colour }) {
  const service = { id: newId(name), name, url, site: siteOf(url), colour, hidden: false };
  return { list: [...list, service], service };
}

// Puts a service at a new place in the list, the rest keeping their order (AC-3).
export function moveService(list, id, to) {
  const from = list.findIndex(s => s.id === id);
  if (from < 0) return list;
  const rest = list.filter(s => s.id !== id);
  const at = Math.max(0, Math.min(to, rest.length));
  return [...rest.slice(0, at), list[from], ...rest.slice(at)];
}

// Hides or shows a service where it is (AC-4).
export function setHidden(list, id, hidden) {
  return list.map(s => (s.id === id ? { ...s, hidden } : s));
}

// Takes a service out of the list (AC-5).
export function removeService(list, id) {
  return list.filter(s => s.id !== id);
}
