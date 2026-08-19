-- Platform holiday calendar templates for Singapore.
-- Source: Singapore Ministry of Manpower public holiday announcements for 2026 and 2027.
-- The seed is intentionally platform-scoped only; tenant provisioning is handled separately.

INSERT INTO leave_calendar (id, start_date, end_date, tenant_id, scope, jurisdiction_id, source_template_id)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-01-01', DATE '2026-12-31', NULL, 'PLATFORM_TEMPLATE', 'SG', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM leave_calendar WHERE id = 'template:SG:2026-01-01_2026-12-31'
);

INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-01-01', 'New Year''s Day'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-01-01' AND holiday_name = 'New Year''s Day');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-02-17', 'Chinese New Year'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-02-17' AND holiday_name = 'Chinese New Year');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-02-18', 'Chinese New Year'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-02-18' AND holiday_name = 'Chinese New Year');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-03-21', 'Hari Raya Puasa'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-03-21' AND holiday_name = 'Hari Raya Puasa');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-04-03', 'Good Friday'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-04-03' AND holiday_name = 'Good Friday');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-05-01', 'Labour Day'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-05-01' AND holiday_name = 'Labour Day');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-05-27', 'Hari Raya Haji'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-05-27' AND holiday_name = 'Hari Raya Haji');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-05-31', 'Vesak Day'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-05-31' AND holiday_name = 'Vesak Day');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-08-09', 'National Day'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-08-09' AND holiday_name = 'National Day');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-11-08', 'Deepavali'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-11-08' AND holiday_name = 'Deepavali');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2026-01-01_2026-12-31', DATE '2026-12-25', 'Christmas Day'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2026-01-01_2026-12-31' AND holiday_date = DATE '2026-12-25' AND holiday_name = 'Christmas Day');

INSERT INTO leave_calendar (id, start_date, end_date, tenant_id, scope, jurisdiction_id, source_template_id)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-01-01', DATE '2027-12-31', NULL, 'PLATFORM_TEMPLATE', 'SG', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM leave_calendar WHERE id = 'template:SG:2027-01-01_2027-12-31'
);

INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-01-01', 'New Year''s Day'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-01-01' AND holiday_name = 'New Year''s Day');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-02-06', 'Chinese New Year'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-02-06' AND holiday_name = 'Chinese New Year');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-02-07', 'Chinese New Year'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-02-07' AND holiday_name = 'Chinese New Year');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-03-10', 'Hari Raya Puasa'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-03-10' AND holiday_name = 'Hari Raya Puasa');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-03-26', 'Good Friday'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-03-26' AND holiday_name = 'Good Friday');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-05-01', 'Labour Day'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-05-01' AND holiday_name = 'Labour Day');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-05-17', 'Hari Raya Haji'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-05-17' AND holiday_name = 'Hari Raya Haji');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-05-20', 'Vesak Day'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-05-20' AND holiday_name = 'Vesak Day');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-08-09', 'National Day'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-08-09' AND holiday_name = 'National Day');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-10-28', 'Deepavali'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-10-28' AND holiday_name = 'Deepavali');
INSERT INTO public_holiday (leave_calendar_id, holiday_date, holiday_name)
SELECT 'template:SG:2027-01-01_2027-12-31', DATE '2027-12-25', 'Christmas Day'
WHERE NOT EXISTS (SELECT 1 FROM public_holiday WHERE leave_calendar_id = 'template:SG:2027-01-01_2027-12-31' AND holiday_date = DATE '2027-12-25' AND holiday_name = 'Christmas Day');
