export type AdminFieldType = 'text' | 'email' | 'date' | 'boolean' | 'select' | 'country' | 'json' | 'password' | 'permissions' | 'number';
export type AdminFieldAudience = 'platform' | 'tenant';

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
  formHidden?: boolean;
  audience?: AdminFieldAudience;
  description?: string;
  defaultValue?: unknown;
  min?: number;
  step?: number;
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

const jurisdictionTypes = ['COUNTRY', 'STATE', 'PROVINCE', 'TERRITORY', 'REGION', 'DISTRICT', 'OTHER'].map((value) => ({ label: value, value }));
const entitlementUnits = ['DAYS', 'HOURS'].map((value) => ({ label: value, value }));
const accrualMethods = ['NONE', 'ANNUAL', 'MONTHLY', 'PER_PAY_PERIOD'].map((value) => ({ label: value, value }));
const prorationMethods = ['NONE', 'CALENDAR_DAYS', 'MONTHS'].map((value) => ({ label: value, value }));
const eligibilityCriterionTypes = ['LOCATION_ID', 'JURISDICTION_CODE', 'SERVICE_MONTHS'].map((value) => ({ label: value, value }));
const eligibilityOperators = ['EQUALS', 'NOT_EQUALS', 'IN', 'NOT_IN', 'GREATER_THAN', 'GREATER_THAN_OR_EQUAL', 'LESS_THAN', 'LESS_THAN_OR_EQUAL'].map((value) => ({ label: value, value }));
const configurationScopes = [
  { label: 'Platform template', value: 'PLATFORM_TEMPLATE' },
  { label: 'Tenant policy', value: 'TENANT' },
];

