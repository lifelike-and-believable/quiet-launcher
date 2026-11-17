import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { loadWithServices } from './services-harness.js';

// A stand-in streaming site on localhost (allowed by the test copy's manifest) with an icon.
const PNG = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64');
let site;
let siteUrl;
let ext;
let page;
before(async () => {
  site = createServer((req, res) => {
    if (req.url === '/icon-512.png') { res.writeHead(200, { 'content-type': 'image/png' }); return res.end(PNG); }
    res.writeHead(200, { 'content-type': 'text/html' });
    res.end('<!doctype html><link rel="apple-touch-icon" sizes="512x512" href="/icon-512.png"><h1>Delta</h1>');
  });
  await new Promise(r => site.listen(0, '127.0.0.1', r));
  siteUrl = `http://localhost:${site.address().port}/`;
  ext = await loadWithServices();
  ext.services.push({ id: 'delta', name: 'Delta', url: siteUrl, site: 'localhost', colour: '#0f4c81', hidden: false });
  await ext.seed();
  page = await ext.launcher();
});
after(async () => { await ext?.close(); site?.close(); });

const icons = () => ext.worker.evaluate(async () => (await chrome.storage.local.get('icons')).icons || {});
const tileIcon = name => page.locator('#services .tile', { hasText: name }).first().locator('img').getAttribute('src');
const waitFor = async (check, ms = 5000) => {
  for (let t = 0; t < ms; t += 100) { if (await check()) return true; await new Promise(r => setTimeout(r, 100)); }
  return false;
};

test('chromebook-icons#AC-6, AC-2: a service with no kept icon gets its own when the launcher opens', async () => {
  assert.ok(await waitFor(async () => Boolean((await icons()).delta)), 'Delta\'s icon was kept');
  assert.equal((await icons()).delta, `data:image/png;base64,${PNG.toString('base64')}`);
});

test('chromebook-icons#AC-3: the tile shows the kept icon', async () => {
  assert.ok(await waitFor(async () => (await tileIcon('Delta'))?.startsWith('data:image/png')), 'Delta shows its icon');
});

test('chromebook-icons#AC-5: a site whose icon cannot be had keeps Chrome\'s small icon, with no message', async () => {
  assert.match(await tileIcon('Alpha'), /^\/_favicon\//);
  assert.equal((await icons()).alpha, undefined);
  assert.equal((await page.locator('#status').textContent()).trim(), '');
});

test('chromebook-icons#AC-4: a site Chrome has not allowed is not fetched', async () => {
  assert.equal((await icons()).gamma, undefined);
  assert.match(await tileIcon('Gamma'), /^\/_favicon\//);
});

test('chromebook-icons#AC-7: removing a service removes its kept icon', async () => {
  await page.getByRole('button', { name: 'Edit services' }).click();
  await page.locator('#services .tile', { hasText: 'Delta' }).first().click();
  await page.locator('[role="menu"]').getByRole('menuitem', { name: 'Remove', exact: true }).click();
  await page.getByRole('alertdialog').getByRole('button', { name: 'Remove' }).click();
  assert.ok(await waitFor(async () => (await icons()).delta === undefined), 'its icon is gone');
});
