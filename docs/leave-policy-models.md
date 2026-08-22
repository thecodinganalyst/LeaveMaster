# Leave policy models

LeaveMaestro separates the way leave is represented from jurisdiction-specific eligibility rules. The core policy model is intentionally jurisdiction-agnostic so the same engine can support statutory and company leave across multiple jurisdictions.

## Policy models

| Model | Meaning | Annual balance |
| --- | --- | --- |
| `ANNUAL_ENTITLEMENT` | A conventional recurring entitlement for a leave year. It can be front-loaded or accrued and can use existing service/jurisdiction eligibility rules. | Yes |
| `CONDITIONAL_ANNUAL_ENTITLEMENT` | A recurring leave-year entitlement that is granted only when its eligibility rules match. | Yes, only when eligible |
| `EVENT_BASED` | Leave whose eligibility and duration come from a qualifying event or external period rather than from a recurring annual allowance. | No |

Existing policies are migrated to `ANNUAL_ENTITLEMENT`, preserving current behaviour.

## Event-based safety boundary

`EVENT_BASED` policies must not be converted into artificial large balances. They therefore cannot use recurring accrual, annual proration, or carry-forward. The annual entitlement generator recognizes these policies and returns `EVENT_BASED_NO_ANNUAL_BALANCE` without creating a `leave_entitlement` row.

The qualifying-event entitlement and request-validation workflow is a separate layer that can subsequently associate duration, evidence, and event-specific limits with the event. Until such a workflow is configured for a leave type, an event-based policy describes the correct domain semantics without pretending that an annual balance exists.

## Jurisdiction-neutral architecture

Core code must not branch on jurisdiction codes such as `SG`. Jurisdictions supply their entitlement amounts, thresholds, rule combinations, and qualifying-event semantics through templates/configuration built on the generic models.

Singapore is the first reference jurisdiction for the richer models. Likely mappings include:

- Childcare / Extended Childcare Leave: `CONDITIONAL_ANNUAL_ENTITLEMENT`
- Maternity / Paternity / Shared Parental / Adoption Leave: `EVENT_BASED`
- National Service Leave: `EVENT_BASED`

Other jurisdictions can use the same models for equivalent parental, adoption, military/reservist, jury-duty, or other event-driven leave without adding a new core policy model solely because the jurisdiction differs.

## Compatibility

Annual and sick-leave policies continue through the existing entitlement-generation path. Conditional annual policies also use that path after normal policy eligibility resolution. This keeps existing policy resolution and generated-balance behaviour backward compatible while creating an explicit boundary for future dependent- and event-based eligibility capabilities.
