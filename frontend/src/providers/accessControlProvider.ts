import type { AccessControlProvider } from '@refinedev/core';

import { ApiError } from '../api/http.ts';
import { getCurrentUser } from '../auth/session.ts';

const permissionByResource: Record<string, { read?: string; write?: string; approve?: string }> = {
  tenants: { read: 'TENANT_READ', write: 'TENANT_WRITE' },
  jurisdictions: { read: 'JURISDICTION_READ', write: 'JURISDICTION_WRITE' },
  'jurisdiction-leave-types': { read: 'JURISDICTION_LEAVE_TYPE_READ', write: 'JURISDICTION_LEAVE_TYPE_WRITE' },
  users: { read: 'USER_READ', write: 'USER_WRITE' },
  roles: { read: 'ROLE_MANAGE', write: 'ROLE_MANAGE' },
  employees: { read: 'STAFF_READ', write: 'STAFF_WRITE' },
  staff: { read: 'STAFF_READ', write: 'STAFF_WRITE' },
  'leave-types': { read: 'LEAVE_TYPE_READ', write: 'LEAVE_TYPE_WRITE' },
  'leave-entitlement-policies': { read: 'LEAVE_ENTITLEMENT_POLICY_READ', write: 'LEAVE_ENTITLEMENT_POLICY_WRITE' },
  'leave-entitlement-policy-eligibility-rules': { read: 'LEAVE_ENTITLEMENT_POLICY_READ', write: 'LEAVE_ENTITLEMENT_POLICY_WRITE' },
  'leave-approvers': { read: 'LEAVE_APPROVER_READ', write: 'LEAVE_APPROVER_WRITE' },
  'leave-calendars': { read: 'LEAVE_CALENDAR_READ', write: 'LEAVE_CALENDAR_WRITE' },
  'public-holidays': { read: 'PUBLIC_HOLIDAY_READ', write: 'PUBLIC_HOLIDAY_WRITE' },
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
  if (resource === 'dashboard') return undefined;
  const permissions = permissionByResource[resource];
  if (!permissions) return null;
  if (['list', 'show', 'read'].includes(action)) return permissions.read ?? null;
  if (['create', 'edit', 'delete', 'write'].includes(action)) return permissions.write ?? null;
  if (['approve', 'reject', 'approve-cancellation', 'reject-cancellation'].includes(action)) return permissions.approve ?? null;
  return null;
};

export const accessControlProvider: AccessControlProvider = {
  can: async ({ resource, action }) => {
    try {
      if (!resource || !action) return { can: false, reason: 'Resource and action are required.' };
      const permission = requiredPermission(resource, action);
      if (permission === undefined) {
        await getCurrentUser();
        return { can: true };
      }
      if (!permission) return { can: false, reason: 'No permission mapping exists for this action.' };
      const user = await getCurrentUser();
      const allowed = user.authorities.includes(permission);
      return allowed ? { can: true } : { can: false, reason: `Missing ${permission}` };
    } catch (error) {
      if (error instanceof ApiError && [401, 403].includes(error.statusCode)) return { can: false, reason: 'Authentication required.' };
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
