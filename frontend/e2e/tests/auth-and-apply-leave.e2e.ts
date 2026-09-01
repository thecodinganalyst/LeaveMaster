import { expect, test } from '@playwright/test';

import { installFailureGuards, mockAuthenticatedBackend } from './support.ts';

test('password login reaches the authenticated application shell without runtime or HTTP failures', async ({ page }) => {
  await mockAuthenticatedBackend(page, 'staff');
  const assertHealthy = installFailureGuards(page);

  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'LeaveMaestro' })).toBeVisible();
  await page.getByLabel('Tenant ID').fill('E2E');
  await page.getByLabel('Login name').fill('e2e-staff');
  await page.getByRole('button', { name: 'Continue' }).click();
  await expect(page.getByText('E2E / e2e-staff')).toBeVisible();
  await page.getByLabel('Password').fill('e2e-password');
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.locator('body')).toContainText('LeaveMaestro');
  await assertHealthy();
});

test('staff can open Apply Leave and submit a deterministic request', async ({ page }) => {
  await mockAuthenticatedBackend(page, 'staff');
  const assertHealthy = installFailureGuards(page);

  await page.goto('/leave-requests/apply');
  await expect(page.getByRole('heading', { name: 'Apply for leave' })).toBeVisible();
  await expect(page.getByLabel('Leave type')).toBeEnabled();
  await page.getByLabel('Leave type').click();
  await page.getByText('Annual Leave', { exact: true }).click();
  await page.getByLabel('From date').fill('2026-09-10');
  await page.getByLabel('To date').fill('2026-09-10');
  await page.getByRole('button', { name: 'Submit request' }).click();

  await expect(page).toHaveURL(/\/leave-requests$/);
  await assertHealthy();
});

test('Apply Leave cannot regress to an empty React page', async ({ page }) => {
  await mockAuthenticatedBackend(page, 'staff');
  const assertHealthy = installFailureGuards(page);

  await page.goto('/leave-requests/apply');
  await expect(page.getByRole('heading', { name: 'Apply for leave' })).toBeVisible();
  await expect(page.getByText('Submit a leave request for one or more working days.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Submit request' })).toBeVisible();
  await assertHealthy();
});
