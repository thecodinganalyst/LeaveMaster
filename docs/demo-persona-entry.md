# Public demo persona entry

LeaveMaestro provides a frictionless public demo entry flow for the configured `DEMO` tenant. Visitors can explore the product without creating a tenant or registering an account.

## Entry points

The frontend exposes `/demo` with three personas:

- **Employee** -> `demo.staff`
- **Manager** -> `demo.manager`
- **HR** -> `demo.hr`

The marketing site links directly to `/demo?persona=employee`, `/demo?persona=manager`, or `/demo?persona=hr`. Supplying a valid persona query parameter starts the corresponding demo session automatically; visiting `/demo` without a persona shows the chooser.

A dedicated hostname such as `demo.leavemaestro.com` can point to the same frontend deployment. No separate frontend codebase is required. Configure the hostname to serve the same SPA and route `/demo` normally.

## Authentication behavior

The browser calls `POST /auth/demo-login` with only the selected persona. The backend:

1. resolves the persona to one of the dedicated seeded demo identities;
2. verifies that `demo.tenant.id` currently refers to a tenant with `TenantType.DEMO`;
3. authenticates the mapped identity using the server-side configured demo credential;
4. stores the resulting Spring Security context in the HTTP session.

The demo credential is therefore not embedded in marketing or frontend JavaScript.

## Authorization and isolation

Demo users use the same roles and permissions as normal tenant users:

- Employee uses the tenant `Staff` role and can work with personal leave flows.
- Manager uses the tenant `Manager` role and can access approval flows according to configured leave approver relationships.
- HR uses the tenant `HR` role and tenant-level HR administration capabilities.

No public demo persona is assigned `PLATFORM_ADMIN`. Existing tenant scoping remains in force, and the login endpoint refuses to create a session if the configured tenant is not classified as `DEMO`.

## Demo labelling and persona switching

Authenticated demo sessions display a persistent **Public demo environment** banner. The banner includes **Switch persona**, which signs out the current session and returns to `/demo`.

The demo landing page also warns visitors to use fictional data only. Demo data can be reset periodically by the mechanism documented in [DEMO tenant seed and reset](demo-tenant-operations.md).

## Configuration

The persona entry flow uses the same configuration as the demo seed service:

```properties
demo.tenant.id=DEMO
demo.tenant.password=Demo123!
```

For deployed environments, supply `demo.tenant.password` through the normal secret/configuration mechanism rather than source control. The seed service and persona login endpoint must use the same value.

The marketing build uses `NEXT_PUBLIC_APP_URL` to construct links to the application `/demo` route.

## Safety notes

- Keep the configured public demo tenant classified as `DEMO`.
- Do not assign platform roles to demo identities.
- Do not reuse the demo identities in STANDARD tenants.
- Do not expose the configured demo password to browser code.
- Keep periodic reset enabled when the public demo is reachable from the internet.
