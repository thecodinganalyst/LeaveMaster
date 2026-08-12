import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

type Rewrite = {
  source: string;
  destination?: string;
  run?: { serviceId?: string };
};

describe('cold-start direct navigation routing', () => {
  it('serves /tenants from the SPA while the readiness endpoint still targets Cloud Run', () => {
    const config = JSON.parse(
      readFileSync(resolve(process.cwd(), 'firebase.json'), 'utf8'),
    ) as { hosting: { rewrites: Rewrite[] } };
    const rewrites = config.hosting.rewrites;

    expect(rewrites.some((rewrite) => rewrite.source === '/tenants' && rewrite.run)).toBe(false);
    expect(rewrites.some((rewrite) => rewrite.source === '/tenants/**' && rewrite.run)).toBe(false);

    const readinessRewrite = rewrites.find((rewrite) => rewrite.source === '/auth/**');
    expect(readinessRewrite?.run?.serviceId).toBe('leavemaster-api');

    const spaFallback = rewrites.find((rewrite) => rewrite.source === '**');
    expect(spaFallback?.destination).toBe('/index.html');
  });
});
