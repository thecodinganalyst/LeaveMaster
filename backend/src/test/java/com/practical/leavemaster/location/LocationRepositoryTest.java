package com.practical.leavemaster.location;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LocationRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void shouldSaveAndFindLocation() {
        Location location = Location.builder()
                .id("sg")
                .locationName("Singapore")
                .country("Singapore")
                .build();

        locationRepository.save(location);

        Optional<Location> found = locationRepository.findById("sg");
        assertThat(found).isPresent();
        assertThat(found.get().getLocationName()).isEqualTo("Singapore");
        assertThat(found.get().getCountry()).isEqualTo("Singapore");
        assertThat(found.get().getState()).isNull();
    }

    @Test
    void shouldSaveLocationWithState() {
        Location location = Location.builder()
                .id("us-ca")
                .locationName("California, USA")
                .country("USA")
                .state("California")
                .build();

        locationRepository.save(location);

        Optional<Location> found = locationRepository.findById("us-ca");
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo("California");
    }

    @Test
    void shouldFindAllLocations() {
        locationRepository.save(Location.builder().id("sg").locationName("Singapore").country("Singapore").build());
        locationRepository.save(Location.builder().id("my").locationName("Malaysia").country("Malaysia").build());

        List<Location> all = locationRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void shouldDeleteLocation() {
        locationRepository.save(Location.builder().id("sg").locationName("Singapore").country("Singapore").build());
        locationRepository.deleteById("sg");
        assertThat(locationRepository.findById("sg")).isEmpty();
    }
}
