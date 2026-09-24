# AskLeaveMaestro observability and feedback

AskLeaveMaestro records a privacy-minimised quality event for each completed request. The correlation ID is the assistant conversation/request identifier and links application logs, tool activity and quality data.

Stored request metadata is limited to tenant ID, one role/authority label, intent category, tool names, provider/model, latency, configured retry allowance, success/failure and a bounded failure category. Prompt text, model response text, tool arguments, staff fields, secrets, credentials and authorization headers are deliberately absent from the quality-event schema.

Failure categories currently distinguish MODEL_TIMEOUT, MODEL_FAILURE, TOOL_FAILURE and AUTHORIZATION_DENIED. Existing audit events continue to redact credential-like argument keys; quality telemetry never stores arguments at all.

Clients may POST /api/assistant/feedback with correlationId, rating (-1 or 1), and an optional short category. Free-text comments are intentionally not accepted, preventing accidental storage of employee data. Platform administrators can retrieve aggregate request counts, success/failure counts, average latency and failure-category counts from GET /api/assistant/quality. Raw quality events are not exposed through this API.

Token usage and cost are not recorded until the configured provider exposes reliable usage metadata through the application integration. Evaluation workflows should retain their model/prompt/version metadata in their own report artifacts; correlation IDs can be used when a live evaluation calls the deployed assistant.
