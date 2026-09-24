package com.practical.leavemaster.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class AssistantQualityDashboardService {
    private final AssistantQualityEventRepository repository;

    @Transactional(readOnly=true)
    Dashboard dashboard(Instant since,String outcome,String failureCategory,String provider,String model) {
        List<AssistantQualityEvent> all=repository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(since);
        List<AssistantQualityEvent> filtered=all.stream().filter(e->matches(e,outcome,failureCategory,provider,model)).toList();
        List<AssistantQualityEvent> requests=filtered.stream().filter(e->"REQUEST".equals(e.getEventType())).toList();
        List<AssistantQualityEvent> feedback=filtered.stream().filter(e->"FEEDBACK".equals(e.getEventType())).toList();
        long successful=requests.stream().filter(AssistantQualityEvent::isSuccess).count();
        List<Long> latencies=requests.stream().map(AssistantQualityEvent::getLatencyMs).filter(Objects::nonNull).sorted().toList();
        long retries=requests.stream().map(AssistantQualityEvent::getRetryCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        Map<String,Long> failures=requests.stream().filter(e->!e.isSuccess()).collect(Collectors.groupingBy(e->safe(e.getFailureCategory(),"UNKNOWN"),TreeMap::new,Collectors.counting()));
        Map<String,Long> tools=requests.stream().flatMap(e->Optional.ofNullable(e.getToolNames()).stream().flatMap(x->Arrays.stream(x.split(",")))).filter(x->!x.isBlank()).collect(Collectors.groupingBy(x->x,TreeMap::new,Collectors.counting()));
        Map<String,Long> models=requests.stream().collect(Collectors.groupingBy(e->safe(e.getProvider(),"unknown")+"/"+safe(e.getModel(),"unknown"),TreeMap::new,Collectors.counting()));
        long positive=feedback.stream().filter(e->Integer.valueOf(1).equals(e.getFeedbackRating())).count();
        long negative=feedback.stream().filter(e->Integer.valueOf(-1).equals(e.getFeedbackRating())).count();
        List<RequestRow> recent=requests.stream().limit(100).map(e->new RequestRow(e.getCorrelationId(),e.getCreatedAt(),e.getActorRole(),e.getIntentCategory(),e.getToolNames(),e.getLatencyMs(),e.getProvider(),e.getModel(),e.getRetryCount(),e.isSuccess(),e.getFailureCategory())).toList();
        return new Dashboard(since,Instant.now(),requests.size(),successful,requests.size()-successful,
                requests.isEmpty()?0d:(successful*100d/requests.size()),average(latencies),percentile(latencies,.95),
                retries,failures,tools,models,positive,negative,recent);
    }
    private boolean matches(AssistantQualityEvent e,String outcome,String failure,String provider,String model){
        if(outcome!=null&&!outcome.isBlank()&&"REQUEST".equals(e.getEventType())&&!("success".equalsIgnoreCase(outcome)==e.isSuccess())) return false;
        if(failure!=null&&!failure.isBlank()&&!failure.equalsIgnoreCase(safe(e.getFailureCategory(),""))) return false;
        if(provider!=null&&!provider.isBlank()&&!provider.equalsIgnoreCase(safe(e.getProvider(),""))) return false;
        return model==null||model.isBlank()||model.equalsIgnoreCase(safe(e.getModel(),""));
    }
    private double average(List<Long> values){return values.stream().mapToLong(Long::longValue).average().orElse(0);}
    private long percentile(List<Long> values,double p){if(values.isEmpty())return 0;return values.get(Math.min(values.size()-1,(int)Math.ceil(values.size()*p)-1));}
    private String safe(String value,String fallback){return value==null||value.isBlank()?fallback:value;}
    record Dashboard(Instant from,Instant to,long requests,long successful,long failed,double successRatePercent,double averageLatencyMs,long p95LatencyMs,long retryCount,Map<String,Long> failuresByCategory,Map<String,Long> tools,Map<String,Long> providerModels,long positiveFeedback,long negativeFeedback,List<RequestRow> recentRequests){}
    record RequestRow(String correlationId,Instant timestamp,String actorRole,String intentCategory,String tools,Long latencyMs,String provider,String model,Integer retries,boolean success,String failureCategory){}
}
