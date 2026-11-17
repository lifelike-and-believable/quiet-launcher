// The launcher page (docs/specs/chromebook-launcher.md, chromebook-services.md,
// chromebook-editing.md; design-chromebook#layout and #editing).
import { hintText, shortcutStatus, formatClock } from './lib/launcher-view.js';
import { loadServices, loadSession, originPattern } from './lib/services.js';
import { addService, moveService, setHidden, removeService, serviceFromAddress, permissionPattern, siteOf } from './lib/editing.js';
import { CATALOGUE } from './lib/catalogue.js';

const $ = id => document.getElementById(id);
const el = (tag, props = {}, ...children) => {
  const e = Object.assign(document.createElement(tag), props);
  for (const c of children) e.append(c);
  return e;
};
const say = text => { $('announce').textContent = text; };

// The clock, in the Chromebook's own 12- or 24-hour form.
function tick() { $('clock').textContent = formatClock(new Date(), navigator.language); }
tick();
setInterval(tick, 15000);

const commands = await chrome.commands.getAll();
const EDIT_HINT = 'Select a service to move, hide, or remove it. Changes are saved as you go.';
function setStatus(text) { $('status').textContent = text; }
setStatus(shortcutStatus(commands));
$('hint').textContent = hintText(commands);

// Editing state (chromebook-editing): a lifted service moves in `working` until put down.
let editing = false;
let lifted = null; // { id, original, working }
let dragging = null; // { id, startX, startY, moved }
let closing = null; // what to do on Esc when a menu or dialog is open

async function services() { return lifted ? lifted.working : loadServices(chrome); }
async function save(list) { await chrome.storage.local.set({ services: list }); }

// chromebook-search#AC-1, AC-3: the search box.
const SEARCH_STATUS = 'Results open from JustWatch, in their own full-screen window. Enter searches.';
$('search').addEventListener('focus', () => setStatus(SEARCH_STATUS));
$('search').addEventListener('blur', () => { if ($('status').textContent === SEARCH_STATUS) setStatus(shortcutStatus(commands)); });
$('search').addEventListener('keydown', e => {
  if (e.key !== 'Enter') return;
  e.preventDefault();
  const text = $('search').value;
  if (!text.trim()) return;
  chrome.runtime.sendMessage({ type: 'search', text });
  $('search').value = '';
});

$('browse').addEventListener('click', () => chrome.runtime.sendMessage({ type: 'back-to-browsing' }));
$('edit').addEventListener('click', () => (editing ? stopEditing() : startEditing()));

function startEditing() {
  editing = true;
  document.body.classList.add('editing');
  $('editText').textContent = 'Done';
  $('hint').textContent = EDIT_HINT;
  say('Editing services');
  render();
}
function stopEditing() {
  closeOverlays();
  lifted = null;
  editing = false;
  document.body.classList.remove('editing');
  $('editText').textContent = 'Edit services';
  $('hint').textContent = hintText(commands);
  render();
}

// One tile: the service's icon and name on its colour, the name again underneath, then Open or
// Videos won't pause (chromebook-services#AC-1, AC-4, AC-8).
function tile(s, { open, allowed, hidden }) {
  const t = el('button', { type: 'button', className: `tile${hidden ? ' hidden' : ''}${lifted?.id === s.id ? ' lifted' : ''}` });
  t.dataset.id = s.id;
  t.dataset.nav = '';
  const notes = [open ? 'Open' : '', allowed ? '' : 'Videos won’t pause'].filter(Boolean);
  t.setAttribute('aria-label', [s.name, ...notes].join(', '));
  const own = keptIcons[s.id];
  // chromebook-icons#AC-3, AC-10: the icon alone on the tile; the name once, underneath.
  const icon = el('img', { alt: '', draggable: false, className: own ? 'own' : 'small',
    src: own || `/_favicon/?pageUrl=${encodeURIComponent(s.url)}&size=64` });
  const banner = el('span', { className: 'banner' }, icon);
  banner.style.background = s.colour;
  t.append(banner, ' ', el('span', { className: 'name', textContent: s.name }));
  if (open) t.append(' ', el('span', { className: 'badge', textContent: 'Open' }));
  if (!allowed) t.append(' ', el('span', { className: 'note', textContent: 'Videos won’t pause' }));
  t.addEventListener('click', () => {
    if (dragging?.moved) return;
    if (editing) openMenu(s, t, allowed);
    else chrome.runtime.sendMessage({ type: 'open-service', id: s.id });
  });
  t.addEventListener('dragstart', e => e.preventDefault()); // Chrome's own dragging would cancel ours
  if (editing && !hidden) t.addEventListener('pointerdown', e => startDrag(e, s.id));
  return t;
}

