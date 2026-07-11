package com.practicallimits.spring_template.staff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private StaffService staffService;

    @Test
    void shouldReturnAllStaff() {
        List<Staff> staffList = List.of(
                Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build(),
                Staff.builder().id("S002").name("Bob Jones").joinDate(LocalDate.of(2023, 6, 1)).workSchedule("WEEKDAYS").build()
        );
        when(staffRepository.findAll()).thenReturn(staffList);

        List<Staff> result = staffService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnStaffById() {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));

        Optional<Staff> result = staffService.findById("S001");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice Smith");
    }

    @Test
    void shouldSaveStaff() {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
        when(staffRepository.save(staff)).thenReturn(staff);

        Staff result = staffService.save(staff);

        assertThat(result.getId()).isEqualTo("S001");
    }

    @Test
    void shouldUpdateStaff() {
        Staff existing = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
        Staff updated = Staff.builder().id("S001").name("Alice Johnson").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS_AND_SATURDAY").build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(existing));
        when(staffRepository.save(existing)).thenReturn(existing);

        Staff result = staffService.update("S001", updated);

        assertThat(result.getName()).isEqualTo("Alice Johnson");
        assertThat(result.getWorkSchedule()).isEqualTo("WEEKDAYS_AND_SATURDAY");
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentStaff() {
        when(staffRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.update("nonexistent", new Staff()))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    void shouldDeleteStaff() {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));

        staffService.delete("S001");

        verify(staffRepository).deleteById("S001");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentStaff() {
        when(staffRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.delete("nonexistent"))
                .isInstanceOf(StaffNotFoundException.class);

        verify(staffRepository, never()).deleteById("nonexistent");
    }
}
