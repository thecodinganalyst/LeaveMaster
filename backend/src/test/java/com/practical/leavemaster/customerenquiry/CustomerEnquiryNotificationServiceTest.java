package com.practical.leavemaster.customerenquiry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerEnquiryNotificationServiceTest {

    @Test
    void shouldSendConfiguredNotification() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        CustomerEnquiryNotificationService service = new CustomerEnquiryNotificationService(
                provider, "sales@leavemaestro.com", "noreply@leavemaestro.com");

        service.notifyNewEnquiry(enquiry());

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSkipWhenRecipientOrTransportIsMissing() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        new CustomerEnquiryNotificationService(provider, "", "noreply@leavemaestro.com")
                .notifyNewEnquiry(enquiry());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));

        when(provider.getIfAvailable()).thenReturn(null);
        new CustomerEnquiryNotificationService(provider, "sales@leavemaestro.com", "noreply@leavemaestro.com")
                .notifyNewEnquiry(enquiry());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    private CustomerEnquiry enquiry() {
        return CustomerEnquiry.builder()
                .id("enquiry-1")
                .name("Jane\nDoe")
                .company("Example\rCompany")
                .email("jane@example.com")
                .enquiryType(CustomerEnquiryType.PRODUCT_DEMO)
                .build();
    }
}
