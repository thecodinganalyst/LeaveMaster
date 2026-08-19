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
  label: 'Public Holidays',
  singular: 'Public holiday',
  idField: 'id',
  fields: [
    { name: 'id', label: 'ID', hidden: true, readOnlyOnEdit: true },
    { name: 'jurisdictionId', label: 'Jurisdiction', required: true, list: true },
    { name: 'year', label: 'Year', type: 'number', list: true, formHidden: true },
    { name: 'holidayDate', label: 'Holiday date', type: 'date', required: true, list: true },
    { name: 'holidayName', label: 'Holiday name', required: true, list: true },
    { name: 'locationId', label: 'Location ID', list: true },
    { name: 'calendarId', label: 'Backing template', readOnlyOnEdit: true, formHidden: true },
  ],
};

export const getAdminResourceConfig = (name?: string) => (
  name === 'public-holidays' ? publicHolidayConfig : getBaseAdminResourceConfig(name)
);

export {
  getAdminResourceInitialValues,
  isAdminFieldVisible,
  normaliseFormValues,
  toFormValues,
};

export type { AdminField, AdminResourceConfig };
