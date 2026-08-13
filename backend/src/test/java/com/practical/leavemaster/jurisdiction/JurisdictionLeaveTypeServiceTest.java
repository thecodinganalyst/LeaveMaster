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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JurisdictionLeaveTypeServiceTest {
    @Mock JurisdictionLeaveTypeRepository leaveTypeRepository;
    @Mock JurisdictionRepository jurisdictionRepository;
    @InjectMocks JurisdictionLeaveTypeService service;

    @Test
    void shouldCreateLeaveTypeWithNormalizedValues() {
        JurisdictionLeaveType leaveType = JurisdictionLeaveType.builder()
                .jurisdictionId("sg").code("annual_leave").name("Annual Leave")
                .statutory(true).active(true).build();
        when(jurisdictionRepository.existsById("SG")).thenReturn(true);
        when(leaveTypeRepository.findByJurisdictionIdAndCode("SG", "ANNUAL_LEAVE")).thenReturn(Optional.empty());
        when(leaveTypeRepository.save(any(JurisdictionLeaveType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JurisdictionLeaveType saved = service.create(leaveType);

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getJurisdictionId()).isEqualTo("SG");
        assertThat(saved.getCode()).isEqualTo("ANNUAL_LEAVE");
    }

    @Test
    void shouldRejectUnknownJurisdictionAndDuplicateCode() {
        JurisdictionLeaveType leaveType = seed("SG", "ANNUAL_LEAVE");
        when(jurisdictionRepository.existsById("SG")).thenReturn(false);
        assertThatThrownBy(() -> service.create(leaveType)).isInstanceOf(IllegalArgumentException.class);

        when(jurisdictionRepository.existsById("SG")).thenReturn(true);
        when(leaveTypeRepository.findByJurisdictionIdAndCode("SG", "ANNUAL_LEAVE")).thenReturn(Optional.of(leaveType));
        assertThatThrownBy(() -> service.create(leaveType)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUpdateAndDeleteLeaveType() {
        JurisdictionLeaveType existing = seed("SG", "ANNUAL_LEAVE");
        JurisdictionLeaveType incoming = seed("SG", "ANNUAL_LEAVE");
        incoming.setName("Annual Vacation Leave");
        when(leaveTypeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(jurisdictionRepository.existsById("SG")).thenReturn(true);
        when(leaveTypeRepository.findByJurisdictionIdAndCode("SG", "ANNUAL_LEAVE")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.save(existing)).thenReturn(existing);

        assertThat(service.update(existing.getId(), incoming).getName()).isEqualTo("Annual Vacation Leave");

        when(leaveTypeRepository.existsById(existing.getId())).thenReturn(true);
        service.delete(existing.getId());
        verify(leaveTypeRepository).deleteById(existing.getId());
    }

    @Test
    void shouldResolveChildAndParentWithChildOverride() {
        Jurisdiction parent = jurisdiction("AU", null);
        Jurisdiction child = jurisdiction("AU-NSW", "AU");
        JurisdictionLeaveType parentAnnual = seed("AU", "ANNUAL_LEAVE");
        parentAnnual.setName("Parent Annual");
        JurisdictionLeaveType childAnnual = seed("AU-NSW", "ANNUAL_LEAVE");
        childAnnual.setName("NSW Annual");
        JurisdictionLeaveType longService = seed("AU-NSW", "LONG_SERVICE_LEAVE");
        when(jurisdictionRepository.findById("AU-NSW")).thenReturn(Optional.of(child));
        when(jurisdictionRepository.findById("AU")).thenReturn(Optional.of(parent));
        when(leaveTypeRepository.findByJurisdictionIdAndActiveTrue("AU-NSW")).thenReturn(List.of(childAnnual, longService));
        when(leaveTypeRepository.findByJurisdictionIdAndActiveTrue("AU")).thenReturn(List.of(parentAnnual));

        List<JurisdictionLeaveType> effective = service.resolveEffective("AU-NSW");

        assertThat(effective).hasSize(2);
        assertThat(effective.stream().filter(item -> item.getCode().equals("ANNUAL_LEAVE")).findFirst().orElseThrow().getName())
                .isEqualTo("NSW Annual");
    }

    @Test
    void shouldRejectHierarchyCycle() {
        when(jurisdictionRepository.findById("A")).thenReturn(Optional.of(jurisdiction("A", "B")));
        when(jurisdictionRepository.findById("B")).thenReturn(Optional.of(jurisdiction("B", "A")));
        when(leaveTypeRepository.findByJurisdictionIdAndActiveTrue(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.resolveEffective("A"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cycle");
    }

    private Jurisdiction jurisdiction(String id, String parentId) {
        return Jurisdiction.builder().id(id).code(id).name(id)
                .jurisdictionType(parentId == null ? JurisdictionType.COUNTRY : JurisdictionType.STATE)
                .parentId(parentId).countryCode(id.length() >= 2 ? id.substring(0, 2) : id).active(true).build();
    }

    private JurisdictionLeaveType seed(String jurisdictionId, String code) {
        return JurisdictionLeaveType.builder().id(jurisdictionId + ":" + code)
                .jurisdictionId(jurisdictionId).code(code).name(code).statutory(true).active(true).build();
    }
}
