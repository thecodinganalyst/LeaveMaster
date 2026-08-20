const tenantResourcesWithHiddenInternalIds = new Set([
  'leave-types',
  'leave-entitlement-policies',
  'leave-entitlement-policy-eligibility-rules',
]);

export const shouldHideTenantInternalId = (
  resourceName: string,
  idField: string,
  fieldName: string,
  platformAdmin: boolean,
) => !platformAdmin
  && tenantResourcesWithHiddenInternalIds.has(resourceName)
  && fieldName === idField;

export const shouldShowResourceIdSubtitle = (resourceName: string, platformAdmin: boolean) => (
  platformAdmin || !tenantResourcesWithHiddenInternalIds.has(resourceName)
);
