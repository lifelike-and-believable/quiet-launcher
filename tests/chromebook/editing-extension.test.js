import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadWithServices } from './services-harness.js';

let ext;
let page;
before(async () => {
  ext = await loadWithServices();
  await ext.seed();
  page = await ext.launcher();
});
after(async () => { await ext?.close(); });

const stored = () => ext.worker.evaluate(async () => (await chrome.storage.local.get('services')).services);
const storedNames = async () => (await stored()).map(s => s.name);
const tile = name => page.locator('#services .tile', { hasText: name }).first();
const menu = () => page.locator('[role="menu"]');
const announced = () => page.locator('#announce').textContent();
// Chrome's permission prompt cannot be answered in a test, so the page's request is replaced.
const answerPermission = allow => page.evaluate(a => {
  window.permissionAsks = [];
  chrome.permissions.request = async req => { window.permissionAsks.push(req); return a; };
}, allow);
const settle = () => new Promise(r => setTimeout(r, 300));

test('chromebook-editing#AC-1: Edit services starts editing', async () => {
  await page.getByRole('button', { name: 'Edit services' }).click();
  assert.equal(await page.getByRole('button', { name: 'Done' }).count(), 1);
  assert.equal((await page.locator('#hint').textContent()).trim(),
    'Select a service to move, hide, or remove it. Changes are saved as you go.');
  assert.equal(await page.locator('#services .add').count(), 1);
  assert.match(await page.locator('#services').textContent(), /Hidden services: not on the launcher\. Select one to show it again\./);
  assert.match(await page.locator('#services').textContent(), /Hidden one/);
  assert.match(await announced(), /Editing services/);
});

test('chromebook-editing#AC-2: selecting a service opens its menu, and does not open the service', async () => {
  const windowsBefore = (await ext.windows()).length;
  await tile('Alpha').click();
  assert.deepEqual(await menu().getByRole('menuitem').allTextContents(), ['Move', 'Hide', 'Choose picture', 'Remove', 'Cancel']);
  await page.keyboard.press('Escape');
  assert.equal(await menu().count(), 0);
  await tile('Gamma').click();
  assert.deepEqual(await menu().getByRole('menuitem').allTextContents(), ['Move', 'Hide', 'Choose picture', 'Allow pausing', 'Remove', 'Cancel']);
  await menu().getByRole('menuitem', { name: 'Cancel' }).click();
  assert.equal(await menu().count(), 0);
  assert.equal((await ext.windows()).length, windowsBefore, 'no service opened');
});

test('chromebook-editing#AC-3: Move, Right, Enter moves a service one place and saves it', async () => {
  await tile('Alpha').click();
  await menu().getByRole('menuitem', { name: 'Move', exact: true }).click();
  await page.keyboard.press('ArrowRight');
  assert.match(await announced(), /Alpha, moved to 2 of 3/);
  await page.keyboard.press('Enter');
  await settle();
  assert.deepEqual(await storedNames(), ['Beta', 'Alpha', 'Hidden one', 'Gamma']);
});

test('chromebook-editing#AC-3: Esc puts a lifted service back where it was', async () => {
  await tile('Gamma').click();
  await menu().getByRole('menuitem', { name: 'Move', exact: true }).click();
  await page.keyboard.press('ArrowLeft');
  await page.keyboard.press('Escape');
  await settle();
  assert.deepEqual(await storedNames(), ['Beta', 'Alpha', 'Hidden one', 'Gamma']);
  const shown = await page.locator('#services .tile:not(.add):not(.hidden)').evaluateAll(els => els.map(e => e.querySelector('.name').textContent.trim()));
  assert.deepEqual(shown, ['Beta', 'Alpha', 'Gamma']);
});

test('chromebook-editing#AC-3: dragging a service moves it and saves the order', async () => {
  const from = await tile('Gamma').boundingBox();
  const to = await tile('Beta').boundingBox();
  await page.mouse.move(from.x + from.width / 2, from.y + 40);
  await page.mouse.down();
  await page.mouse.move(to.x + 20, to.y + 40, { steps: 8 });
  await page.mouse.up();
  await settle();
  const visible = (await stored()).filter(s => !s.hidden).map(s => s.name);
  assert.deepEqual(visible, ['Gamma', 'Beta', 'Alpha']);
});

test('chromebook-editing#AC-4: Hide and Show', async () => {
  await tile('Beta').click();
  await menu().getByRole('menuitem', { name: 'Hide' }).click();
  await settle();
  assert.equal((await stored()).find(s => s.name === 'Beta').hidden, true);
  assert.equal(await page.locator('#services .tile.hidden', { hasText: 'Beta' }).count(), 1);
  await page.locator('#services .tile.hidden', { hasText: 'Beta' }).click();
  assert.deepEqual(await menu().getByRole('menuitem').allTextContents(), ['Move', 'Show', 'Choose picture', 'Remove', 'Cancel']);
  await menu().getByRole('menuitem', { name: 'Show' }).click();
  await settle();
  assert.equal((await stored()).find(s => s.name === 'Beta').hidden, false);
  assert.deepEqual((await stored()).filter(s => !s.hidden).map(s => s.name), ['Gamma', 'Beta', 'Alpha'], 'back in its place');
});

