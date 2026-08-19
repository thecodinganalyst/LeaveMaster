# Platform leave configuration templates

LeaveMaestro uses jurisdiction-scoped platform templates to give newly created tenants a useful leave configuration without giving Platform Admin direct control over tenant-owned HR configuration.

## Scope model

Records that support seeding explicitly distinguish platform templates from tenant-owned configuration.

| Scope | `tenant_id` | `jurisdiction_id` | Managed by |
|---|---|---|---|
| `PLATFORM_TEMPLATE` | `NULL` | Required for jurisdiction-specific templates | Platform Admin |
| `TENANT` | Required | Not stored on the copied tenant record | Tenant Admin / authorized Leave Admin |

The database and application validate these invariants. A null tenant ID alone is not treated as enough information to identify a valid platform template.

Jurisdiction leave types remain the platform catalogue for leave-type definitions. Tenant leave types copied from that catalogue keep `source_jurisdiction_leave_type_id` for traceability.

Entitlement policy templates use `jurisdiction_leave_type_id` rather than a tenant leave-type ID. Tenant policy copies use a tenant `leave_type_id` and retain `source_template_id`.

Platform public holidays are maintained through the dedicated **Public Holidays** resource. The existing platform-template calendar rows remain an internal persistence/provisioning backing store for that jurisdiction/year holiday data; they are not exposed to Platform Admin as normal leave calendars. Tenant leave calendars remain tenant-owned configuration and retain `source_template_id` when provisioned from platform defaults.

Eligibility rules inherit their ownership boundary from the entitlement policy they belong to; they are copied to a newly created tenant policy during provisioning.

See [Platform public holidays](public-holidays.md) for the dedicated API, permissions and admin UI.

## Tenant creation flow

1. Platform Admin creates a tenant and selects a valid jurisdiction.
2. The tenant stores that jurisdiction as its provisioning jurisdiction.
3. The backend resolves effective jurisdiction leave types, including parent-jurisdiction inheritance.
4. Tenant leave types are created from the effective jurisdiction leave-type catalogue.
5. Effective platform entitlement policy templates are resolved for the jurisdiction hierarchy.
6. Each applicable policy is copied into a tenant-scoped policy and remapped to the new tenant leave type.
7. Eligibility rules are copied and remapped to the new tenant policy ID.
8. Effective jurisdiction public-holiday data is copied into tenant calendars.
9. The normal tenant-admin provisioning flow creates the tenant's administrative roles/user configuration.

Provisioning runs within tenant creation transaction boundaries. Template-copy checks use source IDs so retrying provisioning does not create duplicate policy or calendar copies.

## Jurisdiction inheritance

Template resolution starts at the tenant's selected jurisdiction and walks toward its parent jurisdictions. More-specific records win over inherited records for the same logical configuration.

For leave types, the existing jurisdiction leave-type resolution uses the leave-type code as the override key.

For entitlement policy templates, the logical key combines the jurisdiction leave-type code and policy name. For holiday/calendar provisioning, the applicable jurisdiction/year template data supplies the public holidays copied into the tenant calendar.

## Singapore statutory entitlement templates

The platform seeds active Singapore (`SG`) entitlement templates only where the statutory eligibility can be represented safely by the current eligibility engine.

### Annual leave

Singapore annual leave is seeded as eight service bands using `SERVICE_MONTHS` eligibility rules:

| Completed service | Entitlement |
|---|---:|
| 3 to <12 months | 7 days |
| 12 to <24 months | 8 days |
| 24 to <36 months | 9 days |
| 36 to <48 months | 10 days |
| 48 to <60 months | 11 days |
| 60 to <72 months | 12 days |
| 72 to <84 months | 13 days |
| 84 months and later | 14 days |

