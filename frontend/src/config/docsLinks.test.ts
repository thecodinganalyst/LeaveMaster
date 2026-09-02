import { describe, expect, it } from 'vitest';

import { contextualHelpForPath, docsLinks } from './docsLinks.ts';

describe('documentation links', () => {
  it('uses the configured shared documentation base URL', () => {
    expect(docsLinks.userGuide).toMatch(/^https?:\/\//);
    expect(docsLinks.userGuide).toContain('/LeaveMaster/user-guide/');
  });

  it.each([
    ['/leave-requests/apply', 'employee/#apply-for-leave'],
    ['/approvals', 'manager/#review-requests-awaiting-your-action'],
    ['/employees/create', 'hr/#create-a-staff-member'],
    ['/leave-approvers', 'hr/#configure-leave-approvers'],
    ['/leave-types', 'admin/#leave-types'],
    ['/leave-entitlement-policies', 'admin/#entitlement-policies-and-eligibility'],
    ['/account/security', 'account-security/'],
  ])('maps %s to the relevant user guide section', (pathname, expectedPath) => {
    expect(contextualHelpForPath(pathname)?.href).toContain(expectedPath);
  });

  it('maps read-only staff leave calendar access to the employee guide', () => {
    expect(contextualHelpForPath('/leave-calendars')?.href)
      .toContain('employee/#view-the-leave-calendar');
  });

  it('maps approver leave calendar access to the manager guide', () => {
    expect(contextualHelpForPath('/leave-calendars', { canApproveLeave: true })?.href)
      .toContain('manager/#view-team-leave');
  });

  it('maps writable leave calendar access to the administrator guide', () => {
    expect(contextualHelpForPath('/leave-calendars', {
      canApproveLeave: true,
      canEditLeaveCalendars: true,
    })?.href).toContain('admin/#leave-calendars-and-public-holidays');
  });

  it('returns no contextual link for unrelated administration pages', () => {
    expect(contextualHelpForPath('/tenants')).toBeNull();
  });
});
