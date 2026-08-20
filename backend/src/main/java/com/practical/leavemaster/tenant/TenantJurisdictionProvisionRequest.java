package com.practical.leavemaster.tenant;

import java.time.LocalDate;

public record TenantJurisdictionProvisionRequest(
        String jurisdictionId,
        Boolean includePublicHolidays,
        Boolean includeLeaveConfiguration,
        LocalDate calendarStart,
        LocalDate calendarEnd
) {
    public boolean shouldIncludePublicHolidays() {
        return Boolean.TRUE.equals(includePublicHolidays);
    }

    public boolean shouldIncludeLeaveConfiguration() {
        return Boolean.TRUE.equals(includeLeaveConfiguration);
    }

    public TenantJurisdictionProvisionRequest withCalendarDefaults(LocalDate start, LocalDate end) {
        return new TenantJurisdictionProvisionRequest(
                jurisdictionId,
                includePublicHolidays,
                includeLeaveConfiguration,
                calendarStart != null ? calendarStart : start,
                calendarEnd != null ? calendarEnd : end
        );
    }
}
