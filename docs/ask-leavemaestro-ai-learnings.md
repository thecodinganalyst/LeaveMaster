# Ask LeaveMaestro AI learnings and improvement playbook

This document captures engineering lessons learned while building and operating **Ask LeaveMaestro**. It is a design and review guide for improving answer quality, reliability and safety without moving authoritative business logic into the language model.

It complements the operational documentation:

- [Set up Ask LeaveMaestro](assistant-setup.md) for provider configuration, credentials, deployment and troubleshooting.
- [Ask LeaveMaestro AI providers](assistant-providers.md) for provider-neutral runtime configuration.
- [AI assistant security and privacy](assistant-security.md) for authentication, tenant isolation, authorization, write confirmation and audit controls.
- [Architecture](architecture.md) for the wider application and MCP/tool architecture.
- [Troubleshooting](troubleshooting.md) for application and deployment diagnostics.

The guidance below separates **established engineering rules** from **future improvements**. Established rules should be treated as defaults for implementation and review. Future improvements are candidates that still need design, implementation and validation.

---

## 1. Core principle: LeaveMaestro is authoritative; the LLM orchestrates

The most important design rule is:

> **The LLM should explain and orchestrate. LeaveMaestro services and tools should calculate, authorize and retrieve authoritative business facts.**

The assistant must not independently recreate leave calculations that already exist in backend services. This applies to balances, generated entitlements, proration, eligibility, policy resolution, effective dates, approval relationships and other business rules.

A good AI flow is:

1. understand the user's intent;
2. choose the smallest appropriate authorized tool or service operation;
3. retrieve authoritative data;
4. explain that result in a form appropriate to the question;
5. clearly state when authoritative data is unavailable instead of guessing.

A risky AI flow is:

1. retrieve loosely related configuration;
2. infer business rules from incomplete data;
3. perform an independent calculation in the prompt/model;
4. present the inferred result as authoritative.

Keeping calculations in backend services gives the normal UI, API and AI assistant the same source of truth and makes behaviour testable without depending on model reasoning.

---

## 2. Answer-quality principles

### Answer the user's question first

The first sentence or first compact section should contain the answer the user actually asked for. Supporting detail comes after it only when it improves understanding.

For a question such as:

> Why does staff 001 have 5.79 days of annual leave?

Prefer:

> Staff 001 has 5.79 days because the 2026 annual-leave entitlement was prorated from their 3 August 2026 join date. The full-year entitlement is 14 days, and LeaveMaestro applied the policy's proration rule for the remaining eligible portion of 2026.

Do not lead with a long policy dump, every intermediate date, generic policy attributes, or internal tool details.

### Prefer the shortest complete answer

Concise does not mean incomplete. Include the minimum facts needed to establish confidence in the answer.

Default behaviour:

- explanation question -> short explanation with the decisive facts;
- summary question -> structured list or table;
- comparison question -> side-by-side differences;
- action question -> action/status plus any required confirmation;
- troubleshooting question -> likely cause followed by the next useful checks.

### Match response shape to intent

A global instruction such as "always be brief" is too blunt. It can damage answers that genuinely need structure.

Examples:

| User intent | Preferred response shape |
|---|---|
| "Why do I have 5.79 days?" | 1-3 short paragraphs; calculation details only if useful |
| "What are my current leave entitlements?" | compact table/list showing leave type, entitlement, used/remaining where available |
| "Which policies apply to me?" | structured policy summary with effective dates |
| "Can I approve Alice's leave?" | direct yes/no with the authoritative permission/approver reason |
| "Apply 2 days of leave for me" | proposed action with the normal confirmation flow |

### Avoid unnecessary calculation detail

Do not automatically expose every intermediate step. Calculation breakdowns are appropriate when:

- the user explicitly asks to see the calculation;
- the result looks surprising and the breakdown helps establish why;
- a discrepancy is being investigated;
- the calculation itself is the subject of the question.

Even then, use values returned or derived by authoritative backend logic where possible.

### Do not expose internal reasoning or tool chatter

User-visible responses should not include:

- chain-of-thought or hidden reasoning;
- tool selection deliberation;
- raw MCP payloads unless the product explicitly needs them;
- internal retry attempts;
- implementation details that do not help answer the question.

### Distinguish facts from uncertainty

When data is incomplete, unavailable or ambiguous, say so explicitly. Do not fill gaps with plausible policy assumptions.

Prefer:

> I can see the current entitlement record, but the policy that produced it is not available from the current tool response, so I cannot reliably explain the policy rule yet.

Avoid:

> This is probably because your company uses the standard 14-day policy.

---

## 3. Before/after example: excessive detail

