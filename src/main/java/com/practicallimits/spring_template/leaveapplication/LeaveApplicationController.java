package com.practicallimits.spring_template.leaveapplication;

import com.practicallimits.spring_template.leavecalendar.LeaveCalendarNotFoundException;
import com.practicallimits.spring_template.staff.StaffNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leave-applications")
@RequiredArgsConstructor
public class LeaveApplicationController {

    private final LeaveApplicationService leaveApplicationService;

    @GetMapping
    public List<LeaveApplication> getAll() {
        return leaveApplicationService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveApplication> getById(@PathVariable String id) {
        return leaveApplicationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<?> getByStaffId(
            @PathVariable String staffId,
            @RequestParam(required = false) LocalDate date) {
        try {
            LocalDate filterDate = date != null ? date : LocalDate.now();
            List<LeaveApplication> applications = leaveApplicationService.findByStaffId(staffId, filterDate);
            return ResponseEntity.ok(applications);
        } catch (StaffNotFoundException | LeaveCalendarNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/staff/{staffId}/balance")
    public ResponseEntity<List<LeaveBalance>> getLeaveBalances(@PathVariable String staffId) {
        try {
            return ResponseEntity.ok(leaveApplicationService.getLeaveBalances(staffId));
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> apply(@RequestBody LeaveApplicationRequest request) {
        try {
            List<LeaveApplication> applications = leaveApplicationService.apply(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(applications);
        } catch (StaffNotFoundException | com.practicallimits.spring_template.leavetype.LeaveTypeNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody LeaveApplication leaveApplication) {
        try {
            LeaveApplication updated = leaveApplicationService.update(id, leaveApplication);
            return ResponseEntity.ok(updated);
        } catch (LeaveApplicationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            leaveApplicationService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (LeaveApplicationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/approve-cancellation")
    public ResponseEntity<?> approveCancellation(@PathVariable String id) {
        try {
            LeaveApplication updated = leaveApplicationService.approveCancellation(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (LeaveApplicationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/reject-cancellation")
    public ResponseEntity<?> rejectCancellation(@PathVariable String id) {
        try {
            LeaveApplication updated = leaveApplicationService.rejectCancellation(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (LeaveApplicationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
