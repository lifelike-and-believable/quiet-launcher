// docs/specs/chromebook-one-service.md: only one service (or JustWatch's results) is open at a time.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createController } from '../../chromebook/lib/controller.js';
import { fakeBrowser } from './fake-browser.js';

const services = [
  { id: 'prime', name: 'Prime Video', url: 'https://www.primevideo.com/', site: 'primevideo.com', colour: '#1a73e8', hidden: false },
  { id: 'mubi', name: 'MUBI', url: 'https://mubi.com/', site: 'mubi.com', colour: '#222', hidden: false },
  { id: 'gem', name: 'CBC Gem', url: 'https://gem.cbc.ca/', site: 'gem.cbc.ca', colour: '#c8102e', hidden: false },
];
// The in-memory chrome API, with windows.remove that fires onRemoved as Chrome does.
const setup = () => {
  const chrome = fakeBrowser({
    windows: [{ id: 1, type: 'normal', state: 'maximized', focused: true, tabs: [{ url: 'https://example.com/' }] }],
    services, granted: ['www.primevideo.com', 'mubi.com', 'gem.cbc.ca'],
  });
  const c = createController(chrome);
  chrome.windows.remove = async id => {
    chrome.calls.push(['remove', id]);
    const at = chrome.state.windows.findIndex(w => w.id === id);
    if (at < 0) throw new Error(`No window with id: ${id}.`);
    chrome.state.windows.splice(at, 1);
    await c.onWindowRemoved(id);
  };
  return { chrome, c };
};
const isOpen = (chrome, id) => chrome.state.windows.some(w => w.id === id);
const openServices = async chrome => (await chrome.storage.session.get('serviceWindows')).serviceWindows || {};
const follow = async (chrome, c, win, url) => {
  const tab = chrome.state.windows.find(w => w.id === win).tabs[0];
  tab.url = url;
  await c.onTabUpdated(tab.id, { url }, { ...tab, windowId: win });
};

test('chromebook-one-service#AC-1: choosing a different service closes the one that was open', async () => {
  const { chrome, c } = setup();
  const prime = await c.openService('prime');
  await c.goHome();
  const mubi = await c.openService('mubi');
  assert.ok(isOpen(chrome, mubi), 'MUBI is open');
  assert.equal(isOpen(chrome, prime), false, "Prime Video's window closed");
  assert.deepEqual(await openServices(chrome), { mubi });
});

test('chromebook-one-service#AC-1: the new service opens before the old one closes', async () => {
  const { chrome, c } = setup();
  const prime = await c.openService('prime');
  await c.openService('mubi');
  const created = chrome.calls.findIndex(([k, o]) => k === 'create' && o.url === services[1].url);
  const removed = chrome.calls.findIndex(([k, id]) => k === 'remove' && id === prime);
  assert.ok(created >= 0 && removed > created, 'created, then removed');
});

test('chromebook-one-service#AC-2: choosing the open service brings it back and closes nothing', async () => {
  const { chrome, c } = setup();
  const prime = await c.openService('prime');
  await c.goHome();
  chrome.calls.length = 0;
  assert.equal(await c.openService('prime'), prime);
  assert.equal(chrome.calls.filter(([k]) => k === 'remove' || k === 'create' || k === 'tabs.reload').length, 0);
});

test('chromebook-one-service#AC-3: searching closes the service that was open', async () => {
  const { chrome, c } = setup();
  const prime = await c.openService('prime');
  await c.goHome();
  const results = await c.search('Carrie');
  assert.ok(isOpen(chrome, results));
  assert.equal(isOpen(chrome, prime), false);
  assert.deepEqual(await openServices(chrome), {});
});

test('chromebook-one-service#AC-3: searching again, reusing the results window, keeps it open', async () => {
  const { chrome, c } = setup();
  const first = await c.search('Carrie');
  assert.equal(await c.search('Schitt'), first);
  assert.ok(isOpen(chrome, first));
});

test('chromebook-one-service#AC-4: choosing a service closes the results', async () => {
  const { chrome, c } = setup();
  const results = await c.search('Carrie');
  await c.goHome();
  const gem = await c.openService('gem');
  assert.ok(isOpen(chrome, gem));
  assert.equal(isOpen(chrome, results), false);
  assert.equal((await chrome.storage.session.get('searchWindow')).searchWindow ?? null, null);
});

test('chromebook-one-service#AC-5: the results becoming a service close any other service window', async () => {
  const { chrome, c } = setup();
  const results = await c.search('Carrie');
  // A service opened some other way while the results were open (go-last cannot; this is a guard).
  const mubi = await chrome.windows.create({ url: services[1].url, type: 'popup' });
  await chrome.storage.session.set({ serviceWindows: { mubi: mubi.id } });
  await follow(chrome, c, results, 'https://www.primevideo.com/detail/carrie');
  assert.equal(isOpen(chrome, mubi.id), false);
  assert.ok(isOpen(chrome, results));
  assert.deepEqual(await openServices(chrome), { prime: results });
});

test('chromebook-one-service#AC-6: go-last goes to the one open service, or the launcher if none', async () => {
  const { chrome, c } = setup();
  await c.openService('prime');
  const mubi = await c.openService('mubi');
  await c.goHome();
  assert.equal(await c.goLast(), mubi);
  await c.search('Carrie');
  const launcher = await c.goLast();
  assert.ok(chrome.state.windows.find(w => w.id === launcher).tabs[0].url.endsWith('launcher.html'));
});
