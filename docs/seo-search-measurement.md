# Search engine verification and organic search measurement

LeaveMaestro's canonical marketing site is `https://leavemaestro.com`. The marketing build publishes `/robots.txt` and `/sitemap.xml`.

## Verification configuration

Google Search Console and Bing Webmaster Tools can verify ownership through HTML meta tags. Configure the values at **build time**:

```text
NEXT_PUBLIC_GOOGLE_SITE_VERIFICATION=<Google verification content value>
NEXT_PUBLIC_BING_SITE_VERIFICATION=<Bing msvalidate.01 content value>
```

These values are public verification tokens because they are intentionally rendered into the generated HTML. They are not API credentials. Do not put Search Console/Bing API credentials, service-account keys, passwords, or other secrets in `NEXT_PUBLIC_*` variables or the repository.

If either variable is absent, LeaveMaestro simply omits that provider's verification tag; the marketing build continues to work.

For the Cloudflare Pages production project, configure the variables in the production build environment rather than committing live values.

## Google Search Console

1. Add `https://leavemaestro.com` as a URL-prefix property (or use a DNS Domain property if you prefer DNS ownership).
2. For URL-prefix verification, choose the HTML tag method and copy only the `content` value from the Google verification meta tag.
3. Set that value as `NEXT_PUBLIC_GOOGLE_SITE_VERIFICATION` in the production marketing build environment and deploy.
4. Inspect the deployed page source and confirm the `google-site-verification` meta tag is present.
5. Complete verification in Search Console.
6. Submit `https://leavemaestro.com/sitemap.xml` in **Sitemaps**.
7. Use URL Inspection on the homepage and important landing pages after meaningful releases.

DNS verification is also valid, but DNS records are managed outside this application and should not be represented as application secrets.

## Bing Webmaster Tools

1. Add `https://leavemaestro.com` in Bing Webmaster Tools. Importing an already verified Search Console property is also an option.
2. If using the HTML meta-tag method, copy only the `content` value for `msvalidate.01`.
3. Set it as `NEXT_PUBLIC_BING_SITE_VERIFICATION` in the production marketing build environment and deploy.
4. Confirm the deployed page source contains the `msvalidate.01` meta tag.
5. Complete verification.
6. Submit `https://leavemaestro.com/sitemap.xml`.

## Measurement approach

Search Console and Bing Webmaster Tools are the source of truth for search-engine discovery and organic search performance. This issue deliberately does **not** add a third-party browser analytics script or tracking cookies merely for SEO measurement.

Review these dimensions:

| Signal | Why it matters |
| --- | --- |
| Indexed pages / indexing errors | Confirms intended pages can enter search results |
| Impressions | Shows whether search visibility is growing |
| Clicks | Measures visits delivered by organic search |
| CTR | Helps identify snippets/pages that may need clearer titles and descriptions |
| Search queries | Shows the intents for which LeaveMaestro is appearing |
| Landing pages | Identifies which public pages attract organic discovery |
| Country/device | Helps spot meaningful audience or mobile differences |

When evaluating changes, compare sufficiently long periods and account for newly published pages; do not treat day-to-day search fluctuations as a product KPI.

## Recurring review checklist

Monthly, and after major SEO releases:

- check sitemap processing and page indexing;
- review excluded/not-indexed URLs for unexpected canonical, robots, redirect, or 404 problems;
- compare impressions, clicks and CTR with the previous comparable period;
- review queries and landing pages for relevant search intent;
- inspect important pages with unexpectedly high impressions but low CTR;
- inspect pages losing visibility before changing content;
- confirm canonical URLs still use `https://leavemaestro.com`;
- record material observations and follow-up work in GitHub issues.

## Privacy and security

- Search verification meta-tag values are public by design.
- Do not commit provider API credentials or account credentials.
- Do not add user-level tracking solely to measure SEO.
- Prefer aggregated search-engine performance reports for organic discovery.
- If product analytics is added later, review consent, retention and privacy requirements separately.


## Google and Gemini crawlability

LeaveMaestro intentionally allows both `Googlebot` and `Google-Extended` on the public marketing site. `Googlebot` supports normal Google Search discovery. `Google-Extended` is the robots.txt control Google provides for Gemini-related model use and grounding; it is separate from ordinary Search indexing.

After deploying a crawler-policy change:

1. Confirm `https://leavemaestro.com/robots.txt` returns HTTP 200 and contains explicit `Allow: /` rules for `Googlebot` and `Google-Extended`.
2. Confirm `https://leavemaestro.com/sitemap.xml` returns HTTP 200 and contains the intended public URLs.
3. In Google Search Console, inspect the homepage and key Singapore leave guide URLs, run a live test, and request indexing where appropriate.
4. Check Cloudflare bot/AI crawler controls and custom WAF rules. Do not enable a rule that blocks Google-Extended or verified Google crawlers for the public marketing hostname. Cloudflare settings are infrastructure/account configuration and are not controlled by this repository.
5. After Google has had time to recrawl/index the pages, retry Gemini grounding/cross-checking.

If Gemini reports `URL_FETCH_STATUS_GOOGLE_EXTENDED_OPT_OUT`, first inspect the deployed robots.txt and Cloudflare crawler controls. A successful ordinary browser request does not prove that the Google-Extended crawler is permitted.
