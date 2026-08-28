-- Indonesia statutory leave templates reviewed against official Indonesian labour sources on 2026-08-29.
-- Core leave rights are based on Law No. 13 of 2003 (as amended); maternity-related protections
-- reflect Law No. 4 of 2024 on Mother and Child Welfare.

INSERT INTO jurisdiction (id, code, name, jurisdiction_type, parent_id, country_code, subdivision_code, active)
SELECT 'ID', 'ID', 'Indonesia', 'COUNTRY', NULL, 'ID', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM jurisdiction WHERE id = 'ID');

INSERT INTO jurisdiction_leave_type
    (id, jurisdiction_id, code, name, description, statutory, paid, active, source_url, source_name, effective_from, effective_to)
SELECT v.id, 'ID', v.code, v.name, v.description, TRUE, TRUE, TRUE,
       v.source_url, v.source_name, v.effective_from, NULL
FROM (VALUES
    ('ID:ANNUAL_LEAVE', 'ANNUAL_LEAVE', 'Annual Leave', 'Minimum 12 working days after 12 months of continuous service.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:SICK_LEAVE', 'SICK_LEAVE', 'Sick Leave', 'Statutory sickness absence; LeaveMaster tracks absence only and does not calculate statutory wage progression.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:MENSTRUAL_LEAVE', 'MENSTRUAL_LEAVE', 'Menstrual Leave', 'Absence on the first and second day of menstruation when the worker is in pain and unable to work.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:MATERNITY_LEAVE', 'MATERNITY_LEAVE', 'Maternity Leave', 'At least the first three months of maternity leave under Law No. 4 of 2024.',
     'https://jdih.kemnaker.go.id/peraturan/detail/2501/undang-undang-nomor-4-tahun-2024', 'Undang-Undang Nomor 4 Tahun 2024 tentang Kesejahteraan Ibu dan Anak', DATE '2024-07-02'),
    ('ID:MATERNITY_EXTENSION_LEAVE', 'MATERNITY_EXTENSION_LEAVE', 'Maternity Leave Extension', 'Additional medically supported maternity leave, up to three further months for qualifying special conditions.',
     'https://jdih.kemnaker.go.id/peraturan/detail/2501/undang-undang-nomor-4-tahun-2024', 'Undang-Undang Nomor 4 Tahun 2024 tentang Kesejahteraan Ibu dan Anak', DATE '2024-07-02'),
    ('ID:MISCARRIAGE_LEAVE', 'MISCARRIAGE_LEAVE', 'Miscarriage Leave', '1.5 months or the period certified by a doctor, obstetrician or midwife.',
     'https://jdih.kemnaker.go.id/peraturan/detail/2501/undang-undang-nomor-4-tahun-2024', 'Undang-Undang Nomor 4 Tahun 2024 tentang Kesejahteraan Ibu dan Anak', DATE '2024-07-02'),
    ('ID:PATERNITY_LEAVE', 'PATERNITY_LEAVE', 'Birth Accompaniment Leave', 'Two days to accompany a wife during childbirth; additional days may be provided by agreement.',
     'https://jdih.kemnaker.go.id/peraturan/detail/2501/undang-undang-nomor-4-tahun-2024', 'Undang-Undang Nomor 4 Tahun 2024 tentang Kesejahteraan Ibu dan Anak', DATE '2024-07-02'),
    ('ID:MISCARRIAGE_ACCOMPANIMENT_LEAVE', 'MISCARRIAGE_ACCOMPANIMENT_LEAVE', 'Miscarriage Accompaniment Leave', 'Two days for a husband when his wife experiences miscarriage.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:MARRIAGE_LEAVE', 'MARRIAGE_LEAVE', 'Marriage Leave', 'Three days when the worker marries.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:CHILD_MARRIAGE_LEAVE', 'CHILD_MARRIAGE_LEAVE', 'Child Marriage Leave', 'Two days when the worker marries off a child.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:CHILD_CIRCUMCISION_LEAVE', 'CHILD_CIRCUMCISION_LEAVE', 'Child Circumcision Leave', 'Two days when the worker''s child is circumcised.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:CHILD_BAPTISM_LEAVE', 'CHILD_BAPTISM_LEAVE', 'Child Baptism Leave', 'Two days when the worker''s child is baptised.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:SPOUSE_BEREAVEMENT_LEAVE', 'SPOUSE_BEREAVEMENT_LEAVE', 'Spouse Bereavement Leave', 'Two days on death of a spouse.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:PARENT_BEREAVEMENT_LEAVE', 'PARENT_BEREAVEMENT_LEAVE', 'Parent Bereavement Leave', 'Two days on death of a parent.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:PARENT_IN_LAW_BEREAVEMENT_LEAVE', 'PARENT_IN_LAW_BEREAVEMENT_LEAVE', 'Parent-in-Law Bereavement Leave', 'Two days on death of a parent-in-law.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:CHILD_BEREAVEMENT_LEAVE', 'CHILD_BEREAVEMENT_LEAVE', 'Child Bereavement Leave', 'Two days on death of a child.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:CHILD_IN_LAW_BEREAVEMENT_LEAVE', 'CHILD_IN_LAW_BEREAVEMENT_LEAVE', 'Child-in-Law Bereavement Leave', 'Two days on death of a child-in-law.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25'),
    ('ID:HOUSEHOLD_BEREAVEMENT_LEAVE', 'HOUSEHOLD_BEREAVEMENT_LEAVE', 'Household Bereavement Leave', 'One day on death of a household member.',
     'https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003', 'Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan', DATE '2003-03-25')
) AS v(id, code, name, description, source_url, source_name, effective_from)
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
    ('ID_ANNUAL_12', NULL, NULL, 'Indonesia Annual Leave - 12 days after 12 months service', TRUE, 10,
     'DAYS', 12, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:ANNUAL_LEAVE', NULL, 'ANNUAL_ENTITLEMENT', NULL, FALSE, NULL, NULL, 'FIXED'),
    ('ID_MENSTRUAL_EVENT', NULL, NULL, 'Indonesia Menstrual Leave - first and second day', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:MENSTRUAL_LEAVE', NULL, 'EVENT_BASED', 'MENSTRUATION', FALSE, 0, 1, 'FIXED'),
    ('ID_MATERNITY_EVENT', NULL, NULL, 'Indonesia Maternity Leave - childbirth', TRUE, 10,
     'DAYS', 90, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2024-07-02', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:MATERNITY_LEAVE', NULL, 'EVENT_BASED', 'BIRTH', TRUE, 90, 180, 'APPROVED_EVENT_AMOUNT'),
    ('ID_MATERNITY_EXTENSION_EVENT', NULL, NULL, 'Indonesia Maternity Leave - medical extension', TRUE, 10,
     'DAYS', 90, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2024-07-02', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:MATERNITY_EXTENSION_LEAVE', NULL, 'EVENT_BASED', 'MATERNITY_MEDICAL_EXTENSION', TRUE, 0, 180, 'APPROVED_EVENT_AMOUNT'),
    ('ID_MISCARRIAGE_EVENT', NULL, NULL, 'Indonesia Miscarriage Leave', TRUE, 10,
     'DAYS', 45, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2024-07-02', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:MISCARRIAGE_LEAVE', NULL, 'EVENT_BASED', 'MISCARRIAGE', TRUE, 0, 90, 'APPROVED_EVENT_AMOUNT'),
    ('ID_PATERNITY_EVENT', NULL, NULL, 'Indonesia Birth Accompaniment Leave', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2024-07-02', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:PATERNITY_LEAVE', NULL, 'EVENT_BASED', 'BIRTH', TRUE, 0, 5, 'FIXED'),
    ('ID_MISCARRIAGE_ACCOMPANIMENT_EVENT', NULL, NULL, 'Indonesia Miscarriage Accompaniment Leave', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:MISCARRIAGE_ACCOMPANIMENT_LEAVE', NULL, 'EVENT_BASED', 'MISCARRIAGE', TRUE, 0, 5, 'FIXED'),
    ('ID_MARRIAGE_EVENT', NULL, NULL, 'Indonesia Marriage Leave', TRUE, 10,
     'DAYS', 3, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:MARRIAGE_LEAVE', NULL, 'EVENT_BASED', 'MARRIAGE', TRUE, 0, 30, 'FIXED'),
    ('ID_CHILD_MARRIAGE_EVENT', NULL, NULL, 'Indonesia Child Marriage Leave', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:CHILD_MARRIAGE_LEAVE', NULL, 'EVENT_BASED', 'CHILD_MARRIAGE', TRUE, 0, 30, 'FIXED'),
    ('ID_CHILD_CIRCUMCISION_EVENT', NULL, NULL, 'Indonesia Child Circumcision Leave', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:CHILD_CIRCUMCISION_LEAVE', NULL, 'EVENT_BASED', 'CHILD_CIRCUMCISION', TRUE, 0, 30, 'FIXED'),
    ('ID_CHILD_BAPTISM_EVENT', NULL, NULL, 'Indonesia Child Baptism Leave', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:CHILD_BAPTISM_LEAVE', NULL, 'EVENT_BASED', 'CHILD_BAPTISM', TRUE, 0, 30, 'FIXED'),
    ('ID_DEATH_SPOUSE_EVENT', NULL, NULL, 'Indonesia Spouse Bereavement Leave', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:SPOUSE_BEREAVEMENT_LEAVE', NULL, 'EVENT_BASED', 'DEATH_SPOUSE', TRUE, 0, 30, 'FIXED'),
    ('ID_DEATH_PARENT_EVENT', NULL, NULL, 'Indonesia Parent Bereavement Leave', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:PARENT_BEREAVEMENT_LEAVE', NULL, 'EVENT_BASED', 'DEATH_PARENT', TRUE, 0, 30, 'FIXED'),
    ('ID_DEATH_PARENT_IN_LAW_EVENT', NULL, NULL, 'Indonesia Parent-in-Law Bereavement Leave', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:PARENT_IN_LAW_BEREAVEMENT_LEAVE', NULL, 'EVENT_BASED', 'DEATH_PARENT_IN_LAW', TRUE, 0, 30, 'FIXED'),
    ('ID_DEATH_CHILD_EVENT', NULL, NULL, 'Indonesia Child Bereavement Leave', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:CHILD_BEREAVEMENT_LEAVE', NULL, 'EVENT_BASED', 'DEATH_CHILD', TRUE, 0, 30, 'FIXED'),
    ('ID_DEATH_CHILD_IN_LAW_EVENT', NULL, NULL, 'Indonesia Child-in-Law Bereavement Leave', TRUE, 10,
     'DAYS', 2, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:CHILD_IN_LAW_BEREAVEMENT_LEAVE', NULL, 'EVENT_BASED', 'DEATH_CHILD_IN_LAW', TRUE, 0, 30, 'FIXED'),
    ('ID_DEATH_HOUSEHOLD_EVENT', NULL, NULL, 'Indonesia Household Bereavement Leave', TRUE, 10,
     'DAYS', 1, 'NONE', NULL, 'NONE', FALSE, NULL, NULL, DATE '2003-03-25', NULL,
     'PLATFORM_TEMPLATE', 'ID', 'ID:HOUSEHOLD_BEREAVEMENT_LEAVE', NULL, 'EVENT_BASED', 'DEATH_HOUSEHOLD_MEMBER', TRUE, 0, 30, 'FIXED');

INSERT INTO leave_entitlement_policy_eligibility
    (id, policy_id, criterion_type, operator, criterion_value, active, sort_order)
VALUES
    ('ID_ANNUAL_12_MIN_SERVICE', 'ID_ANNUAL_12', 'SERVICE_MONTHS', 'GREATER_THAN_OR_EQUAL', '12', TRUE, 10);
