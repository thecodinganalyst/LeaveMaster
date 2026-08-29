-- Issue #429: Singapore company-default leave semantics.
-- Compassionate and marriage leave are event-based allowances; generic unpaid leave is request-based.
-- Historical generated entitlements and leave applications are intentionally preserved.

UPDATE leave_entitlement_policy
SET policy_model = 'EVENT_BASED',
    qualifying_event_type_code = 'BEREAVEMENT',
    event_requires_verification = FALSE,
    event_validity_days_before = 0,
    event_validity_days_after = 30,
    event_entitlement_amount_mode = 'FIXED',
    accrual_method = 'NONE',
    accrual_rate = NULL,
    proration_method = 'NONE',
    carry_forward_allowed = FALSE,
    carry_forward_limit = NULL,
    carry_forward_expiry_months = NULL
WHERE id = 'SG_COMPASSIONATE_DEFAULT'
   OR source_template_id = 'SG_COMPASSIONATE_DEFAULT';

UPDATE leave_entitlement_policy
SET policy_model = 'EVENT_BASED',
    qualifying_event_type_code = 'MARRIAGE',
    event_requires_verification = FALSE,
    event_validity_days_before = 0,
    event_validity_days_after = 30,
    event_entitlement_amount_mode = 'FIXED',
    accrual_method = 'NONE',
    accrual_rate = NULL,
    proration_method = 'NONE',
    carry_forward_allowed = FALSE,
    carry_forward_limit = NULL,
    carry_forward_expiry_months = NULL
WHERE id = 'SG_MARRIAGE_DEFAULT'
   OR source_template_id = 'SG_MARRIAGE_DEFAULT';

UPDATE leave_entitlement_policy
SET policy_model = 'REQUEST_BASED',
    entitlement_amount = 0,
    qualifying_event_type_code = NULL,
    event_requires_verification = FALSE,
    event_validity_days_before = NULL,
    event_validity_days_after = NULL,
    event_entitlement_amount_mode = 'FIXED',
    accrual_method = 'NONE',
    accrual_rate = NULL,
    proration_method = 'NONE',
    carry_forward_allowed = FALSE,
    carry_forward_limit = NULL,
    carry_forward_expiry_months = NULL
WHERE id = 'SG_UNPAID_DEFAULT'
   OR source_template_id = 'SG_UNPAID_DEFAULT';