Source: Singapore Ministry of Manpower, [Annual leave eligibility and entitlement](https://www.mom.gov.sg/employment-practices/leave/annual-leave/eligibility-and-entitlement).

### Sick and hospitalisation leave

The statutory service progression is represented with separate outpatient sick-leave and hospitalisation-leave templates:

| Completed service | Outpatient sick leave | Hospitalisation leave |
|---|---:|---:|
| 3 to <4 months | 5 days | 15 days |
| 4 to <5 months | 8 days | 30 days |
| 5 to <6 months | 11 days | 45 days |
| 6 months and later | 14 days | 60 days |

The hospitalisation limit includes outpatient sick leave already taken. For example, an employee who has used all 14 outpatient sick-leave days has at most 46 additional hospitalisation-leave days from the 60-day combined statutory limit. LeaveMaestro currently stores the two entitlement ceilings separately, so downstream entitlement/balance generation must not treat 14 + 60 as an additive 74-day statutory allowance.

Source: Singapore Ministry of Manpower, [Sick leave eligibility and entitlement](https://www.mom.gov.sg/employment-practices/leave/sick-leave/eligibility-and-entitlement).

### Family leave not yet seeded as active policies

Singapore maternity, paternity, shared parental, childcare, extended childcare, unpaid infant care and adoption leave are intentionally **not seeded as active entitlement-policy templates yet**. This is a safety constraint, not an indication that these statutory leave types do not exist.

The current eligibility engine can evaluate only:

- `LOCATION_ID`
- `JURISDICTION_CODE`
- `SERVICE_MONTHS`

Family-leave schemes require additional facts such as parent role/sex, child age, child Singapore citizenship, marital or parental relationship, child date of birth, adoption/Formal Intent to Adopt dates and, for Shared Parental Leave, the employee's allocated share. Seeding only `SERVICE_MONTHS >= 3` would cause tenant provisioning to create policies that can match employees who are not legally eligible.

As of 17 August 2026, examples of current statutory entitlements include 16 weeks of Government-Paid Maternity Leave, 4 weeks of Government-Paid Paternity Leave for qualifying births/adoptions from 1 April 2025, up to 10 weeks of Shared Parental Leave for qualifying births/Formal Intent to Adopt dates from 1 April 2026, 6 days of Government-Paid Childcare Leave, 2 days of Extended Childcare Leave, 12 days of Unpaid Infant Care Leave, and 12 weeks of Adoption Leave. These schemes must be modelled only after the employee/child eligibility data and rules are available.

Official references:

- [Government-Paid Maternity Leave](https://www.profamilyleave.msf.gov.sg/schemes/maternity-leave/)
- [Government-Paid Paternity Leave](https://www.profamilyleave.msf.gov.sg/schemes/paternity-leave/)
- [Shared Parental Leave](https://www.profamilyleave.msf.gov.sg/schemes/shared-parental-leave/)
- [Childcare and Extended Childcare Leave](https://www.profamilyleave.msf.gov.sg/schemes/childcare-leave/)
- [Support for adoptive parents](https://www.profamilyleave.msf.gov.sg/adoptive-parents)
- [Adoption Leave](https://www.profamilyleave.msf.gov.sg/schemes/adoption-leave/)

## Authorization boundary

Platform Admin can manage platform templates and global jurisdiction catalogue data, including platform public holidays through `PUBLIC_HOLIDAY_READ` and `PUBLIC_HOLIDAY_WRITE`. Platform Admin does not receive general CRUD access to tenant-owned entitlement policies, eligibility rules, or leave calendars.

Tenant-scoped users with the appropriate permissions can manage records belonging to their own tenant and cannot use those APIs to alter platform templates or platform public-holiday seed data.

The service layer and security rules enforce the scope and tenant boundary even if a client submits conflicting values.

## Independence after provisioning

Tenant copies are snapshots of the defaults available when the tenant is provisioned. They are deliberately independent after creation:

- editing a tenant policy or calendar does not edit platform reference data;
- editing a platform template or public holiday affects future tenant provisioning only;
- existing tenant records are not silently overwritten when platform reference data changes;
- source IDs are retained for provenance, not live synchronization.

A future explicit template synchronization feature would need its own conflict and tenant-customization rules; it is not part of the current provisioning model.

## Existing data migration

Existing tenants are assigned Singapore (`SG`) as the migration default because LeaveMaestro's pre-template seed data is Singapore-oriented. Existing null-tenant calendar rows that hold seeded jurisdiction holidays are treated as platform backing templates for provisioning/reference data. Platform Admin manages their individual holiday entries through the dedicated **Public Holidays** resource rather than through tenant **Leave Calendars**.
