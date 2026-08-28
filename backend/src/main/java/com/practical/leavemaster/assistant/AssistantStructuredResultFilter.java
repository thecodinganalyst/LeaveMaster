package com.practical.leavemaster.assistant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AssistantStructuredResultFilter {

    private static final String FOCUSED_ENTITLEMENT_TOOL = "getStaffLeaveEntitlement";

    private AssistantStructuredResultFilter() {
    }

    static List<AssistantDtos.StructuredResult> scope(List<AssistantDtos.StructuredResult> results) {
        if (results == null || results.isEmpty()) return results == null ? List.of() : List.copyOf(results);

        Set<String> leaveTypes = focusedLeaveTypes(results);
        if (leaveTypes.isEmpty()) return List.copyOf(results);

        Set<String> policyIds = relevantPolicyIds(results, leaveTypes);
        List<AssistantDtos.StructuredResult> scoped = new ArrayList<>();
        for (AssistantDtos.StructuredResult result : results) {
            Object data = switch (result.toolName()) {
                case "getStaffById", "getAllStaff" -> scopeStaffData(result.data(), leaveTypes);
                case "getLeaveEntitlementConfigurationByJurisdiction" -> filterListByField(result.data(), "leaveType", leaveTypes);
                case "getEntitlementPoliciesByJurisdiction" -> policyIds.isEmpty()
                        ? result.data() : filterListByField(result.data(), "id", policyIds);
                default -> result.data();
            };
            if (!isEmptyCollection(data)) {
                scoped.add(new AssistantDtos.StructuredResult(result.toolName(), data));
            }
        }
        return List.copyOf(scoped);
    }

    private static Set<String> focusedLeaveTypes(List<AssistantDtos.StructuredResult> results) {
        Set<String> leaveTypes = new LinkedHashSet<>();
        results.stream()
                .filter(result -> FOCUSED_ENTITLEMENT_TOOL.equals(result.toolName()))
                .map(AssistantDtos.StructuredResult::data)
                .forEach(data -> collectFieldValues(data, "leaveTypeName", leaveTypes));
        return leaveTypes;
    }

    private static Set<String> relevantPolicyIds(List<AssistantDtos.StructuredResult> results, Set<String> leaveTypes) {
        Set<String> policyIds = new LinkedHashSet<>();
        results.stream()
                .filter(result -> "getStaffById".equals(result.toolName()) || "getAllStaff".equals(result.toolName()))
                .map(AssistantDtos.StructuredResult::data)
                .forEach(data -> collectPolicyIds(data, leaveTypes, policyIds));
        return policyIds;
    }

    private static Object scopeStaffData(Object data, Set<String> leaveTypes) {
        if (data instanceof Map<?, ?> map) return scopeStaffMap(map, leaveTypes);
        if (data instanceof List<?> list) {
            return list.stream()
                    .map(item -> item instanceof Map<?, ?> map ? scopeStaffMap(map, leaveTypes) : item)
                    .toList();
        }
        return data;
    }

    private static Map<String, Object> scopeStaffMap(Map<?, ?> source, Set<String> leaveTypes) {
        Map<String, Object> copy = stringKeyMap(source);
        Object entitlements = copy.get("leaveEntitlements");
        if (entitlements instanceof List<?>) {
            copy.put("leaveEntitlements", filterListByField(entitlements, "leaveTypeName", leaveTypes));
        }
        return copy;
    }

    private static Object filterListByField(Object data, String field, Set<String> acceptedValues) {
        if (!(data instanceof List<?> list)) return data;
        return list.stream()
                .filter(item -> item instanceof Map<?, ?> map && matches(map.get(field), acceptedValues))
                .toList();
    }

    private static void collectPolicyIds(Object data, Set<String> leaveTypes, Set<String> policyIds) {
        if (data instanceof Map<?, ?> map) {
            Object entitlements = map.get("leaveEntitlements");
            if (entitlements instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> entitlement
                            && matches(entitlement.get("leaveTypeName"), leaveTypes)
                            && entitlement.get("policyId") != null) {
                        policyIds.add(entitlement.get("policyId").toString());
                    }
                }
            }
        } else if (data instanceof List<?> list) {
            list.forEach(item -> collectPolicyIds(item, leaveTypes, policyIds));
        }
    }

    private static void collectFieldValues(Object data, String field, Set<String> values) {
        if (data instanceof Map<?, ?> map) {
            Object value = map.get(field);
            if (value != null) values.add(value.toString());
        } else if (data instanceof List<?> list) {
            list.forEach(item -> collectFieldValues(item, field, values));
        }
    }

    private static boolean matches(Object value, Set<String> acceptedValues) {
        if (value == null) return false;
        return acceptedValues.stream().anyMatch(accepted -> accepted.equalsIgnoreCase(value.toString()));
    }

    private static boolean isEmptyCollection(Object value) {
        return value instanceof List<?> list && list.isEmpty();
    }

    private static Map<String, Object> stringKeyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return copy;
    }
}
