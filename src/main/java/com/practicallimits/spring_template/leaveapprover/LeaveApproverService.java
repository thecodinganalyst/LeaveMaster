package com.practicallimits.spring_template.leaveapprover;

import com.practicallimits.spring_template.staff.Staff;
import com.practicallimits.spring_template.staff.StaffNotFoundException;
import com.practicallimits.spring_template.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveApproverService {

    private final LeaveApproverRepository leaveApproverRepository;
    private final StaffRepository staffRepository;

    public List<LeaveApprover> findAll() {
        return leaveApproverRepository.findAll();
    }

    public List<LeaveApprover> findByStaffId(String staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        return leaveApproverRepository.findByStaff(staff);
    }

    public Optional<LeaveApprover> findById(String id) {
        return leaveApproverRepository.findById(id);
    }

    public LeaveApprover save(LeaveApprover leaveApprover) {
        return leaveApproverRepository.save(leaveApprover);
    }

    public LeaveApprover update(String id, LeaveApprover updated) {
        LeaveApprover existing = leaveApproverRepository.findById(id)
                .orElseThrow(() -> new LeaveApproverNotFoundException(id));
        existing.setStaff(updated.getStaff());
        existing.setApprover(updated.getApprover());
        existing.setEffectiveFrom(updated.getEffectiveFrom());
        existing.setEffectiveTo(updated.getEffectiveTo());
        existing.setAdmin(updated.getAdmin());
        existing.setAdminDate(updated.getAdminDate());
        return leaveApproverRepository.save(existing);
    }

    public void delete(String id) {
        leaveApproverRepository.findById(id)
                .orElseThrow(() -> new LeaveApproverNotFoundException(id));
        leaveApproverRepository.deleteById(id);
    }
}
