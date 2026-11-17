import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const manifest = JSON.parse(readFileSync(new URL('../../chromebook/manifest.json', import.meta.url)));

test('chromebook-launcher#AC-1: a Manifest V3 extension named Quiet Launcher', () => {
  assert.equal(manifest.manifest_version, 3);
  assert.equal(manifest.name, 'Quiet Launcher');
});

test('chromebook-launcher#AC-1: it does not replace the new tab page', () => {
  assert.equal(manifest.chrome_url_overrides, undefined);
});

test('chromebook-launcher#AC-1: go-home is Alt+Shift+H, go-last is Alt+Shift+J', () => {
  const { commands } = manifest;
  assert.ok(commands, 'the manifest declares commands');
  assert.equal(commands['go-home'].suggested_key.default, 'Alt+Shift+H');
  assert.equal(commands['go-home'].suggested_key.chromeos, 'Alt+Shift+H');
  assert.equal(commands['go-home'].description, 'Back to the launcher');
  assert.equal(commands['go-last'].suggested_key.default, 'Alt+Shift+J');
  assert.equal(commands['go-last'].suggested_key.chromeos, 'Alt+Shift+J');
  assert.equal(commands['go-last'].description, 'Back to the last service');
});
