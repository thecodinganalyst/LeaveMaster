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

class AustraliaJurisdictionCatalogInitializerTest {
    private JurisdictionCatalogInitializer initializer;
    private Map<String, JurisdictionLeaveType> stored;

    @BeforeEach
    void setUp() {
        JurisdictionRepository jurisdictionRepository = mock(JurisdictionRepository.class);
        JurisdictionLeaveTypeRepository leaveTypeRepository = mock(JurisdictionLeaveTypeRepository.class);
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
    void shouldSeedFederalAustralianLeaveTypesAndStateLongServiceSources() {
        initializer.seedLeaveTypes();

        Map<String, JurisdictionLeaveType> federal = byJurisdiction("AU");
        assertThat(federal.keySet()).containsExactlyInAnyOrder(
                "ANNUAL_LEAVE", "PERSONAL_CARERS_LEAVE", "COMPASSIONATE_LEAVE", "PARENTAL_LEAVE",
                "COMMUNITY_SERVICE_LEAVE", "FAMILY_DOMESTIC_VIOLENCE_LEAVE", "LONG_SERVICE_LEAVE");
        assertThat(federal.get("PERSONAL_CARERS_LEAVE").getPaid()).isNull();
        assertThat(federal.get("COMPASSIONATE_LEAVE").getPaid()).isNull();
        assertThat(federal.get("PARENTAL_LEAVE").getPaid()).isFalse();
        assertThat(federal.get("FAMILY_DOMESTIC_VIOLENCE_LEAVE").getPaid()).isTrue();

        for (String jurisdiction : Set.of("AU-ACT", "AU-NSW", "AU-NT", "AU-QLD", "AU-SA", "AU-TAS", "AU-VIC", "AU-WA")) {
            Map<String, JurisdictionLeaveType> state = byJurisdiction(jurisdiction);
            assertThat(state).containsOnlyKeys("LONG_SERVICE_LEAVE");
            assertThat(state.get("LONG_SERVICE_LEAVE").getSourceName()).isNotBlank();
            assertThat(state.get("LONG_SERVICE_LEAVE").getSourceUrl()).startsWith("https://");
        }
    }

    @Test
    void shouldRemainIdempotentAndLeaveSingaporeCatalogueUnchanged() {
        initializer.seedLeaveTypes();
        Set<String> firstIds = Set.copyOf(stored.keySet());
        initializer.seedLeaveTypes();

        assertThat(stored.keySet()).containsExactlyInAnyOrderElementsOf(firstIds);
        assertThat(byJurisdiction("SG")).hasSize(15);
    }

    private Map<String, JurisdictionLeaveType> byJurisdiction(String jurisdictionId) {
        return stored.values().stream()
                .filter(value -> jurisdictionId.equals(value.getJurisdictionId()))
                .collect(Collectors.toMap(JurisdictionLeaveType::getCode, value -> value, (left, right) -> right, LinkedHashMap::new));
    }
}
