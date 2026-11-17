import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadWithServices } from './services-harness.js';

let ext;
before(async () => {
  ext = await loadWithServices();
  await ext.seed();
  await ext.worker.evaluate(() => chrome.storage.local.set({ icons: {
    alpha: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==' } }));
});
after(async () => { await ext?.close(); });

test('chromebook-icons#AC-10: the name is shown once, under the tile, not on it', async () => {
  const page = await ext.launcher();
  await page.waitForFunction(() => document.querySelector('#services .tile img')?.src.startsWith('data:'));
  for (const name of ['Alpha', 'Beta']) {
    const tile = page.locator('#services .tile', { hasText: name }).first();
    assert.equal((await tile.locator('.banner').textContent()).trim(), '', `${name}: no text on the tile`);
    assert.equal((await tile.locator('.name').textContent()).trim(), name);
  }
});

test('chromebook-icons#AC-10: Chrome\'s small icon is shown at 48 px on a tile with no kept icon', async () => {
  const page = await ext.launcher();
  const size = await page.locator('#services .tile', { hasText: 'Beta' }).first().locator('img')
    .evaluate(img => [img.getBoundingClientRect().width, img.getBoundingClientRect().height]);
  assert.deepEqual(size, [48, 48]);
});
