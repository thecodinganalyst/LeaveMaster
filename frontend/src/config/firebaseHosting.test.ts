import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

type HostingRewrite = { source: string; run?: { serviceId?: string } };
type FirebaseConfig = { hosting: { rewrites: HostingRewrite[] } };

const restResources = [
  'tenants',
  'users',
  'roles',
  'staff',
  'locations',
  'leave-types',
  'leave-approvers',
  'leave-calendars',
  'leave-applications',
];

describe('Firebase Hosting configuration', () => {
  it('routes every REST resource to Cloud Run before the SPA fallback', () => {
    const config = JSON.parse(
      readFileSync(new URL('../../firebase.json', import.meta.url), 'utf8'),
    ) as FirebaseConfig;
    const rewrites = config.hosting.rewrites;
    const spaFallbackIndex = rewrites.findIndex((rewrite) => rewrite.source === '**');

    expect(spaFallbackIndex).toBeGreaterThan(-1);

    for (const resource of restResources) {
      for (const source of [`/${resource}`, `/${resource}/**`]) {
        const index = rewrites.findIndex((rewrite) => rewrite.source === source);
        expect(index, `${source} should be routed before the SPA fallback`).toBeGreaterThan(-1);
        expect(index).toBeLessThan(spaFallbackIndex);
        expect(rewrites[index]?.run?.serviceId).toBe('leavemaster-api');
      }
    }
  });
});
