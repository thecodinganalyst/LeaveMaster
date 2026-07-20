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
        if (request.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        if (request.getEffectiveTo() != null && !request.getEffectiveTo().isAfter(request.getEffectiveFrom())) {
            throw new IllegalArgumentException("effectiveTo must be after effectiveFrom");
        }
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new StaffNotFoundException(request.getStaffId()));
        Staff approver = staffRepository.findById(request.getApproverId())
                .orElseThrow(() -> new StaffNotFoundException(request.getApproverId()));
        Staff admin = staffRepository.findById(request.getAdminId())
                .orElseThrow(() -> new StaffNotFoundException(request.getAdminId()));
        LeaveApprover leaveApprover = LeaveApprover.builder()
                .staff(staff)
                .approver(approver)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .admin(admin)
                .adminDate(LocalDate.now())
                .build();
        return leaveApproverRepository.save(leaveApprover);
    }

    public LeaveApprover update(String id, LeaveApproverRequest request) {
        if (request.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        if (request.getEffectiveTo() != null && !request.getEffectiveTo().isAfter(request.getEffectiveFrom())) {
            throw new IllegalArgumentException("effectiveTo must be after effectiveFrom");
        }
        LeaveApprover existing = leaveApproverRepository.findById(id)
                .orElseThrow(() -> new LeaveApproverNotFoundException(id));
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new StaffNotFoundException(request.getStaffId()));
        Staff approver = staffRepository.findById(request.getApproverId())
                .orElseThrow(() -> new StaffNotFoundException(request.getApproverId()));
        Staff admin = staffRepository.findById(request.getAdminId())
                .orElseThrow(() -> new StaffNotFoundException(request.getAdminId()));
        existing.setStaff(staff);
        existing.setApprover(approver);
        existing.setEffectiveFrom(request.getEffectiveFrom());
        existing.setEffectiveTo(request.getEffectiveTo());
        existing.setAdmin(admin);
        existing.setAdminDate(LocalDate.now());
        return leaveApproverRepository.save(existing);
    }

    public void delete(String id) {
        leaveApproverRepository.findById(id)
                .orElseThrow(() -> new LeaveApproverNotFoundException(id));
        leaveApproverRepository.deleteById(id);
    }
}
