package com.practical.leavemaster.leavecalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveCalendarRepository extends JpaRepository<LeaveCalendar, String> {

    List<LeaveCalendar> findAllByOrderByStartAsc();

    Optional<LeaveCalendar> findByStartLessThanEqualAndEndGreaterThanEqual(LocalDate start, LocalDate end);

    Optional<LeaveCalendar> findTopByOrderByEndDesc();

    boolean existsByStartLessThanEqualAndEndGreaterThanEqual(LocalDate end, LocalDate start);

    void deleteAllByTenantId(String tenantId);
}
