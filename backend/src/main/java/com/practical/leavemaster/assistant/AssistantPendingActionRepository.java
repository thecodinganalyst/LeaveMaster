package com.practical.leavemaster.assistant;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface AssistantPendingActionRepository extends JpaRepository<AssistantPendingAction, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AssistantPendingAction a where a.confirmationToken = :token")
    Optional<AssistantPendingAction> findByTokenForUpdate(@Param("token") String token);
}
