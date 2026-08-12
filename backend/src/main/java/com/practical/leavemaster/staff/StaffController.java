package com.practical.leavemaster.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/staff", "/api/staff"})
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public List<Staff> getAll() {
        return staffService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Staff> getById(@PathVariable String id) {
        return staffService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Staff staff) {
        try {
            Staff saved = staffService.save(staff);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Staff staff) {
        try {
            Staff updated = staffService.update(id, staff);
            return ResponseEntity.ok(updated);
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            staffService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (StaffInUseException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{id}/terminate")
    public ResponseEntity<?> terminate(@PathVariable String id, @RequestParam LocalDate termDate) {
        try {
            TerminationResult result = staffService.terminate(id, termDate);
            return ResponseEntity.ok(result);
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
