// An in-memory stand-in for the chrome.* API, for the services slice
// (docs/specs/chromebook-services.md). Windows are { id, type, state, focused, tabs: [{ id, url }] }.
// Every call that changes something is logged in `calls`, in order.
export function fakeBrowser({ windows = [], services = [], granted = [] } = {}) {
  let nextWindow = 100;
  let nextTab = 1000;
  const local = { services };
  const session = {};
  const calls = [];
  const state = {
    windows: windows.map(w => ({ focused: false, state: 'normal', ...w,
      tabs: (w.tabs || []).map(t => ({ id: nextTab++, ...t })) })),
    playing: new Set(), // tab ids with a playing video
  };
  const find = id => state.windows.find(w => w.id === id);
  const copy = w => ({ ...w, tabs: w.tabs.map(t => ({ ...t })) });
  const host = url => new URL(url).hostname;
  const focus = w => { state.windows.forEach(x => { x.focused = false; }); w.focused = true; };
  const store = obj => ({
    async get(key) {
      if (key == null) return { ...obj };
      const keys = Array.isArray(key) ? key : [key];
      return Object.fromEntries(keys.filter(k => k in obj).map(k => [k, obj[k]]));
    },
    async set(values) { Object.assign(obj, values); },
  });
  return {
    calls,
    state,
    runtime: { getURL: path => `chrome-extension://quiet/${path}` },
    windows: {
      WINDOW_ID_NONE: -1,
      async getAll({ populate } = {}) { return state.windows.map(w => (populate ? copy(w) : { ...w, tabs: undefined })); },
      async get(id, { populate } = {}) {
        const w = find(id);
        if (!w) throw new Error(`No window with id: ${id}.`);
        return populate ? copy(w) : { ...w, tabs: undefined };
      },
      async getLastFocused({ populate } = {}) {
        const w = state.windows.find(x => x.focused) || state.windows[state.windows.length - 1];
        return populate ? copy(w) : { ...w, tabs: undefined };
      },
      async create(opts = {}) {
        calls.push(['create', opts]);
        const w = { id: nextWindow++, type: opts.type || 'normal', state: opts.state || 'normal', focused: false,
          tabs: [{ id: nextTab++, url: opts.url || 'chrome://newtab/' }] };
        state.windows.push(w);
        if (opts.focused) focus(w);
        return copy(w);
      },
      async update(id, opts) {
        calls.push(['update', id, opts]);
        const w = find(id);
        if (!w) throw new Error(`No window with id: ${id}.`);
        if (opts.state) w.state = opts.state;
        if (opts.focused) focus(w);
        return copy(w);
      },
    },
    tabs: {
      async update(id, opts) { calls.push(['tabs.update', id, opts]); return { id }; },
      async reload(id) { calls.push(['tabs.reload', id]); },
    },
    scripting: {
      async executeScript({ target, func }) {
        const tab = state.windows.flatMap(w => w.tabs).find(t => t.id === target.tabId);
        if (!tab) throw new Error(`No tab with id: ${target.tabId}`);
        if (!granted.includes(host(tab.url))) throw new Error('Cannot access contents of the page.');
        calls.push(['executeScript', target, func]);
        const paused = state.playing.has(tab.id) ? 1 : 0;
        state.playing.delete(tab.id);
        return [{ frameId: 0, result: paused }];
      },
    },
    permissions: {
      async contains({ origins = [] }) { return origins.every(o => granted.includes(host(o.replace('/*', '/')))); },
    },
    storage: { local: store(local), session: store(session) },
  };
}
