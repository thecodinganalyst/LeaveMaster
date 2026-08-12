# SPA and API route separation

Firebase Hosting must reserve browser-facing application paths such as `/tenants`, `/users`, `/roles`, `/locations`, and `/login` for the React single-page application. If those paths are rewritten directly to Cloud Run, a direct browser navigation waits for the backend before React can render the cold-start screen.

Frontend CRUD requests therefore use the `/api/...` namespace, while session bootstrap and login use `/auth/...`. Firebase sends those backend-only namespaces to the `leavemaster-api` Cloud Run service and sends unmatched browser routes to `/index.html`.

The backend retains the previous resource controller paths alongside their `/api/...` aliases for compatibility with existing API consumers.
