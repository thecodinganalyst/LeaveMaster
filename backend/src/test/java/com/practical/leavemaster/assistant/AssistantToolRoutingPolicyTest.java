package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantToolRoutingPolicyTest {

    @Test
    void shouldRouteExactLiveEvaluationPromptToFocusedEntitlementTool() {
        ToolCallback entitlement = callback("getStaffLeaveEntitlement");
        ToolCallback balance = callback("getLeaveBalances");
        ToolCallback leaveType = callback("getLeaveTypeById");

        ToolCallback[] routed = AssistantToolRoutingPolicy.route(
                new ToolCallback[]{balance, entitlement, leaveType},
                "Explain why I have my current annual leave entitlement.");

        assertThat(routed).containsExactly(entitlement);
    }

    @Test
    void shouldRecognizeCommonEntitlementExplanationPhrasings() {
        assertThat(AssistantToolRoutingPolicy.isEntitlementExplanation("Why do I have 13 days of Annual Leave?")).isTrue();
        assertThat(AssistantToolRoutingPolicy.isEntitlementExplanation("How was my annual leave entitlement calculated?")).isTrue();
        assertThat(AssistantToolRoutingPolicy.isEntitlementExplanation("Explain my sick leave entitlement.")).isTrue();
        assertThat(AssistantToolRoutingPolicy.isEntitlementExplanation("How did LeaveMaestro calculate my annual leave entitlement?")).isTrue();
    }

    @Test
    void shouldNotHijackBalanceListOrPolicyQuestions() {
        assertThat(AssistantToolRoutingPolicy.isEntitlementExplanation("How much annual leave do I have?")).isFalse();
        assertThat(AssistantToolRoutingPolicy.isEntitlementExplanation("Why is my annual leave balance 10 days?")).isFalse();
        assertThat(AssistantToolRoutingPolicy.isEntitlementExplanation("What are my current leave entitlements?")).isFalse();
        assertThat(AssistantToolRoutingPolicy.isEntitlementExplanation("Does annual leave get prorated?")).isFalse();
    }

    @Test
    void shouldKeepOriginalToolsWhenFocusedToolIsUnavailable() {
        ToolCallback balance = callback("getLeaveBalances");
        ToolCallback leaveType = callback("getLeaveTypeById");
        ToolCallback[] original = new ToolCallback[]{balance, leaveType};

        ToolCallback[] routed = AssistantToolRoutingPolicy.route(
                original,
                "Explain why I have my current annual leave entitlement.");

        assertThat(routed).containsExactly(original);
    }

    private ToolCallback callback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{\"type\":\"object\"}")
                .build());
        return callback;
    }
}
