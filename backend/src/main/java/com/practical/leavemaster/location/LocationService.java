package com.practical.leavemaster.location;

import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final StaffRepository staffRepository;
    private final TenantActivityService tenantActivityService;

    public List<Location> findAll() {
        return locationRepository.findAll();
    }

    public Optional<Location> findById(String id) {
        return locationRepository.findById(id);
    }

    public Location save(Location location) {
        Location saved = locationRepository.save(location);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public Location update(String id, Location updated) {
        Location existing = locationRepository.findById(id)
                .orElseThrow(() -> new LocationNotFoundException(id));
        existing.setLocationName(updated.getLocationName());
        existing.setCountry(updated.getCountry());
        existing.setState(updated.getState());
        Location saved = locationRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void delete(String id) {
        Location existing = locationRepository.findById(id)
                .orElseThrow(() -> new LocationNotFoundException(id));
        if (staffRepository.existsByLocationId(id)) {
            throw new LocationInUseException(id);
        }
        locationRepository.deleteById(id);
        tenantActivityService.touch(existing.getTenantId());
    }
}
