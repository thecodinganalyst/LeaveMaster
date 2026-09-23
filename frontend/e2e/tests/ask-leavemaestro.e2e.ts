import { expect, test, type Page, type Route } from '@playwright/test';
import { installFailureGuards, mockAuthenticatedBackend } from './support';

type Reply = { conversationId: string; message: string; pendingActions: unknown[]; structuredResults?: Array<{ toolName: string; data: unknown }> };
const json = (route: Route, body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
const open = async (page: Page) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'Open Ask LeaveMaestro assistant' }).click();
  await expect(page.getByRole('heading', { name: 'Ask LeaveMaestro' })).toBeVisible();
};
const ask = async (page: Page, question: string) => {
  await page.getByRole('textbox', { name: 'Message Ask LeaveMaestro' }).fill(question);
  await page.getByRole('button', { name: 'Send message' }).click();
};

test.describe('Ask LeaveMaestro critical journeys', () => {
  test('@smoke staff sees authoritative own balance and entitlement facts', async ({ page }) => {
    const assertNoFailures = installFailureGuards(page);
    await mockAuthenticatedBackend(page, 'staff');
    let turn = 0;
    await page.route('**/api/assistant/chat', async (route) => {
      turn += 1;
      return turn === 1
        ? json(route, { conversationId: 'assistant-581', message: 'Your Annual Leave balance is shown below.', pendingActions: [], structuredResults: [{ toolName: 'getLeaveBalances', data: [{ leaveTypeName: 'Annual Leave', entitlement: 14, used: 2, balance: 12 }] }] } satisfies Reply)
        : json(route, { conversationId: 'assistant-581', message: 'Your entitlement is based on the configured policy and employment dates.', pendingActions: [], structuredResults: [{ toolName: 'getStaffLeaveEntitlement', data: { staffName: 'E2E Normal Staff', joinDate: '2026-01-01', leaveTypeName: 'Annual Leave', entitlement: 14, configuredEntitlementAmount: 14, sourcePolicyResolved: true } }] } satisfies Reply);
    });
    await open(page);
    await ask(page, 'How much annual leave do I have?');
    await expect(page.getByText('Your Annual Leave balance is shown below.')).toBeVisible();
    await page.getByRole('button', { name: 'View source data' }).click();
    await expect(page.getByText('Authoritative LeaveMaestro data')).toBeVisible();
    await expect(page.getByText('12', { exact: true })).toBeVisible();
    await ask(page, 'Why is that my entitlement?');
    await expect(page.getByText(/configured policy and employment dates/)).toBeVisible();
    await page.getByRole('button', { name: 'View source data' }).last().click();
    await expect(page.getByText('2026-01-01')).toBeVisible();
    await assertNoFailures();
  });

  test('@smoke multi-turn follow-up retains conversation context', async ({ page }) => {
    const assertNoFailures = installFailureGuards(page);
    await mockAuthenticatedBackend(page, 'staff');
    const bodies: Array<{ message: string; conversationId: string | null }> = [];
    await page.route('**/api/assistant/chat', async (route) => {
      bodies.push(route.request().postDataJSON());
      return json(route, { conversationId: 'context-581', message: bodies.length === 1 ? 'You have 12 days of Annual Leave.' : 'You have 14 days of Sick Leave.', pendingActions: [], structuredResults: [] } satisfies Reply);
    });
    await open(page);
    await ask(page, 'How much annual leave do I have?');
    await expect(page.getByText('You have 12 days of Annual Leave.')).toBeVisible();
    await ask(page, 'What about sick leave?');
    await expect(page.getByText('You have 14 days of Sick Leave.')).toBeVisible();
    expect(bodies).toEqual([{ message: 'How much annual leave do I have?', conversationId: null }, { message: 'What about sick leave?', conversationId: 'context-581' }]);
    await assertNoFailures();
  });

  test('@smoke unauthorized staff lookup is rejected without leaking facts', async ({ page }) => {
    const assertNoFailures = installFailureGuards(page, [403]);
    await mockAuthenticatedBackend(page, 'staff');
    await page.route('**/api/assistant/chat', (route) => json(route, { message: "You are not authorized to access another employee's leave information.", conversationId: 'denied-581', error: 'FORBIDDEN' }, 403));
    await open(page);
    await ask(page, 'How much leave does another employee have?');
    await expect(page.getByText(/not authorized/i)).toBeVisible();
    await expect(page.getByText(/another employee has \d+/i)).toHaveCount(0);
    await expect(page.getByRole('heading', { name: 'Ask LeaveMaestro' })).toBeVisible();
    await assertNoFailures();
  });

  test('@smoke manager can ask a permitted team question', async ({ page }) => {
    const assertNoFailures = installFailureGuards(page);
    await mockAuthenticatedBackend(page, 'manager');
    await page.route('**/api/assistant/chat', (route) => json(route, { conversationId: 'manager-581', message: 'Your assigned team member has 7 days of Annual Leave remaining.', pendingActions: [], structuredResults: [{ toolName: 'getLeaveBalances', data: [{ leaveTypeName: 'Annual Leave', balance: 7 }] }] } satisfies Reply));
    await open(page);
    await ask(page, "What is my assigned team member's annual leave balance?");
    await expect(page.getByText('Your assigned team member has 7 days of Annual Leave remaining.')).toBeVisible();
    await assertNoFailures();
  });

  test('@smoke backend failure is safe and assistant remains usable', async ({ page }) => {
    const assertNoFailures = installFailureGuards(page, [502]);
    await mockAuthenticatedBackend(page, 'staff');
    await page.route('**/api/assistant/chat', (route) => json(route, { message: 'LeaveMaestro could not complete the assistant data lookup. Please try again.', conversationId: 'failure-581', error: 'ASSISTANT_TOOL_FAILURE' }, 502));
    await open(page);
    await ask(page, 'Give me my leave balance');
    await expect(page.getByText(/could not complete the assistant data lookup/i)).toBeVisible();
    await expect(page.getByText(/LazyInitializationException|stack trace/i)).toHaveCount(0);
    await expect(page.getByRole('textbox', { name: 'Message Ask LeaveMaestro' })).toBeEnabled();
    await assertNoFailures();
  });

  test('@smoke mobile assistant remains usable', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    const assertNoFailures = installFailureGuards(page);
    await mockAuthenticatedBackend(page, 'staff');
    await page.route('**/api/assistant/chat', (route) => json(route, { conversationId: 'mobile-581', message: 'You have 12 days of Annual Leave.', pendingActions: [], structuredResults: [] } satisfies Reply));
    await open(page);
    const panel = page.getByLabel('Ask LeaveMaestro assistant');
    const box = await panel.boundingBox();
    expect(box?.width ?? 0).toBeGreaterThan(340);
    await ask(page, 'My annual leave balance?');
    await expect(page.getByText('You have 12 days of Annual Leave.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Close assistant' })).toBeVisible();
    await assertNoFailures();
  });
});
