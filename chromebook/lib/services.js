// The viewer's services and the windows that belong to them (docs/specs/chromebook-services.md).
// Services live in chrome.storage.local; which window is whose lives in chrome.storage.session.

export async function loadServices(chrome) {
  return (await chrome.storage.local.get('services')).services || [];
}

export async function loadSession(chrome) {
  const s = await chrome.storage.session.get(['serviceWindows', 'lastService', 'lastFocused', 'lastBrowsingWindow']);
  return { serviceWindows: {}, ...s };
}

// The service id whose window this is, if any.
export function serviceOfWindow(session, windowId) {
  return Object.keys(session.serviceWindows).find(id => session.serviceWindows[id] === windowId);
}

// The permission pattern that covers a service's site (ports are not part of a pattern).
export function originPattern(url) {
  const u = new URL(url);
  return `${u.protocol}//${u.hostname}/*`;
}

// Runs in each frame of a service's page: pauses every playing video and says how many.
export function pauseVideosInPage() {
  let paused = 0;
  for (const v of document.querySelectorAll('video')) {
    if (!v.paused) { v.pause(); paused++; }
  }
  return paused;
}
