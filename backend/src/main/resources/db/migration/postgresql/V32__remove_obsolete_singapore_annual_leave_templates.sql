-- Remove the obsolete Singapore annual-leave platform templates that granted 7-12 days.
--
-- Do this in a new migration rather than editing V19 so existing Flyway installations keep
-- their migration checksums valid. These platform templates are superseded by the 14-24 day
-- company-default progression introduced in V29.
--
-- Eligibility rows are deleted first so no stale or orphaned rules remain for the obsolete
-- policy ids. Generated entitlement references are nullable and must be cleared before policy
-- deletion to preserve the FK from leave_entitlement.policy_id during upgrades.

UPDATE leave_entitlement
SET policy_id = NULL
WHERE policy_id IN (
    'SG_ANNUAL_03_11',
    'SG_ANNUAL_12_23',
    'SG_ANNUAL_24_35',
    'SG_ANNUAL_36_47',
    'SG_ANNUAL_48_59',
    'SG_ANNUAL_60_71'
);

DELETE FROM leave_entitlement_policy_eligibility
WHERE policy_id IN (
    'SG_ANNUAL_03_11',
    'SG_ANNUAL_12_23',
    'SG_ANNUAL_24_35',
    'SG_ANNUAL_36_47',
    'SG_ANNUAL_48_59',
    'SG_ANNUAL_60_71'
);

DELETE FROM leave_entitlement_policy
WHERE scope = 'PLATFORM_TEMPLATE'
  AND jurisdiction_id = 'SG'
  AND id IN (
      'SG_ANNUAL_03_11',
      'SG_ANNUAL_12_23',
      'SG_ANNUAL_24_35',
      'SG_ANNUAL_36_47',
      'SG_ANNUAL_48_59',
      'SG_ANNUAL_60_71'
  );
