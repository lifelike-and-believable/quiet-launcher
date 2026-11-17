// The extension's service worker: the shortcuts, window events, and the launcher's requests go to
// the controller.
import { createController } from './lib/controller.js';

// Exposed so the extension tests can call what the shortcuts call (commands cannot be pressed in
// headless Chromium); the launcher's messages also go through it.
globalThis.quietLauncher = createController(chrome);

chrome.commands.onCommand.addListener(command => {
  if (command === 'go-home') globalThis.quietLauncher.goHome();
  if (command === 'go-last') globalThis.quietLauncher.goLast();
});

chrome.windows.onFocusChanged.addListener(windowId => globalThis.quietLauncher.onFocus(windowId));
chrome.tabs.onUpdated.addListener((tabId, change, tab) => globalThis.quietLauncher.onTabUpdated(tabId, change, tab));
chrome.windows.onRemoved.addListener(windowId => globalThis.quietLauncher.onWindowRemoved(windowId));

chrome.runtime.onMessage.addListener(message => {
  if (message?.type === 'back-to-browsing') globalThis.quietLauncher.backToBrowsing();
  if (message?.type === 'open-service') globalThis.quietLauncher.openService(message.id);
  if (message?.type === 'search') globalThis.quietLauncher.search(message.text);
  if (message?.type === 'close-service') globalThis.quietLauncher.closeService(message.id);
});
