import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadExtension, openLauncher } from './extension-harness.js';

let ext;
before(async () => { ext = await loadExtension(); });
after(async () => { await ext?.close(); });

const launcherWindows = windows => windows.filter(w => w.urls.some(u => u === ext.launcherUrl));

test('chromebook-launcher#AC-2, AC-4: go-home opens one full-screen popup launcher and touches no other window', async () => {
  const before = await ext.windows();
  await ext.goHome();
  const after = await ext.windows();
  const launchers = launcherWindows(after);
  assert.equal(launchers.length, 1);
  assert.equal(launchers[0].type, 'popup');
  assert.equal(launchers[0].state, 'fullscreen');
  for (const w of before) {
    const now = after.find(x => x.id === w.id);
    assert.deepEqual(now.urls, w.urls, 'no tab opened in another window');
    assert.equal(now.state, w.state, 'another window kept its state');
  }
});

test('chromebook-launcher#AC-3: go-home again brings the same launcher forward, never a second', async () => {
  const first = launcherWindows(await ext.windows())[0];
  await ext.goHome();
  await ext.goHome();
  const launchers = launcherWindows(await ext.windows());
  assert.equal(launchers.length, 1);
  assert.equal(launchers[0].id, first.id);
});

test('chromebook-launcher#AC-5: the top row, an empty status line, and the no-services message with Edit services selected', async () => {
  const page = await openLauncher(ext);
  assert.equal(await page.getByRole('searchbox', { name: 'Search for a show' }).count(), 1);
  assert.equal(await page.getByRole('button', { name: 'Edit services' }).count(), 1);
  assert.equal(await page.getByRole('button', { name: 'Back to browsing' }).count(), 1);
  const clock = page.locator('#clock');
  assert.match(await clock.textContent(), /^\d{1,2}:\d{2}$/);
  assert.equal(await clock.evaluate(el => el.tabIndex), -1, 'the clock cannot be selected');
  assert.equal((await page.locator('#status').textContent()).trim(), '');
  assert.match(await page.locator('#services').textContent(), /No services yet\.\s*Choose Edit services, then Add a service\./);
  assert.equal(await page.evaluate(() => document.activeElement?.textContent.trim()), 'Edit services');
});

test('chromebook-launcher#AC-6: the hint line shows the shortcuts Chrome gave the commands', async () => {
  const page = await openLauncher(ext);
  const shortcuts = await ext.worker.evaluate(async () =>
    Object.fromEntries((await chrome.commands.getAll()).map(c => [c.name, c.shortcut])));
  const expected = [shortcuts['go-home'] ? `${shortcuts['go-home']} brings you back here` : 'No shortcut back here yet',
    ...(shortcuts['go-last'] ? [`${shortcuts['go-last']} goes back to the last service`] : [])].join(' · ');
  assert.equal((await page.locator('#hint').textContent()).trim(), expected);
});

test('chromebook-launcher#AC-8: Left and Right move along the top row without wrapping; Tab follows reading order', async () => {
  const page = await openLauncher(ext);
  const selected = () => page.evaluate(() => document.activeElement?.getAttribute('aria-label') || document.activeElement?.textContent.trim());
  await page.getByRole('button', { name: 'Edit services' }).focus();
  await page.keyboard.press('ArrowLeft');
  assert.equal(await selected(), 'Search for a show');
  await page.keyboard.press('ArrowLeft');
  assert.equal(await selected(), 'Search for a show', 'no wrapping at the left end');
  await page.keyboard.press('ArrowRight');
  assert.equal(await selected(), 'Edit services');
  await page.keyboard.press('ArrowRight');
  assert.equal(await selected(), 'Back to browsing');
  await page.keyboard.press('ArrowRight');
  assert.equal(await selected(), 'Back to browsing', 'no wrapping, and the clock is not selectable');
  await page.getByRole('searchbox', { name: 'Search for a show' }).focus();
  await page.keyboard.press('Tab');
  assert.equal(await selected(), 'Edit services');
  await page.keyboard.press('Tab');
  assert.equal(await selected(), 'Back to browsing');
  await page.keyboard.press('Shift+Tab');
  assert.equal(await selected(), 'Edit services');
});

test('chromebook-launcher#AC-8: the selected control has the selection outline', async () => {
  const page = await openLauncher(ext);
  await page.getByRole('button', { name: 'Back to browsing' }).focus();
  await page.keyboard.press('ArrowLeft'); // keyboard focus, so :focus-visible applies
  const outline = await page.evaluate(() => {
    const s = getComputedStyle(document.activeElement);
    return { style: s.outlineStyle, colour: s.outlineColor, width: parseFloat(s.outlineWidth) };
  });
  assert.equal(outline.style, 'solid');
  assert.equal(outline.colour, 'rgb(255, 255, 255)');
  assert.ok(outline.width >= 4);
});

test('chromebook-launcher#AC-7, AC-8: Enter on Back to browsing asks the service worker to go back to browsing', async () => {
  await ext.worker.evaluate(() => {
    globalThis.backCalls = 0;
    globalThis.quietLauncher.backToBrowsing = async () => { globalThis.backCalls++; };
  });
  const page = await openLauncher(ext);
  await page.getByRole('button', { name: 'Back to browsing' }).focus();
  await page.keyboard.press('Enter');
  await page.waitForTimeout(300);
  assert.equal(await ext.worker.evaluate(() => globalThis.backCalls), 1);
});
