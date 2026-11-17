import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadWithServices } from './services-harness.js';

// A 32x18 red PNG, as the TV app would send a banner.
const RED = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAASCAIAAAC1qksFAAAAI0lEQVR4nGM8wUBbwERj80ctGLVg1IJRC0YtGLWAgYGBgQEA1SkA7IRfy9wAAAAASUVORK5CYII=';
const OLD = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';
const file = (apps, name = 'tv-banners.json') => ({ name, mimeType: 'application/json', buffer: Buffer.from(JSON.stringify({ from: 'Quiet Launcher TV', apps })) });

let ext;
let page;
before(async () => {
  ext = await loadWithServices();
  await ext.seed();
  await ext.worker.evaluate(old => chrome.storage.local.set({ pictures: { beta: old } }), OLD);
  page = await ext.launcher();
  await page.getByRole('button', { name: 'Edit services' }).click();
});
after(async () => { await ext?.close(); });

const pictures = () => ext.worker.evaluate(async () => (await chrome.storage.local.get('pictures')).pictures || {});
const settle = (ms = 500) => new Promise(r => setTimeout(r, ms));
const importTile = () => page.locator('#services .tile', { hasText: 'Import TV pictures' });
async function importFile(f) {
  const chooser = page.waitForEvent('filechooser');
  await importTile().click();
  await (await chooser).setFiles(f);
  await settle();
}
const size = src => page.evaluate(s => new Promise(r => { const i = new Image(); i.onload = () => r([i.width, i.height]); i.src = s; }), src);

test('tv-banners#AC-6: while editing, Import TV pictures follows Add a service and opens the file picker', async () => {
  const names = await page.locator('#services .grid').first().locator('.tile').allTextContents();
  assert.deepEqual(names.slice(-2).map(n => n.trim()), ['+ Add a service', 'Import TV pictures']);
  const chooser = page.waitForEvent('filechooser', { timeout: 3000 });
  await importTile().click();
  const c = await chooser;
  assert.equal(c.isMultiple(), false);
  await c.setFiles([]);
});

test('tv-banners#AC-9: a file that is not a TV pictures file changes nothing, and the status line says so', async () => {
  await importFile({ name: 'notes.json', mimeType: 'application/json', buffer: Buffer.from('{"hello": 1}') });
  assert.deepEqual(await pictures(), { beta: OLD });
  assert.equal((await page.locator('#status').textContent()).trim(),
    'That file isn’t a TV pictures file. Make one with quiet-banners on the PC.');
});

test('tv-banners#AC-7, AC-8: matched services, hidden ones too, get the TV banners; the status line counts them', async () => {
  await importFile(file([
    { name: 'ALPHA', package: 'com.example.alpha', picture: RED },
    { name: 'The Beta+', package: 'com.example.beta', picture: RED },
    { name: 'Hidden One', package: 'com.example.hidden', picture: RED },
    { name: 'Delta', package: 'com.example.delta', picture: RED },
  ]));
  const kept = await pictures();
  assert.deepEqual(Object.keys(kept).sort(), ['alpha', 'beta', 'hidden']);
  for (const id of ['alpha', 'beta', 'hidden']) {
    assert.match(kept[id], /^data:image\/png;base64,/);
    assert.deepEqual(await size(kept[id]), [32, 18], id);
  }
  assert.notEqual(kept.beta, OLD, 'a picture it had is replaced');
  assert.equal((await page.locator('#status').textContent()).trim(), 'Pictures set for 3 of 4 services. No TV banner for Gamma.');
  const alpha = page.locator('#services .tile', { hasText: 'Alpha' }).first();
  assert.equal(await alpha.locator('.banner img').getAttribute('src'), kept.alpha);
});

test('tv-banners#AC-6: Import TV pictures is not shown when not editing', async () => {
  await page.getByRole('button', { name: 'Done' }).click();
  assert.equal(await importTile().count(), 0);
});
