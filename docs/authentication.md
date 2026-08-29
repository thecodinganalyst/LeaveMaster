# Authentication

## Password authentication

Tenant users authenticate with three values:

- **Tenant ID** — manually entered by the user. The application does not expose a tenant picker on the login screen.
- **Login name** — unique only within that tenant.
- **Password**.

The backend authenticates the tuple `(tenantId, loginName, password)`. It does not concatenate tenant ID and login name into a persisted or synthetic username. After authentication, the Spring Security principal name is the immutable `app_user.user_id`, so session lookups remain unambiguous even when two tenants use the same login name.

All invalid tenant, login-name, and password combinations return the same generic `401 Unauthorized` result.

## Platform administrator realm

Platform administrators do not belong to a tenant. They sign in using the reserved tenant/realm value:

`PLATFORM`

`PLATFORM` is case-insensitive during authentication and cannot be used as a normal tenant ID. The platform administrator is resolved only from users whose `tenant_id` is null.

## HTTP Basic

HTTP Basic authentication is disabled for application users. Its standard username/password credential shape cannot safely represent the required tenant ID + login name + password tuple.

## OAuth / GitHub login

OAuth login remains independent of password authentication. Existing users are matched by the globally unique `(oidcProvider, oidcSubject)` mapping. Once OAuth authentication succeeds, application authorization and tenant context continue to come from the matched `AppUser` record.

## Sessions, CSRF, and logout

Password login continues to use the existing session-based Spring Security flow and CSRF protection. `/auth/login` requires a valid CSRF token, successful authentication is stored in the HTTP session, `/auth/me` resolves the user using the immutable authenticated `user_id`, and `/logout` invalidates the authenticated session using the existing Spring Security logout flow.

Account activation is intentionally separate. Tenant-aware activation endpoints are handled by the follow-up activation work rather than weakening password authentication to accommodate login-name-only activation.
