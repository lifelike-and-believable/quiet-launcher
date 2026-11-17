import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadWithServices, playing } from './services-harness.js';

let ext;
let page;
before(async () => { ext = await loadWithServices(); await ext.seed(); page = await ext.launcher(); });
after(async () => { await ext?.close(); });

const settle = (ms = 500) => new Promise(r => setTimeout(r, ms));
const searchWindows = async () => (await ext.windows()).filter(w => w.urls.some(u => u.startsWith('https://www.justwatch.com/ca/search')));
const box = () => page.getByRole('searchbox', { name: 'Search for a show' });

test('chromebook-search#AC-3: while the box is selected, the status line says where results open', async () => {
  await box().focus();
  assert.equal((await page.locator('#status').textContent()).trim(),
    'Results open from JustWatch, in their own full-screen window. Enter searches.');
  await page.getByRole('button', { name: 'Edit services' }).focus();
  assert.equal((await page.locator('#status').textContent()).trim(), '');
});

test('chromebook-search#AC-1: Enter with nothing typed does nothing', async () => {
  await box().fill('   ');
  await box().press('Enter');
  await settle();
  assert.equal((await searchWindows()).length, 0);
});

test('chromebook-search#AC-1: Enter opens JustWatch for the text in a full-screen window and empties the box', async () => {
  await box().fill('Carrie');
  await box().press('Enter');
  await settle(1500);
  const [w] = await searchWindows();
  assert.ok(w, 'a search window');
  assert.equal(w.urls[0], 'https://www.justwatch.com/ca/search?q=Carrie');
  assert.equal(w.type, 'popup');
  assert.equal(w.state, 'fullscreen');
  assert.equal(await box().inputValue(), '');
});

test('chromebook-search#AC-4: following a link to a service makes the window that service\'s, Open and paused', async () => {
  const [w] = await searchWindows();
  await ext.worker.evaluate(async ([id, url]) => {
    const [tab] = await chrome.tabs.query({ windowId: id });
    await chrome.tabs.update(tab.id, { url });
  }, [w.id, ext.url('localhost', 'beta')]);
  let service;
  for (let i = 0; i < 50 && !service; i++) {
    service = ext.context.pages().find(p => p.url().startsWith(ext.url('localhost', 'beta')));
    if (!service) await settle(100);
  }
  await service.waitForLoadState();
  await settle();
  page = await ext.launcher();
  await settle();
  assert.match(await page.locator('#services .tile', { hasText: 'Beta' }).first().textContent(), /\bOpen\b/);
  await ext.worker.evaluate(id => chrome.windows.update(id, { focused: true }), w.id);
  await service.evaluate(() => [document, document.querySelector('iframe').contentDocument]
    .flatMap(d => [...d.querySelectorAll('video')]).forEach(v => v.play()));
  assert.equal(await playing(service), true);
  await ext.call('goHome');
  await settle();
  assert.equal(await playing(service), false, 'go-home paused it');
  await ext.call('openService', 'beta');
  await settle();
  assert.equal(await ext.focusedId(), w.id, 'choosing Beta brings that window back');
});

test('chromebook-search#AC-5: nothing about the search is stored', async () => {
  const stored = await ext.worker.evaluate(async () => JSON.stringify([await chrome.storage.local.get(null), await chrome.storage.session.get(null)]));
  assert.doesNotMatch(stored, /Carrie/);
});
