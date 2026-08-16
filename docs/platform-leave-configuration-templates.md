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

Calendar templates use the same scope model and copied tenant calendars retain `source_template_id`.

Eligibility rules inherit their ownership boundary from the entitlement policy they belong to; they are copied to a newly created tenant policy during provisioning.

## Tenant creation flow

1. Platform Admin creates a tenant and selects a valid jurisdiction.
2. The tenant stores that jurisdiction as its provisioning jurisdiction.
3. The backend resolves effective jurisdiction leave types, including parent-jurisdiction inheritance.
4. Tenant leave types are created from the effective jurisdiction leave-type catalogue.
5. Effective platform entitlement policy templates are resolved for the jurisdiction hierarchy.
6. Each applicable policy is copied into a tenant-scoped policy and remapped to the new tenant leave type.
7. Eligibility rules are copied and remapped to the new tenant policy ID.
8. Effective jurisdiction calendar templates and public holidays are copied into tenant calendars.
9. The normal tenant-admin provisioning flow creates the tenant's administrative roles/user configuration.

Provisioning runs within tenant creation transaction boundaries. Template-copy checks use source IDs so retrying provisioning does not create duplicate policy or calendar copies.

## Jurisdiction inheritance

Template resolution starts at the tenant's selected jurisdiction and walks toward its parent jurisdictions. More-specific records win over inherited records for the same logical configuration.

For leave types, the existing jurisdiction leave-type resolution uses the leave-type code as the override key.

For entitlement policy templates, the logical key combines the jurisdiction leave-type code and policy name. For calendars, the date range is the logical override key.

## Authorization boundary

Platform Admin can manage platform templates and global jurisdiction catalogue data. Platform Admin does not receive general CRUD access to tenant-owned entitlement policies, eligibility rules, or calendars.

Tenant-scoped users with the appropriate permissions can manage records belonging to their own tenant and cannot use those APIs to alter platform templates.

The service layer enforces the scope and tenant boundary even if a client submits conflicting `scope` or `tenantId` values.

## Independence after provisioning

Tenant copies are snapshots of the defaults available when the tenant is provisioned. They are deliberately independent after creation:

- editing a tenant policy does not edit the source template;
- editing a platform template affects future tenant provisioning only;
- existing tenant records are not silently overwritten when a platform template changes;
- source IDs are retained for provenance, not live synchronization.

A future explicit template synchronization feature would need its own conflict and tenant-customization rules; it is not part of the current provisioning model.

## Existing data migration

Existing tenants are assigned Singapore (`SG`) as the migration default because LeaveMaestro's pre-template seed data is Singapore-oriented. Existing leave calendars with a null tenant ID are converted to Singapore platform calendar templates. Administrators should review migrated jurisdiction assignments before relying on them for future provisioning in installations that already contain non-Singapore tenants.
