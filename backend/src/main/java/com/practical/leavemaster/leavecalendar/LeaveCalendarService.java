package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaveCalendarService {
    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";

    private final LeaveCalendarRepository leaveCalendarRepository;
    private final TenantActivityService tenantActivityService;
    private final AppUserRepository appUserRepository;

    public List<LeaveCalendar> findAll() {
        Optional<AppUser> user = currentUser();
        if (user.isEmpty()) return leaveCalendarRepository.findAllByOrderByStartAsc();
        if (isPlatformAdmin(user.get())) {
            return leaveCalendarRepository.findAll().stream()
                    .filter(calendar -> calendar.getScope() == ConfigurationScope.PLATFORM_TEMPLATE && calendar.getTenantId() == null)
                    .sorted(Comparator.comparing(LeaveCalendar::getStart))
                    .toList();
        }
        return leaveCalendarRepository.findAllByTenantIdOrderByStartAsc(requiredTenantId(user.get())).stream()
                .filter(calendar -> calendar.getScope() == ConfigurationScope.TENANT)
                .toList();
    }

    /**
     * Returns platform holiday-calendar templates for a jurisdiction that overlap the requested calendar year.
     * This is intentionally read-only and does not provision tenant calendars.
     */
    public List<LeaveCalendar> findPlatformTemplates(String jurisdictionId, int year) {
        if (jurisdictionId == null || jurisdictionId.isBlank()) {
            throw new IllegalArgumentException("jurisdictionId is required");
        }
        if (year < 1900 || year > 9999) {
            throw new IllegalArgumentException("year must be between 1900 and 9999");
        }
        Optional<AppUser> user = currentUser();
        if (user.isPresent() && !isPlatformAdmin(user.get())) {
            throw new IllegalStateException("Only Platform Admin can access platform holiday calendar templates");
        }

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        return leaveCalendarRepository
                .findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, jurisdictionId.trim())
                .stream()
                .filter(calendar -> calendar.getTenantId() == null)
                .filter(calendar -> !calendar.getStart().isAfter(yearEnd) && !calendar.getEnd().isBefore(yearStart))
                .sorted(Comparator.comparing(LeaveCalendar::getStart))
                .toList();
    }

    public Optional<LeaveCalendar> findById(String id) {
        return leaveCalendarRepository.findById(id).filter(this::isAccessibleToCurrentUser);
    }

    public LeaveCalendar create(LeaveCalendar leaveCalendar) {
        LeaveCalendar normalized = copyOf(leaveCalendar);
        applyCurrentUsersScope(normalized);
        validate(normalized);

        if (normalized.getId() == null || normalized.getId().isBlank()) normalized.setId(defaultId(normalized));
        if (leaveCalendarRepository.existsById(normalized.getId())) {
            throw new LeaveCalendarConflictException("Leave calendar already exists: " + normalized.getId());
        }
        if (normalized.getScope() == ConfigurationScope.TENANT) {
            if (leaveCalendarRepository.existsByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(
                    normalized.getTenantId(), normalized.getJurisdictionId(), normalized.getEnd(), normalized.getStart())) {
                throw new LeaveCalendarConflictException("Leave calendar overlaps an existing tenant calendar for this jurisdiction");
            }
        } else {
            ensureNoOverlap(normalized, null);
        }

        LeaveCalendar saved = leaveCalendarRepository.save(normalized);
        touchTenant(saved);
        return saved;
    }

    public LeaveCalendar update(String id, LeaveCalendar requested) {
        LeaveCalendar existing = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave calendar not found: " + id));
        LeaveCalendar normalized = copyOf(requested);
        normalized.setId(id);
        normalized.setScope(existing.getScope());
        normalized.setTenantId(existing.getTenantId());
        normalized.setSourceTemplateId(existing.getSourceTemplateId());
        validate(normalized);
        ensureNoOverlap(normalized, id);
        LeaveCalendar saved = leaveCalendarRepository.save(normalized);
        touchTenant(saved);
        return saved;
    }

    public void delete(String id) {
        LeaveCalendar existing = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave calendar not found: " + id));
        leaveCalendarRepository.delete(existing);
        touchTenant(existing);
    }

    public Optional<LeaveCalendar> getCalendarFor(LocalDate date) {
        Optional<AppUser> user = currentUser();
        if (user.isPresent()) {
            if (isPlatformAdmin(user.get())) return Optional.empty();
            String tenantId = requiredTenantId(user.get());
            List<LeaveCalendar> matching = leaveCalendarRepository.findAllByTenantIdOrderByStartAsc(tenantId).stream()
                    .filter(calendar -> calendar.getScope() == ConfigurationScope.TENANT)
                    .filter(calendar -> !calendar.getStart().isAfter(date) && !calendar.getEnd().isBefore(date))
                    .toList();
            return matching.size() == 1 ? Optional.of(matching.get(0)) : Optional.empty();
        }

        Optional<LeaveCalendar> existing = leaveCalendarRepository.findByStartLessThanEqualAndEndGreaterThanEqual(date, date);
        if (existing.isPresent()) return existing;

        Optional<LeaveCalendar> latest = leaveCalendarRepository.findTopByOrderByEndDesc();
        if (latest.isEmpty() || !date.isAfter(latest.get().getEnd())) return Optional.empty();
        LeaveCalendar calendar = latest.get();
        while (date.isAfter(calendar.getEnd())) {
            calendar = leaveCalendarRepository.save(nextCalendarFrom(calendar));
            touchTenant(calendar);
        }
        return Optional.of(calendar);
    }

    public Optional<LeaveCalendar> getCalendarFor(String jurisdictionId, LocalDate date) {
        if (jurisdictionId == null || jurisdictionId.isBlank()) {
            throw new IllegalArgumentException("jurisdictionId is required");
        }
        Optional<AppUser> user = currentUser();
        if (user.isPresent()) {
            if (isPlatformAdmin(user.get())) return Optional.empty();
            return getTenantCalendarFor(requiredTenantId(user.get()), jurisdictionId.trim(), date);
        }
        return leaveCalendarRepository.findAll().stream()
                .filter(calendar -> calendar.getScope() == ConfigurationScope.TENANT)
                .filter(calendar -> jurisdictionId.trim().equals(calendar.getJurisdictionId()))
                .filter(calendar -> !calendar.getStart().isAfter(date) && !calendar.getEnd().isBefore(date))
                .findFirst();
    }

    private Optional<LeaveCalendar> getTenantCalendarFor(String tenantId, String jurisdictionId, LocalDate date) {
        Optional<LeaveCalendar> existing = leaveCalendarRepository
                .findByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(tenantId, jurisdictionId, date, date);
        if (existing.isPresent()) return existing;
        Optional<LeaveCalendar> latest = leaveCalendarRepository
                .findTopByTenantIdAndJurisdictionIdOrderByEndDesc(tenantId, jurisdictionId);
        if (latest.isEmpty() || !date.isAfter(latest.get().getEnd())) return Optional.empty();
        LeaveCalendar calendar = latest.get();
        while (date.isAfter(calendar.getEnd())) {
            calendar = leaveCalendarRepository.save(nextCalendarFrom(calendar));
            touchTenant(calendar);
        }
        return Optional.of(calendar);
    }

    private void ensureNoOverlap(LeaveCalendar calendar, String excludedId) {
        List<LeaveCalendar> candidates = calendar.getScope() == ConfigurationScope.PLATFORM_TEMPLATE
                ? leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, calendar.getJurisdictionId())
                : leaveCalendarRepository.findAllByTenantIdAndJurisdictionIdOrderByStartAsc(calendar.getTenantId(), calendar.getJurisdictionId());
        boolean overlaps = candidates.stream()
                .filter(existing -> excludedId == null || !excludedId.equals(existing.getId()))
                .anyMatch(existing -> !existing.getStart().isAfter(calendar.getEnd()) && !existing.getEnd().isBefore(calendar.getStart()));
        if (overlaps) {
            throw new LeaveCalendarConflictException(calendar.getScope() == ConfigurationScope.PLATFORM_TEMPLATE
                    ? "Leave calendar template overlaps an existing jurisdiction template"
                    : "Leave calendar overlaps an existing tenant calendar for this jurisdiction");
        }
    }

    private void validate(LeaveCalendar leaveCalendar) {
        if (leaveCalendar.getStart() == null || leaveCalendar.getEnd() == null) {
            throw new IllegalArgumentException("Leave calendar start and end dates are required");
        }
        if (leaveCalendar.getStart().isAfter(leaveCalendar.getEnd())) {
            throw new IllegalArgumentException("Leave calendar start date must be on or before end date");
        }
        if (leaveCalendar.getScope() == null) throw new IllegalArgumentException("Leave calendar scope is required");
        if (leaveCalendar.getJurisdictionId() == null || leaveCalendar.getJurisdictionId().isBlank()) {
            throw new IllegalArgumentException("jurisdictionId is required for leave calendars");
        }
        leaveCalendar.setJurisdictionId(leaveCalendar.getJurisdictionId().trim());
        if (leaveCalendar.getScope() == ConfigurationScope.PLATFORM_TEMPLATE) {
            if (leaveCalendar.getTenantId() != null) throw new IllegalArgumentException("Platform calendar templates must not have a tenantId");
        } else if (leaveCalendar.getTenantId() == null || leaveCalendar.getTenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId is required for tenant calendars");
        }

        List<PublicHoliday> publicHolidays = leaveCalendar.getPublicHolidays() == null ? List.of() : leaveCalendar.getPublicHolidays();
        Set<String> holidayKeys = new HashSet<>();
        for (PublicHoliday publicHoliday : publicHolidays) {
            if (publicHoliday.getHolidayDate() == null || publicHoliday.getHolidayName() == null || publicHoliday.getHolidayName().isBlank()) {
                throw new IllegalArgumentException("Public holidays require both holidayDate and holidayName");
            }
            if (publicHoliday.getHolidayDate().isBefore(leaveCalendar.getStart()) || publicHoliday.getHolidayDate().isAfter(leaveCalendar.getEnd())) {
                throw new IllegalArgumentException("Public holiday must be within the leave calendar range");
            }
            String normalizedName = publicHoliday.getHolidayName().trim().toLowerCase(Locale.ROOT);
            String key = publicHoliday.getHolidayDate() + "\u0000" + normalizedName;
            if (!holidayKeys.add(key)) {
                throw new IllegalArgumentException("Duplicate public holiday for date and name: "
                        + publicHoliday.getHolidayDate() + " / " + publicHoliday.getHolidayName().trim());
            }
        }
    }

    private LeaveCalendar nextCalendarFrom(LeaveCalendar current) {
        LocalDate nextStart = current.getStart().plusYears(1);
        LocalDate nextEnd = current.getEnd().plusYears(1);
        List<PublicHoliday> nextPublicHolidays = current.getPublicHolidays().stream()
                .map(publicHoliday -> PublicHoliday.builder()
                        .holidayDate(publicHoliday.getHolidayDate().plusYears(1))
                        .holidayName(publicHoliday.getHolidayName())
                        .build())
                .toList();
        return LeaveCalendar.builder()
                .id(current.getTenantId() == null
                        ? defaultId(current) + "-next"
                        : current.getTenantId() + ":" + current.getJurisdictionId() + ":" + nextStart + "_" + nextEnd)
                .start(nextStart)
                .end(nextEnd)
                .publicHolidays(nextPublicHolidays)
                .tenantId(current.getTenantId())
                .scope(current.getScope())
                .jurisdictionId(current.getJurisdictionId())
                .sourceTemplateId(current.getSourceTemplateId())
                .build();
    }

    private LeaveCalendar copyOf(LeaveCalendar leaveCalendar) {
        List<PublicHoliday> publicHolidays = leaveCalendar.getPublicHolidays() == null
                ? new ArrayList<>()
                : leaveCalendar.getPublicHolidays().stream()
                .map(publicHoliday -> PublicHoliday.builder()
                        .holidayDate(publicHoliday.getHolidayDate())
                        .holidayName(publicHoliday.getHolidayName())
                        .build())
                .toList();
        return LeaveCalendar.builder()
                .id(leaveCalendar.getId())
                .start(leaveCalendar.getStart())
                .end(leaveCalendar.getEnd())
                .publicHolidays(publicHolidays)
                .tenantId(leaveCalendar.getTenantId())
                .scope(leaveCalendar.getScope())
                .jurisdictionId(leaveCalendar.getJurisdictionId())
                .sourceTemplateId(leaveCalendar.getSourceTemplateId())
                .build();
    }

    private void applyCurrentUsersScope(LeaveCalendar calendar) {
        Optional<AppUser> user = currentUser();
        if (user.isEmpty()) return;
        if (isPlatformAdmin(user.get())) {
            calendar.setScope(ConfigurationScope.PLATFORM_TEMPLATE);
            calendar.setTenantId(null);
            calendar.setSourceTemplateId(null);
        } else {
            calendar.setScope(ConfigurationScope.TENANT);
            calendar.setTenantId(requiredTenantId(user.get()));
            calendar.setSourceTemplateId(null);
        }
    }

    private boolean isAccessibleToCurrentUser(LeaveCalendar calendar) {
        Optional<AppUser> user = currentUser();
        if (user.isEmpty()) return true;
        if (isPlatformAdmin(user.get())) {
            return calendar.getScope() == ConfigurationScope.PLATFORM_TEMPLATE && calendar.getTenantId() == null;
        }
        return calendar.getScope() == ConfigurationScope.TENANT && Objects.equals(requiredTenantId(user.get()), calendar.getTenantId());
    }

    private void touchTenant(LeaveCalendar calendar) {
        if (calendar.getScope() == ConfigurationScope.TENANT && calendar.getTenantId() != null) tenantActivityService.touch(calendar.getTenantId());
    }

    private Optional<AppUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) return Optional.empty();
        return appUserRepository.findById(authentication.getName());
    }

    private String requiredTenantId(AppUser user) {
        if (user.getTenantId() == null || user.getTenantId().isBlank()) {
            throw new IllegalStateException("Authenticated tenant user does not have a tenant id");
        }
        return user.getTenantId();
    }

    private boolean isPlatformAdmin(AppUser user) {
        return user != null && user.isActive() && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role != null && role.isActive() && PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(role.getId()));
    }

    private String defaultId(LeaveCalendar calendar) {
        String prefix = calendar.getScope() == ConfigurationScope.PLATFORM_TEMPLATE
                ? "template:" + calendar.getJurisdictionId() + ":"
                : calendar.getTenantId() + ":" + calendar.getJurisdictionId() + ":";
        return prefix + calendar.getStart() + "_" + calendar.getEnd();
    }
}
