package com.practical.leavemaster.leavetype;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    public List<LeaveType> findAll() {
        return leaveTypeRepository.findAll();
    }

    public Optional<LeaveType> findById(String id) {
        return leaveTypeRepository.findById(id);
    }

    public LeaveType save(LeaveType leaveType) {
        return leaveTypeRepository.save(leaveType);
    }

    public LeaveType update(String id, LeaveType updated) {
        LeaveType existing = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new LeaveTypeNotFoundException(id));
        existing.setName(updated.getName());
        existing.setUsed(updated.isUsed());
        return leaveTypeRepository.save(existing);
    }

    public void delete(String id) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new LeaveTypeNotFoundException(id));
        if (leaveType.isUsed()) {
            throw new LeaveTypeInUseException(id);
        }
        leaveTypeRepository.deleteById(id);
    }
}
