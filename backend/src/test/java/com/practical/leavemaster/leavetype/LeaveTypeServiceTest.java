package com.practical.leavemaster.leavetype;

import com.practical.leavemaster.tenant.TenantActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveTypeServiceTest {

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private TenantActivityService tenantActivityService;

    @InjectMocks
    private LeaveTypeService leaveTypeService;

    @Test
    void shouldReturnAllLeaveTypes() {
        List<LeaveType> leaveTypes = List.of(
                LeaveType.builder().id("annual").name("Annual Leave").used(false).build(),
                LeaveType.builder().id("medical").name("Medical Leave").used(true).build()
        );
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);

        List<LeaveType> result = leaveTypeService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnLeaveTypeById() {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(false).build();
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType));

        Optional<LeaveType> result = leaveTypeService.findById("annual");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Annual Leave");
    }

    @Test
    void shouldSaveLeaveType() {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(true).build();
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveType result = leaveTypeService.save(leaveType);

        assertThat(result.getId()).isEqualTo("annual");
        assertThat(result.isUsed()).isFalse();
    }

    @Test
    void shouldUpdateLeaveType() {
        LeaveType existing = LeaveType.builder().id("annual").name("Annual Leave").used(false).build();
        LeaveType updated = LeaveType.builder().id("annual").name("Annual Leave Updated").used(true).build();
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.save(existing)).thenReturn(existing);

        LeaveType result = leaveTypeService.update("annual", updated);

        assertThat(result.getName()).isEqualTo("Annual Leave Updated");
        assertThat(result.isUsed()).isFalse();
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentLeaveType() {
        when(leaveTypeRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveTypeService.update("nonexistent", new LeaveType()))
                .isInstanceOf(LeaveTypeNotFoundException.class);
    }

    @Test
    void shouldDeleteLeaveTypeWhenNotInUse() {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(false).build();
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType));

        leaveTypeService.delete("annual");

        verify(leaveTypeRepository).deleteById("annual");
    }

    @Test
    void shouldThrowWhenDeletingLeaveTypeInUse() {
        LeaveType leaveType = LeaveType.builder().id("medical").name("Medical Leave").used(true).build();
        when(leaveTypeRepository.findById("medical")).thenReturn(Optional.of(leaveType));

        assertThatThrownBy(() -> leaveTypeService.delete("medical"))
                .isInstanceOf(LeaveTypeInUseException.class);

        verify(leaveTypeRepository, never()).deleteById("medical");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentLeaveType() {
        when(leaveTypeRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveTypeService.delete("nonexistent"))
                .isInstanceOf(LeaveTypeNotFoundException.class);
    }
}
