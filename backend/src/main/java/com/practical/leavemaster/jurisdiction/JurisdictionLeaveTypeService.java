package com.practical.leavemaster.jurisdiction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JurisdictionLeaveTypeService {
    private final JurisdictionLeaveTypeRepository leaveTypeRepository;
    private final JurisdictionRepository jurisdictionRepository;

    @Transactional(readOnly = true)
    public List<JurisdictionLeaveType> findAll() {
        return leaveTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<JurisdictionLeaveType> findById(String id) {
        return leaveTypeRepository.findById(id);
    }

    @Transactional
    public JurisdictionLeaveType create(JurisdictionLeaveType leaveType) {
        normalize(leaveType);
        ensureJurisdiction(leaveType.getJurisdictionId());
        if (leaveTypeRepository.findByJurisdictionIdAndCode(leaveType.getJurisdictionId(), leaveType.getCode()).isPresent()) {
            throw new IllegalArgumentException("Leave type code already exists for the jurisdiction");
        }
        if (leaveType.getId() == null || leaveType.getId().isBlank()) leaveType.setId(UUID.randomUUID().toString());
        return leaveTypeRepository.save(leaveType);
    }

    @Transactional
    public JurisdictionLeaveType update(String id, JurisdictionLeaveType incoming) {
        JurisdictionLeaveType existing = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Jurisdiction leave type not found"));
        String previousJurisdictionId = existing.getJurisdictionId();
        String previousCode = existing.getCode();
        existing.setJurisdictionId(incoming.getJurisdictionId());
        existing.setCode(incoming.getCode());
        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setStatutory(incoming.isStatutory());
        existing.setPaid(incoming.getPaid());
        existing.setActive(incoming.isActive());
        existing.setSourceUrl(incoming.getSourceUrl());
        existing.setSourceName(incoming.getSourceName());
        existing.setEffectiveFrom(incoming.getEffectiveFrom());
        existing.setEffectiveTo(incoming.getEffectiveTo());
        normalize(existing);
        ensureJurisdiction(existing.getJurisdictionId());
        leaveTypeRepository.findByJurisdictionIdAndCode(existing.getJurisdictionId(), existing.getCode())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new IllegalArgumentException("Leave type code already exists for the jurisdiction"); });
        if (existing.getJurisdictionId() == null) existing.setJurisdictionId(previousJurisdictionId);
        if (existing.getCode() == null) existing.setCode(previousCode);
        return leaveTypeRepository.save(existing);
    }

    @Transactional
    public void delete(String id) {
        if (!leaveTypeRepository.existsById(id)) throw new IllegalArgumentException("Jurisdiction leave type not found");
        leaveTypeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<JurisdictionLeaveType> resolveEffective(String jurisdictionId) {
        Map<String, JurisdictionLeaveType> byCode = new LinkedHashMap<>();
        String currentId = jurisdictionId;
        List<String> visited = new ArrayList<>();
        while (currentId != null && !currentId.isBlank()) {
            if (visited.contains(currentId)) throw new IllegalStateException("Jurisdiction hierarchy contains a cycle");
            visited.add(currentId);
            Jurisdiction jurisdiction = jurisdictionRepository.findById(currentId)
                    .orElseThrow(() -> new IllegalArgumentException("Jurisdiction not found"));
            for (JurisdictionLeaveType leaveType : leaveTypeRepository.findByJurisdictionIdAndActiveTrue(currentId)) {
                byCode.putIfAbsent(leaveType.getCode(), leaveType);
            }
            currentId = jurisdiction.getParentId();
        }
        return List.copyOf(byCode.values());
    }

    private void ensureJurisdiction(String jurisdictionId) {
        if (jurisdictionId == null || jurisdictionId.isBlank() || !jurisdictionRepository.existsById(jurisdictionId)) {
            throw new IllegalArgumentException("Jurisdiction not found");
        }
    }

    private void normalize(JurisdictionLeaveType leaveType) {
        if (leaveType.getJurisdictionId() != null) leaveType.setJurisdictionId(leaveType.getJurisdictionId().trim().toUpperCase(Locale.ROOT));
        if (leaveType.getCode() != null) leaveType.setCode(leaveType.getCode().trim().toUpperCase(Locale.ROOT));
    }
}
