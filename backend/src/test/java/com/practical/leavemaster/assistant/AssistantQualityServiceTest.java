package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AssistantQualityServiceTest {
 @Test void recordsOnlyMetadataAndSanitizesToolNames() {
   var repo=mock(AssistantQualityEventRepository.class);
   var service=new AssistantQualityService(repo);
   var auth=new UsernamePasswordAuthenticationToken("user","n/a",List.of(new SimpleGrantedAuthority("DEMO_Staff")));
   service.recordRequest("corr","DEMO",auth,"google","gemini",List.of("getLeaveBalance","bad tool(secret)"),123,1,true,null);
   var captor=org.mockito.ArgumentCaptor.forClass(AssistantQualityEvent.class);
   verify(repo).save(captor.capture());
   var e=captor.getValue();
   assertThat(e.getCorrelationId()).isEqualTo("corr");
   assertThat(e.getToolNames()).isEqualTo("getLeaveBalance,badtoolsecret");
   assertThat(e.getActorRole()).isEqualTo("DEMO_Staff");
   assertThat(e.getClass().getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
      .doesNotContain("prompt","message","response","arguments");
 }
 @Test void validatesFeedbackAndReturnsAggregateFailureData() {
   var repo=mock(AssistantQualityEventRepository.class);
   when(repo.findAllByTenantId("DEMO")).thenReturn(List.of(
     AssistantQualityEvent.builder().eventType("REQUEST").success(true).latencyMs(100L).build(),
     AssistantQualityEvent.builder().eventType("REQUEST").success(false).latencyMs(300L).failureCategory("TOOL_FAILURE").build()));
   var service=new AssistantQualityService(repo);
   var summary=service.summary("DEMO");
   assertThat(summary.total()).isEqualTo(2);
   assertThat(summary.averageLatencyMs()).isEqualTo(200);
   assertThat(summary.failuresByCategory()).containsEntry("TOOL_FAILURE",1L);
   service.recordFeedback("c","DEMO",null,1,"useful<script>");
   var feedback=org.mockito.ArgumentCaptor.forClass(AssistantQualityEvent.class);
   verify(repo, atLeast(2)).save(feedback.capture());
   assertThat(feedback.getAllValues().getLast().getFeedbackCategory()).isEqualTo("usefulscript");
   org.assertj.core.api.Assertions.assertThatThrownBy(()->service.recordFeedback("c","DEMO",null,0,"x"))
     .isInstanceOf(IllegalArgumentException.class);
 }
}
