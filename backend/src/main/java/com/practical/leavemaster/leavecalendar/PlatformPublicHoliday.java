package com.practical.leavemaster.leavecalendar;

import java.time.LocalDate;

public record PlatformPublicHoliday(
        String id,
        String calendarId,
        String jurisdictionId,
        int year,
        LocalDate holidayDate,
        String holidayName
) {
}
