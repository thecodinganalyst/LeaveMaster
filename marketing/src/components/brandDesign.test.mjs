import assert from 'node:assert/strict';
import { readFile, readdir } from 'node:fs/promises';
import { join } from 'node:path';
import test from 'node:test';

const read = (relativePath) => readFile(new URL(relativePath, import.meta.url), 'utf8');

async function readMarketingSourceFiles() {
  const roots = [new URL('../app/', import.meta.url), new URL('./', import.meta.url)];
  const contents = [];

  for (const root of roots) {
    const entries = await readdir(root, { recursive: true, withFileTypes: true });
    for (const entry of entries) {
      if (!entry.isFile() || !/\.(?:tsx|css)$/.test(entry.name)) continue;
      contents.push(await readFile(join(entry.parentPath, entry.name), 'utf8'));
    }
  }

  return contents.join('\n');
}

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

test('marketing surfaces stay light and the homepage starts on white', async () => {
  const source = await readMarketingSourceFiles();
  const home = await read('../app/page.tsx');
  const globals = await read('../app/globals.css');

  for (const darkSurface of ['bg-slate-950', 'bg-slate-900', 'bg-brand-950', 'bg-brand-900', 'bg-black']) {
    assert.equal(source.includes(darkSurface), false, `${darkSurface} must not be used as a marketing surface`);
  }

  assert.match(home, /<section className="bg-white">/);
  assert.match(globals, /body\s*\{[\s\S]*@apply bg-white text-slate-900 antialiased;/);
  assert.equal(globals.includes('#092b28'), false, 'global marketing background must not use near-black teal');
});

test('marketing pages use reusable product visuals', async () => {
  const home = await read('../app/page.tsx');
  const features = await read('../app/features/page.tsx');
  const demo = await read('../app/demo/page.tsx');
  const visuals = await read('./ProductVisuals.tsx');

  assert.match(home, /<HeroProductVisual\s*\/>/);
  assert.match(home, /<LeaveWorkflowVisual\s*\/>/);
  assert.match(features, /<LeaveWorkflowVisual\s*\/>/);
  assert.match(demo, /<PersonaVisual persona="employee"\s*\/>/);
  assert.match(demo, /<PersonaVisual persona="manager"\s*\/>/);
  assert.match(demo, /<PersonaVisual persona="hr"\s*\/>/);
  assert.match(visuals, /Representative LeaveMaestro interface/);
  assert.match(visuals, /Policy.*Eligibility.*Entitlement.*Request.*Approval.*Balance/s);
});

test('navigation uses the reusable Conductor LM brand mark', async () => {
  const navigation = await read('./Navigation.tsx');
  const logo = await read('./LeaveMaestroLogo.tsx');

  assert.match(navigation, /<LeaveMaestroLogo\s*\/>/);
  assert.match(logo, /LeaveMaestro/);
  assert.match(logo, /<circle cx="31" cy="19"/);
  assert.match(logo, /M42 27l11-15/);
});
