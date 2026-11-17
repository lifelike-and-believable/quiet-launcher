// The launcher page (docs/specs/chromebook-launcher.md, chromebook-services.md;
// design-chromebook#layout).
import { hintText, shortcutStatus, formatClock } from './lib/launcher-view.js';
import { loadServices, loadSession, originPattern } from './lib/services.js';

const $ = id => document.getElementById(id);

// The clock, in the Chromebook's own 12- or 24-hour form.
function tick() { $('clock').textContent = formatClock(new Date(), navigator.language); }
tick();
setInterval(tick, 15000);

// The shortcuts in use, and what to do if go-home has none.
const commands = await chrome.commands.getAll();
$('hint').textContent = hintText(commands);
$('status').textContent = shortcutStatus(commands);

$('browse').addEventListener('click', () => chrome.runtime.sendMessage({ type: 'back-to-browsing' }));

// chromebook-services#AC-1, AC-4, AC-8: the services, with Open and Videos won't pause.
async function render() {
  const services = (await loadServices(chrome)).filter(s => !s.hidden);
  const session = await loadSession(chrome);
  const open = new Set(Object.keys(session.serviceWindows));
  const allowed = await Promise.all(services.map(s =>
    chrome.permissions.contains({ origins: [originPattern(s.url)] }).catch(() => false)));
  const selectedId = document.activeElement?.dataset?.id;
  const box = $('services');
  if (!services.length) {
    box.innerHTML = '<p class="empty">No services yet.<br>Choose <b>Edit services</b>, then <b>Add a service</b>.</p>';
    return;
  }
  const grid = document.createElement('div');
  grid.className = 'grid';
  services.forEach((s, i) => {
    const t = document.createElement('button');
    t.type = 'button';
    t.className = 'tile';
    t.dataset.id = s.id;
    t.dataset.nav = '';
    const notes = [open.has(s.id) ? 'Open' : '', allowed[i] ? '' : 'Videos won’t pause'].filter(Boolean);
    t.setAttribute('aria-label', [s.name, ...notes].join(', '));
    const banner = document.createElement('span');
    banner.className = 'banner';
    banner.style.background = s.colour;
    const icon = document.createElement('img');
    icon.alt = '';
    icon.src = `/_favicon/?pageUrl=${encodeURIComponent(s.url)}&size=32`;
    const title = document.createElement('span');
    title.className = 'title';
    title.textContent = s.name;
    banner.append(icon, ' ', title);
    const name = document.createElement('span');
    name.className = 'name';
    name.textContent = s.name;
    t.append(banner, ' ', name);
    if (open.has(s.id)) t.insertAdjacentHTML('beforeend', ' <span class="badge">Open</span>');
    if (!allowed[i]) t.insertAdjacentHTML('beforeend', ' <span class="note">Videos won’t pause</span>');
    t.addEventListener('click', () => chrome.runtime.sendMessage({ type: 'open-service', id: s.id }));
    grid.append(t);
  });
  box.replaceChildren(grid);
  if (selectedId) box.querySelector(`[data-id="${CSS.escape(selectedId)}"]`)?.focus();
}

await render();
chrome.storage.onChanged.addListener(() => render());
window.addEventListener('focus', () => render());

// Arrow keys move between neighbours, as on the TV, without wrapping (chromebook-launcher#AC-8,
// chromebook-services#AC-9).
const centre = el => { const r = el.getBoundingClientRect(); return { x: r.left + r.width / 2, y: r.top + r.height / 2, h: r.height }; };
function neighbour(from, key) {
  const a = centre(from);
  const items = [...document.querySelectorAll('[data-nav]')].filter(el => el !== from);
  if (key === 'ArrowLeft' || key === 'ArrowRight') {
    const dir = key === 'ArrowRight' ? 1 : -1;
    const row = items.filter(el => { const b = centre(el); return Math.abs(b.y - a.y) < Math.max(a.h, b.h) / 2 && (b.x - a.x) * dir > 0; });
    return row.sort((p, q) => Math.abs(centre(p).x - a.x) - Math.abs(centre(q).x - a.x))[0];
  }
  const dir = key === 'ArrowDown' ? 1 : -1;
  const beyond = items.filter(el => (centre(el).y - a.y) * dir > 10);
  if (!beyond.length) return undefined;
  const nearestRow = Math.min(...beyond.map(el => Math.abs(centre(el).y - a.y)));
  return beyond.filter(el => Math.abs(centre(el).y - a.y) - nearestRow < 10)
    .sort((p, q) => Math.abs(centre(p).x - a.x) - Math.abs(centre(q).x - a.x))[0];
}
document.addEventListener('keydown', e => {
  if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(e.key)) return;
  const from = document.activeElement;
  if (!from?.hasAttribute('data-nav')) return;
  // In the search box, Left and Right move the caret until it reaches an end.
  if (from === $('search') && (e.key === 'ArrowLeft' || e.key === 'ArrowRight')) {
    const { selectionStart: s, selectionEnd: end, value } = from;
    if (e.key === 'ArrowLeft' && !(s === 0 && end === 0)) return;
    if (e.key === 'ArrowRight' && !(s === value.length)) return;
  }
  const next = neighbour(from, e.key);
  e.preventDefault();
  next?.focus();
});

// The first service is selected; with none, Edit services.
($('services').querySelector('.tile') || $('edit')).focus();
document.body.dataset.ready = 'true';
