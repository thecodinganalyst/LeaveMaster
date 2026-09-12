import { expect, type Page } from '@playwright/test';

import { createPersistedScenario, deletePersistedScenario } from './scenario-api';
import { test } from './scenario-fixture';
import { installFailureGuards } from './support';

const login = async (
  page: Page,
  tenantId: string,
  loginName: string,
  password: string,
) => {
  await page.context().clearCookies();
  await page.goto('/login');
  await page.getByLabel('Tenant ID').fill(tenantId);
  await page.getByLabel('Login name').fill(loginName);
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/$/);
};

const applyAnnualLeave = async (page: Page, date: string) => {
  await page.goto('/leave-requests/apply');
  await expect(page.getByRole('heading', { name: 'Apply for leave' })).toBeVisible();
  await page.getByLabel('Leave type').click();
  await page.getByText('Annual Leave', { exact: true }).click();
  await page.getByLabel('From date').fill(date);
  await page.getByLabel('To date').fill(date);
  await page.getByRole('button', { name: 'Submit request' }).click();
  await expect(page).toHaveURL(/\/leave-requests$/);
};

const reviewRequest = async (page: Page, staffName: string, decision: 'Approve' | 'Reject') => {
  await page.goto('/approvals');
  await expect(page.getByRole('heading', { name: 'Approval inbox' })).toBeVisible();
  const row = page.getByRole('row').filter({ hasText: staffName }).first();
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: 'Review' }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.getByRole('dialog').getByRole('button', { name: decision }).click();
  await expect(page.getByText(decision === 'Approve' ? 'Request approved.' : 'Request rejected.')).toBeVisible();
};

test('staff apply -> assigned manager approve -> staff sees approved request and reduced balance', async ({ page, scenario }) => {
  const assertHealthy = installFailureGuards(page);
  const staff = scenario.users.staff001;
  const manager = scenario.users.manager01;

  await login(page, scenario.tenantId, staff.loginName, scenario.password);
  await applyAnnualLeave(page, '2026-09-14');
  await expect(page.getByText('Pending', { exact: true }).first()).toBeVisible();

  await login(page, scenario.tenantId, manager.loginName, scenario.password);
  await reviewRequest(page, 'E2E Normal Staff', 'Approve');

  await login(page, scenario.tenantId, staff.loginName, scenario.password);
  await page.goto('/leave-requests');
  await expect(page.getByText('Approved', { exact: true }).first()).toBeVisible();
  await page.goto('/');
  await expect(page.locator('body')).toContainText(/13(?:\.0+)?/);
  await assertHealthy();
});

test('staff apply -> assigned manager reject -> staff sees rejected request without consuming balance', async ({ page, scenario }) => {
  const assertHealthy = installFailureGuards(page);
  const staff = scenario.users.staff001;
  const manager = scenario.users.manager01;

  await login(page, scenario.tenantId, staff.loginName, scenario.password);
  await applyAnnualLeave(page, '2026-09-15');

  await login(page, scenario.tenantId, manager.loginName, scenario.password);
  await reviewRequest(page, 'E2E Normal Staff', 'Reject');

  await login(page, scenario.tenantId, staff.loginName, scenario.password);
  await page.goto('/leave-requests');
  await expect(page.getByText('Rejected', { exact: true }).first()).toBeVisible();
  await page.goto('/');
  await expect(page.locator('body')).toContainText(/14(?:\.0+)?/);
  await assertHealthy();
});

test('unrelated manager cannot see or act on another manager\'s pending request', async ({ page, scenario }) => {
  const assertHealthy = installFailureGuards(page);
  const staff = scenario.users.staff001;
  const unrelatedManager = scenario.users.manager02;

  await login(page, scenario.tenantId, staff.loginName, scenario.password);
  await applyAnnualLeave(page, '2026-09-16');

  await login(page, scenario.tenantId, unrelatedManager.loginName, scenario.password);
  await page.goto('/approvals');
  await expect(page.getByRole('heading', { name: 'Approval inbox' })).toBeVisible();
  await expect(page.getByRole('row').filter({ hasText: 'E2E Normal Staff' })).toHaveCount(0);
  await assertHealthy();
});

test('browser staff list remains isolated between persisted tenants', async ({ page, request, scenario }, testInfo) => {
  const secondScenarioId = `tenant-b-${testInfo.workerIndex}-${testInfo.testId}`
    .replace(/[^A-Za-z0-9_-]/g, '-')
    .slice(0, 70);
  const second = await createPersistedScenario(request, secondScenarioId);
  const assertHealthy = installFailureGuards(page);

  try {
    await login(page, scenario.tenantId, scenario.users.hr.loginName, scenario.password);
    await page.goto('/staff');
    await expect(page.getByText('E2E Normal Staff', { exact: true })).toBeVisible();
    await expect(page.locator('body')).not.toContainText(second.tenantId);
    await expect(page.locator('body')).not.toContainText(second.users.staff001.staffId);
    await assertHealthy();
  } finally {
    await deletePersistedScenario(request, second.scenarioId);
  }
});

test('role matrix exposes staff self-service, manager approvals, HR staff management and tenant admin management', async ({ page, scenario }) => {
  const assertHealthy = installFailureGuards(page);

  await login(page, scenario.tenantId, scenario.users.staff001.loginName, scenario.password);
  await page.goto('/leave-requests/apply');
  await expect(page.getByRole('heading', { name: 'Apply for leave' })).toBeVisible();
  await page.goto('/approvals');
  await expect(page.getByText('You do not have permission to approve leave applications.')).toBeVisible();

  await login(page, scenario.tenantId, scenario.users.manager01.loginName, scenario.password);
  await page.goto('/approvals');
  await expect(page.getByRole('heading', { name: 'Approval inbox' })).toBeVisible();

  await login(page, scenario.tenantId, scenario.users.hr.loginName, scenario.password);
  await page.goto('/staff');
  await expect(page.getByRole('heading', { name: /staff/i }).first()).toBeVisible();

  await login(page, scenario.tenantId, scenario.users.admin.loginName, scenario.password);
  await page.goto('/staff');
  await expect(page.getByRole('heading', { name: /staff/i }).first()).toBeVisible();
  await assertHealthy();
});
