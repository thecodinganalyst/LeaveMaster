package com.practical.leavemaster.rbac;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppRoleRepository extends JpaRepository<AppRole, String> {

    @Override
    @EntityGraph(attributePaths = {"permissions"})
    List<AppRole> findAll();

    @Override
    @EntityGraph(attributePaths = {"permissions"})
    Optional<AppRole> findById(String id);
}
