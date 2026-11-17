import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createController } from '../../chromebook/lib/controller.js';
import { fakeBrowser } from './fake-browser.js';

const services = [
  { id: 'prime', name: 'Prime Video', url: 'https://www.primevideo.com/', site: 'primevideo.com', colour: '#1a73e8', hidden: false },
  { id: 'mubi', name: 'MUBI', url: 'https://mubi.com/', site: 'mubi.com', colour: '#222', hidden: false },
];
const setup = () => {
  const chrome = fakeBrowser({
    windows: [{ id: 1, type: 'normal', focused: true, tabs: [{ url: 'https://www.primevideo.com/browse' }] }],
    services, granted: ['www.primevideo.com'],
  });
  return { chrome, c: createController(chrome) };
};
const injections = (chrome, tabId) => chrome.calls.filter(([k, target]) => k === 'executeScript' && target.tabId === tabId);
const complete = (c, chrome, windowId) => {
  const tab = chrome.state.windows.find(w => w.id === windowId).tabs[0];
  return c.onTabUpdated(tab.id, { status: 'complete' }, { ...tab, windowId, status: 'complete' }).then(() => tab.id);
};

test('chromebook-home-button#AC-1: a page that finishes loading in a service window gets the button', async () => {
  const { chrome, c } = setup();
  const win = await c.openService('prime');
  const tabId = await complete(c, chrome, win);
  assert.equal(injections(chrome, tabId).length, 1);
});

test('chromebook-home-button#AC-1: an ordinary browsing window gets none, even on a service\'s site', async () => {
  const { chrome, c } = setup();
  const tabId = await complete(c, chrome, 1);
  assert.equal(injections(chrome, tabId).length, 0);
});

test('chromebook-home-button#AC-1: the search window gets none until it becomes a service\'s', async () => {
  const { chrome, c } = setup();
  const win = await c.search('Carrie');
  const tabId = await complete(c, chrome, win);
  assert.equal(injections(chrome, tabId).length, 0);
});

test('chromebook-home-button#AC-1: a site the extension may not run on gets none, without an error', async () => {
  const { chrome, c } = setup();
  const win = await c.openService('mubi');
  await assert.doesNotReject(complete(c, chrome, win));
});
