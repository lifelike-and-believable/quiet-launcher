// A small in-memory stand-in for the parts of the chrome.* API the controller uses, so its rules
// can be tested in Node without a browser. Windows are { id, type, state, focused, tabs: [{ url }] }.
export function fakeChrome({ windows = [], createdState = 'fullscreen' } = {}) {
  let nextId = 100;
  const session = {};
  const calls = [];
  const state = { windows: windows.map(w => ({ focused: false, state: 'normal', tabs: [], ...w })) };
  const find = id => state.windows.find(w => w.id === id);
  const chrome = {
    calls,
    state,
    runtime: { getURL: path => `chrome-extension://quiet/${path}` },
    windows: {
      WINDOW_ID_NONE: -1,
      async getAll({ populate } = {}) {
        return state.windows.map(w => ({ ...w, tabs: populate ? w.tabs.map(t => ({ ...t })) : undefined }));
      },
      async get(id) {
        const w = find(id);
        if (!w) throw new Error(`No window with id: ${id}.`);
        return { ...w };
      },
      async create(opts = {}) {
        calls.push(['create', opts]);
        const w = { id: nextId++, type: opts.type || 'normal', state: opts.state ? createdState : 'normal',
          focused: !!opts.focused, tabs: [{ url: opts.url || 'chrome://newtab/' }] };
        if (w.focused) state.windows.forEach(x => { x.focused = false; });
        state.windows.push(w);
        return { ...w };
      },
      async update(id, opts) {
        calls.push(['update', id, opts]);
        const w = find(id);
        if (!w) throw new Error(`No window with id: ${id}.`);
        if (opts.state) w.state = opts.state;
        if (opts.focused) { state.windows.forEach(x => { x.focused = false; }); w.focused = true; }
        return { ...w };
      },
    },
    storage: {
      session: {
        async get(key) { return key in session ? { [key]: session[key] } : {}; },
        async set(obj) { Object.assign(session, obj); },
      },
    },
  };
  return chrome;
}
