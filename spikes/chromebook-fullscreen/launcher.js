const tiles = document.getElementById('tiles');
for (const s of SERVICES) {
  const b = document.createElement('button');
  b.className = 'tile';
  b.textContent = s.name;
  b.style.background = s.colour;
  b.addEventListener('click', () => chrome.runtime.sendMessage({ type: 'open', name: s.name }));
  tiles.appendChild(b);
}

document.getElementById('search').addEventListener('submit', e => {
  e.preventDefault();
  const text = document.getElementById('query').value.trim();
  if (text) chrome.runtime.sendMessage({ type: 'search', text });
});

async function fullScreen(why) {
  const win = await chrome.windows.getCurrent();
  if (win.state === 'fullscreen') return;
  await chrome.windows.update(win.id, { state: 'fullscreen' });
  chrome.runtime.sendMessage({ type: 'log', text: `Launcher put the window in full screen (${why}); it was ${win.state}` });
}

const auto = document.getElementById('auto');
chrome.storage.local.get({ auto: true }).then(({ auto: on }) => {
  auto.checked = on;
  if (on) fullScreen('on opening');
});
auto.addEventListener('change', () => chrome.storage.local.set({ auto: auto.checked }));
chrome.storage.local.get({ mode: 'tabs' }).then(({ mode }) => {
  for (const r of document.querySelectorAll('input[name=mode]')) {
    r.checked = r.value === mode;
    r.addEventListener('change', () => {
      chrome.storage.local.set({ mode: r.value });
      chrome.runtime.sendMessage({ type: 'log', text: `Mode: ${r.value}` });
    });
  }
});
document.getElementById('full').addEventListener('click', () => fullScreen('button'));

chrome.commands.getAll().then(commands => {
  for (const c of commands) {
    const el = document.getElementById(c.name === 'go-home' ? 'home-key' : c.name === 'go-last' ? 'last-key' : '');
    if (el) el.textContent = c.shortcut || 'not set';
  }
});

const logEl = document.getElementById('log');
async function showLog() {
  const { entries = [] } = await chrome.storage.local.get('entries');
  logEl.textContent = entries.join('\n') || 'Nothing yet.';
  logEl.scrollTop = logEl.scrollHeight;
}
chrome.storage.onChanged.addListener(changes => { if (changes.entries) showLog(); });
showLog();

document.getElementById('copy').addEventListener('click', async () => {
  const { entries = [] } = await chrome.storage.local.get('entries');
  await navigator.clipboard.writeText(`${navigator.userAgent}\n\n${entries.join('\n')}`);
  chrome.runtime.sendMessage({ type: 'log', text: 'Results copied' });
});
document.getElementById('clear').addEventListener('click', () => chrome.storage.local.set({ entries: [] }));

window.addEventListener('focus', () => document.getElementById('query').focus());
document.getElementById('query').focus();
