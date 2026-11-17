import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadWithServices, playing } from './services-harness.js';

let ext;
before(async () => { ext = await loadWithServices(); await ext.seed(); });
after(async () => { await ext?.close(); });

const tiles = page => page.locator('#services .tile');
const tile = (page, name) => page.locator('#services .tile', { hasText: name }).first();
const windowsFor = async url => (await ext.windows()).filter(w => w.urls.some(u => u === url));
const settle = () => new Promise(r => setTimeout(r, 400));

test('chromebook-services#AC-1: the services in order, hidden ones left out, the first selected', async () => {
  const page = await ext.launcher();
  const names = await tiles(page).evaluateAll(els => els.map(e => e.querySelector('.name').textContent.trim()));
  assert.deepEqual(names, ['Alpha', 'Beta', 'Gamma']);
  const icon = await tile(page, 'Alpha').locator('img').getAttribute('src');
  assert.match(icon, /^\/_favicon\/\?pageUrl=/);
  assert.match(await page.evaluate(() => document.activeElement?.className || ''), /\btile\b/);
  assert.match(await page.evaluate(() => document.activeElement?.textContent || ''), /Alpha/);
});

test('chromebook-services#AC-2: choosing a service opens it in a new full-screen popup window', async () => {
  const page = await ext.launcher();
  await tile(page, 'Alpha').click();
  const service = await ext.page(ext.url('localhost', 'alpha'));
  await service.waitForLoadState();
  await settle();
  const [w] = await windowsFor(ext.url('localhost', 'alpha'));
  assert.ok(w, 'a window for Alpha');
  assert.equal(w.type, 'popup');
  assert.equal(w.state, 'fullscreen');
  assert.equal(await ext.focusedId(), w.id);
});

test('chromebook-services#AC-4: an open service is marked Open', async () => {
  const page = await ext.launcher();
  await settle();
  assert.match(await tile(page, 'Alpha').textContent(), /\bOpen\b/);
  assert.doesNotMatch(await tile(page, 'Beta').textContent(), /\bOpen\b/);
  assert.match(await tile(page, 'Alpha').getAttribute('aria-label'), /Alpha, Open/);
});

test('chromebook-services#AC-5: go-home from a playing service pauses it, in every frame', async () => {
  const service = await ext.page(ext.url('localhost', 'alpha'));
  const [w] = await windowsFor(ext.url('localhost', 'alpha'));
  await service.evaluate(() => [document, document.querySelector('iframe').contentDocument]
    .flatMap(d => [...d.querySelectorAll('video')]).forEach(v => v.play()));
  await ext.worker.evaluate(id => chrome.windows.update(id, { focused: true }), w.id);
  assert.equal(await playing(service), true);
  await ext.call('goHome');
  await settle();
  assert.equal(await playing(service), false);
  const launcher = (await windowsFor(ext.launcherUrl))[0];
  assert.equal(await ext.focusedId(), launcher.id);
});

test('chromebook-services#AC-3: choosing an open service brings its window back without reloading it', async () => {
  const hitsBefore = ext.hits.alpha;
  const page = await ext.launcher();
  await tile(page, 'Alpha').click();
  await settle();
  const windows = await windowsFor(ext.url('localhost', 'alpha'));
  assert.equal(windows.length, 1);
  assert.equal(await ext.focusedId(), windows[0].id);
  assert.equal(ext.hits.alpha, hitsBefore, 'not loaded again');
});

test('chromebook-services#AC-7: go-last brings the last service back', async () => {
  await ext.launcher();
  await ext.call('goLast');
  await settle();
  const [w] = await windowsFor(ext.url('localhost', 'alpha'));
  assert.equal(await ext.focusedId(), w.id);
});

test('chromebook-services#AC-4: the Open marker goes when the service window closes', async () => {
  const [w] = await windowsFor(ext.url('localhost', 'alpha'));
  await ext.worker.evaluate(id => chrome.windows.remove(id), w.id);
  const page = await ext.launcher();
  await page.waitForFunction(() => !/\bOpen\b/.test(document.querySelector('#services .tile').textContent), null, { timeout: 3000 });
});

test('chromebook-services#AC-8: a service without site permission says so, opens, and is not paused', async () => {
  const page = await ext.launcher();
  assert.match(await tile(page, 'Gamma').textContent(), /Videos won.t pause/);
  assert.match(await tile(page, 'Gamma').getAttribute('aria-label'), /Gamma, Videos won.t pause/);
  assert.doesNotMatch(await tile(page, 'Alpha').textContent(), /Videos won.t pause/);
  await tile(page, 'Gamma').click();
  const service = await ext.page(ext.url('127.0.0.1', 'gamma'));
  await service.waitForLoadState();
  await settle();
  assert.equal(await playing(service), true);
  await ext.call('goHome');
  await settle();
  assert.equal(await playing(service), true, 'not paused');
  const launcher = await ext.launcher();
  assert.equal((await launcher.locator('#status').textContent()).trim(), '');
});

test('chromebook-services#AC-9: arrow keys move between tiles and up to the top row; Enter opens a tile', async () => {
  const page = await ext.launcher();
  const selected = () => page.evaluate(() => {
    const el = document.activeElement;
    return el?.querySelector?.('.name')?.textContent.trim() || el?.getAttribute('aria-label') || el?.textContent.trim();
  });
  await tile(page, 'Alpha').focus();
  await page.keyboard.press('ArrowLeft');
  assert.equal(await selected(), 'Alpha', 'no wrapping');
  await page.keyboard.press('ArrowRight');
  assert.equal(await selected(), 'Beta');
  await page.keyboard.press('ArrowUp');
  assert.ok(['Search for a show', 'Edit services', 'Back to browsing'].includes(await selected()), 'up to the top row');
  await page.keyboard.press('ArrowDown');
  assert.ok(['Alpha', 'Beta', 'Gamma'].includes(await selected()), 'down to a tile');
  await tile(page, 'Beta').focus();
  await page.keyboard.press('Enter');
  const service = await ext.page(ext.url('localhost', 'beta'));
  await service.waitForLoadState();
  assert.equal((await windowsFor(ext.url('localhost', 'beta'))).length, 1);
});
