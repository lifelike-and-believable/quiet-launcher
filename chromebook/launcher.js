// The launcher page (docs/specs/chromebook-launcher.md; design-chromebook#layout).
import { hintText, shortcutStatus, formatClock } from './lib/launcher-view.js';

const $ = id => document.getElementById(id);

// AC-9: the clock, in the Chromebook's own 12- or 24-hour form.
function tick() { $('clock').textContent = formatClock(new Date(), navigator.language); }
tick();
setInterval(tick, 15000);

// AC-5: with no services yet (adding them comes with editing), say how to add them.
$('services').innerHTML = '<p class="empty">No services yet.<br>Choose <b>Edit services</b>, then <b>Add a service</b>.</p>';

// AC-6: the shortcuts in use, and what to do if go-home has none.
const commands = await chrome.commands.getAll();
$('hint').textContent = hintText(commands);
$('status').textContent = shortcutStatus(commands);

// AC-7: Back to browsing.
$('browse').addEventListener('click', () => chrome.runtime.sendMessage({ type: 'back-to-browsing' }));

// AC-8: Left and Right move along the top row, without wrapping. Tab order is the page's order.
const topRow = [...document.querySelectorAll('[data-row="top"]')];
document.addEventListener('keydown', e => {
  if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
  const i = topRow.indexOf(document.activeElement);
  if (i < 0) return;
  // In the search box, the arrows move the caret until it reaches an end.
  if (document.activeElement === $('search')) {
    const { selectionStart: s, selectionEnd: end, value } = $('search');
    if (e.key === 'ArrowLeft' && !(s === 0 && end === 0)) return;
    if (e.key === 'ArrowRight' && !(s === value.length)) return;
  }
  const next = topRow[i + (e.key === 'ArrowRight' ? 1 : -1)];
  if (next) { e.preventDefault(); next.focus(); }
});

// AC-5: with no services, Edit services is selected.
$('edit').focus();
document.body.dataset.ready = 'true';
