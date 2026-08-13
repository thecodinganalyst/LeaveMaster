package com.practical.leavemaster.jurisdiction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JurisdictionLeaveTypeRepository extends JpaRepository<JurisdictionLeaveType, String> {
    List<JurisdictionLeaveType> findByJurisdictionId(String jurisdictionId);
    List<JurisdictionLeaveType> findByJurisdictionIdAndActiveTrue(String jurisdictionId);
    Optional<JurisdictionLeaveType> findByJurisdictionIdAndCode(String jurisdictionId, String code);
    boolean existsByJurisdictionId(String jurisdictionId);
}
