package com.practical.leavemaster.leaveapprover;

import com.practical.leavemaster.staff.StaffNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/leave-approvers", "/api/leave-approvers"})
@RequiredArgsConstructor
public class LeaveApproverController {

    private final LeaveApproverService leaveApproverService;

    @GetMapping
    public List<LeaveApprover> getAll() {
        return leaveApproverService.findAll();
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<LeaveApprover>> getByStaffId(@PathVariable String staffId) {
        try {
            return ResponseEntity.ok(leaveApproverService.findByStaffId(staffId));
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveApprover> getById(@PathVariable String id) {
        return leaveApproverService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LeaveApproverRequest request) {
        try {
            LeaveApprover saved = leaveApproverService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody LeaveApproverRequest request) {
        try {
            LeaveApprover updated = leaveApproverService.update(id, request);
            return ResponseEntity.ok(updated);
        } catch (LeaveApproverNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            leaveApproverService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (LeaveApproverNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
