package com.practical.leavemaster.jurisdiction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JurisdictionHierarchyReconciliationInitializerTest {

    @Test
    void shouldRestoreMissingCountryParentForExistingSubdivision() throws Exception {
        JurisdictionRepository repository = mock(JurisdictionRepository.class);
        Jurisdiction nsw = subdivision("AU-NSW", "AU", null);
        when(repository.findAll()).thenReturn(List.of(nsw));
        when(repository.existsById("AU")).thenReturn(true);

        new JurisdictionHierarchyReconciliationInitializer(repository).run(null);

        assertThat(nsw.getParentId()).isEqualTo("AU");
        verify(repository).save(nsw);
    }

    @Test
    void shouldPreserveExplicitParentForNestedHierarchy() throws Exception {
        JurisdictionRepository repository = mock(JurisdictionRepository.class);
        Jurisdiction locality = subdivision("AU-NSW-SYD", "AU", "AU-NSW");
        when(repository.findAll()).thenReturn(List.of(locality));

        new JurisdictionHierarchyReconciliationInitializer(repository).run(null);

        assertThat(locality.getParentId()).isEqualTo("AU-NSW");
        verify(repository, never()).save(locality);
    }

    @Test
    void shouldIgnoreCountryJurisdictionsAndUnknownParents() throws Exception {
        JurisdictionRepository repository = mock(JurisdictionRepository.class);
        Jurisdiction australia = Jurisdiction.builder()
                .id("AU").code("AU").name("Australia")
                .jurisdictionType(JurisdictionType.COUNTRY)
                .countryCode("AU").active(true).build();
        Jurisdiction orphan = subdivision("ZZ-TEST", "ZZ", null);
        when(repository.findAll()).thenReturn(List.of(australia, orphan));
        when(repository.existsById("ZZ")).thenReturn(false);

        new JurisdictionHierarchyReconciliationInitializer(repository).run(null);

        assertThat(australia.getParentId()).isNull();
        assertThat(orphan.getParentId()).isNull();
        verify(repository, never()).save(australia);
        verify(repository, never()).save(orphan);
    }

    private Jurisdiction subdivision(String id, String countryCode, String parentId) {
        return Jurisdiction.builder()
                .id(id).code(id).name(id)
                .jurisdictionType(JurisdictionType.STATE)
                .countryCode(countryCode)
                .subdivisionCode(id)
                .parentId(parentId)
                .active(true)
                .build();
    }
}
