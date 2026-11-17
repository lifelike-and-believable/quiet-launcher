// Loads the extension in ../../chromebook into Playwright's Chromium (headless, with extensions)
// and gives the tests its service worker and its pages.
import { chromium } from 'playwright';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

export const EXTENSION = fileURLToPath(new URL('../../chromebook', import.meta.url));

export async function loadExtension() {
  const profile = mkdtempSync(join(tmpdir(), 'quiet-launcher-'));
  const context = await chromium.launchPersistentContext(profile, {
    channel: 'chromium',
    headless: !process.env.DISPLAY,
    args: [`--disable-extensions-except=${EXTENSION}`, `--load-extension=${EXTENSION}`],
  });
  let [worker] = context.serviceWorkers();
  if (!worker) worker = await context.waitForEvent('serviceworker');
  const id = new URL(worker.url()).host;
  return {
    context,
    worker,
    launcherUrl: `chrome-extension://${id}/launcher.html`,
    async close() { await context.close(); rmSync(profile, { recursive: true, force: true }); },
    // Every window, with its tabs' addresses: what the user would see in Chrome.
    windows: () => worker.evaluate(async () => (await chrome.windows.getAll({ populate: true }))
      .map(w => ({ id: w.id, type: w.type, state: w.state, urls: w.tabs.map(t => t.url || t.pendingUrl) }))),
    goHome: () => worker.evaluate(() => globalThis.quietLauncher.goHome()),
  };
}

export async function openLauncher(ext) {
  await ext.goHome();
  const page = ext.context.pages().find(p => p.url() === ext.launcherUrl)
    || await ext.context.waitForEvent('page', p => p.url() === ext.launcherUrl);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForFunction(() => document.body.dataset.ready === 'true');
  return page;
}
