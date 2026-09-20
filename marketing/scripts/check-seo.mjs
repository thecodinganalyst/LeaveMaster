import assert from 'node:assert/strict';
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const outputDir = new URL('../out/', import.meta.url).pathname;
const expectedRoutes = ['/', '/features', '/demo', '/contact', '/privacy', '/terms'];

function htmlPath(route) {
  if (route === '/') return join(outputDir, 'index.html');
  const nested = join(outputDir, route.slice(1), 'index.html');
  return existsSync(nested) ? nested : join(outputDir, `${route.slice(1)}.html`);
}

function match(html, pattern, label, route) {
  const value = html.match(pattern)?.[1]?.trim();
  assert.ok(value, `${route}: missing ${label}`);
  return value;
}

const seenTitles = new Map();
const seenDescriptions = new Map();

for (const route of expectedRoutes) {
  const file = htmlPath(route);
  assert.ok(existsSync(file), `${route}: generated HTML not found`);
  const html = readFileSync(file, 'utf8');

  const title = match(html, /<title>([^<]+)<\/title>/i, 'title', route);
  const description = match(html, /<meta[^>]+name=["']description["'][^>]+content=["']([^"']+)["']/i, 'meta description', route);
  const canonical = match(html, /<link[^>]+rel=["']canonical["'][^>]+href=["']([^"']+)["']/i, 'canonical URL', route);
  const h1Count = (html.match(/<h1(?:\s|>)/gi) ?? []).length;

  assert.equal(h1Count, 1, `${route}: expected exactly one h1, found ${h1Count}`);
  assert.equal(canonical, `https://leavemaestro.com${route === '/' ? '' : route}`, `${route}: unexpected canonical URL`);
  assert.ok(!seenTitles.has(title), `${route}: duplicate title also used by ${seenTitles.get(title)}`);
  assert.ok(!seenDescriptions.has(description), `${route}: duplicate description also used by ${seenDescriptions.get(description)}`);
  seenTitles.set(title, route);
  seenDescriptions.set(description, route);
}

const robots = readFileSync(join(outputDir, 'robots.txt'), 'utf8');
assert.match(robots, /Sitemap:\s*https:\/\/leavemaestro\.com\/sitemap\.xml/i, 'robots.txt must advertise the canonical sitemap');

const sitemap = readFileSync(join(outputDir, 'sitemap.xml'), 'utf8');
for (const route of expectedRoutes) {
  const url = `https://leavemaestro.com${route === '/' ? '' : route}`;
  assert.ok(sitemap.includes(`<loc>${url}</loc>`), `sitemap.xml missing ${url}`);
}

const home = readFileSync(htmlPath('/'), 'utf8');
const jsonLd = [...home.matchAll(/<script[^>]+type=["']application\/ld\+json["'][^>]*>(.*?)<\/script>/gis)]
  .map((match) => JSON.parse(match[1]));
assert.ok(jsonLd.length >= 2, 'homepage must contain site and software JSON-LD');
const types = jsonLd.flatMap((entry) => Array.isArray(entry) ? entry.map((item) => item['@type']) : [entry['@type']]);
for (const type of ['Organization', 'WebSite', 'SoftwareApplication']) {
  assert.ok(types.includes(type), `homepage structured data missing ${type}`);
}

const htmlFiles = [];
function walk(dir) {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) walk(path);
    else if (name.endsWith('.html')) htmlFiles.push(path);
  }
}
walk(outputDir);

const generatedRoutes = new Set(expectedRoutes);
for (const file of htmlFiles) {
  const html = readFileSync(file, 'utf8');
  for (const href of html.matchAll(/<a[^>]+href=["']([^"'#?]+)[^"']*["']/gi)) {
    const target = href[1];
    if (!target.startsWith('/') || target.startsWith('//')) continue;
    if (target.startsWith('/_next/')) continue;
    const normalized = target.length > 1 ? target.replace(/\/$/, '') : '/';
    assert.ok(generatedRoutes.has(normalized), `${relative(outputDir, file)}: internal link ${target} does not resolve to a known public route`);
  }
}

console.log(`SEO checks passed for ${expectedRoutes.length} public routes and ${htmlFiles.length} generated HTML files.`);
