// The launcher's window rules (docs/specs/chromebook-launcher.md, chromebook-services.md).
// Everything goes through the chrome API object it is given, so the rules can be tested without a
// browser.
import { loadServices, loadSession, serviceOfWindow, pauseVideosInPage, originPattern } from './services.js';
import { fetchServiceIcon } from './icons.js';

export const LAUNCHER_PAGE = 'launcher.html';
const SEARCH = 'https://www.justwatch.com/ca/search';
const hostOf = url => { try { return new URL(url).hostname; } catch { return ''; } };

export function createController(chrome) {
  const fetchingIcons = new Set();
  const launcherUrl = chrome.runtime.getURL(LAUNCHER_PAGE);
  const isLauncherTab = tab => (tab.url || tab.pendingUrl || '').startsWith(launcherUrl);

  async function findLauncherWindow() {
    const windows = await chrome.windows.getAll({ populate: true });
    return windows.find(w => (w.tabs || []).some(isLauncherTab));
  }

  // A window Chrome did not make full screen, or that has left it, is put back.
  async function ensureFullscreen(id, state) {
    if (state !== 'fullscreen') await chrome.windows.update(id, { state: 'fullscreen' });
  }

  // A new full-screen popup window. On some systems Chrome shows a new popup in a normal window
  // until it is drawn (seen in Chromium on Linux), so full screen is asked for again while the
  // window settles: until its page has loaded and it is full screen, or for 5 seconds at most. On
  // ChromeOS it is full screen from the start, and nothing more is asked.
  async function openFullscreen(url, recordAs = async () => {}) {
    const created = await chrome.windows.create({ url, type: 'popup', state: 'fullscreen', focused: true });
    // Recorded at once, before it settles, so a page that loads or a link followed meanwhile counts.
    await recordAs(created.id);
    for (let waited = 0; waited <= 5000; waited += 100) {
      let w;
      try { w = await chrome.windows.get(created.id, { populate: true }); } catch { break; }
      if (w.state !== 'fullscreen') await chrome.windows.update(created.id, { state: 'fullscreen' });
      else if (!(w.tabs || []).some(t => t.status === 'loading')) break;
      await new Promise(r => setTimeout(r, 100));
    }
    return created.id;
  }

  async function bringForward(id) {
    const w = await chrome.windows.get(id);
    await ensureFullscreen(id, w.state);
    await chrome.windows.update(id, { focused: true });
    return id;
  }

  // chromebook-services#AC-5, AC-8: pause every playing video in every frame of the window. A site
  // the extension may not run on is left playing, without an error.
  async function pauseWindow(windowId) {
    let w;
    try { w = await chrome.windows.get(windowId, { populate: true }); } catch { return; }
    for (const tab of w.tabs || []) {
      try {
        await chrome.scripting.executeScript({ target: { tabId: tab.id, allFrames: true }, func: pauseVideosInPage });
      } catch { /* no permission on this site, or the page is gone */ }
    }
  }

  // chromebook-home-button#AC-1: the Home button, into a page of a service's window. Injecting
  // twice is harmless; a site the extension may not run on gets none.
  async function injectButton(tabId) {
    try {
      await chrome.scripting.executeScript({ target: { tabId }, files: ['homebutton.js'] });
    } catch { /* no permission on this site, or the page went */ }
  }
  async function injectButtonInWindow(windowId) {
    try {
      const w = await chrome.windows.get(windowId, { populate: true });
      for (const tab of w.tabs || []) if (tab.status !== 'loading') await injectButton(tab.id);
    } catch { /* it closed */ }
  }

  async function bringLauncherForward() {
    const open = await findLauncherWindow();
    if (open) return bringForward(open.id);
    return openFullscreen(launcherUrl);
  }

  // Remembers the last ordinary browsing window to have focus, for Back to browsing.
  async function noteFocus(windowId) {
    if (windowId === chrome.windows.WINDOW_ID_NONE) return;
    try {
      const w = await chrome.windows.get(windowId);
      if (w.type === 'normal') await chrome.storage.session.set({ lastBrowsingWindow: windowId });
    } catch { /* the window closed before we looked */ }
  }

  return {
    // chromebook-launcher#AC-2 to AC-4; chromebook-services#AC-5: pause the service being left,
    // then one launcher window, full screen, focused.
    async goHome() {
      const session = await loadSession(chrome);
      let focused;
      try { focused = await chrome.windows.getLastFocused(); } catch { /* no windows */ }
      const left = focused && serviceOfWindow(session, focused.id);
      if (left) {
        await pauseWindow(focused.id);
        await chrome.storage.session.set({ lastService: left });
      }
      return bringLauncherForward();
    },

    // chromebook-services#AC-2, AC-3: the service's own window, or a new full-screen one.
    async openService(serviceId) {
      const service = (await loadServices(chrome)).find(s => s.id === serviceId);
      if (!service) return bringLauncherForward();
      const session = await loadSession(chrome);
      const known = session.serviceWindows[serviceId];
      if (known !== undefined) {
        try {
          await bringForward(known);
          await chrome.storage.session.set({ lastService: serviceId });
          return known;
        } catch { /* it closed without our hearing */ }
      }
      const id = await openFullscreen(service.url, async created => {
        const latest = await loadSession(chrome);
        await chrome.storage.session.set({
          serviceWindows: { ...latest.serviceWindows, [serviceId]: created },
          lastService: serviceId,
        });
      });
      // In case the page loaded before the window was recorded as the service's.
      await injectButtonInWindow(id);
      return id;
    },

    // chromebook-services#AC-7: the last service used, as it was; else the launcher.
    async goLast() {
      const session = await loadSession(chrome);
      const id = session.lastService && session.serviceWindows[session.lastService];
      if (id !== undefined && id !== null) {
        try { return await bringForward(id); } catch { /* it closed */ }
      }
      return bringLauncherForward();
    },

    // chromebook-services#AC-6: the launcher gaining focus from a service's window pauses it.
    async onFocus(windowId) {
      if (windowId === chrome.windows.WINDOW_ID_NONE) return;
      const session = await loadSession(chrome);
      await chrome.storage.session.set({ lastFocused: windowId });
      await noteFocus(windowId);
      const now = serviceOfWindow(session, windowId);
      if (now) { await chrome.storage.session.set({ lastService: now }); return; }
      const previous = session.lastFocused;
      if (previous === undefined || previous === windowId) return;
      const left = serviceOfWindow(session, previous);
      if (!left) return;
      const launcher = await findLauncherWindow();
      if (launcher && launcher.id === windowId) {
        await pauseWindow(previous);
        await chrome.storage.session.set({ lastService: left });
      }
    },

    // chromebook-editing#AC-5: a removed service's window closes.
    async closeService(serviceId) {
      const session = await loadSession(chrome);
      const id = session.serviceWindows[serviceId];
      if (id === undefined) return;
      const serviceWindows = { ...session.serviceWindows };
      delete serviceWindows[serviceId];
      await chrome.storage.session.set({ serviceWindows });
      try { await chrome.windows.remove(id); } catch { /* already closed */ }
    },

    // A closed service window is forgotten, so choosing the service opens it again.
    async onWindowRemoved(windowId) {
      const session = await loadSession(chrome);
      const id = serviceOfWindow(session, windowId);
      if (!id) return;
      const serviceWindows = { ...session.serviceWindows };
      delete serviceWindows[id];
      await chrome.storage.session.set({ serviceWindows });
    },

    // chromebook-search#AC-1, AC-2: JustWatch's results for Canada in their own full-screen
    // window, reusing it while it still shows JustWatch. The text is never stored (AC-5).
    async search(text) {
      if (!text || !text.trim()) return undefined;
      const url = `${SEARCH}?q=${encodeURIComponent(text)}`;
      const { searchWindow } = await chrome.storage.session.get('searchWindow');
      if (searchWindow != null) {
        try {
          const w = await chrome.windows.get(searchWindow, { populate: true });
          const tab = (w.tabs || [])[0];
          if (tab && hostOf(tab.url || tab.pendingUrl) === 'www.justwatch.com') {
            await chrome.tabs.update(tab.id, { url });
            return await bringForward(searchWindow);
          }
        } catch { /* it closed */ }
      }
      return openFullscreen(url, created => chrome.storage.session.set({ searchWindow: created }));
    },

    // chromebook-search#AC-4: the search window going to one of the viewer's services becomes
    // that service's window.
    async onTabUpdated(tabId, change, tab) {
      if (!tab) return;
      // chromebook-home-button#AC-1: a page that has loaded in a service's window gets the button.
      if (change.status === 'complete') {
        const session = await loadSession(chrome);
        if (serviceOfWindow(session, tab.windowId)) await injectButton(tabId);
      }
      if (!change.url) return;
      const { searchWindow } = await chrome.storage.session.get('searchWindow');
      if (searchWindow == null || tab.windowId !== searchWindow) return;
      const host = hostOf(change.url);
      // On the service's site; if several services share it, the one whose own address it is under.
      const candidates = (await loadServices(chrome)).filter(s => host === s.site || host.endsWith(`.${s.site}`));
      const service = candidates.find(s => change.url.startsWith(s.url)) || candidates[0];
      if (!service) return;
      const session = await loadSession(chrome);
      await chrome.storage.session.set({
        serviceWindows: { ...session.serviceWindows, [service.id]: searchWindow },
        lastService: service.id,
      });
      await chrome.storage.session.set({ searchWindow: null }); // it is the service's now
    },

    // chromebook-icons#AC-2, AC-6: each allowed service without a kept icon gets its own.
    async refreshIcons() {
      const services = await loadServices(chrome);
      const allowed = s => chrome.permissions.contains({ origins: [originPattern(s.url)] }).catch(() => false);
      for (const s of services) {
        const { icons = {} } = await chrome.storage.local.get('icons');
        if (icons[s.id] || fetchingIcons.has(s.id)) continue;
        fetchingIcons.add(s.id);
        try {
          const data = await fetchServiceIcon(s, (...a) => fetch(...a), allowed);
          if (!data) continue;
          const latest = (await chrome.storage.local.get('icons')).icons || {};
          const stillThere = (await loadServices(chrome)).some(x => x.id === s.id);
          if (stillThere) await chrome.storage.local.set({ icons: { ...latest, [s.id]: data } });
        } finally {
          fetchingIcons.delete(s.id);
        }
      }
    },

    noteFocus,

    // chromebook-launcher#AC-7: the last browsing window, else any normal window, else a new one.
    async backToBrowsing() {
      const { lastBrowsingWindow } = await chrome.storage.session.get('lastBrowsingWindow');
      const windows = await chrome.windows.getAll();
      const normal = windows.filter(w => w.type === 'normal');
      const target = normal.find(w => w.id === lastBrowsingWindow) || normal[0];
      if (target) {
        await chrome.windows.update(target.id, { focused: true });
        return target.id;
      }
      const created = await chrome.windows.create({ focused: true });
      return created.id;
    },
  };
}
