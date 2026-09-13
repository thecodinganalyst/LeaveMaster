# Business scenario reporting

LeaveMaestro maintains a business-facing automated test catalogue in `docs/testing/scenario-catalogue.json`. The catalogue is the source of truth for stable scenario identifiers, business metadata, suite membership, and test mappings.

## Scenario IDs

Published scenario IDs use the stable form `SCN-001`, `SCN-002`, and so on. Do not renumber an existing scenario. If a scenario is retired or replaced, keep its historical ID and introduce a new ID for the replacement.

The initial catalogue covers the ten core business scenarios introduced by issue #528, including provisioning, proration, employment-date boundaries, approval/rejection, weekend/public-holiday calculation, role access, tenant isolation, and multi-jurisdiction behaviour.

## Adding or changing a scenario

1. Add the scenario to `docs/testing/scenario-catalogue.json` with a unique `SCN-*` ID.
2. Set `area`, `roles`, `jurisdictions`, `suites`, `required`, and `automationStatus`.
3. Add backend JUnit method names under `tests.backend` and/or Playwright test titles under `tests.playwright`.
4. Prefer adding the scenario ID directly to Playwright business-test titles, for example `SCN-005 staff applies leave and manager approves @smoke`.
5. Put only high-value critical scenarios in the `smoke` suite. Full business coverage belongs in `regression`.
6. Run the appropriate suite and generate the report locally before opening a PR.

A business scenario may have multiple tests and may be covered by both backend and Playwright layers. The report aggregates all matching executions under the same scenario ID rather than counting them as separate business scenarios.

## Local commands

Run the existing test suites first:

```bash
cd backend
./gradlew smokeTest
# or
./gradlew regressionTest

cd ../frontend/e2e
npm install
npx playwright install chromium
npm run test:smoke
# or
npm run test:regression
```

Then generate the business scenario report from the repository root:

```bash
node scripts/testing/generate-scenario-report.mjs \
  --suite smoke \
  --backend-results backend/build/test-results/smokeTest \
  --playwright-results frontend/e2e/test-results/playwright-results.json \
  --output build/scenario-report-smoke
```

For full regression, change `--suite` to `regression`, use `backend/build/test-results/regressionTest`, and choose a regression output directory.

The generator produces:

- `scenario-report.md` — concise business-readable summary;
- `scenario-report.json` — machine-readable data for future dashboards/trend analysis;
- `scenario-report.html` — richer standalone report for review/audit.

## Aggregation rules

The generator reads Gradle JUnit XML from the selected backend task and Playwright's JSON reporter output. Backend method names and Playwright titles are mapped through the catalogue; a Playwright title can also carry a direct `SCN-*` ID.

For each scenario:

- `FAIL` if any mapped execution failed;
- `PASS` if at least one mapped execution passed and none failed;
- `SKIP` if executions exist but none passed or failed;
- `NOT_EXECUTED` if no mapped execution appeared in the selected suite output.

Durations from all matching executions are aggregated. Functional coverage is grouped by the catalogue's `area` value.

## PR smoke versus full regression

The PR smoke workflow reports only scenarios whose catalogue `suites` contains `smoke`. It keeps pull-request feedback focused on the critical paths defined in issue #529.

The full regression workflow reports every scenario whose catalogue `suites` contains `regression`. A scenario marked `required.regression: true` causes the reporting step to fail if no mapped execution is found. This prevents a catalogue entry from silently losing regression coverage.

The same rule applies to required smoke scenarios in the PR smoke report.

## GitHub Actions outputs

Both E2E workflows append the generated Markdown report to the GitHub Actions job summary. The summary includes:

- defined, executed, passed, failed, and not-executed counts;
- scenario ID/name, functional area, executed layer(s), result, and duration;
- functional-area coverage;
- failed scenarios;
- missing/not-executed scenarios.

The workflows upload the generated Markdown, JSON, and HTML together as a scenario-coverage artifact. Existing Playwright HTML reports, traces/screenshots/videos on failure, backend test reports, and backend logs remain separate and are not duplicated by the scenario reporter.

## Latest successful report on GitHub Pages

The latest successful full-regression HTML report is published at:

```text
https://thecodinganalyst.github.io/LeaveMaster/testing/regression-report/
```

The [Automated regression](automated-regression.md) documentation page provides the user-facing entry point.

`.github/workflows/docs-pages.yml` is the repository's only GitHub Pages deployer. On a successful `Full business regression` workflow completion, it rebuilds the documentation, downloads that run's `scenario-coverage-full-regression` artifact, and overlays `scenario-report.html` at the stable Pages route. Documentation pushes and manual Pages deployments resolve the most recent successful full-regression run on `main` and use its artifact.

Only successful full-regression runs are eligible for Pages publication. A failed or incomplete regression does not trigger publication. If a later documentation deployment cannot resolve or download a valid successful report, the Pages build fails before deployment, preserving the previously published successful site/report.

The published HTML is accompanied by source metadata including the regression workflow run, source commit SHA, and Pages publication timestamp. Historical report outputs remain in the per-run GitHub Actions artifacts according to their configured retention period.

## Security and maintainability

The catalogue and reports contain business metadata, test names, statuses, and durations only. Do not add passwords, tokens, OAuth secrets, raw authentication responses, or sensitive user data to scenario metadata or report output.

Keep business descriptions in the catalogue instead of repeating them across reporting code. Test mappings should point to deterministic test names, and scenario IDs must remain stable once published.
