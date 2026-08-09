import type { IResourceItem } from '@refinedev/core';

export const resources: IResourceItem[] = [
  {
    name: 'dashboard',
    list: '/',
    meta: {
      label: 'Dashboard',
      icon: 'dashboard',
    },
  },
  {
    name: 'leave-requests',
    list: '/leave-requests',
    create: '/leave-requests/create',
    edit: '/leave-requests/edit/:id',
    show: '/leave-requests/show/:id',
    meta: {
      label: 'Leave Requests',
      icon: 'calendar',
    },
  },
  {
    name: 'employees',
    list: '/employees',
    create: '/employees/create',
    edit: '/employees/edit/:id',
    show: '/employees/show/:id',
    meta: {
      label: 'Employees',
      icon: 'team',
    },
  },
];
