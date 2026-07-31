package com.practical.leavemaster.leavetype;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
