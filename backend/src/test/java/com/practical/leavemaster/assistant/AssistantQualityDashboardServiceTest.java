package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AssistantQualityDashboardServiceTest {
 @Test void aggregatesPrivacySafeMetricsWithoutPromptPayloads(){
  var repo=mock(AssistantQualityEventRepository.class);
  when(repo.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(any())).thenReturn(List.of(
   AssistantQualityEvent.builder().id("1").correlationId("c1").eventType("REQUEST").actorRole("PLATFORM_ADMIN").intentCategory("LEAVE").provider("gemini").model("model").toolNames("getLeaveBalances").latencyMs(100L).retryCount(1).success(true).createdAt(Instant.now()).build(),
   AssistantQualityEvent.builder().id("2").correlationId("c2").eventType("REQUEST").actorRole("STAFF").provider("gemini").model("model").latencyMs(300L).retryCount(0).success(false).failureCategory("TOOL").createdAt(Instant.now()).build(),
   AssistantQualityEvent.builder().id("3").correlationId("c1").eventType("FEEDBACK").feedbackRating(1).success(true).createdAt(Instant.now()).build()));
  var result=new AssistantQualityDashboardService(repo).dashboard(Instant.now().minusSeconds(60),null,null,null,null);
  assertThat(result.requests()).isEqualTo(2); assertThat(result.successful()).isEqualTo(1); assertThat(result.failed()).isEqualTo(1);
  assertThat(result.successRatePercent()).isEqualTo(50); assertThat(result.averageLatencyMs()).isEqualTo(200); assertThat(result.p95LatencyMs()).isEqualTo(300);
  assertThat(result.retryCount()).isEqualTo(1); assertThat(result.failuresByCategory()).containsEntry("TOOL",1L);
  assertThat(result.tools()).containsEntry("getLeaveBalances",1L); assertThat(result.positiveFeedback()).isEqualTo(1);
  assertThat(result.recentRequests()).allSatisfy(row -> assertThat(row.toString()).doesNotContain("prompt","employeePayload"));
 }
 @Test void filtersOutcomeAndProvider(){
  var repo=mock(AssistantQualityEventRepository.class);
  when(repo.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(any())).thenReturn(List.of(
   AssistantQualityEvent.builder().id("1").correlationId("c1").eventType("REQUEST").provider("gemini").model("m").success(true).createdAt(Instant.now()).build(),
   AssistantQualityEvent.builder().id("2").correlationId("c2").eventType("REQUEST").provider("other").model("m").success(false).createdAt(Instant.now()).build()));
  var result=new AssistantQualityDashboardService(repo).dashboard(Instant.EPOCH,"failure",null,"other",null);
  assertThat(result.requests()).isEqualTo(1); assertThat(result.failed()).isEqualTo(1);
 }
}
