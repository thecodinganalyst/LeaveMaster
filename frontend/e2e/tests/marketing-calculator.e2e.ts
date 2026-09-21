import { expect, test } from '@playwright/test';

test('public Singapore annual leave calculator validates and calculates without login', async ({ page }) => {
  await page.goto('/tools/singapore-annual-leave-calculator');
  await expect(page.getByRole('heading', { level: 1, name: /Singapore annual leave entitlement calculator/i })).toBeVisible();
  await page.getByLabel('Employment start date').fill('2026-01-14');
  await page.getByLabel('Calculation date').fill('2026-07-14');
  await page.getByRole('button', { name: 'Calculate entitlement' }).click();
  await expect(page.getByText('4 days', { exact: true })).toBeVisible();

  await page.getByLabel('Calculation date').fill('2025-12-31');
  await page.getByRole('button', { name: 'Calculate entitlement' }).click();
  await expect(page.getByRole('alert')).toContainText('on or after');
});
