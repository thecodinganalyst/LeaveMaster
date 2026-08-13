package com.practical.leavemaster.jurisdiction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JurisdictionServiceTest {

    @Mock
    private JurisdictionRepository jurisdictionRepository;

    @Mock
    private JurisdictionLeaveTypeRepository leaveTypeRepository;

    @InjectMocks
    private JurisdictionService service;

    @Test
    void shouldReturnAllJurisdictions() {
        when(jurisdictionRepository.findAll()).thenReturn(List.of(country("SG")));
        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void shouldReturnJurisdictionById() {
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(country("SG")));
        assertThat(service.findById("SG")).isPresent();
    }

    @Test
    void shouldCreateCountryUsingNormalizedCodeAsId() {
        Jurisdiction jurisdiction = Jurisdiction.builder()
                .code("sg")
                .name("Singapore")
                .jurisdictionType(JurisdictionType.COUNTRY)
                .countryCode("sg")
                .active(true)
                .build();
        when(jurisdictionRepository.existsById("SG")).thenReturn(false);
        when(jurisdictionRepository.findByCode("SG")).thenReturn(Optional.empty());
        when(jurisdictionRepository.save(any(Jurisdiction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Jurisdiction saved = service.create(jurisdiction);

        assertThat(saved.getId()).isEqualTo("SG");
        assertThat(saved.getCode()).isEqualTo("SG");
        assertThat(saved.getCountryCode()).isEqualTo("SG");
    }

    @Test
    void shouldRejectDuplicateJurisdictionCode() {
        Jurisdiction jurisdiction = country("SG");
        when(jurisdictionRepository.existsById("SG")).thenReturn(true);

        assertThatThrownBy(() -> service.create(jurisdiction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldRequireParentForSubdivision() {
        Jurisdiction jurisdiction = Jurisdiction.builder()
                .id("AU-NSW")
                .code("AU-NSW")
                .name("New South Wales")
                .jurisdictionType(JurisdictionType.STATE)
                .countryCode("AU")
                .active(true)
                .build();
        when(jurisdictionRepository.existsById("AU-NSW")).thenReturn(false);
        when(jurisdictionRepository.findByCode("AU-NSW")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(jurisdiction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("require a parent");
    }

    @Test
    void shouldCreateSubdivisionWithSameCountryParent() {
        Jurisdiction parent = country("AU");
        Jurisdiction jurisdiction = Jurisdiction.builder()
                .id("AU-NSW")
                .code("AU-NSW")
                .name("New South Wales")
                .jurisdictionType(JurisdictionType.STATE)
                .parentId("AU")
                .countryCode("AU")
                .subdivisionCode("AU-NSW")
                .active(true)
                .build();
        when(jurisdictionRepository.existsById("AU-NSW")).thenReturn(false);
        when(jurisdictionRepository.findByCode("AU-NSW")).thenReturn(Optional.empty());
        when(jurisdictionRepository.findById("AU")).thenReturn(Optional.of(parent));
        when(jurisdictionRepository.save(any(Jurisdiction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(jurisdiction).getParentId()).isEqualTo("AU");
    }

    @Test
    void shouldRejectCrossCountryParent() {
        Jurisdiction jurisdiction = Jurisdiction.builder()
                .id("AU-NSW")
                .code("AU-NSW")
                .name("New South Wales")
                .jurisdictionType(JurisdictionType.STATE)
                .parentId("CA")
                .countryCode("AU")
                .active(true)
                .build();
        when(jurisdictionRepository.existsById("AU-NSW")).thenReturn(false);
        when(jurisdictionRepository.findByCode("AU-NSW")).thenReturn(Optional.empty());
        when(jurisdictionRepository.findById("CA")).thenReturn(Optional.of(country("CA")));

        assertThatThrownBy(() -> service.create(jurisdiction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same country");
    }

    @Test
    void shouldUpdateJurisdiction() {
        Jurisdiction existing = country("SG");
        Jurisdiction incoming = country("SG");
        incoming.setName("Republic of Singapore");
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(existing));
        when(jurisdictionRepository.save(existing)).thenReturn(existing);

        Jurisdiction updated = service.update("SG", incoming);

        assertThat(updated.getName()).isEqualTo("Republic of Singapore");
    }

    @Test
    void shouldRejectDeleteWhenChildrenExist() {
        when(jurisdictionRepository.existsById("AU")).thenReturn(true);
        when(jurisdictionRepository.findByParentId("AU")).thenReturn(List.of(new Jurisdiction()));

        assertThatThrownBy(() -> service.delete("AU"))
                .isInstanceOf(IllegalStateException.class);
        verify(jurisdictionRepository, never()).deleteById("AU");
    }

    @Test
    void shouldRejectDeleteWhenLeaveTypesExist() {
        when(jurisdictionRepository.existsById("SG")).thenReturn(true);
        when(jurisdictionRepository.findByParentId("SG")).thenReturn(List.of());
        when(leaveTypeRepository.existsByJurisdictionId("SG")).thenReturn(true);

        assertThatThrownBy(() -> service.delete("SG"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldDeleteUnreferencedJurisdiction() {
        when(jurisdictionRepository.existsById("XX")).thenReturn(true);
        when(jurisdictionRepository.findByParentId("XX")).thenReturn(List.of());
        when(leaveTypeRepository.existsByJurisdictionId("XX")).thenReturn(false);

        service.delete("XX");

        verify(jurisdictionRepository).deleteById("XX");
    }

    private Jurisdiction country(String code) {
        return Jurisdiction.builder()
                .id(code)
                .code(code)
                .name(code)
                .jurisdictionType(JurisdictionType.COUNTRY)
                .countryCode(code)
                .active(true)
                .build();
    }
}
