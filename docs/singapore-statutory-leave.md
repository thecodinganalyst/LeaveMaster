# Singapore statutory leave reference templates

Verified against official Singapore sources on **22 August 2026**. These templates are jurisdiction configuration layered on the generic conditional/event-based leave framework; the core entitlement engine does not branch on `SG`.

> Statutory rules can change. The source and effective dates below should be re-verified before changing the seeded templates.

## Seeded reference policies

| Scheme | LeaveMaster model | Reference entitlement | Key configured eligibility / handling |
| --- | --- | --- | --- |
| Childcare Leave (Singapore citizen child under 7) | Conditional annual | 6 days/year | At least 3 months' service; matching child under 7 and Singapore citizen; no carry-forward. The official scheme permits employer proration for employees with less than a year of service, subject to the statutory table/minimum, but the reference template does not force optional proration. |
| Childcare Leave (Employment Act path) | Conditional annual | 2 days/year | At least 3 months' service; matching child under 7; lower-priority fallback when the 6-day citizen-child policy does not match. The 2 days are not prorated. |
| Extended Childcare Leave | Conditional annual | 2 days/year | At least 3 months' service; youngest child is a Singapore citizen aged 7–12 inclusive. Not prorated. A parent with children in both childcare age bands remains subject to the statutory overall 6-day paid-childcare cap. |
| Unpaid Infant Care Leave | Conditional annual | 12 days/year | At least 3 months' service; Singapore citizen child below 2. The 12-day entitlement applies from 1 January 2024. |
| Maternity Leave | Event based | Up to 16 weeks | Birth event; verification required. The event stores an approved allocation, capped at 16 weeks, so HR can represent the applicable 16-week Government-Paid or 12-week Employment Act outcome after verifying statutory facts. |
| Paternity Leave | Event based | 4 weeks | Child-arrival event on/after 1 April 2025; at least 3 months' service; verification required. Weeks are converted using the employee's configured weekly work schedule. |
| Shared Parental Leave | Event based | Shared pool: 6 weeks for qualifying events 1 Apr 2025–31 Mar 2026; 10 weeks from 1 Apr 2026 | Verification required. The event stores the employee's approved share and the generic resolver caps it at the policy maximum, preventing the full shared pool from being granted independently to both parents. |
| Adoption Leave | Event based | 12 weeks | Adoption event; at least 3 months' service; verification required. Weeks are converted using the employee's configured weekly work schedule. |
| National Service / reservist leave | Event based | Verified call-up period | Military call-up event; verification required. There is no annual balance. The grant is derived from scheduled working days within the verified call-up start/end period and does not consume Annual Leave. |

## Generic eligibility primitives

`HAS_DEPENDANT_MATCHING` evaluates a single dependant against a reusable predicate at the relevant eligibility date. Supported predicate keys are `relationship`, `citizenship`, `residency`, `age_lt`, `age_lte`, `age_gt`, `age_gte`, and `youngest`.

Example:

```text
relationship=CHILD;citizenship=SG;age_lt=7
```

All facts in one predicate must match the same dependant. The primitive is jurisdiction-neutral; for example, `relationship=CHILD;citizenship=AU;age_lt=7` uses the same engine.

## Event entitlement amount modes

- `FIXED`: grant the configured amount. `WEEKS` are converted to day-equivalent leave using the employee's actual weekly work schedule.
- `APPROVED_EVENT_AMOUNT`: use a verified event-specific allocation, capped at the configured policy amount. This supports schemes such as Shared Parental Leave and maternity outcomes that cannot safely be inferred from the currently stored facts.
- `EVENT_PERIOD_WORKING_DAYS`: derive the grant from scheduled working days in the qualifying event period. This is used for NS/reservist call-ups.

Event-specific policies do not create conventional annual balances and cannot use annual accrual, proration, or carry-forward semantics.

## Provisioning

The H2 and PostgreSQL V31 migrations create platform templates (`tenant_id = NULL`) scoped to Singapore. Tenant provisioning copies the applicable policy, generic eligibility rules, event configuration, and amount mode into independent tenant snapshots. Adoption Leave is also part of the Singapore jurisdiction leave-type catalog.

## Administrative expectations

LeaveMaster only auto-resolves facts that can be represented safely from staff/dependant data. Verification-gated schemes require HR/admin review of the relevant evidence and event facts before the event entitlement becomes usable. For `APPROVED_EVENT_AMOUNT`, HR also records the approved number of weeks; the configured maximum cannot be exceeded.

For childcare, rerun entitlement generation when dependant facts or service eligibility changes so conditional annual balances can be reconciled. The template deliberately does not invent citizenship, parent-role, adoption, birth-order, or shared-parent allocation facts.

## Official sources

- Ministry of Manpower — Childcare leave: https://www.mom.gov.sg/employment-practices/leave/childcare-leave
- Ministry of Manpower — Childcare eligibility and entitlement: https://www.mom.gov.sg/employment-practices/leave/childcare-leave/eligibility-and-entitlement
- Ministry of Manpower — Pro-rated childcare leave: https://www.mom.gov.sg/employment-practices/leave/childcare-leave/pro-rated-childcare-leave
- Ministry of Manpower — Extended childcare leave: https://www.mom.gov.sg/employment-practices/leave/childcare-leave/extended-childcare-leave
- Ministry of Manpower — Unpaid infant care leave: https://www.mom.gov.sg/employment-practices/leave/unpaid-infant-care-leave
- Ministry of Manpower — Maternity leave eligibility and entitlement: https://www.mom.gov.sg/employment-practices/leave/maternity-leave/eligibility-and-entitlement
- Ministry of Manpower — Paternity leave: https://www.mom.gov.sg/employment-practices/leave/paternity-leave
- Ministry of Manpower — Shared parental leave: https://www.mom.gov.sg/employment-practices/leave/shared-parental-leave
- Ministry of Manpower — Adoption leave: https://www.mom.gov.sg/employment-practices/leave/adoption-leave
- NS Portal / MINDEF — NSmen service and Make-Up Pay guidance: https://www.ns.gov.sg/web/portal/nsmen/home/nstopics/make-up-pay