let drawn = []; // the list last drawn, which a drag starts from
let keptIcons = {}; // chromebook-icons#AC-3: each service's own icon, once fetched
async function render() {
  const list = await services();
  drawn = list;
  keptIcons = (await chrome.storage.local.get('icons')).icons || {};
  const visible = list.filter(s => !s.hidden);
  const hiddenOnes = list.filter(s => s.hidden);
  const session = await loadSession(chrome);
  const allowedOf = async s => chrome.permissions.contains({ origins: [originPattern(s.url)] }).catch(() => false);
  const allowed = new Map(await Promise.all(list.map(async s => [s.id, await allowedOf(s)])));
  const selectedId = document.activeElement?.dataset?.id;
  const box = $('services');
  if (!visible.length && !editing) {
    box.replaceChildren(el('p', { className: 'empty', innerHTML: 'No services yet.<br>Choose <b>Edit services</b>, then <b>Add a service</b>.' }));
    return;
  }
  const info = s => ({ open: s.id in session.serviceWindows, allowed: allowed.get(s.id), hidden: s.hidden });
  const grid = el('div', { className: 'grid' }, ...visible.map(s => tile(s, info(s))));
  const parts = [grid];
  if (editing) {
    const add = el('button', { type: 'button', className: 'tile add' }, el('span', { className: 'banner', textContent: '+ Add a service' }));
    add.dataset.id = '__add';
    add.dataset.nav = '';
    add.addEventListener('click', openAddPanel);
    grid.append(add);
    if (hiddenOnes.length) {
      parts.push(el('p', { className: 'section', textContent: 'Hidden services: not on the launcher. Select one to show it again.' }),
        el('div', { className: 'grid' }, ...hiddenOnes.map(s => tile(s, info(s)))));
    }
  }
  box.replaceChildren(...parts);
  if (selectedId) box.querySelector(`[data-id="${CSS.escape(selectedId)}"]`)?.focus();
}

// A service's menu while editing (chromebook-editing#AC-2).
function closeOverlays() {
  document.querySelectorAll('.menu, .dialog').forEach(x => x.remove());
  closing = null;
}
function openMenu(s, from, allowed) {
  closeOverlays();
  const items = [
    ['Move', () => lift(s.id)],
    [s.hidden ? 'Show' : 'Hide', async () => { await save(setHidden(await loadServices(chrome), s.id, !s.hidden)); }],
    ...(allowed ? [] : [['Allow pausing', () => askPermission(s.site)]]),
    ['Remove', () => confirmRemove(s)],
    ['Cancel', () => {}],
  ];
  const menu = el('div', { className: 'menu' });
  menu.setAttribute('role', 'menu');
  menu.setAttribute('aria-label', s.name);
  for (const [label, act] of items) {
    const b = el('button', { type: 'button', textContent: label });
    b.setAttribute('role', 'menuitem');
    b.addEventListener('click', async e => {
      e.stopPropagation();
      closeOverlays();
      await act();
      if (label !== 'Move' && label !== 'Remove') from.isConnected ? from.focus() : null;
    });
    menu.append(b);
  }
  const r = from.getBoundingClientRect();
  menu.style.left = `${Math.min(r.left + 10, innerWidth - 240)}px`;
  menu.style.top = `${Math.min(r.top + 150, innerHeight - 260)}px`;
  document.body.append(menu);
  closing = () => { closeOverlays(); from.focus(); };
  menu.querySelector('button').focus();
}

// Moving with the keyboard (chromebook-editing#AC-3).
async function lift(id) {
  // Lifted at once, from the list on screen, so a key pressed straight after Move moves it.
  const list = drawn;
  lifted = { id, original: list, working: list };
  $('hint').textContent = `Moving ${list.find(s => s.id === id).name}: arrow keys or drag to move it, Enter or release to put it down, Esc to put it back.`;
  await render();
  document.querySelector(`[data-id="${CSS.escape(id)}"]`)?.focus();
}
function columns() {
  const tiles = [...document.querySelectorAll('#services .grid:first-child .tile')];
  return tiles.filter(t => t.offsetTop === tiles[0]?.offsetTop).length || 1;
}
async function step(by) {
  const visible = lifted.working.filter(s => !s.hidden);
  const at = visible.findIndex(s => s.id === lifted.id);
  const to = Math.max(0, Math.min(visible.length - 1, at + by));
  if (to === at) return;
  lifted.working = moveService(lifted.working, lifted.id, lifted.working.indexOf(visible[to]));
  await render();
  document.querySelector(`[data-id="${CSS.escape(lifted.id)}"]`)?.focus();
  say(`${visible[at].name}, moved to ${to + 1} of ${visible.length}`);
}
async function putDown() {
  const { working, id } = lifted;
  lifted = null;
  await save(working);
  $('hint').textContent = EDIT_HINT;
  await render();
  document.querySelector(`[data-id="${CSS.escape(id)}"]`)?.focus();
}
async function putBack() {
  const { id } = lifted;
  lifted = null;
  $('hint').textContent = EDIT_HINT;
  await render();
  document.querySelector(`[data-id="${CSS.escape(id)}"]`)?.focus();
}

