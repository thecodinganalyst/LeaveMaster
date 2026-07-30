package com.practical.leavemaster.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {

    boolean existsByLoginName(String loginName);

    Optional<AppUser> findByLoginName(String loginName);

    Optional<AppUser> findByStaffId(String staffId);
}
