import { test } from 'node:test';
import assert from 'node:assert/strict';
import { hintText, shortcutStatus, formatClock } from '../../chromebook/lib/launcher-view.js';

const commands = (home, last) => [
  { name: 'go-home', shortcut: home }, { name: 'go-last', shortcut: last }, { name: '_execute_action', shortcut: '' },
];

test('chromebook-launcher#AC-6: the hint line names the shortcuts in use', () => {
  assert.equal(hintText(commands('Alt+Shift+H', 'Alt+Shift+J')),
    'Alt+Shift+H brings you back here · Alt+Shift+J goes back to the last service');
  assert.equal(hintText(commands('Ctrl+Shift+1', 'Alt+Shift+J')),
    'Ctrl+Shift+1 brings you back here · Alt+Shift+J goes back to the last service');
});

test('chromebook-launcher#AC-6: with no go-home shortcut, the hint says so and the status line explains', () => {
  assert.equal(hintText(commands('', 'Alt+Shift+J')),
    'No shortcut back here yet · Alt+Shift+J goes back to the last service');
  assert.equal(shortcutStatus(commands('', 'Alt+Shift+J')),
    'Alt+Shift+H is used by something else, so it is not set. Set a shortcut at chrome://extensions/shortcuts.');
});

test('chromebook-launcher#AC-6: with no go-last shortcut, it is left out of the hint', () => {
  assert.equal(hintText(commands('Alt+Shift+H', '')), 'Alt+Shift+H brings you back here');
});

test('chromebook-launcher#AC-6: with both shortcuts set, the status line is empty', () => {
  assert.equal(shortcutStatus(commands('Alt+Shift+H', 'Alt+Shift+J')), '');
});

test('chromebook-launcher#AC-9: the clock shows hours and minutes, 12-hour without AM or PM', () => {
  const t = new Date(2026, 10, 17, 20, 25);
  assert.equal(formatClock(t, 'en-US'), '8:25');
  assert.equal(formatClock(new Date(2026, 10, 17, 9, 5), 'en-US'), '9:05');
});

test('chromebook-launcher#AC-9: the clock follows a 24-hour locale', () => {
  assert.equal(formatClock(new Date(2026, 10, 17, 20, 25), 'en-GB'), '20:25');
});
