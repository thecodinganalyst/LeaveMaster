package com.practical.leavemaster.jurisdiction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JurisdictionRepository extends JpaRepository<Jurisdiction, String> {
    Optional<Jurisdiction> findByCode(String code);
    List<Jurisdiction> findByParentId(String parentId);
}
