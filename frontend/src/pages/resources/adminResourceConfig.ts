export type AdminFieldType = 'text' | 'email' | 'date' | 'boolean' | 'select' | 'country' | 'json' | 'password' | 'permissions';

export interface AdminField {
  name: string;
  label: string;
  type?: AdminFieldType;
  required?: boolean;
  requiredOnCreate?: boolean;
  readOnlyOnEdit?: boolean;
  options?: { label: string; value: string | boolean }[];
  list?: boolean;
  hidden?: boolean;
}

export interface AdminResourceConfig {
  name: string;
  label: string;
  singular: string;
  idField: string;
  fields: AdminField[];
  creatable?: boolean;
  editable?: boolean;
  deletable?: boolean;
}

const tenantStatus = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Dormant', value: 'DORMANT' },
  { label: 'Terminated', value: 'TERMINATED' },
];

export const adminResourceConfigs: Record<string, AdminResourceConfig> = {
  tenants: {
    name: 'tenants', label: 'Tenants', singular: 'Tenant', idField: 'id',
    fields: [
      { name: 'id', label: 'ID', required: true, readOnlyOnEdit: true, list: true },
      { name: 'name', label: 'Name', required: true, list: true },
      { name: 'startDate', label: 'Start date', type: 'date', required: true, list: true },
      { name: 'endDate', label: 'End date', type: 'date', list: true },
      { name: 'status', label: 'Status', type: 'select', required: true, options: tenantStatus, list: true },
    ],
  },
  employees: {
    name: 'employees', label: 'Staff', singular: 'Staff member', idField: 'id',
    fields: [
      { name: 'id', label: 'Staff ID', required: true, readOnlyOnEdit: true, list: true },
      { name: 'name', label: 'Name', required: true, list: true },
      { name: 'email', label: 'Email', type: 'email', list: true },
      { name: 'joinDate', label: 'Join date', type: 'date', required: true, list: true },
      { name: 'termDate', label: 'Termination date', type: 'date', list: true },
      { name: 'loginName', label: 'Login name', list: true },
      { name: 'location', label: 'Location (JSON)', type: 'json' },
      { name: 'workSchedule', label: 'Work schedule (JSON)', type: 'json' },
      { name: 'leaveEntitlements', label: 'Leave entitlements (JSON)', type: 'json' },
    ],
  },
  users: {
    name: 'users', label: 'App Users', singular: 'App user', idField: 'loginName',
    fields: [
      { name: 'loginName', label: 'Login name', required: true, readOnlyOnEdit: true, list: true },
      { name: 'password', label: 'Password', type: 'password', requiredOnCreate: true },
      { name: 'active', label: 'Active', type: 'boolean', list: true },
      { name: 'staffId', label: 'Staff ID', list: true },
      { name: 'oidcProvider', label: 'OIDC provider' },
      { name: 'oidcSubject', label: 'OIDC subject' },
    ],
  },
  roles: {
    name: 'roles', label: 'Roles', singular: 'Role', idField: 'id', deletable: false,
    fields: [
      { name: 'id', label: 'Role ID', required: true, readOnlyOnEdit: true, list: true },
      { name: 'description', label: 'Description', required: true, list: true },
      { name: 'active', label: 'Active', type: 'boolean', list: true },
      { name: 'permissionCodes', label: 'Permissions', type: 'permissions', required: true },
    ],
  },
  locations: {
    name: 'locations', label: 'Locations', singular: 'Location', idField: 'id',
    fields: [
      { name: 'id', label: 'ID', required: true, readOnlyOnEdit: true, list: true },
      { name: 'locationName', label: 'Location name', required: true, list: true },
      { name: 'country', label: 'Country', type: 'country', required: true, list: true },
      { name: 'state', label: 'State / province', list: true },
    ],
  },
  'leave-types': {
    name: 'leave-types', label: 'Leave Types', singular: 'Leave type', idField: 'id',
    fields: [
      { name: 'id', label: 'ID', required: true, readOnlyOnEdit: true, list: true },
      { name: 'name', label: 'Name', required: true, list: true },
      { name: 'used', label: 'In use', type: 'boolean', list: true },
    ],
  },
  'leave-calendars': {
    name: 'leave-calendars', label: 'Leave Calendars', singular: 'Leave calendar', idField: 'id', editable: false, deletable: false,
    fields: [
      { name: 'id', label: 'ID', required: true, readOnlyOnEdit: true, list: true },
      { name: 'start', label: 'Start date', type: 'date', required: true, list: true },
      { name: 'end', label: 'End date', type: 'date', required: true, list: true },
      { name: 'publicHolidays', label: 'Public holidays (JSON)', type: 'json' },
    ],
  },
  'leave-approvers': {
    name: 'leave-approvers', label: 'Leave Approvers', singular: 'Leave approver', idField: 'id',
    fields: [
      { name: 'id', label: 'ID', readOnlyOnEdit: true, list: true, hidden: true },
      { name: 'staffId', label: 'Staff ID', required: true, list: true },
      { name: 'approverId', label: 'Approver ID', required: true, list: true },
      { name: 'effectiveFrom', label: 'Effective from', type: 'date', required: true, list: true },
      { name: 'effectiveTo', label: 'Effective to', type: 'date', list: true },
      { name: 'adminId', label: 'Admin staff ID', required: true, list: true },
    ],
  },
};

export const getAdminResourceConfig = (name?: string) => (name ? adminResourceConfigs[name] : undefined);

export const normaliseFormValues = (config: AdminResourceConfig, values: Record<string, unknown>) => {
  const result = { ...values };
  for (const field of config.fields) {
    if (field.type === 'json' && typeof result[field.name] === 'string') {
      const raw = String(result[field.name]).trim();
      result[field.name] = raw ? JSON.parse(raw) : [];
    }
    if (result[field.name] === '') delete result[field.name];
  }
  return result;
};

export const toFormValues = (config: AdminResourceConfig, record: Record<string, unknown>) => {
  const result = { ...record };
  for (const field of config.fields) {
    if (field.type === 'json' && result[field.name] !== undefined) result[field.name] = JSON.stringify(result[field.name], null, 2);
  }
  if (config.name === 'roles' && Array.isArray(record.permissions)) {
    result.permissionCodes = (record.permissions as Array<{ code?: string }>).map((permission) => permission.code).filter(Boolean);
  }
  if (config.name === 'leave-approvers') {
    const staff = record.staff as { id?: string } | undefined;
    const approver = record.approver as { id?: string } | undefined;
    const admin = record.admin as { id?: string } | undefined;
    result.staffId = staff?.id ?? result.staffId;
    result.approverId = approver?.id ?? result.approverId;
    result.adminId = admin?.id ?? result.adminId;
  }
  return result;
};
