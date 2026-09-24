package com.practical.leavemaster.assistant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.Instant;
interface AssistantQualityEventRepository extends JpaRepository<AssistantQualityEvent,String> {
 List<AssistantQualityEvent> findAllByTenantId(String tenantId);
 List<AssistantQualityEvent> findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Instant since);
}
