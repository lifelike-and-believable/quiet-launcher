import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadWithServices } from './services-harness.js';

let ext;
before(async () => { ext = await loadWithServices(); await ext.seed(); });
after(async () => { await ext?.close(); });

test('chromebook-back-to-browsing#AC-1: Back to browsing leaves only the browsing windows', async () => {
  const browsing = (await ext.windows()).filter(w => w.type === 'normal').map(w => w.id);
  await ext.call('openService', 'alpha');
  await ext.call('openService', 'beta');
  const page = await ext.launcher();
  // Beta and the launcher: opening Beta closed Alpha (chromebook-one-service#AC-1).
  assert.ok((await ext.windows()).length >= browsing.length + 2);
  await page.getByRole('button', { name: 'Back to browsing' }).click();
  let left;
  for (let i = 0; i < 30; i++) {
    left = await ext.windows();
    if (left.length === browsing.length) break;
    await new Promise(r => setTimeout(r, 100));
  }
  assert.deepEqual(left.map(w => w.id).sort(), [...browsing].sort());
});
