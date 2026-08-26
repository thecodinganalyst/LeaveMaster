# Australia statutory leave and 2026 public-holiday seed

Source review date: **26 August 2026**.

## Jurisdiction model

LeaveMaestro seeds `AU` as the country/federal jurisdiction and these children: `AU-ACT`, `AU-NSW`, `AU-NT`, `AU-QLD`, `AU-SA`, `AU-TAS`, `AU-VIC`, and `AU-WA`.

Federal leave types are inherited by state/territory jurisdictions. Long Service Leave is overridden at state/territory level so the tenant copy retains the relevant state authority as its source.

## Seeded statutory leave types

Federal catalogue:

- Annual Leave
- Personal / Carer's Leave
- Compassionate Leave
- Unpaid Parental Leave
- Community Service Leave
- Family and Domestic Violence Leave
- Long Service Leave

Primary federal source: Fair Work Ombudsman, https://www.fairwork.gov.au/leave

State/territory Long Service Leave leave-type records contain the applicable state or territory authority URL. They are catalogue/provenance records, not automated balance policies.

## Why Australian entitlement policies are intentionally not auto-seeded yet

Issue #348 explicitly requires LeaveMaestro not to invent simplified rules merely to fit the current schema. The current eligibility engine supports jurisdiction, service-month and dependant/event criteria, but does **not** yet model employee type (full-time, part-time, casual), ordinary weekly hours, shiftworker status, or award/agreement coverage.

That prevents safe automated balance policies for several NES entitlements:

- **Annual Leave:** full-time and part-time employees receive four weeks based on ordinary hours; casuals do not receive paid annual leave; qualifying shiftworkers can receive five weeks.
- **Personal / Carer's Leave:** full-time employees receive 10 days and part-time employees receive a pro-rata amount; casual employees instead have unpaid carer's leave rules.
- **Compassionate Leave:** two days per occasion; paid for full-time/part-time employees and unpaid for casuals. This is event-based, not an annual balance.
- **Unpaid Parental Leave:** eligibility includes 12 months service and additional regular/systematic casual requirements, and it is event-based.
- **Community Service Leave:** duration and payment vary by the qualifying community-service event, including jury service.
- **Family and Domestic Violence Leave:** all employees receive 10 paid days upfront, but the entitlement renews on the employee's work anniversary rather than a universal calendar leave year.
- **Long Service Leave:** state/territory legislation includes continuous-service accrual, access thresholds, pro-rata termination rules and exceptions that cannot be represented faithfully by a simple recurring annual entitlement.

Accordingly, this seed adds the statutory leave catalogue and source provenance but does not create Australian `leave_entitlement_policy` or eligibility-rule rows that could generate an incorrect balance. A future schema enhancement should add employment-type/hours/shiftworker criteria and entitlement-period semantics before these calculations are automated.

## 2026 public holidays

Source: Fair Work Ombudsman 2026 public holidays, https://www.fairwork.gov.au/employment-conditions/public-holidays/2026-public-holidays (page last updated 7 August 2026 when reviewed).

LeaveMaestro seeds one platform template for each state/territory for calendar year 2026. The seed is idempotent and replaces each seed-owned template's holiday list with the reviewed source data.

### Intentionally omitted holidays

The current holiday model stores a jurisdiction and a `LocalDate`; it cannot assign a holiday to a sub-state region or a portion of a day. To avoid applying a local/partial holiday to every employee in a state, these are intentionally omitted:

- Queensland: Royal Queensland Show (Brisbane area only); Christmas Eve partial-day holiday.
- Northern Territory: Christmas Eve and New Year's Eve partial-day holidays; regional show days.
- South Australia: Christmas Eve and New Year's Eve partial-day holidays.
- Tasmania: Royal Hobart Regatta, Royal Hobart Show and Recreation Day regional holidays; Easter Tuesday (generally Tasmanian Public Service only).
- Victoria: Melbourne Cup date because some regional areas observe the public holiday on a different date.
- Western Australia: King's Birthday because some regional areas observe it on a different date.

The remaining seed entries are state/territory-wide full-day holidays and applicable substitute/additional days listed by Fair Work Ombudsman.

## Updating future years

For a new year:

1. Review the Fair Work Ombudsman annual public-holiday page and the linked state/territory authorities.
2. Add a new year-specific platform template per state/territory; do not modify prior-year templates.
3. Exclude regional/capital-city-only or partial-day holidays until the data model can represent their scope accurately.
4. Add tests for representative jurisdiction differences, substitutes/additional holidays and duplicate prevention.
5. Run the full backend build and coverage gate before release.
