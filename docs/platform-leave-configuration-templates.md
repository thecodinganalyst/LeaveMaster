# Platform leave configuration templates

LeaveMaestro uses jurisdiction-scoped platform templates to give newly created tenants a useful leave configuration without giving Platform Admin direct control over tenant-owned HR configuration.

## Scope model

Records that support seeding explicitly distinguish platform templates from tenant-owned configuration.

| Scope | `tenant_id` | `jurisdiction_id` | Managed by |
|---|---|---|---|
| `PLATFORM_TEMPLATE` | `NULL` | Required for jurisdiction-specific templates | Platform Admin |
| `TENANT` | Required | Not stored on the copied tenant record | Tenant Admin / authorized Leave Admin |

The database and application validate these invariants. A null tenant ID alone is not enough to identify a valid platform template.

Jurisdiction leave types remain the platform catalogue for leave-type definitions. Tenant leave types copied from that catalogue keep `source_jurisdiction_leave_type_id` for traceability. Entitlement policy templates use `jurisdiction_leave_type_id`; tenant policy copies use a tenant `leave_type_id` and retain `source_template_id`.

Eligibility rules inherit their ownership boundary from the entitlement policy they belong to and are copied during tenant provisioning. Platform public holidays are maintained through the dedicated **Public Holidays** resource; tenant leave calendars remain tenant-owned configuration and retain template lineage when provisioned.

See [Platform public holidays](public-holidays.md) for the dedicated API, permissions and admin UI.

## Tenant creation flow

1. Platform Admin creates a tenant and selects a valid jurisdiction.
2. The backend resolves effective jurisdiction leave types, including parent-jurisdiction inheritance.
3. Tenant leave types are created from the effective jurisdiction leave-type catalogue.
4. Effective platform entitlement policy templates are resolved for the jurisdiction hierarchy.
5. Each applicable policy is copied into a tenant-scoped policy and remapped to the new tenant leave type.
6. Eligibility rules are copied and remapped to the new tenant policy ID.
7. Selected jurisdiction public-holiday data is copied into tenant calendars.

Provisioning uses source IDs to make retries idempotent: retrying provisioning does not create duplicate policy or calendar copies.

## Jurisdiction inheritance

Template resolution starts at the tenant's selected jurisdiction and walks toward its parent jurisdictions. More-specific records win over inherited records for the same logical configuration.

For leave types, the jurisdiction leave-type code is the override key. For entitlement policy templates, the logical key combines the jurisdiction leave-type code and policy name.

## Singapore default entitlement templates

Singapore (`SG`) contains a mix of statutory-safe templates and LeaveMaster company defaults. The distinction matters: company defaults are convenient starting configuration for a new tenant and are **not** represented as Ministry of Manpower statutory entitlements.

### Annual leave — company default

LeaveMaster's default Singapore annual leave policy is a configurable company benefit. It intentionally exceeds the statutory minimum and replaces the previous MOM-style 7-to-14-day seed progression.

Annual leave is granted upfront for the applicable leave year and uses `CALENDAR_DAYS` proration when an employee joins after the leave-calendar period begins. The service tier is selected from completed service at the entitlement calculation date.

| Completed service | Full-year entitlement |
|---|---:|
| < 2 years | 14 days |
| 2 to < 4 years | 16 days |
| 4 to < 6 years | 18 days |
| 6 to < 8 years | 20 days |
| 8 to < 10 years | 22 days |
| 10 years and later | 24 days |

The 24-day tier is the hard maximum. Service after 10 completed years does not increase the default entitlement further.

For a joiner, generation first resolves the applicable service tier and then prorates the tier amount against the configured entitlement period:

`prorated = full entitlement × eligible inclusive calendar days / total inclusive calendar days`

The result is rounded to **2 decimal places using `HALF_UP`**. If the employee was already employed on the first day of the leave period, no join-date proration is applied. The calculation uses the supplied/configured leave-period boundaries and therefore does not require a January-to-December calendar.

Singapore MOM annual-leave requirements remain an external legal baseline that tenants must consider when changing their configuration. The seeded LeaveMaster company default should not be interpreted as a statement of the statutory schedule.

### Compassionate, marriage and unpaid leave — company defaults

New Singapore tenants that provision leave configuration receive these additional non-statutory company defaults:

