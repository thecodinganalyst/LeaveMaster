package com.practicallimits.spring_template.leaveapprover;

import com.practicallimits.spring_template.staff.StaffNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leave-approvers")
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
    public ResponseEntity<LeaveApprover> create(@RequestBody LeaveApprover leaveApprover) {
        LeaveApprover saved = leaveApproverService.save(leaveApprover);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveApprover> update(@PathVariable String id, @RequestBody LeaveApprover leaveApprover) {
        try {
            LeaveApprover updated = leaveApproverService.update(id, leaveApprover);
            return ResponseEntity.ok(updated);
        } catch (LeaveApproverNotFoundException e) {
            return ResponseEntity.notFound().build();
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
