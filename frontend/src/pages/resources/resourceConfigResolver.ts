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

export const getAdminResourceConfig = (name?: string) => {
  if (name === 'public-holidays') return publicHolidayConfig;
  if (name === 'tenant-jurisdictions') return tenantJurisdictionConfig;
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
