export interface TenantJurisdictionSummary {
  jurisdictionId: string;
}

export interface JurisdictionOption {
  label: string;
  value: string | boolean;
}

export const tenantOnboardingInitialValues = (resourceName: string | undefined, year = new Date().getFullYear()) => {
  if (!['tenants', 'tenant-jurisdictions'].includes(resourceName ?? '')) return {};

  const calendarValues = {
    calendarStart: `${year}-01-01`,
    calendarEnd: `${year}-12-31`,
  };

  if (resourceName === 'tenants') {
    return {
      ...calendarValues,
      jurisdictions: [{ includePublicHolidays: true, includeLeaveConfiguration: true }],
    };
  }

  return {
    ...calendarValues,
    includePublicHolidays: true,
    includeLeaveConfiguration: true,
  };
};

export const excludeExistingJurisdictions = (
  options: JurisdictionOption[],
  existingJurisdictions: TenantJurisdictionSummary[],
) => {
  const existing = new Set(existingJurisdictions.map((item) => item.jurisdictionId));
  return options.filter((option) => !existing.has(String(option.value)));
};
