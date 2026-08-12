# Frontend routing configuration

Browser routes are owned by the React SPA and must fall through to Firebase Hosting's `index.html` rewrite. Backend calls use dedicated namespaces (`/api/**` and `/auth/**`) so Cloud Run cold starts cannot block the initial SPA document from rendering.
