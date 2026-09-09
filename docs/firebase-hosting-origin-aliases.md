# Firebase Hosting origin aliases

LeaveMaestro supports the canonical production application domain and the two Firebase Hosting aliases for the configured Hosting site.

For a Hosting site ID such as `leavemaster-production`, the backend CORS allowlist is derived automatically from:

- the canonical `PUBLIC_APP_URL`, for example `https://app.leavemaestro.com`;
- `https://leavemaster-production.web.app`; and
- `https://leavemaster-production.firebaseapp.com`.

Any origins supplied through `ALLOWED_FRONTEND_ORIGINS` are merged as additional exact origins. They no longer replace the canonical or Firebase Hosting origins. Duplicate origins are removed.

Wildcard origins remain invalid. Production CORS continues to use exact HTTPS origins so credentialed browser sessions are not opened to arbitrary sites.

## Why both Firebase aliases are supported

Firebase Hosting serves a site through both `web.app` and `firebaseapp.com`. Supporting both aliases provides a fallback URL for environments where the custom domain is unavailable or blocked, while keeping the custom domain as the canonical application URL.

The aliases are derived from `FIREBASE_HOSTING_SITE` when an explicit site ID is configured, or from `<GCP_PROJECT_ID>-<FRONTEND_ENVIRONMENT>` otherwise. No GitHub variable needs to duplicate the Firebase aliases manually.

## Canonical redirects and OAuth

`PUBLIC_APP_URL` remains the canonical application URL used for OAuth callback and redirect behavior. Enabling the Firebase aliases for CORS does not change registered OAuth callback URLs.

This means username/password login and authenticated API calls can use either Firebase Hosting alias, while OAuth providers can continue to return users to the custom domain.

## Additional origins

`ALLOWED_FRONTEND_ORIGINS` remains available for intentional extra origins such as an approved preview environment. Supply it as a JSON list in the GitHub production environment, for example:

```text
["https://preview.example.com"]
```

The final runtime allowlist will contain the canonical URL, both Firebase aliases, and any explicitly configured additional origins.

## Verification

After deployment, verify all intended origins with username/password authentication and an authenticated API request. An arbitrary origin such as `https://unapproved.example.com` should still be rejected.
