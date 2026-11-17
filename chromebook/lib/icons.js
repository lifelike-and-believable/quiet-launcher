// Each service's own icon (docs/specs/chromebook-icons.md): the largest its site publishes for
// phones' home screens and installed web apps, fetched once and kept as image data.

const attributes = tag => Object.fromEntries(
  [...tag.matchAll(/([a-z-]+)\s*=\s*("([^"]*)"|'([^']*)'|([^\s>]+))/gi)]
    .map(m => [m[1].toLowerCase(), m[3] ?? m[4] ?? m[5]]));
const sizeOf = sizes => {
  if (!sizes) return 0;
  if (/\bany\b/i.test(sizes)) return 1024;
  return Math.max(0, ...sizes.split(/\s+/).map(s => parseInt(s.split(/x/i)[0], 10) || 0));
};
const isSvg = c => /svg/i.test(c.type || '') || /\.svg(\?|$)/i.test(c.url);

// AC-1: the icons a page's <link> tags offer, with addresses taken relative to the page.
export function findIconCandidates(html, pageUrl) {
  const found = [];
  for (const [tag] of (html || '').matchAll(/<link\b[^>]*>/gi)) {
    const a = attributes(tag);
    const rel = (a.rel || '').toLowerCase().split(/\s+/);
    if (!a.href || rel.includes('mask-icon')) continue;
    const touch = rel.some(r => r.startsWith('apple-touch-icon'));
    if (!touch && !rel.includes('icon')) continue;
    let url;
    try { url = new URL(a.href, pageUrl).href; } catch { continue; }
    found.push({ url, size: sizeOf(a.sizes) || (touch ? 180 : 16), type: a.type });
  }
  return found;
}

// The web app manifest a page names, if any.
export function findManifest(html, pageUrl) {
  for (const [tag] of (html || '').matchAll(/<link\b[^>]*>/gi)) {
    const a = attributes(tag);
    if ((a.rel || '').toLowerCase().split(/\s+/).includes('manifest') && a.href) {
      try { return new URL(a.href, pageUrl).href; } catch { return null; }
    }
  }
  return null;
}

// AC-1: the largest, passing over SVG and icons under 96 px when a larger one is offered.
export function chooseIcon(candidates) {
  if (!candidates.length) return null;
  const raster = candidates.filter(c => !isSvg(c));
  const pool = raster.length ? raster : candidates;
  return [...pool].sort((p, q) => q.size - p.size)[0];
}

function toBase64(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.length; i += 0x8000) binary += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  return btoa(binary);
}

// AC-2, AC-4, AC-5: the service's icon as a data: address, or null. Nothing is fetched unless the
// site is allowed, and nothing but the service's page, its manifest, and the icon they name.
export async function fetchServiceIcon(service, fetchFn = fetch, allowed = async () => false) {
  try {
    if (!(await allowed(service))) return null;
    const pageResponse = await fetchFn(service.url);
    const html = pageResponse.ok ? await pageResponse.text() : '';
    const candidates = findIconCandidates(html, service.url);
    const manifestUrl = findManifest(html, service.url);
    if (manifestUrl) {
      try {
        const m = await fetchFn(manifestUrl);
        const icons = m.ok ? ((await m.json()).icons || []) : [];
        for (const icon of icons) {
          if (!icon.src) continue;
          candidates.push({ url: new URL(icon.src, manifestUrl).href, size: sizeOf(icon.sizes), type: icon.type });
        }
      } catch { /* a manifest that cannot be read is left out */ }
    }
    const chosen = chooseIcon(candidates) || { url: new URL('/apple-touch-icon.png', service.url).href };
    const image = await fetchFn(chosen.url);
    const type = (image.headers.get('content-type') || '').split(';')[0].trim().toLowerCase();
    if (!image.ok || !type.startsWith('image/')) return null;
    return `data:${type};base64,${toBase64(await image.arrayBuffer())}`;
  } catch {
    return null;
  }
}
