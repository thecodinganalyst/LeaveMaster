import {
  getAdminResourceConfig as getBaseAdminResourceConfig,
  getAdminResourceInitialValues as getBaseAdminResourceInitialValues,
  isAdminFieldVisible,
  normaliseFormValues as normaliseBaseFormValues,
  toFormValues as toBaseFormValues,
  type AdminField,
  type AdminResourceConfig,
} from './adminResourceConfig.ts';
import { DEFAULT_STAFF_WORK_SCHEDULE } from './staffFormHelpers.ts';

const publicHolidayConfig: AdminResourceConfig = {
  name: 'public-holidays',
  label: 'Public Holiday Templates',
  singular: 'Public holiday',
  idField: 'id',
  fields: [
    { name: 'id', label: 'ID', hidden: true, readOnlyOnEdit: true },
    { name: 'jurisdictionId', label: 'Jurisdiction', required: true, list: true },
    { name: 'year', label: 'Year', type: 'number', list: true, formHidden: true },
    { name: 'holidayDate', label: 'Holiday date', type: 'date', required: true, list: true },
    { name: 'holidayName', label: 'Holiday name', required: true, list: true },
    { name: 'calendarId', label: 'Backing template', readOnlyOnEdit: true, formHidden: true },
  ],
};

const tenantJurisdictionConfig: AdminResourceConfig = {
  name: 'tenant-jurisdictions',
  label: 'Tenant Jurisdictions',
  singular: 'Tenant jurisdiction',
  idField: 'id',
  editable: false,
  deletable: false,
  fields: [
    { name: 'id', label: 'ID', hidden: true, readOnlyOnEdit: true },
    { name: 'tenantId', label: 'Tenant ID', hidden: true },
    { name: 'jurisdictionId', label: 'Jurisdiction', required: true, list: true },
    {
      name: 'includePublicHolidays',
      label: 'Add public holidays from template',
      type: 'boolean',
      defaultValue: true,
      description: 'Creates a tenant leave calendar for this jurisdiction and copies applicable public holidays from the platform template.',
    },
    {
      name: 'includeLeaveConfiguration',
      label: 'Add leave types, entitlement policies and eligibility rules from template',
      type: 'boolean',
      defaultValue: true,
      description: 'Copies the active jurisdiction leave configuration into tenant-owned records that can be customized later.',
    },
    { name: 'calendarStart', label: 'Calendar start', type: 'date', required: true },
    { name: 'calendarEnd', label: 'Calendar end', type: 'date', required: true },
    { name: 'createdAt', label: 'Added', list: true, formHidden: true },
  ],
};

const leaveTypeConfig = () => {
  const base = getBaseAdminResourceConfig('leave-types');
  if (!base) return undefined;
  return {
    ...base,
    fields: [
      ...base.fields.map((field) => field.name === 'sourceJurisdictionLeaveTypeId'
        ? { ...field, hidden: true, formHidden: true }
        : { ...field }),
      { name: 'active', label: 'Active', type: 'boolean' as const, list: true },
      { name: 'statutory', label: 'Statutory', type: 'boolean' as const, list: true },
      { name: 'paid', label: 'Paid', type: 'boolean' as const, list: true },
      { name: 'sourceName', label: 'Source name' },
      { name: 'sourceUrl', label: 'Source URL' },
      { name: 'effectiveFrom', label: 'Effective from', type: 'date' as const },
      { name: 'effectiveTo', label: 'Effective to', type: 'date' as const },
    ],
  };
};

const entitlementPolicyConfig = () => {
  const base = getBaseAdminResourceConfig('leave-entitlement-policies');
  if (!base) return undefined;

  const fields = base.fields.map((field) => {
    if (field.name === 'scope' || field.name === 'sourceTemplateId') return { ...field, audience: 'platform' as const };
    if (field.name === 'leaveTypeId') return { ...field, label: 'Leave type' };
    return { ...field };
  });

  const unit = fields.find((field) => field.name === 'entitlementUnit');
  const reorderedFields = fields.filter((field) => field.name !== 'entitlementUnit');
  const amountIndex = reorderedFields.findIndex((field) => field.name === 'entitlementAmount');
  if (unit && amountIndex >= 0) reorderedFields.splice(amountIndex + 1, 0, unit);

  return { ...base, fields: reorderedFields };
};

const eligibilityRuleConfig = () => {
  const base = getBaseAdminResourceConfig('leave-entitlement-policy-eligibility-rules');
  if (!base) return undefined;
  return {
    ...base,
    fields: base.fields.map((field) => field.name === 'policyId' ? { ...field, label: 'Entitlement policy' } : { ...field }),
  };
};

export const getAdminResourceConfig = (name?: string) => {
  if (name === 'public-holidays') return publicHolidayConfig;
  if (name === 'tenant-jurisdictions') return tenantJurisdictionConfig;
  if (name === 'leave-types') return leaveTypeConfig();
  if (name === 'leave-entitlement-policies') return entitlementPolicyConfig();
  if (name === 'leave-entitlement-policy-eligibility-rules') return eligibilityRuleConfig();
  return getBaseAdminResourceConfig(name);
};

export const getAdminResourceInitialValues = (config: AdminResourceConfig) => ({
  ...getBaseAdminResourceInitialValues(config),
  ...(config.name === 'employees' ? { workSchedule: DEFAULT_STAFF_WORK_SCHEDULE.map((entry) => ({ ...entry })) } : {}),
});

export const toFormValues = (config: AdminResourceConfig, record: Record<string, unknown>) =>
  config.name === 'employees' ? { ...record } : toBaseFormValues(config, record);

export const normaliseFormValues = (config: AdminResourceConfig, values: Record<string, unknown>) => {
  const result = normaliseBaseFormValues(config, values);
  if (config.name !== 'employees' || !Array.isArray(result.leaveEntitlements)) return result;

  result.leaveEntitlements = (result.leaveEntitlements as Array<Record<string, unknown>>).map((entitlement) => {
    const leaveType = entitlement.leaveType as { id?: unknown } | undefined;
    const normalized = { ...entitlement };
    delete normalized.leaveType;
    return {
      ...normalized,
      leaveTypeId: leaveType?.id,
    };
  });
  return result;
};

export {
  isAdminFieldVisible,
};

export type { AdminField, AdminResourceConfig };
