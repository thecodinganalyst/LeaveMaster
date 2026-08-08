package com.practical.leavemaster.location;

import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private TenantActivityService tenantActivityService;

    @InjectMocks
    private LocationService locationService;

    @Test
    void shouldReturnAllLocations() {
        List<Location> locations = List.of(
                Location.builder().id("sg").locationName("Singapore").country("Singapore").build(),
                Location.builder().id("my").locationName("Malaysia").country("Malaysia").build()
        );
        when(locationRepository.findAll()).thenReturn(locations);

        List<Location> result = locationService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnLocationById() {
        Location location = Location.builder().id("sg").locationName("Singapore").country("Singapore").build();
        when(locationRepository.findById("sg")).thenReturn(Optional.of(location));

        Optional<Location> result = locationService.findById("sg");

        assertThat(result).isPresent();
        assertThat(result.get().getLocationName()).isEqualTo("Singapore");
    }

    @Test
    void shouldSaveLocation() {
        Location location = Location.builder().id("sg").locationName("Singapore").country("Singapore").build();
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Location result = locationService.save(location);

        assertThat(result.getId()).isEqualTo("sg");
    }

    @Test
    void shouldUpdateLocation() {
        Location existing = Location.builder().id("sg").locationName("Singapore").country("Singapore").build();
        Location updated = Location.builder().id("sg").locationName("Singapore (Updated)").country("Singapore").state("Central").build();
        when(locationRepository.findById("sg")).thenReturn(Optional.of(existing));
        when(locationRepository.save(existing)).thenReturn(existing);

        Location result = locationService.update("sg", updated);

        assertThat(result.getLocationName()).isEqualTo("Singapore (Updated)");
        assertThat(result.getState()).isEqualTo("Central");
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentLocation() {
        when(locationRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.update("nonexistent", new Location()))
                .isInstanceOf(LocationNotFoundException.class);
    }

    @Test
    void shouldDeleteLocationWhenNotInUse() {
        when(locationRepository.findById("sg")).thenReturn(Optional.of(
                Location.builder().id("sg").locationName("Singapore").country("Singapore").build()));
        when(staffRepository.existsByLocationId("sg")).thenReturn(false);

        locationService.delete("sg");

        verify(locationRepository).deleteById("sg");
    }

    @Test
    void shouldThrowWhenDeletingLocationInUse() {
        when(locationRepository.findById("sg")).thenReturn(Optional.of(
                Location.builder().id("sg").locationName("Singapore").country("Singapore").build()));
        when(staffRepository.existsByLocationId("sg")).thenReturn(true);

        assertThatThrownBy(() -> locationService.delete("sg"))
                .isInstanceOf(LocationInUseException.class);

        verify(locationRepository, never()).deleteById("sg");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentLocation() {
        when(locationRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.delete("nonexistent"))
                .isInstanceOf(LocationNotFoundException.class);
    }
}
