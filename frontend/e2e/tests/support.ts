import { expect, type Page, type Route } from '@playwright/test';

export type E2ERole = 'staff' | 'manager' | 'hr' | 'admin';

const roleAuthorities: Record<E2ERole, string[]> = {
  staff: ['LEAVE_APPLICATION_READ', 'LEAVE_APPLICATION_WRITE', 'LEAVE_CALENDAR_READ'],
  manager: ['LEAVE_APPLICATION_READ', 'LEAVE_APPLICATION_WRITE', 'LEAVE_CALENDAR_READ', 'LEAVE_APPROVAL_READ', 'LEAVE_APPROVAL_WRITE'],
  hr: ['LEAVE_APPLICATION_READ', 'LEAVE_APPLICATION_WRITE', 'LEAVE_CALENDAR_READ', 'LEAVE_CALENDAR_WRITE', 'STAFF_READ', 'STAFF_WRITE'],
  admin: ['LEAVE_APPLICATION_READ', 'LEAVE_APPLICATION_WRITE', 'LEAVE_CALENDAR_READ', 'LEAVE_CALENDAR_WRITE', 'STAFF_READ', 'STAFF_WRITE', 'LEAVE_TYPE_READ', 'LEAVE_TYPE_WRITE'],
};

const json = (route: Route, body: unknown, status = 200) =>
  route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });

export const installFailureGuards = (page: Page, allowedStatuses: number[] = []) => {
  const pageErrors: string[] = [];
  const consoleErrors: string[] = [];
  const badResponses: string[] = [];
  const failedRequests: string[] = [];

  page.on('pageerror', (error) => pageErrors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });
  page.on('requestfailed', (request) => failedRequests.push(`${request.method()} ${request.url()} ${request.failure()?.errorText ?? ''}`));
  page.on('response', (response) => {
    if (response.status() >= 400 && !allowedStatuses.includes(response.status())) {
      badResponses.push(`${response.status()} ${response.request().method()} ${response.url()}`);
    }
  });

  return async () => {
    await expect(page.locator('body')).not.toBeEmpty();
    expect(pageErrors, `Uncaught page errors:\n${pageErrors.join('\n')}`).toEqual([]);
    expect(consoleErrors, `Console errors:\n${consoleErrors.join('\n')}`).toEqual([]);
    expect(failedRequests, `Failed requests:\n${failedRequests.join('\n')}`).toEqual([]);
    expect(badResponses, `Unexpected HTTP failures:\n${badResponses.join('\n')}`).toEqual([]);
  };
};

export const mockAuthenticatedBackend = async (
  page: Page,
  role: E2ERole = 'staff',
  initiallyAuthenticated = true,
) => {
  let authenticated = initiallyAuthenticated;
  const currentUser = {
    loginName: `e2e-${role}`,
    staffId: role === 'admin' ? 'E2E-ADMIN' : `E2E-${role.toUpperCase()}`,
    tenantId: 'E2E',
    country: 'SG',
    active: true,
    platformAdmin: false,
    authorities: roleAuthorities[role],
  };

  await page.route('**/auth/csrf', (route) => json(route, { token: 'e2e-csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' }));
  await page.route('**/auth/me', (route) => authenticated ? json(route, currentUser) : json(route, { message: 'Unauthenticated' }, 401));
  await page.route('**/account-activation/lookup', (route) => json(route, { nextStep: 'PASSWORD' }));
  await page.route('**/auth/login', (route) => {
    authenticated = true;
    return route.fulfill({ status: 204 });
  });
  await page.route('**/leave-application-options/leave-types', (route) => json(route, [
    { id: 'E2E:ANNUAL_LEAVE', name: 'Annual Leave' },
    { id: 'E2E:SICK_LEAVE', name: 'Sick Leave' },
  ]));
  await page.route('**/leave-applications/policy-metadata**', (route) => json(route, {
    policyModel: 'ANNUAL_ENTITLEMENT',
    eventBased: false,
    eventRequiresVerification: false,
  }));
  await page.route('**/leave-applications/staff/**/balance', (route) => json(route, [{
    leaveType: { id: 'E2E:ANNUAL_LEAVE', name: 'Annual Leave' }, entitlement: 14, used: 2, balance: 12,
  }]));
  await page.route('**/leave-applications**', async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/policy-metadata') || url.pathname.includes('/staff/')) {
      return route.fallback();
    }
    if (route.request().method() === 'POST') {
      return json(route, [{
        id: 'E2E-LEAVE-1',
        staff: { id: 'E2E-STAFF', name: 'E2E Staff' },
        leaveDate: '2026-09-10',
        leaveType: { id: 'E2E:ANNUAL_LEAVE', name: 'Annual Leave' },
        leaveDuration: 'FULL',
        status: 'PENDING',
        applicationDate: '2026-09-01',
      }]);
    }
    return json(route, []);
  });

  // Resource list pages use Refine's REST provider. Empty deterministic collections are enough
  // for browser/RBAC smoke coverage and keep these E2E tests independent of production data.
  await page.route('**/api/**', (route) => json(route, []));
};
