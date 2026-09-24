package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveCalendarRepository extends JpaRepository<LeaveCalendar, String> {

    @EntityGraph(attributePaths = "publicHolidays")
    List<LeaveCalendar> findAllByOrderByStartAsc();

    @EntityGraph(attributePaths = "publicHolidays")
    List<LeaveCalendar> findAllByTenantIdOrderByStartAsc(String tenantId);

    List<LeaveCalendar> findAllByTenantIdAndJurisdictionIdOrderByStartAsc(String tenantId, String jurisdictionId);

    List<LeaveCalendar> findAllByScopeAndJurisdictionId(ConfigurationScope scope, String jurisdictionId);

    @EntityGraph(attributePaths = "publicHolidays")
    Optional<LeaveCalendar> findByStartLessThanEqualAndEndGreaterThanEqual(LocalDate start, LocalDate end);

    Optional<LeaveCalendar> findByTenantIdAndStartLessThanEqualAndEndGreaterThanEqual(String tenantId, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = "publicHolidays")
    Optional<LeaveCalendar> findByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(
            String tenantId, String jurisdictionId, LocalDate start, LocalDate end);

    Optional<LeaveCalendar> findByTenantIdAndJurisdictionIdAndStartAndEnd(
            String tenantId, String jurisdictionId, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = "publicHolidays")
    Optional<LeaveCalendar> findTopByOrderByEndDesc();

    Optional<LeaveCalendar> findTopByTenantIdOrderByEndDesc(String tenantId);

    @EntityGraph(attributePaths = "publicHolidays")
    Optional<LeaveCalendar> findTopByTenantIdAndJurisdictionIdOrderByEndDesc(String tenantId, String jurisdictionId);

    boolean existsByStartLessThanEqualAndEndGreaterThanEqual(LocalDate end, LocalDate start);

    boolean existsByTenantIdAndStartLessThanEqualAndEndGreaterThanEqual(String tenantId, LocalDate end, LocalDate start);

    boolean existsByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(
            String tenantId, String jurisdictionId, LocalDate end, LocalDate start);

    boolean existsByTenantIdAndSourceTemplateId(String tenantId, String sourceTemplateId);

    void deleteAllByTenantId(String tenantId);
}
