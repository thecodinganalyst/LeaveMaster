package com.practicallimits.spring_template.staff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StaffController.class)
class StaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StaffService staffService;

    @Test
    void shouldReturnAllStaff() throws Exception {
        List<Staff> staffList = List.of(
                Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build(),
                Staff.builder().id("S002").name("Bob Jones").joinDate(LocalDate.of(2023, 6, 1)).workSchedule("WEEKDAYS").build()
        );
        when(staffService.findAll()).thenReturn(staffList);

        mockMvc.perform(get("/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("S001"))
                .andExpect(jsonPath("$[1].id").value("S002"));
    }

    @Test
    void shouldReturnStaffById() throws Exception {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
        when(staffService.findById("S001")).thenReturn(Optional.of(staff));

        mockMvc.perform(get("/staff/S001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("S001"))
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.workSchedule").value("WEEKDAYS"));
    }

    @Test
    void shouldReturn404WhenStaffNotFound() throws Exception {
        when(staffService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/staff/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateStaff() throws Exception {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
        when(staffService.save(any(Staff.class))).thenReturn(staff);

        mockMvc.perform(post("/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("S001"))
                .andExpect(jsonPath("$.name").value("Alice Smith"));
    }

    @Test
    void shouldUpdateStaff() throws Exception {
        Staff updated = Staff.builder().id("S001").name("Alice Johnson").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS_AND_SATURDAY").build();
        when(staffService.update(eq("S001"), any(Staff.class))).thenReturn(updated);

        mockMvc.perform(put("/staff/S001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.workSchedule").value("WEEKDAYS_AND_SATURDAY"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentStaff() throws Exception {
        when(staffService.update(eq("nonexistent"), any(Staff.class)))
                .thenThrow(new StaffNotFoundException("nonexistent"));

        mockMvc.perform(put("/staff/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Staff())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteStaff() throws Exception {
        doNothing().when(staffService).delete("S001");

        mockMvc.perform(delete("/staff/S001"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentStaff() throws Exception {
        doThrow(new StaffNotFoundException("nonexistent")).when(staffService).delete("nonexistent");

        mockMvc.perform(delete("/staff/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
