package com.practicallimits.spring_template.leaveapprover;

import com.practicallimits.spring_template.staff.Staff;
import com.practicallimits.spring_template.staff.StaffNotFoundException;
import com.practicallimits.spring_template.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    public LeaveApprover create(LeaveApproverRequest request) {
        validateDates(request);
        Staff[] staffEntities = resolveStaff(request);
        LeaveApprover leaveApprover = LeaveApprover.builder()
                .staff(staffEntities[0])
                .approver(staffEntities[1])
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .admin(staffEntities[2])
                .adminDate(LocalDate.now())
                .build();
        return leaveApproverRepository.save(leaveApprover);
    }

    public LeaveApprover update(String id, LeaveApproverRequest request) {
        validateDates(request);
        LeaveApprover existing = leaveApproverRepository.findById(id)
                .orElseThrow(() -> new LeaveApproverNotFoundException(id));
        Staff[] staffEntities = resolveStaff(request);
        existing.setStaff(staffEntities[0]);
        existing.setApprover(staffEntities[1]);
        existing.setEffectiveFrom(request.getEffectiveFrom());
        existing.setEffectiveTo(request.getEffectiveTo());
        existing.setAdmin(staffEntities[2]);
        existing.setAdminDate(LocalDate.now());
        return leaveApproverRepository.save(existing);
    }

    public void delete(String id) {
        leaveApproverRepository.findById(id)
                .orElseThrow(() -> new LeaveApproverNotFoundException(id));
        leaveApproverRepository.deleteById(id);
    }

    private void validateDates(LeaveApproverRequest request) {
        if (request.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        if (request.getEffectiveTo() != null && !request.getEffectiveTo().isAfter(request.getEffectiveFrom())) {
            throw new IllegalArgumentException("effectiveTo must be after effectiveFrom");
        }
    }

    private Staff[] resolveStaff(LeaveApproverRequest request) {
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new StaffNotFoundException(request.getStaffId()));
        Staff approver = staffRepository.findById(request.getApproverId())
                .orElseThrow(() -> new StaffNotFoundException(request.getApproverId()));
        Staff admin = staffRepository.findById(request.getAdminId())
                .orElseThrow(() -> new StaffNotFoundException(request.getAdminId()));
        return new Staff[]{staff, approver, admin};
    }
}
