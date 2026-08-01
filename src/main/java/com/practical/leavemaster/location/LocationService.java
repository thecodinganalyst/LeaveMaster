package com.practical.leavemaster.location;

import com.practical.leavemaster.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final StaffRepository staffRepository;

    public List<Location> findAll() {
        return locationRepository.findAll();
    }

    public Optional<Location> findById(String id) {
        return locationRepository.findById(id);
    }

    public Location save(Location location) {
        return locationRepository.save(location);
    }

    public Location update(String id, Location updated) {
        Location existing = locationRepository.findById(id)
                .orElseThrow(() -> new LocationNotFoundException(id));
        existing.setLocationName(updated.getLocationName());
        existing.setCountry(updated.getCountry());
        existing.setState(updated.getState());
        return locationRepository.save(existing);
    }

    public void delete(String id) {
        locationRepository.findById(id)
                .orElseThrow(() -> new LocationNotFoundException(id));
        if (staffRepository.existsByLocationId(id)) {
            throw new LocationInUseException(id);
        }
        locationRepository.deleteById(id);
    }
}
