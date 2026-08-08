package com.practical.leavemaster.location;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocationController.class)
@WithMockUser
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LocationService locationService;

    @MockitoBean
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldReturnAllLocations() throws Exception {
        List<Location> locations = List.of(
                Location.builder().id("sg").locationName("Singapore").country("Singapore").build(),
                Location.builder().id("my").locationName("Malaysia").country("Malaysia").build()
        );
        when(locationService.findAll()).thenReturn(locations);

        mockMvc.perform(get("/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("sg"))
                .andExpect(jsonPath("$[1].id").value("my"));
    }

    @Test
    void shouldReturnLocationById() throws Exception {
        Location location = Location.builder().id("sg").locationName("Singapore").country("Singapore").build();
        when(locationService.findById("sg")).thenReturn(Optional.of(location));

        mockMvc.perform(get("/locations/sg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sg"))
                .andExpect(jsonPath("$.locationName").value("Singapore"))
                .andExpect(jsonPath("$.country").value("Singapore"));
    }

    @Test
    void shouldReturn404WhenLocationNotFound() throws Exception {
        when(locationService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/locations/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateLocation() throws Exception {
        Location location = Location.builder().id("sg").locationName("Singapore").country("Singapore").build();
        when(locationService.save(any(Location.class))).thenReturn(location);

        mockMvc.perform(post("/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("sg"))
                .andExpect(jsonPath("$.locationName").value("Singapore"));
    }

    @Test
    void shouldUpdateLocation() throws Exception {
        Location updated = Location.builder().id("sg").locationName("Singapore (Updated)").country("Singapore").state("Central").build();
        when(locationService.update(eq("sg"), any(Location.class))).thenReturn(updated);

        mockMvc.perform(put("/locations/sg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationName").value("Singapore (Updated)"))
                .andExpect(jsonPath("$.state").value("Central"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentLocation() throws Exception {
        when(locationService.update(eq("nonexistent"), any(Location.class)))
                .thenThrow(new LocationNotFoundException("nonexistent"));

        mockMvc.perform(put("/locations/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Location())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteLocation() throws Exception {
        doNothing().when(locationService).delete("sg");

        mockMvc.perform(delete("/locations/sg"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentLocation() throws Exception {
        doThrow(new LocationNotFoundException("nonexistent")).when(locationService).delete("nonexistent");

        mockMvc.perform(delete("/locations/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenDeletingLocationInUse() throws Exception {
        doThrow(new LocationInUseException("sg")).when(locationService).delete("sg");

        mockMvc.perform(delete("/locations/sg"))
                .andExpect(status().isConflict());
    }
}
