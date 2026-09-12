#!/usr/bin/env node

import { existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';

const args = process.argv.slice(2);
const option = (name, fallback) => {
  const index = args.indexOf(`--${name}`);
  return index >= 0 && args[index + 1] ? args[index + 1] : fallback;
};

const suite = option('suite', 'regression');
const cataloguePath = resolve(option('catalogue', 'docs/testing/scenario-catalogue.json'));
const backendResultsDir = resolve(option('backend-results', `backend/build/test-results/${suite === 'smoke' ? 'smokeTest' : 'regressionTest'}`));
const playwrightResults = resolve(option('playwright-results', 'frontend/e2e/test-results/playwright-results.json'));
const outputDir = resolve(option('output', 'build/scenario-report'));

if (!['smoke', 'regression'].includes(suite)) throw new Error(`Unsupported suite: ${suite}`);
if (!existsSync(cataloguePath)) throw new Error(`Scenario catalogue not found: ${cataloguePath}`);

const catalogue = JSON.parse(readFileSync(cataloguePath, 'utf8'));
const scenarios = catalogue.scenarios ?? [];
const uniqueIds = new Set();
const backendMappings = new Map();
const playwrightMappings = new Map();
for (const scenario of scenarios) {
  if (!/^SCN-\d{3}$/.test(scenario.id)) throw new Error(`Invalid scenario id: ${scenario.id}`);
  if (uniqueIds.has(scenario.id)) throw new Error(`Duplicate scenario id: ${scenario.id}`);
  uniqueIds.add(scenario.id);
  for (const testName of scenario.tests?.backend ?? []) {
    const ids = backendMappings.get(testName) ?? [];
    ids.push(scenario.id);
    backendMappings.set(testName, ids);
  }
  for (const testName of scenario.tests?.playwright ?? []) {
    const ids = playwrightMappings.get(testName) ?? [];
    ids.push(scenario.id);
    playwrightMappings.set(testName, ids);
  }
}

const decodeXml = (value) => value
  .replaceAll('&quot;', '"').replaceAll('&apos;', "'").replaceAll('&lt;', '<')
  .replaceAll('&gt;', '>').replaceAll('&amp;', '&');

const walkFiles = (root, suffix) => {
  if (!existsSync(root)) return [];
  const files = [];
  for (const entry of readdirSync(root)) {
    const path = join(root, entry);
    const stat = statSync(path);
    if (stat.isDirectory()) files.push(...walkFiles(path, suffix));
    else if (entry.endsWith(suffix)) files.push(path);
  }
  return files;
};

const executions = [];
for (const file of walkFiles(backendResultsDir, '.xml')) {
  const xml = readFileSync(file, 'utf8');
  const testcaseRegex = /<testcase\b([^>]*)>([\s\S]*?)<\/testcase>|<testcase\b([^>]*)\/>/g;
  let match;
  while ((match = testcaseRegex.exec(xml)) !== null) {
    const attrs = match[1] ?? match[3] ?? '';
    const body = match[2] ?? '';
    const attr = (name) => {
      const found = attrs.match(new RegExp(`${name}="([^"]*)"`));
      return found ? decodeXml(found[1]) : '';
    };
    const testName = attr('name');
    const methodName = testName.replace(/\(.*$/, '');
    const scenarioIds = backendMappings.get(methodName) ?? [];
    const failed = /<(failure|error)\b/.test(body);
    const skipped = /<skipped\b/.test(body);
    for (const scenarioId of scenarioIds) {
      executions.push({
        scenarioId,
        layer: 'Backend',
        test: methodName,
        result: failed ? 'FAIL' : skipped ? 'SKIP' : 'PASS',
        durationMs: Math.round(Number(attr('time') || 0) * 1000),
      });
    }
  }
}

const collectPlaywrightSpecs = (node, output = []) => {
  for (const spec of node.specs ?? []) output.push(spec);
  for (const child of node.suites ?? []) collectPlaywrightSpecs(child, output);
  return output;
};

if (existsSync(playwrightResults)) {
  const report = JSON.parse(readFileSync(playwrightResults, 'utf8'));
  for (const spec of collectPlaywrightSpecs(report)) {
    const normalizedTitle = spec.title.replace(/\s+@smoke\b/g, '').replace(/^SCN-\d{3}\s+/, '').trim();
    const scenarioIds = [...new Set([
      ...(spec.title.match(/SCN-\d{3}/g) ?? []),
      ...(playwrightMappings.get(normalizedTitle) ?? []),
    ])];
    if (!scenarioIds.length) continue;
    for (const test of spec.tests ?? []) {
      const results = test.results ?? [];
      const failed = results.some((result) => ['failed', 'timedOut', 'interrupted'].includes(result.status));
      const passed = results.some((result) => result.status === 'passed');
      const durationMs = results.reduce((total, result) => total + Number(result.duration ?? 0), 0);
      for (const scenarioId of scenarioIds) {
        executions.push({
          scenarioId,
          layer: 'Playwright',
          test: spec.title,
          result: failed ? 'FAIL' : passed ? 'PASS' : 'SKIP',
          durationMs,
        });
      }
    }
  }
}

const rows = scenarios.map((scenario) => {
  const relevant = executions.filter((execution) => execution.scenarioId === scenario.id);
  const layers = [...new Set(relevant.map((execution) => execution.layer))];
  const failed = relevant.some((execution) => execution.result === 'FAIL');
  const passed = relevant.some((execution) => execution.result === 'PASS');
  return {
    ...scenario,
    layers,
    result: failed ? 'FAIL' : passed ? 'PASS' : relevant.length ? 'SKIP' : 'NOT_EXECUTED',
    durationMs: relevant.reduce((total, execution) => total + execution.durationMs, 0),
    executions: relevant,
  };
});

const selected = rows.filter((row) => row.suites.includes(suite));
const required = selected.filter((row) => row.required?.[suite]);
const failed = selected.filter((row) => row.result === 'FAIL');
const notExecuted = selected.filter((row) => row.result === 'NOT_EXECUTED');
const unmapped = selected.filter((row) => row.automationStatus !== 'automated' || !((row.tests?.backend?.length ?? 0) + (row.tests?.playwright?.length ?? 0)));
const missingRequired = required.filter((row) => row.result === 'NOT_EXECUTED');

const areaSummary = Object.values(selected.reduce((acc, row) => {
  const item = acc[row.area] ?? { area: row.area, defined: 0, passed: 0 };
  item.defined += 1;
  if (row.result === 'PASS') item.passed += 1;
  acc[row.area] = item;
  return acc;
}, {}));

const metrics = {
  defined: selected.length,
  executed: selected.filter((row) => row.result !== 'NOT_EXECUTED').length,
  passed: selected.filter((row) => row.result === 'PASS').length,
  failed: failed.length,
  notExecuted: notExecuted.length,
  unmapped: unmapped.length,
};
const generatedAt = new Date().toISOString();
const reportJson = { version: 1, suite, generatedAt, metrics, areas: areaSummary, scenarios: selected };
const esc = (value) => String(value).replaceAll('|', '\\|').replaceAll('\n', ' ');
const markdown = [
  `# LeaveMaestro ${suite === 'smoke' ? 'PR Smoke' : 'Full Regression'} Scenario Coverage`, '',
  `Generated: ${generatedAt}`, '',
  `**${metrics.passed} / ${metrics.defined} business scenarios passed**`, '',
  `- Scenarios defined: ${metrics.defined}`,
  `- Scenarios executed: ${metrics.executed}`,
  `- Passed: ${metrics.passed}`,
  `- Failed: ${metrics.failed}`,
  `- Not executed: ${metrics.notExecuted}`,
  `- Unmapped: ${metrics.unmapped}`, '',
  '## Scenario results', '',
  '| Scenario | Description | Area | Layer | Result | Duration |',
  '|---|---|---|---|---|---:|',
  ...selected.map((row) => `| ${row.id} | ${esc(row.name)} | ${esc(row.area)} | ${row.layers.join(' + ') || '—'} | ${row.result} | ${(row.durationMs / 1000).toFixed(2)}s |`), '',
  '## Functional coverage', '',
  ...areaSummary.map((area) => `- ${area.area}: ${area.passed} / ${area.defined}`), '',
  '## Failed scenarios', '',
  ...(failed.length ? failed.map((row) => `- **${row.id} ${row.name}** — ${row.executions.filter((item) => item.result === 'FAIL').map((item) => `${item.layer}: ${item.test}`).join('; ')}`) : ['None.']), '',
  '## Missing or not executed', '',
  ...(notExecuted.length ? notExecuted.map((row) => `- **${row.id} ${row.name}**`) : ['None.']), '',
  'Detailed artifacts: `scenario-report.json`, `scenario-report.html`, Playwright HTML report, and backend test reports.', '',
].join('\n');

const escapeHtml = (value) => String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
const htmlRows = selected.map((row) => `<tr><td>${row.id}</td><td>${escapeHtml(row.name)}</td><td>${escapeHtml(row.area)}</td><td>${escapeHtml(row.layers.join(' + ') || '—')}</td><td>${row.result}</td><td>${(row.durationMs / 1000).toFixed(2)}s</td></tr>`).join('');
const html = `<!doctype html><html><head><meta charset="utf-8"><title>LeaveMaestro ${suite} scenario coverage</title><style>body{font-family:system-ui,sans-serif;max-width:1100px;margin:2rem auto;padding:0 1rem}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ddd;padding:.5rem;text-align:left}th{background:#f5f5f5}.metrics{display:flex;gap:1rem;flex-wrap:wrap}.metric{border:1px solid #ddd;border-radius:.5rem;padding:.75rem 1rem}</style></head><body><h1>LeaveMaestro ${suite === 'smoke' ? 'PR Smoke' : 'Full Regression'} Scenario Coverage</h1><p>Generated ${generatedAt}</p><div class="metrics"><div class="metric">Defined <strong>${metrics.defined}</strong></div><div class="metric">Executed <strong>${metrics.executed}</strong></div><div class="metric">Passed <strong>${metrics.passed}</strong></div><div class="metric">Failed <strong>${metrics.failed}</strong></div><div class="metric">Not executed <strong>${metrics.notExecuted}</strong></div></div><h2>Scenario results</h2><table><thead><tr><th>Scenario</th><th>Description</th><th>Area</th><th>Layer</th><th>Result</th><th>Duration</th></tr></thead><tbody>${htmlRows}</tbody></table><h2>Functional coverage</h2><ul>${areaSummary.map((area) => `<li>${escapeHtml(area.area)}: ${area.passed} / ${area.defined}</li>`).join('')}</ul></body></html>`;

mkdirSync(outputDir, { recursive: true });
writeFileSync(join(outputDir, 'scenario-report.json'), JSON.stringify(reportJson, null, 2));
writeFileSync(join(outputDir, 'scenario-report.md'), markdown);
writeFileSync(join(outputDir, 'scenario-report.html'), html);
console.log(markdown);

if (unmapped.length) console.error(`Unmapped catalogue scenarios: ${unmapped.map((row) => row.id).join(', ')}`);
if (missingRequired.length) {
  console.error(`Required ${suite} scenarios not executed: ${missingRequired.map((row) => row.id).join(', ')}`);
  process.exitCode = 2;
}
