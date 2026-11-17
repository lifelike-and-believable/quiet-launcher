// For the services slice: a copy of the extension whose manifest also grants http://localhost/*,
// stand-in service pages served locally (each with a playing video), and helpers to put services
// in the extension's storage. Optional permissions cannot be granted without a person in headless
// Chromium (docs/specs/chromebook-services.md, APIs and assumptions).
import { chromium } from 'playwright';
import { cpSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { createServer } from 'node:http';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const SOURCE = fileURLToPath(new URL('../../chromebook', import.meta.url));

const PAGE = name => `<!doctype html><title>${name}</title>
<h1>${name}</h1><canvas id="c" width="64" height="64"></canvas>
<iframe srcdoc="<video id=v muted></video><script>const c=document.createElement('canvas');c.width=c.height=8;const x=c.getContext('2d');setInterval(()=>{x.fillStyle='#'+Math.floor(Math.random()*999);x.fillRect(0,0,8,8)},50);v.srcObject=c.captureStream(10);v.play();</script>"></iframe>
<video id="v" muted></video>
<script>
  const c = document.getElementById('c'), x = c.getContext('2d');
  setInterval(() => { x.fillStyle = '#' + Math.floor(Math.random() * 999); x.fillRect(0, 0, 64, 64); }, 50);
  const v = document.getElementById('v'); v.srcObject = c.captureStream(20); v.play();
</script>`;

export async function loadWithServices() {
  const dir = mkdtempSync(join(tmpdir(), 'quiet-launcher-ext-'));
  cpSync(SOURCE, dir, { recursive: true });
  const manifest = JSON.parse(readFileSync(join(dir, 'manifest.json')));
  manifest.host_permissions = [...(manifest.host_permissions || []), 'http://localhost/*'];
  writeFileSync(join(dir, 'manifest.json'), JSON.stringify(manifest, null, 2));

  const hits = {};
  const server = createServer((req, res) => {
    const name = req.url.slice(1) || 'index';
    hits[name] = (hits[name] || 0) + 1;
    res.writeHead(200, { 'content-type': 'text/html' });
    res.end(PAGE(name));
  });
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const port = server.address().port;

  const profile = mkdtempSync(join(tmpdir(), 'quiet-launcher-'));
  const context = await chromium.launchPersistentContext(profile, {
    channel: 'chromium', headless: true,
    args: [`--disable-extensions-except=${dir}`, `--load-extension=${dir}`, '--autoplay-policy=no-user-gesture-required'],
  });
  let [worker] = context.serviceWorkers();
  if (!worker) worker = await context.waitForEvent('serviceworker');
  const id = new URL(worker.url()).host;
  const launcherUrl = `chrome-extension://${id}/launcher.html`;
  const url = (host, name) => `http://${host}:${port}/${name}`;

  const ext = {
    context, worker, launcherUrl, hits, url,
    // Alpha and Beta are on localhost (permission granted); Gamma on 127.0.0.1 (not granted).
    services: [
      { id: 'alpha', name: 'Alpha', url: url('localhost', 'alpha'), site: 'localhost', colour: '#1a73e8', hidden: false },
      { id: 'beta', name: 'Beta', url: url('localhost', 'beta'), site: 'localhost', colour: '#c4302b', hidden: false },
      { id: 'hidden', name: 'Hidden one', url: url('localhost', 'hidden'), site: 'localhost', colour: '#333', hidden: true },
      { id: 'gamma', name: 'Gamma', url: url('127.0.0.1', 'gamma'), site: '127.0.0.1', colour: '#6c2bd9', hidden: false },
    ],
    async seed() { await worker.evaluate(s => chrome.storage.local.set({ services: s }), ext.services); },
    call: (method, ...args) => worker.evaluate(([m, a]) => globalThis.quietLauncher[m](...a), [method, args]),
    windows: () => worker.evaluate(async () => (await chrome.windows.getAll({ populate: true }))
      .map(w => ({ id: w.id, type: w.type, state: w.state, urls: w.tabs.map(t => t.url || t.pendingUrl) }))),
    focusedId: () => worker.evaluate(async () => (await chrome.windows.getLastFocused()).id),
    async page(match) {
      const found = context.pages().find(p => p.url().startsWith(match));
      return found || context.waitForEvent('page', p => p.url().startsWith(match));
    },
    async launcher() {
      await ext.call('goHome');
      const page = await ext.page(launcherUrl);
      await page.waitForFunction(() => document.body.dataset.ready === 'true');
      return page;
    },
    async close() {
      await context.close();
      server.close();
      rmSync(profile, { recursive: true, force: true });
      rmSync(dir, { recursive: true, force: true });
    },
  };
  return ext;
}

// Whether any video in the page (including its frame) is playing.
export const playing = page => page.evaluate(() => [document, document.querySelector('iframe')?.contentDocument]
  .filter(Boolean).flatMap(d => [...d.querySelectorAll('video')]).some(v => !v.paused));
