import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { loadWithServices, playing } from './services-harness.js';

let ext;
let service;
before(async () => {
  ext = await loadWithServices();
  await ext.seed();
  await ext.launcher();
  await ext.call('openService', 'alpha');
  service = await ext.page(ext.url('localhost', 'alpha'));
  await service.waitForLoadState('load');
  await settle(800);
});
after(async () => { await ext?.close(); });

function settle(ms = 300) { return new Promise(r => setTimeout(r, ms)); }
// A touch (or another pointer) at a point on the page, as the page's own scripts would see it.
const pointer = (page, type, x = 400, y = 300) => page.evaluate(([t, px, py]) => {
  window.dispatchEvent(new PointerEvent('pointerdown', { pointerType: t, clientX: px, clientY: py, bubbles: true }));
}, [type, x, y]);
// The button is in a closed shadow root: whether it is there is told by what is at its place.
const corner = page => page.evaluate(() => ({ x: 24 + 32, y: innerHeight - 24 - 32 }));
const shownAt = async page => {
  const { x, y } = await corner(page);
  return page.evaluate(([px, py]) => document.elementFromPoint(px, py)?.id === 'quiet-launcher-home', [x, y]);
};
const hostBox = page => page.evaluate(() => {
  const h = document.getElementById('quiet-launcher-home');
  return h ? true : false;
});

test('chromebook-home-button#AC-1, AC-2: a service page gets the button, hidden until a touch', async () => {
  assert.equal(await hostBox(service), true, 'the button is on the page');
  assert.equal(await shownAt(service), false, 'hidden before a touch');
  await pointer(service, 'mouse');
  await settle();
  assert.equal(await shownAt(service), false, 'a mouse does not show it');
  await pointer(service, 'touch');
  await settle();
  assert.equal(await shownAt(service), true, 'a touch shows it');
});

test('chromebook-home-button#AC-2: it goes 4 seconds after the last touch', async () => {
  await pointer(service, 'touch');
  await settle(3000);
  assert.equal(await shownAt(service), true, 'still there after 3 s');
  await settle(1500);
  assert.equal(await shownAt(service), false, 'gone after 4 s');
});

test('chromebook-home-button#AC-6: it reads Back to the launcher, button', async () => {
  const cdp = await ext.context.newCDPSession(service);
  const { nodes } = await cdp.send('Accessibility.getFullAXTree');
  const names = nodes.filter(n => n.role?.value === 'button').map(n => n.name?.value);
  assert.ok(names.includes('Back to the launcher'), JSON.stringify(names));
  await cdp.detach();
});

test('chromebook-home-button#AC-5: the site\'s styles and scripts cannot reach it', async () => {
  await service.addStyleTag({ content: 'button, div, * { display: none !important; visibility: hidden !important; }' });
  await service.evaluate(() => document.body.style.setProperty('display', 'block', 'important'));
  await pointer(service, 'touch');
  await settle();
  assert.equal(await shownAt(service), true, 'still shown');
  assert.equal(await service.evaluate(() => document.getElementById('quiet-launcher-home').shadowRoot), null);
  await service.evaluate(() => document.querySelectorAll('style').forEach(s => { if (s.textContent.includes('display: none')) s.remove(); }));
});

test('chromebook-home-button#AC-7: with reduce motion, no fading', async () => {
  await service.emulateMedia({ reducedMotion: 'reduce' });
  await pointer(service, 'touch');
  const shownAtOnce = await shownAt(service);
  await service.emulateMedia({ reducedMotion: 'no-preference' });
  assert.equal(shownAtOnce, true);
});

test('chromebook-home-button#AC-4: over the site\'s own full-screen player', async () => {
  await service.evaluate(() => document.querySelector('iframe').parentElement.requestFullscreen());
  await settle(500);
  await pointer(service, 'touch');
  await settle();
  assert.equal(await shownAt(service), true);
  await service.evaluate(() => document.exitFullscreen());
  await settle(500);
});

test('chromebook-home-button#AC-3: tapping it pauses the video and brings the launcher forward', async () => {
  const [w] = (await ext.windows()).filter(x => x.urls.some(u => u === ext.url('localhost', 'alpha')));
  await ext.worker.evaluate(id => chrome.windows.update(id, { focused: true }), w.id);
  await service.evaluate(() => [document, document.querySelector('iframe').contentDocument]
    .flatMap(d => [...d.querySelectorAll('video')]).forEach(v => v.play()));
  assert.equal(await playing(service), true);
  await pointer(service, 'touch');
  await settle();
  const { x, y } = await corner(service);
  await service.mouse.click(x, y);
  await settle(800);
  assert.equal(await playing(service), false, 'paused');
  const launcher = (await ext.windows()).find(x => x.urls.some(u => u === ext.launcherUrl));
  assert.equal(await ext.focusedId(), launcher.id, 'the launcher is in front');
});

test('chromebook-home-button#AC-1: the launcher and ordinary browsing pages get none', async () => {
  const launcher = await ext.page(ext.launcherUrl);
  assert.equal(await hostBox(launcher), false);
  const browsing = await ext.context.newPage();
  await browsing.goto(ext.url('localhost', 'beta'));
  await settle(800);
  assert.equal(await hostBox(browsing), false);
  await browsing.close();
});
