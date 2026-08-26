-- Malaysia statutory entitlement templates reviewed against official labour sources on 2026-08-26.
-- 2025-05-01 is used as the conservative nationwide effective date because the Sabah and
-- Sarawak labour ordinance amendments aligning these core minimums took effect on that date.

INSERT INTO jurisdiction (id, code, name, jurisdiction_type, parent_id, country_code, subdivision_code, active)
SELECT 'MY', 'MY', 'Malaysia', 'COUNTRY', NULL, 'MY', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM jurisdiction WHERE id = 'MY');

INSERT INTO jurisdiction (id, code, name, jurisdiction_type, parent_id, country_code, subdivision_code, active)
SELECT v.id, v.id, v.name, v.jurisdiction_type, 'MY', 'MY', v.id, TRUE
FROM (VALUES
    ('MY-JHR', 'Johor', 'STATE'), ('MY-KDH', 'Kedah', 'STATE'), ('MY-KTN', 'Kelantan', 'STATE'),
    ('MY-MLK', 'Melaka', 'STATE'), ('MY-NSN', 'Negeri Sembilan', 'STATE'), ('MY-PHG', 'Pahang', 'STATE'),
    ('MY-PNG', 'Pulau Pinang', 'STATE'), ('MY-PRK', 'Perak', 'STATE'), ('MY-PLS', 'Perlis', 'STATE'),
    ('MY-SBH', 'Sabah', 'STATE'), ('MY-SWK', 'Sarawak', 'STATE'), ('MY-SGR', 'Selangor', 'STATE'),
    ('MY-TRG', 'Terengganu', 'STATE'), ('MY-KUL', 'Kuala Lumpur', 'TERRITORY'),
    ('MY-LBN', 'Labuan', 'TERRITORY'), ('MY-PJY', 'Putrajaya', 'TERRITORY')
) AS v(id, name, jurisdiction_type)
WHERE NOT EXISTS (SELECT 1 FROM jurisdiction j WHERE j.id = v.id);

INSERT INTO jurisdiction_leave_type
    (id, jurisdiction_id, code, name, description, statutory, paid, active, source_url, source_name, effective_from, effective_to)
SELECT v.id, 'MY', v.code, v.name, NULL, TRUE, TRUE, TRUE,
       'https://jtksm.mohr.gov.my/sites/default/files/2023-11/Akta%20Kerja%201955%20%28Akta%20265%29.pdf',
       'Jabatan Tenaga Kerja Semenanjung Malaysia', DATE '2025-05-01', NULL
FROM (VALUES
    ('MY:ANNUAL_LEAVE', 'ANNUAL_LEAVE', 'Annual Leave'),
    ('MY:SICK_LEAVE', 'SICK_LEAVE', 'Sick Leave'),
    ('MY:HOSPITALISATION_LEAVE', 'HOSPITALISATION_LEAVE', 'Hospitalisation Leave'),
    ('MY:MATERNITY_LEAVE', 'MATERNITY_LEAVE', 'Maternity Leave'),
    ('MY:PATERNITY_LEAVE', 'PATERNITY_LEAVE', 'Paternity Leave')
) AS v(id, code, name)
WHERE NOT EXISTS (SELECT 1 FROM jurisdiction_leave_type t WHERE t.id = v.id);

