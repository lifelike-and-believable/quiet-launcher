// docs/specs/chromebook-one-service.md, AC-7: in the extension, in Chromium.
import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadWithServices } from './services-harness.js';

let ext;
before(async () => { ext = await loadWithServices(); await ext.seed(); });
after(async () => { await ext?.close(); });

const tile = (page, name) => page.locator('#services .tile', { hasText: name }).first();
const windowsFor = async url => (await ext.windows()).filter(w => w.urls.some(u => u === url));
const settle = () => new Promise(r => setTimeout(r, 400));

test('chromebook-one-service#AC-7: choosing one service, then another, leaves only the second open, marked Open', async () => {
  let page = await ext.launcher();
  await tile(page, 'Alpha').click();
  await (await ext.page(ext.url('localhost', 'alpha'))).waitForLoadState();
  await settle();
  await ext.call('goHome');
  page = await ext.launcher();
  await tile(page, 'Beta').click();
  await (await ext.page(ext.url('localhost', 'beta'))).waitForLoadState();
  await settle();
  assert.equal((await windowsFor(ext.url('localhost', 'alpha'))).length, 0, "Alpha's window closed");
  assert.equal((await windowsFor(ext.url('localhost', 'beta'))).length, 1, 'Beta is open');
  await ext.call('goHome');
  page = await ext.launcher();
  await settle();
  assert.match(await tile(page, 'Beta').textContent(), /\bOpen\b/);
  assert.doesNotMatch(await tile(page, 'Alpha').textContent(), /\bOpen\b/);
});
