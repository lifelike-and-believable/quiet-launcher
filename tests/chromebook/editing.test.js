import { test } from 'node:test';
import assert from 'node:assert/strict';
import { siteOf, permissionPattern, serviceFromAddress, addService, moveService, setHidden, removeService }
  from '../../chromebook/lib/editing.js';

const svc = (id, hidden = false) => ({ id, name: id.toUpperCase(), url: `https://${id}.example/`, site: `${id}.example`, colour: '#000', hidden });
const ids = list => list.map(s => s.id);

test('chromebook-editing#AC-3: moving a service puts it at the new place and keeps the rest in order', () => {
  const list = [svc('a'), svc('b'), svc('c'), svc('d')];
  assert.deepEqual(ids(moveService(list, 'a', 2)), ['b', 'c', 'a', 'd']);
  assert.deepEqual(ids(moveService(list, 'd', 0)), ['d', 'a', 'b', 'c']);
  assert.deepEqual(ids(moveService(list, 'b', 9)), ['a', 'c', 'd', 'b'], 'past the end goes last');
  assert.deepEqual(ids(list), ['a', 'b', 'c', 'd'], 'the original list is unchanged');
});

test('chromebook-editing#AC-4: hiding and showing keep the service in its place in the order', () => {
  const list = [svc('a'), svc('b'), svc('c')];
  const hidden = setHidden(list, 'b', true);
  assert.deepEqual(ids(hidden), ['a', 'b', 'c']);
  assert.equal(hidden[1].hidden, true);
  assert.equal(setHidden(hidden, 'b', false)[1].hidden, false);
});

test('chromebook-editing#AC-5: removing takes the service out of the list', () => {
  assert.deepEqual(ids(removeService([svc('a'), svc('b'), svc('c')], 'b')), ['a', 'c']);
});

test('chromebook-editing#AC-7: an address becomes a service named after its site', () => {
  assert.deepEqual(serviceFromAddress('hollywoodsuite.ca'),
    { name: 'hollywoodsuite.ca', url: 'https://hollywoodsuite.ca/', site: 'hollywoodsuite.ca', colour: '#2b343c' });
  assert.deepEqual(serviceFromAddress('  https://www.Example.com/watch?x=1 '),
    { name: 'example.com', url: 'https://www.example.com/watch?x=1', site: 'example.com', colour: '#2b343c' });
  assert.equal(serviceFromAddress('http://localhost:8080/tv').url, 'http://localhost:8080/tv');
  assert.equal(serviceFromAddress('http://localhost:8080/tv').site, 'localhost');
});

test('chromebook-editing#AC-7: text that is not a site address gives no service', () => {
  for (const text of ['', '   ', 'not a site', 'ftp://example.com/', 'https://', 'javascript:alert(1)']) {
    assert.equal(serviceFromAddress(text), null, text);
  }
});

test('chromebook-editing#AC-7: the site and the permission asked for', () => {
  assert.equal(siteOf('https://www.primevideo.com/'), 'primevideo.com');
  assert.equal(siteOf('https://gem.cbc.ca/live'), 'gem.cbc.ca');
  assert.equal(permissionPattern('primevideo.com'), '*://*.primevideo.com/*');
});

test('chromebook-editing#AC-7: an added service goes at the end, visible, with an id of its own', () => {
  const list = [svc('a')];
  const { list: after, service } = addService(list, { name: 'Crave', url: 'https://www.crave.ca/', colour: '#0f4c81' });
  assert.deepEqual(ids(after).slice(0, 1), ['a']);
  assert.equal(after.length, 2);
  assert.equal(after[1], service);
  assert.equal(service.name, 'Crave');
  assert.equal(service.site, 'crave.ca');
  assert.equal(service.hidden, false);
  assert.ok(service.id && service.id !== 'a');
  const again = addService(after, { name: 'Crave', url: 'https://www.crave.ca/', colour: '#0f4c81' });
  assert.notEqual(again.service.id, service.id, 'each added service has its own id');
});
