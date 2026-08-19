import type { IResourceItem } from '@refinedev/core';

const crud = (name: string, label: string, icon: string): IResourceItem => ({
  name,
  list: `/${name}`,
  create: `/${name}/create`,
  edit: `/${name}/edit/:id`,
  show: `/${name}/show/:id`,
  meta: { label, icon },
});

export const resources: IResourceItem[] = [
  { name: 'dashboard', list: '/', meta: { label: 'Dashboard', icon: 'dashboard' } },
  crud('leave-requests', 'Leave Requests', 'calendar'),
  crud('employees', 'Staff', 'team'),
  crud('tenants', 'Tenants', 'bank'),
  crud('jurisdictions', 'Jurisdictions', 'global'),
  crud('jurisdiction-leave-types', 'Jurisdiction Leave Types', 'tags'),
  crud('public-holidays', 'Public Holidays', 'calendar'),
  crud('users', 'App Users', 'user'),
  crud('roles', 'Roles', 'safety'),
  crud('locations', 'Locations', 'environment'),
  crud('leave-types', 'Leave Types', 'tags'),
  crud('leave-entitlement-policies', 'Entitlement Policies', 'solution'),
  crud('leave-entitlement-policy-eligibility-rules', 'Eligibility Rules', 'filter'),
  {
    name: 'leave-calendars',
    list: '/leave-calendars',
    create: '/leave-calendars/create',
    meta: { label: 'Leave Calendars', icon: 'calendar' },
  },
  crud('leave-approvers', 'Leave Approvers', 'audit'),
];
