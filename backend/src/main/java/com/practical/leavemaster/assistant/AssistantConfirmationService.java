package com.practical.leavemaster.assistant;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class AssistantConfirmationService {
    private static final String PENDING = "PENDING";
    private static final String EXECUTED = "EXECUTED";

    private final AssistantPendingActionRepository repository;
    private final ToolCallbackProvider toolProvider;
    private final AppUserRepository userRepository;
    private final AssistantAuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${app.assistant.confirmation-ttl-seconds:300}")
    private long confirmationTtlSeconds;

    AssistantDtos.PendingAction issue(String toolName, Map<String, Object> arguments, String requiredAuthority,
                                      AppUser user, String conversationId) {
        validateTenant(toolName, arguments, user.getTenantId(), requiredAuthority);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofSeconds(confirmationTtlSeconds));
        String token = UUID.randomUUID().toString();
        try {
            repository.save(AssistantPendingAction.builder()
                    .confirmationToken(token)
                    .toolName(toolName)
                    .argumentsJson(objectMapper.writeValueAsString(arguments))
                    .requiredAuthority(requiredAuthority)
                    .actorLoginName(user.getLoginName())
                    .actorStaffId(user.getStaffId())
                    .tenantId(user.getTenantId())
                    .conversationId(conversationId)
                    .status(PENDING)
                    .createdAt(now)
                    .expiresAt(expiresAt)
                    .build());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to persist assistant action", e);
        }
        auditService.record(AssistantAuditService.ACTION_CONFIRMATION, user.getLoginName(), user.getTenantId(),
                conversationId, toolName, arguments, "PENDING", "Awaiting explicit user confirmation");
        return new AssistantDtos.PendingAction(toolName, Map.copyOf(arguments), requiredAuthority,
                user.getLoginName(), user.getStaffId(), user.getTenantId(), token, expiresAt);
    }

    @Transactional
    AssistantDtos.ConfirmationResponse confirm(String token, Authentication authentication) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("confirmationToken is required");
        AssistantPendingAction action = repository.findByTokenForUpdate(token)
                .orElseThrow(() -> new IllegalArgumentException("Confirmation token is invalid"));

        AppUser user = userRepository.findById(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated LeaveMaster user was not found"));
        validateActorAndTenant(action, user);
        requireAuthority(authentication, action.getRequiredAuthority());

        if (EXECUTED.equals(action.getStatus())) {
            return new AssistantDtos.ConfirmationResponse(action.getToolName(), "EXECUTED", action.getResultJson(), true);
        }
        if (!PENDING.equals(action.getStatus())) throw new IllegalArgumentException("Confirmation token is no longer usable");
        if (Instant.now().isAfter(action.getExpiresAt())) {
            action.setStatus("EXPIRED");
            repository.save(action);
            auditService.record(AssistantAuditService.ACTION_CONFIRMATION, user.getLoginName(), user.getTenantId(),
                    action.getConversationId(), action.getToolName(), readArguments(action), "EXPIRED", null);
            throw new IllegalArgumentException("Confirmation token has expired");
        }

        Map<String, Object> arguments = readArguments(action);
        validateTenant(action.getToolName(), arguments, user.getTenantId(), action.getRequiredAuthority());
        ToolCallback callback = java.util.Arrays.stream(toolProvider.getToolCallbacks())
                .filter(candidate -> action.getToolName().equals(candidate.getToolDefinition().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Confirmed tool is no longer available"));

        try {
            String result = callback.call(action.getArgumentsJson());
            action.setStatus(EXECUTED);
            action.setResultJson(result == null ? "" : result);
            action.setExecutedAt(Instant.now());
            repository.save(action);
            auditService.record(AssistantAuditService.TOOL_EXECUTION, user.getLoginName(), user.getTenantId(),
                    action.getConversationId(), action.getToolName(), arguments, "SUCCESS", null);
            return new AssistantDtos.ConfirmationResponse(action.getToolName(), EXECUTED, action.getResultJson(), false);
        } catch (RuntimeException e) {
            auditService.record(AssistantAuditService.TOOL_EXECUTION, user.getLoginName(), user.getTenantId(),
                    action.getConversationId(), action.getToolName(), arguments, "FAILED", e.getClass().getSimpleName());
            throw e;
        }
    }

    private void validateActorAndTenant(AssistantPendingAction action, AppUser user) {
        if (!action.getActorLoginName().equals(user.getLoginName())) throw new AccessDeniedException("Confirmation belongs to another user");
        if (!java.util.Objects.equals(action.getTenantId(), user.getTenantId())) throw new AccessDeniedException("Confirmation belongs to another tenant");
    }

    private void requireAuthority(Authentication authentication, String required) {
        boolean allowed = authentication.getAuthorities().stream().anyMatch(a -> required.equals(a.getAuthority()));
        if (!allowed) throw new AccessDeniedException("Missing " + required);
    }

    private void validateTenant(String toolName, Object value, String trustedTenant, String requiredAuthority) {
        if (trustedTenant == null || RbacPermissions.TENANT_WRITE.equals(requiredAuthority)) return;
        if (containsMismatchedTenant(value, trustedTenant)) throw new AccessDeniedException("Cross-tenant assistant action is not allowed");
    }

    private boolean containsMismatchedTenant(Object value, String trustedTenant) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey()).replace("_", "").toLowerCase(java.util.Locale.ROOT);
                if ("tenantid".equals(key) && entry.getValue() != null && !trustedTenant.equals(String.valueOf(entry.getValue()))) return true;
                if (containsMismatchedTenant(entry.getValue(), trustedTenant)) return true;
            }
        } else if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) if (containsMismatchedTenant(item, trustedTenant)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readArguments(AssistantPendingAction action) {
        try {
            Map<String, Object> result = objectMapper.readValue(action.getArgumentsJson(), Map.class);
            return result == null ? Map.of() : result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Stored confirmation arguments are invalid", e);
        }
    }
}
