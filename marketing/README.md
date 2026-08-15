# LeaveMaestro Marketing Site

A Next.js 14 App Router marketing website for LeaveMaestro.

## Prerequisites

- Node.js 18.17+ or 20+
- npm, pnpm, or yarn

## Local development

1. Install dependencies:
   ```bash
   npm install
   ```
2. Copy environment variables if needed:
   ```bash
   cp .env.example .env.local
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```
4. Open `http://localhost:3000`.

## Environment variables

- `NEXT_PUBLIC_APP_URL` - app URL for product CTAs
- `NEXT_PUBLIC_API_URL` - API base URL reference
- `NEXT_PUBLIC_DEMO_URL` - demo destination used on CTA links and embed links

## Build for static export

```bash
npm run build
```

The static site is generated in the `out/` directory.

## Deploy to Cloudflare Pages

1. Install dependencies.
2. Build the project:
   ```bash
   npm run build
   ```
3. Deploy the `out/` directory to Cloudflare Pages, or use Wrangler:
   ```bash
   npx wrangler pages deploy out
   ```

Wrangler configuration is stored in `wrangler.toml`.
