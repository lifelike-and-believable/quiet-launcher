// The launcher's window rules (docs/specs/chromebook-launcher.md). Everything goes through the
// chrome API object it is given, so the rules can be tested without a browser.
export const LAUNCHER_PAGE = 'launcher.html';

export function createController(chrome) {
  const launcherUrl = chrome.runtime.getURL(LAUNCHER_PAGE);
  const isLauncherTab = tab => (tab.url || tab.pendingUrl || '').startsWith(launcherUrl);

  async function findLauncherWindow() {
    const windows = await chrome.windows.getAll({ populate: true });
    return windows.find(w => (w.tabs || []).some(isLauncherTab));
  }

  // A window Chrome did not make full screen, or that has left it, is put back (AC-2, AC-3).
  async function ensureFullscreen(id, state) {
    if (state !== 'fullscreen') await chrome.windows.update(id, { state: 'fullscreen' });
  }

  return {
    // AC-2, AC-3, AC-4: one launcher window, full screen, focused; nothing else touched.
    async goHome() {
      const open = await findLauncherWindow();
      if (open) {
        await ensureFullscreen(open.id, open.state);
        await chrome.windows.update(open.id, { focused: true });
        return open.id;
      }
      const created = await chrome.windows.create({ url: launcherUrl, type: 'popup', state: 'fullscreen', focused: true });
      // Chrome can report a new popup as full screen and then show it in a normal window (seen in
      // headless Chromium), so full screen is asked for again once the window exists.
      await chrome.windows.update(created.id, { state: 'fullscreen' });
      return created.id;
    },

    // Remembers the last ordinary browsing window to have focus, for Back to browsing (AC-7).
    async noteFocus(windowId) {
      if (windowId === chrome.windows.WINDOW_ID_NONE) return;
      try {
        const w = await chrome.windows.get(windowId);
        if (w.type === 'normal') await chrome.storage.session.set({ lastBrowsingWindow: windowId });
      } catch { /* the window closed before we looked */ }
    },

    // Stubs for chromebook-services (tests first).
    async openService() { return -1; },
    async goLast() { return -1; },
    async onFocus() {},
    async onWindowRemoved() {},

    // AC-7: the last browsing window, else any normal window, else a new one. The launcher stays.
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
