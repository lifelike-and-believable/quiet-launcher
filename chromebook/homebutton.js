// The Home button on a service's page (docs/specs/chromebook-home-button.md;
// design-chromebook#the-home-button-on-service-pages). Injected by the extension into pages in a
// service's window. A touch shows it in the bottom-left corner for 4 seconds; tapping it does
// what Alt+Shift+H does. It lives in a closed shadow root, so the site cannot style or reach it.
(() => {
  if (window.top !== window || document.getElementById('quiet-launcher-home')) return;
  const host = document.createElement('div');
  host.id = 'quiet-launcher-home';
  // Inline !important beats any rule the site's style sheets have (AC-5).
  for (const [p, v] of [['all', 'initial'], ['display', 'block'], ['visibility', 'visible'], ['position', 'static']]) {
    host.style.setProperty(p, v, 'important');
  }
  const root = host.attachShadow({ mode: 'closed' });
  root.innerHTML = `<style>
    button { all: initial; position: fixed; left: 24px; bottom: 24px; z-index: 2147483647; box-sizing: border-box;
      width: 64px; height: 64px; border-radius: 32px; border: 2px solid #59646e; background: rgba(16, 20, 24, .85);
      color: #eef2f5; display: flex; align-items: center; justify-content: center; cursor: pointer;
      opacity: 0; pointer-events: none; transition: opacity .2s; touch-action: manipulation; }
    button.shown { opacity: 1; pointer-events: auto; }
    button:focus-visible { outline: 4px solid #fff; outline-offset: 2px; }
    @media (prefers-reduced-motion: reduce) { button { transition: none; } }
  </style>
  <button type="button" aria-label="Back to the launcher"><svg width="30" height="30" viewBox="0 0 24 24" aria-hidden="true">
    <path d="M3 11l9-8 9 8M5 9.5V21h5v-6h4v6h5V9.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg></button>`;
  const button = root.querySelector('button');
  let timer;

  // Over the site's own full-screen player when there is one (AC-4); a video element cannot hold it.
  const place = () => {
    const fs = document.fullscreenElement;
    (fs && fs.tagName !== 'VIDEO' ? fs : document.documentElement).appendChild(host);
  };
  const show = () => {
    place();
    button.classList.add('shown');
    clearTimeout(timer);
    timer = setTimeout(() => button.classList.remove('shown'), 4000);
  };

  // A touch shows it; a mouse or touchpad does not (AC-2).
  window.addEventListener('pointerdown', e => { if (e.pointerType === 'touch') show(); }, { capture: true, passive: true });
  document.addEventListener('fullscreenchange', place);
  // Tapping it does what Alt+Shift+H does (AC-3).
  button.addEventListener('click', e => {
    e.stopPropagation();
    button.classList.remove('shown');
    chrome.runtime.sendMessage({ type: 'home-button' });
  });
  place();
})();
