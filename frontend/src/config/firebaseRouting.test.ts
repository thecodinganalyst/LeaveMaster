import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

type RunRewrite = {
  source: string;
  run: { serviceId: string; region: string };
};

type DestinationRewrite = {
  source: string;
  destination: string;
};

type Rewrite = RunRewrite | DestinationRewrite;

const firebaseConfig = JSON.parse(
  readFileSync(new URL('../../firebase.json', import.meta.url), 'utf8'),
) as { hosting: { rewrites: Rewrite[] } };

const backendSources = [
  '/api/**',
  '/auth/**',
  '/account-activation/**',
  '/oauth2/**',
  '/login/oauth2/**',
  '/logout',
  '/leave-applications',
  '/leave-applications/**',
];

const isRunRewrite = (rewrite: Rewrite): rewrite is RunRewrite => 'run' in rewrite;

describe('Firebase Hosting backend routing contract', () => {
  it.each(backendSources)('routes %s to the production Cloud Run service', (source) => {
    const rewrite = firebaseConfig.hosting.rewrites.find((candidate) => candidate.source === source);

    expect(rewrite).toBeDefined();
    expect(rewrite && isRunRewrite(rewrite) ? rewrite.run : undefined).toEqual({
      serviceId: 'leavemaster-api',
      region: 'asia-southeast1',
    });
  });

  it('keeps all backend rewrites before the SPA fallback', () => {
    const rewrites = firebaseConfig.hosting.rewrites;
    const fallbackIndex = rewrites.findIndex((rewrite) => rewrite.source === '**');

    expect(fallbackIndex).toBe(rewrites.length - 1);
    expect(rewrites[fallbackIndex]).toEqual({ source: '**', destination: '/index.html' });
    for (const source of backendSources) {
      expect(rewrites.findIndex((rewrite) => rewrite.source === source)).toBeLessThan(fallbackIndex);
    }
  });
});
