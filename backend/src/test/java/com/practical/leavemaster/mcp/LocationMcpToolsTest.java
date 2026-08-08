package com.practical.leavemaster.mcp;

import com.practical.leavemaster.location.Location;
import com.practical.leavemaster.location.LocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationMcpToolsTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationMcpTools locationMcpTools;

    @Test
    void shouldGetAllLocations() {
        List<Location> locations = List.of(Location.builder().id("sg").locationName("Singapore").build());
        when(locationService.findAll()).thenReturn(locations);

        List<Location> result = locationMcpTools.getAllLocations();

        assertThat(result).hasSize(1);
        verify(locationService).findAll();
    }

    @Test
    void shouldGetLocationById() {
        Location location = Location.builder().id("sg").locationName("Singapore").build();
        when(locationService.findById("sg")).thenReturn(Optional.of(location));

        Optional<Location> result = locationMcpTools.getLocationById("sg");

        assertThat(result).isPresent();
        verify(locationService).findById("sg");
    }

    @Test
    void shouldCreateLocation() {
        Location location = Location.builder().id("sg").locationName("Singapore").build();
        when(locationService.save(location)).thenReturn(location);

        Location result = locationMcpTools.createLocation(location);

        assertThat(result.getId()).isEqualTo("sg");
        verify(locationService).save(location);
    }

    @Test
    void shouldUpdateLocation() {
        Location location = Location.builder().id("sg").locationName("Updated").build();
        when(locationService.update("sg", location)).thenReturn(location);

        Location result = locationMcpTools.updateLocation("sg", location);

        assertThat(result.getLocationName()).isEqualTo("Updated");
        verify(locationService).update("sg", location);
    }

    @Test
    void shouldDeleteLocation() {
        doNothing().when(locationService).delete("sg");

        locationMcpTools.deleteLocation("sg");

        verify(locationService).delete("sg");
    }
}
