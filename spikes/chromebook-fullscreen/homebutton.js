// Spike: a Home button on service pages for tablet mode. It appears when the screen is touched,
// fades after 4 seconds, and does what Alt+Shift+H does. It lives in a shadow root so the site's
// styles cannot reach it, and moves into the site's own full-screen element when there is one.
(() => {
  if (window.top !== window || document.getElementById('quiet-launcher-home')) return;
  const host = document.createElement('div');
  host.id = 'quiet-launcher-home';
  const root = host.attachShadow({ mode: 'closed' });
  root.innerHTML = `<style>
    button { position: fixed; left: 24px; bottom: 24px; z-index: 2147483647; width: 64px; height: 64px;
      border-radius: 32px; border: 2px solid #59646e; background: rgba(16,20,24,.85); color: #eef2f5;
      display: flex; align-items: center; justify-content: center; opacity: 0; pointer-events: none;
      transition: opacity .2s; touch-action: manipulation; }
    button.shown { opacity: 1; pointer-events: auto; }
    @media (prefers-reduced-motion: reduce) { button { transition: none; } }
  </style>
  <button aria-label="Back to the launcher"><svg width="30" height="30" viewBox="0 0 24 24" aria-hidden="true">
    <path d="M3 11l9-8 9 8M5 9.5V21h5v-6h4v6h5V9.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg></button>`;
  const button = root.querySelector('button');
  let timer;
  const place = () => (document.fullscreenElement && document.fullscreenElement.tagName !== 'VIDEO'
    ? document.fullscreenElement : document.documentElement).appendChild(host);
  const log = text => chrome.runtime.sendMessage({ type: 'log', text: `Home button: ${text}` });
  const show = () => {
    place();
    if (!button.classList.contains('shown')) {
      const fs = document.fullscreenElement;
      log(`shown on ${location.hostname}; site's own full screen: ${fs ? 'yes, on ' + fs.tagName.toLowerCase() : 'no'}`);
    }
    button.classList.add('shown');
    clearTimeout(timer);
    timer = setTimeout(() => button.classList.remove('shown'), 4000);
  };
  window.addEventListener('pointerdown', e => { if (e.pointerType === 'touch') show(); }, { capture: true, passive: true });
  document.addEventListener('fullscreenchange', place);
  button.addEventListener('click', e => {
    e.stopPropagation();
    button.classList.remove('shown');
    chrome.runtime.sendMessage({ type: 'home-button' });
  });
  place();
})();
