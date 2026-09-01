import { env } from './env.ts';

const joinDocsUrl = (path: string) => `${env.docsBaseUrl}/${path.replace(/^\/+/, '')}`;

export const docsLinks = {
  userGuide: joinDocsUrl('user-guide/'),
  gettingStarted: joinDocsUrl('user-guide/getting-started/'),
  accountSecurity: joinDocsUrl('user-guide/account-security/'),
  applyLeave: joinDocsUrl('user-guide/employee/#apply-for-leave'),
  leaveBalances: joinDocsUrl('user-guide/employee/#view-your-leave-balances-and-entitlements'),
  employeeLeaveCalendar: joinDocsUrl('user-guide/employee/#view-the-leave-calendar'),
  leaveRequests: joinDocsUrl('user-guide/employee/#view-your-leave-requests'),
  approvals: joinDocsUrl('user-guide/manager/#review-and-decide-leave-requests'),
  managerLeaveCalendar: joinDocsUrl('user-guide/manager/#view-team-leave'),
  createStaff: joinDocsUrl('user-guide/hr/#create-staff'),
  staffAndApprovers: joinDocsUrl('user-guide/hr/#maintain-staff-and-leave-approvers'),
  leaveTypes: joinDocsUrl('user-guide/admin/#manage-leave-types'),
  leaveCalendars: joinDocsUrl('user-guide/admin/#manage-leave-calendars-and-public-holidays'),
  entitlementPolicies: joinDocsUrl('user-guide/admin/#manage-entitlement-policies-and-eligibility'),
} as const;

export interface ContextualHelpLink {
  href: string;
  label: string;
}

export const contextualHelpForPath = (pathname: string): ContextualHelpLink | null => {
  if (pathname === '/leave-requests/apply') return { href: docsLinks.applyLeave, label: 'How to apply for leave' };
  if (pathname === '/leave-requests' || pathname.startsWith('/leave-requests/show/')) return { href: docsLinks.leaveRequests, label: 'Help with leave requests' };
  if (pathname === '/approvals' || pathname.startsWith('/approvals/')) return { href: docsLinks.approvals, label: 'Help with leave approvals' };
  if (pathname === '/employees/create') return { href: docsLinks.createStaff, label: 'Help creating staff' };
  if (pathname.startsWith('/employees/') || pathname === '/employees') return { href: docsLinks.staffAndApprovers, label: 'Help with staff and approvers' };
  if (pathname.startsWith('/leave-approvers')) return { href: docsLinks.staffAndApprovers, label: 'Help with leave approvers' };
  if (pathname.startsWith('/leave-types')) return { href: docsLinks.leaveTypes, label: 'Help with leave types' };
  if (pathname.startsWith('/leave-calendars')) return { href: docsLinks.leaveCalendars, label: 'Help with leave calendars' };
  if (pathname.startsWith('/leave-entitlement-policies') || pathname.startsWith('/leave-entitlement-policy-eligibility-rules')) {
    return { href: docsLinks.entitlementPolicies, label: 'Help with entitlement policies' };
  }
  if (pathname.startsWith('/account/security') || pathname.startsWith('/account/change-password')) return { href: docsLinks.accountSecurity, label: 'Help with account security' };
  if (pathname === '/') return { href: docsLinks.leaveBalances, label: 'Help with leave balances' };
  return null;
};
