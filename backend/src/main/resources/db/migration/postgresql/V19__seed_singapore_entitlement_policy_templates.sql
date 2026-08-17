-- Singapore statutory entitlement templates that can be represented safely by the
-- current eligibility engine (LOCATION_ID, JURISDICTION_CODE, SERVICE_MONTHS).
-- Sources checked 2026-08-17:
-- Annual leave: https://www.mom.gov.sg/employment-practices/leave/annual-leave/eligibility-and-entitlement
-- Sick leave: https://www.mom.gov.sg/employment-practices/leave/sick-leave/eligibility-and-entitlement
--
-- Family leave schemes are intentionally not seeded as active entitlement policies here.
-- Their statutory eligibility depends on child/parent attributes that the current engine
-- cannot represent. See docs/platform-leave-configuration-templates.md.

INSERT INTO leave_entitlement_policy (
    id, tenant_id, leave_type_id, name, active, priority,
    entitlement_unit, entitlement_amount, accrual_method, accrual_rate,
    proration_method, carry_forward_allowed, carry_forward_limit,
    carry_forward_expiry_months, effective_from, effective_to,
    scope, jurisdiction_id, jurisdiction_leave_type_id, source_template_id
) VALUES
    ('SG_ANNUAL_03_11', NULL, NULL, 'Singapore Annual Leave - 1st year', TRUE, 10, 'DAYS', 7, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_12_23', NULL, NULL, 'Singapore Annual Leave - 2nd year', TRUE, 20, 'DAYS', 8, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_24_35', NULL, NULL, 'Singapore Annual Leave - 3rd year', TRUE, 30, 'DAYS', 9, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_36_47', NULL, NULL, 'Singapore Annual Leave - 4th year', TRUE, 40, 'DAYS', 10, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_48_59', NULL, NULL, 'Singapore Annual Leave - 5th year', TRUE, 50, 'DAYS', 11, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_60_71', NULL, NULL, 'Singapore Annual Leave - 6th year', TRUE, 60, 'DAYS', 12, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_72_83', NULL, NULL, 'Singapore Annual Leave - 7th year', TRUE, 70, 'DAYS', 13, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_84_PLUS', NULL, NULL, 'Singapore Annual Leave - 8th year and later', TRUE, 80, 'DAYS', 14, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_SICK_03', NULL, NULL, 'Singapore Sick Leave - 3 months service', TRUE, 10, 'DAYS', 5, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:SICK_LEAVE', NULL),
    ('SG_SICK_04', NULL, NULL, 'Singapore Sick Leave - 4 months service', TRUE, 20, 'DAYS', 8, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:SICK_LEAVE', NULL),
    ('SG_SICK_05', NULL, NULL, 'Singapore Sick Leave - 5 months service', TRUE, 30, 'DAYS', 11, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:SICK_LEAVE', NULL),
    ('SG_SICK_06_PLUS', NULL, NULL, 'Singapore Sick Leave - 6 months service and later', TRUE, 40, 'DAYS', 14, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:SICK_LEAVE', NULL),
    ('SG_HOSP_03', NULL, NULL, 'Singapore Hospitalisation Leave - 3 months service', TRUE, 10, 'DAYS', 15, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:HOSPITALISATION_LEAVE', NULL),
    ('SG_HOSP_04', NULL, NULL, 'Singapore Hospitalisation Leave - 4 months service', TRUE, 20, 'DAYS', 30, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:HOSPITALISATION_LEAVE', NULL),
    ('SG_HOSP_05', NULL, NULL, 'Singapore Hospitalisation Leave - 5 months service', TRUE, 30, 'DAYS', 45, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:HOSPITALISATION_LEAVE', NULL),
    ('SG_HOSP_06_PLUS', NULL, NULL, 'Singapore Hospitalisation Leave - 6 months service and later', TRUE, 40, 'DAYS', 60, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2026-08-17', NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:HOSPITALISATION_LEAVE', NULL);

-- Annual leave: first year is months 3-11, second year starts at month 12,
-- and the statutory cap of 14 days applies from the 8th year onward.
INSERT INTO leave_entitlement_policy_eligibility (
    id, policy_id, criterion_type, operator, criterion_value, active, sort_order
) VALUES
    ('SG_ANNUAL_03_11_MIN', 'SG_ANNUAL_03_11', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '3', TRUE, 10),
    ('SG_ANNUAL_03_11_MAX', 'SG_ANNUAL_03_11', 'SERVICE_MONTHS', 'LESS_THAN', '12', TRUE, 20),
    ('SG_ANNUAL_12_23_MIN', 'SG_ANNUAL_12_23', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '12', TRUE, 10),
    ('SG_ANNUAL_12_23_MAX', 'SG_ANNUAL_12_23', 'SERVICE_MONTHS', 'LESS_THAN', '24', TRUE, 20),
    ('SG_ANNUAL_24_35_MIN', 'SG_ANNUAL_24_35', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '24', TRUE, 10),
    ('SG_ANNUAL_24_35_MAX', 'SG_ANNUAL_24_35', 'SERVICE_MONTHS', 'LESS_THAN', '36', TRUE, 20),
    ('SG_ANNUAL_36_47_MIN', 'SG_ANNUAL_36_47', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '36', TRUE, 10),
    ('SG_ANNUAL_36_47_MAX', 'SG_ANNUAL_36_47', 'SERVICE_MONTHS', 'LESS_THAN', '48', TRUE, 20),
    ('SG_ANNUAL_48_59_MIN', 'SG_ANNUAL_48_59', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '48', TRUE, 10),
    ('SG_ANNUAL_48_59_MAX', 'SG_ANNUAL_48_59', 'SERVICE_MONTHS', 'LESS_THAN', '60', TRUE, 20),
    ('SG_ANNUAL_60_71_MIN', 'SG_ANNUAL_60_71', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '60', TRUE, 10),
    ('SG_ANNUAL_60_71_MAX', 'SG_ANNUAL_60_71', 'SERVICE_MONTHS', 'LESS_THAN', '72', TRUE, 20),
    ('SG_ANNUAL_72_83_MIN', 'SG_ANNUAL_72_83', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '72', TRUE, 10),
    ('SG_ANNUAL_72_83_MAX', 'SG_ANNUAL_72_83', 'SERVICE_MONTHS', 'LESS_THAN', '84', TRUE, 20),
    ('SG_ANNUAL_84_PLUS_MIN', 'SG_ANNUAL_84_PLUS', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '84', TRUE, 10),

    ('SG_SICK_03_MIN', 'SG_SICK_03', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '3', TRUE, 10),
    ('SG_SICK_03_MAX', 'SG_SICK_03', 'SERVICE_MONTHS', 'LESS_THAN', '4', TRUE, 20),
    ('SG_SICK_04_MIN', 'SG_SICK_04', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '4', TRUE, 10),
    ('SG_SICK_04_MAX', 'SG_SICK_04', 'SERVICE_MONTHS', 'LESS_THAN', '5', TRUE, 20),
    ('SG_SICK_05_MIN', 'SG_SICK_05', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '5', TRUE, 10),
    ('SG_SICK_05_MAX', 'SG_SICK_05', 'SERVICE_MONTHS', 'LESS_THAN', '6', TRUE, 20),
    ('SG_SICK_06_PLUS_MIN', 'SG_SICK_06_PLUS', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '6', TRUE, 10),

    ('SG_HOSP_03_MIN', 'SG_HOSP_03', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '3', TRUE, 10),
    ('SG_HOSP_03_MAX', 'SG_HOSP_03', 'SERVICE_MONTHS', 'LESS_THAN', '4', TRUE, 20),
    ('SG_HOSP_04_MIN', 'SG_HOSP_04', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '4', TRUE, 10),
    ('SG_HOSP_04_MAX', 'SG_HOSP_04', 'SERVICE_MONTHS', 'LESS_THAN', '5', TRUE, 20),
    ('SG_HOSP_05_MIN', 'SG_HOSP_05', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '5', TRUE, 10),
    ('SG_HOSP_05_MAX', 'SG_HOSP_05', 'SERVICE_MONTHS', 'LESS_THAN', '6', TRUE, 20),
    ('SG_HOSP_06_PLUS_MIN', 'SG_HOSP_06_PLUS', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '6', TRUE, 10);