test('chromebook-editing#AC-5: Remove asks first, then removes the service and closes its window', async () => {
  await ext.call('openService', 'alpha');
  page = await ext.launcher();
  if (!(await page.getByRole('button', { name: 'Done' }).count())) await page.getByRole('button', { name: 'Edit services' }).click();
  await tile('Alpha').click();
  await menu().getByRole('menuitem', { name: 'Remove' }).click();
  const dialog = page.getByRole('alertdialog');
  assert.match(await dialog.textContent(), /Remove Alpha\? Its window will close\./);
  await dialog.getByRole('button', { name: 'Cancel' }).click();
  assert.ok((await storedNames()).includes('Alpha'), 'Cancel keeps it');
  await tile('Alpha').click();
  await menu().getByRole('menuitem', { name: 'Remove' }).click();
  await page.getByRole('alertdialog').getByRole('button', { name: 'Remove' }).click();
  await settle();
  assert.ok(!(await storedNames()).includes('Alpha'));
  assert.equal((await ext.windows()).filter(w => w.urls.some(u => u === ext.url('localhost', 'alpha'))).length, 0, 'its window closed');
});

test('chromebook-editing#AC-6: Add a service lists services in Canada and an address field', async () => {
  await page.locator('#services .add').click();
  const panel = page.getByRole('dialog', { name: 'Add a service' });
  assert.equal(await panel.count(), 1);
  const choices = await panel.locator('.choice').allTextContents();
  for (const name of ['Prime Video', 'YouTube', 'Apple TV+', 'Criterion Channel', 'MUBI', 'CBC Gem', 'Tubi', 'Plex', 'myfilmfriend', 'Crave']) {
    assert.ok(choices.some(c => c.includes(name)), name);
  }
  assert.equal(await panel.getByRole('textbox').count(), 1);
  assert.equal(await panel.getByRole('button', { name: 'Add' }).count(), 1);
  await page.keyboard.press('Escape');
  assert.equal(await panel.count(), 0);
});

test('chromebook-editing#AC-7: choosing one adds it at the end, asking nothing; one out of reach says it will not pause', async () => {
  await answerPermission(false);
  await page.locator('#services .add').click();
  await page.getByRole('dialog', { name: 'Add a service' }).locator('.choice', { hasText: 'Crave' }).click();
  await settle();
  assert.deepEqual(await page.evaluate(() => window.permissionAsks), [], 'nothing was asked');
  const list = await stored();
  assert.equal(list[list.length - 1].name, 'Crave');
  assert.equal(list[list.length - 1].url, 'https://www.crave.ca/');
  assert.match(await tile('Crave').textContent(), /Videos won.t pause/);
});

test('chromebook-editing#AC-6: services already added are marked and not offered again', async () => {
  await page.locator('#services .add').click();
  const crave = page.getByRole('dialog', { name: 'Add a service' }).locator('.choice', { hasText: 'Crave' });
  assert.equal(await crave.getAttribute('aria-disabled'), 'true');
  await page.keyboard.press('Escape');
});

test('chromebook-editing#AC-7: an address is added by name of its site; text that is not one is refused', async () => {
  await answerPermission(true);
  await page.locator('#services .add').click();
  const panel = page.getByRole('dialog', { name: 'Add a service' });
  await panel.getByRole('textbox').fill('not a site');
  await panel.getByRole('button', { name: 'Add' }).click();
  assert.equal((await page.locator('#status').textContent()).trim(), 'That isn’t a site address.');
  await panel.getByRole('textbox').fill(`localhost:${new URL(ext.url('localhost', 'x')).port}/delta`);
  await panel.getByRole('button', { name: 'Add' }).click();
  await settle();
  const added = (await stored()).at(-1);
  assert.equal(added.name, 'localhost');
  assert.equal(added.url, `https://localhost:${new URL(ext.url('localhost', 'x')).port}/delta`);
  assert.deepEqual(await page.evaluate(() => window.permissionAsks), [], 'nothing was asked');
});

test('chromebook-editing#AC-8: Allow pausing asks Chrome again for the site', async () => {
  await answerPermission(true);
  await tile('Gamma').click();
  await menu().getByRole('menuitem', { name: 'Allow pausing' }).click();
  await settle();
  assert.deepEqual(await page.evaluate(() => window.permissionAsks), [{ origins: ['http://127.0.0.1/*'] }]);
});

test('chromebook-editing#AC-1: Done ends editing; Esc does too', async () => {
  await page.getByRole('button', { name: 'Done' }).click();
  assert.equal(await page.getByRole('button', { name: 'Edit services' }).count(), 1);
  assert.equal(await page.locator('#services .add').count(), 0);
  await page.getByRole('button', { name: 'Edit services' }).click();
  await page.keyboard.press('Escape');
  assert.equal(await page.getByRole('button', { name: 'Edit services' }).count(), 1);
});

test('chromebook-editing#AC-9: the changes are there after the launcher restarts', async () => {
  const expected = (await stored()).filter(s => !s.hidden).map(s => s.name);
  await page.reload();
  await page.waitForFunction(() => document.body.dataset.ready === 'true');
  const shown = await page.locator('#services .tile').evaluateAll(els => els.map(e => e.querySelector('.name').textContent.trim()));
  assert.deepEqual(shown, expected);
});
