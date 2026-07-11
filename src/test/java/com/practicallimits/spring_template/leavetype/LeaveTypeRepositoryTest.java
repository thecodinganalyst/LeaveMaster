package com.practicallimits.spring_template.leavetype;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LeaveTypeRepositoryTest {

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Test
    void shouldSaveAndFindLeaveType() {
        LeaveType leaveType = LeaveType.builder()
                .id("annual")
                .name("Annual Leave")
                .used(false)
                .build();

        leaveTypeRepository.save(leaveType);

        Optional<LeaveType> found = leaveTypeRepository.findById("annual");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Annual Leave");
        assertThat(found.get().isUsed()).isFalse();
    }

    @Test
    void shouldFindAllLeaveTypes() {
        leaveTypeRepository.save(LeaveType.builder().id("annual").name("Annual Leave").used(false).build());
        leaveTypeRepository.save(LeaveType.builder().id("medical").name("Medical Leave").used(true).build());

        List<LeaveType> all = leaveTypeRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void shouldDeleteLeaveType() {
        leaveTypeRepository.save(LeaveType.builder().id("annual").name("Annual Leave").used(false).build());
        leaveTypeRepository.deleteById("annual");
        assertThat(leaveTypeRepository.findById("annual")).isEmpty();
    }
}