### Before

An overly detailed answer to "Why is staff 001 given 5.79 days of leave?" can contain all of the following even when they were not requested:

- full-year entitlement description;
- eligible service-day count;
- total calendar days in the year;
- explicit formula;
- rounding discussion;
- accrual settings;
- carry-forward settings;
- generic policy notes;
- unrelated eligibility details.

The answer may be technically informative but makes the user search for the actual reason.

### After

> Staff 001 has **5.79 days of Annual Leave because their 2026 entitlement was prorated from their 3 August 2026 join date**. Their applicable full-year entitlement is 14 days, and LeaveMaestro applied the policy's proration rule for the eligible portion of the year.
>
> If useful, I can also show the exact proration calculation.

Why this is better:

- the cause appears immediately;
- it preserves the decisive policy fact;
- it avoids unrelated configuration;
- detailed calculation remains available when the user wants it.

For a different question such as "What are my current leave entitlements?", the assistant should **not** force the same short-paragraph format. A structured table is more useful there.

---

## 4. Prompt and instruction design

### Put stable behavioural rules in the stable prompt

Good candidates for stable system/developer instructions include:

- LeaveMaestro tools/services are authoritative for business facts and calculations;
- never invent policy, eligibility or entitlement rules;
- respect tenant and permission boundaries supplied by the backend;
- answer the user's question first;
- adapt response structure to intent;
- avoid unnecessary detail;
- do not expose internal reasoning or tool chatter;
- state uncertainty when authoritative data is unavailable;
- writes require the product's normal confirmation flow.

### Keep dynamic context dynamic

Do not permanently bake transient business facts into the stable prompt. Retrieve or derive them at request time, including:

- authenticated actor and tenant;
- current staff record;
- current entitlement/balance data;
- applicable jurisdiction;
- current/effective policy records;
- approver relationships;
- date-sensitive leave calendar information.

This reduces stale context and makes answers reflect the current application state.

### Prefer positive, testable instructions

Prompt rules should describe observable behaviour. For example:

- "Answer the direct question in the first paragraph."
- "Use a table when the user requests a multi-item entitlement summary."
- "Do not calculate a leave balance if a LeaveMaestro tool provides it."
- "If required authoritative data is missing, identify what is missing rather than infer it."

These are easier to evaluate than vague instructions such as "be helpful" or "be smart".

### Avoid conflicting blanket instructions

Common conflicts include:

- "always be concise" versus a request that needs a structured multi-row answer;
- "always explain calculations" versus a simple explanation question;
- "always use a table" versus a yes/no permission question.

Use intent-aware defaults instead of universal formatting rules.

### Keep provider behaviour out of business rules

The business contract should not depend on whether OpenAI or Gemini is selected. Provider-specific adaptation belongs in provider integration/configuration only when unavoidable.

The same evaluation cases should be usable against every supported provider/model.

---

## 5. Tool and data-retrieval design

### Prefer purpose-built authoritative tools

Tools should answer business questions at the right abstraction level. If a user asks for current entitlements, a tool that returns the current entitlement view is safer than requiring the model to combine raw policy, eligibility, staff and calendar records itself.

Prefer:

- composite read tools for common questions;
- stable structured fields;
- human-meaningful names in addition to identifiers where useful;
- explicit effective dates;
- already-authorized/scoped results;
- backend-computed totals and statuses.

### Minimize tool payloads

Large payloads increase cost, latency and the chance that the model focuses on irrelevant fields. Return only the data needed for the supported question when practical.

Avoid sending whole entity graphs when a compact DTO can answer the question.

### Do not leak persistence concerns into AI workflows

AI-facing service/tool results should be fully materialized DTOs or similarly safe structures. Do not rely on later lazy loading of detached JPA entities.

This is both a reliability lesson and a boundary-design lesson: the provider call can outlive the persistence context that originally loaded an entity.

### Fail closed on missing authoritative data

When a required tool fails or cannot return the necessary data:

- do not ask the model to reconstruct the missing value from unrelated context;
- return a user-safe failure or partial answer;
- log the diagnostic category with request correlation information;
- let the user know what could not be verified.

---

## 6. Leave-domain context lessons

### Policy, eligibility and entitlement are different concepts

The assistant must not conflate:

- **leave type**: the category of leave;
- **entitlement policy**: how an entitlement is determined;
- **eligibility rule**: whether/when a policy applies;
- **staff entitlement**: the generated/current entitlement for an individual;
- **leave balance**: the usable/remaining amount after relevant transactions/application effects.

When explaining a current staff result, prefer the individual's authoritative generated/current record and reference the policy/rule that produced it when available.

