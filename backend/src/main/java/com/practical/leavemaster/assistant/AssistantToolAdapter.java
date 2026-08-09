package com.practical.leavemaster.assistant;

import com.practical.leavemaster.user.AppUser;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.security.core.Authentication;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

final class AssistantToolAdapter {
    private AssistantToolAdapter() {
    }

    static ToolCallback[] forUser(
            ToolCallback[] callbacks,
            Authentication authentication,
            AppUser user,
            ObjectMapper objectMapper,
            List<AssistantDtos.PendingAction> pendingActions
    ) {
        return Arrays.stream(callbacks)
                .filter(callback -> isAuthorized(callback, authentication))
                .map(callback -> AssistantToolPolicy.WRITE_TOOLS.contains(toolName(callback))
                        ? pendingWrite(callback, authentication, user, objectMapper, pendingActions)
                        : callback)
                .toArray(ToolCallback[]::new);
    }

    private static boolean isAuthorized(ToolCallback callback, Authentication authentication) {
        String required = AssistantToolPolicy.REQUIRED_AUTHORITY.get(toolName(callback));
        if (required == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(authority -> required.equals(authority.getAuthority()));
    }

    private static String toolName(ToolCallback callback) {
        return callback.getToolDefinition().name();
    }

    private static ToolCallback pendingWrite(
            ToolCallback delegate,
            Authentication authentication,
            AppUser user,
            ObjectMapper objectMapper,
            List<AssistantDtos.PendingAction> pendingActions
    ) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return delegate.getToolDefinition();
            }

            @Override
            public ToolMetadata getToolMetadata() {
                return delegate.getToolMetadata();
            }

            @Override
            public String call(String toolInput) {
                return record(toolInput);
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                return record(toolInput);
            }

            private String record(String toolInput) {
                String toolName = toolName(delegate);
                String required = AssistantToolPolicy.REQUIRED_AUTHORITY.get(toolName);
                boolean authorized = authentication.getAuthorities().stream()
                        .anyMatch(authority -> required.equals(authority.getAuthority()));
                if (!authorized) {
                    throw new org.springframework.security.access.AccessDeniedException("Missing " + required);
                }

                Map<String, Object> arguments;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsed = objectMapper.readValue(toolInput, Map.class);
                    arguments = parsed == null ? Map.of() : Map.copyOf(parsed);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid tool arguments", e);
                }

                pendingActions.add(new AssistantDtos.PendingAction(
                        toolName,
                        arguments,
                        required,
                        user.getLoginName(),
                        user.getStaffId(),
                        user.getTenantId()
                ));
                return "Action requires explicit user confirmation and has not been executed.";
            }
        };
    }
}
