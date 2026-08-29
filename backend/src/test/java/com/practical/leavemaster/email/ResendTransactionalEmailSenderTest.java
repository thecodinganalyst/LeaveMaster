package com.practical.leavemaster.email;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendTransactionalEmailSenderTest {

    @Test
    void shouldBuildConfiguredResendActivationEmail() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendTransactionalEmailSender sender = new ResendTransactionalEmailSender(
                builder, "test-api-key", "https://api.resend.com", "onboarding@resend.dev", "LeaveMaster");

        server.expect(once(), requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "from":"LeaveMaster <onboarding@resend.dev>",
                          "to":["alice@example.com"],
                          "subject":"Your LeaveMaster verification PIN"
                        }
                        """, false))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Alice &amp; Bob"),
                        org.hamcrest.Matchers.containsString("123456"),
                        org.hamcrest.Matchers.containsString("expires in 15 minutes"),
                        org.hamcrest.Matchers.containsString("initial LeaveMaster account activation"))))
                .andRespond(withSuccess("{\"id\":\"email-id\"}", MediaType.APPLICATION_JSON));

        sender.sendAccountActivationPin("alice@example.com", "Alice & Bob", "123456", 15);

        server.verify();
    }

    @Test
    void shouldBuildContactEnquiryReplyEmailAndEscapeSubmittedContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendTransactionalEmailSender sender = new ResendTransactionalEmailSender(
                builder, "test-api-key", "https://api.resend.com", "onboarding@resend.dev", "LeaveMaestro");

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "from":"LeaveMaestro <onboarding@resend.dev>",
                          "to":["alice@example.com"],
                          "subject":"Re: Your LeaveMaestro enquiry"
                        }
                        """, false))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Alice &lt;Admin&gt;"),
                        org.hamcrest.Matchers.containsString("Thanks &amp; welcome"),
                        org.hamcrest.Matchers.containsString("Need &lt;help&gt;"))))
                .andRespond(withSuccess("{\"id\":\"email-id\"}", MediaType.APPLICATION_JSON));

        sender.sendContactEnquiryReply("alice@example.com", "Alice <Admin>", "Need <help>", "Thanks & welcome");
        server.verify();
    }

    @Test
    void shouldAllowCustomVerifiedSenderWithoutCodeChange() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendTransactionalEmailSender sender = new ResendTransactionalEmailSender(
                builder, "test-api-key", "https://api.resend.com", "account@mail.example.com", "LeaveMaster Accounts");

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "LeaveMaster Accounts <account@mail.example.com>")))
                .andRespond(withSuccess("{\"id\":\"email-id\"}", MediaType.APPLICATION_JSON));

        sender.sendAccountActivationPin("alice@example.com", "Alice", "123456", 15);
        server.verify();
    }

    @Test
    void shouldReturnSanitizedFailureWithoutSecretsOrPin() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendTransactionalEmailSender sender = new ResendTransactionalEmailSender(
                builder, "secret-api-key", "https://api.resend.com", "onboarding@resend.dev", "LeaveMaster");

        server.expect(requestTo("https://api.resend.com/emails")).andRespond(withServerError());

        assertThatThrownBy(() -> sender.sendAccountActivationPin("alice@example.com", "Alice", "654321", 15))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageNotContaining("secret-api-key")
                .hasMessageNotContaining("654321");
    }

    @Test
    void shouldRejectMissingApiKeyWhenResendIsEnabled() {
        assertThatThrownBy(() -> new ResendTransactionalEmailSender(
                RestClient.builder(), " ", "https://api.resend.com", "onboarding@resend.dev", "LeaveMaster"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESEND_API_KEY");
    }
}
