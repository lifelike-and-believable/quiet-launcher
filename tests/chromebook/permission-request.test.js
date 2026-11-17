import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadExtension, openLauncher } from './extension-harness.js';

// The real chrome.permissions.request, not a stand-in: Chrome refuses a pattern its manifest does
// not cover at once, with an error, and shows its prompt for one it does. The prompt cannot be
// answered here, so "the prompt is showing" is what passing looks like.
let ext;
let page;
before(async () => { ext = await loadExtension(); page = await openLauncher(ext); });
after(async () => { await ext?.close(); });

const ask = (site, url) => page.evaluate(async ([s, u]) => {
  const { permissionPattern } = await import('./lib/editing.js');
  try {
    const p = chrome.permissions.request({ origins: [permissionPattern(s, u)] });
    return await Promise.race([p.then(v => `answered ${v}`), new Promise(r => setTimeout(() => r('prompt'), 1500))]);
  } catch (e) {
    return `refused: ${e.message}`;
  }
}, [site, url]);

test('chromebook-editing#AC-11: Chrome accepts the permission asked for a service in the list', async () => {
  assert.match(await ask('primevideo.com', 'https://www.primevideo.com/'), /^(prompt|answered true)$/);
});

test('chromebook-editing#AC-11: and for an http address, a bare host, and an IP address', async () => {
  assert.match(await ask('example.org', 'http://www.example.org/'), /^(prompt|answered true)$/);
  assert.match(await ask('localhost', 'https://localhost:8080/tv'), /^(prompt|answered true)$/);
  assert.match(await ask('127.0.0.1', 'http://127.0.0.1:8080/'), /^(prompt|answered true)$/);
});