// Dragging with the touchpad or a finger (chromebook-editing#AC-3).
function startDrag(e, id) {
  dragging = { id, startX: e.clientX, startY: e.clientY, moved: false };
  e.currentTarget.setPointerCapture?.(e.pointerId);
}
document.addEventListener('pointermove', e => {
  if (!dragging) return;
  if (!dragging.moved && Math.hypot(e.clientX - dragging.startX, e.clientY - dragging.startY) < 8) return;
  if (!dragging.moved) {
    // Lift at once, from the list last drawn, so no movement is lost while storage is read.
    dragging.moved = true;
    lifted = { id: dragging.id, original: drawn, working: drawn };
  }
  const under = document.elementsFromPoint(e.clientX, e.clientY)
    .map(x => x.closest?.('.tile'))
    .find(t => t && !t.classList.contains('add') && !t.classList.contains('hidden') && t.dataset.id !== dragging.id);
  if (under) {
    const target = lifted.working.findIndex(x => x.id === under.dataset.id);
    if (target >= 0) lifted.working = moveService(lifted.working, dragging.id, target);
  }
  render();
});
document.addEventListener('pointerup', async () => {
  if (!dragging) return;
  const moved = dragging.moved;
  if (moved && lifted) await putDown();
  setTimeout(() => { dragging = null; }, 0); // the click after a drag does not open the menu
  if (!moved) dragging = null;
});

// Removing, after asking (chromebook-editing#AC-5).
function confirmRemove(s) {
  closeOverlays();
  const remove = el('button', { type: 'button', className: 'btn primary', textContent: 'Remove' });
  const cancel = el('button', { type: 'button', className: 'btn', textContent: 'Cancel' });
  const text = el('p', { textContent: `Remove ${s.name}? Its window will close.` });
  text.id = 'remove-text';
  const panel = el('div', { className: 'panel' }, text, el('div', { className: 'row' }, remove, cancel));
  panel.setAttribute('role', 'alertdialog');
  panel.setAttribute('aria-modal', 'true');
  panel.setAttribute('aria-describedby', 'remove-text');
  panel.setAttribute('aria-label', `Remove ${s.name}`);
  const dialog = el('div', { className: 'dialog' }, panel);
  document.body.append(dialog);
  const back = () => { closeOverlays(); document.querySelector(`[data-id="${CSS.escape(s.id)}"]`)?.focus(); };
  closing = back;
  cancel.addEventListener('click', back);
  remove.addEventListener('click', async () => {
    closeOverlays();
    await save(removeService(await loadServices(chrome), s.id));
    const { icons = {} } = await chrome.storage.local.get('icons'); // chromebook-icons#AC-7
    delete icons[s.id];
    await chrome.storage.local.set({ icons });
    chrome.runtime.sendMessage({ type: 'close-service', id: s.id });
    $('edit').focus();
  });
  cancel.focus();
}

// Asking Chrome for a site; it must be the first thing a click or key does (chromebook-editing#AC-7, AC-8).
function askPermission(site) {
  return chrome.permissions.request({ origins: [permissionPattern(site)] }).catch(() => false)
    .then(() => { chrome.runtime.sendMessage({ type: 'refresh-icons' }); return render(); });
}

