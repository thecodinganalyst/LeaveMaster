import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

type HostingRewrite = {
  source: string;
  run?: { serviceId?: string };
  destination?: string;
};
type FirebaseConfig = { hosting: { rewrites: HostingRewrite[] } };

const frontendRoutes = [
  '/login',
  '/tenants',
  '/users',
  '/roles',
  '/staff',
  '/locations',
  '/leave-types',
  '/leave-approvers',
  '/leave-calendars',
];

describe('Firebase Hosting configuration', () => {
  const loadRewrites = () => {
    const config = JSON.parse(
      readFileSync(resolve(process.cwd(), 'firebase.json'), 'utf8'),
    ) as FirebaseConfig;
    return config.hosting.rewrites;
  };

  it('routes the API namespace to Cloud Run before the SPA fallback', () => {
    const rewrites = loadRewrites();
    const apiIndex = rewrites.findIndex((rewrite) => rewrite.source === '/api/**');
    const spaFallbackIndex = rewrites.findIndex((rewrite) => rewrite.source === '**');

    expect(apiIndex).toBeGreaterThan(-1);
    expect(spaFallbackIndex).toBeGreaterThan(-1);
    expect(apiIndex).toBeLessThan(spaFallbackIndex);
    expect(rewrites[apiIndex]?.run?.serviceId).toBe('leavemaster-api');
    expect(rewrites[spaFallbackIndex]?.destination).toBe('/index.html');
  });

  it('does not send frontend routes such as /tenants to Cloud Run', () => {
    const rewrites = loadRewrites();

    for (const route of frontendRoutes) {
      const directRewrite = rewrites.find((rewrite) => rewrite.source === route);
      const nestedRewrite = rewrites.find((rewrite) => rewrite.source === `${route}/**`);

      expect(directRewrite?.run, `${route} must be served by the SPA`).toBeUndefined();
      expect(nestedRewrite?.run, `${route}/** must be served by the SPA`).toBeUndefined();
    }
  });

  it('keeps backend-only auth and OAuth routes ahead of the SPA fallback', () => {
    const rewrites = loadRewrites();
    const spaFallbackIndex = rewrites.findIndex((rewrite) => rewrite.source === '**');

    for (const source of ['/auth/**', '/oauth2/**', '/login/oauth2/**', '/logout']) {
      const index = rewrites.findIndex((rewrite) => rewrite.source === source);
      expect(index, `${source} should be routed before the SPA fallback`).toBeGreaterThan(-1);
      expect(index).toBeLessThan(spaFallbackIndex);
      expect(rewrites[index]?.run?.serviceId).toBe('leavemaster-api');
    }
  });
});
