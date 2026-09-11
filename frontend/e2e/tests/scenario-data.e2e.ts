import { expect, test } from '@playwright/test';
import { createStandardSingaporeScenario, personForRole } from './scenario-data';

test('standard scenario provides representative isolated staff data', async () => {
  const first = createStandardSingaporeScenario('worker-1');
  const second = createStandardSingaporeScenario('worker-2');

  expect(first.tenantId).toBe('E2E-worker-1');
  expect(Object.keys(first.people)).toHaveLength(9);
  expect(first.people.staff001.approverAlias).toBe('manager01');
  expect(first.people.staff002.joinDate).toBe('2026-07-01');
  expect(first.people.staff005.approverAlias).toBeUndefined();
  expect(personForRole(first, 'manager').alias).toBe('manager01');
  expect(first.people.staff001.staffId).not.toBe(second.people.staff001.staffId);
});
