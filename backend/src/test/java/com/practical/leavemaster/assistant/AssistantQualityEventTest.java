package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class AssistantQualityEventTest {
 @Test void lombokEntityAccessorsAndBuilderRemainUsableForJpa() {
   var now=Instant.now();
   var event=AssistantQualityEvent.builder().id("id").correlationId("corr").tenantId("T").actorRole("R")
     .eventType("REQUEST").intentCategory("LEAVE").provider("gemini").model("model").toolNames("tool")
     .failureCategory("NONE").latencyMs(10L).retryCount(1).success(true).feedbackRating(1)
     .feedbackCategory("helpful").createdAt(now).build();
   assertThat(event.getId()).isEqualTo("id");
   assertThat(event.getCorrelationId()).isEqualTo("corr");
   assertThat(event.getTenantId()).isEqualTo("T");
   assertThat(event.getActorRole()).isEqualTo("R");
   assertThat(event.getEventType()).isEqualTo("REQUEST");
   assertThat(event.getIntentCategory()).isEqualTo("LEAVE");
   assertThat(event.getProvider()).isEqualTo("gemini");
   assertThat(event.getModel()).isEqualTo("model");
   assertThat(event.getToolNames()).isEqualTo("tool");
   assertThat(event.getFailureCategory()).isEqualTo("NONE");
   assertThat(event.getLatencyMs()).isEqualTo(10);
   assertThat(event.getRetryCount()).isEqualTo(1);
   assertThat(event.isSuccess()).isTrue();
   assertThat(event.getFeedbackRating()).isEqualTo(1);
   assertThat(event.getFeedbackCategory()).isEqualTo("helpful");
   assertThat(event.getCreatedAt()).isEqualTo(now);
   event.setSuccess(false); event.setLatencyMs(20L); event.setFailureCategory("TOOL_FAILURE");
   assertThat(event.isSuccess()).isFalse();
   assertThat(event.getLatencyMs()).isEqualTo(20);
   assertThat(event.getFailureCategory()).isEqualTo("TOOL_FAILURE");
 }
 @Test void noArgsConstructorSupportsJpa() {
   var event=new AssistantQualityEvent();
   event.setId("x"); event.setCorrelationId("c"); event.setEventType("FEEDBACK"); event.setSuccess(true); event.setCreatedAt(Instant.now());
   assertThat(event.getId()).isEqualTo("x");
 }
}
