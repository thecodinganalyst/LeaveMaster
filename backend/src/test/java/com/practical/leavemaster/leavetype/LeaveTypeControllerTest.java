package com.practical.leavemaster.leavetype;

import com.practical.leavemaster.leaveentitlementpolicy.EligibilityCriterionType;
import com.practical.leavemaster.leaveentitlementpolicy.EligibilityOperator;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRule;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityService;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveTypeController.class)
@WithMockUser
class LeaveTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveTypeService leaveTypeService;

    @MockitoBean
    private LeaveEntitlementPolicyService entitlementPolicyService;

    @MockitoBean
    private LeaveEntitlementPolicyEligibilityService eligibilityService;

    @MockitoBean
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldReturnAllLeaveTypes() throws Exception {
        List<LeaveType> leaveTypes = List.of(
                LeaveType.builder().id("annual").name("Annual Leave").used(false).build(),
                LeaveType.builder().id("medical").name("Medical Leave").used(true).build()
        );
        when(leaveTypeService.findAll()).thenReturn(leaveTypes);

        mockMvc.perform(get("/leave-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("annual"))
                .andExpect(jsonPath("$[1].id").value("medical"));
    }

    @Test
    void shouldReturnLeaveTypeById() throws Exception {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(false).build();
        when(leaveTypeService.findById("annual")).thenReturn(Optional.of(leaveType));

        mockMvc.perform(get("/leave-types/annual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("annual"))
                .andExpect(jsonPath("$.name").value("Annual Leave"))
                .andExpect(jsonPath("$.used").value(false));
    }

    @Test
    void shouldReturnStaffSafeEntitlementViewForLeaveType() throws Exception {
        LeaveType leaveType = LeaveType.builder().id("annual").tenantId("TENANT-A").name("Annual Leave").build();
        LeaveEntitlementPolicy matching = LeaveEntitlementPolicy.builder()
                .id("policy-1")
                .tenantId("TENANT-A")
                .leaveTypeId("annual")
                .name("internal policy name")
                .entitlementAmount(BigDecimal.valueOf(14))
                .entitlementUnit(EntitlementUnit.DAYS)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .active(true)
                .build();
        LeaveEntitlementPolicy other = LeaveEntitlementPolicy.builder()
                .id("policy-2")
                .tenantId("TENANT-A")
                .leaveTypeId("medical")
                .name("other")
                .entitlementAmount(BigDecimal.valueOf(14))
                .entitlementUnit(EntitlementUnit.DAYS)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .active(true)
                .build();
        LeaveEntitlementPolicyEligibilityRule rule = LeaveEntitlementPolicyEligibilityRule.builder()
                .id("rule-1")
                .policyId("policy-1")
                .criterionType(EligibilityCriterionType.SERVICE_MONTHS)
                .operator(EligibilityOperator.GREATER_THAN_OR_EQUAL)
                .value("3")
                .active(true)
                .sortOrder(1)
                .build();

        when(leaveTypeService.findById("annual")).thenReturn(Optional.of(leaveType));
        when(entitlementPolicyService.findAll()).thenReturn(List.of(matching, other));
        when(eligibilityService.findAll("policy-1")).thenReturn(List.of(rule));

        mockMvc.perform(get("/api/leave-types/annual/entitlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("policy-1"))
                .andExpect(jsonPath("$[0].entitlementAmount").value(14))
                .andExpect(jsonPath("$[0].entitlementUnit").value("DAYS"))
                .andExpect(jsonPath("$[0].effectiveFrom").value("2026-01-01"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].eligibilityRules[0].criterionType").value("SERVICE_MONTHS"))
                .andExpect(jsonPath("$[0].eligibilityRules[0].operator").value("GREATER_THAN_OR_EQUAL"))
                .andExpect(jsonPath("$[0].eligibilityRules[0].value").value("3"))
                .andExpect(jsonPath("$[0].tenantId").doesNotExist())
                .andExpect(jsonPath("$[0].name").doesNotExist())
                .andExpect(jsonPath("$[0].eligibilityRules[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].eligibilityRules[0].policyId").doesNotExist());
    }

    @Test
    void shouldReturn404WhenEntitlementLeaveTypeNotFound() throws Exception {
        when(leaveTypeService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/leave-types/nonexistent/entitlements"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(entitlementPolicyService, eligibilityService);
    }

    @Test
    void shouldReturn404WhenLeaveTypeNotFound() throws Exception {
        when(leaveTypeService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/leave-types/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateLeaveType() throws Exception {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(false).build();
        when(leaveTypeService.save(any(LeaveType.class))).thenReturn(leaveType);

        mockMvc.perform(post("/leave-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaveType)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("annual"))
                .andExpect(jsonPath("$.name").value("Annual Leave"));
    }

    @Test
    void shouldUpdateLeaveType() throws Exception {
        LeaveType updated = LeaveType.builder().id("annual").name("Annual Leave Updated").used(true).build();
        when(leaveTypeService.update(eq("annual"), any(LeaveType.class))).thenReturn(updated);

        mockMvc.perform(put("/leave-types/annual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Annual Leave Updated"))
                .andExpect(jsonPath("$.used").value(true));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentLeaveType() throws Exception {
        when(leaveTypeService.update(eq("nonexistent"), any(LeaveType.class)))
                .thenThrow(new LeaveTypeNotFoundException("nonexistent"));

        mockMvc.perform(put("/leave-types/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveType())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteLeaveType() throws Exception {
        doNothing().when(leaveTypeService).delete("annual");

        mockMvc.perform(delete("/leave-types/annual"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentLeaveType() throws Exception {
        doThrow(new LeaveTypeNotFoundException("nonexistent")).when(leaveTypeService).delete("nonexistent");

        mockMvc.perform(delete("/leave-types/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenDeletingLeaveTypeInUse() throws Exception {
        doThrow(new LeaveTypeInUseException("medical")).when(leaveTypeService).delete("medical");

        mockMvc.perform(delete("/leave-types/medical"))
                .andExpect(status().isConflict());
    }
}