### Jurisdiction-specific rules must be retrieved

Do not assume that a policy from one country/state applies to another. Jurisdiction, effective date and tenant-specific overrides matter.

The model should never rely on generic knowledge such as "Singapore employees normally get X" when LeaveMaestro has authoritative configured policy data for the tenant/staff member.

### Effective dates matter

Policy and approver records can change over time. Date-sensitive questions must use the effective record for the requested/current period rather than merely the newest row.

For example, "Why was my 2026 entitlement generated this way?" must use the policy/rule effective for the 2026 generation period, not a policy that begins in 2027.

### Explain the rule that actually produced the result

When possible, explanations should connect:

`staff fact -> applicable eligibility/policy -> authoritative generated result`

This is more useful than dumping all policies visible to the user.

---

## 7. Reliability and provider lessons

Operational details remain in [assistant-setup.md](assistant-setup.md) and [assistant-providers.md](assistant-providers.md). The lessons that should influence design are:

### Provider and model selection must be configuration-driven

Switching between supported providers/models should not require rewriting business logic or frontend behaviour.

### Model availability is an external dependency

A model can be renamed, deprecated or unavailable even when LeaveMaestro code has not changed. A provider `404` can therefore indicate model availability/configuration rather than an application routing defect.

When a provider error occurs, diagnose the provider/model configuration before treating the failure as a LeaveMaestro business-code defect.

### Retry only transient failures

Retries are appropriate for selected transient failures such as some `429`, timeout or `5xx` cases, subject to the existing retry policy.

Do not blindly retry deterministic failures such as:

- invalid credentials;
- unsupported model identifiers;
- malformed requests caused by application code;
- authorization failures;
- validation failures.

### Budget timeouts end-to-end

Cloud Run/request timeout, application timeout, provider timeout and retry count interact. A retry policy that can exceed the outer request budget produces poor user experience and ambiguous `502`/timeout failures.

Timeout design should ensure the application has time to:

- detect provider timeout/failure;
- stop retrying when the request budget is exhausted;
- return a controlled error response;
- emit useful diagnostics.

### User errors should be useful but sanitized

User-facing provider errors should explain the category and next action where possible without exposing credentials, raw provider payloads or sensitive internal data.

---

## 8. Observability and debugging

A production AI request should be traceable without logging sensitive prompt content.

Useful diagnostic context includes:

| Field | Why it matters |
|---|---|
| provider | distinguishes OpenAI/Gemini integration paths |
| model | catches obsolete/incorrect model configuration |
| conversation/request ID | correlates logs across the workflow |
| tenant ID | establishes scoped execution context |
| authenticated actor/login | identifies the server-authenticated requester |
| elapsed time / timeout budget | distinguishes slow provider/tool behaviour |
| tool name | identifies the authoritative operation attempted |
| failure category | separates provider, tool, validation, auth and application failures |
| provider HTTP/status category | distinguishes `404`, `429`, `5xx`, timeout, etc. |
| retry attempt/count | shows whether retry policy contributed to latency |

### Never log

- OpenAI/Gemini API keys;
- OAuth/client secrets;
- database credentials;
- activation PINs;
- authorization/session tokens;
- unnecessarily sensitive prompt text;
- full tool payloads containing personal/sensitive data when diagnostic metadata is sufficient.

Redaction should happen before values enter logs, not as a later cleanup step.

---

## 9. AI behavioural regression and evaluation strategy

Normal Java/TypeScript tests verify application logic, but they do not fully verify response quality. Ask LeaveMaestro needs a small repeatable behavioural evaluation set.

### Evaluation cases

At minimum maintain representative cases for:

1. **Concise explanation**  
   Example: why a staff member received a specific prorated entitlement.

2. **Structured summary**  
   Example: current leave entitlements across several leave types.

3. **Authoritative calculation**  
   The answer must rely on the correct backend tool/service rather than independent model arithmetic.

4. **Ambiguous question**  
   The response should use available context safely and avoid invented assumptions.

5. **Unauthorized request**  
   The assistant must not expose information the authenticated user cannot access.

6. **Cross-tenant request**  
   Tenant boundaries must remain enforced by the backend even if the prompt requests another tenant's data.

7. **Tool failure**  
   The answer should not fabricate the unavailable result.

8. **Provider failure**  
   The application should return a controlled, useful error and diagnostic category.

9. **Unknown/unsupported policy data**  
   The assistant should explicitly state that the authoritative rule cannot be established.

### Assert characteristics, not only exact strings

Model wording can vary. Evaluation assertions should focus on properties such as:

