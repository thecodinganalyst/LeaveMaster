package com.practical.leavemaster.leavetype;

import com.practical.leavemaster.tenant.TenantActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final TenantActivityService tenantActivityService;

    public List<LeaveType> findAll() {
        return leaveTypeRepository.findAll();
    }

    public Optional<LeaveType> findById(String id) {
        return leaveTypeRepository.findById(id);
    }

    public LeaveType save(LeaveType leaveType) {
        leaveType.setUsed(false);
        LeaveType saved = leaveTypeRepository.save(leaveType);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public LeaveType update(String id, LeaveType updated) {
        LeaveType existing = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new LeaveTypeNotFoundException(id));
        existing.setName(updated.getName());
        LeaveType saved = leaveTypeRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void delete(String id) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new LeaveTypeNotFoundException(id));
        if (leaveType.isUsed()) {
            throw new LeaveTypeInUseException(id);
        }
        leaveTypeRepository.deleteById(id);
        tenantActivityService.touch(leaveType.getTenantId());
    }
}
