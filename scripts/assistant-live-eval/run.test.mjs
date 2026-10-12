import test from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';

const runner = 'scripts/assistant-live-eval/run.mjs';
function run(env = {}) {
  return spawnSync(process.execPath, [runner], {
    encoding: 'utf8',
    env: { ...process.env, LIVE_EVAL_BASE_URL: '', LIVE_EVAL_TENANT_ID: '', LIVE_EVAL_PASSWORD: '', LIVE_EVAL_RESET_TOKEN: '', ...env }
  });
}
test('missing configuration fails before network requests', () => {
  const result = run();
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Dedicated HTTPS evaluation configuration is required/);
});
for (const tenant of ['DEMO', 'PLATFORM']) {
  test('public or platform tenant is rejected: ' + tenant, () => {
    const result = run({ LIVE_EVAL_BASE_URL: 'https://example.com', LIVE_EVAL_TENANT_ID: tenant, LIVE_EVAL_PASSWORD: 'not-real', LIVE_EVAL_RESET_TOKEN: 'not-real' });
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /Dedicated HTTPS evaluation configuration is required/);
  });
}
test('insecure evaluation endpoint is rejected', () => {
  const result = run({ LIVE_EVAL_BASE_URL: 'http://example.com', LIVE_EVAL_TENANT_ID: 'EVALUATION', LIVE_EVAL_PASSWORD: 'not-real', LIVE_EVAL_RESET_TOKEN: 'not-real' });
  assert.notEqual(result.status, 0);
});
