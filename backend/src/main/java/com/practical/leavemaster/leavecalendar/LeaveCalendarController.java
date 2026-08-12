package com.practical.leavemaster.leavecalendar;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/current")
    public ResponseEntity<LeaveCalendar> getCurrent(@RequestParam(required = false) LocalDate date) {
        return leaveCalendarService.getCalendarFor(date == null ? LocalDate.now() : date)
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
}
