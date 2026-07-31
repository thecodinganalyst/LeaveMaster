# Facebook Login setup

1. Open the **Meta for Developers** dashboard and create/select an app.
2. Add the **Facebook Login** product.
3. Under **Facebook Login → Settings**, add this **Valid OAuth Redirect URI**:
   - `http://localhost:8080/login/oauth2/code/facebook`
4. Copy app credentials and set:
   - `FACEBOOK_CLIENT_ID` (App ID)
   - `FACEBOOK_CLIENT_SECRET` (App Secret)
5. Ensure your app mode and permitted users/domains are configured for your environment.

Default authorization endpoint in LeaveMaster:

- `http://localhost:8080/oauth2/authorization/facebook`
