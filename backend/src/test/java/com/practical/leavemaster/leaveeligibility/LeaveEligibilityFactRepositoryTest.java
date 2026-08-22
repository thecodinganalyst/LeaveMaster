package com.practical.leavemaster.leaveeligibility;

import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class LeaveEligibilityFactRepositoryTest {

    @Autowired StaffRepository staffRepository;
    @Autowired StaffDependantRepository dependantRepository;
    @Autowired QualifyingLeaveEventRepository eventRepository;

    @Test
    void persistsCrossJurisdictionGenericFacts() {
        Staff staff = staffRepository.save(Staff.builder()
                .id("FACT-STAFF")
                .name("Fact Staff")
                .joinDate(LocalDate.of(2024, 1, 1))
                .tenantId("FACT-TENANT")
                .jurisdictionId("AU")
                .build());

        StaffDependant dependant = dependantRepository.save(StaffDependant.builder()
                .id("FACT-DEPENDANT")
                .tenantId(staff.getTenantId())
                .staffId(staff.getId())
                .name("Dependent")
                .relationshipCode("CHILD")
                .citizenshipCode("AU")
                .active(true)
                .build());

        QualifyingLeaveEvent event = eventRepository.save(QualifyingLeaveEvent.builder()
                .id("FACT-EVENT")
                .tenantId(staff.getTenantId())
                .staffId(staff.getId())
                .dependantId(dependant.getId())
                .eventTypeCode("ADOPTION")
                .eventDate(LocalDate.of(2026, 4, 1))
                .status(QualifyingEventStatus.VERIFIED)
                .build());

        assertNotNull(dependantRepository.findById(dependant.getId()).orElseThrow());
        assertEquals("ADOPTION", eventRepository.findById(event.getId()).orElseThrow().getEventTypeCode());
        assertEquals(1, dependantRepository.findAllByTenantIdAndStaffId("FACT-TENANT", "FACT-STAFF").size());
        assertEquals(1, eventRepository.findAllByTenantIdAndStaffId("FACT-TENANT", "FACT-STAFF").size());
    }
}
