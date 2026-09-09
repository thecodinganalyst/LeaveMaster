import { expect, test } from '@playwright/test';

import { installFailureGuards, mockAuthenticatedBackend, type E2ERole } from './support.ts';

for (const role of ['staff', 'manager', 'hr', 'admin'] as E2ERole[]) {
  test(`${role} can render the leave calendar list without authorization or runtime failures`, async ({ page }) => {
    await mockAuthenticatedBackend(page, role);
    const assertHealthy = installFailureGuards(page);

    await page.goto('/leave-calendars');
    await expect(page.locator('body')).toContainText('Leave Calendars');
    await expect(page.locator('body')).not.toContainText('Something went wrong');
    await assertHealthy();
  });
}

test('staff and manager do not get leave-calendar write controls', async ({ page }) => {
  for (const role of ['staff', 'manager'] as E2ERole[]) {
    await mockAuthenticatedBackend(page, role);
    await page.goto('/leave-calendars');
    await expect(page.getByRole('button', { name: 'Create' })).toHaveCount(0);
  }
});

test('HR and tenant admin get leave-calendar write controls', async ({ page }) => {
  for (const role of ['hr', 'admin'] as E2ERole[]) {
    await mockAuthenticatedBackend(page, role);
    await page.goto('/leave-calendars');
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible();
  }
});

test('tenant admin staff page renders only staff returned for the authenticated tenant', async ({ page }) => {
  await mockAuthenticatedBackend(page, 'admin');
  const assertHealthy = installFailureGuards(page);

  await page.route('**/api/staff', (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify([
      {
        id: 'E2E-ADMIN',
        name: 'Tenant E2E Admin',
        tenantId: 'E2E',
        joinDate: '2026-01-01',
        jurisdictionId: 'SG',
      },
    ]),
  }));

  await page.goto('/employees');

  await expect(page.locator('body')).toContainText('Tenant E2E Admin');
  await expect(page.locator('body')).not.toContainText('Other Tenant Staff');
  await expect(page.locator('body')).not.toContainText('Something went wrong');
  await assertHealthy();
});
