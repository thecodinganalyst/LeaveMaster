package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    public Optional<Tenant> findById(String id) {
        return tenantRepository.findById(id);
    }

    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    public Tenant update(String id, Tenant updated) {
        Tenant existing = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        existing.setName(updated.getName());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setStatus(updated.getStatus());
        return tenantRepository.save(existing);
    }

    public void delete(String id) {
        tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        tenantRepository.deleteById(id);
    }
}
