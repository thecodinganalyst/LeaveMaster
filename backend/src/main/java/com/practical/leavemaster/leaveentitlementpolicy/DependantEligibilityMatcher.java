package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.leaveeligibility.StaffDependant;
import com.practical.leavemaster.leaveeligibility.StaffDependantRepository;
import com.practical.leavemaster.staff.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Evaluates one jurisdiction-neutral dependant predicate against one dependant at a time.
 *
 * <p>The criterion value is a semicolon-separated set of key/value pairs. Supported keys are:
 * relationship, citizenship, residency, age_lt, age_lte, age_gt, age_gte and youngest.
 * Example: {@code relationship=CHILD;citizenship=SG;age_lt=7}.</p>
 */
@Component
@RequiredArgsConstructor
public class DependantEligibilityMatcher {

    private final StaffDependantRepository dependantRepository;

    public boolean matches(Staff staff, LocalDate effectiveDate, String expression) {
        Map<String, String> criteria = parse(expression);
        List<StaffDependant> dependants = dependantRepository
                .findAllByTenantIdAndStaffId(staff.getTenantId(), staff.getId()).stream()
                .filter(dependant -> activeOn(dependant, effectiveDate))
                .toList();

        if (Boolean.parseBoolean(criteria.getOrDefault("youngest", "false"))) {
            String relationship = criteria.get("relationship");
            dependants = dependants.stream()
                    .filter(dependant -> relationship == null || equalsCode(dependant.getRelationshipCode(), relationship))
                    .filter(dependant -> dependant.getDateOfBirth() != null)
                    .max(Comparator.comparing(StaffDependant::getDateOfBirth))
                    .stream()
                    .toList();
        }
        return dependants.stream().anyMatch(dependant -> matches(dependant, effectiveDate, criteria));
    }

    static Map<String, String> parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Dependant criterion value is required");
        }
        Map<String, String> criteria = new LinkedHashMap<>();
        Arrays.stream(expression.split(";"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .forEach(token -> {
                    int equals = token.indexOf('=');
                    if (equals <= 0 || equals == token.length() - 1) {
                        throw new IllegalArgumentException("Invalid dependant criterion token: " + token);
                    }
                    String key = token.substring(0, equals).trim().toLowerCase(Locale.ROOT);
                    String value = token.substring(equals + 1).trim();
                    if (!supportedKey(key)) {
                        throw new IllegalArgumentException("Unsupported dependant criterion key: " + key);
                    }
                    criteria.put(key, value);
                });
        if (criteria.isEmpty()) {
            throw new IllegalArgumentException("At least one dependant criterion is required");
        }
        validateNumeric(criteria, "age_lt");
        validateNumeric(criteria, "age_lte");
        validateNumeric(criteria, "age_gt");
        validateNumeric(criteria, "age_gte");
        if (criteria.containsKey("youngest")
                && !criteria.get("youngest").equalsIgnoreCase("true")
                && !criteria.get("youngest").equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("youngest must be true or false");
        }
        return criteria;
    }

    private boolean matches(StaffDependant dependant, LocalDate effectiveDate, Map<String, String> criteria) {
        if (criteria.containsKey("relationship") && !equalsCode(dependant.getRelationshipCode(), criteria.get("relationship"))) {
            return false;
        }
        if (criteria.containsKey("citizenship") && !equalsCode(dependant.getCitizenshipCode(), criteria.get("citizenship"))) {
            return false;
        }
        if (criteria.containsKey("residency") && !equalsCode(dependant.getResidencyCode(), criteria.get("residency"))) {
            return false;
        }
        if (criteria.keySet().stream().anyMatch(key -> key.startsWith("age_"))) {
            if (dependant.getDateOfBirth() == null || dependant.getDateOfBirth().isAfter(effectiveDate)) {
                return false;
            }
            long age = ChronoUnit.YEARS.between(dependant.getDateOfBirth(), effectiveDate);
            if (criteria.containsKey("age_lt") && age >= number(criteria, "age_lt")) return false;
            if (criteria.containsKey("age_lte") && age > number(criteria, "age_lte")) return false;
            if (criteria.containsKey("age_gt") && age <= number(criteria, "age_gt")) return false;
            if (criteria.containsKey("age_gte") && age < number(criteria, "age_gte")) return false;
        }
        return true;
    }

    private boolean activeOn(StaffDependant dependant, LocalDate date) {
        return dependant.isActive()
                && (dependant.getEffectiveFrom() == null || !date.isBefore(dependant.getEffectiveFrom()))
                && (dependant.getEffectiveTo() == null || !date.isAfter(dependant.getEffectiveTo()));
    }

    private static boolean supportedKey(String key) {
        return switch (key) {
            case "relationship", "citizenship", "residency", "age_lt", "age_lte", "age_gt", "age_gte", "youngest" -> true;
            default -> false;
        };
    }

    private static void validateNumeric(Map<String, String> criteria, String key) {
        if (!criteria.containsKey(key)) return;
        try {
            if (Integer.parseInt(criteria.get(key)) < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a non-negative integer");
        }
    }

    private static long number(Map<String, String> criteria, String key) {
        return Long.parseLong(criteria.get(key));
    }

    private static boolean equalsCode(String actual, String expected) {
        return actual != null && expected != null && actual.trim().equalsIgnoreCase(expected.trim());
    }
}
