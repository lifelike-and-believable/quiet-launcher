// Spike: keeps one launcher, opens services, and logs what the shortcuts do to the window's state,
// so the launcher page can show the results. Two modes:
// - tabs: every service is a tab in the launcher's window.
// - windows: every service gets its own full-screen window with no tab strip, so going back and
//   forth switches windows rather than tabs.
importScripts('services.js');

const LAUNCHER = chrome.runtime.getURL('launcher.html');

async function log(text) {
  const { entries = [] } = await chrome.storage.local.get('entries');
  entries.push(`${new Date().toLocaleTimeString()}  ${text}`);
  await chrome.storage.local.set({ entries: entries.slice(-200) });
}

async function mode() {
  return (await chrome.storage.local.get({ mode: 'tabs' })).mode;
}

function serviceOf(url) {
  try {
    const host = new URL(url).hostname;
    return SERVICES.find(s => host === s.site || host.endsWith('.' + s.site));
  } catch { return undefined; }
}

function isLauncher(tab) {
  const url = tab.pendingUrl || tab.url || '';
  return url.startsWith(LAUNCHER) || url === 'chrome://newtab/';
}

async function windowState(windowId) {
  try { return (await chrome.windows.get(windowId)).state; } catch { return 'gone'; }
}

const wait = ms => new Promise(r => setTimeout(r, ms));

// If the window left full screen, say so, put it back, and report whether that worked.
async function report(windowId, why, before, waitMs) {
  await wait(waitMs);
  const after = await windowState(windowId);
  const m = await mode();
  if (before === 'fullscreen' && after !== 'fullscreen') {
    await chrome.windows.update(windowId, { state: 'fullscreen' });
    await wait(500);
    const restored = await windowState(windowId);
    return log(`[${m}] ${why}: window ${before} -> ${after}  << LEFT FULL SCREEN; put back: ${restored}`);
  }
  await log(`[${m}] ${why}: window ${before} -> ${after}`);
}

// The tabs to look in: the current window's in tabs mode, every window's in windows mode.
async function candidateTabs() {
  if ((await mode()) === 'windows') return chrome.tabs.query({});
  return chrome.tabs.query({ windowId: (await chrome.windows.getLastFocused()).id });
}

// Bring a tab forward: its window first (windows mode), then the tab itself.
async function switchTo(tab, why) {
  const before = await windowState(tab.windowId);
  await chrome.windows.update(tab.windowId, { focused: true });
  if (!tab.active) await chrome.tabs.update(tab.id, { active: true });
  await report(tab.windowId, why, before, 500);
}

// Open a page: a new tab in tabs mode, a new full-screen window with no tab strip in windows mode.
async function openPage(url, why) {
  if ((await mode()) === 'windows') {
    const win = await chrome.windows.create({ url, type: 'popup', state: 'fullscreen', focused: true });
    await report(win.id, `${why} in its own window`, 'fullscreen', 1500);
    return win.tabs[0];
  }
  const windowId = (await chrome.windows.getLastFocused()).id;
  const before = await windowState(windowId);
  const tab = await chrome.tabs.create({ windowId, url });
  await report(windowId, `${why} in a new tab`, before, 1500);
  return tab;
}

async function goHome(source) {
  const focused = await chrome.windows.getLastFocused({ populate: true });
  const active = focused.tabs.find(t => t.active);
  if (active && !isLauncher(active)) {
    const service = serviceOf(active.url || '');
    await chrome.storage.local.set({ lastTab: active.id });
    await log(`${source} received while on ${service ? service.name : (active.url || 'an unknown page')}`);
  } else {
    await log(`${source} received while on the launcher`);
  }
  const launcher = (await candidateTabs()).find(isLauncher);
  if (launcher) return switchTo(launcher, 'Back to the launcher');
  await openPage(LAUNCHER, 'No launcher open, opened one');
}

async function goLast(source) {
  const { lastTab } = await chrome.storage.local.get('lastTab');
  await log(`${source} received`);
  try {
    const tab = await chrome.tabs.get(lastTab);
    await switchTo(tab, `Back to ${serviceOf(tab.url || '')?.name || 'the last tab'}`);
  } catch {
    await log('No last service to go back to');
  }
}

async function openService(name) {
  const service = SERVICES.find(s => s.name === name);
  const open = (await candidateTabs()).find(t => serviceOf(t.url || t.pendingUrl || '') === service);
  if (open) {
    await chrome.storage.local.set({ lastTab: open.id });
    return switchTo(open, `${name} was already open, switched to it`);
  }
  const tab = await openPage(service.url, `Opened ${name}`);
  await chrome.storage.local.set({ lastTab: tab.id });
}

const ENGINES = {
  'google': ['Google', t => 'https://www.google.com/search?q=' + encodeURIComponent(t)],
  'google-watch': ['Google, "where to watch"', t => 'https://www.google.com/search?q=' + encodeURIComponent(t + ' where to watch')],
  'justwatch': ['JustWatch', t => 'https://www.justwatch.com/ca/search?q=' + encodeURIComponent(t)],
};

async function search(text, engine = 'google') {
  const [name, url] = ENGINES[engine] || ENGINES.google;
  await openPage(url(text), `Searched ${name} for "${text}"`);
}

chrome.commands.onCommand.addListener(command => {
  if (command === 'go-home') goHome('Shortcut: back to the launcher');
  if (command === 'go-last') goLast('Shortcut: back to the last service');
});

chrome.action.onClicked.addListener(() => goHome('Toolbar button'));

chrome.runtime.onMessage.addListener(message => {
  if (message.type === 'open') openService(message.name);
  if (message.type === 'search') search(message.text, message.engine);
  if (message.type === 'log') log(message.text);
});

chrome.runtime.onInstalled.addListener(async () => {
  const commands = await chrome.commands.getAll();
  for (const c of commands) {
    if (c.name.startsWith('_')) continue;
    await log(`Shortcut "${c.description}": ${c.shortcut || 'NOT SET (taken by something else; set one at chrome://extensions/shortcuts)'}`);
  }
});
