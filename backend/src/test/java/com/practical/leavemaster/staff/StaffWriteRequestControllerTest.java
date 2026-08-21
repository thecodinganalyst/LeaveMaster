package com.practical.leavemaster.staff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaffController.class)
@WithMockUser
class StaffWriteRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaffService staffService;

    @MockitoBean
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldBindEntitlementByLeaveTypeIdWithoutDeserializingLeaveTypeEntity() throws Exception {
        Staff saved = Staff.builder()
                .id("S001")
                .name("Alice")
                .joinDate(LocalDate.of(2026, 8, 3))
                .build();
        when(staffService.save(argThat(staff ->
                staff != null
                        && staff.getLeaveEntitlements().size() == 1
                        && "LT-ANNUAL".equals(staff.getLeaveEntitlements().getFirst().getLeaveType().getId())
                        && new BigDecimal("10.0").compareTo(staff.getLeaveEntitlements().getFirst().getEntitlement()) == 0
                        && BigDecimal.ZERO.compareTo(staff.getLeaveEntitlements().getFirst().getCarriedForwardAmount()) == 0
                        && BigDecimal.ZERO.compareTo(staff.getLeaveEntitlements().getFirst().getAdjustmentAmount()) == 0)))
                .thenReturn(saved);

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "S001",
                                  "name": "Alice",
                                  "joinDate": "2026-08-03",
                                  "jurisdictionId": "SG",
                                  "leaveEntitlements": [{
                                    "leaveTypeId": "LT-ANNUAL",
                                    "from": "2026-08-03",
                                    "to": "2026-12-31",
                                    "entitlement": 10.0
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("S001"));
    }

    @Test
    void shouldIgnoreLegacyNestedLeaveTypeFieldsInsteadOfFailingOnNullPrimitive() throws Exception {
        Staff saved = Staff.builder()
                .id("S002")
                .name("Bob")
                .joinDate(LocalDate.of(2026, 8, 3))
                .build();
        when(staffService.save(argThat(staff ->
                staff != null
                        && staff.getLeaveEntitlements().size() == 1
                        && "LT-ANNUAL".equals(staff.getLeaveEntitlements().getFirst().getLeaveType().getId()))))
                .thenReturn(saved);

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "S002",
                                  "name": "Bob",
                                  "joinDate": "2026-08-03",
                                  "jurisdictionId": "SG",
                                  "leaveEntitlements": [{
                                    "leaveTypeId": "LT-ANNUAL",
                                    "leaveType": {
                                      "id": "LT-ANNUAL",
                                      "name": "Annual Leave",
                                      "used": null
                                    },
                                    "from": "2026-08-03",
                                    "to": "2026-12-31",
                                    "entitlement": 10.0
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("S002"));
    }
}
