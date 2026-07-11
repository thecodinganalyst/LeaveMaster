package com.practicallimits.spring_template.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/staff")
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
    public ResponseEntity<Staff> create(@RequestBody Staff staff) {
        Staff saved = staffService.save(staff);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Staff> update(@PathVariable String id, @RequestBody Staff staff) {
        try {
            Staff updated = staffService.update(id, staff);
            return ResponseEntity.ok(updated);
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            staffService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (StaffNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