| Leave type | Default entitlement | Treatment |
|---|---:|---|
| Compassionate Leave | 2 days | Granted upfront annually |
| Marriage Leave | 2 days | Granted upfront annually |
| Unpaid Leave | 14 days | Annual allowance / limit |

These policies have no service-based increase by default. They are tenant-owned after provisioning and may be changed by an authorized tenant administrator.

### Sick and hospitalisation leave — statutory rule represented as separate balances

Singapore's statutory paid medical-leave rule uses an outpatient sick-leave limit inside a larger combined medical-leave maximum. The statutory progression is:

| Completed service | Outpatient sick-leave limit | Combined medical-leave maximum including outpatient sick leave |
|---|---:|---:|
| 3 to <4 months | 5 days | 15 days |
| 4 to <5 months | 8 days | 30 days |
| 5 to <6 months | 11 days | 45 days |
| 6 months and later | 14 days | 60 days |

LeaveMaster does not yet support a shared entitlement pool across leave types. To avoid presenting the outpatient and combined ceilings as additive balances, the Singapore seed deliberately stores the Hospitalisation Leave entitlement as only the **additional hospitalisation allowance after reserving the outpatient sick-leave component**:

| Completed service | Seeded Sick Leave | Seeded Hospitalisation Leave (additional) | Combined seeded maximum |
|---|---:|---:|---:|
| 3 to <4 months | 5 days | 10 days | 15 days |
| 4 to <5 months | 8 days | 22 days | 30 days |
| 5 to <6 months | 11 days | 34 days | 45 days |
| 6 months and later | 14 days | 46 days | 60 days |

This is an intentional implementation simplification, **not** a statement that Singapore law itself defines hospitalisation leave as 10, 22, 34 or 46 days. Under the statutory rule, the larger 15/30/45/60-day figure is the overall medical-leave maximum and includes outpatient sick leave already taken.

Operationally, LeaveMaster keeps Sick Leave and Hospitalisation Leave as independent balances whose sum equals that combined maximum. No shared-pool or cross-leave-type deduction logic is introduced by this representation.

AskLeaveMaestro and other explanatory surfaces should therefore distinguish the statutory rule from the storage representation. For a staff member with at least 6 months of service, an appropriate explanation is: the statutory maximum is 60 days of paid medical leave including up to 14 days of outpatient sick leave; LeaveMaster represents that as 14 days Sick Leave plus 46 additional days Hospitalisation Leave.

Source: Singapore Ministry of Manpower, [Sick leave eligibility and entitlement](https://www.mom.gov.sg/employment-practices/leave/sick-leave/eligibility-and-entitlement).

### Family leave not yet seeded as active policies

Singapore maternity, paternity, shared parental, childcare, extended childcare, unpaid infant care and adoption leave are intentionally **not seeded as active entitlement-policy templates yet**. This is a safety constraint, not an indication that these statutory leave types do not exist.

The current eligibility engine can evaluate only `JURISDICTION_CODE` and `SERVICE_MONTHS`. Family-leave schemes require additional employee/child facts that the current model cannot safely represent. Seeding partial rules could incorrectly grant statutory entitlements to ineligible employees.

Official references include Singapore MOM and the Government-Paid Leave portal for the applicable family-leave schemes.

## Authorization boundary

Platform Admin can manage platform templates and global jurisdiction catalogue data, including platform public holidays through `PUBLIC_HOLIDAY_READ` and `PUBLIC_HOLIDAY_WRITE`. Platform Admin does not receive general CRUD access to tenant-owned entitlement policies, eligibility rules, or leave calendars.

Tenant-scoped users with the appropriate permissions can manage records belonging to their own tenant and cannot use those APIs to alter platform templates or platform public-holiday seed data.

## Independence after provisioning

Tenant copies are snapshots of the defaults available when the tenant is provisioned. They are deliberately independent after creation:

- editing a tenant policy or calendar does not edit platform reference data;
- editing a platform template or public holiday affects future tenant provisioning only;
- existing tenant records are not silently overwritten when platform reference data changes;
- source IDs are retained for provenance and retry idempotency, not live synchronization.

A future explicit template synchronization feature would need its own conflict and tenant-customization rules; it is not part of the current provisioning model.

## Existing data migration

Existing tenant-owned policies are not silently rewritten by changes to platform defaults. The revised Singapore templates affect future provisioning and platform-template resolution. Existing tenants remain independent unless an explicit synchronization/migration feature is introduced.
