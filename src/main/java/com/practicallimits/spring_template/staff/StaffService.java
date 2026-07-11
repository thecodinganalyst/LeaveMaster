package com.practicallimits.spring_template.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    public List<Staff> findAll() {
        return staffRepository.findAll();
    }

    public Optional<Staff> findById(String id) {
        return staffRepository.findById(id);
    }

    public Staff save(Staff staff) {
        return staffRepository.save(staff);
    }

    public Staff update(String id, Staff updated) {
        Staff existing = staffRepository.findById(id)
                .orElseThrow(() -> new StaffNotFoundException(id));
        existing.setName(updated.getName());
        existing.setJoinDate(updated.getJoinDate());
        existing.setWorkSchedule(updated.getWorkSchedule());
        existing.setTermDate(updated.getTermDate());
        return staffRepository.save(existing);
    }

    public void delete(String id) {
        staffRepository.findById(id)
                .orElseThrow(() -> new StaffNotFoundException(id));
        staffRepository.deleteById(id);
    }
}
