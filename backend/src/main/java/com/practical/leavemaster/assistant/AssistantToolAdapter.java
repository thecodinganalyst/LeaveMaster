package com.practical.leavemaster.assistant;

import com.practical.leavemaster.user.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
final class AssistantToolAdapter {
    private AssistantToolAdapter() {
    }

    static ToolCallback[] forUser(
            ToolCallback[] callbacks,
            Authentication authentication,
            AppUser user,
            ObjectMapper objectMapper,
            List<AssistantDtos.PendingAction> pendingActions,
            String conversationId,
            AssistantConfirmationService confirmationService,
            AssistantAuditService auditService
    ) {
        return forUser(callbacks, authentication, user, objectMapper, pendingActions, new ArrayList<>(),
                conversationId, confirmationService, auditService, new AssistantRequestTrace());
    }

    static ToolCallback[] forUser(
            ToolCallback[] callbacks,
            Authentication authentication,
            AppUser user,
            ObjectMapper objectMapper,
            List<AssistantDtos.PendingAction> pendingActions,
            List<AssistantDtos.StructuredResult> structuredResults,
            String conversationId,
            AssistantConfirmationService confirmationService,
            AssistantAuditService auditService
    ) {
        return forUser(callbacks, authentication, user, objectMapper, pendingActions, structuredResults,
                conversationId, confirmationService, auditService, new AssistantRequestTrace());
    }

    static ToolCallback[] forUser(
            ToolCallback[] callbacks,
            Authentication authentication,
            AppUser user,
            ObjectMapper objectMapper,
            List<AssistantDtos.PendingAction> pendingActions,
            List<AssistantDtos.StructuredResult> structuredResults,
            String conversationId,
            AssistantConfirmationService confirmationService,
            AssistantAuditService auditService,
            AssistantRequestTrace trace
    ) {
        return Arrays.stream(callbacks)
                .filter(callback -> isAuthorized(callback, authentication))
                .map(callback -> AssistantToolPolicy.WRITE_TOOLS.contains(toolName(callback))
                        ? pendingWrite(callback, authentication, user, objectMapper, pendingActions, conversationId,
                        confirmationService, trace)
                        : auditedRead(callback, user, objectMapper, structuredResults, conversationId, auditService, trace))
                .toArray(ToolCallback[]::new);
    }

    private static boolean isAuthorized(ToolCallback callback, Authentication authentication) {
        String required = AssistantToolPolicy.REQUIRED_AUTHORITY.get(toolName(callback));
        return required != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> required.equals(authority.getAuthority()));
    }

    private static String toolName(ToolCallback callback) {
        return callback.getToolDefinition().name();
    }

    private static ToolCallback pendingWrite(
            ToolCallback delegate,
            Authentication authentication,
            AppUser user,
            ObjectMapper objectMapper,
            List<AssistantDtos.PendingAction> pendingActions,
            String conversationId,
            AssistantConfirmationService confirmationService,
            AssistantRequestTrace trace
    ) {
        return wrapper(delegate, (toolInput, context) -> timedCall(delegate, conversationId, trace, () -> {
            String name = toolName(delegate);
            String required = AssistantToolPolicy.REQUIRED_AUTHORITY.get(name);
            if (authentication.getAuthorities().stream().noneMatch(a -> required.equals(a.getAuthority()))) {
                throw new AccessDeniedException("Missing " + required);
            }
            Map<String, Object> arguments = parseArguments(objectMapper, toolInput);
            pendingActions.add(confirmationService.issue(name, arguments, required, user, conversationId));
            return "Action requires explicit user confirmation and has not been executed.";
        }));
    }

    private static ToolCallback auditedRead(ToolCallback delegate, AppUser user, ObjectMapper objectMapper,
                                             List<AssistantDtos.StructuredResult> structuredResults,
                                             String conversationId, AssistantAuditService auditService,
                                             AssistantRequestTrace trace) {
        return wrapper(delegate, (toolInput, context) -> timedCall(delegate, conversationId, trace, () -> {
            Map<String, Object> arguments = parseArguments(objectMapper, toolInput);
            try {
                String result = context == null ? delegate.call(toolInput) : delegate.call(toolInput, context);
                if (AssistantToolPolicy.STRUCTURED_RESULT_TOOLS.contains(toolName(delegate))) {
                    structuredResults.add(new AssistantDtos.StructuredResult(toolName(delegate), parseResult(objectMapper, result)));
                }
                auditService.record(AssistantAuditService.TOOL_EXECUTION, user.getLoginName(), user.getTenantId(),
                        conversationId, toolName(delegate), arguments, "SUCCESS", null);
                return result;
            } catch (RuntimeException e) {
                auditService.record(AssistantAuditService.TOOL_EXECUTION, user.getLoginName(), user.getTenantId(),
                        conversationId, toolName(delegate), arguments, "FAILED", e.getClass().getSimpleName());
                throw e;
            }
        }));
    }

    private static String timedCall(ToolCallback delegate, String conversationId, AssistantRequestTrace trace,
                                    ToolInvocation invocation) {
        String name = toolName(delegate);
        int callNumber = trace.toolStarted(name);
        long startedAtNanos = System.nanoTime();
        log.info("Ask LeaveMaestro tool started: conversationId={}, tool={}, toolCallNumber={}",
                conversationId, name, callNumber);
        try {
            String result = invocation.call();
            trace.toolCompleted(name);
            log.info("Ask LeaveMaestro tool completed: conversationId={}, tool={}, toolCallNumber={}, durationMs={}, status=SUCCESS",
                    conversationId, name, callNumber, elapsedMillis(startedAtNanos));
            return result;
        } catch (RuntimeException e) {
            trace.toolFailed(name);
            log.warn("Ask LeaveMaestro tool completed: conversationId={}, tool={}, toolCallNumber={}, durationMs={}, status=FAILED, exceptionType={}",
                    conversationId, name, callNumber, elapsedMillis(startedAtNanos), e.getClass().getName());
            throw e;
        }
    }

    private static long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    private static Map<String, Object> parseArguments(ObjectMapper objectMapper, String toolInput) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(toolInput, Map.class);
            return parsed == null ? Map.of() : Map.copyOf(parsed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid tool arguments", e);
        }
    }

    private static Object parseResult(ObjectMapper objectMapper, String result) {
        if (result == null || result.isBlank()) return result == null ? "" : result;
        try {
            return objectMapper.readValue(result, Object.class);
        } catch (Exception ignored) {
            return result;
        }
    }

    private static ToolCallback wrapper(ToolCallback delegate, CallbackCall call) {
        return new ToolCallback() {
            @Override public ToolDefinition getToolDefinition() { return delegate.getToolDefinition(); }
            @Override public ToolMetadata getToolMetadata() { return delegate.getToolMetadata(); }
            @Override public String call(String toolInput) { return call.invoke(toolInput, null); }
            @Override public String call(String toolInput, ToolContext toolContext) { return call.invoke(toolInput, toolContext); }
        };
    }

    @FunctionalInterface
    private interface CallbackCall {
        String invoke(String input, ToolContext context);
    }

    @FunctionalInterface
    private interface ToolInvocation {
        String call();
    }
}
