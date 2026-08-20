package com.practical.leavemaster.leavecalendar;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/leave-calendars", "/api/leave-calendars"})
@RequiredArgsConstructor
public class LeaveCalendarController {

    private final LeaveCalendarService leaveCalendarService;

    @GetMapping
    public List<LeaveCalendar> getAll() {
        return leaveCalendarService.findAll();
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('JURISDICTION_READ')")
    public ResponseEntity<?> getTemplates(
            @RequestParam String jurisdictionId,
            @RequestParam int year) {
        try {
            return ResponseEntity.ok(leaveCalendarService.findPlatformTemplates(jurisdictionId, year));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<LeaveCalendar> getCurrent(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String jurisdictionId) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return (jurisdictionId == null || jurisdictionId.isBlank()
                ? leaveCalendarService.getCalendarFor(targetDate)
                : leaveCalendarService.getCalendarFor(jurisdictionId, targetDate))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveCalendar> getById(@PathVariable String id) {
        return leaveCalendarService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LeaveCalendar leaveCalendar) {
        try {
            LeaveCalendar saved = leaveCalendarService.create(leaveCalendar);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (LeaveCalendarConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody LeaveCalendar leaveCalendar) {
        try {
            return ResponseEntity.ok(leaveCalendarService.update(id, leaveCalendar));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (LeaveCalendarConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            leaveCalendarService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
