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
    ['/leave-calendars', 'admin/#leave-calendars-and-public-holidays'],
    ['/leave-entitlement-policies', 'admin/#entitlement-policies-and-eligibility'],
    ['/account/security', 'account-security/'],
  ])('maps %s to the relevant user guide section', (pathname, expectedPath) => {
    expect(contextualHelpForPath(pathname)?.href).toContain(expectedPath);
  });

  it('returns no contextual link for unrelated administration pages', () => {
    expect(contextualHelpForPath('/tenants')).toBeNull();
  });
});