INSERT INTO leave_entitlement_policy (
    id, tenant_id, leave_type_id, name, active, priority,
    entitlement_unit, entitlement_amount, accrual_method, accrual_rate,
    proration_method, carry_forward_allowed, carry_forward_limit,
    carry_forward_expiry_months, effective_from, effective_to,
    scope, jurisdiction_id, jurisdiction_leave_type_id, source_template_id,
    policy_model, qualifying_event_type_code, event_requires_verification,
    event_validity_days_before, event_validity_days_after, event_entitlement_amount_mode
) VALUES
    ('MY_ANNUAL_12_23', NULL, NULL, 'Malaysia Annual Leave - 1 to under 2 years service', TRUE, 10,
     'DAYS', 8, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2025-05-01', NULL,
     'PLATFORM_TEMPLATE', 'MY', 'MY:ANNUAL_LEAVE', NULL,
     'ANNUAL_ENTITLEMENT', NULL, FALSE, NULL, NULL, 'FIXED'),
    ('MY_ANNUAL_24_59', NULL, NULL, 'Malaysia Annual Leave - 2 to under 5 years service', TRUE, 20,
     'DAYS', 12, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2025-05-01', NULL,
     'PLATFORM_TEMPLATE', 'MY', 'MY:ANNUAL_LEAVE', NULL,
     'ANNUAL_ENTITLEMENT', NULL, FALSE, NULL, NULL, 'FIXED'),
    ('MY_ANNUAL_60_PLUS', NULL, NULL, 'Malaysia Annual Leave - 5 years service and above', TRUE, 30,
     'DAYS', 16, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2025-05-01', NULL,
     'PLATFORM_TEMPLATE', 'MY', 'MY:ANNUAL_LEAVE', NULL,
     'ANNUAL_ENTITLEMENT', NULL, FALSE, NULL, NULL, 'FIXED'),
    ('MY_SICK_LT24', NULL, NULL, 'Malaysia Sick Leave - under 2 years service', TRUE, 10,
     'DAYS', 14, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2025-05-01', NULL,
     'PLATFORM_TEMPLATE', 'MY', 'MY:SICK_LEAVE', NULL,
     'ANNUAL_ENTITLEMENT', NULL, FALSE, NULL, NULL, 'FIXED'),
    ('MY_SICK_24_59', NULL, NULL, 'Malaysia Sick Leave - 2 to under 5 years service', TRUE, 20,
     'DAYS', 18, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2025-05-01', NULL,
     'PLATFORM_TEMPLATE', 'MY', 'MY:SICK_LEAVE', NULL,
     'ANNUAL_ENTITLEMENT', NULL, FALSE, NULL, NULL, 'FIXED'),
    ('MY_SICK_60_PLUS', NULL, NULL, 'Malaysia Sick Leave - 5 years service and above', TRUE, 30,
     'DAYS', 22, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2025-05-01', NULL,
     'PLATFORM_TEMPLATE', 'MY', 'MY:SICK_LEAVE', NULL,
     'ANNUAL_ENTITLEMENT', NULL, FALSE, NULL, NULL, 'FIXED'),
    ('MY_HOSPITALISATION', NULL, NULL, 'Malaysia Hospitalisation Leave', TRUE, 10,
     'DAYS', 60, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2025-05-01', NULL,
     'PLATFORM_TEMPLATE', 'MY', 'MY:HOSPITALISATION_LEAVE', NULL,
     'ANNUAL_ENTITLEMENT', NULL, FALSE, NULL, NULL, 'FIXED'),
    ('MY_MATERNITY_EVENT', NULL, NULL, 'Malaysia Maternity Leave - verified confinement', TRUE, 10,
     'DAYS', 98, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2025-05-01', NULL,
     'PLATFORM_TEMPLATE', 'MY', 'MY:MATERNITY_LEAVE', NULL,
     'EVENT_BASED', 'BIRTH', TRUE, 30, 365, 'FIXED'),
    ('MY_PATERNITY_EVENT', NULL, NULL, 'Malaysia Paternity Leave - verified confinement', TRUE, 10,
     'DAYS', 7, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2025-05-01', NULL,
     'PLATFORM_TEMPLATE', 'MY', 'MY:PATERNITY_LEAVE', NULL,
     'EVENT_BASED', 'BIRTH', TRUE, 0, 30, 'FIXED');

INSERT INTO leave_entitlement_policy_eligibility
    (id, policy_id, criterion_type, operator, criterion_value, active, sort_order)
VALUES
    ('MY_ANNUAL_12_23_MIN', 'MY_ANNUAL_12_23', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '12', TRUE, 10),
    ('MY_ANNUAL_12_23_MAX', 'MY_ANNUAL_12_23', 'SERVICE_MONTHS', 'LESS_THAN', '24', TRUE, 20),
    ('MY_ANNUAL_24_59_MIN', 'MY_ANNUAL_24_59', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '24', TRUE, 10),
    ('MY_ANNUAL_24_59_MAX', 'MY_ANNUAL_24_59', 'SERVICE_MONTHS', 'LESS_THAN', '60', TRUE, 20),
    ('MY_ANNUAL_60_PLUS_MIN', 'MY_ANNUAL_60_PLUS', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '60', TRUE, 10),
    ('MY_SICK_LT24_MAX', 'MY_SICK_LT24', 'SERVICE_MONTHS', 'LESS_THAN', '24', TRUE, 10),
    ('MY_SICK_24_59_MIN', 'MY_SICK_24_59', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '24', TRUE, 10),
    ('MY_SICK_24_59_MAX', 'MY_SICK_24_59', 'SERVICE_MONTHS', 'LESS_THAN', '60', TRUE, 20),
    ('MY_SICK_60_PLUS_MIN', 'MY_SICK_60_PLUS', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '60', TRUE, 10),
    ('MY_PATERNITY_SERVICE', 'MY_PATERNITY_EVENT', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '12', TRUE, 10);
