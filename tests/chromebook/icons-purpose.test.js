import { test } from 'node:test';
import assert from 'node:assert/strict';
import { fetchServiceIcon } from '../../chromebook/lib/icons.js';

const PAGE = 'https://www.example.tv/';
const PNG = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 9]).buffer;
const answer = (type, body) => ({ ok: true, status: 200, headers: new Map([['content-type', type]]),
  text: async () => body, json: async () => JSON.parse(body), arrayBuffer: async () => PNG });

test('chromebook-icons#AC-9: a manifest icon only for monochrome use is passed over', async () => {
  const asked = [];
  const f = async url => {
    asked.push(url);
    if (url === PAGE) return answer('text/html', '<link rel="manifest" href="/m.json">');
    if (url === 'https://www.example.tv/m.json') return answer('application/json', JSON.stringify({ icons: [
      { src: '/mono-512.png', sizes: '512x512', type: 'image/png', purpose: 'monochrome' },
      { src: '/colour-192.png', sizes: '192x192', type: 'image/png', purpose: 'any maskable' },
    ] }));
    return answer('image/png', '');
  };
  await fetchServiceIcon({ id: 'x', url: PAGE, site: 'example.tv' }, f, async () => true);
  assert.equal(asked.at(-1), 'https://www.example.tv/colour-192.png');
});
