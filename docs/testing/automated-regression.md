# Automated regression

LeaveMaestro publishes the latest successful **Full business regression** scenario report on this documentation site.

[View latest regression report](../regression-report/){ .md-button .md-button--primary }

The report answers the business-facing question **which LeaveMaestro behaviours were exercised and what passed or failed?** It aggregates the stable `SCN-*` catalogue across backend and Playwright tests and shows scenario status, functional area, executed layer, and duration.

## What this report is

The business scenario report is different from code coverage. JaCoCo and frontend coverage measure which code was executed; the regression report measures whether defined business behaviours such as leave approval, employment-date boundaries, tenant isolation, permissions, proration, and multi-jurisdiction handling were exercised successfully.

The stable report URL is:

```text
https://thecodinganalyst.github.io/LeaveMaster/testing/regression-report/
```

The page is updated only from a successful full-regression workflow on `main`. The published HTML includes the source commit and a link back to the GitHub Actions run used to publish it.

## Last-successful behaviour

A failed or incomplete regression run does **not** replace the currently published report. `docs-pages.yml` remains the only workflow that deploys GitHub Pages. It publishes a new report only after resolving a successful `Full business regression` run and downloading that run's `scenario-coverage-full-regression` artifact.

If the documentation workflow cannot obtain a valid successful regression artifact, it fails before Pages deployment. That leaves the previous successful Pages deployment intact instead of replacing it with a missing or partial report.

## Historical reports

GitHub Pages intentionally exposes the convenient **latest successful** report only. Per-run reports remain available from **GitHub Actions → Full business regression → Artifacts → `scenario-coverage-full-regression`** for the artifact retention period.

The Actions artifact contains:

- `scenario-report.html` — rich standalone report;
- `scenario-report.md` — concise Markdown report;
- `scenario-report.json` — machine-readable report data.

Playwright reports, backend test reports, traces, screenshots, videos, and failure logs remain separate Actions artifacts.

## Running and generating locally

See [Business scenario reporting](scenario-reporting.md) for the scenario catalogue, mapping conventions, local smoke/regression commands, and report-generation command.
