package com.practical.leavemaster.leaveeligibility;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping({"/staff/{staffId}", "/api/staff/{staffId}"})
@RequiredArgsConstructor
public class LeaveEligibilityFactController {

    private final LeaveEligibilityFactService service;

    @GetMapping("/dependants")
    public ResponseEntity<List<StaffDependant>> getDependants(@PathVariable String staffId) {
        try {
            return ResponseEntity.ok(service.findDependants(staffId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/dependants/{dependantId}")
    public ResponseEntity<StaffDependant> getDependant(@PathVariable String staffId, @PathVariable String dependantId) {
        try {
            return ResponseEntity.ok(service.findDependant(staffId, dependantId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/dependants")
    public ResponseEntity<?> createDependant(@PathVariable String staffId, @RequestBody StaffDependantWriteRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.createDependant(staffId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/dependants/{dependantId}")
    public ResponseEntity<?> updateDependant(@PathVariable String staffId, @PathVariable String dependantId,
                                             @RequestBody StaffDependantWriteRequest request) {
        try {
            return ResponseEntity.ok(service.updateDependant(staffId, dependantId, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/dependants/{dependantId}")
    public ResponseEntity<?> deleteDependant(@PathVariable String staffId, @PathVariable String dependantId) {
        try {
            service.deleteDependant(staffId, dependantId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/qualifying-events")
    public ResponseEntity<List<QualifyingLeaveEvent>> getEvents(@PathVariable String staffId) {
        try {
            return ResponseEntity.ok(service.findEvents(staffId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/qualifying-events/{eventId}")
    public ResponseEntity<QualifyingLeaveEvent> getEvent(@PathVariable String staffId, @PathVariable String eventId) {
        try {
            return ResponseEntity.ok(service.findEvent(staffId, eventId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/qualifying-events")
    public ResponseEntity<?> createEvent(@PathVariable String staffId, @RequestBody QualifyingLeaveEventWriteRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.createEvent(staffId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/qualifying-events/{eventId}")
    public ResponseEntity<?> updateEvent(@PathVariable String staffId, @PathVariable String eventId,
                                         @RequestBody QualifyingLeaveEventWriteRequest request) {
        try {
            return ResponseEntity.ok(service.updateEvent(staffId, eventId, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/qualifying-events/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String staffId, @PathVariable String eventId) {
        try {
            service.deleteEvent(staffId, eventId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
