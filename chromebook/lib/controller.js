// The launcher's window rules (docs/specs/chromebook-launcher.md, chromebook-services.md).
// Everything goes through the chrome API object it is given, so the rules can be tested without a
// browser.
import { loadServices, loadSession, serviceOfWindow, pauseVideosInPage } from './services.js';

export const LAUNCHER_PAGE = 'launcher.html';

export function createController(chrome) {
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
  // until it is drawn (seen in Chromium on Linux), so full screen is asked for again once the page
  // has loaded, or after 5 seconds at most. On ChromeOS it is already full screen, and asking again
  // changes nothing.
  async function openFullscreen(url) {
    const created = await chrome.windows.create({ url, type: 'popup', state: 'fullscreen', focused: true });
    await chrome.windows.update(created.id, { state: 'fullscreen' });
    for (let waited = 0; waited < 5000; waited += 100) {
      let w;
      try { w = await chrome.windows.get(created.id, { populate: true }); } catch { return created.id; }
      if (!(w.tabs || []).some(t => t.status === 'loading')) {
        if (w.state !== 'fullscreen') await chrome.windows.update(created.id, { state: 'fullscreen' });
        return created.id;
      }
      await new Promise(r => setTimeout(r, 100));
    }
    await chrome.windows.update(created.id, { state: 'fullscreen' });
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
      const id = await openFullscreen(service.url);
      await chrome.storage.session.set({
        serviceWindows: { ...session.serviceWindows, [serviceId]: id },
        lastService: serviceId,
      });
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

    // A closed service window is forgotten, so choosing the service opens it again.
    async onWindowRemoved(windowId) {
      const session = await loadSession(chrome);
      const id = serviceOfWindow(session, windowId);
      if (!id) return;
      const serviceWindows = { ...session.serviceWindows };
      delete serviceWindows[id];
      await chrome.storage.session.set({ serviceWindows });
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
