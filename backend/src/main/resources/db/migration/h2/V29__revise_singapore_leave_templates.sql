-- LeaveMaster Singapore company-default leave configuration.
-- Annual leave is granted upfront, prorated by inclusive calendar days for joiners,
-- and increases by 2 days for every 2 completed years of service up to 24 days.
-- Compassionate, marriage and unpaid leave are company defaults, not MOM statutory entitlements.

DELETE FROM leave_entitlement_policy_eligibility
WHERE policy_id IN (
    'SG_ANNUAL_03_11', 'SG_ANNUAL_12_23', 'SG_ANNUAL_24_35', 'SG_ANNUAL_36_47',
    'SG_ANNUAL_48_59', 'SG_ANNUAL_60_71', 'SG_ANNUAL_72_83', 'SG_ANNUAL_84_PLUS'
);

DELETE FROM leave_entitlement_policy
WHERE scope = 'PLATFORM_TEMPLATE'
  AND jurisdiction_id = 'SG'
  AND id IN (
      'SG_ANNUAL_03_11', 'SG_ANNUAL_12_23', 'SG_ANNUAL_24_35', 'SG_ANNUAL_36_47',
      'SG_ANNUAL_48_59', 'SG_ANNUAL_60_71', 'SG_ANNUAL_72_83', 'SG_ANNUAL_84_PLUS'
  );

INSERT INTO leave_entitlement_policy (
    id, tenant_id, leave_type_id, name, active, priority,
    entitlement_unit, entitlement_amount, accrual_method, accrual_rate,
    proration_method, carry_forward_allowed, carry_forward_limit,
    carry_forward_expiry_months, effective_from, effective_to,
    scope, jurisdiction_id, jurisdiction_leave_type_id, source_template_id
) VALUES
    ('SG_ANNUAL_00_23', NULL, NULL, 'Singapore Annual Leave - less than 2 years service', TRUE, 10, 'DAYS', 14, 'NONE', NULL, 'CALENDAR_DAYS', FALSE, NULL, NULL, NULL, NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_24_47', NULL, NULL, 'Singapore Annual Leave - 2 to less than 4 years service', TRUE, 20, 'DAYS', 16, 'NONE', NULL, 'CALENDAR_DAYS', FALSE, NULL, NULL, NULL, NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_48_71', NULL, NULL, 'Singapore Annual Leave - 4 to less than 6 years service', TRUE, 30, 'DAYS', 18, 'NONE', NULL, 'CALENDAR_DAYS', FALSE, NULL, NULL, NULL, NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_72_95', NULL, NULL, 'Singapore Annual Leave - 6 to less than 8 years service', TRUE, 40, 'DAYS', 20, 'NONE', NULL, 'CALENDAR_DAYS', FALSE, NULL, NULL, NULL, NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_96_119', NULL, NULL, 'Singapore Annual Leave - 8 to less than 10 years service', TRUE, 50, 'DAYS', 22, 'NONE', NULL, 'CALENDAR_DAYS', FALSE, NULL, NULL, NULL, NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_ANNUAL_120_PLUS', NULL, NULL, 'Singapore Annual Leave - 10 years service and later', TRUE, 60, 'DAYS', 24, 'NONE', NULL, 'CALENDAR_DAYS', FALSE, NULL, NULL, NULL, NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:ANNUAL_LEAVE', NULL),
    ('SG_COMPASSIONATE_DEFAULT', NULL, NULL, 'Singapore Compassionate Leave - company default', TRUE, 10, 'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, NULL, NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:COMPASSIONATE_LEAVE', NULL),
    ('SG_MARRIAGE_DEFAULT', NULL, NULL, 'Singapore Marriage Leave - company default', TRUE, 10, 'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, NULL, NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:MARRIAGE_LEAVE', NULL),
    ('SG_UNPAID_DEFAULT', NULL, NULL, 'Singapore Unpaid Leave - company default', TRUE, 10, 'DAYS', 14, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, NULL, NULL, 'PLATFORM_TEMPLATE', 'SG', 'SG:UNPAID_LEAVE', NULL);

INSERT INTO leave_entitlement_policy_eligibility (
    id, policy_id, criterion_type, operator, criterion_value, active, sort_order
) VALUES
    ('SG_ANNUAL_00_23_MAX', 'SG_ANNUAL_00_23', 'SERVICE_MONTHS', 'LESS_THAN', '24', TRUE, 10),
    ('SG_ANNUAL_24_47_MIN', 'SG_ANNUAL_24_47', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '24', TRUE, 10),
    ('SG_ANNUAL_24_47_MAX', 'SG_ANNUAL_24_47', 'SERVICE_MONTHS', 'LESS_THAN', '48', TRUE, 20),
    ('SG_ANNUAL_48_71_MIN', 'SG_ANNUAL_48_71', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '48', TRUE, 10),
    ('SG_ANNUAL_48_71_MAX', 'SG_ANNUAL_48_71', 'SERVICE_MONTHS', 'LESS_THAN', '72', TRUE, 20),
    ('SG_ANNUAL_72_95_MIN', 'SG_ANNUAL_72_95', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '72', TRUE, 10),
    ('SG_ANNUAL_72_95_MAX', 'SG_ANNUAL_72_95', 'SERVICE_MONTHS', 'LESS_THAN', '96', TRUE, 20),
    ('SG_ANNUAL_96_119_MIN', 'SG_ANNUAL_96_119', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '96', TRUE, 10),
    ('SG_ANNUAL_96_119_MAX', 'SG_ANNUAL_96_119', 'SERVICE_MONTHS', 'LESS_THAN', '120', TRUE, 20),
    ('SG_ANNUAL_120_PLUS_MIN', 'SG_ANNUAL_120_PLUS', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '120', TRUE, 10);