- required facts are present;
- prohibited hallucinated facts are absent;
- correct authoritative tool/service was used;
- no cross-tenant/unauthorized information appears;
- answer length/structure fits the intent;
- calculation detail is omitted or included appropriately;
- uncertainty is stated when data is missing;
- write operations still require confirmation;
- sensitive internal/tool information is not exposed.

Exact-string assertions are still useful for deterministic application errors, API contracts and UI labels, but should not be the only AI-quality test method.

### Suggested evaluation record

Each regression case should capture:

| Field | Example |
|---|---|
| ID | `AI-QUALITY-001` |
| user intent | explain current entitlement |
| fixture/context | staff, tenant, policy, effective date |
| expected tool(s) | current entitlement detail tool |
| required facts | 14-day full-year policy; prorated result |
| prohibited behaviour | inventing another policy; cross-tenant data |
| expected shape | <= 2 short paragraphs unless calculation requested |
| providers/models tested | configured supported set |

When a production defect or poor answer is fixed, add a regression case representing that behaviour before considering the learning complete.

---

## 10. Engineering review checklist

Use this checklist when changing prompts, tools, provider integration or assistant-facing business flows.

### Answer quality

- [ ] Does the answer address the user's actual question first?
- [ ] Is it the shortest response that fully answers the question?
- [ ] Does the response shape match the intent?
- [ ] Is unnecessary calculation/configuration detail omitted?
- [ ] Are facts clearly distinguished from uncertainty?

### Authority and domain correctness

- [ ] Is the LLM calculating something that a backend service/tool should calculate?
- [ ] Does the answer use the current/generated staff record where appropriate?
- [ ] Are jurisdiction and effective dates handled explicitly?
- [ ] Are policy, eligibility, entitlement and balance concepts kept distinct?
- [ ] If authoritative data is missing, does the assistant avoid guessing?

### Security

- [ ] Could this expose data across tenants or permissions?
- [ ] Does every write still require the established confirmation path?
- [ ] Are identity, tenant and authorization derived from backend context rather than prompt claims?
- [ ] Are sensitive fields excluded/redacted from logs and diagnostics?

### Reliability

- [ ] Are provider/model differences isolated from business behaviour where practical?
- [ ] Are retryable and non-retryable errors distinguished?
- [ ] Does timeout/retry behaviour fit inside the outer request budget?
- [ ] Can failures be diagnosed from provider/model/request/tool metadata without sensitive payloads?

### Regression protection

- [ ] Is there a behavioural evaluation case for the change or defect?
- [ ] Are required facts and prohibited behaviours defined?
- [ ] Can the same case be run against each supported provider/model where relevant?
- [ ] Do normal backend/frontend tests and coverage gates still pass for any code changes?

---

## 11. Established guidance vs future improvements

Everything in sections 1-10 represents the current engineering direction and should be followed unless a future design deliberately replaces it.

The items below are **future improvements**, not statements that the repository already implements them.

### Intent-aware response templates

Introduce lightweight response policies/templates for common intent classes such as:

- explanation;
- entitlement/balance summary;
- policy comparison;
- approval/permission check;
- action proposal;
- troubleshooting.

The goal is consistent shape without turning the assistant into rigid hard-coded prose.

### Automated AI evaluation in CI

Create a deterministic fixture-based evaluation harness that can run representative questions and score required/prohibited characteristics. Keep live-provider tests separate from fast deterministic unit/integration gates where cost and nondeterminism matter.

### Provider/model comparison

Run the same evaluation dataset against supported provider/model candidates before changing the production default. Compare correctness, tool use, latency, verbosity and failure handling rather than selecting a model from anecdotal prompts.

### Response-quality telemetry

Explore privacy-safe metrics such as:

- response latency;
- tool success/failure rate;
- provider failure category;
- user retry/rephrase rate;
- confirmation completion rate;
- response length distribution by intent.

Do not introduce telemetry that requires indiscriminate storage of sensitive prompt/tool content.

### Better structured tools for common leave questions

Where evaluations show repeated model-side joining or interpretation, introduce higher-level backend DTOs/tools that return exactly the authoritative view needed for common questions.

---

## 12. How to record new learnings

When a new AI defect or improvement is discovered:

1. capture the concrete user-visible problem;
2. identify whether the root cause is prompt, tool contract, business-data retrieval, provider integration, security, timeout/retry behaviour or UI presentation;
3. fix the problem at the lowest authoritative layer possible;
4. add or update a regression/evaluation case;
5. update this playbook only when the lesson generalizes beyond the one defect;
6. keep provider setup/runbook details in the operational docs rather than duplicating them here.

This keeps the document useful as an engineering playbook instead of turning it into a chronological incident log.
