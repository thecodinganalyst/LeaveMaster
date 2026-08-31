package com.practical.leavemaster.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
@Slf4j
public class ResendTransactionalEmailSender implements TransactionalEmailSender {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_PROVIDER_MESSAGE_LENGTH = 300;

    private final RestClient restClient;
    private final String fromAddress;
    private final String fromName;

    public ResendTransactionalEmailSender(
            RestClient.Builder restClientBuilder,
            @Value("${app.email.resend.api-key:}") String apiKey,
            @Value("${app.email.resend.base-url:https://api.resend.com}") String baseUrl,
            @Value("${app.email.from-address:onboarding@resend.dev}") String fromAddress,
            @Value("${app.email.from-name:LeaveMaster}") String fromName) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY must be configured when app.email.provider=resend");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("EMAIL_FROM_ADDRESS must not be blank");
        }
        this.fromAddress = fromAddress.trim();
        this.fromName = (fromName == null || fromName.isBlank()) ? "LeaveMaster" : fromName.trim();
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    public void sendAccountActivationPin(String recipient, String staffName, String pin, int expiryMinutes) {
        String displayName = staffName == null || staffName.isBlank() ? "there" : escapeHtml(staffName.trim());
        String html = """
                <div style=\"font-family:Arial,sans-serif;line-height:1.5;color:#1f2937\">
                  <h2>LeaveMaster account activation</h2>
                  <p>Hello %s,</p>
                  <p>Use the verification PIN below to complete your initial LeaveMaster account activation and password setup.</p>
                  <p style=\"font-size:28px;font-weight:700;letter-spacing:6px\">%s</p>
                  <p>This PIN expires in %d minutes and can be used only for this account activation.</p>
                  <p>If you did not request this PIN, ignore this email and contact your LeaveMaster administrator if you are concerned.</p>
                </div>
                """.formatted(displayName, pin, expiryMinutes);
        sendHtmlEmail(recipient, "Your LeaveMaster verification PIN", html, "account activation");
    }

    @Override
    public void sendContactEnquiryReply(String recipient, String contactName, String originalMessage, String replyBody) {
        String displayName = contactName == null || contactName.isBlank() ? "there" : escapeHtml(contactName.trim());
        String html = """
                <div style=\"font-family:Arial,sans-serif;line-height:1.5;color:#1f2937\">
                  <h2>LeaveMaestro enquiry reply</h2>
                  <p>Hello %s,</p>
                  <div style=\"white-space:pre-wrap\">%s</div>
                  <hr style=\"margin:24px 0;border:0;border-top:1px solid #e5e7eb\" />
                  <p style=\"color:#6b7280\">Your original enquiry:</p>
                  <div style=\"white-space:pre-wrap;color:#6b7280\">%s</div>
                </div>
                """.formatted(displayName, escapeHtml(replyBody), escapeHtml(originalMessage));
        sendHtmlEmail(recipient, "Re: Your LeaveMaestro enquiry", html, "contact enquiry reply");
    }

    private void sendHtmlEmail(String recipient, String subject, String html, String purpose) {
        ResendEmailRequest request = new ResendEmailRequest(
                "%s <%s>".formatted(fromName, fromAddress), List.of(recipient), subject, html);
        try {
            log.info("Sending {} email through Resend", purpose);
            restClient.post().uri("/emails").contentType(MediaType.APPLICATION_JSON).body(request)
                    .retrieve().toBodilessEntity();
            log.info("Resend accepted {} email", purpose);
        } catch (RestClientResponseException ex) {
            ResendProviderError providerError = parseProviderError(ex.getResponseBodyAsString());
            log.warn("Resend rejected {} email with HTTP status {} type={} message={}",
                    purpose,
                    ex.getStatusCode().value(),
                    providerError.type(),
                    providerError.message());
            throw new EmailDeliveryException("Transactional email provider rejected the request", ex);
        } catch (RestClientException ex) {
            log.warn("Resend {} email delivery failed due to a transport error", purpose);
            throw new EmailDeliveryException("Transactional email provider is unavailable", ex);
        }
    }

    private static ResendProviderError parseProviderError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new ResendProviderError("unknown", "No provider error message returned");
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            String type = firstText(root, "type", "name", "code");
            String message = firstText(root, "message", "error");
            return new ResendProviderError(
                    sanitizeProviderValue(type, "unknown"),
                    sanitizeProviderValue(message, "Provider returned an unstructured error"));
        } catch (JsonProcessingException ex) {
            return new ResendProviderError("unparseable", "Provider returned a non-JSON error response");
        }
    }

    private static String firstText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = root.get(fieldName);
            if (value != null && value.isValueNode() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private static String sanitizeProviderValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (sanitized.length() > MAX_PROVIDER_MESSAGE_LENGTH) {
            return sanitized.substring(0, MAX_PROVIDER_MESSAGE_LENGTH) + "...";
        }
        return sanitized;
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    record ResendEmailRequest(String from, List<String> to, String subject, String html) {}

    record ResendProviderError(String type, String message) {}
}
