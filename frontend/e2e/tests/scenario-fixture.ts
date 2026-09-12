import { test as base } from '@playwright/test';
import { createPersistedScenario, deletePersistedScenario, type BootstrapScenario } from './scenario-api';

export const test = base.extend<{ scenario: BootstrapScenario }>({
  page: async ({ page }, use) => {
    // Refine sends non-essential usage telemetry from the browser. Chromium can block the
    // cross-origin response with ERR_BLOCKED_BY_ORB in CI, which is correctly reported by
    // installFailureGuards as a failed request even though the LeaveMaestro flow succeeded.
    // Intercept only this known third-party endpoint so application/API failures remain strict.
    await page.route('https://telemetry.refine.dev/**', (route) => route.fulfill({ status: 204, body: '' }));
    await use(page);
  },
  scenario: async ({ request }, provideScenario, testInfo) => {
    const runId = process.env.GITHUB_RUN_ID ?? 'local';
    const scenarioId = `pw-${runId}-${testInfo.workerIndex}-${testInfo.testId}`
      .replace(/[^A-Za-z0-9_-]/g, '-')
      .slice(0, 80);

    const scenario = await createPersistedScenario(request, scenarioId);
    try {
      await provideScenario(scenario);
    } finally {
      await deletePersistedScenario(request, scenario.scenarioId);
    }
  },
});

export { expect } from '@playwright/test';
