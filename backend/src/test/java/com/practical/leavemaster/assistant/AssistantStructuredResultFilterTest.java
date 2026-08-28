package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantStructuredResultFilterTest {

    @Test
    void shouldScopeSingleLeaveQuestionToResolvedAnnualLeaveEvidence() {
        List<AssistantDtos.StructuredResult> results = List.of(
                new AssistantDtos.StructuredResult("getStaffById", Map.of(
                        "id", "001",
                        "name", "Normal Staff",
                        "joinDate", "2026-08-03",
                        "jurisdictionId", "SG",
                        "leaveEntitlements", List.of(
                                entitlement("Annual Leave", "SG_ANNUAL_00_23", "5.79"),
                                entitlement("Sick Leave", "SG_SICK_03", "5")))),
                new AssistantDtos.StructuredResult("getStaffLeaveEntitlement", Map.of(
                        "staffName", "Normal Staff",
                        "joinDate", "2026-08-03",
                        "jurisdictionId", "SG",
                        "leaveTypeName", "Annual Leave",
                        "entitlement", "5.79")),
                new AssistantDtos.StructuredResult("getLeaveEntitlementConfigurationByJurisdiction", List.of(
                        config("Annual Leave"),
                        config("Sick Leave"),
                        config("Compassionate Leave"))),
                new AssistantDtos.StructuredResult("getEntitlementPoliciesByJurisdiction", List.of(
                        Map.of("id", "SG_ANNUAL_00_23", "name", "Annual 0-23"),
                        Map.of("id", "SG_SICK_03", "name", "Sick 3 months")))
        );

        List<AssistantDtos.StructuredResult> scoped = AssistantStructuredResultFilter.scope(results);

        assertThat(scoped).hasSize(4);
        Map<?, ?> staff = (Map<?, ?>) scoped.get(0).data();
        assertThat((List<?>) staff.get("leaveEntitlements"))
                .singleElement()
                .satisfies(item -> assertThat(((Map<?, ?>) item).get("leaveTypeName")).isEqualTo("Annual Leave"));

        assertThat((List<?>) scoped.get(2).data())
                .singleElement()
                .satisfies(item -> assertThat(((Map<?, ?>) item).get("leaveType")).isEqualTo("Annual Leave"));
        assertThat((List<?>) scoped.get(3).data())
                .singleElement()
                .satisfies(item -> assertThat(((Map<?, ?>) item).get("id")).isEqualTo("SG_ANNUAL_00_23"));
    }

    @Test
    void shouldKeepAllResolvedLeaveTypesForComparisonQuestion() {
        List<AssistantDtos.StructuredResult> results = List.of(
                new AssistantDtos.StructuredResult("getStaffLeaveEntitlement", Map.of("leaveTypeName", "Annual Leave")),
                new AssistantDtos.StructuredResult("getStaffLeaveEntitlement", Map.of("leaveTypeName", "Sick Leave")),
                new AssistantDtos.StructuredResult("getLeaveEntitlementConfigurationByJurisdiction", List.of(
                        config("Annual Leave"), config("Sick Leave"), config("Marriage Leave")))
        );

        List<AssistantDtos.StructuredResult> scoped = AssistantStructuredResultFilter.scope(results);
        List<String> leaveTypes = ((List<?>) scoped.get(2).data()).stream()
                .map(item -> String.valueOf(((Map<?, ?>) item).get("leaveType")))
                .toList();

        assertThat(leaveTypes).containsExactly("Annual Leave", "Sick Leave");
    }

    @Test
    void shouldLeaveBroadSourceResultsUntouchedWhenNoFocusedEntitlementWasResolved() {
        List<AssistantDtos.StructuredResult> results = List.of(
                new AssistantDtos.StructuredResult("getLeaveEntitlementConfigurationByJurisdiction", List.of(
                        config("Annual Leave"), config("Sick Leave")))
        );

        assertThat(AssistantStructuredResultFilter.scope(results)).isEqualTo(results);
    }

    @Test
    void chatResponseShouldApplySourceScopingBeforeReturningToClient() {
        var response = new AssistantDtos.ChatResponse(
                "conversation-1",
                "Annual Leave is 5.79 days.",
                List.of(),
                List.of(
                        new AssistantDtos.StructuredResult("getStaffLeaveEntitlement", Map.of("leaveTypeName", "Annual Leave")),
                        new AssistantDtos.StructuredResult("getLeaveEntitlementConfigurationByJurisdiction", List.of(
                                config("Annual Leave"), config("Sick Leave"))))
        );

        assertThat((List<?>) response.structuredResults().get(1).data()).hasSize(1);
    }

    private Map<String, Object> entitlement(String leaveType, String policyId, String amount) {
        return Map.of("leaveTypeName", leaveType, "policyId", policyId, "entitlement", amount);
    }

    private Map<String, Object> config(String leaveType) {
        return Map.of("leaveType", leaveType, "policies", List.of());
    }
}
