package com.practical.leavemaster.jurisdiction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class MalaysiaJurisdictionCatalogInitializerTest {
    private JurisdictionCatalogInitializer initializer;
    private Map<String, Jurisdiction> jurisdictions;
    private Map<String, JurisdictionLeaveType> leaveTypes;

    @BeforeEach
    void setUp() {
        JurisdictionRepository jurisdictionRepository = mock(JurisdictionRepository.class);
        JurisdictionLeaveTypeRepository leaveTypeRepository = mock(JurisdictionLeaveTypeRepository.class);
        jurisdictions = new LinkedHashMap<>();
        leaveTypes = new LinkedHashMap<>();

        when(jurisdictionRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(jurisdictions.get(invocation.getArgument(0))));
        when(jurisdictionRepository.save(any(Jurisdiction.class)))
                .thenAnswer(invocation -> {
                    Jurisdiction value = invocation.getArgument(0);
                    jurisdictions.put(value.getId(), value);
                    return value;
                });
        when(leaveTypeRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(leaveTypes.get(invocation.getArgument(0))));
        when(leaveTypeRepository.save(any(JurisdictionLeaveType.class)))
                .thenAnswer(invocation -> {
                    JurisdictionLeaveType value = invocation.getArgument(0);
                    leaveTypes.put(value.getId(), value);
                    return value;
                });
        doAnswer(invocation -> {
            JurisdictionLeaveType value = invocation.getArgument(0);
            leaveTypes.remove(value.getId());
            return null;
        }).when(leaveTypeRepository).delete(any(JurisdictionLeaveType.class));

        initializer = new JurisdictionCatalogInitializer(jurisdictionRepository, leaveTypeRepository);
    }

    @Test
    void shouldSeedAllMalaysiaStatesAndFederalTerritoriesUnderMalaysia() {
        initializer.run(null);

        Set<String> children = jurisdictions.values().stream()
                .filter(jurisdiction -> "MY".equals(jurisdiction.getParentId()))
                .map(Jurisdiction::getId)
                .collect(Collectors.toSet());

        assertThat(children).containsExactlyInAnyOrder(
                "MY-JHR", "MY-KDH", "MY-KTN", "MY-MLK", "MY-NSN", "MY-PHG", "MY-PNG", "MY-PRK",
                "MY-PLS", "MY-SBH", "MY-SWK", "MY-SGR", "MY-TRG", "MY-KUL", "MY-LBN", "MY-PJY");
        assertThat(jurisdictions.get("MY-KUL").getJurisdictionType()).isEqualTo(JurisdictionType.TERRITORY);
        assertThat(jurisdictions.get("MY-SGR").getJurisdictionType()).isEqualTo(JurisdictionType.STATE);
    }

    @Test
    void shouldSeedMalaysiaStatutoryLeaveTypesWithSabahAndSarawakSourceOverrides() {
        initializer.seedLeaveTypes();

        assertThat(byJurisdiction("MY")).containsOnlyKeys(
                "ANNUAL_LEAVE", "SICK_LEAVE", "HOSPITALISATION_LEAVE", "MATERNITY_LEAVE", "PATERNITY_LEAVE");
        assertThat(byJurisdiction("MY").values()).allSatisfy(leaveType -> {
            assertThat(leaveType.isStatutory()).isTrue();
            assertThat(leaveType.getPaid()).isTrue();
            assertThat(leaveType.getSourceUrl()).startsWith("https://");
        });
        assertThat(byJurisdiction("MY-SBH").get("ANNUAL_LEAVE").getSourceName()).contains("Sabah");
        assertThat(byJurisdiction("MY-SWK").get("ANNUAL_LEAVE").getSourceName()).contains("Sarawak");
    }

    @Test
    void shouldRemainIdempotent() {
        initializer.run(null);
        Set<String> jurisdictionIds = Set.copyOf(jurisdictions.keySet());
        Set<String> leaveTypeIds = Set.copyOf(leaveTypes.keySet());

        initializer.run(null);

        assertThat(jurisdictions.keySet()).containsExactlyInAnyOrderElementsOf(jurisdictionIds);
        assertThat(leaveTypes.keySet()).containsExactlyInAnyOrderElementsOf(leaveTypeIds);
    }

    private Map<String, JurisdictionLeaveType> byJurisdiction(String jurisdictionId) {
        return leaveTypes.values().stream()
                .filter(value -> jurisdictionId.equals(value.getJurisdictionId()))
                .collect(Collectors.toMap(JurisdictionLeaveType::getCode, value -> value, (left, right) -> right, LinkedHashMap::new));
    }
}
