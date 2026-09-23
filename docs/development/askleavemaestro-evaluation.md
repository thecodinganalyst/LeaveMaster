# AskLeaveMaestro deterministic evaluation

AskLeaveMaestro has a model-independent regression catalogue for assistant behaviour. It is designed to catch orchestration, authorization, factual and conversational regressions without calling Gemini or another live model.

## Run locally

From `backend/`:

```bash
./gradlew assistantEvaluation
```

Reports are written to:

- `backend/build/reports/assistant-evaluation/assistant-evaluation.md`
- `backend/build/reports/assistant-evaluation/assistant-evaluation.json`

The same task runs in backend CI. Critical scenario failures fail the Gradle task and therefore the pipeline.

## Add a regression scenario

Add a declarative entry to `backend/src/test/resources/assistant-evaluation/scenarios.json`.

Each scenario defines:

- stable scenario ID and category;
- whether the scenario is critical;
- authenticated actor, staff ID, authorities and tenant;
- one or more conversational turns;
- stubbed model response and tool calls/results;
- expected ordered tools;
- expected tool arguments;
- facts that must be present;
- facts that must never be present;
- expected authorization outcome.

Prefer business facts and tool contracts over exact prose. For example, assert that an Annual Leave response contains the authoritative balance, not the complete generated sentence.

Multi-turn behaviour is represented by multiple `turns` in one scenario. This keeps a conversation together without relying on a live provider.

## Categories

The starter catalogue covers leave balances, policy/eligibility, employment boundaries, calendars/work schedules, approvers, tenant isolation, RBAC, multi-turn follow-ups and failure handling.

Every production AskLeaveMaestro defect should gain a scenario that reproduces the failure before or alongside its fix.

## Live-model tests

Do not add Gemini credentials or live model calls to this suite. This framework is intentionally deterministic. Live-provider quality evaluation is tracked separately so normal PR checks are stable and reproducible.


## Live Gemini evaluation

The deterministic suite remains the PR gate. A separate `AskLeaveMaestro live Gemini evaluation` workflow exercises the deployed DEMO tenant against the model configured in that deployment. It runs nightly and can also be started manually before/after an intentional prompt or model change.

The workflow uses public DEMO persona authentication and never receives or logs the Gemini API key. Set repository variable `ASSISTANT_LIVE_EVAL_BASE_URL` to evaluate a deployment other than the documented default, or provide `base_url` when manually dispatching the workflow.

Live scenarios are in `scripts/assistant-live-eval/scenarios.json`. They are deliberately read-only. The runner records HTTP outcome, latency, observed structured-result tool names, pass/fail reasons and every retry. It stores JSON plus a Markdown workflow summary/artifact for 30 days. Response excerpts are truncated in JSON and omitted from the Markdown report; do not add prompts that request secrets or personal data.

Quality gates are strict: every critical scenario must pass, every tenant-isolation scenario must pass, and the complete live catalogue must achieve at least 95%. A scenario is attempted at most twice so a transient provider failure can be distinguished from a repeatable product regression; a successful retry still remains visible in the report.

When a run fails, first inspect the scenario's HTTP status, latency, observed tools and per-attempt failure reasons. Re-run once manually if the evidence indicates a provider/network transient. For repeated failures, compare the current prompt/model/configuration and application changes, reproduce with the deterministic scenario where possible, and add/update a deterministic regression before fixing the product. Tenant-isolation failures are never treated as model variability.

Token and monetary-cost metrics are not currently exposed by the application chat response, so the report does not invent them. Add them when provider usage metadata is safely surfaced by the backend. Provider/model identification likewise comes from deployment configuration/logging; the workflow describes the target as the configured deployment provider rather than exposing credentials.
