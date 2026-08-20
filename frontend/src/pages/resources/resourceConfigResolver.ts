import {
  getAdminResourceConfig as getBaseAdminResourceConfig,
  getAdminResourceInitialValues,
  isAdminFieldVisible,
  normaliseFormValues,
  toFormValues,
  type AdminField,
  type AdminResourceConfig,
} from './adminResourceConfig.ts';

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

export {
  getAdminResourceInitialValues,
  isAdminFieldVisible,
  normaliseFormValues,
  toFormValues,
};

export type { AdminField, AdminResourceConfig };
