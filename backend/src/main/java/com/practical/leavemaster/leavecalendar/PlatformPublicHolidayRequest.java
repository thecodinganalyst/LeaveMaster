package com.practical.leavemaster.leavecalendar;

import java.time.LocalDate;

public record PlatformPublicHolidayRequest(
        String jurisdictionId,
        LocalDate holidayDate,
        String holidayName
) {
}
