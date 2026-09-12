import { test as base } from '@playwright/test';
import { createPersistedScenario, deletePersistedScenario, type BootstrapScenario } from './scenario-api';

export const test = base.extend<{ scenario: BootstrapScenario }>({
  scenario: async ({ request }, use, testInfo) => {
    const runId = process.env.GITHUB_RUN_ID ?? 'local';
    const scenarioId = `pw-${runId}-${testInfo.workerIndex}-${testInfo.testId}`
      .replace(/[^A-Za-z0-9_-]/g, '-')
      .slice(0, 80);

    const scenario = await createPersistedScenario(request, scenarioId);
    try {
      await use(scenario);
    } finally {
      await deletePersistedScenario(request, scenario.scenarioId);
    }
  },
});

export { expect } from '@playwright/test';
