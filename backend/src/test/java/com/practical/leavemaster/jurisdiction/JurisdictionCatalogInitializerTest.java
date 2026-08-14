package com.practical.leavemaster.jurisdiction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JurisdictionCatalogInitializerTest {

    private JurisdictionLeaveTypeRepository leaveTypeRepository;
    private JurisdictionCatalogInitializer initializer;
    private Map<String, JurisdictionLeaveType> stored;

    @BeforeEach
    void setUp() {
        JurisdictionRepository jurisdictionRepository = mock(JurisdictionRepository.class);
        leaveTypeRepository = mock(JurisdictionLeaveTypeRepository.class);
        stored = new LinkedHashMap<>();

        when(leaveTypeRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(leaveTypeRepository.save(any(JurisdictionLeaveType.class)))
                .thenAnswer(invocation -> {
                    JurisdictionLeaveType value = invocation.getArgument(0);
                    stored.put(value.getId(), value);
                    return value;
                });
        doAnswer(invocation -> {
            JurisdictionLeaveType value = invocation.getArgument(0);
            stored.remove(value.getId());
            return null;
        }).when(leaveTypeRepository).delete(any(JurisdictionLeaveType.class));

        initializer = new JurisdictionCatalogInitializer(jurisdictionRepository, leaveTypeRepository);
    }

    @Test
    void shouldSeedCompleteSingaporeCatalogue() {
        initializer.seedLeaveTypes();

        Map<String, JurisdictionLeaveType> singapore = singaporeLeaveTypes();
        assertThat(singapore).hasSize(14);
        assertThat(singapore.keySet()).containsExactlyInAnyOrder(
                "ANNUAL_LEAVE",
                "SICK_LEAVE",
                "HOSPITALISATION_LEAVE",
                "MATERNITY_LEAVE",
                "PATERNITY_LEAVE",
                "SHARED_PARENTAL_LEAVE",
                "CHILDCARE_LEAVE",
                "EXTENDED_CHILDCARE_LEAVE",
                "UNPAID_INFANT_CARE_LEAVE",
                "UNPAID_LEAVE",
                "COMPASSIONATE_LEAVE",
                "MARRIAGE_LEAVE",
                "NS_LEAVE",
                "OFF_IN_LIEU"
        );
        assertThat(singapore).doesNotContainKeys("OUTPATIENT_SICK_LEAVE", "INFANT_CARE_LEAVE");

        assertThat(singapore.get("SICK_LEAVE").getName()).isEqualTo("Sick Leave");
        assertThat(singapore.get("UNPAID_INFANT_CARE_LEAVE").getName()).isEqualTo("Unpaid Infant Care Leave");
        assertThat(singapore.get("UNPAID_INFANT_CARE_LEAVE").getPaid()).isFalse();
        assertThat(singapore.get("EXTENDED_CHILDCARE_LEAVE").isStatutory()).isTrue();
        assertThat(singapore.get("UNPAID_LEAVE").isStatutory()).isFalse();
        assertThat(singapore.get("COMPASSIONATE_LEAVE").isStatutory()).isFalse();
        assertThat(singapore.get("MARRIAGE_LEAVE").isStatutory()).isFalse();
        assertThat(singapore.get("OFF_IN_LIEU").isStatutory()).isFalse();
    }

    @Test
    void shouldMigrateLegacySingaporeCodesAndPreserveMetadata() {
        stored.put("SG:OUTPATIENT_SICK_LEAVE", JurisdictionLeaveType.builder()
                .id("SG:OUTPATIENT_SICK_LEAVE")
                .jurisdictionId("SG")
                .code("OUTPATIENT_SICK_LEAVE")
                .name("Outpatient Sick Leave")
                .description("Existing sick leave description")
                .statutory(true)
                .paid(true)
                .active(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .build());
        stored.put("SG:INFANT_CARE_LEAVE", JurisdictionLeaveType.builder()
                .id("SG:INFANT_CARE_LEAVE")
                .jurisdictionId("SG")
                .code("INFANT_CARE_LEAVE")
                .name("Infant Care Leave")
                .description("Existing infant care description")
                .statutory(true)
                .paid(false)
                .active(true)
                .build());

        initializer.seedLeaveTypes();

        assertThat(stored).doesNotContainKeys("SG:OUTPATIENT_SICK_LEAVE", "SG:INFANT_CARE_LEAVE");
        JurisdictionLeaveType sickLeave = stored.get("SG:SICK_LEAVE");
        assertThat(sickLeave.getCode()).isEqualTo("SICK_LEAVE");
        assertThat(sickLeave.getName()).isEqualTo("Sick Leave");
        assertThat(sickLeave.getDescription()).isEqualTo("Existing sick leave description");
        assertThat(sickLeave.getEffectiveFrom()).isEqualTo(LocalDate.of(2025, 1, 1));

        JurisdictionLeaveType infantCare = stored.get("SG:UNPAID_INFANT_CARE_LEAVE");
        assertThat(infantCare.getCode()).isEqualTo("UNPAID_INFANT_CARE_LEAVE");
        assertThat(infantCare.getName()).isEqualTo("Unpaid Infant Care Leave");
        assertThat(infantCare.getDescription()).isEqualTo("Existing infant care description");
        assertThat(infantCare.getPaid()).isFalse();
        assertThat(singaporeLeaveTypes()).hasSize(14);
    }

    @Test
    void shouldReconcileIdempotentlyWithoutDuplicates() {
        initializer.seedLeaveTypes();
        Set<String> idsAfterFirstRun = Set.copyOf(stored.keySet());

        initializer.seedLeaveTypes();

        assertThat(stored.keySet()).containsExactlyInAnyOrderElementsOf(idsAfterFirstRun);
        assertThat(singaporeLeaveTypes()).hasSize(14);
        assertThat(stored.keySet().stream().filter(id -> id.startsWith("SG:")).collect(Collectors.toSet())).hasSize(14);
    }

    private Map<String, JurisdictionLeaveType> singaporeLeaveTypes() {
        return stored.values().stream()
                .filter(leaveType -> "SG".equals(leaveType.getJurisdictionId()))
                .collect(Collectors.toMap(
                        JurisdictionLeaveType::getCode,
                        leaveType -> leaveType,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }
}
