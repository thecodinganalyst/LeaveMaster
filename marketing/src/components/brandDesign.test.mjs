import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = (relativePath) => readFile(new URL(relativePath, import.meta.url), 'utf8');

test('marketing palette contains the teal brand and no legacy navy values', async () => {
  const tailwind = await read('../../tailwind.config.ts');
  const globals = await read('../app/globals.css');
  const source = `${tailwind}\n${globals}`.toLowerCase();

  for (const legacyNavy of ['#0f2740', '#112f4d', '#1c4b73', '#334e68']) {
    assert.equal(source.includes(legacyNavy), false, `legacy navy ${legacyNavy} must not be used`);
  }

  assert.match(source, /#2d9c8f/);
  assert.match(source, /#23877c/);
  assert.match(source, /#103f3b/);
});

test('navigation uses the reusable Conductor LM brand mark', async () => {
  const navigation = await read('./Navigation.tsx');
  const logo = await read('./LeaveMaestroLogo.tsx');

  assert.match(navigation, /<LeaveMaestroLogo\s*\/>/);
  assert.match(logo, /LeaveMaestro/);
  assert.match(logo, /<circle cx="31" cy="19"/);
  assert.match(logo, /M42 27l11-15/);
});
