package com.practical.leavemaster.jurisdiction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JurisdictionService {
    private final JurisdictionRepository jurisdictionRepository;
    private final JurisdictionLeaveTypeRepository leaveTypeRepository;

    @Transactional(readOnly = true)
    public List<Jurisdiction> findAll() {
        return jurisdictionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Jurisdiction> findById(String id) {
        return jurisdictionRepository.findById(id);
    }

    @Transactional
    public Jurisdiction create(Jurisdiction jurisdiction) {
        normalize(jurisdiction);
        if (jurisdiction.getId() == null || jurisdiction.getId().isBlank()) {
            jurisdiction.setId(jurisdiction.getCode());
        }
        if (jurisdictionRepository.existsById(jurisdiction.getId()) || jurisdictionRepository.findByCode(jurisdiction.getCode()).isPresent()) {
            throw new IllegalArgumentException("Jurisdiction id/code already exists");
        }
        validateParent(jurisdiction);
        return jurisdictionRepository.save(jurisdiction);
    }

    @Transactional
    public Jurisdiction update(String id, Jurisdiction incoming) {
        Jurisdiction existing = jurisdictionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Jurisdiction not found"));
        existing.setName(incoming.getName());
        existing.setJurisdictionType(incoming.getJurisdictionType());
        existing.setParentId(incoming.getParentId());
        existing.setCountryCode(incoming.getCountryCode());
        existing.setSubdivisionCode(incoming.getSubdivisionCode());
        existing.setActive(incoming.isActive());
        normalize(existing);
        validateParent(existing);
        return jurisdictionRepository.save(existing);
    }

    @Transactional
    public void delete(String id) {
        if (!jurisdictionRepository.existsById(id)) {
            throw new IllegalArgumentException("Jurisdiction not found");
        }
        if (!jurisdictionRepository.findByParentId(id).isEmpty() || leaveTypeRepository.existsByJurisdictionId(id)) {
            throw new IllegalStateException("Jurisdiction is referenced and cannot be deleted; deactivate it instead");
        }
        jurisdictionRepository.deleteById(id);
    }

    private void validateParent(Jurisdiction jurisdiction) {
        if (jurisdiction.getParentId() == null || jurisdiction.getParentId().isBlank()) {
            if (jurisdiction.getJurisdictionType() != JurisdictionType.COUNTRY) {
                throw new IllegalArgumentException("Non-country jurisdictions require a parent jurisdiction");
            }
            return;
        }
        if (jurisdiction.getId().equals(jurisdiction.getParentId())) {
            throw new IllegalArgumentException("A jurisdiction cannot be its own parent");
        }
        Jurisdiction parent = jurisdictionRepository.findById(jurisdiction.getParentId())
                .orElseThrow(() -> new IllegalArgumentException("Parent jurisdiction not found"));
        if (!parent.getCountryCode().equalsIgnoreCase(jurisdiction.getCountryCode())) {
            throw new IllegalArgumentException("Parent jurisdiction must belong to the same country");
        }
    }

    private void normalize(Jurisdiction jurisdiction) {
        if (jurisdiction.getCode() != null) jurisdiction.setCode(jurisdiction.getCode().trim().toUpperCase(Locale.ROOT));
        if (jurisdiction.getCountryCode() != null) jurisdiction.setCountryCode(jurisdiction.getCountryCode().trim().toUpperCase(Locale.ROOT));
        if (jurisdiction.getSubdivisionCode() != null && !jurisdiction.getSubdivisionCode().isBlank()) {
            jurisdiction.setSubdivisionCode(jurisdiction.getSubdivisionCode().trim().toUpperCase(Locale.ROOT));
        }
        if (jurisdiction.getId() != null) jurisdiction.setId(jurisdiction.getId().trim().toUpperCase(Locale.ROOT));
        if (jurisdiction.getParentId() != null && !jurisdiction.getParentId().isBlank()) jurisdiction.setParentId(jurisdiction.getParentId().trim().toUpperCase(Locale.ROOT));
    }
}
