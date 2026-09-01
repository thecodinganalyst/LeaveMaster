import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

type HostingRewrite = {
  source: string;
  run?: { serviceId?: string; region?: string };
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

const backendRoutes = [
  '/api/**',
  '/auth/**',
  '/account-activation/**',
  '/oauth2/**',
  '/login/oauth2/**',
  '/logout',
  '/leave-applications',
  '/leave-applications/**',
  '/leave-application-options/**',
];

describe('Firebase Hosting configuration', () => {
  const loadRewrites = () => {
    const config = JSON.parse(
      readFileSync(resolve(process.cwd(), 'firebase.json'), 'utf8'),
    ) as FirebaseConfig;
    return config.hosting.rewrites;
  };

  it.each(backendRoutes)('routes backend path %s to Cloud Run before the SPA fallback', (source) => {
    const rewrites = loadRewrites();
    const backendIndex = rewrites.findIndex((rewrite) => rewrite.source === source);
    const spaFallbackIndex = rewrites.findIndex((rewrite) => rewrite.source === '**');

    expect(backendIndex, `${source} must have a Firebase Hosting backend rewrite`).toBeGreaterThan(-1);
    expect(spaFallbackIndex).toBeGreaterThan(-1);
    expect(backendIndex).toBeLessThan(spaFallbackIndex);
    expect(rewrites[backendIndex]?.run).toEqual({
      serviceId: 'leavemaster-api',
      region: 'asia-southeast1',
    });
  });

  it('keeps the SPA fallback last so it cannot intercept backend paths', () => {
    const rewrites = loadRewrites();
    const spaFallbackIndex = rewrites.findIndex((rewrite) => rewrite.source === '**');

    expect(spaFallbackIndex).toBe(rewrites.length - 1);
    expect(rewrites[spaFallbackIndex]).toEqual({ source: '**', destination: '/index.html' });
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
});
