import type { AccessControlProvider } from '@refinedev/core';

import { ApiError } from '../api/http.ts';
import { getCurrentUser } from '../auth/session.ts';

const permissionByResource: Record<string, { read?: string; write?: string; approve?: string }> = {
  tenants: { read: 'TENANT_READ', write: 'TENANT_WRITE' },
  users: { read: 'USER_READ', write: 'USER_WRITE' },
  roles: { read: 'ROLE_MANAGE', write: 'ROLE_MANAGE' },
  employees: { read: 'STAFF_READ', write: 'STAFF_WRITE' },
  staff: { read: 'STAFF_READ', write: 'STAFF_WRITE' },
  locations: { read: 'LOCATION_READ', write: 'LOCATION_WRITE' },
  'leave-types': { read: 'LEAVE_TYPE_READ', write: 'LEAVE_TYPE_WRITE' },
  'leave-approvers': { read: 'LEAVE_APPROVER_READ', write: 'LEAVE_APPROVER_WRITE' },
  'leave-calendars': { read: 'LEAVE_CALENDAR_READ', write: 'LEAVE_CALENDAR_WRITE' },
  'leave-requests': {
    read: 'LEAVE_APPLICATION_READ',
    write: 'LEAVE_APPLICATION_WRITE',
    approve: 'LEAVE_APPLICATION_APPROVE',
  },
  'leave-applications': {
    read: 'LEAVE_APPLICATION_READ',
    write: 'LEAVE_APPLICATION_WRITE',
    approve: 'LEAVE_APPLICATION_APPROVE',
  },
};

const requiredPermission = (resource: string, action: string) => {
  if (resource === 'dashboard') {
    return undefined;
  }

  const permissions = permissionByResource[resource];
  if (!permissions) {
    return null;
  }

  if (['list', 'show', 'read'].includes(action)) {
    return permissions.read ?? null;
  }
  if (['create', 'edit', 'delete', 'write'].includes(action)) {
    return permissions.write ?? null;
  }
  if (['approve', 'reject', 'approve-cancellation', 'reject-cancellation'].includes(action)) {
    return permissions.approve ?? null;
  }

  return null;
};

export const accessControlProvider: AccessControlProvider = {
  can: async ({ resource, action }) => {
    try {
      const permission = requiredPermission(resource, action);
      if (permission === undefined) {
        await getCurrentUser();
        return { can: true };
      }
      if (!permission) {
        return { can: false, reason: 'No permission mapping exists for this action.' };
      }

      const user = await getCurrentUser();
      return {
        can: user.authorities.includes(permission),
        reason: user.authorities.includes(permission) ? undefined : `Missing ${permission}`,
      };
    } catch (error) {
      if (error instanceof ApiError && [401, 403].includes(error.statusCode)) {
        return { can: false, reason: 'Authentication required.' };
      }
      throw error;
    }
  },
  options: {
    buttons: {
      enableAccessControl: true,
      hideIfUnauthorized: true,
    },
  },
};
