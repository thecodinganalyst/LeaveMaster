package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leavecalendar.LeaveCalendarNotFoundException;
import com.practical.leavemaster.staff.StaffNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leave-applications")
@RequiredArgsConstructor
public class LeaveApplicationController {

    private final LeaveApplicationService leaveApplicationService;

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam String staffId) {
        try {
            return ResponseEntity.ok(leaveApplicationService.findVisibleForStaff(staffId));
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveApplication> getById(@PathVariable String id) {
        return leaveApplicationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/approver/{approverId}")
    public ResponseEntity<?> getByApproverId(@PathVariable String approverId) {
        try {
            List<LeaveApplication> applications = leaveApplicationService.findPendingByApproverId(approverId);
            return ResponseEntity.ok(applications);
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> apply(
            @RequestPart("request") LeaveApplicationRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            List<LeaveApplication> applications = leaveApplicationService.apply(request, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(applications);
        } catch (StaffNotFoundException | com.practical.leavemaster.leavetype.LeaveTypeNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> applyJson(@RequestBody LeaveApplicationRequest request) {
        try {
            List<LeaveApplication> applications = leaveApplicationService.apply(request, null);
            return ResponseEntity.status(HttpStatus.CREATED).body(applications);
        } catch (StaffNotFoundException | com.practical.leavemaster.leavetype.LeaveTypeNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAttachment(
            @PathVariable String id,
            @RequestPart("file") MultipartFile file) {
        try {
            LeaveApplication updated = leaveApplicationService.uploadAttachment(id, file);
            return ResponseEntity.ok(updated);
        } catch (LeaveApplicationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/attachment")
    public void getAttachment(@PathVariable String id, HttpServletResponse response) throws IOException {
        try {
            leaveApplicationService.serveAttachment(id, response);
        } catch (LeaveApplicationNotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
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

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id, @RequestParam String approverId) {
        try {
            LeaveApplication updated = leaveApplicationService.approve(id, approverId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (LeaveApplicationNotFoundException | StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id, @RequestParam String approverId) {
        try {
            LeaveApplication updated = leaveApplicationService.reject(id, approverId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (LeaveApplicationNotFoundException | StaffNotFoundException e) {
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
