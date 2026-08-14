package com.practical.leavemaster.leaveentitlementpolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveEntitlementPolicyControllerTest {

    @Mock
    private LeaveEntitlementPolicyService policyService;

    @InjectMocks
    private LeaveEntitlementPolicyController controller;

    @Test
    void getAllReturnsPolicies() {
        LeaveEntitlementPolicy policy = LeaveEntitlementPolicy.builder().id("p1").name("Annual").build();
        when(policyService.findAll()).thenReturn(List.of(policy));

        assertThat(controller.getAll()).containsExactly(policy);
    }

    @Test
    void getByIdReturnsOkWhenFound() {
        LeaveEntitlementPolicy policy = LeaveEntitlementPolicy.builder().id("p1").name("Annual").build();
        when(policyService.findById("p1")).thenReturn(Optional.of(policy));

        var response = controller.getById("p1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(policy);
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() {
        when(policyService.findById("missing")).thenReturn(Optional.empty());

        assertThat(controller.getById("missing").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createReturnsCreatedPolicy() {
        LeaveEntitlementPolicy request = LeaveEntitlementPolicy.builder().name("Annual").build();
        LeaveEntitlementPolicy saved = LeaveEntitlementPolicy.builder().id("p1").name("Annual").build();
        when(policyService.create(request)).thenReturn(saved);

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(saved);
    }

    @Test
    void updateReturnsUpdatedPolicy() {
        LeaveEntitlementPolicy request = LeaveEntitlementPolicy.builder().name("Annual 18").build();
        LeaveEntitlementPolicy saved = LeaveEntitlementPolicy.builder().id("p1").name("Annual 18").build();
        when(policyService.update("p1", request)).thenReturn(saved);

        assertThat(controller.update("p1", request)).isSameAs(saved);
    }

    @Test
    void deleteReturnsNoContent() {
        var response = controller.delete("p1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(policyService).delete("p1");
    }
}
