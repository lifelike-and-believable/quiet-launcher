import { test } from 'node:test';
import assert from 'node:assert/strict';
import { findIconCandidates, chooseIcon, fetchServiceIcon } from '../../chromebook/lib/icons.js';

const PAGE = 'https://www.example.tv/browse';

test('chromebook-icons#AC-1: icon links with sizes, relative to the page', () => {
  const html = `<html><head>
    <link rel="icon" href="/favicon-32.png" sizes="32x32">
    <link rel="apple-touch-icon" href="touch-180.png" sizes="180x180">
    <link href="https://cdn.example.tv/i/512.png" rel="icon" sizes="512x512" type="image/png">
    <link rel="stylesheet" href="/site.css">
  </head></html>`;
  const found = findIconCandidates(html, PAGE);
  assert.deepEqual(found.map(c => [c.url, c.size]).sort(), [
    ['https://cdn.example.tv/i/512.png', 512],
    ['https://www.example.tv/favicon-32.png', 32],
    ['https://www.example.tv/touch-180.png', 180],
  ]);
  assert.equal(chooseIcon(found).url, 'https://cdn.example.tv/i/512.png');
});

test('chromebook-icons#AC-1: an apple-touch-icon without sizes counts as 180', () => {
  const found = findIconCandidates(`<link rel='apple-touch-icon-precomposed' href='/apple.png'>`, PAGE);
  assert.deepEqual(found.map(c => [c.url, c.size]), [['https://www.example.tv/apple.png', 180]]);
});

test('chromebook-icons#AC-1: SVG and small icons are passed over when a larger one is offered', () => {
  const choice = chooseIcon([
    { url: 'https://x/a.svg', size: 1024, type: 'image/svg+xml' },
    { url: 'https://x/b.png', size: 64 },
    { url: 'https://x/c.png', size: 192 },
  ]);
  assert.equal(choice.url, 'https://x/c.png');
  assert.equal(chooseIcon([{ url: 'https://x/b.png', size: 64 }]).url, 'https://x/b.png', 'a small one is better than none');
  assert.equal(chooseIcon([]), null);
});

// A stand-in for fetch: answers from a table of address to { type, body }, and records each address.
function fakeFetch(table) {
  const asked = [];
  const f = async url => {
    asked.push(url);
    const r = table[url];
    if (!r) return { ok: false, status: 404, headers: new Map([['content-type', 'text/html']]), text: async () => '', json: async () => ({}), arrayBuffer: async () => new ArrayBuffer(0) };
    return {
      ok: true, status: 200, headers: new Map([['content-type', r.type]]),
      text: async () => String(r.body), json: async () => JSON.parse(r.body),
      arrayBuffer: async () => (typeof r.body === 'string' ? new TextEncoder().encode(r.body).buffer : r.body),
    };
  };
  f.asked = asked;
  return f;
}
const PNG = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 1, 2, 3]).buffer;
const service = { id: 'tv', name: 'Example', url: PAGE, site: 'example.tv' };

test('chromebook-icons#AC-1, AC-2: the largest of the manifest\'s and the page\'s icons, kept as image data', async () => {
  const f = fakeFetch({
    [PAGE]: { type: 'text/html', body: '<link rel="manifest" href="/app.webmanifest"><link rel="apple-touch-icon" href="/t.png">' },
    'https://www.example.tv/app.webmanifest': { type: 'application/manifest+json', body: JSON.stringify({ icons: [
      { src: 'icons/192.png', sizes: '192x192', type: 'image/png' }, { src: 'icons/512.png', sizes: '512x512', type: 'image/png' }] }) },
    'https://www.example.tv/icons/512.png': { type: 'image/png', body: PNG },
  });
  const data = await fetchServiceIcon(service, f, async () => true);
  assert.equal(data, `data:image/png;base64,${Buffer.from(PNG).toString('base64')}`);
});

test('chromebook-icons#AC-1: with no icon named, /apple-touch-icon.png on the site', async () => {
  const f = fakeFetch({
    [PAGE]: { type: 'text/html', body: '<html><head><title>x</title></head></html>' },
    'https://www.example.tv/apple-touch-icon.png': { type: 'image/png', body: PNG },
  });
  assert.match(await fetchServiceIcon(service, f, async () => true), /^data:image\/png;base64,/);
});

test('chromebook-icons#AC-2, AC-5: a response that is not an image is not kept, and nothing fails', async () => {
  const f = fakeFetch({
    [PAGE]: { type: 'text/html', body: '<link rel="apple-touch-icon" href="/t.png">' },
    'https://www.example.tv/t.png': { type: 'text/html', body: '<html>not found</html>' },
  });
  assert.equal(await fetchServiceIcon(service, f, async () => true), null);
  assert.equal(await fetchServiceIcon(service, async () => { throw new Error('offline'); }, async () => true), null);
});

test('chromebook-icons#AC-4: nothing is fetched for a site Chrome has not allowed', async () => {
  const f = fakeFetch({ [PAGE]: { type: 'text/html', body: '' } });
  assert.equal(await fetchServiceIcon(service, f, async () => false), null);
  assert.deepEqual(f.asked, []);
});

test('chromebook-icons#AC-4: only the service\'s page and the icon addresses it names are fetched', async () => {
  const f = fakeFetch({
    [PAGE]: { type: 'text/html', body: '<link rel="apple-touch-icon" href="https://cdn.example.tv/t.png">' },
    'https://cdn.example.tv/t.png': { type: 'image/png', body: PNG },
  });
  await fetchServiceIcon(service, f, async () => true);
  assert.deepEqual(f.asked, [PAGE, 'https://cdn.example.tv/t.png']);
});
