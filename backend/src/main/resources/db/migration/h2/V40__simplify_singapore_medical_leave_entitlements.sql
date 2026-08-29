-- LeaveMaster intentionally represents Singapore's combined paid medical-leave entitlement
-- as two independent balances because shared entitlement pools are not yet supported.
--
-- Statutory position: the hospitalisation entitlement is an overall medical-leave maximum
-- that includes outpatient sick leave (15/30/45/60 days by service), while outpatient sick
-- leave itself is capped at 5/8/11/14 days. To avoid exposing an additive 20/38/56/74-day
-- balance, the Hospitalisation Leave template stores only the additional amount beyond the
-- outpatient component: 10/22/34/46 days.
--
-- This is an implementation representation, not a statement that Singapore law defines
-- hospitalisation leave as 10/22/34/46 days. Source:
-- https://www.mom.gov.sg/employment-practices/leave/sick-leave/eligibility-and-entitlement

UPDATE leave_entitlement_policy
SET entitlement_amount = CASE id
    WHEN 'SG_HOSP_03' THEN 10
    WHEN 'SG_HOSP_04' THEN 22
    WHEN 'SG_HOSP_05' THEN 34
    WHEN 'SG_HOSP_06_PLUS' THEN 46
END
WHERE scope = 'PLATFORM_TEMPLATE'
  AND jurisdiction_id = 'SG'
  AND id IN ('SG_HOSP_03', 'SG_HOSP_04', 'SG_HOSP_05', 'SG_HOSP_06_PLUS');
