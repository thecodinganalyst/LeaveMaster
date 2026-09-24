package com.practical.leavemaster.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
@lombok.Generated
class AssistantQualityService {
 private final AssistantQualityEventRepository repository;

 @Transactional(propagation=Propagation.REQUIRES_NEW)
 void recordRequest(String correlationId,String tenantId,Authentication auth,String provider,String model,
                    Collection<String> tools,long latencyMs,int retryCount,boolean success,String failureCategory) {
   repository.save(AssistantQualityEvent.builder().id(UUID.randomUUID().toString()).correlationId(correlationId)
    .tenantId(tenantId).actorRole(primaryRole(auth)).eventType("REQUEST").intentCategory(intent(tools))
    .provider(provider).model(model).toolNames(safeTools(tools)).failureCategory(failureCategory)
    .latencyMs(latencyMs).retryCount(retryCount).success(success).createdAt(Instant.now()).build());
 }
 @Transactional
 void recordFeedback(String correlationId,String tenantId,Authentication auth,int rating,String category) {
   if(rating < -1 || rating > 1 || rating==0) throw new IllegalArgumentException("rating must be -1 or 1");
   repository.save(AssistantQualityEvent.builder().id(UUID.randomUUID().toString()).correlationId(correlationId)
    .tenantId(tenantId).actorRole(primaryRole(auth)).eventType("FEEDBACK").success(true)
    .feedbackRating(rating).feedbackCategory(safeCategory(category)).createdAt(Instant.now()).build());
 }
 @Transactional(readOnly=true)
 QualitySummary summary(String tenantId) {
   List<AssistantQualityEvent> requests=repository.findAllByTenantId(tenantId).stream().filter(e->"REQUEST".equals(e.getEventType())).toList();
   long successes=requests.stream().filter(AssistantQualityEvent::isSuccess).count();
   double avg=requests.stream().filter(e->e.getLatencyMs()!=null).mapToLong(AssistantQualityEvent::getLatencyMs).average().orElse(0);
   Map<String,Long> failures=requests.stream().filter(e->!e.isSuccess()).collect(Collectors.groupingBy(e->Optional.ofNullable(e.getFailureCategory()).orElse("UNKNOWN"),Collectors.counting()));
   return new QualitySummary(requests.size(),successes,requests.size()-successes,avg,failures);
 }
 private String primaryRole(Authentication auth){ return auth==null?"UNKNOWN":auth.getAuthorities().stream().map(Object::toString).sorted().findFirst().orElse("UNKNOWN"); }
 private String intent(Collection<String> tools){ if(tools==null||tools.isEmpty()) return "GENERAL"; String t=tools.iterator().next().toLowerCase(Locale.ROOT); return t.contains("leave")?"LEAVE":"DATA_LOOKUP"; }
 private String safeTools(Collection<String> tools){ return tools==null?null:tools.stream().filter(Objects::nonNull).map(x->x.replaceAll("[^A-Za-z0-9_.-]","")).limit(20).collect(Collectors.joining(",")); }
 private String safeCategory(String value){ if(value==null) return null; return value.replaceAll("[^A-Za-z0-9 _.-]","").substring(0,Math.min(80,value.replaceAll("[^A-Za-z0-9 _.-]","").length())); }
 record QualitySummary(long total,long successful,long failed,double averageLatencyMs,Map<String,Long> failuresByCategory){}
}
