// Renders each state of mockup.html to a 1366x768 PNG: node render.js (needs Playwright).
const { chromium } = require('playwright');
const path = require('path');
const states = ['default', 'open', 'search', 'edit', 'edit-menu', 'moving', 'hidden', 'add', 'permission', 'empty', 'shortcut-taken', 'home-button'];
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1366, height: 768 }, deviceScaleFactor: 1 });
  for (const s of states) {
    await page.goto('file://' + path.join(__dirname, 'mockup.html') + '#' + s);
    await page.reload();
    await page.screenshot({ path: path.join(__dirname, s + '.png') });
    console.log('rendered ' + s + '.png');
  }
  await browser.close();
})();
