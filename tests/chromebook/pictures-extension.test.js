import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadWithServices } from './services-harness.js';

// A 32x18 red PNG and a not-an-image file.
const PNG = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAACAAAAASCAIAAACWyxqdAAAAG0lEQVR42mP8z8Dwn4EIwDiqcFThqMJRhQAA2KgXAZ9WoYoAAAAASUVORK5CYII=', 'base64');
const ICON = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';

let ext;
let page;
before(async () => {
  ext = await loadWithServices();
  await ext.seed();
  await ext.worker.evaluate(icon => chrome.storage.local.set({ icons: { alpha: icon, beta: icon } }), ICON);
  page = await ext.launcher();
});
after(async () => { await ext?.close(); });

const tile = name => page.locator('#services .tile', { hasText: name }).first();
const menu = () => page.locator('[role="menu"]');
const pictures = () => ext.worker.evaluate(async () => (await chrome.storage.local.get('pictures')).pictures || {});
const settle = (ms = 400) => new Promise(r => setTimeout(r, ms));
async function choosePicture(name, file) {
  await tile(name).click();
  const chooser = page.waitForEvent('filechooser');
  await menu().getByRole('menuitem', { name: 'Choose picture', exact: true }).click();
  await (await chooser).setFiles(file);
  await settle();
}

test('chromebook-pictures#AC-1: a kept icon is shown at 100 px', async () => {
  await page.waitForFunction(() => document.querySelector('#services .tile img')?.src.startsWith('data:'));
  const size = await tile('Alpha').locator('img').evaluate(i => [i.getBoundingClientRect().width, i.getBoundingClientRect().height]);
  assert.deepEqual(size, [100, 100]);
});

test('chromebook-pictures#AC-2: the menu has Choose picture, and Remove picture only once there is one', async () => {
  await page.getByRole('button', { name: 'Edit services' }).click();
  await tile('Alpha').click();
  const items = await menu().getByRole('menuitem').allTextContents();
  assert.deepEqual(items, ['Move', 'Hide', 'Choose picture', 'Remove', 'Cancel']);
  await page.keyboard.press('Escape');
});

test('chromebook-pictures#AC-3: a chosen picture is kept, scaled, and fills the tile', async () => {
  await choosePicture('Alpha', { name: 'alpha.png', mimeType: 'image/png', buffer: PNG });
  const kept = (await pictures()).alpha;
  assert.match(kept || '', /^data:image\/png;base64,/);
  const img = tile('Alpha').locator('.banner img');
  assert.equal(await img.getAttribute('src'), kept);
  const [tw, th] = await tile('Alpha').locator('.banner').evaluate(b => [b.clientWidth, b.clientHeight]);
  const box = await img.evaluate(i => [i.getBoundingClientRect().width, i.getBoundingClientRect().height, getComputedStyle(i).objectFit]);
  assert.deepEqual(box, [tw, th, 'cover']);
  await tile('Alpha').click();
  assert.deepEqual(await menu().getByRole('menuitem').allTextContents(), ['Move', 'Hide', 'Choose picture', 'Remove picture', 'Remove', 'Cancel']);
  await page.keyboard.press('Escape');
});

test('chromebook-pictures#AC-3: a picture stays when icons are fetched again', async () => {
  await ext.worker.evaluate(() => globalThis.quietLauncher.refreshIcons());
  await settle();
  assert.match((await pictures()).alpha || '', /^data:image\/png/);
});

test('chromebook-pictures#AC-3: a big picture is scaled to fit 640 by 360', async () => {
  const big = await page.evaluate(() => {
    const c = document.createElement('canvas'); c.width = 1920; c.height = 1080;
    const x = c.getContext('2d'); x.fillStyle = '#0a0'; x.fillRect(0, 0, 1920, 1080);
    return c.toDataURL('image/jpeg', 0.9).split(',')[1];
  });
  await choosePicture('Beta', { name: 'beta.jpg', mimeType: 'image/jpeg', buffer: Buffer.from(big, 'base64') });
  const size = await page.evaluate(src => new Promise(r => { const i = new Image(); i.onload = () => r([i.width, i.height]); i.src = src; }), (await pictures()).beta);
  assert.deepEqual(size, [640, 360]);
});

test('chromebook-pictures#AC-5: a file that is not an image is not kept, and the status line says so', async () => {
  await choosePicture('Gamma', { name: 'notes.txt', mimeType: 'text/plain', buffer: Buffer.from('not a picture') });
  assert.equal((await pictures()).gamma, undefined);
  assert.equal((await page.locator('#status').textContent()).trim(), 'That file isn’t a picture.');
});

test('chromebook-pictures#AC-4: Remove picture goes back to the icon', async () => {
  await tile('Beta').click();
  await menu().getByRole('menuitem', { name: 'Remove picture', exact: true }).click();
  await settle();
  assert.equal((await pictures()).beta, undefined);
  assert.equal(await tile('Beta').locator('img').getAttribute('src'), ICON);
});

test('chromebook-pictures#AC-6: removing a service removes its picture', async () => {
  await tile('Alpha').click();
  await menu().getByRole('menuitem', { name: 'Remove', exact: true }).click();
  await page.getByRole('alertdialog').getByRole('button', { name: 'Remove' }).click();
  await settle();
  assert.equal((await pictures()).alpha, undefined);
});
