import { expect, test } from './scenario-fixture';

test('persisted E2E scenario exposes deterministic user aliases and credentials', async ({ scenario }) => {
  expect(scenario.tenantId).toBe(`E2E-${scenario.scenarioId}`);
  expect(scenario.jurisdictionId).toBe('SG');
  expect(scenario.password).toBe('e2e-password');
  expect(scenario.users.staff001.loginName).toBe('staff001');
  expect(scenario.users.manager01.loginName).toBe('manager01');
  expect(scenario.users.hr.loginName).toBe('hr');
  expect(scenario.users.admin.loginName).toBe('admin');
});
