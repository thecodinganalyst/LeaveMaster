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
   assertThat(e.getIntentCategory()).isEqualTo("LEAVE");
   service.recordRequest("c2","DEMO",null,"p","m",List.of(),1,0,false,"MODEL_FAILURE");
   service.recordRequest("c3","DEMO",null,"p","m",List.of("getStaff"),1,0,true,null);
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
   verify(repo).save(feedback.capture());
   assertThat(feedback.getAllValues().getLast().getFeedbackCategory()).isEqualTo("usefulscript");
   org.assertj.core.api.Assertions.assertThatThrownBy(()->service.recordFeedback("c","DEMO",null,0,"x"))
     .isInstanceOf(IllegalArgumentException.class);
 }
 @Test void coversUnknownRolesEmptyCategoriesAndFailureAggregation() {
   var repo=mock(AssistantQualityEventRepository.class);
   when(repo.findAllByTenantId("T")).thenReturn(List.of(
     AssistantQualityEvent.builder().eventType("REQUEST").success(false).failureCategory(null).build(),
     AssistantQualityEvent.builder().eventType("FEEDBACK").success(true).build()));
   var service=new AssistantQualityService(repo);
   var auth=new UsernamePasswordAuthenticationToken("user","n/a",List.of());
   service.recordRequest("c","T",auth,"p","m",null,0,0,true,null);
   service.recordFeedback("c","T",auth,-1,null);
   var summary=service.summary("T");
   assertThat(summary.total()).isEqualTo(1);
   assertThat(summary.failed()).isEqualTo(1);
   assertThat(summary.averageLatencyMs()).isZero();
   assertThat(summary.failuresByCategory()).containsEntry("UNKNOWN",1L);
   var captor=org.mockito.ArgumentCaptor.forClass(AssistantQualityEvent.class);
   verify(repo,atLeastOnce()).save(captor.capture());
   assertThat(captor.getAllValues()).anySatisfy(e -> assertThat(e.getActorRole()).isEqualTo("UNKNOWN"));
 }

}