export const adminResourceConfigs: Record<string, AdminResourceConfig> = {
  tenants: {
    name: 'tenants', label: 'Tenants', singular: 'Tenant', idField: 'id',
    fields: [
      { name: 'id', label: 'ID', required: true, readOnlyOnEdit: true, list: true },
      { name: 'name', label: 'Name', required: true, list: true },
      { name: 'jurisdictionId', label: 'Jurisdiction', required: true, readOnlyOnEdit: true, list: true },
      { name: 'startDate', label: 'Start date', type: 'date', required: true, list: true },
      { name: 'endDate', label: 'End date', type: 'date', list: true },
      { name: 'status', label: 'Status', type: 'select', required: true, options: tenantStatus, list: true },
    ],
  },
  jurisdictions: {
    name: 'jurisdictions', label: 'Jurisdictions', singular: 'Jurisdiction', idField: 'id',
    fields: [
      { name: 'id', label: 'ID', readOnlyOnEdit: true, list: true },
      { name: 'code', label: 'Code', required: true, readOnlyOnEdit: true, list: true },
      { name: 'name', label: 'Name', required: true, list: true },
      { name: 'jurisdictionType', label: 'Type', type: 'select', required: true, options: jurisdictionTypes, list: true },
      { name: 'parentId', label: 'Parent jurisdiction', list: true },
      { name: 'countryCode', label: 'Country code', required: true, list: true },
      { name: 'subdivisionCode', label: 'Subdivision code', list: true },
      { name: 'active', label: 'Active', type: 'boolean', list: true },
    ],
  },
  'jurisdiction-leave-types': {
    name: 'jurisdiction-leave-types', label: 'Jurisdiction Leave Types', singular: 'Jurisdiction leave type', idField: 'id',
    fields: [
      { name: 'id', label: 'ID', readOnlyOnEdit: true, list: true },
      { name: 'jurisdictionId', label: 'Jurisdiction', required: true, list: true },
      { name: 'code', label: 'Code', required: true, list: true },
      { name: 'name', label: 'Name', required: true, list: true },
      { name: 'description', label: 'Description' },
      { name: 'statutory', label: 'Statutory', type: 'boolean', list: true },
      { name: 'paid', label: 'Paid', type: 'boolean', list: true },
      { name: 'active', label: 'Active', type: 'boolean', list: true },
      { name: 'sourceName', label: 'Source name' },
      { name: 'sourceUrl', label: 'Source URL' },
      { name: 'effectiveFrom', label: 'Effective from', type: 'date' },
      { name: 'effectiveTo', label: 'Effective to', type: 'date' },
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
      { name: 'sourceJurisdictionLeaveTypeId', label: 'Source jurisdiction leave type', list: true, readOnlyOnEdit: true },
    ],
  },
  'leave-entitlement-policies': {
    name: 'leave-entitlement-policies', label: 'Entitlement Policies', singular: 'Entitlement policy', idField: 'id',
    fields: [
      { name: 'id', label: 'ID', readOnlyOnEdit: true, list: true, hidden: true },
      { name: 'scope', label: 'Policy type', type: 'select', options: configurationScopes, list: true, formHidden: true },
      { name: 'tenantId', label: 'Tenant ID', hidden: true },
      { name: 'jurisdictionId', label: 'Jurisdiction', required: true, list: true, audience: 'platform' },
      { name: 'jurisdictionLeaveTypeId', label: 'Jurisdiction leave type ID', required: true, list: true, audience: 'platform' },
      { name: 'leaveTypeId', label: 'Leave type ID', required: true, list: true, audience: 'tenant' },
      { name: 'sourceTemplateId', label: 'Source template ID', list: true, formHidden: true, audience: 'tenant' },
      { name: 'name', label: 'Name', required: true, list: true },
      { name: 'active', label: 'Active', type: 'boolean', list: true },
      {
        name: 'priority',
        label: 'Priority (higher number wins)',
        type: 'number',
        required: true,
        list: true,
        min: 0,
        step: 1,
        defaultValue: 10,
        description: 'Determines which policy wins when more than one policy matches an employee. Start with 10 for the default policy and use higher values such as 20 or 30 for more specific rules. Using increments of 10 leaves room for intermediate priorities. Avoid giving overlapping policies the same highest priority because that makes policy resolution ambiguous.',
      },
      { name: 'entitlementUnit', label: 'Unit', type: 'select', required: true, options: entitlementUnits, list: true },
      { name: 'entitlementAmount', label: 'Entitlement amount', required: true, list: true },
      { name: 'accrualMethod', label: 'Accrual method', type: 'select', required: true, options: accrualMethods, list: true },
      { name: 'accrualRate', label: 'Accrual rate' },
      { name: 'prorationMethod', label: 'Proration method', type: 'select', required: true, options: prorationMethods, list: true },
      { name: 'carryForwardAllowed', label: 'Carry forward allowed', type: 'boolean', list: true },
      { name: 'carryForwardLimit', label: 'Carry forward limit' },
      { name: 'carryForwardExpiryMonths', label: 'Carry forward expiry (months)' },
      { name: 'effectiveFrom', label: 'Effective from', type: 'date', required: true, list: true },
      { name: 'effectiveTo', label: 'Effective to', type: 'date', list: true },
    ],
  },
  'leave-entitlement-policy-eligibility-rules': {
    name: 'leave-entitlement-policy-eligibility-rules', label: 'Eligibility Rules', singular: 'Eligibility rule', idField: 'id',
    fields: [
      { name: 'id', label: 'ID', readOnlyOnEdit: true, hidden: true },
      { name: 'policyId', label: 'Policy ID', required: true, readOnlyOnEdit: true, list: true },
      { name: 'criterionType', label: 'Criterion', type: 'select', required: true, options: eligibilityCriterionTypes, list: true },
      { name: 'operator', label: 'Operator', type: 'select', required: true, options: eligibilityOperators, list: true },
      { name: 'value', label: 'Value', required: true, list: true },
      { name: 'active', label: 'Active', type: 'boolean', list: true },
      { name: 'sortOrder', label: 'Sort order', required: true, list: true },
    ],
  },
  'leave-calendars': {
    name: 'leave-calendars', label: 'Leave Calendars', singular: 'Leave calendar', idField: 'id',
    fields: [
      { name: 'id', label: 'ID', readOnlyOnEdit: true, list: true },
      { name: 'scope', label: 'Scope', list: true, readOnlyOnEdit: true },
      { name: 'tenantId', label: 'Tenant ID', list: true, readOnlyOnEdit: true },
      { name: 'jurisdictionId', label: 'Template jurisdiction', list: true },
      { name: 'sourceTemplateId', label: 'Source template ID', list: true, readOnlyOnEdit: true },
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

export const isAdminFieldVisible = (field: AdminField, platformAdmin: boolean) => {
  if (field.audience === 'platform') return platformAdmin;
  if (field.audience === 'tenant') return !platformAdmin;
  return true;
};

export const getAdminResourceInitialValues = (config: AdminResourceConfig) => Object.fromEntries(
  config.fields
    .filter((field) => field.defaultValue !== undefined)
    .map((field) => [field.name, field.defaultValue]),
);

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