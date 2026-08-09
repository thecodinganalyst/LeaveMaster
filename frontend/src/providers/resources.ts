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
  crud('users', 'App Users', 'user'),
  crud('roles', 'Roles', 'safety'),
  crud('locations', 'Locations', 'environment'),
  crud('leave-types', 'Leave Types', 'tags'),
  crud('leave-calendars', 'Leave Calendars', 'calendar'),
  crud('leave-approvers', 'Leave Approvers', 'audit'),
];
