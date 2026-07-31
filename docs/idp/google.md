# Google OIDC setup

1. Open Google Cloud Console and create/select a project.
2. Go to **APIs & Services → OAuth consent screen** and configure the app.
3. Go to **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
4. Choose **Web application** and add this redirect URI:
   - `http://localhost:8080/login/oauth2/code/google`
5. Copy the generated client credentials and set:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`

Default authorization endpoint in LeaveMaster:

- `http://localhost:8080/oauth2/authorization/google`
