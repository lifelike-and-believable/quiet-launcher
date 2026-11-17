import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readBannersFile, matchBanners, importStatus } from '../../chromebook/lib/tv-banners.js';

const pic = s => `data:image/png;base64,${s}`;
const app = (name, pkg, picture = pic(name.replace(/\W/g, ''))) => ({ name, package: pkg, picture });
const service = (id, name, site) => ({ id, name, url: `https://www.${site}/`, site, colour: '#000', hidden: false });

test('tv-banners#AC-9: a file from quiet-banners is read', () => {
  const apps = [app('YouTube', 'com.google.android.youtube.tv'), app('Netflix', 'com.netflix.ninja')];
  assert.deepEqual(readBannersFile(JSON.stringify({ from: 'Quiet Launcher TV', apps })), apps);
});

test('tv-banners#AC-9: anything else is not a TV banners file', () => {
  for (const text of ['not json', '[]', '{}', '{"apps": "x"}', '{"apps": []}',
    JSON.stringify({ apps: [{ name: 'A', package: 'a', picture: 'https://example.com/a.png' }] }),
    JSON.stringify({ apps: [{ name: 'A', package: 'a' }] })]) {
    assert.equal(readBannersFile(text), null, text);
  }
});

test('tv-banners#AC-9: entries without a PNG picture are left out of a file that has some', () => {
  const good = app('Plex', 'tv.plex');
  const text = JSON.stringify({ apps: [good, { name: 'Odd', package: 'odd', picture: 'data:text/html,hi' }, null] });
  assert.deepEqual(readBannersFile(text), [good]);
});

test('tv-banners#AC-7: services are matched by name, ignoring case, spaces, punctuation, "The", and "+"', () => {
  const apps = [app('Apple TV', 'com.apple.atve.androidtv.appletv'), app('The Criterion Channel', 'com.criterionchannel'),
    app('CBC Gem', 'ca.cbc.android.cbctv'), app('prime video', 'com.amazon.amazonvideo.livingroom')];
  const services = [service('appletv', 'Apple TV+', 'tv.apple.com'), service('criterion', 'Criterion Channel', 'criterionchannel.com'),
    service('gem', 'CBC Gem', 'gem.cbc.ca'), service('prime', 'Prime Video', 'primevideo.com')];
  const { pictures, unmatched } = matchBanners(services, apps);
  assert.deepEqual(pictures, { appletv: pic('AppleTV'), criterion: pic('TheCriterionChannel'), gem: pic('CBCGem'), prime: pic('primevideo') });
  assert.deepEqual(unmatched, []);
});

test('tv-banners#AC-7: failing the name, by the site name being one part of exactly one package', () => {
  const apps = [app('YouTube for TV', 'com.google.android.youtube.tv'), app('Tubi - Free Movies', 'com.tubitv'),
    app('Plex A', 'tv.plex.a'), app('Plex B', 'tv.plex.b')];
  const services = [service('yt', 'YouTube', 'youtube.com'), service('tubi', 'Tubi', 'tubitv.com'), service('plex', 'Plex', 'plex.tv')];
  const { pictures, unmatched } = matchBanners(services, apps);
  assert.deepEqual(pictures, { yt: pic('YouTubeforTV'), tubi: pic('TubiFreeMovies') });
  assert.deepEqual(unmatched, ['Plex'], 'two packages have "plex": no guess');
});

test('tv-banners#AC-7: hidden services are matched too, and unmatched ones are named in order', () => {
  const services = [service('a', 'Alpha', 'alpha.com'), { ...service('h', 'Hidden one', 'hidden.com'), hidden: true }, service('z', 'Zeta', 'zeta.com')];
  const { pictures, unmatched } = matchBanners(services, [app('HIDDEN ONE', 'com.x.hiddenapp')]);
  assert.deepEqual(Object.keys(pictures), ['h']);
  assert.deepEqual(unmatched, ['Alpha', 'Zeta']);
});

test('tv-banners#AC-8: the status line says how many were set and which had no banner', () => {
  assert.equal(importStatus(4, 4, []), 'Pictures set for 4 of 4 services.');
  assert.equal(importStatus(3, 4, ['Gamma']), 'Pictures set for 3 of 4 services. No TV banner for Gamma.');
  assert.equal(importStatus(1, 3, ['Beta', 'Gamma']), 'Pictures set for 1 of 3 services. No TV banner for Beta, Gamma.');
});
