# GitHub OAuth app setup

1. Open GitHub **Settings → Developer settings → OAuth Apps → New OAuth App**.
2. Set **Application name** and **Homepage URL** (for local development use `http://localhost:8080`).
3. Set **Authorization callback URL**:
   - `http://localhost:8080/login/oauth2/code/github`
4. Create the app and copy:
   - `Client ID` → `GITHUB_CLIENT_ID`
   - `Client secret` → `GITHUB_CLIENT_SECRET`

Default authorization endpoint in LeaveMaster:

- `http://localhost:8080/oauth2/authorization/github`
