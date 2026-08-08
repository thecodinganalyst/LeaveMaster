package com.practical.leavemaster.leavetype;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @GetMapping
    public List<LeaveType> getAll() {
        return leaveTypeService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveType> getById(@PathVariable String id) {
        return leaveTypeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LeaveType> create(@RequestBody LeaveType leaveType) {
        LeaveType saved = leaveTypeService.save(leaveType);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveType> update(@PathVariable String id, @RequestBody LeaveType leaveType) {
        try {
            LeaveType updated = leaveTypeService.update(id, leaveType);
            return ResponseEntity.ok(updated);
        } catch (LeaveTypeNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            leaveTypeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (LeaveTypeNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (LeaveTypeInUseException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
