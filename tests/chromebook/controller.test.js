import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createController } from '../../chromebook/lib/controller.js';
import { fakeChrome } from './fake-chrome.js';

const LAUNCHER = 'chrome-extension://quiet/launcher.html';
const browsing = { id: 1, type: 'normal', state: 'maximized', tabs: [{ url: 'https://example.com/' }] };
const launchers = chrome => chrome.state.windows.filter(w => w.tabs.some(t => t.url === LAUNCHER));

test('chromebook-launcher#AC-2: with no launcher, go-home opens one full-screen popup window and focuses it', async () => {
  const chrome = fakeChrome({ windows: [{ ...browsing, focused: true }] });
  const id = await createController(chrome).goHome();
  const [launcher] = launchers(chrome);
  assert.ok(launcher, 'a launcher window was opened');
  assert.equal(launcher.id, id);
  assert.equal(launcher.type, 'popup');
  assert.equal(launcher.state, 'fullscreen');
  assert.equal(launcher.focused, true);
});

test('chromebook-launcher#AC-2: if Chrome does not create it full screen, go-home makes it full screen', async () => {
  const chrome = fakeChrome({ windows: [browsing], createdState: 'normal' });
  await createController(chrome).goHome();
  assert.equal(launchers(chrome)[0].state, 'fullscreen');
});

test('chromebook-launcher#AC-3: with a launcher open, go-home brings it forward and opens no second one', async () => {
  const chrome = fakeChrome({ windows: [{ ...browsing, focused: true },
    { id: 7, type: 'popup', state: 'fullscreen', tabs: [{ url: LAUNCHER }] }] });
  const id = await createController(chrome).goHome();
  assert.equal(id, 7);
  assert.equal(launchers(chrome).length, 1);
  assert.equal(launchers(chrome)[0].focused, true);
  assert.equal(chrome.calls.filter(([c]) => c === 'create').length, 0);
});

test('chromebook-launcher#AC-3: a launcher that left full screen is put back in full screen', async () => {
  const chrome = fakeChrome({ windows: [browsing, { id: 7, type: 'popup', state: 'normal', tabs: [{ url: LAUNCHER }] }] });
  await createController(chrome).goHome();
  assert.equal(launchers(chrome)[0].state, 'fullscreen');
});

test('chromebook-launcher#AC-4: go-home changes nothing about the browsing window', async () => {
  const chrome = fakeChrome({ windows: [{ ...browsing, focused: true }] });
  await createController(chrome).goHome();
  const w = chrome.state.windows.find(x => x.id === 1);
  assert.equal(w.state, 'maximized');
  assert.deepEqual(w.tabs, browsing.tabs);
  assert.equal(chrome.calls.filter(([c, id]) => c === 'update' && id === 1).length, 0);
});

test('chromebook-launcher#AC-7: Back to browsing brings forward the last normal window that had focus', async () => {
  const chrome = fakeChrome({ windows: [browsing, { id: 2, type: 'normal', tabs: [{ url: 'https://two.example/' }] }] });
  const c = createController(chrome);
  await c.noteFocus(1);
  await c.goHome();
  const launcherId = launchers(chrome)[0].id;
  await c.noteFocus(launcherId); // a popup: not a browsing window
  assert.equal(await c.backToBrowsing(), 1);
  assert.equal(chrome.state.windows.find(w => w.id === 1).focused, true);
  assert.ok(chrome.state.windows.some(w => w.id === launcherId), 'the launcher stays open');
});

test('chromebook-launcher#AC-7: if that window has closed, any normal window', async () => {
  const chrome = fakeChrome({ windows: [browsing] });
  const c = createController(chrome);
  await c.noteFocus(1);
  chrome.state.windows.splice(0, 1, { id: 3, type: 'normal', tabs: [{ url: 'https://three.example/' }] });
  assert.equal(await c.backToBrowsing(), 3);
});

test('chromebook-launcher#AC-7: with no normal window at all, a new one', async () => {
  const chrome = fakeChrome({ windows: [] });
  const c = createController(chrome);
  await c.goHome();
  const id = await c.backToBrowsing();
  const w = chrome.state.windows.find(x => x.id === id);
  assert.ok(w, 'a window was opened');
  assert.equal(w.type, 'normal');
  assert.equal(w.focused, true);
});
