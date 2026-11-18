import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createController } from '../../chromebook/lib/controller.js';
import { fakeBrowser } from './fake-browser.js';

const LAUNCHER = 'chrome-extension://quiet/launcher.html';
const services = [
  { id: 'prime', name: 'Prime Video', url: 'https://www.primevideo.com/', site: 'primevideo.com', colour: '#1a73e8', hidden: false },
  { id: 'mubi', name: 'MUBI', url: 'https://mubi.com/', site: 'mubi.com', colour: '#222', hidden: false },
];
// The in-memory chrome API, with windows.remove (closing a window that is gone fails, as in Chrome).
const setup = () => {
  const chrome = fakeBrowser({
    windows: [{ id: 1, type: 'normal', state: 'maximized', focused: true, tabs: [{ url: 'https://example.com/' }] },
      { id: 2, type: 'normal', tabs: [{ url: 'https://news.example/' }] }],
    services, granted: ['www.primevideo.com', 'mubi.com'],
  });
  chrome.windows.remove = async id => {
    chrome.calls.push(['remove', id]);
    const at = chrome.state.windows.findIndex(w => w.id === id);
    if (at < 0) throw new Error(`No window with id: ${id}.`);
    chrome.state.windows.splice(at, 1);
  };
  return { chrome, c: createController(chrome) };
};
const ids = chrome => chrome.state.windows.map(w => w.id).sort();

test('chromebook-back-to-browsing#AC-1: it brings the browsing window forward and closes the launcher, services, and search', async () => {
  const { chrome, c } = setup();
  await c.noteFocus(1);
  await c.goHome();
  await c.openService('prime');
  await c.openService('mubi');
  await c.search('Carrie');
  // Two browsing windows, the launcher, and the results: opening MUBI closed Prime Video, and the
  // search closed MUBI (chromebook-one-service#AC-1, AC-3).
  assert.equal(chrome.state.windows.length, 4);
  chrome.calls.length = 0;
  await c.backToBrowsing();
  assert.deepEqual(ids(chrome), [1, 2], 'only the browsing windows are left');
  assert.equal(chrome.state.windows.find(w => w.id === 1).focused, true);
  assert.equal(chrome.state.windows.find(w => w.id === 1).state, 'maximized', 'left as it was');
  const focusAt = chrome.calls.findIndex(([k, id, o]) => k === 'update' && id === 1 && o?.focused);
  const firstRemove = chrome.calls.findIndex(([k]) => k === 'remove');
  assert.ok(focusAt >= 0 && focusAt < firstRemove, 'the browsing window comes forward first');
});

test('chromebook-back-to-browsing#AC-2: afterwards services open afresh and go-last brings up the launcher', async () => {
  const { chrome, c } = setup();
  await c.noteFocus(1);
  const first = await c.openService('prime');
  await c.backToBrowsing();
  const again = await c.openService('prime');
  assert.notEqual(again, first);
  await c.backToBrowsing();
  const last = await c.goLast();
  assert.ok(chrome.state.windows.find(w => w.id === last).tabs.some(t => t.url === LAUNCHER), 'go-last brought up the launcher');
});

test('chromebook-back-to-browsing#AC-3: a window already closed by hand does not stop the others closing', async () => {
  const { chrome, c } = setup();
  await c.noteFocus(1);
  await c.goHome();
  const prime = await c.openService('prime');
  await c.openService('mubi');
  chrome.state.windows = chrome.state.windows.filter(w => w.id !== prime); // closed without our hearing
  await c.backToBrowsing();
  assert.deepEqual(ids(chrome), [1, 2]);
});
