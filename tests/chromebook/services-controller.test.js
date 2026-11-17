import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createController } from '../../chromebook/lib/controller.js';
import { fakeBrowser } from './fake-browser.js';

const LAUNCHER = 'chrome-extension://quiet/launcher.html';
const services = [
  { id: 'prime', name: 'Prime Video', url: 'https://www.primevideo.com/', site: 'primevideo.com', colour: '#1a73e8', hidden: false },
  { id: 'mubi', name: 'MUBI', url: 'https://mubi.com/', site: 'mubi.com', colour: '#222', hidden: false },
];
const setup = (opts = {}) => {
  const chrome = fakeBrowser({
    windows: [{ id: 1, type: 'normal', state: 'maximized', focused: true, tabs: [{ url: 'https://example.com/' }] }],
    services, granted: ['www.primevideo.com', 'mubi.com'], ...opts,
  });
  return { chrome, c: createController(chrome) };
};
const windowOf = (chrome, url) => chrome.state.windows.filter(w => w.tabs.some(t => t.url === url));
const launcherWindow = chrome => windowOf(chrome, LAUNCHER)[0];
const tabOf = (chrome, url) => windowOf(chrome, url)[0].tabs[0];

test('chromebook-services#AC-2: a service that is not open opens in a new full-screen popup window, focused', async () => {
  const { chrome, c } = setup();
  const id = await c.openService('prime');
  const [w] = windowOf(chrome, 'https://www.primevideo.com/');
  assert.ok(w, 'a window for the service');
  assert.equal(w.id, id);
  assert.equal(w.type, 'popup');
  assert.equal(w.state, 'fullscreen');
  assert.equal(w.focused, true);
});

test('chromebook-services#AC-3: a service that is open is brought forward, full screen, not opened again or reloaded', async () => {
  const { chrome, c } = setup();
  const first = await c.openService('prime');
  await c.goHome();
  chrome.state.windows.find(w => w.id === first).state = 'normal'; // it left full screen meanwhile
  const again = await c.openService('prime');
  assert.equal(again, first);
  assert.equal(windowOf(chrome, 'https://www.primevideo.com/').length, 1);
  const w = chrome.state.windows.find(x => x.id === first);
  assert.equal(w.focused, true);
  assert.equal(w.state, 'fullscreen');
  assert.equal(chrome.calls.filter(([k]) => k === 'tabs.update' || k === 'tabs.reload').length, 0);
});

test('chromebook-services#AC-5: go-home from a service pauses its video, in every frame, before the launcher comes forward', async () => {
  const { chrome, c } = setup();
  await c.openService('prime');
  const tab = tabOf(chrome, 'https://www.primevideo.com/');
  chrome.state.playing.add(tab.id);
  await c.goHome();
  const pause = chrome.calls.findIndex(([k, target, func]) => k === 'executeScript' && target.tabId === tab.id && typeof func === 'function');
  assert.ok(pause >= 0, 'the service was paused');
  assert.equal(chrome.calls[pause][1].allFrames, true);
  const focusLauncher = chrome.calls.findIndex(([k, id, o]) => k === 'update' && id === launcherWindow(chrome)?.id && o.focused);
  const created = chrome.calls.findIndex(([k, o]) => k === 'create' && o.url === LAUNCHER);
  assert.ok(pause < Math.max(focusLauncher, created), 'paused before the launcher came forward');
  assert.equal(chrome.state.playing.has(tab.id), false);
});

test('chromebook-services#AC-5: go-home from an ordinary browsing window pauses nothing', async () => {
  const { chrome, c } = setup();
  await c.goHome();
  assert.equal(chrome.calls.filter(([k]) => k === 'executeScript').length, 0);
});

test('chromebook-services#AC-6: the launcher gaining focus from a service window pauses that service', async () => {
  const { chrome, c } = setup();
  await c.goHome();
  const launcher = launcherWindow(chrome).id;
  const prime = await c.openService('prime');
  await c.onFocus(prime);
  chrome.calls.length = 0;
  await c.onFocus(launcher);
  const tab = tabOf(chrome, 'https://www.primevideo.com/');
  assert.equal(chrome.calls.filter(([k, t]) => k === 'executeScript' && t.tabId === tab.id).length, 1);
});

test('chromebook-services#AC-6: the launcher gaining focus from a browsing window pauses nothing', async () => {
  const { chrome, c } = setup();
  await c.goHome();
  const launcher = launcherWindow(chrome).id;
  await c.onFocus(1);
  chrome.calls.length = 0;
  await c.onFocus(launcher);
  assert.equal(chrome.calls.filter(([k]) => k === 'executeScript').length, 0);
});

test('chromebook-services#AC-7: go-last brings the last service used forward, full screen', async () => {
  const { chrome, c } = setup();
  await c.openService('mubi');
  const prime = await c.openService('prime');
  await c.goHome();
  chrome.state.windows.find(w => w.id === prime).state = 'normal';
  assert.equal(await c.goLast(), prime);
  const w = chrome.state.windows.find(x => x.id === prime);
  assert.equal(w.focused, true);
  assert.equal(w.state, 'fullscreen');
});

test('chromebook-services#AC-7: go-last with no service used brings the launcher forward', async () => {
  const { chrome, c } = setup();
  const id = await c.goLast();
  assert.equal(id, launcherWindow(chrome).id);
  assert.equal(launcherWindow(chrome).focused, true);
});

test('chromebook-services#AC-3: a service whose window was closed opens again in a new window', async () => {
  const { chrome, c } = setup();
  const first = await c.openService('prime');
  chrome.state.windows = chrome.state.windows.filter(w => w.id !== first);
  await c.onWindowRemoved(first);
  const second = await c.openService('prime');
  assert.notEqual(second, first);
  assert.equal(windowOf(chrome, 'https://www.primevideo.com/').length, 1);
});

test('chromebook-services#AC-8: without site permission, the launcher still comes back and nothing fails', async () => {
  const { chrome, c } = setup({ granted: [] });
  await c.openService('prime');
  await assert.doesNotReject(c.goHome());
  assert.equal(launcherWindow(chrome).focused, true);
});
