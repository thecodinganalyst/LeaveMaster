# Flyway migration versioning

Flyway versioned migrations must have a unique version within each configured migration location.

When multiple feature branches add migrations concurrently, do not assume the next version used on the branch will still be available when the branch is merged. Before merging, compare the migration directory against the latest `main` branch and renumber any migration whose version now conflicts.

Do not renumber or rewrite a migration that may already have been applied in a deployed environment. Preserve that migration version and renumber the later, unapplied migration to the next unused version.

The August 2026 collision between the jurisdiction-agnostic policy-model migration and the Singapore leave-template migration was resolved by preserving `V26__add_leave_policy_model.sql` and moving the later Singapore template migration to `V29__revise_singapore_leave_templates.sql`, after existing V27 and V28 migrations.
