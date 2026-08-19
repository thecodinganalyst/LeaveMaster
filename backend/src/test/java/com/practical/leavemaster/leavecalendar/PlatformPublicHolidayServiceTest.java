package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformPublicHolidayServiceTest {

    @Mock private LeaveCalendarRepository leaveCalendarRepository;
    @InjectMocks private PlatformPublicHolidayService service;

    @Test
    void findAllReturnsOnlyPlatformHolidaySeedDataAndSupportsFilters() {
        LeaveCalendar platform = platformCalendar("template:SG:2026-01-01_2026-12-31", "SG", 2026);
        platform.getPublicHolidays().add(holiday("New Year's Day", LocalDate.of(2026, 1, 1)));
        LeaveCalendar tenant = LeaveCalendar.builder()
                .id("tenant-1:2026").tenantId("tenant-1").scope(ConfigurationScope.TENANT)
                .start(LocalDate.of(2026, 1, 1)).end(LocalDate.of(2026, 12, 31))
                .jurisdictionId(null).publicHolidays(new ArrayList<>(List.of(holiday("Tenant Day", LocalDate.of(2026, 2, 1)))))
                .build();
        when(leaveCalendarRepository.findAll()).thenReturn(List.of(tenant, platform));

        List<PlatformPublicHoliday> result = service.findAll("SG", 2026);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().holidayName()).isEqualTo("New Year's Day");
        assertThat(result.getFirst().jurisdictionId()).isEqualTo("SG");
        assertThat(result.getFirst().calendarId()).isEqualTo(platform.getId());
    }

    @Test
    void createAddsHolidayToExistingPlatformTemplate() {
        LeaveCalendar platform = platformCalendar("template:SG:2026-01-01_2026-12-31", "SG", 2026);
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG"))
                .thenReturn(List.of(platform));
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformPublicHoliday created = service.create(new PlatformPublicHolidayRequest(
                " SG ", LocalDate.of(2026, 8, 9), " National Day ", null));

        assertThat(created.jurisdictionId()).isEqualTo("SG");
        assertThat(created.holidayName()).isEqualTo("National Day");
        assertThat(platform.getPublicHolidays()).extracting(PublicHoliday::getHolidayName).containsExactly("National Day");
        verify(leaveCalendarRepository).save(platform);
    }

    @Test
    void createCreatesBackingTemplateWhenYearDoesNotExist() {
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG"))
                .thenReturn(List.of());
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformPublicHoliday created = service.create(new PlatformPublicHolidayRequest(
                "SG", LocalDate.of(2028, 1, 1), "New Year's Day", null));

        assertThat(created.calendarId()).isEqualTo("template:SG:2028-01-01_2028-12-31");
        assertThat(created.year()).isEqualTo(2028);
    }

    @Test
    void createRejectsDuplicateDateAndName() {
        LeaveCalendar platform = platformCalendar("template:SG:2026-01-01_2026-12-31", "SG", 2026);
        platform.getPublicHolidays().add(holiday("National Day", LocalDate.of(2026, 8, 9)));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG"))
                .thenReturn(List.of(platform));

        assertThatThrownBy(() -> service.create(new PlatformPublicHolidayRequest(
                "SG", LocalDate.of(2026, 8, 9), " national day ", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate public holiday");
    }

    @Test
    void updateCanMoveHolidayToAnotherJurisdictionAndYear() {
        LeaveCalendar source = platformCalendar("template:SG:2026-01-01_2026-12-31", "SG", 2026);
        PublicHoliday existing = holiday("National Day", LocalDate.of(2026, 8, 9));
        source.getPublicHolidays().add(existing);
        String id = encodedId(source.getId(), existing.getHolidayDate(), existing.getHolidayName());
        when(leaveCalendarRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "MY"))
                .thenReturn(List.of());
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformPublicHoliday updated = service.update(id, new PlatformPublicHolidayRequest(
                "MY", LocalDate.of(2027, 8, 31), "National Day", "KL"));

        assertThat(source.getPublicHolidays()).isEmpty();
        assertThat(updated.jurisdictionId()).isEqualTo("MY");
        assertThat(updated.year()).isEqualTo(2027);
        assertThat(updated.locationId()).isEqualTo("KL");
    }

    @Test
    void findByIdAndDeleteUseSyntheticId() {
        LeaveCalendar platform = platformCalendar("template:SG:2026-01-01_2026-12-31", "SG", 2026);
        PublicHoliday existing = holiday("National Day", LocalDate.of(2026, 8, 9));
        platform.getPublicHolidays().add(existing);
        String id = encodedId(platform.getId(), existing.getHolidayDate(), existing.getHolidayName());
        when(leaveCalendarRepository.findById(platform.getId())).thenReturn(Optional.of(platform));
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.findById(id).holidayName()).isEqualTo("National Day");
        service.delete(id);

        assertThat(platform.getPublicHolidays()).isEmpty();
        verify(leaveCalendarRepository).save(platform);
    }

    @Test
    void rejectsInvalidRequestAndInvalidIds() {
        assertThatThrownBy(() -> service.create(new PlatformPublicHolidayRequest("", null, "", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findById("not-base64"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid platform public holiday id");
    }

    private LeaveCalendar platformCalendar(String id, String jurisdictionId, int year) {
        return LeaveCalendar.builder()
                .id(id)
                .start(LocalDate.of(year, 1, 1))
                .end(LocalDate.of(year, 12, 31))
                .tenantId(null)
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId(jurisdictionId)
                .publicHolidays(new ArrayList<>())
                .build();
    }

    private PublicHoliday holiday(String name, LocalDate date) {
        return PublicHoliday.builder().holidayName(name).holidayDate(date).build();
    }

    private String encodedId(String calendarId, LocalDate date, String name) {
        String raw = calendarId + "\n" + date + "\n" + name;
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
