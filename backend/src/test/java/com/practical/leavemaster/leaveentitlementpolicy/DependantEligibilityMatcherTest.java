package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.leaveeligibility.StaffDependant;
import com.practical.leavemaster.leaveeligibility.StaffDependantRepository;
import com.practical.leavemaster.staff.Staff;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DependantEligibilityMatcherTest {

    private final StaffDependantRepository repository = mock(StaffDependantRepository.class);
    private final DependantEligibilityMatcher matcher = new DependantEligibilityMatcher(repository);

    @Test
    void requiresAllFactsToMatchTheSameDependant() {
        Staff staff = staff();
        when(repository.findAllByTenantIdAndStaffId("tenant-1", "staff-1")).thenReturn(List.of(
                child("child-1", LocalDate.of(2022, 1, 1), "AU"),
                child("child-2", LocalDate.of(2010, 1, 1), "SG")
        ));

        assertThat(matcher.matches(staff, LocalDate.of(2026, 8, 22),
                "relationship=CHILD;citizenship=SG;age_lt=7")).isFalse();
    }

    @Test
    void youngestPredicateEvaluatesTheYoungestMatchingChild() {
        Staff staff = staff();
        when(repository.findAllByTenantIdAndStaffId("tenant-1", "staff-1")).thenReturn(List.of(
                child("child-1", LocalDate.of(2017, 1, 1), "SG"),
                child("child-2", LocalDate.of(2019, 1, 1), "SG")
        ));

        assertThat(matcher.matches(staff, LocalDate.of(2026, 8, 22),
                "relationship=CHILD;citizenship=SG;youngest=true;age_gte=7;age_lte=12")).isTrue();
    }

    @Test
    void supportsNonSingaporeConfigurationWithoutNewCriterionTypes() {
        Staff staff = staff();
        when(repository.findAllByTenantIdAndStaffId("tenant-1", "staff-1")).thenReturn(List.of(
                child("child-au", LocalDate.of(2023, 6, 1), "AU")
        ));

        assertThat(matcher.matches(staff, LocalDate.of(2026, 8, 22),
                "relationship=CHILD;citizenship=AU;age_lt=7")).isTrue();
    }

    private Staff staff() {
        return Staff.builder().id("staff-1").tenantId("tenant-1").build();
    }

    private StaffDependant child(String id, LocalDate dateOfBirth, String citizenship) {
        return StaffDependant.builder()
                .id(id)
                .tenantId("tenant-1")
                .staffId("staff-1")
                .relationshipCode("CHILD")
                .dateOfBirth(dateOfBirth)
                .citizenshipCode(citizenship)
                .active(true)
                .build();
    }
}
