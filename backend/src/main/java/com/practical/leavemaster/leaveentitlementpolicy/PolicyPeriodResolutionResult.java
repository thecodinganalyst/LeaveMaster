package com.practical.leavemaster.leaveentitlementpolicy;

import java.time.LocalDate;

/**
 * Result of looking for the first matching policy within a date range.
 */
public record PolicyPeriodResolutionResult(
        PolicyResolutionResult resolution,
        LocalDate matchedDate,
        boolean templatesFound) {
}
