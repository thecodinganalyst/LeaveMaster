-- The Singapore statutory policy templates were originally stamped with the date
-- the seed migration was introduced (2026-08-17). That is a software release date,
-- not a policy-validity date. These current statutory rules are intentionally
-- open-ended until a future legislative version supersedes them.
--
-- Correct both the platform templates and tenant copies already provisioned from
-- those templates. Restrict the update to the known artificial date so future
-- policy versions with genuine effective dates are never altered.

UPDATE leave_entitlement_policy
SET effective_from = NULL
WHERE effective_from = DATE '2026-08-17'
  AND (
      id IN (
          'SG_ANNUAL_03_11',
          'SG_ANNUAL_12_23',
          'SG_ANNUAL_24_35',
          'SG_ANNUAL_36_47',
          'SG_ANNUAL_48_59',
          'SG_ANNUAL_60_71',
          'SG_ANNUAL_72_83',
          'SG_ANNUAL_84_PLUS',
          'SG_SICK_03',
          'SG_SICK_04',
          'SG_SICK_05',
          'SG_SICK_06_PLUS',
          'SG_HOSP_03',
          'SG_HOSP_04',
          'SG_HOSP_05',
          'SG_HOSP_06_PLUS'
      )
      OR source_template_id IN (
          'SG_ANNUAL_03_11',
          'SG_ANNUAL_12_23',
          'SG_ANNUAL_24_35',
          'SG_ANNUAL_36_47',
          'SG_ANNUAL_48_59',
          'SG_ANNUAL_60_71',
          'SG_ANNUAL_72_83',
          'SG_ANNUAL_84_PLUS',
          'SG_SICK_03',
          'SG_SICK_04',
          'SG_SICK_05',
          'SG_SICK_06_PLUS',
          'SG_HOSP_03',
          'SG_HOSP_04',
          'SG_HOSP_05',
          'SG_HOSP_06_PLUS'
      )
  );
