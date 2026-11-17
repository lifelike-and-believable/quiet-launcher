// Text the launcher page shows that depends on Chrome's state (docs/specs/chromebook-launcher.md).
const shortcutOf = (commands, name) => (commands.find(c => c.name === name) || {}).shortcut || '';

// AC-6: the hint line names the shortcuts Chrome has given the commands.
export function hintText(commands) {
  const home = shortcutOf(commands, 'go-home');
  const last = shortcutOf(commands, 'go-last');
  return [home ? `${home} brings you back here` : 'No shortcut back here yet',
    ...(last ? [`${last} goes back to the last service`] : [])].join(' · ');
}

// AC-6: the status line explains a go-home shortcut Chrome could not give the extension.
export function shortcutStatus(commands) {
  return shortcutOf(commands, 'go-home') ? ''
    : 'Alt+Shift+H is used by something else, so it is not set. Set a shortcut at chrome://extensions/shortcuts.';
}

// AC-9: hours and minutes in the locale's 12- or 24-hour form, without AM or PM.
export function formatClock(date, locale) {
  return new Intl.DateTimeFormat(locale, { hour: 'numeric', minute: '2-digit' })
    .formatToParts(date)
    .filter(p => p.type !== 'dayPeriod')
    .map(p => p.value)
    .join('')
    .trim();
}
