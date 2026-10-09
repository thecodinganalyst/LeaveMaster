package com.practical.leavemaster.assistant;

import com.practical.leavemaster.rbac.RbacPermissions;
import org.springframework.security.core.Authentication;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Refuses explicit requests for private balances outside the authenticated user's scope
 * before a model can select a broad read tool. Tool-level authorization remains mandatory.
 */
final class AssistantPrivateBalanceGuard {
    private static final Pattern FOREIGN_TENANT = Pattern.compile(
            "(?i)\\b(?:in|from|of|for)\\s+(?:another|other|different|foreign|tenant[-\\s]?[a-z0-9-]+)\\s+tenant\\b|\\btenant[-\\s]?[a-z0-9-]+\\b");
    private static final Pattern OTHER_EMPLOYEE = Pattern.compile(
            "(?i)\\b(?:another|other|someone else's|different)\\s+(?:employee|staff|person)\\b");

    private AssistantPrivateBalanceGuard() {}

    static String refusal(String message, Authentication authentication) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (!(lower.contains("balance") || lower.contains("entitlement"))) return null;
        if (FOREIGN_TENANT.matcher(message).find()) {
            return "I cannot access private leave balances from another tenant.";
        }
        boolean broadStaffRead = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> RbacPermissions.STAFF_READ.equals(a.getAuthority()));
        if (!broadStaffRead && OTHER_EMPLOYEE.matcher(message).find()) {
            return "I cannot access another employee's private leave balance without permission.";
        }
        return null;
    }
}
