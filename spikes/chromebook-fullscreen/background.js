// Spike: keeps one launcher tab per window, opens services in their own tabs, and logs what the
// shortcuts do to the window's state, so the launcher page can show the results.
importScripts('services.js');

const LAUNCHER = chrome.runtime.getURL('launcher.html');

async function log(text) {
  const { entries = [] } = await chrome.storage.local.get('entries');
  entries.push(`${new Date().toLocaleTimeString()}  ${text}`);
  await chrome.storage.local.set({ entries: entries.slice(-200) });
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

// If the window left full screen, say so, put it back, and report whether that worked.
async function report(windowId, why, before, waitMs) {
  await new Promise(r => setTimeout(r, waitMs));
  const after = await windowState(windowId);
  if (before === 'fullscreen' && after !== 'fullscreen') {
    await chrome.windows.update(windowId, { state: 'fullscreen' });
    await new Promise(r => setTimeout(r, 500));
    const restored = await windowState(windowId);
    return log(`${why}: window ${before} -> ${after}  << LEFT FULL SCREEN; put back: ${restored}`);
  }
  await log(`${why}: window ${before} -> ${after}`);
}

// Switch to a tab and report the window's state before, and half a second after.
async function switchTo(tab, why) {
  const before = await windowState(tab.windowId);
  await chrome.tabs.update(tab.id, { active: true });
  await report(tab.windowId, why, before, 500);
}

async function currentWindowId() {
  return (await chrome.windows.getLastFocused()).id;
}

async function goHome(source) {
  const windowId = await currentWindowId();
  const [active] = await chrome.tabs.query({ active: true, windowId });
  if (active && !isLauncher(active)) {
    const service = serviceOf(active.url || '');
    await chrome.storage.local.set({ lastTab: active.id });
    await log(`${source} received while on ${service ? service.name : (active.url || 'an unknown page')}`);
  } else {
    await log(`${source} received while on the launcher`);
  }
  const tabs = await chrome.tabs.query({ windowId });
  const launcher = tabs.find(isLauncher);
  if (launcher) return switchTo(launcher, 'Back to the launcher');
  const before = await windowState(windowId);
  await chrome.tabs.create({ windowId, url: LAUNCHER });
  await report(windowId, 'No launcher tab, opened one', before, 1000);
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

async function openService(name, windowId) {
  const service = SERVICES.find(s => s.name === name);
  const tabs = await chrome.tabs.query({ windowId });
  const open = tabs.find(t => serviceOf(t.url || t.pendingUrl || '') === service);
  if (open) {
    await chrome.storage.local.set({ lastTab: open.id });
    return switchTo(open, `${name} was already open, switched to it`);
  }
  const before = await windowState(windowId);
  const tab = await chrome.tabs.create({ windowId, url: service.url });
  await chrome.storage.local.set({ lastTab: tab.id });
  await report(windowId, `Opened ${name} in a new tab`, before, 1500);
}

async function search(text, windowId) {
  const before = await windowState(windowId);
  await chrome.tabs.create({ windowId, url: 'https://www.google.com/search?q=' + encodeURIComponent(text) });
  await report(windowId, `Searched Google for "${text}"`, before, 1500);
}

chrome.commands.onCommand.addListener(command => {
  if (command === 'go-home') goHome('Shortcut: back to the launcher');
  if (command === 'go-last') goLast('Shortcut: back to the last service');
});

chrome.action.onClicked.addListener(() => goHome('Toolbar button'));

chrome.runtime.onMessage.addListener((message, sender) => {
  const windowId = sender.tab?.windowId;
  if (message.type === 'open') openService(message.name, windowId);
  if (message.type === 'search') search(message.text, windowId);
  if (message.type === 'log') log(message.text);
});

chrome.runtime.onInstalled.addListener(async () => {
  const commands = await chrome.commands.getAll();
  for (const c of commands) {
    if (c.name.startsWith('_')) continue;
    await log(`Shortcut "${c.description}": ${c.shortcut || 'NOT SET (taken by something else; set one at chrome://extensions/shortcuts)'}`);
  }
});
