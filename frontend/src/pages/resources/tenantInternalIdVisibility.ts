const resourcesWithHiddenInternalIds = new Set([
  'leave-types',
]);

const tenantResourcesWithHiddenInternalIds = new Set([
  'leave-entitlement-policies',
  'leave-entitlement-policy-eligibility-rules',
]);

export const shouldHideTenantInternalId = (
  resourceName: string,
  idField: string,
  fieldName: string,
  platformAdmin: boolean,
) => fieldName === idField
  && (resourcesWithHiddenInternalIds.has(resourceName)
    || (!platformAdmin && tenantResourcesWithHiddenInternalIds.has(resourceName)));

export const shouldShowResourceIdSubtitle = (resourceName: string, platformAdmin: boolean) => (
  !resourcesWithHiddenInternalIds.has(resourceName)
  && (platformAdmin || !tenantResourcesWithHiddenInternalIds.has(resourceName))
);
