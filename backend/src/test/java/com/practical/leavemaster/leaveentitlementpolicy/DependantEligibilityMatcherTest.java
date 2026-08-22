package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.leaveeligibility.StaffDependant;
import com.practical.leavemaster.leaveeligibility.StaffDependantRepository;
import com.practical.leavemaster.staff.Staff;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                child("child-2", LocalDate.of(2019, 1, 1), "SG"),
                dependant("spouse", "SPOUSE", null, "SG", null)
        ));

        assertThat(matcher.matches(staff, LocalDate.of(2026, 8, 22),
                "relationship=CHILD;citizenship=SG;youngest=true;age_gte=7;age_lte=12")).isTrue();
        assertThat(matcher.matches(staff, LocalDate.of(2026, 8, 22),
                "relationship=SPOUSE;youngest=true")).isFalse();
    }

    @Test
    void supportsResidencyAgeBoundsAndCaseInsensitiveCodes() {
        Staff staff = staff();
        StaffDependant child = dependant("child-1", " child ", LocalDate.of(2018, 8, 22), " sg ", "PR");
        when(repository.findAllByTenantIdAndStaffId("tenant-1", "staff-1")).thenReturn(List.of(child));

        LocalDate date = LocalDate.of(2026, 8, 22);
        assertThat(matcher.matches(staff, date,
                "relationship=CHILD;citizenship=SG;residency=pr;age_gt=7;age_gte=8;age_lt=9;age_lte=8")).isTrue();
        assertThat(matcher.matches(staff, date, "residency=CITIZEN")).isFalse();
        assertThat(matcher.matches(staff, date, "age_gt=8")).isFalse();
        assertThat(matcher.matches(staff, date, "age_gte=9")).isFalse();
        assertThat(matcher.matches(staff, date, "age_lt=8")).isFalse();
        assertThat(matcher.matches(staff, date, "age_lte=7")).isFalse();
    }

    @Test
    void ignoresInactiveOutOfRangeAndFutureBornDependants() {
        Staff staff = staff();
        LocalDate date = LocalDate.of(2026, 8, 22);
        StaffDependant inactive = child("inactive", LocalDate.of(2022, 1, 1), "SG");
        inactive.setActive(false);
        StaffDependant futureEffective = child("future-effective", LocalDate.of(2022, 1, 1), "SG");
        futureEffective.setEffectiveFrom(date.plusDays(1));
        StaffDependant expired = child("expired", LocalDate.of(2022, 1, 1), "SG");
        expired.setEffectiveTo(date.minusDays(1));
        StaffDependant futureBorn = child("future-born", date.plusDays(1), "SG");
        when(repository.findAllByTenantIdAndStaffId("tenant-1", "staff-1"))
                .thenReturn(List.of(inactive, futureEffective, expired, futureBorn));

        assertThat(matcher.matches(staff, date, "relationship=CHILD;age_lt=7")).isFalse();
    }

    @Test
    void activeEffectiveBoundariesAreInclusive() {
        Staff staff = staff();
        LocalDate date = LocalDate.of(2026, 8, 22);
        StaffDependant child = child("child", LocalDate.of(2022, 1, 1), "SG");
        child.setEffectiveFrom(date);
        child.setEffectiveTo(date);
        when(repository.findAllByTenantIdAndStaffId("tenant-1", "staff-1")).thenReturn(List.of(child));
        assertThat(matcher.matches(staff, date, "relationship=CHILD")).isTrue();
    }

    @Test
    void validatesPredicateSyntaxAndValues() {
        assertThatThrownBy(() -> DependantEligibilityMatcher.parse(null)).hasMessageContaining("required");
        assertThatThrownBy(() -> DependantEligibilityMatcher.parse("   ")).hasMessageContaining("required");
        assertThatThrownBy(() -> DependantEligibilityMatcher.parse("relationship")).hasMessageContaining("Invalid");
        assertThatThrownBy(() -> DependantEligibilityMatcher.parse("relationship=")).hasMessageContaining("Invalid");
        assertThatThrownBy(() -> DependantEligibilityMatcher.parse("unknown=value")).hasMessageContaining("Unsupported");
        assertThatThrownBy(() -> DependantEligibilityMatcher.parse("age_lt=-1")).hasMessageContaining("non-negative");
        assertThatThrownBy(() -> DependantEligibilityMatcher.parse("age_gte=abc")).hasMessageContaining("non-negative");
        assertThatThrownBy(() -> DependantEligibilityMatcher.parse("youngest=maybe")).hasMessageContaining("true or false");
        assertThat(DependantEligibilityMatcher.parse(" ; relationship=child ; citizenship = SG ; youngest=false "))
                .containsEntry("relationship", "child")
                .containsEntry("citizenship", "SG")
                .containsEntry("youngest", "false");
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
        return dependant(id, "CHILD", dateOfBirth, citizenship, null);
    }

    private StaffDependant dependant(String id, String relationship, LocalDate dateOfBirth,
                                     String citizenship, String residency) {
        return StaffDependant.builder()
                .id(id)
                .tenantId("tenant-1")
                .staffId("staff-1")
                .relationshipCode(relationship)
                .dateOfBirth(dateOfBirth)
                .citizenshipCode(citizenship)
                .residencyCode(residency)
                .active(true)
                .build();
    }
}
