package com.practical.leavemaster.customerenquiry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerEnquiryServiceTest {

    @Mock
    private CustomerEnquiryRepository repository;

    @Mock
    private CustomerEnquiryNotificationService notificationService;

    @InjectMocks
    private CustomerEnquiryService service;

    @Test
    void shouldNormalizePersistAndNotify() {
        when(repository.save(any(CustomerEnquiry.class))).thenAnswer(invocation -> {
            CustomerEnquiry enquiry = invocation.getArgument(0);
            enquiry.setId("enquiry-1");
            return enquiry;
        });

        service.submit(validRequest());

        ArgumentCaptor<CustomerEnquiry> captor = ArgumentCaptor.forClass(CustomerEnquiry.class);
        verify(repository).save(captor.capture());
        CustomerEnquiry saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Jane Doe");
        assertThat(saved.getCompany()).isEqualTo("Example Pte Ltd");
        assertThat(saved.getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getPhone()).isNull();
        assertThat(saved.getStatus()).isEqualTo(CustomerEnquiryStatus.NEW);
        verify(notificationService).notifyNewEnquiry(saved);
    }

    @Test
    void shouldKeepPersistedSubmissionWhenNotificationFails() {
        when(repository.save(any(CustomerEnquiry.class))).thenAnswer(invocation -> {
            CustomerEnquiry enquiry = invocation.getArgument(0);
            enquiry.setId("enquiry-2");
            return enquiry;
        });
        doThrow(new IllegalStateException("mail unavailable"))
                .when(notificationService).notifyNewEnquiry(any(CustomerEnquiry.class));

        service.submit(validRequest());

        verify(repository).save(any(CustomerEnquiry.class));
    }

    @Test
    void shouldRejectMissingAndInvalidFields() {
        assertThatThrownBy(() -> service.submit(null))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessage("Request body is required");
        assertThatThrownBy(() -> service.submit(new CustomerEnquiryRequest(
                "Jane", "Example", "invalid", null, null, null, CustomerEnquiryType.OTHER, "Hello", "")))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessage("A valid work email is required");
        assertThatThrownBy(() -> service.submit(new CustomerEnquiryRequest(
                "Jane", "Example", "jane@example.com", null, null, null, null, "Hello", "")))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessage("enquiryType is required");
        assertThatThrownBy(() -> service.submit(new CustomerEnquiryRequest(
                "Jane", "Example", "jane@example.com", null, null, null, CustomerEnquiryType.OTHER, "Hello", "bot")))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessage("Invalid submission");
    }

    @Test
    void shouldEnforceFieldLengths() {
        assertThatThrownBy(() -> service.submit(new CustomerEnquiryRequest(
                "x".repeat(121), "Example", "jane@example.com", null, null, null,
                CustomerEnquiryType.OTHER, "Hello", "")))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessageContaining("name exceeds");
        assertThatThrownBy(() -> service.submit(new CustomerEnquiryRequest(
                "Jane", "Example", "jane@example.com", "x".repeat(41), null, null,
                CustomerEnquiryType.OTHER, "Hello", "")))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessageContaining("phone exceeds");
    }

    private CustomerEnquiryRequest validRequest() {
        return new CustomerEnquiryRequest(
                " Jane Doe ",
                " Example Pte Ltd ",
                " JANE@EXAMPLE.COM ",
                " ",
                "21-100",
                "Singapore",
                CustomerEnquiryType.PRODUCT_DEMO,
                " Please arrange a demo. ",
                "");
    }
}
