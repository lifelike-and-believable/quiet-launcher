import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createController } from '../../chromebook/lib/controller.js';
import { fakeBrowser } from './fake-browser.js';

const services = [
  { id: 'prime', name: 'Prime Video', url: 'https://www.primevideo.com/', site: 'primevideo.com', colour: '#1a73e8', hidden: false },
  { id: 'gem', name: 'CBC Gem', url: 'https://gem.cbc.ca/', site: 'gem.cbc.ca', colour: '#c8102e', hidden: false },
];
const setup = () => {
  const chrome = fakeBrowser({
    windows: [{ id: 1, type: 'normal', focused: true, tabs: [{ url: 'https://example.com/' }] }],
    services, granted: ['www.primevideo.com', 'gem.cbc.ca'],
  });
  return { chrome, c: createController(chrome) };
};
const RESULTS = q => `https://www.justwatch.com/ca/search?q=${encodeURIComponent(q)}`;
const windowsShowing = (chrome, url) => chrome.state.windows.filter(w => w.tabs.some(t => t.url === url));

test('chromebook-search#AC-1: a search opens JustWatch Canada for exactly the text, full screen, focused', async () => {
  const { chrome, c } = setup();
  const id = await c.search('Carrie & the 1976 film');
  const [w] = windowsShowing(chrome, RESULTS('Carrie & the 1976 film'));
  assert.ok(w, 'a window with the results');
  assert.equal(w.id, id);
  assert.equal(w.type, 'popup');
  assert.equal(w.state, 'fullscreen');
  assert.equal(w.focused, true);
});

test('chromebook-search#AC-1: nothing typed, or only spaces, does nothing', async () => {
  const { chrome, c } = setup();
  await c.search('');
  await c.search('   ');
  assert.equal(chrome.calls.filter(([k]) => k === 'create').length, 0);
});

test('chromebook-search#AC-2: searching again reuses the search window while it shows JustWatch', async () => {
  const { chrome, c } = setup();
  const first = await c.search('Carrie');
  chrome.state.windows.find(w => w.id === first).focused = false;
  const second = await c.search('Severance');
  assert.equal(second, first);
  assert.equal(chrome.calls.filter(([k]) => k === 'create').length, 1);
  const tab = chrome.state.windows.find(w => w.id === first).tabs[0];
  assert.ok(chrome.calls.some(([k, id, o]) => k === 'tabs.update' && id === tab.id && o.url === RESULTS('Severance')));
  assert.equal(chrome.state.windows.find(w => w.id === first).focused, true);
});

test('chromebook-search#AC-4: the search window going to a service becomes that service\'s window', async () => {
  const { chrome, c } = setup();
  const win = await c.search('Carrie');
  const tab = chrome.state.windows.find(w => w.id === win).tabs[0];
  tab.url = 'https://www.primevideo.com/detail/carrie';
  await c.onTabUpdated(tab.id, { url: tab.url }, { ...tab, windowId: win });
  assert.equal(await c.openService('prime'), win, 'choosing the service brings that window back');
  assert.equal(chrome.calls.filter(([k, o]) => k === 'create' && o.url === services[0].url).length, 0);
});

test('chromebook-search#AC-4: a site under the service\'s counts; another site does not', async () => {
  const { chrome, c } = setup();
  const win = await c.search('Schitt');
  const tab = chrome.state.windows.find(w => w.id === win).tabs[0];
  tab.url = 'https://www.netflix.com/title/1';
  await c.onTabUpdated(tab.id, { url: tab.url }, { ...tab, windowId: win });
  const created = chrome.calls.filter(([k]) => k === 'create').length;
  await c.openService('prime');
  assert.equal(chrome.calls.filter(([k]) => k === 'create').length, created + 1, 'Prime Video opened its own window');
  tab.url = 'https://gem.cbc.ca/schitts-creek';
  await c.onTabUpdated(tab.id, { url: tab.url }, { ...tab, windowId: win });
  assert.equal(await c.openService('gem'), win);
});

test('chromebook-search#AC-4: a window the service already had stays open but is no longer its own', async () => {
  const { chrome, c } = setup();
  const old = await c.openService('prime');
  const win = await c.search('Carrie');
  const tab = chrome.state.windows.find(w => w.id === win).tabs[0];
  tab.url = 'https://www.primevideo.com/detail/carrie';
  await c.onTabUpdated(tab.id, { url: tab.url }, { ...tab, windowId: win });
  assert.equal(await c.openService('prime'), win);
  assert.ok(chrome.state.windows.some(w => w.id === old), 'the old window is still open');
});

test('chromebook-search#AC-4: a service window reached from search is paused by go-home', async () => {
  const { chrome, c } = setup();
  const win = await c.search('Carrie');
  const tab = chrome.state.windows.find(w => w.id === win).tabs[0];
  tab.url = 'https://www.primevideo.com/detail/carrie';
  await c.onTabUpdated(tab.id, { url: tab.url }, { ...tab, windowId: win });
  chrome.state.playing.add(tab.id);
  await c.goHome();
  assert.equal(chrome.state.playing.has(tab.id), false);
});