// Adding a service (chromebook-editing#AC-6, AC-7).
function openAddPanel() {
  closeOverlays();
  const have = new Set(drawn.map(s => s.site));
  const choices = el('div', { className: 'choices' });
  for (const entry of CATALOGUE) {
    const already = have.has(siteOf(entry.url));
    const b = el('button', { type: 'button', className: `choice${already ? ' on' : ''}` }, entry.name, el('span', { textContent: already ? '✓' : '' }));
    b.setAttribute('aria-disabled', String(already));
    if (already) b.setAttribute('aria-label', `${entry.name}, already in your services`);
    b.addEventListener('click', () => { if (!already) add(entry); });
    choices.append(b);
  }
  const field = el('input', { type: 'text', placeholder: 'Or another site’s address, such as hollywoodsuite.ca', autocomplete: 'off' });
  field.setAttribute('aria-label', 'Another site’s address');
  const addButton = el('button', { type: 'button', className: 'btn primary', textContent: 'Add' });
  const cancel = el('button', { type: 'button', className: 'btn', textContent: 'Cancel' });
  const panel = el('div', { className: 'panel' },
    el('h2', { textContent: 'Add a service' }),
    el('p', { textContent: 'Streaming services available in Canada. Ones you already have are marked.' }),
    choices, el('div', { className: 'row' }, field, addButton, cancel));
  panel.setAttribute('role', 'dialog');
  panel.setAttribute('aria-modal', 'true');
  panel.setAttribute('aria-label', 'Add a service');
  document.body.append(el('div', { className: 'dialog' }, panel));
  const back = () => { closeOverlays(); document.querySelector('[data-id="__add"]')?.focus(); };
  closing = back;
  cancel.addEventListener('click', back);
  const fromField = () => {
    const service = serviceFromAddress(field.value);
    if (!service) { setStatus('That isn’t a site address.'); field.focus(); return; }
    add(service);
  };
  addButton.addEventListener('click', fromField);
  field.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); fromField(); } });
  (choices.querySelector('.choice:not(.on)') || field).focus();

  function add(entry) {
    const asked = chrome.permissions.request({ origins: [permissionPattern(siteOf(entry.url))] }).catch(() => false);
    (async () => {
      const { list, service } = addService(await loadServices(chrome), entry);
      closeOverlays();
      setStatus(shortcutStatus(commands));
      await save(list);
      await asked;
      chrome.runtime.sendMessage({ type: 'refresh-icons' }); // chromebook-icons#AC-2
      await render();
      document.querySelector(`[data-id="${CSS.escape(service.id)}"]`)?.focus();
      say(`${service.name} added`);
    })();
  }
}

await render();
chrome.runtime.sendMessage({ type: 'refresh-icons' }); // chromebook-icons#AC-6
chrome.storage.onChanged.addListener(() => { if (!dragging) render(); });
window.addEventListener('focus', () => { if (!dragging) render(); });

// Arrow keys move between neighbours, as on the TV, without wrapping (chromebook-launcher#AC-8,
// chromebook-services#AC-9); in a menu, up and down; a lifted service moves instead.
const centre = e => { const r = e.getBoundingClientRect(); return { x: r.left + r.width / 2, y: r.top + r.height / 2, h: r.height }; };
function neighbour(from, key) {
  const a = centre(from);
  const items = [...document.querySelectorAll('[data-nav]')].filter(x => x !== from);
  if (key === 'ArrowLeft' || key === 'ArrowRight') {
    const dir = key === 'ArrowRight' ? 1 : -1;
    const row = items.filter(x => { const b = centre(x); return Math.abs(b.y - a.y) < Math.max(a.h, b.h) / 2 && (b.x - a.x) * dir > 0; });
    return row.sort((p, q) => Math.abs(centre(p).x - a.x) - Math.abs(centre(q).x - a.x))[0];
  }
  const dir = key === 'ArrowDown' ? 1 : -1;
  const beyond = items.filter(x => (centre(x).y - a.y) * dir > 10);
  if (!beyond.length) return undefined;
  const nearestRow = Math.min(...beyond.map(x => Math.abs(centre(x).y - a.y)));
  return beyond.filter(x => Math.abs(centre(x).y - a.y) - nearestRow < 10)
    .sort((p, q) => Math.abs(centre(p).x - a.x) - Math.abs(centre(q).x - a.x))[0];
}
document.addEventListener('keydown', async e => {
  if (e.key === 'Escape') {
    e.preventDefault();
    if (closing) return closing();
    if (lifted) return putBack();
    if (editing) { stopEditing(); $('edit').focus(); }
    return;
  }
  if (lifted && !closing) {
    const by = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -columns(), ArrowDown: columns() }[e.key];
    if (by) { e.preventDefault(); return step(by); }
    if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); return putDown(); }
    return;
  }
  const from = document.activeElement;
  if (from?.getAttribute('role') === 'menuitem' && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
    e.preventDefault();
    (e.key === 'ArrowDown' ? from.nextElementSibling : from.previousElementSibling)?.focus();
    return;
  }
  if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(e.key)) return;
  if (!from?.hasAttribute('data-nav')) return;
  // In the search box, Left and Right move the caret until it reaches an end.
  if (from === $('search') && (e.key === 'ArrowLeft' || e.key === 'ArrowRight')) {
    const { selectionStart: s, selectionEnd: end, value } = from;
    if (e.key === 'ArrowLeft' && !(s === 0 && end === 0)) return;
    if (e.key === 'ArrowRight' && !(s === value.length)) return;
  }
  e.preventDefault();
  neighbour(from, e.key)?.focus();
});

// The first service is selected; with none, Edit services.
($('services').querySelector('.tile') || $('edit')).focus();
document.body.dataset.ready = 'true';
