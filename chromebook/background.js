// The extension's service worker: the shortcuts and the launcher's requests go to the controller.
import { createController } from './lib/controller.js';

// Exposed so the extension tests can call what the shortcut calls (commands cannot be pressed in
// headless Chromium); the launcher's messages also go through it.
globalThis.quietLauncher = createController(chrome);

chrome.commands.onCommand.addListener(command => {
  if (command === 'go-home') globalThis.quietLauncher.goHome();
});

chrome.windows.onFocusChanged.addListener(windowId => globalThis.quietLauncher.noteFocus(windowId));

chrome.runtime.onMessage.addListener(message => {
  if (message?.type === 'back-to-browsing') globalThis.quietLauncher.backToBrowsing();
});
