# Microsoft Entra ID OIDC setup

1. Open **Microsoft Entra admin center** and go to **App registrations → New registration**.
2. Set a name and supported account type for your tenant needs.
3. Add this redirect URI under **Web**:
   - `http://localhost:8080/login/oauth2/code/microsoft`
4. After creation, copy:
   - **Application (client) ID** → `MICROSOFT_CLIENT_ID`
   - **Directory (tenant) ID** → `MICROSOFT_TENANT_ID`
5. Go to **Certificates & secrets** and create a new client secret:
   - secret value → `MICROSOFT_CLIENT_SECRET`
6. In **API permissions**, ensure OpenID scopes are available (`openid`, `profile`, `email`).

Default authorization endpoint in LeaveMaster:

- `http://localhost:8080/oauth2/authorization/microsoft`

For multi-tenant sign-in, set `MICROSOFT_TENANT_ID=common`.
