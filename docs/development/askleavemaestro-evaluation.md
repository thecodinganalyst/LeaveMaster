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
