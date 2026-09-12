import { expect, type Page, type Route } from '@playwright/test';
import { createStandardSingaporeScenario, personForRole, type ScenarioRole } from './scenario-data';

export type E2ERole = ScenarioRole;

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
  page.on('requestfailed', (request) => {
    const failure = request.failure()?.errorText ?? '';
    const path = new URL(request.url()).pathname;
    if (request.method() === 'POST' && path === '/auth/login' && failure === 'net::ERR_ABORTED') return;
    failedRequests.push(`${request.method()} ${request.url()} ${failure}`);
  });
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
  scenarioId = 'browser-smoke',
) => {
  let authenticated = initiallyAuthenticated;
  const scenario = createStandardSingaporeScenario(scenarioId);
  const person = personForRole(scenario, role);
  const currentUser = {
    loginName: person.loginName,
    staffId: person.staffId,
    tenantId: scenario.tenantId,
    country: scenario.jurisdictionId,
    active: true,
    platformAdmin: false,
    authorities: roleAuthorities[role],
  };

  await page.route('https://telemetry.refine.dev/**', (route) => json(route, {}));
  await page.route('**/auth/csrf', (route) => json(route, { token: 'e2e-csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' }));
  await page.route('**/auth/me', (route) => authenticated ? json(route, currentUser) : json(route, { message: 'Unauthenticated' }, 401));
  await page.route('**/account-activation/lookup', (route) => json(route, { nextStep: 'PASSWORD' }));
  await page.route('**/auth/login', (route) => {
    authenticated = true;
    return json(route, {});
  });
  await page.route('**/leave-application-options/leave-types', (route) => json(route, [
    { id: scenario.annualLeaveTypeId, name: 'Annual Leave' },
    { id: `${scenario.tenantId}:SG:SICK_LEAVE`, name: 'Sick Leave' },
  ]));
  await page.route('**/leave-applications/policy-metadata**', (route) => json(route, {
    policyModel: 'ANNUAL_ENTITLEMENT',
    eventBased: false,
    eventRequiresVerification: false,
  }));
  await page.route('**/leave-applications/staff/**/balance', (route) => json(route, [{
    leaveType: { id: scenario.annualLeaveTypeId, name: 'Annual Leave' }, entitlement: 14, used: 2, balance: 12,
  }]));
  await page.route('**/leave-applications**', async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/policy-metadata') || url.pathname.includes('/staff/')) {
      return route.fallback();
    }
    if (route.request().method() === 'POST') {
      return json(route, [{
        id: `${scenario.tenantId}-leave-1`,
        staff: { id: person.staffId, name: person.name },
        leaveDate: '2026-09-10',
        leaveType: { id: scenario.annualLeaveTypeId, name: 'Annual Leave' },
        leaveDuration: 'FULL',
        status: 'PENDING',
        applicationDate: '2026-09-01',
      }]);
    }
    return json(route, []);
  });

  await page.route('**/api/**', (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === `/api/staff/${currentUser.staffId}`) {
      // The existing Apply Leave browser test intentionally verifies the HTML date bounds
      // against this fixed employment window. Keep that contract deterministic while the
      // reusable scenario factory supplies the authenticated identity and other scenario data.
      const joinDate = role === 'staff' ? '2026-09-01' : person.joinDate;
      const termDate = role === 'staff' ? '2026-09-30' : person.termDate;
      return json(route, {
        id: person.staffId,
        name: person.name,
        joinDate,
        termDate,
        jurisdictionId: person.jurisdictionId,
        tenantId: scenario.tenantId,
      });
    }
    return json(route, []);
  });

  return scenario;
};
